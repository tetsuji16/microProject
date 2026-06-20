package com.projectlibre1.pm.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.projectlibre1.options.CalendarOption;

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
}
