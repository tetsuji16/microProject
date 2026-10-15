/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.spreadsheet.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.util.Calendar;
import java.awt.event.InputEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.JFrame;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.field.Field;
import com.microproject.field.FieldContext;
import com.microproject.configuration.FieldDictionary;
import com.microproject.datatype.Duration;
import com.microproject.datatype.DurationFormat;
import com.microproject.datatype.DurationDefaults;
import com.microproject.datatype.TimeUnit;
import com.microproject.graphic.configuration.SpreadSheetCategories;
import com.microproject.configuration.Dictionary;
import com.microproject.graphic.configuration.BarStyles;
import com.microproject.pm.graphic.gantt.Gantt;
import com.microproject.grouping.core.Node;
import com.microproject.pm.graphic.timescale.CoordinatesConverter;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetModel;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetUtils;
import com.microproject.pm.graphic.spreadsheet.editor.DateEditor;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.ProjectFactory;
import com.microproject.exchange.MpoFileImporter;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.DateTime;

/** U-26 physical multi-character spreadsheet input transaction coverage. */
class U26SpreadsheetInputTransactionGuiAcceptanceTest {
	private JFrame frame;
	private JTabbedPane tabs;

	@AfterEach
	void closeWindow() throws Exception {
		if (frame != null) SwingUtilities.invokeAndWait(() -> {
			frame.dispose();
			frame = null;
		});
	}

