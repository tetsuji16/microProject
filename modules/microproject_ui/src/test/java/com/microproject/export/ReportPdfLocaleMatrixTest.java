/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.Predicate;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
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
import com.microproject.print.GraphPageable;
import com.microproject.reports.adapter.DataSourceProvider;
import com.microproject.reports.adapter.ReportDataSource;
import com.microproject.reports.model.ReportLayout;
import com.microproject.reports.model.ReportModel;
import com.microproject.reports.model.ReportModelFactory;
import com.microproject.reports.render.ReportPrintable;
import com.microproject.strings.Messages;
import com.microproject.undo.DataFactoryUndoController;

/**
 * M2.4 (#794): each built-in report, rendered to PDF for the same fixture in Japanese and English, must show
 * every fixture cell in row order, never split a row across pages, repeat the localized table header on every
 * page, and paginate deterministically (landscape and the narrower portrait page).
 */
class ReportPdfLocaleMatrixTest {
	private static final List<String> REPORTS = List.of("Task Information", "Resource Information",
		"Who Does What", "Project Details");
	private static final int TASK_COUNT = 45;
	@TempDir Path directory;

	static List<Object[]> cases() {
		ArrayList<Object[]> cases = new ArrayList<>();
		for (Locale locale : List.of(Locale.JAPANESE, Locale.ENGLISH))
			for (String report : REPORTS) cases.add(new Object[] { locale, report });
		return cases;
	}

	@ParameterizedTest(name = "{1} / {0}")
	@MethodSource("cases")
	void reportPdfShowsFixtureCellsInOrderWithLocalizedLabelsAndStablePages(Locale locale, String name)
			throws Exception {
		Locale originalLocale = Locale.getDefault();
		Object originalConfiguration = swapSingleton(Configuration.class, null);
		Object originalDictionary = swapSingleton(Dictionary.class, null);
		try {
			Locale.setDefault(locale);
			Messages.reset();
			Configuration.getInstance();
			Dictionary.getInstance();
			Project project = fixtureProject();
			ReportDefinition definition = DataSourceProvider.getFromName(name);
			assertNotNull(definition, name);
			SpreadSheetFieldArray selected = definition.getColumnsList().isEmpty() ? null
				: (SpreadSheetFieldArray) definition.getMainFieldArray().clone();
			if (selected != null) selected.removeIf(Field::isGraphical);
			ReportModel landscape = ReportModelFactory.create(definition, selected, source(definition, project),
				project.getName());
			ReportModel portrait = new ReportModel(landscape.title(), landscape.columns(), landscape.rows(),
				landscape.columnWidths(), landscape.groupColumn(), landscape.groupKeys(), landscape.projectName(),
				landscape.totalRow(), landscape.labelValueSheet(), true, landscape.displayLabels(),
				landscape.sections());
			for (ReportModel model : List.of(landscape, portrait)) {
				String stem = name.replace(' ', '-') + "-" + locale.getLanguage() + "-" + model.portrait();
				ReportPrintable printable = new ReportPrintable(model);
				List<String> pages = pdfPages(printable, stem);
				assertEquals(printable.getNumberOfPages(), pages.size(), "PDF page count equals layout page count");
				ReportPrintable again = new ReportPrintable(model);
				assertEquals(printable.layout().pages(), again.layout().pages(), "layout is deterministic");
				assertEquals(pages, pdfPages(again, stem + "-again"), "PDF text is deterministic");
				if (name.equals("Task Information") && !model.portrait())
					assertTrue(pages.size() >= 3, "fixture must need at least three pages, got " + pages.size() + " rows=" + model.rows().size() + " seg=" + printable.layout().segmentCount() + " widths=" + java.util.Arrays.toString(model.columnWidths()) + " cols=" + model.columns().stream().map(Field::getId).toList());
				if (model.labelValueSheet()) assertSheet(model, locale, pages);
				else assertTable(model, printable.layout(), locale, pages);
			}
		} finally {
			Locale.setDefault(originalLocale);
			Messages.reset();
			swapSingleton(Configuration.class, originalConfiguration);
			swapSingleton(Dictionary.class, originalDictionary);
		}
	}

