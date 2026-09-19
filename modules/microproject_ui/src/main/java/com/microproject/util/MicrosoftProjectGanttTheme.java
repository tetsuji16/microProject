/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.util;

import java.awt.Color;

import com.microproject.graphic.configuration.BarFormat;
import com.microproject.pm.scheduling.Schedule;

/** MS Project-compatible colors used by the default Gantt chart palette. */
public final class MicrosoftProjectGanttTheme {
	public static final Color TASK = new Color(0x4472C4);
	public static final Color TASK_PROGRESS = new Color(0x2F5597);
	public static final Color TASK_TRACK = new Color(0xD9E2F3);
	public static final Color SUMMARY = new Color(0x404040);
	public static final Color BASELINE = new Color(0x7F7F7F);
	public static final Color CRITICAL = new Color(0xC00000);
	public static final Color ASSIGNMENT = new Color(0x70AD47);
	public static final Color DEPENDENCY = new Color(0x404040);
	public static final Color PROJECT_LINE = new Color(0xC00000);
	public static final Color STATUS_DATE = new Color(0x70AD47);
	public static final Color BACKGROUND = Color.WHITE;
	public static final Color GRID_LINE = new Color(0xD9E2F3);
	public static final Color SUMMARY_BACKGROUND = new Color(0xE7E6E6);

	private MicrosoftProjectGanttTheme() {
	}

	public static Color statusColor(Schedule schedule, Object context) {
		return TASK;
	}

	public static Color accentColor(BarFormat format, Color statusColor) {
		if (format == null || format.getId() == null)
			return TASK;
		String id = format.getId();
		if ("Bar.assignment".equals(id))
			return ASSIGNMENT;
		if ("Bar.summary".equals(id))
			return SUMMARY;
		if ("Bar.baseline".equals(id) || id.startsWith("Bar.baseline"))
			return BASELINE;
		if ("Bar.critical".equals(id))
			return CRITICAL;
		if ("Link.link1".equals(id))
			return DEPENDENCY;
		return statusColor == null ? TASK : statusColor;
	}

	public static Color textColorFor(Color fill) {
		return Color.WHITE;
	}
}
