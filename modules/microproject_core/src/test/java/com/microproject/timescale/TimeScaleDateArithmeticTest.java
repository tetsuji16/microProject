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
package com.microproject.timescale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.util.Calendar;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class TimeScaleDateArithmeticTest {
	@Test
	void floorAndCeilPreserveCalendarMonthBoundariesInGmt() {
		TimeScale scale = scale(Calendar.MONTH, 3);
		long input = millis("2024-05-17T13:22:34Z");

		assertEquals(millis("2024-04-01T00:00:00Z"), scale.floor1(input));
		assertEquals(millis("2024-07-01T00:00:00Z"), scale.ceil1(input));
	}

	@Test
	void iteratorKeepsLeapDayAndMonthIntervals() {
		TimeScale scale = scale(Calendar.DAY_OF_MONTH, 1);
		scale.setCalendarField2(Calendar.MONTH);
		scale.setNumber2(1);
		TimeIterator iterator = new TimeIterator(millis("2024-02-28T00:00:00Z"),
				millis("2024-03-02T00:00:00Z"), scale, -1);

		TimeInterval february28 = iterator.next();
		assertEquals(millis("2024-02-28T00:00:00Z"), february28.getStart1());
		assertEquals(millis("2024-02-29T00:00:00Z"), february28.getEnd1());
		assertEquals(millis("2024-02-01T00:00:00Z"), february28.getStart2());
		assertEquals(millis("2024-03-01T00:00:00Z"), february28.getEnd2());

		TimeInterval february29 = iterator.next();
		assertEquals(millis("2024-02-29T00:00:00Z"), february29.getStart1());
		assertEquals(millis("2024-03-01T00:00:00Z"), february29.getEnd1());
		assertNull(february29.getText2());

		TimeInterval march1 = iterator.next();
		assertEquals(millis("2024-03-01T00:00:00Z"), march1.getStart1());
		assertEquals(millis("2024-03-02T00:00:00Z"), march1.getEnd1());
		assertFalse(iterator.hasNext());
	}

	@Test
	void dayIteratorIncludesBothEndpointDaysAcrossLeapDay() {
		CalendarUtil.DayIterator days = new CalendarUtil.DayIterator();
		days.setInterval(new TestInterval(millis("2024-02-28T12:00:00Z"), millis("2024-03-01T08:00:00Z")));

		assertEquals(millis("2024-02-28T00:00:00Z"), days.nextDay());
		assertEquals(millis("2024-02-29T00:00:00Z"), days.nextDay());
		assertEquals(millis("2024-03-01T00:00:00Z"), days.nextDay());
		org.junit.jupiter.api.Assertions.assertFalse(days.hasMoreDays());
	}

	@Test
	void weekFloorUsesTheConfiguredLocaleFirstDay() {
		Locale original = Locale.getDefault();
		try {
			Locale.setDefault(Locale.US);
			TimeScale scale = scale(Calendar.WEEK_OF_YEAR, 1);
			assertEquals(millis("2024-05-12T00:00:00Z"), scale.floor1(millis("2024-05-17T13:00:00Z")));
		} finally {
			Locale.setDefault(original);
		}
	}

	@Test
	void groupedHourScaleKeepsLegacyTwelveHourFlooringAndDurationStep() {
		TimeScale scale = scale(Calendar.HOUR_OF_DAY, 6);
		long afternoon = millis("2024-05-17T17:45:00Z");

		assertEquals(millis("2024-05-17T12:00:00Z"), scale.floor1(afternoon));
		assertEquals(millis("2024-05-17T18:00:00Z"), scale.ceil1(afternoon));
	}

	private static TimeScale scale(int field, int number) {
		TimeScale scale = new TimeScale();
		scale.setCalendarField1(field);
		scale.setNumber1(number);
		scale.setPattern1("yyyy-MM-dd");
		scale.setPattern2("yyyy");
		return scale;
	}

	private static long millis(String instant) {
		return Instant.parse(instant).toEpochMilli();
	}

	private record TestInterval(long start, long end) implements com.microproject.model.time.HasStartAndEnd {
		@Override public long getStart() { return start; }
		@Override public long getEnd() { return end; }
	}
}
