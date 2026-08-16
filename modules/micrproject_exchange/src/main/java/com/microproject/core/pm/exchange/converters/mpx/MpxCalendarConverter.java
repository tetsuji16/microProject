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

import com.microproject.core.time.TimeUtil;
import com.microproject.exchange.ImportedCalendarService;
import com.microproject.pm.calendar.WorkCalendar;
import com.microproject.pm.calendar.WorkDay;
import com.microproject.pm.calendar.WorkingCalendar;

import net.sf.mpxj.Day;
import net.sf.mpxj.ProjectCalendar;
import net.sf.mpxj.ProjectCalendarException;
import net.sf.mpxj.ProjectCalendarHours;

/**
 * Converts an MPXJ ProjectCalendar into a microproject WorkingCalendar.
 * Exact per-day working hours are collapsed to the standard working-day template
 * (see issue #154); calendar exceptions are carried as date-bounded WorkDay
 * entries.
 * @author Laurent Chretienneau
 */
public class MpxCalendarConverter {
	public void from(ProjectCalendar mpxCalendar, WorkingCalendar calendar, MpxImportState state){
		calendar.setName(mpxCalendar.getName());
		calendar.setId(mpxCalendar.getUniqueID());

		// base calendar
		WorkingCalendar standardCalendar = WorkingCalendar.getStandardBasedInstance();
		WorkingCalendar baseCalendar = null;
		if (mpxCalendar.isDerived()) {
			ProjectCalendar mpxBaseCalendar = mpxCalendar.getParent();
			if (mpxBaseCalendar != null) {
				baseCalendar = (WorkingCalendar) state.getImportedCalendar(mpxBaseCalendar);
			}
			if (baseCalendar == null)
				baseCalendar = standardCalendar;
			try {
				calendar.setBaseCalendar(baseCalendar);
			} catch (com.microproject.configuration.CircularDependencyException e) {
				// ignore: keep unbased calendar
			}
		}

		// work weeks
		for (int i = 0; i < 7; i++) {
			Day mpxDayId = Day.getInstance(i + 1);
			ProjectCalendarHours mpxDay = mpxCalendar.getCalendarHours(mpxDayId);
			net.sf.mpxj.DayType mpxDayType = mpxCalendar.getDayType(mpxDayId);
			WorkDay day = null;
			if (mpxDay == null) {
				if (mpxCalendar.isDerived() && baseCalendar != null) {
					if (mpxDayType == net.sf.mpxj.DayType.DEFAULT)
						day = baseCalendar.getWeekDay(i);
					else if (mpxBaseCalendarIsWorking(mpxCalendar, mpxDayId))
						day = WorkDay.getNonWorkingDay();
				}
			} else {
				if (mpxDayType == net.sf.mpxj.DayType.WORKING) {
					day = WorkDay.getDefaultWorkDay();
				} else {
					day = WorkDay.getNonWorkingDay();
				}
			}
			if (day != null)
				calendar.setWeekDay(i, day);
		}

		// exceptions
		MpxExceptionConverter exceptionConverter = new MpxExceptionConverter();
		for (ProjectCalendarException mpxException : mpxCalendar.getCalendarExceptions()) {
			long from = TimeUtil.removeTimeZoneOffset(mpxException.getFromDate().getTime());
			long to = TimeUtil.removeTimeZoneOffset(mpxException.getToDate().getTime());
			WorkDay exception = new WorkDay(from, to);
			exceptionConverter.from(mpxException, exception);
			calendar.addOrReplaceException(exception);
		}
	}

	private static boolean mpxBaseCalendarIsWorking(ProjectCalendar mpxCalendar, Day day) {
		try {
			return mpxCalendar.isWorkingDay(day);
		} catch (Exception e) {
			return false;
		}
	}
}
