package com.projectlibre1.pm.graphic.spreadsheet;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;

import javax.swing.table.TableColumn;

import org.junit.jupiter.api.Test;

import com.projectlibre1.configuration.FieldDictionary;
import com.projectlibre1.field.Field;
import com.projectlibre1.graphic.configuration.SpreadSheetCategories;
import com.projectlibre1.graphic.configuration.SpreadSheetFieldArray;

class SpreadSheetUtilsTest {
	@Test
	void usageSpreadsheetCategoriesExposeCombinedFieldLists() {
		FieldDictionary dictionary = FieldDictionary.getInstance();

		assertSame(dictionary.getTaskAndAssignmentFields(),
			SpreadSheetUtils.getFieldsForCategory(SpreadSheetCategories.taskAssignmentSpreadsheetCategory));
		assertSame(dictionary.getResourceAndAssignmentFields(),
			SpreadSheetUtils.getFieldsForCategory(SpreadSheetCategories.resourceAssignmentSpreadsheetCategory));
		assertSame(dictionary.getAssignmentFields(),
			SpreadSheetUtils.getFieldsForCategory(SpreadSheetCategories.timesheetSpreadsheetCategory));
	}

	@Test
	void delaySpreadsheetResolvesFromConfiguredTaskFieldArray() {
		SpreadSheetFieldArray fieldArray = SpreadSheetFieldArray.getFromId(
			SpreadSheetCategories.taskSpreadsheetCategory,
			"Spreadsheet.Task.delay");

		assertNotNull(fieldArray);
		assertTrue(indexOf(fieldArray, "Field.delay") > 0);
		assertTrue(indexOf(fieldArray, "Field.levelingDelay") > indexOf(fieldArray, "Field.delay"));
		assertTrue(indexOf(fieldArray, "Field.predecessors") > indexOf(fieldArray, "Field.levelingDelay"));
		assertTrue(indexOf(fieldArray, "Field.resourceNames") > indexOf(fieldArray, "Field.predecessors"));
	}

	@Test
	void existingTaskSpreadsheetsStillResolve() {
		assertNotNull(SpreadSheetFieldArray.getFromId(
			SpreadSheetCategories.taskSpreadsheetCategory,
			"Spreadsheet.Task.entry"));
		assertNotNull(SpreadSheetFieldArray.getFromId(
			SpreadSheetCategories.taskSpreadsheetCategory,
			"Spreadsheet.Task.tracking"));
		assertNotNull(SpreadSheetFieldArray.getFromId(
			SpreadSheetCategories.taskSpreadsheetCategory,
			"Spreadsheet.Task.usage"));
	}

	@Test
	void trackingSpreadsheetKeepsTaskIndicatorsVisible() {
		SpreadSheetFieldArray fields = SpreadSheetFieldArray.getFromId(
			SpreadSheetCategories.taskSpreadsheetCategory,
			"Spreadsheet.Task.tracking");

		assertTrue(indexOf(fields, "Field.indicators") > indexOf(fields, "Field.id"));
		assertTrue(indexOf(fields, "Field.indicators") < indexOf(fields, "Field.name"));
	}

	@Test
	void viewColumnResolutionFollowsColumnsAfterReordering() {
		SpreadSheetFieldArray fields = SpreadSheetFieldArray.getFromId(
			SpreadSheetCategories.taskSpreadsheetCategory,
			"Spreadsheet.Task.entry");
		SpreadSheetColumnModel columns = new SpreadSheetColumnModel(fields);
		for (int i = 0; i < 3; i++)
			columns.addColumn(new TableColumn(i));

		Field firstDisplayed = (Field) fields.get(1);
		Field secondDisplayed = (Field) fields.get(2);
		assertSame(firstDisplayed, columns.getFieldInViewColumn(0));
		assertSame(secondDisplayed, columns.getFieldInViewColumn(1));

		columns.moveColumn(0, 1);
		assertSame(secondDisplayed, columns.getFieldInViewColumn(0));
		assertSame(firstDisplayed, columns.getFieldInViewColumn(1));
	}

	private static int indexOf(SpreadSheetFieldArray fieldArray, String fieldId) {
		int index = 0;
		for (Iterator i = fieldArray.iterator(); i.hasNext(); index++) {
			Field field = (Field) i.next();
			if (fieldId.equals(field.getId())) {
				return index;
			}
		}
		return -1;
	}
}
