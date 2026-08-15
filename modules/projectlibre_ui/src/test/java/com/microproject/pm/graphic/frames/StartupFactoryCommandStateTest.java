package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.JPanel;

import org.junit.jupiter.api.Test;

import com.microproject.menu.MenuManager;

class StartupFactoryCommandStateTest {
	@Test
	void successfulLoginRestoresGlobalFileCommandsAfterStartupGate() {
		GraphicManager graphicManager = new GraphicManager(new JPanel());
		MenuManager menuManager = graphicManager.getMenuManager();
		menuManager.createRibbonPanel(MenuManager.STANDARD_RIBBON, null);

		graphicManager.setConnected(false);
		assertCommandsEnabled(menuManager, false);

		StartupFactory.markLoginSuccessful(graphicManager);

		assertCommandsEnabled(menuManager, true);
	}

	private static void assertCommandsEnabled(MenuManager menuManager, boolean expected) {
		for (String id : List.of("RibbonNewProject", "RibbonOpenProject", "RibbonRecentProjects", "RibbonImportProject")) {
			AbstractButton button = menuManager.getToolButtonsFromId(id).stream()
				.map(AbstractButton.class::cast)
				.filter(candidate -> id.equals(candidate.getActionCommand()))
				.findFirst()
				.orElseThrow(() -> new AssertionError(id + " was not created"));
			if (expected) {
				assertTrue(button.isEnabled(), () -> id + " must be enabled after a successful login");
			} else {
				assertFalse(button.isEnabled(), () -> id + " must be disabled while startup is gated");
			}
		}
	}
}
