package com.projectlibre.ui.theme;

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
	void ribbonChromeMatchesTheWindowsTitleBarGray() {
		assertEquals(new Color(0xF2F2F2), ProjectLibreThemeTokens.light().ribbonChromeBackground());
	}
}
