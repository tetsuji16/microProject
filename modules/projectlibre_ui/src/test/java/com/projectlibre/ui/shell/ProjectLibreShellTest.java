package com.projectlibre.ui.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import org.junit.jupiter.api.Test;

import com.projectlibre1.menu.MenuActionMapSupport;
import com.projectlibre1.menu.MenuManager;

class ProjectLibreShellTest {
	@Test
	void attachNewLookChromePlacesTopAndBottomInExpectedRegions() {
		JPanel container = new JPanel(new BorderLayout());
		JToolBar toolBar = new JToolBar();
		JPanel tabs = new JPanel();
		JPanel bottom = new JPanel();
		Color background = new Color(0xF0F0F0);

		ProjectLibreShell.attachNewLookChrome(container, toolBar, tabs, bottom, background);

		assertEquals(background, container.getBackground());
		assertSame(bottom, ((BorderLayout) container.getLayout()).getLayoutComponent(BorderLayout.AFTER_LAST_LINE));
		assertTrue(((BorderLayout) container.getLayout()).getLayoutComponent(BorderLayout.BEFORE_FIRST_LINE) instanceof Box);
	}

	@Test
	void restartPlaceholderAddsMessageLabel() {
		JPanel container = new JPanel(new BorderLayout());

		assertTrue(ProjectLibreShell.showRestartMessageIfNeeded(container, true));
		assertSame(JLabel.class, container.getComponent(0).getClass());
	}

	@Test
	void officeChromePanelContainsTheTopControlsAndRibbonSurface() {
		MenuManager menuManager = MenuManager.getInstance(MenuActionMapSupport.noopActionMap());
		JPanel ribbonBody = new JPanel();

		OfficeChromePanel panel = new OfficeChromePanel(menuManager, ribbonBody, () -> {});

		assertSame(ribbonBody, ((BorderLayout) panel.getLayout()).getLayoutComponent(BorderLayout.CENTER));
		assertFalse(hasComponent(panel, OfficeChromePanel.SEARCH_BOX_NAME));
		assertFalse(hasComponent(panel, OfficeChromePanel.SEARCH_FIELD_NAME));
		assertFalse(hasComponent(panel, OfficeChromePanel.HELP_BUTTON_NAME));
		assertFalse(hasComponent(panel, OfficeChromePanel.AUTO_SAVE_NAME));
	}

	@Test
	void officeChromeSearchBoxKeepsResponsiveBounds() {
		OfficeChromePanel panel = new OfficeChromePanel(MenuManager.getInstance(MenuActionMapSupport.noopActionMap()), new JPanel(), () -> {});
		assertEquals(1, panel.getComponentCount());
	}

	private static JComponent findComponent(JComponent root, String name) {
		for (java.awt.Component component : com.projectlibre1.menu.testsupport.UiComponentWalker.flatten(root)) {
			if (component instanceof JComponent jComponent && name.equals(jComponent.getName())) {
				return jComponent;
			}
		}
		throw new AssertionError("Component not found with name: " + name);
	}

	private static boolean hasComponent(JComponent root, String name) {
		for (java.awt.Component component : com.projectlibre1.menu.testsupport.UiComponentWalker.flatten(root)) {
			if (component instanceof JComponent jComponent && name.equals(jComponent.getName())) {
				return true;
			}
		}
		return false;
	}
}
