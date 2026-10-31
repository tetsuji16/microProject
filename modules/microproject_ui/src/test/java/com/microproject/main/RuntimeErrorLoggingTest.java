package com.microproject.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

class RuntimeErrorLoggingTest {
	@Test
	void logDirectoryUsesPlatformConventions() {
		assertEquals(Path.of("C:/Users/test/microProject/logs"), RuntimeErrorLogging.defaultLogDirectory(
				"C:/Users/test", "Windows 11", "C:/Users/home"));
		assertEquals(Path.of("/home/test/.microproject/logs"), RuntimeErrorLogging.defaultLogDirectory(
				null, "Linux", "/home/test"));
		assertEquals(Path.of("C:/Users/home/AppData/Local/microProject/logs"), RuntimeErrorLogging.defaultLogDirectory(
				null, "Windows 11", "C:/Users/home"));
		assertEquals(Path.of("/Users/test/.microproject/logs"), RuntimeErrorLogging.defaultLogDirectory(
				null, "Darwin", "/Users/test"));
	}

	@Test
	void formatterKeepsStackEvidenceButOmitsPotentiallySensitiveMessages() throws Exception {
		var formatterClass = Class.forName("com.microproject.main.RuntimeErrorLogging$RedactedFormatter");
		var constructor = formatterClass.getDeclaredConstructor();
		constructor.setAccessible(true);
		var formatter = (java.util.logging.Formatter) constructor.newInstance();
		LogRecord record = new LogRecord(Level.SEVERE, "token=do-not-record");
		record.setLoggerName("test.logger");
		record.setSourceClassName("C:\\private\\apiKey=source-secret");
		record.setSourceMethodName("token=method-secret");
		IllegalStateException error = new IllegalStateException("password=do-not-record",
				new IllegalArgumentException("secret-cause"));
		error.setStackTrace(new StackTraceElement[] {
				new StackTraceElement("RootFrames", "first", "C:\\private\\path-secret.json", 1),
				new StackTraceElement("RootFrames", "second", "RootFrames.java", 2),
				new StackTraceElement("RootFrames", "third", "RootFrames.java", 3),
				new StackTraceElement("RootFrames", "fourth", "RootFrames.java", 4),
				new StackTraceElement("RootFrames", "unused", "RootFrames.java", 5)
		});
		error.getCause().setStackTrace(new StackTraceElement[] {
				new StackTraceElement("CauseFrames", "first", "CauseFrames.java", 1),
				new StackTraceElement("CauseFrames", "second", "CauseFrames.java", 2)
		});
		error.addSuppressed(new UnsupportedOperationException("suppressed-secret"));
		error.getSuppressed()[0].setStackTrace(new StackTraceElement[] {
				new StackTraceElement("SuppressedFrames", "first", "SuppressedFrames.java", 1)
		});
		for (int i = 0; i < 200; i++) error.addSuppressed(new IllegalArgumentException("extra-secret"));
		record.setThrown(error);
		String output = formatter.format(record);
		assertTrue(output.contains("java.lang.IllegalStateException"));
		assertTrue(output.contains("java.lang.IllegalArgumentException"));
		assertTrue(output.contains("java.lang.UnsupportedOperationException"));
		assertFalse(output.contains("do-not-record"));
		assertFalse(output.contains("secret-cause"));
		assertFalse(output.contains("suppressed-secret"));
		assertFalse(output.contains("extra-secret"));
		assertFalse(output.contains("source-secret"));
		assertFalse(output.contains("method-secret"));
		assertFalse(output.contains("path-secret"));
		assertTrue(output.contains("RootFrames.first(line 1)"));
		assertTrue(output.indexOf("RootFrames.first") < output.indexOf("RootFrames.second"));
		assertTrue(output.indexOf("RootFrames.second") < output.indexOf("caused by [java.lang.IllegalArgumentException]"));
		assertTrue(output.indexOf("CauseFrames.second") < output.indexOf("suppressed [java.lang.UnsupportedOperationException]"));
		assertTrue(output.indexOf("SuppressedFrames.first") > output.indexOf("suppressed [java.lang.UnsupportedOperationException]"));
		long throwableCount = output.lines().filter(line -> line.startsWith("suppressed [")
				|| line.startsWith("caused by [") || line.startsWith("thrown [")).count();
		long frameCount = output.lines().filter(line -> line.startsWith("\tat ")).count();
		assertTrue(throwableCount <= 8);
		assertTrue(frameCount >= throwableCount);
		assertTrue(output.length() <= 40_000);
		LogRecord warning = new LogRecord(Level.WARNING, "apiKey=warning-secret");
		warning.setLoggerName("com.microproject.ui.WarningService");
		warning.setSourceClassName("com.microproject.ui.WarningService");
		warning.setSourceMethodName("refresh");
		String warningOutput = formatter.format(warning);
		assertTrue(warningOutput.contains("WARNING com.microproject.ui.WarningService"));
		assertTrue(warningOutput.contains("source=com.microproject.ui.WarningService#refresh"));
		assertFalse(warningOutput.contains("warning-secret"));
		LogRecord secondWarning = new LogRecord(Level.WARNING, "password=other-secret");
		secondWarning.setLoggerName("com.microproject.ui.WarningService");
		secondWarning.setSourceClassName("com.microproject.ui.WarningService");
		secondWarning.setSourceMethodName("rebuild");
		String secondWarningOutput = formatter.format(secondWarning);
		assertTrue(secondWarningOutput.contains("source=com.microproject.ui.WarningService#rebuild"));
		assertNotEquals(warningOutput, secondWarningOutput);
		assertFalse(secondWarningOutput.contains("other-secret"));
		LogRecord untrustedWarning = new LogRecord(Level.WARNING, "token=warning-token-secret");
		untrustedWarning.setLoggerName("token=logger-secret");
		untrustedWarning.setSourceClassName("com.microproject.ui.password=source-secret");
		untrustedWarning.setSourceMethodName("apiKey=method-secret");
		String untrustedOutput = formatter.format(untrustedWarning);
		assertTrue(untrustedOutput.contains("WARNING external"));
		assertTrue(untrustedOutput.contains("[source=external]"));
		assertFalse(untrustedOutput.contains("logger-secret"));
		assertFalse(untrustedOutput.contains("source-secret"));
		assertFalse(untrustedOutput.contains("method-secret"));
		assertFalse(untrustedOutput.contains("warning-token-secret"));
		LogRecord fakeWatchdogRecord = new LogRecord(Level.WARNING, "thread dump includes apiKey=watchdog-secret");
		fakeWatchdogRecord.setLoggerName("com.microproject.ui.diagnostics.EdtWatchdog");
		fakeWatchdogRecord.setSourceClassName("com.microproject.ui.diagnostics.EdtWatchdog");
		fakeWatchdogRecord.setSourceMethodName("watch");
		String fakeWatchdogOutput = formatter.format(fakeWatchdogRecord);
		assertFalse(fakeWatchdogOutput.contains("watchdog-secret"));
		assertFalse(fakeWatchdogOutput.contains("thread dump includes"));
	}

