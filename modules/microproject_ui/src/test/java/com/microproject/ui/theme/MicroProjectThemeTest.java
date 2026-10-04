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

class MicroProjectThemeTest {
	@Test
	void installLightPublishesSemanticColorsAndSpacing() {
		MicroProjectTheme.installLight();

		assertEquals(MicroProjectTheme.tokens().ribbonChromeBackground(), UIManager.getColor("MicroProject.ribbonChromeBackground"));
		assertEquals(MicroProjectTheme.tokens().ribbonSurfaceBackground(), UIManager.getColor("MicroProject.ribbonSurfaceBackground"));
		assertEquals(MicroProjectTheme.tokens().ribbonChromeBackground(), UIManager.getColor("TitlePane.background"));
		assertEquals(new Color(0x0F773D), UIManager.getColor("MicroProject.ribbonAccentColor"));
		assertEquals(new Color(0x0F773D), UIManager.getColor("MicroProject.ribbonTabUnderlineColor"));
		assertEquals(MicroProjectTheme.tokens().tableForeground(), UIManager.getColor("TitlePane.foreground"));
		assertEquals(MicroProjectTheme.tokens().headerBackground(), UIManager.getColor("TableHeader.background"));
		assertEquals(MicroProjectTheme.tokens().spreadsheetHeaderSelectedBackground(), UIManager.getColor("MicroProject.spreadsheetHeaderSelectedBackground"));
		assertEquals(MicroProjectTheme.tokens().spreadsheetGridColor(), UIManager.getColor("MicroProject.spreadsheetGridColor"));
		assertEquals(MicroProjectTheme.tokens().tableSelectionBackground(), UIManager.getColor("Table.selectionBackground"));
		assertEquals(MicroProjectTheme.tokens().tableSelectionForeground(), UIManager.getColor("Table.selectionForeground"));
		assertEquals(MicroProjectTheme.tokens().accentColor(), UIManager.getColor("Component.focusColor"));
		assertTrue(((Integer) UIManager.get("MicroProject.dialogButtonHeight")).intValue() >= 30);
		assertEquals(MicroProjectTheme.tokens().ribbonChromeHeight(), ((Integer) UIManager.get("MicroProject.ribbonChromeHeight")).intValue());
		assertEquals(MicroProjectTheme.tokens().ribbonSearchPreferredWidth(), ((Integer) UIManager.get("MicroProject.ribbonSearchPreferredWidth")).intValue());
		assertEquals(MicroProjectTheme.tokens().ribbonLargeButtonHeight(), ((Integer) UIManager.get("MicroProject.ribbonLargeButtonHeight")).intValue());
		assertEquals(48, MicroProjectTheme.tokens().ribbonChromeHeight(), "current Office ribbon title row height");
	}

	@Test
	void installDarkPublishesDarkSurfacesAndReadableForegrounds() {
		try {
			MicroProjectTheme.installDark();
			assertEquals(MicroProjectThemeTokens.dark().workspaceBackground(), UIManager.getColor("MicroProject.workspaceBackground"));
			assertEquals(MicroProjectThemeTokens.dark().dialogSurfaceBackground(), UIManager.getColor("MicroProject.dialogSurfaceBackground"));
			assertEquals(MicroProjectThemeTokens.dark().tableForeground(), UIManager.getColor("Table.foreground"));
			assertEquals(MicroProjectThemeTokens.dark().spreadsheetGridColor(), UIManager.getColor("Table.gridColor"));
			assertEquals(new Color(0x353B44), UIManager.getColor("MicroProject.ribbonTabHoverColor"));
		} finally {
			MicroProjectTheme.installLight();
		}
	}

	@Test
	void lightTokensKeepHeaderAndSelectionPaletteAligned() {
		assertEquals(MicroProjectTheme.tokens().headerBackground(), MicroProjectTheme.tokens().spreadsheetHeaderBackground());
		assertEquals(MicroProjectTheme.tokens().tableGridColor(), MicroProjectTheme.tokens().spreadsheetGridColor());
		assertEquals(MicroProjectTheme.tokens().tableSelectionBackground(), MicroProjectTheme.tokens().spreadsheetRangeSelectionBackground());
	}

	@Test
	void taskTableUsesTheMicrosoftProjectPalette() {
		MicroProjectThemeTokens tokens = MicroProjectThemeTokens.light();

		assertEquals(new Color(0xF2F2F2), tokens.spreadsheetHeaderBackground());
		assertEquals(new Color(0xD9EAF7), tokens.spreadsheetHeaderSelectedBackground());
		assertEquals(new Color(0xCCE4F7), tokens.spreadsheetRangeSelectionBackground());
		assertEquals(new Color(0x0078D7), tokens.spreadsheetActiveCellBorder());
		assertEquals(new Color(0xD9D9D9), tokens.spreadsheetGridColor());
	}

	@Test
	void ribbonUsesTheReferenceOfficeSurfaceColor() {
		assertEquals(new Color(0xF3F2F1), MicroProjectThemeTokens.light().ribbonChromeBackground());
		assertEquals(Color.WHITE, MicroProjectThemeTokens.light().ribbonSurfaceBackground());
	}

	@Test
	void darkTokensKeepReadableTextAndLayeredSurfaces() {
		MicroProjectThemeTokens tokens = MicroProjectThemeTokens.dark();

		assertTrue(tokens.tableForeground().getRed() > tokens.tableBackground().getRed());
		assertTrue(tokens.dialogSurfaceBackground().getRed() > tokens.dialogBackground().getRed());
		assertTrue(tokens.borderColor().getRed() > tokens.tableBackground().getRed());
	}
}
