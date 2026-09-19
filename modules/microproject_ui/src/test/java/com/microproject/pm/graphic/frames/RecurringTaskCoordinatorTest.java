/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
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
package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import com.microproject.options.CalendarOption;
import com.microproject.pm.task.RecurringTaskSpec;
import com.microproject.util.DateTime;

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
