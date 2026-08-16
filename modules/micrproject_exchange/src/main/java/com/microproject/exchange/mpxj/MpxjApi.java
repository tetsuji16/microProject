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
