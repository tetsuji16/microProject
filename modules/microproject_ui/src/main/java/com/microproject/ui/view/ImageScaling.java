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

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * Scales raster images with the JDK only.
 *
 * <p>Shrinking is done in halves while the image is more than twice the target size and then in one
 * step to the exact target, which keeps detail that a single large reduction loses. Enlarging is a
 * single step.</p>
 */
public final class ImageScaling {
	private ImageScaling() {
	}

	/**
	 * Returns a new {@link BufferedImage} of exactly {@code width} x {@code height}.
	 *
	 * @throws IllegalArgumentException when the source is missing or a target size is not positive
	 */
	public static BufferedImage resize(BufferedImage source, int width, int height) {
		if (source == null)
			throw new IllegalArgumentException("source image is null");
		if (width < 1 || height < 1)
			throw new IllegalArgumentException("target size must be positive: " + width + "x" + height);
		boolean shrinking = width < source.getWidth() || height < source.getHeight();
		if (!shrinking)
			return draw(source, width, height, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

		BufferedImage current = source;
		int currentWidth = source.getWidth();
		int currentHeight = source.getHeight();
		while (currentWidth > width * 2 || currentHeight > height * 2) {
			int nextWidth = Math.max(width, currentWidth / 2);
			int nextHeight = Math.max(height, currentHeight / 2);
			if (nextWidth == currentWidth && nextHeight == currentHeight)
				break;
			current = draw(current, nextWidth, nextHeight, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			currentWidth = nextWidth;
			currentHeight = nextHeight;
		}
		return draw(current, width, height, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
	}

	private static BufferedImage draw(BufferedImage source, int width, int height, Object interpolation) {
		BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = target.createGraphics();
		try {
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolation);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
					RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
			g.drawImage(source, 0, 0, width, height, null);
		} finally {
			g.dispose();
		}
		return target;
	}
}
