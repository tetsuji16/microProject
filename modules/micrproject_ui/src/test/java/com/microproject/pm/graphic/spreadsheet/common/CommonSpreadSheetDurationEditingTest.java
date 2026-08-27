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
package com.microproject.pm.graphic.spreadsheet.common;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.MouseEvent;

import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.graphic.configuration.SpreadSheetCategories;
import com.microproject.grouping.core.Node;
import com.microproject.options.CalendarOption;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetModel;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetUtils;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

class CommonSpreadSheetDurationEditingTest {
	@Test
	void validDurationTextCommitsToTask() throws Exception {
		SpreadsheetFixture fixture = createFixture();

		SwingUtilities.invokeAndWait(() -> {
			fixture.sheet.changeSelection(fixture.taskRow, fixture.durationColumn, false, false);
			assertTrue(fixture.sheet.editCellAt(fixture.taskRow, fixture.durationColumn, doubleClick(fixture.sheet)));
			JTextField editor = (JTextField) fixture.sheet.getEditorComponent();
			editor.setText("3");
			assertTrue(fixture.sheet.getCellEditor().stopCellEditing());
		});

		assertTrue(fixture.sheet.getLastException() == null);
		assertEquals(3L * CalendarOption.getInstance().getMillisPerDay(), fixture.task.getRawDuration());
	}

	@Test
	void unchangedDurationTextCancelsCleanly() throws Exception {
		SpreadsheetFixture fixture = createFixture();

		SwingUtilities.invokeAndWait(() -> {
			fixture.sheet.changeSelection(fixture.taskRow, fixture.durationColumn, false, false);
			assertTrue(fixture.sheet.editCellAt(fixture.taskRow, fixture.durationColumn, doubleClick(fixture.sheet)));
			assertTrue(fixture.sheet.getCellEditor().stopCellEditing());
		});

		assertFalse(fixture.sheet.isEditing());
		assertTrue(fixture.task.getDuration() == fixture.originalDuration);
	}

	@Test
	void invalidDurationTextLeavesEditorInErrorState() throws Exception {
		SpreadsheetFixture fixture = createFixture();
		final boolean[] stopped = new boolean[1];

		SwingUtilities.invokeAndWait(() -> {
			fixture.sheet.changeSelection(fixture.taskRow, fixture.durationColumn, false, false);
			assertTrue(fixture.sheet.editCellAt(fixture.taskRow, fixture.durationColumn, doubleClick(fixture.sheet)));
			JTextField editor = (JTextField) fixture.sheet.getEditorComponent();
			editor.setText("not-a-duration");
			stopped[0] = fixture.sheet.getCellEditor().stopCellEditing();
		});

		assertFalse(stopped[0]);
		assertNotNull(fixture.sheet.getLastException());
		assertTrue(fixture.task.getDuration() == fixture.originalDuration);
	}

	private static SpreadsheetFixture createFixture() throws Exception {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("duration-edit-test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);

		NormalTask task = project.createScriptedTask();
		task.setName("Duration edit");
		task.setDuration(CalendarOption.getInstance().getMillisPerDay());

		final SpreadsheetFixture[] fixtureRef = new SpreadsheetFixture[1];
		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet sheet = new SpreadSheet();
			sheet.setSpreadSheetCategory(SpreadSheetCategories.taskSpreadsheetCategory);
			NodeModelCache cache = NodeModelCacheFactory.getInstance().createFilteredCache(
				NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel()),
				"duration-edit-test",
				null);
			SpreadSheetUtils.setFieldsAndContext(
				sheet,
				cache,
				SpreadSheetCategories.taskSpreadsheetCategory,
				"Spreadsheet.Task.entry",
				true);
			Node taskNode = (Node) cache.getModel().search(task);
			fixtureRef[0] = new SpreadsheetFixture(sheet, task,
				modelRowForTask((SpreadSheetModel) sheet.getModel(), cache, taskNode));
		});
		return fixtureRef[0];
	}

	private static int modelRowForTask(SpreadSheetModel model, NodeModelCache cache, Node taskNode) {
		return model.findGraphicNodeRow(cache.getGraphicNode(taskNode));
	}

	private static MouseEvent doubleClick(SpreadSheet sheet) {
		return new MouseEvent(sheet, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 0, 0, 2, false);
	}

	private static final class SpreadsheetFixture {
		private final SpreadSheet sheet;
		private final NormalTask task;
		private final int taskRow;
		private final int durationColumn;
		private final long originalDuration;

		private SpreadsheetFixture(SpreadSheet sheet, NormalTask task, int taskRow) {
			this.sheet = sheet;
			this.task = task;
			this.taskRow = taskRow;
			this.durationColumn = findColumnByFieldId(sheet, "Field.duration");
			this.originalDuration = task.getDuration();
		}

		private static int findColumnByFieldId(SpreadSheet sheet, String fieldId) {
			SpreadSheetModel model = (SpreadSheetModel) sheet.getModel();
			for (int column = 0; column < model.getColumnCount(); column++) {
				com.microproject.field.Field field = model.getFieldInColumn(column);
				if (field != null && fieldId.equals(field.getId())) {
					return sheet.convertColumnIndexToView(column);
				}
			}
			throw new IllegalArgumentException("Missing field: " + fieldId);
		}
	}

}
