package com.projectlibre1.pm.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.projectlibre1.datatype.Duration;
import com.projectlibre1.options.CalendarOption;
import com.projectlibre1.pm.calendar.CalendarService;
import com.projectlibre1.pm.calendar.WorkCalendar;
import com.projectlibre1.util.DateTime;

class RecurringTaskGeneratorTest {
	@Test
	void dailyRecurringUsesOccurrenceCountLimit() {
		RecurringTaskSpec spec = new RecurringTaskSpec(
			"Daily",
			start(2026, Calendar.JUNE, 1),
			Duration.setAsEstimated(CalendarOption.getInstance().getMillisPerDay(), true),
			RecurringTaskSpec.PatternType.DAILY,
			RecurringTaskSpec.RangeType.END_AFTER_OCCURRENCES,
			0L,
			3,
			null);

		List<RecurringTaskGenerator.Occurrence> occurrences =
			RecurringTaskGenerator.generateOccurrences(spec, defaultCalendar());

		assertEquals(3, occurrences.size());
		assertEquals(start(2026, Calendar.JUNE, 1), occurrences.get(0).getStart());
		assertEquals(start(2026, Calendar.JUNE, 2), occurrences.get(1).getStart());
		assertEquals(start(2026, Calendar.JUNE, 3), occurrences.get(2).getStart());
	}

	@Test
	void weeklyRecurringUsesSelectedWeekdaysOnly() {
		RecurringTaskSpec spec = new RecurringTaskSpec(
			"Weekly",
			start(2026, Calendar.JUNE, 1),
			0L,
			RecurringTaskSpec.PatternType.WEEKLY,
			RecurringTaskSpec.RangeType.END_AFTER_OCCURRENCES,
			0L,
			4,
			new LinkedHashSet<Integer>(Arrays.asList(Calendar.MONDAY, Calendar.WEDNESDAY)));

		List<RecurringTaskGenerator.Occurrence> occurrences =
			RecurringTaskGenerator.generateOccurrences(spec, defaultCalendar());

		assertEquals(4, occurrences.size());
		assertEquals(start(2026, Calendar.JUNE, 1), occurrences.get(0).getStart());
		assertEquals(start(2026, Calendar.JUNE, 3), occurrences.get(1).getStart());
		assertEquals(start(2026, Calendar.JUNE, 8), occurrences.get(2).getStart());
		assertEquals(start(2026, Calendar.JUNE, 10), occurrences.get(3).getStart());
	}

	@Test
	void monthlyRecurringKeepsSameDayOfMonthWhenAvailable() {
		RecurringTaskSpec spec = new RecurringTaskSpec(
			"Monthly",
			start(2026, Calendar.JANUARY, 13),
			0L,
			RecurringTaskSpec.PatternType.MONTHLY,
			RecurringTaskSpec.RangeType.END_AFTER_OCCURRENCES,
			0L,
			3,
			null);

		List<RecurringTaskGenerator.Occurrence> occurrences =
			RecurringTaskGenerator.generateOccurrences(spec, defaultCalendar());

		assertEquals(3, occurrences.size());
		assertEquals(start(2026, Calendar.JANUARY, 13), occurrences.get(0).getStart());
		assertEquals(start(2026, Calendar.FEBRUARY, 13), occurrences.get(1).getStart());
		assertEquals(start(2026, Calendar.MARCH, 13), occurrences.get(2).getStart());
	}

	@Test
	void endByDateStopsAtBoundary() {
		RecurringTaskSpec spec = new RecurringTaskSpec(
			"Boundary",
			start(2026, Calendar.JUNE, 1),
			0L,
			RecurringTaskSpec.PatternType.DAILY,
			RecurringTaskSpec.RangeType.END_BY_DATE,
			end(2026, Calendar.JUNE, 3),
			0,
			null);

		List<RecurringTaskGenerator.Occurrence> occurrences =
			RecurringTaskGenerator.generateOccurrences(spec, defaultCalendar());

		assertEquals(3, occurrences.size());
		assertEquals(start(2026, Calendar.JUNE, 3), occurrences.get(2).getStart());
	}

	@Test
	void nonZeroDurationProducesLaterFinish() {
		long oneDay = Duration.setAsEstimated(CalendarOption.getInstance().getMillisPerDay(), true);
		RecurringTaskSpec spec = new RecurringTaskSpec(
			"Duration",
			start(2026, Calendar.JUNE, 1),
			oneDay,
			RecurringTaskSpec.PatternType.DAILY,
			RecurringTaskSpec.RangeType.END_AFTER_OCCURRENCES,
			0L,
			1,
			null);

		List<RecurringTaskGenerator.Occurrence> occurrences =
			RecurringTaskGenerator.generateOccurrences(spec, defaultCalendar());

		assertEquals(1, occurrences.size());
		assertTrue(occurrences.get(0).getFinish() > occurrences.get(0).getStart());
	}

	@Test
	void weeklyRecurringRequiresAtLeastOneWeekday() {
		assertThrows(IllegalArgumentException.class,
			() -> new RecurringTaskSpec(
				"Invalid",
				start(2026, Calendar.JUNE, 1),
				0L,
				RecurringTaskSpec.PatternType.WEEKLY,
				RecurringTaskSpec.RangeType.END_AFTER_OCCURRENCES,
				0L,
				1,
				new LinkedHashSet<Integer>()));
	}

	private WorkCalendar defaultCalendar() {
		return CalendarService.getInstance().getDefaultInstance();
	}

	private long start(int year, int month, int dayOfMonth) {
		return CalendarOption.getInstance().makeValidStart(
			DateTime.calendarInstance(year, month, dayOfMonth).getTimeInMillis(),
			true);
	}

	private long end(int year, int month, int dayOfMonth) {
		return CalendarOption.getInstance().makeValidEnd(
			DateTime.calendarInstance(year, month, dayOfMonth).getTimeInMillis(),
			true);
	}
}
