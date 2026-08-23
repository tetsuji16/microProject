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

import java.awt.Color;

/**
 * Semantic design tokens for the modernized desktop UI.
 */
public final class ProjectLibreThemeTokens {
	private final Color appBackground;
	private final Color workspaceBackground;
	private final Color panelBackground;
	private final Color dialogBackground;
	private final Color dialogSurfaceBackground;
	private final Color ribbonChromeBackground;
	private final Color ribbonSurfaceBackground;
	private final Color tableBackground;
	private final Color tableForeground;
	private final Color tableSelectionBackground;
	private final Color tableSelectionForeground;
	private final Color spreadsheetBodyBackground;
	private final Color spreadsheetReadOnlyForeground;
	private final Color spreadsheetHeaderBackground;
	private final Color spreadsheetHeaderSelectedBackground;
	private final Color spreadsheetRangeSelectionBackground;
	private final Color spreadsheetActiveCellBorder;
	private final Color spreadsheetGridColor;
	private final Color headerBackground;
	private final Color headerForeground;
	private final Color labelForeground;
	private final Color disabledForeground;
	private final Color borderColor;
	private final Color separatorColor;
	private final Color accentColor;
	private final Color errorColor;
	private final Color tableGridColor;
	private final int compactSpacing;
	private final int contentSpacing;
	private final int sectionSpacing;
	private final int dialogButtonHeight;
	private final int dialogCornerRadius;
	private final int ribbonChromeHeight;
	private final int ribbonChromeVerticalInset;
	private final int ribbonHorizontalInset;
	private final int ribbonTabHeight;
	private final int ribbonTabHorizontalPadding;
	private final int ribbonTabVerticalPadding;
	private final int ribbonSurfaceHeight;
	private final int ribbonBandVerticalInset;
	private final int ribbonButtonVerticalInset;
	private final int ribbonSearchHeight;
	private final int ribbonSearchPreferredWidth;
	private final int ribbonSearchMaxWidth;
	private final int ribbonCornerRadius;
	private final int ribbonButtonArc;
	private final int ribbonQuickAccessButtonSize;
	private final int ribbonLargeButtonHeight;
	private final int ribbonLargeButtonMinWidth;
	private final int ribbonInlineButtonHeight;
	private final int ribbonInlineButtonMediumMinWidth;
	private final int ribbonInlineButtonSmallMinWidth;
	private final int ribbonBandTitleHeight;

