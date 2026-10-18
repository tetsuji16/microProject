package com.microproject.dialog;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.table.DefaultTableModel;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.gantt.TaskSeriesCollection;

import org.junit.jupiter.api.Test;

import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.AssignmentService;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.undo.DataFactoryUndoController;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microproject.reports.model.ReportCsvWriter;

class CustomReportDialogBoxCsvTest {
	private static final long HOUR = 60L * 60L * 1000L;
	@Test
	void reportDialogSubscribesToScheduleChangesAndCleansUpOnDispose() throws Exception {
		String source = reportDialogSource();
		assertTrue(source.contains("implements ScheduleEventListener"));
		assertTrue(source.contains("project.addScheduleListener(this)"));
		assertTrue(source.contains("SwingUtilities.invokeLater"));
		assertTrue(source.contains("project.removeScheduleListener(this)"));
	}

	private static String reportDialogSource() throws java.io.IOException {
		for (java.nio.file.Path current = java.nio.file.Path.of("").toAbsolutePath(); current != null; current = current.getParent()) {
			java.nio.file.Path source = current.resolve(
				"modules/microproject_ui/src/main/java/com/microproject/dialog/CustomReportDialogBox.java");
			if (java.nio.file.Files.isRegularFile(source))
				return java.nio.file.Files.readString(source);
		}
		throw new java.nio.file.NoSuchFileException("CustomReportDialogBox.java");
	}

	@Test
	void reportTemplatesExposeTheFourMicrosoftProjectStartingPoints() {
		assertEquals(List.of(), CustomReportDialogBox.ReportTemplate.BLANK.fieldIds());
		assertEquals(List.of("Field.name", "Field.work", "Field.actualWork", "Field.remainingWork"),
			CustomReportDialogBox.ReportTemplate.CHART.fieldIds());
		assertEquals(CustomReportDialogBox.ReportTemplate.TABLE,
			CustomReportDialogBox.ReportTemplate.fromCode("unknown"));
		assertEquals(CustomReportDialogBox.ReportTemplate.COMPARISON,
			CustomReportDialogBox.ReportTemplate.fromCode("comparison"));
	}

	@Test
	void customReportPresetRoundTripsWithoutGlobalPreferences() {
		Map<String, String> original = new LinkedHashMap<>();
		original.put("columns", "Field.name,Field.start");
		original.put("filter", "未完了");
		original.put("group", "リソース");
		original.put("sort", "Field.start");
		original.put("summary", "true");

		assertEquals(original, CustomReportDialogBox.decodePreset(CustomReportDialogBox.encodePreset(original)));
		assertNull(CustomReportDialogBox.decodePreset("not-a-preset"));
	}

	@Test
	void chartTemplateBuildsTheMicrosoftProjectWorkSeries() {
		CategoryDataset dataset = CustomReportDialogBox.createWorkChart(List.of()).getCategoryPlot().getDataset();
		assertEquals(3, dataset.getRowCount());
		assertEquals("Work", dataset.getRowKey(0));
		assertEquals("Actual Work", dataset.getRowKey(1));
		assertEquals("Remaining Work", dataset.getRowKey(2));
		assertEquals(0D, dataset.getValue("Work", "Project").doubleValue());
		assertEquals(0D, dataset.getValue("Actual Work", "Project").doubleValue());
		assertEquals(0D, dataset.getValue("Remaining Work", "Project").doubleValue());
	}

	@Test
	void aggregationDeduplicatesVisibleSummaryRollupsAndPreservesOwnFixedCosts() {
		Project project = newProject();
		NormalTask summary = task(project, "Summary");
		NormalTask nested = task(project, "Nested");
		NormalTask first = task(project, "First");
		NormalTask second = task(project, "Second");
		attach(summary, nested, second);
		attach(nested, first);
		summary.setFixedCost(5D);
		nested.setFixedCost(7D);
		first.setFixedCost(11D);
		second.setFixedCost(13D);
		setAssignment(first, 8L * HOUR, 2L * HOUR);
		setAssignment(second, 4L * HOUR, 1L * HOUR);

		CustomReportDialogBox.TaskAggregation aggregation = CustomReportDialogBox.aggregateTasks(
			List.of(summary, nested, first, second));
		assertEquals(List.of(first, second), aggregation.tasks());
		assertEquals(12D, aggregation.summaryOwnFixedCost(), 0.001D);
		assertEquals(36D, aggregation.summaryOwnFixedCost() + aggregation.tasks().stream()
			.mapToDouble(task -> ((NormalTask) task).getCost(null)).sum(), 0.001D);
		var dataset = CustomReportDialogBox.createWorkChart(List.of(summary, nested, first, second))
			.getCategoryPlot().getDataset();
		assertEquals(12D, dataset.getValue("Work", "Project").doubleValue(), 0.000001D);
		assertEquals(3D, dataset.getValue("Actual Work", "Project").doubleValue(), 0.000001D);
		assertEquals(9D, dataset.getValue("Remaining Work", "Project").doubleValue(), 0.000001D);
	}

