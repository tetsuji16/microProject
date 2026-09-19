/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.util;

import java.awt.Color;
import java.awt.Paint;
import java.awt.geom.Rectangle2D;

import com.microproject.graphic.configuration.BarFormat;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.scheduling.Schedule;

/** Flat MS Project-compatible palette for the default Gantt chart. */
public final class MicrosoftProjectGanttPalette implements GanttColorPalette {
	@Override
	public String getName() {
		return "Microsoft Project";
	}

	@Override public Color getChartBackground() { return MicrosoftProjectGanttTheme.BACKGROUND; }
	@Override public Color getGridLine() { return MicrosoftProjectGanttTheme.GRID_LINE; }
	@Override public Color getTaskBar(Color statusColor) { return statusColor == null ? MicrosoftProjectGanttTheme.TASK : statusColor; }
	@Override public Color getStatusColor(Schedule schedule, Object context) { return MicrosoftProjectGanttTheme.statusColor(schedule, context); }
	@Override public Color getAnnotationColor(BarFormat format) { return MicrosoftProjectGanttTheme.accentColor(format, MicrosoftProjectGanttTheme.TASK); }

	@Override
	public Color getAccentColor(BarFormat format, Color statusColor, Object context) {
		return context instanceof Assignment ? MicrosoftProjectGanttTheme.ASSIGNMENT
			: MicrosoftProjectGanttTheme.accentColor(format, statusColor);
	}

	@Override public Color getTextColor(Color fillColor) { return MicrosoftProjectGanttTheme.textColorFor(fillColor); }
	@Override public Paint createBarPaint(Color fillColor, Rectangle2D bounds, boolean backgroundLayer, boolean textured) { return fillColor; }
	@Override public Color getProgressTrackColor(Color statusColor) { return MicrosoftProjectGanttTheme.TASK_TRACK; }
	@Override public Color getProgressFillColor(Color statusColor) { return MicrosoftProjectGanttTheme.TASK_PROGRESS; }
	@Override public Color getBaselineBarColor() { return MicrosoftProjectGanttTheme.BASELINE; }
	@Override public Color getCriticalTaskColor() { return MicrosoftProjectGanttTheme.CRITICAL; }
	@Override public Color getExternalLinkColor() { return MicrosoftProjectGanttTheme.DEPENDENCY; }
	@Override public Color getDependencyLinkColor() { return MicrosoftProjectGanttTheme.DEPENDENCY; }
	@Override public Color getProjectLineColor() { return MicrosoftProjectGanttTheme.PROJECT_LINE; }
	@Override public Color getStatusDateLineColor() { return MicrosoftProjectGanttTheme.STATUS_DATE; }
	@Override public Color getSummaryBackgroundColor(Color statusColor) { return MicrosoftProjectGanttTheme.SUMMARY_BACKGROUND; }
	@Override public Color getSummaryProgressColor(Color statusColor) { return MicrosoftProjectGanttTheme.SUMMARY; }
}
