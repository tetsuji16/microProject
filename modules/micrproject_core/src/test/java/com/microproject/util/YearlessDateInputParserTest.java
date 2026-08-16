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
package com.microproject.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.junit.jupiter.api.Test;

class YearlessDateInputParserTest {
	@Test
	void parsesMonthDayUsingReferenceYearWhenDateIsInThePast() throws Exception {
		Date reference = DateTime.calendarInstance(2020, Calendar.DECEMBER, 1).getTime();
		Date parsed = YearlessDateInputParser.parse("2/2", new SimpleDateFormat("yyyy/MM/dd"), reference);

		assertEquals(DateTime.calendarInstance(2021, Calendar.FEBRUARY, 2).getTime(), parsed);
	}

	@Test
	void parsesMonthDayUsingReferenceYearWhenDateIsInTheFutureInSameYear() throws Exception {
		Date reference = DateTime.calendarInstance(2020, Calendar.DECEMBER, 1).getTime();
		Date parsed = YearlessDateInputParser.parse("12/2", new SimpleDateFormat("yyyy/MM/dd"), reference);

		assertEquals(DateTime.calendarInstance(2020, Calendar.DECEMBER, 2).getTime(), parsed);
	}

	@Test
	void rollsToNextYearWhenTheReferenceDayHasAlreadyStarted() throws Exception {
		Calendar referenceCalendar = DateTime.calendarInstance(2020, Calendar.DECEMBER, 1);
		referenceCalendar.set(Calendar.HOUR_OF_DAY, 13);
		referenceCalendar.set(Calendar.MINUTE, 45);
		Date parsed = YearlessDateInputParser.parse("12/1", new SimpleDateFormat("yyyy/MM/dd"), referenceCalendar.getTime());

		assertEquals(DateTime.calendarInstance(2021, Calendar.DECEMBER, 1).getTime(), parsed);
	}

	@Test
	void parsesLeapDayWhenItIsValidForTheReferenceYear() throws Exception {
		Date reference = DateTime.calendarInstance(2020, Calendar.FEBRUARY, 28).getTime();
		Date parsed = YearlessDateInputParser.parse("2/29", new SimpleDateFormat("yyyy/MM/dd"), reference);

		assertEquals(DateTime.calendarInstance(2020, Calendar.FEBRUARY, 29).getTime(), parsed);
	}

	@Test
	void handlesNullAndBlankText() throws Exception {
		assertNull(YearlessDateInputParser.parse(null, new SimpleDateFormat("yyyy/MM/dd"), null));
		assertNull(YearlessDateInputParser.parse("   ", new SimpleDateFormat("yyyy/MM/dd"), null));
	}

	@Test
	void parsesNumericTimeSuffix() throws Exception {
		Date reference = DateTime.calendarInstance(2020, Calendar.FEBRUARY, 28).getTime();
		Date parsed = YearlessDateInputParser.parse("2/29 9:30:15", new SimpleDateFormat("yyyy/MM/dd"), reference);

		Calendar parsedCalendar = DateTime.calendarInstance();
		parsedCalendar.setTime(parsed);
		assertEquals(2020, parsedCalendar.get(Calendar.YEAR));
		assertEquals(Calendar.FEBRUARY, parsedCalendar.get(Calendar.MONTH));
		assertEquals(29, parsedCalendar.get(Calendar.DAY_OF_MONTH));
		assertEquals(9, parsedCalendar.get(Calendar.HOUR_OF_DAY));
		assertEquals(30, parsedCalendar.get(Calendar.MINUTE));
		assertEquals(15, parsedCalendar.get(Calendar.SECOND));
	}

	@Test
	void rejectsInvalidDates() throws Exception {
		assertThrows(ParseException.class, () ->
			YearlessDateInputParser.parse("2/30", new SimpleDateFormat("yyyy/MM/dd"), new Date()));
	}
}
