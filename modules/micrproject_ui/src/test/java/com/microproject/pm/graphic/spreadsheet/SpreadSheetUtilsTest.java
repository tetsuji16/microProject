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
package com.microproject.pm.graphic.spreadsheet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;

import javax.swing.SwingUtilities;
import javax.swing.table.TableColumn;

import org.junit.jupiter.api.Test;

import com.microproject.configuration.FieldDictionary;
import com.microproject.field.Field;
import com.microproject.graphic.configuration.SpreadSheetCategories;
import com.microproject.graphic.configuration.SpreadSheetFieldArray;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.graphic.model.cache.ReferenceNodeModelCache;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

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
	void filteredAssignmentSpreadsheetKeepsTheRequestedColumnCategory() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			DataFactoryUndoController undoController = new DataFactoryUndoController();
			Project project = Project.createProject(
				ResourcePool.createRourcePool("filtered-category", undoController), undoController);
			ReferenceNodeModelCache reference = NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel());
			NodeModelCache cache = NodeModelCacheFactory.getInstance().createFilteredCache(
				reference, "View.ResourceInformation.Assignments", null);

			SpreadSheet sheet = SpreadSheetUtils.createFilteredSpreadsheet(cache,
				SpreadSheetCategories.taskAssignmentSpreadsheetCategory,
				"Spreadsheet.Assignment.taskUsage", false, new String[] { "Delete" });

			assertEquals(SpreadSheetCategories.taskAssignmentSpreadsheetCategory, sheet.getSpreadSheetCategory());
			assertSame(FieldDictionary.getInstance().getTaskAndAssignmentFields(), sheet.getAvailableFields());
			assertEquals("Delete", sheet.getActionList()[0]);
			sheet.cleanUp();
		});
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

	@Test
	void usageSpreadsheetsStillResolve() {
		assertNotNull(SpreadSheetFieldArray.getFromId(
			SpreadSheetCategories.taskAssignmentSpreadsheetCategory,
			"Spreadsheet.Assignment.taskUsage"));
		assertNotNull(SpreadSheetFieldArray.getFromId(
			SpreadSheetCategories.resourceAssignmentSpreadsheetCategory,
			"Spreadsheet.Assignment.resourceUsage"));
		assertNotNull(SpreadSheetFieldArray.getFromId(
			SpreadSheetCategories.timeSpreadsheetCategory,
			"Spreadsheet.TaskUsage.default"));
		assertNotNull(SpreadSheetFieldArray.getFromId(
			SpreadSheetCategories.timeSpreadsheetCategory,
			"Spreadsheet.ResourceUsage.default"));
	}

	@Test
	void clearingAnUninitializedSpreadsheetDoesNotAssumeACommonModel() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet sheet = new SpreadSheet();
			try {
				sheet.clearActions();
			} finally {
				sheet.cleanUp();
			}
		});
	}

	@Test
	void taskColumnMinimumWidthsProtectReadableValues() {
		assertTrue(SpreadSheetColumnModel.minimumReadableWidth("Field.name") >= 140);
		assertTrue(SpreadSheetColumnModel.minimumReadableWidth("Field.duration") >= 60);
		assertTrue(SpreadSheetColumnModel.minimumReadableWidth("Field.start") >= 100);
		assertTrue(SpreadSheetColumnModel.minimumReadableWidth("Field.finish") >= 100);
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
