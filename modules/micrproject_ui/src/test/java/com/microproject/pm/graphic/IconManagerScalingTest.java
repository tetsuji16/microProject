package com.microproject.pm.graphic;

import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IconManagerScalingTest {

	@Test
	void scaleToCanvasFitsAndCentersOnCanvas() {
		BufferedImage source = new BufferedImage(32, 16, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 32; x++) {
				source.setRGB(x, y, 0xFFFF0000);
			}
		}
		BufferedImage canvas = IconManager.scaleToCanvas(source, 16, 16);
		assertEquals(16, canvas.getWidth(), "canvas width");
		assertEquals(16, canvas.getHeight(), "canvas height");
		// 32x16 scaled to fit 16x16 -> 16x8, centered vertically (rows 4..11 filled)
		assertEquals(0xFFFF0000, canvas.getRGB(8, 8), "centered content pixel");
		assertEquals(0x00000000, canvas.getRGB(8, 2), "top padding must stay transparent");
		assertEquals(0x00000000, canvas.getRGB(8, 14), "bottom padding must stay transparent");
	}

	@Test
	void scaleToCanvasGuardsInvalidInput() {
		BufferedImage source = new BufferedImage(32, 16, BufferedImage.TYPE_INT_ARGB);
		assertEquals(null, IconManager.scaleToCanvas(null, 16, 16));
		assertEquals(null, IconManager.scaleToCanvas(source, 0, 16));
		assertEquals(null, IconManager.scaleToCanvas(source, 16, -1));
	}
}
