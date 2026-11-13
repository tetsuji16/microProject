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
package com.microproject.pm.graphic.spreadsheet.time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;

import javax.swing.JLabel;

import org.junit.jupiter.api.Test;

import com.microproject.ui.theme.MicroProjectTheme;
import com.microproject.util.ColorLuminance;
import com.microproject.util.FlatUiSupport;

/** Issue #1125: a time-phased cell that cannot be edited follows the theme. */
class TimeSpreadSheetReadOnlyCellTest {
	private static final double MINIMUM_TEXT_CONTRAST = 4.5;

	@Test
	void readOnlyCellStaysReadableInTheLightTheme() {
		MicroProjectTheme.installLight();

		assertReadOnlyCellIsReadableAndDistinct();
	}

	@Test
	void readOnlyCellStaysReadableInTheDarkTheme() {
		try {
			MicroProjectTheme.installDark();

			assertReadOnlyCellIsReadableAndDistinct();
			assertTrue(ColorLuminance.isDark(FlatUiSupport.spreadsheetReadOnlyBackground()),
				"the dark theme must not paint a light read-only cell");
		} finally {
			MicroProjectTheme.installLight();
		}
	}

	@Test
	void selectedAndEditableCellsKeepTheRendererColours() {
		MicroProjectTheme.installLight();
		Color rendererBackground = new Color(0x123456);

		JLabel selected = cell(rendererBackground);
		TimeSpreadSheet.applyReadOnlyBackground(selected, false, true);
		assertEquals(rendererBackground, selected.getBackground());

		JLabel editable = cell(rendererBackground);
		TimeSpreadSheet.applyReadOnlyBackground(editable, true, false);
		assertEquals(rendererBackground, editable.getBackground());
	}

	private static void assertReadOnlyCellIsReadableAndDistinct() {
		JLabel cell = cell(FlatUiSupport.spreadsheetBodyBackground());
		cell.setForeground(FlatUiSupport.tableForeground());

		TimeSpreadSheet.applyReadOnlyBackground(cell, false, false);

		assertEquals(FlatUiSupport.spreadsheetReadOnlyBackground(), cell.getBackground());
		assertNotEquals(FlatUiSupport.spreadsheetBodyBackground(), cell.getBackground(),
			"a read-only cell must differ from an editable cell");
		double contrast = contrast(cell.getForeground(), cell.getBackground());
		assertTrue(contrast >= MINIMUM_TEXT_CONTRAST, "text contrast on a read-only cell was " + contrast);
	}

	private static JLabel cell(Color background) {
		JLabel cell = new JLabel("8h");
		cell.setOpaque(true);
		cell.setBackground(background);
		return cell;
	}

	private static double contrast(Color first, Color second) {
		double a = ColorLuminance.relativeLuminance(first);
		double b = ColorLuminance.relativeLuminance(second);
		return (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05);
	}
}
