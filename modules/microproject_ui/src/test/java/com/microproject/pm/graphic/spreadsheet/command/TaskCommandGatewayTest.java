/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.spreadsheet.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.atomic.AtomicReference;
import java.util.List;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.configuration.FieldDictionary;
import com.microproject.graphic.configuration.SpreadSheetCategories;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.graphic.model.cache.ProjectionRowKey;
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
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.scheduling.Schedule;
import com.microproject.undo.DataFactoryUndoController;

class TaskCommandGatewayTest {
	@Test
	void dependencyIntentUsesStableTaskRowsAndSharesUndoAndPersistencePath() throws Exception {
		Fixture fixture = createDependencyFixture();
		NormalTask successor = fixture.project().getTaskList().stream()
			.filter(task -> "Successor".equals(task.getName())).map(NormalTask.class::cast).findFirst().orElseThrow();
		NormalTask finalTask = fixture.project().getTaskList().stream()
			.filter(task -> "Final task".equals(task.getName())).map(NormalTask.class::cast).findFirst().orElseThrow();
		var projection = fixture.cache().getVisibleNodes().getProjectionIndex();
		List<ProjectionRowKey.TaskRow> rows = List.of(taskRowFor(projection, fixture.task()),
			taskRowFor(projection, successor), taskRowFor(projection, finalTask));
		TaskDependencyIntent link = new TaskDependencyIntent(TaskDependencyIntent.Operation.LINK, rows,
			projection.topologyRevision(), null);
		TaskDependencyIntent staleLink = new TaskDependencyIntent(TaskDependencyIntent.Operation.LINK, rows,
			projection.topologyRevision() + 1, null);
		AtomicReference<TaskCommandResult> stale = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> {
			try {
				stale.set(TaskCommandGateway.execute(fixture.sheet(), staleLink, this));
			} catch (Exception failure) {
				throw new RuntimeException(failure);
			}
		});
		assertEquals(TaskCommandResult.Status.STALE_PROJECTION, stale.get().status());
		assertTrue(successor.getPredecessorList().isEmpty(), "stale task rows must not create a dependency");
		AtomicReference<TaskCommandResult> linked = new AtomicReference<>();

