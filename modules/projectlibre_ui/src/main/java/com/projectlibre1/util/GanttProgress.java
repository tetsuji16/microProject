package com.projectlibre1.util;

import com.projectlibre1.pm.scheduling.Schedule;
import com.projectlibre1.pm.task.TaskSpecificFields;

public final class GanttProgress {
	private GanttProgress() {
	}

	public static double ratio(Schedule schedule, Object impl) {
		if (impl instanceof TaskSpecificFields)
			return DisplayMath.clampProgressValue(((TaskSpecificFields)impl).getPercentComplete());
		return schedule == null ? 0.0d : DisplayMath.clampProgressValue(schedule.getPercentComplete());
	}

	public static double ratioForObject(Object impl) {
		return ratio(impl instanceof Schedule ? (Schedule)impl : null, impl);
	}

	public static boolean hasVisibleProgress(Object impl) {
		return ratioForObject(impl) > 0.0d;
	}

	/**
	 * Resolves the Complete Through date used by Microsoft Project-style progress
	 * lines. Task progress is duration based (% Complete), while the schedule
	 * supplies the calendar-aware completion date.
	 */
	public static long progressLineDate(Schedule schedule, long referenceDate) {
		if (schedule == null)
			return referenceDate;
		long start = schedule.getStart();
		long end = schedule.getEnd();
		double progress = DisplayMath.clampProgressValue(schedule.getPercentComplete());
		if (referenceDate != 0L && progress >= 1.0d && end <= referenceDate)
			return referenceDate;
		if (referenceDate != 0L && progress <= 0.0d && start >= referenceDate)
			return referenceDate;
		long completedThrough = schedule.getCompletedThrough();
		if (completedThrough <= 0L)
			return start;
		return Math.max(start, Math.min(end, completedThrough));
	}

}
