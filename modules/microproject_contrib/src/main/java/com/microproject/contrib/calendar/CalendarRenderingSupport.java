/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.contrib.calendar;

import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Shared rendering policy for calendar components. */
public final class CalendarRenderingSupport {
	private CalendarRenderingSupport() {
	}

	/** Applies the quality settings required for readable HiDPI calendar text. */
	public static void applyQualityHints(Graphics2D graphics) {
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
				RenderingHints.VALUE_FRACTIONALMETRICS_ON);
	}
}
