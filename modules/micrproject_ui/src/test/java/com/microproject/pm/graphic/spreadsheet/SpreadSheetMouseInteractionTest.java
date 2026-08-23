/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
 * Copyright (c) 2026 microProject
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package com.microproject.pm.graphic.spreadsheet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.graphic.configuration.SpreadSheetCategories;
import com.microproject.grouping.core.Node;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.graphic.spreadsheet.common.SpreadSheetRowHeader;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.strings.Messages;
import com.microproject.undo.DataFactoryUndoController;

class SpreadSheetMouseInteractionTest {
	@Test
	void msProjectShortcutMovesAWholeTaskRowAndHonorsBoundaries() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			Fixture fixture=createFixture();
			RecordingSpreadSheet sheet=fixture.sheet();
			int firstRow=findRow(sheet,fixture.firstTask());
			int secondRow=findRow(sheet,fixture.secondTask());
			sheet.selectRowAndAllColumns(secondRow);
			assertTrue(sheet.canMoveSelectedTaskRows(-1,true));
			sheet.getActionMap().get(SpreadSheet.MOVE_TASK_UP_ACTION).actionPerformed(
				new ActionEvent(sheet,ActionEvent.ACTION_PERFORMED,SpreadSheet.MOVE_TASK_UP_ACTION));

