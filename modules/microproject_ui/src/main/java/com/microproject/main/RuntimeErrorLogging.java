/* Copyright (c) 2026 microProject. SPDX-License-Identifier: MIT */
package com.microproject.main;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.function.Consumer;

/** Installs a bounded, always-on log for serious runtime failures. */
final class RuntimeErrorLogging {
	private static final int LOG_SIZE_BYTES = 2 * 1024 * 1024;
	private static final int LOG_FILE_COUNT = 3;
	private static final int MAX_THROWABLES = 8;
	private static final int MAX_STACK_FRAMES = 32;
	private static final int MAX_RECORD_CHARS = 100_000;
	private static final int MAX_THROWABLE_CHARS = 40_000;
	private static final int FRAMES_PER_THROWABLE = MAX_STACK_FRAMES / MAX_THROWABLES;
	private static final Logger LOGGER = Logger.getLogger("com.microproject.runtime");
	private static final Logger WATCHDOG_LOGGER = Logger.getLogger("com.microproject.ui.diagnostics.EdtWatchdog");
	private static Handler handler;
	private static Level previousRuntimeLevel;
	private static Level previousWatchdogLevel;
	private static boolean previousRuntimeParents;
	private static boolean previousWatchdogParents;
	private static Thread.UncaughtExceptionHandler previousUncaughtHandler;
	private static Thread.UncaughtExceptionHandler installedUncaughtHandler;

	private RuntimeErrorLogging() { }

	static synchronized boolean configure(Path logDirectory) {
		return configure(logDirectory, LOG_SIZE_BYTES, LOG_FILE_COUNT);
	}

	static synchronized boolean configure(Path logDirectory, int sizeLimit, int fileCount) {
		return configure(logDirectory, sizeLimit, fileCount, Thread::setDefaultUncaughtExceptionHandler);
	}

	static synchronized boolean configure(Path logDirectory, int sizeLimit, int fileCount,
			Consumer<Thread.UncaughtExceptionHandler> defaultHandlerInstaller) {
		if (handler != null) return true;
		FileHandler fileHandler = null;
		Logger rootLogger = Logger.getLogger("");
		boolean runtimeParents = LOGGER.getUseParentHandlers();
		boolean watchdogParents = WATCHDOG_LOGGER.getUseParentHandlers();
		Level runtimeLevel = LOGGER.getLevel();
		Level watchdogLevel = WATCHDOG_LOGGER.getLevel();
		Thread.UncaughtExceptionHandler oldUncaughtHandler = Thread.getDefaultUncaughtExceptionHandler();
		boolean rootAttached = false;
		boolean runtimeAttached = false;
		boolean watchdogAttached = false;
		try {
			Files.createDirectories(logDirectory);
			fileHandler = new FileHandler(logDirectory.resolve("errors-%g.log").toString(),
				sizeLimit, fileCount, true);
			fileHandler.setLevel(Level.WARNING);
			fileHandler.setFormatter(new RedactedFormatter());
			rootLogger.addHandler(fileHandler);
			rootAttached = true;
			LOGGER.setUseParentHandlers(false);
			LOGGER.setLevel(Level.SEVERE);
			LOGGER.addHandler(fileHandler);
			runtimeAttached = true;
			WATCHDOG_LOGGER.setUseParentHandlers(false);
			WATCHDOG_LOGGER.setLevel(Level.WARNING);
			WATCHDOG_LOGGER.addHandler(fileHandler);
			watchdogAttached = true;
			Thread.UncaughtExceptionHandler uncaughtHandler = (thread, error) -> {
				try {
					LOGGER.log(Level.SEVERE, "Uncaught exception", error);
				} catch (RuntimeException ignored) {
					System.err.println("microProject could not write an uncaught exception to its error log.");
				}
			};
			defaultHandlerInstaller.accept(uncaughtHandler);
			handler = fileHandler;
			previousRuntimeParents = runtimeParents;
			previousWatchdogParents = watchdogParents;
			previousRuntimeLevel = runtimeLevel;
			previousWatchdogLevel = watchdogLevel;
			previousUncaughtHandler = oldUncaughtHandler;
			installedUncaughtHandler = uncaughtHandler;
			return true;
		} catch (IOException | SecurityException | IllegalArgumentException failure) {
			if (watchdogAttached) safelyRemoveHandler(WATCHDOG_LOGGER, fileHandler);
			if (runtimeAttached) safelyRemoveHandler(LOGGER, fileHandler);
			if (rootAttached) safelyRemoveHandler(rootLogger, fileHandler);
			try { LOGGER.setUseParentHandlers(runtimeParents); } catch (SecurityException ignored) { }
			try { LOGGER.setLevel(runtimeLevel); } catch (SecurityException ignored) { }
			try { WATCHDOG_LOGGER.setUseParentHandlers(watchdogParents); } catch (SecurityException ignored) { }
			try { WATCHDOG_LOGGER.setLevel(watchdogLevel); } catch (SecurityException ignored) { }
			if (fileHandler != null) fileHandler.close();
			try {
				if (Thread.getDefaultUncaughtExceptionHandler() != oldUncaughtHandler)
					Thread.setDefaultUncaughtExceptionHandler(oldUncaughtHandler);
			} catch (SecurityException ignored) { }
			System.err.println("microProject error logging is unavailable; startup will continue.");
			return false;
		}
	}

