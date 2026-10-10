/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.testsupport;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
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
