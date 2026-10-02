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
import org.pushingpixels.flamingo.api.common.AbstractCommandButton;
import org.pushingpixels.flamingo.api.ribbon.JRibbon;
import org.pushingpixels.flamingo.api.ribbon.RibbonTask;
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
		MenuManager menuManager = MenuManager.getInstance(MenuActionMapSupport.noopActionMap());
		ExtToolBarFactory buttonFactory = new ExtToolBarFactory(
			MenuActionMapSupport.noopActionMap(),
			MenuDefinitionSupport.ribbonBundles(Locale.JAPAN));
		SwingRibbonFactory ribbonFactory = new SwingRibbonFactory(
			new com.microproject.menu.MenuRibbonCommandSource(buttonFactory),
			MenuDefinitionSupport.ribbonBundles(Locale.JAPAN));
		JPanel ribbonPanel = ribbonFactory.createPanel(MenuManager.STANDARD_RIBBON, () -> {});
		OfficeChromePanel panel = new OfficeChromePanel(menuManager, ribbonPanel, () -> {});
		panel.setSize(1024, 160);
		panel.doLayout();
		layoutRecursively(panel);
		assertNativeRibbonIsBuilt(panel);

		BufferedImage image = new BufferedImage(1024, 160, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		try {
			panel.printAll(graphics);
		} finally {
			graphics.dispose();
		}

		Path output = Path.of("build", "reports", "ribbon", "office-chrome-ribbon-smoke.png");
		Files.createDirectories(output.getParent());
		ImageIO.write(image, "png", output.toFile());

		assertTrue(Files.exists(output));
		assertTrue(hasVisibleInk(image));
	}

	private static void assertNativeRibbonIsBuilt(JPanel panel) {
		JRibbon ribbon = findRibbon(panel);
		assertTrue(ribbon.getTaskCount() > 0, "Flamingo must own the regular ribbon tabs");
		assertTrue(ribbon.getSelectedTask() != null, "Flamingo must select a task after construction");
		assertTrue(UiComponentWalker.flatten(ribbon).stream().anyMatch(AbstractCommandButton.class::isInstance),
			"Flamingo JRibbon must render native command buttons");
	}

	@Test
	void buildsNativeTabsAtOfficeReferenceWidthsInEnglishAndJapanese() throws IOException {
		for (Locale locale : List.of(Locale.ROOT, Locale.JAPAN)) {
			for (int width : List.of(320, 480, 720, 760, 1024, 1200, 1440)) {
				assertRibbonStructure(locale, width);
			}
		}
	}

	private static void assertRibbonStructure(Locale locale, int width) {
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
		panel.setSize(width, 160);
		panel.doLayout();
		layoutRecursively(panel);
		JRibbon ribbon = findRibbon(ribbonPanel);
		assertTrue(ribbon.getTaskCount() > 0, "native ribbon tabs must remain available at " + width + "px");
		for (var tab : model.getTabs()) {
			if (tab.isContextual()) continue;
			RibbonTask task = findTask(ribbonPanel, tab.getTitle());
			assertTrue(task.getBandCount() > 0, tab.getId() + " must retain its native Flamingo bands");
		}
	}

	private static JRibbon findRibbon(JComponent root) {
		return UiComponentWalker.flatten(root).stream().filter(JRibbon.class::isInstance)
			.map(JRibbon.class::cast).findFirst().orElseThrow(() -> new AssertionError("JRibbon was not created"));
	}

	private static RibbonTask findTask(JComponent root, String title) {
		JRibbon ribbon = findRibbon(root);
		for (int index = 0; index < ribbon.getTaskCount(); index++) {
			RibbonTask task = ribbon.getTask(index);
			if (title.equals(task.getTitle())) return task;
		}
		throw new AssertionError("Ribbon task not found: " + title);
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
