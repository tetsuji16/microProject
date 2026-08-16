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
package com.microproject.util;

import com.microproject.pm.scheduling.Schedule;
import com.microproject.pm.task.TaskSpecificFields;

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
