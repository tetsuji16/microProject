/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.microproject.testsupport.GuiJourney;
import com.microproject.testsupport.GuiJourney.InputMechanism;
import java.awt.GraphicsEnvironment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.microproject.exchange.MpoFileImporter;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

/**
 * GUI-MPO-SAVE-03: closing the application window ends the process, so the route
 * runs in an application of its own.  The close request is the message the title
 * bar's X button sends, and the proof is what stays in the project's folder.
 */
class ApplicationCloseGuiAcceptanceTest {
	private static final long START_SECONDS = 120L;
	private static final long EXIT_SECONDS = 60L;

	@TempDir
	Path temp;

	@Test
	@GuiJourney(InputMechanism.WINDOW_SHELL)
	void closingTheApplicationWindowLeavesOnlyTheProjectFile() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required.");
		Assumptions.assumeTrue(System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win"),
			"The close request is sent through the Windows window manager.");
		Path folder = Files.createDirectory(temp.resolve("project"));
		Path mpo = folder.resolve("plan.mpo");
		writeProject(mpo);
		Path log = temp.resolve("application.log");
		Process application = new ProcessBuilder(
			ProcessHandle.current().info().command().orElseThrow(), "--enable-native-access=ALL-UNNAMED",
			"-cp", System.getProperty("java.class.path"), "com.microproject.main.Main", mpo.toString())
			.redirectErrorStream(true).redirectOutput(log.toFile()).start();
		try {
			awaitFile(folder.resolve("plan.microproject-sync.json"), application, log);

			sendWindowClose(application.pid());

			if (!application.waitFor(EXIT_SECONDS, TimeUnit.SECONDS)) {
				fail("the application did not exit after its window was closed\n" + logText(log));
			}
			assertEquals(0, application.exitValue());
			try (var files = Files.list(folder)) {
				assertEquals(List.of("plan.mpo"), files.map(file -> file.getFileName().toString()).sorted().toList(),
					"closing the application must leave only the project file beside it");
			}
		} finally {
			application.destroyForcibly().waitFor();
		}
	}

	private static void writeProject(Path target) throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("application close pool", undo), undo);
		project.initialize(false, false);
		project.setName("Application close");
		MpoFileImporter exporter = new MpoFileImporter();
		exporter.setProject(project);
		exporter.setFileName(target.toString());
		exporter.exportFile();
	}

	private static void awaitFile(Path file, Process application, Path log) throws Exception {
		try {
			com.microproject.testsupport.TestAwait.awaitTrue(() -> Files.exists(file) || !application.isAlive(), java.time.Duration.ofSeconds(START_SECONDS),
				"the project to open");
		} catch (AssertionError timedOut) {
			fail("the project did not open\n" + logText(log));
		}
		if (!Files.exists(file)) fail("the application stopped while opening\n" + logText(log));
	}

	/** The application writes its console in the platform's encoding. */
	private static String logText(Path log) throws Exception {
		return new String(Files.readAllBytes(log));
	}

	/** {@code CloseMainWindow} posts WM_CLOSE, as the title bar's X button does. */
	private static void sendWindowClose(long pid) throws Exception {
		String script = "$deadline=(Get-Date).AddSeconds(" + START_SECONDS + ");"
			+ "do{$p=Get-Process -Id " + pid + ";if($p.MainWindowHandle -ne 0){break};Start-Sleep -Milliseconds 200}"
			+ "while((Get-Date) -lt $deadline);"
			+ "if(-not $p.CloseMainWindow()){exit 1}";
		Process sender = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-Command", script)
			.redirectErrorStream(true).start();
		String output = new String(sender.getInputStream().readAllBytes());
		assertEquals(0, sender.waitFor(), "the window close request was not delivered: " + output);
	}
}