	@Test
	void configurationCreatesBoundedLogAndIsIdempotent() throws Exception {
		RuntimeErrorLogging.close();
		Path directory = Files.createTempDirectory("microproject-errors-");
		Logger root = Logger.getLogger("");
		Logger runtimeLogger = Logger.getLogger("com.microproject.runtime");
		Logger watchdogLogger = Logger.getLogger("com.microproject.ui.diagnostics.EdtWatchdog");
		var rootHandlers = root.getHandlers();
		var runtimeHandlers = runtimeLogger.getHandlers();
		var watchdogHandlers = watchdogLogger.getHandlers();
		boolean runtimeParents = runtimeLogger.getUseParentHandlers();
		boolean watchdogParents = watchdogLogger.getUseParentHandlers();
		Level watchdogLevel = watchdogLogger.getLevel();
		Level runtimeLevel = runtimeLogger.getLevel();
		Thread.UncaughtExceptionHandler previousUncaughtHandler = Thread.getDefaultUncaughtExceptionHandler();
		assertFalse(RuntimeErrorLogging.configure(directory, 8_192, 2, ignored -> { throw new SecurityException(); }));
		assertTrue(java.util.Arrays.equals(rootHandlers, root.getHandlers()));
		assertTrue(java.util.Arrays.equals(runtimeHandlers, runtimeLogger.getHandlers()));
		assertTrue(java.util.Arrays.equals(watchdogHandlers, watchdogLogger.getHandlers()));
		assertEquals(runtimeParents, runtimeLogger.getUseParentHandlers());
		assertEquals(runtimeLevel, runtimeLogger.getLevel());
		assertEquals(watchdogParents, watchdogLogger.getUseParentHandlers());
		assertEquals(watchdogLevel, watchdogLogger.getLevel());
		assertEquals(previousUncaughtHandler, Thread.getDefaultUncaughtExceptionHandler());
		assertTrue(RuntimeErrorLogging.configure(directory, 8_192, 2));
		assertTrue(RuntimeErrorLogging.configure(directory.resolve("second")));
		assertEquals(Level.WARNING, watchdogLogger.getLevel());
		assertEquals(Level.SEVERE, runtimeLogger.getLevel());
		assertNotNull(Thread.getDefaultUncaughtExceptionHandler());
		Level rootLevel = root.getLevel();
		try {
			root.setLevel(Level.OFF);
			Thread uncaughtThread = new Thread(() -> {
				throw new IllegalStateException("password=thread-secret", new IllegalArgumentException("cause-secret"));
			}, "test-uncaught");
			uncaughtThread.start();
			uncaughtThread.join(5_000);
			assertFalse(uncaughtThread.isAlive());
		} finally {
			root.setLevel(rootLevel);
		}
		LogRecord watchdogWarning = new LogRecord(Level.WARNING, "WATCHDOG_SINGLE_RECORD_MARKER");
		watchdogWarning.setLoggerName(watchdogLogger.getName());
		watchdogWarning.setSourceClassName(watchdogLogger.getName());
		watchdogWarning.setSourceMethodName("watch");
		watchdogLogger.log(watchdogWarning);
		flushLogs();
		String uncaughtContents = readLogs(directory);
		assertTrue(uncaughtContents.contains("com.microproject.runtime - serious runtime failure"));
		assertEquals(1, uncaughtContents.split("com.microproject.ui.diagnostics.EdtWatchdog - serious runtime failure", -1).length - 1);
		assertFalse(uncaughtContents.contains("WATCHDOG_SINGLE_RECORD_MARKER"));
		assertTrue(uncaughtContents.contains("java.lang.IllegalArgumentException"));
		assertFalse(uncaughtContents.contains("thread-secret"));
		assertFalse(uncaughtContents.contains("cause-secret"));
		Logger runtime = Logger.getLogger("com.microproject.runtime");
		for (int i = 0; i < 600; i++) runtime.log(Level.SEVERE, "token=do-not-record",
				new IllegalStateException("password=do-not-record", new IllegalArgumentException("cause-secret")));
		flushLogs();
		RuntimeErrorLogging.close();
		assertEquals(previousUncaughtHandler, Thread.getDefaultUncaughtExceptionHandler());
		String contents = readLogs(directory);
		assertTrue(contents.contains("java.lang.IllegalStateException"));
		assertTrue(contents.contains("java.lang.IllegalArgumentException"));
		assertFalse(contents.contains("thread-secret"));
		assertFalse(contents.contains("cause-secret"));
		assertFalse(contents.contains("do-not-record"));
		try (var files = Files.list(directory)) {
			var rotations = files.filter(path -> path.getFileName().toString().startsWith("errors-")
					&& path.getFileName().toString().endsWith(".log")).toList();
			assertTrue(rotations.size() <= 2);
			for (Path rotation : rotations) assertTrue(Files.size(rotation) <= 12_000);
		}
		assertFalse(Files.exists(directory.resolve("second")));
	}

	private static void flushLogs() {
		for (var loggerName : new String[] { "", "com.microproject.runtime", "com.microproject.ui.diagnostics.EdtWatchdog" })
			for (var handler : Logger.getLogger(loggerName).getHandlers()) handler.flush();
	}

	private static String readLogs(Path directory) throws Exception {
		StringBuilder contents = new StringBuilder();
		try (var files = Files.list(directory)) {
			for (Path path : files.filter(file -> file.getFileName().toString().startsWith("errors-")
					&& file.getFileName().toString().endsWith(".log")).toList())
				contents.append(Files.readString(path));
		}
		return contents.toString();
	}
}
