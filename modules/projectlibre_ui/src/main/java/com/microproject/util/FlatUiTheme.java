package com.microproject.util;

import java.awt.Color;

import javax.swing.UIManager;

import com.projectlibre.ui.theme.ProjectLibreTheme;

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
