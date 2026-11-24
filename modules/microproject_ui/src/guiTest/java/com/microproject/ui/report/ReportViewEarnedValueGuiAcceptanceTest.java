/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;
import javax.swing.JComboBox;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.microproject.datatype.Rate;
import com.microproject.datatype.TimeUnit;
import com.microproject.field.Field;
import com.microproject.view.configuration.SpreadSheetFieldArray;
import com.microproject.options.CalendarOption;
import com.microproject.model.assignment.Assignment;
import com.microproject.model.assignment.AssignmentService;
import com.microproject.ui.frame.DocumentFrame;
import com.microproject.ui.frame.GraphicManager;
import com.microproject.ui.frame.MainRibbonFrame;
import com.microproject.model.resource.ResourceImpl;
import com.microproject.model.resource.ResourcePool;
import com.microproject.model.snapshot.Snapshottable;
import com.microproject.model.task.NormalTask;
import com.microproject.model.task.Project;
import com.microproject.reports.model.ReportLayout;
import com.microproject.reports.model.ReportModel;
import com.microproject.reports.render.ReportPreview;
import com.microproject.i18n.Messages;
import com.microproject.ui.testsupport.GuiAcceptanceSupport;
import com.microproject.ui.testsupport.GuiRobot;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.UiServices;

/**
 * Issue #943: the production report route (Reports tab, Custom Report, report
 * "Task Information", column preset "Earned Value - Cost") must display the task
 * rows and their values, and the CSV exported from that visible preview must
 * repeat the same values. The historical defect showed only the column headers.
 *
 * <p>The plan is the hand-calculated fixture of
 * {@code com.microproject.ui.report.EarnedValueProjectValuesTest}: 80 baseline
 * hours at 60 per hour (BAC 4800), work raised to 160 hours, 40 hours reported
 * as actual, and a status date three working days after the pinned Monday.
 */
class ReportViewEarnedValueGuiAcceptanceTest {
	private static final long HOUR_MILLIS = 3_600_000L;
	private static final long CALENDAR_DAY = 24L * HOUR_MILLIS;
	private static final double RATE_PER_HOUR = 60.0D;
	private static final long DAY = CalendarOption.getInstance().getMillisPerDay();
	private static final long PLANNED_HOURS = 10L * DAY / HOUR_MILLIS;
	private static final long ELAPSED_HOURS = 3L * DAY / HOUR_MILLIS;
	private static final long START = LocalDateTime.of(2026, 11, 16, 8, 0)
		.atZone(ZoneId.of("Asia/Tokyo")).toInstant().toEpochMilli();
	private static final String PRESET = "Spreadsheet.Task.earnedValueCostIndicators";
	private static final String TASK_NAME = "EVM report task";
	private static final List<String> EARNED_VALUE_FIELDS = List.of("Field.bcws", "Field.bcwp", "Field.cv",
		"Field.cpi", "Field.bac", "Field.eac", "Field.vac");

	private MainRibbonFrame frame;
	private GraphicManager manager;
	private UiServices.FileChooserProvider previousChooser;
	private Path exportDirectory;

	@AfterEach
	void disposeAndRestoreChooser() throws Exception {
		if (previousChooser != null) {
			UiServices.setFileChooserProvider(previousChooser);
			previousChooser = null;
		}
		if (exportDirectory != null && Files.exists(exportDirectory)) {
			try (var files = Files.list(exportDirectory)) {
				for (Path file : files.toList())
					Files.deleteIfExists(file);
			}
			Files.deleteIfExists(exportDirectory);
			exportDirectory = null;
		}
		SwingUtilities.invokeAndWait(() -> {
			for (java.awt.Window window : java.awt.Window.getWindows())
				if (window.isDisplayable())
					window.dispose();
		});
		if (manager != null) {
			SwingUtilities.invokeAndWait(() -> manager.cleanUp());
			manager = null;
		}
	}

