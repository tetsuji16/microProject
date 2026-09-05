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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.function.Supplier;

import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.field.Field;
import com.microproject.graphic.configuration.SpreadSheetCategories;
import com.microproject.grouping.core.Node;
import com.microproject.menu.MenuActionConstants;
import com.microproject.pm.graphic.ChangeAwareTextField;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.graphic.spreadsheet.editor.SimpleEditor;
import com.microproject.pm.graphic.spreadsheet.editor.SpreadSheetCellEditorAdapter;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.undo.DataFactoryUndoController;

class SpreadSheetHierarchyNavigationTest {
	@Test
	void ctrlUpAndDownAreBoundForBothTableAndEditorBoundaryNavigation() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			Fixture fixture = createHierarchyFixture();
			SpreadSheet sheet = fixture.sheet();
			InputMap tableInputMap = sheet.getInputMap(JComponent.WHEN_FOCUSED);
			assertEquals("spreadsheet.nameColumnJumpFirst",
				tableInputMap.get(KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.CTRL_DOWN_MASK)));
			assertEquals("spreadsheet.nameColumnJumpLast",
				tableInputMap.get(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.CTRL_DOWN_MASK)));

			SpreadSheetCellEditorAdapter adapter = new SpreadSheetCellEditorAdapter(new SimpleEditor(String.class));
			int nameColumn = findNameColumn(sheet);
			int row = findRow(sheet, fixture.firstChild());
			JComponent editor = (JComponent) adapter.getTableCellEditorComponent(sheet, "Task", true, row, nameColumn);
			InputMap editorInputMap = editor.getInputMap(JComponent.WHEN_FOCUSED);
			assertEquals(SpreadSheetCellEditorAdapterNameBindings.FIRST_ACTION,
				editorInputMap.get(KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.CTRL_DOWN_MASK)));
			assertEquals(SpreadSheetCellEditorAdapterNameBindings.LAST_ACTION,
				editorInputMap.get(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.CTRL_DOWN_MASK)));
			assertNotNull(editor.getClientProperty(ChangeAwareTextField.NAME_HIERARCHY_PREVIOUS_ACTION_PROPERTY));
			assertNotNull(editor.getClientProperty(ChangeAwareTextField.NAME_HIERARCHY_NEXT_ACTION_PROPERTY));
		});
	}

	@Test
	void nameEditorGivesTaskShortcutsPriorityOverTextEntry() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			Fixture fixture = createHierarchyFixture();
			SpreadSheet sheet = fixture.sheet();
			SpreadSheetCellEditorAdapter adapter = new SpreadSheetCellEditorAdapter(new SimpleEditor(String.class));
			int nameColumn = findNameColumn(sheet);
			int row = findRow(sheet, fixture.firstChild());
			JComponent editor = (JComponent) adapter.getTableCellEditorComponent(sheet, "Task", true, row, nameColumn);
			InputMap editorInputMap = editor.getInputMap(JComponent.WHEN_FOCUSED);

			assertEquals(SpreadSheet.NAME_COLUMN_INDENT_ACTION,
				editorInputMap.get(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0)));
			assertEquals(SpreadSheet.NAME_COLUMN_OUTDENT_ACTION,
				editorInputMap.get(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, KeyEvent.SHIFT_DOWN_MASK)));
			assertEquals(ChangeAwareTextField.NAME_HIERARCHY_COLLAPSE_ACTION_PROPERTY,
				editorInputMap.get(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.CTRL_DOWN_MASK)));
			assertEquals(ChangeAwareTextField.NAME_HIERARCHY_EXPAND_ACTION_PROPERTY,
				editorInputMap.get(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.CTRL_DOWN_MASK)));
			assertNull(editorInputMap.get(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK)));
			assertNull(editorInputMap.get(KeyStroke.getKeyStroke(KeyEvent.VK_Y, KeyEvent.CTRL_DOWN_MASK)));
		});
	}

	@Test
	void insertingAColumnUpdatesTheTaskLayoutAndSelectsTheInsertedField() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			Fixture fixture = createHierarchyFixture();
			SpreadSheet sheet = fixture.sheet();
			Field field = sheet.getAvailableFields().stream()
				.filter(candidate -> !sheet.getFieldArray().contains(candidate))
				.findFirst()
				.orElseThrow();
			int fieldCount = sheet.getFieldArray().size();
			int columnCount = sheet.getColumnCount();

			assertTrue(sheet.insertColumn(2, field));
			assertEquals(fieldCount + 1, sheet.getFieldArray().size());
			assertEquals(columnCount + 1, sheet.getColumnCount());
			assertEquals(field, sheet.getFieldArray().get(2));
			assertEquals(1, sheet.getSelectedColumn());

			fixture.project().getUndoController().undo();
			assertEquals(fieldCount, sheet.getFieldArray().size());
			assertEquals(columnCount, sheet.getColumnCount());
			fixture.project().getUndoController().redo();
			assertEquals(fieldCount + 1, sheet.getFieldArray().size());
			assertEquals(field, sheet.getFieldArray().get(2));
		});
	}

	@Test
	void ctrlUpAndDownMoveToTheFirstAndLastVisibleTaskRows() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			Fixture fixture = createHierarchyFixture();
			SpreadSheet sheet = fixture.sheet();
			int nameColumn = findNameColumn(sheet);
			int firstRow = firstTaskRow(sheet);
			int lastRow = lastTaskRow(sheet);
			assertTrue(firstRow >= 0);
			assertTrue(lastRow >= 0);

			sheet.setRowSelectionInterval(lastRow, lastRow);
			sheet.setColumnSelectionInterval(nameColumn, nameColumn);
			sheet.executeNameCellBoundaryJump(false);
			assertEquals(firstRow, sheet.getSelectedRow());
			assertEquals(nameColumn, sheet.getSelectedColumn());

			sheet.executeNameCellBoundaryJump(true);
			assertEquals(lastRow, sheet.getSelectedRow());
			assertEquals(nameColumn, sheet.getSelectedColumn());
		});
	}

	@Test
	void nameCellEditorReceivesBoundaryNavigationActionsThroughClientProperties() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			Fixture fixture = createHierarchyFixture();
			SpreadSheet sheet = fixture.sheet();
			int nameColumn = findNameColumn(sheet);
			int row = findRow(sheet, fixture.firstChild());
			assertTrue(row >= 0);

			SpreadSheetCellEditorAdapter adapter = new SpreadSheetCellEditorAdapter(new SimpleEditor(String.class));
			JComponent editor = (JComponent) adapter.getTableCellEditorComponent(sheet, "Task", true, row, nameColumn);
			assertNotNull(editor.getClientProperty(ChangeAwareTextField.NAME_HIERARCHY_PREVIOUS_ACTION_PROPERTY));
			assertNotNull(editor.getClientProperty(ChangeAwareTextField.NAME_HIERARCHY_NEXT_ACTION_PROPERTY));
		});
	}

	@Test
	void ctrlUpAndDownStayAtTheVisibleBoundaries() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			Fixture fixture = createHierarchyFixture();
			SpreadSheet sheet = fixture.sheet();
			int nameColumn = findNameColumn(sheet);
			int firstRow = firstTaskRow(sheet);
			int lastRow = lastTaskRow(sheet);
			assertTrue(firstRow >= 0);
			assertTrue(lastRow >= 0);

			sheet.setRowSelectionInterval(firstRow, firstRow);
			sheet.setColumnSelectionInterval(nameColumn, nameColumn);
			sheet.executeNameCellBoundaryJump(false);
			assertEquals(firstRow, sheet.getSelectedRow());

			sheet.setRowSelectionInterval(lastRow, lastRow);
			sheet.setColumnSelectionInterval(nameColumn, nameColumn);
			sheet.executeNameCellBoundaryJump(true);
			assertEquals(lastRow, sheet.getSelectedRow());
		});
	}

	@Test
	void tabIndentAndOutdentCollapseToTheCurrentNameRowBeforeDispatch() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			Fixture fixture = createHierarchyFixture(RecordingSpreadSheet::new);
			RecordingSpreadSheet sheet = (RecordingSpreadSheet) fixture.sheet();
			int nameColumn = findNameColumn(sheet);
			int firstChildRow = findRow(sheet, fixture.firstChild());
			int secondChildRow = findRow(sheet, fixture.secondChild());
			assertTrue(firstChildRow >= 0);
			assertTrue(secondChildRow >= 0);

			sheet.setRowSelectionInterval(firstChildRow, secondChildRow);
			sheet.setColumnSelectionInterval(nameColumn, nameColumn);
			sheet.executeNameCellTabAction(false);
			assertEquals(MenuActionConstants.ACTION_INDENT, sheet.lastActionId);
			assertEquals(1, sheet.selectedRowsDuringAction.length);
			assertEquals(firstChildRow, sheet.selectedRowsDuringAction[0]);

			sheet.setRowSelectionInterval(firstChildRow, secondChildRow);
			sheet.setColumnSelectionInterval(nameColumn, nameColumn);
			sheet.executeNameCellTabAction(true);
			assertEquals(MenuActionConstants.ACTION_OUTDENT, sheet.lastActionId);
			assertEquals(1, sheet.selectedRowsDuringAction.length);
			assertEquals(firstChildRow, sheet.selectedRowsDuringAction[0]);
		});
	}

	private Fixture createHierarchyFixture() {
		return createHierarchyFixture(SpreadSheet::new);
	}

	private Fixture createHierarchyFixture(Supplier<SpreadSheet> sheetFactory) {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("shortcut-test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);

		NormalTask parent = createTask(project, "Parent");
		NormalTask firstChild = createTask(project, "First child");
		NormalTask secondChild = createTask(project, "Second child");
		project.getTaskOutlines().addToAll(parent, null);
		project.getTaskOutlines().addToAll(firstChild, null);
		project.getTaskOutlines().addToAll(secondChild, null);

		NodeModelCache cache = NodeModelCacheFactory.getInstance().createFilteredCache(
			NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel()),
			"shortcut-test",
			null);
		SpreadSheet sheet = sheetFactory.get();
		sheet.setSpreadSheetCategory(SpreadSheetCategories.taskSpreadsheetCategory);
		SpreadSheetUtils.setFieldsAndContext(sheet,
			cache,
			SpreadSheetCategories.taskSpreadsheetCategory,
			"Spreadsheet.Task.entry",
			true);

		Node parentNode = (Node) project.getTaskModel().search(parent);
		Node firstChildNode = (Node) project.getTaskModel().search(firstChild);
		Node secondChildNode = (Node) project.getTaskModel().search(secondChild);
		try {
			cache.createHierarchyDependency((com.microproject.pm.graphic.model.cache.GraphicNode) cache.getGraphicNode(parentNode),
				(com.microproject.pm.graphic.model.cache.GraphicNode) cache.getGraphicNode(firstChildNode));
			cache.createHierarchyDependency((com.microproject.pm.graphic.model.cache.GraphicNode) cache.getGraphicNode(parentNode),
				(com.microproject.pm.graphic.model.cache.GraphicNode) cache.getGraphicNode(secondChildNode));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		cache.update();

		return new Fixture(sheet, project, firstChild, secondChild);
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
		if (!(sheet.getModel() instanceof SpreadSheetModel model)) {
			return -1;
		}
		Node node = (Node) sheet.getCache().getModel().search(task);
		return model.findGraphicNodeRow(sheet.getCache().getGraphicNode(node));
	}

	private int firstTaskRow(SpreadSheet sheet) {
		if (!(sheet.getModel() instanceof SpreadSheetModel model)) return -1;
		for (int row = 0; row < sheet.getRowCount(); row++)
			if (model.getNode(row) != null && !model.getNode(row).isVoid()) return row;
		return -1;
	}

	private int lastTaskRow(SpreadSheet sheet) {
		if (!(sheet.getModel() instanceof SpreadSheetModel model)) return -1;
		for (int row = sheet.getRowCount() - 1; row >= 0; row--)
			if (model.getNode(row) != null && !model.getNode(row).isVoid()) return row;
		return -1;
	}

	private record Fixture(SpreadSheet sheet, Project project, NormalTask firstChild, NormalTask secondChild) {}

	private static final class RecordingSpreadSheet extends SpreadSheet {
		private static final long serialVersionUID = 1L;
		private String lastActionId;
		private int[] selectedRowsDuringAction = new int[0];

		@Override
		public void executeAction(String actionId) {
			lastActionId = actionId;
			selectedRowsDuringAction = getSelectedRows();
		}
	}

	private static final class SpreadSheetCellEditorAdapterNameBindings {
		private static final String FIRST_ACTION = "spreadsheet.nameColumnFirst";
		private static final String LAST_ACTION = "spreadsheet.nameColumnLast";
	}
}