	private static void safelyRemoveHandler(Logger logger, Handler handler) {
		try { logger.removeHandler(handler); } catch (SecurityException ignored) { }
	}

	static synchronized void close() {
		if (handler == null) return;
		Logger.getLogger("").removeHandler(handler);
		LOGGER.removeHandler(handler);
		WATCHDOG_LOGGER.removeHandler(handler);
		handler.close();
		try { LOGGER.setUseParentHandlers(previousRuntimeParents); } catch (SecurityException ignored) { }
		try { LOGGER.setLevel(previousRuntimeLevel); } catch (SecurityException ignored) { }
		try { WATCHDOG_LOGGER.setUseParentHandlers(previousWatchdogParents); } catch (SecurityException ignored) { }
		try { WATCHDOG_LOGGER.setLevel(previousWatchdogLevel); } catch (SecurityException ignored) { }
		try {
			if (Thread.getDefaultUncaughtExceptionHandler() == installedUncaughtHandler)
				Thread.setDefaultUncaughtExceptionHandler(previousUncaughtHandler);
		} catch (SecurityException ignored) { }
		handler = null;
	}

	static Path defaultLogDirectory(String localAppData, String osName, String userHome) {
		if (localAppData != null && !localAppData.isBlank())
			return Path.of(localAppData, "microProject", "logs");
		if (osName != null && osName.toLowerCase(java.util.Locale.ROOT).contains("win"))
			return Path.of(userHome == null || userHome.isBlank() ? "." : userHome, "AppData", "Local", "microProject", "logs");
		return Path.of(userHome == null || userHome.isBlank() ? "." : userHome, ".microproject", "logs");
	}

	private static final class RedactedFormatter extends Formatter {
		@Override
		public String format(LogRecord record) {
			StringBuilder text = new StringBuilder().append(record.getInstant()).append(' ')
					.append(record.getLevel().getName()).append(' ')
					.append(trustedTypeName(record.getLoggerName()) ? record.getLoggerName() : "external")
					.append(" - serious runtime failure");
			String sourceClass = record.getSourceClassName();
			String sourceMethod = record.getSourceMethodName();
			if (trustedTypeName(sourceClass) && trustedMethodName(sourceMethod))
				text.append(" [source=").append(sourceClass).append('#').append(sourceMethod).append(']');
			else
				text.append(" [source=external]");
			if (com.microproject.ui.diagnostics.EdtWatchdog.isDiagnosticLogRecord(record)) {
				String dump = record.getMessage();
				if (dump != null) text.append('\n').append(dump, 0, Math.min(dump.length(), 100_000));
			}
			Throwable thrown = record.getThrown();
			if (thrown != null) {
				StringBuilder throwableText = new StringBuilder();
				appendThrowable(throwableText, thrown, Collections.newSetFromMap(new IdentityHashMap<>()),
						new int[] { 0 }, new int[] { 0 }, 0, "thrown");
				appendLimited(text, throwableText, MAX_RECORD_CHARS);
			}
			if (text.length() > MAX_RECORD_CHARS) text.setLength(MAX_RECORD_CHARS);
			return text.append(System.lineSeparator()).toString();
		}

