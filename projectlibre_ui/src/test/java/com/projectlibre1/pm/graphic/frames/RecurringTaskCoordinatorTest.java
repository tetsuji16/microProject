package com.projectlibre1.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import com.projectlibre1.options.CalendarOption;
import com.projectlibre1.pm.task.RecurringTaskSpec;
import com.projectlibre1.util.DateTime;

class RecurringTaskCoordinatorTest {
	@Test
	void headlessModeSkipsDialogAndInsertion() {
		AtomicBoolean dialogShown = new AtomicBoolean(false);
		AtomicBoolean inserted = new AtomicBoolean(false);
		RecurringTaskCoordinator coordinator = new RecurringTaskCoordinator(
			() -> true,
			frame -> {
				dialogShown.set(true);
				return sampleSpec();
			},
			(frame, spec) -> inserted.set(true));

		boolean result = coordinator.openDialogAndInsert(null);

		assertFalse(result);
		assertFalse(dialogShown.get());
		assertFalse(inserted.get());
	}

	@Test
	void confirmedSpecTriggersInsertion() {
		AtomicBoolean inserted = new AtomicBoolean(false);
		RecurringTaskCoordinator coordinator = new RecurringTaskCoordinator(
			() -> false,
			frame -> sampleSpec(),
			(frame, spec) -> inserted.set(spec != null));

		boolean result = coordinator.openDialogAndInsert(null);

		assertTrue(result);
		assertTrue(inserted.get());
	}

	private RecurringTaskSpec sampleSpec() {
		return new RecurringTaskSpec(
			"Recurring",
			CalendarOption.getInstance().makeValidStart(
				DateTime.calendarInstance(2026, Calendar.JUNE, 1).getTimeInMillis(),
				true),
			0L,
			RecurringTaskSpec.PatternType.DAILY,
			RecurringTaskSpec.RangeType.END_AFTER_OCCURRENCES,
			0L,
			1,
			null);
	}
}
