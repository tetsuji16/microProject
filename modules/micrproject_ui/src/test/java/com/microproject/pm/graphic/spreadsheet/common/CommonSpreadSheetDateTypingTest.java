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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.KeyEvent;
import java.lang.reflect.Method;
import java.util.Calendar;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.graphic.configuration.SpreadSheetCategories;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetModel;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetUtils;
import com.microproject.pm.graphic.spreadsheet.editor.DateEditor;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.DateTime;

class CommonSpreadSheetDateTypingTest {
	@Test
	void firstTypedDigitClearsExistingDateText() throws Exception {
		SpreadsheetFixture fixture = createFixture();

		SwingUtilities.invokeAndWait(() -> {
			fixture.sheet.changeSelection(0, fixture.startColumn, false, false);
			fixture.sheet.processKeyEvent(new KeyEvent(fixture.sheet, KeyEvent.KEY_TYPED, System.currentTimeMillis(), 0, KeyEvent.VK_UNDEFINED, '3'));
		});
		SwingUtilities.invokeAndWait(() -> {
		});

		DateEditor.ExtDateField editor = (DateEditor.ExtDateField) fixture.sheet.getEditorComponent();
		assertEquals("3", editor.getTextField().getText());
		assertNull(editor.getTextField().getSelectedText());
		assertEquals(1, editor.getTextField().getCaretPosition());
	}

	@Test
	void nonDigitDoesNotUseDateClearOnStartRule() throws Exception {
		SpreadsheetFixture fixture = createFixture();
		Method method = CommonSpreadSheet.class.getDeclaredMethod("shouldClearFieldOnTypedDigit", int.class, int.class, char.class);
		method.setAccessible(true);

		assertFalse((Boolean) method.invoke(fixture.sheet, 0, fixture.startColumn, 'a'));
		assertFalse((Boolean) method.invoke(fixture.sheet, 0, fixture.durationColumn, '3'));
		assertTrue((Boolean) method.invoke(fixture.sheet, 0, fixture.startColumn, '3'));
	}

	private static SpreadsheetFixture createFixture() throws Exception {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("date-typing-test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);

		NormalTask task = project.createScriptedTask();
		task.setName("Date typing");
		task.setStart(DateTime.calendarInstance(2026, Calendar.JUNE, 6).getTimeInMillis());
		project.connectTask(task);
		project.getTaskOutlines().addToAll(task, null);

		final SpreadsheetFixture[] fixtureRef = new SpreadsheetFixture[1];
		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet sheet = new SpreadSheet();
			sheet.setSpreadSheetCategory(SpreadSheetCategories.taskSpreadsheetCategory);
			NodeModelCache cache = NodeModelCacheFactory.getInstance().createFilteredCache(
				NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel()),
				"date-typing-test",
				null);
			SpreadSheetUtils.setFieldsAndContext(
				sheet,
				cache,
				SpreadSheetCategories.taskSpreadsheetCategory,
				"Spreadsheet.Task.entry",
				true);
			fixtureRef[0] = new SpreadsheetFixture(sheet);
		});
		return fixtureRef[0];
	}

	private static final class SpreadsheetFixture {
		private final SpreadSheet sheet;
		private final int startColumn;
		private final int durationColumn;

		private SpreadsheetFixture(SpreadSheet sheet) {
			this.sheet = sheet;
			this.startColumn = findColumnByFieldId(sheet, "Field.start");
			this.durationColumn = findColumnByFieldId(sheet, "Field.duration");
		}

		private static int findColumnByFieldId(SpreadSheet sheet, String fieldId) {
			SpreadSheetModel model = (SpreadSheetModel) sheet.getModel();
			for (int column = 0; column < model.getColumnCount(); column++) {
				com.microproject.field.Field field = model.getFieldInColumn(column);
				if (field != null && fieldId.equals(field.getId())) {
					return column;
				}
			}
			throw new IllegalArgumentException("Missing field: " + fieldId);
		}
	}
}
