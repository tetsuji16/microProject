package com.projectlibre1.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
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
		GregorianCalendar candidate = DateTime.calendarInstance();
		candidate.setLenient(false);
		candidate.setTime(referenceDate);
		candidate.set(Calendar.MONTH, month - 1);
		candidate.set(Calendar.DAY_OF_MONTH, day);
		candidate.set(Calendar.HOUR_OF_DAY, 0);
		candidate.set(Calendar.MINUTE, 0);
		candidate.set(Calendar.SECOND, 0);
		candidate.set(Calendar.MILLISECOND, 0);

		GregorianCalendar reference = DateTime.calendarInstance();
		reference.setTime(referenceDate);
		reference.set(Calendar.MILLISECOND, 0);

		try {
			if (!candidate.after(reference)) {
				candidate.add(Calendar.YEAR, 1);
			}

			if (remainder != null && !remainder.isEmpty()) {
				applyTime(candidate, remainder);
			}

			return candidate.getTime();
		} catch (IllegalArgumentException e) {
			ParseException parseException = new ParseException("Invalid yearless date", 0);
			parseException.initCause(e);
			throw parseException;
		}
	}

	private static void applyTime(GregorianCalendar calendar, String remainder) {
		String[] parts = remainder.trim().split(":");
		if (parts.length < 2) {
			return;
		}
		try {
			int hour = Integer.parseInt(parts[0].trim());
			int minute = Integer.parseInt(parts[1].trim());
			calendar.set(Calendar.HOUR_OF_DAY, hour);
			calendar.set(Calendar.MINUTE, minute);
			if (parts.length > 2) {
				calendar.set(Calendar.SECOND, Integer.parseInt(parts[2].trim()));
			}
		} catch (NumberFormatException ignored) {
			// Leave the time at midnight when the suffix is not a simple numeric time.
		}
	}
}
