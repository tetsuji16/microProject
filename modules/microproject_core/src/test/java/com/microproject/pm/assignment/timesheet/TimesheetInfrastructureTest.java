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
package com.microproject.pm.assignment.timesheet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.configuration.Configuration;
import com.microproject.field.Field;
import com.microproject.graphic.configuration.SpreadSheetCategories;
import com.microproject.graphic.configuration.SpreadSheetFieldArray;
import com.microproject.options.TimesheetOption;

class TimesheetInfrastructureTest {
	@Test
	void timesheetOptionResolvesConfiguredCompletionFieldArray() {
		SpreadSheetFieldArray fieldArray = TimesheetOption.getInstance().getTimesheetFieldArray();

		assertNotNull(fieldArray);
		assertNotNull(SpreadSheetFieldArray.getFromId(
			SpreadSheetCategories.timesheetSpreadsheetCategory,
			"Spreadsheet.Timesheet.completion"));
		assertNotNull(findField(fieldArray, "Field.actualWork"));
		assertNotNull(findField(fieldArray, "Field.remainingWork"));
		assertNotNull(findField(fieldArray, "Field.percentComplete"));
		assertNotNull(findField(fieldArray, "Field.timesheetStatus"));
		assertNotNull(findField(fieldArray, "Field.lastTimesheetUpdate"));
	}

	@Test
	void timesheetFieldsAreRegisteredInConfiguration() {
		assertNotNull(Configuration.getFieldFromId("Field.lastTimesheetUpdate"));
		assertNotNull(Configuration.getFieldFromId("Field.pendingTimesheetUpdate"));
		assertNotNull(Configuration.getFieldFromId("Field.timesheetStatus"));
	}

	@Test
	void timesheetHelperIgnoresNoDataAndFlagsMixedStatuses() {
		assertEquals(
			TimesheetStatus.ENTERED,
			TimesheetHelper.getTimesheetStatus(updates(
				new StubUpdatesFromTimesheet(TimesheetStatus.NO_DATA),
				new StubUpdatesFromTimesheet(TimesheetStatus.ENTERED))));

		assertEquals(
			TimesheetStatus.MIXED,
			TimesheetHelper.getTimesheetStatus(updates(
				new StubUpdatesFromTimesheet(TimesheetStatus.ENTERED),
				new StubUpdatesFromTimesheet(TimesheetStatus.INTEGRATED))));
	}

	@Test
	void timesheetHelperAppliesEveryChildAndReturnsWhetherAnythingChanged() {
		StubUpdatesFromTimesheet changed = new StubUpdatesFromTimesheet(TimesheetStatus.ENTERED);
		changed.applyResult = true;
		StubUpdatesFromTimesheet unchanged = new StubUpdatesFromTimesheet(TimesheetStatus.NO_DATA);
		List<UpdatesFromTimesheet> children = updates(changed, unchanged);
		List<String> fieldArray = List.of("timesheet-field");

		assertTrue(TimesheetHelper.applyTimesheet(children, fieldArray, 1234L));
		assertEquals(1, changed.applyCount);
		assertEquals(1, unchanged.applyCount);
		assertEquals(fieldArray, changed.appliedFieldArray);
		assertEquals(1234L, unchanged.appliedUpdateDate);
	}

	@Test
	void timesheetHelperAggregatesLatestUpdateAndPendingState() {
		StubUpdatesFromTimesheet older = new StubUpdatesFromTimesheet(TimesheetStatus.ENTERED);
		older.lastUpdate = 100L;
		StubUpdatesFromTimesheet newerPending = new StubUpdatesFromTimesheet(TimesheetStatus.VALIDATED);
		newerPending.lastUpdate = 250L;
		newerPending.pending = true;
		List<UpdatesFromTimesheet> children = updates(older, newerPending);

		assertEquals(250L, TimesheetHelper.getLastTimesheetUpdate(children));
		assertTrue(TimesheetHelper.isPendingTimesheetUpdate(children));
		assertEquals(0L, TimesheetHelper.getLastTimesheetUpdate(List.of()));
		assertEquals(false, TimesheetHelper.isPendingTimesheetUpdate(List.of()));
	}

	@Test
	void timesheetStatusFieldUsesUpdatesFromTimesheetProperty() {
		Field field = Configuration.getFieldFromId("Field.timesheetStatus");

		assertEquals(
			"entered",
			field.getValue(new StubUpdatesFromTimesheet(TimesheetStatus.ENTERED), null));
	}

	private static List<UpdatesFromTimesheet> updates(UpdatesFromTimesheet... entries) {
		return Arrays.asList(entries);
	}

	private static Field findField(SpreadSheetFieldArray fieldArray, String fieldId) {
		for (Field field : fieldArray) {
			if (fieldId.equals(field.getId())) {
				return field;
			}
		}
		return null;
	}

	private static final class StubUpdatesFromTimesheet implements UpdatesFromTimesheet {
		private final int status;
		private long lastUpdate;
		private boolean pending;
		private boolean applyResult;
		private int applyCount;
		private Collection appliedFieldArray;
		private long appliedUpdateDate;

		private StubUpdatesFromTimesheet(int status) {
			this.status = status;
		}

		public boolean applyTimesheet(Collection fieldArray, long timesheetUpdateDate) {
			applyCount++;
			appliedFieldArray = fieldArray;
			appliedUpdateDate = timesheetUpdateDate;
			return applyResult;
		}

		public long getLastTimesheetUpdate() {
			return lastUpdate;
		}

		public boolean isPendingTimesheetUpdate() {
			return pending;
		}

		public int getTimesheetStatus() {
			return status;
		}

		public String getTimesheetStatusName() {
			return status == TimesheetStatus.ENTERED ? "entered" : "other";
		}
	}
}
