/*
 * MIT License
 * Copyright (c) 2026 microProject
 */
package com.microproject.pm.calendar;

import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import com.microproject.util.DateTime;

/** A finite Microsoft Project-style recurrence definition for a calendar exception. */
public final class CalendarRecurrence implements Serializable, Cloneable {
	private static final long serialVersionUID = 1L;
	private static final int MAX_OCCURRENCES = 1_000_000;

	public enum Pattern { DAILY, WEEKLY, MONTHLY, YEARLY }
	public enum EndMode { BY_DATE, AFTER_OCCURRENCES }

	private Pattern pattern;
	private EndMode endMode;
	private long startDate;
	private long finishDate;
	private int occurrenceCount;
	private int frequency = 1;
	private boolean workingDaysOnly;
	/** ISO weekdays, Monday=1 through Sunday=7. */
	private BitSet weekdays = new BitSet(8);
	private boolean relative;
	/** For monthly/yearly relative patterns: 1..4, or -1 for last. */
	private int ordinal = 1;
	/** ISO weekday, Monday=1 through Sunday=7. */
	private int weekday = 1;
	/** Month 1..12 for yearly patterns. */
	private int month = 1;
	/** Day 1..31 for absolute monthly/yearly patterns. */
	private int dayOfMonth = 1;

	private CalendarRecurrence() { }

	public static CalendarRecurrence daily(long start, int everyDays, boolean weekdaysOnly,
		EndMode endMode, long finish, int count) {
		CalendarRecurrence result = new CalendarRecurrence();
		result.pattern = Pattern.DAILY;
		result.frequency = everyDays;
		result.workingDaysOnly = weekdaysOnly;
		result.setRange(start, endMode, finish, count);
		return result;
	}

	public static CalendarRecurrence weekly(long start, int everyWeeks, BitSet isoWeekdays,
		EndMode endMode, long finish, int count) {
		CalendarRecurrence result = new CalendarRecurrence();
		result.pattern = Pattern.WEEKLY;
		result.frequency = everyWeeks;
		result.weekdays = (BitSet) Objects.requireNonNull(isoWeekdays, "weekdays").clone();
		result.setRange(start, endMode, finish, count);
		return result;
	}

	public static CalendarRecurrence monthly(long start, int everyMonths, boolean relative,
		int dayOfMonth, int ordinal, int isoWeekday, EndMode endMode, long finish, int count) {
		CalendarRecurrence result = new CalendarRecurrence();
		result.pattern = Pattern.MONTHLY;
		result.frequency = everyMonths;
		result.relative = relative;
		result.dayOfMonth = dayOfMonth;
		result.ordinal = ordinal;
		result.weekday = isoWeekday;
		result.setRange(start, endMode, finish, count);
		return result;
	}

	public static CalendarRecurrence yearly(long start, int everyYears, boolean relative,
		int month, int dayOfMonth, int ordinal, int isoWeekday,
		EndMode endMode, long finish, int count) {
		CalendarRecurrence result = new CalendarRecurrence();
		result.pattern = Pattern.YEARLY;
		result.frequency = everyYears;
		result.relative = relative;
		result.month = month;
		result.dayOfMonth = dayOfMonth;
		result.ordinal = ordinal;
		result.weekday = isoWeekday;
		result.setRange(start, endMode, finish, count);
		return result;
	}

	private void setRange(long start, EndMode mode, long finish, int count) {
		startDate = DateTime.dayFloor(start);
		endMode = Objects.requireNonNull(mode, "endMode");
		finishDate = DateTime.dayFloor(finish);
		occurrenceCount = count;
		validate();
	}

	public void validate() {
		if (pattern == null || endMode == null) throw new IllegalArgumentException("Recurrence pattern and range are required");
		if (frequency < 1 || frequency > 999) throw new IllegalArgumentException("Recurrence frequency must be 1 through 999");
		if (endMode == EndMode.BY_DATE && finishDate < startDate)
			throw new IllegalArgumentException("Recurrence finish must not be before its start");
		if (endMode == EndMode.AFTER_OCCURRENCES && (occurrenceCount < 1 || occurrenceCount > MAX_OCCURRENCES))
			throw new IllegalArgumentException("Occurrence count must be 1 through " + MAX_OCCURRENCES);
		if (pattern == Pattern.WEEKLY && weekdays.isEmpty())
			throw new IllegalArgumentException("Select at least one weekday for a weekly recurrence");
		if ((pattern == Pattern.MONTHLY || pattern == Pattern.YEARLY) && relative) {
			if (!(ordinal == -1 || ordinal >= 1 && ordinal <= 4) || weekday < 1 || weekday > 7)
				throw new IllegalArgumentException("Relative recurrence requires a valid weekday and ordinal");
		}
		if ((pattern == Pattern.MONTHLY || pattern == Pattern.YEARLY) && !relative
				&& (dayOfMonth < 1 || dayOfMonth > 31))
			throw new IllegalArgumentException("Recurrence day must be 1 through 31");
		if (pattern == Pattern.YEARLY && (month < 1 || month > 12))
			throw new IllegalArgumentException("Recurrence month must be 1 through 12");
	}

