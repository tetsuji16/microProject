package com.projectlibre1.pm.graphic.spreadsheet.renderer;

import java.awt.Color;

import com.projectlibre1.util.FlatUiSupport;

final class SpreadsheetAppearanceSupport {
	private static final float BODY_BLEND = 0.82f;
	private static final float DARK_BODY_BLEND = 0.94f;

	private SpreadsheetAppearanceSupport() {
	}

	static Color resolveBodyBackground(Color background, int row) {
		if (background == null) {
			return FlatUiSupport.spreadsheetAlternateRowBackground(row);
		}
		return soften(background);
	}

	static Color resolveSelectionBackground(Color background) {
		return FlatUiSupport.spreadsheetRangeSelectionBackground();
	}

	static Color resolveForeground(Color foreground) {
		return foreground == null ? FlatUiSupport.tableForeground() : foreground;
	}

	static Color soften(Color color) {
		if (color == null) {
			return FlatUiSupport.spreadsheetBodyBackground();
		}
		float blend = isDark(color) ? DARK_BODY_BLEND : BODY_BLEND;
		return FlatUiSupport.blend(color, FlatUiSupport.spreadsheetBodyBackground(), blend);
	}

	private static boolean isDark(Color color) {
		if (color == null) {
			return false;
		}
		float luminance = (0.2126f * color.getRed() + 0.7152f * color.getGreen() + 0.0722f * color.getBlue()) / 255f;
		return luminance < 0.45f;
	}
}
