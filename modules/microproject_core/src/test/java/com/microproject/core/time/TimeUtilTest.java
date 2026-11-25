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
package com.microproject.core.time;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

/**
 * Issue #1390: pins the UTC text produced by {@link TimeUtil#toUTCString(long)}
 * before and after it moves from a per-call SimpleDateFormat to java.time.
 */
class TimeUtilTest {

	private static final String EPOCH = "1970-01-01 00:00:00";
	private static final String KNOWN_INSTANT = "2023-11-14 22:13:20";
	private static final String LEAP_DAY_WITH_MILLIS = "2024-02-29 23:59:59";
	private static final String LAST_SUPPORTED_YEAR = "2049-12-31 00:00:00";

	@Test
	void toUTCStringFormatsEpoch() {
		assertEquals(EPOCH, TimeUtil.toUTCString(0L));
	}

	@Test
	void toUTCStringFormatsKnownInstant() {
		assertEquals(KNOWN_INSTANT, TimeUtil.toUTCString(1_700_000_000_000L));
	}

	@Test
	void toUTCStringTruncatesMilliseconds() {
		assertEquals(LEAP_DAY_WITH_MILLIS,
				TimeUtil.toUTCString(utcMillis(2024, 2, 29, 23, 59, 59, 999)));
	}

	@Test
	void toUTCStringFormatsLastSupportedYear() {
		assertEquals(LAST_SUPPORTED_YEAR,
				TimeUtil.toUTCString(utcMillis(2049, 12, 31, 0, 0, 0, 0)));
	}

	@Test
	void toUTCStringIgnoresDefaultTimeZone() {
		TimeZone original = TimeZone.getDefault();
		try {
			for (String zoneId : new String[] { "Asia/Tokyo", "America/Los_Angeles" }) {
				TimeZone.setDefault(TimeZone.getTimeZone(zoneId));
				assertEquals(EPOCH, TimeUtil.toUTCString(0L), zoneId);
				assertEquals(KNOWN_INSTANT, TimeUtil.toUTCString(1_700_000_000_000L), zoneId);
				assertEquals(LEAP_DAY_WITH_MILLIS,
						TimeUtil.toUTCString(utcMillis(2024, 2, 29, 23, 59, 59, 999)), zoneId);
				assertEquals(LAST_SUPPORTED_YEAR,
						TimeUtil.toUTCString(utcMillis(2049, 12, 31, 0, 0, 0, 0)), zoneId);
			}
		} finally {
			TimeZone.setDefault(original);
		}
	}

	private static long utcMillis(int year, int month, int day, int hour, int minute, int second, int millis) {
		return LocalDateTime.of(year, month, day, hour, minute, second, millis * 1_000_000)
				.toInstant(ZoneOffset.UTC).toEpochMilli();
	}
}