	@Test
	void robotTypesDurationAndPercentAsOneInputTransaction() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
			"A desktop session is required for U-26 physical input coverage.");
		Fixture fixture = createFixture();
		showFixture(fixture);
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(35);
		SwingUtilities.invokeAndWait(() -> tabs.setSelectedIndex(0));
		activate(fixture.entrySheet, robot);
		editWithPhysicalKeys(robot, fixture.entrySheet, fixture.entryRow, fixture.durationColumn, "10", "8 days");
		assertEquals(10L * 60L * 60L * 1000L, Duration.millis(fixture.task.getRawDuration()),
			"physical bare duration input must use the owning project's default hours");
		// Task schedules normalize entered durations to physical milliseconds; parsing
		// the bare input with this project's Hours setting records that per-task unit.
		assertEquals(TimeUnit.HOURS, fixture.task.getProject().getDurationDefaults().durationEnteredIn(),
			"the owning project must retain Hours as its duration-entry and display unit");
		assertEquals(DurationFormat.getProjectInstance().format(fixture.task.getDurationForDisplay(),
			fixture.task.getProject().getDurationDefaults()), durationText(fixture.task,
			((SpreadSheetModel) fixture.entrySheet.getModel()).getFieldContext()),
			"the committed duration cell must render using the owning project's Hours default");

		editWithPhysicalKeys(robot, fixture.entrySheet, fixture.entryRow, fixture.startColumn, "2026/9/25");
		Calendar committedStart = DateTime.calendarInstance();
		committedStart.setTimeInMillis(fixture.task.getStart());
		int startModelColumn = fixture.entrySheet.convertColumnIndexToModel(fixture.startColumn);
		Field startField = ((SpreadSheetModel) fixture.entrySheet.getModel()).getFieldInColumn(startModelColumn);
		assertEquals(2026, committedStart.get(Calendar.YEAR), "physical date input must commit the year");
		assertEquals(Calendar.SEPTEMBER, committedStart.get(Calendar.MONTH),
			"physical date input must commit the month: " + committedStart.getTime()
				+ " field=" + (startField == null ? null : startField.getId())
				+ " editable=" + fixture.entrySheet.isCellEditable(fixture.entryRow, fixture.startColumn)
				+ " cell=" + ((SpreadSheetModel) fixture.entrySheet.getModel()).getValueAt(fixture.entryRow, startModelColumn));
		assertEquals(25, committedStart.get(Calendar.DAY_OF_MONTH), "physical date input must commit the day");
		assertEquals(DurationFormat.getProjectInstance().format(new Duration(fixture.task.getRemainingDuration()),
			fixture.task.getProject().getDurationDefaults()), remainingDurationText(fixture),
			"0% renderer must show the full remaining duration using project defaults");
		fixture.setProgressBaseline();

		SwingUtilities.invokeAndWait(() -> tabs.setSelectedIndex(1));
		activate(fixture.trackingSheet, robot);
		editWithPhysicalKeys(robot, fixture.trackingSheet, fixture.trackingRow, fixture.percentColumn, "10");
		assertEquals(0.10d, fixture.task.getPercentComplete(), 0.00001d,
			"physical multi-character percent input must commit the complete value");

		assertTrue(fixture.task.getRemainingDuration() > 0L, "10% progress must leave a positive remaining duration");
		assertEquals(DurationFormat.getProjectInstance().format(new Duration(fixture.task.getRemainingDuration()),
			fixture.task.getProject().getDurationDefaults()), remainingDurationText(fixture),
			"10% renderer must show the intermediate remaining duration");
		GanttGeometry afterTen = ganttGeometry(fixture);
		assertEquals(fixture.progressViewportBefore(), afterTen.viewport(),
			"10% task-table input must not move the Gantt timescale viewport");
		assertEquals(fixture.progressBarBefore(), afterTen.bar(),
			"10% task-table input must not change the planned bar geometry");
		editWithPhysicalKeys(robot, fixture.trackingSheet, fixture.trackingRow, fixture.percentColumn, "50");
		assertEquals(0.50d, fixture.task.getPercentComplete(), 0.00001d,
			"physical intermediate percent input must commit 50%");
		assertTrue(fixture.task.getRemainingDuration() > 0L, "50% progress must leave a positive remaining duration");
		GanttGeometry afterHalf = ganttGeometry(fixture);
		assertEquals(fixture.progressViewportBefore(), afterHalf.viewport(),
			"50% task-table input must not move the Gantt timescale viewport");
		assertEquals(fixture.progressBarBefore(), afterHalf.bar(),
			"50% task-table input must not change the planned bar geometry");
		editWithPhysicalKeys(robot, fixture.trackingSheet, fixture.trackingRow, fixture.percentColumn, "99");
		assertEquals(0.99d, fixture.task.getPercentComplete(), 0.00001d,
			"physical near-complete percent input must commit 99%");
		assertTrue(fixture.task.getRemainingDuration() > 0L, "99% progress must leave a positive remaining duration");
		GanttGeometry afterNearComplete = ganttGeometry(fixture);
		assertEquals(fixture.progressViewportBefore(), afterNearComplete.viewport(),
			"99% task-table input must not move the Gantt timescale viewport");
		assertEquals(fixture.progressBarBefore(), afterNearComplete.bar(),
			"99% task-table input must not change the planned bar geometry");
		editWithPhysicalKeys(robot, fixture.trackingSheet, fixture.trackingRow, fixture.percentColumn, "100");
		assertEquals(1.0d, fixture.task.getPercentComplete(), 0.00001d,
			"physical 100% input must commit the complete value");
		assertEquals(DurationFormat.getProjectInstance().format(new Duration(fixture.task.getRemainingDuration()),
			fixture.task.getProject().getDurationDefaults()), remainingDurationText(fixture),
			"100% renderer must show zero remaining duration using project defaults");
		GanttGeometry afterComplete = ganttGeometry(fixture);
		assertEquals(fixture.progressViewportBefore(), afterComplete.viewport(),
			"100% task-table input must not move the Gantt timescale viewport");
		assertEquals(fixture.progressBarBefore(), afterComplete.bar(),
			"100% task-table input must not change the planned bar geometry");

		SwingUtilities.invokeAndWait(() -> tabs.setSelectedIndex(0));
		activate(fixture.entrySheet, robot);
		editWithPhysicalKeys(robot, fixture.entrySheet, fixture.entryRow, fixture.durationColumn, "1 day", "10 hours");
		assertEquals(6L * 60L * 60L * 1000L, Duration.millis(fixture.task.getRawDuration()),
			"with a six-hour project day, physical 1 day input must store six hours");
		assertEquals(TimeUnit.DAYS, fixture.task.getDurationDisplayUnit());
		assertEquals("1 day", durationText(fixture.task,
			((SpreadSheetModel) fixture.entrySheet.getModel()).getFieldContext()));
		editWithPhysicalKeys(robot, fixture.entrySheet, fixture.entryRow, fixture.durationColumn, "6 hours", "1 day");
		assertEquals(6L * 60L * 60L * 1000L, Duration.millis(fixture.task.getRawDuration()),
			"physical 6 hours input must match 1 day on this project's calendar");
		assertEquals(TimeUnit.HOURS, fixture.task.getDurationDisplayUnit());
		assertEquals("6 hours", durationText(fixture.task,
			((SpreadSheetModel) fixture.entrySheet.getModel()).getFieldContext()));
		editWithPhysicalKeys(robot, fixture.entrySheet, fixture.entryRow, fixture.durationColumn, "10", "6 hours");
		assertEquals(10L * 60L * 60L * 1000L, Duration.millis(fixture.task.getRawDuration()),
			"restore the bare project-hour duration for the structural save/reload assertions");
		assertEquals(TimeUnit.HOURS, fixture.task.getDurationDisplayUnit());

		Project project = fixture.task.getProject();
		NormalTask structuralTask = project.createScriptedTask();
		structuralTask.setName("U-26 structural round-trip");
		project.setLocalParent(structuralTask, fixture.task);
		assertEquals(fixture.task, structuralTask.getWbsParentTask(), "fixture must create a child before outdent");
		project.setLocalParent(structuralTask, null);
		assertEquals(null, structuralTask.getWbsParentTask(), "outdent must restore the task to the project root");

		ByteArrayOutputStream mpo = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(project, mpo), "MPO save after structural change must succeed");
		MpoFileImporter reader = new MpoFileImporter();
		reader.setProjectFactory(ProjectFactory.getInstance());
		Project reopened = reader.loadProject(new ByteArrayInputStream(mpo.toByteArray()));
		NormalTask restored = (NormalTask) reopened.findByUniqueId(fixture.task.getUniqueId());
		NormalTask restoredStructuralTask = (NormalTask) reopened.findByUniqueId(structuralTask.getUniqueId());
		assertEquals(10L * 60L * 60L * 1000L, Duration.millis(restored.getRawDuration()),
			"the project-unit duration must survive a structural edit and MPO reload");
		assertEquals(null, restoredStructuralTask.getWbsParentTask(), "outdented task must remain at project root after reload");
		assertEquals(TimeUnit.HOURS, reopened.getDurationDefaults().durationEnteredIn());
		FieldContext reopenedContext = new FieldContext();
		reopenedContext.setDurationDefaults(reopened.getDurationDefaults());
		assertEquals(TimeUnit.HOURS, restored.getDurationDisplayUnit(),
			"MPO task format must restore the explicit per-task duration unit");
		assertEquals(DurationFormat.getProjectInstance().format(restored.getDurationForDisplay(),
			reopened.getDurationDefaults()), durationText(restored, reopenedContext),
			"MPO reopen must retain the project duration display unit for the task cell");

	}

	private void showFixture(Fixture fixture) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			frame = new JFrame("U-26 spreadsheet input transaction");
			JComponent root = frame.getRootPane();
			root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "EditField");
			ActionMap actions = root.getActionMap();
			actions.put("EditField", new AbstractAction() {
				@Override public void actionPerformed(java.awt.event.ActionEvent event) {
					fixture.entrySheet.editActiveCell();
				}
			});
			tabs = new JTabbedPane();
			tabs.addTab("Entry", new JScrollPane(fixture.entrySheet));
			tabs.addTab("Tracking", new JScrollPane(fixture.trackingSheet));
			frame.add(new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tabs, new JScrollPane(fixture.gantt)));
			frame.setPreferredSize(new Dimension(1400, 560));
			frame.pack();
			frame.setLocationByPlatform(true);
			frame.setAlwaysOnTop(true);
			frame.setVisible(true);
		});
	}

	private void activate(SpreadSheet sheet, Robot robot) throws Exception {
		GuiAcceptanceSupport.await(() -> isSelectedSheetReady(sheet, false),
			"selected U-26 task table did not finish showing and laying out");
		GuiAcceptanceSupport.activateAndAwaitRenderedWindow(frame, tabs, robot, true,
			"U-26 spreadsheet window");
		SwingUtilities.invokeAndWait(sheet::requestFocusInWindow);
		GuiAcceptanceSupport.await(() -> isSelectedSheetReady(sheet, true),
			"selected U-26 task table was not laid out, visible, and focused");
	}

	private boolean isSelectedSheetReady(SpreadSheet sheet, boolean requireFocus) {
		boolean[] ready = new boolean[1];
		try {
			SwingUtilities.invokeAndWait(() -> ready[0] = frame.isShowing()
				&& sheet.isShowing() && sheet.isValid() && sheet.getWidth() > 0 && sheet.getHeight() > 0
				&& sheet.getVisibleRect().width > 0 && sheet.getVisibleRect().height > 0
				&& SwingUtilities.isDescendingFrom(sheet, tabs.getSelectedComponent())
				&& (!requireFocus || frame.isActive() && sheet.isFocusOwner()));
		} catch (Exception exception) {
			throw new AssertionError("could not inspect the selected U-26 sheet on the EDT", exception);
		}
		return ready[0];
	}

	private void editWithPhysicalKeys(Robot robot, SpreadSheet sheet, int row, int column, String value) throws Exception {
		editWithPhysicalKeys(robot, sheet, row, column, value, null);
	}

	private void editWithPhysicalKeys(Robot robot, SpreadSheet sheet, int row, int column, String value,
			String expectedEditorSeed) throws Exception {
		Rectangle anchor = cellOnScreen(sheet, row, Math.min(1, column));
		clickCell(robot, anchor, 0);
		robot.waitForIdle();
		SwingUtilities.invokeAndWait(() -> {
			sheet.changeSelection(row, column, false, false);
			assertTrue(sheet.editCellAt(row, column, null),
				"the input transaction must start editing row=" + row + " column=" + column);
		});
		GuiAcceptanceSupport.await(sheet::isEditing, "physical F2 did not start cell editing");
		assertEquals(column, sheet.getEditingColumn(), "physical click/F2 must edit the clicked field");
		JTextComponent input = editorInputComponent(sheet);
		GuiAcceptanceSupport.await(input::isFocusOwner, "cell editor did not receive focus");
		assertEquals(input, KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner(),
			"physical input must target the active cell editor");
		SwingUtilities.invokeAndWait(input::requestFocusInWindow);
		GuiAcceptanceSupport.await(input::isFocusOwner, "cell editor lost focus before physical typing");
		if (expectedEditorSeed != null)
			assertEquals(expectedEditorSeed, input.getText(), "duration editor must seed from the task's display format");
		robot.waitForIdle();
		robot.delay(150);
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_A);
		robot.keyRelease(KeyEvent.VK_A);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		for (char character : value.toCharArray()) {
			int keyCode = KeyEvent.getExtendedKeyCodeForChar(character);
			if (keyCode == KeyEvent.VK_UNDEFINED) throw new AssertionError("No key code for " + character);
			robot.keyPress(keyCode);
			robot.keyRelease(keyCode);
		}
		robot.waitForIdle();
		assertEquals(value, activeEditorText(sheet), "physical keys must reach the active editor before commit");
		robot.keyPress(KeyEvent.VK_ENTER);
		robot.keyRelease(KeyEvent.VK_ENTER);
		GuiAcceptanceSupport.await(() -> !sheet.isEditing(), "physical input did not commit: " + value);
	}

	private Rectangle cellOnScreen(SpreadSheet sheet, int row, int column) throws Exception {
		Rectangle[] result = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> {
			Component selectedTab = tabs.getSelectedComponent();
			boolean selectedAncestor = selectedTab != null && SwingUtilities.isDescendingFrom(sheet, selectedTab);
			if (!sheet.isShowing() || !sheet.isValid() || !selectedAncestor)
				throw new AssertionError("U-26 cell coordinates require the selected, laid-out task table to be showing: "
					+ "frameShowing=" + frame.isShowing() + ", selectedIndex=" + tabs.getSelectedIndex()
					+ ", selectedTab=" + componentState(selectedTab) + ", sheet=" + componentState(sheet)
					+ ", selectedAncestor=" + selectedAncestor);
			sheet.changeSelection(row, Math.max(0, column - 1), false, false);
			Rectangle bounds = sheet.getCellRect(row, column, true);
			sheet.scrollRectToVisible(bounds);
			if (!sheet.getVisibleRect().intersects(bounds))
				throw new AssertionError("U-26 target cell is outside the laid-out spreadsheet viewport");
			Point location = sheet.getLocationOnScreen();
			result[0] = new Rectangle(location.x + bounds.x, location.y + bounds.y, bounds.width, bounds.height);
		});
		return result[0];
	}

	private static String componentState(Component component) {
		if (component == null) return "null";
		return component.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(component))
			+ "[showing=" + component.isShowing() + ",valid=" + component.isValid()
			+ ",parent=" + (component.getParent() == null ? "null"
				: component.getParent().getClass().getSimpleName() + "@"
					+ Integer.toHexString(System.identityHashCode(component.getParent())))
			+ ",size=" + component.getWidth() + "x" + component.getHeight() + "]";
	}

	private static void clickCell(Robot robot, Rectangle cell, int xOffset) {
		robot.mouseMove(cell.x + cell.width / 2 + xOffset, cell.y + cell.height / 2);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
	}

	private static JTextComponent editorInputComponent(SpreadSheet sheet) throws Exception {
		JTextComponent[] result = new JTextComponent[1];
		SwingUtilities.invokeAndWait(() -> {
			Component editor = sheet.getEditorComponent();
			if (editor instanceof DateEditor.ExtDateField date) {
				result[0] = date.getTextField();
			} else {
				result[0] = findTextComponent(editor);
			}
		});
		return result[0];
	}

	private static JTextComponent findTextComponent(Component component) {
		if (component instanceof JTextComponent text) return text;
		if (component instanceof java.awt.Container container) {
			for (Component child : container.getComponents()) {
				JTextComponent result = findTextComponent(child);
				if (result != null) return result;
			}
		}
		return null;
	}

	private static String activeEditorText(SpreadSheet sheet) throws Exception {
		String[] result = new String[1];
		SwingUtilities.invokeAndWait(() -> {
			if (sheet.getEditorComponent() instanceof JTextComponent text) result[0] = text.getText();
			else if (sheet.getEditorComponent() instanceof DateEditor.ExtDateField date) result[0] = date.getTextField().getText();
		});
		return result[0];
	}

	private static Fixture createFixture() throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("u26-physical-input", undo), undo);
		project.setDurationDefaults(new DurationDefaults(360.0D, 1800.0D, 20.0D,
			TimeUnit.HOURS, TimeUnit.HOURS));
		project.initialize(false, false);
		NormalTask task = project.createScriptedTask();
		task.setName("U-26 physical input");
		task.setStart(DateTime.calendarInstance(2026, Calendar.SEPTEMBER, 1).getTimeInMillis());
		task.setDuration(Duration.getInstance(8.0D, TimeUnit.DAYS, project.getDurationDefaults()));
		task.setPercentComplete(0.0d);
		final Fixture[] result = new Fixture[1];
		SwingUtilities.invokeAndWait(() -> {
			SheetSetup entry = createSheet(project, "u26-physical-input-entry", "Spreadsheet.Task.entry");
			SheetSetup tracking = createSheet(project, "u26-physical-input-tracking", "Spreadsheet.Task.tracking");
		int entryRow = findTaskRow(entry, task);
		int trackingRow = findTaskRow(tracking, task);
			Gantt gantt = new Gantt(project, "Gantt");
			gantt.setCache(entry.cache());
			gantt.setCoord(new CoordinatesConverter(project));
			gantt.setBarStyles((BarStyles) Dictionary.get(BarStyles.category, "standard"));
			gantt.updateSize();
			result[0] = new Fixture(entry.sheet(), tracking.sheet(), gantt, task, entryRow, trackingRow,
				findColumn(entry.sheet(), "Field.duration"), findColumn(tracking.sheet(), "Field.percentComplete"),
				findColumn(entry.sheet(), "Field.start"));
		});
		return result[0];
	}

	private static SheetSetup createSheet(Project project, String cacheId, String spreadsheetId) {
		SpreadSheet sheet = new SpreadSheet();
		sheet.setSpreadSheetCategory(SpreadSheetCategories.taskSpreadsheetCategory);
		NodeModelCache cache = NodeModelCacheFactory.getInstance().createFilteredCache(
			NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel()), cacheId, null);
		SpreadSheetUtils.setFieldsAndContext(sheet, cache, SpreadSheetCategories.taskSpreadsheetCategory, spreadsheetId, true);
		((SpreadSheetModel) sheet.getModel()).getFieldContext().setDurationDefaultsResolver(project::getDurationDefaults);
		return new SheetSetup(sheet, cache);
	}

	private static int findTaskRow(SheetSetup setup, NormalTask task) {
		SpreadSheetModel model = (SpreadSheetModel) setup.sheet().getModel();
		Node node = (Node) setup.cache().getModel().search(task);
		return model.findGraphicNodeRow(setup.cache().getGraphicNode(node));
	}

	private static int findColumn(SpreadSheet sheet, String fieldId) {
		SpreadSheetModel model = (SpreadSheetModel) sheet.getModel();
		for (int column = 0; column < model.getColumnCount(); column++) {
			Field field = model.getFieldInColumn(column);
			if (field != null && fieldId.equals(field.getId())) return sheet.convertColumnIndexToView(column);
		}
		throw new IllegalArgumentException("Missing field: " + fieldId);
	}

	private static String remainingDurationText(Fixture fixture) {
		Field field = FieldDictionary.getInstance().getFieldFromId("Field.remainingDuration");
		return field.getText(fixture.task,
			((SpreadSheetModel) fixture.entrySheet.getModel()).getFieldContext());
	}

	private static String durationText(NormalTask task, FieldContext context) {
		Field field = FieldDictionary.getInstance().getFieldFromId("Field.duration");
		return field.getText(task, context);
	}

	private static GanttGeometry ganttGeometry(Fixture fixture) throws Exception {
		GanttGeometry[] result = new GanttGeometry[1];
		SwingUtilities.invokeAndWait(() -> {
			JScrollPane pane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, fixture.gantt);
			Point viewport = pane == null ? new Point() : pane.getViewport().getViewPosition();
			double start = fixture.gantt.getCoord().toX(fixture.task.getStart());
			double width = fixture.gantt.getCoord().toX(fixture.task.getEnd()) - start;
			result[0] = new GanttGeometry(viewport, new BarGeometry((int) Math.round(start), (int) Math.round(width)));
		});
		return result[0];
	}

	private record BarGeometry(int startX, int width) { }
	private record GanttGeometry(Point viewport, BarGeometry bar) { }

	private static final class Fixture {
		final SpreadSheet entrySheet;
		final SpreadSheet trackingSheet;
		final Gantt gantt;
		final NormalTask task;
		final int entryRow, trackingRow, durationColumn, percentColumn, startColumn;
		private Point progressViewportBefore;
		private BarGeometry progressBarBefore;

		Fixture(SpreadSheet entrySheet, SpreadSheet trackingSheet, Gantt gantt, NormalTask task, int entryRow,
			int trackingRow, int durationColumn, int percentColumn, int startColumn) {
			this.entrySheet = entrySheet; this.trackingSheet = trackingSheet; this.gantt = gantt; this.task = task;
			this.entryRow = entryRow; this.trackingRow = trackingRow; this.durationColumn = durationColumn;
			this.percentColumn = percentColumn; this.startColumn = startColumn;
		}

		void setProgressBaseline() throws Exception {
			GanttGeometry geometry = ganttGeometry(this);
			progressViewportBefore = geometry.viewport();
			progressBarBefore = geometry.bar();
		}
		Point progressViewportBefore() { return progressViewportBefore; }
		BarGeometry progressBarBefore() { return progressBarBefore; }
	}
	private record SheetSetup(SpreadSheet sheet, NodeModelCache cache) { }
}
