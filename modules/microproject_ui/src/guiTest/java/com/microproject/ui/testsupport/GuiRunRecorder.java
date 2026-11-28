/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.testsupport;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.Window;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Stream;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.RepaintManager;
import javax.swing.text.JTextComponent;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.microproject.ui.frame.DocumentFrame;
import com.microproject.ui.frame.GraphicManager;

/**
 * Records every GUI acceptance test as structured files under
 * {@code <microproject.gui.artifacts.dir>/runs/<class>/<test>/}. Recording never changes a test's
 * result unless {@code microproject.gui.failOnUncaught=true}. See {@code docs/gui-run-harness.md}.
 */
public final class GuiRunRecorder implements BeforeEachCallback, AfterEachCallback, TestWatcher {
	private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(GuiRunRecorder.class);
	private static final String SESSION_KEY = "session";
	private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
	private static final ObjectMapper LINE_MAPPER = new ObjectMapper();
	private static final List<String> COUNTED = List.of("log-warning", "log-severe", "uncaught", "edt-stall",
		"edt-violation", "step", "snapshot");
	private static final int MAX_VIOLATIONS = 200;
	private static final long STALL_THRESHOLD_NANOS = 500_000_000L;
	private static final long PING_INTERVAL_NANOS = 100_000_000L;

	private static volatile Session active;
	private static volatile GraphicManager attached;

	// ---- static API used by tests ----

	/** The running test's recorder; when nothing is recording every method of it does nothing. */
	public static Session current() {
		Session session = active;
		return session == null ? Session.NOOP : session;
	}

	/** Records a named step together with the Gantt chart's left and right edge. */
	public static void step(String name, Map<String, ?> attributes) {
		current().step(name, attributes);
	}

	/** Writes {@code state-<n>.json}. */
	public static void snapshot(String label) {
		current().snapshot(label);
	}

	/** Tells the recorder which main window to read snapshots from. */
	public static void attach(GraphicManager manager) {
		attached = manager;
	}

	private static boolean failOnUncaught() {
		return Boolean.parseBoolean(System.getProperty("microproject.gui.failOnUncaught", "false"));
	}

	// ---- extension callbacks ----

	@Override
	public void beforeEach(ExtensionContext context) {
		try {
			attached = null;
			Session session = new Session(testDirectory(context), context);
			context.getStore(NAMESPACE).put(SESSION_KEY, session);
			session.start();
			active = session;
		} catch (Throwable failure) {
			System.err.println("GuiRunRecorder could not start: " + failure);
		}
	}

	@Override
	public void afterEach(ExtensionContext context) {
		Session session = context.getStore(NAMESPACE).get(SESSION_KEY, Session.class);
		if (session == null) return;
		try {
			session.finish(context.getExecutionException().isPresent());
		} catch (Throwable failure) {
			System.err.println("GuiRunRecorder could not finish: " + failure);
		} finally {
			if (active == session) active = null;
			attached = null;
		}
		if (failOnUncaught()) {
			int uncaught = session.count("uncaught");
			int severe = session.count("log-severe");
			if (uncaught > 0 || severe > 0) {
				throw new AssertionError("GUI run recorded " + uncaught + " uncaught exception(s) and " + severe
					+ " SEVERE log record(s); see " + session.directory);
			}
		}
	}

	@Override
	public void testSuccessful(ExtensionContext context) {
		writeSummary(context, "passed", null);
	}

	@Override
	public void testFailed(ExtensionContext context, Throwable cause) {
		writeSummary(context, "failed", cause);
	}

	@Override
	public void testAborted(ExtensionContext context, Throwable cause) {
		writeSummary(context, "aborted", cause);
	}

	@Override
	public void testDisabled(ExtensionContext context, java.util.Optional<String> reason) {
		writeSummary(context, "disabled", null);
	}

