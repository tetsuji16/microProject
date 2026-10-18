/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.reports.adapter;

/** Shared report zoom constraints. */
public final class ReportViewer {
	private ReportViewer() { }
	public static float clampZoomRatio(float zoomRatio) {
		if (Float.isNaN(zoomRatio) || Float.isInfinite(zoomRatio)) return 1.0f;
		return Math.max(0.1f, Math.min(zoomRatio, 4.0f));
	}
}
