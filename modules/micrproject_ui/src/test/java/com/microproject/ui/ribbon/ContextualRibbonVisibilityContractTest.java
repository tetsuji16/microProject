/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.ribbon;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.menu.MenuActionMapSupport;
import com.microproject.menu.MenuManager;

/** Locks the view-dependent contextual-tab visibility contract without duplicating routing. */
class ContextualRibbonVisibilityContractTest {
	@Test
	void contextualFormatTabIsHiddenByDefaultAndCanBeShownForTheActiveView() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			MenuManager manager = MenuManager.getInstance(MenuActionMapSupport.noopActionMap());
			JPanel host = manager.createRibbonPanel(MenuManager.STANDARD_RIBBON, null);
			ModernRibbonPanel panel = (ModernRibbonPanel) host.getClientProperty(ModernRibbonPanel.CONTEXTUAL_TABS_PROPERTY);

			assertFalse(panel.isContextualTabVisible("FormatRibbonTask"));
			panel.setVisibleContextualTabs(Set.of("FormatRibbonTask"));
			panel.setContextualTabTitles(Map.of("FormatRibbonTask", "Gantt Chart Format"));
			assertTrue(panel.isContextualTabVisible("FormatRibbonTask"));
			assertTrue(host.getComponentCount() > 0);

			// A non-Gantt view clears the contextual selection rather than leaving a
			// stale Format tab advertised after the view transition.
			panel.setVisibleContextualTabs(Set.of());
			assertFalse(panel.isContextualTabVisible("FormatRibbonTask"));
		});
	}
}