	private ProjectLibreThemeTokens(
		Color appBackground,
		Color workspaceBackground,
		Color panelBackground,
		Color dialogBackground,
		Color dialogSurfaceBackground,
		Color ribbonChromeBackground,
		Color ribbonSurfaceBackground,
		Color tableBackground,
		Color tableForeground,
		Color tableSelectionBackground,
		Color tableSelectionForeground,
		Color spreadsheetBodyBackground,
		Color spreadsheetReadOnlyForeground,
		Color spreadsheetHeaderBackground,
		Color spreadsheetHeaderSelectedBackground,
		Color spreadsheetRangeSelectionBackground,
		Color spreadsheetActiveCellBorder,
		Color spreadsheetGridColor,
		Color headerBackground,
		Color headerForeground,
		Color labelForeground,
		Color disabledForeground,
		Color borderColor,
		Color separatorColor,
		Color accentColor,
		Color errorColor,
		Color tableGridColor,
		int compactSpacing,
		int contentSpacing,
		int sectionSpacing,
		int dialogButtonHeight,
		int dialogCornerRadius,
		int ribbonChromeHeight,
		int ribbonChromeVerticalInset,
		int ribbonHorizontalInset,
		int ribbonTabHeight,
		int ribbonTabHorizontalPadding,
		int ribbonTabVerticalPadding,
		int ribbonSurfaceHeight,
		int ribbonBandVerticalInset,
		int ribbonButtonVerticalInset,
		int ribbonSearchHeight,
		int ribbonSearchPreferredWidth,
		int ribbonSearchMaxWidth,
		int ribbonCornerRadius,
		int ribbonButtonArc,
		int ribbonQuickAccessButtonSize,
		int ribbonLargeButtonHeight,
		int ribbonLargeButtonMinWidth,
		int ribbonInlineButtonHeight,
		int ribbonInlineButtonMediumMinWidth,
		int ribbonInlineButtonSmallMinWidth,
		int ribbonBandTitleHeight) {
		this.appBackground = appBackground;
		this.workspaceBackground = workspaceBackground;
		this.panelBackground = panelBackground;
		this.dialogBackground = dialogBackground;
		this.dialogSurfaceBackground = dialogSurfaceBackground;
		this.ribbonChromeBackground = ribbonChromeBackground;
		this.ribbonSurfaceBackground = ribbonSurfaceBackground;
		this.tableBackground = tableBackground;
		this.tableForeground = tableForeground;
		this.tableSelectionBackground = tableSelectionBackground;
		this.tableSelectionForeground = tableSelectionForeground;
		this.spreadsheetBodyBackground = spreadsheetBodyBackground;
		this.spreadsheetReadOnlyForeground = spreadsheetReadOnlyForeground;
		this.spreadsheetHeaderBackground = spreadsheetHeaderBackground;
		this.spreadsheetHeaderSelectedBackground = spreadsheetHeaderSelectedBackground;
		this.spreadsheetRangeSelectionBackground = spreadsheetRangeSelectionBackground;
		this.spreadsheetActiveCellBorder = spreadsheetActiveCellBorder;
		this.spreadsheetGridColor = spreadsheetGridColor;
		this.headerBackground = headerBackground;
		this.headerForeground = headerForeground;
		this.labelForeground = labelForeground;
		this.disabledForeground = disabledForeground;
		this.borderColor = borderColor;
		this.separatorColor = separatorColor;
		this.accentColor = accentColor;
		this.errorColor = errorColor;
		this.tableGridColor = tableGridColor;
		this.compactSpacing = compactSpacing;
		this.contentSpacing = contentSpacing;
		this.sectionSpacing = sectionSpacing;
		this.dialogButtonHeight = dialogButtonHeight;
		this.dialogCornerRadius = dialogCornerRadius;
		this.ribbonChromeHeight = ribbonChromeHeight;
		this.ribbonChromeVerticalInset = ribbonChromeVerticalInset;
		this.ribbonHorizontalInset = ribbonHorizontalInset;
		this.ribbonTabHeight = ribbonTabHeight;
		this.ribbonTabHorizontalPadding = ribbonTabHorizontalPadding;
		this.ribbonTabVerticalPadding = ribbonTabVerticalPadding;
		this.ribbonSurfaceHeight = ribbonSurfaceHeight;
		this.ribbonBandVerticalInset = ribbonBandVerticalInset;
		this.ribbonButtonVerticalInset = ribbonButtonVerticalInset;
		this.ribbonSearchHeight = ribbonSearchHeight;
		this.ribbonSearchPreferredWidth = ribbonSearchPreferredWidth;
		this.ribbonSearchMaxWidth = ribbonSearchMaxWidth;
		this.ribbonCornerRadius = ribbonCornerRadius;
		this.ribbonButtonArc = ribbonButtonArc;
		this.ribbonQuickAccessButtonSize = ribbonQuickAccessButtonSize;
		this.ribbonLargeButtonHeight = ribbonLargeButtonHeight;
		this.ribbonLargeButtonMinWidth = ribbonLargeButtonMinWidth;
		this.ribbonInlineButtonHeight = ribbonInlineButtonHeight;
		this.ribbonInlineButtonMediumMinWidth = ribbonInlineButtonMediumMinWidth;
		this.ribbonInlineButtonSmallMinWidth = ribbonInlineButtonSmallMinWidth;
		this.ribbonBandTitleHeight = ribbonBandTitleHeight;
	}

	public static ProjectLibreThemeTokens light() {
		return new ProjectLibreThemeTokens(
			new Color(0xF1F5F9),
			new Color(0xEDF2F7),
			new Color(0xF8FAFC),
			new Color(0xF8FAFC),
			Color.WHITE,
			new Color(0xF2F2F2),
			Color.WHITE,
			Color.WHITE,
			new Color(0x1F2937),
			new Color(0xEAF3FF),
			new Color(0x1F2937),
			Color.WHITE,
			new Color(0x6B7280),
			new Color(0xF5F6F8),
			new Color(0xDCEBFF),
			new Color(0xEAF3FF),
			new Color(0x217346),
			new Color(0xD8E0EA),
			new Color(0xF5F6F8),
			new Color(0x1F2937),
			new Color(0x1F2937),
			new Color(0x6B7280),
			new Color(0xD8E0EA),
			new Color(0xD8E0EA),
			new Color(0x217346),
			new Color(0xD13438),
			new Color(0xD8E0EA),
			6,
			10,
			14,
			30,
			6,
			32,
			3,
			8,
			36,
			8,
			3,
			92,
			1,
			4,
			24,
			300,
			340,
			8,
			6,
			24,
			78,
			60,
			22,
			76,
			62,
			16);
	}

