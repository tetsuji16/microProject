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
