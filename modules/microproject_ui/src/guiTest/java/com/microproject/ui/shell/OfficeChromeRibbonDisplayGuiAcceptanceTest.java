/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.shell;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.BorderLayout;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.util.Arrays;

import javax.swing.AbstractButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.dialog.UsabilityStrings;
import com.microproject.menu.MenuActionMapSupport;
import com.microproject.menu.MenuManager;
import com.microproject.menu.testsupport.UiComponentWalker;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.ui.ribbon.RibbonController;
import com.microproject.ui.ribbon.RibbonDisplayMode;

class OfficeChromeRibbonDisplayGuiAcceptanceTest {
	private JFrame frame;

	@AfterEach
	void closeWindow() throws Exception {
		if (frame != null) SwingUtilities.invokeAndWait(() -> frame.dispose());
	}

	@Test
	void titleBarDisplayOptionsPhysicallySwitchBetweenTabsOnlyAndAlwaysShow() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		MenuManager manager = MenuManager.getInstance(MenuActionMapSupport.noopActionMap());
		JPanel ribbonHost = manager.createRibbonPanel(MenuManager.STANDARD_RIBBON, null);
		RibbonController ribbon = (RibbonController) ribbonHost.getClientProperty(RibbonController.CONTEXTUAL_TABS_PROPERTY);
		OfficeChromePanel chrome = new OfficeChromePanel(manager, ribbonHost, () -> { });
		SwingUtilities.invokeAndWait(() -> {
			frame = new JFrame("Office chrome ribbon display acceptance");
			frame.add(chrome, BorderLayout.NORTH);
			frame.add(new JPanel(), BorderLayout.CENTER);
			frame.setSize(1200, 500);
			frame.setLocationByPlatform(true);
			frame.setAlwaysOnTop(true);
			frame.setVisible(true);
			frame.toFront();
			frame.requestFocus();
		});
		AbstractButton options = findButton(chrome, OfficeChromePanel.RIBBON_DISPLAY_OPTIONS_NAME);
		Robot robot = new Robot();
		robot.setAutoDelay(40);
		GuiAcceptanceSupport.await(frame::isActive, "office chrome test window did not become active");
		GuiAcceptanceSupport.await(() -> options.isShowing()
			&& options.getWidth() > 0 && options.getHeight() > 0,
			"title-bar display options button did not become laid out");
		assertTrue(OfficeChromePanel.RIGHT_ACTIONS_NAME.equals(options.getParent().getName()),
			"ribbon display options should share the title bar's right-side command cluster");
		Rectangle surfaceBounds = bounds(chrome, OfficeChromePanel.RIBBON_SURFACE_NAME);
		Rectangle buttonBounds = bounds(chrome, OfficeChromePanel.RIBBON_DISPLAY_OPTIONS_NAME);
		assertTrue(buttonBounds.getMaxX() >= surfaceBounds.getMaxX() - 80,
			"ribbon display options are not aligned with the window's right edge: surface="
				+ surfaceBounds + ", button=" + buttonBounds);
		assertTrue(buttonBounds.getY() < surfaceBounds.getY(),
			"ribbon display options must remain outside the ribbon surface: surface="
				+ surfaceBounds + ", button=" + buttonBounds);
		robot.waitForIdle();
		click(robot, options);
		click(robot, popupItem(UsabilityStrings.text("chrome.ribbonTabsOnly")));
		GuiAcceptanceSupport.await(() -> ribbon.getRibbonDisplayMode() == RibbonDisplayMode.TABS_ONLY,
			"title-bar display options did not switch to tabs-only mode");
		assertTrue(!ribbon.isCommandSurfaceVisible());

		GuiAcceptanceSupport.await(options::isShowing,
			"title-bar display options became unreachable after collapsing the command bands");
		click(robot, options);
		click(robot, popupItem(UsabilityStrings.text("chrome.ribbonAlwaysShow")));
		GuiAcceptanceSupport.await(() -> ribbon.getRibbonDisplayMode() == RibbonDisplayMode.ALWAYS_SHOW,
			"title-bar display options did not restore the command surface");
		assertTrue(ribbon.isCommandSurfaceVisible());
	}

	private static AbstractButton findButton(JPanel root, String name) {
		return UiComponentWalker.flatten(root).stream().filter(AbstractButton.class::isInstance)
			.map(AbstractButton.class::cast).filter(button -> name.equals(button.getName())).findFirst().orElseThrow();
	}

	private static AbstractButton popupItem(String text) throws Exception {
		GuiAcceptanceSupport.await(() -> Arrays.stream(MenuSelectionManager.defaultManager().getSelectedPath())
			.anyMatch(JPopupMenu.class::isInstance), "ribbon display options popup did not open");
		JPopupMenu popup = Arrays.stream(MenuSelectionManager.defaultManager().getSelectedPath())
			.filter(JPopupMenu.class::isInstance).map(JPopupMenu.class::cast)
			.filter(candidate -> OfficeChromePanel.RIBBON_DISPLAY_OPTIONS_POPUP_NAME.equals(candidate.getName()))
			.findFirst().orElseThrow();
		AbstractButton item = UiComponentWalker.flatten(popup).stream().filter(AbstractButton.class::isInstance)
			.map(AbstractButton.class::cast).filter(button -> text.equals(button.getText())).findFirst().orElseThrow();
		GuiAcceptanceSupport.await(item::isShowing, "ribbon display menu item did not become visible: " + text);
		return item;
	}

	private static Rectangle bounds(JPanel root, String name) throws Exception {
		java.awt.Component component = UiComponentWalker.flatten(root).stream()
			.filter(candidate -> name.equals(candidate.getName())).findFirst().orElseThrow();
		Rectangle[] result = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> result[0] = new Rectangle(component.getLocationOnScreen(), component.getSize()));
		return result[0];
	}

	private static void click(Robot robot, AbstractButton button) {
		robot.waitForIdle();
		Point point = button.getLocationOnScreen();
		robot.mouseMove(point.x + button.getWidth() / 2, point.y + button.getHeight() / 2);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
	}
}
