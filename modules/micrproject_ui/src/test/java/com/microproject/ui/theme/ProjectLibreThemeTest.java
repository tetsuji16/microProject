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
package com.microproject.ui.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;

import javax.swing.UIManager;

import org.junit.jupiter.api.Test;

class ProjectLibreThemeTest {
	@Test
	void installLightPublishesSemanticColorsAndSpacing() {
		ProjectLibreTheme.installLight();

		assertEquals(ProjectLibreTheme.tokens().ribbonChromeBackground(), UIManager.getColor("ProjectLibre.ribbonChromeBackground"));
		assertEquals(ProjectLibreTheme.tokens().ribbonSurfaceBackground(), UIManager.getColor("ProjectLibre.ribbonSurfaceBackground"));
		assertEquals(ProjectLibreTheme.tokens().headerBackground(), UIManager.getColor("TableHeader.background"));
		assertEquals(ProjectLibreTheme.tokens().spreadsheetHeaderSelectedBackground(), UIManager.getColor("ProjectLibre.spreadsheetHeaderSelectedBackground"));
		assertEquals(ProjectLibreTheme.tokens().spreadsheetGridColor(), UIManager.getColor("ProjectLibre.spreadsheetGridColor"));
		assertEquals(ProjectLibreTheme.tokens().tableSelectionBackground(), UIManager.getColor("Table.selectionBackground"));
		assertEquals(ProjectLibreTheme.tokens().tableSelectionForeground(), UIManager.getColor("Table.selectionForeground"));
		assertEquals(ProjectLibreTheme.tokens().accentColor(), UIManager.getColor("Component.focusColor"));
		assertTrue(((Integer) UIManager.get("ProjectLibre.dialogButtonHeight")).intValue() >= 30);
		assertEquals(ProjectLibreTheme.tokens().ribbonChromeHeight(), ((Integer) UIManager.get("ProjectLibre.ribbonChromeHeight")).intValue());
		assertEquals(ProjectLibreTheme.tokens().ribbonSearchPreferredWidth(), ((Integer) UIManager.get("ProjectLibre.ribbonSearchPreferredWidth")).intValue());
		assertEquals(ProjectLibreTheme.tokens().ribbonLargeButtonHeight(), ((Integer) UIManager.get("ProjectLibre.ribbonLargeButtonHeight")).intValue());
	}

	@Test
	void lightTokensKeepHeaderAndSelectionPaletteAligned() {
		assertEquals(ProjectLibreTheme.tokens().headerBackground(), ProjectLibreTheme.tokens().spreadsheetHeaderBackground());
		assertEquals(ProjectLibreTheme.tokens().tableGridColor(), ProjectLibreTheme.tokens().spreadsheetGridColor());
		assertEquals(ProjectLibreTheme.tokens().tableSelectionBackground(), ProjectLibreTheme.tokens().spreadsheetRangeSelectionBackground());
	}

	@Test
	void taskTableUsesTheMicrosoftProjectPalette() {
		ProjectLibreThemeTokens tokens = ProjectLibreThemeTokens.light();

		assertEquals(new Color(0xF2F2F2), tokens.spreadsheetHeaderBackground());
		assertEquals(new Color(0xD9EAF7), tokens.spreadsheetHeaderSelectedBackground());
		assertEquals(new Color(0xCCE4F7), tokens.spreadsheetRangeSelectionBackground());
		assertEquals(new Color(0x0078D7), tokens.spreadsheetActiveCellBorder());
		assertEquals(new Color(0xD9D9D9), tokens.spreadsheetGridColor());
	}

	@Test
	void ribbonChromeMatchesTheWindowsTitleBarGray() {
		assertEquals(new Color(0xF2F2F2), ProjectLibreThemeTokens.light().ribbonChromeBackground());
	}
}
