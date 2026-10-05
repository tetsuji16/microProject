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

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.swing.table.TableColumn;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import com.microproject.configuration.FieldDictionary;
import com.microproject.field.Field;
import com.microproject.graphic.configuration.SpreadSheetCategories;
import com.microproject.graphic.configuration.SpreadSheetFieldArray;

class SpreadsheetCategoryAndColumnOrderTest {
	private record CategoryCase(String name, String category, Supplier<List> expected) {}
	private record ColumnCase(String name, int count, int from, int to) {}

	@TestFactory
	Stream<DynamicTest> spreadsheetCategoryCases() {
		FieldDictionary dictionary = FieldDictionary.getInstance();
		List<CategoryCase> cases = List.of(
			new CategoryCase("project", SpreadSheetCategories.projectSpreadsheetCategory, dictionary::getProjectFields),
			new CategoryCase("task", SpreadSheetCategories.taskSpreadsheetCategory, dictionary::getTaskFields),
			new CategoryCase("resource", SpreadSheetCategories.resourceSpreadsheetCategory, dictionary::getResourceFields),
			new CategoryCase("task-assignment", SpreadSheetCategories.taskAssignmentSpreadsheetCategory, dictionary::getTaskAndAssignmentFields),
			new CategoryCase("resource-assignment", SpreadSheetCategories.resourceAssignmentSpreadsheetCategory, dictionary::getResourceAndAssignmentFields),
			new CategoryCase("timesheet", SpreadSheetCategories.timesheetSpreadsheetCategory, dictionary::getAssignmentFields),
			new CategoryCase("legacy-assignment-entry-alias", "assignmentEntrySpreadsheet", dictionary::getAssignmentFields),
			new CategoryCase("dependency", SpreadSheetCategories.dependencySpreadsheetCategory, dictionary::getDependencyFields),
			new CategoryCase("unknown", "unknownSpreadsheetCategory", () -> null),
			new CategoryCase("null", null, () -> null));
		return cases.stream().map(c -> DynamicTest.dynamicTest(c.name, () -> {
			List actual = SpreadSheetUtils.getFieldsForCategory(c.category);
			List expected = c.expected.get();
			if (expected == null) assertNull(actual); else assertSame(expected, actual);
		}));
	}

	@TestFactory
	Stream<DynamicTest> reorderedColumnResolutionCases() {
		List<ColumnCase> cases = List.of(
			new ColumnCase("adjacent-swap", 2, 0, 1),
			new ColumnCase("first-to-last", 3, 0, 2),
			new ColumnCase("last-to-first", 3, 2, 0),
			new ColumnCase("middle-to-last", 6, 2, 5),
			new ColumnCase("last-to-middle", 6, 5, 2));
		SpreadSheetFieldArray fields = SpreadSheetFieldArray.getFromId(
			SpreadSheetCategories.taskSpreadsheetCategory, "Spreadsheet.Task.entry");
		return cases.stream().map(c -> DynamicTest.dynamicTest(c.name, () -> {
			SpreadSheetColumnModel columns = new SpreadSheetColumnModel(fields);
			List<Field> expected = new ArrayList<>();
			for (int modelColumn = 0; modelColumn <= c.count; modelColumn++) {
				columns.addColumn(new TableColumn(modelColumn));
				if (modelColumn > 0)
					expected.add((Field) fields.get(modelColumn));
			}
			columns.moveColumn(c.from, c.to);
			Field moved = expected.remove(c.from);
			expected.add(c.to, moved);
			for (int column = 0; column < c.count; column++)
				assertSame(expected.get(column), columns.getFieldInViewColumn(column));
		}));
	}

}
