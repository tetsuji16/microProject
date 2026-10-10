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
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import java.util.prefs.Preferences;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.microproject.ui.theme.MicroProjectTheme;
import com.microproject.menu.MenuActionMapSupport;
import com.microproject.menu.MenuManager;
import com.microproject.ui.ribbon.RibbonDisplayPreferences;

class MicroProjectShellTest {
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
		Color background = new Color(0xF3F2F1);

		MicroProjectShell.attachNewLookChrome(container, toolBar, tabs, bottom, background);

		assertEquals(background, container.getBackground());
		assertSame(bottom, ((BorderLayout) container.getLayout()).getLayoutComponent(BorderLayout.AFTER_LAST_LINE));
		assertTrue(((BorderLayout) container.getLayout()).getLayoutComponent(BorderLayout.BEFORE_FIRST_LINE) instanceof Box);
	}

	@Test
	void officeChromePanelKeepsWindowControlsAtTheTopAndRibbonOptionsAtTheBottomRight() {
		MenuManager menuManager = MenuManager.getInstance(MenuActionMapSupport.noopActionMap());
		JPanel ribbonBody = new JPanel();

		OfficeChromePanel panel = new OfficeChromePanel(menuManager, ribbonBody, () -> {});

		JComponent surface = (JComponent) ((BorderLayout) panel.getLayout()).getLayoutComponent(BorderLayout.CENTER);
		assertEquals(OfficeChromePanel.RIBBON_SURFACE_NAME, surface.getName());
		assertTrue(java.util.Arrays.asList(((java.awt.Container) surface).getComponents()).contains(ribbonBody));
		assertTrue(hasComponent(panel, OfficeChromePanel.SEARCH_BOX_NAME));
		assertTrue(hasComponent(panel, OfficeChromePanel.SEARCH_FIELD_NAME));
		assertTrue(hasComponent(panel, OfficeChromePanel.HELP_BUTTON_NAME));
		assertTrue(hasComponent(panel, OfficeChromePanel.AUTO_SAVE_NAME));
		assertTrue(hasComponent(panel, OfficeChromePanel.DOCUMENT_TITLE_NAME));
		assertFalse(hasComponent(panel, OfficeChromePanel.WINDOW_BUTTONS_PLACEHOLDER_NAME),
			"the shared header fixture has no native window button area to reserve");
		assertTrue(hasComponent(panel, OfficeChromePanel.RIBBON_OPTIONS_ROW_NAME),
			"the ribbon display-options trigger belongs at the lower-right of the ribbon surface");
		assertTrue(hasComponent(panel, OfficeChromePanel.RIBBON_DISPLAY_OPTIONS_NAME));
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

		assertEquals(48, header.getPreferredSize().height);
		assertEquals(24, search.getPreferredSize().height);
		assertEquals(18, autoSave.getPreferredSize().height);
	}

	@Test
	void compactDocumentTitleKeepsOnlyTheFileName() {
		assertEquals("Commercial construction project plan2.pod *",
			OfficeChromePanel.compactDocumentTitle("microProject - C:\\projects\\Commercial construction project plan2.pod *"));
		assertEquals("microProject", OfficeChromePanel.compactDocumentTitle(""));
	}

	@Test
	void officeChromeQuickAccessUsesMicrosoftProjectOrder() {
		OfficeChromePanel panel = new OfficeChromePanel(MenuManager.getInstance(MenuActionMapSupport.noopActionMap()), new JPanel(), () -> {});
		panel.setSize(900, 160);
		layoutRecursively(panel);

		JComponent quickAccess = findComponent(panel, OfficeChromePanel.QUICK_ACCESS_COMMANDS_NAME);
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
	void officeChromeQuickAccessButtonsRetainVisibleIconsAndHitAreas() {
		OfficeChromePanel panel = new OfficeChromePanel(MenuManager.getInstance(MenuActionMapSupport.noopActionMap()), new JPanel(), () -> {});
		panel.setSize(900, 160);
		layoutRecursively(panel);

		JComponent quickAccessCommands = findComponent(panel, OfficeChromePanel.QUICK_ACCESS_COMMANDS_NAME);
		int previousX = -1;
		for (java.awt.Component component : quickAccessCommands.getComponents()) {
			if (component instanceof javax.swing.AbstractButton button
				&& button.getName() != null && button.getName().startsWith("RibbonTopBar")) {
				assertTrue(button.isVisible());
				assertTrue(button.getIcon() != null, button.getName() + " must retain its QAT icon");
				assertEquals(24, button.getWidth());
				assertTrue(button.getX() > previousX);
				previousX = button.getX();
			}
		}
	}

	@Test
	void officeChromeQuickAccessReflectsSelectedStateFromItsCommandAction() {
		MenuManager manager = MenuManager.getInstance(MenuActionMapSupport.noopActionMap());
		Action gridlines = manager.getActionFromId("RibbonGridlines");
		assertTrue(gridlines != null);
		Object previousSelected = gridlines.getValue(Action.SELECTED_KEY);
		Preferences preferences = Preferences.userNodeForPackage(RibbonDisplayPreferences.class);
		String previousCommands = preferences.get("ribbonQuickAccessCommands", null);
		Action[] buttonAction = new Action[1];
		Object[] previousButtonSelected = new Object[1];
		try {
			RibbonDisplayPreferences.saveQuickAccessCommands(java.util.List.of("RibbonGridlines"));
			OfficeChromePanel panel = new OfficeChromePanel(manager, new JPanel(), () -> {});
			AbstractButton button = (AbstractButton) findComponent(panel, "RibbonGridlines");
			buttonAction[0] = button.getAction();
			previousButtonSelected[0] = buttonAction[0].getValue(Action.SELECTED_KEY);
			buttonAction[0].putValue(Action.SELECTED_KEY, Boolean.TRUE);
			BufferedImage selectedImage = paint(button);
			buttonAction[0].putValue(Action.SELECTED_KEY, Boolean.FALSE);
			assertFalse(imagesEqual(selectedImage, paint(button)),
				"the visible QAT button treatment must follow its command action's selected state");
		} finally {
			gridlines.putValue(Action.SELECTED_KEY, previousSelected);
			if (buttonAction[0] != null) buttonAction[0].putValue(Action.SELECTED_KEY, previousButtonSelected[0]);
			if (previousCommands == null) preferences.remove("ribbonQuickAccessCommands");
			else preferences.put("ribbonQuickAccessCommands", previousCommands);
		}
	}

	@Test
	void officeChromeHelpButtonPreservesItsMulticolorQuestionMarkIcon() {
		OfficeChromePanel panel = new OfficeChromePanel(MenuManager.getInstance(MenuActionMapSupport.noopActionMap()), new JPanel(), () -> {});
		AbstractButton help = (AbstractButton) findComponent(panel, OfficeChromePanel.HELP_BUTTON_NAME);

		assertTrue(help.getIcon() instanceof ImageIcon, "help uses the mapped scalable ribbon icon");
		ImageIcon icon = (ImageIcon) help.getIcon();
		BufferedImage image = (BufferedImage) icon.getImage();
		Set<Integer> visibleColors = new HashSet<>();
		int whitePixels = 0;
		int greenPixels = 0;
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				int pixel = image.getRGB(x, y);
				if ((pixel >>> 24) == 0) continue;
				int red = (pixel >>> 16) & 0xff;
				int green = (pixel >>> 8) & 0xff;
				int blue = pixel & 0xff;
				visibleColors.add(pixel & 0x00ffffff);
				if (red > 225 && green > 225 && blue > 225) whitePixels++;
				if (green > 90 && green > red * 1.5 && green > blue * 1.2) greenPixels++;
			}
		}

		assertEquals(16, image.getWidth());
		assertEquals(16, image.getHeight());
		assertTrue(whitePixels > 0, "the question-mark icon must retain its white circular fill");
		assertTrue(greenPixels > 0, "the question mark must retain its MSP-green strokes");
		assertTrue(visibleColors.size() > 1, "the icon must retain multiple source colors rather than becoming a solid monochrome silhouette");
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

	private static BufferedImage paint(AbstractButton button) {
		button.setSize(button.getPreferredSize());
		BufferedImage image = new BufferedImage(button.getWidth(), button.getHeight(), BufferedImage.TYPE_INT_ARGB);
		java.awt.Graphics graphics = image.getGraphics();
		try {
			button.paint(graphics);
		} finally {
			graphics.dispose();
		}
		return image;
	}

	private static boolean imagesEqual(BufferedImage first, BufferedImage second) {
		if (first.getWidth() != second.getWidth() || first.getHeight() != second.getHeight()) return false;
		for (int y = 0; y < first.getHeight(); y++) {
			for (int x = 0; x < first.getWidth(); x++) {
				if (first.getRGB(x, y) != second.getRGB(x, y)) return false;
			}
		}
		return true;
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
