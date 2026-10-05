/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.testsupport;

import java.awt.KeyboardFocusManager;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.geom.AffineTransform;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.opentest4j.TestAbortedException;

/** Shared last-resort desktop cleanup for every GUI acceptance fixture. */
public final class GuiEnvironmentExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback,
	AfterEachCallback, TestExecutionExceptionHandler {
	private static final ExtensionContext.Namespace SESSION_NAMESPACE =
		ExtensionContext.Namespace.create(GuiEnvironmentExtension.class);
	private static final String SESSION_LEASE = "desktop-session-lease";
	private static final String OVERLAP_MARK = "foreground-overlap-mark";
	private static final String TEST_FAILURE = "original-test-failure";

	private record DesktopSession(GuiDesktopSessionCoordinator.Lease lease,
		GuiDesktopSessionCoordinator.ForegroundMonitor monitor) {
		private void close() throws Exception {
			Exception failure = null;
			if (monitor != null) {
				try {
					monitor.close();
				} catch (Exception closeFailure) {
					failure = closeFailure;
				}
			}
			try {
				lease.close();
			} catch (Exception closeFailure) {
				if (failure == null) failure = closeFailure;
				else failure.addSuppressed(closeFailure);
			}
			if (failure != null) throw failure;
		}
	}

	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
		GuiDesktopSessionCoordinator.failIfEnvironmentContended();
		GuiDesktopSessionCoordinator.Lease lease;
		try {
			lease = GuiDesktopSessionCoordinator.acquireForCurrentProject();
		} catch (Exception | Error failure) {
			try {
				GuiDesktopSessionCoordinator.markEnvironmentContended("GUI_ENVIRONMENT_CONTENDED: "
					+ failure.getClass().getSimpleName() + ": " + failure.getMessage());
			} catch (IOException markerFailure) {
				failure.addSuppressed(markerFailure);
			}
			throw failure;
		}
		GuiDesktopSessionCoordinator.ForegroundMonitor monitor = null;
		try {
			String configuredDirectory = System.getProperty("microproject.gui.artifacts.dir", "build/reports/guiTest-artifacts");
			monitor = GuiDesktopSessionCoordinator.monitorForegroundOverlaps(Path.of(configuredDirectory));
			context.getStore(SESSION_NAMESPACE).put(SESSION_LEASE, new DesktopSession(lease, monitor));
		} catch (Exception | Error failure) {
			try {
				GuiDesktopSessionCoordinator.markEnvironmentContended("GUI_ENVIRONMENT_MONITOR_UNAVAILABLE: "
					+ failure.getClass().getSimpleName() + ": " + failure.getMessage());
			} catch (IOException markerFailure) {
				failure.addSuppressed(markerFailure);
			}
			if (monitor != null) {
				try {
					monitor.close();
				} catch (Exception closeFailure) {
					failure.addSuppressed(closeFailure);
				}
			}
			try {
				lease.close();
			} catch (Exception closeFailure) {
				failure.addSuppressed(closeFailure);
			}
			throw failure;
		}
	}

	@Override
	public void afterAll(ExtensionContext context) throws Exception {
		Throwable failure = null;
		try {
			cleanupDesktop("GUI fixture class desktop cleanup");
		} catch (Throwable cleanupFailure) {
			failure = cleanupFailure;
		}
		DesktopSession session = context.getStore(SESSION_NAMESPACE).remove(SESSION_LEASE, DesktopSession.class);
		if (session != null) {
			try {
				session.close();
			} catch (Throwable closeFailure) {
				if (failure == null) failure = closeFailure;
				else failure.addSuppressed(closeFailure);
			}
		}
		if (failure instanceof Exception checked) throw checked;
		if (failure instanceof Error error) throw error;
		if (failure != null) throw new AssertionError("GUI desktop session cleanup failed", failure);
	}

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		GuiDesktopSessionCoordinator.failIfEnvironmentContended();
		cleanupDesktop("GUI fixture pre-test cleanup");
		DesktopSession session = context.getParent().map(parent -> parent.getStore(SESSION_NAMESPACE)
			.get(SESSION_LEASE, DesktopSession.class)).orElse(null);
		if (session != null && session.monitor() != null) {
			GuiDesktopSessionCoordinator.beginRobotTest(session.monitor());
			context.getStore(ExtensionContext.Namespace.create(GuiEnvironmentExtension.class, context.getUniqueId()))
				.put(OVERLAP_MARK, session.monitor().mark());
		}
	}

	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		String overlapEvents = "";
		IOException monitorFailure = null;
		try {
			overlapEvents = foregroundOverlapEvents(context);
		} catch (IOException failure) {
			monitorFailure = failure;
			overlapEvents = "GUI_ENVIRONMENT_MONITOR_UNAVAILABLE: " + failure.getMessage();
		}
		DesktopSession session = context.getParent().map(parent -> parent.getStore(SESSION_NAMESPACE)
			.get(SESSION_LEASE, DesktopSession.class)).orElse(null);
		if (session != null) GuiDesktopSessionCoordinator.endRobotTest(session.monitor());
		if (!overlapEvents.isBlank() || monitorFailure != null) {
			String reason = monitorFailure != null
				? "GUI_ENVIRONMENT_MONITOR_UNAVAILABLE: desktop ownership could not be verified. "
					+ monitorFailure.getMessage()
				: "GUI_ENVIRONMENT_CONTENDED: a foreign foreground window overlapped a test window during physical acceptance.";
			GuiDesktopSessionCoordinator.DesktopContendedException contention =
				new GuiDesktopSessionCoordinator.DesktopContendedException(reason
					+ " The original assertion is retained and no retry was applied. See the foreground-overlap artifact.");
			if (monitorFailure != null) contention.addSuppressed(monitorFailure);
			Throwable testFailure = context.getStore(ExtensionContext.Namespace.create(
				GuiEnvironmentExtension.class, context.getUniqueId())).get(TEST_FAILURE, Throwable.class);
			if (testFailure == null) testFailure = context.getExecutionException().orElse(null);
			try {
				captureFailure(context);
			} catch (Throwable captureFailure) {
				contention.addSuppressed(captureFailure);
			}
			try {
				GuiDesktopSessionCoordinator.markEnvironmentContended(contention.getMessage()
					+ System.lineSeparator() + overlapEvents);
			} catch (IOException markerFailure) {
				contention.addSuppressed(markerFailure);
			}
			try {
				cleanupDesktop("GUI fixture contended-test cleanup");
			} catch (Throwable cleanupFailure) {
				contention.addSuppressed(cleanupFailure);
			}
			if (testFailure != null) {
				testFailure.addSuppressed(contention);
				return;
			}
			throw contention;
		}
		cleanupDesktop("GUI fixture desktop cleanup");
	}

	@Override
	public void handleTestExecutionException(ExtensionContext context, Throwable failure) throws Throwable {
		context.getStore(ExtensionContext.Namespace.create(GuiEnvironmentExtension.class, context.getUniqueId()))
			.put(TEST_FAILURE, failure);
		if (failure instanceof TestAbortedException) throw failure;
		try {
			captureFailure(context);
		} catch (Throwable captureFailure) {
			failure.addSuppressed(captureFailure);
		}
		throw failure;
	}

	private static void captureFailure(ExtensionContext context) throws Exception {
		String configuredDirectory = System.getProperty("microproject.gui.artifacts.dir", "build/reports/guiTest-artifacts");
		Path directory = Path.of(configuredDirectory);
		Files.createDirectories(directory);
		String safeName = (context.getRequiredTestClass().getSimpleName() + "-" + context.getDisplayName())
			.replaceAll("[^A-Za-z0-9_.-]", "_");
		writeEnvironmentSnapshot(directory, safeName);
		writeDesktopSessionSnapshot(directory, safeName);
		writeForegroundOverlapEvidence(context, directory, safeName);
		if (GraphicsEnvironment.isHeadless()) return;
		Rectangle desktop = null;
		for (GraphicsDevice device : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
			Rectangle bounds = device.getDefaultConfiguration().getBounds();
			desktop = desktop == null ? new Rectangle(bounds) : desktop.union(bounds);
		}
		if (desktop != null && desktop.width > 0 && desktop.height > 0) {
			BufferedImage screenshot = new Robot().createScreenCapture(desktop);
			ImageIO.write(screenshot, "png", directory.resolve(safeName + ".failure.png").toFile());
		}
		StringBuilder windows = new StringBuilder();
		for (Window window : Window.getWindows()) {
			windows.append(window.getClass().getName())
				.append(" showing=").append(window.isShowing())
				.append(" displayable=").append(window.isDisplayable())
				.append(" bounds=").append(window.getBounds())
				.append(" title=").append(window instanceof java.awt.Frame frame ? frame.getTitle() : "")
				.append(System.lineSeparator());
		}
		Files.writeString(directory.resolve(safeName + ".windows.txt"), windows.toString());
		captureNativeDesktopState(directory, safeName);
	}

	private static void writeDesktopSessionSnapshot(Path directory, String safeName) throws IOException {
		Path lockFile = Path.of(System.getProperty("java.io.tmpdir"), "microproject-gui-acceptance.lock");
		Path ownerFile = lockFile.resolveSibling(lockFile.getFileName() + ".owner");
		String state = "lockFile=" + lockFile + System.lineSeparator()
			+ "testProcessPid=" + ProcessHandle.current().pid() + System.lineSeparator()
			+ "lockOwner=" + (Files.exists(ownerFile) ? Files.readString(ownerFile) : "unavailable")
			+ System.lineSeparator();
		Files.writeString(directory.resolve(safeName + ".desktop-session.txt"), state);
	}

	private static void writeForegroundOverlapEvidence(ExtensionContext context, Path directory, String safeName) {
		try {
			DesktopSession session = context.getParent().map(parent -> parent.getStore(SESSION_NAMESPACE)
				.get(SESSION_LEASE, DesktopSession.class)).orElse(null);
			if (session == null || session.monitor() == null) return;
			String observed = foregroundOverlapEvents(context);
			String classification = observed.contains("GUI_ENVIRONMENT_MONITOR_UNAVAILABLE")
				? "GUI_ENVIRONMENT_MONITOR_UNAVAILABLE: desktop contention could not be ruled out."
				: observed.isBlank()
					? "No foreign foreground window overlapping a test window was observed during this test."
				: "GUI_ENVIRONMENT_CONTENDED_CANDIDATE: a foreign foreground window overlapped a test window during this test. "
					+ "The original assertion remains a failure; no retry or suppression was applied.";
			String process = session.monitor().processDiagnostics();
			String evidence = classification + System.lineSeparator() + observed + System.lineSeparator()
				+ "monitorProcessOutput=" + process;
			Files.writeString(directory.resolve(safeName + ".foreground-overlap.txt"), evidence);
		} catch (Exception diagnosticFailure) {
			try {
				Files.writeString(directory.resolve(safeName + ".foreground-overlap.txt"),
					"Foreground overlap evidence unavailable: " + diagnosticFailure.getClass().getSimpleName());
			} catch (IOException ignored) {
				// Preserve the original GUI assertion if diagnostic storage also fails.
			}
		}
	}

	private static String foregroundOverlapEvents(ExtensionContext context) throws IOException {
		DesktopSession session = context.getParent().map(parent -> parent.getStore(SESSION_NAMESPACE)
			.get(SESSION_LEASE, DesktopSession.class)).orElse(null);
		if (session == null || session.monitor() == null) return "";
		Long mark = context.getStore(ExtensionContext.Namespace.create(GuiEnvironmentExtension.class, context.getUniqueId()))
			.get(OVERLAP_MARK, Long.class);
		return mark == null ? "" : session.monitor().eventsSince(mark);
	}

	private static void captureNativeDesktopState(Path directory, String safeName) throws IOException {
		if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("windows")) return;
		Path projectDirectory = Path.of(System.getProperty("microproject.project.dir", "."));
		Path script = projectDirectory.resolve(".github/scripts/capture-gui-desktop-state.ps1");
		Path output = directory.resolve(safeName + ".native-desktop.txt");
		if (!Files.isRegularFile(script)) {
			Files.writeString(output, "native window snapshot unavailable: " + script + " not found");
			return;
		}
		Process process = null;
		try {
			process = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-WindowStyle", "Hidden",
				"-ExecutionPolicy", "Bypass", "-File", script.toString(), "-TargetPid",
				Long.toString(ProcessHandle.current().pid())).redirectErrorStream(true).start();
			if (!process.waitFor(5, TimeUnit.SECONDS)) {
				process.destroyForcibly();
				Files.writeString(output, "native window snapshot timed out after 5 seconds");
				return;
			}
			String snapshot = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
			Files.writeString(output, "exitCode=" + process.exitValue() + System.lineSeparator() + snapshot);
		} catch (InterruptedException interrupted) {
			if (process != null) process.destroyForcibly();
			Thread.currentThread().interrupt();
			Files.writeString(output, "native window snapshot interrupted");
		} catch (IOException exception) {
			Files.writeString(output, "native window snapshot failed: " + exception.getClass().getSimpleName());
		}
	}

	static void writeEnvironmentSnapshot(Path directory, String safeName) throws Exception {
		Files.createDirectories(directory);
		StringBuilder environment = new StringBuilder()
			.append("os.name=").append(System.getProperty("os.name")).append(System.lineSeparator())
			.append("os.version=").append(System.getProperty("os.version")).append(System.lineSeparator())
			.append("os.kernelVersion=").append(windowsKernelVersion()).append(System.lineSeparator())
			.append("os.arch=").append(System.getProperty("os.arch")).append(System.lineSeparator())
			.append("java.version=").append(System.getProperty("java.version")).append(System.lineSeparator())
			.append("locale.default=").append(Locale.getDefault().toLanguageTag()).append(System.lineSeparator())
			.append("locale.language=").append(System.getProperty("user.language")).append(System.lineSeparator())
			.append("locale.country=").append(System.getProperty("user.country")).append(System.lineSeparator())
			.append("java2d.uiScale=").append(System.getProperty("sun.java2d.uiScale", "default"))
				.append(System.lineSeparator());
		if (!GraphicsEnvironment.isHeadless()) {
			for (GraphicsDevice device : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
				var configuration = device.getDefaultConfiguration();
				Rectangle bounds = configuration.getBounds();
				java.awt.Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(configuration);
				Rectangle usable = new Rectangle(bounds.x + insets.left, bounds.y + insets.top,
					bounds.width - insets.left - insets.right, bounds.height - insets.top - insets.bottom);
				AffineTransform transform = configuration.getDefaultTransform();
				environment.append("screen=").append(device.getIDstring())
					.append(" bounds=").append(bounds)
					.append(" insets=").append(insets)
					.append(" usable=").append(usable)
					.append(" scale=").append(transform.getScaleX()).append('x').append(transform.getScaleY())
					.append(System.lineSeparator());
			}
		}
		Files.writeString(directory.resolve(safeName + ".environment.txt"), environment.toString());
	}

	private static String windowsKernelVersion() {
		if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("windows")) return "not-applicable";
		Process process = null;
		try {
			process = new ProcessBuilder("cmd.exe", "/c", "ver").redirectErrorStream(true).start();
			if (!process.waitFor(3, TimeUnit.SECONDS)) {
				process.destroyForcibly();
				return "query timed out";
			}
			String output = new String(process.getInputStream().readAllBytes(), java.nio.charset.Charset.defaultCharset())
				.replace('\r', ' ').replace('\n', ' ').trim();
			return process.exitValue() == 0 ? output : "query failed: " + output;
		} catch (Exception exception) {
			if (process != null) process.destroyForcibly();
			return "query failed: " + exception.getClass().getSimpleName();
		}
	}

	private static void cleanupDesktop(String description) throws Exception {
		GuiAcceptanceSupport.runOnEdtWithTimeout(() -> {
			for (Window window : Window.getWindows()) {
				if (window.isDisplayable()) window.dispose();
			}
			KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
			Toolkit.getDefaultToolkit().sync();
		}, description);
	}
}