	private static void writeSummary(ExtensionContext context, String status, Throwable cause) {
		try {
			Session session = context.getStore(NAMESPACE).get(SESSION_KEY, Session.class);
			Path directory = session != null ? session.directory : testDirectory(context);
			Files.createDirectories(directory);
			Map<String, Object> summary = new LinkedHashMap<>();
			summary.put("class", context.getRequiredTestClass().getSimpleName());
			summary.put("method", context.getTestMethod().map(method -> method.getName()).orElse(context.getDisplayName()));
			summary.put("status", status);
			summary.put("durationMillis", session == null ? 0L : session.durationMillis);
			Map<String, Object> counts = new LinkedHashMap<>();
			for (String key : COUNTED) counts.put(key, session == null ? 0 : session.count(key));
			summary.put("counts", counts);
			if (cause != null) {
				Map<String, Object> failure = new LinkedHashMap<>();
				failure.put("type", cause.getClass().getName());
				failure.put("message", cause.getMessage());
				failure.put("stackTrace", stack(cause.getStackTrace(), 20));
				summary.put("failure", failure);
			}
			MAPPER.writeValue(directory.resolve("summary.json").toFile(), summary);
		} catch (Throwable failure) {
			System.err.println("GuiRunRecorder could not write summary.json: " + failure);
		}
	}

	static Path artifactsDirectory() {
		return Path.of(System.getProperty("microproject.gui.artifacts.dir", "build/reports/guiTest-artifacts"));
	}

	private static Path testDirectory(ExtensionContext context) {
		String method = context.getTestMethod().map(m -> m.getName()).orElse("class");
		String name = safe(method);
		if (!context.getDisplayName().equals(method + "()")) name += "-" + safe(context.getDisplayName());
		if (name.length() > 100) name = name.substring(0, 100);
		return artifactsDirectory().resolve("runs").resolve(safe(context.getRequiredTestClass().getSimpleName()))
			.resolve(name);
	}

	private static String safe(String value) {
		return value.replaceAll("[^A-Za-z0-9_.-]", "_");
	}

	private static List<String> stack(StackTraceElement[] elements, int max) {
		List<String> lines = new ArrayList<>();
		for (int i = 0; i < elements.length && i < max; i++) lines.add(elements[i].toString());
		return lines;
	}

	// ---- per-test recorder ----

	/** The recorder of one test. The read methods are meant for tests of the recorder itself. */
	public static class Session {
		static final Session NOOP = new Session();

		private final Path directory;
		private final String className;
		private final String methodName;
		private final List<Map<String, Object>> events = new ArrayList<>();
		private final Map<String, Integer> counts = new LinkedHashMap<>();
		private final long startNanos = System.nanoTime();
		private final Object lock = new Object();
		private BufferedWriter writer;
		private int seq;
		private int snapshots;
		private int stalls;
		private int violations;
		private volatile long durationMillis;
		private volatile boolean running;
		private Thread watchdog;
		private Thread.UncaughtExceptionHandler previousHandler;
		private Thread.UncaughtExceptionHandler installedHandler;
		private LogHandler logHandler;
		private Logger rootLogger;
		private Logger runtimeLogger;
		private RepaintManager previousRepaintManager;
		private WatchingRepaintManager repaintManager;
		// EDT watchdog state, guarded by lock
		private long pendingPing;
		private long lastPing;
		private boolean stalled;
		private Map<String, Object> stallEvent;

		private Session() {
			directory = null;
			className = "";
			methodName = "";
		}

		private Session(Path directory, ExtensionContext context) {
			this.directory = directory;
			this.className = context.getRequiredTestClass().getSimpleName();
			this.methodName = context.getTestMethod().map(m -> m.getName()).orElse(context.getDisplayName());
		}

		private boolean isNoop() {
			return directory == null;
		}

		private void start() throws IOException {
			if (Files.exists(directory)) deleteTree(directory);
			Files.createDirectories(directory);
			writer = Files.newBufferedWriter(directory.resolve("events.jsonl"), StandardCharsets.UTF_8);
			running = true;
			record("test-start", Map.of("class", className, "method", methodName));
			previousHandler = Thread.getDefaultUncaughtExceptionHandler();
			installedHandler = (thread, error) -> {
				try {
					Map<String, Object> fields = new LinkedHashMap<>();
					fields.put("thread", thread.getName());
					fields.put("exception", error.getClass().getName());
					fields.put("message", error.getMessage());
					fields.put("stackTrace", stack(error.getStackTrace(), 15));
					record("uncaught", fields);
				} catch (Throwable ignored) {
					// recording must never disturb the test
				}
				if (previousHandler != null) previousHandler.uncaughtException(thread, error);
				else System.err.println("Exception in thread \"" + thread.getName() + "\" " + error);
			};
			Thread.setDefaultUncaughtExceptionHandler(installedHandler);
			logHandler = new LogHandler();
			rootLogger = Logger.getLogger("");
			runtimeLogger = Logger.getLogger("com.microproject.runtime");
			rootLogger.addHandler(logHandler);
			runtimeLogger.addHandler(logHandler);
			previousRepaintManager = RepaintManager.currentManager(null);
			repaintManager = new WatchingRepaintManager();
			repaintManager.setDoubleBufferingEnabled(previousRepaintManager.isDoubleBufferingEnabled());
			RepaintManager.setCurrentManager(repaintManager);
			watchdog = new Thread(this::watchEdt, "gui-run-recorder-edt-watchdog");
			watchdog.setDaemon(true);
			watchdog.start();
		}

