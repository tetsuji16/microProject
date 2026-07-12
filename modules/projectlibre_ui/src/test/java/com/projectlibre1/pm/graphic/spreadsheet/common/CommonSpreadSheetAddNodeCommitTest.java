package com.projectlibre1.pm.graphic.spreadsheet.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.MouseEvent;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.projectlibre1.graphic.configuration.SpreadSheetCategories;
import com.projectlibre1.grouping.core.Node;
import com.projectlibre1.pm.graphic.model.cache.NodeModelCache;
import com.projectlibre1.pm.graphic.model.cache.NodeModelCacheFactory;
import com.projectlibre1.pm.graphic.spreadsheet.SpreadSheet;
import com.projectlibre1.pm.graphic.spreadsheet.SpreadSheetUtils;
import com.projectlibre1.pm.graphic.spreadsheet.renderer.NameCellComponent;
import com.projectlibre1.pm.resource.ResourcePool;
import com.projectlibre1.pm.task.NormalTask;
import com.projectlibre1.pm.task.Project;
import com.projectlibre1.undo.DataFactoryUndoController;

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
			NodeModelCache cache = NodeModelCacheFactory.getInstance().createFilteredCache(
				NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel()),
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
