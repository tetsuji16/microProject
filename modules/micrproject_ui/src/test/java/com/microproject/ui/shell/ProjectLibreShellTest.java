/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
 * Copyright (c) 2026 microProject
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package com.microproject.ui.shell;

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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.microproject.ui.theme.MicroProjectTheme;
import com.microproject.menu.MenuActionMapSupport;
import com.microproject.menu.MenuManager;

class ProjectLibreShellTest {
	@BeforeAll
	static void installMicroProjectTheme() {
		MicroProjectTheme.installLight();
	}

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
		assertTrue(hasComponent(panel, OfficeChromePanel.SEARCH_BOX_NAME));
		assertTrue(hasComponent(panel, OfficeChromePanel.SEARCH_FIELD_NAME));
		assertTrue(hasComponent(panel, OfficeChromePanel.APPLICATION_ICON_NAME));
		assertTrue(hasComponent(panel, OfficeChromePanel.HELP_BUTTON_NAME));
		assertTrue(hasComponent(panel, OfficeChromePanel.AUTO_SAVE_NAME));
		assertTrue(hasComponent(panel, OfficeChromePanel.DOCUMENT_TITLE_NAME));
		assertTrue(hasComponent(panel, OfficeChromePanel.WINDOW_BUTTONS_PLACEHOLDER_NAME));
	}

	@Test
	void compactDocumentTitleKeepsTheFileNameAndModifiedMarker() {
		assertEquals("Commercial construction project plan2.pod *",
			OfficeChromePanel.compactDocumentTitle("C:\\projects\\Commercial construction project plan2.pod *"));
		assertEquals("ProjectLibre", OfficeChromePanel.compactDocumentTitle(""));
	}

	@Test
	void officeChromeSearchBoxKeepsResponsiveBounds() {
		OfficeChromePanel panel = new OfficeChromePanel(MenuManager.getInstance(MenuActionMapSupport.noopActionMap()), new JPanel(), () -> {});
		assertEquals(2, panel.getComponentCount());
		JComponent search = findComponent(panel, OfficeChromePanel.SEARCH_BOX_NAME);
		assertEquals(180, search.getMinimumSize().width);
		assertTrue(search.getMaximumSize().width >= search.getPreferredSize().width);
	}

	@Test
	void officeChromeHeaderUsesCompactOfficeLikeHeight() {
		OfficeChromePanel panel = new OfficeChromePanel(MenuManager.getInstance(MenuActionMapSupport.noopActionMap()), new JPanel(), () -> {});
		JComponent header = (JComponent) ((BorderLayout) panel.getLayout()).getLayoutComponent(BorderLayout.NORTH);
		JComponent search = findComponent(panel, OfficeChromePanel.SEARCH_BOX_NAME);
		JComponent autoSave = findComponent(panel, OfficeChromePanel.AUTO_SAVE_NAME);

		assertEquals(32, header.getPreferredSize().height);
		assertEquals(24, search.getPreferredSize().height);
		assertEquals(18, autoSave.getPreferredSize().height);
	}

	@Test
	void officeChromeQuickAccessUsesMicrosoftProjectOrder() {
		OfficeChromePanel panel = new OfficeChromePanel(MenuManager.getInstance(MenuActionMapSupport.noopActionMap()), new JPanel(), () -> {});
		panel.setSize(900, 160);
		layoutRecursively(panel);

		JComponent quickAccess = findComponent(panel, OfficeChromePanel.QUICK_ACCESS_NAME);
		java.util.List<String> actionIds = new java.util.ArrayList<>();
		for (java.awt.Component component : quickAccess.getComponents()) {
			if (component instanceof JComponent child && child.getName() != null
				&& child.getName().startsWith("RibbonTopBar")) {
				actionIds.add(child.getName());
			}
		}
		assertEquals(java.util.List.of("RibbonTopBarSaveProject", "RibbonTopBarUndo", "RibbonTopBarRedo"), actionIds);
	}

	@Test
	void officeChromeRightActionsStayAnchoredToTheWindowEdge() {
		OfficeChromePanel panel = new OfficeChromePanel(MenuManager.getInstance(MenuActionMapSupport.noopActionMap()), new JPanel(), () -> {});
		panel.setSize(292, 160);
		layoutRecursively(panel);
		JComponent right = findComponent(panel, OfficeChromePanel.RIGHT_ACTIONS_NAME);
		assertTrue(right.getX() >= 0);
		assertTrue(right.getX() + right.getWidth() <= panel.getWidth(),
			"right=" + right.getX() + "+" + right.getWidth() + ", panel=" + panel.getWidth());
		assertTrue(right.getX() >= panel.getWidth() / 3,
			"right title-bar actions must not drift into the left/content cluster");
	}

	private static void layoutRecursively(java.awt.Component component) {
		component.doLayout();
		if (component instanceof java.awt.Container container) {
			for (java.awt.Component child : container.getComponents()) layoutRecursively(child);
		}
	}

	private static JComponent findComponent(JComponent root, String name) {
		for (java.awt.Component component : com.microproject.menu.testsupport.UiComponentWalker.flatten(root)) {
			if (component instanceof JComponent jComponent && name.equals(jComponent.getName())) {
				return jComponent;
			}
		}
		throw new AssertionError("Component not found with name: " + name);
	}

	private static boolean hasComponent(JComponent root, String name) {
		for (java.awt.Component component : com.microproject.menu.testsupport.UiComponentWalker.flatten(root)) {
			if (component instanceof JComponent jComponent && name.equals(jComponent.getName())) {
				return true;
			}
		}
		return false;
	}
}
