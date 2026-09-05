/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dialog;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.pm.graphic.frames.workspace.FrameManager;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.session.SessionFactory;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.Environment;

/** GUI-MSP-WINDOW-07: a malformed standalone file reports a visible, non-destructive error. */
class InvalidStandaloneProjectOpenGuiAcceptanceTest {
	private MainRibbonFrame window;
	private GraphicManager graphicManager;
	private Path invalidFile;
	private boolean previousClientSide;
	private boolean previousStandalone;
	private boolean previousRibbonUi;
	private boolean previousNewLook;

	@AfterEach
	void closeWindow() throws Exception {
		if (graphicManager != null) SwingUtilities.invokeAndWait(() -> graphicManager.cleanUp());
		if (window != null) SwingUtilities.invokeAndWait(() -> window.dispose());
		if (invalidFile != null) Files.deleteIfExists(invalidFile);
		Environment.setClientSide(previousClientSide);
		Environment.setStandAlone(previousStandalone);
		Environment.setRibbonUI(previousRibbonUi);
		Environment.setNewLook(previousNewLook);
	}

	@Test
	void robotReportsInvalidStandaloneFileAndLeavesExistingDocumentUntouched() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		previousClientSide = Environment.isClientSide();
		previousStandalone = Environment.getStandAlone();
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setClientSide(true);
		Environment.setStandAlone(true);
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		invalidFile = Files.createTempFile("invalid-standalone-project-", ".mpo");
		Files.writeString(invalidFile, "not a MPOF ZIP archive");

		DocumentFrame[] original = new DocumentFrame[1];
		FrameManager[] frameManager = new FrameManager[1];
		SwingUtilities.invokeAndWait(() -> {
			window = new MainRibbonFrame("microProject — Invalid standalone-file GUI acceptance", null, null);
			InvalidStandaloneGraphicManager manager = new InvalidStandaloneGraphicManager(window);
			graphicManager = manager;
			window.setGraphicManager(manager);
			manager.initView();
			SessionFactory.getInstance().setJobQueue(manager.getJobQueue());
			frameManager[0] = manager.getFrameManager();
			original[0] = manager.addProjectFrame(project("Existing independent project"));
			window.setSize(960, 600);
			window.setLocationByPlatform(true);
			window.setAlwaysOnTop(true);
			window.setVisible(true);
			manager.openForTest(invalidFile.toString());
		});
		GuiAcceptanceSupport.await(() -> findErrorDialog() != null, "invalid standalone project did not show an error dialog");

		Dialog dialog = findErrorDialog();
		String text = dialogText(dialog);
		assertTrue(text.contains(invalidFile.toString()), text);
		assertTrue(text.contains("invalid or could not be imported"), text);
		assertTrue(text.contains("already open were not changed"), text);
		assertSame(original[0], graphicManager.getCurrentFrame());
		assertEquals(1, frameManager[0].getAllFrames().size());

