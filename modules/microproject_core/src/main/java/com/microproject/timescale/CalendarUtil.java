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
package com.microproject.timescale;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

/** Utilities for time-scale field arithmetic on GMT-read epoch milliseconds. */
public final class CalendarUtil {
	public static final long SECOND_ms = 1000;
	public static final long MINUTE_ms = 60 * SECOND_ms;
	public static final long HOUR_ms = 60 * MINUTE_ms;
	public static final long DAY_ms = 24 * HOUR_ms;
	public static final long WEEK_ms = 7 * DAY_ms;
	public static final long MONTH_ms = 28 * DAY_ms;
	public static final long YEAR_ms = 365 * DAY_ms;

	/* Numeric values used by existing configuration.xml time-scale definitions. */
	public static final int YEAR_FIELD = 1;
	public static final int MONTH_FIELD = 2;
	public static final int WEEK_OF_YEAR_FIELD = 3;
	public static final int DAY_OF_MONTH_FIELD = 5;
	public static final int DAY_OF_WEEK_FIELD = 7;
	public static final int HOUR_OF_DAY_FIELD = 11;
	public static final int MINUTE_FIELD = 12;
	public static final int SECOND_FIELD = 13;

	private static final ZoneId GMT = ZoneOffset.UTC;
	private CalendarUtil() {
	}

	public static long min(long first, long second) {
		return Math.min(first, second);
	}

	public static long max(long first, long second) {
		return Math.max(first, second);
	}

	public static <T extends Comparable<? super T>> T min(T first, T second) {
		return first.compareTo(second) < 0 ? first : second;
	}

	public static <T extends Comparable<? super T>> T max(T first, T second) {
		return first.compareTo(second) > 0 ? first : second;
	}

	public static int compareEra(ZonedDateTime first, ZonedDateTime second) {
		return Integer.compare(first.get(ChronoField.ERA), second.get(ChronoField.ERA));
	}

	public static int compareYear(ZonedDateTime first, ZonedDateTime second) {
		int era = compareEra(first, second);
		return era != 0 ? era : Integer.compare(first.get(ChronoField.YEAR_OF_ERA), second.get(ChronoField.YEAR_OF_ERA));
	}

	public static int compareMonth(ZonedDateTime first, ZonedDateTime second) {
		int year = compareYear(first, second);
		return year != 0 ? year : Integer.compare(first.getMonthValue(), second.getMonthValue());
	}

	public static int compareDay(ZonedDateTime first, ZonedDateTime second) {
		int year = compareYear(first, second);
		return year != 0 ? year : Integer.compare(first.getDayOfYear(), second.getDayOfYear());
	}

	public static ZonedDateTime secondFloor(ZonedDateTime dateTime) {
		return dateTime.withNano(0);
	}

	public static ZonedDateTime minuteFloor(ZonedDateTime dateTime) {
		return secondFloor(dateTime).withSecond(0);
	}

	/** Preserves the legacy scale grouping, which is based on the 12-hour field. */
	public static ZonedDateTime hourFloor(ZonedDateTime dateTime, int number) {
		ZonedDateTime result = minuteFloor(dateTime).withMinute(0);
		if (number <= 1) return result;
		int halfDayBase = result.getHour() / 12 * 12;
		int hourWithinHalfDay = result.getHour() % 12;
		return result.withHour(halfDayBase + (hourWithinHalfDay / number) * number);
	}

	public static ZonedDateTime dayFloor(ZonedDateTime dateTime) {
		return dateTime.toLocalDate().atStartOfDay(dateTime.getZone());
	}

	public static ZonedDateTime weekFloor(ZonedDateTime dateTime) {
		var firstDay = WeekFields.of(Locale.getDefault()).getFirstDayOfWeek();
		LocalDate date = dateTime.toLocalDate().with(TemporalAdjusters.previousOrSame(firstDay));
		return date.atStartOfDay(dateTime.getZone());
	}

	public static ZonedDateTime monthFloor(ZonedDateTime dateTime) {
		return monthFloor(dateTime, 1);
	}

