/*******************************************************************************
 * MIT License
 *
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
package com.microproject.ui.view;

import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageScalingTest {

	@Test
	void producesTheRequestedSizeAndType() {
		assertResized(64, 64, 16, 16);
		assertResized(16, 16, 8, 8);
		assertResized(10, 10, 30, 30);
		assertResized(1, 1, 1, 1);
	}

	@Test
	void keepsASolidColourWhenShrinking() {
		BufferedImage source = solid(64, 64, 0xFF336699);
		BufferedImage result = ImageScaling.resize(source, 16, 16);
		for (int y = 0; y < result.getHeight(); y++) {
			for (int x = 0; x < result.getWidth(); x++) {
				assertColourNear(0xFF336699, result.getRGB(x, y), x, y);
			}
		}
	}

	@Test
	void keepsTransparentCornersTransparent() {
		BufferedImage source = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
		for (int y = 16; y < 48; y++) {
			for (int x = 16; x < 48; x++) {
				source.setRGB(x, y, 0xFFFF0000);
			}
		}
		BufferedImage result = ImageScaling.resize(source, 16, 16);
		assertEquals(0, result.getRGB(0, 0) >>> 24, "top left alpha");
		assertEquals(0, result.getRGB(15, 0) >>> 24, "top right alpha");
		assertEquals(0, result.getRGB(0, 15) >>> 24, "bottom left alpha");
		assertEquals(0, result.getRGB(15, 15) >>> 24, "bottom right alpha");
	}

	@Test
	void rejectsInvalidInput() {
		BufferedImage source = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
		assertThrows(IllegalArgumentException.class, () -> ImageScaling.resize(null, 4, 4));
		assertThrows(IllegalArgumentException.class, () -> ImageScaling.resize(source, 0, 4));
		assertThrows(IllegalArgumentException.class, () -> ImageScaling.resize(source, 4, 0));
	}

	private static void assertResized(int sourceWidth, int sourceHeight, int width, int height) {
		BufferedImage result = ImageScaling.resize(solid(sourceWidth, sourceHeight, 0xFF00FF00), width, height);
		assertEquals(width, result.getWidth(), "width");
		assertEquals(height, result.getHeight(), "height");
		assertEquals(BufferedImage.TYPE_INT_ARGB, result.getType(), "type");
	}

	private static BufferedImage solid(int width, int height, int argb) {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				image.setRGB(x, y, argb);
			}
		}
		return image;
	}

	private static void assertColourNear(int expected, int actual, int x, int y) {
		for (int shift = 0; shift < 32; shift += 8) {
			int expectedChannel = (expected >>> shift) & 0xFF;
			int actualChannel = (actual >>> shift) & 0xFF;
			assertTrue(Math.abs(expectedChannel - actualChannel) <= 1,
					"channel " + shift + " at " + x + "," + y + ": expected " + expectedChannel + " but was " + actualChannel);
		}
	}
}
