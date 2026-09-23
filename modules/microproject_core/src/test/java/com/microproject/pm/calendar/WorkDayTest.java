/*******************************************************************************
 * MIT License
 *
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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.Date;

import org.junit.jupiter.api.Test;

class WorkDayTest {
	@Test
	void intersectionUsesTheOverlapOfBothStartAndEndBounds() {
		WorkDay first = new WorkDay(10, 50);
		WorkDay second = new WorkDay(20, 60);

		WorkDay intersection = first.intersectWith(second);

		assertEquals(20, intersection.getStart());
		assertEquals(50, intersection.getEnd());
	}

	@Test
	void compareToOrdersExtremeStartValuesWithoutOverflow() {
		WorkDay earliest = new WorkDay(Long.MIN_VALUE);
		WorkDay latest = new WorkDay(Long.MAX_VALUE);

		assertTrue(earliest.compareTo(latest) < 0);
		assertTrue(latest.compareTo(earliest) > 0);
	}

	@Test
	void compareToPreservesLegacyDateAndCalendarOperands() {
		WorkDay day = new WorkDay(1234L);
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(1234L);

		assertEquals(0, day.compareTo(new Date(1234L)));
		assertEquals(0, day.compareTo(calendar));
	}
}
