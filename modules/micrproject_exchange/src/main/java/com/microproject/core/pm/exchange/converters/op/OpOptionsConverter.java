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
package com.microproject.core.pm.exchange.converters.op;

import java.util.GregorianCalendar;

import com.microproject.pm.calendar.CalendarOptions;

/**
 * @author Laurent Chretienneau
 *
 */
public class OpOptionsConverter {
	public static void to(com.microproject.options.CalendarOption opCalendarOptions, CalendarOptions calendarOptions, OpImportState state) {
		opCalendarOptions.setHoursPerDay(calendarOptions.getHoursPerDay());
		opCalendarOptions.setHoursPerWeek(calendarOptions.getHoursPerWeek());
		opCalendarOptions.setDaysPerMonth(calendarOptions.getDaysPerMonth());
		if (calendarOptions.getDefaultStart()>0){
			GregorianCalendar defaultStart = new GregorianCalendar();
			defaultStart.setTimeInMillis(calendarOptions.getDefaultStart());
			opCalendarOptions.setDefaultStartTime(defaultStart);			
		}
		if (calendarOptions.getDefaultEnd()>0){
			GregorianCalendar defaultEnd = new GregorianCalendar();
			defaultEnd.setTimeInMillis(calendarOptions.getDefaultEnd());
			opCalendarOptions.setDefaultEndTime(defaultEnd);			
		}
	}
}