			assertTrue(findRow(sheet,fixture.secondTask())<findRow(sheet,fixture.firstTask()));
			assertFalse(sheet.canMoveSelectedTaskRows(-1,true));
			assertEquals(firstRow,findRow(sheet,fixture.secondTask()));
		});
	}

	@Test
	void singleClickSelectsTheClickedTaskRowWithoutStartingAnEdit() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			Fixture fixture = createFixture();
			RecordingSpreadSheet sheet = fixture.sheet();
			int row = findRow(sheet, fixture.secondTask());
			int column = findNameColumn(sheet);

			sheet.handleTableMousePressed(mousePress(sheet, row, column, MouseEvent.BUTTON1, 1));

			assertEquals(row, sheet.getSelectedRow());
			assertEquals(sheet.getColumnCount(), sheet.getSelectedColumnCount(),
				"a task-cell click must select the complete task row");
			assertTrue(sheet.isRowFullySelected(row));
			assertFalse(sheet.isEditing());
			assertEquals(0, sheet.informationOpenCount);
		});
	}

	@Test
	void doubleClickSelectsTheTaskAndOpensInformationWithoutEditingTheCell() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			Fixture fixture = createFixture();
			RecordingSpreadSheet sheet = fixture.sheet();
			int row = findRow(sheet, fixture.secondTask());
			int column = findNameColumn(sheet);

			sheet.handleTableMousePressed(mousePress(sheet, row, column, MouseEvent.BUTTON1, 2));

			assertEquals(row, sheet.getSelectedRow());
			assertTrue(sheet.getSelection().isActiveCell(row, column));
			assertTrue(sheet.isRowFullySelected(row));
			assertFalse(sheet.isEditing());
			assertEquals(1, sheet.informationOpenCount);
			assertEquals(row, sheet.informationRow);
			assertEquals(column, sheet.informationColumn);
		});
	}

	@Test
	void rightClickSelectsTheClickedTaskAndShowsItsPopup() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			Fixture fixture = createFixture();
			RecordingSpreadSheet sheet = fixture.sheet();
			int firstRow = findRow(sheet, fixture.firstTask());
			int secondRow = findRow(sheet, fixture.secondTask());
			int column = findNameColumn(sheet);
			sheet.changeSelection(firstRow, column, false, false);

			sheet.handleTableMousePressed(mousePress(sheet, secondRow, column, MouseEvent.BUTTON3, 1));

			assertEquals(secondRow, sheet.getSelectedRow());
			assertTrue(sheet.getSelection().isActiveCell(secondRow, column));
			assertTrue(sheet.isRowFullySelected(secondRow));
			assertTrue(sheet.popupShown);
			assertEquals(secondRow, sheet.shownPopup.getRow());
			assertEquals(column, sheet.shownPopup.getCol());
		});
	}

	@Test
	void taskPopupStartsWithTaskInformationCommand() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			Fixture fixture = createFixture();
			RecordingSpreadSheet sheet = fixture.sheet();
			SpreadSheetPopupMenu popup = sheet.getPopup();
			JMenuItem information = assertInstanceOf(JMenuItem.class, popup.getComponent(0));

			assertEquals(Messages.getString("TaskInformationDialog.TaskInformation"), information.getText());
			information.doClick();
			assertEquals(1, sheet.informationOpenCount);
		});
	}

	@Test
	void repeatedRowHeaderClickKeepsTheTaskRowSelected() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			Fixture fixture = createFixture();
			RecordingSpreadSheet sheet = fixture.sheet();
			int row = findRow(sheet, fixture.secondTask());
			SpreadSheetRowHeader rowHeader = sheet.getRowHeader();

			fireProjectLibreRowHeaderPress(rowHeader, row, 1);
			assertTrue(sheet.isRowFullySelected(row));

			fireProjectLibreRowHeaderPress(rowHeader, row, 1);
			assertTrue(sheet.isRowFullySelected(row));
		});
	}

	@Test
	void f2EditsTheClickedCellAfterWholeRowSelection() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			Fixture fixture = createFixture();
			RecordingSpreadSheet sheet = fixture.sheet();
			int row = findRow(sheet, fixture.secondTask());
			int column = findNameColumn(sheet);

			sheet.handleTableMousePressed(mousePress(sheet, row, column, MouseEvent.BUTTON1, 1));

			assertTrue(sheet.editActiveCell());
			assertEquals(row, sheet.getEditingRow());
			assertEquals(column, sheet.getEditingColumn());
		});
	}

	@Test
	void rowHeaderSelectionReturnsKeyboardFocusToTheTaskTable() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			Fixture fixture = createFixture();
			RecordingSpreadSheet sheet = fixture.sheet();
			int row = findRow(sheet, fixture.firstTask());

			fireProjectLibreRowHeaderPress(sheet.getRowHeader(), row, 1);

			assertEquals(1, sheet.focusRequestCount,
					"selecting a row from its header must return keyboard input to the task table");
		});
	}

	@Test
	void draggingAcrossTaskCellsCreatesACellRangeForClipboardOperations() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			Fixture fixture = createFixture();
			RecordingSpreadSheet sheet = fixture.sheet();
			int firstRow = findRow(sheet, fixture.firstTask());
			int secondRow = findRow(sheet, fixture.secondTask());
			int nameColumn = findNameColumn(sheet);
			int nextColumn = Math.min(nameColumn + 1, sheet.getColumnCount() - 1);

			MouseEvent press = mousePress(sheet, firstRow, nameColumn, MouseEvent.BUTTON1, 1);
			sheet.beginCellRangeSelection(press);
			sheet.handleTableMousePressed(press);
			sheet.extendCellRangeSelection(mouseDrag(sheet, secondRow, nextColumn));

			assertEquals(2, sheet.getSelectedRowCount());
			assertEquals(nextColumn - nameColumn + 1, sheet.getSelectedColumnCount());
			assertFalse(sheet.isRowHeaderSelectionActive(),
					"a task-table drag must select cells rather than turn into a whole-row copy");
			assertEquals(2, sheet.getSelectedFields().size());
		});
	}

	@Test
	void ctrlClickingTaskIdsSelectsNonadjacentWholeRows() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			Fixture fixture = createFixture();
			RecordingSpreadSheet sheet = fixture.sheet();
			int firstRow = findRow(sheet, fixture.firstTask());
			int secondRow = findRow(sheet, fixture.secondTask());
			SpreadSheetRowHeader rowHeader = sheet.getRowHeader();

			fireProjectLibreRowHeaderPress(rowHeader, firstRow, 1);
			fireProjectLibreRowHeaderPress(rowHeader, secondRow, 1, true);

			assertEquals(2, sheet.getSelectedRowCount());
			assertEquals(sheet.getColumnCount(), sheet.getSelectedColumnCount());
			assertTrue(sheet.getSelectionModel().isSelectedIndex(firstRow));
			assertTrue(sheet.getSelectionModel().isSelectedIndex(secondRow));
		});
	}

	@Test
	void rowHeaderDoubleClickOpensInformationForThatTask() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			Fixture fixture = createFixture();
			RecordingSpreadSheet sheet = fixture.sheet();
			int row = findRow(sheet, fixture.secondTask());
			SpreadSheetRowHeader rowHeader = sheet.getRowHeader();

			fireProjectLibreRowHeaderPress(rowHeader, row, 2);

			assertTrue(sheet.isRowFullySelected(row));
			assertEquals(1, sheet.informationOpenCount);
			assertEquals(row, sheet.informationRow);
		});
	}

	@Test
	void rowHeaderDragSelectsARangeOfRowsAndDoesNotMoveTasks() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			Fixture fixture = createFixture();
			RecordingSpreadSheet sheet = fixture.sheet();
			SpreadSheetRowHeader rowHeader = sheet.getRowHeader();
			int firstRow = findRow(sheet, fixture.firstTask());
			int secondRow = findRow(sheet, fixture.secondTask());
			long uniqueId = fixture.firstTask().getUniqueId();

			// Press and drag through the row header (ID column) without releasing,
			// exactly like a user selecting a range of rows.
			dispatchProjectLibreRowHeaderDrag(rowHeader, firstRow, secondRow, true, false);

			// The selection must already span the dragged range so the Gantt chart
			// row highlight follows the drag live (issue #179).
			int[] selected = sheet.getSelectedRows();
			Arrays.sort(selected);
			assertEquals(Arrays.toString(new int[] { firstRow, secondRow }),
					Arrays.toString(selected),
					"dragging the row header must select the full range immediately");
			assertEquals(sheet.getColumnCount(), sheet.getSelectedColumnCount(),
					"row-header drag selects whole rows");

			dispatchRowHeaderRelease(rowHeader, secondRow, true);

			// Selecting by dragging must never reorder tasks or change unique ids.
			assertEquals(uniqueId, fixture.firstTask().getUniqueId());
			assertEquals(firstRow, findRow(sheet, fixture.firstTask()));
			assertEquals(secondRow, findRow(sheet, fixture.secondTask()));
		});
	}

	@Test
	void rowHeaderDragDoesNotRelocateTasksOrChangeOutlineLevel() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			Fixture fixture = createFixture();
			RecordingSpreadSheet sheet = fixture.sheet();
			Node firstNode = sheet.getCache().getModel().search(fixture.firstTask());
			Node secondNode = sheet.getCache().getModel().search(fixture.secondTask());
			assertTrue(sheet.getCache().getModel().relocate(Collections.singletonList(secondNode), firstNode, 0,
					com.microproject.grouping.core.model.NodeModel.NORMAL));
			sheet.getCache().update();
			int firstRow = findRow(sheet, fixture.firstTask());
			int secondRow = findRow(sheet, fixture.secondTask());
			SpreadSheetRowHeader rowHeader = sheet.getRowHeader();

			// Drag the row header across rows: this is a selection gesture, so it
			// must not relocate tasks or change the outline level (issue #179).
			// Previously the same gesture attempted a move and could change the
			// outline level, and beeped on an invalid drop.
			dispatchProjectLibreRowHeaderDrag(rowHeader, firstRow, secondRow, false, false);
			dispatchRowHeaderRelease(rowHeader, secondRow, false);

			assertTrue(((Node) firstNode.getParent()).isRoot());
			assertEquals(0, fixture.firstTask().getOutlineLevel());
			assertEquals(2, sheet.getSelectedRowCount());
		});
	}

	@Test
	void keyboardMoveWorksFromASingleTaskCellSelection() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			Fixture fixture = createFixture();
			RecordingSpreadSheet sheet = fixture.sheet();
			int firstRow = findRow(sheet, fixture.firstTask());
			int secondRow = findRow(sheet, fixture.secondTask());
			int column = findNameColumn(sheet);

			// Reproduce the reported sequence: select a column, then click a single
			// cell. This collapses the selection to one cell (1 row, 1 column) instead
			// of a whole row.
			sheet.selectColumnAndAllRows(column);
			assertTrue(sheet.isColumnFullySelected(column),
					"after selectColumnAndAllRows the column must be fully selected");
			sheet.changeSelection(secondRow, column, false, false);
			assertEquals(1, sheet.getSelectedRowCount(),
					"after selecting a column then a single cell, exactly one row is selected");
			assertEquals(1, sheet.getSelectedColumnCount(),
					"after selecting a column then a single cell, exactly one column is selected");
			assertFalse(sheet.getColumnCount() == sheet.getSelectedColumnCount(),
					"the selection is a single cell, NOT a whole row");

			// Before the fix the keyboard move required the entire row and was silently
			// rejected here, so the task list never refreshed. Microsoft Project moves
			// the selected task from any selected cell, so the keyboard shortcut (which
			// uses requireEntireRow=false) must now move and refresh.
			assertTrue(sheet.canMoveSelectedTaskRows(-1, false),
					"keyboard/drag move precondition must accept a single task-cell selection");
			sheet.getActionMap().get(SpreadSheet.MOVE_TASK_UP_ACTION).actionPerformed(
					new ActionEvent(sheet, ActionEvent.ACTION_PERFORMED, SpreadSheet.MOVE_TASK_UP_ACTION));
			assertTrue(findRow(sheet, fixture.secondTask()) < findRow(sheet, fixture.firstTask()),
					"the selected task must move up after the keyboard shortcut from a single cell");
		});
	}

	@Test
	void dragMovePreconditionAcceptsSingleCellSelection() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			Fixture fixture = createFixture();
			RecordingSpreadSheet sheet = fixture.sheet();
			int firstRow = findRow(sheet, fixture.firstTask());
			int secondRow = findRow(sheet, fixture.secondTask());
			int column = findNameColumn(sheet);

			// Reproduce the reported sequence again, this time for the ID-column
			// drag-and-drop move path: select a column, then click a single cell.
			// The drag drop target validation is canMoveSelectedTaskRowsTo, which
			// (unlike the keyboard/ribbon paths) still required the entire row.
			sheet.selectColumnAndAllRows(column);
			sheet.changeSelection(secondRow, column, false, false);
			assertEquals(1, sheet.getSelectedRowCount(),
					"after selecting a column then a single cell, exactly one row is selected");
			assertEquals(1, sheet.getSelectedColumnCount(),
					"after selecting a column then a single cell, exactly one column is selected");
			assertFalse(sheet.getColumnCount() == sheet.getSelectedColumnCount(),
					"the selection is a single cell, NOT a whole row");

			// The single selected task row must be a valid drag target, just like the
			// keyboard/ribbon move. Before the fix this returned false and the drop
			// was silently rejected (beep only), so the list never refreshed.
			assertTrue(sheet.canMoveSelectedTaskRowsTo(firstRow, false),
					"drag-drop move target must accept a single task-cell selection");
			assertTrue(sheet.moveSelectedTaskRowsTo(firstRow, false),
					"drag-drop move must succeed from a single task-cell selection");
			assertTrue(findRow(sheet, fixture.secondTask()) < findRow(sheet, fixture.firstTask()),
					"the selected task must move above the target from a single-cell drag");
		});
	}

	@Test
	void taskMoveIsRejectedWhenSelectionContainsANonTaskRow() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			Fixture fixture = createFixture();
			RecordingSpreadSheet sheet = fixture.sheet();
			int column = findNameColumn(sheet);
			// A single-cell selection on a real task row must still be movable (the
			// regression from issue #45); this guards that the relaxed precondition
			// did not accidentally start accepting non-task rows.
			sheet.selectColumnAndAllRows(column);
			sheet.changeSelection(findRow(sheet, fixture.secondTask()), column, false, false);
			assertTrue(sheet.canMoveSelectedTaskRows(-1, false),
					"a single task cell must remain movable");
		});
	}

	@Test
	void movePreconditionStillRequiresEntireRowWhenExplicitlyRequested() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			Fixture fixture = createFixture();
			RecordingSpreadSheet sheet = fixture.sheet();
			int row = findRow(sheet, fixture.secondTask());
			int column = findNameColumn(sheet);

			// Whole-row selection (all columns): move precondition is satisfied.
			sheet.selectRowAndAllColumns(row);
			assertTrue(sheet.isRowFullySelected(row),
					"selecting the whole row must be a fully-selected row");
			assertEquals(sheet.getColumnCount(), sheet.getSelectedColumnCount(),
					"whole-row selection selects every column");
			assertTrue(sheet.canMoveSelectedTaskRows(-1, true),
					"move up must be allowed when the whole row is selected");

			// A single-cell selection must still be rejected when the caller explicitly
			// requires the entire row (drag path keeps its stricter check).
			sheet.selectColumnAndAllRows(column);
			assertTrue(sheet.isColumnFullySelected(column),
					"after selectColumnAndAllRows the column must be fully selected");
			sheet.changeSelection(row, column, false, false);
			assertEquals(1, sheet.getSelectedColumnCount(),
					"after selecting a column then a single cell, exactly one column is selected");
			assertFalse(sheet.canMoveSelectedTaskRows(-1, true),
					"explicit requireEntireRow=true must still reject a single-cell selection");
			assertTrue(sheet.moveSelectedTaskRowsFromCommand(-1),
					"Move Up/Down command (requireEntireRow=false) still works on a single-cell selection");
		});
	}

	private Fixture createFixture() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("mouse-test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		NormalTask firstTask = createTask(project, "First task");
		NormalTask secondTask = createTask(project, "Second task");

		NodeModelCache cache = NodeModelCacheFactory.getInstance().createFilteredCache(
			NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel()),
			"mouse-test",
			null);
		RecordingSpreadSheet sheet = new RecordingSpreadSheet();
		sheet.setSpreadSheetCategory(SpreadSheetCategories.taskSpreadsheetCategory);
		SpreadSheetUtils.setFieldsAndContext(sheet,
			cache,
			SpreadSheetCategories.taskSpreadsheetCategory,
			"Spreadsheet.Task.entry",
			true);
		cache.update();
		return new Fixture(sheet, firstTask, secondTask);
	}

	private NormalTask createTask(Project project, String name) {
		NormalTask task = project.createScriptedTask();
		task.setName(name);
		project.connectTask(task);
		project.getTaskOutlines().addToAll(task, null);
		return task;
	}

	private int findNameColumn(SpreadSheet sheet) {
		for (int column = 0; column < sheet.getColumnCount(); column++) {
			if (sheet.isNameFieldColumn(column)) {
				return column;
			}
		}
		return -1;
	}

	private int findRow(SpreadSheet sheet, NormalTask task) {
		SpreadSheetModel model = (SpreadSheetModel) sheet.getModel();
		Node node = (Node) sheet.getCache().getModel().search(task);
		return model.findGraphicNodeRow(sheet.getCache().getGraphicNode(node));
	}

	private MouseEvent mousePress(SpreadSheet sheet, int row, int column, int button, int clickCount) {
		Rectangle bounds = sheet.getCellRect(row, column, true);
		int modifiers = button == MouseEvent.BUTTON1
			? MouseEvent.BUTTON1_DOWN_MASK
			: MouseEvent.BUTTON3_DOWN_MASK;
		return new MouseEvent(sheet,
			MouseEvent.MOUSE_PRESSED,
			System.currentTimeMillis(),
			modifiers,
			bounds.x + Math.max(1, bounds.width / 2),
			bounds.y + Math.max(1, bounds.height / 2),
			clickCount,
			button == MouseEvent.BUTTON3,
			button);
	}

	private MouseEvent mouseDrag(SpreadSheet sheet, int row, int column) {
		Rectangle bounds = sheet.getCellRect(row, column, true);
		return new MouseEvent(sheet,
			MouseEvent.MOUSE_DRAGGED,
			System.currentTimeMillis(),
			MouseEvent.BUTTON1_DOWN_MASK,
			bounds.x + Math.max(1, bounds.width / 2),
			bounds.y + Math.max(1, bounds.height / 2),
			0,
			false,
			MouseEvent.NOBUTTON);
	}

	private MouseEvent rowHeaderMousePress(SpreadSheetRowHeader rowHeader, int row, int clickCount) {
		return rowHeaderMousePress(rowHeader, row, clickCount, false);
	}

	private MouseEvent rowHeaderMousePress(SpreadSheetRowHeader rowHeader, int row, int clickCount, boolean controlDown) {
		Rectangle bounds = rowHeader.getCellRect(row, 0, true);
		return new MouseEvent(rowHeader,
			MouseEvent.MOUSE_PRESSED,
			System.currentTimeMillis(),
			MouseEvent.BUTTON1_DOWN_MASK | (controlDown ? MouseEvent.CTRL_DOWN_MASK : 0),
			bounds.x + Math.max(1, bounds.width / 2),
			bounds.y + Math.max(1, bounds.height / 2),
			clickCount,
			false,
			MouseEvent.BUTTON1);
	}

	private void fireProjectLibreRowHeaderPress(SpreadSheetRowHeader rowHeader, int row, int clickCount) {
		fireProjectLibreRowHeaderPress(rowHeader, row, clickCount, false);
	}

	private void fireProjectLibreRowHeaderPress(SpreadSheetRowHeader rowHeader, int row, int clickCount, boolean controlDown) {
		rowHeader.setUI(null);
		rowHeader.dispatchEvent(rowHeaderMousePress(rowHeader,row,clickCount,controlDown));
	}

	private void fireProjectLibreRowHeaderDrag(SpreadSheetRowHeader rowHeader, int sourceRow, int targetRow, boolean after) {
		dispatchProjectLibreRowHeaderDrag(rowHeader,sourceRow,targetRow,after,true);
	}

	private void dispatchProjectLibreRowHeaderDrag(SpreadSheetRowHeader rowHeader,int sourceRow,int targetRow,boolean after,boolean release) {
		rowHeader.setUI(null);
		rowHeader.dispatchEvent(rowHeaderMousePress(rowHeader,sourceRow,1));
		Rectangle target = rowHeader.getCellRect(targetRow, 0, true);
		int y = after ? target.y + target.height - 2 : target.y + 2;
		MouseEvent drag = new MouseEvent(rowHeader, MouseEvent.MOUSE_DRAGGED, System.currentTimeMillis(),
			MouseEvent.BUTTON1_DOWN_MASK, target.x + Math.max(1, target.width / 2), y, 0, false, MouseEvent.NOBUTTON);
		rowHeader.dispatchEvent(drag);
		if (release) dispatchRowHeaderRelease(rowHeader,targetRow,after);
	}

	private void dispatchRowHeaderRelease(SpreadSheetRowHeader rowHeader,int targetRow,boolean after) {
		Rectangle target=rowHeader.getCellRect(targetRow,0,true);
		int y=after?target.y+target.height-2:target.y+2;
		MouseEvent release = new MouseEvent(rowHeader, MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(),
			0,target.x+Math.max(1,target.width/2),y,1,false,MouseEvent.BUTTON1);
		rowHeader.dispatchEvent(release);
	}

	private record Fixture(RecordingSpreadSheet sheet, NormalTask firstTask, NormalTask secondTask) {}

	private static final class RecordingSpreadSheet extends SpreadSheet {
		private static final long serialVersionUID = 1L;
		private int informationOpenCount;
		private int informationRow = -1;
		private int informationColumn = -1;
		private boolean popupShown;
		private SpreadSheetPopupMenu shownPopup;
		private int focusRequestCount;

		@Override
		public boolean requestFocusInWindow() {
			focusRequestCount++;
			return true;
		}

		@Override
		public void doDoubleClick(int row, int col) {
			informationOpenCount++;
			informationRow = row;
			informationColumn = col;
		}

		@Override
		protected void showPopupMenu(SpreadSheetPopupMenu popup, MouseEvent e) {
			popupShown = true;
			shownPopup = popup;
		}
	}
}
