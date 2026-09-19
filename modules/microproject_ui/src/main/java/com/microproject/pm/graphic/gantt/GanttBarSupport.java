/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
 * Copyright (c) 2026 microProject
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package com.microproject.pm.graphic.gantt;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.microproject.graphic.configuration.BarFormat;
import com.microproject.pm.scheduling.Schedule;
import com.microproject.pm.scheduling.ScheduleInterval;
import com.microproject.util.DisplayMath;

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

	static boolean shouldPreserveSplitIntervals(BarFormat format) {
		if (format == null || format.getId() == null) {
			return false;
		}
		String id = format.getId();
		return "Bar.task".equals(id) || "Bar.critical".equals(id) || "Bar.assignment".equals(id);
	}

	static boolean shouldUseUniformEndpointColor(BarFormat format) {
		return shouldUseModernCapsuleBar(format);
	}

	static boolean isIndividuallyFormattable(BarFormat format) {
		if (format == null)
			return false;
		String id = format.getId();
		return "Bar.task".equals(id)
				|| "Bar.critical".equals(id)
				|| "Bar.summary".equals(id)
				|| "Bar.milestone".equals(id);
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

	static List<ScheduleInterval> displayIntervals(BarFormat format, Iterable<ScheduleInterval> generatedIntervals,
			ScheduleInterval plannedInterval) {
		List<ScheduleInterval> normalized = normalizeIntervals(generatedIntervals);
		if (shouldPreserveSplitIntervals(format) && normalized.size() > 1) {
			return normalized;
		}
		if (shouldUsePlannedEnvelopeInterval(format) && plannedInterval != null) {
			return List.of(plannedInterval);
		}
		ScheduleInterval merged = mergeIntervalsForDisplay(normalized);
		return merged == null ? List.of() : List.of(merged);
	}

	static List<ScheduleInterval> normalizeIntervals(Iterable<ScheduleInterval> intervals) {
		if (intervals == null) {
			return List.of();
		}
		ArrayList<ScheduleInterval> sorted = new ArrayList<>();
		for (ScheduleInterval interval : intervals) {
			if (interval != null && interval.getStart() <= interval.getEnd()) {
				sorted.add(new ScheduleInterval(interval.getStart(), interval.getEnd()));
			}
		}
		sorted.sort(Comparator.comparingLong(ScheduleInterval::getStart)
				.thenComparingLong(ScheduleInterval::getEnd));
		if (sorted.isEmpty()) {
			return List.of();
		}

		ArrayList<ScheduleInterval> normalized = new ArrayList<>();
		long start = sorted.get(0).getStart();
		long end = sorted.get(0).getEnd();
		for (int i = 1; i < sorted.size(); i++) {
			ScheduleInterval next = sorted.get(i);
			if (next.getStart() <= end) {
				end = Math.max(end, next.getEnd());
			} else {
				normalized.add(new ScheduleInterval(start, end));
				start = next.getStart();
				end = next.getEnd();
			}
		}
		normalized.add(new ScheduleInterval(start, end));
		return List.copyOf(normalized);
	}

	static List<ScheduleInterval> splitGaps(List<ScheduleInterval> intervals) {
		List<ScheduleInterval> normalized = normalizeIntervals(intervals);
		if (normalized.size() < 2) {
			return List.of();
		}
		ArrayList<ScheduleInterval> gaps = new ArrayList<>(normalized.size() - 1);
		for (int i = 1; i < normalized.size(); i++) {
			gaps.add(new ScheduleInterval(normalized.get(i - 1).getEnd(), normalized.get(i).getStart()));
		}
		return List.copyOf(gaps);
	}

	static List<Double> progressRatiosForIntervals(List<ScheduleInterval> intervals, double progressRatio) {
		List<ScheduleInterval> normalized = normalizeIntervals(intervals);
		if (normalized.isEmpty()) {
			return List.of();
		}
		double clampedRatio = DisplayMath.clampProgressValue(progressRatio);
		double totalDuration = 0.0d;
		for (ScheduleInterval interval : normalized) {
			totalDuration += Math.max(0L, interval.getEnd() - interval.getStart());
		}
		if (totalDuration <= 0.0d) {
			ArrayList<Double> zeroDurationRatios = new ArrayList<>(normalized.size());
			for (int i = 0; i < normalized.size(); i++) {
				zeroDurationRatios.add(i == 0 ? clampedRatio : 0.0d);
			}
			return List.copyOf(zeroDurationRatios);
		}

		double remainingCompletedDuration = totalDuration * clampedRatio;
		ArrayList<Double> ratios = new ArrayList<>(normalized.size());
		for (ScheduleInterval interval : normalized) {
			double duration = Math.max(0L, interval.getEnd() - interval.getStart());
			double completed = Math.min(duration, Math.max(0.0d, remainingCompletedDuration));
			ratios.add(duration <= 0.0d ? 0.0d : completed / duration);
			remainingCompletedDuration -= completed;
		}
		return List.copyOf(ratios);
	}

	static double progressRatioForSchedule(Schedule schedule) {
		return DisplayMath.clampProgressRatio(schedule);
	}
}
