package com.projectlibre1.pm.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Calendar;

import org.junit.jupiter.api.Test;

import com.projectlibre1.datatype.Duration;
import com.projectlibre1.util.DateTime;

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
