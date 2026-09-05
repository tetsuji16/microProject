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

import javax.swing.BorderFactory;
import javax.swing.UIManager;
import javax.swing.border.Border;

/**
 * Installs microProject light-theme defaults on top of FlatLaf.
 */
public final class MicroProjectTheme {
	private static final MicroProjectThemeTokens LIGHT_TOKENS = MicroProjectThemeTokens.light();

	private MicroProjectTheme() {
	}

	public static void installLight() {
		MicroProjectThemeTokens tokens = LIGHT_TOKENS;
		Border dialogBorder = BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(tokens.borderColor()),
			BorderFactory.createEmptyBorder(tokens.contentSpacing(), tokens.sectionSpacing(), tokens.contentSpacing(), tokens.sectionSpacing()));
		UIManager.put("MicroProject.ribbonChromeBackground", tokens.ribbonChromeBackground());
		UIManager.put("MicroProject.ribbonSurfaceBackground", tokens.ribbonSurfaceBackground());
		// The ribbon intentionally uses the Office blue independently from the
		// application accent (which remains the microProject green).
		UIManager.put("MicroProject.ribbonAccentColor", new java.awt.Color(0x0F6CBD));
		UIManager.put("MicroProject.workspaceBackground", tokens.workspaceBackground());
		UIManager.put("MicroProject.dialogBackground", tokens.dialogBackground());
		UIManager.put("MicroProject.dialogSurfaceBackground", tokens.dialogSurfaceBackground());
		UIManager.put("MicroProject.borderColor", tokens.borderColor());
		UIManager.put("MicroProject.separatorColor", tokens.separatorColor());
		UIManager.put("MicroProject.accentColor", tokens.accentColor());
		UIManager.put("MicroProject.spreadsheetBodyBackground", tokens.spreadsheetBodyBackground());
		UIManager.put("MicroProject.spreadsheetReadOnlyForeground", tokens.spreadsheetReadOnlyForeground());
		UIManager.put("MicroProject.spreadsheetHeaderBackground", tokens.spreadsheetHeaderBackground());
		UIManager.put("MicroProject.spreadsheetHeaderSelectedBackground", tokens.spreadsheetHeaderSelectedBackground());
		UIManager.put("MicroProject.spreadsheetRangeSelectionBackground", tokens.spreadsheetRangeSelectionBackground());
		UIManager.put("MicroProject.spreadsheetActiveCellBorder", tokens.spreadsheetActiveCellBorder());
		UIManager.put("MicroProject.spreadsheetGridColor", tokens.spreadsheetGridColor());
		UIManager.put("MicroProject.dialogButtonHeight", Integer.valueOf(tokens.dialogButtonHeight()));
		UIManager.put("MicroProject.dialogCornerRadius", Integer.valueOf(tokens.dialogCornerRadius()));
		UIManager.put("MicroProject.contentSpacing", Integer.valueOf(tokens.contentSpacing()));
		UIManager.put("MicroProject.sectionSpacing", Integer.valueOf(tokens.sectionSpacing()));
		UIManager.put("MicroProject.ribbonChromeHeight", Integer.valueOf(tokens.ribbonChromeHeight()));
		UIManager.put("MicroProject.ribbonChromeVerticalInset", Integer.valueOf(tokens.ribbonChromeVerticalInset()));
		UIManager.put("MicroProject.ribbonHorizontalInset", Integer.valueOf(tokens.ribbonHorizontalInset()));
		UIManager.put("MicroProject.ribbonTabHeight", Integer.valueOf(tokens.ribbonTabHeight()));
		UIManager.put("MicroProject.ribbonTabHorizontalPadding", Integer.valueOf(tokens.ribbonTabHorizontalPadding()));
		UIManager.put("MicroProject.ribbonTabVerticalPadding", Integer.valueOf(tokens.ribbonTabVerticalPadding()));
		UIManager.put("MicroProject.ribbonSurfaceHeight", Integer.valueOf(tokens.ribbonSurfaceHeight()));
		UIManager.put("MicroProject.ribbonBandVerticalInset", Integer.valueOf(tokens.ribbonBandVerticalInset()));
		UIManager.put("MicroProject.ribbonButtonVerticalInset", Integer.valueOf(tokens.ribbonButtonVerticalInset()));
		UIManager.put("MicroProject.ribbonSearchHeight", Integer.valueOf(tokens.ribbonSearchHeight()));
		UIManager.put("MicroProject.ribbonSearchPreferredWidth", Integer.valueOf(tokens.ribbonSearchPreferredWidth()));
		UIManager.put("MicroProject.ribbonSearchMaxWidth", Integer.valueOf(tokens.ribbonSearchMaxWidth()));
		UIManager.put("MicroProject.ribbonCornerRadius", Integer.valueOf(tokens.ribbonCornerRadius()));
		UIManager.put("MicroProject.ribbonButtonArc", Integer.valueOf(tokens.ribbonButtonArc()));
		UIManager.put("MicroProject.ribbonQuickAccessButtonSize", Integer.valueOf(tokens.ribbonQuickAccessButtonSize()));
		UIManager.put("MicroProject.ribbonLargeButtonHeight", Integer.valueOf(tokens.ribbonLargeButtonHeight()));
		UIManager.put("MicroProject.ribbonLargeButtonMinWidth", Integer.valueOf(tokens.ribbonLargeButtonMinWidth()));
		UIManager.put("MicroProject.ribbonInlineButtonHeight", Integer.valueOf(tokens.ribbonInlineButtonHeight()));
		UIManager.put("MicroProject.ribbonInlineButtonMediumMinWidth", Integer.valueOf(tokens.ribbonInlineButtonMediumMinWidth()));
		UIManager.put("MicroProject.ribbonInlineButtonSmallMinWidth", Integer.valueOf(tokens.ribbonInlineButtonSmallMinWidth()));
		UIManager.put("MicroProject.ribbonBandTitleHeight", Integer.valueOf(tokens.ribbonBandTitleHeight()));
		UIManager.put("TitlePane.background", tokens.ribbonChromeBackground());
		UIManager.put("TitlePane.inactiveBackground", tokens.ribbonChromeBackground());
		UIManager.put("TitlePane.foreground", tokens.tableForeground());
		UIManager.put("TitlePane.inactiveForeground", tokens.tableForeground());
		UIManager.put("TitlePane.unifiedBackground", Boolean.TRUE);
		UIManager.put("MenuBar.background", tokens.ribbonChromeBackground());
		UIManager.put("MenuBar.borderColor", tokens.ribbonChromeBackground());
		UIManager.put("Menu.background", tokens.ribbonChromeBackground());
		UIManager.put("MenuItem.background", tokens.ribbonChromeBackground());
		UIManager.put("Panel.background", tokens.panelBackground());
		UIManager.put("OptionPane.background", tokens.dialogBackground());
		UIManager.put("OptionPane.messageArea.background", tokens.dialogBackground());
		UIManager.put("OptionPane.buttonArea.background", tokens.dialogBackground());
		UIManager.put("OptionPane.border", dialogBorder);
		UIManager.put("Table.background", tokens.spreadsheetBodyBackground());
		UIManager.put("Table.foreground", tokens.tableForeground());
		UIManager.put("Table.selectionBackground", tokens.spreadsheetRangeSelectionBackground());
		UIManager.put("Table.selectionForeground", tokens.tableSelectionForeground());
		UIManager.put("Table.alternateRowColor", tokens.spreadsheetBodyBackground());
		UIManager.put("Table.gridColor", tokens.spreadsheetGridColor());
		UIManager.put("TableHeader.background", tokens.headerBackground());
		UIManager.put("TableHeader.foreground", tokens.headerForeground());
		UIManager.put("Component.borderColor", tokens.borderColor());
		UIManager.put("Separator.foreground", tokens.separatorColor());
		UIManager.put("Component.focusColor", tokens.accentColor());
		UIManager.put("ProgressBar.foreground", tokens.accentColor());
		UIManager.put("Actions.Blue", tokens.accentColor());
		UIManager.put("Actions.Red", tokens.errorColor());
		UIManager.put("Component.errorFocusColor", tokens.errorColor());
	}

	public static MicroProjectThemeTokens tokens() {
		return LIGHT_TOKENS;
	}
}
