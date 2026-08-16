package com.microproject.pm.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Guards the contract that {@link WorkingHours#hasHours()} is a pure query
 * (issue #156): it must not zero the cached {@code duration}, which is used
 * by {@link WorkDay#isWorking()} and the CalendarService working-day checks.
 */
class WorkingHoursTest {

	@Test
	void hasHoursDoesNotMutateDuration() {
		WorkingHours hours = new WorkingHours();
		assertEquals(0, hours.getDuration());
		assertFalse(hours.hasHours());

		WorkingHours working = new WorkingHours();
		try {
			// 8:00-12:00 and 13:00-17:00 -> 8 hours
			working.setInterval(0, WorkingHours.hourTime(8), WorkingHours.hourTime(12));
			working.setInterval(1, WorkingHours.hourTime(13), WorkingHours.hourTime(17));
		} catch (WorkRangeException e) {
			throw new AssertionError("unexpected", e);
		}

		long durationBefore = working.getDuration();
		assertTrue(durationBefore > 0);
		assertTrue(working.hasHours());
		// hasHours() must not change the cached working time
		assertEquals(durationBefore, working.getDuration());
	}

	@Test
	void workDayIsWorkingUsesUncorruptedDuration() {
		WorkingHours working = new WorkingHours();
		try {
			working.setInterval(0, WorkingHours.hourTime(8), WorkingHours.hourTime(12));
		} catch (WorkRangeException e) {
			throw new AssertionError("unexpected", e);
		}
		WorkDay day = new WorkDay();
		day.setWorkingHours(working);

		assertTrue(day.isWorking());
		assertTrue(day.getWorkingHours().hasHours());
		// previously this zeroed duration and made isWorking() false
		assertTrue(day.isWorking());
		assertEquals(working.getDuration(), day.getDuration());
	}
}
