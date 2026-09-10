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

import net.sf.mpxj.DateRange;
import net.sf.mpxj.ProjectCalendarException;

import com.microproject.core.pm.exchange.converters.type.DateHoursMinsConverter;
import com.microproject.pm.calendar.WorkDay;
import com.microproject.pm.calendar.WorkRangeException;
import com.microproject.pm.calendar.WorkingHours;

/**
 * Converts an MPXJ calendar exception into a microproject WorkDay exception.
 * The exception date bounds are supplied via the WorkDay constructor; this
 * converter copies the exception's working hours, if any.
 * @author Laurent Chretienneau
 */
public class MpxExceptionConverter {

	public void from(ProjectCalendarException mpxException, WorkDay exception) {
		// copy the exception's working hours, if any
		java.util.Date from = mpxException.getFromDate();
		java.util.Date to = mpxException.getToDate();
		if (from != null && to != null) {
			DateHoursMinsConverter converter = new DateHoursMinsConverter();
			WorkingHours workingHours = new WorkingHours();
			// The exception bounds describe the date, not its working interval.
			// Preserve every interval supplied by MSP instead of treating the whole
			// exception as one continuous range.
			int targetIndex = 0;
			for (int i = 0; i < mpxException.size() && targetIndex < 5; i++) {
				DateRange range = mpxException.get(i);
				if (range == null || range.getStart() == null || range.getEnd() == null)
					continue;
				long start = (Long) converter.from(range.getStart());
				long end = (Long) converter.from(range.getEnd());
				try {
					if (end > 0 && end < start)
						continue; // one exception cannot express a cross-midnight day
					workingHours.setInterval(targetIndex++, start, end);
				} catch (WorkRangeException error) {
					throw new IllegalArgumentException("Invalid MSP calendar exception interval", error);
				}
			}
			exception.setWorkingHours(workingHours);
		}
	}
}
