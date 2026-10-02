/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.spreadsheet.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.configuration.FieldDictionary;
import com.microproject.graphic.configuration.SpreadSheetCategories;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.graphic.model.cache.RevisionedProjectionIndex;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetModel;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetUtils;
import com.microproject.exchange.MpoFileImporter;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.ProjectTaskKey;
import com.microproject.pm.task.Task;
import com.microproject.undo.DataFactoryUndoController;

class TaskCommandGatewayTest {
	@Test
	void spreadsheetModelRoutesTaskFieldInputThroughStableIntent() throws Exception {
		Fixture fixture = createFixture();
		var projection = fixture.cache().getVisibleNodes().getProjectionIndex();
		int row = projection.rowForKey(new com.microproject.pm.graphic.model.cache.ProjectionRowKey.TaskRow(
			fixture.taskKey(), 0));
		int nameColumn = -1;
		for (int column = 1; column < fixture.model().getColumnCount(); column++) {
			if ("Field.name".equals(fixture.model().getFieldInColumn(column).getId())) {
				nameColumn = column;
				break;
			}
		}
		if (row < 0 || nameColumn < 0)
			throw new AssertionError("task projection or name field is missing");
		int committedRow = row;
		int committedColumn = nameColumn;

		SwingUtilities.invokeAndWait(() -> fixture.model().setValueAt("After", committedRow, committedColumn));

		assertEquals("After", fixture.task().getName());
		assertTrue(fixture.project().getUndoController().canUndo(), "task field command must preserve the canonical Undo edit");
		ByteArrayOutputStream saved = new ByteArrayOutputStream();
		MpoFileImporter serializer = new MpoFileImporter();
		assertTrue(serializer.saveProject(fixture.project(), saved), "edited task must serialize successfully");
		Project reopened = serializer.loadProject(new ByteArrayInputStream(saved.toByteArray()));
		assertTrue(reopened.getTaskList().stream().map(Task::getName).anyMatch("After"::equals),
			"task field intent value must survive native project serialization");
		SwingUtilities.invokeAndWait(() -> fixture.project().getUndoController().undo());
		assertEquals("Before", fixture.task().getName(), "one Undo must restore the pre-edit task value");
	}

	@Test
	void stableKeyEditUpdatesTheIntendedTaskAndNoOpDoesNotCreateAnotherMutation() throws Exception {
		Fixture fixture = createFixture();
		var projection = fixture.cache().getVisibleNodes().getProjectionIndex();
		TaskFieldEditIntent intent = new TaskFieldEditIntent(fixture.taskKey(), 0,
			projection.topologyRevision(), FieldDictionary.getInstance().getFieldFromId("Field.name"),
			"Before", "After");

		AtomicReference<TaskCommandResult> changed = new AtomicReference<>();
		AtomicReference<TaskCommandResult> unchanged = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> {
			try {
				changed.set(TaskCommandGateway.execute(fixture.model(), intent, this));
				unchanged.set(TaskCommandGateway.execute(fixture.model(),
					new TaskFieldEditIntent(fixture.taskKey(), 0, projection.topologyRevision(), intent.field(), "After", "After"), this));
			} catch (Exception failure) {
				throw new RuntimeException(failure);
			}
		});

		assertEquals(TaskCommandResult.Status.CHANGED, changed.get().status());
		assertEquals(TaskCommandResult.Status.NO_CHANGE, unchanged.get().status());
		assertEquals("After", fixture.task().getName());
		SwingUtilities.invokeAndWait(() -> fixture.project().getUndoController().undo());
		assertEquals("Before", fixture.task().getName(), "no-op must not add a second Undo edit");
	}

	@Test
	void staleProjectionAndStaleValueAreRejectedWithoutChangingTheTask() throws Exception {
		Fixture fixture = createFixture();
		var projection = fixture.cache().getVisibleNodes().getProjectionIndex();
		var name = FieldDictionary.getInstance().getFieldFromId("Field.name");

		TaskCommandResult staleProjection = TaskCommandGateway.execute(fixture.model(),
			new TaskFieldEditIntent(fixture.taskKey(), 0, projection.topologyRevision() + 1, name, "Before", "Wrong"), this);
		TaskCommandResult staleValue = TaskCommandGateway.execute(fixture.model(),
			new TaskFieldEditIntent(fixture.taskKey(), 0, projection.topologyRevision(), name, "Other", "Wrong"), this);

		assertEquals(TaskCommandResult.Status.STALE_PROJECTION, staleProjection.status());
		assertEquals(TaskCommandResult.Status.STALE_VALUE, staleValue.status());
		assertEquals("Before", fixture.task().getName());
	}

	@Test
	void keyThatDoesNotBelongToTheCurrentProjectionIsPruned() throws Exception {
		Fixture fixture = createFixture();
		RevisionedProjectionIndex projection = fixture.cache().getVisibleNodes().getProjectionIndex();
		TaskCommandResult result = TaskCommandGateway.execute(fixture.model(),
			new TaskFieldEditIntent(new ProjectTaskKey(fixture.project().getUniqueId(), fixture.task().getUniqueId() + 1000),
				0, projection.topologyRevision(), FieldDictionary.getInstance().getFieldFromId("Field.name"), "Before", "Wrong"), this);

		assertEquals(TaskCommandResult.Status.MISSING_TASK, result.status());
		assertEquals("Before", fixture.task().getName());
	}

	private Fixture createFixture() throws Exception {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("task-field-edit", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		NormalTask task = project.createScriptedTask();
		task.setName("Before");
		project.connectTask(task);
		project.getTaskOutlines().addToAll(task, null);
		NodeModelCache cache = NodeModelCacheFactory.getInstance().createFilteredCache(
			NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel()), "task-field-edit", null);
		cache.update();
		SpreadSheet[] sheet = new SpreadSheet[1];
		SwingUtilities.invokeAndWait(() -> {
			sheet[0] = new SpreadSheet();
			sheet[0].setSpreadSheetCategory(SpreadSheetCategories.taskSpreadsheetCategory);
			SpreadSheetUtils.setFieldsAndContext(sheet[0], cache, SpreadSheetCategories.taskSpreadsheetCategory,
				"Spreadsheet.Task.entry", true);
		});
		return new Fixture(project, task, cache, (SpreadSheetModel) sheet[0].getModel(),
			ProjectTaskKey.from(task).orElseThrow());
	}

	private record Fixture(Project project, NormalTask task, NodeModelCache cache, SpreadSheetModel model,
			ProjectTaskKey taskKey) { }
}
