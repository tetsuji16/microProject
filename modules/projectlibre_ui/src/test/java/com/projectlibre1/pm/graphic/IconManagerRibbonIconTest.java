package com.projectlibre1.pm.graphic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.pushingpixels.flamingo.api.common.AsynchronousLoading;

import com.projectlibre1.menu.testsupport.RibbonInventory;

class IconManagerRibbonIconTest {
	@Test
	void officeRibbonIconsKeepColourAndUseTranslucentDisabledVariants() {
		var normal = IconManager.getRibbonIcon("ribbon.open", 32, 32);
		var disabled = IconManager.getRibbonIconDisabled("ribbon.open", 32, 32);
		assertNotNull(normal);
		assertNotNull(disabled);
		BufferedImage normalCanvas = paint(normal, 32, 32);
		BufferedImage disabledCanvas = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
		var disabledGraphics = disabledCanvas.createGraphics();
		try {
			disabled.paintIcon(null, disabledGraphics, 0, 0);
		} finally {
			disabledGraphics.dispose();
		}
		assertTrue(visibleColorCount(normalCanvas) >= 3, "enabled SVG should retain semantic colours");
		assertTrue(maxAlpha(disabledCanvas) <= 100, "disabled SVG should use approximately 38% opacity");
	}
	static Set<String> standardRibbonKeys() {
		Set<String> keys = new LinkedHashSet<>();
		for (var spec : RibbonInventory.standardRibbon().buttons().values()) {
			if (spec.requiresIcon()) {
				keys.add(spec.iconKey());
			}
		}
		return keys;
	}

	@ParameterizedTest
	@ValueSource(strings = {"ribbon.save", "ribbon.open", "ribbon.print", "ribbon.pdf", "ribbon.insertRecurring", "application.icon", "logo.ProjectLibre.ribbon"})
	void ribbonIconsLoadSynchronouslyAndPaintVisiblePixels(String key) {
		var icon = IconManager.getRibbonIcon(key, 32, 32);
		assertNotNull(icon, () -> key + " did not resolve to a ribbon icon");
		assertFalse(icon instanceof AsynchronousLoading, () -> key + " should not use asynchronous icon loading");

		icon.setDimension(new Dimension(32, 32));
		BufferedImage canvas = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
		var g2 = canvas.createGraphics();
		try {
			icon.paintIcon(null, g2, 0, 0);
		} finally {
			g2.dispose();
		}

		assertTrue(hasVisiblePixel(canvas), () -> key + " rendered only transparent pixels");
	}

	@ParameterizedTest
	@MethodSource("standardRibbonKeys")
	void standardRibbonIconsKeepRequestedDimensions(String key) {
		var icon = IconManager.getRibbonIcon(key, 20, 20);
		assertNotNull(icon);
		assertEquals(20, icon.getIconWidth());
		assertEquals(20, icon.getIconHeight());
	}

	@ParameterizedTest
	@MethodSource("standardRibbonKeys")
	void standardRibbonIconsLoadSynchronouslyAndPaintVisiblePixels(String key) {
		var icon = IconManager.getRibbonIcon(key, 32, 32);
		assertNotNull(icon, () -> key + " did not resolve to a ribbon icon");
		assertFalse(icon instanceof AsynchronousLoading, () -> key + " should not use asynchronous icon loading");

		icon.setDimension(new Dimension(32, 32));
		BufferedImage canvas = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
		var g2 = canvas.createGraphics();
		try {
			icon.paintIcon(null, g2, 0, 0);
		} finally {
			g2.dispose();
		}

		assertTrue(hasVisiblePixel(canvas), () -> key + " rendered only transparent pixels");
	}

	private static boolean hasVisiblePixel(BufferedImage image) {
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				if (((image.getRGB(x, y) >>> 24) & 0xFF) != 0) {
					return true;
				}
			}
		}
		return false;
	}

	private static BufferedImage paint(javax.swing.Icon icon, int width, int height) {
		BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		var graphics = canvas.createGraphics();
		try {
			icon.paintIcon(null, graphics, 0, 0);
		} finally {
			graphics.dispose();
		}
		return canvas;
	}

	private static int visibleColorCount(BufferedImage image) {
		Set<Integer> colors = new LinkedHashSet<>();
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				int argb = image.getRGB(x, y);
				if (((argb >>> 24) & 0xff) > 32) colors.add(argb & 0x00ffffff);
			}
		}
		return colors.size();
	}

	private static int maxAlpha(BufferedImage image) {
		int max = 0;
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				max = Math.max(max, (image.getRGB(x, y) >>> 24) & 0xff);
			}
		}
		return max;
	}
}
