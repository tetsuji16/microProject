package com.microproject.util;

import java.awt.Color;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import com.microproject.graphic.configuration.BarFormat;
import com.microproject.pm.scheduling.Schedule;
/**
 * Shared monday.com-inspired colors and painting helpers for Gantt rendering.
 */
public final class MondayGanttTheme {
	private static final int BACKGROUND_BAR_ALPHA = 76;
	private static final Color TEXT_DARK = new Color(0x202124);
	private static final Color SUMMARY_BACKGROUND = new Color(0xDDE7F5);
	private static final Color SUMMARY_PROGRESS_BACKGROUND = new Color(0xEEF3FA);
	private static final Color CRITICAL_ACCENT = new Color(0x5F, 0x64, 0x6D);

	public static final Color DONE = new Color(0x00C875);
	public static final Color WORKING_ON_IT = new Color(0xFDAB3D);
	public static final Color STUCK = new Color(0xE2445C);
	public static final Color NOT_STARTED = new Color(0xC4C4C4);
	public static final Color BASELINE = new Color(0xA1A1A1);
	public static final Color NO_COMPARISON = new Color(0xE5E5E5);
	public static final Color GROUP_A = new Color(0x579BFC);
	public static final Color GROUP_B = new Color(0xA25DDC);
	public static final Color DEPENDENCY_LINK = new Color(0x5F, 0x64, 0x6D);
	public static final Color BACKGROUND = Color.WHITE;
	public static final Color HEADER_BACKGROUND = new Color(0xF5F6F8);
	public static final Color GRID_LINE = new Color(0xE1E1E1);

	private MondayGanttTheme() {
	}

	public static Color chartBackground() {
		return BACKGROUND;
	}

	public static Color headerBackground() {
		return HEADER_BACKGROUND;
	}

	public static Color gridLine() {
		return GRID_LINE;
	}

	public static Color projectLine() {
		return GROUP_A;
	}

	public static Color statusDateLine() {
		return GROUP_B;
	}

	public static Color externalLinkColor() {
		return GROUP_B;
	}

	public static double progressRatio(Schedule schedule, Object impl) {
		return GanttProgress.ratio(schedule, impl);
	}

	public static Color statusColor(Schedule schedule, Object impl) {
		if (schedule == null && impl == null)
			return GROUP_A;
		double progress = progressRatio(schedule, impl);
		if (progress >= 1.0d)
			return DONE;
		if (progress <= 0.0d)
			return NOT_STARTED;
		return WORKING_ON_IT;
	}

	public static Color accentColor(BarFormat format, Color statusColor) {
		if (format == null)
			return GROUP_A;

		String id = format.getId();
		if (id == null)
			return GROUP_A;

		if ("Bar.assignment".equals(id))
			return GROUP_B;
		if ("Bar.summary".equals(id))
			return CRITICAL_ACCENT;
		if ("Bar.deadline".equals(id))
			return GROUP_A;
		if ("Bar.baseline".equals(id) || id.startsWith("Bar.baseline"))
			return BASELINE;
		if ("Bar.totalSlack".equals(id))
			return GROUP_A;
		if ("Bar.critical".equals(id))
			return CRITICAL_ACCENT;
		if ("Link.link1".equals(id))
			return DEPENDENCY_LINK;
		if ("Bar.milestone".equals(id))
			return statusColor == null ? GROUP_A : shade(statusColor, 0.18f);
		if ("Bar.task".equals(id))
			return statusColor == null ? GROUP_A : shade(statusColor, 0.18f);
		return GROUP_A;
	}

	public static Color summaryBackground() {
		return SUMMARY_BACKGROUND;
	}

	public static Color summaryProgressBackground() {
		return SUMMARY_PROGRESS_BACKGROUND;
	}

	public static Color criticalAccent() {
		return CRITICAL_ACCENT;
	}

	public static Paint createLayerPaint(Color baseColor, Rectangle2D bounds, boolean backgroundLayer) {
		if (bounds == null || baseColor == null)
			return baseColor;

		Color top = backgroundLayer ? withAlpha(soften(baseColor, 0.28f), BACKGROUND_BAR_ALPHA)
				: soften(baseColor, 0.18f);
		Color middle = backgroundLayer ? withAlpha(baseColor, BACKGROUND_BAR_ALPHA) : baseColor;
		Color bottom = backgroundLayer ? withAlpha(shade(baseColor, 0.10f), BACKGROUND_BAR_ALPHA)
				: shade(baseColor, 0.08f);
		return new LinearGradientPaint(
				new Point2D.Double(bounds.getX(), bounds.getY()),
				new Point2D.Double(bounds.getX(), bounds.getMaxY()),
				new float[] { 0.0f, 0.55f, 1.0f },
				new Color[] { top, middle, bottom });
	}

	public static Color withAlpha(Color color, int alpha) {
		if (color == null)
			return null;
		int clampedAlpha = Math.max(0, Math.min(255, alpha));
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), clampedAlpha);
	}

	public static Color textColorFor(Color fill) {
		return isDark(fill) ? Color.WHITE : TEXT_DARK;
	}

	public static boolean isDark(Color color) {
		if (color == null)
			return false;
		double luminance = (0.2126d * color.getRed() + 0.7152d * color.getGreen() + 0.0722d * color.getBlue()) / 255d;
		return luminance < 0.52d;
	}

	public static Color soften(Color color, float ratio) {
		return mix(color, Color.WHITE, ratio);
	}

	public static Color shade(Color color, float ratio) {
		return mix(color, Color.BLACK, ratio);
	}

	private static Color mix(Color color, Color target, float ratio) {
		if (color == null)
			return target;
		float weight = Math.max(0f, Math.min(1f, ratio));
		float targetWeight = 1f - weight;
		int red = Math.round(color.getRed() * targetWeight + target.getRed() * weight);
		int green = Math.round(color.getGreen() * targetWeight + target.getGreen() * weight);
		int blue = Math.round(color.getBlue() * targetWeight + target.getBlue() * weight);
		return new Color(red, green, blue, color.getAlpha());
	}

	private static double clamp(double value) {
		if (value < 0.0d)
			return 0.0d;
		if (value > 1.0d)
			return 1.0d;
		return value;
	}
}
