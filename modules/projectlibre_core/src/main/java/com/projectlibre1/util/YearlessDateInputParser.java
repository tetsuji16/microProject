package com.projectlibre1.util;

import java.text.DateFormat;
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
			return fallbackFormat.parse(trimmed);
		} catch (ParseException e) {
			if (!YEAR_PATTERN.matcher(trimmed).find()) {
				throw e;
			}
		}
		throw new ParseException(trimmed, 0);
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
		String[] parts = remainder.trim().split(":");
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
