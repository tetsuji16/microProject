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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CustomReportDialogBoxCsvTest {
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