		private void finish(boolean testFailed) {
			try {
				snapshot("final");
			} catch (Throwable ignored) {
				// already reported by snapshot
			}
			if (testFailed) writeComponents();
			running = false;
			if (watchdog != null) watchdog.interrupt();
			if (Thread.getDefaultUncaughtExceptionHandler() == installedHandler) {
				Thread.setDefaultUncaughtExceptionHandler(previousHandler);
			}
			if (rootLogger != null) rootLogger.removeHandler(logHandler);
			if (runtimeLogger != null) runtimeLogger.removeHandler(logHandler);
			if (RepaintManager.currentManager(null) == repaintManager) {
				RepaintManager.setCurrentManager(previousRepaintManager);
			}
			durationMillis = (System.nanoTime() - startNanos) / 1_000_000L;
			synchronized (lock) {
				Map<String, Object> end = new LinkedHashMap<>();
				end.put("durationMillis", durationMillis);
				recordLocked("test-end", end);
				try {
					writer.close();
					List<String> lines = new ArrayList<>();
					for (Map<String, Object> event : events) lines.add(LINE_MAPPER.writeValueAsString(event));
					Files.write(directory.resolve("events.jsonl"), lines, StandardCharsets.UTF_8);
				} catch (IOException failure) {
					System.err.println("GuiRunRecorder could not write events.jsonl: " + failure);
				}
			}
		}

		// ---- recording ----

		/** Records an event of any type; returns the stored event (live, for later additions). */
		Map<String, Object> record(String type, Map<String, ?> fields) {
			synchronized (lock) {
				return recordLocked(type, fields);
			}
		}

		private Map<String, Object> recordLocked(String type, Map<String, ?> fields) {
			Map<String, Object> event = new LinkedHashMap<>();
			event.put("seq", ++seq);
			event.put("t", (System.nanoTime() - startNanos) / 1_000_000L);
			event.put("wall", OffsetDateTime.now().toString());
			event.put("type", type);
			if (fields != null) event.putAll(fields);
			events.add(event);
			counts.merge(type, 1, Integer::sum);
			if (type.equals("log")) {
				counts.merge(Level.parse(String.valueOf(event.get("levelValue"))).intValue() >= Level.SEVERE.intValue()
					? "log-severe" : "log-warning", 1, Integer::sum);
			}
			try {
				if (writer != null && running) {
					writer.write(LINE_MAPPER.writeValueAsString(event));
					writer.newLine();
					writer.flush();
				}
			} catch (IOException failure) {
				System.err.println("GuiRunRecorder could not write an event: " + failure);
			}
			return event;
		}

		/** Records a step; its {@code gantt} item holds the chart's left and right edge. */
		public void step(String name, Map<String, ?> attributes) {
			if (isNoop()) return;
			try {
				Map<String, Object> fields = new LinkedHashMap<>();
				fields.put("name", name);
				if (attributes != null) fields.put("attributes", attributes);
				fields.put("gantt", ganttNow());
				record("step", fields);
			} catch (Throwable failure) {
				System.err.println("GuiRunRecorder step failed: " + failure);
			}
		}

		private Map<String, Object> ganttNow() {
			try {
				GraphicManager manager = manager();
				if (manager == null) return null;
				Map<String, Object> result = new LinkedHashMap<>();
				GuiAcceptanceSupport.runOnEdtWithTimeout(() -> {
					DocumentFrame frame = manager.getCurrentFrame();
					Map<String, Object> gantt = frame == null ? null : GuiStateSnapshot.gantt(frame);
					if (gantt != null) result.putAll(gantt);
				}, "Gantt edge read");
				return result.isEmpty() ? null : result;
			} catch (Throwable failure) {
				return null;
			}
		}

