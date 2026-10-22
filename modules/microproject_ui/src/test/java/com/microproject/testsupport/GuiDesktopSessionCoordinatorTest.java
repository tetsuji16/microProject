/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.testsupport;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GuiDesktopSessionCoordinatorTest {
	@TempDir
	Path temporaryDirectory;

	@Test
	void competingRobotSessionGetsAnExplicitEnvironmentContentionFailure() throws Exception {
		Path lockFile = temporaryDirectory.resolve("shared-desktop.lock");
		try (GuiDesktopSessionCoordinator.Lease owner = GuiDesktopSessionCoordinator.acquire(lockFile,
				Duration.ofMillis(100))) {
			String competingProcess = runProbe(lockFile, "100");
			assertTrue(competingProcess.startsWith("CONTENDED"), competingProcess);
			assertTrue(competingProcess.contains("GUI environment contended"), competingProcess);
			assertTrue(competingProcess.contains("pid=" + ProcessHandle.current().pid()), competingProcess);
		}
		assertTrue(runProbe(lockFile, "1000").startsWith("ACQUIRED"));
	}

	@Test
	void contentionMarkerStopsLaterForksBeforeTheyReachRobotSetup() throws Exception {
		Path marker = temporaryDirectory.resolve("environment-contended.marker");
		GuiDesktopSessionCoordinator.failIfEnvironmentContended(marker);
		GuiDesktopSessionCoordinator.markEnvironmentContended(marker, "foreign foreground window overlapped the test frame");
		GuiDesktopSessionCoordinator.DesktopContendedException failure = assertThrows(
			GuiDesktopSessionCoordinator.DesktopContendedException.class,
			() -> GuiDesktopSessionCoordinator.failIfEnvironmentContended(marker));
		assertTrue(failure.getMessage().contains("later Robot tests were stopped before their setup"));
		assertTrue(failure.getMessage().contains("foreign foreground window overlapped"));
	}

	@Test
	void monitorDiagnosticsDoNotBecomeForegroundOverlapEvents() throws Exception {
		assumeTrue(System.getProperty("os.name", "").toLowerCase().contains("windows"));
		Path projectDirectory = Path.of("").toAbsolutePath();
		while (projectDirectory != null
			&& !Files.isRegularFile(projectDirectory.resolve(".github/scripts/capture-gui-desktop-state.ps1"))) {
			projectDirectory = projectDirectory.getParent();
		}
		assertTrue(projectDirectory != null, "could not find the repository root for the desktop monitor script");
		String previousProjectDirectory = System.getProperty("microproject.project.dir");
		System.setProperty("microproject.project.dir", projectDirectory.toString());
		Path eventLog;
		try {
			try (GuiDesktopSessionCoordinator.ForegroundMonitor monitor =
					GuiDesktopSessionCoordinator.monitorForegroundOverlaps(temporaryDirectory)) {
				long eventMark = GuiDesktopSessionCoordinator.beginRobotTest(monitor);
				try {
					try (var files = Files.list(temporaryDirectory)) {
						eventLog = files.filter(path -> path.getFileName().toString().startsWith("desktop-overlap-")
							&& path.getFileName().toString().endsWith(".log")
							&& !path.getFileName().toString().endsWith(".diagnostic.log"))
							.findFirst().orElseThrow(() -> new AssertionError("desktop event log was not created"));
					}
					Path diagnosticLog = eventLog.resolveSibling(eventLog.getFileName().toString()
						.replaceFirst("\\.log$", ".diagnostic.log"));
					long probeDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
					while (System.nanoTime() < probeDeadline && (!Files.exists(diagnosticLog)
						|| !Files.readString(diagnosticLog, StandardCharsets.UTF_8)
							.contains("monitorFirstOverlapProbeExited="))) {
						Thread.sleep(50);
					}
					assertTrue(Files.readString(diagnosticLog, StandardCharsets.UTF_8)
						.contains("monitorFirstOverlapProbeExited="),
						"monitor did not complete its first overlap probe before the test deadline");
					assertTrue(monitor.eventsSince(eventMark).isBlank(),
						"monitor diagnostics must not be treated as foreground-overlap events");
					Files.writeString(eventLog, "GUI_ENVIRONMENT_CONTENDED observed=" + Instant.now()
						+ " foreground pid=42 process=foreign class=Window title=foreign bounds=0,0,1,1 "
						+ "overlaps=testWindow=fixture testBounds=0,0,1,1" + System.lineSeparator(),
						StandardCharsets.UTF_8, StandardOpenOption.APPEND);
					GuiDesktopSessionCoordinator.DesktopContendedException failure = assertThrows(
						GuiDesktopSessionCoordinator.DesktopContendedException.class,
						GuiDesktopSessionCoordinator::verifyDesktopBeforeRobotInput);
					assertTrue(failure.getMessage().contains("Robot input blocked before dispatch"), failure.getMessage());
				} finally {
					GuiDesktopSessionCoordinator.endRobotTest(monitor);
				}
			}
		} finally {
			if (previousProjectDirectory == null) System.clearProperty("microproject.project.dir");
			else System.setProperty("microproject.project.dir", previousProjectDirectory);
		}
		String eventText = Files.readString(eventLog, StandardCharsets.UTF_8);
		assertTrue(eventText.contains("GUI_ENVIRONMENT_CONTENDED observed="), eventText);
		assertFalse(eventText.contains("monitorFirst") || eventText.contains("monitorSlow")
			|| eventText.contains("monitorPid=") || eventText.contains("monitorStarted=")
			|| eventText.contains("monitorStopped="), eventText);
		Path diagnosticLog = eventLog.resolveSibling(eventLog.getFileName().toString()
			.replaceFirst("\\.log$", ".diagnostic.log"));
		String diagnosticText = Files.readString(diagnosticLog, StandardCharsets.UTF_8);
		assertTrue(diagnosticText.contains("monitorPid="), diagnosticText);
		assertTrue(diagnosticText.contains("monitorStarted="), diagnosticText);
		assertTrue(diagnosticText.contains("monitorFirstTargetCheckExited="), diagnosticText);
		assertTrue(diagnosticText.contains("monitorFirstOverlapProbeExited="), diagnosticText);
	}

	private static String runProbe(Path lockFile, String waitMillis) throws Exception {
		Path classes = Path.of(GuiDesktopSessionCoordinatorTest.class.getProtectionDomain().getCodeSource().getLocation().toURI());
		String javaExecutable = Path.of(System.getProperty("java.home"), "bin",
			System.getProperty("os.name", "").toLowerCase().contains("windows") ? "java.exe" : "java").toString();
		Process child = new ProcessBuilder(javaExecutable, "-cp", classes.toString(),
			GuiDesktopSessionLockProbe.class.getName(), lockFile.toString(), waitMillis)
			.start();
		if (!child.waitFor(5, TimeUnit.SECONDS)) {
			child.destroyForcibly();
			throw new AssertionError("desktop-lock child process did not finish within 5 seconds");
		}
		String output = new String(child.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
		String errors = new String(child.getErrorStream().readAllBytes(), StandardCharsets.UTF_8).trim();
		org.junit.jupiter.api.Assertions.assertEquals(0, child.exitValue(), errors);
		return output;
	}
}
