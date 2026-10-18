/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.reports.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import com.microproject.configuration.Configuration;
import com.microproject.configuration.Dictionary;
import com.microproject.configuration.ReportDefinition;
import com.microproject.field.Field;
import com.microproject.graphic.configuration.SpreadSheetFieldArray;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.grouping.core.transform.filtering.PredicatedNodeFilterIterator;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.AssignmentService;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.reports.adapter.DataSourceProvider;
import com.microproject.reports.adapter.ReportDataSource;
import com.microproject.reports.render.ReportPrintable;
import com.microproject.undo.DataFactoryUndoController;

/** Compares replacement output and data against frozen pages captured from the legacy Jasper renderer. */
class LegacyReportBaselineTest {
	private static final List<String> EXPECTED_REPORTS = List.of(
		"Task Information", "Resource Information", "Who Does What", "Project Details");

	@Test
	void allFourLegacyReportsRenderRealProjectRowsThatReplacementRetains() throws Exception {
		Configuration.getInstance();
		Dictionary.getInstance();
		installWhoDoesWhatFieldArrays();
		Project project = fixtureProject();
		Set<String> measuredDifferences = new TreeSet<>();
		Locale originalLocale = Locale.getDefault();
		try {
		for (Locale locale : List.of(Locale.JAPANESE, Locale.ENGLISH)) {
			Locale.setDefault(locale);
			int reportCount = 0;
		for (String name : EXPECTED_REPORTS) {
			ReportDefinition definition = DataSourceProvider.getFromName(name);
			assertNotNull(definition, name);
			SpreadSheetFieldArray selected = null;
			if (!definition.getColumnsList().isEmpty()) {
				selected = (SpreadSheetFieldArray) definition.getMainFieldArray().clone();
				selected.removeIf(Field::isGraphical);
			}
			List<Field> legacyFields = !definition.getColumnsList().isEmpty() ? List.copyOf(selected)
				: ReportModelFactory.projectDetailsFields();
			List<ReportDataSource.Row> probeRows = source(definition, project).rows(legacyFields, true, null);
			assertTrue(!probeRows.isEmpty(), name + " fixture contains real rows");
			List<List<String>> oldPages = legacyPages(name, locale.getLanguage());
			List<String> oldText = oldPages.stream().flatMap(List::stream).toList();
			assertTrue(!oldPages.isEmpty(), name + " frozen legacy pages");
			assertEquals(frozenRowCount(name, locale.getLanguage()), probeRows.size(),
				name + " frozen legacy and replacement receive the same project row count");
			int expectedLegacyPages = switch (name) {
				case "Task Information", "Who Does What" -> 2;
				default -> 1;
			};
			assertEquals(expectedLegacyPages, oldPages.size(), name + " frozen " + locale + " page count");
			String requiredValue = name.equals("Project Details") ? "fixture 日本語 project"
				: name.equals("Resource Information") || name.equals("Who Does What")
					? "fixture 日本語 resource A" : "fixture 日本語 task 1";
			assertTrue(oldText.stream().anyMatch(text -> text.contains(requiredValue)),
				name + " frozen legacy output is missing fixture value " + requiredValue + ": " + oldText + " model=" + probeRows);

			SpreadSheetFieldArray fields = !definition.getColumnsList().isEmpty() ? selected : null;
			ReportDataSource replacementRows = source(definition, project);
			ReportModel replacement = ReportModelFactory.create(definition, fields, replacementRows, project.getName());
			if (name.equals("Project Details")) {
				assertEquals(List.of("Field.start", "Field.baselineStart", "Field.actualStart", "Field.finish",
					"Field.baselineFinish", "Field.actualFinish", "Field.duration", "Field.baselineDuration",
					"Field.remainingDuration", "Field.actualDuration", "Field.percentComplete", "Field.work",
					"Field.baselineWork", "Field.remainingWork", "Field.actualWork", "Field.cost", "Field.baselineCost",
					"Field.cv", "Field.remainingCost", "Field.actualCost", "Field.notes"),
					replacement.columns().stream().map(Field::getId).toList());
				assertEquals(locale.equals(Locale.JAPANESE)
					? List.of("日付", "期間", "作業時間", "コスト", "メモ")
					: List.of("Dates", "Duration", "Work", "Costs", "Notes"),
					replacement.sections().stream().map(ReportModel.Section::title).toList());
				assertEquals(21, replacement.displayLabels().size());
				assertTrue(replacement.columns().stream().noneMatch(field -> List.of("Field.name", "Field.manager",
					"Field.percentWorkComplete").contains(field.getId())));
			}
			ReportPrintable printable = new ReportPrintable(replacement);
			assertTrue(printable.getNumberOfPages() > 0, name + " replacement pages");
			assertEquals(expectedReplacementPages(name), printable.layout().pages(), name + " frozen replacement page ranges");
			int expectedLayoutRows = replacement.labelValueSheet() ? replacement.columns().size() : replacement.rows().size();
			assertEquals(expectedLayoutRows, printable.layout().pages().stream()
				.mapToInt(page -> page.endRow() - page.firstRow()).sum() / printable.layout().segmentCount(),
				name + " replacement layout skipped or duplicated rows");
			if (name.equals("Task Information")) {
				assertTrue(oldPages.get(0).stream().anyMatch(text -> text.contains("fixture 日本語 task 30"))
					&& oldPages.get(0).stream().noneMatch(text -> text.contains("fixture 日本語 task 31")),
					"legacy task page 1 ends after task 30");
				assertTrue(oldPages.get(1).stream().anyMatch(text -> text.contains("fixture 日本語 task 31"))
					&& oldPages.get(1).stream().anyMatch(text -> text.contains("fixture 日本語 task 45")),
					"legacy task page 2 contains tasks 31 through 45");
				int firstTaskPage = pageContaining(oldPages, "fixture 日本語 task 1");
				int lastTaskPage = pageContaining(oldPages, "fixture 日本語 task 45");
				assertTrue(firstTaskPage >= 0 && lastTaskPage >= 0 && firstTaskPage != lastTaskPage,
					"legacy task report records the page boundary between the first and last task");
			}
			if (name.equals("Who Does What")) {
				int resourceAPage = pageContaining(oldPages, "fixture 日本語 resource A");
				int resourceBPage = pageContaining(oldPages, "fixture 日本語 resource B");
				assertTrue(resourceAPage >= 0 && resourceBPage >= 0 && resourceAPage != resourceBPage,
					"legacy page boundaries retain the two resource groups");
				long summaryRows = java.util.stream.IntStream.range(0, replacement.rows().size())
					.filter(replacement::summaryRow).count();
				assertTrue(summaryRows >= 2, "each assignment resource group gets a SUM row");
				assertTrue(oldPages.stream().flatMap(List::stream).anyMatch(text -> text.contains("184 hours"))
					&& oldPages.stream().flatMap(List::stream).anyMatch(text -> text.contains("176 hours")),
					"legacy grouped SUM includes both measured nonzero work totals: " + oldPages);
				int workColumn = java.util.stream.IntStream.range(0, replacement.columns().size())
					.filter(index -> "Field.work".equals(replacement.columns().get(index).getId())).findFirst().orElseThrow();
				List<String> nativeSums = java.util.stream.IntStream.range(0, replacement.rows().size())
					.filter(replacement::summaryRow).mapToObj(index -> replacement.rows().get(index).get(workColumn).toString()).toList();
				assertTrue(nativeSums.contains("184 hours") && nativeSums.contains("176 hours"),
					"replacement grouped SUMs match the legacy totals: " + nativeSums);
				assertEquals(0, pageForSummary(printable.layout(), replacement, workColumn, "184 hours"));
				assertEquals(1, pageForSummary(printable.layout(), replacement, workColumn, "176 hours"));
			}
			int sourceRow = 0;
			for (int row = 0; row < replacement.rows().size(); row++) {
				if (replacement.summaryRow(row)) continue;
				for (int column = 0; column < replacement.rows().get(row).size(); column++) {
					Object value = replacement.rows().get(row).get(column);
					if (!(value instanceof String expected) || expected.isBlank()) continue;
					if (oldText.stream().anyMatch(text -> text.contains(expected))) continue;
			Object raw = sourceRow < probeRows.size() ? probeRows.get(sourceRow).rawValues().get(column) : null;
					if (raw == null && "<N/A>".equals(expected)) {
						measuredDifferences.add(name + " null display delta " + replacement.columns().get(column).getId());
						continue;
					}
					assertTrue(raw instanceof Number && oldText.stream().anyMatch(text -> text.contains(raw.toString())),
					name + " frozen legacy output lacks model value: " + expected + " raw=" + raw + " pages=" + oldPages);
					measuredDifferences.add(name + " formatting delta " + replacement.columns().get(column).getId());
				}
				sourceRow++;
			}
			reportCount++;
		}
		assertEquals(EXPECTED_REPORTS.size(), reportCount);
		}
		assertEquals(Set.of("Resource Information formatting delta Field.standardRate",
			"Resource Information formatting delta Field.overtimeRate",
			"Resource Information formatting delta Field.costPerUse"), measuredDifferences);
		} finally { Locale.setDefault(originalLocale); }
	}

