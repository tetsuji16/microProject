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

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

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
	void timesheetStatusFieldUsesUpdatesFromTimesheetProperty() {
		Field field = Configuration.getFieldFromId("Field.timesheetStatus");

		assertEquals(
			"entered",
			field.getValue(new StubUpdatesFromTimesheet(TimesheetStatus.ENTERED), null));
	}

	private static Collection updates(UpdatesFromTimesheet... entries) {
		return Arrays.asList(entries);
	}

	private static Field findField(SpreadSheetFieldArray fieldArray, String fieldId) {
		for (Iterator i = fieldArray.iterator(); i.hasNext();) {
			Field field = (Field) i.next();
			if (fieldId.equals(field.getId())) {
				return field;
			}
		}
		return null;
	}

	private static final class StubUpdatesFromTimesheet implements UpdatesFromTimesheet {
		private final int status;

		private StubUpdatesFromTimesheet(int status) {
			this.status = status;
		}

		public boolean applyTimesheet(Collection fieldArray, long timesheetUpdateDate) {
			return false;
		}

		public long getLastTimesheetUpdate() {
			return 0;
		}

		public boolean isPendingTimesheetUpdate() {
			return status == TimesheetStatus.VALIDATED;
		}

		public int getTimesheetStatus() {
			return status;
		}

		public String getTimesheetStatusName() {
			return status == TimesheetStatus.ENTERED ? "entered" : "other";
		}
	}
}