		private static GraphicManager manager() {
			GraphicManager manager = attached;
			return manager != null ? manager : GuiStateSnapshot.discover();
		}

		/** Writes {@code state-<n>.json} and records a {@code snapshot} event. */
		public void snapshot(String label) {
			if (isNoop()) return;
			int number;
			synchronized (lock) {
				number = ++snapshots;
			}
			Map<String, Object> state = new LinkedHashMap<>();
			state.put("label", label);
			try {
				state.putAll(GuiStateSnapshot.capture(manager()));
			} catch (Throwable failure) {
				state.put("error", failure.toString());
			}
			try {
				MAPPER.writeValue(directory.resolve("state-" + number + ".json").toFile(), state);
			} catch (Throwable failure) {
				System.err.println("GuiRunRecorder could not write state-" + number + ".json: " + failure);
			}
			Map<String, Object> fields = new LinkedHashMap<>();
			fields.put("label", label);
			fields.put("file", "state-" + number + ".json");
			record("snapshot", fields);
		}

		// ---- read API ----

		/** All events recorded so far. */
		public List<Map<String, Object>> events() {
			synchronized (lock) {
				return new ArrayList<>(events);
			}
		}

		/** The events of one type. */
		public List<Map<String, Object>> events(String type) {
			List<Map<String, Object>> result = new ArrayList<>();
			for (Map<String, Object> event : events()) {
				if (type.equals(event.get("type"))) result.add(event);
			}
			return result;
		}

		/** The count of one summary key ({@code log-severe}, {@code uncaught}, ...). */
		public int count(String key) {
			synchronized (lock) {
				return counts.getOrDefault(key, 0);
			}
		}

		/** The directory of this test's files. */
		public Path directory() {
			return directory;
		}

		// ---- EDT watchdog ----

		private void watchEdt() {
			while (running) {
				try {
					Thread.sleep(25);
				} catch (InterruptedException interrupted) {
					return;
				}
				try {
					long now = System.nanoTime();
					synchronized (lock) {
						if (pendingPing == 0) {
							if (now - lastPing >= PING_INTERVAL_NANOS) {
								pendingPing = now;
								lastPing = now;
								long sent = now;
								EventQueue.invokeLater(() -> pong(sent));
							}
						} else if (!stalled && now - pendingPing >= STALL_THRESHOLD_NANOS) {
							stalled = true;
							int number = ++stalls;
							Map<String, Object> fields = new LinkedHashMap<>();
							fields.put("dump", "edt-stall-" + number + ".txt");
							stallEvent = recordLocked("edt-stall", fields);
							writeDump(number);
						}
					}
				} catch (Throwable failure) {
					System.err.println("GuiRunRecorder watchdog failed: " + failure);
				}
			}
		}

		private void pong(long sent) {
			synchronized (lock) {
				if (stalled && stallEvent != null) {
					stallEvent.put("durationMillis", (System.nanoTime() - sent) / 1_000_000L);
				}
				stalled = false;
				stallEvent = null;
				pendingPing = 0;
			}
		}

		private void writeDump(int number) {
			try {
				StringBuilder text = new StringBuilder();
				for (ThreadInfo info : ManagementFactory.getThreadMXBean().dumpAllThreads(true, true)) {
					text.append('"').append(info.getThreadName()).append("\" ").append(info.getThreadState()).append('\n');
					for (StackTraceElement element : info.getStackTrace()) text.append("\tat ").append(element).append('\n');
					text.append('\n');
				}
				Files.writeString(directory.resolve("edt-stall-" + number + ".txt"), text.toString(), StandardCharsets.UTF_8);
			} catch (Throwable failure) {
				System.err.println("GuiRunRecorder could not write the thread dump: " + failure);
			}
		}

		// ---- components ----

		private void writeComponents() {
			try {
				List<Object> windows = new ArrayList<>();
				GuiAcceptanceSupport.runOnEdtWithTimeout(() -> {
					for (Window window : Window.getWindows()) {
						if (window.isShowing()) windows.add(describe(window, 0));
					}
				}, "component dump");
				MAPPER.writeValue(directory.resolve("components-final.json").toFile(), windows);
			} catch (Throwable failure) {
				System.err.println("GuiRunRecorder could not write components-final.json: " + failure);
			}
		}

