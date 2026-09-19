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

import java.util.Locale;

import javax.swing.AbstractButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.menu.ExtToolBarFactory;
import com.microproject.menu.MenuActionMapSupport;
import com.microproject.menu.MenuManager;
import com.microproject.menu.MenuRibbonCommandSource;
import com.microproject.menu.testsupport.MenuDefinitionSupport;
import com.microproject.menu.testsupport.UiComponentWalker;

class ModernRibbonPanelDisplayModeTest {
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
			ribbon.revealAutoHiddenRibbon();
			assertTrue(ribbon.isVisible());
			assertTrue(ribbon.isCommandSurfaceVisible(), "Alt-style reveal must restore command bands");
		});
	}

	private static AbstractButton findButton(JPanel root, String text) {
		return UiComponentWalker.flatten(root).stream()
			.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
			.filter(button -> text.equals(button.getText())).findFirst().orElseThrow();
	}

	private static AbstractButton findButtonByCommand(JPanel root, String command) {
		return UiComponentWalker.flatten(root).stream()
			.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
			.filter(button -> command.equals(button.getActionCommand())).findFirst().orElseThrow();
	}
}
