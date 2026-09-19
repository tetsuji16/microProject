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
import com.microproject.ui.ribbon.ModernRibbonPanel;
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
		ModernRibbonPanel ribbon = (ModernRibbonPanel) ribbonHost.getClientProperty(ModernRibbonPanel.CONTEXTUAL_TABS_PROPERTY);
		OfficeChromePanel chrome = new OfficeChromePanel(manager, ribbonHost, () -> { });
		SwingUtilities.invokeAndWait(() -> {
			frame = new JFrame("Office chrome ribbon display acceptance");
			frame.add(chrome, BorderLayout.NORTH);
			frame.add(new JPanel(), BorderLayout.CENTER);
			frame.setSize(1200, 500);
			frame.setLocationByPlatform(true);
			frame.setVisible(true);
		});
		AbstractButton options = findButton(chrome, OfficeChromePanel.RIBBON_DISPLAY_OPTIONS_NAME);
		Robot robot = new Robot();
		robot.setAutoDelay(40);
		click(robot, options);
		click(robot, popupItem(UsabilityStrings.text("chrome.ribbonTabsOnly")));
		GuiAcceptanceSupport.await(() -> ribbon.getRibbonDisplayMode() == RibbonDisplayMode.TABS_ONLY,
			"title-bar display options did not switch to tabs-only mode");
		assertTrue(!ribbon.isCommandSurfaceVisible());

		GuiAcceptanceSupport.await(options::isShowing,
			"title-bar display options became unreachable after collapsing the ribbon");
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

	private static void click(Robot robot, AbstractButton button) {
		Point point = button.getLocationOnScreen();
		robot.mouseMove(point.x + button.getWidth() / 2, point.y + button.getHeight() / 2);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
	}
}
