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

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;

public final class TimeInputParser {
	private static final DateTimeFormatter TIME_FORMATTER = new DateTimeFormatterBuilder()
		.appendValue(ChronoField.HOUR_OF_DAY, 1, 2, java.time.format.SignStyle.NOT_NEGATIVE)
		.optionalStart()
		.appendLiteral(':')
		.appendValue(ChronoField.MINUTE_OF_HOUR, 1, 2, java.time.format.SignStyle.NOT_NEGATIVE)
		.optionalStart()
		.appendLiteral(':')
		.appendValue(ChronoField.SECOND_OF_MINUTE, 1, 2, java.time.format.SignStyle.NOT_NEGATIVE)
		.optionalEnd()
		.optionalEnd()
		.parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
		.parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
		.toFormatter();

	private TimeInputParser() {
	}

	public static int parseHour(String value, int fallback) {
		if (value == null) {
			return fallback;
		}
		String trimmed = value.trim();
		if (trimmed.isEmpty()) {
			return fallback;
		}
		try {
			TemporalAccessor parsed = TIME_FORMATTER.parse(trimmed);
			return LocalTime.from(parsed).getHour();
		} catch (DateTimeParseException e) {
			return fallback;
		}
	}
}
