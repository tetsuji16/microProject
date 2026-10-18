/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.export;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfArray;
import com.lowagie.text.pdf.DefaultFontMapper;
import com.lowagie.text.pdf.PdfDictionary;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PRStream;
import com.lowagie.text.Document;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.microproject.field.Field;
import com.microproject.print.GraphPageable;
import com.microproject.print.ExtendedPageFormat;
import com.microproject.print.ViewPrintable;
import com.microproject.reports.model.ReportModel;
import com.microproject.reports.model.ReportModel.Section;
import com.microproject.reports.render.ReportPrintable;
import com.microproject.offline_graphics.GanttSVGRenderer;
import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.task.Project;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.undo.DataFactoryUndoController;

class PageablePdfWriterTest {
	@TempDir Path directory;

	@Test
	void projectDetailsPdfKeepsAllSectionHeadingsAndValuesSelectableInBothLocales() throws Exception {
		Locale originalLocale = Locale.getDefault();
		try {
			for (Locale locale : List.of(Locale.ENGLISH, Locale.JAPANESE)) {
				Locale.setDefault(locale);
				ArrayList<Field> fields = new ArrayList<>();
				ArrayList<String> labels = new ArrayList<>();
				ArrayList<Object> values = new ArrayList<>();
				String[] ids = { "Field.start", "Field.baselineStart", "Field.actualStart", "Field.finish",
					"Field.baselineFinish", "Field.actualFinish", "Field.duration", "Field.baselineDuration",
					"Field.remainingDuration", "Field.actualDuration", "Field.percentComplete", "Field.work",
					"Field.baselineWork", "Field.remainingWork", "Field.actualWork", "Field.cost", "Field.baselineCost",
					"Field.cv", "Field.remainingCost", "Field.actualCost", "Field.notes" };
				for (int i = 0; i < ids.length; i++) {
					Field field = new Field(); field.setId(ids[i]); field.setName("Hidden field " + i); fields.add(field);
					labels.add("Visible label " + i); values.add("Value " + i + " 日本語");
				}
				List<Section> sections = List.of(new Section(locale.equals(Locale.JAPANESE) ? "日付" : "Dates", 0, 6),
					new Section(locale.equals(Locale.JAPANESE) ? "期間" : "Duration", 6, 11),
					new Section(locale.equals(Locale.JAPANESE) ? "作業時間" : "Work", 11, 15),
					new Section(locale.equals(Locale.JAPANESE) ? "コスト" : "Costs", 15, 20),
					new Section(locale.equals(Locale.JAPANESE) ? "メモ" : "Notes", 20, 21));
				ReportModel model = new ReportModel("Project Details", fields, List.of(values),
					new int[fields.size()], -1, java.util.Collections.singletonList(null), "日本語プロジェクト",
					false, true, true, labels, sections);
				File output = directory.resolve("project-details-" + locale.getLanguage() + ".pdf").toFile();
				new PageablePdfWriter().write(new GraphPageable(new ReportPrintable(model), false), output, ignored -> { });
				try (PdfReader reader = new PdfReader(output.getAbsolutePath())) {
					String text = new PdfTextExtractor(reader).getTextFromPage(1);
					for (Section section : sections) assertTrue(text.contains(section.title()), "Missing " + section.title() + " in " + text);
					for (int i = 0; i < values.size(); i++)
						assertTrue(text.contains((String) values.get(i)), "Missing value " + i + " in " + text);
					assertTrue(text.contains("日本語プロジェクト"));
					assertTrue(!text.contains("Hidden field"));
					assertTrue(hasEmbeddedReportFace(reader, "NotoSansJP-Regular"), "PDF must embed the regular report font");
					assertTrue(hasEmbeddedReportFace(reader, "NotoSansJP-Bold"), "PDF must embed the bold report font");
				}
			}
		} finally { Locale.setDefault(originalLocale); }
	}

