/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Point;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.border.LineBorder;
import javax.swing.table.TableCellRenderer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.graphic.configuration.SpreadSheetCategories;
import com.microproject.graphic.configuration.SpreadSheetFieldArray;
import com.microproject.configuration.Dictionary;
import com.microproject.graphic.configuration.BarStyles;
import com.microproject.pm.graphic.gantt.Gantt;
import com.microproject.pm.graphic.gantt.GanttUI;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetColumnMenu;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetUtils;
import com.microproject.pm.graphic.spreadsheet.editor.DateEditor;
import com.microproject.pm.graphic.spreadsheet.selection.SpreadSheetColumnsPopupMenu;
import com.microproject.pm.graphic.timescale.CoordinatesConverter;
import com.microproject.pm.graphic.views.synchro.ScrollPaneSynchronizer;
import com.microproject.pm.graphic.views.synchro.Synchronizer;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.options.CalendarOption;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.NormalTask;
import com.microproject.field.Field;
import com.microproject.exchange.MpoFileImporter;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.FlatUiSupport;
import com.microproject.util.GanttProgress;
import com.microproject.workspace.SavableToWorkspace;
import com.microproject.workspace.WorkspaceSetting;

/** Verifies that the visible task-table/Gantt pair shares one grid-style path. */
class TaskTableGanttGridGuiAcceptanceTest {
	private JFrame frame;
	private Gantt gantt;
	private TaskSelectionController selectionController;

	@AfterEach
	void closeWindow() throws Exception {
		if (selectionController != null) {
			SwingUtilities.invokeAndWait(() -> selectionController.close());
			selectionController = null;
		}
		if (frame != null) {
			SwingUtilities.invokeAndWait(() -> {
				frame.dispose();
				frame = null;
			});
		}
		if (gantt != null) {
			gantt.cleanUp();
			gantt = null;
		}
	}

	@Test
	void visibleTaskTableAndGanttRemainLaidOutWhenTheirSharedGridStyleChanges() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture();
		showFixture(fixture);

		Robot robot = new Robot();
		robot.setAutoDelay(40);
		SwingUtilities.invokeAndWait(() -> {
			frame.toFront();
			frame.requestFocus();
		});
		GuiAcceptanceSupport.await(() -> fixture.sheet.isShowing() && fixture.gantt.isShowing(), "task table or Gantt was not visible");
		robot.delay(500);
		assertTrue(hasRenderedGanttNode(fixture.gantt), "Gantt must render at least one task node");
		SwingUtilities.invokeAndWait(() -> {
			String tableName = fixture.sheet.getAccessibleContext().getAccessibleName();
			String rowHeaderName = fixture.sheet.getRowHeader().getAccessibleContext().getAccessibleName();
			assertTrue(tableName != null && !tableName.isBlank(),
				"the visible task table must expose an accessible name");
			assertTrue(rowHeaderName != null && !rowHeaderName.isBlank(),
				"the visible task row header must expose an accessible name");
			assertFalse(tableName.equals(rowHeaderName),
				"the task table and row-header accessible names must be distinct");
		});
		captureVisibleLayout(robot);