		SwingUtilities.invokeAndWait(() -> {
			try {
				linked.set(TaskCommandGateway.execute(fixture.sheet(), link, this));
			} catch (Exception failure) {
				throw new RuntimeException(failure);
			}
		});
		assertEquals(TaskCommandResult.Status.CHANGED, linked.get().status());
		assertEquals(1, successor.getPredecessorList().size());
		assertEquals(1, finalTask.getPredecessorList().size());
		assertTrue(fixture.project().getUndoController().canUndo());
		AtomicReference<TaskCommandResult> unchanged = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> {
			try {
				unchanged.set(TaskCommandGateway.execute(fixture.sheet(), link, this));
			} catch (Exception failure) {
				throw new RuntimeException(failure);
			}
		});
		assertEquals(TaskCommandResult.Status.NO_CHANGE, unchanged.get().status(),
			"repeating the same link must not publish a second mutation");

		SwingUtilities.invokeAndWait(() -> fixture.project().getUndoController().undo());
		assertTrue(successor.getPredecessorList().isEmpty(), "one Undo must remove the created dependency");
		assertTrue(finalTask.getPredecessorList().isEmpty(), "one Undo must remove every dependency in the batch");
		SwingUtilities.invokeAndWait(() -> fixture.project().getUndoController().redo());
		assertEquals(1, successor.getPredecessorList().size(), "one Redo must restore the created dependency");
		assertEquals(1, finalTask.getPredecessorList().size(), "one Redo must restore every dependency in the batch");

		ByteArrayOutputStream saved = new ByteArrayOutputStream();
		MpoFileImporter serializer = new MpoFileImporter();
		assertTrue(serializer.saveProject(fixture.project(), saved));
		Project reopened = serializer.loadProject(new ByteArrayInputStream(saved.toByteArray()));
		assertEquals(1, reopened.getTaskList().stream().filter(task -> "Successor".equals(task.getName()))
			.findFirst().orElseThrow().getPredecessorList().size(), "linked dependency must survive MPO reload; ids="
				+ fixture.project().getTaskList().stream().map(task -> task.getName() + "=" + task.getUniqueId()).toList());
		assertEquals(1, reopened.getTaskList().stream().filter(task -> "Final task".equals(task.getName()))
			.findFirst().orElseThrow().getPredecessorList().size(), "every dependency in the batch must survive MPO reload");

		var currentProjection = fixture.cache().getVisibleNodes().getProjectionIndex();
		TaskDependencyIntent.DependencyTarget dependency = TaskDependencyIntent.DependencyTarget.from(
			(Dependency) successor.getPredecessorList().iterator().next());
		TaskDependencyIntent unlink = new TaskDependencyIntent(TaskDependencyIntent.Operation.UNLINK,
			List.of(taskRowFor(currentProjection, successor)), currentProjection.topologyRevision(), dependency);
		AtomicReference<TaskCommandResult> unlinked = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> {
			try {
				unlinked.set(TaskCommandGateway.execute(fixture.sheet(), unlink, this));
			} catch (Exception failure) {
				throw new RuntimeException(failure);
			}
		});
		assertEquals(TaskCommandResult.Status.CHANGED, unlinked.get().status());
		assertTrue(successor.getPredecessorList().isEmpty(), "stable endpoint keys must remove the selected dependency");
		assertEquals(1, finalTask.getPredecessorList().size(), "targeted unlink must leave the other batch dependency intact");
		SwingUtilities.invokeAndWait(() -> fixture.project().getUndoController().undo());
		assertEquals(1, successor.getPredecessorList().size(), "Undo must restore the specifically unlinked dependency");
		SwingUtilities.invokeAndWait(() -> fixture.project().getUndoController().redo());
		assertTrue(successor.getPredecessorList().isEmpty(), "Redo must remove the specifically unlinked dependency");
		SwingUtilities.invokeAndWait(() -> fixture.project().getUndoController().undo());

		TaskDependencyIntent unlinkAll = new TaskDependencyIntent(TaskDependencyIntent.Operation.UNLINK, rows,
			currentProjection.topologyRevision(), null);
		AtomicReference<TaskCommandResult> allUnlinked = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> {
			try {
				allUnlinked.set(TaskCommandGateway.execute(fixture.sheet(), unlinkAll, this));
			} catch (Exception failure) {
				throw new RuntimeException(failure);
			}
		});
		assertEquals(TaskCommandResult.Status.CHANGED, allUnlinked.get().status());
		assertTrue(successor.getPredecessorList().isEmpty());
		assertTrue(finalTask.getPredecessorList().isEmpty());
		SwingUtilities.invokeAndWait(() -> fixture.project().getUndoController().undo());
		assertEquals(1, successor.getPredecessorList().size(), "one Undo must restore every removed link");
		assertEquals(1, finalTask.getPredecessorList().size(), "one Undo must restore every removed link");
		SwingUtilities.invokeAndWait(() -> fixture.project().getUndoController().redo());
		assertTrue(successor.getPredecessorList().isEmpty());
		assertTrue(finalTask.getPredecessorList().isEmpty());
	}

	@Test
	void viewCacheDependencyCreationUsesTheStableTaskCommandGateway() throws Exception {
		Fixture fixture = createDependencyFixture();
		NormalTask successor = fixture.project().getTaskList().stream()
			.filter(task -> "Successor".equals(task.getName())).map(NormalTask.class::cast).findFirst().orElseThrow();
		RevisionedProjectionIndex projection = fixture.cache().getVisibleNodes().getProjectionIndex();
		var predecessorNode = projection.nodeAt(projection.rowForKey(taskRowFor(projection, fixture.task())));
		var successorNode = projection.nodeAt(projection.rowForKey(taskRowFor(projection, successor)));
		assertTrue(successor.getPredecessorList().isEmpty());

		SwingUtilities.invokeAndWait(() -> {
			try {
				fixture.cache().createDependency(predecessorNode, successorNode);
			} catch (Exception failure) {
				throw new RuntimeException(failure);
			}
		});

		assertEquals(1, successor.getPredecessorList().size(),
			"Gantt/Network cache route must share the canonical dependency mutation");
		SwingUtilities.invokeAndWait(() -> fixture.project().getUndoController().undo());
		assertTrue(successor.getPredecessorList().isEmpty());
		SwingUtilities.invokeAndWait(() -> fixture.project().getUndoController().redo());
		assertEquals(1, successor.getPredecessorList().size());
	}

	@Test
	void structuralTaskPasteUsesStableAnchorAndOneUndoAcrossSaveReload() throws Exception {
		Fixture fixture = createFixture();
		DataFactoryUndoController sourceUndo = new DataFactoryUndoController();
		Project sourceProject = Project.createProject(ResourcePool.createRourcePool("task-paste-source", sourceUndo), sourceUndo);
		sourceProject.initialize(false, false);
		NormalTask sourceTask = sourceProject.createScriptedTask();
		sourceTask.setName("Pasted");
		sourceProject.connectTask(sourceTask);
		sourceProject.getTaskOutlines().addToAll(sourceTask, null);
		List<Node> copiedRoots = sourceProject.getTaskModel().copy(
			List.of(sourceProject.getTaskModel().search(sourceTask)), NodeModel.SILENT);
		fixture.sheet().selectRowAndAllColumns(0);
		int taskCountBefore = fixture.project().getTaskList().size();
		ByteArrayOutputStream baseline = new ByteArrayOutputStream();
		MpoFileImporter serializer = new MpoFileImporter();
		assertTrue(serializer.saveProject(fixture.project(), baseline));
		int persistedTaskCountBefore = serializer.loadProject(new ByteArrayInputStream(baseline.toByteArray())).getTaskList().size();
		List<Long> existingTaskIds = fixture.project().getTaskList().stream().map(Task::getUniqueId).toList();
		boolean[] pasted = new boolean[1];

		SwingUtilities.invokeAndWait(() -> pasted[0] = fixture.sheet().pasteNodesFromClipboard(copiedRoots));
		assertTrue(pasted[0]);
		assertEquals(taskCountBefore + 1, fixture.project().getTaskList().size());
		assertTrue(fixture.project().getTaskList().stream().anyMatch(task -> "Pasted".equals(task.getName())));
		Task pastedTask = fixture.project().getTaskList().stream().filter(task -> "Pasted".equals(task.getName())).findFirst().orElseThrow();
		assertTrue(existingTaskIds.stream().noneMatch(id -> id == pastedTask.getUniqueId()),
			"pasted task identity must not collide with existing task identities");
		assertTrue(fixture.project().getUndoController().canUndo());

		SwingUtilities.invokeAndWait(() -> fixture.project().getUndoController().undo());
		assertEquals(taskCountBefore, fixture.project().getTaskList().size());
		assertNull(fixture.project().getTaskModel().search(copiedRoots.getFirst().getImpl()));
		SwingUtilities.invokeAndWait(() -> fixture.project().getUndoController().redo());
		assertEquals(taskCountBefore + 1, fixture.project().getTaskList().size());

		ByteArrayOutputStream saved = new ByteArrayOutputStream();
		assertTrue(serializer.saveProject(fixture.project(), saved), "pasted task batch must serialize successfully");
		Project reopened = serializer.loadProject(new ByteArrayInputStream(saved.toByteArray()));
		assertEquals(persistedTaskCountBefore + 1, reopened.getTaskList().size(),
			"paste must add one persisted task even when the fixture contains duplicate legacy entries");
		assertTrue(reopened.getTaskList().stream().anyMatch(task -> "Pasted".equals(task.getName())));
	}

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
	void ganttScheduleIntentRejectsStaleProjectionAndScheduleBeforeInvokingMutation() throws Exception {
		Fixture fixture = createFixture();
		RevisionedProjectionIndex projection = fixture.cache().getVisibleNodes().getProjectionIndex();
		Schedule schedule = (Schedule)fixture.task();
		ProjectionRowKey.TaskRow row = taskRowFor(projection, fixture.task());
		TaskScheduleEditIntent current = new TaskScheduleEditIntent(row, projection.topologyRevision(),
			TaskScheduleEditIntent.Operation.PROGRESS, schedule.getStart(), schedule.getEnd(),
			schedule.getCompletedThrough(), schedule.getStart(), schedule.getEnd(), fixture.task().getConstraintType(),
			fixture.task().getConstraintDate(), schedule.getStart(), schedule.getEnd(),
			schedule.getCompletedThrough() + 1);
		TaskScheduleEditIntent staleProjection = new TaskScheduleEditIntent(row, projection.topologyRevision() + 1,
			current.operation(), current.expectedScheduleStart(), current.expectedScheduleEnd(),
			current.expectedCompletedThrough(), current.expectedIntervalStart(), current.expectedIntervalEnd(),
			current.expectedConstraintType(), current.expectedConstraintDate(), current.requestedStart(),
			current.requestedEnd(), current.requestedValue());
		TaskScheduleEditIntent staleSchedule = new TaskScheduleEditIntent(row, projection.topologyRevision(),
			current.operation(), current.expectedScheduleStart() + 1, current.expectedScheduleEnd(),
			current.expectedCompletedThrough(), current.expectedIntervalStart(), current.expectedIntervalEnd(),
			current.expectedConstraintType(), current.expectedConstraintDate(), current.requestedStart(),
			current.requestedEnd(), current.requestedValue());
		TaskScheduleEditIntent noOp = new TaskScheduleEditIntent(row, projection.topologyRevision(),
			current.operation(), current.expectedScheduleStart(), current.expectedScheduleEnd(),
			current.expectedCompletedThrough(), current.expectedIntervalStart(), current.expectedIntervalEnd(),
			current.expectedConstraintType(), current.expectedConstraintDate(), current.expectedIntervalStart(),
			current.expectedIntervalEnd(), current.expectedCompletedThrough());
		TaskScheduleEditIntent staleSplitInterval = new TaskScheduleEditIntent(row, projection.topologyRevision(),
			TaskScheduleEditIntent.Operation.SPLIT, current.expectedScheduleStart(), current.expectedScheduleEnd(),
			current.expectedCompletedThrough(), Long.MIN_VALUE, Long.MAX_VALUE, current.expectedConstraintType(),
			current.expectedConstraintDate(), Long.MIN_VALUE, Long.MAX_VALUE, 1L);
		AtomicReference<TaskCommandResult> staleProjectionResult = new AtomicReference<>();
		AtomicReference<TaskCommandResult> staleScheduleResult = new AtomicReference<>();
		AtomicReference<TaskCommandResult> noOpResult = new AtomicReference<>();
		AtomicReference<TaskCommandResult> staleSplitResult = new AtomicReference<>();
		AtomicReference<Boolean> mutationCalled = new AtomicReference<>(false);
		SwingUtilities.invokeAndWait(() -> {
			staleProjectionResult.set(TaskCommandGateway.executeScheduleEdit(fixture.cache(), staleProjection,
				fixture.sheet(), () -> { mutationCalled.set(true); return true; }));
			staleScheduleResult.set(TaskCommandGateway.executeScheduleEdit(fixture.cache(), staleSchedule,
				fixture.sheet(), () -> { mutationCalled.set(true); return true; }));
			noOpResult.set(TaskCommandGateway.executeScheduleEdit(fixture.cache(), noOp,
				fixture.sheet(), () -> { mutationCalled.set(true); return true; }));
			staleSplitResult.set(TaskCommandGateway.executeScheduleEdit(fixture.cache(), staleSplitInterval,
				fixture.sheet(), () -> { mutationCalled.set(true); return true; }));
		});
		assertEquals(TaskCommandResult.Status.STALE_PROJECTION, staleProjectionResult.get().status());
		assertEquals(TaskCommandResult.Status.STALE_VALUE, staleScheduleResult.get().status());
		assertEquals(TaskCommandResult.Status.NO_CHANGE, noOpResult.get().status());
		assertEquals(TaskCommandResult.Status.STALE_VALUE, staleSplitResult.get().status(),
			"split must reject a captured interval that no longer exists");
		assertFalse(mutationCalled.get(), "a stale gesture must not enter the scheduling mutation");

		AtomicReference<TaskCommandResult> currentResult = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> currentResult.set(TaskCommandGateway.executeScheduleEdit(fixture.cache(), current,
			fixture.sheet(), () -> { mutationCalled.set(true); return true; })));
		assertEquals(TaskCommandResult.Status.CHANGED, currentResult.get().status());
		assertTrue(mutationCalled.get(), "a current, validated gesture must reach the single mutation callback");
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
		return new Fixture(project, task, cache, (SpreadSheetModel) sheet[0].getModel(), sheet[0],
			ProjectTaskKey.from(task).orElseThrow());
	}

	private Fixture createDependencyFixture() throws Exception {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("task-dependency-edit", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		NormalTask predecessor = project.createScriptedTask();
		predecessor.setName("Before");
		NormalTask successor = project.createScriptedTask();
		successor.setName("Successor");
		NormalTask finalTask = project.createScriptedTask();
		finalTask.setName("Final task");
		NodeModelCache cache = NodeModelCacheFactory.getInstance().createFilteredCache(
			NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel()), "task-dependency-edit", null);
		cache.update();
		SpreadSheet[] sheet = new SpreadSheet[1];
		SwingUtilities.invokeAndWait(() -> {
			sheet[0] = new SpreadSheet();
			sheet[0].setSpreadSheetCategory(SpreadSheetCategories.taskSpreadsheetCategory);
			SpreadSheetUtils.setFieldsAndContext(sheet[0], cache, SpreadSheetCategories.taskSpreadsheetCategory,
				"Spreadsheet.Task.entry", true);
		});
		return new Fixture(project, predecessor, cache, (SpreadSheetModel) sheet[0].getModel(), sheet[0],
			ProjectTaskKey.from(predecessor).orElseThrow());
	}

	private static ProjectionRowKey.TaskRow taskRowFor(RevisionedProjectionIndex projection, Task task) {
		for (int row = 0; row < projection.size(); row++) {
			if (projection.nodeAt(row).getNode().getImpl() == task
					&& projection.keyAt(row) instanceof ProjectionRowKey.TaskRow taskRow)
				return taskRow;
		}
		throw new AssertionError("Task is absent from the current visible projection: " + task.getName());
	}

	private record Fixture(Project project, NormalTask task, NodeModelCache cache, SpreadSheetModel model, SpreadSheet sheet,
			ProjectTaskKey taskKey) { }
}
