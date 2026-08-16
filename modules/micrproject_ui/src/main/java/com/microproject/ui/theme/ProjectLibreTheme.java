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
 * Installs ProjectLibre-specific light theme defaults on top of FlatLaf.
 */
public final class ProjectLibreTheme {
	private static final ProjectLibreThemeTokens LIGHT_TOKENS = ProjectLibreThemeTokens.light();

	private ProjectLibreTheme() {
	}

	public static void installLight() {
		ProjectLibreThemeTokens tokens = LIGHT_TOKENS;
		Border dialogBorder = BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(tokens.borderColor()),
			BorderFactory.createEmptyBorder(tokens.contentSpacing(), tokens.sectionSpacing(), tokens.contentSpacing(), tokens.sectionSpacing()));
		UIManager.put("ProjectLibre.ribbonChromeBackground", tokens.ribbonChromeBackground());
		UIManager.put("ProjectLibre.ribbonSurfaceBackground", tokens.ribbonSurfaceBackground());
		// The ribbon intentionally uses the Office blue independently from the
		// application accent (which remains the ProjectLibre green).
		UIManager.put("ProjectLibre.ribbonAccentColor", new java.awt.Color(0x0F6CBD));
		UIManager.put("ProjectLibre.workspaceBackground", tokens.workspaceBackground());
		UIManager.put("ProjectLibre.dialogBackground", tokens.dialogBackground());
		UIManager.put("ProjectLibre.dialogSurfaceBackground", tokens.dialogSurfaceBackground());
		UIManager.put("ProjectLibre.borderColor", tokens.borderColor());
		UIManager.put("ProjectLibre.separatorColor", tokens.separatorColor());
		UIManager.put("ProjectLibre.accentColor", tokens.accentColor());
		UIManager.put("ProjectLibre.spreadsheetBodyBackground", tokens.spreadsheetBodyBackground());
		UIManager.put("ProjectLibre.spreadsheetReadOnlyForeground", tokens.spreadsheetReadOnlyForeground());
		UIManager.put("ProjectLibre.spreadsheetHeaderBackground", tokens.spreadsheetHeaderBackground());
		UIManager.put("ProjectLibre.spreadsheetHeaderSelectedBackground", tokens.spreadsheetHeaderSelectedBackground());
		UIManager.put("ProjectLibre.spreadsheetRangeSelectionBackground", tokens.spreadsheetRangeSelectionBackground());
		UIManager.put("ProjectLibre.spreadsheetActiveCellBorder", tokens.spreadsheetActiveCellBorder());
		UIManager.put("ProjectLibre.spreadsheetGridColor", tokens.spreadsheetGridColor());
		UIManager.put("ProjectLibre.dialogButtonHeight", Integer.valueOf(tokens.dialogButtonHeight()));
		UIManager.put("ProjectLibre.dialogCornerRadius", Integer.valueOf(tokens.dialogCornerRadius()));
		UIManager.put("ProjectLibre.contentSpacing", Integer.valueOf(tokens.contentSpacing()));
		UIManager.put("ProjectLibre.sectionSpacing", Integer.valueOf(tokens.sectionSpacing()));
		UIManager.put("ProjectLibre.ribbonChromeHeight", Integer.valueOf(tokens.ribbonChromeHeight()));
		UIManager.put("ProjectLibre.ribbonChromeVerticalInset", Integer.valueOf(tokens.ribbonChromeVerticalInset()));
		UIManager.put("ProjectLibre.ribbonHorizontalInset", Integer.valueOf(tokens.ribbonHorizontalInset()));
		UIManager.put("ProjectLibre.ribbonTabHeight", Integer.valueOf(tokens.ribbonTabHeight()));
		UIManager.put("ProjectLibre.ribbonTabHorizontalPadding", Integer.valueOf(tokens.ribbonTabHorizontalPadding()));
		UIManager.put("ProjectLibre.ribbonTabVerticalPadding", Integer.valueOf(tokens.ribbonTabVerticalPadding()));
		UIManager.put("ProjectLibre.ribbonSurfaceHeight", Integer.valueOf(tokens.ribbonSurfaceHeight()));
		UIManager.put("ProjectLibre.ribbonBandVerticalInset", Integer.valueOf(tokens.ribbonBandVerticalInset()));
		UIManager.put("ProjectLibre.ribbonButtonVerticalInset", Integer.valueOf(tokens.ribbonButtonVerticalInset()));
		UIManager.put("ProjectLibre.ribbonSearchHeight", Integer.valueOf(tokens.ribbonSearchHeight()));
		UIManager.put("ProjectLibre.ribbonSearchPreferredWidth", Integer.valueOf(tokens.ribbonSearchPreferredWidth()));
		UIManager.put("ProjectLibre.ribbonSearchMaxWidth", Integer.valueOf(tokens.ribbonSearchMaxWidth()));
		UIManager.put("ProjectLibre.ribbonCornerRadius", Integer.valueOf(tokens.ribbonCornerRadius()));
		UIManager.put("ProjectLibre.ribbonButtonArc", Integer.valueOf(tokens.ribbonButtonArc()));
		UIManager.put("ProjectLibre.ribbonQuickAccessButtonSize", Integer.valueOf(tokens.ribbonQuickAccessButtonSize()));
		UIManager.put("ProjectLibre.ribbonLargeButtonHeight", Integer.valueOf(tokens.ribbonLargeButtonHeight()));
		UIManager.put("ProjectLibre.ribbonLargeButtonMinWidth", Integer.valueOf(tokens.ribbonLargeButtonMinWidth()));
		UIManager.put("ProjectLibre.ribbonInlineButtonHeight", Integer.valueOf(tokens.ribbonInlineButtonHeight()));
		UIManager.put("ProjectLibre.ribbonInlineButtonMediumMinWidth", Integer.valueOf(tokens.ribbonInlineButtonMediumMinWidth()));
		UIManager.put("ProjectLibre.ribbonInlineButtonSmallMinWidth", Integer.valueOf(tokens.ribbonInlineButtonSmallMinWidth()));
		UIManager.put("ProjectLibre.ribbonBandTitleHeight", Integer.valueOf(tokens.ribbonBandTitleHeight()));
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

	public static ProjectLibreThemeTokens tokens() {
		return LIGHT_TOKENS;
	}
}