	public Color appBackground() {
		return appBackground;
	}

	public Color workspaceBackground() {
		return workspaceBackground;
	}

	public Color panelBackground() {
		return panelBackground;
	}

	public Color dialogBackground() {
		return dialogBackground;
	}

	public Color dialogSurfaceBackground() {
		return dialogSurfaceBackground;
	}

	public Color ribbonChromeBackground() {
		return ribbonChromeBackground;
	}

	public Color ribbonSurfaceBackground() {
		return ribbonSurfaceBackground;
	}

	public Color tableBackground() {
		return tableBackground;
	}

	public Color tableForeground() {
		return tableForeground;
	}

	public Color tableSelectionBackground() {
		return tableSelectionBackground;
	}

	public Color tableSelectionForeground() {
		return tableSelectionForeground;
	}

	public Color spreadsheetBodyBackground() {
		return spreadsheetBodyBackground;
	}

	public Color spreadsheetReadOnlyForeground() {
		return spreadsheetReadOnlyForeground;
	}

	public Color spreadsheetHeaderBackground() {
		return spreadsheetHeaderBackground;
	}

	public Color spreadsheetHeaderSelectedBackground() {
		return spreadsheetHeaderSelectedBackground;
	}

	public Color spreadsheetRangeSelectionBackground() {
		return spreadsheetRangeSelectionBackground;
	}

	public Color spreadsheetActiveCellBorder() {
		return spreadsheetActiveCellBorder;
	}

	public Color spreadsheetGridColor() {
		return spreadsheetGridColor;
	}

	public Color headerBackground() {
		return headerBackground;
	}

	public Color headerForeground() {
		return headerForeground;
	}

	public Color labelForeground() {
		return labelForeground;
	}

	public Color disabledForeground() {
		return disabledForeground;
	}

	public Color borderColor() {
		return borderColor;
	}

	public Color separatorColor() {
		return separatorColor;
	}

	public Color accentColor() {
		return accentColor;
	}

	public Color errorColor() {
		return errorColor;
	}

	public Color tableGridColor() {
		return tableGridColor;
	}

	public int compactSpacing() {
		return compactSpacing;
	}

	public int contentSpacing() {
		return contentSpacing;
	}

	public int sectionSpacing() {
		return sectionSpacing;
	}

	public int dialogButtonHeight() {
		return dialogButtonHeight;
	}

	public int dialogCornerRadius() {
		return dialogCornerRadius;
	}

	public int ribbonChromeHeight() {
		return ribbonChromeHeight;
	}

	public int ribbonChromeVerticalInset() {
		return ribbonChromeVerticalInset;
	}

	public int ribbonHorizontalInset() {
		return ribbonHorizontalInset;
	}

	public int ribbonTabHeight() {
		return ribbonTabHeight;
	}

	public int ribbonTabHorizontalPadding() {
		return ribbonTabHorizontalPadding;
	}

	public int ribbonTabVerticalPadding() {
		return ribbonTabVerticalPadding;
	}

	public int ribbonSurfaceHeight() {
		return ribbonSurfaceHeight;
	}

	public int ribbonBandVerticalInset() {
		return ribbonBandVerticalInset;
	}

	public int ribbonButtonVerticalInset() {
		return ribbonButtonVerticalInset;
	}

	public int ribbonSearchHeight() {
		return ribbonSearchHeight;
	}

	public int ribbonSearchPreferredWidth() {
		return ribbonSearchPreferredWidth;
	}

	public int ribbonSearchMaxWidth() {
		return ribbonSearchMaxWidth;
	}

	public int ribbonCornerRadius() {
		return ribbonCornerRadius;
	}

	public int ribbonButtonArc() {
		return ribbonButtonArc;
	}

	public int ribbonQuickAccessButtonSize() {
		return ribbonQuickAccessButtonSize;
	}

	public int ribbonLargeButtonHeight() {
		return ribbonLargeButtonHeight;
	}

	public int ribbonLargeButtonMinWidth() {
		return ribbonLargeButtonMinWidth;
	}

	public int ribbonInlineButtonHeight() {
		return ribbonInlineButtonHeight;
	}

	public int ribbonInlineButtonMediumMinWidth() {
		return ribbonInlineButtonMediumMinWidth;
	}

	public int ribbonInlineButtonSmallMinWidth() {
		return ribbonInlineButtonSmallMinWidth;
	}

	public int ribbonBandTitleHeight() {
		return ribbonBandTitleHeight;
	}
}
