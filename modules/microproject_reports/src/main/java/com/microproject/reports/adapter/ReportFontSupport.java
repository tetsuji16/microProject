/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.reports.adapter;

/** Shared font choices for reports rendered in the JasperReports preview. */
final class ReportFontSupport {
	/**
	 * A Java logical font is a composite font. Unlike a physical font such as
	 * Arial, it delegates missing glyphs (including Japanese) to installed
	 * platform fallback fonts. PDF font settings are configured separately by
	 * JasperReports and are not changed by this preview font choice.
	 */
	static final String PREVIEW_FONT_NAME = "Dialog";

	private ReportFontSupport() {
	}
}