		SwingUtilities.invokeAndWait(() -> {
			assertTrue(fixture.sheet.getWidth() > 200 && fixture.sheet.getHeight() > 150, "task table layout collapsed");
			assertTrue(fixture.gantt.getWidth() > 200 && fixture.gantt.getHeight() > 150, "Gantt layout collapsed");

			TaskGanttSyncSupport.applySpreadsheetGridStyle(fixture.sheet, fixture.gantt, false, FlatUiSupport.tableGridColor());
			assertFalse(fixture.sheet.getShowHorizontalLines());
			assertFalse(fixture.sheet.getShowVerticalLines());
			assertFalse(fixture.sheet.getRowHeader().getShowHorizontalLines());
			assertFalse(fixture.gantt.isGridLinesVisible());

			TaskGanttSyncSupport.applySpreadsheetGridStyle(fixture.sheet, fixture.gantt, true, FlatUiSupport.tableGridColor());
			assertTrue(fixture.sheet.getShowHorizontalLines());
			assertTrue(fixture.sheet.getShowVerticalLines());
			assertTrue(fixture.sheet.getRowHeader().getShowHorizontalLines());
			assertTrue(fixture.gantt.isGridLinesVisible());
		});
	}

	private static boolean hasRenderedGanttNode(Gantt chart) throws Exception {
		boolean[] rendered = new boolean[1];
		SwingUtilities.invokeAndWait(() -> {
			for (int y = 0; y < Math.min(chart.getHeight(), chart.getRowHeight() * 20) && !rendered[0]; y += 2) {
				for (int x = 0; x < Math.min(chart.getWidth(), 1200) && !rendered[0]; x += 2) {
					rendered[0] = chart.getUI().getNodeAt(x, y) != null;
				}
			}
		});
		return rendered[0];
	}

	@Test
	void twentyMixedTasksRemainAccessibleAfterMouseScrollbarClick() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture(20);
		showFixture(fixture);

		Robot robot = new Robot();
		robot.setAutoDelay(40);
		SwingUtilities.invokeAndWait(() -> {
			frame.toFront();
			frame.requestFocus();
			fixture.sheet.requestFocusInWindow();
		});
		GuiAcceptanceSupport.await(() -> fixture.sheet.isShowing() && fixture.gantt.isShowing(), "20-task table or Gantt was not visible");
		int initialRows = ((com.microproject.pm.graphic.spreadsheet.SpreadSheetModel) fixture.sheet.getModel()).getRowCount();
		assertTrue(initialRows >= 20, "all 20 task rows must be present before scrolling");
		assertEquals(20, fixture.taskCount, "fixture must contain exactly 20 tasks");
		assertEquals(9, fixture.sequentialDependencyCount, "first ten tasks must form one FS chain");
		assertEquals(10, fixture.independentTaskCount, "last ten tasks must remain independent");
		JScrollPane tableScroll = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, fixture.sheet);
		assertTrue(tableScroll != null, "20-task table must be hosted by a scroll pane");
		assertTrue(tableScroll.getVerticalScrollBar().getMaximum() > tableScroll.getVerticalScrollBar().getVisibleAmount(),
			"20-task table must have a scrollable vertical range");
		Rectangle scrollBounds = new Rectangle();
		SwingUtilities.invokeAndWait(() -> {
			Rectangle bounds = tableScroll.getVerticalScrollBar().getBounds();
			java.awt.Point location = tableScroll.getVerticalScrollBar().getLocationOnScreen();
			scrollBounds.setBounds(location.x, location.y, bounds.width, bounds.height);
		});
		robot.mouseMove(scrollBounds.x + scrollBounds.width / 2, scrollBounds.y + scrollBounds.height - 8);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.delay(300);
		GuiAcceptanceSupport.await(() -> tableScroll.getVerticalScrollBar().getValue() > 0,
			"mouse wheel must scroll the 20-task table");
		assertTrue(tableScroll.getVerticalScrollBar().getValue() > 0, "table must remain scrollable with 20 tasks");
	}

	@Test
	void physicalGanttWheelScrollsByOneOwnerStep() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture(40);
		showFixture(fixture);
		SwingUtilities.invokeAndWait(() -> {
			frame.setSize(1200, 480);
			frame.setLocation(20, 20);
		});

		Robot robot = new Robot();
		robot.setAutoDelay(40);
		SwingUtilities.invokeAndWait(() -> {
			frame.toFront();
			frame.requestFocus();
		});
		GuiAcceptanceSupport.await(() -> fixture.sheet.isShowing() && fixture.gantt.isShowing(),
			"40-task table or Gantt was not visible");
		JScrollPane ganttScroll = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, fixture.gantt);
		JScrollPane tableScroll = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, fixture.sheet);
		assertTrue(ganttScroll != null, "Gantt must be hosted by its scroll pane");
		assertTrue(tableScroll != null, "task table must be hosted by its scroll pane");
		assertTrue(ganttScroll.getVerticalScrollBar().getMaximum() > ganttScroll.getVerticalScrollBar().getVisibleAmount(),
			"40-task Gantt must have a vertical scroll range");
		Synchronizer synchronizer = new Synchronizer();
		SwingUtilities.invokeAndWait(() -> synchronizer.addSynchro(ganttScroll, tableScroll,
			ScrollPaneSynchronizer.HORIZONTAL));

		try {
			int before = ganttScroll.getVerticalScrollBar().getValue();
			int oneOwnerStep = fixture.gantt.getRowHeight() * 5;
			Rectangle visibleGantt = new Rectangle();
			SwingUtilities.invokeAndWait(() -> visibleGantt.setBounds(fixture.gantt.getVisibleRect()));
			Point point = screenCenter(fixture.gantt, visibleGantt);
			robot.mouseMove(point.x, point.y);
			robot.delay(150);
			robot.mouseWheel(1);
			GuiAcceptanceSupport.await(() -> ganttScroll.getVerticalScrollBar().getValue() > before,
				"physical Gantt wheel did not scroll the visible chart (before=" + before + ", after="
					+ ganttScroll.getVerticalScrollBar().getValue() + ", point=" + point + ")");
			assertEquals(before + oneOwnerStep, ganttScroll.getVerticalScrollBar().getValue(),
				"one physical Gantt wheel gesture in a synchronized view must be applied by one vertical-scroll owner");
		} finally {
			SwingUtilities.invokeAndWait(() -> synchronizer.removeSynchro(ganttScroll, tableScroll,
				ScrollPaneSynchronizer.HORIZONTAL));
		}
	}

	@Test
	void physicalTaskRowHeaderDragReordersStableTasksAndSurvivesUndoAndReload() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture(3);
		showFixture(fixture);
		Robot robot = new Robot();
		robot.setAutoDelay(40);
		List<String> originalOrder = taskOrder(fixture.project);
		assertEquals(List.of("Sequential 1", "Sequential 2", "Sequential 3"), originalOrder);
		int sourceRow = taskRow(fixture.sheet, fixture.project, "Sequential 2");
		int targetRow = taskRow(fixture.sheet, fixture.project, "Sequential 1");
		java.awt.Point[] points = new java.awt.Point[2];
		int[] dragState = { 0, -1 };
		SwingUtilities.invokeAndWait(() -> {
			frame.toFront();
			frame.requestFocus();
			fixture.sheet.requestFocusInWindow();
			var rowHeader = fixture.sheet.getRowHeader();
			Rectangle source = rowHeader.getCellRect(sourceRow, 0, true);
			Rectangle target = rowHeader.getCellRect(targetRow, 0, true);
			Point location = rowHeader.getLocationOnScreen();
			points[0] = new Point(location.x + source.x + source.width / 2,
				location.y + source.y + source.height / 2);
			points[1] = new Point(location.x + target.x + target.width / 2, location.y + target.y + 3);
			rowHeader.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
				@Override
				public void mouseDragged(java.awt.event.MouseEvent event) {
					dragState[0]++;
					dragState[1] = rowHeader.rowAtPoint(event.getPoint());
				}
			});
		});
		GuiAcceptanceSupport.await(() -> fixture.sheet.isShowing() && fixture.sheet.getRowHeader().isShowing(),
			"task row header was not visible for the physical move gesture");
		GuiAcceptanceSupport.await(() -> frame.isActive(), "task table frame did not become active for row drag");
		robot.mouseMove(points[0].x, points[0].y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.delay(100);
		GuiAcceptanceSupport.await(() -> {
			boolean[] selected = new boolean[1];
			try {
				SwingUtilities.invokeAndWait(() -> selected[0] = fixture.sheet.isRowFullySelected(sourceRow));
			} catch (Exception exception) {
				throw new AssertionError("could not inspect row selection on the EDT", exception);
			}
			return selected[0];
		},
			"physical row-header press must select the complete source task row before dragging");
		robot.mouseMove((points[0].x + points[1].x) / 2, (points[0].y + points[1].y) / 2);
		robot.delay(100);
		robot.mouseMove(points[1].x, points[1].y);
		robot.delay(150);
		int[] observedDrag = new int[3];
		SwingUtilities.invokeAndWait(() -> {
			observedDrag[0] = dragState[0];
			observedDrag[1] = dragState[1];
			observedDrag[2] = fixture.sheet.getRowHeader().getCursor().getType();
		});
		assertTrue(observedDrag[0] > 0, "physical row-header drag must deliver MOUSE_DRAGGED");
		assertEquals(targetRow, observedDrag[1], "physical drag must reach the intended target task row");
		assertEquals(Cursor.MOVE_CURSOR, observedDrag[2], "valid row-header target must show move cursor before release");
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);

		GuiAcceptanceSupport.await(() -> visibleMoveConfirmation() != null,
			"physical row-header drag did not ask for its documented move confirmation");
		Window confirmation = visibleMoveConfirmation();
		JButton accept = findButton(confirmation, "OK", "確認", "はい");
		assertTrue(accept != null, "move confirmation must expose its affirmative button");
		Rectangle acceptBounds = new Rectangle();
		SwingUtilities.invokeAndWait(() -> {
			Point location = accept.getLocationOnScreen();
			acceptBounds.setBounds(location.x, location.y, accept.getWidth(), accept.getHeight());
		});
		robot.mouseMove(acceptBounds.x + acceptBounds.width / 2, acceptBounds.y + acceptBounds.height / 2);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		GuiAcceptanceSupport.await(() -> taskOrder(fixture.project).equals(
			List.of("Sequential 2", "Sequential 1", "Sequential 3")),
			"accepted physical task row drag did not reorder the selected stable task");
		assertEquals(0, taskRow(fixture.sheet, fixture.project, "Sequential 2"),
			"the task table projection must redraw the moved task at its new row");
		assertEquals(1, taskRow(fixture.sheet, fixture.project, "Sequential 1"),
			"the task table projection must redraw the relocation anchor after the moved task");
		captureVisibleLayout(robot, "task-row-drag-move.png");

		Project reopened = new MpoFileImporter().loadProject(writeProject(fixture.project));
		assertEquals(List.of("Sequential 2", "Sequential 1", "Sequential 3"), taskOrder(reopened),
			"row drag hierarchy mutation must survive native project save/reload");
		SwingUtilities.invokeAndWait(() -> {
			frame.toFront();
			frame.requestFocus();
			fixture.sheet.requestFocusInWindow();
		});
		GuiAcceptanceSupport.await(() -> frame.isActive() && fixture.sheet.isFocusOwner(),
			"task table did not regain focus for the physical Undo/Redo route");
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_Z);
		robot.keyRelease(KeyEvent.VK_Z);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		GuiAcceptanceSupport.await(() -> taskOrder(fixture.project).equals(originalOrder),
			"one Undo must restore the exact pre-drag task order");
		assertEquals(0, taskRow(fixture.sheet, fixture.project, "Sequential 1"),
			"Undo must redraw the original task table order");
		assertEquals(1, taskRow(fixture.sheet, fixture.project, "Sequential 2"),
			"Undo must restore the moved task's original row");
		assertEquals(originalOrder, taskOrder(fixture.project));
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_Y);
		robot.keyRelease(KeyEvent.VK_Y);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		GuiAcceptanceSupport.await(() -> taskOrder(fixture.project).equals(
			List.of("Sequential 2", "Sequential 1", "Sequential 3")),
			"one Redo must reapply the task row relocation");
		assertEquals(0, taskRow(fixture.sheet, fixture.project, "Sequential 2"),
			"Redo must redraw the moved task at its relocated row");
	}

	@Test
	void physicalGanttPopupSelectsTheTaskAndOpensOnceOnThePlatformTrigger() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture(3);
		showFixture(fixture);
		Robot robot = new Robot();
		robot.setAutoDelay(40);
		GraphicNode node = (GraphicNode) fixture.gantt.getModel().getCache().getElementAt(0);
		NormalTask task = (NormalTask) node.getNode().getImpl();
		BarGeometry bar = barGeometry(fixture.gantt, task);
		int x = bar.startX() + Math.max(1, bar.width() / 2);
		int row = fixture.gantt.getModel().getCache().getVisibleNodes().getProjectionIndex().rowForNode(node);
		int y = row * fixture.gantt.getRowHeight() + fixture.gantt.getRowHeight() / 2;
		Point point = screenCenter(fixture.gantt, new Rectangle(x, y, 1, 1));
		long startBefore = task.getStart();
		long durationBefore = task.getRawDuration();
		SwingUtilities.invokeAndWait(() -> {
			frame.toFront();
			frame.requestFocus();
		});
		robot.mouseMove(point.x, point.y);
		robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
		GuiAcceptanceSupport.await(TaskTableGanttGridGuiAcceptanceTest::ganttPopupIsSelected,
			"a physical Gantt right-click did not open its popup on the platform popup trigger");
		SwingUtilities.invokeAndWait(() -> assertEquals(node,
			((GanttUI) fixture.gantt.getUI()).getInteractor().getSelectedObject(),
			"the task under the popup gesture must become the Gantt selection"));
		assertEquals(startBefore, task.getStart(), "opening the Gantt popup must not change task dates");
		assertEquals(durationBefore, task.getRawDuration(), "opening the Gantt popup must not change task duration");
		robot.keyPress(KeyEvent.VK_ESCAPE);
		robot.keyRelease(KeyEvent.VK_ESCAPE);
		GuiAcceptanceSupport.await(() -> !ganttPopupIsSelected(), "Escape did not close the Gantt popup");
	}

	private static boolean ganttPopupIsSelected() {
		MenuElement[][] path = new MenuElement[1][];
		try {
			SwingUtilities.invokeAndWait(() -> path[0] = MenuSelectionManager.defaultManager().getSelectedPath());
		} catch (Exception exception) {
			throw new IllegalStateException("Unable to inspect the active popup path", exception);
		}
		for (MenuElement element : path[0]) {
			if (element instanceof JPopupMenu) return true;
		}
		return false;
	}

	@Test
	void emptyTaskBetweenTasksKeepsGanttRowsSeparated() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixtureWithEmptyMiddle();
		showFixture(fixture);

		Robot robot = new Robot();
		robot.setAutoDelay(40);
		SwingUtilities.invokeAndWait(() -> {
			frame.toFront();
			frame.requestFocus();
		});
		GuiAcceptanceSupport.await(() -> fixture.sheet.isShowing() && fixture.gantt.isShowing(), "task table or Gantt was not visible");
		robot.delay(500);
		captureVisibleLayout(robot, "task-table-gantt-grid-empty-middle.png");
	}

	@Test
	void physicalCellAndColumnHeaderClicksKeepSelectionHighlightCoordinatesAligned() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture(3);
		showFixture(fixture);

		Robot robot = new Robot();
		robot.setAutoDelay(40);
		SwingUtilities.invokeAndWait(() -> {
			selectionController = new TaskSelectionController(fixture.gantt, fixture.sheet);
			frame.toFront();
			frame.requestFocus();
			fixture.sheet.requestFocusInWindow();
		});
		GuiAcceptanceSupport.await(() -> frame.isActive(), "task table selection test frame did not become active");
		GuiAcceptanceSupport.await(() -> fixture.sheet.isShowing() && fixture.sheet.getRowCount() >= 3,
			"task table was not ready for physical selection verification");
		robot.delay(300);

		final int row = 1;
		final int column = Math.min(1, fixture.sheet.getColumnCount() - 1);
		Point cellPoint = screenCenter(fixture.sheet, fixture.sheet.getCellRect(row, column, true));
		robot.mouseMove(cellPoint.x, cellPoint.y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.delay(150);
		SwingUtilities.invokeAndWait(() -> {
			assertEquals(row, fixture.sheet.getSelectedRow(), "physical cell click selected a different row");
			assertEquals(column, fixture.sheet.getSelection().getActiveColumn(),
				"active cell column must match the physically clicked column");
			assertTrue(fixture.sheet.getSelection().isActiveCell(row, column),
				"active cell must match the physically clicked cell");
			assertTrue(fixture.sheet.isRowFullySelected(row), "physical task-cell click must select the full task row");
			assertFalse(fixture.sheet.isHeaderColumnSelectionActive(),
				"a task-cell click must not be rendered as a column-header selection");
			assertHeaderHighlight(fixture.sheet, column);
			assertEquals(Set.of(row), fixture.gantt.getHighlightedRows(),
				"a task-cell selection must project only its task row to the Gantt");
		});
		GraphicNode[] retainedTask = new GraphicNode[1];
		int[] retainedRow = new int[1];
		SwingUtilities.invokeAndWait(() -> {
			retainedTask[0] = fixture.sheet.getSelectedGraphicNodes().getFirst();
			GraphicNode deletedEarlierTask = (GraphicNode) fixture.sheet.getCache().getElementAt(0);
			fixture.sheet.getCache().deleteNodes(List.of(deletedEarlierTask.getNode()));
		});
		SwingUtilities.invokeAndWait(() -> {
			assertTrue(fixture.sheet.getSelectedGraphicNodes().contains(retainedTask[0]),
				"deleting an earlier projected row must preserve the selected task identity");
			assertEquals(0, fixture.sheet.getSelectedRow(),
				"the retained task must be selected at its new table row after deletion");
			retainedRow[0] = fixture.sheet.getSelectedRow();
			assertEquals(Set.of(retainedRow[0]), fixture.gantt.getHighlightedRows(),
				"the Gantt highlight must follow the retained identity to its new row");
		});

		Point headerPoint = screenCenter(fixture.sheet.getTableHeader(), fixture.sheet.getTableHeader().getHeaderRect(column));
		robot.mouseMove(headerPoint.x, headerPoint.y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.delay(150);
		SwingUtilities.invokeAndWait(() -> {
			assertEquals(column, fixture.sheet.getSelectedColumn(),
				"physical column-header click selected a different column");
			assertTrue(fixture.sheet.isColumnFullySelected(column),
				"physical column-header click must select the complete column");
			assertTrue(fixture.sheet.isHeaderColumnSelectionActive(),
				"column-header selection must use the column-header rendering state");
			assertFalse(fixture.sheet.getSelection().isActiveCell(retainedRow[0], column),
				"a full column selection must not retain a misleading active-cell highlight");
			assertEquals(Set.of(retainedRow[0]), fixture.gantt.getHighlightedRows(),
				"a presentation-only column selection must not select every Gantt task row");
		});

		int cellRow = 0;
		Point cellAfterHeader = screenCenter(fixture.sheet, fixture.sheet.getCellRect(cellRow, column, true));
		robot.mouseMove(cellAfterHeader.x, cellAfterHeader.y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.keyPress(java.awt.event.KeyEvent.VK_RIGHT);
		robot.keyRelease(java.awt.event.KeyEvent.VK_RIGHT);
		GuiAcceptanceSupport.await(() -> fixture.sheet.getSelection().isActiveCell(cellRow, column + 1),
			"after a column selection, a cell click and Right Arrow must move to its adjacent visible cell");
		SwingUtilities.invokeAndWait(() -> {
			assertFalse(fixture.sheet.isHeaderColumnSelectionActive(),
				"a task-cell click must end column presentation selection before keyboard navigation");
			assertTrue(fixture.sheet.getSelection().isActiveCell(cellRow, column + 1),
				"Right Arrow must retain the clicked row and advance exactly one visible column");
		});

		SwingUtilities.invokeAndWait(() ->
			fixture.sheet.getCache().deleteNodes(List.of(retainedTask[0].getNode())));
		GuiAcceptanceSupport.await(() -> fixture.sheet.getCache().getVisibleNodes().getProjectionIndex()
			.rowForNode(retainedTask[0]) < 0, "deleted selected task must leave the visible projection");
		SwingUtilities.invokeAndWait(() -> {
			assertEquals(0, fixture.sheet.getSelectedRowCount(),
				"deleting the selected task must clear its table selection");
			assertTrue(fixture.gantt.getHighlightedRows().isEmpty(),
				"a deleted selection key must be pruned from the Gantt and not reappear implicitly");
		});
	}

	@Test
	void workspaceV2RestoresTaskSelectionAndScrollAnchorAfterEarlierTaskDeletion() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture(30);
		showFixture(fixture);

		Robot robot = new Robot();
		robot.setAutoDelay(40);
		JScrollPane tableScroll = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, fixture.sheet);
		assertTrue(tableScroll != null, "task table must be hosted by a scroll pane");
		SwingUtilities.invokeAndWait(() -> {
			selectionController = new TaskSelectionController(fixture.gantt, fixture.sheet);
			frame.setSize(1100, 500);
			frame.toFront();
			frame.requestFocus();
		});
		GuiAcceptanceSupport.await(() -> frame.isActive() && fixture.sheet.isShowing(),
			"workspace acceptance frame or task table was not ready");
		robot.delay(250);

		int selectedRow = 15;
		int anchorRow = 10;
		GraphicNode[] selectedTask = new GraphicNode[1];
		GraphicNode[] scrollAnchor = new GraphicNode[1];
		com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheet.Workspace[] saved =
			new com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheet.Workspace[1];
		SwingUtilities.invokeAndWait(() -> {
			JViewport viewport = tableScroll.getViewport();
			viewport.setViewPosition(new Point(0, fixture.sheet.getCellRect(anchorRow, 0, true).y + 5));
			selectedTask[0] = (GraphicNode) fixture.sheet.getCache().getElementAt(selectedRow);
		});
		Point selectedCell = screenCenter(fixture.sheet, fixture.sheet.getCellRect(selectedRow, 1, true));
		robot.mouseMove(selectedCell.x, selectedCell.y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.delay(120);
		SwingUtilities.invokeAndWait(() -> {
			assertEquals(selectedRow, fixture.sheet.getSelectedRow(), "physical row selection must choose the target task");
			JViewport viewport = tableScroll.getViewport();
			int topRow = fixture.sheet.rowAtPoint(new Point(0, viewport.getViewPosition().y));
			assertEquals(anchorRow, topRow, "the chosen task must remain below the saved scroll anchor");
			scrollAnchor[0] = (GraphicNode) fixture.sheet.getCache().getElementAt(topRow);
			saved[0] = (com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheet.Workspace)
				fixture.sheet.createWorkspace(SavableToWorkspace.VIEW);
			assertTrue(saved[0].isStableTaskSelectionPresent(), "V2 workspace must contain durable selected task keys");
			assertTrue(saved[0].isStableScrollAnchorPresent(), "V2 workspace must contain the visible task scroll anchor");
		});
		WorkspaceSetting serializedWorkspace = roundTripWorkspace(saved[0]);

		SwingUtilities.invokeAndWait(() -> {
			GraphicNode earlierTask = (GraphicNode) fixture.sheet.getCache().getElementAt(0);
			fixture.sheet.getCache().deleteNodes(List.of(earlierTask.getNode()));
		});
		GuiAcceptanceSupport.await(() -> fixture.sheet.getCache().getVisibleNodes().getProjectionIndex()
			.rowForNode(selectedTask[0]) == selectedRow - 1, "deleting the first task must shift the selected task projection");
		SwingUtilities.invokeAndWait(() -> fixture.sheet.restoreWorkspace(serializedWorkspace, SavableToWorkspace.VIEW));
		SwingUtilities.invokeAndWait(() -> {
			assertTrue(fixture.sheet.getSelectedGraphicNodes().contains(selectedTask[0]),
				"V2 restore must retain task identity after an earlier row is removed");
			assertEquals(selectedRow - 1, fixture.sheet.getSelectedRow(),
				"V2 selection must resolve to the task's new table row");
			assertEquals(Set.of(selectedRow - 1), fixture.gantt.getHighlightedRows(),
				"the Gantt highlight must follow the restored stable selection");
			JViewport viewport = tableScroll.getViewport();
			int restoredTopRow = fixture.sheet.rowAtPoint(new Point(0, viewport.getViewPosition().y));
			assertTrue(restoredTopRow >= 0, "restored viewport must begin on a visible row");
			assertSame(scrollAnchor[0].getNode(), ((com.microproject.pm.graphic.spreadsheet.SpreadSheetModel)
				fixture.sheet.getModel()).getNodeForDisplayRow(restoredTopRow),
				"V2 scroll restore must keep the same task at the top anchor after row changes");
		});
		robot.delay(150);
		captureVisibleLayout(robot, "workspace-v2-restored-selection-anchor.png");

		SwingUtilities.invokeAndWait(() -> fixture.sheet.getCache().deleteNodes(List.of(selectedTask[0].getNode())));
		GuiAcceptanceSupport.await(() -> fixture.sheet.getCache().getVisibleNodes().getProjectionIndex()
			.rowForNode(selectedTask[0]) < 0, "deleted V2 selection key must leave the projection");
		SwingUtilities.invokeAndWait(() -> {
			fixture.sheet.restoreWorkspace(serializedWorkspace, SavableToWorkspace.VIEW);
			assertEquals(0, fixture.sheet.getSelectedRowCount(),
				"a valid V2 key that no longer exists must be pruned instead of resolving by its old row number");
			assertTrue(fixture.gantt.getHighlightedRows().isEmpty(),
				"a missing V2 selection key must not highlight a different task");
		});

		com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheet.Workspace legacy =
			new com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheet.Workspace();
		legacy.setWorkspaceVersion(1);
		legacy.setSelectedRows(new int[] { 2 });
		legacy.setViewPosition(new Point(0, fixture.sheet.getCellRect(1, 0, true).y));
		SwingUtilities.invokeAndWait(() -> {
			fixture.sheet.clearSelection();
			fixture.sheet.restoreWorkspace(legacy, SavableToWorkspace.VIEW);
			assertEquals(2, fixture.sheet.getSelectedRow(), "V1 must continue restoring its legacy row selection");
			assertEquals(legacy.getViewPosition(), tableScroll.getViewport().getViewPosition(),
				"V1 must continue restoring its legacy pixel scroll position");
		});

		com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheet.Workspace malformed =
			new com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheet.Workspace();
		malformed.setWorkspaceVersion(2);
		malformed.setStableTaskSelectionPresent(true);
		malformed.setSelectedProjectIds(new long[] { 1L, 1L });
		malformed.setSelectedTaskIds(new long[] { 2L });
		malformed.setSelectedOccurrences(new int[] { 0, 0 });
		malformed.setSelectedRows(new int[] { 3 });
		malformed.setViewPosition(new Point(0, fixture.sheet.getCellRect(1, 0, true).y));
		SwingUtilities.invokeAndWait(() -> {
			fixture.sheet.clearSelection();
			fixture.sheet.restoreWorkspace(malformed, SavableToWorkspace.VIEW);
			assertEquals(3, fixture.sheet.getSelectedRow(), "malformed V2 selection must fall back to its V1 row array");
			assertEquals(malformed.getViewPosition(), tableScroll.getViewport().getViewPosition(),
				"malformed V2 anchor must fall back to the V1 pixel position");
		});
	}

	private static WorkspaceSetting roundTripWorkspace(WorkspaceSetting workspace) throws Exception {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
			output.writeObject(workspace);
		}
		try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
			return (WorkspaceSetting) input.readObject();
		}
	}

	@Test
	void physicalHeaderRightClickHidesTheChosenColumnThroughTheColumnPopup() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture(3);
		showFixture(fixture);
		Robot robot = new Robot();
		robot.setAutoDelay(40);
		SwingUtilities.invokeAndWait(() -> {
			frame.toFront();
			frame.requestFocus();
		});
		GuiAcceptanceSupport.await(() -> fixture.sheet.isShowing() && fixture.sheet.getColumnCount() > 1,
			"task table was not ready for a header context-menu operation");

		int column = 1;
		int beforeFields = fixture.sheet.getFieldArray().size();
		// HeaderMouseListener passes the visible header index plus one because the
		// persisted field array reserves index zero for the hidden task ID field.
		String hiddenFieldId = fixture.sheet.getFieldArray().get(column + 1).getId();
		Point headerPoint = screenCenter(fixture.sheet.getTableHeader(),
			fixture.sheet.getTableHeader().getHeaderRect(column));
		robot.mouseMove(headerPoint.x, headerPoint.y);
		robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
		final SpreadSheetColumnMenu[] popup = new SpreadSheetColumnMenu[1];
		GuiAcceptanceSupport.await(() -> {
			for (MenuElement element : MenuSelectionManager.defaultManager().getSelectedPath()) {
				if (element instanceof SpreadSheetColumnMenu menu) {
					popup[0] = menu;
					return true;
				}
			}
			return false;
		}, "physical header right-click did not open the column popup");
		JMenuItem hide = (JMenuItem) popup[0].getComponent(1);
		Point hideLocation = new Point();
		SwingUtilities.invokeAndWait(() -> hideLocation.setLocation(hide.getLocationOnScreen()));
		robot.mouseMove(hideLocation.x + hide.getWidth() / 2, hideLocation.y + hide.getHeight() / 2);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		GuiAcceptanceSupport.await(() -> fixture.sheet.getFieldArray().size() == beforeFields - 1,
			"Hide Column from the physical popup did not update the persistent field layout");
		SwingUtilities.invokeAndWait(() -> {
			assertEquals(beforeFields - 1, fixture.sheet.getColumnCount() + 1,
				"the visible table must remove exactly the header column chosen from its popup");
			assertFalse(fixture.sheet.getFieldArray().stream().anyMatch(field -> hiddenFieldId.equals(field.getId())),
				"the physically hidden field must leave the visible field array");
		});
		fixture.project.getUndoController().undo();
		assertEquals(beforeFields, fixture.sheet.getFieldArray().size(), "Undo must restore the hidden column");
		fixture.project.getUndoController().redo();
		assertEquals(beforeFields - 1, fixture.sheet.getFieldArray().size(), "Redo must reapply the hidden column");

		ByteArrayOutputStream saved = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(fixture.project, saved),
			"MPO save rejected the physical Hide Column layout");
		Project reloaded = new MpoFileImporter().loadProject(new ByteArrayInputStream(saved.toByteArray()));
		assertFalse(reloaded.getFieldArray().stream().anyMatch(field -> hiddenFieldId.equals(field.getId())),
			"MPO reload restored a column that was physically hidden");
	}

	@Test
	void physicalHeaderInsertColumnUsesColumnDialogAndRoundTripsTheProjectLayout() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture(3);
		showFixture(fixture);
		Robot robot = new Robot();
		robot.setAutoDelay(40);
		SwingUtilities.invokeAndWait(() -> {
			frame.toFront();
			frame.requestFocus();
		});
		GuiAcceptanceSupport.await(() -> fixture.sheet.isShowing() && fixture.sheet.getColumnCount() > 1,
			"task table was not ready for a physical Insert Column operation");

		int beforeFields = fixture.sheet.getFieldArray().size();

		int column = 1;
		Point headerPoint = screenCenter(fixture.sheet.getTableHeader(),
			fixture.sheet.getTableHeader().getHeaderRect(column));
		robot.mouseMove(headerPoint.x, headerPoint.y);
		robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
		final SpreadSheetColumnMenu[] popup = new SpreadSheetColumnMenu[1];
		GuiAcceptanceSupport.await(() -> {
			for (MenuElement element : MenuSelectionManager.defaultManager().getSelectedPath()) {
				if (element instanceof SpreadSheetColumnMenu menu) {
					popup[0] = menu;
					return true;
				}
			}
			return false;
		}, "physical header right-click did not open the column popup for Insert Column");

		JMenuItem insert = (JMenuItem) popup[0].getComponent(0);
		clickComponent(robot, insert);
		Window dialog = awaitWindowWith(JComboBox.class, "ColumnDialog did not open from the physical column popup");
		JComboBox<?> combo = findComponent(dialog, JComboBox.class);
		assertTrue(combo != null && combo.isShowing(), "ColumnDialog must expose a visible field selector");
		clickComponent(robot, combo);
		robot.keyPress(KeyEvent.VK_HOME);
		robot.keyRelease(KeyEvent.VK_HOME);
		robot.keyPress(KeyEvent.VK_ENTER);
		robot.keyRelease(KeyEvent.VK_ENTER);
		Field[] selected = new Field[1];
		SwingUtilities.invokeAndWait(() -> selected[0] = (Field) combo.getSelectedItem());
		assertTrue(selected[0] != null, "ColumnDialog physical selection must produce a field");
		JButton ok = findButton(dialog, "OK", "確認", "適用");
		assertTrue(ok != null && ok.isShowing() && ok.isEnabled(), "ColumnDialog must expose an enabled OK button");
		clickComponent(robot, ok);

		GuiAcceptanceSupport.await(() -> fixture.sheet.getFieldArray().size() == beforeFields + 1,
			"physical ColumnDialog confirmation did not update the task field layout");
		SwingUtilities.invokeAndWait(() -> assertTrue(fixture.sheet.getFieldArray().stream()
			.anyMatch(field -> selected[0].getId().equals(field.getId())),
			"the field selected through ColumnDialog was not inserted into the visible layout"));

		ByteArrayOutputStream saved = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(fixture.project, saved),
			"MPO save rejected the project after physical Insert Column");
		Project reloaded = new MpoFileImporter().loadProject(new ByteArrayInputStream(saved.toByteArray()));
		assertTrue(reloaded.getFieldArray().stream().anyMatch(field -> selected[0].getId().equals(field.getId())),
			"MPO reload lost the column layout created through the physical popup");
	}

	@Test
	void physicalColumnAutoFilterUsesDisplayedValuesAndUpdatesTheVisibleRows() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture(3);
		showFixture(fixture);
		Robot robot = new Robot();
		robot.setAutoDelay(40);
		SwingUtilities.invokeAndWait(() -> {
			frame.toFront();
			frame.requestFocus();
		});
		GuiAcceptanceSupport.await(() -> fixture.sheet.isShowing() && fixture.sheet.getRowCount() >= 3,
			"task table was not ready for a physical AutoFilter operation");
		int beforeRows = fixture.sheet.getRowCount();

		int nameFieldIndex = -1;
		for (int index = 0; index < fixture.sheet.getFieldArray().size(); index++) {
			if ("Field.name".equals(fixture.sheet.getFieldArray().get(index).getId())) {
				nameFieldIndex = index;
				break;
			}
		}
		assertTrue(nameFieldIndex > 0, "fixture must expose the task Name field after its hidden ID field");
		int nameColumn = nameFieldIndex - 1;
		Point headerPoint = screenCenter(fixture.sheet.getTableHeader(), fixture.sheet.getTableHeader().getHeaderRect(nameColumn));
		robot.mouseMove(headerPoint.x, headerPoint.y);
		robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);

		final SpreadSheetColumnMenu[] popup = new SpreadSheetColumnMenu[1];
		GuiAcceptanceSupport.await(() -> {
			for (MenuElement element : MenuSelectionManager.defaultManager().getSelectedPath()) {
				if (element instanceof SpreadSheetColumnMenu menu) {
					popup[0] = menu;
					return true;
				}
			}
			return false;
		}, "physical header right-click did not open the column popup for AutoFilter");

		// AutoFilter is the final command for both localized and English menus.
		Component lastMenuComponent = popup[0].getComponent(popup[0].getComponentCount() - 1);
		assertTrue(lastMenuComponent instanceof JMenuItem, "column popup must expose its final AutoFilter command");
		JMenuItem autoFilter = (JMenuItem) lastMenuComponent;
		clickComponent(robot, autoFilter);
		Window dialog = awaitWindowWith(JCheckBox.class, "AutoFilter dialog did not open from the physical column popup");
		JCheckBox value = findCheckBox(dialog, "Sequential 1");
		assertTrue(value != null && value.isSelected(), "AutoFilter dialog must expose the selected Sequential 1 task value");
		clickComponent(robot, value);
		JButton apply = findButton(dialog, "Apply", "適用");
		assertTrue(apply != null && apply.isShowing() && apply.isEnabled(), "AutoFilter dialog must expose an enabled Apply button");
		clickComponent(robot, apply);

		GuiAcceptanceSupport.await(() -> fixture.sheet.getRowCount() == beforeRows - 1,
			"applying a physical AutoFilter selection did not remove exactly one visible task row");
		assertEquals(beforeRows - 1, fixture.gantt.getModel().getCache().getSize(),
			"AutoFilter must apply through the shared task-table/Gantt visible cache");
	}

	@Test
	void physicalCustomColumnRenameUsesTheHeaderPopupDialog() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture(3);
		Field customField = fixture.sheet.getAvailableFields().stream().filter(Field::isCustom).findFirst().orElseThrow(
			() -> new AssertionError("task field catalog must expose a custom field for Rename"));
		String originalAlias = customField.getAlias();
		SwingUtilities.invokeAndWait(() -> fixture.sheet.setFieldArray(
			((SpreadSheetFieldArray) fixture.sheet.getFieldArray()).insertField(fixture.sheet.getFieldArray().size(), customField)));
		showFixture(fixture);
		Robot robot = new Robot();
		robot.setAutoDelay(40);
		SwingUtilities.invokeAndWait(() -> {
			frame.toFront();
			frame.requestFocus();
		});
		GuiAcceptanceSupport.await(() -> fixture.sheet.isShowing() && fixture.sheet.getColumnCount() > 1,
			"task table was not ready for a physical custom-column Rename operation");

		int fieldIndex = fixture.sheet.getFieldArray().indexOf(customField);
		assertTrue(fieldIndex > 0, "custom field must be visible after the hidden ID field");
		int customColumn = fieldIndex - 1;
		SwingUtilities.invokeAndWait(() -> fixture.sheet.scrollRectToVisible(fixture.sheet.getCellRect(0, customColumn, true)));
		Point headerPoint = screenCenter(fixture.sheet.getTableHeader(), fixture.sheet.getTableHeader().getHeaderRect(customColumn));
		robot.mouseMove(headerPoint.x, headerPoint.y);
		robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);

		final SpreadSheetColumnMenu[] popup = new SpreadSheetColumnMenu[1];
		GuiAcceptanceSupport.await(() -> {
			for (MenuElement element : MenuSelectionManager.defaultManager().getSelectedPath()) {
				if (element instanceof SpreadSheetColumnMenu menu) {
					popup[0] = menu;
					return true;
				}
			}
			return false;
		}, "physical header right-click did not open the custom-column popup");

		JMenuItem rename = (JMenuItem) popup[0].getComponent(2);
		clickComponent(robot, rename);
		Window dialog = awaitWindowWith(JTextField.class, "Rename dialog did not open from the physical custom-column popup");
		JTextField input = findComponent(dialog, JTextField.class);
		assertTrue(input != null && input.isShowing(), "Rename dialog must expose its visible alias input");
		clickComponent(robot, input);
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_A);
		robot.keyRelease(KeyEvent.VK_A);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		for (char character : "gui alias".toCharArray()) {
			int keyCode = KeyEvent.getExtendedKeyCodeForChar(character);
			robot.keyPress(keyCode);
			robot.keyRelease(keyCode);
		}
		JButton ok = findButton(dialog, "OK", "確認");
		assertTrue(ok != null && ok.isShowing() && ok.isEnabled(), "Rename dialog must expose an enabled OK button");
		clickComponent(robot, ok);
		GuiAcceptanceSupport.await(() -> "gui alias".equals(customField.getName()),
			"physical Rename did not update the custom column alias");
		assertEquals("gui alias", fixture.sheet.getColumnName(customColumn),
			"spreadsheet header did not redraw the alias entered through Rename");
		// Fields are dictionary singletons. Restore the fixture-independent alias
		// so this physical route does not leak state into the remaining GUI suite.
		SwingUtilities.invokeAndWait(() -> customField.setAlias(originalAlias));
	}

	@Test
	void physicalCornerPresetChangesTaskColumnsAndRoundTripsTheProjectLayout() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture(3);
		showFixture(fixture);
		Robot robot = new Robot();
		robot.setAutoDelay(40);
		SwingUtilities.invokeAndWait(() -> {
			frame.toFront();
			frame.requestFocus();
		});
		GuiAcceptanceSupport.await(() -> fixture.sheet.isShowing() && fixture.sheet.getRowCount() >= 3,
			"task table was not ready for a physical column preset operation");

		JScrollPane tableScroll = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, fixture.sheet);
		assertTrue(tableScroll != null, "task table must be hosted by a scroll pane");
		Component corner = tableScroll.getCorner(JScrollPane.UPPER_LEFT_CORNER);
		assertTrue(corner != null && corner.isShowing(), "task table corner must expose the column preset route");
		String beforeName = fixture.sheet.getFieldArray().toString();
		Point cornerPoint = screenCenter((JComponent) corner, new Rectangle(0, 0, corner.getWidth(), corner.getHeight()));
		robot.mouseMove(cornerPoint.x, cornerPoint.y);
		robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);

		final SpreadSheetColumnsPopupMenu[] popup = new SpreadSheetColumnsPopupMenu[1];
		GuiAcceptanceSupport.await(() -> {
			for (MenuElement element : MenuSelectionManager.defaultManager().getSelectedPath()) {
				if (element instanceof SpreadSheetColumnsPopupMenu menu) {
					popup[0] = menu;
					return true;
				}
			}
			return false;
		}, "physical corner right-click did not open the column preset popup");

		JRadioButtonMenuItem preset = null;
		for (int index = 0; index < popup[0].getComponentCount(); index++) {
			Component component = popup[0].getComponent(index);
			if (component instanceof JRadioButtonMenuItem item && !item.isSelected()) {
				preset = item;
				break;
			}
		}
		assertTrue(preset != null, "column preset popup must expose an alternative preset");
		String selectedPreset = preset.getText();
		clickComponent(robot, preset);
		GuiAcceptanceSupport.await(() -> !beforeName.equals(fixture.sheet.getFieldArray().toString()),
			"physical column preset selection did not change the task field layout");
		assertTrue(fixture.sheet.getFieldArray().size() > 1, "selected column preset must retain visible task fields");

		ByteArrayOutputStream saved = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(fixture.project, saved),
			"MPO save rejected the physical column preset layout");
		Project reloaded = new MpoFileImporter().loadProject(new ByteArrayInputStream(saved.toByteArray()));
		assertEquals(fixture.sheet.getFieldArray().size(), reloaded.getFieldArray().size(),
			"MPO reload changed the field count selected by the physical preset");
		List<String> selectedIds = fixture.sheet.getFieldArray().stream().map(Field::getId).toList();
		List<String> reloadedIds = reloaded.getFieldArray().stream().map(Field::getId).toList();
		assertEquals(selectedIds, reloadedIds, "MPO reload lost the field layout selected by preset " + selectedPreset);
	}

	private static void clickComponent(Robot robot, Component component) throws Exception {
		Point location = new Point();
		SwingUtilities.invokeAndWait(() -> location.setLocation(component.getLocationOnScreen()));
		robot.mouseMove(location.x + component.getWidth() / 2, location.y + component.getHeight() / 2);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
	}

	private static Window awaitWindowWith(Class<? extends Component> type, String message) throws Exception {
		final Window[] result = new Window[1];
		GuiAcceptanceSupport.await(() -> {
			for (Window window : Window.getWindows()) {
				if (window.isShowing() && findComponent(window, type) != null) {
					result[0] = window;
					return true;
				}
			}
			return false;
		}, message);
		return result[0];
	}

	private static <T extends Component> T findComponent(Component root, Class<T> type) {
		if (type.isInstance(root)) return type.cast(root);
		if (root instanceof Container container) {
			for (Component child : container.getComponents()) {
				T found = findComponent(child, type);
				if (found != null) return found;
			}
		}
		return null;
	}

	private static JCheckBox findCheckBox(Component root, String text) {
		if (root instanceof JCheckBox checkBox && text.equals(checkBox.getText())) return checkBox;
		if (root instanceof Container container) {
			for (Component child : container.getComponents()) {
				JCheckBox found = findCheckBox(child, text);
				if (found != null) return found;
			}
		}
		return null;
	}

	private static JButton findButton(Window window, String... labels) {
		for (String label : labels) {
			JButton button = findButton(window, label);
			if (button != null) return button;
		}
		return null;
	}

	private static JButton findButton(Component root, String label) {
		if (root instanceof JButton button && label.equals(button.getText())) return button;
		if (root instanceof Container container) {
			for (Component child : container.getComponents()) {
				JButton found = findButton(child, label);
				if (found != null) return found;
			}
		}
		return null;
	}

	@Test
	void calendarWhitespaceDragExtendsVisibleTaskSelectionWithoutEditingBars() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture(3);
		showFixture(fixture);
		Robot robot = new Robot();
		robot.setAutoDelay(40);
		SwingUtilities.invokeAndWait(() -> {
			selectionController = new TaskSelectionController(fixture.gantt, fixture.sheet);
			frame.toFront();
			frame.requestFocus();
		});
		GuiAcceptanceSupport.await(() -> frame.isActive(), "calendar drag test frame did not become active");
		GuiAcceptanceSupport.await(() -> fixture.sheet.isShowing() && fixture.gantt.isShowing(),
			"task table or Gantt was not visible for calendar whitespace drag");

		Point chartLocation = new Point();
		Rectangle visibleChart = new Rectangle();
		int rowHeight = fixture.gantt.getRowHeight();
		SwingUtilities.invokeAndWait(() -> {
			chartLocation.setLocation(fixture.gantt.getLocationOnScreen());
			visibleChart.setBounds(fixture.gantt.getVisibleRect());
		});
		int x = chartLocation.x + visibleChart.x + visibleChart.width - 12;
		int startY = chartLocation.y + rowHeight / 2;
		int endY = chartLocation.y + rowHeight * 2 + rowHeight / 2;
		robot.mouseMove(x, startY);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseMove(x, endY);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		GuiAcceptanceSupport.await(() -> fixture.sheet.getSelectedRowCount() == 3,
			"dragging over calendar whitespace must extend the selected visible task range");
		SwingUtilities.invokeAndWait(() -> {
			assertEquals(Set.of(0, 1, 2), fixture.gantt.getHighlightedRows(),
				"the Gantt highlight must match the whitespace-drag task range");
			assertEquals(fixture.sheet.getColumnCount(), fixture.sheet.getSelectedColumnCount(),
				"whitespace selection must remain a task-row selection rather than a cell range");
		});
	}

	@Test
	void zeroPercentProgressLinePointSelectsTaskWithoutPanningTimescale() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture(1);
		showFixture(fixture);
		Robot robot = new Robot();
		robot.setAutoDelay(40);
		AtomicReference<Gantt.BarClick> click = new AtomicReference<>();
		GraphicNode node = (GraphicNode) fixture.gantt.getModel().getCache().getElementAt(0);
		com.microproject.pm.task.Task task = (com.microproject.pm.task.Task) node.getNode().getImpl();
		SwingUtilities.invokeAndWait(() -> {
			task.setPercentComplete(0.0d);
			fixture.gantt.setProgressLineEnabled(true);
			fixture.gantt.setBarSelectionListener(click::set);
			frame.toFront();
			frame.requestFocus();
		});
		GuiAcceptanceSupport.await(() -> fixture.gantt.isShowing(), "Gantt was not visible for progress-line hit testing");
		robot.delay(250);

		GanttUI ui = (GanttUI) fixture.gantt.getUI();
		int x = (int) Math.round(fixture.gantt.getCoord().toX(
			GanttProgress.progressLineDate(task, task.getProject().getStatusDate())));
		int row = fixture.gantt.getModel().getCache().getVisibleNodes().getProjectionIndex().rowForNode(node);
		int y = (int) Math.round(ui.getBarY(row) + node.getGanttShapeOffset()
			+ node.getGanttShapeHeight() / 2.0d);
		JScrollPane pane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, fixture.gantt);
		assertTrue(pane != null, "Gantt must be hosted by a scroll pane");
		Point before = pane.getViewport().getViewPosition();
		Point location = new Point();
		SwingUtilities.invokeAndWait(() -> location.setLocation(fixture.gantt.getLocationOnScreen()));
		robot.mouseMove(location.x + x, location.y + y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		GuiAcceptanceSupport.await(() -> click.get() != null,
			"clicking the zero-percent progress point must select a task instead of starting a pan");
		assertEquals(node, click.get().node(), "progress-line click must select the rendered task node");
		Point after = pane.getViewport().getViewPosition();
		assertEquals(before, after, "progress-line click must not move the timescale viewport");
	}

	@Test
	void calendarRowWhitespaceClickSelectsTheMatchingTaskTableRow() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture(3);
		showFixture(fixture);

		Robot robot = new Robot();
		robot.setAutoDelay(40);
		SwingUtilities.invokeAndWait(() -> {
			selectionController = new TaskSelectionController(fixture.gantt, fixture.sheet);
			frame.toFront();
			frame.requestFocus();
		});
		GuiAcceptanceSupport.await(() -> frame.isActive(), "calendar row test frame did not become active");
		GuiAcceptanceSupport.await(() -> fixture.sheet.isShowing() && fixture.gantt.isShowing(),
			"task table or Gantt was not visible for calendar-row selection");

		int row = 1;
		Point chartLocation = new Point();
		Rectangle visibleChart = new Rectangle();
		int rowHeight = fixture.gantt.getRowHeight();
		SwingUtilities.invokeAndWait(() -> {
			chartLocation.setLocation(fixture.gantt.getLocationOnScreen());
			visibleChart.setBounds(fixture.gantt.getVisibleRect());
		});
		// The far right edge is calendar whitespace for this one-day fixture;
		// selecting there must not require a task-bar hit.
		int x = chartLocation.x + visibleChart.x + visibleChart.width - 12;
		int y = chartLocation.y + row * rowHeight + rowHeight - 3;
		SwingUtilities.invokeAndWait(() -> assertTrue(((GanttUI) fixture.gantt.getUI()).getTaskRowAt(
			row * rowHeight + rowHeight - 3) != null, "test click must target a visible task row"));
		robot.mouseMove(x, y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		GuiAcceptanceSupport.await(() -> fixture.sheet.getSelectedRow() == row,
			"calendar-row click did not select the corresponding task-table row");
		SwingUtilities.invokeAndWait(() -> {
			assertEquals(row, fixture.sheet.getSelectedRow(), "calendar row must select its table row");
			assertTrue(fixture.sheet.isRowFullySelected(row), "calendar row selection must highlight the full table row");
			assertTrue(fixture.gantt.getHighlightedRows().contains(row), "calendar row selection must retain the Gantt highlight");
		});

		int secondRow = 2;
		int secondY = chartLocation.y + secondRow * rowHeight + rowHeight - 3;
		robot.keyPress(java.awt.event.KeyEvent.VK_CONTROL);
		robot.mouseMove(x, secondY);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.keyRelease(java.awt.event.KeyEvent.VK_CONTROL);
		GuiAcceptanceSupport.await(() -> fixture.sheet.getSelectedRowCount() == 2,
			"Ctrl+calendar-row click did not retain a noncontiguous task selection");
		SwingUtilities.invokeAndWait(() -> {
			assertTrue(fixture.sheet.getSelectionModel().isSelectedIndex(row), "first calendar row must remain selected");
			assertTrue(fixture.sheet.getSelectionModel().isSelectedIndex(secondRow), "Ctrl click must add the second calendar row");
			assertEquals(fixture.sheet.getColumnCount(), fixture.sheet.getSelectedColumnCount(),
				"every selected Gantt row must highlight all task-table columns");
		});
	}

	@Test
	void taskCellCtrlClickAndVerticalDragRetainEverySelectedRow() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture(3);
		showFixture(fixture);

		Robot robot = new Robot();
		robot.setAutoDelay(50);
		SwingUtilities.invokeAndWait(() -> {
			selectionController = new TaskSelectionController(fixture.gantt, fixture.sheet);
			frame.toFront();
			frame.requestFocus();
			fixture.sheet.requestFocusInWindow();
		});
		int nameColumn = findColumn(fixture.sheet, "Field.name");
		Point firstCell = screenCenter(fixture.sheet, fixture.sheet.getCellRect(0, nameColumn, true));
		Point thirdCell = screenCenter(fixture.sheet, fixture.sheet.getCellRect(2, nameColumn, true));
		robot.mouseMove(firstCell.x, firstCell.y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.mouseMove(thirdCell.x, thirdCell.y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		GuiAcceptanceSupport.await(() -> fixture.sheet.getSelectedRowCount() == 2,
			"Ctrl+click in the task table did not retain two noncontiguous rows");
		SwingUtilities.invokeAndWait(() -> {
			assertTrue(fixture.sheet.getSelectionModel().isSelectedIndex(0), "the original task-table row must stay selected");
			assertTrue(fixture.sheet.getSelectionModel().isSelectedIndex(2), "Ctrl+click must select the second task-table row");
			assertEquals(fixture.sheet.getColumnCount(), fixture.sheet.getSelectedColumnCount(),
				"Ctrl+click must highlight every selected task row across all columns");
			assertEquals(Set.of(0, 2), fixture.gantt.getHighlightedRows(),
				"Gantt highlighting must match noncontiguous task-table selection");
		});

		Point secondCell = screenCenter(fixture.sheet, fixture.sheet.getCellRect(1, nameColumn, true));
		robot.mouseMove(firstCell.x, firstCell.y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseMove(firstCell.x, firstCell.y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.delay(100);
		robot.mouseMove(secondCell.x, secondCell.y);
		robot.delay(100);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		GuiAcceptanceSupport.await(() -> fixture.sheet.getSelectedRowCount() == 2,
			"task-table drag selection collapsed after mouse release");
		SwingUtilities.invokeAndWait(() -> {
			assertTrue(fixture.sheet.getSelectionModel().isSelectedIndex(0), "drag range must retain its first task row");
			assertTrue(fixture.sheet.getSelectionModel().isSelectedIndex(1), "drag range must retain its last task row");
			assertEquals(Set.of(0, 1), fixture.gantt.getHighlightedRows(),
				"Gantt highlighting must retain every row selected by a task-table drag");
		});
	}

	private static void assertHeaderHighlight(SpreadSheet sheet, int activeColumn) {
		for (int column = 0; column < sheet.getColumnCount(); column++) {
			TableCellRenderer renderer = sheet.getColumnModel().getColumn(column).getHeaderRenderer();
			Component component = renderer.getTableCellRendererComponent(sheet, null, false, false, -1, column);
			assertTrue(component instanceof javax.swing.JComponent, "header renderer must return a Swing component");
			assertTrue(component.getBackground().equals(column == activeColumn
					? FlatUiSupport.spreadsheetHeaderSelectedBackground()
					: FlatUiSupport.spreadsheetHeaderBackground()),
				"header renderer must highlight only the clicked view column");
			if (column == activeColumn)
				assertTrue(((javax.swing.JComponent) component).getBorder() instanceof LineBorder,
					"active header must retain its visible focus border");
		}
	}

	private static Point screenCenter(javax.swing.JComponent component, Rectangle bounds) throws Exception {
		Point location = new Point();
		SwingUtilities.invokeAndWait(() -> location.setLocation(component.getLocationOnScreen()));
		return new Point(location.x + bounds.x + bounds.width / 2, location.y + bounds.y + bounds.height / 2);
	}

	private static java.util.List<String> taskOrder(Project project) {
		java.util.List<String> names = new ArrayList<>();
		java.util.Iterator<com.microproject.pm.task.Task> iterator = project.getTaskOutlineIterator();
		while (iterator.hasNext())
			names.add(iterator.next().getName());
		return names;
	}

	private static com.microproject.grouping.core.Node findTaskNode(Project project, String name) {
		java.util.Iterator<com.microproject.pm.task.Task> iterator = project.getTaskOutlineIterator();
		while (iterator.hasNext()) {
			var task = iterator.next();
			if (name.equals(task.getName()))
				return project.getTaskModel().search(task);
		}
		throw new AssertionError("task not found in fixture: " + name);
	}

	private static int taskRow(SpreadSheet sheet, Project project, String name) {
		GraphicNode graphicNode = (GraphicNode) sheet.getCache().getGraphicNode(findTaskNode(project, name));
		int row = sheet.getCache().getRowAt(graphicNode);
		if (row < 0)
			throw new AssertionError("task row not visible in fixture: " + name);
		return row;
	}

	private static Window visibleMoveConfirmation() {
		for (Window window : Window.getWindows()) {
			if (window instanceof java.awt.Dialog && window.isShowing()
					&& findButton(window, "OK", "確認", "はい") != null)
				return window;
		}
		return null;
	}

	private static ByteArrayInputStream writeProject(Project project) throws Exception {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(project, output), "task order must serialize successfully");
		return new ByteArrayInputStream(output.toByteArray());
	}

	private void captureVisibleLayout(Robot robot) throws Exception {
		captureVisibleLayout(robot, "task-table-gantt-grid.png");
	}

	private void captureVisibleLayout(Robot robot, String fileName) throws Exception {
		Rectangle[] bounds = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> bounds[0] = new Rectangle(frame.getRootPane().getLocationOnScreen(), frame.getRootPane().getSize()));
		BufferedImage screenshot = robot.createScreenCapture(bounds[0]);
		Path directory = Path.of(System.getProperty("microproject.gui.artifacts.dir", "build/guiTest-artifacts"));
		Files.createDirectories(directory);
		ImageIO.write(screenshot, "png", directory.resolve(fileName).toFile());
		assertTrue(screenshot.getWidth() > 400 && screenshot.getHeight() > 300, "captured layout is unexpectedly small");
	}

	private void showFixture(Fixture fixture) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			frame = new JFrame("Task table and Gantt GUI acceptance");
			JScrollPane ganttPane = new JScrollPane(fixture.gantt);
			// The production GanttView puts a time-scale header above the chart.
			// Keep this lightweight fixture's row origin aligned with the table.
			int headerHeight = fixture.sheet.getTableHeader().getPreferredSize().height;
			JPanel header = new JPanel();
			header.setPreferredSize(new Dimension(980, headerHeight));
			ganttPane.setColumnHeaderView(header);
			JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				new JScrollPane(fixture.sheet), ganttPane);
			splitPane.setResizeWeight(0.45);
			frame.add(splitPane);
			frame.setPreferredSize(new Dimension(1100, 520));
			frame.pack();
			frame.setLocationByPlatform(true);
			frame.setAlwaysOnTop(true);
			frame.setVisible(true);
			gantt.updateSize();
		});
	}

	private Fixture createFixture() throws Exception {
		return createFixture(1);
	}

	private Fixture createFixtureWithEmptyMiddle() throws Exception {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("gui-task-gantt-empty-middle-test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		List<NormalTask> tasks = new ArrayList<>();
		for (int index = 1; index <= 3; index++) {
			NormalTask task = project.createScriptedTask();
			task.setName(index == 2 ? "" : "Task " + (index == 1 ? "A" : "B"));
			task.getCurrentSchedule().setStart(project.getStart());
			task.setDuration(index == 2 ? 0L : (index == 1
			? CalendarOption.getInstance().getMillisPerDay() / 2
			: CalendarOption.getInstance().getMillisPerDay() * 3));
			tasks.add(task);
		}
		project.recalculate();
		Fixture[] fixture = new Fixture[1];
		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet sheet = new SpreadSheet();
			sheet.setSpreadSheetCategory(SpreadSheetCategories.taskSpreadsheetCategory);
			NodeModelCache cache = NodeModelCacheFactory.getInstance().createFilteredCache(
				NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel()), "gui-task-gantt-empty-middle-test", null);
			SpreadSheetUtils.setFieldsAndContext(sheet, cache, SpreadSheetCategories.taskSpreadsheetCategory, "Spreadsheet.Task.entry", true);
			gantt = new Gantt(project, "Gantt");
			gantt.setCache(cache);
			gantt.setCoord(new CoordinatesConverter(project));
			gantt.setBarStyles((BarStyles) Dictionary.get(BarStyles.category, "standard"));
			gantt.updateSize();
			fixture[0] = new Fixture(sheet, gantt, project, tasks.size(), 0, 0);
		});
		return fixture[0];
	}

	private Fixture createFixture(int taskCount) throws Exception {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("gui-task-gantt-grid-test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		List<NormalTask> tasks = new ArrayList<>();
		for (int index = 1; index <= taskCount; index++) {
			NormalTask task = project.createScriptedTask();
			task.setName(index <= 10 ? "Sequential " + index : "Independent " + index);
			task.getCurrentSchedule().setStart(project.getStart());
			task.setDuration(fixtureDuration(index, taskCount));
			tasks.add(task);
			if (index > 1 && index <= 10) {
				DependencyService.getInstance().newDependency(tasks.get(index - 2), task, DependencyType.FS, 0L, project);
			}
		}
		project.recalculate();
		int sequentialDependencyCount = (int) tasks.subList(1, Math.min(10, tasks.size())).stream()
			.filter(task -> task.getPredecessorList().size() == 1).count();
		int independentTaskCount = (int) tasks.subList(Math.min(10, tasks.size()), tasks.size()).stream()
			.filter(task -> task.getPredecessorList().isEmpty()).count();
		Fixture[] fixture = new Fixture[1];
		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet sheet = new SpreadSheet();
			sheet.setSpreadSheetCategory(SpreadSheetCategories.taskSpreadsheetCategory);
			NodeModelCache cache = NodeModelCacheFactory.getInstance().createFilteredCache(
				NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel()), "gui-task-gantt-grid-test", null);
			SpreadSheetUtils.setFieldsAndContext(sheet, cache, SpreadSheetCategories.taskSpreadsheetCategory, "Spreadsheet.Task.entry", true);
			gantt = new Gantt(project, "Gantt");
			gantt.setCache(cache);
			gantt.setCoord(new CoordinatesConverter(project));
			gantt.setBarStyles((BarStyles) Dictionary.get(BarStyles.category, "standard"));
			gantt.updateSize();
			fixture[0] = new Fixture(sheet, gantt, project, tasks.size(), sequentialDependencyCount, independentTaskCount);
		});
		return fixture[0];
	}

	@Test
	void physicalTaskTableDurationEditDoesNotPanGanttViewport() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture(1);
		showFixture(fixture);
		Robot robot = new Robot();
		robot.setAutoDelay(35);
		GraphicNode node = (GraphicNode) fixture.gantt.getModel().getCache().getElementAt(0);
		NormalTask task = (NormalTask) node.getNode().getImpl();
		int durationColumn = findColumn(fixture.sheet, "Field.duration");
		SwingUtilities.invokeAndWait(() -> {
			frame.toFront();
			frame.requestFocus();
			fixture.sheet.changeSelection(fixture.sheet.getCache().getRowAt(node), durationColumn, false, false);
		});
		GuiAcceptanceSupport.await(() -> fixture.sheet.isShowing() && fixture.gantt.isShowing(),
			"task table and Gantt must be visible before duration input");
		JScrollPane pane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, fixture.gantt);
		assertTrue(pane != null, "Gantt must be hosted by a scroll pane");
		Point viewportBefore = pane.getViewport().getViewPosition();
		BarGeometry before = barGeometry(fixture.gantt, task);
		editCellPhysically(robot, fixture.sheet, fixture.sheet.getCache().getRowAt(node), durationColumn, "10");
		assertEquals(10L * CalendarOption.getInstance().getMillisPerDay(), task.getRawDuration(),
			"the task-table physical duration edit must commit the complete value");
		GuiAcceptanceSupport.await(() -> task.getRawDuration() == 10L * CalendarOption.getInstance().getMillisPerDay(),
			"duration edit did not reach the task model");
		SwingUtilities.invokeAndWait(fixture.gantt::updateSize);
		Point viewportAfter = pane.getViewport().getViewPosition();
		BarGeometry after = barGeometry(fixture.gantt, task);
		assertEquals(viewportBefore, viewportAfter,
			"a task-table duration commit must not move the Gantt timescale viewport");
		assertEquals(before.startX(), after.startX(),
			"the duration edit must preserve the task start bar position");
		assertTrue(after.width() > before.width(),
			"the committed duration must change only the planned bar width, not the viewport");
	}

	@Test
	void physicalTaskTableDateEditRepositionsBarWithoutPanningGanttViewport() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture(1);
		showFixture(fixture);
		Robot robot = new Robot();
		robot.setAutoDelay(35);
		GraphicNode node = (GraphicNode) fixture.gantt.getModel().getCache().getElementAt(0);
		NormalTask task = (NormalTask) node.getNode().getImpl();
		int startColumn = findColumn(fixture.sheet, "Field.start");
		JScrollPane pane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, fixture.gantt);
		assertTrue(pane != null, "Gantt must be hosted by a scroll pane");
		GuiAcceptanceSupport.await(() -> fixture.sheet.isShowing() && fixture.gantt.isShowing(),
			"task table and Gantt must be visible before date input");
		Point viewportBefore = pane.getViewport().getViewPosition();
		long originalStart = task.getStart();
		Calendar expectedStart = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
		expectedStart.setTimeInMillis(originalStart);
		expectedStart.add(Calendar.DAY_OF_MONTH, 3);
		// The standard project calendar does not schedule task starts on weekends.
		// Keep the physical edit target on a working weekday so this test verifies
		// date entry and viewport behavior instead of weekend scheduling policy.
		while (expectedStart.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY
				|| expectedStart.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
			expectedStart.add(Calendar.DAY_OF_MONTH, 1);
		}
		String inputDate = String.format(java.util.Locale.ROOT, "%d/%d/%d",
			expectedStart.get(Calendar.YEAR), expectedStart.get(Calendar.MONTH) + 1,
			expectedStart.get(Calendar.DAY_OF_MONTH));
		editCellPhysically(robot, fixture.sheet, fixture.sheet.getCache().getRowAt(node), startColumn, inputDate);
		Calendar committed = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
		committed.setTimeInMillis(task.getStart());
		assertEquals(expectedStart.get(Calendar.YEAR), committed.get(Calendar.YEAR), "date input must commit the year");
		assertEquals(expectedStart.get(Calendar.MONTH), committed.get(Calendar.MONTH), "date input must commit the month");
		assertEquals(expectedStart.get(Calendar.DAY_OF_MONTH), committed.get(Calendar.DAY_OF_MONTH),
			"date input must commit the day");
		assertTrue(task.getStart() != originalStart, "a valid date input must change the task start");
		Point viewportAfter = pane.getViewport().getViewPosition();
		assertEquals(viewportBefore, viewportAfter,
			"a task-table date commit must not move the Gantt timescale viewport");
	}

	@Test
	void physicalTaskTableDurationEditRoundTripsThroughMpo() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture(1);
		showFixture(fixture);
		Robot robot = new Robot();
		robot.setAutoDelay(35);
		GraphicNode node = (GraphicNode) fixture.gantt.getModel().getCache().getElementAt(0);
		editCellPhysically(robot, fixture.sheet, fixture.sheet.getCache().getRowAt(node),
			findColumn(fixture.sheet, "Field.duration"), "10");
		assertEquals(10L * CalendarOption.getInstance().getMillisPerDay(), fixture.project.getTasks().stream()
			.filter(value -> value instanceof NormalTask && "Sequential 1".equals(((NormalTask) value).getName()))
			.map(value -> ((NormalTask) value).getRawDuration()).findFirst().orElseThrow());
		ByteArrayOutputStream saved = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(fixture.project, saved),
			"MPO save rejected the physical duration edit");
		Project reloaded = new MpoFileImporter().loadProject(new ByteArrayInputStream(saved.toByteArray()));
		NormalTask restored = reloaded.getTasks().stream()
			.filter(value -> value instanceof NormalTask && "Sequential 1".equals(((NormalTask) value).getName()))
			.map(value -> (NormalTask) value).findFirst().orElseThrow();
		assertEquals(10L * CalendarOption.getInstance().getMillisPerDay(), restored.getRawDuration(),
			"MPO reload lost the physical duration edit");
	}

	private static void editCellPhysically(Robot robot, SpreadSheet sheet, int row, int column, String value) throws Exception {
		Rectangle bounds = new Rectangle();
		SwingUtilities.invokeAndWait(() -> {
			Rectangle cell = sheet.getCellRect(row, column, true);
			Point location = sheet.getLocationOnScreen();
			bounds.setBounds(location.x + cell.x, location.y + cell.y, cell.width, cell.height);
		});
		robot.mouseMove(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		GuiAcceptanceSupport.await(sheet::isFocusOwner, "duration cell did not receive focus");
		SwingUtilities.invokeAndWait(() -> assertTrue(sheet.editCellAt(row, column, null),
			"duration cell editor did not start"));
		GuiAcceptanceSupport.await(sheet::isEditing, "duration editor did not start");
		SwingUtilities.invokeAndWait(() -> sheet.getEditorComponent().requestFocusInWindow());
		GuiAcceptanceSupport.await(() -> sheet.getEditorComponent() != null && sheet.getEditorComponent().isFocusOwner(),
			"duration editor did not receive focus");
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_A);
		robot.keyRelease(KeyEvent.VK_A);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		for (char character : value.toCharArray()) {
			int keyCode = KeyEvent.getExtendedKeyCodeForChar(character);
			robot.keyPress(keyCode);
			robot.keyRelease(keyCode);
		}
		assertEquals(value, activeEditorText(sheet), "physical duration input must reach the editor");
		robot.keyPress(KeyEvent.VK_ENTER);
		robot.keyRelease(KeyEvent.VK_ENTER);
		GuiAcceptanceSupport.await(() -> !sheet.isEditing(), "duration edit did not commit");
	}

	private static String activeEditorText(SpreadSheet sheet) throws Exception {
		String[] text = new String[1];
		SwingUtilities.invokeAndWait(() -> {
			if (sheet.getEditorComponent() instanceof JTextComponent component) text[0] = component.getText();
			else if (sheet.getEditorComponent() instanceof DateEditor.ExtDateField date) text[0] = date.getTextField().getText();
		});
		return text[0];
	}

	private static int findColumn(SpreadSheet sheet, String fieldId) {
		com.microproject.pm.graphic.spreadsheet.SpreadSheetModel model =
			(com.microproject.pm.graphic.spreadsheet.SpreadSheetModel) sheet.getModel();
		for (int column = 0; column < model.getColumnCount(); column++) {
			Field field = model.getFieldInColumn(column);
			if (field != null && fieldId.equals(field.getId())) return sheet.convertColumnIndexToView(column);
		}
		throw new IllegalArgumentException("Missing field: " + fieldId);
	}

	private static BarGeometry barGeometry(Gantt chart, NormalTask task) throws Exception {
		double[] values = new double[2];
		SwingUtilities.invokeAndWait(() -> {
			values[0] = chart.getCoord().toX(task.getStart());
			values[1] = chart.getCoord().toX(task.getEnd()) - values[0];
		});
		return new BarGeometry((int) Math.round(values[0]), (int) Math.round(values[1]));
	}

	private record BarGeometry(int startX, int width) { }

	private static long fixtureDuration(int index, int taskCount) {
		long day = CalendarOption.getInstance().getMillisPerDay();
		if (taskCount == 1) return day;
		return switch (index % 4) {
			case 0 -> day * 5;
			case 1 -> day / 2;
			case 2 -> day;
			default -> day * 3;
		};
	}

	private record Fixture(SpreadSheet sheet, Gantt gantt, Project project, int taskCount, int sequentialDependencyCount,
		int independentTaskCount) { }
}
