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
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.WeakHashMap;

import org.junit.jupiter.api.Test;

import com.microproject.datatype.Duration;
import com.microproject.util.DateTime;

class CalendarDefinitionTest {
	@Test
	void globalScheduleCacheRegistryUsesWeakKeys() throws NoSuchFieldException {
		assertEquals(WeakHashMap.class,
				CalendarDefinition.class.getDeclaredField("cachedInstances").getType());
	}

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
	void rangedExceptionAppliesToEveryCoveredCalendarDay() {
		CalendarDefinition calendar = standardWeekCalendar();
		long start = DateTime.calendarInstance(2024, Calendar.JUNE, 4).getTimeInMillis();
		long end = DateTime.calendarInstance(2024, Calendar.JUNE, 5).getTimeInMillis();
		calendar.addOrReplaceException(nonWorkingRange(start, end));
		calendar.addSentinelsAndMakeArray();

		assertFalse(calendar.getWorkDay(timestamp(2024, Calendar.JUNE, 4, 12)).isWorking());
		assertFalse(calendar.getWorkDay(timestamp(2024, Calendar.JUNE, 5, 12)).isWorking());
		assertTrue(calendar.getWorkDay(timestamp(2024, Calendar.JUNE, 6, 12)).isWorking());
	}

	@Test
	void schedulingSkipsEntireRangedNonWorkingException() {
		CalendarDefinition calendar = standardWeekCalendar();
		long start = DateTime.calendarInstance(2024, Calendar.JUNE, 4).getTimeInMillis();
		long end = DateTime.calendarInstance(2024, Calendar.JUNE, 5).getTimeInMillis();
		calendar.addOrReplaceException(nonWorkingRange(start, end));
		calendar.addSentinelsAndMakeArray();

		long mondayAtNine = timestamp(2024, Calendar.JUNE, 3, 9);
		assertEquals(timestamp(2024, Calendar.JUNE, 6, 9), calendar.add(mondayAtNine, eightHours(), true));
		assertEquals(eightHours(), calendar.compare(timestamp(2024, Calendar.JUNE, 6, 9), mondayAtNine, false));
	}

	@Test
	void datedWorkWeekPatternDrivesSchedulingAndDayExceptionsRemainMoreSpecific() {
		CalendarDefinition calendar = standardWeekCalendar();
		WorkWeek datedWeek = new WorkWeek();
		for (int day = 0; day < WorkWeek.DAYS_IN_WEEK; day++)
			datedWeek.setWeekDay(day, nonWorkingDay(0L));
		datedWeek.setWeekDay(Calendar.TUESDAY - 1, copyOf(WorkDay.getDefaultWorkDay()));
		datedWeek.setWeekDay(Calendar.THURSDAY - 1, copyOf(WorkDay.getDefaultWorkDay()));
		long monday = DateTime.calendarInstance(2024, Calendar.JUNE, 3).getTimeInMillis();
		long sunday = DateTime.calendarInstance(2024, Calendar.JUNE, 9).getTimeInMillis();
		calendar.addOrReplaceWorkWeekPeriod(new WorkWeekPeriod("Summer shift", monday, sunday, datedWeek));

		assertFalse(calendar.getWorkDay(timestamp(2024, Calendar.JUNE, 3, 12)).isWorking());
		assertTrue(calendar.getWorkDay(timestamp(2024, Calendar.JUNE, 4, 12)).isWorking());
		assertTrue(calendar.getWorkDay(timestamp(2024, Calendar.JUNE, 6, 12)).isWorking());
		assertTrue(calendar.getWorkDay(timestamp(2024, Calendar.JUNE, 10, 12)).isWorking(),
				"The default weekly pattern resumes after the dated Work Week ends");

		long mondayAtNine = timestamp(2024, Calendar.JUNE, 3, 9);
		assertEquals(timestamp(2024, Calendar.JUNE, 4, 17), calendar.add(mondayAtNine, eightHours(), true));
		assertEquals(eightHours(), calendar.compare(timestamp(2024, Calendar.JUNE, 4, 17), mondayAtNine, false));

		WorkDay holiday = nonWorkingDay(DateTime.calendarInstance(2024, Calendar.JUNE, 4).getTimeInMillis());
		calendar.addOrReplaceException(holiday);
		assertFalse(calendar.getWorkDay(timestamp(2024, Calendar.JUNE, 4, 12)).isWorking(),
				"A specific calendar exception must override its active dated work-week pattern");
	}

	@Test
	void datedWorkWeekIsCopiedIntoScratchCalendarsWithoutAliasing() {
		WorkingCalendar source = WorkingCalendar.getStandardBasedInstance();
		WorkWeek pattern = new WorkWeek();
		for (int day = 0; day < WorkWeek.DAYS_IN_WEEK; day++) pattern.setWeekDay(day, nonWorkingDay(0L));
		long start = DateTime.calendarInstance(2024, Calendar.JUNE, 3).getTimeInMillis();
		long end = DateTime.calendarInstance(2024, Calendar.JUNE, 9).getTimeInMillis();
		source.addOrReplaceWorkWeekPeriod(new WorkWeekPeriod("Temporary", start, end, pattern));

		WorkingCalendar scratch = source.makeScratchCopy();
		assertEquals(1, scratch.getEffectiveWorkWeekPeriods().size());
		assertFalse(scratch.getMonthDayDescriptor(timestamp(2024, Calendar.JUNE, 3, 12)).isWorking());
		WorkWeekPeriod exposedCopy = scratch.getWorkWeekPeriods().getFirst();
		exposedCopy.setName("Detached edit");
		assertEquals("Temporary", scratch.getWorkWeekPeriods().getFirst().getName(),
			"Public getters must not expose mutable periods owned by the calendar");
	}

	@Test
	void cloneKeepsCalendarExceptionsDeeplyIndependent() throws CloneNotSupportedException {
		CalendarDefinition original = standardWeekCalendar();
		WorkDay exception = new WorkDay(timestamp(2024, Calendar.JUNE, 8, 0));
		exception.setWorkingHours(WorkingHours.getDefault().clone());
		original.addOrReplaceException(exception);

		CalendarDefinition copy = original.clone();
		WorkDay copiedException = copy.dayExceptions.first();
		assertNotSame(exception, copiedException);
		assertNotSame(exception.getWorkingHours(), copiedException.getWorkingHours());
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
	void addWithNonWorkingWeekAndWorkingExceptionSchedulesAsElapsedTime() {
		// Issue #175: a week with no working weekdays is degenerate even when an
		// exception day carries working time - the fine-tuning walk would loop
		// forever when the schedule direction never reaches that exception. It
		// must degrade to elapsed-time arithmetic instead of hanging.
		CalendarDefinition calendar = new CalendarDefinition();
		for (int day = 0; day < WorkWeek.DAYS_IN_WEEK; day++) {
			calendar.week.setWeekDay(day, nonWorkingDay(0L));
		}
		WorkDay workingException = new WorkDay(timestamp(2024, Calendar.JUNE, 1, 0));
		workingException.setWorkingHours(WorkingHours.getDefault().clone());
		workingException.initialize();
		calendar.addOrReplaceException(workingException);
		calendar.addSentinelsAndMakeArray();
		assertTrue(workingException.getDuration() > 0); // the exception really carries working time

		long start = timestamp(2024, Calendar.JUNE, 3, 9); // after the working exception
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

	private static WorkDay nonWorkingRange(long start, long end) {
		return new WorkDay(start, end);
	}

	private static WorkDay copyOf(WorkDay workDay) {
		return workDay.clone();
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
