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
package com.microproject.core.pm.exchange.converters.mpx;

import com.microproject.exchange.ImportedCalendarService;
import com.microproject.pm.calendar.WorkCalendar;
import com.microproject.pm.calendar.WorkDay;
import com.microproject.pm.calendar.WorkingCalendar;
import com.microproject.pm.calendar.WorkingHours;
import com.microproject.pm.calendar.WorkWeek;
import com.microproject.pm.calendar.WorkWeekPeriod;
import com.microproject.pm.calendar.RecurringCalendarException;
import com.microproject.util.DateTime;

import org.mpxj.ProjectCalendar;
import org.mpxj.ProjectCalendarException;
import org.mpxj.ProjectCalendarHours;
import org.mpxj.ProjectCalendarWeek;

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
			java.time.DayOfWeek mpxDayId = MpxDates.toDayOfWeek(i + 1);
			ProjectCalendarHours mpxDay = mpxCalendar.getCalendarHours(mpxDayId);
			org.mpxj.DayType mpxDayType = mpxCalendar.getCalendarDayType(mpxDayId);
			WorkDay day = declaredDay(mpxDayType, mpxDay, rangeConverter);
			if (day == null && mpxDayType == org.mpxj.DayType.WORKING
					&& mpxCalendar.isDerived() && baseCalendar != null) {
				WorkDay inheritedDay = baseCalendar.getConcreteInstance().getWeekDay(i);
				if (inheritedDay == null || !inheritedDay.isWorking()) {
					ProjectCalendarHours effectiveHours = mpxCalendar.getHours(mpxDayId);
					WorkDay effectiveDay = declaredDay(mpxDayType, effectiveHours, rangeConverter);
					if (effectiveDay != null && effectiveDay.isWorking()) day = effectiveDay;
				}
			}
			if (day != null)
				calendar.setWeekDay(i, day);
		}
		for (ProjectCalendarWeek mpxWeek : mpxCalendar.getWorkWeeks()) {
			if (mpxWeek.getDateRange() == null || mpxWeek.getDateRange().getStart() == null
					|| mpxWeek.getDateRange().getEnd() == null) continue;
			long start = MpxCalendarDateConverter.toModelDay(MpxDates.toDate(mpxWeek.getDateRange().getStart()));
			long end = MpxCalendarDateConverter.toModelDay(MpxDates.toDate(mpxWeek.getDateRange().getEnd()));
			WorkWeek week = new WorkWeek();
			for (int i = 0; i < 7; i++) {
				java.time.DayOfWeek dayId = MpxDates.toDayOfWeek(i + 1);
				ProjectCalendarHours hours = mpxWeek.getCalendarHours(dayId);
				org.mpxj.DayType dayType = mpxWeek.getCalendarDayType(dayId);
				WorkDay declared = declaredDay(dayType, hours, rangeConverter);
				if (declared != null) {
					week.setWeekDay(i, declared);
				} else if (dayType == org.mpxj.DayType.WORKING) {
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
			// MPXJ represents calendar dates using the host-local calendar day;
			// map the date fields back to the model's UTC day keys explicitly.
			long from = recurrence == null
				? MpxCalendarDateConverter.toModelDay(MpxDates.toDate(mpxException.getFromDate()))
				: recurrence.getStartDate();
			// For recurring MPXJ exceptions getToDate() is the final recurrence
			// occurrence, not the duration of one occurrence. Calendar exceptions
			// selected from MSP's date grid are one-day events.
			long to = recurrence != null || mpxException.getToDate() == null ? from
				: MpxCalendarDateConverter.toModelDay(MpxDates.toDate(mpxException.getToDate()));
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

	private WorkDay declaredDay(org.mpxj.DayType dayType, ProjectCalendarHours hours,
		MpxRangeConverter rangeConverter) {
		if (dayType == org.mpxj.DayType.NON_WORKING) return WorkDay.getNonWorkingDay();
		if (hours == null) return null;
		WorkingHours workingHours = rangeConverter.from(hours);
		if (workingHours.getDuration() <= 0) return WorkDay.getNonWorkingDay();
		WorkDay day = new WorkDay();
		day.setWorkingHours(workingHours);
		return day;
	}

}
