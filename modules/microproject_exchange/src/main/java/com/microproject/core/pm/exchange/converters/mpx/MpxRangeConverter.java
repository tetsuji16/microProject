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
package com.microproject.core.pm.exchange.converters.mpx;

import com.microproject.core.pm.exchange.converters.type.DateHoursMinsConverter;
import com.microproject.pm.calendar.WorkRange;
import com.microproject.pm.calendar.WorkRangeException;
import com.microproject.pm.calendar.WorkingHours;

import net.sf.mpxj.DateRange;
import net.sf.mpxj.ProjectCalendarHours;

/**
 * Builds a microproject WorkingHours representation from MPXJ calendar hours.
 * Copies every MPXJ time range into the model's normalized time-of-day ranges.
 * @author Laurent Chretienneau
 */
public class MpxRangeConverter {

	public void from(ProjectCalendarHours mpxRange, WorkRange range) {
		throw new UnsupportedOperationException("Use from(ProjectCalendarHours) to preserve all calendar ranges");
	}

	public WorkingHours from(ProjectCalendarHours mpxHours) {
		WorkingHours hours = new WorkingHours();
		if (mpxHours == null)
			return hours;
		if (mpxHours.size() > 5)
			throw new IllegalArgumentException("MSP calendar has more working intervals than the model supports: "
					+ mpxHours.size());
		DateHoursMinsConverter converter = new DateHoursMinsConverter();
		int targetIndex = 0;
		for (int i = 0; i < mpxHours.size(); i++) {
			DateRange range = mpxHours.get(i);
			if (range == null || range.getStart() == null || range.getEnd() == null)
				continue;
			long start = (Long) converter.from(range.getStart());
			long end = (Long) converter.from(range.getEnd());
			try {
				// A range crossing midnight belongs to two calendar dates. The
				// current model cannot represent that relation in one weekday;
				// retain the pre-existing standard-day fallback rather than
				// silently assigning the hours to the wrong date.
				if (end > 0 && end < start)
					return WorkingHours.getDefault();
				hours.setInterval(targetIndex++, start, end);
			} catch (WorkRangeException error) {
				throw new IllegalArgumentException("Invalid MSP working interval " + start + "-" + end, error);
			}
		}
		return hours;
	}
}
