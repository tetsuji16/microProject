/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.menu.MenuActionMapSupport;
import com.microproject.menu.MenuManager;

/** A rebuilt ribbon shell must not leave the Quick Access controls of the replaced one registered. */
class OfficeChromeRebuildLeakTest {
	private static final String COMMAND = "RibbonTopBarSaveProject";

	@Test
	void rebuildingTheShellKeepsTheNumberOfRegisteredQuickAccessControls() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			MenuManager manager = MenuManager.getInstance(MenuActionMapSupport.noopActionMap());
			JPanel first = manager.createRibbonPanel(MenuManager.STANDARD_RIBBON, null);
			OfficeChromePanel previous = new OfficeChromePanel(manager, first, () -> { });
			int registered = manager.getRibbonFactory().getRibbonControlsFromId(COMMAND).size();
			assertTrue(registered >= 1, "the Quick Access control must be registered for its command");
			for (int rebuild = 0; rebuild < 2; rebuild++) {
				JPanel ribbon = manager.createRibbonPanel(MenuManager.STANDARD_RIBBON, null);
				OfficeChromePanel next = new OfficeChromePanel(manager, ribbon, () -> { });
				MicroProjectShell.releaseRibbonShell(previous);
				previous = next;
				assertEquals(registered, manager.getRibbonFactory().getRibbonControlsFromId(COMMAND).size(),
					"only the new shell's controls stay registered after rebuild " + (rebuild + 1));
			}
		});
	}
}
