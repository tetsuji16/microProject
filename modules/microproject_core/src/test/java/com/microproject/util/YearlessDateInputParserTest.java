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
package com.microproject.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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
	void rejectsFullDateWhenFallbackFormatIsMissing() {
		assertThrows(ParseException.class, () ->
			YearlessDateInputParser.parse("2026/09/02", null, null));
	}

	@Test
	void parsesExplicitIsoDatesInTheCanonicalUtcScheduleZone() throws Exception {
		TimeZone previous = TimeZone.getDefault();
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"));
		try {
			Date parsed = YearlessDateInputParser.parse("2026/9/25", new SimpleDateFormat("yyyy/MM/dd"), null);
			assertEquals(DateTime.calendarInstance(2026, Calendar.SEPTEMBER, 25).getTime(), parsed);
		} finally {
			TimeZone.setDefault(previous);
		}
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

	@Test
		void rejectsInvalidNumericTimeSuffix() throws Exception {
			Date reference = DateTime.calendarInstance(2026, Calendar.JUNE, 1).getTime();
			assertThrows(ParseException.class, () ->
				YearlessDateInputParser.parse("6/2 25:00", new SimpleDateFormat("yyyy/MM/dd"), reference));
		}

		private TimeZone originalDefaultTimeZone;

		@BeforeEach
		void setDefaultTimeZoneToAsiaTokyo() {
			originalDefaultTimeZone = TimeZone.getDefault();
			TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"));
		}

		@AfterEach
		void restoreDefaultTimeZone() {
			TimeZone.setDefault(originalDefaultTimeZone);
		}

		private static DateFormat fallbackFormat() {
			SimpleDateFormat f = new SimpleDateFormat("yyyy/MM/dd", Locale.ROOT);
			f.setTimeZone(TimeZone.getTimeZone("GMT"));
			return f;
		}

		private static Date parseOrNull(String text) throws ParseException {
			try {
				return YearlessDateInputParser.parse(text, fallbackFormat(), null);
			} catch (ParseException e) {
				return null; // signal ParseException via null; see expectedException below
			}
		}

		static Stream<Arguments> parseStrictIsoDateCases() {
			return Stream.of(
				// Accepted: standard ISO-like with zero-padded month/day
				Arguments.of("2026/09/25", "2026-09-25T00:00:00Z"),
				Arguments.of("2026-09-25", "2026-09-25T00:00:00Z"),
				// Accepted: single-digit month/day
				Arguments.of("2026/9/5", "2026-09-05T00:00:00Z"),
				Arguments.of("2026-9-5", "2026-09-05T00:00:00Z"),
				// Accepted: leap day 2024
				Arguments.of("2024/02/29", "2024-02-29T00:00:00Z"),
				// Rejected: invalid dates
				Arguments.of("2026/02/29", null),
				Arguments.of("2026/02/30", null),
				Arguments.of("2026/13/01", null),
				Arguments.of("2026/00/10", null),
				Arguments.of("2026/04/31", null),
				// Rejected: trailing garbage / time component
				Arguments.of("2026/09/25x", null),
				Arguments.of("2026/09/25 10:00", null),
				// Accepted: surrounding whitespace
				Arguments.of(" 2026/09/25 ", "2026-09-25T00:00:00Z"),
				// Unspecified: leading zeros beyond expected digits — record actual behavior
							Arguments.of("2026/009/25", "2026-09-25T00:00:00Z"),
							Arguments.of("2026/09/025", "2026-09-25T00:00:00Z")
			);
		}

		@ParameterizedTest(name = "{index}: \"{0}\" -> {1}")
		@MethodSource("parseStrictIsoDateCases")
		void parseStrictIsoDate(String input, String expectedIsoInstant) throws ParseException {
			Date result = parseOrNull(input);
			if (expectedIsoInstant == null) {
				assertThrows(ParseException.class, () -> YearlessDateInputParser.parse(input, fallbackFormat(), null),
					"Expected ParseException for input: " + input);
			} else {
				Date expected = Date.from(java.time.Instant.parse(expectedIsoInstant));
				assertEquals(expected, result, "Input: " + input);
			}
		}
	}
