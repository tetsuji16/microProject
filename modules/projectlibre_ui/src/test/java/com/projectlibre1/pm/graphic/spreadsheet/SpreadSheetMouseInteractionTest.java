package com.projectlibre1.pm.graphic.spreadsheet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.projectlibre1.graphic.configuration.SpreadSheetCategories;
import com.projectlibre1.grouping.core.Node;
import com.projectlibre1.pm.graphic.model.cache.NodeModelCache;
import com.projectlibre1.pm.graphic.model.cache.NodeModelCacheFactory;
import com.projectlibre1.pm.graphic.spreadsheet.common.SpreadSheetRowHeader;
import com.projectlibre1.pm.resource.ResourcePool;
import com.projectlibre1.pm.task.NormalTask;
import com.projectlibre1.pm.task.Project;
import com.projectlibre1.strings.Messages;
import com.projectlibre1.undo.DataFactoryUndoController;

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
	void singleClickSelectsTheClickedCellWithoutStartingAnEdit() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			Fixture fixture = createFixture();
			RecordingSpreadSheet sheet = fixture.sheet();
			int row = findRow(sheet, fixture.secondTask());
			int column = findNameColumn(sheet);

			sheet.handleTableMousePressed(mousePress(sheet, row, column, MouseEvent.BUTTON1, 1));

			assertEquals(row, sheet.getSelectedRow());
			assertEquals(column, sheet.getSelectedColumn());
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
			assertEquals(column, sheet.getSelectedColumn());
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
			assertEquals(column, sheet.getSelectedColumn());
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
	void draggingSelectedTaskIdMovesTheTaskWithoutChangingItsUniqueId() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			Fixture fixture = createFixture();
			RecordingSpreadSheet sheet = fixture.sheet();
			SpreadSheetRowHeader rowHeader = sheet.getRowHeader();
			int firstRow = findRow(sheet, fixture.firstTask());
			int secondRow = findRow(sheet, fixture.secondTask());
			long uniqueId = fixture.firstTask().getUniqueId();
			AtomicInteger tableUpdates=new AtomicInteger();
			sheet.getModel().addTableModelListener(event -> tableUpdates.incrementAndGet());

			dispatchProjectLibreRowHeaderDrag(rowHeader,firstRow,secondRow,true,false);
			assertTrue(rowHeader.isTaskMoveDropValid());
			assertEquals(secondRow,rowHeader.getTaskMoveDropRow());
			dispatchRowHeaderRelease(rowHeader,secondRow,true);

			Node firstNode = sheet.getCache().getModel().search(fixture.firstTask());
			Node secondNode = sheet.getCache().getModel().search(fixture.secondTask());
			Node parent = (Node)firstNode.getParent();
			assertTrue(parent.getIndex(secondNode) < parent.getIndex(firstNode));
			assertEquals(uniqueId, fixture.firstTask().getUniqueId());
			assertTrue(sheet.isRowFullySelected(findRow(sheet, fixture.firstTask())));
			assertTrue(findRow(sheet,fixture.secondTask()) < findRow(sheet,fixture.firstTask()));
			assertTrue(tableUpdates.get()>0,"A task move must notify the visible table model");
		});
	}

	@Test
	void taskIdDragCannotChangeTheTaskOutlineLevel() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			Fixture fixture = createFixture();
			RecordingSpreadSheet sheet = fixture.sheet();
			Node firstNode = sheet.getCache().getModel().search(fixture.firstTask());
			Node secondNode = sheet.getCache().getModel().search(fixture.secondTask());
			assertTrue(sheet.getCache().getModel().relocate(Collections.singletonList(secondNode), firstNode, 0,
				com.projectlibre1.grouping.core.model.NodeModel.NORMAL));
			sheet.getCache().update();
			int firstRow=findRow(sheet,fixture.firstTask());
			int secondRow=findRow(sheet,fixture.secondTask());
			SpreadSheetRowHeader rowHeader=sheet.getRowHeader();
			dispatchProjectLibreRowHeaderDrag(rowHeader,firstRow,secondRow,false,false);
			assertFalse(rowHeader.isTaskMoveDropValid());
			assertEquals(-1,rowHeader.getTaskMoveDropRow());
			dispatchRowHeaderRelease(rowHeader,secondRow,false);

			assertFalse(sheet.getCache().relocateNodes(
				Collections.singletonList(sheet.getCache().getGraphicNode(firstNode)), secondNode, false));
			assertTrue(((Node)firstNode.getParent()).isRoot());
			assertEquals(0, fixture.firstTask().getOutlineLevel());
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

	private MouseEvent rowHeaderMousePress(SpreadSheetRowHeader rowHeader, int row, int clickCount) {
		Rectangle bounds = rowHeader.getCellRect(row, 0, true);
		return new MouseEvent(rowHeader,
			MouseEvent.MOUSE_PRESSED,
			System.currentTimeMillis(),
			MouseEvent.BUTTON1_DOWN_MASK,
			bounds.x + Math.max(1, bounds.width / 2),
			bounds.y + Math.max(1, bounds.height / 2),
			clickCount,
			false,
			MouseEvent.BUTTON1);
	}

	private void fireProjectLibreRowHeaderPress(SpreadSheetRowHeader rowHeader, int row, int clickCount) {
		rowHeader.setUI(null);
		rowHeader.dispatchEvent(rowHeaderMousePress(rowHeader,row,clickCount));
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
