package com.projectlibre1.pm.graphic.spreadsheet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

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
		MouseEvent event = rowHeaderMousePress(rowHeader, row, clickCount);
		for (MouseListener listener : rowHeader.getMouseListeners()) {
			if (listener.getClass().getName().startsWith(SpreadSheetRowHeader.class.getName() + "$")) {
				listener.mousePressed(event);
				return;
			}
		}
		throw new AssertionError("ProjectLibre row-header mouse listener was not installed");
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
