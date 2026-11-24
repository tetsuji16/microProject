/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.ui.view.spreadsheet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.configuration.FieldDictionary;
import com.microproject.datatype.Rate;
import com.microproject.field.FieldContext;
import com.microproject.view.configuration.SpreadSheetFieldArray;
import com.microproject.ui.view.model.cache.GraphicNode;
import com.microproject.ui.view.model.cache.NodeModelCache;
import com.microproject.ui.view.model.cache.NodeModelCacheFactory;
import com.microproject.ui.view.model.cache.ReferenceNodeModelCache;
import com.microproject.ui.view.model.transform.NodeCacheTransformer;
import com.microproject.view.configuration.SpreadSheetCategories;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.assignment.AssignmentService;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

class ProjectSummaryRowSpreadsheetTest {

	@Test
	void projectAggregateFieldsShowActualNonzeroProjectValues() throws Exception {
		var setup = new AtomicReference<Fixture>();
		SwingUtilities.invokeAndWait(() -> {
			Fixture fixture = createFixture();
			NormalTask task = (NormalTask) fixture.project().getTaskList().get(0);
			ResourceImpl resource = (ResourceImpl) fixture.project().getResourcePool().createScriptedResource();
			resource.setName("Summary resource");
			resource.setStandardRate(new Rate(125.0D));
			var assignment = AssignmentService.getInstance().newAssignment(task, resource, 1.0D, 0L, this);
			assignment.setWork(8L * 60L * 60L * 1000L, null);
			task.setPercentComplete(0.5D);
			setup.set(fixture);
		});
		Fixture fixture = setup.get();
		var displayed = new AtomicReference<Object[]>();
		SwingUtilities.invokeAndWait(() -> {
			var fields = new SpreadSheetFieldArray();
			fields.addField("Field.name");
			fields.addField("Field.work");
			fields.addField("Field.cost");
			fields.addField("Field.percentComplete");
			fixture.sheet().setCache(fixture.cache(), fields, fields.getCellStyle(), fields.getActionList());
			FieldContext context = new FieldContext();
			context.setLeftAssociation(true);
			context.setTaskSheetUpdate(true);
			var model = (SpreadSheetModel) fixture.sheet().getModel();
			model.setFieldContext(context);
			model.getCache().update();
			int row = 0; // The synthetic project row is first after the spreadsheet cache is rebound.
			assertTrue(model.getNode(row).getNode().getImpl() instanceof Project);
			displayed.set(new Object[] { context, model.getValueAt(row, findColumn(model, "Field.work")),
				model.getValueAt(row, findColumn(model, "Field.cost")),
				model.getValueAt(row, findColumn(model, "Field.percentComplete")) });
		});
		FieldContext context = (FieldContext) displayed.get()[0];
		Object displayedWork = displayed.get()[1];
		Object displayedCost = displayed.get()[2];
		Object displayedProgress = displayed.get()[3];
		assertTrue(fixture.project().getWork(context) > 0L);
		assertTrue(fixture.project().getCost(context) > 0.0D);
		assertTrue(fixture.project().getPercentComplete() > 0.0D);
		assertEquals(FieldDictionary.getInstance().getFieldFromId("Field.work").getValue(fixture.project(), context), displayedWork);
		assertEquals(FieldDictionary.getInstance().getFieldFromId("Field.cost").getValue(fixture.project(), context), displayedCost);
		assertEquals(FieldDictionary.getInstance().getFieldFromId("Field.percentComplete").getValue(fixture.project(), context), displayedProgress);
		assertNotNull(displayedWork, "Project Work column must show the aggregate work value");
		assertNotNull(displayedCost, "Project Cost column must show the aggregate cost value");
		assertNotNull(displayedProgress, "Project % Complete column must show the aggregate progress value");
	}

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
		for (int column = 1; column < model.getColumnCount(); column++)
			if (column != nameColumn) assertFalse(model.isCellEditable(row, column),
				"only the name column may be edited: " + model.getFieldInColumn(column).getId());
		assertEquals(new java.util.Date(fixture.project().getStart()), model.getValueAt(row, otherColumn));
		for (int column = 1; column < model.getColumnCount(); column++)
			model.getValueAt(row, column); // every default task-sheet column must tolerate the synthetic Project row
		assertEquals(fixture.project().getName(), model.getValueAt(row, findColumn(model, "Field.name")));
		assertEquals(new java.util.Date(fixture.project().getEnd()), model.getValueAt(row, findColumn(model, "Field.finish")));
		assertEquals(fixture.project().getDuration(), ((com.microproject.datatype.Duration) model.getValueAt(row,
			findColumn(model, "Field.duration"))).getEncodedMillis());
		assertEquals(null, model.getValueAt(row, findColumn(model, "Field.percentWorkComplete")));
		assertEquals(null, model.getValueAt(row, findColumn(model, "Field.manuallyScheduled")));
		assertEquals(null, model.getValueAt(row, findColumn(model, "Field.resourceNames")));
		assertEquals(null, model.getValueAt(row, findColumn(model, "Field.predecessors")));

		SwingUtilities.invokeAndWait(() -> model.setValueAt("Renamed project", row, nameColumn));
		assertEquals("Renamed project", fixture.project().getName());
		assertTrue(fixture.project().isDirty(), "renaming the project must mark it dirty for saving");
		SwingUtilities.invokeAndWait(() -> fixture.project().getUndoController().undo());
		assertEquals("Summary Sheet Test", fixture.project().getName());
		SwingUtilities.invokeAndWait(() -> fixture.project().getUndoController().redo());
		assertEquals("Renamed project", fixture.project().getName());
		java.io.ByteArrayOutputStream saved = new java.io.ByteArrayOutputStream();
		assertTrue(new com.microproject.exchange.MpoFileImporter().saveProject(fixture.project(), saved));
		Project reopened = new com.microproject.exchange.MpoFileImporter().loadProject(
			new java.io.ByteArrayInputStream(saved.toByteArray()));
		assertNotNull(reopened);
		assertEquals("Renamed project", reopened.getName(), "the edited project name must survive MPO save/reload");
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
			com.microproject.ui.menu.MenuActionConstants.ACTION_INDENT, new int[] { row }));
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
		return new Fixture(project, sheet, cache, summaryRow);
	}

	private static int findColumn(SpreadSheetModel model, String fieldId) {
		for (int column = 1; column < model.getColumnCount(); column++)
			if (fieldId.equals(model.getFieldInColumn(column).getId())) return column;
		throw new IllegalArgumentException("Missing field " + fieldId);
	}

	private record Fixture(Project project, SpreadSheet sheet, NodeModelCache cache, GraphicNode summaryRow) { }
}