		Robot robot = new Robot();
		robot.setAutoDelay(40);
		capture(robot, dialog);
		clickDismissButton(robot, dialog);
		GuiAcceptanceSupport.await(() -> !dialog.isShowing(), "OK did not dismiss the standalone-file error");
		assertSame(original[0], graphicManager.getCurrentFrame());
		assertEquals(1, frameManager[0].getAllFrames().size());
	}

	@Test
	void robotReportsMissingStandaloneFileAndLeavesExistingDocumentUntouched() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for GUI acceptance coverage.");
		previousClientSide = Environment.isClientSide();
		previousStandalone = Environment.getStandAlone();
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setClientSide(true);
		Environment.setStandAlone(true);
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		invalidFile = Path.of(System.getProperty("java.io.tmpdir"), "missing-standalone-project-" + System.nanoTime() + ".mpo");
		DocumentFrame[] original = new DocumentFrame[1];
		FrameManager[] frameManager = new FrameManager[1];
		SwingUtilities.invokeAndWait(() -> {
			window = new MainRibbonFrame("microProject — Missing standalone-file GUI acceptance", null, null);
			InvalidStandaloneGraphicManager manager = new InvalidStandaloneGraphicManager(window);
			graphicManager = manager;
			window.setGraphicManager(manager);
			manager.initView();
			SessionFactory.getInstance().setJobQueue(manager.getJobQueue());
			frameManager[0] = manager.getFrameManager();
			original[0] = manager.addProjectFrame(project("Existing independent project for missing-file test"));
			window.setSize(960, 600);
			window.setLocationByPlatform(true);
			window.setAlwaysOnTop(true);
			window.setVisible(true);
			manager.openForTest(invalidFile.toString());
		});
		GuiAcceptanceSupport.await(() -> findErrorDialog() != null, "missing standalone project did not show an error dialog");
		Dialog dialog = findErrorDialog();
		assertTrue(dialogText(dialog).contains(invalidFile.toString()), dialogText(dialog));
		assertSame(original[0], graphicManager.getCurrentFrame());
		assertEquals(1, frameManager[0].getAllFrames().size());
		Robot robot = new Robot();
		capture(robot, dialog, "missing-standalone-project-open-error.png");
		clickDismissButton(robot, dialog);
		GuiAcceptanceSupport.await(() -> !dialog.isShowing(), "OK did not dismiss the missing-file error");
		assertSame(original[0], graphicManager.getCurrentFrame());
		assertEquals(1, frameManager[0].getAllFrames().size());
	}

	@Test
	void robotReportsAccessDeniedStandaloneFileAndLeavesExistingDocumentUntouched() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for GUI acceptance coverage.");
		Assumptions.assumeTrue(System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win"), "ACL test requires Windows.");
		previousClientSide = Environment.isClientSide();
		previousStandalone = Environment.getStandAlone();
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setClientSide(true);
		Environment.setStandAlone(true);
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		invalidFile = Files.createTempFile("access-denied-standalone-project-", ".mpo");
		Files.writeString(invalidFile, "placeholder");
		String user = System.getProperty("user.name");
		setAcl(invalidFile, user, "/deny", "R");
		try {
			DocumentFrame[] original = new DocumentFrame[1];
			FrameManager[] frameManager = new FrameManager[1];
			SwingUtilities.invokeAndWait(() -> {
				window = new MainRibbonFrame("microProject — Access-denied standalone-file GUI acceptance", null, null);
				InvalidStandaloneGraphicManager manager = new InvalidStandaloneGraphicManager(window);
				graphicManager = manager;
				window.setGraphicManager(manager);
				manager.initView();
				SessionFactory.getInstance().setJobQueue(manager.getJobQueue());
				frameManager[0] = manager.getFrameManager();
				original[0] = manager.addProjectFrame(project("Existing independent project for access-denied test"));
				window.setSize(960, 600);
				window.setLocationByPlatform(true);
				window.setAlwaysOnTop(true);
				window.setVisible(true);
				manager.openForTest(invalidFile.toString());
			});
			GuiAcceptanceSupport.await(() -> findErrorDialog() != null, "access-denied standalone project did not show an error dialog");
			Dialog dialog = findErrorDialog();
			String text = dialogText(dialog);
			assertTrue(text.contains(invalidFile.toString()), text);
			assertSame(original[0], graphicManager.getCurrentFrame());
			assertEquals(1, frameManager[0].getAllFrames().size());
		Robot robot = new Robot();
		capture(robot, dialog, "access-denied-standalone-project-open-error.png");
		clickDismissButton(robot, dialog);
			GuiAcceptanceSupport.await(() -> !dialog.isShowing(), "OK did not dismiss the access-denied error");
		} finally {
			setAcl(invalidFile, user, "/reset", null);
		}
	}

	private static void setAcl(Path file, String user, String operation, String permission) throws Exception {
		ProcessBuilder builder = permission == null
				? new ProcessBuilder("icacls", file.toString(), operation)
				: new ProcessBuilder("icacls", file.toString(), operation, user + ":" + permission);
		Process process = builder.redirectErrorStream(true).start();
		if (process.waitFor() != 0) throw new AssertionError("icacls failed: " + new String(process.getInputStream().readAllBytes()));
	}

	private static Project project(String name) {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool(name, undo), undo);
		project.initialize(false, false);
		project.setName(name);
		return project;
	}

	private static Dialog findErrorDialog() {
		for (Window candidate : Window.getWindows())
			if (candidate instanceof Dialog dialog && dialog.isShowing() && dialogText(dialog).contains("already open were not changed")) return dialog;
		return null;
	}

	private static String dialogText(java.awt.Container container) {
		StringBuilder text = new StringBuilder();
		for (java.awt.Component component : container.getComponents()) {
			if (component instanceof javax.swing.JLabel label) text.append(label.getText());
			if (component instanceof JTextComponent componentText) text.append(componentText.getText());
			if (component instanceof java.awt.Container child) text.append(dialogText(child));
		}
		return text.toString();
	}

	private void capture(Robot robot, Dialog dialog) throws Exception {
		capture(robot, dialog, "invalid-standalone-project-open-error.png");
	}

	private void capture(Robot robot, Dialog dialog, String fileName) throws Exception {
		Rectangle[] bounds = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> bounds[0] = new Rectangle(window.getBounds()).union(dialog.getBounds()));
		robot.waitForIdle();
		Path artifact = Path.of(System.getProperty("micrproject.gui.artifacts.dir", "build/guiTest-artifacts"), fileName);
		Files.createDirectories(artifact.getParent());
		BufferedImage image = robot.createScreenCapture(bounds[0]);
		ImageIO.write(image, "png", artifact.toFile());
	}

	private static void clickDismissButton(Robot robot, Dialog dialog) throws Exception {
		Rectangle[] bounds = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> {
			JButton button = findDismissButton(dialog);
			if (button == null) throw new AssertionError("standalone-file error has no dismissal button");
			java.awt.Point point = button.getLocationOnScreen();
			bounds[0] = new Rectangle(point.x, point.y, button.getWidth(), button.getHeight());
		});
		robot.mouseMove(bounds[0].x + bounds[0].width / 2, bounds[0].y + bounds[0].height / 2);
		robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
	}

	private static JButton findDismissButton(java.awt.Container container) {
		for (java.awt.Component component : container.getComponents()) {
			if (component instanceof JButton button) return button;
			if (component instanceof java.awt.Container child) {
				JButton button = findDismissButton(child);
				if (button != null) return button;
			}
		}
		return null;
	}

	private static final class InvalidStandaloneGraphicManager extends GraphicManager {
		private static final long serialVersionUID = 1L;
		InvalidStandaloneGraphicManager(MainRibbonFrame window) { super(window); }
		void openForTest(String fileName) { loadLocalDocument(fileName, false); }
	}
}
