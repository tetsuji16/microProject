/*
 * MIT License
 * Copyright (c) 2026 microProject
 */
package com.microproject.pm.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.microproject.pm.time.ImmutableInterval;
import com.microproject.pm.time.MutableInterval;

class CalendarEventTest {
	@Test
	void eventEqualityIncludesDescriptionAndIsSymmetricWithIntervals() {
		CalendarEvent event = new CalendarEvent(10L, 20L, "meeting");
		CalendarEvent sameEvent = new CalendarEvent(10L, 20L, "meeting");
		CalendarEvent differentDescription = new CalendarEvent(10L, 20L, "review");
		ImmutableInterval interval = new ImmutableInterval(10L, 20L);

		assertEquals(event, sameEvent);
		assertEquals(event.hashCode(), sameEvent.hashCode());
		assertFalse(event.equals(differentDescription));
		assertFalse(event.equals(interval));
		assertFalse(interval.equals(event));
	}

	@Test
	void plainIntervalsRetainCrossImplementationValueEquality() {
		ImmutableInterval immutable = new ImmutableInterval(10L, 20L);
		MutableInterval mutable = new MutableInterval(10L, 20L);

		assertTrue(immutable.equals(mutable));
		assertTrue(mutable.equals(immutable));
	}

	@Test
	void compareUsesOverflowSafeStartOrdering() {
		CalendarEvent comparator = new CalendarEvent(0L);

		assertEquals(-1, comparator.compare(
				new CalendarEvent(Long.MIN_VALUE), new CalendarEvent(Long.MAX_VALUE)));
		assertEquals(1, comparator.compare(
				new CalendarEvent(Long.MAX_VALUE), new CalendarEvent(Long.MIN_VALUE)));
	}
}
