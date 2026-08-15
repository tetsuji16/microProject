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
