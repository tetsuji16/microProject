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

	private static boolean dark() {
		return FlatUiSupport.isDarkTheme();
	}

	/** Chart surface: the workspace body color in the dark theme, white otherwise. */
	public static Color background() {
		return dark() ? FlatUiSupport.spreadsheetBodyBackground() : BACKGROUND;
	}

	public static Color gridLine() {
		return dark() ? FlatUiSupport.ganttHeaderGridColor() : GRID_LINE;
	}

	public static Color progressTrack() {
		return dark() ? new Color(0x3A4A68) : TASK_TRACK;
	}

	public static Color progressFill() {
		return dark() ? new Color(0x6F9BE8) : TASK_PROGRESS;
	}

	public static Color summary() {
		return dark() ? new Color(0xC8CCD2) : SUMMARY;
	}

	public static Color summaryBackground() {
		return dark() ? new Color(0x4A4E55) : SUMMARY_BACKGROUND;
	}

	public static Color dependency() {
		return dark() ? new Color(0xB4B9C0) : DEPENDENCY;
	}

	public static Color baseline() {
		return dark() ? new Color(0x9AA0A8) : BASELINE;
	}

	/** Non-working day shading: a faint lift of the dark chart surface. */
	public static Color nonWorkingFill() {
		Color base = background();
		return new Color(
			Math.min(255, base.getRed() + 14),
			Math.min(255, base.getGreen() + 14),
			Math.min(255, base.getBlue() + 14));
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
			return summary();
		if ("Bar.baseline".equals(id) || id.startsWith("Bar.baseline"))
			return baseline();
		if ("Bar.critical".equals(id))
			return CRITICAL;
		if ("Link.link1".equals(id))
			return dependency();
		return statusColor == null ? TASK : statusColor;
	}

	public static Color textColorFor(Color fill) {
		return Color.WHITE;
	}
}
