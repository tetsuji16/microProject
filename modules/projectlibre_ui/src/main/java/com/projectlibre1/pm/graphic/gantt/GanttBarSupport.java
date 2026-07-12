package com.projectlibre1.pm.graphic.gantt;

import java.awt.geom.Rectangle2D;

import com.projectlibre1.graphic.configuration.BarFormat;
import com.projectlibre1.pm.scheduling.Schedule;
import com.projectlibre1.pm.scheduling.ScheduleInterval;
import com.projectlibre1.util.DisplayMath;

/**
 * Shared helpers for gantt bar classification and geometry.
 */
final class GanttBarSupport {
	private GanttBarSupport() {
	}

	static boolean isBaselineBarFormat(BarFormat format) {
		if (format == null || format.getId() == null) {
			return false;
		}
		String id = format.getId();
		return "Bar.baseline".equals(id) || id.startsWith("Bar.baseline");
	}

	static boolean shouldUseModernCapsuleBar(BarFormat format) {
		if (format == null || format.getId() == null) {
			return false;
		}
		String id = format.getId();
		return "Bar.task".equals(id) || "Bar.critical".equals(id) || "Bar.assignment".equals(id) || "Bar.summary".equals(id);
	}

	static boolean shouldUsePlannedEnvelopeInterval(BarFormat format) {
		if (format == null || format.getId() == null) {
			return false;
		}
		String id = format.getId();
		return "Bar.task".equals(id) || "Bar.critical".equals(id) || "Bar.summary".equals(id);
	}

	static boolean shouldUseUniformEndpointColor(BarFormat format) {
		return shouldUseModernCapsuleBar(format);
	}

	static Rectangle2D createCapsuleBarBounds(double x, double y, double width, double height) {
		double safeWidth = Math.max(1.5d, width);
		double safeHeight = Math.max(2.0d, height);
		return new Rectangle2D.Double(x, y - safeHeight / 2.0d, safeWidth, safeHeight);
	}

	static Rectangle2D createSummaryBandBounds(double x, double y, double width, double height) {
		Rectangle2D baseBounds = createCapsuleBarBounds(x, y, width, height);
		double adjustedHeight = Math.max(5.5d, baseBounds.getHeight() * 0.5d);
		return new Rectangle2D.Double(
				baseBounds.getX(),
				baseBounds.getCenterY() - adjustedHeight / 2.0d,
				baseBounds.getWidth(),
				adjustedHeight);
	}

	static Rectangle2D progressOverlayBounds(double x, double y, double totalWidth, double progressHeight, double progressRatio) {
		double completedWidth = DisplayMath.progressWidth(totalWidth, progressRatio);
		if (completedWidth <= 0.0d) {
			return null;
		}
		return createCapsuleBarBounds(x, y, completedWidth, progressHeight);
	}

	static Rectangle2D summaryProgressBounds(Rectangle2D summaryBounds, double progressRatio) {
		if (summaryBounds == null) {
			return null;
		}
		double clampedRatio = DisplayMath.clampProgressValue(progressRatio);
		if (clampedRatio <= 0.0d) {
			return null;
		}
		double progressWidth = Math.max(1.5d, summaryBounds.getWidth() * clampedRatio);
		return new Rectangle2D.Double(
				summaryBounds.getX(),
				summaryBounds.getY(),
				Math.min(summaryBounds.getWidth(), progressWidth),
				summaryBounds.getHeight());
	}

	static ScheduleInterval mergeIntervalsForDisplay(Iterable<ScheduleInterval> intervals) {
		return DisplayMath.mergeIntervals(intervals);
	}

	static double progressRatioForSchedule(Schedule schedule) {
		return DisplayMath.clampProgressRatio(schedule);
	}
}
