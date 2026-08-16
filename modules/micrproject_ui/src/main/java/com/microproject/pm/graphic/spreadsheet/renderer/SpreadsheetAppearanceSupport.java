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
package com.microproject.pm.graphic.spreadsheet.renderer;

import java.awt.Color;

import com.microproject.util.FlatUiSupport;

final class SpreadsheetAppearanceSupport {
	private static final float BODY_BLEND = 0.82f;
	private static final float DARK_BODY_BLEND = 0.94f;

	private SpreadsheetAppearanceSupport() {
	}

	static Color resolveBodyBackground(Color background, int row) {
		if (background == null) {
			return FlatUiSupport.spreadsheetAlternateRowBackground(row);
		}
		return soften(background);
	}

	static Color resolveSelectionBackground(Color background) {
		return FlatUiSupport.spreadsheetRangeSelectionBackground();
	}

	static Color resolveForeground(Color foreground) {
		return foreground == null ? FlatUiSupport.tableForeground() : foreground;
	}

	static Color soften(Color color) {
		if (color == null) {
			return FlatUiSupport.spreadsheetBodyBackground();
		}
		float blend = isDark(color) ? DARK_BODY_BLEND : BODY_BLEND;
		return FlatUiSupport.blend(color, FlatUiSupport.spreadsheetBodyBackground(), blend);
	}

	private static boolean isDark(Color color) {
		if (color == null) {
			return false;
		}
		float luminance = (0.2126f * color.getRed() + 0.7152f * color.getGreen() + 0.0722f * color.getBlue()) / 255f;
		return luminance < 0.45f;
	}
}
