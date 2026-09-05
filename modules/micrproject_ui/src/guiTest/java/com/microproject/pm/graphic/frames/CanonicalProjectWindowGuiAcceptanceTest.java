/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.pm.graphic.frames.workspace.FrameManager;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.Environment;

/** GUI-MSP-WINDOW-06: canonical aliases focus an existing document instead of opening a duplicate. */
class CanonicalProjectWindowGuiAcceptanceTest {
	private MainRibbonFrame window;
	private FrameManager frameManager;
	private GraphicManager graphicManager;
	private Path file;
	private boolean previousRibbonUi;
	private boolean previousNewLook;

	@AfterEach
	void closeWindow() throws Exception {
		if (graphicManager != null) SwingUtilities.invokeAndWait(() -> graphicManager.cleanUp());
		if (window != null) SwingUtilities.invokeAndWait(() -> window.dispose());
		if (file != null) Files.deleteIfExists(file);
		Environment.setRibbonUI(previousRibbonUi);
		Environment.setNewLook(previousNewLook);
	}

	@Test
	void robotFocusesTheExistingFrameForAnEquivalentCanonicalPath() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		file = Files.createTempFile("canonical-window-", ".mpo");
		Project first = project("Canonical project", file.toRealPath().toString());
		Project alias = project("Duplicate object must not create a frame", file.getParent().resolve(".").resolve(file.getFileName()).toString());
		DocumentFrame[] frames = new DocumentFrame[2];
		GraphicManager[] managers = new GraphicManager[1];
		int[] existingFrameCount = new int[1];
		SwingUtilities.invokeAndWait(() -> {
			window = new MainRibbonFrame("microProject — Canonical project-window GUI acceptance", null, null);
			GraphicManager manager = new GraphicManager(window);
			graphicManager = manager;
			managers[0] = manager;
			window.setGraphicManager(manager);
			manager.initView();
			frameManager = manager.getFrameManager();
			existingFrameCount[0] = frameManager.getAllFrames().size();
			frames[0] = manager.addProjectFrame(first);
			frames[1] = manager.addProjectFrame(alias);
			window.setSize(920, 560);
			window.setLocationByPlatform(true);
			window.setAlwaysOnTop(true);
			window.setVisible(true);
		});
		GuiAcceptanceSupport.await(() -> window.isShowing() && frames[0].isShowing(), "canonical project frame did not become visible");
		assertSame(frames[0], frames[1]);
		assertSame(frames[0], managers[0].getCurrentFrame());
		assertEquals(existingFrameCount[0] + 1, frameManager.getAllFrames().size());
		Robot robot = new Robot();
		robot.setAutoDelay(40);
		capture(robot);
	}

	private void capture(Robot robot) throws Exception {
		Rectangle[] bounds = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> bounds[0] = new Rectangle(window.getLocationOnScreen(), window.getSize()));
		BufferedImage image = robot.createScreenCapture(bounds[0]);
		Path artifact = Path.of(System.getProperty("micrproject.gui.artifacts.dir", "build/guiTest-artifacts"), "canonical-project-window-no-duplicate.png");
		Files.createDirectories(artifact.getParent());
		ImageIO.write(image, "png", artifact.toFile());
		assertTrue(image.getWidth() > 400 && image.getHeight() > 300);
	}

	private static Project project(String name, String fileName) {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool(name, undo), undo);
		project.initialize(false, false);
		project.setName(name);
		project.setFileName(fileName);
		return project;
	}
}
