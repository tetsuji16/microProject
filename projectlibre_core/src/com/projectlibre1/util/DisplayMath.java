package com.projectlibre1.util;

import com.projectlibre1.pm.scheduling.Schedule;
import com.projectlibre1.pm.scheduling.ScheduleInterval;

public final class DisplayMath {
	private DisplayMath() {
	}

	public static double clampProgressRatio(Schedule schedule) {
		return schedule == null ? 0.0d : clampProgressValue(schedule.getPercentComplete());
	}

	public static double clampProgressValue(double value) {
		if (Double.isNaN(value)) {
			return 0.0d;
		}
		if (value < 0.0d) {
			return 0.0d;
		}
		if (value > 1.0d) {
			return 1.0d;
		}
		return value;
	}

	public static double progressWidth(double totalWidth, double progressRatio) {
		return totalWidth * clampProgressValue(progressRatio);
	}

	public static ScheduleInterval mergeIntervals(Iterable<ScheduleInterval> intervals) {
		if (intervals == null) {
			return null;
		}
		long start = Long.MAX_VALUE;
		long end = Long.MIN_VALUE;
		boolean found = false;
		for (ScheduleInterval interval : intervals) {
			if (interval == null) {
				continue;
			}
			start = Math.min(start, interval.getStart());
			end = Math.max(end, interval.getEnd());
			found = true;
		}
		return found ? new ScheduleInterval(start, end) : null;
	}
}
