/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.reports.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.field.Field;
import com.microproject.reports.model.ReportLayout;
import com.microproject.reports.model.ReportModel;
import com.microproject.reports.model.ReportTextLayout;

class ReportRenderingTest {
	@Test
	void bundledJapaneseFacesRenderGlyphsAndProvideSharedLayoutMetrics() {
		var regular = ReportFontProvider.regular(10);
		var bold = ReportFontProvider.bold(10);
		assertTrue(regular.canDisplay('日') && bold.canDisplay('本'));
		assertNotEquals(regular.getFontName(), bold.getFontName());
		assertTrue(ReportTextLayout.lineCount("日本語の長いレポート文字列", 45) > 1);
	}

	@Test
	void bundledFontsPaintJapaneseInkWithoutInstalledSystemFonts() throws Exception {
		ReportModel model = new ReportModel("日本語レポート", List.of(field("Field.name", "名称")),
			List.of(List.of("日本語の本文")), new int[] { 400 }, -1,
			java.util.Collections.singletonList(null), "日本語プロジェクト", false, false, false);
		BufferedImage image = new BufferedImage(800, 1200, BufferedImage.TYPE_INT_ARGB);
		ReportPrintable printable = new ReportPrintable(model);
		var graphics = image.createGraphics();
		try { printable.print(graphics, printable.getPageFormat(0), 0); }
		finally { graphics.dispose(); }
		assertTrue(inkPixels(image) > 100);
	}

	@Test
	void wideGroupedTableSplitsColumnsAndRepeatsEveryRowOnEachSegment() throws Exception {
		List<Field> columns = List.of(field("Field.name", "Task"), field("Field.start", "Start"),
			field("Field.finish", "Finish"), field("Field.cost", "Cost"));
		ReportModel model = new ReportModel("Task Information", columns,
			List.of(List.of("Alpha 日本語", "2026-01-01", "2026-01-02", 10.5),
				List.of("Beta", "2026-01-03", "2026-01-04", 20.0)),
			new int[] { 600, 500, 500, 600 }, 0, List.of("A", "B"), "Plan", true, false, false);
		ReportPrintable printable = new ReportPrintable(model);
		ReportLayout layout = printable.layout();
		assertTrue(layout.segmentCount() > 1);
		assertEquals(layout.segmentCount(), printable.getNumberOfPages());
		for (int page = 0; page < printable.getNumberOfPages(); page++) {
			assertEquals(0, layout.page(page).firstRow());
			assertEquals(2, layout.page(page).endRow());
			BufferedImage image = new BufferedImage(1200, 800, BufferedImage.TYPE_INT_ARGB);
			printable.print(image.createGraphics(), printable.getPageFormat(page), page);
			assertTrue(inkPixels(image) > 100, "Expected the report page to contain rendered marks");
		}
	}

	@Test
	void projectDetailsNotesUseVariablePagesWithoutLosingFields() throws Exception {
		ArrayList<Field> fields = new ArrayList<>();
		ArrayList<Object> values = new ArrayList<>();
		for (int i = 0; i < 40; i++) {
			String id = i == 9 ? "Field.notes" : "Field.value" + i;
			fields.add(field(id, "Field " + i));
			values.add(i == 9 ? "日本語 long notes ".repeat(500) : "value-" + i);
		}
		ReportModel model = new ReportModel("Project Details", fields, List.of(values),
			fields.stream().mapToInt(ignored -> 120).toArray(), -1, java.util.Collections.singletonList(null),
			"日本語プロジェクト", false, true, true);
		ReportPrintable printable = new ReportPrintable(model);
		assertTrue(printable.getNumberOfPages() > 1);
		int next = 0;
		int previousFragmentEnd = -1;
		for (int page = 0; page < printable.getNumberOfPages(); page++) {
			ReportLayout.Page range = printable.layout().page(page);
			if (range.hasRowFragment()) {
				assertEquals(9, range.firstRow());
				assertEquals(10, range.endRow());
				assertEquals(previousFragmentEnd < 0 ? 0 : previousFragmentEnd, range.firstRowLine());
				assertTrue(range.endRowLine() > range.firstRowLine());
				previousFragmentEnd = range.endRowLine();
			} else {
				if (previousFragmentEnd >= 0 && next == 9) assertTrue(range.firstRow() > 9);
				else assertEquals(next, range.firstRow());
				assertTrue(range.endRow() > range.firstRow());
				next = range.endRow();
			}
			BufferedImage image = new BufferedImage(800, 1200, BufferedImage.TYPE_INT_ARGB);
			printable.print(image.createGraphics(), printable.getPageFormat(page), page);
			assertTrue(inkPixels(image) > 100, "Expected the Project Details page to contain rendered marks");
		}
		assertEquals(fields.size(), next);
		assertTrue(previousFragmentEnd > 1, "Expected the long Notes field to continue across pages");
	}

	@Test
	void sectionStartingAtPageBoundaryMovesTogetherWithItsFirstValue() throws Exception {
		ArrayList<Field> fields = new ArrayList<>();
		ArrayList<Object> values = new ArrayList<>();
		for (int i = 0; i < 8; i++) {
			fields.add(field("Field.section" + i, "Value " + i));
			values.add("boundary value " + i + "日".repeat(i < 2 ? 240 : 210));
		}
		fields.add(field("Field.nextSection", "Next value"));
		values.add("next section value 日本語");
		ReportModel model = new ReportModel("Section boundary", fields, List.of(values),
			fields.stream().mapToInt(ignored -> 400).toArray(), -1,
			java.util.Collections.singletonList(null), "Section boundary", false, true, true,
			fields.stream().map(Field::getName).toList(), List.of(
				new ReportModel.Section("First section", 0, 8), new ReportModel.Section("Next section", 8, 9)));
		ReportPrintable printable = new ReportPrintable(model);
		assertEquals(8, printable.layout().page(0).endRow(), "the first section should end at the page boundary");
		assertEquals(8, printable.layout().page(1).firstRow(), "the next section and heading should start together");
		BufferedImage image = new BufferedImage(800, 1200, BufferedImage.TYPE_INT_ARGB);
		var graphics = image.createGraphics();
		try { printable.print(graphics, printable.getPageFormat(1), 1); }
		finally { graphics.dispose(); }
		assertTrue(inkPixels(image) > 100, "the continuation page must render its heading and first value");
	}

	private static Field field(String id, String name) {
		Field field = new Field();
		field.setId(id);
		field.setName(name);
		field.setColumnWidth(100);
		return field;
	}

	private static long inkPixels(BufferedImage image) {
		long count = 0;
		for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
			int pixel = image.getRGB(x, y);
			if ((pixel >>> 24) != 0 && (pixel & 0x00ffffff) != 0x00ffffff) count++;
		}
		return count;
	}
}