		private static void appendThrowable(StringBuilder text, Throwable error, Set<Throwable> seen,
				int[] throwableCount, int[] frameCount, int depth, String relation) {
			if (error == null || depth >= MAX_THROWABLES || throwableCount[0] >= MAX_THROWABLES
					|| text.length() >= MAX_THROWABLE_CHARS || !seen.add(error)) return;
			throwableCount[0]++;
			appendLimited(text, "\n" + relation + " [" + safeIdentifier(error.getClass().getName()) + "]", MAX_THROWABLE_CHARS);
			StackTraceElement[] frames = error.getStackTrace();
			int frameLimit = Math.min(FRAMES_PER_THROWABLE, frames.length);
			for (int i = 0; i < frameLimit && frameCount[0] < MAX_STACK_FRAMES
					&& text.length() < MAX_THROWABLE_CHARS; i++) {
				appendLimited(text, "\n\tat " + safeIdentifier(frames[i].getClassName()) + "."
						+ safeIdentifier(frames[i].getMethodName()) + "(line " + frames[i].getLineNumber() + ")",
						MAX_THROWABLE_CHARS);
				frameCount[0]++;
			}
			appendThrowable(text, error.getCause(), seen, throwableCount, frameCount, depth + 1, "caused by");
			for (Throwable suppressed : error.getSuppressed()) {
				if (throwableCount[0] >= MAX_THROWABLES || text.length() >= MAX_THROWABLE_CHARS) break;
				appendThrowable(text, suppressed, seen, throwableCount, frameCount, depth + 1, "suppressed");
			}
		}

		private static String safeIdentifier(String value) {
			if (value == null || value.isEmpty()) return "unknown";
			StringBuilder safe = new StringBuilder(Math.min(value.length(), 200));
			for (int i = 0; i < value.length() && safe.length() < 200; i++) {
				char character = value.charAt(i);
				safe.append((character >= 'a' && character <= 'z') || (character >= 'A' && character <= 'Z')
						|| (character >= '0' && character <= '9') || "._$-".indexOf(character) >= 0 ? character : '?');
			}
			return safe.toString();
		}

		private static boolean trustedTypeName(String value) {
			if (value == null || !value.startsWith("com.microproject.")) return false;
			String name = value.substring("com.microproject.".length());
			if (name.isEmpty()) return false;
			for (String component : name.split("\\.", -1))
				if (!isIdentifier(component)) return false;
			return true;
		}

		private static boolean trustedMethodName(String value) {
			return "<init>".equals(value) || "<clinit>".equals(value) || isIdentifier(value);
		}

		private static boolean isIdentifier(String value) {
			if (value == null || value.isEmpty()) return false;
			for (int i = 0; i < value.length(); i++) {
				char character = value.charAt(i);
				boolean allowed = (character >= 'a' && character <= 'z') || (character >= 'A' && character <= 'Z')
						|| character == '_' || character == '$' || (i > 0 && character >= '0' && character <= '9');
				if (!allowed) return false;
			}
			return true;
		}

		private static void appendLimited(StringBuilder target, CharSequence value, int limit) {
			int remaining = limit - target.length();
			if (remaining > 0) target.append(value, 0, Math.min(value.length(), remaining));
		}
	}
}
