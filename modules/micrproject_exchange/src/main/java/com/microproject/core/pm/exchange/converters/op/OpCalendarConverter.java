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

import java.util.logging.Level;
import java.util.logging.Logger;

import com.microproject.pm.calendar.WorkCalendar;
import com.microproject.pm.calendar.WorkDay;
import com.microproject.pm.calendar.WorkWeek;
import com.microproject.configuration.CircularDependencyException;
import com.microproject.pm.calendar.CalendarService;
import com.microproject.pm.calendar.WorkingCalendar;

/**
 * Copies a microproject WorkCalendar into a microproject WorkingCalendar (the .pod
 * (de)serialization path). Both sides use the same microproject model, so this is a
 * direct copy. Exact working hours are simplified to the default working day (see
 * issue #154).
 * @author Laurent Chretienneau
 */
public class OpCalendarConverter {
	private static final Logger logger = Logger.getLogger(OpCalendarConverter.class.getName());

	public void to(WorkingCalendar opCalendar, WorkCalendar calendar, OpImportState state){
		WorkingCalendar src;
		if (calendar instanceof WorkingCalendar)
			src = (WorkingCalendar) calendar;
		else
			src = CalendarService.getInstance().getStandardBasedInstance();

		if (calendar.getName() != null)
			opCalendar.setName(calendar.getName());

		//base calendar
		WorkingCalendar opStandardCalendar = CalendarService.getInstance().getStandardInstance();
		WorkingCalendar baseCalendar = (WorkingCalendar) src.getBaseCalendar();
		if (baseCalendar == null)
			baseCalendar = opStandardCalendar;
		try {
			opCalendar.setBaseCalendar(baseCalendar);
		} catch (CircularDependencyException e) {
			logger.log(Level.WARNING, "Failed to set calendar base", e);
		}

		//work weeks
		WorkDay day;
		com.microproject.pm.calendar.WorkDay opDay;
		for (int i = 0; i < 7; i++){
			day = src.getWeekDay(i);
			if (day == null || !day.isWorking())
				opDay = com.microproject.pm.calendar.WorkDay.getNonWorkingDay();
			else {
				opDay = com.microproject.pm.calendar.WorkDay.getDefaultWorkDay();
				if (opStandardCalendar.getWeekDay(i).hasSameWorkHours(opDay))
					opDay = null;
			}
			opCalendar.setWeekDay(i, opDay);
		}

		//exceptions
		for (WorkDay exception : src.getExceptionDays()) {
			com.microproject.pm.calendar.WorkDay opExceptionDay =
					new com.microproject.pm.calendar.WorkDay(exception.getStart(), exception.getEnd());
			opCalendar.addOrReplaceException(opExceptionDay);
		}

		opCalendar.removeEmptyDays();
		for (int i = 0; i < 7; i++) {
			logger.log(Level.FINE, "Calendar weekday {0}: {1}", new Object[] { i, opCalendar.getWeekDay(i) });
		}
	}
}
