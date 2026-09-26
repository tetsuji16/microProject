/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.dialog.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.jdesktop.swing.calendar.DateSpan;
import org.junit.jupiter.api.Test;

import com.microproject.contrib.calendar.ContribIntervals;

class IntervalsTraversalTest {
	@Test
	void constructionAndAddAllPreserveIntervalValuesAndContainment() {
		ContribIntervals source = new ContribIntervals();
		source.add(new DateSpan(10L, 20L));
		source.add(new DateSpan(30L, 40L));

		Intervals copied = new Intervals(source);
		assertEquals(10L, copied.getStart());
		assertEquals(40L, copied.getEnd());
		assertTrue(copied.containsDate(20L));
		assertFalse(copied.containsDate(25L));

		Intervals added = new Intervals(null);
		assertTrue(added.addAll(List.of(new CalendarInterval(50L, 60L), new CalendarInterval(70L, 80L))));
		assertEquals(2, added.size());
		assertTrue(added.containsDate(75L));
		assertFalse(added.containsDate(65L));
	}
}
