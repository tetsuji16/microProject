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
package com.microproject.ui.layout;

import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Toolkit;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;

/**
 * Converts dialog units and points to pixels.
 * <p>
 * A horizontal dialog unit is a quarter of the average character width of the
 * dialog font, and a vertical dialog unit is an eighth of its height, measured
 * with the font metrics of the component being laid out. The dialog font is
 * {@code UIManager.getFont("Button.font")}, looked up again when the look and feel
 * changes. These are the rules the dialogs were designed with, so the pixel
 * sizes stay the same as before.
 */
final class DialogUnits {
	private static final String AVERAGE_WIDTH_SAMPLE = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
	private static final int POINTS_PER_INCH = 72;

	/** Fixed base units for tests; {@code null} measures the dialog font. */
	private static volatile double[] testBaseUnits;

	private static LookAndFeel cachedLookAndFeel;
	private static Font cachedDialogFont;
	private static FontMetrics cachedMetrics;
	private static double[] cachedBaseUnits;

	private DialogUnits() {
	}

	static int horizontalToPixels(int dialogUnits, Component component) {
		if (dialogUnits == 0)
			return 0;
		return (int) Math.round(dialogUnits * baseUnits(component)[0] / 4);
	}

	static int verticalToPixels(int dialogUnits, Component component) {
		if (dialogUnits == 0)
			return 0;
		return (int) Math.round(dialogUnits * baseUnits(component)[1] / 8);
	}

	static int pointsToPixels(int points, Component component) {
		if (points == 0)
			return 0;
		Toolkit toolkit = component == null ? null : component.getToolkit();
		int resolution = (toolkit == null ? Toolkit.getDefaultToolkit() : toolkit).getScreenResolution();
		return (int) Math.round(resolution * (double) points / POINTS_PER_INCH);
	}

	/** Uses fixed base units (average character width, character height) until reset with {@code null}. */
	static void setBaseUnitsForTesting(double[] baseUnits) {
		testBaseUnits = baseUnits == null ? null : baseUnits.clone();
	}

	private static synchronized double[] baseUnits(Component component) {
		double[] fixed = testBaseUnits;
		if (fixed != null)
			return fixed;
		Font font = dialogFont();
		FontMetrics metrics = (component == null ? new JPanel(null) : component).getFontMetrics(font);
		if (!metrics.equals(cachedMetrics)) {
			cachedMetrics = metrics;
			cachedBaseUnits = measure(metrics);
		}
		return cachedBaseUnits;
	}

	private static Font dialogFont() {
		LookAndFeel lookAndFeel = UIManager.getLookAndFeel();
		if (lookAndFeel != cachedLookAndFeel || cachedDialogFont == null) {
			cachedLookAndFeel = lookAndFeel;
			Font buttonFont = UIManager.getFont("Button.font");
			cachedDialogFont = buttonFont != null ? buttonFont : new JButton().getFont();
			cachedMetrics = null;
		}
		return cachedDialogFont;
	}

	private static double[] measure(FontMetrics metrics) {
		double averageWidth = (double) metrics.stringWidth(AVERAGE_WIDTH_SAMPLE) / AVERAGE_WIDTH_SAMPLE.length();
		int ascent = metrics.getAscent();
		double height = ascent > 14 ? ascent : ascent + (15 - ascent) / 3;
		return new double[] { averageWidth, height };
	}
}