	private static void assertTable(ReportModel model, ReportLayout layout, Locale locale, List<String> pages) {
		for (int segmentIndex = 0; segmentIndex < layout.segmentCount(); segmentIndex++) {
			ReportLayout.Segment segment = layout.segment(segmentIndex);
			int nextRow = 0;
			for (int pageIndex = 0; pageIndex < layout.pageCount(); pageIndex++) {
				ReportLayout.Page range = layout.page(pageIndex);
				if (range.segmentIndex() != segmentIndex) continue;
				assertTrue(!range.hasRowFragment(), "no row is split across pages");
				assertEquals(nextRow, range.firstRow(), "each page continues where the previous one ended");
				nextRow = range.endRow();
				String text = strip(pages.get(pageIndex));
				// Every page, including continuations, repeats this segment's localized column headers in order.
				int cursor = 0;
				for (int column = segment.startColumn(); column < segment.endColumn(); column++) {
					Field field = model.columns().get(column);
					String header = strip(Messages.getString(field.getId()));
					assertEquals(header, strip(field.getName()), "header comes from the " + locale + " bundle");
					int at = text.indexOf(header, cursor);
					assertTrue(at >= 0, "page " + pageIndex + " lacks header '" + header + "' in " + text);
					cursor = at + header.length();
				}
				if (locale.getLanguage().equals("ja")) assertTrue(hasJapanese(text), "Japanese labels: " + text);
				// Every cell of this page's rows appears, in row-major order, after the header.
				for (int row = range.firstRow(); row < range.endRow(); row++) {
					for (int column = segment.startColumn(); column < segment.endColumn(); column++) {
						Object value = model.rows().get(row).get(column);
						String cell = strip(value == null ? "" : value.toString());
						if (cell.isEmpty()) continue;
						int at = text.indexOf(cell, cursor);
						assertTrue(at >= 0, "row " + row + " column " + column + " '" + cell
							+ "' missing or out of order on page " + pageIndex + ": " + text);
						cursor = at + cell.length();
					}
				}
				// Nothing more and nothing less than this page's cells is rendered: every row name occurs on the
				// page exactly as often as inside the page's own expected text (no leak, no duplication).
				StringBuilder expected = new StringBuilder();
				for (int column = segment.startColumn(); column < segment.endColumn(); column++)
					expected.append(strip(model.columns().get(column).getName()));
				for (int row = range.firstRow(); row < range.endRow(); row++)
					for (int column = segment.startColumn(); column < segment.endColumn(); column++) {
						Object value = model.rows().get(row).get(column);
						expected.append(strip(value == null ? "" : value.toString()));
					}
				for (int row = 0; row < model.rows().size(); row++) {
					for (int column = segment.startColumn(); column < segment.endColumn(); column++) {
						Object value = model.rows().get(row).get(column);
						if (value == null || !value.toString().matches(".*task [0-9]+$")) continue;
						String cell = strip(value.toString());
						assertEquals(occurrences(expected.toString(), cell), occurrences(text, cell),
							"'" + cell + "' on page " + pageIndex + " must match the page's own rows " + range);
					}
				}
			}
			assertEquals(model.rows().size(), nextRow, "segment " + segmentIndex + " covers every row exactly once");
		}
	}

