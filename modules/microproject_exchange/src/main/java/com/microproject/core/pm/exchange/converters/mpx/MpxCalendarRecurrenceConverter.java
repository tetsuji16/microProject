/*
 * MIT License
 * Copyright (c) 2026 microProject
 */
package com.microproject.core.pm.exchange.converters.mpx;

import java.util.BitSet;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import com.microproject.pm.calendar.CalendarRecurrence;

import org.mpxj.RecurringData;
import org.mpxj.RecurrenceType;

/** Converts MPXJ recurrence metadata without flattening it to finite dates. */
	public final class MpxCalendarRecurrenceConverter {
	public CalendarRecurrence from(RecurringData source) {
		if (source == null || source.getStartDate() == null || source.getRecurrenceType() == null)
			throw new IllegalArgumentException("MSP calendar recurrence is missing its start date or pattern");
		long start = MpxCalendarDateConverter.toModelDay(MpxDates.toDate(source.getStartDate()));
		boolean byDate = source.getFinishDate() != null;
		long finish = byDate
			? MpxCalendarDateConverter.toModelDay(MpxDates.toDate(source.getFinishDate())) : 0L;
		int count = source.getOccurrences() == null ? 1 : source.getOccurrences();
		CalendarRecurrence.EndMode endMode = byDate
			? CalendarRecurrence.EndMode.BY_DATE : CalendarRecurrence.EndMode.AFTER_OCCURRENCES;
		int frequency = positive(source.getFrequency());
		CalendarRecurrence result;
		switch (source.getRecurrenceType()) {
			case DAILY -> result = CalendarRecurrence.daily(start, frequency, source.getWorkingDaysOnly(), endMode, finish, count);
			case WEEKLY -> {
				BitSet weekdays = new BitSet(8);
				for (int day = 1; day <= 7; day++)
					if (source.getWeeklyDay(MpxDates.toDayOfWeek(day))) weekdays.set(toIsoWeekday(day));
				result = CalendarRecurrence.weekly(start, frequency, weekdays, endMode, finish, count);
			}
			case MONTHLY -> result = CalendarRecurrence.monthly(start, frequency, source.getRelative(),
				positive(source.getDayNumber()), relativeOrdinal(source), isoWeekday(source.getDayOfWeek()),
				endMode, finish, count);
			case YEARLY -> result = CalendarRecurrence.yearly(start, frequency, source.getRelative(),
				positive(source.getMonthNumber()), positive(source.getDayNumber()), relativeOrdinal(source),
				isoWeekday(source.getDayOfWeek()), endMode, finish, count);
			default -> throw new IllegalArgumentException("Unsupported MSP calendar recurrence pattern: "
				+ source.getRecurrenceType());
		}
		LocalDate[] mpxjDates = source.getDates();
		if (mpxjDates == null || mpxjDates.length == 0)
			throw new IllegalArgumentException("MSP calendar recurrence has no occurrences");
		List<Long> sourceDates = Arrays.stream(mpxjDates)
			.mapToLong(date -> MpxCalendarDateConverter.toModelDay(MpxDates.toDate(date)))
			.boxed().toList();
		if (!result.occurrenceDates().equals(sourceDates))
			throw new IllegalArgumentException("MSP calendar recurrence " + result.getPattern()
				+ " cannot be represented without changing its occurrence dates: MSP/MPXJ=" + sourceDates
				+ ", microProject=" + result.occurrenceDates());
		return result;
	}

	public RecurringData to(CalendarRecurrence source) {
		source.validate();
		RecurringData result = new RecurringData();
		result.setStartDate(MpxDates.toLocalDate(MpxCalendarDateConverter.toMpxDate(source.getStartDate())));
		result.setRecurrenceType(switch (source.getPattern()) {
			case DAILY -> RecurrenceType.DAILY;
			case WEEKLY -> RecurrenceType.WEEKLY;
			case MONTHLY -> RecurrenceType.MONTHLY;
			case YEARLY -> RecurrenceType.YEARLY;
		});
		result.setFrequency(source.getFrequency());
		result.setUseEndDate(source.getEndMode() == CalendarRecurrence.EndMode.BY_DATE);
		if (source.getEndMode() == CalendarRecurrence.EndMode.BY_DATE) {
			result.setFinishDate(MpxDates.toLocalDate(MpxCalendarDateConverter.toMpxDate(source.getFinishDate())));
		} else {
			result.setOccurrences(source.getOccurrenceCount());
		}
		switch (source.getPattern()) {
			case DAILY -> result.setWorkingDaysOnly(source.isWorkingDaysOnly());
			case WEEKLY -> {
				BitSet weekdays = source.getWeekdays();
				for (int iso = weekdays.nextSetBit(1); iso >= 1; iso = weekdays.nextSetBit(iso + 1))
					result.setWeeklyDay(MpxDates.toDayOfWeek(toMpxDay(iso)), true);
			}
			case MONTHLY -> {
				result.setRelative(source.isRelative());
				if (source.isRelative()) {
					result.setDayOfWeek(MpxDates.toDayOfWeek(toMpxDay(source.getWeekday())));
					result.setDayNumber(mpxOrdinal(source.getOrdinal()));
				} else {
					result.setDayNumber(source.getDayOfMonth());
				}
			}
			case YEARLY -> {
				result.setRelative(source.isRelative());
				result.setMonthNumber(source.getMonth());
				if (source.isRelative()) {
					result.setDayOfWeek(MpxDates.toDayOfWeek(toMpxDay(source.getWeekday())));
					result.setDayNumber(mpxOrdinal(source.getOrdinal()));
				} else {
					result.setDayNumber(source.getDayOfMonth());
				}
			}
		}
		return result;
	}

	private static int positive(Integer value) {
		return value == null || value < 1 ? 1 : value;
	}

	private static int relativeOrdinal(RecurringData source) {
		if (!source.getRelative()) return 1;
		int ordinal = positive(source.getDayNumber());
		return ordinal == 5 ? -1 : ordinal;
	}

	private static int isoWeekday(DayOfWeek day) {
		return day == null ? 1 : toIsoWeekday(MpxDates.toMpxDay(day));
	}

	private static int toIsoWeekday(int mpxDay) {
		return mpxDay == 1 ? 7 : mpxDay - 1;
	}

	private static int toMpxDay(int isoDay) {
		return isoDay == 7 ? 1 : isoDay + 1;
	}

	private static int mpxOrdinal(int ordinal) {
		return ordinal == -1 ? 5 : ordinal;
	}
}
