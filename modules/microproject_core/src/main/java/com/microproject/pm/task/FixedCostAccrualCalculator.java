/*******************************************************************************
 * MIT License
 *
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
package com.microproject.pm.task;

import com.microproject.pm.calendar.WorkCalendar;
import com.microproject.pm.costing.Accrual;

/** Computes a task's fixed-cost amount within a requested schedule interval. */
final class FixedCostAccrualCalculator {
	private FixedCostAccrualCalculator() {
	}

	static double calculate(Accrual.Kind accrual, double fixedCost, long taskStart, long taskEnd,
		long taskDuration, WorkCalendar calendar, long intervalStart, long intervalEnd) {
		if (accrual == Accrual.Kind.START)
			return taskStart >= intervalStart && taskStart <= intervalEnd ? fixedCost : 0.0D;

		if (accrual == Accrual.Kind.PRORATED) {
			long overlapStart = Math.max(intervalStart, taskStart);
			long overlapEnd = Math.min(intervalEnd, taskEnd);
			if (overlapStart >= overlapEnd)
				return 0.0D;

			long overlappingDuration = calendar.compare(overlapEnd, overlapStart, false);
			double fraction = ((double) overlappingDuration) / taskDuration;
			return fixedCost * fraction;
		}

		return taskEnd >= intervalStart && taskEnd <= intervalEnd ? fixedCost : 0.0D;
	}
}
