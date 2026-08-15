package com.microproject.reports.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ReportViewerZoomTest {
	@Test
	void clampZoomRatioKeepsValuesWithinSupportedBounds() {
		assertEquals(0.1f, ReportViewer.clampZoomRatio(0.01f), 0.001f);
		assertEquals(2.5f, ReportViewer.clampZoomRatio(2.5f), 0.001f);
		assertEquals(4.0f, ReportViewer.clampZoomRatio(100.0f), 0.001f);
	}

	@Test
	void clampZoomRatioUsesDefaultForNaN() {
		assertEquals(1.0f, ReportViewer.clampZoomRatio(Float.NaN), 0.001f);
	}
}
