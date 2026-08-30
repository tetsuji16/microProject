/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.spreadsheet.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.AWTEvent;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.JFrame;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.field.Field;
import com.microproject.graphic.configuration.SpreadSheetCategories;
import com.microproject.grouping.core.Node;
import com.microproject.options.CalendarOption;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetModel;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetUtils;
import com.microproject.pm.graphic.spreadsheet.renderer.NameCellComponent;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.util.DateTime;

/** Non-headless regression coverage for the visible task duration edit flow. */
class TaskDurationGuiAcceptanceTest {
	private JFrame frame;
	private DialogCapture dialogs;

	@AfterEach
	void closeWindow() throws Exception {
		if (dialogs != null)
			dialogs.close();
		if (frame != null) {
			SwingUtilities.invokeAndWait(() -> {
				frame.dispose();
				frame = null;
			});
		}
	}

	@Test
	void f2DurationEditCommitsThroughTheVisibleSpreadsheetWithoutUnexpectedDialog() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture();
		dialogs = new DialogCapture();
		dialogs.open();
		showFixture(fixture);

		Robot robot = new Robot();
		robot.setAutoDelay(40);
		try {
			activateFixtureWindow(fixture);
			clickCell(robot, fixture);
			GuiAcceptanceSupport.await(() -> frame.isFocused() && fixture.sheet.isFocusOwner(),
				"the Robot click did not leave the spreadsheet in the active fixture window");
			GuiAcceptanceSupport.await(() -> fixture.sheet.isRowFullySelected(fixture.taskRow),
				"the task-cell click did not preserve the complete task-row selection");
			assertEquals(0, fixture.sheet.getSelectedColumn(),
				"a complete task-row selection has the first column as JTable's lead selection");
			assertEquals(fixture.durationColumn, fixture.sheet.getSelection().getActiveColumn());
			assertEquals(fixture.sheet.getColumnCount(), fixture.sheet.getSelectedColumnCount(),
				"a real task-cell click must highlight the complete task row");
			assertTrue(fixture.sheet.isRowFullySelected(fixture.taskRow),
				"the clicked task row must remain fully highlighted while its active cell is retained");
			assertNull(fixture.sheet.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
				.get(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0)), "ETable must not intercept the global F2 shortcut");
			assertNull(fixture.sheet.getInputMap(JComponent.WHEN_FOCUSED)
				.get(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0)), "the spreadsheet itself must not intercept the global F2 shortcut");

			// Dispatch the F2 KeyEvent through Swing's focused-component route. Calling the root-pane action directly would
			// bypass the ETable ancestor InputMap that caused issue #415, so it
			// cannot prove that the duplicate shortcut route stays removed.
			dispatchF2(fixture.sheet);
			GuiAcceptanceSupport.await(() -> Boolean.TRUE.equals(fixture.sheet.getClientProperty("gui.edit.started")),
				"F2 did not reach the root-pane EditField action");
			GuiAcceptanceSupport.await(fixture.sheet::isEditing, "F2 did not start editing");
			GuiAcceptanceSupport.await(() -> fixture.sheet.getEditorComponent() != null && fixture.sheet.getEditorComponent().isFocusOwner(), "editor did not receive focus");
			assertEquals(fixture.durationColumn, fixture.sheet.getEditingColumn(), "F2 must edit the selected duration column");
			SwingUtilities.invokeAndWait(() -> {
				JTextComponent editor = (JTextComponent) fixture.sheet.getEditorComponent();
				editor.setText("3");
				assertTrue(fixture.sheet.getCellEditor().stopCellEditing(), "duration editor rejected valid text");
			});
			GuiAcceptanceSupport.await(() -> !fixture.sheet.isEditing(), "Enter did not commit the edit");

			assertEquals(3L * CalendarOption.getInstance().getMillisPerDay(), fixture.task.getRawDuration());
			assertTrue(dialogs.messages().isEmpty(), "unexpected dialog: " + dialogs.messages());
		} catch (AssertionError | RuntimeException failure) {
			captureFailure(robot, failure);
			throw failure;
		}
	}

	@Test
	void newTaskNameAndDurationEditsKeepTheVisibleDatesAndActiveCellInSync() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		NewTaskFixture fixture = createEmptyFixture();
		showSheet(fixture.sheet);
		Robot robot = new Robot();
		robot.setAutoDelay(40);
		try {
			activateFixtureWindow(fixture.sheet);
			clickCell(robot, fixture.sheet, fixture.newTaskRow, fixture.nameColumn);
			dispatchF2(fixture.sheet);
			GuiAcceptanceSupport.await(fixture.sheet::isEditing, "F2 did not start name editing on the new-task row");
			SwingUtilities.invokeAndWait(() -> {
				NameCellComponent editor = (NameCellComponent)fixture.sheet.getEditorComponent();
				((JTextComponent)editor.getTextComponent()).setText("GUI added task");
				assertTrue(fixture.sheet.getCellEditor().stopCellEditing(), "name editor rejected a new task name");
			});
			GuiAcceptanceSupport.await(() -> !fixture.sheet.isEditing(), "new task name did not commit");

			NormalTask task = taskAt(fixture.sheet, fixture.newTaskRow);
			assertEquals("GUI added task", task.getName());
			clickCell(robot, fixture.sheet, fixture.newTaskRow, fixture.durationColumn);
			assertEquals(fixture.durationColumn, fixture.sheet.getSelection().getActiveColumn(),
				"clicking Duration must retain Duration as the active view column before editing");
			dispatchF2(fixture.sheet);
			GuiAcceptanceSupport.await(fixture.sheet::isEditing, "F2 did not start duration editing on the added task");
			assertEquals(fixture.durationColumn, fixture.sheet.getEditingColumn(),
				"the active view column and editor column diverged");
			SwingUtilities.invokeAndWait(() -> {
				JTextComponent editor = (JTextComponent)fixture.sheet.getEditorComponent();
				editor.setText("5");
				assertTrue(fixture.sheet.getCellEditor().stopCellEditing(), "duration editor rejected valid text");
			});
			GuiAcceptanceSupport.await(() -> !fixture.sheet.isEditing(), "new task duration did not commit");

			assertEquals(5L * CalendarOption.getInstance().getMillisPerDay(), task.getRawDuration());
			assertTrue(task.getEnd() > task.getStart(), "duration edit must move Finish after Start for a newly added task: start="
				+ task.getStart() + ", finish=" + task.getEnd());
			assertNotEquals(fixture.sheet.getValueAt(fixture.newTaskRow, fixture.startColumn),
				fixture.sheet.getValueAt(fixture.newTaskRow, fixture.finishColumn),
				"visible Start and Finish cells must refresh after the duration edit");
			captureNewTaskScenario(robot);
		} catch (AssertionError | RuntimeException failure) {
			captureFailure(robot, failure);
			throw failure;
		}
	}

	private void showFixture(Fixture fixture) throws Exception {
		showSheet(fixture.sheet);
	}

	private void showSheet(SpreadSheet sheet) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			frame = new JFrame("Task duration GUI acceptance");
			JComponent rootPane = frame.getRootPane();
			rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "EditField");
			ActionMap actions = rootPane.getActionMap();
			actions.put("EditField", new AbstractAction() {
				@Override public void actionPerformed(java.awt.event.ActionEvent event) {
					sheet.putClientProperty("gui.edit.started", sheet.editActiveCell());
				}
			});
			frame.add(new JScrollPane(sheet));
			frame.setPreferredSize(new Dimension(900, 420));
			frame.pack();
			frame.setLocationByPlatform(true);
			frame.setAlwaysOnTop(true);
			frame.setVisible(true);
		});
	}

	private void activateFixtureWindow(Fixture fixture) throws Exception {
		activateFixtureWindow(fixture.sheet);
	}

	private void activateFixtureWindow(SpreadSheet sheet) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			frame.toFront();
			frame.requestFocus();
			sheet.requestFocusInWindow();
		});
		GuiAcceptanceSupport.await(sheet::isFocusOwner, "spreadsheet did not receive focus");
	}

	private static Fixture createFixture() throws Exception {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("gui-duration-edit-test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		NormalTask task = project.createScriptedTask();
		task.setName("Duration edit");
		task.setDuration(CalendarOption.getInstance().getMillisPerDay());
		final Fixture[] fixture = new Fixture[1];
		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet sheet = new SpreadSheet();
			sheet.setSpreadSheetCategory(SpreadSheetCategories.taskSpreadsheetCategory);
			NodeModelCache cache = NodeModelCacheFactory.getInstance().createFilteredCache(
				NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel()), "gui-duration-edit-test", null);
			SpreadSheetUtils.setFieldsAndContext(sheet, cache, SpreadSheetCategories.taskSpreadsheetCategory, "Spreadsheet.Task.entry", true);
			Node taskNode = (Node) cache.getModel().search(task);
			int taskRow = ((SpreadSheetModel) sheet.getModel()).findGraphicNodeRow(cache.getGraphicNode(taskNode));
			fixture[0] = new Fixture(sheet, task, taskRow, findDurationColumn(sheet));
		});
		return fixture[0];
	}

	private static int findDurationColumn(SpreadSheet sheet) {
		SpreadSheetModel model = (SpreadSheetModel) sheet.getModel();
		for (int modelColumn = 0; modelColumn < model.getColumnCount(); modelColumn++) {
			Field field = model.getFieldInColumn(modelColumn);
			if (field != null && "Field.duration".equals(field.getId()))
				return sheet.convertColumnIndexToView(modelColumn);
		}
		throw new IllegalArgumentException("Missing duration column");
	}

	private static void clickCell(Robot robot, Fixture fixture) throws Exception {
		clickCell(robot, fixture.sheet, fixture.taskRow, fixture.durationColumn);
	}

	private static void clickCell(Robot robot, SpreadSheet sheet, int row, int column) throws Exception {
		final Rectangle[] cell = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> {
			int otherColumn = column == 0 ? 1 : column - 1;
			sheet.changeSelection(row, otherColumn, false, false);
			Rectangle bounds = sheet.getCellRect(row, column, true);
			Point location = sheet.getLocationOnScreen();
			cell[0] = new Rectangle(location.x + bounds.x, location.y + bounds.y, bounds.width, bounds.height);
		});
		robot.mouseMove(cell[0].x + cell[0].width / 2, cell[0].y + cell[0].height / 2);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
	}

	private static void dispatchF2(SpreadSheet sheet) throws Exception {
		SwingUtilities.invokeAndWait(() -> KeyboardFocusManager.getCurrentKeyboardFocusManager().dispatchEvent(
			new KeyEvent(sheet, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_F2, KeyEvent.CHAR_UNDEFINED)));
	}

	private void captureFailure(Robot robot, Throwable failure) {
		try {
			if (frame == null)
				return;
			Path directory = Path.of(System.getProperty("micrproject.gui.artifacts.dir", "build/guiTest-artifacts"));
			Files.createDirectories(directory);
			Rectangle bounds = rootPaneBounds();
			BufferedImage screenshot = robot.createScreenCapture(bounds);
			ImageIO.write(screenshot, "png", directory.resolve("task-duration-edit-failure.png").toFile());
		} catch (Exception captureFailure) {
			failure.addSuppressed(captureFailure);
		}
	}

	private void captureNewTaskScenario(Robot robot) throws Exception {
		Path directory = Path.of(System.getProperty("micrproject.gui.artifacts.dir", "build/guiTest-artifacts"));
		Files.createDirectories(directory);
		ImageIO.write(robot.createScreenCapture(rootPaneBounds()), "png", directory.resolve("new-task-duration-date-sync.png").toFile());
	}

	private Rectangle rootPaneBounds() throws Exception {
		Rectangle[] bounds = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> bounds[0] = new Rectangle(frame.getRootPane().getLocationOnScreen(), frame.getRootPane().getSize()));
		return bounds[0];
	}

	private static NewTaskFixture createEmptyFixture() throws Exception {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("gui-new-task-date-sync", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		project.setStart(DateTime.calendarInstance(2026, Calendar.SEPTEMBER, 1).getTimeInMillis());
		final NewTaskFixture[] fixture = new NewTaskFixture[1];
		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet sheet = new SpreadSheet();
			sheet.setSpreadSheetCategory(SpreadSheetCategories.taskSpreadsheetCategory);
			NodeModelCache cache = NodeModelCacheFactory.getInstance().createFilteredCache(
				NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel()), "gui-new-task-date-sync", null);
			SpreadSheetUtils.setFieldsAndContext(sheet, cache, SpreadSheetCategories.taskSpreadsheetCategory, "Spreadsheet.Task.entry", true);
			int nameColumn = findColumn(sheet, "Field.name");
			int durationColumn = findColumn(sheet, "Field.duration");
			int startColumn = findColumn(sheet, "Field.start");
			int finishColumn = findColumn(sheet, "Field.finish");
			setColumnWidth(sheet, nameColumn, 210);
			setColumnWidth(sheet, durationColumn, 90);
			setColumnWidth(sheet, startColumn, 120);
			setColumnWidth(sheet, finishColumn, 120);
			fixture[0] = new NewTaskFixture(project, sheet, 0, nameColumn, durationColumn, startColumn, finishColumn);
		});
		return fixture[0];
	}

	private static NormalTask taskAt(SpreadSheet sheet, int row) throws Exception {
		final NormalTask[] task = new NormalTask[1];
		SwingUtilities.invokeAndWait(() -> task[0] = (NormalTask)((SpreadSheetModel)sheet.getModel()).getNodeInRow(row).getImpl());
		return task[0];
	}

	private static int findColumn(SpreadSheet sheet, String fieldId) {
		SpreadSheetModel model = (SpreadSheetModel)sheet.getModel();
		for (int modelColumn = 0; modelColumn < model.getColumnCount(); modelColumn++) {
			Field field = model.getFieldInColumn(modelColumn);
			if (field != null && fieldId.equals(field.getId()))
				return sheet.convertColumnIndexToView(modelColumn);
		}
		throw new IllegalArgumentException("Missing " + fieldId + " column");
	}

	private static void setColumnWidth(SpreadSheet sheet, int column, int width) {
		var tableColumn = sheet.getColumnModel().getColumn(column);
		tableColumn.setPreferredWidth(width);
		tableColumn.setWidth(width);
	}

	private record Fixture(SpreadSheet sheet, NormalTask task, int taskRow, int durationColumn) { }
	private record NewTaskFixture(Project project, SpreadSheet sheet, int newTaskRow, int nameColumn, int durationColumn,
			int startColumn, int finishColumn) { }

	private static final class DialogCapture implements AWTEventListener {
		private final List<String> messages = new ArrayList<>();
		void open() { Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.WINDOW_EVENT_MASK); }
		void close() { Toolkit.getDefaultToolkit().removeAWTEventListener(this); }
		List<String> messages() { synchronized (messages) { return List.copyOf(messages); } }
		@Override public void eventDispatched(AWTEvent event) {
			if (event instanceof WindowEvent windowEvent && windowEvent.getID() == WindowEvent.WINDOW_OPENED && windowEvent.getWindow() instanceof Dialog dialog) {
				synchronized (messages) { messages.add(dialog.getTitle()); }
			}
		}
	}

}
