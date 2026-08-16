package com.projectlibre.ui.shell;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import javax.swing.AbstractButton;
import javax.imageio.ImageIO;
import javax.swing.JPanel;

import org.junit.jupiter.api.Test;

import com.projectlibre.ui.ribbon.SwingRibbonFactory;
import com.projectlibre.ui.theme.ProjectLibreTheme;
import com.microproject.menu.ExtToolBarFactory;
import com.microproject.menu.MenuActionMapSupport;
import com.microproject.menu.MenuManager;
import com.microproject.menu.testsupport.MenuDefinitionSupport;
import com.microproject.menu.testsupport.UiComponentWalker;

class OfficeChromePanelVisualSmokeTest {
	@Test
	void rendersOfficeChromeRibbonSnapshot() throws IOException {
		ProjectLibreTheme.installLight();
		MenuManager menuManager = MenuManager.getInstance(MenuActionMapSupport.noopActionMap());
		ExtToolBarFactory buttonFactory = new ExtToolBarFactory(
			MenuActionMapSupport.noopActionMap(),
			MenuDefinitionSupport.ribbonBundles(Locale.JAPAN));
		SwingRibbonFactory ribbonFactory = new SwingRibbonFactory(buttonFactory, MenuDefinitionSupport.ribbonBundles(Locale.JAPAN));
		JPanel ribbonPanel = ribbonFactory.createPanel(MenuManager.STANDARD_RIBBON, () -> {});
		OfficeChromePanel panel = new OfficeChromePanel(menuManager, ribbonPanel, () -> {});
		panel.setSize(1024, 160);
		panel.doLayout();
		layoutRecursively(panel);

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

	@Test
	void rendersEveryTabAtOfficeReferenceWidthsInEnglishAndJapanese() throws IOException {
		for (Locale locale : List.of(Locale.ROOT, Locale.JAPAN)) {
			for (int width : List.of(720, 760, 1024, 1200, 1440)) {
				renderTabContactSheet(locale, width);
			}
		}
	}

	private static void renderTabContactSheet(Locale locale, int width) throws IOException {
		MenuManager menuManager = MenuManager.getInstance(MenuActionMapSupport.noopActionMap());
		ExtToolBarFactory buttonFactory = new ExtToolBarFactory(
			MenuActionMapSupport.noopActionMap(),
			MenuDefinitionSupport.ribbonBundles(locale));
		SwingRibbonFactory ribbonFactory = new SwingRibbonFactory(buttonFactory, MenuDefinitionSupport.ribbonBundles(locale));
		var model = ribbonFactory.createModel(MenuManager.STANDARD_RIBBON);
		JPanel ribbonPanel = ribbonFactory.createPanel(model, () -> {});
		OfficeChromePanel panel = new OfficeChromePanel(menuManager, ribbonPanel, () -> {});
		int rowHeight = 160;
		BufferedImage sheet = new BufferedImage(width, rowHeight * model.getTabs().size(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D sheetGraphics = sheet.createGraphics();
		try {
			for (int index = 0; index < model.getTabs().size(); index++) {
				var tab = model.getTabs().get(index);
				findButton(panel, tab.getTitle()).doClick();
				panel.setSize(width, rowHeight);
				panel.doLayout();
				layoutRecursively(panel);
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
			for (java.awt.Component child : container.getComponents()) {
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
