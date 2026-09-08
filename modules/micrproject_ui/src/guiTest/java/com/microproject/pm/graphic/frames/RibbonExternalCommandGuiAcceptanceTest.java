/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.dialog.AbstractDialog;
import com.microproject.dialog.AboutDialog;
import com.microproject.dialog.HelpDialog;
import com.microproject.dialog.LocaleDialog;
import com.microproject.dialog.ProjectDialog;
import com.microproject.pm.task.Project;
import com.microproject.session.SessionFactory;
import com.microproject.strings.Messages;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.util.Environment;
import com.microproject.util.UiServices;

/**
 * Verifies the real File-ribbon command pipeline rather than a recording
 * ActionMap.  A dispatch-only test can pass while the production action is
 * disabled, throws, or returns before showing its dialog.
 */
class RibbonExternalCommandGuiAcceptanceTest {
	private MainRibbonFrame window;
	private GraphicManager manager;
	private boolean previousRibbonUi;
	private boolean previousNewLook;
	private boolean previousStandalone;
	private boolean previousClientSide;
	private UiServices.FileChooserProvider previousChooser;
	private Path legacyPod;

	@AfterEach
	void closeWindow() throws Exception {
		if (manager != null) SwingUtilities.invokeAndWait(manager::cleanUp);
		if (window != null) SwingUtilities.invokeAndWait(window::dispose);
		Environment.setRibbonUI(previousRibbonUi);
		Environment.setNewLook(previousNewLook);
		Environment.setStandAlone(previousStandalone);
		Environment.setClientSide(previousClientSide);
		UiServices.setFileChooserProvider(previousChooser);
		if (legacyPod != null) Files.deleteIfExists(legacyPod);
		for (Window open : Window.getWindows()) {
			if (open instanceof ProjectDialog || open instanceof LocaleDialog
				|| open instanceof HelpDialog || open instanceof AboutDialog) {
				open.dispose();
			}
		}
	}

	/** GUI-RIBBON-FILE-02: File/Open must reach the legacy importer, not MPO zip validation. */
	@Test
	void robotOpensLegacyPodFromTheFileRibbon() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
			"A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		previousStandalone = Environment.getStandAlone();
		previousClientSide = Environment.isClientSide();
		previousChooser = UiServices.getFileChooserProvider();
		Environment.setStandAlone(true);
		Environment.setClientSide(true);
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		legacyPod = Files.createTempFile("ribbon-legacy-sample-", ".pod");
		Files.copy(Path.of(System.getProperty("micrproject.project.dir"), "samples", "June_1_sample.pod"), legacyPod,
			java.nio.file.StandardCopyOption.REPLACE_EXISTING);
		UiServices.setFileChooserProvider(new UiServices.FileChooserProvider() {
			@Override public String chooseFileName(boolean save, String selectedFileName, Object parent) { return null; }
			@Override public List<String> chooseFileNames(boolean save, String selectedFileName, Object parent) {
				return save ? List.of() : List.of(legacyPod.toString());
			}
		});