	private static boolean hasEmbeddedReportFace(PdfReader reader, String faceName) throws Exception {
		for (int pageNumber = 1; pageNumber <= reader.getNumberOfPages(); pageNumber++) {
			PdfDictionary resources = reader.getPageN(pageNumber).getAsDict(PdfName.RESOURCES);
			if (resources == null) continue;
			PdfDictionary fonts = resources.getAsDict(PdfName.FONT);
			if (fonts == null) continue;
			for (PdfName key : fonts.getKeys()) {
				PdfDictionary font = fonts.getAsDict(key);
				if (font == null) continue;
				PdfArray descendants = font.getAsArray(PdfName.DESCENDANTFONTS);
				if (descendants != null && descendants.size() > 0) font = descendants.getAsDict(0);
				if (font == null) continue;
				PdfName name = font.getAsName(PdfName.BASEFONT);
				PdfDictionary descriptor = font.getAsDict(PdfName.FONTDESCRIPTOR);
				if (name == null || descriptor == null || !name.toString().contains(faceName)) continue;
				for (PdfName fontFile : List.of(PdfName.FONTFILE, PdfName.FONTFILE2, PdfName.FONTFILE3)) {
					if (PdfReader.getPdfObject(descriptor.get(fontFile)) instanceof PRStream stream
							&& PdfReader.getStreamBytes(stream).length > 0)
						return true;
				}
			}
		}
		return false;
	}

	@Test
	void cjkPdfMapperUsesDistinctRealFacesForRegularAndBoldStylesWhenAvailable() {
		DefaultFontMapper mapper = PageablePdfWriter.embeddedJapaneseFontMapper();
		DefaultFontMapper.BaseFontParameters regular = mapper.getBaseFontParameters("Dialog.plain");
		DefaultFontMapper.BaseFontParameters bold = mapper.getBaseFontParameters("Dialog.bold");
		assertTrue(regular != null && bold != null, "the bundled Japanese font must map both logical styles");
		assertNotEquals(regular.fontName, bold.fontName, "regular and bold must use distinct bundled faces");
		assertTrue(regular.ttfAfm.length > 0 && bold.ttfAfm.length > 0, "font data must come from bundled resources");
	}

	@Test
	void embedsCjkFontAndKeepsJapaneseTextSelectableInReportPdf() throws Exception {
		Field field = new Field();
		field.setId("Field.name");
		field.setName("プロジェクト名");
		field.setColumnWidth(400);
		ReportModel model = new ReportModel("日本語プロジェクト", List.of(field),
			List.of(List.of("日本語の選択可能なPDF文字列")), new int[] { 400 }, -1,
			java.util.Collections.singletonList(null), "日本語プロジェクト", false, false, false);
		Locale originalLocale = Locale.getDefault();
		try {
			for (Locale locale : List.of(Locale.ENGLISH, Locale.JAPANESE)) {
				Locale.setDefault(locale);
				File output = directory.resolve("report-" + locale.getLanguage() + ".pdf").toFile();
				new PageablePdfWriter().write(new GraphPageable(new ReportPrintable(model), false), output, ignored -> { });
				try (PdfReader reader = new PdfReader(output.getAbsolutePath())) {
					String text = new PdfTextExtractor(reader).getTextFromPage(1);
					assertTrue(text.contains("日本語プロジェクト"), () -> "Expected embedded-font Japanese PDF text, got: " + text);
					assertTrue(text.contains("選択可能なPDF文字列"), () -> "Expected selectable Japanese report data, got: " + text);
					assertTrue(text.contains(locale.equals(Locale.JAPANESE) ? "ページ 1" : "Page 1"),
						() -> "Expected locale-specific footer for " + locale + ", got: " + text);
				}
			}
		} finally {
			Locale.setDefault(originalLocale);
		}
	}

