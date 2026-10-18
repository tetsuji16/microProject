/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.reports.model;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import com.microproject.reports.render.ReportFontProvider;

/** Shared line wrapping used by report pagination and painting. */
public final class ReportTextLayout {
	private static final FontMetrics METRICS = createMetrics();

	private ReportTextLayout() { }

	private static FontMetrics createMetrics() {
		Graphics2D graphics = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
		try { return graphics.getFontMetrics(ReportFontProvider.regular(10)); }
		finally { graphics.dispose(); }
	}

	public static List<String> wrap(String text, int width) {
		if (text == null || text.isEmpty()) return List.of("");
		ArrayList<String> lines = new ArrayList<>();
		StringBuilder line = new StringBuilder();
		for (int offset = 0; offset < text.length();) {
			int codePoint = text.codePointAt(offset);
			offset += Character.charCount(codePoint);
			String next = new String(Character.toChars(codePoint));
			if (line.length() > 0 && METRICS.stringWidth(line.toString() + next) > width) {
				lines.add(line.toString());
				line.setLength(0);
			}
			line.append(next);
		}
		lines.add(line.toString());
		return List.copyOf(lines);
	}

	public static int lineCount(String text, int width) { return wrap(text, width).size(); }
}