	private static void assertSheet(ReportModel model, Locale locale, List<String> pages) {
		String all = strip(String.join("", pages));
		ResourceBundle bundle = ResourceBundle.getBundle("com.microproject.reports.model.ReportLabels", locale);
		assertEquals(List.of("dates", "duration", "work", "costs", "notes").stream().map(bundle::getString).toList(),
			model.sections().stream().map(ReportModel.Section::title).toList(), "section titles from the bundle");
		for (ReportModel.Section section : model.sections())
			assertTrue(all.contains(strip(section.title())), "missing section heading " + section.title());
		for (String label : model.displayLabels()) assertTrue(all.contains(strip(label)), "missing label " + label);
		if (locale.getLanguage().equals("ja")) assertTrue(hasJapanese(all), "Japanese labels expected");
		for (List<Object> row : model.rows())
			for (Object value : row)
				if (value != null && !value.toString().isBlank())
					assertTrue(all.contains(strip(value.toString())), "missing value " + value);
	}

	private List<String> pdfPages(ReportPrintable printable, String stem) throws Exception {
		File output = directory.resolve(stem + ".pdf").toFile();
		new PageablePdfWriter().write(new GraphPageable(printable, false), output, ignored -> { });
		try (PdfReader reader = new PdfReader(output.getAbsolutePath())) {
			ArrayList<String> pages = new ArrayList<>();
			for (int page = 1; page <= reader.getNumberOfPages(); page++)
				pages.add(new PdfTextExtractor(reader).getTextFromPage(page));
			return List.copyOf(pages);
		}
	}

	private static String strip(String text) { return text.replaceAll("\\s+", ""); }

	private static boolean hasJapanese(String text) {
		return text.codePoints().anyMatch(cp -> {
			Character.UnicodeScript script = Character.UnicodeScript.of(cp);
			return script == Character.UnicodeScript.HAN || script == Character.UnicodeScript.HIRAGANA
				|| script == Character.UnicodeScript.KATAKANA;
		});
	}

	private static int occurrences(String text, String needle) {
		int count = 0;
		for (int at = text.indexOf(needle); at >= 0; at = text.indexOf(needle, at + needle.length())) count++;
		return count;
	}

	private static Object swapSingleton(Class<?> type, Object replacement) throws Exception {
		java.lang.reflect.Field field = type.getDeclaredField("instance");
		field.setAccessible(true);
		synchronized (type) {
			Object old = field.get(null);
			field.set(null, replacement);
			return old;
		}
	}

	private static Project fixtureProject() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(com.microproject.pm.resource.ResourcePool.createRourcePool("m2-4", undo), undo);
		project.initialize(false, false);
		project.setStartDate(java.time.LocalDate.of(2026, 10, 19)
			.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
		project.setName("fixture 日本語 project");
		List<NormalTask> tasks = new ArrayList<>();
		for (int i = 1; i <= TASK_COUNT; i++) {
			NormalTask task = (NormalTask) project.createLocalTaskNode(null).getImpl();
			task.setName("fixture 日本語 task " + i);
			tasks.add(task);
		}
		ResourceImpl[] resources = new ResourceImpl[2];
		for (int i = 0; i < resources.length; i++) {
			resources[i] = (ResourceImpl) project.getResourcePool().createScriptedResource();
			resources[i].setName("fixture 日本語 resource " + (i == 0 ? "A" : "B"));
		}
		for (int i = 0; i < tasks.size(); i++) {
			Assignment assignment = Assignment.getInstance(tasks.get(i), resources[i % resources.length], 1.0D, 0L);
			assignment.setWork(i == 0 ? 36_000_000L : i == 1 ? 72_000_000L : 0L, null);
			assertTrue(AssignmentService.getInstance().connect(assignment, null, false));
		}
		return project;
	}

	private static ReportDataSource source(ReportDefinition definition, Project project) {
		if (definition.getColumnsList().isEmpty())
			return DataSourceProvider.createProjectDataSource(project, definition.isTimeBased());
		NodeModel model = definition.getCollectionType() == DataSourceProvider.TASKS_TREE
			? project.getTaskModel() : project.getResourceModel();
		return DataSourceProvider.createDataSource(definition.getCollectionType(), project,
			new ModelIterator(model.iterator()), model, definition.isTimeBased());
	}

	private static final class ModelIterator implements PredicatedNodeFilterIterator {
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
