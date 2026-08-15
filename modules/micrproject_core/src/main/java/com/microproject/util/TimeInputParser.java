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