	@Test
	void reportRouteShowsEarnedValueRowsAndExportsTheSameValues() throws Exception {
		assumeFalse(GraphicsEnvironment.isHeadless(),
			"A desktop session is required for the reports-tab acceptance route.");
		previousChooser = UiServices.getFileChooserProvider();
		exportDirectory = Files.createTempDirectory("report-view-earned-value-");
		Path csv = exportDirectory.resolve("earned-value-report.csv");
		AtomicInteger chooserCalls = new AtomicInteger();
		Project project = earnedValueProject();

		DocumentFrame[] document = new DocumentFrame[1];
		SwingUtilities.invokeAndWait(() -> {
			frame = new MainRibbonFrame("Reports tab earned value acceptance", null);
			manager = new GraphicManager(frame);
			frame.setGraphicManager(manager);
			manager.initView();
			document[0] = manager.addProjectFrame(project);
			frame.setBounds(40, 40, 1240, 800);
			frame.setAlwaysOnTop(true);
			frame.setVisible(true);
		});
		Robot robot = new GuiRobot();
		robot.setAutoDelay(45);
		GuiAcceptanceSupport.await(() -> frame.isShowing(), "the main window never became visible");
		// The window's own activation handlers still run after setVisible; let them
		// settle before the Reports tab is selected, or they replace it again.
		GuiAcceptanceSupport.awaitInputAndEventQueueIdle(robot, "document tab activation");

		// The Reports tab's Custom Report command routes through this production entry point.
		SwingUtilities.invokeAndWait(() -> manager.showReportView());
		ReportView[] holder = new ReportView[1];
		GuiAcceptanceSupport.await(() -> {
			if (!(document[0].getReportView() instanceof ReportView view) || view.getPrintable() == null)
				return false;
			holder[0] = view;
			return view.isShowing();
		}, "the report view did not render a report on the Reports tab route");
		ReportView view = holder[0];
		GuiAcceptanceSupport.activateAndAwaitRenderedWindow(frame, view, robot, true, "reports tab");

		JComboBox<?>[] choices = new JComboBox<?>[2];
		SwingUtilities.invokeAndWait(() -> {
			choices[0] = view.reportChoice;
			choices[1] = view.columnsChoice;
		});
		assertNotNull(choices[0], "the report selector must be visible on the reports tab route");
		assertNotNull(choices[1], "the column-preset selector must be visible on the reports tab route");

		// 1. Pick the report "Task Information" through the visible combo box.
		int taskInformation = comboIndex(choices[0], item -> "Task Information".equals(String.valueOf(item)));
		assertTrue(taskInformation >= 0, "the Task Information report must be offered: " + comboItems(choices[0]));
		GuiAcceptanceSupport.selectComboBoxItem(robot, choices[0], taskInformation);
		GuiAcceptanceSupport.await(() -> model(view) != null
			&& "Task Information".equals(model(view).title()),
			"the selected Task Information report did not reach the preview model");
		GuiAcceptanceSupport.await(() -> choices[1].getItemCount() > 1,
			"the Task Information report did not populate the column presets");

		// 2. Pick the column preset "Earned Value - Cost" through the visible combo box.
		String presetName = Messages.getString(PRESET);
		int earnedValueCost = comboIndex(choices[1], item -> item instanceof SpreadSheetFieldArray array
			&& presetName.equals(String.valueOf(array)));
		assertTrue(earnedValueCost >= 0,
			"the " + presetName + " preset must be offered: " + comboItems(choices[1]));
		GuiAcceptanceSupport.selectComboBoxItem(robot, choices[1], earnedValueCost);
		GuiAcceptanceSupport.await(() -> model(view) != null
			&& model(view).columns().stream().anyMatch(field -> "Field.bcws".equals(field.getId())),
			"the Earned Value - Cost preset never reached the preview model");

		ReportModel model = model(view);
		SpreadSheetFieldArray preset = (SpreadSheetFieldArray) choices[1].getSelectedItem();
		assertEquals(preset.stream().map(Field::getId).toList(),
			model.columns().stream().map(Field::getId).toList(),
			"the preview must use every column of the selected Earned Value - Cost preset");
		for (String id : EARNED_VALUE_FIELDS)
			assertTrue(model.columns().stream().anyMatch(field -> id.equals(field.getId())),
				"the earned value preview is missing " + id);

		// 3. The preview must carry the task row and the project total, never only the headers.
		// Costs are stored as rate per hour × work in milliseconds, and the report prints that
		// stored value the same way the task sheet formats it, so the expectations below are the
		// hand calculation of the fixture rather than a value read back out of the model.
		assertEquals(2, model.rows().size(),
			"the preview must contain the task row plus the project total row");
		assertTrue(model.summaryRow(1), "the Earned Value preset sums its columns, so the last row is the total");
		int row = rowIndexOf(model, TASK_NAME);
		double bcws = RATE_PER_HOUR * ELAPSED_HOURS * HOUR_MILLIS;
		assertMoney(model, row, "Field.bcws", bcws, "BCWS is the three elapsed days of the baseline");
		assertMoney(model, row, "Field.bcwp", bcws / 2.0D, "BCWP is the earned half of those three days");
		assertMoney(model, row, "Field.cv", -bcws / 2.0D, "CV = BCWP - ACWP");
		assertNumber(model, row, "Field.cpi", 0.5D, "CPI = BCWP / ACWP");
		assertMoney(model, row, "Field.bac", RATE_PER_HOUR * PLANNED_HOURS * HOUR_MILLIS,
			"BAC is the baseline cost");
		NormalTask previewed = taskNamed(project, TASK_NAME);
		for (String id : EARNED_VALUE_FIELDS) {
			String rendered = String.valueOf(cell(model, row, id));
			assertFalse(rendered.isBlank(), id + " must show a value for the task row");
			Field field = model.columns().get(columnOf(model, id));
			assertEquals(field.getText(previewed, null), rendered,
				"the preview must format " + id + " the way the task sheet does");
		}
		double eac = parseNumber(String.valueOf(cell(model, row, "Field.eac")), "Field.eac");
		double vac = parseNumber(String.valueOf(cell(model, row, "Field.vac")), "Field.vac");
		assertEquals(parseNumber(String.valueOf(cell(model, row, "Field.bac")), "Field.bac") - eac, vac,
			0.0005D * Math.abs(vac), "VAC = BAC - EAC in the preview");
		ReportLayout layout = view.getPrintable().layout();
		assertTrue(layout.pageCount() >= 1, "the preview must have at least one page");
		for (int page = 0; page < layout.pageCount(); page++) {
			ReportLayout.Page layoutPage = layout.page(page);
			assertTrue(layoutPage.endRow() > layoutPage.firstRow(),
				"preview page " + page + " repeats only the column headers without data rows");
		}
		assertTrue(containsComponent(view, ReportPreview.class),
			"the visible preview surface must be the report preview");
		java.util.List<String> dump = new java.util.ArrayList<>();
		for (int rowIndex = 0; rowIndex < model.rows().size(); rowIndex++)
			dump.add("row " + rowIndex + " summary=" + model.summaryRow(rowIndex) + " " + model.rows().get(rowIndex));
		for (int page = 0; page < layout.pageCount(); page++)
			dump.add("page " + page + " segment=" + layout.page(page).segmentIndex()
				+ " rows=" + layout.page(page).firstRow() + ".." + layout.page(page).endRow());
		Files.writeString(Path.of(System.getProperty("microproject.gui.artifacts.dir",
			"build/reports/guiTest-artifacts")).resolve("report-view-earned-value.txt"),
			String.join("\n", dump) + "\n", StandardCharsets.UTF_8);
		capture(robot, "report-view-earned-value.png");

		// 4. Export CSV from the visible preview and compare every column of the task row.
		UiServices.setFileChooserProvider(new UiServices.FileChooserProvider() {
			@Override public String chooseFileName(boolean save, String selectedFileName, Object parent) {
				throw new AssertionError("The report view must use the specialized shared chooser contract");
			}

			@Override public String chooseFileName(UiServices.FileChooserOptions options, Object parent) {
				chooserCalls.incrementAndGet();
				return csv.toString();
			}
		});
		javax.swing.AbstractButton export = GuiAcceptanceSupport.findShowingButtonByText(view,
			Messages.getString("ReportView.ExportCsv"));
		assertNotNull(export, "the visible preview must offer the CSV export command");
		com.microproject.ui.testsupport.WindowAcceptanceAssertions.clickComponent(robot, export);
		GuiAcceptanceSupport.await(() -> Files.exists(csv), "the report view CSV export did not write a file");
		assertEquals(1, chooserCalls.get(), "one CSV export must ask the shared chooser exactly once");
		List<String> csvValues = readCsvRow(csv, TASK_NAME);
		List<Object> previewRow = model.rows().get(row);
		assertEquals(previewRow.size(), csvValues.size(),
			"the CSV row must contain every preview column");
		for (int column = 0; column < previewRow.size(); column++)
			assertEquals(String.valueOf(previewRow.get(column)), csvValues.get(column),
				"the exported CSV must repeat preview column " + model.columns().get(column).getId());

		// 5. The CSV header must name the same columns the preview displayed.
		assertEquals(model.columns().stream().map(Field::getName).toList(), readCsvHeader(csv),
			"the exported CSV headers must repeat the preview columns");
	}

