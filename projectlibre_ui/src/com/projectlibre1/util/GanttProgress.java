package com.projectlibre1.util;

import com.projectlibre1.pm.scheduling.Schedule;
import com.projectlibre1.pm.task.NormalTask;
import com.projectlibre1.pm.task.TaskSpecificFields;

public final class GanttProgress {
	private GanttProgress() {
	}

	public static double ratio(Schedule schedule, Object impl) {
		if (impl instanceof TaskSpecificFields) {
			TaskSpecificFields task = (TaskSpecificFields)impl;
			if (task.isWbsParent())
				return DisplayMath.clampProgressValue(task.getPercentWorkComplete());

			double workProgress = DisplayMath.clampProgressValue(task.getPercentWorkComplete());
			if (impl instanceof NormalTask && ((NormalTask)impl).hasPercentWorkCompleteOverride())
				return workProgress;

			double percentComplete = DisplayMath.clampProgressValue(task.getPercentComplete());
			if (workProgress > 0.0d || percentComplete <= 0.0d)
				return workProgress;
			return percentComplete;
		}
		return schedule == null ? 0.0d : DisplayMath.clampProgressValue(schedule.getPercentComplete());
	}

	public static double ratioForObject(Object impl) {
		return ratio(impl instanceof Schedule ? (Schedule)impl : null, impl);
	}

	public static boolean hasVisibleProgress(Object impl) {
		return ratioForObject(impl) > 0.0d;
	}

	public static long progressDate(long start, long end, double progressRatio, long referenceDate) {
		double progress = DisplayMath.clampProgressValue(progressRatio);
		if (referenceDate != 0L && progress >= 1.0d && end <= referenceDate)
			return referenceDate;
		if (referenceDate != 0L && progress <= 0.0d && start >= referenceDate)
			return referenceDate;
		return start + Math.round((end - start) * progress);
	}
}
