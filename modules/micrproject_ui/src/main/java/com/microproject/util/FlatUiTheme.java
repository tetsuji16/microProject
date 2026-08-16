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
package com.microproject.util;

import java.awt.Color;

import javax.swing.UIManager;

import com.microproject.ui.theme.ProjectLibreTheme;

/**
 * Shared color roles for the FlatLaf-based UI.
 *
 * Keep the palette here so the app-facing helpers and the UI defaults stay in sync.
 */
final class FlatUiTheme {
	static final Color APP_BACKGROUND = ProjectLibreTheme.tokens().appBackground();
	static final Color RIBBON_CHROME_BACKGROUND = ProjectLibreTheme.tokens().ribbonChromeBackground();
	static final Color RIBBON_SURFACE = ProjectLibreTheme.tokens().ribbonSurfaceBackground();
	static final Color TABLE_BACKGROUND = ProjectLibreTheme.tokens().tableBackground();
	static final Color TABLE_CONTENT_BACKGROUND = ProjectLibreTheme.tokens().tableBackground();
	static final Color TABLE_FOREGROUND = ProjectLibreTheme.tokens().tableForeground();
	static final Color TABLE_SELECTION_BACKGROUND = ProjectLibreTheme.tokens().tableSelectionBackground();
	static final Color TABLE_SELECTION_FOREGROUND = ProjectLibreTheme.tokens().tableSelectionForeground();
	static final Color SPREADSHEET_BODY_BACKGROUND = ProjectLibreTheme.tokens().spreadsheetBodyBackground();
	static final Color SPREADSHEET_READ_ONLY_FOREGROUND = ProjectLibreTheme.tokens().spreadsheetReadOnlyForeground();
	static final Color SPREADSHEET_HEADER_BACKGROUND = ProjectLibreTheme.tokens().spreadsheetHeaderBackground();
	static final Color SPREADSHEET_HEADER_SELECTED_BACKGROUND = ProjectLibreTheme.tokens().spreadsheetHeaderSelectedBackground();
	static final Color SPREADSHEET_RANGE_SELECTION_BACKGROUND = ProjectLibreTheme.tokens().spreadsheetRangeSelectionBackground();
	static final Color SPREADSHEET_ACTIVE_CELL_BORDER = ProjectLibreTheme.tokens().spreadsheetActiveCellBorder();
	static final Color SPREADSHEET_GRID = ProjectLibreTheme.tokens().spreadsheetGridColor();
	static final Color HEADER_BACKGROUND = ProjectLibreTheme.tokens().headerBackground();
	static final Color HEADER_FOREGROUND = ProjectLibreTheme.tokens().headerForeground();
	static final Color LABEL_FOREGROUND = ProjectLibreTheme.tokens().labelForeground();
	static final Color DISABLED_FOREGROUND = ProjectLibreTheme.tokens().disabledForeground();
	static final Color BORDER = ProjectLibreTheme.tokens().borderColor();
	static final Color SEPARATOR = ProjectLibreTheme.tokens().separatorColor();
	static final Color ACCENT = ProjectLibreTheme.tokens().accentColor();
	static final Color ERROR = ProjectLibreTheme.tokens().errorColor();
	static final Color TABLE_GRID = ProjectLibreTheme.tokens().tableGridColor();
	static final Color INFO_FOREGROUND = LABEL_FOREGROUND;

	private FlatUiTheme() {
	}

	static void installIntoUIManager() {
		UIManager.put("TableHeader.background", SPREADSHEET_HEADER_BACKGROUND);
		UIManager.put("TableHeader.foreground", HEADER_FOREGROUND);
	}
}
