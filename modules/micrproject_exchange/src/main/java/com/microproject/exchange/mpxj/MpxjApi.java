package com.microproject.exchange.mpxj;

import net.sf.mpxj.TaskType;
import net.sf.mpxj.WorkContour;

/**
 * Keeps MPXJ API version details out of ProjectLibre's scheduling model.
 */
public final class MpxjApi {
	private static final TaskType[] TASK_TYPES = {
		TaskType.FIXED_UNITS,
		TaskType.FIXED_DURATION,
		TaskType.FIXED_WORK
	};
	private static final WorkContour[] WORK_CONTOURS = {
		WorkContour.FLAT,
		WorkContour.BACK_LOADED,
		WorkContour.FRONT_LOADED,
		WorkContour.DOUBLE_PEAK,
		WorkContour.EARLY_PEAK,
		WorkContour.LATE_PEAK,
		WorkContour.BELL,
		WorkContour.TURTLE,
		WorkContour.CONTOURED
	};

	private MpxjApi() {
	}

	public static int schedulingTypeId(TaskType type) {
		for (int index = 0; index < TASK_TYPES.length; index++) {
			if (TASK_TYPES[index] == type)
				return index;
		}
		return 2;
	}

	public static TaskType taskType(int id) {
		return id >= 0 && id < TASK_TYPES.length ? TASK_TYPES[id] : TaskType.FIXED_WORK;
	}

	public static WorkContour workContour(int id) {
		return id >= 0 && id < WORK_CONTOURS.length ? WORK_CONTOURS[id] : WorkContour.FLAT;
	}
}
