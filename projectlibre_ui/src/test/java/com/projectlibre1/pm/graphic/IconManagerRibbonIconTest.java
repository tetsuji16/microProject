package com.projectlibre1.pm.graphic;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.pushingpixels.flamingo.api.common.AsynchronousLoading;

class IconManagerRibbonIconTest {
	@ParameterizedTest
	@ValueSource(strings = {"ribbon.save", "ribbon.open", "ribbon.print", "ribbon.pdf", "ribbon.insertRecurring", "application.icon"})
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
}
