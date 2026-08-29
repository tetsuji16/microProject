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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomReportDialogBoxCsvTest {
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
				"modules/micrproject_ui/src/main/java/com/microproject/dialog/CustomReportDialogBox.java");
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

	@Test
	void nullCellsBecomeEmptyFieldsAndSpecialValuesRoundTrip() throws Exception {
		DefaultTableModel model = new DefaultTableModel(new Object[]{"Name", "Note"}, 0);
		model.addRow(new Object[]{"Task A", "plain"});
		model.addRow(new Object[]{null, "comma, inside"});
		model.addRow(new Object[]{"quote \" inside", "line\nbreak"});
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		CustomReportDialogBox.writeReportCsv(model, out);
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
		CustomReportDialogBox.writeReportCsv(model, out);
		assertEquals("A,B\r\n", bodyWithoutBom(out), "RFC 4180 uses CRLF record separators");
	}
}