	@Test
	void assignedSummaryAndSummaryOnlyRowsKeepTheirOwnRollupValues() {
		Project project = newProject();
		NormalTask assignedSummary = task(project, "Assigned summary");
		NormalTask child = task(project, "Child");
		attach(assignedSummary, child);
		assignedSummary.setFixedCost(17D);
		child.setFixedCost(19D);
		setAssignment(assignedSummary, 10L * HOUR, 4L * HOUR);
		setAssignment(child, 6L * HOUR, 2L * HOUR);

		CustomReportDialogBox.TaskAggregation visibleChildren = CustomReportDialogBox.aggregateTasks(
			List.of(assignedSummary, child));
		assertEquals(List.of(assignedSummary, child), visibleChildren.tasks());
		assertEquals(0D, visibleChildren.summaryOwnFixedCost());
		CustomReportDialogBox.TaskAggregation summaryOnly = CustomReportDialogBox.aggregateTasks(List.of(assignedSummary));
		assertEquals(List.of(assignedSummary), summaryOnly.tasks());
		assertEquals(0D, summaryOnly.summaryOwnFixedCost());
		var dataset = CustomReportDialogBox.createWorkChart(List.of(assignedSummary, child))
			.getCategoryPlot().getDataset();
		assertEquals(16D, dataset.getValue("Work", "Project").doubleValue(), 0.000001D);
		assertEquals(6D, dataset.getValue("Actual Work", "Project").doubleValue(), 0.000001D);
		assertEquals(10D, dataset.getValue("Remaining Work", "Project").doubleValue(), 0.000001D);
	}

	private Project newProject() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("report-test", undo), undo);
		project.initialize(false, false);
		return project;
	}

	private NormalTask task(Project project, String name) {
		NormalTask task = project.createScriptedTask();
		task.setName(name);
		project.connectTask(task);
		return task;
	}

	private void attach(NormalTask parent, NormalTask... children) {
		java.util.ArrayList<com.microproject.grouping.core.Node> nodes = new java.util.ArrayList<>();
		for (NormalTask child : children) {
			child.setWbsParent(parent);
			nodes.add(com.microproject.grouping.core.NodeFactory.getInstance().createNode(child));
		}
		parent.setWbsChildrenNodes(nodes);
	}

	private void setAssignment(NormalTask task, long work, long actualWork) {
		ResourceImpl resource = task.getProject().getResourcePool().newResourceInstance();
		Assignment assignment = AssignmentService.getInstance().newAssignment(task, resource, 1D, 0L, this);
		assignment.setWork(work, null);
		assignment.setActualWork(actualWork, null);
	}

	@Test
	void comparisonTemplateBuildsSeparateCurrentAndBaselineTimelineSeries() {
		TaskSeriesCollection dataset = (TaskSeriesCollection) CustomReportDialogBox.createComparisonChart(List.of())
			.getCategoryPlot().getDataset();
		assertEquals(2, dataset.getSeriesCount());
		assertTrue(!dataset.getSeries(0).getKey().equals(dataset.getSeries(1).getKey()));
		assertEquals(0, dataset.getSeries(0).getItemCount());
		assertEquals(0, dataset.getSeries(1).getItemCount());
	}

	private static String bodyWithoutBom(ByteArrayOutputStream out) {
		byte[] bytes = out.toByteArray();
		return StandardCharsets.UTF_8.decode(ByteBuffer.wrap(bytes, 3, bytes.length - 3)).toString();
	}

	private static void writeCsv(DefaultTableModel model, ByteArrayOutputStream out) throws java.io.IOException {
		List<String> headers = java.util.stream.IntStream.range(0, model.getColumnCount())
			.mapToObj(model::getColumnName).toList();
		List<? extends List<?>> rows = java.util.stream.IntStream.range(0, model.getRowCount())
			.mapToObj(row -> java.util.stream.IntStream.range(0, model.getColumnCount())
				.mapToObj(column -> model.getValueAt(row, column)).toList()).toList();
		ReportCsvWriter.write(headers, rows, out);
	}

	@Test
	void nullCellsBecomeEmptyFieldsAndSpecialValuesRoundTrip() throws Exception {
		DefaultTableModel model = new DefaultTableModel(new Object[]{"Name", "Note"}, 0);
		model.addRow(new Object[]{"Task A", "plain"});
		model.addRow(new Object[]{null, "comma, inside"});
		model.addRow(new Object[]{"quote \" inside", "line\nbreak"});
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		writeCsv(model, out);
		byte[] bytes = out.toByteArray();
		assertArrayEquals(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}, java.util.Arrays.copyOf(bytes, 3), "UTF-8 BOM must prefix the file for Excel");
		List<CSVRecord> records;
		try (CSVParser parser = CSVParser.parse(bodyWithoutBom(out), CSVFormat.RFC4180)) {
			records = parser.getRecords();
		}
		assertEquals(4, records.size());
		assertEquals("Name", records.get(0).get(0));
		assertEquals("Note", records.get(0).get(1));
		assertEquals("Task A", records.get(1).get(0));
		assertEquals("plain", records.get(1).get(1));
		assertEquals("", records.get(2).get(0), "null cell must become an empty field, not the literal \"null\"");
		assertEquals("comma, inside", records.get(2).get(1));
		assertEquals("quote \" inside", records.get(3).get(0));
		assertEquals("line\nbreak", records.get(3).get(1), "embedded newline must survive the round-trip");
	}

	@Test
	void emptyModelProducesHeaderOnly() throws Exception {
		DefaultTableModel model = new DefaultTableModel(new Object[]{"A", "B"}, 0);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		writeCsv(model, out);
		assertEquals("A,B\r\n", bodyWithoutBom(out), "RFC 4180 uses CRLF record separators");
	}
}