	private static Project earnedValueProject() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("report-view-earned-value", undo), undo);
		project.setStart(START);
		project.initialize(false, false);
		NormalTask task = project.createScriptedTask();
		task.setName(TASK_NAME);
		project.connectTask(task);
		task.setDuration(10L * DAY);
		ResourceImpl resource = project.getResourcePool().newResourceInstance();
		resource.setStandardRate(new Rate(RATE_PER_HOUR, TimeUnit.HOURS));
		Assignment assignment = AssignmentService.getInstance().newAssignment(task, resource, 1.0D, 0L, null);
		assignment.setWork(PLANNED_HOURS * HOUR_MILLIS, null);
		project.saveCurrentToSnapshot(Snapshottable.BASELINE, true, null);
		assignment.setWork(PLANNED_HOURS * 2L * HOUR_MILLIS, null);
		assignment.setActualWork(PLANNED_HOURS / 2L * HOUR_MILLIS, null);
		project.setStatusDate(task.getStart() + 3L * CALENDAR_DAY);
		project.recalculate();
		return project;
	}

	private static ReportModel model(ReportView view) {
		return view.getPrintable() == null ? null : view.getPrintable().model();
	}

	private static NormalTask taskNamed(Project project, String name) {
		return project.getTaskList().stream().filter(NormalTask.class::isInstance)
			.map(NormalTask.class::cast).filter(task -> name.equals(task.getName()))
			.findFirst().orElseThrow(() -> new AssertionError("the project is missing the task " + name));
	}

	private static int comboIndex(JComboBox<?> combo, java.util.function.Predicate<Object> match) {
		for (int index = 0; index < combo.getItemCount(); index++)
			if (match.test(combo.getItemAt(index)))
				return index;
		return -1;
	}

	private static List<String> comboItems(JComboBox<?> combo) {
		List<String> items = new ArrayList<>();
		for (int index = 0; index < combo.getItemCount(); index++)
			items.add(String.valueOf(combo.getItemAt(index)));
		return items;
	}

	private static Object cell(ReportModel model, int row, String fieldId) {
		return model.rows().get(row).get(columnOf(model, fieldId));
	}

	private static int columnOf(ReportModel model, String fieldId) {
		for (int index = 0; index < model.columns().size(); index++)
			if (fieldId.equals(model.columns().get(index).getId()))
				return index;
		throw new AssertionError("The preview is missing the column " + fieldId);
	}

	private static int rowIndexOf(ReportModel model, String taskName) {
		for (int row = 0; row < model.rows().size(); row++) {
			if (model.summaryRow(row))
				continue;
			if (model.rows().get(row).stream().anyMatch(taskName::equals))
				return row;
		}
		throw new AssertionError("The preview does not contain the task row " + taskName);
	}

	/** Asserts a rendered amount against the hand-calculated amount in display currency. */
	private static void assertMoney(ReportModel model, int row, String fieldId, double expected, String message) {
		String rendered = String.valueOf(cell(model, row, fieldId));
		double actual = parseNumber(rendered, fieldId);
		assertEquals(expected, actual, Math.abs(expected) * 1.0e-9D + 0.005D,
			fieldId + " (" + rendered + "): " + message);
	}

	private static void assertNumber(ReportModel model, int row, String fieldId, double expected, String message) {
		String rendered = String.valueOf(cell(model, row, fieldId));
		assertEquals(expected, parseNumber(rendered, fieldId), 1.0e-6D,
			fieldId + " (" + rendered + "): " + message);
	}

	private static double parseNumber(String rendered, String fieldId) {
		StringBuilder digits = new StringBuilder();
		for (int index = 0; index < rendered.length(); index++) {
			char character = rendered.charAt(index);
			if (Character.isDigit(character) || character == '.' || character == '-')
				digits.append(character);
		}
		if (digits.isEmpty())
			throw new AssertionError(fieldId + " rendered no number: " + rendered);
		return Double.parseDouble(digits.toString());
	}

	private static List<String> readCsvHeader(Path csv) throws IOException {
		try (var parser = org.apache.commons.csv.CSVParser.parse(readCsv(csv),
			org.apache.commons.csv.CSVFormat.RFC4180)) {
			return parser.getRecords().get(0).toList();
		}
	}

	private static List<String> readCsvRow(Path csv, String taskName) throws IOException {
		try (var parser = org.apache.commons.csv.CSVParser.parse(readCsv(csv),
			org.apache.commons.csv.CSVFormat.RFC4180)) {
			for (var record : parser.getRecords())
				if (record.toList().contains(taskName))
					return record.toList();
		}
		throw new AssertionError("The exported CSV does not contain the task row " + taskName);
	}

	private static String readCsv(Path csv) throws IOException {
		String text = Files.readString(csv, StandardCharsets.UTF_8);
		assertTrue(text.startsWith("\uFEFF"), "the report CSV must keep its UTF-8 BOM");
		return text.substring(1);
	}

	private static boolean containsComponent(Container root, Class<?> type) {
		for (Component child : root.getComponents()) {
			if (type.isInstance(child))
				return true;
			if (child instanceof Container nested && containsComponent(nested, type))
				return true;
		}
		return false;
	}

	private void capture(Robot robot, String fileName) throws IOException {
		Rectangle bounds = frame.getBounds();
		java.awt.image.BufferedImage screenshot = robot.createScreenCapture(bounds);
		Path directory = Path.of(System.getProperty("microproject.gui.artifacts.dir", "build/reports/guiTest-artifacts"));
		Files.createDirectories(directory);
		ImageIO.write(screenshot, "png", directory.resolve(fileName).toFile());
		assertTrue(screenshot.getWidth() > 400 && screenshot.getHeight() > 300,
			"the captured reports-tab layout is unexpectedly small");
	}
}