	private static List<List<String>> legacyPages(String report, String locale) throws Exception {
		ArrayList<List<String>> pages = new ArrayList<>();
		for (int page = 1; page <= frozenPageCount(report, locale); page++) pages.add(frozenPage(report, locale, page));
		return List.copyOf(pages);
	}

	private static int frozenPageCount(String report, String locale) throws Exception {
		return (int)baselineLines().stream().filter(line -> line.startsWith(report + "\t" + locale + "\tPAGE\t")).count();
	}

	private static int frozenRowCount(String report, String locale) throws Exception {
		return baselineLines().stream().filter(line -> line.startsWith(report + "\t" + locale + "\tROWS\t"))
			.map(line -> Integer.parseInt(line.substring(line.lastIndexOf('\t') + 1))).findFirst().orElseThrow();
	}

	private static List<String> frozenPage(String report, String locale, int page) throws Exception {
		String prefix = report + "\t" + locale + "\tPAGE\t" + page + "\t";
		String payload = baselineLines().stream().filter(line -> line.startsWith(prefix)).findFirst().orElseThrow();
		String encoded = payload.substring(prefix.length());
		return java.util.Arrays.asList(new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8).split("\u001f", -1));
	}

	private static List<String> baselineLines() throws Exception {
		try (var input = LegacyReportBaselineTest.class.getResourceAsStream("/issue-753-legacy-pages.tsv")) {
			assertNotNull(input, "Frozen legacy report baseline resource");
			return new String(input.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
		}
	}

	static Project fixtureProject() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(com.microproject.pm.resource.ResourcePool.createRourcePool("issue-753", undo), undo);
		project.initialize(false, false);
		project.setStartDate(java.time.LocalDate.of(2026, 10, 19)
			.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
		project.setName("fixture 日本語 project");
		List<NormalTask> tasks = new ArrayList<>();
		for (int i = 1; i <= 45; i++) {
			NormalTask task = (NormalTask)project.createLocalTaskNode(null).getImpl();
			task.setName("fixture 日本語 task " + i);
			tasks.add(task);
		}
		ResourceImpl[] resources = new ResourceImpl[2];
		for (int i = 0; i < resources.length; i++) {
			resources[i] = (ResourceImpl) project.getResourcePool().createScriptedResource();
			resources[i].setName("fixture 日本語 resource " + (i == 0 ? "A" : "B"));
		}
		for (int i = 0; i < tasks.size(); i++) {
			ResourceImpl resource = resources[i % resources.length];
			Assignment assignment = Assignment.getInstance(tasks.get(i), resource, 1.0D, 0L);
			assignment.setWork(i == 0 ? 36_000_000L : i == 1 ? 72_000_000L : 0L, null);
			assertTrue(AssignmentService.getInstance().connect(assignment, null, false));
		}
		return project;
	}

	private static int pageContaining(List<List<String>> pages, String text) {
		for (int page = 0; page < pages.size(); page++) {
			if (pages.get(page).stream().anyMatch(value -> value.contains(text))) return page;
		}
		return -1;
	}

	private static List<ReportLayout.Page> expectedReplacementPages(String name) {
		return switch (name) {
			case "Task Information" -> List.of(new ReportLayout.Page(0, 0, 28, false),
				new ReportLayout.Page(0, 28, 45, true), new ReportLayout.Page(1, 0, 28, false),
				new ReportLayout.Page(1, 28, 45, true));
			case "Resource Information" -> List.of(new ReportLayout.Page(0, 0, 2, false),
				new ReportLayout.Page(1, 0, 2, false), new ReportLayout.Page(2, 0, 2, false));
			case "Who Does What" -> List.of(new ReportLayout.Page(0, 0, 25, false),
				new ReportLayout.Page(0, 25, 47, true));
			// Explicit legacy fields and section headings now fit on one page with the bundled font metrics.
			case "Project Details" -> List.of(new ReportLayout.Page(0, 0, 21, false));
			default -> throw new IllegalArgumentException("Unknown report baseline " + name);
		};
	}

	private static int pageForSummary(ReportLayout layout, ReportModel model, int column, String value) {
		for (int row = 0; row < model.rows().size(); row++) {
			if (!model.summaryRow(row) || !value.equals(String.valueOf(model.rows().get(row).get(column)))) continue;
			for (int page = 0; page < layout.pages().size(); page++) {
				ReportLayout.Page range = layout.page(page);
				if (range.segmentIndex() == 0 && row >= range.firstRow() && row < range.endRow()) return page;
			}
		}
		return -1;
	}

	static void installWhoDoesWhatFieldArrays() {
		ReportDefinition definition = DataSourceProvider.getFromName("Who Does What");
		for (var columns : definition.getColumnsList()) {
			SpreadSheetFieldArray fields = columns.getFieldArray();
			assertNotNull(fields, "production Dictionary loads report spreadsheet " + columns.getIdSpreadSheet());
			List<String> expectedFieldIds = columns.getIdSpreadSheet().endsWith("reportAssignmentResource")
				? List.of("Field.assignmentResourceId", "Field.assignmentResourceName")
				: List.of("Field.assignmentTaskId", "Field.assignmentTaskName", "Field.work",
					"Field.assignmentUnits", "Field.delay", "Field.start", "Field.finish");
			assertEquals(expectedFieldIds, fields.stream().map(Field::getId).toList(),
				"production view.xml preserves report field order");
		}
	}

	static ReportDataSource source(ReportDefinition definition, Project project) {
		if (definition.getColumnsList().isEmpty()) return DataSourceProvider.createProjectDataSource(project, definition.isTimeBased());
		NodeModel model = definition.getCollectionType() == DataSourceProvider.TASKS_TREE
			? project.getTaskModel() : project.getResourceModel();
		return DataSourceProvider.createDataSource(definition.getCollectionType(), project,
			new ModelIterator(model.iterator()), model, definition.isTimeBased());
	}

	static final class ModelIterator implements PredicatedNodeFilterIterator {
		private final Iterator<?> delegate;
		private Predicate<Object> predicate = ignored -> true;
		private Object next;
		private boolean prepared;
		private boolean nodeBased;
		private ModelIterator(Iterator<?> delegate) { this.delegate = delegate; }
		@Override public void setPredicate(Predicate<Object> predicate) { this.predicate = predicate; }
		@Override public void setNodeBased(boolean nodeBased) { this.nodeBased = nodeBased; }
		@Override public boolean hasNext() {
			while (!prepared && delegate.hasNext()) {
				Object candidate = delegate.next();
				Object value = candidate instanceof com.microproject.grouping.core.Node node ? node.getImpl() : candidate;
				if (predicate.test(value)) { next = candidate; prepared = true; }
			}
			return prepared;
		}
		@Override public Object next() {
			if (!hasNext()) throw new java.util.NoSuchElementException();
			prepared = false;
			return !nodeBased && next instanceof com.microproject.grouping.core.Node node ? node.getImpl() : next;
		}
	}

}