	public List<Long> occurrenceDates() {
		validate();
		LocalDate start = localDate(startDate);
		LocalDate finish = endMode == EndMode.BY_DATE ? localDate(finishDate) : LocalDate.MAX;
		ArrayList<Long> result = new ArrayList<>();
		switch (pattern) {
			case DAILY -> generateDaily(start, finish, result);
			case WEEKLY -> generateWeekly(start, finish, result);
			case MONTHLY -> generateMonths(start, finish, result);
			case YEARLY -> generateYears(start, finish, result);
		}
		return List.copyOf(result);
	}

	private void generateDaily(LocalDate start, LocalDate finish, List<Long> result) {
		LocalDate candidate = start;
		int guard = 0;
		while (!candidate.isAfter(finish) && withinCount(result)) {
			if (!workingDaysOnly || isWeekday(candidate)) addDate(candidate, result);
			candidate = candidate.plusDays(frequency);
			if (++guard > MAX_OCCURRENCES * 7) throw new IllegalArgumentException("Recurrence expands beyond the supported limit");
		}
	}

	private void generateWeekly(LocalDate start, LocalDate finish, List<Long> result) {
		LocalDate firstSunday = start.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
		for (int week = 0; withinCount(result); week += frequency) {
			LocalDate weekStart = firstSunday.plusWeeks(week);
			if (weekStart.isAfter(finish)) break;
			for (int day = weekdays.nextSetBit(1); day >= 1; day = weekdays.nextSetBit(day + 1)) {
				LocalDate candidate = weekStart.plusDays(day % 7);
				if (!candidate.isBefore(start) && !candidate.isAfter(finish)) addDate(candidate, result);
			}
		}
	}

	private void generateMonths(LocalDate start, LocalDate finish, List<Long> result) {
		for (int offset = 0; withinCount(result); offset += frequency) {
			LocalDate monthStart = start.withDayOfMonth(1).plusMonths(offset);
			if (monthStart.isAfter(finish)) break;
			LocalDate candidate = relative ? relativeDate(monthStart) : absoluteDate(monthStart, dayOfMonth);
			if (candidate != null && !candidate.isBefore(start) && !candidate.isAfter(finish)) addDate(candidate, result);
		}
	}

	private void generateYears(LocalDate start, LocalDate finish, List<Long> result) {
		for (int offset = 0; withinCount(result); offset += frequency) {
			LocalDate yearStart = start.withDayOfYear(1).plusYears(offset);
			LocalDate monthStart = yearStart.withMonth(month).withDayOfMonth(1);
			if (monthStart.isAfter(finish)) break;
			LocalDate candidate = relative ? relativeDate(monthStart) : absoluteDate(monthStart, dayOfMonth);
			if (candidate != null && !candidate.isBefore(start) && !candidate.isAfter(finish)) addDate(candidate, result);
		}
	}

	private LocalDate relativeDate(LocalDate monthStart) {
		DayOfWeek day = DayOfWeek.of(weekday);
		if (ordinal == -1) return monthStart.with(TemporalAdjusters.lastInMonth(day));
		return monthStart.with(TemporalAdjusters.dayOfWeekInMonth(ordinal, day));
	}

	private static LocalDate absoluteDate(LocalDate monthStart, int day) {
		// Project's recurring exception engine keeps the requested month/day and
		// clamps it to that month's final day when the requested day is absent
		// (for example, February 29 becomes February 28 in a non-leap year).
		return monthStart.withDayOfMonth(Math.min(day, monthStart.lengthOfMonth()));
	}

	private boolean withinCount(List<Long> dates) {
		return endMode != EndMode.AFTER_OCCURRENCES || dates.size() < occurrenceCount;
	}

	private void addDate(LocalDate date, List<Long> dates) {
		if (withinCount(dates)) dates.add(DateTime.dayFloor(date.atStartOfDay(zone()).toInstant().toEpochMilli()));
		if (dates.size() > MAX_OCCURRENCES) throw new IllegalArgumentException("Recurrence expands beyond the supported limit");
	}

	private static boolean isWeekday(LocalDate date) {
		return date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY;
	}

	private static LocalDate localDate(long date) {
		return new Date(date).toInstant().atZone(zone()).toLocalDate();
	}

	private static ZoneId zone() {
		return DateTime.calendarInstance().getTimeZone().toZoneId();
	}

	public Pattern getPattern() { return pattern; }
	public EndMode getEndMode() { return endMode; }
	public long getStartDate() { return startDate; }
	public long getFinishDate() { return finishDate; }
	public int getOccurrenceCount() { return occurrenceCount; }
	public int getFrequency() { return frequency; }
	public boolean isWorkingDaysOnly() { return workingDaysOnly; }
	public BitSet getWeekdays() { return (BitSet) weekdays.clone(); }
	public boolean isRelative() { return relative; }
	public int getOrdinal() { return ordinal; }
	public int getWeekday() { return weekday; }
	public int getMonth() { return month; }
	public int getDayOfMonth() { return dayOfMonth; }

	@Override public CalendarRecurrence clone() {
		try {
			CalendarRecurrence result = (CalendarRecurrence) super.clone();
			result.weekdays = (BitSet) weekdays.clone();
			return result;
		} catch (CloneNotSupportedException impossible) {
			throw new IllegalStateException("CalendarRecurrence must be cloneable", impossible);
		}
	}
}
