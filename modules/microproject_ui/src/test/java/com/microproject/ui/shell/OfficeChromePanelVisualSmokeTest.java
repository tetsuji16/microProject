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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.imageio.ImageIO;
import javax.swing.JPanel;

import org.junit.jupiter.api.Test;

import com.microproject.ui.ribbon.SwingRibbonFactory;
import com.microproject.ui.ribbon.ModernRibbonPanel;
import com.microproject.ui.ribbon.RibbonController;
import com.microproject.ui.theme.MicroProjectTheme;
import com.microproject.menu.ExtToolBarFactory;
import com.microproject.menu.MenuActionMapSupport;
import com.microproject.menu.MenuManager;
import com.microproject.menu.testsupport.MenuDefinitionSupport;
import com.microproject.menu.testsupport.UiComponentWalker;

class OfficeChromePanelVisualSmokeTest {
	@Test
	void rendersOfficeChromeRibbonSnapshot() throws IOException {
		MicroProjectTheme.installLight();
		assertEquals(new java.awt.Color(0xF3F2F1), MicroProjectTheme.tokens().ribbonChromeBackground(),
			"Office chrome should retain its light neutral gray");
		assertEquals(java.awt.Color.WHITE, MicroProjectTheme.tokens().ribbonSurfaceBackground(),
			"the expanded Office ribbon command surface should be white");
		MenuManager menuManager = MenuManager.getInstance(MenuActionMapSupport.noopActionMap());
		ExtToolBarFactory buttonFactory = new ExtToolBarFactory(
			MenuActionMapSupport.noopActionMap(),
			MenuDefinitionSupport.ribbonBundles(Locale.JAPAN));
		SwingRibbonFactory ribbonFactory = new SwingRibbonFactory(
			new com.microproject.menu.MenuRibbonCommandSource(buttonFactory),
			MenuDefinitionSupport.ribbonBundles(Locale.JAPAN));
		JPanel ribbonPanel = ribbonFactory.createPanel(MenuManager.STANDARD_RIBBON, () -> {});
		OfficeChromePanel panel = new OfficeChromePanel(menuManager, ribbonPanel, () -> {});
		panel.setSize(1024, 192);
		panel.doLayout();
		layoutRecursively(panel);
		assertSearchIsAttachedToTabRow(panel);
		JComponent searchBox = findNamedComponent(panel, OfficeChromePanel.SEARCH_BOX_NAME);
		RibbonController ribbonController = (RibbonController) ribbonPanel.getClientProperty(
			RibbonController.CONTEXTUAL_TABS_PROPERTY);
		ribbonController.setTabRowAccessory(null);
		assertNull(searchBox.getParent(), "removing the optional tab-row accessory should detach it");
		ribbonController.setTabRowAccessory(searchBox);
		assertSearchIsAttachedToTabRow(panel);
		assertTrue(ribbonPanel.getHeight() >= ribbonPanel.getPreferredSize().height,
			"the screenshot viewport must contain the full ribbon, including group captions");
		assertRibbonBandsUseTheAvailableWidth(panel);
		assertRibbonCommandsAreLeftAligned(panel);

		BufferedImage image = new BufferedImage(1024, 192, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		try {
			panel.printAll(graphics);
		} finally {
			graphics.dispose();
		}
		assertRibbonColorsArePresentInRenderedPixels(image, panel);
		assertSearchBoxHasNoOutline(image, panel, searchBox);

		Path output = Path.of("build", "reports", "ribbon", "office-chrome-ribbon-smoke.png");
		Files.createDirectories(output.getParent());
		ImageIO.write(image, "png", output.toFile());

		assertTrue(Files.exists(output));
		assertTrue(hasVisibleInk(image));
	}

	private static void assertRibbonBandsUseTheAvailableWidth(JPanel panel) {
		JComponent surface = UiComponentWalker.flatten(panel).stream()
			.filter(JComponent.class::isInstance)
			.map(JComponent.class::cast)
			.filter(component -> "projectLibreRibbonSurface".equals(component.getName()))
			.findFirst()
			.orElseThrow(() -> new AssertionError("ribbon surface was not created"));
		int rightEdge = UiComponentWalker.flatten(surface).stream()
			.filter(JComponent.class::isInstance)
			.map(JComponent.class::cast)
			.filter(component -> "projectLibreRibbonBand".equals(component.getName()))
			.mapToInt(component -> component.getX() + component.getWidth())
			.max()
			.orElse(0);
		assertTrue(rightEdge >= Math.ceil(surface.getWidth() * 0.70d),
			() -> "ribbon leaves more than 30% unused on the right: surface=" + surface.getWidth()
				+ " rightEdge=" + rightEdge);
	}

	private static void assertRibbonCommandsAreLeftAligned(JPanel panel) {
		UiComponentWalker.flatten(panel).stream()
			.filter(JComponent.class::isInstance)
			.map(JComponent.class::cast)
			.filter(component -> "projectLibreRibbonBand".equals(component.getName()))
			.forEach(band -> {
				int leftmostCommand = UiComponentWalker.flatten(band).stream()
					.filter(AbstractButton.class::isInstance)
					.map(AbstractButton.class::cast)
					.mapToInt(button -> javax.swing.SwingUtilities.convertPoint(button, 0, 0, band).x)
					.min()
					.orElse(-1);
				if (leftmostCommand >= 0) {
					assertTrue(leftmostCommand <= 16,
						() -> "ribbon commands are centered inside their band: band=" + band.getWidth()
							+ " leftmostCommand=" + leftmostCommand);
				}
			});
	}

	@Test
	void rendersEveryTabAtOfficeReferenceWidthsInEnglishAndJapanese() throws IOException {
		for (Locale locale : List.of(Locale.ROOT, Locale.JAPAN)) {
			for (int width : List.of(320, 480, 720, 760, 1024, 1200, 1440)) {
				renderTabContactSheet(locale, width);
			}
		}
	}

	private static void renderTabContactSheet(Locale locale, int width) throws IOException {
		// Keep screenshots independent of JUnit test ordering and other LAF tests.
		MicroProjectTheme.installLight();
		MenuManager menuManager = MenuManager.getInstance(MenuActionMapSupport.noopActionMap());
		ExtToolBarFactory buttonFactory = new ExtToolBarFactory(
			MenuActionMapSupport.noopActionMap(),
			MenuDefinitionSupport.ribbonBundles(locale));
		SwingRibbonFactory ribbonFactory = new SwingRibbonFactory(
			new com.microproject.menu.MenuRibbonCommandSource(buttonFactory),
			MenuDefinitionSupport.ribbonBundles(locale));
		var model = ribbonFactory.createModel(MenuManager.STANDARD_RIBBON);
		JPanel ribbonPanel = ribbonFactory.createPanel(model, () -> {});
		OfficeChromePanel panel = new OfficeChromePanel(menuManager, ribbonPanel, () -> {});
		int rowHeight = 192;
		BufferedImage sheet = new BufferedImage(width, rowHeight * model.getTabs().size(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D sheetGraphics = sheet.createGraphics();
		try {
			for (int index = 0; index < model.getTabs().size(); index++) {
				var tab = model.getTabs().get(index);
				findButton(panel, tab.getTitle()).doClick();
				panel.setSize(width, rowHeight);
				panel.doLayout();
				layoutRecursively(panel);
				assertSearchIsAttachedToTabRow(panel);
				if (width == 320) {
					assertTrue(!findNamedComponent(panel, OfficeChromePanel.SEARCH_BOX_NAME).isVisible(),
						"the search accessory should yield to tabs at narrow widths");
				}
				if (width >= 1024) {
					assertTrue(findNamedComponent(panel, OfficeChromePanel.SEARCH_BOX_NAME).isVisible(),
						"the search accessory should be visible when the tab row has room");
				}
				if (Locale.JAPAN.equals(locale) && "ProjectRibbonTask".equals(tab.getId()) && width >= 1024) {
					assertJapaneseStatusDateCaption(panel);
				}
				assertCollapsedGroupsUseIconTriggers(panel);
				assertNoVisibleCollapsedTabLauncher(panel);
				assertResponsiveGroupsRemainReachable(panel, tab.getId(), width);
				Graphics2D rowGraphics = (Graphics2D) sheetGraphics.create(0, index * rowHeight, width, rowHeight);
				try {
					panel.printAll(rowGraphics);
				} finally {
					rowGraphics.dispose();
				}
			}
		} finally {
			sheetGraphics.dispose();
		}

		String localeName = Locale.JAPAN.equals(locale) ? "ja" : "en";
		Path output = Path.of("build", "reports", "ribbon", "office-ribbon-" + localeName + "-" + width + ".png");
		Files.createDirectories(output.getParent());
		ImageIO.write(sheet, "png", output.toFile());
		assertTrue(hasVisibleInk(sheet));
	}

	private static void assertSearchIsAttachedToTabRow(JPanel panel) {
		JComponent searchBox = findNamedComponent(panel, OfficeChromePanel.SEARCH_BOX_NAME);
		JComponent tabRow = (JComponent) searchBox.getParent();
		assertEquals("projectLibreRibbonTabRow", tabRow.getName(),
			"the search field belongs at the right side of the ribbon tab row");
		if (searchBox.isVisible()) {
			assertTrue(searchBox.getWidth() <= searchBox.getMaximumSize().width,
				"the search field must not stretch beyond its intended Office-style width");
			Component tabs = tabRow.getComponent(0);
			assertTrue(searchBox.getX() >= tabs.getX() + tabs.getWidth(),
				"the search field must remain to the right of the ribbon tabs");
		}
	}

	private static void assertSearchBoxHasNoOutline(BufferedImage image, JPanel panel, JComponent searchBox) {
		java.awt.Point topCenter = javax.swing.SwingUtilities.convertPoint(searchBox,
			searchBox.getWidth() / 2, 0, panel);
		java.awt.Point bottomCenter = javax.swing.SwingUtilities.convertPoint(searchBox,
			searchBox.getWidth() / 2, searchBox.getHeight() - 1, panel);
		int chromeRgb = MicroProjectTheme.tokens().ribbonChromeBackground().getRGB();
		assertEquals(chromeRgb, image.getRGB(topCenter.x, topCenter.y),
			"the search accessory should not draw an Office-absent rounded outline");
		assertEquals(chromeRgb, image.getRGB(bottomCenter.x, bottomCenter.y),
			"the search accessory should remain visually open against the tab strip");
	}

	private static void assertRibbonColorsArePresentInRenderedPixels(BufferedImage image, JPanel panel) {
		assertEquals(MicroProjectTheme.tokens().ribbonChromeBackground().getRGB(), image.getRGB(512, 10),
			"the rendered Office title/tab chrome must use the reference gray");
		JComponent surface = findNamedComponent(panel, "projectLibreRibbonSurface");
		assertTrue(surface.isOpaque(), "the ribbon command surface must paint its own background");
		assertEquals(MicroProjectTheme.tokens().ribbonSurfaceBackground(), surface.getBackground(),
			"the expanded ribbon command surface must use the white theme token");
		int white = MicroProjectTheme.tokens().ribbonSurfaceBackground().getRGB();
		int whitePixels = 0;
		for (int y = 40; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				if (image.getRGB(x, y) == white) whitePixels++;
			}
		}
		assertTrue(whitePixels > 10_000,
			"the expanded ribbon command area must render a substantial white surface");
	}

	private static JComponent findNamedComponent(JPanel panel, String name) {
		return UiComponentWalker.flatten(panel).stream()
			.filter(JComponent.class::isInstance)
			.map(JComponent.class::cast)
			.filter(component -> name.equals(component.getName()))
			.findFirst()
			.orElseThrow(() -> new AssertionError("component not found: " + name));
	}

	private static void assertJapaneseStatusDateCaption(JPanel panel) {
		var statusDateLabels = UiComponentWalker.flatten(panel).stream()
			.filter(AbstractButton.class::isInstance)
			.map(AbstractButton.class::cast)
			.filter(button -> button.getText() != null && button.getText().contains("StatusDate"))
			.toList();
		assertTrue(statusDateLabels.isEmpty(),
			() -> "Japanese ribbon fell back to a raw action name: "
				+ statusDateLabels.stream().map(AbstractButton::getText).toList());
	}

	private static void assertCollapsedGroupsUseIconTriggers(JPanel panel) {
		UiComponentWalker.flatten(panel).stream()
			.filter(AbstractButton.class::isInstance)
			.map(AbstractButton.class::cast)
			.filter(button -> Boolean.TRUE.equals(
				button.getClientProperty(ModernRibbonPanel.BAND_PROXY_PROPERTY)))
			.forEach(button -> {
				assertEquals("▾", button.getText(), "collapsed group button should not repeat a truncating group name");
				assertTrue(button.getToolTipText() != null && !button.getToolTipText().isBlank(),
					"collapsed group must retain its full name as a tooltip");
			});
	}

	private static void assertNoVisibleCollapsedTabLauncher(JPanel panel) {
		boolean launcherVisible = UiComponentWalker.flatten(panel).stream()
			.filter(AbstractButton.class::isInstance)
			.map(AbstractButton.class::cast)
			.anyMatch(button -> Boolean.TRUE.equals(
				button.getClientProperty(ModernRibbonPanel.COLLAPSED_TAB_LAUNCHER_PROPERTY))
				&& isVisibleInHierarchy(button));
		assertTrue(!launcherVisible,
			"a standard ribbon tab must not hide its entire command surface behind one launcher");
	}

	private static void assertResponsiveGroupsRemainReachable(JPanel panel, String tabId, int width) {
		if (width > 320 || !"TaskRibbonTask".equals(tabId)) return;
		long proxies = UiComponentWalker.flatten(panel).stream()
			.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
			.filter(button -> Boolean.TRUE.equals(button.getClientProperty(ModernRibbonPanel.BAND_PROXY_PROPERTY)))
			.count();
		assertTrue(proxies >= 2,
			"narrow ribbon must expose multiple group proxies rather than a tab-level launcher");
		if (width <= 320) {
			boolean canScroll = UiComponentWalker.flatten(panel).stream()
				.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
				.anyMatch(button -> Boolean.TRUE.equals(button.getClientProperty(ModernRibbonPanel.SCROLL_NEXT_PROPERTY)));
			assertTrue(canScroll, "group proxies wider than the client area must have a horizontal scroll control");
		}
	}

	private static boolean isVisibleInHierarchy(java.awt.Component component) {
		for (java.awt.Component current = component; current != null; current = current.getParent()) {
			if (!current.isVisible()) {
				return false;
			}
		}
		return true;
	}

	private static AbstractButton findButton(java.awt.Component root, String text) {
		return UiComponentWalker.flatten(root).stream()
			.filter(AbstractButton.class::isInstance)
			.map(AbstractButton.class::cast)
			.filter(button -> text.equals(button.getText()))
			.findFirst()
			.orElseThrow(() -> new AssertionError("Button not found: " + text));
	}

	private static void layoutRecursively(java.awt.Component component) {
		component.doLayout();
		if (component instanceof java.awt.Container container) {
			java.awt.Component[] children = container.getComponents();
			for (java.awt.Component child : children) {
				layoutRecursively(child);
			}
		}
	}

	private static boolean hasVisibleInk(BufferedImage image) {
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				int alpha = (image.getRGB(x, y) >>> 24) & 0xFF;
				if (alpha != 0) {
					return true;
				}
			}
		}
		return false;
	}
}
