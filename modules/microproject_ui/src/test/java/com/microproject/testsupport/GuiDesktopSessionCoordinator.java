/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.testsupport;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/** Serializes Robot acceptance sessions across Gradle forks and worktrees for this user. */
public final class GuiDesktopSessionCoordinator {
	private static final Duration DEFAULT_WAIT = Duration.ofMinutes(10);
	private static final long RETRY_MILLIS = 100;
	private static volatile ForegroundMonitor activeMonitor;
	private static volatile long activeTestMark;

	private GuiDesktopSessionCoordinator() {
	}

	public static Lease acquireForCurrentProject() throws IOException, InterruptedException {
		Path lockFile = Path.of(System.getProperty("java.io.tmpdir"), "microproject-gui-acceptance.lock");
		long waitMillis = Long.getLong("microproject.gui.desktopLockWaitMillis", DEFAULT_WAIT.toMillis());
		return acquire(lockFile, Duration.ofMillis(Math.max(0, waitMillis)));
	}

	public static void failIfEnvironmentContended() throws IOException {
		failIfEnvironmentContended(contentionMarker());
	}

	static void failIfEnvironmentContended(Path marker) throws IOException {
		if (marker != null && Files.isRegularFile(marker)) {
			throw new DesktopContendedException("GUI_ENVIRONMENT_CONTENDED: this GUI acceptance invocation already "
				+ "detected desktop interference; later Robot tests were stopped before their setup. "
				+ Files.readString(marker, StandardCharsets.UTF_8));
		}
	}

	public static void markEnvironmentContended(String evidence) throws IOException {
		markEnvironmentContended(contentionMarker(), evidence);
	}

	public static void beginRobotTest(ForegroundMonitor monitor) throws IOException {
		activeTestMark = monitor == null ? 0 : monitor.mark();
		activeMonitor = monitor;
	}

	public static void endRobotTest(ForegroundMonitor monitor) {
		if (activeMonitor == monitor) activeMonitor = null;
	}

	public static void verifyDesktopBeforeRobotInput() {
		ForegroundMonitor monitor = activeMonitor;
		if (monitor == null) return;
		try {
			String overlap = monitor.eventsSince(activeTestMark);
			if (!overlap.isBlank()) {
				markEnvironmentContended("GUI_ENVIRONMENT_CONTENDED: Robot input blocked because a foreign foreground window overlaps the test window. "
					+ overlap);
				throw new DesktopContendedException("GUI_ENVIRONMENT_CONTENDED: Robot input blocked before dispatch. " + overlap);
			}
		} catch (IOException failure) {
			try {
				markEnvironmentContended("GUI_ENVIRONMENT_MONITOR_UNAVAILABLE: Robot input blocked because foreground ownership could not be verified. "
					+ failure.getMessage());
			} catch (IOException markerFailure) {
				failure.addSuppressed(markerFailure);
			}
			throw new IllegalStateException("GUI_ENVIRONMENT_MONITOR_UNAVAILABLE: Robot input blocked before dispatch.", failure);
		}
	}

	static void markEnvironmentContended(Path marker, String evidence) throws IOException {
		if (marker == null) return;
		Path parent = marker.toAbsolutePath().getParent();
		if (parent != null) Files.createDirectories(parent);
		Files.writeString(marker, evidence, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
			StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
	}

	private static Path contentionMarker() {
		String configuredPath = System.getProperty("microproject.gui.contentionMarker");
		return configuredPath == null || configuredPath.isBlank() ? null : Path.of(configuredPath);
	}

	public static ForegroundMonitor monitorForegroundOverlaps(Path artifactDirectory) throws IOException, InterruptedException {
		if (!System.getProperty("os.name", "").toLowerCase().contains("windows")) return null;
		Path projectDirectory = Path.of(System.getProperty("microproject.project.dir", "."));
		Path script = projectDirectory.resolve(".github/scripts/capture-gui-desktop-state.ps1");
		if (!Files.isRegularFile(script)) {
			throw new IOException("GUI environment monitor unavailable: " + script + " was not found");
		}
		Files.createDirectories(artifactDirectory);
		String processId = Long.toString(ProcessHandle.current().pid());
		Path eventLog = artifactDirectory.resolve("desktop-overlap-" + processId + ".log");
		Path processLog = artifactDirectory.resolve("desktop-monitor-process-" + processId + ".log");
		Process monitor = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-WindowStyle", "Hidden",
			"-ExecutionPolicy", "Bypass", "-File", script.toString(), "-TargetPid", processId,
			"-Watch", "-LogFile", eventLog.toString()).redirectErrorStream(true)
			.redirectOutput(processLog.toFile()).start();
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		while (System.nanoTime() < deadline && monitor.isAlive()) {
			if (Files.exists(eventLog) && Files.readString(eventLog, StandardCharsets.UTF_8).contains("monitorStarted=")) {
				return new ForegroundMonitor(monitor, eventLog, processLog);
			}
			Thread.sleep(50);
		}
		monitor.destroyForcibly();
		String failureOutput = Files.exists(processLog) ? Files.readString(processLog, StandardCharsets.UTF_8) : "no process output";
		throw new IOException("GUI environment monitor failed to start: " + failureOutput.trim());
	}

