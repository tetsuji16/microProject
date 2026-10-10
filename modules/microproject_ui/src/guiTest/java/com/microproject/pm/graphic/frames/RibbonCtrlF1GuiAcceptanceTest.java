/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.awt.Robot;
import java.awt.event.KeyEvent;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.ui.ribbon.RibbonController;
import com.microproject.ui.ribbon.RibbonDisplayMode;
import com.microproject.util.Environment;

/** Physical Ctrl+F1 coverage for the document root-pane shortcut. */
class RibbonCtrlF1GuiAcceptanceTest {
	private MainRibbonFrame window;
	private GraphicManager manager;
	private boolean previousRibbonUi;
	private boolean previousNewLook;

	@AfterEach
	void closeWindow() throws Exception {
		if (manager != null) SwingUtilities.invokeAndWait(() -> manager.cleanUp());
		if (window != null) SwingUtilities.invokeAndWait(() -> window.dispose());
		Environment.setRibbonUI(previousRibbonUi);
		Environment.setNewLook(previousNewLook);
	}

	@Test
	void physicalCtrlF1TogglesRibbonCommandsAndRestoresAutoHiddenRibbon() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		SwingUtilities.invokeAndWait(() -> {
			window = new MainRibbonFrame("Ctrl+F1 ribbon acceptance", null);
			manager = new GraphicManager(window);
			window.setGraphicManager(manager);
			manager.initView();
			window.setSize(920, 560);
			window.setLocationByPlatform(true);
			window.setAlwaysOnTop(true);
			window.setVisible(true);
		});
		GuiAcceptanceSupport.await(() -> window.isShowing() && window.getRibbonPanel() != null,
			"document window and ribbon did not become visible");
		RibbonController ribbon = (RibbonController) window.getRibbonPanel()
			.getClientProperty(RibbonController.CONTEXTUAL_TABS_PROPERTY);
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(40);
		java.awt.Rectangle[] windowBounds = new java.awt.Rectangle[1];
		SwingUtilities.invokeAndWait(() -> windowBounds[0] = new java.awt.Rectangle(window.getLocationOnScreen(), window.getSize()));
		robot.mouseMove(windowBounds[0].x + Math.min(40, windowBounds[0].width / 2), windowBounds[0].y + 12);
		robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
		GuiAcceptanceSupport.await(window::isActive,
			"physical title-bar activation did not give the document window keyboard focus");
		pressCtrlF1(robot);
		GuiAcceptanceSupport.await(() -> ribbon.getRibbonDisplayMode() == RibbonDisplayMode.TABS_ONLY,
			"physical Ctrl+F1 did not hide ribbon commands");
		assertTrue(!ribbon.isCommandSurfaceVisible());

		pressCtrlF1(robot);
		GuiAcceptanceSupport.await(() -> ribbon.getRibbonDisplayMode() == RibbonDisplayMode.ALWAYS_SHOW,
			"physical Ctrl+F1 did not restore ribbon commands");
		assertTrue(ribbon.isCommandSurfaceVisible());

		SwingUtilities.invokeAndWait(() -> ribbon.setRibbonDisplayMode(RibbonDisplayMode.AUTO_HIDE));
		pressCtrlF1(robot);
		GuiAcceptanceSupport.await(() -> ribbon.getRibbonDisplayMode() == RibbonDisplayMode.ALWAYS_SHOW,
			"physical Ctrl+F1 did not restore an auto-hidden ribbon");
	}

	private static void pressCtrlF1(Robot robot) {
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_F1);
		robot.keyRelease(KeyEvent.VK_F1);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		robot.waitForIdle();
	}
}
