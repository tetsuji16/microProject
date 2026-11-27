/*******************************************************************************
 * MIT License
 *
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
package com.microproject.ui.view.spreadsheet.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.model.resource.ResourcePool;
import com.microproject.model.task.NormalTask;
import com.microproject.model.task.Project;
import com.microproject.ui.view.spreadsheet.SpreadSheet;
import com.microproject.ui.view.spreadsheet.SpreadSheetUtils;
import com.microproject.ui.view.model.cache.NodeModelCache;
import com.microproject.ui.view.model.cache.NodeModelCacheFactory;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.view.configuration.SpreadSheetCategories;

/**
 * Sheet navigation keys follow Microsoft Project's table shortcuts: Tab and
 * Shift+Tab move one field, Home/End move to the first/last field of the row,
 * and Ctrl+Home/Ctrl+End move to the first field of the first row and the last
 * field of the last task row.  Every key moves from the visible active cell, never
 * from an incidental JTable lead, and never starts an edit (#1432).
 */
class SpreadSheetKeyboardNavigationTest {
	@Test
	void tabAndShiftTabMoveOneFieldWithinTheRow() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet sheet = createSheet();
			sheet.changeSelection(1, 1, false, false);

			press(sheet, KeyEvent.VK_TAB, 0);
			assertActiveCell(sheet, 1, 2);

			press(sheet, KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK);
			press(sheet, KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK);
			assertActiveCell(sheet, 1, 0);
			assertFalse(sheet.isEditing(), "navigation must not start an edit");
		});
	}

	@Test
	void homeAndEndMoveToTheFirstAndLastFieldOfTheRow() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet sheet = createSheet();
			sheet.changeSelection(1, 1, false, false);

			press(sheet, KeyEvent.VK_END, 0);
			assertActiveCell(sheet, 1, sheet.getColumnCount() - 1);

			press(sheet, KeyEvent.VK_HOME, 0);
			assertActiveCell(sheet, 1, 0);
		});
	}

	@Test
	void ctrlHomeAndCtrlEndMoveToTheCornersOfTheSheet() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet sheet = createSheet();
			sheet.changeSelection(1, 1, false, false);
			int lastTaskRow = 2; // three tasks, followed by blank entry rows
			assertTrue(sheet.getRowCount() > 3, "the fixture must have trailing blank rows");

			press(sheet, KeyEvent.VK_END, InputEvent.CTRL_DOWN_MASK);
			assertActiveCell(sheet, lastTaskRow, sheet.getColumnCount() - 1);

			press(sheet, KeyEvent.VK_HOME, InputEvent.CTRL_DOWN_MASK);
			assertActiveCell(sheet, 0, 0);
		});
	}

	@Test
	void navigationStartsFromTheActiveCellOfAWholeRowSelection() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet sheet = createSheet();
			// A task-table click selects the whole row and keeps the clicked field active.
			sheet.selectRowAndAllColumns(1);
			sheet.getSelection().setActiveCell(1, 2);

			press(sheet, KeyEvent.VK_TAB, 0);
			assertActiveCell(sheet, 1, 3);
		});
	}

	private static void assertActiveCell(SpreadSheet sheet, int row, int column) {
		assertEquals(row, sheet.getSelection().getActiveRow(), "active row");
		assertEquals(column, sheet.getSelection().getActiveColumn(), "active column");
		assertTrue(sheet.isCellSelected(row, column), "the active cell must be selected");
		assertEquals(1, sheet.getSelectedRowCount(), "navigation collapses the selection to one row");
		assertEquals(1, sheet.getSelectedColumnCount(), "navigation collapses the selection to one column");
	}

	private static void press(SpreadSheet sheet, int keyCode, int modifiers) {
		sheet.processKeyEvent(new KeyEvent(sheet, KeyEvent.KEY_PRESSED, System.currentTimeMillis(),
			modifiers, keyCode, KeyEvent.CHAR_UNDEFINED));
	}

	private static SpreadSheet createSheet() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("navigation-test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		for (String name : new String[] {"First task", "Second task", "Third task"})
			createTask(project, name);
		NodeModelCache cache = NodeModelCacheFactory.getInstance().createFilteredCache(
			NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel()),
			"navigation-test",
			null);
		SpreadSheet sheet = new SpreadSheet();
		sheet.setSpreadSheetCategory(SpreadSheetCategories.taskSpreadsheetCategory);
		SpreadSheetUtils.setFieldsAndContext(sheet,
			cache,
			SpreadSheetCategories.taskSpreadsheetCategory,
			"Spreadsheet.Task.entry",
			true);
		cache.update();
		return sheet;
	}

	private static void createTask(Project project, String name) {
		NormalTask task = project.createScriptedTask();
		task.setName(name);
		project.connectTask(task);
		project.getTaskOutlines().addToAll(task, null);
	}
}