	static Lease acquire(Path lockFile, Duration maximumWait) throws IOException, InterruptedException {
		Files.createDirectories(lockFile.toAbsolutePath().getParent());
		long deadline = System.nanoTime() + maximumWait.toNanos();
		FileChannel channel = FileChannel.open(lockFile, StandardOpenOption.CREATE,
			StandardOpenOption.READ, StandardOpenOption.WRITE);
		try {
			while (true) {
				FileLock lock = tryLock(channel);
				if (lock != null) {
					writeOwner(ownerFile(lockFile));
					return new Lease(channel, lock, lockFile);
				}
				if (System.nanoTime() >= deadline) {
					throw new DesktopContendedException("GUI environment contended: another ProjectLibre Robot session "
						+ "owns the desktop lock " + lockFile + ". Current owner: " + readOwner(lockFile));
				}
				Thread.sleep(Math.min(RETRY_MILLIS,
					Math.max(1, TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime()))));
			}
		} catch (IOException | InterruptedException | RuntimeException | Error failure) {
			try {
				channel.close();
			} catch (IOException closeFailure) {
				failure.addSuppressed(closeFailure);
			}
			throw failure;
		}
	}

	private static FileLock tryLock(FileChannel channel) throws IOException {
		try {
			return channel.tryLock();
		} catch (OverlappingFileLockException alreadyLockedInThisProcess) {
			return null;
		}
	}

	private static void writeOwner(Path ownerFile) throws IOException {
		String projectDirectory = System.getProperty("microproject.project.dir", "unknown");
		String metadata = "pid=" + ProcessHandle.current().pid() + System.lineSeparator()
			+ "user=" + System.getProperty("user.name", "unknown") + System.lineSeparator()
			+ "started=" + Instant.now() + System.lineSeparator()
			+ "project=" + projectDirectory + System.lineSeparator();
		Files.writeString(ownerFile, metadata, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
			StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
	}

	private static String readOwner(Path lockFile) {
		try {
			return Files.readString(ownerFile(lockFile), StandardCharsets.UTF_8)
				.replaceAll("[\\r\\n]+", "; ").trim();
		} catch (IOException ignored) {
			return "owner metadata unavailable";
		}
	}

	private static Path ownerFile(Path lockFile) {
		return lockFile.resolveSibling(lockFile.getFileName() + ".owner");
	}

	static final class Lease implements AutoCloseable {
		private final FileChannel channel;
		private final FileLock lock;
		private final Path lockFile;
		private boolean closed;

		private Lease(FileChannel channel, FileLock lock, Path lockFile) {
			this.channel = channel;
			this.lock = lock;
			this.lockFile = lockFile;
		}

		Path lockFile() {
			return lockFile;
		}

		@Override
		public void close() throws IOException {
			if (closed) return;
			closed = true;
			try {
				lock.release();
			} finally {
				channel.close();
			}
		}
	}

	static final class DesktopContendedException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		DesktopContendedException(String message) {
			super(message);
		}
	}

	public static final class ForegroundMonitor implements AutoCloseable {
		private final Process process;
		private final Path eventLog;
		private final Path processLog;

		private ForegroundMonitor(Process process, Path eventLog, Path processLog) {
			this.process = process;
			this.eventLog = eventLog;
			this.processLog = processLog;
		}

		public long mark() throws IOException {
			ensureRunning();
			return Files.exists(eventLog) ? Files.size(eventLog) : 0;
		}

		public String eventsSince(long mark) throws IOException {
			ensureRunning();
			if (!Files.exists(eventLog)) return "";
			byte[] contents = Files.readAllBytes(eventLog);
			int start = (int) Math.min(Math.max(0, mark), contents.length);
			return new String(contents, start, contents.length - start, StandardCharsets.UTF_8);
		}

		public String processDiagnostics() throws IOException {
			return Files.exists(processLog) ? Files.readString(processLog, StandardCharsets.UTF_8) : "monitor log unavailable";
		}

		private void ensureRunning() throws IOException {
			if (process.isAlive()) return;
			throw new IOException("GUI environment monitor exited unexpectedly: " + process.exitValue() + ". "
				+ processDiagnostics());
		}

		@Override
		public void close() throws IOException, InterruptedException {
			process.destroy();
			if (!process.waitFor(2, TimeUnit.SECONDS)) {
				process.destroyForcibly();
				if (!process.waitFor(2, TimeUnit.SECONDS)) {
					throw new IOException("GUI environment monitor did not terminate: " + process.pid());
				}
			}
		}
	}
}