		createWindow("microProject — Legacy sample File/Open acceptance");
		Robot robot = new Robot();
		robot.setAutoDelay(45);
		AbstractButton openButton = findCommandButton(window, "RibbonOpenProject");
		click(robot, openButton);
		GuiAcceptanceSupport.await(() -> manager.findFrameForProjectFile(legacyPod.toString()) != null,
			"File/Open rejected the selected legacy POD before its importer ran");
		assertTrue(manager.findFrameForProjectFile(legacyPod.toString()).getProject() != null,
			"File/Open registered a legacy POD frame without a project model");
	}

	@Test
	void robotInvokesRealFileRibbonCommandsAndOpensTheirDialogs() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
			"A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		previousStandalone = Environment.getStandAlone();
		previousClientSide = Environment.isClientSide();
		Environment.setStandAlone(true);
		Environment.setClientSide(true);
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);

		SwingUtilities.invokeAndWait(() -> {
			window = new MainRibbonFrame("microProject — real ribbon command acceptance", null, null);
			manager = new GraphicManager(window);
			window.setGraphicManager(manager);
			manager.initView();
			SessionFactory.getInstance().setJobQueue(manager.getJobQueue());
			manager.setConnected(true);
			window.setSize(1200, 700);
			window.setLocationByPlatform(true);
			window.setAlwaysOnTop(true);
			window.setVisible(true);
		});
		GuiAcceptanceSupport.await(() -> window.isShowing(), "real ribbon window did not become visible");

		Robot robot = new Robot();
		robot.setAutoDelay(45);
		clickAndClose(robot, "RibbonNewProject", ProjectDialog.class);
		clickAndClose(robot, "RibbonLocale", LocaleDialog.class);
		clickAndClose(robot, "RibbonProjectLibreDocumentation", HelpDialog.class);
		clickAndClose(robot, "RibbonAboutProjectLibre", AboutDialog.class);
	}

	/**
	 * GUI-RIBBON-FILE-01: New is not accepted until its physical OK route has
	 * created a visible document with the entered model name.  This catches the
	 * historic EDT failure before the dialog as well as the later silent failure
	 * between dialog submission and document-frame registration.
	 */
	@Test
	void robotCreatesNewLocalProjectFromTheFileRibbon() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
			"A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		previousStandalone = Environment.getStandAlone();
		previousClientSide = Environment.isClientSide();
		Environment.setStandAlone(true);
		Environment.setClientSide(true);
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);

		createStartedWindow("microProject — New project creation acceptance");
		Robot robot = new Robot();
		robot.setAutoDelay(45);
		AbstractButton newButton = findCommandButton(window, "RibbonNewProject");
		click(robot, newButton);
		GuiAcceptanceSupport.await(() -> visibleDialog(ProjectDialog.class) != null,
			"New did not show the project dialog");
		ProjectDialog dialog = (ProjectDialog) visibleDialog(ProjectDialog.class);
		JTextField name = findProjectNameField(dialog);
		String expectedName = "Robot File Ribbon New Project";
		click(robot, name);
		robot.keyPress(java.awt.event.KeyEvent.VK_CONTROL);
		robot.keyPress(java.awt.event.KeyEvent.VK_A);
		robot.keyRelease(java.awt.event.KeyEvent.VK_A);
		robot.keyRelease(java.awt.event.KeyEvent.VK_CONTROL);
		type(robot, expectedName);
		AbstractButton ok = findButton(dialog, Messages.getString("ButtonText.OK"));
		click(robot, ok);
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame() != null,
			"New accepted the dialog but did not register a document frame");
		Project created = manager.getCurrentFrame().getProject();
		assertEquals(expectedName, created.getName(), "New did not apply the dialog name to the created project");
		assertTrue(manager.getFrameManager().getAllFrames().contains(manager.getCurrentFrame()),
			"New did not expose the created project through the frame manager");
	}

	private void createWindow(String title) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			window = new MainRibbonFrame(title, null, null);
			manager = new GraphicManager(window);
			window.setGraphicManager(manager);
			manager.initView();
			SessionFactory.getInstance().setJobQueue(manager.getJobQueue());
			window.setSize(1200, 700);
			window.setLocationByPlatform(true);
			window.setAlwaysOnTop(true);
			window.setVisible(true);
		});
		GuiAcceptanceSupport.await(() -> window.isShowing(), "real ribbon window did not become visible");
	}

	/**
	 * Starts the same standalone factory path as the desktop launcher.  Building
	 * a GraphicManager directly cannot prove that startup restored the connected
	 * command state before the File ribbon is shown.
	 */
	private void createStartedWindow(String title) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			window = new MainRibbonFrame(title, null, null);
			manager = new ApplicationStartupFactory(new HashMap<>()).instanceFromNewSession(window, false);
			window.setGraphicManager(manager);
			if (!window.isShowing()) {
				window.setSize(1200, 700);
				window.setLocationByPlatform(true);
				window.setAlwaysOnTop(true);
				window.setVisible(true);
			}
		});
		GuiAcceptanceSupport.await(() -> window.isShowing(), "startup ribbon window did not become visible");
	}

	private void clickAndClose(Robot robot, String commandId, Class<? extends Window> dialogType) throws Exception {
		AbstractButton button = findCommandButton(window, commandId);
		assertTrue(button.isShowing(), commandId + " is not physically visible");
		assertTrue(button.isEnabled(), commandId + " is disabled in the real application state");
		click(robot, button);
		GuiAcceptanceSupport.await(() -> visibleDialog(dialogType) != null,
			commandId + " did not open " + dialogType.getSimpleName());
		Window dialog = visibleDialog(dialogType);
		clickCancel(robot, dialog);
		GuiAcceptanceSupport.await(() -> visibleDialog(dialogType) == null,
			commandId + " did not close its dialog through the physical Cancel route");
	}

	private static void clickCancel(Robot robot, Window dialog) throws Exception {
		assertTrue(dialog instanceof AbstractDialog, "Expected an AbstractDialog: " + dialog);
		SwingUtilities.invokeAndWait(() -> {
			dialog.toFront();
			dialog.requestFocus();
		});
		robot.keyPress(java.awt.event.KeyEvent.VK_ESCAPE);
		robot.keyRelease(java.awt.event.KeyEvent.VK_ESCAPE);
	}

	private static void click(Robot robot, Component component) throws Exception {
		var point = component.getLocationOnScreen();
		robot.mouseMove(point.x + component.getWidth() / 2, point.y + component.getHeight() / 2);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
	}

	private static void type(Robot robot, String text) {
		for (char character : text.toCharArray()) {
			int keyCode = java.awt.event.KeyEvent.getExtendedKeyCodeForChar(character);
			if (Character.isUpperCase(character)) robot.keyPress(java.awt.event.KeyEvent.VK_SHIFT);
			robot.keyPress(keyCode);
			robot.keyRelease(keyCode);
			if (Character.isUpperCase(character)) robot.keyRelease(java.awt.event.KeyEvent.VK_SHIFT);
		}
	}

	private static Window visibleDialog(Class<? extends Window> type) {
		for (Window window : Window.getWindows()) {
			if (type.isInstance(window) && window.isShowing()) return window;
		}
		return null;
	}

	private static AbstractButton findCommandButton(Component root, String commandId) {
		for (Component component : flatten(root)) {
			if (component instanceof AbstractButton button && commandId.equals(button.getActionCommand()))
				return button;
		}
		throw new AssertionError("Ribbon command is not present: " + commandId);
	}

	private static AbstractButton findButton(Component root, String text) {
		for (Component component : flatten(root)) {
			if (component instanceof AbstractButton button && text.equals(button.getText()) && button.isShowing())
				return button;
		}
		throw new AssertionError("Visible dialog button is not present: " + text);
	}

	private static JTextField findProjectNameField(ProjectDialog dialog) {
		for (Component component : flatten(dialog)) {
			if (component instanceof JTextField field && field.isShowing()) return field;
		}
		throw new AssertionError("New Project dialog does not expose a visible project-name field");
	}

	private static List<Component> flatten(Component root) {
		List<Component> result = new ArrayList<>();
		result.add(root);
		if (root instanceof Container container) {
			for (Component child : container.getComponents()) result.addAll(flatten(child));
		}
		return result;
	}
}