	@Test
	void longProjectDetailsNotesContinueToTheLastPdfPage() throws Exception {
		Field notes = new Field();
		notes.setId("Field.notes");
		notes.setName("Notes / 注記");
		notes.setColumnWidth(400);
		String ending = "ISSUE753_NOTES_END_日本語";
		String longText = "長文Notesの継続確認。 ".repeat(220) + ending;
		ReportModel model = new ReportModel("Project Details", List.of(notes), List.of(List.of(longText)),
			new int[] { 400 }, -1, java.util.Collections.singletonList(null), "日本語 project", false, true, true);
		ReportPrintable report = new ReportPrintable(model);
		assertTrue(report.getNumberOfPages() > 1, "Long Notes must be split across physical pages");
		File output = directory.resolve("long-notes.pdf").toFile();
		new PageablePdfWriter().write(new GraphPageable(report, false), output, ignored -> { });
		try (PdfReader reader = new PdfReader(output.getAbsolutePath())) {
			assertTrue(reader.getNumberOfPages() > 1);
			StringBuilder text = new StringBuilder();
			for (int page = 1; page <= reader.getNumberOfPages(); page++)
				text.append(new PdfTextExtractor(reader).getTextFromPage(page));
			assertTrue(text.toString().replaceAll("\\s+", "").contains(ending),
				"Final Notes sentinel was clipped: " + text);
		}
	}

	@Test
	void sharedPdfWriterStillRendersGraphPageableTaskText() throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("graph-pdf-test", undo), undo);
		project.initialize(false, false);
		NormalTask task = project.createScriptedTask();
		task.setName("Graph PDF task 日本語");
		project.recalculate();
		var referenceCache = NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel());
		var cache = NodeModelCacheFactory.getInstance().createFilteredCache(referenceCache, "OfflineGantt", null);
		GanttSVGRenderer renderer = new GanttSVGRenderer();
		renderer.init(project, cache, null, null, -1, true);
		GraphPageable pageable = new GraphPageable(renderer, false, false, true);
		File output = directory.resolve("graph.pdf").toFile();
		File legacyOutput = directory.resolve("graph-legacy-baseline.pdf").toFile();
		writeWithLegacyGraphPdfRoute(pageable, legacyOutput);
		new PageablePdfWriter().write(pageable, output, ignored -> { });
		try (PdfReader reader = new PdfReader(output.getAbsolutePath());
				PdfReader legacyReader = new PdfReader(legacyOutput.getAbsolutePath())) {
			assertTrue(reader.getNumberOfPages() > 0, "the shared graph path must still emit printable pages");
			String text = new PdfTextExtractor(reader).getTextFromPage(1);
			String legacyText = new PdfTextExtractor(legacyReader).getTextFromPage(1);
			assertTrue(text.contains("Graph PDF task"), "the existing graph PDF path must retain rendered task text: " + text);
			assertTrue(text.equals(legacyText), "the graph PDF route must preserve the previous default-font output; baseline="
				+ legacyText + " current=" + text);
			assertTrue(text.contains("?0%"), "the old default-font route did not support CJK task text on this host: " + text);
		}
	}

	/** Recreates the pre-#753 GraphPageable PDF path as a test-only output baseline. */
	private static void writeWithLegacyGraphPdfRoute(GraphPageable pageable, File file) throws Exception {
		Document document = new Document();
		try (FileOutputStream output = new FileOutputStream(file)) {
			PdfWriter writer = PdfWriter.getInstance(document, output);
			try {
				pageable.update();
				int pageCount = pageable.getNumberOfPages();
				if (pageCount == 0) return;
				ViewPrintable printable = pageable.getSafePrintable();
				ExtendedPageFormat pageFormat = pageable.getSafePageFormat();
				float width = (float) pageFormat.getWidth(), height = (float) pageFormat.getHeight();
				for (int page = 0; page < pageCount; page++) {
					document.setPageSize(new Rectangle(width, height));
					if (page == 0) document.open(); else document.newPage();
					java.awt.Graphics2D graphics = writer.getDirectContent().createGraphics(width, height);
					try { printable.print(graphics, page); } finally { graphics.dispose(); }
				}
			} finally {
				if (document.isOpen()) document.close();
			}
		}
	}
}
