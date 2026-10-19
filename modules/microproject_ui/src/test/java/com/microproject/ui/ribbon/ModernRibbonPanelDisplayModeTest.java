/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.ribbon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.AbstractButton;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.menu.ExtToolBarFactory;
import com.microproject.menu.MenuActionMapSupport;
import com.microproject.menu.MenuManager;
import com.microproject.menu.MenuRibbonCommandSource;
import com.microproject.menu.testsupport.MenuDefinitionSupport;
import com.microproject.menu.testsupport.UiComponentWalker;
import com.microproject.dialog.UsabilityStrings;

class ModernRibbonPanelDisplayModeTest {
	@Test
	void displayModePopupUsesOnePopupTriggerRouteAndItsActionChangesMode() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			JPanel host = createRibbonHost();
			ModernRibbonPanel ribbon = (ModernRibbonPanel) host.getClientProperty(ModernRibbonPanel.CONTEXTUAL_TABS_PROPERTY);
			AbstractButton tab = findButton(host, "Task");
			List<JPopupMenu> shownPopups = new ArrayList<>();
			ribbon.setDisplayModePopupPresenter((popup, event) -> shownPopups.add(popup));

			dispatch(tab, MouseEvent.MOUSE_PRESSED, true, MouseEvent.BUTTON3, 1);
			dispatch(tab, MouseEvent.MOUSE_RELEASED, true, MouseEvent.BUTTON3, 1);
			assertPopup(shownPopups, "chrome.ribbonCollapse");
			((javax.swing.JMenuItem) shownPopups.getFirst().getComponent(0)).doClick();
			assertEquals(RibbonDisplayMode.TABS_ONLY, ribbon.getRibbonDisplayMode());

			shownPopups.clear();
			dispatch(tab, MouseEvent.MOUSE_PRESSED, false, MouseEvent.BUTTON3, 1);
			dispatch(tab, MouseEvent.MOUSE_RELEASED, true, MouseEvent.BUTTON3, 1);
			assertPopup(shownPopups, "chrome.ribbonShow");
			((javax.swing.JMenuItem) shownPopups.getFirst().getComponent(0)).doClick();
			assertEquals(RibbonDisplayMode.ALWAYS_SHOW, ribbon.getRibbonDisplayMode());

			shownPopups.clear();
			dispatch(tab, MouseEvent.MOUSE_PRESSED, true, MouseEvent.BUTTON3, 1);
			dispatch(tab, MouseEvent.MOUSE_RELEASED, false, MouseEvent.BUTTON3, 1);
			assertPopup(shownPopups, "chrome.ribbonCollapse");

