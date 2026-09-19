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
import com.microproject.pm.scheduling.ScheduleInterval;

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
