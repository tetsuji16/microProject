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
