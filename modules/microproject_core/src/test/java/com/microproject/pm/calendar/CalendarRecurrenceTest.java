/*
 * MIT License
 * Copyright (c) 2026 microProject
 */
package com.microproject.pm.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.BitSet;
import java.util.Calendar;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.util.DateTime;

/**
 * Microsoft documents daily, weekly, monthly, and yearly exception recurrence,
 * plus recurrence ranges in Project Desktop's calendar exception workflow:
 * https://support.microsoft.com/en-us/project/add-a-holiday-to-the-project-calendar
 * The XML fields are specified at:
 * https://learn.microsoft.com/en-us/office-project/xml-data-interchange/exception-element?view=project-client-2016
 * The absent-month-day clamp is a document-derived decision cross-checked
 * against MPXJ dates, not an empirical MSP-client result.
 */
class CalendarRecurrenceTest {
	@Test
	void dailyWeekdaysOnlyCountsGeneratedWorkingDatesThroughAnOccurrenceLimit() {
		var recurrence = CalendarRecurrence.daily(day(2024, Calendar.JUNE, 7), 1, true,
			CalendarRecurrence.EndMode.AFTER_OCCURRENCES, 0, 3);

		assertEquals(List.of(day(2024, Calendar.JUNE, 7), day(2024, Calendar.JUNE, 10), day(2024, Calendar.JUNE, 11)),
			recurrence.occurrenceDates());
	}

	@Test
	void weeklyRuleHonorsSelectedDaysAndEndDate() {
		BitSet days = new BitSet(8);
		days.set(CalendarRecurrenceIso.MONDAY);
		days.set(CalendarRecurrenceIso.WEDNESDAY);
		var recurrence = CalendarRecurrence.weekly(day(2024, Calendar.JUNE, 4), 1, days,
			CalendarRecurrence.EndMode.BY_DATE, day(2024, Calendar.JUNE, 12), 0);

		assertEquals(List.of(day(2024, Calendar.JUNE, 5), day(2024, Calendar.JUNE, 10), day(2024, Calendar.JUNE, 12)),
			recurrence.occurrenceDates());
	}

	@Test
	void monthlyAndYearlyRulesSupportLastWeekdayAndClampMissingMonthDays() {
		var monthly = CalendarRecurrence.monthly(day(2024, Calendar.JANUARY, 26), 1, true,
			1, -1, CalendarRecurrenceIso.FRIDAY,
			CalendarRecurrence.EndMode.AFTER_OCCURRENCES, 0, 3);
		assertEquals(List.of(day(2024, Calendar.JANUARY, 26), day(2024, Calendar.FEBRUARY, 23),
			day(2024, Calendar.MARCH, 29)), monthly.occurrenceDates());

		var yearly = CalendarRecurrence.yearly(day(2024, Calendar.FEBRUARY, 29), 1, false,
			Calendar.FEBRUARY + 1, 29, 1, 1,
			CalendarRecurrence.EndMode.AFTER_OCCURRENCES, 0, 2);
		assertEquals(List.of(day(2024, Calendar.FEBRUARY, 29), day(2025, Calendar.FEBRUARY, 28)),
			yearly.occurrenceDates());
	}

	@Test
	void recurringExceptionExpandsIntoCalendarWorkdaysAndSurvivesCalendarClone() {
		long start = day(2024, Calendar.JANUARY, 1);
		CalendarRecurrence recurrence = CalendarRecurrence.yearly(start, 1, false,
			Calendar.JANUARY + 1, 1, 1, 1, CalendarRecurrence.EndMode.AFTER_OCCURRENCES, 0, 3);
		WorkDay template = nonWorkingDay(start, "New Year");
		CalendarDefinition calendar = standardWeekCalendar();
		calendar.addOrReplaceRecurringException(new RecurringCalendarException(template, recurrence));
		calendar.addSentinelsAndMakeArray();

		assertFalse(calendar.getWorkDay(day(2024, Calendar.JANUARY, 1)).isWorking());
		assertFalse(calendar.getWorkDay(day(2025, Calendar.JANUARY, 1)).isWorking());
		assertFalse(calendar.getWorkDay(day(2026, Calendar.JANUARY, 1)).isWorking());
		assertTrue(calendar.getWorkDay(day(2027, Calendar.JANUARY, 1)).isWorking());
		CalendarDefinition copy;
		try {
			copy = (CalendarDefinition) calendar.clone();
		} catch (CloneNotSupportedException impossible) {
			throw new AssertionError(impossible);
		}
		copy.addSentinelsAndMakeArray();
		assertFalse(copy.getWorkDay(day(2026, Calendar.JANUARY, 1)).isWorking());
	}

	@Test
	void invalidOrUnboundedRecurrenceIsRejected() {
		assertThrows(IllegalArgumentException.class, () -> CalendarRecurrence.daily(day(2024, 0, 1), 0, false,
			CalendarRecurrence.EndMode.AFTER_OCCURRENCES, 0, 1));
		assertThrows(IllegalArgumentException.class, () -> CalendarRecurrence.weekly(day(2024, 0, 1), 1,
			new BitSet(), CalendarRecurrence.EndMode.BY_DATE, day(2024, 1, 1), 0));
		assertThrows(IllegalArgumentException.class, () -> CalendarRecurrence.monthly(day(2024, 0, 1), 1, false,
			32, 1, 1, CalendarRecurrence.EndMode.BY_DATE, day(2024, 1, 1), 0));
	}

	private static long day(int year, int month, int day) {
		return DateTime.calendarInstance(year, month, day).getTimeInMillis();
	}

	private static WorkDay nonWorkingDay(long day, String name) {
		WorkDay result = new WorkDay(day, day, name);
		result.setWorkingHours(new WorkingHours());
		return result;
	}

	private static CalendarDefinition standardWeekCalendar() {
		CalendarDefinition calendar = new CalendarDefinition();
		for (int day = 0; day < 7; day++)
			calendar.week.setWeekDay(day, day == Calendar.SATURDAY - 1 || day == Calendar.SUNDAY - 1
				? WorkDay.getNonWorkingDay() : WorkDay.getDefaultWorkDay());
		calendar.addSentinelsAndMakeArray();
		return calendar;
	}

	/** Recurrence uses ISO weekday numbers, unlike java.util.Calendar's Sunday-first constants. */
	private static final class CalendarRecurrenceIso {
		private static final int MONDAY = 1;
		private static final int WEDNESDAY = 3;
		private static final int FRIDAY = 5;
	}
}