			shownPopups.clear();
			dispatch(tab, MouseEvent.MOUSE_PRESSED, false, MouseEvent.BUTTON1, 1);
			dispatch(tab, MouseEvent.MOUSE_RELEASED, false, MouseEvent.BUTTON1, 1);
			assertTrue(shownPopups.isEmpty(), "an ordinary click must not show the display-mode popup");
			dispatch(tab, MouseEvent.MOUSE_CLICKED, false, MouseEvent.BUTTON1, 2);
			assertEquals(RibbonDisplayMode.TABS_ONLY, ribbon.getRibbonDisplayMode(),
				"double-left-click collapse behavior must remain available");
		});
	}

	@Test
	void displayModesKeepTheSelectedTabAndCommandRegistrationWhileChangingOnlyChromeVisibility() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			ExtToolBarFactory buttons = new ExtToolBarFactory(MenuActionMapSupport.noopActionMap(),
				MenuDefinitionSupport.ribbonBundles(Locale.ROOT));
			SwingRibbonFactory factory = new SwingRibbonFactory(new MenuRibbonCommandSource(buttons),
				MenuDefinitionSupport.ribbonBundles(Locale.ROOT));
			JPanel host = factory.createPanel(MenuManager.STANDARD_RIBBON, () -> { });
			ModernRibbonPanel ribbon = (ModernRibbonPanel) host.getClientProperty(ModernRibbonPanel.CONTEXTUAL_TABS_PROPERTY);
			AbstractButton taskTab = findButton(host, "Task");
			taskTab.doClick();
			assertTrue(taskTab.isSelected());

			ribbon.setRibbonDisplayMode(RibbonDisplayMode.TABS_ONLY);
			assertEquals(RibbonDisplayMode.TABS_ONLY, ribbon.getRibbonDisplayMode());
			assertTrue(taskTab.isVisible(), "tabs-only mode must retain tab navigation");
			assertFalse(ribbon.isCommandSurfaceVisible(), "tabs-only mode must hide command bands");
			assertNotNull(findButtonByCommand(host, "RibbonInsert").getAction(),
				"hiding chrome must not unregister commands");

			ribbon.setRibbonDisplayMode(RibbonDisplayMode.ALWAYS_SHOW);
			assertTrue(taskTab.isSelected(), "visibility changes must not replace the active tab");
			assertTrue(ribbon.isCommandSurfaceVisible());

			ribbon.setRibbonDisplayMode(RibbonDisplayMode.AUTO_HIDE);
			assertFalse(ribbon.isVisible(), "auto-hide must remove the ribbon surface");
			ribbon.toggleRibbonCollapseMode();
			assertEquals(RibbonDisplayMode.ALWAYS_SHOW, ribbon.getRibbonDisplayMode(),
				"Ctrl+F1 from auto-hide must restore the full ribbon");
			ribbon.toggleRibbonCollapseMode();
			assertEquals(RibbonDisplayMode.TABS_ONLY, ribbon.getRibbonDisplayMode(),
				"Ctrl+F1 from full mode must collapse command bands");
			ribbon.toggleRibbonCollapseMode();
			assertEquals(RibbonDisplayMode.ALWAYS_SHOW, ribbon.getRibbonDisplayMode(),
				"Ctrl+F1 from tabs-only mode must restore command bands");

			ribbon.setRibbonDisplayMode(RibbonDisplayMode.AUTO_HIDE);
			ribbon.revealAutoHiddenRibbon();
			assertTrue(ribbon.isVisible());
			assertTrue(ribbon.isCommandSurfaceVisible(), "Alt-style reveal must restore command bands");
			ribbon.dismissAutoHiddenRibbon();
			assertEquals(RibbonDisplayMode.AUTO_HIDE, ribbon.getRibbonDisplayMode(),
				"temporary dismissal must preserve the user's selected display mode");
			assertFalse(ribbon.isVisible(), "dismissing a temporary reveal must hide the ribbon surface again");
		});
	}

	private static AbstractButton findButton(JPanel root, String text) {
		return UiComponentWalker.flatten(root).stream()
			.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
			.filter(button -> text.equals(button.getText())).findFirst().orElseThrow();
	}

	private static JPanel createRibbonHost() {
		ExtToolBarFactory buttons = new ExtToolBarFactory(MenuActionMapSupport.noopActionMap(),
			MenuDefinitionSupport.ribbonBundles(Locale.ROOT));
		SwingRibbonFactory factory = new SwingRibbonFactory(new MenuRibbonCommandSource(buttons),
			MenuDefinitionSupport.ribbonBundles(Locale.ROOT));
		return factory.createPanel(MenuManager.STANDARD_RIBBON, () -> { });
	}

	private static void dispatch(AbstractButton button, int id, boolean popupTrigger, int buttonNumber, int clickCount) {
		MouseEvent event = new MouseEvent(button, id, 1L, 0, 2, 3, clickCount, popupTrigger, buttonNumber);
		for (java.awt.event.MouseListener listener : button.getMouseListeners()) {
			switch (id) {
				case MouseEvent.MOUSE_PRESSED -> listener.mousePressed(event);
				case MouseEvent.MOUSE_RELEASED -> listener.mouseReleased(event);
				case MouseEvent.MOUSE_CLICKED -> listener.mouseClicked(event);
				default -> throw new IllegalArgumentException("unsupported mouse event: " + id);
			}
		}
	}

	private static void assertPopup(List<JPopupMenu> popups, String labelKey) {
		assertEquals(1, popups.size(), "one popup must be presented for one gesture");
		JPopupMenu popup = popups.getFirst();
		assertEquals(ModernRibbonPanel.DISPLAY_MODE_POPUP_NAME, popup.getName());
		assertEquals(1, popup.getComponentCount());
		assertEquals(UsabilityStrings.text(labelKey), ((javax.swing.JMenuItem) popup.getComponent(0)).getText());
	}

	private static AbstractButton findButtonByCommand(JPanel root, String command) {
		return UiComponentWalker.flatten(root).stream()
			.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
			.filter(button -> command.equals(button.getActionCommand())).findFirst().orElseThrow();
	}
}
