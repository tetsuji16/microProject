package com.projectlibre.ui.shell;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import org.junit.jupiter.api.Test;

import com.projectlibre.ui.ribbon.SwingRibbonFactory;
import com.projectlibre.ui.theme.ProjectLibreTheme;
import com.projectlibre1.menu.ExtToolBarFactory;
import com.projectlibre1.menu.MenuActionMapSupport;
import com.projectlibre1.menu.MenuManager;
import com.projectlibre1.menu.testsupport.MenuDefinitionSupport;

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
