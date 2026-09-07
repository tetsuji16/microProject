/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.dialog.AbstractDialog;
import com.microproject.dialog.AboutDialog;
import com.microproject.dialog.HelpDialog;
import com.microproject.dialog.LocaleDialog;
import com.microproject.dialog.ProjectDialog;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.util.Environment;

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

	@AfterEach
	void closeWindow() throws Exception {
		if (manager != null) SwingUtilities.invokeAndWait(manager::cleanUp);
		if (window != null) SwingUtilities.invokeAndWait(window::dispose);
		Environment.setRibbonUI(previousRibbonUi);
		Environment.setNewLook(previousNewLook);
		for (Window open : Window.getWindows()) {
			if (open instanceof ProjectDialog || open instanceof LocaleDialog
				|| open instanceof HelpDialog || open instanceof AboutDialog) {
				open.dispose();
			}
		}
	}

	@Test
	void robotInvokesRealFileRibbonCommandsAndOpensTheirDialogs() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
			"A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);

		SwingUtilities.invokeAndWait(() -> {
			window = new MainRibbonFrame("microProject — real ribbon command acceptance", null, null);
			manager = new GraphicManager(window);
			window.setGraphicManager(manager);
			manager.initView();
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

	private static List<Component> flatten(Component root) {
		List<Component> result = new ArrayList<>();
		result.add(root);
		if (root instanceof Container container) {
			for (Component child : container.getComponents()) result.addAll(flatten(child));
		}
		return result;
	}
}
