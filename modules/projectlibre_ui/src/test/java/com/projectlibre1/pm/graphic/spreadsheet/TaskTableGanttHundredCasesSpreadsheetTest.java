package com.projectlibre1.pm.graphic.spreadsheet;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.swing.table.TableColumn;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import com.projectlibre1.configuration.FieldDictionary;
import com.projectlibre1.field.Field;
import com.projectlibre1.graphic.configuration.SpreadSheetCategories;
import com.projectlibre1.graphic.configuration.SpreadSheetFieldArray;

class TaskTableGanttHundredCasesSpreadsheetTest {
	private record CategoryCase(String category, Supplier<List> expected) {}
	private record ColumnCase(int count, int from, int to) {}

	@TestFactory
	Stream<DynamicTest> spreadsheetCategoryCases() {
		FieldDictionary dictionary = FieldDictionary.getInstance();
		List<CategoryCase> cases = List.of(
			new CategoryCase(SpreadSheetCategories.projectSpreadsheetCategory, dictionary::getProjectFields),
			new CategoryCase(SpreadSheetCategories.taskSpreadsheetCategory, dictionary::getTaskFields),
			new CategoryCase(SpreadSheetCategories.resourceSpreadsheetCategory, dictionary::getResourceFields),
			new CategoryCase(SpreadSheetCategories.taskAssignmentSpreadsheetCategory, dictionary::getTaskAndAssignmentFields),
			new CategoryCase(SpreadSheetCategories.resourceAssignmentSpreadsheetCategory, dictionary::getResourceAndAssignmentFields),
			new CategoryCase(SpreadSheetCategories.timesheetSpreadsheetCategory, dictionary::getAssignmentFields),
			new CategoryCase("assignmentEntrySpreadsheet", dictionary::getAssignmentFields),
			new CategoryCase(SpreadSheetCategories.dependencySpreadsheetCategory, dictionary::getDependencyFields),
			new CategoryCase("unknownSpreadsheetCategory", () -> null),
			new CategoryCase(null, () -> null));
		return IntStream.range(0, cases.size()).mapToObj(index -> DynamicTest.dynamicTest(
			id(index + 61), () -> {
				CategoryCase c = cases.get(index);
				List actual = SpreadSheetUtils.getFieldsForCategory(c.category);
				List expected = c.expected.get();
				if (expected == null) assertNull(actual); else assertSame(expected, actual);
			}));
	}

	@TestFactory
	Stream<DynamicTest> reorderedColumnResolutionCases() {
		List<ColumnCase> cases = List.of(
			new ColumnCase(2, 0, 1), new ColumnCase(2, 1, 0),
			new ColumnCase(3, 0, 2), new ColumnCase(3, 2, 0),
			new ColumnCase(4, 1, 3), new ColumnCase(4, 3, 1),
			new ColumnCase(5, 0, 4), new ColumnCase(5, 4, 0),
			new ColumnCase(6, 2, 5), new ColumnCase(6, 5, 2));
		SpreadSheetFieldArray fields = SpreadSheetFieldArray.getFromId(
			SpreadSheetCategories.taskSpreadsheetCategory, "Spreadsheet.Task.entry");
		return IntStream.range(0, cases.size()).mapToObj(index -> DynamicTest.dynamicTest(
			id(index + 71), () -> {
				ColumnCase c = cases.get(index);
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

	private static String id(int number) {
		return "TC100-S" + String.format("%03d", number);
	}
}
