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
import com.microproject.pm.calendar.WorkingHours;
import com.microproject.pm.calendar.WorkWeek;
import com.microproject.pm.calendar.WorkWeekPeriod;
import com.microproject.pm.calendar.RecurringCalendarException;
import com.microproject.util.DateTime;

import net.sf.mpxj.Day;
import net.sf.mpxj.ProjectCalendar;
import net.sf.mpxj.ProjectCalendarException;
import net.sf.mpxj.ProjectCalendarHours;
import net.sf.mpxj.ProjectCalendarWeek;

/**
 * Converts an MPXJ ProjectCalendar into a microproject WorkingCalendar.
 * Per-weekday MSP working ranges and date-bounded calendar exceptions are
 * copied into the WorkingCalendar without collapsing them to a standard template.
 * @author Laurent Chretienneau
 */
public class MpxCalendarConverter {
	public void from(ProjectCalendar mpxCalendar, WorkingCalendar calendar, MpxImportState state){
		java.util.IdentityHashMap<ProjectCalendarException, com.microproject.pm.calendar.CalendarRecurrence> recurrenceRules
			= new java.util.IdentityHashMap<>();
		for (ProjectCalendarException mpxException : mpxCalendar.getCalendarExceptions()) {
			if (mpxException.getRecurring() != null) {
				recurrenceRules.put(mpxException, new MpxCalendarRecurrenceConverter().from(mpxException.getRecurring()));
			}
		}
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
		MpxRangeConverter rangeConverter = new MpxRangeConverter();
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
				WorkingHours hours = rangeConverter.from(mpxDay);
				day = hours.getDuration() > 0 ? new WorkDay() : WorkDay.getNonWorkingDay();
				if (hours.getDuration() > 0)
					day.setWorkingHours(hours);
			}
			if (day != null)
				calendar.setWeekDay(i, day);
		}
		for (ProjectCalendarWeek mpxWeek : mpxCalendar.getWorkWeeks()) {
			if (mpxWeek.getDateRange() == null || mpxWeek.getDateRange().getStart() == null
					|| mpxWeek.getDateRange().getEnd() == null) continue;
			long start = DateTime.dayFloor(TimeUtil.addTimeZoneOffset(mpxWeek.getDateRange().getStart().getTime()));
			long end = DateTime.dayFloor(TimeUtil.addTimeZoneOffset(mpxWeek.getDateRange().getEnd().getTime()));
			WorkWeek week = new WorkWeek();
			for (int i = 0; i < 7; i++) {
				Day dayId = Day.getInstance(i + 1);
				ProjectCalendarHours hours = mpxWeek.getCalendarHours(dayId);
				net.sf.mpxj.DayType dayType = mpxWeek.getCalendarDayType(dayId);
				if (dayType == net.sf.mpxj.DayType.NON_WORKING) {
					week.setWeekDay(i, WorkDay.getNonWorkingDay());
				} else if (hours != null) {
					WorkingHours workingHours = rangeConverter.from(hours);
					WorkDay workDay = workingHours.getDuration() > 0 ? new WorkDay() : WorkDay.getNonWorkingDay();
					if (workingHours.getDuration() > 0) workDay.setWorkingHours(workingHours);
					week.setWeekDay(i, workDay);
				} else if (dayType == net.sf.mpxj.DayType.WORKING) {
					week.setWeekDay(i, new WorkDay());
				}
			}
			calendar.addOrReplaceWorkWeekPeriod(new WorkWeekPeriod(
				mpxWeek.getName() == null || mpxWeek.getName().isBlank() ? "Work week" : mpxWeek.getName(),
				start, end, week));
		}

		// exceptions
		MpxExceptionConverter exceptionConverter = new MpxExceptionConverter();
		for (ProjectCalendarException mpxException : mpxCalendar.getCalendarExceptions()) {
			com.microproject.pm.calendar.CalendarRecurrence recurrence = recurrenceRules.get(mpxException);
			// MPXJ exposes calendar exception boundaries as local-midnight dates.
			// Convert those local dates to the core's UTC day keys; subtracting the
			// offset moved Japanese/negative-offset locales onto the previous day.
			long from = recurrence == null
				? DateTime.dayFloor(TimeUtil.addTimeZoneOffset(mpxException.getFromDate().getTime()))
				: recurrence.getStartDate();
			// For recurring MPXJ exceptions getToDate() is the final recurrence
			// occurrence, not the duration of one occurrence. Calendar exceptions
			// selected from MSP's date grid are one-day events.
			long to = recurrence != null || mpxException.getToDate() == null ? from
				: DateTime.dayFloor(TimeUtil.addTimeZoneOffset(mpxException.getToDate().getTime()));
			WorkDay exception = new WorkDay(from, to, mpxException.getName());
			exception.setWorkingHours(new WorkingHours());
			exceptionConverter.from(mpxException, exception);
			if (recurrence == null) {
				calendar.addOrReplaceException(exception);
			} else {
				calendar.addOrReplaceRecurringException(new RecurringCalendarException(exception, recurrence));
			}
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
