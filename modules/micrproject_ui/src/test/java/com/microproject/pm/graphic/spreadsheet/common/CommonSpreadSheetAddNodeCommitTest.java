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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.MouseEvent;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.graphic.configuration.SpreadSheetCategories;
import com.microproject.grouping.core.Node;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetUtils;
import com.microproject.pm.graphic.spreadsheet.renderer.NameCellComponent;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

class CommonSpreadSheetAddNodeCommitTest {
	@Test
	void addNodeForImplCommitsActiveCellEditBeforeInsertingNode() throws Exception {
		final DataFactoryUndoController undoController = new DataFactoryUndoController();
		final Project project = createProject(undoController);
		final NormalTask originalTask = createTask(project, "Original");
		final Node[] insertedRef = new Node[1];
		final int[] taskCountBefore = new int[] { project.getTasks().size() };
		final int[] taskCountAfter = new int[1];

		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet sheet = new SpreadSheet();
			sheet.setSpreadSheetCategory(SpreadSheetCategories.taskSpreadsheetCategory);
			var reference = NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel());
			reference.setTaskCommandGateway(new com.microproject.application.task.TaskCommandGateway(project));
			NodeModelCache cache = NodeModelCacheFactory.getInstance().createFilteredCache(
				reference,
				"add-node-test",
				null);
			SpreadSheetUtils.setFieldsAndContext(sheet,
				cache,
				SpreadSheetCategories.taskSpreadsheetCategory,
				"Spreadsheet.Task.entry",
				true);
			sheet.setRowSelectionInterval(0, 0);
			sheet.setColumnSelectionInterval(1, 1);
			assertTrue(sheet.editCellAt(0, 1, new MouseEvent(sheet, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 0, 0, 2, false)));
			JTextField editor = (JTextField) ((NameCellComponent) sheet.getEditorComponent()).getTextComponent();
			editor.setText("Edited");
			insertedRef[0] = sheet.addNodeForImpl(project.createScriptedTask());
			taskCountAfter[0] = project.getTasks().size();
		});

		assertEquals("Edited", originalTask.getName());
		assertNotNull(insertedRef[0]);
		assertEquals(taskCountBefore[0] + 1, taskCountAfter[0]);

		SwingUtilities.invokeAndWait(undoController::undo);

		assertEquals(taskCountBefore[0], project.getTasks().size());
	}

	private Project createProject(DataFactoryUndoController undoController) {
		ResourcePool resourcePool = ResourcePool.createRourcePool("add-node-test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		return project;
	}

	private NormalTask createTask(Project project, String name) {
		NormalTask task = project.createScriptedTask();
		task.setName(name);
		project.connectTask(task);
		project.getTaskOutlines().addToAll(task, null);
		return task;
	}
}
