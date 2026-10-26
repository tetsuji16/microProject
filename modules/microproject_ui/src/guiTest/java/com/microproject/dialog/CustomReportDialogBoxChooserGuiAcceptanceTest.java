/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.awt.Robot;
import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.SwingUtilities;
import javax.swing.JList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.pm.task.Project;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.AssignmentService;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.snapshot.Snapshottable;
import com.microproject.datatype.Rate;
import com.microproject.datatype.TimeUnit;
import com.microproject.field.Field;
import com.microproject.reports.model.ReportModel;
import com.microproject.reports.render.ReportPreview;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.util.UiServices;

/** Exercises Custom Report's physical export route through the shared file chooser (#759). */
class CustomReportDialogBoxChooserGuiAcceptanceTest {
	private CustomReportDialogBox dialog;
	private UiServices.FileChooserProvider previousChooser;
	private Path exportDirectory;

	@AfterEach
	void closeDialogAndRestoreChooser() throws Exception {
		if (dialog != null)
			SwingUtilities.invokeAndWait(dialog::dispose);
		UiServices.setFileChooserProvider(previousChooser);
		if (exportDirectory != null && Files.exists(exportDirectory)) {
			try (var files = Files.list(exportDirectory)) {
				for (Path file : files.toList())
					Files.deleteIfExists(file);
			}
			Files.deleteIfExists(exportDirectory);
		}
	}

	@Test
	void robotExportsCsvThroughSharedChooserAndCancelCreatesNoFile() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for report chooser acceptance coverage.");
		previousChooser = UiServices.getFileChooserProvider();
		exportDirectory = Files.createTempDirectory("custom-report-chooser-");
		Path csv = exportDirectory.resolve("custom-report.csv");
		Path cancelled = exportDirectory.resolve("cancelled-report.csv");
		AtomicInteger selection = new AtomicInteger();
		List<UiServices.FileChooserOptions> requestedOptions = new CopyOnWriteArrayList<>();
		Project project = CriticalChainStatusDialogGuiAcceptanceTest.newProjectWithTasks();
		SwingUtilities.invokeAndWait(() -> {
			dialog = new CustomReportDialogBox(null, project);
			dialog.setVisible(true);
		});
		// FlatLafDialog initializes the application theme in its constructor, which
		// installs the production provider. Override it only after that initialization.
		UiServices.setFileChooserProvider(new UiServices.FileChooserProvider() {
			@Override
			public String chooseFileName(boolean save, String selectedFileName, Object parent) {
				throw new AssertionError("Custom Report must use the specialized shared chooser contract");
			}

			@Override
			public String chooseFileName(UiServices.FileChooserOptions options, Object parent) {
				requestedOptions.add(options);
				return selection.getAndIncrement() == 0 ? csv.toString() : null;
			}
		});
		GuiAcceptanceSupport.await(() -> dialog.isShowing(), "Custom Report dialog did not open");
		GuiAcceptanceSupport.await(() -> dialog.isActive(), "Custom Report dialog did not become active");
		SwingUtilities.invokeAndWait(() -> { dialog.setAlwaysOnTop(true); dialog.toFront(); dialog.requestFocus(); });

		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		clickExport(robot);
		GuiAcceptanceSupport.await(() -> Files.exists(csv), "Custom Report chooser approval did not write the CSV");
		assertEquals(new UiServices.FileChooserOptions(true, "project-report.csv", "Export custom report", "CSV (*.csv)", "csv"),
			requestedOptions.get(0), "Custom Report must request the shared CSV save contract");
		byte[] content = Files.readAllBytes(csv);
		assertTrue(content.length > 3, "approved report must have content");
		assertEquals(0xEF, content[0] & 0xff, "CSV must keep its UTF-8 BOM");
		assertTrue(new String(content, 3, content.length - 3, StandardCharsets.UTF_8).contains("Design"),
			"CSV must contain the fixture task produced by the existing report model");

