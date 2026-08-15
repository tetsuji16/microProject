package com.microproject.contrib.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jdesktop.swing.calendar.DateSpan;
import org.junit.jupiter.api.Test;

class ContribIntervalsTest {
	@Test
	void dateSpanStoresStartAndEndInstants() {
		DateSpan span = new DateSpan(10L, 20L);

		assertEquals(10L, span.getStart());
		assertEquals(20L, span.getEnd());
	}

	@Test
	void intervalsMergeOverlappingDateSpans() {
		ContribIntervals intervals = new ContribIntervals();

		intervals.add(new DateSpan(10L, 20L));
		intervals.add(new DateSpan(18L, 25L));

		assertEquals(1, intervals.size());
		assertEquals(10L, intervals.getStart());
		assertEquals(25L, intervals.getEnd());
		assertTrue(intervals.containsDate(12L));
		assertTrue(intervals.containsDate(24L));
	}
}
