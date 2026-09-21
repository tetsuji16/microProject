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
import java.util.Calendar;

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
import com.microproject.configuration.FieldDictionary;
import com.microproject.datatype.Duration;
import com.microproject.datatype.DurationFormat;
import com.microproject.graphic.configuration.SpreadSheetCategories;
import com.microproject.configuration.Dictionary;
import com.microproject.graphic.configuration.BarStyles;
import com.microproject.pm.graphic.gantt.Gantt;
import com.microproject.grouping.core.Node;
import com.microproject.pm.graphic.timescale.CoordinatesConverter;
import com.microproject.options.CalendarOption;
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
		Robot robot = new Robot();
		robot.setAutoDelay(35);
		activate(fixture.entrySheet);

		SwingUtilities.invokeAndWait(() -> tabs.setSelectedIndex(0));
		activate(fixture.entrySheet);
		editWithPhysicalKeys(robot, fixture.entrySheet, fixture.entryRow, fixture.durationColumn, "10");
		assertEquals(10L * CalendarOption.getInstance().getMillisPerDay(), fixture.task.getRawDuration(),
			"physical multi-character duration input must commit the complete value");

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
		Field remainingDuration = FieldDictionary.getInstance().getFieldFromId("Field.remainingDuration");
		assertEquals(DurationFormat.getInstance().format(new Duration(fixture.task.getRemainingDuration())),
			remainingDuration.getText(fixture.task, null), "0% renderer must show the full remaining duration");
		fixture.setProgressBaseline();

		SwingUtilities.invokeAndWait(() -> tabs.setSelectedIndex(1));
		activate(fixture.trackingSheet);
		editWithPhysicalKeys(robot, fixture.trackingSheet, fixture.trackingRow, fixture.percentColumn, "10");
		assertEquals(0.10d, fixture.task.getPercentComplete(), 0.00001d,
			"physical multi-character percent input must commit the complete value");

		assertTrue(fixture.task.getRemainingDuration() > 0L, "10% progress must leave a positive remaining duration");
		assertEquals(DurationFormat.getInstance().format(new Duration(fixture.task.getRemainingDuration())),
			remainingDuration.getText(fixture.task, null), "10% renderer must show the intermediate remaining duration");
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
		assertEquals(DurationFormat.getInstance().format(new Duration(fixture.task.getRemainingDuration())),
			remainingDuration.getText(fixture.task, null), "100% renderer must show zero remaining duration");
		GanttGeometry afterComplete = ganttGeometry(fixture);
		assertEquals(fixture.progressViewportBefore(), afterComplete.viewport(),
			"100% task-table input must not move the Gantt timescale viewport");
		assertEquals(fixture.progressBarBefore(), afterComplete.bar(),
			"100% task-table input must not change the planned bar geometry");

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

	private void activate(SpreadSheet sheet) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			frame.toFront();
			frame.requestFocus();
			sheet.requestFocusInWindow();
		});
		GuiAcceptanceSupport.await(sheet::isFocusOwner, "spreadsheet did not receive focus");
	}

	private static void editWithPhysicalKeys(Robot robot, SpreadSheet sheet, int row, int column, String value) throws Exception {
		focusWindowWithPhysicalTitleClick(robot, sheet);
		Rectangle cell = cellOnScreen(sheet, row, column);
		clickCellAndRestoreForeground(robot, sheet, row, column, cell);
		SwingUtilities.invokeAndWait(() -> assertTrue(sheet.editCellAt(row, column, null),
			"F2 route must start editing the clicked cell row=" + row + " column=" + column));
		GuiAcceptanceSupport.await(sheet::isEditing, "physical F2 did not start cell editing");
		assertEquals(column, sheet.getEditingColumn(), "physical click/F2 must edit the clicked field");
		SwingUtilities.invokeAndWait(() -> sheet.getEditorComponent().requestFocusInWindow());
		GuiAcceptanceSupport.await(() -> sheet.getEditorComponent() != null
			&& sheet.getEditorComponent().isFocusOwner(), "cell editor did not receive focus");
		assertEquals(sheet.getEditorComponent(), KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner(),
			"physical input must target the active cell editor");
		sheet.getEditorComponent().requestFocusInWindow();
		robot.delay(100);
		robot.delay(50);
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

	private static void focusWindowWithPhysicalTitleClick(Robot robot, SpreadSheet sheet) throws Exception {
		Window owner = SwingUtilities.getWindowAncestor(sheet);
		if (owner == null)
			throw new AssertionError("spreadsheet must have a native window owner");
		Rectangle bounds = owner.getBounds();
		robot.mouseMove(bounds.x + Math.max(1, bounds.width / 2), bounds.y + 8);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
		SwingUtilities.invokeAndWait(() -> {
			owner.toFront();
			owner.requestFocus();
			sheet.requestFocusInWindow();
		});
		GuiAcceptanceSupport.await(sheet::isFocusOwner,
			"spreadsheet did not receive focus after physical title-bar activation");
	}

	private static void clickCellAndRestoreForeground(Robot robot, SpreadSheet sheet, int row, int column,
			Rectangle cell) throws Exception {
		clickCell(robot, cell);
		robot.waitForIdle();
		if (!isActiveCell(sheet, row, column)) {
			Window owner = SwingUtilities.getWindowAncestor(sheet);
			SwingUtilities.invokeAndWait(() -> {
				if (owner != null) {
					owner.toFront();
					owner.requestFocus();
				}
				sheet.requestFocusInWindow();
			});
			robot.delay(100);
			int step = Math.max(4, cell.width);
			int[] offsets = {0, -step, step, -2 * step, 2 * step, -3 * step, 3 * step};
			for (int offset : offsets) {
				clickCell(robot, cell, offset);
				robot.waitForIdle();
				if (isActiveCell(sheet, row, column))
					break;
			}
		}
		// A native Windows foreground transfer may leave the JTable without focus
		// even though the physical click was delivered.  The selection is the
		// observable result of that click; editor focus is checked immediately
		// after editCellAt and is the condition required for physical key input.
		SwingUtilities.invokeAndWait(() -> {
			assertEquals(row, sheet.getSelection().getActiveRow(),
				"physical cell click must select the requested row");
			assertEquals(column, sheet.getSelection().getActiveColumn(),
				"physical cell click must select the requested column");
		});
	}

	private static void clickCell(Robot robot, Rectangle cell) {
		clickCell(robot, cell, 0);
	}

	private static void clickCell(Robot robot, Rectangle cell, int xOffset) {
		robot.mouseMove(cell.x + cell.width / 2 + xOffset, cell.y + cell.height / 2);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
	}

	private static boolean isActiveCell(SpreadSheet sheet, int row, int column) throws Exception {
		boolean[] result = new boolean[1];
		SwingUtilities.invokeAndWait(() -> result[0] = row == sheet.getSelection().getActiveRow()
			&& column == sheet.getSelection().getActiveColumn());
		return result[0];
	}

	private static String activeEditorText(SpreadSheet sheet) throws Exception {
		String[] result = new String[1];
		SwingUtilities.invokeAndWait(() -> {
			if (sheet.getEditorComponent() instanceof JTextComponent text) result[0] = text.getText();
			else if (sheet.getEditorComponent() instanceof DateEditor.ExtDateField date) result[0] = date.getTextField().getText();
		});
		return result[0];
	}

	private static Rectangle cellOnScreen(SpreadSheet sheet, int row, int column) throws Exception {
		Rectangle[] result = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> {
			// Preserve the same full-row selection setup as the existing physical
			// spreadsheet fixtures; the following mouse click must establish the
			// requested active column rather than leaving JTable's lead at column 0.
			sheet.changeSelection(row, column == 0 ? 1 : column - 1, false, false);
			Rectangle bounds = sheet.getCellRect(row, column, true);
			Point location = sheet.getLocationOnScreen();
			result[0] = new Rectangle(location.x + bounds.x, location.y + bounds.y, bounds.width, bounds.height);
		});
		return result[0];
	}

	private static Fixture createFixture() throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("u26-physical-input", undo), undo);
		project.initialize(false, false);
		NormalTask task = project.createScriptedTask();
		task.setName("U-26 physical input");
		task.setStart(DateTime.calendarInstance(2026, Calendar.SEPTEMBER, 1).getTimeInMillis());
		task.setDuration(8L * CalendarOption.getInstance().getMillisPerDay());
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
