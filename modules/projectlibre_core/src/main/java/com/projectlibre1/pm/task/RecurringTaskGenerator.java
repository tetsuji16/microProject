package com.projectlibre1.pm.task;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.projectlibre1.options.CalendarOption;
import com.projectlibre1.pm.calendar.WorkCalendar;
import com.projectlibre1.util.DateTime;

public final class RecurringTaskGenerator {
	private static final int MAX_GENERATION_ATTEMPTS = 5000;

	public static final class Occurrence {
		private final long start;
		private final long finish;

		public Occurrence(long start, long finish) {
			this.start = start;
			this.finish = finish;
		}

		public long getStart() {
			return start;
		}

		public long getFinish() {
			return finish;
		}
	}

	private RecurringTaskGenerator() {
	}

	public static List<Occurrence> generateOccurrences(RecurringTaskSpec spec, WorkCalendar calendar) {
		validateInputs(spec, calendar);
		long normalizedStart = calendar.adjustInsideCalendar(spec.getStart(), false);
		ArrayList<Occurrence> occurrences = new ArrayList<Occurrence>();
		HashSet<Long> seenStarts = new HashSet<Long>();
		GregorianCalendar cursor = DateTime.calendarInstance();
		cursor.setTimeInMillis(normalizedStart);
		int targetDayOfMonth = cursor.get(Calendar.DAY_OF_MONTH);
		long endBoundary = resolveEndBoundary(spec);
		int generationAttempts = 0;

		while (!reachedRangeLimit(spec, occurrences.size(), cursor.getTimeInMillis(), endBoundary)) {
			if (++generationAttempts > MAX_GENERATION_ATTEMPTS)
				throw new IllegalStateException("Recurring task generation exceeded safety limit");
			Long candidate = nextCandidate(spec, cursor, normalizedStart, targetDayOfMonth);
			if (candidate == null)
				continue;
			long adjustedStart = calendar.adjustInsideCalendar(candidate.longValue(), false);
			if (adjustedStart < normalizedStart)
				continue;
			if (adjustedStart > endBoundary)
				break;
			if (!seenStarts.add(Long.valueOf(adjustedStart)))
				continue;
			occurrences.add(new Occurrence(adjustedStart, calculateFinish(spec, calendar, adjustedStart)));
		}
		return occurrences;
	}

	private static void validateInputs(RecurringTaskSpec spec, WorkCalendar calendar) {
		if (spec == null)
			throw new IllegalArgumentException("spec is required");
		if (calendar == null)
			throw new IllegalArgumentException("calendar is required");
	}

	private static boolean reachedRangeLimit(RecurringTaskSpec spec, int currentCount, long candidate, long endBoundary) {
		if (spec.getRangeType() == RecurringTaskSpec.RangeType.END_AFTER_OCCURRENCES)
			return currentCount >= spec.getOccurrenceCount();
		return candidate > endBoundary;
	}

	private static long resolveEndBoundary(RecurringTaskSpec spec) {
		return spec.getRangeType() == RecurringTaskSpec.RangeType.END_BY_DATE
			? spec.getEndDate()
			: Long.MAX_VALUE;
	}

	private static long calculateFinish(RecurringTaskSpec spec, WorkCalendar calendar, long adjustedStart) {
		long durationMillis = spec.getDuration();
		if (durationMillis == 0L)
			return adjustedStart;
		return calendar.add(adjustedStart, durationMillis, false);
	}

	private static Long nextCandidate(
		RecurringTaskSpec spec,
		GregorianCalendar cursor,
		long normalizedStart,
		int targetDayOfMonth) {
		switch (spec.getPatternType()) {
		case DAILY:
			return nextDailyCandidate(cursor);
		case WEEKLY:
			return nextWeeklyCandidate(spec, cursor, normalizedStart);
		case MONTHLY:
			return nextMonthlyCandidate(cursor, normalizedStart, targetDayOfMonth);
		default:
			throw new IllegalArgumentException("Unsupported pattern type " + spec.getPatternType());
		}
	}

	private static Long nextDailyCandidate(GregorianCalendar cursor) {
		long daily = cursor.getTimeInMillis();
		cursor.add(Calendar.DAY_OF_MONTH, 1);
		return Long.valueOf(daily);
	}

	private static Long nextWeeklyCandidate(
		RecurringTaskSpec spec,
		GregorianCalendar cursor,
		long normalizedStart) {
		while (true) {
			long weekly = cursor.getTimeInMillis();
			int dayOfWeek = cursor.get(Calendar.DAY_OF_WEEK);
			cursor.add(Calendar.DAY_OF_MONTH, 1);
			if (weekly >= normalizedStart && spec.getWeeklyDays().contains(Integer.valueOf(dayOfWeek)))
				return Long.valueOf(weekly);
		}
	}

	private static Long nextMonthlyCandidate(
		GregorianCalendar cursor,
		long normalizedStart,
		int targetDayOfMonth) {
		while (true) {
			GregorianCalendar candidate = createMonthlyCandidate(cursor);
			int maxDay = candidate.getActualMaximum(Calendar.DAY_OF_MONTH);
			cursor.add(Calendar.MONTH, 1);
			cursor.set(Calendar.DAY_OF_MONTH, 1);
			if (targetDayOfMonth > maxDay)
				continue;
			candidate.set(Calendar.DAY_OF_MONTH, targetDayOfMonth);
			long monthly = candidate.getTimeInMillis();
			if (monthly >= normalizedStart)
				return Long.valueOf(monthly);
		}
	}

	private static GregorianCalendar createMonthlyCandidate(GregorianCalendar cursor) {
		int year = cursor.get(Calendar.YEAR);
		int month = cursor.get(Calendar.MONTH);
		GregorianCalendar candidate = DateTime.calendarInstance(year, month, 1);
		candidate.set(Calendar.HOUR_OF_DAY, cursor.get(Calendar.HOUR_OF_DAY));
		candidate.set(Calendar.MINUTE, cursor.get(Calendar.MINUTE));
		candidate.set(Calendar.SECOND, cursor.get(Calendar.SECOND));
		candidate.set(Calendar.MILLISECOND, cursor.get(Calendar.MILLISECOND));
		return candidate;
	}
}
