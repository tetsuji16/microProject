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
package com.microproject.pm.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Calendar;

import org.junit.jupiter.api.Test;

import com.microproject.datatype.Duration;
import com.microproject.util.DateTime;

class CalendarDefinitionTest {
	@Test
	void calendarDefinitionExposesIdentityAndValidity() {
		CalendarDefinition calendar = standardWeekCalendar();

		assertEquals("CalendarDefinition", calendar.getName());
		assertEquals(WorkCalendar.CALENDAR_CATEGORY, calendar.getCategory());
		assertFalse(calendar.isInvalid());
		assertEquals(null, calendar.getBaseCalendar());
		assertEquals(true, calendar.dependsOn(calendar));

		calendar.setName("Custom");
		assertEquals("Custom", calendar.getName());
		assertThrows(IllegalArgumentException.class, () -> calendar.setName(null));
	}
	@Test
	void addSkipsWeekendAndExceptionDays() {
		CalendarDefinition calendar = standardWeekCalendar();
		calendar.addOrReplaceException(nonWorkingDay(DateTime.calendarInstance(2024, Calendar.JUNE, 4).getTimeInMillis()));

		long start = timestamp(2024, Calendar.JUNE, 3, 9);
		long result = calendar.add(start, eightHours(), true);

		assertEquals(timestamp(2024, Calendar.JUNE, 5, 9), result);
	}

	@Test
	void compareCountsWorkingTimeAcrossExceptionDays() {
		CalendarDefinition calendar = standardWeekCalendar();
		calendar.addOrReplaceException(nonWorkingDay(DateTime.calendarInstance(2024, Calendar.JUNE, 4).getTimeInMillis()));

		long earlier = timestamp(2024, Calendar.JUNE, 3, 9);
		long later = timestamp(2024, Calendar.JUNE, 5, 9);

		assertEquals(eightHours(), calendar.compare(later, earlier, false));
		assertEquals(-eightHours(), calendar.compare(earlier, later, false));
	}

	@Test
	void getWorkDayPrefersExceptionDaysOverWeekdays() {
		CalendarDefinition calendar = standardWeekCalendar();
		WorkDay exception = nonWorkingDay(DateTime.calendarInstance(2024, Calendar.JUNE, 4).getTimeInMillis());
		calendar.addOrReplaceException(exception);

		assertEquals(exception.getStart(), calendar.getWorkDay(timestamp(2024, Calendar.JUNE, 4, 0)).getStart());
		assertEquals(exception.getStart(), calendar.getWorkDay(timestamp(2024, Calendar.JUNE, 4, 9)).getStart());
		assertEquals(exception.getStart(), calendar.getWorkDay(timestamp(2024, Calendar.JUNE, 4, 18)).getStart());
		assertEquals(WorkDay.getDefaultWorkDay().getDuration(), calendar.getWorkDay(timestamp(2024, Calendar.JUNE, 3, 9)).getDuration());
	}

	@Test
	void addWithZeroWorkingTimeWeekDegradesGracefully() {
		// Issue #175: a week with no working time must not divide by zero
		// (ArithmeticException) or walk non-working days forever.
		CalendarDefinition calendar = new CalendarDefinition();
		for (int day = 0; day < WorkWeek.DAYS_IN_WEEK; day++) {
			calendar.week.setWeekDay(day, nonWorkingDay(0L));
		}
		calendar.addSentinelsAndMakeArray();

		long start = timestamp(2024, Calendar.JUNE, 3, 9);
		long result = calendar.add(start, eightHours(), true);

		// Falls back to elapsed-time arithmetic: no crash, deterministic result.
		assertEquals(start + eightHours(), result);
	}

	@Test
	void addWithZeroWorkingTimeWeekIsSignConsistentWithElapsedPath() {
		// Issue #175: a no-working-time calendar must behave exactly like the
		// elapsed-time path, including for negative (reverse-scheduling) dates.
		CalendarDefinition calendar = new CalendarDefinition();
		for (int day = 0; day < WorkWeek.DAYS_IN_WEEK; day++) {
			calendar.week.setWeekDay(day, nonWorkingDay(0L));
		}
		calendar.addSentinelsAndMakeArray();

		long start = -timestamp(2024, Calendar.JUNE, 3, 9);
		long result = calendar.add(start, eightHours(), true);

		assertEquals(start + eightHours(), result);
	}

	@Test
	void addWithUninitializedWeekTreatsNullDaysAsDefaultWorkingDays() {
		// Issue #175: an uninitialized week (all null weekdays) resolves each day
		// to the default working day instead of dividing by the cached zero.
		CalendarDefinition calendar = new CalendarDefinition();
		calendar.addSentinelsAndMakeArray();

		long start = timestamp(2024, Calendar.JUNE, 3, 9);
		long result = calendar.add(start, eightHours(), true);

		// Default 8h working day: Mon 09:00 + 8h -> Tue 09:00.
		assertEquals(timestamp(2024, Calendar.JUNE, 4, 9), result);
	}

	private static CalendarDefinition standardWeekCalendar() {
		CalendarDefinition calendar = new CalendarDefinition();
		for (int day = 0; day < WorkWeek.DAYS_IN_WEEK; day++) {
			calendar.week.setWeekDay(day, nonWorkingDay(0L));
		}
		for (int day = Calendar.MONDAY - 1; day <= Calendar.FRIDAY - 1; day++) {
			calendar.week.setWeekDay(day, copyOf(WorkDay.getDefaultWorkDay()));
		}
		calendar.addSentinelsAndMakeArray();
		return calendar;
	}

	private static WorkDay nonWorkingDay(long day) {
		return new WorkDay(day);
	}

	private static WorkDay copyOf(WorkDay workDay) {
		return (WorkDay) workDay.clone();
	}

	private static long timestamp(int year, int month, int dayOfMonth, int hourOfDay) {
		Calendar calendar = DateTime.calendarInstance(year, month, dayOfMonth);
		calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
		return calendar.getTimeInMillis();
	}

	private static long eightHours() {
		return 8L * 60L * 60L * 1000L;
	}
}
