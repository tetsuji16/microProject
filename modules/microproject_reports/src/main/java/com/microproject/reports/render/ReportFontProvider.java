/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.reports.render;

import java.awt.Font;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/** Supplies the bundled faces shared by report layout, painting, and PDF export. */
public final class ReportFontProvider {
	private static final byte[] REGULAR_BYTES = bytes("NotoSansJP-Regular.otf");
	private static final byte[] BOLD_BYTES = bytes("NotoSansJP-Bold.otf");
	private static final Font REGULAR = load(REGULAR_BYTES);
	private static final Font BOLD = load(BOLD_BYTES);

	private ReportFontProvider() { }

	public static Font regular(float size) { return REGULAR.deriveFont(Font.PLAIN, size); }
	public static Font bold(float size) { return BOLD.deriveFont(Font.BOLD, size); }
	public static byte[] regularBytes() { return REGULAR_BYTES.clone(); }
	public static byte[] boldBytes() { return BOLD_BYTES.clone(); }

	private static Font load(byte[] bytes) {
		try { return Font.createFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(bytes)); }
		catch (Exception exception) { throw new ExceptionInInitializerError(exception); }
	}

	private static byte[] bytes(String name) {
		try (InputStream input = resource(name)) { return input.readAllBytes(); }
		catch (IOException exception) { throw new IllegalStateException("Unable to read bundled report font " + name, exception); }
	}

	private static InputStream resource(String name) {
		InputStream input = ReportFontProvider.class.getResourceAsStream("/com/microproject/reports/fonts/" + name);
		if (input == null) throw new IllegalStateException("Missing bundled report font " + name);
		return input;
	}
}
