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

import com.microproject.ui.theme.MicroProjectTheme;

/**
 * Shared color roles for the FlatLaf-based UI.
 *
 * Keep the palette here so the app-facing helpers and the UI defaults stay in sync.
 */
final class FlatUiTheme {
	static Color appBackground() { return MicroProjectTheme.tokens().appBackground(); }
	static Color ribbonChromeBackground() { return MicroProjectTheme.tokens().ribbonChromeBackground(); }
	static Color tableBackground() { return MicroProjectTheme.tokens().tableBackground(); }
	static Color tableForeground() { return MicroProjectTheme.tokens().tableForeground(); }
	static Color tableSelectionBackground() { return MicroProjectTheme.tokens().tableSelectionBackground(); }
	static Color tableSelectionForeground() { return MicroProjectTheme.tokens().tableSelectionForeground(); }
	static Color spreadsheetBodyBackground() { return MicroProjectTheme.tokens().spreadsheetBodyBackground(); }
	static Color spreadsheetReadOnlyForeground() { return MicroProjectTheme.tokens().spreadsheetReadOnlyForeground(); }
	static Color spreadsheetHeaderBackground() { return MicroProjectTheme.tokens().spreadsheetHeaderBackground(); }
	static Color spreadsheetHeaderSelectedBackground() { return MicroProjectTheme.tokens().spreadsheetHeaderSelectedBackground(); }
	static Color spreadsheetRangeSelectionBackground() { return MicroProjectTheme.tokens().spreadsheetRangeSelectionBackground(); }
	static Color spreadsheetActiveCellBorder() { return MicroProjectTheme.tokens().spreadsheetActiveCellBorder(); }
	static Color spreadsheetGrid() { return MicroProjectTheme.tokens().spreadsheetGridColor(); }
	static Color headerBackground() { return MicroProjectTheme.tokens().headerBackground(); }
	static Color headerForeground() { return MicroProjectTheme.tokens().headerForeground(); }
	static Color labelForeground() { return MicroProjectTheme.tokens().labelForeground(); }
	static Color disabledForeground() { return MicroProjectTheme.tokens().disabledForeground(); }
	static Color border() { return MicroProjectTheme.tokens().borderColor(); }
	static Color separator() { return MicroProjectTheme.tokens().separatorColor(); }
	static Color accent() { return MicroProjectTheme.tokens().accentColor(); }
	static Color error() { return MicroProjectTheme.tokens().errorColor(); }
	static Color tableGrid() { return MicroProjectTheme.tokens().tableGridColor(); }
	static Color infoForeground() { return MicroProjectTheme.tokens().labelForeground(); }

	private FlatUiTheme() {
	}

}
