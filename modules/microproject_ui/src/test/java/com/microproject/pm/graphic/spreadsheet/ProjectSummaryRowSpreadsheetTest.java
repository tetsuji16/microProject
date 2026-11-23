/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.graphic.spreadsheet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.grouping.core.Node;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.graphic.model.cache.ReferenceNodeModelCache;
import com.microproject.pm.graphic.model.transform.NodeCacheTransformer;
import com.microproject.graphic.configuration.SpreadSheetCategories;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

class ProjectSummaryRowSpreadsheetTest {

	@Test
	void onlyProjectNameIsEditableAndOneUndoRestoresIt() throws Exception {
		var setup = new AtomicReference<Fixture>();
		SwingUtilities.invokeAndWait(() -> setup.set(createFixture()));
		Fixture fixture = setup.get();
		var model = (SpreadSheetModel) fixture.sheet().getModel();
		int row = model.findGraphicNodeRow(fixture.summaryRow());
		int nameColumn = findColumn(model, "Field.name");
		int otherColumn = findColumn(model, "Field.start");
		assertTrue(model.isCellEditable(row, nameColumn));
		assertFalse(model.isCellEditable(row, otherColumn));
		assertEquals(new java.util.Date(fixture.project().getStart()), model.getValueAt(row, otherColumn));

		SwingUtilities.invokeAndWait(() -> model.setValueAt("Renamed project", row, nameColumn));
		assertEquals("Renamed project", fixture.project().getName());
		SwingUtilities.invokeAndWait(() -> fixture.project().getUndoController().undo());
		assertEquals("Summary Sheet Test", fixture.project().getName());
	}

	@Test
	void projectSummaryRowCannotBeDeletedOrCutFromTaskSheet() throws Exception {
		var setup = new AtomicReference<Fixture>();
		SwingUtilities.invokeAndWait(() -> setup.set(createFixture()));
		Fixture fixture = setup.get();		var model = (SpreadSheetModel) fixture.sheet().getModel();
		int row = model.findGraphicNodeRow(fixture.summaryRow());
		SwingUtilities.invokeAndWait(() -> fixture.sheet().setRowSelectionInterval(row, row));
		assertTrue(fixture.sheet().getSelectedNodes().stream().anyMatch(node -> node.getImpl() instanceof Project));
		assertTrue(fixture.sheet().getSelectedDeletableRows().isEmpty());
		assertTrue(fixture.sheet().getSelectedCuttableRows(fixture.sheet().getSelectedNodes()).isEmpty());
		int taskCount = fixture.project().getTaskList().size();
	SwingUtilities.invokeAndWait(() -> fixture.sheet().executeAction(
			com.microproject.menu.MenuActionConstants.ACTION_INDENT, new int[] { row }));
		assertEquals(taskCount, fixture.project().getTaskList().size(), "indent must ignore the whole-project row");
	}

	private Fixture createFixture() {
		var undo = new DataFactoryUndoController();
		var pool = ResourcePool.createRourcePool("summary-sheet-test", undo);
		Project project = Project.createProject(pool, undo);
		project.initialize(false, false);
		project.setName("Summary Sheet Test");
		NormalTask task = project.newNormalTaskInstance();
		task.setName("One task");
		project.getTaskOutlines().addToAll(task, null);
		ReferenceNodeModelCache reference = NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel());
		NodeModelCache cache = NodeModelCacheFactory.getInstance().createFilteredCache(reference, "summary-sheet-test", null);
		((NodeCacheTransformer) cache.getVisibleNodes().getTransformer()).setShowProjectSummaryTask(true);
		SpreadSheet sheet = new SpreadSheet();
		sheet.setSpreadSheetCategory(SpreadSheetCategories.taskSpreadsheetCategory);
		SpreadSheetUtils.setFieldsAndContext(sheet, cache, SpreadSheetCategories.taskSpreadsheetCategory,
			"Spreadsheet.Task.entry", true);
		GraphicNode summaryRow = cache.getVisibleNodes().getElements().stream()
			.filter(node -> node.getNode().getImpl() instanceof Project).findFirst().orElseThrow();
		return new Fixture(project, sheet, summaryRow);
	}

	private static int findColumn(SpreadSheetModel model, String fieldId) {
		for (int column = 1; column < model.getColumnCount(); column++)
			if (fieldId.equals(model.getFieldInColumn(column).getId())) return column;
		throw new IllegalArgumentException("Missing field " + fieldId);
	}

	private record Fixture(Project project, SpreadSheet sheet, GraphicNode summaryRow) { }
}
