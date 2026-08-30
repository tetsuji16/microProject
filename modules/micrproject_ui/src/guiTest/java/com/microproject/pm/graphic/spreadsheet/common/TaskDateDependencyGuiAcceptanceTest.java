/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.spreadsheet.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Calendar;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.JFrame;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.microproject.field.Field;
import com.microproject.graphic.configuration.SpreadSheetCategories;
import com.microproject.grouping.core.Node;
import com.microproject.options.CalendarOption;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetModel;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetUtils;
import com.microproject.pm.graphic.spreadsheet.editor.DateEditor;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.DateTime;

/** Robot coverage for editing a predecessor start date through the visible task table. */
class TaskDateDependencyGuiAcceptanceTest {
	private JFrame frame;

	@AfterEach
	void closeWindow() throws Exception {
		if (frame != null) SwingUtilities.invokeAndWait(() -> {
			frame.dispose();
			frame = null;
		});
	}

	@ParameterizedTest(name = "{0} dependency")
	@ValueSource(ints = { DependencyType.FS, DependencyType.SS, DependencyType.FF, DependencyType.SF })
	void robotDateEditRecalculatesSuccessor(int type) throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture(type);
		showFixture(fixture);
		Robot robot = new Robot();
		robot.setAutoDelay(45);
		activate(fixture);
		clickCell(robot, fixture);
		dispatchF2(fixture.sheet);
		GuiAcceptanceSupport.await(fixture.sheet::isEditing, "F2 did not start date editing");
		assertEquals(fixture.startColumn, fixture.sheet.getEditingColumn(), "F2 must edit the start-date cell");
		SwingUtilities.invokeAndWait(() -> {
			DateEditor.ExtDateField editor = (DateEditor.ExtDateField) fixture.sheet.getEditorComponent();
			editor.getTextField().setText("2026/06/09");
			assertTrue(fixture.sheet.getCellEditor().stopCellEditing(), "date editor rejected a valid date");
		});
		GuiAcceptanceSupport.await(() -> !fixture.sheet.isEditing(), "date edit did not commit");
		SwingUtilities.invokeAndWait(fixture.project::recalculate);
		long expectedSuccessorStart = fixture.dependency.calcForwardDependencyDate(
			fixture.predecessor.getStart(), fixture.predecessor.getEnd(), true);
		assertTrue(fixture.predecessor.getStart() > fixture.originalStart, "predecessor start must move after GUI edit");
		assertEquals(expectedSuccessorStart, fixture.successor.getStart(), "FS successor must match dependency date after GUI edit");
		capture(robot, type);
	}

	private void showFixture(Fixture fixture) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			frame = new JFrame("Task date dependency GUI acceptance");
			JComponent rootPane = frame.getRootPane();
			rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "EditField");
			ActionMap actions = rootPane.getActionMap();
			actions.put("EditField", new AbstractAction() {
				@Override public void actionPerformed(java.awt.event.ActionEvent event) {
					fixture.sheet.editActiveCell();
				}
			});
			frame.add(new JScrollPane(fixture.sheet));
			frame.setPreferredSize(new Dimension(1000, 460));
			frame.pack();
			frame.setLocationByPlatform(true);
			frame.setAlwaysOnTop(true);
			frame.setVisible(true);
		});
	}

	private void activate(Fixture fixture) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			frame.toFront();
			frame.requestFocus();
			fixture.sheet.requestFocusInWindow();
		});
		GuiAcceptanceSupport.await(fixture.sheet::isFocusOwner, "task table did not receive focus");
	}

	private static void clickCell(Robot robot, Fixture fixture) throws Exception {
		Rectangle cell = cellOnScreen(fixture.sheet, fixture.predecessorRow, fixture.startColumn);
		robot.mouseMove(cell.x + cell.width / 2, cell.y + cell.height / 2);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
	}

	private static void dispatchF2(SpreadSheet sheet) throws Exception {
		SwingUtilities.invokeAndWait(() -> KeyboardFocusManager.getCurrentKeyboardFocusManager().dispatchEvent(
			new KeyEvent(sheet, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_F2, KeyEvent.CHAR_UNDEFINED)));
	}

	private static Rectangle cellOnScreen(SpreadSheet sheet, int row, int column) throws Exception {
		Rectangle[] result = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> {
			Rectangle bounds = sheet.getCellRect(row, column, true);
			Point location = sheet.getLocationOnScreen();
			result[0] = new Rectangle(location.x + bounds.x, location.y + bounds.y, bounds.width, bounds.height);
		});
		return result[0];
	}

	private void capture(Robot robot, int type) throws Exception {
		Rectangle[] bounds = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> bounds[0] = new Rectangle(frame.getRootPane().getLocationOnScreen(), frame.getRootPane().getSize()));
		BufferedImage screenshot = robot.createScreenCapture(bounds[0]);
		Path directory = Path.of(System.getProperty("micrproject.gui.artifacts.dir", "build/guiTest-artifacts"));
		Files.createDirectories(directory);
		javax.imageio.ImageIO.write(screenshot, "png", directory.resolve("task-date-dependency-edit-" + dependencyTypeName(type) + ".png").toFile());
	}

	private static String dependencyTypeName(int type) {
		return switch (type) {
			case DependencyType.FS -> "FS";
			case DependencyType.SS -> "SS";
			case DependencyType.FF -> "FF";
			case DependencyType.SF -> "SF";
			default -> "unknown";
		};
	}

	private static Fixture createFixture(int type) throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("gui-date-dependency", undo);
		Project project = Project.createProject(pool, undo);
		project.initialize(false, false);
		NormalTask predecessor = task(project, "GUI predecessor", 1L);
		NormalTask successor = task(project, "GUI successor", 1L);
		long originalStart = DateTime.calendarInstance(2026, Calendar.JUNE, 6).getTimeInMillis();
		predecessor.setStart(originalStart);
		Dependency dependency = DependencyService.getInstance().newDependency(predecessor, successor, type, 0L, project);
		project.recalculate();
		final Fixture[] result = new Fixture[1];
		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet sheet = new SpreadSheet();
			sheet.setSpreadSheetCategory(SpreadSheetCategories.taskSpreadsheetCategory);
			NodeModelCache cache = NodeModelCacheFactory.getInstance().createFilteredCache(
				NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel()), "gui-date-dependency", null);
			SpreadSheetUtils.setFieldsAndContext(sheet, cache, SpreadSheetCategories.taskSpreadsheetCategory, "Spreadsheet.Task.entry", true);
			Node node = (Node) cache.getModel().search(predecessor);
			int row = ((SpreadSheetModel) sheet.getModel()).findGraphicNodeRow(cache.getGraphicNode(node));
			result[0] = new Fixture(project, sheet, predecessor, successor, dependency, originalStart, row, findStartColumn(sheet));
		});
		return result[0];
	}

	private static NormalTask task(Project project, String name, long days) {
		NormalTask task = new NormalTask(project);
		task.setName(name);
		project.connectTask(task);
		project.getSchedulingAlgorithm().addObject(task);
		task.getCurrentSchedule().setStart(project.getStart());
		task.setDuration(days * CalendarOption.getInstance().getMillisPerDay());
		return task;
	}

	private static int findStartColumn(SpreadSheet sheet) {
		SpreadSheetModel model = (SpreadSheetModel) sheet.getModel();
		for (int column = 0; column < model.getColumnCount(); column++) {
			Field field = model.getFieldInColumn(column);
			if (field != null && "Field.start".equals(field.getId())) return sheet.convertColumnIndexToView(column);
		}
		throw new IllegalArgumentException("Missing start field");
	}

	private record Fixture(Project project, SpreadSheet sheet, NormalTask predecessor, NormalTask successor,
		Dependency dependency, long originalStart, int predecessorRow, int startColumn) { }
}