	public static ZonedDateTime monthFloor(ZonedDateTime dateTime, int number) {
		int month = dateTime.getMonthValue() - 1;
		int firstMonth = number > 1 ? month / number * number : month;
		return dateTime.withMonth(firstMonth + 1).withDayOfMonth(1).toLocalDate()
				.atStartOfDay(dateTime.getZone());
	}

	public static ZonedDateTime yearFloor(ZonedDateTime dateTime) {
		return dateTime.withDayOfYear(1).toLocalDate().atStartOfDay(dateTime.getZone());
	}

	public static ZonedDateTime floor(ZonedDateTime dateTime, int field) {
		return floor(dateTime, field, 1);
	}

	public static ZonedDateTime floor(ZonedDateTime dateTime, int field, int number) {
		return switch (field) {
		case YEAR_FIELD -> yearFloor(dateTime);
		case MONTH_FIELD -> monthFloor(dateTime, number);
		case WEEK_OF_YEAR_FIELD -> weekFloor(dateTime);
		case DAY_OF_WEEK_FIELD, DAY_OF_MONTH_FIELD -> dayFloor(dateTime);
		case HOUR_OF_DAY_FIELD -> hourFloor(dateTime, number);
		case MINUTE_FIELD -> minuteFloor(dateTime);
		case SECOND_FIELD -> secondFloor(dateTime);
		default -> dateTime;
		};
	}

	public static ZonedDateTime increment(ZonedDateTime dateTime, int field, int number) {
		return switch (field) {
		case YEAR_FIELD -> dateTime.plusYears(number);
		case MONTH_FIELD -> dateTime.plusMonths(number);
		case WEEK_OF_YEAR_FIELD -> dateTime.plusWeeks(number);
		case DAY_OF_WEEK_FIELD, DAY_OF_MONTH_FIELD -> dateTime.plusDays(number);
		case HOUR_OF_DAY_FIELD -> dateTime.plusHours(number);
		case MINUTE_FIELD -> dateTime.plusMinutes(number);
		case SECOND_FIELD -> dateTime.plusSeconds(number);
		default -> dateTime;
		};
	}

	public static long getMinDuration(int field) {
		return switch (field) {
		case YEAR_FIELD -> YEAR_ms;
		case MONTH_FIELD -> MONTH_ms;
		case WEEK_OF_YEAR_FIELD -> WEEK_ms;
		case DAY_OF_WEEK_FIELD, DAY_OF_MONTH_FIELD -> DAY_ms;
		case HOUR_OF_DAY_FIELD -> HOUR_ms;
		case MINUTE_FIELD -> MINUTE_ms;
		case SECOND_FIELD -> SECOND_ms;
		default -> -1;
		};
	}

	/** Formats an epoch value using the default locale and time zone. */
	public static String toString(long epochMillis) {
		DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
				.withLocale(Locale.getDefault()).withZone(ZoneId.systemDefault());
		return formatter.format(Instant.ofEpochMilli(epochMillis));
	}

	public static long toLongTime(double time) {
		return Math.round(time / 1000) * 1000;
	}

	/** Preserves the historical seconds-rounding rule used by the time scale. */
	public static long roundTime(double time) {
		long roundedSecond = toLongTime(time);
		int second = Instant.ofEpochMilli(roundedSecond).atZone(GMT).getSecond();
		return second > 30 ? roundedSecond + 60 - second : roundedSecond - second;
	}

	public static final class DayIterator {
		private LocalDate current;
		private LocalDate end;

		public void setInterval(com.microproject.model.time.HasStartAndEnd interval) {
			current = Instant.ofEpochMilli(interval.getStart()).atZone(GMT).toLocalDate();
			end = Instant.ofEpochMilli(interval.getEnd()).atZone(GMT).toLocalDate();
		}

		public boolean hasMoreDays() {
			return current != null && !current.isAfter(end);
		}

		public long nextDay() {
			LocalDate result = current;
			current = current.plusDays(1);
			return result.atStartOfDay(GMT).toInstant().toEpochMilli();
		}
	}
}
