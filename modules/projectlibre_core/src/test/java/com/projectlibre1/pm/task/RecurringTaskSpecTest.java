package com.projectlibre1.pm.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.LinkedHashSet;

import org.junit.jupiter.api.Test;

import com.projectlibre1.options.CalendarOption;
import com.projectlibre1.util.DateTime;

class RecurringTaskSpecTest {
	@Test
	void weeklySpecRequiresSelectedWeekdays() {
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
			() -> new RecurringTaskSpec(
				"Weekly Sync",
				start(2026, Calendar.JUNE, 1),
				0L,
				RecurringTaskSpec.PatternType.WEEKLY,
				RecurringTaskSpec.RangeType.END_AFTER_OCCURRENCES,
				0L,
				2,
				new LinkedHashSet<Integer>()));

		assertEquals("weeklyDays must not be empty", error.getMessage());
	}

	@Test
	void endByDateRequiresBoundaryOnOrAfterStart() {
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
			() -> new RecurringTaskSpec(
				"Boundary",
				start(2026, Calendar.JUNE, 5),
				0L,
				RecurringTaskSpec.PatternType.DAILY,
				RecurringTaskSpec.RangeType.END_BY_DATE,
				start(2026, Calendar.JUNE, 4),
				0,
				null));

		assertEquals("endDate must not be before start", error.getMessage());
	}

	@Test
	void weeklyDaysRemainDefensivelyCopied() {
		LinkedHashSet<Integer> weeklyDays = new LinkedHashSet<Integer>();
		weeklyDays.add(Integer.valueOf(Calendar.MONDAY));
		RecurringTaskSpec spec = new RecurringTaskSpec(
			"Copied",
			start(2026, Calendar.JUNE, 1),
			0L,
			RecurringTaskSpec.PatternType.WEEKLY,
			RecurringTaskSpec.RangeType.END_AFTER_OCCURRENCES,
			0L,
			2,
			weeklyDays);

		weeklyDays.add(Integer.valueOf(Calendar.WEDNESDAY));

		assertEquals(1, spec.getWeeklyDays().size());
		assertTrue(spec.getWeeklyDays().contains(Integer.valueOf(Calendar.MONDAY)));
	}

	private long start(int year, int month, int dayOfMonth) {
		return CalendarOption.getInstance().makeValidStart(
			DateTime.calendarInstance(year, month, dayOfMonth).getTimeInMillis(),
			true);
	}
}
