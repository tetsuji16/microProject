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
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;

import org.junit.jupiter.api.Test;

import com.microproject.options.CalendarOption;
import com.microproject.pm.task.RecurringTaskSpec;
import com.microproject.util.DateTime;

class RecurringTaskDialogTest {
	@Test
	void buildsDailySpecFromValidatedInput() {
		RecurringTaskDialog.ValidationResult result = RecurringTaskDialog.validateAndBuildSpec(
			new RecurringTaskDialog.SpecInput(
				"Daily Standup",
				date(2026, Calendar.JUNE, 1),
				"1d",
				RecurringTaskSpec.PatternType.DAILY,
				Collections.<Integer>emptySet(),
				false,
				null,
				3));

		assertTrue(result.isValid());
		assertEquals(RecurringTaskSpec.PatternType.DAILY, result.getSpec().getPatternType());
		assertEquals(RecurringTaskSpec.RangeType.END_AFTER_OCCURRENCES, result.getSpec().getRangeType());
		assertEquals(3, result.getSpec().getOccurrenceCount());
	}

	@Test
	void buildsWeeklySpecWithSelectedDays() {
		LinkedHashSet<Integer> weeklyDays = new LinkedHashSet<Integer>();
		weeklyDays.add(Integer.valueOf(Calendar.MONDAY));
		weeklyDays.add(Integer.valueOf(Calendar.WEDNESDAY));
		RecurringTaskDialog.ValidationResult result = RecurringTaskDialog.validateAndBuildSpec(
			new RecurringTaskDialog.SpecInput(
				"Weekly Sync",
				date(2026, Calendar.JUNE, 1),
				"0d",
				RecurringTaskSpec.PatternType.WEEKLY,
				weeklyDays,
				false,
				null,
				4));

		assertTrue(result.isValid());
		assertEquals(2, result.getSpec().getWeeklyDays().size());
	}

	@Test
	void buildsMonthlySpecWithEndDateRange() {
		RecurringTaskDialog.ValidationResult result = RecurringTaskDialog.validateAndBuildSpec(
			new RecurringTaskDialog.SpecInput(
				"Monthly Review",
				date(2026, Calendar.JANUARY, 13),
				"2d",
				RecurringTaskSpec.PatternType.MONTHLY,
				Collections.<Integer>emptySet(),
				true,
				date(2026, Calendar.MARCH, 31),
				0));

		assertTrue(result.isValid());
		assertEquals(RecurringTaskSpec.PatternType.MONTHLY, result.getSpec().getPatternType());
		assertEquals(RecurringTaskSpec.RangeType.END_BY_DATE, result.getSpec().getRangeType());
	}

	@Test
	void reportsWeeklyValidationErrorBeforeBuildingSpec() {
		RecurringTaskDialog.ValidationResult result = RecurringTaskDialog.validateAndBuildSpec(
			new RecurringTaskDialog.SpecInput(
				"Broken Weekly",
				date(2026, Calendar.JUNE, 1),
				"1d",
				RecurringTaskSpec.PatternType.WEEKLY,
				Collections.<Integer>emptySet(),
				false,
				null,
				2));

		assertEquals("RecurringTaskDialog.ErrorWeekdays", result.getErrorKey());
	}

	private Date date(int year, int month, int dayOfMonth) {
		long value = CalendarOption.getInstance().makeValidStart(
			DateTime.calendarInstance(year, month, dayOfMonth).getTimeInMillis(),
			true);
		return new Date(value);
	}
}
