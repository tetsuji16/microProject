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
package com.microproject.pm.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.LinkedHashSet;

import org.junit.jupiter.api.Test;

import com.microproject.options.CalendarOption;
import com.microproject.util.DateTime;

class RecurringTaskSpecTest {
	@Test
	void weeklySpecRequiresSelectedWeekdays() {
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
			() -> new RecurringTaskSpec(
				"Weekly Sync",
				start(2026, Calendar.JUNE, 1),
				0L,
				RecurringTaskSpec.PatternType.WEEKLY,
				RecurringTaskSpec.RangeType.END_AFTER_OCCURRENCES,
				0L,
				2,
				new LinkedHashSet<Integer>()));

		assertEquals("weeklyDays must not be empty", error.getMessage());
	}

	@Test
	void endByDateRequiresBoundaryOnOrAfterStart() {
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
			() -> new RecurringTaskSpec(
				"Boundary",
				start(2026, Calendar.JUNE, 5),
				0L,
				RecurringTaskSpec.PatternType.DAILY,
				RecurringTaskSpec.RangeType.END_BY_DATE,
				start(2026, Calendar.JUNE, 4),
				0,
				null));

		assertEquals("endDate must not be before start", error.getMessage());
	}

	@Test
	void weeklyDaysRemainDefensivelyCopied() {
		LinkedHashSet<Integer> weeklyDays = new LinkedHashSet<Integer>();
		weeklyDays.add(Integer.valueOf(Calendar.MONDAY));
		RecurringTaskSpec spec = new RecurringTaskSpec(
			"Copied",
			start(2026, Calendar.JUNE, 1),
			0L,
			RecurringTaskSpec.PatternType.WEEKLY,
			RecurringTaskSpec.RangeType.END_AFTER_OCCURRENCES,
			0L,
			2,
			weeklyDays);

		weeklyDays.add(Integer.valueOf(Calendar.WEDNESDAY));

		assertEquals(1, spec.getWeeklyDays().size());
		assertTrue(spec.getWeeklyDays().contains(Integer.valueOf(Calendar.MONDAY)));
	}

	private long start(int year, int month, int dayOfMonth) {
		return CalendarOption.getInstance().makeValidStart(
			DateTime.calendarInstance(year, month, dayOfMonth).getTimeInMillis(),
			true);
	}
}
