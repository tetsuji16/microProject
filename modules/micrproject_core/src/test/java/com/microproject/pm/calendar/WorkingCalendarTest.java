package com.microproject.pm.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;

import org.junit.jupiter.api.Test;

import com.microproject.options.CalendarOption;
import com.microproject.util.DateTime;

class WorkingCalendarTest {
	@Test
	void makeScratchCopyCopiesNameWithoutSharingIdentity() {
		WorkingCalendar original = WorkingCalendar.getInstance();
		original.setName("Original");

		WorkingCalendar copy = original.makeScratchCopy();

		assertNotSame(original, copy);
		assertEquals("Original", copy.getName());
		copy.setName("Copy");
		assertEquals("Original", original.getName());
	}

	@Test
	void mutatorsInvalidateConcreteCalendarCache() {
		WorkingCalendar calendar = WorkingCalendar.getInstance();
		calendar.getConcreteInstance();
		assertFalse(calendar.isInvalid());

		calendar.addCalendarTime(0L, CalendarOption.getInstance().getMillisPerDay());

		assertTrue(calendar.isInvalid());
	}

	@Test
	void derivedCalendarScratchCopyKeepsExceptionsIndependent() throws Exception {
		WorkingCalendar derived = WorkingCalendar.getStandardBasedInstance();
		WorkDay exception = new WorkDay(timestamp(2024, Calendar.JUNE, 4));
		derived.addOrReplaceException(exception);

		WorkingCalendar copy = derived.makeScratchCopy();
		copy.removeException(findException(copy, exception.getStart()));

		assertEquals(3, derived.getConcreteInstance().getExceptions().length);
		assertEquals(2, copy.getConcreteInstance().getExceptions().length);
		assertNotEquals(derived.getConcreteInstance().getExceptions().length, copy.getConcreteInstance().getExceptions().length);
	}

	private static long timestamp(int year, int month, int dayOfMonth) {
		return DateTime.calendarInstance(year, month, dayOfMonth).getTimeInMillis();
	}

	private static WorkDay findException(WorkingCalendar calendar, long start) {
		for (WorkDay day : calendar.getExceptionDays()) {
			if (day != null && day.getStart() == start) {
				return day;
			}
		}
		throw new AssertionError("Expected exception day not found: " + start);
	}
}
