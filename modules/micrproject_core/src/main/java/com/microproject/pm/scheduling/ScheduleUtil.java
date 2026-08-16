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
package com.microproject.pm.scheduling;

import com.microproject.datatype.Duration;
import com.microproject.grouping.core.summaries.DivisionSummaryVisitor;

public class ScheduleUtil {
	
	/**
	 * Return a closure that will compute the percent complete using weighted values
	 * @return
	 */
	public static DivisionSummaryVisitor percentCompleteClosureInstance(final boolean nodeBased) {
		return new DivisionSummaryVisitor(nodeBased) {
			public double getNumerator(Object impl) {
				if (impl instanceof Schedule) {
					Schedule schedule = ((Schedule)impl);
					return schedule.getPercentComplete() * Duration.millis(schedule.getDuration());
				}
				return 0;
			}
			public double getDenominator(Object impl) {
				if (impl instanceof Schedule) {
					Schedule schedule = ((Schedule)impl);
					return Duration.millis(schedule.getDuration());
				}
				return 0;
			}
		};
		
	}
	
	public static void setComplete(Schedule s,boolean complete) {
		if (complete)
			s.setPercentComplete(1.0D);
	}

}