		clickExport(robot);
		GuiAcceptanceSupport.await(() -> requestedOptions.size() == 2, "Custom Report did not route its second export through the shared chooser");
		assertEquals(requestedOptions.get(0), requestedOptions.get(1), "repeated export must preserve the shared CSV filter and defaults");
		assertFalse(Files.exists(cancelled), "a chooser cancellation must not create an output file");
	}

	@Test
	void robotSelectsEarnedValueColumnsAndExportsTheirPreviewValues() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for report chooser acceptance coverage.");
		previousChooser = UiServices.getFileChooserProvider();
		exportDirectory = Files.createTempDirectory("custom-report-earned-value-");
		Path csv = exportDirectory.resolve("earned-value-report.csv");
		AtomicInteger selection = new AtomicInteger();
		List<UiServices.FileChooserOptions> requestedOptions = new CopyOnWriteArrayList<>();
		Project project = earnedValueProject();
		SwingUtilities.invokeAndWait(() -> {
			dialog = new CustomReportDialogBox(null, project);
			dialog.setVisible(true);
		});
		UiServices.setFileChooserProvider(new UiServices.FileChooserProvider() {
			@Override public String chooseFileName(boolean save, String selectedFileName, Object parent) {
				throw new AssertionError("Custom Report must use the specialized shared chooser contract");
			}
			@Override public String chooseFileName(UiServices.FileChooserOptions options, Object parent) {
				requestedOptions.add(options);
				return selection.getAndIncrement() == 0 ? csv.toString() : null;
			}
		});
		GuiAcceptanceSupport.await(() -> dialog.isShowing() && dialog.isActive(), "Custom Report dialog did not become active");
		SwingUtilities.invokeAndWait(() -> { dialog.setAlwaysOnTop(true); dialog.toFront(); dialog.requestFocus(); });

		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		JList<?>[] columnsHolder = new JList<?>[1];
		SwingUtilities.invokeAndWait(() -> columnsHolder[0] = columnList());
		JList<?> columns = columnsHolder[0];
		List<String> earnedValueIds = List.of("Field.bcws", "Field.bcwp", "Field.acwp", "Field.cv", "Field.sv", "Field.cpi", "Field.spi");
		for (String id : earnedValueIds)
			robotSelectField(robot, columns, id);
		SwingUtilities.invokeAndWait(() -> assertTrue(columns.getSelectedValuesList().stream()
			.filter(Field.class::isInstance).map(Field.class::cast).map(Field::getId).toList().containsAll(earnedValueIds),
			"the visible custom report column list must retain all selected EVM fields"));

		CriticalChainStatusDialogGuiAcceptanceTest.click(robot,
			CriticalChainStatusDialogGuiAcceptanceTest.findButton(dialog, UsabilityStrings.text("report.generate")));
		GuiAcceptanceSupport.await(this::previewIsVisible,
			"generated report must display the page preview");
		ReportModel report = reportModel();
		assertTrue(report.columns().stream().map(Field::getId).toList().containsAll(earnedValueIds));
		assertEquals(1, report.rows().size(), "the preview must contain the known-value task row");
		NormalTask task = project.getTaskList().stream().filter(NormalTask.class::isInstance)
			.map(NormalTask.class::cast).filter(candidate -> "EVM fixture".equals(candidate.getName())).findFirst().orElseThrow();
		for (String id : earnedValueIds) {
			int column = java.util.stream.IntStream.range(0, report.columns().size())
				.filter(index -> id.equals(report.columns().get(index).getId())).findFirst().orElseThrow();
			String renderedValue = report.rows().get(0).get(column).toString();
			assertEquals(report.columns().get(column).getText(task, null), renderedValue,
				"the page preview model must use the task's calculated " + id + " value");
			assertFalse(renderedValue.isBlank(), id + " must render a value");
		}

		clickExport(robot);
		GuiAcceptanceSupport.await(() -> Files.exists(csv), "EVM report chooser approval did not write the CSV");
		String csvText = new String(Files.readAllBytes(csv), StandardCharsets.UTF_8);
		assertTrue(csvText.startsWith("\uFEFF"), "CSV must keep its UTF-8 BOM");
		String csvBody = csvText.substring(1);
		try (var parser = org.apache.commons.csv.CSVParser.parse(csvBody, org.apache.commons.csv.CSVFormat.RFC4180)) {
			var records = parser.getRecords();
			assertEquals(2, records.size(), "CSV must contain a header and the EVM fixture row");
			for (String id : earnedValueIds) {
				int column = java.util.stream.IntStream.range(0, report.columns().size())
					.filter(index -> id.equals(report.columns().get(index).getId())).findFirst().orElseThrow();
				String label = report.columns().get(column).getName();
				int csvColumn = records.get(0).toList().indexOf(label);
				assertTrue(csvColumn >= 0, "CSV must include the selected " + id + " column");
				assertEquals(report.rows().get(0).get(column).toString(), records.get(1).get(csvColumn),
					"CSV must preserve the preview's calculated " + id + " value");
			}
		}
		assertEquals(new UiServices.FileChooserOptions(true, "project-report.csv", "Export custom report", "CSV (*.csv)", "csv"),
			requestedOptions.get(0), "EVM export must use the shared CSV save contract");
	}

	private static Project earnedValueProject() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("custom-report-evm", undo);
		pool.setLocal(true);
		Project project = Project.createProject(pool, undo);
		project.initialize(false, false);
		NormalTask task = (NormalTask) project.createLocalTaskNode(null).getImpl();
		task.setName("EVM fixture");
		long hours = 60L * 60L * 1000L;
		task.setDuration(4L * hours);
		ResourceImpl resource = pool.newResourceInstance();
		resource.setStandardRate(new Rate(60.0D, TimeUnit.HOURS));
		Assignment assignment = AssignmentService.getInstance().newAssignment(task, resource, 1.0D, 0L, project);
		assignment.setWork(4L * hours, null);
		project.saveCurrentToSnapshot(Snapshottable.BASELINE, true, null);
		assignment.setWork(8L * hours, null);
		assignment.setActualWork(2L * hours, null);
		project.setStatusDate(task.getEnd());
		return project;
	}

	private JList<?> columnList() {
		try {
			var field = CustomReportDialogBox.class.getDeclaredField("columns");
			field.setAccessible(true);
			return (JList<?>) field.get(dialog);
		} catch (ReflectiveOperationException error) {
			throw new AssertionError("Custom Report column list was not available", error);
		}
	}

	private static void robotSelectField(Robot robot, JList<?> list, String id) throws Exception {
		int[] indexHolder = { -1 };
		Point point = new Point();
		SwingUtilities.invokeAndWait(() -> {
			for (int candidate = 0; candidate < list.getModel().getSize(); candidate++) {
				if (list.getModel().getElementAt(candidate) instanceof Field field && id.equals(field.getId())) {
					indexHolder[0] = candidate;
					break;
				}
			}
			if (indexHolder[0] < 0) throw new AssertionError("Missing visible report field " + id);
			int selectedIndex = indexHolder[0];
			list.ensureIndexIsVisible(selectedIndex);
			Rectangle cell = list.getCellBounds(selectedIndex, selectedIndex);
			Point origin = list.getLocationOnScreen();
			point.setLocation(origin.x + cell.x + cell.width / 2, origin.y + cell.y + cell.height / 2);
		});
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.mouseMove(point.x, point.y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		robot.waitForIdle();
	}

	private boolean previewIsVisible() {
		boolean[] visible = { false };
		try {
			SwingUtilities.invokeAndWait(() -> visible[0] = containsComponent(dialog, ReportPreview.class));
		} catch (Exception error) {
			throw new AssertionError("Could not inspect the generated report preview", error);
		}
		return visible[0];
	}

	private static boolean containsComponent(Container root, Class<?> type) {
		for (Component child : root.getComponents()) {
			if (type.isInstance(child)) return true;
			if (child instanceof Container nested && containsComponent(nested, type)) return true;
		}
		return false;
	}

	private ReportModel reportModel() throws Exception {
		var field = CustomReportDialogBox.class.getDeclaredField("reportModel");
		field.setAccessible(true);
		return (ReportModel) field.get(dialog);
	}

	private void clickExport(Robot robot) throws Exception {
		CriticalChainStatusDialogGuiAcceptanceTest.click(robot,
			CriticalChainStatusDialogGuiAcceptanceTest.findButton(dialog, UsabilityStrings.text("report.export")));
	}
}
