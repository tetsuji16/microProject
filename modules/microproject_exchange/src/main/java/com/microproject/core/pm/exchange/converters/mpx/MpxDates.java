/*
 * MIT License
 * Copyright (c) 2026 microProject
 */
package com.microproject.core.pm.exchange.converters.mpx;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * Bridges MPXJ 16 java.time values to the java.util.Date values used by the
 * converters. MPXJ 11 produced host-local dates, so values are interpreted in
 * the system default zone to keep the previous behaviour.
 */
public final class MpxDates {
	private MpxDates() { }

	public static Date toDate(LocalDateTime value) {
		return value == null ? null : Date.from(value.atZone(ZoneId.systemDefault()).toInstant());
	}

	public static Date toDate(LocalDate value) {
		return value == null ? null : toDate(value.atStartOfDay());
	}

	public static Date toDate(LocalTime value) {
		return value == null ? null : toDate(value.atDate(LocalDate.of(1970, 1, 1)));
	}

	public static LocalDateTime toLocalDateTime(Date value) {
		return value == null ? null : LocalDateTime.ofInstant(value.toInstant(), ZoneId.systemDefault());
	}

	/** Converts MPX day numbers (1 = Sunday .. 7 = Saturday) to a DayOfWeek. */
	public static DayOfWeek toDayOfWeek(int mpxDay) {
		return DayOfWeek.of(mpxDay == 1 ? 7 : mpxDay - 1);
	}

	/** Converts a DayOfWeek to MPX day numbers (1 = Sunday .. 7 = Saturday). */
	public static int toMpxDay(DayOfWeek day) {
		return day.getValue() % 7 + 1;
	}

	public static LocalDate toLocalDate(Date value) {
		return value == null ? null : toLocalDateTime(value).toLocalDate();
	}
}