		private static Map<String, Object> describe(Component component, int depth) {
			Map<String, Object> map = new LinkedHashMap<>();
			map.put("class", component.getClass().getName());
			map.put("name", component.getName());
			String text = null;
			if (component instanceof AbstractButton button) text = button.getText();
			else if (component instanceof JLabel label) text = label.getText();
			else if (component instanceof JTextComponent field) text = field.getText();
			else if (component instanceof Frame frame) text = frame.getTitle();
			else if (component instanceof Dialog dialog) text = dialog.getTitle();
			map.put("text", text);
			map.put("visible", component.isVisible());
			map.put("enabled", component.isEnabled());
			Rectangle rectangle = component.getBounds();
			if (component.isShowing()) {
				rectangle = new Rectangle(component.getLocationOnScreen(), component.getSize());
			}
			map.put("bounds", List.of(rectangle.x, rectangle.y, rectangle.width, rectangle.height));
			if (component instanceof Container container && depth < 40) {
				List<Object> children = new ArrayList<>();
				for (Component child : container.getComponents()) children.add(describe(child, depth + 1));
				map.put("children", children);
			}
			return map;
		}

		// ---- log handler and repaint manager ----

		private final class LogHandler extends Handler {
			private LogRecord last;

			@Override
			public void publish(LogRecord record) {
				if (record == null || record.getLevel().intValue() < Level.WARNING.intValue()) return;
				synchronized (this) {
					// one record reaches both loggers when it propagates to the root
					if (record == last) return;
					last = record;
				}
				try {
					Map<String, Object> fields = new LinkedHashMap<>();
					fields.put("logger", record.getLoggerName());
					fields.put("level", record.getLevel().getName());
					fields.put("levelValue", record.getLevel().getName());
					fields.put("message", new SimpleFormatter().formatMessage(record));
					Throwable thrown = record.getThrown();
					if (thrown != null) {
						fields.put("exception", thrown.getClass().getName());
						fields.put("exceptionMessage", thrown.getMessage());
					}
					record("log", fields);
				} catch (Throwable ignored) {
					// recording must never disturb the test
				}
			}

			@Override
			public void flush() {
			}

			@Override
			public void close() {
			}
		}

		private final class WatchingRepaintManager extends RepaintManager {
			@Override
			public synchronized void addInvalidComponent(JComponent invalidComponent) {
				check("addInvalidComponent", false);
				super.addInvalidComponent(invalidComponent);
			}

			@Override
			public void addDirtyRegion(JComponent component, int x, int y, int w, int h) {
				check("addDirtyRegion", true);
				super.addDirtyRegion(component, x, y, w, h);
			}

			private void check(String method, boolean filterDirectRepaint) {
				if (EventQueue.isDispatchThread() || !running) return;
				try {
					StackTraceElement[] stack = new Throwable().getStackTrace();
					if (filterDirectRepaint && isDirectRepaint(stack)) return;
					synchronized (lock) {
						if (++violations > MAX_VIOLATIONS) return;
					}
					Map<String, Object> fields = new LinkedHashMap<>();
					fields.put("method", method);
					fields.put("thread", Thread.currentThread().getName());
					fields.put("stackTrace", stack(stack, 15));
					record("edt-violation", fields);
				} catch (Throwable ignored) {
					// recording must never disturb the test
				}
			}
		}

		/** repaint() itself may be called from any thread; only Swing calling it on a change is a violation. */
		private static boolean isDirectRepaint(StackTraceElement[] stack) {
			for (StackTraceElement frame : stack) {
				String cls = frame.getClassName();
				if (cls.contains("GuiRunRecorder") || cls.equals("javax.swing.RepaintManager")) continue;
				if (frame.getMethodName().equals("repaint") && (cls.startsWith("javax.swing.") || cls.startsWith("java.awt."))) {
					continue;
				}
				return !(cls.startsWith("javax.swing.") || cls.startsWith("java.awt.") || cls.startsWith("sun.swing.")
					|| cls.startsWith("sun.awt."));
			}
			return true;
		}
	}

	private static void deleteTree(Path directory) throws IOException {
		try (Stream<Path> paths = Files.walk(directory)) {
			for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
		}
	}

	static String stackText(Throwable error) {
		StringWriter text = new StringWriter();
		error.printStackTrace(new PrintWriter(text));
		return text.toString();
	}
}
