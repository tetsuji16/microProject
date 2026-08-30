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

import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.ParseException;
import java.util.Date;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class YearlessDateInputParser {
	private static final Pattern YEARLESS_DATE_PATTERN = Pattern.compile("^\\s*(\\d{1,2})\\s*[/-]\\s*(\\d{1,2})(?:\\s+(.*?))?\\s*$");
	private static final Pattern YEAR_PATTERN = Pattern.compile("(?<!\\d)\\d{4}(?!\\d)");

	private YearlessDateInputParser() {
	}

	public static Date parse(String text, DateFormat fallbackFormat, Date referenceDate) throws ParseException {
		if (text == null) {
			return null;
		}
		String trimmed = text.trim();
		if (trimmed.isEmpty()) {
			return null;
		}

		if (!YEAR_PATTERN.matcher(trimmed).find()) {
			Matcher matcher = YEARLESS_DATE_PATTERN.matcher(trimmed);
			if (matcher.matches()) {
				int month = Integer.parseInt(matcher.group(1));
				int day = Integer.parseInt(matcher.group(2));
				String remainder = matcher.group(3);
				Date reference = referenceDate != null ? referenceDate : new Date();
				return buildDate(reference, month, day, remainder);
			}
		}

		try {
			return parseStrict(fallbackFormat, trimmed);
		} catch (ParseException e) {
			if (!YEAR_PATTERN.matcher(trimmed).find()) {
				throw e;
			}
		}
		throw new ParseException(trimmed, 0);
	}

	/**
	 * DateFormat.parse(String) accepts a valid prefix and silently normalizes
	 * impossible dates (for example a swapped year/month).  That is dangerous
	 * in a spreadsheet editor because a typo can move a task to an unrelated
	 * date.  Require the complete input to parse and disable lenient rollover.
	 */
	private static Date parseStrict(DateFormat format, String text) throws ParseException {
		if (format == null) {
			throw new ParseException(text, 0);
		}
		DateFormat strict = (DateFormat) format.clone();
		strict.setLenient(false);
		ParsePosition position = new ParsePosition(0);
		Date result = strict.parse(text, position);
		if (result == null || position.getIndex() != text.length()) {
			int error = position.getErrorIndex() >= 0 ? position.getErrorIndex() : position.getIndex();
			throw new ParseException(text, error);
		}
		return result;
	}

	private static Date buildDate(Date referenceDate, int month, int day, String remainder) throws ParseException {
		ZoneId zone = DateTime.calendarInstance().getTimeZone().toZoneId();
		ZonedDateTime reference = ZonedDateTime.ofInstant(referenceDate.toInstant(), zone);
		ZonedDateTime candidate;
		try {
			candidate = reference
				.withMonth(month)
				.withDayOfMonth(day)
				.withHour(0)
				.withMinute(0)
				.withSecond(0)
				.withNano(0);

			if (!candidate.isAfter(reference)) {
				candidate = candidate.plusYears(1);
			}

			if (remainder != null && !remainder.isEmpty()) {
				candidate = applyTime(candidate, remainder);
			}

			return Date.from(candidate.toInstant());
		} catch (DateTimeException e) {
			ParseException parseException = new ParseException("Invalid yearless date", 0);
			parseException.initCause(e);
			throw parseException;
		}
	}

	private static ZonedDateTime applyTime(ZonedDateTime dateTime, String remainder) {
		String[] parts = remainder.trim().split(":", -1);
		if (parts.length < 2) {
			return dateTime;
		}
		try {
			int hour = Integer.parseInt(parts[0].trim());
			int minute = Integer.parseInt(parts[1].trim());
			ZonedDateTime result = dateTime.withHour(hour).withMinute(minute);
			if (parts.length > 2) {
				result = result.withSecond(Integer.parseInt(parts[2].trim()));
			}
			return result.withNano(0);
		} catch (NumberFormatException ignored) {
			// Leave the time at midnight when the suffix is not a simple numeric time.
			return dateTime;
		}
	}
}
