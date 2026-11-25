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

import java.util.TimeZone;

import org.junit.jupiter.api.Test;

/**
 * Issue #1390: pins the strings that {@link TimeUtil#toUTCString(long)} produces
 * before the method moves from {@code SimpleDateFormat} to
 * {@code java.time.format.DateTimeFormatter}. The output is the GMT calendar
 * field of the instant, milliseconds are truncated, and the default time zone
 * must not leak into the result.
 */
class TimeUtilTest {
	private static final long EPOCH = 0L;
	private static final long SAMPLE_INSTANT = 1_700_000_000_000L; // 2023-11-14T22:13:20Z
	private static final long LEAP_DAY_WITH_MILLIS = 1_709_251_199_999L; // 2024-02-29T23:59:59.999Z
	private static final long LAST_DAY_OF_2049 = 2_524_521_600_000L; // 2049-12-31T00:00:00Z

	@Test
	void formatsTheEpochAndASampleInstantInGmt() {
		assertEquals("1970-01-01 00:00:00", TimeUtil.toUTCString(EPOCH));
		assertEquals("2023-11-14 22:13:20", TimeUtil.toUTCString(SAMPLE_INSTANT));
	}

	@Test
	void truncatesMillisecondsAndKeepsTheLeapDay() {
		assertEquals("2024-02-29 23:59:59", TimeUtil.toUTCString(LEAP_DAY_WITH_MILLIS));
		assertEquals("2049-12-31 00:00:00", TimeUtil.toUTCString(LAST_DAY_OF_2049));
	}

	@Test
	void ignoresTheDefaultTimeZone() {
		TimeZone original = TimeZone.getDefault();
		try {
			for (String zone : new String[] { "Asia/Tokyo", "America/Los_Angeles" }) {
				TimeZone.setDefault(TimeZone.getTimeZone(zone));
				assertEquals("1970-01-01 00:00:00", TimeUtil.toUTCString(EPOCH), zone);
				assertEquals("2023-11-14 22:13:20", TimeUtil.toUTCString(SAMPLE_INSTANT), zone);
				assertEquals("2024-02-29 23:59:59", TimeUtil.toUTCString(LEAP_DAY_WITH_MILLIS), zone);
				assertEquals("2049-12-31 00:00:00", TimeUtil.toUTCString(LAST_DAY_OF_2049), zone);
			}
		} finally {
			TimeZone.setDefault(original);
		}
	}
}
