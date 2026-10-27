/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.spreadsheet.command;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.List;

import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.table.TableRowSorter;

import org.junit.jupiter.api.Test;

import com.microproject.collaboration.CollaborationSession;
import com.microproject.document.ObjectEvent;
import com.microproject.configuration.FieldDictionary;
import com.microproject.datatype.Duration;
import com.microproject.datatype.PercentFormat;
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
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.AssignmentService;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.ProjectTaskKey;
import com.microproject.pm.task.Task;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.scheduling.Schedule;
import com.microproject.pm.scheduling.ScheduleEvent;
import com.microproject.pm.scheduling.ScheduleEventListener;
import com.microproject.pm.scheduling.ScheduleService;
import com.microproject.undo.DataFactoryUndoController;

class TaskCommandGatewayTest {
	@Test
	void restoringTaskSelectionConvertsCacheModelRowToSortedViewRow() throws Exception {
		Fixture fixture = createDependencyFixture();
		Task finalTask = fixture.project().getTaskList().stream()
			.filter(task -> "Final task".equals(task.getName())).findFirst().orElseThrow();
		Node finalNode = fixture.project().getTaskModel().search(finalTask);
		AtomicReference<Throwable> failure = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> {
			try {
				int nameColumn = -1;
				for (int column = 0; column < fixture.model().getColumnCount(); column++) {
					if ("Field.name".equals(fixture.model().getFieldInColumn(column).getId())) {
						nameColumn = column;
						break;
					}
				}
				assertTrue(nameColumn >= 0, "task sheet must expose its Name field");
				TableRowSorter<SpreadSheetModel> sorter = new TableRowSorter<>(fixture.model());
				sorter.setSortKeys(List.of(new javax.swing.RowSorter.SortKey(nameColumn, SortOrder.ASCENDING)));
				fixture.sheet().setRowSorter(sorter);
				int modelRow = fixture.model().findGraphicNodeRow(
					fixture.cache().getGraphicNode(finalNode));
				int expectedViewRow = sorter.convertRowIndexToView(modelRow);
				assertNotEquals(modelRow, expectedViewRow,
					"the fixture must distinguish transformed model coordinates from sorted view coordinates");
				fixture.sheet().restoreTaskRowSelection(List.of(finalNode));
				assertArrayEquals(new int[] { expectedViewRow }, fixture.sheet().getSelectedRows());
				assertEquals(modelRow, sorter.convertRowIndexToModel(fixture.sheet().getSelectedRow()),
					"the selected view row must still resolve to the requested task's model identity");
			} catch (Throwable thrown) {
				failure.set(thrown);
			}
		});
		if (failure.get() != null) throw new AssertionError("sorted task selection restoration failed", failure.get());
	}

	/**
	 * Microsoft defines task % Complete as Actual Duration / Duration; it is distinct
	 * from % Work Complete and an assignment's completion percentage. See
	 * https://support.microsoft.com/en-us/project/percent-complete-fields.
	 */
	@Test
	void sampleTaskPercentInputSurvivesAssignmentReconciliationUndoRedoAndMpoReload() throws Exception {
		MpoFileImporter serializer = new MpoFileImporter();
		Project project;
		try (InputStream sample = getClass().getResourceAsStream("ccpm-progress-sample.mpo")) {
			assertNotNull(sample, "the regression sample must be the pinned c573 MPO fixture");
			project = serializer.loadProject(sample);
		}
		NodeModelCache cache = NodeModelCacheFactory.getInstance().createFilteredCache(
			NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel()), "ccpm-percent-edit", null);
		cache.update();
		SpreadSheet[] sheet = new SpreadSheet[1];
		SwingUtilities.invokeAndWait(() -> {
			sheet[0] = new SpreadSheet();
			sheet[0].setSpreadSheetCategory(SpreadSheetCategories.taskSpreadsheetCategory);
			SpreadSheetUtils.setFieldsAndContext(sheet[0], cache, SpreadSheetCategories.taskSpreadsheetCategory,
				"Spreadsheet.Task.summary", true);
		});
		SpreadSheetModel model = (SpreadSheetModel) sheet[0].getModel();
		int percentColumn = -1;
		for (int column = 1; column < model.getColumnCount(); column++) {
			if ("Field.percentComplete".equals(model.getFieldInColumn(column).getId())) {
				percentColumn = column;
				break;
			}
		}
		assertTrue(percentColumn > 0, "the task sheet must expose Percent Complete");
		List<Task> leaves = project.getTaskList().stream().filter(task -> !task.isSummary()).map(Task.class::cast).toList();
		AtomicReference<Throwable> editFailure = new AtomicReference<>();
		int committedColumn = percentColumn;
		SwingUtilities.invokeAndWait(() -> {
			try {
				for (double target : new double[] { 0.0d, 0.25d }) {
					Object value = PercentFormat.getInstance().parseObject(target == 0.0d ? "0%" : "25%");
					for (Task task : leaves) {
						var projection = cache.getVisibleNodes().getProjectionIndex();
						int row = projection.rowForKey(taskRowFor(projection, task));
						model.setValueAt(value, row, committedColumn);
					}
				}
				for (Task task : leaves) {
					var projection = cache.getVisibleNodes().getProjectionIndex();
					int row = projection.rowForKey(taskRowFor(projection, task));
					assertEquals(0.25d, ((Number) model.getValueAt(row, committedColumn)).doubleValue(), 0.00001d,
						"the task-sheet model must render the entered Percent Complete for " + task.getName());
				}
			} catch (Throwable failure) {
				editFailure.set(failure);
			}
		});
		if (editFailure.get() != null) throw new AssertionError("canonical task-field edit failed", editFailure.get());
		for (Task task : leaves) {
			assertEquals(0.25d, task.getPercentComplete(), 0.00001d,
				"task Percent Complete must remain the entered duration percentage: " + task.getName());
		}
		assertProjectPercent(project, leaves);
		Task completion = leaves.stream().filter(task -> "プロジェクト完了".equals(task.getName())).findFirst().orElseThrow();
		SwingUtilities.invokeAndWait(() -> project.getUndoController().undo());
		assertEquals(0.0d, completion.getPercentComplete(), 0.00001d,
			"one Undo must restore the last task's previous Percent Complete value");
		assertProjectPercent(project, leaves);
		SwingUtilities.invokeAndWait(() -> project.getUndoController().redo());
		assertEquals(0.25d, completion.getPercentComplete(), 0.00001d,
			"one Redo must restore the edited task Percent Complete value");
		assertProjectPercent(project, leaves);
		ByteArrayOutputStream saved = new ByteArrayOutputStream();
		assertTrue(serializer.saveProject(project, saved), "edited progress must serialize as MPO");
		Project reopened = serializer.loadProject(new ByteArrayInputStream(saved.toByteArray()));
		for (Task task : reopened.getTaskList().stream().filter(task -> !task.isSummary()).toList()) {
			assertEquals(0.25d, task.getPercentComplete(), 0.00001d,
				"task Percent Complete must survive MPO reload: " + task.getName());
		}
		List<Task> reopenedLeaves = reopened.getTaskList().stream().filter(task -> !task.isSummary()).map(Task.class::cast).toList();
		assertProjectPercent(reopened, reopenedLeaves);
	}

	private static void assertProjectPercent(Project project, List<Task> leaves) {
		double completed = 0.0D;
		double planned = 0.0D;
		for (Task task : leaves) {
			double duration = Duration.millis(task.getDuration());
			completed += duration * task.getPercentComplete();
			planned += duration;
		}
		assertTrue(planned > 0.0D, "the sample summary must have planned leaf duration");
		assertEquals(completed / planned, project.getPercentComplete(), 0.00001D,
			"project summary Percent Complete must be weighted by task duration, not assignment calendar duration");
	}

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
	void taskPasteGatewayReturnsCommandAndStableSelectedAndAffectedIds() throws Exception {
		Fixture fixture = createFixture();
		DataFactoryUndoController sourceUndo = new DataFactoryUndoController();
		Project sourceProject = Project.createProject(ResourcePool.createRourcePool("task-paste-metadata", sourceUndo), sourceUndo);
		sourceProject.initialize(false, false);
		NormalTask sourceTask = sourceProject.createScriptedTask();
		sourceTask.setName("Metadata pasted");
		sourceProject.connectTask(sourceTask);
		sourceProject.getTaskOutlines().addToAll(sourceTask, null);
		List<Node> copiedRoots = sourceProject.getTaskModel().copy(
			List.of(sourceProject.getTaskModel().search(sourceTask)), NodeModel.SILENT);
		RevisionedProjectionIndex projection = fixture.cache().getVisibleNodes().getProjectionIndex();
		ProjectionRowKey.TaskRow selected = taskRowFor(projection, fixture.task());
		fixture.sheet().restoreTaskRowSelection(List.of(fixture.project().getTaskModel().search(fixture.task())));

		TaskCommandResult result = TaskCommandGateway.execute(fixture.sheet(), new TaskPasteIntent(copiedRoots,
			List.of(selected), projection.topologyRevision(), TaskPasteIntent.PASTE_INSERT));

		assertEquals(TaskCommandResult.Status.CHANGED, result.status());
		assertEquals(TaskPasteIntent.PASTE_INSERT, result.commandId());
		assertEquals(List.of(fixture.task().getUniqueId()), result.selectedTaskIds());
		Task pasted = fixture.project().getTaskList().stream()
			.filter(task -> "Metadata pasted".equals(task.getName())).findFirst().orElseThrow();
		assertEquals(List.of(pasted.getUniqueId()), result.affectedTaskIds());
	}

	@Test
	void spreadsheetModelRoutesTaskFieldInputThroughStableIntent() throws Exception {
		Fixture fixture = createFixture();
		List<TaskCommandResult> publishedOutcomes = new java.util.ArrayList<>();
		List<List<Long>> publishedSelectionSnapshots = new java.util.ArrayList<>();
		List<Long> selectedBeforeEdit = List.of(fixture.task().getUniqueId());
		fixture.model().setTaskFieldEditOutcomeHandler(new SpreadSheetModel.TaskFieldEditOutcomeHandler() {
			private int captureCount;

			@Override
			public SpreadSheetModel.TaskFieldEditOutcomeContext capture() {
				assertEquals(captureCount++ == 0 ? "Before" : "After", fixture.task().getName(),
					"field outcome selection must be captured before gateway mutation");
				return new SpreadSheetModel.TaskFieldEditOutcomeContext(selectedBeforeEdit);
			}

			@Override
			public void publish(TaskCommandResult result, Task task,
					SpreadSheetModel.TaskFieldEditOutcomeContext context) {
				assertEquals(fixture.task(), task, "field outcome must retain the edited task identity");
				publishedSelectionSnapshots.add(context.selectedTaskIds());
				publishedOutcomes.add(result);
			}
		});
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
		SwingUtilities.invokeAndWait(() -> fixture.model().setValueAt("After", committedRow, committedColumn));

		assertEquals("After", fixture.task().getName());
		assertEquals(List.of(TaskCommandResult.Status.CHANGED, TaskCommandResult.Status.NO_CHANGE),
			publishedOutcomes.stream().map(TaskCommandResult::status).toList(),
			"the shared UI outcome hook must publish both a mutation and a no-op from the gateway");
		assertEquals(List.of(selectedBeforeEdit, selectedBeforeEdit), publishedSelectionSnapshots,
			"field edit outcomes must retain the immutable selection snapshot captured before gateway execution");
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
		long revisionBefore = fixture.project().getMutationState().revision();
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
		assertEquals(revisionBefore + 1, fixture.project().getMutationState().revision(),
			"the changed edit advances one revision and the subsequent no-op does not");
		assertEquals("After", fixture.task().getName());
		SwingUtilities.invokeAndWait(() -> fixture.project().getUndoController().undo());
		assertEquals(revisionBefore + 2, fixture.project().getMutationState().revision(),
			"Undo must publish one outer revision even when the edit emits nested events");
		assertEquals("Before", fixture.task().getName(), "no-op must not add a second Undo edit");
		SwingUtilities.invokeAndWait(() -> fixture.project().getUndoController().redo());
		assertEquals(revisionBefore + 3, fixture.project().getMutationState().revision(),
			"Redo must publish one outer revision even when the edit emits nested events");
		assertEquals("After", fixture.task().getName());
	}

	@Test
	void scheduleApplyFailureRestoresProgressUndoAndObserversBeforeReleasingProjectBarrier() throws Exception {
		Fixture fixture = createFixture();
		long day = com.microproject.options.CalendarOption.getInstance().getMillisPerDay();
		fixture.task().setDuration(8L * day);
		ResourceImpl resource = fixture.project().getResourcePool().newResourceInstance();
		resource.setName("Schedule atomicity resource");
		AssignmentService.getInstance().newAssignment(fixture.task(), resource, 1.0d, 0L, this);
		Assignment assignment = (Assignment) fixture.task().getAssignments().iterator().next();
		SwingUtilities.invokeAndWait(() -> fixture.cache().update());

		var nameField = FieldDictionary.getInstance().getFieldFromId("Field.name");
		RevisionedProjectionIndex seedProjection = fixture.cache().getVisibleNodes().getProjectionIndex();
		AtomicReference<TaskCommandResult> seedEdit = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> {
			try {
				seedEdit.set(TaskCommandGateway.execute(fixture.model(), new TaskFieldEditIntent(fixture.taskKey(), 0,
					seedProjection.topologyRevision(), nameField, "Before", "Undo/Redo seed"), this));
			} catch (com.microproject.field.FieldParseException failure) {
				throw new RuntimeException(failure);
			}
		});
		assertEquals(TaskCommandResult.Status.CHANGED, seedEdit.get().status());
		SwingUtilities.invokeAndWait(() -> fixture.project().getUndoController().undo());
		SwingUtilities.invokeAndWait(() -> fixture.cache().update());
		assertTrue(fixture.project().getUndoController().canUndo());
		assertTrue(fixture.project().getUndoController().canRedo(), "the failure must preserve a pre-existing Redo state");

		RevisionedProjectionIndex projection = fixture.cache().getVisibleNodes().getProjectionIndex();
		Schedule schedule = (Schedule) fixture.task();
		ProjectionRowKey.TaskRow row = taskRowFor(projection, fixture.task());
		long completedThrough = fixture.task().getEffectiveWorkCalendar().add(schedule.getStart(), 4L * day, false);
		long[] scheduleBefore = scheduleState(fixture.task());
		double progressBefore = fixture.task().getPercentComplete();
		long[] assignmentBefore = { assignment.getCompletedThrough(), assignment.getActualStart() };
		double assignmentProgressBefore = assignment.getPercentComplete();
		TaskScheduleEditIntent intent = new TaskScheduleEditIntent(row, projection.topologyRevision(),
			TaskScheduleEditIntent.Operation.PROGRESS, schedule.getStart(), schedule.getEnd(),
			schedule.getCompletedThrough(), schedule.getStart(), schedule.getEnd(), fixture.task().getConstraintType(),
			fixture.task().getConstraintDate(), schedule.getStart(), schedule.getEnd(), completedThrough);
		long revisionBefore = fixture.project().getMutationState().revision();
		List<String> undoNamesBefore = undoNames(fixture.project());
		boolean canUndoBefore = fixture.project().getUndoController().canUndo();
		boolean canRedoBefore = fixture.project().getUndoController().canRedo();
		String undoNameBefore = fixture.project().getUndoController().getUndoName();
		String redoNameBefore = fixture.project().getUndoController().getRedoName();
		int pendingEventsBefore = fixture.project().pendingEventCount();
		AtomicInteger publishedObjects = new AtomicInteger();
		AtomicInteger publishedSchedules = new AtomicInteger();
		AtomicReference<Boolean> mutationChanged = new AtomicReference<>(false);
		ObjectEvent.Listener objectListener = event -> publishedObjects.incrementAndGet();
		ScheduleEventListener scheduleListener = event -> publishedSchedules.incrementAndGet();
		fixture.project().addObjectListener(objectListener);
		fixture.project().addScheduleListener(scheduleListener);
		try {
			assertThrows(IllegalStateException.class, () -> TaskCommandGateway.executeScheduleEdit(fixture.cache(), intent,
				fixture.sheet(), () -> {
					mutationChanged.set(ScheduleService.getInstance().setCompleted(this, schedule, completedThrough,
						fixture.project().getUndoController().getEditSupport()));
					throw new IllegalStateException("injected after progress mutation");
				}));
			assertTrue(mutationChanged.get(), "the injected exception must follow a real progress mutation");
			assertArrayEquals(scheduleBefore, scheduleState(fixture.task()), "schedule rollback must restore task dates and completion");
			assertEquals(progressBefore, fixture.task().getPercentComplete(), 0.00001d,
				"schedule rollback must restore task progress");
			assertArrayEquals(assignmentBefore, new long[] { assignment.getCompletedThrough(), assignment.getActualStart() },
				"schedule rollback must restore assignment completion dates");
			assertEquals(assignmentProgressBefore, assignment.getPercentComplete(), 0.00001d,
				"schedule rollback must restore assignment progress");
			assertEquals(undoNamesBefore, undoNames(fixture.project()), "failed schedule edits must not append Undo history");
			assertEquals(canUndoBefore, fixture.project().getUndoController().canUndo());
			assertEquals(canRedoBefore, fixture.project().getUndoController().canRedo());
			assertEquals(undoNameBefore, fixture.project().getUndoController().getUndoName());
			assertEquals(redoNameBefore, fixture.project().getUndoController().getRedoName());
			assertEquals(revisionBefore, fixture.project().getMutationState().revision(), "failed schedule edits must not publish a revision");
			assertEquals(pendingEventsBefore, fixture.project().pendingEventCount(), "failed schedule edits must clear their deferred events");
			assertEquals(0, publishedObjects.get(), "failed schedule edits must not notify object observers");
			assertEquals(0, publishedSchedules.get(), "failed schedule edits must not notify schedule observers");
			CountDownLatch barrierAvailable = new CountDownLatch(1);
			Thread writer = new Thread(() -> {
				try (var ignored = fixture.project().getMutationState().begin()) { barrierAvailable.countDown(); }
			}, "gateway-failed-schedule-barrier-check");
			writer.setDaemon(true);
			writer.start();
			assertTrue(barrierAvailable.await(2, TimeUnit.SECONDS), "failed schedule edit must release the project barrier");
			writer.join(2_000);
			assertFalse(writer.isAlive(), "barrier probe must complete");
		} finally {
			fixture.project().removeObjectListener(objectListener);
			fixture.project().removeScheduleListener(scheduleListener);
		}
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
		fixture.task().setDuration(com.microproject.options.CalendarOption.getInstance().getMillisPerDay());
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
		long[] splitPointHolder = { Long.MIN_VALUE };
		schedule.consumeIntervals(interval -> {
			long candidate = Math.max(interval.getStart() + (interval.getEnd() - interval.getStart()) / 2L,
				current.expectedCompletedThrough());
			if (splitPointHolder[0] == Long.MIN_VALUE && candidate < interval.getEnd()) splitPointHolder[0] = candidate;
		});
		long splitPoint = splitPointHolder[0];
		assertTrue(splitPoint != Long.MIN_VALUE, "test fixture must expose a current work interval");
		TaskScheduleEditIntent splitPointWithinWork = new TaskScheduleEditIntent(row, projection.topologyRevision(),
			TaskScheduleEditIntent.Operation.SPLIT, current.expectedScheduleStart(), current.expectedScheduleEnd(),
			current.expectedCompletedThrough(), Long.MIN_VALUE, Long.MAX_VALUE, current.expectedConstraintType(),
			current.expectedConstraintDate(), Long.MIN_VALUE, Long.MAX_VALUE, splitPoint);
		AtomicReference<TaskCommandResult> staleProjectionResult = new AtomicReference<>();
		AtomicReference<TaskCommandResult> staleScheduleResult = new AtomicReference<>();
		AtomicReference<TaskCommandResult> noOpResult = new AtomicReference<>();
		AtomicReference<TaskCommandResult> staleSplitResult = new AtomicReference<>();
		AtomicReference<TaskCommandResult> splitPointResult = new AtomicReference<>();
		AtomicReference<Boolean> mutationCalled = new AtomicReference<>(false);
		AtomicReference<Boolean> splitMutationCalled = new AtomicReference<>(false);
		SwingUtilities.invokeAndWait(() -> {
			staleProjectionResult.set(TaskCommandGateway.executeScheduleEdit(fixture.cache(), staleProjection,
				fixture.sheet(), () -> { mutationCalled.set(true); return true; }));
			staleScheduleResult.set(TaskCommandGateway.executeScheduleEdit(fixture.cache(), staleSchedule,
				fixture.sheet(), () -> { mutationCalled.set(true); return true; }));
			noOpResult.set(TaskCommandGateway.executeScheduleEdit(fixture.cache(), noOp,
				fixture.sheet(), () -> { mutationCalled.set(true); return true; }));
			staleSplitResult.set(TaskCommandGateway.executeScheduleEdit(fixture.cache(), staleSplitInterval,
				fixture.sheet(), () -> { mutationCalled.set(true); return true; }));
			splitPointResult.set(TaskCommandGateway.executeScheduleEdit(fixture.cache(), splitPointWithinWork,
				fixture.sheet(), () -> { splitMutationCalled.set(true); return true; }));
		});
		assertEquals(TaskCommandResult.Status.STALE_PROJECTION, staleProjectionResult.get().status());
		assertEquals(TaskCommandResult.Status.STALE_VALUE, staleScheduleResult.get().status());
		assertEquals(TaskCommandResult.Status.NO_CHANGE, noOpResult.get().status());
		assertEquals(TaskCommandResult.Status.STALE_VALUE, staleSplitResult.get().status(),
			"split must reject a point outside the current work interval");
		assertEquals(TaskCommandResult.Status.CHANGED, splitPointResult.get().status(),
			"split must validate its actual point even when the rendered interval is visually expanded");
		assertTrue(splitMutationCalled.get(), "a current split point must reach the shared mutation callback");
		assertFalse(mutationCalled.get(), "a stale gesture must not enter the scheduling mutation");

		AtomicReference<TaskCommandResult> currentResult = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> currentResult.set(TaskCommandGateway.executeScheduleEdit(fixture.cache(), current,
			fixture.sheet(), () -> { mutationCalled.set(true); return true; })));
		assertEquals(TaskCommandResult.Status.CHANGED, currentResult.get().status());
		assertTrue(mutationCalled.get(), "a current, validated gesture must reach the single mutation callback");
	}

	@Test
	void ganttScheduleIntentRejectsReadOnlyProjectWithoutMutationOrUndoChange() throws Exception {
		Fixture fixture = createFixture();
		fixture.task().setDuration(com.microproject.options.CalendarOption.getInstance().getMillisPerDay());
		TaskScheduleEditIntent intent = currentProgressIntent(fixture);
		fixture.project().setReadOnly(true);
		boolean canUndoBefore = fixture.project().getUndoController().canUndo();
		String undoNameBefore = fixture.project().getUndoController().getUndoPresentationName();
		long[] scheduleBefore = scheduleState(fixture.task());
		AtomicReference<Boolean> mutationCalled = new AtomicReference<>(false);
		AtomicReference<TaskCommandResult> result = new AtomicReference<>();

		SwingUtilities.invokeAndWait(() -> result.set(TaskCommandGateway.executeScheduleEdit(fixture.cache(), intent,
			fixture.sheet(), () -> { mutationCalled.set(true); return true; })));

		assertEquals(TaskCommandResult.Status.REJECTED, result.get().status());
		assertFalse(mutationCalled.get(), "read-only rejection must happen before the mutation callback");
		assertArrayEquals(scheduleBefore, scheduleState(fixture.task()), "read-only rejection must preserve schedule and constraints");
		assertEquals(canUndoBefore, fixture.project().getUndoController().canUndo());
		assertEquals(undoNameBefore, fixture.project().getUndoController().getUndoPresentationName());
	}

	@Test
	void ganttScheduleIntentRejectsCollaborationLockWithoutMutationOrUndoChange() throws Exception {
		Fixture fixture = createFixture();
		fixture.task().setDuration(com.microproject.options.CalendarOption.getInstance().getMillisPerDay());
		TaskScheduleEditIntent intent = currentProgressIntent(fixture);
		fixture.project().setCollaborationSession(new RejectingCollaborationSession(fixture.project()));
		boolean canUndoBefore = fixture.project().getUndoController().canUndo();
		String undoNameBefore = fixture.project().getUndoController().getUndoPresentationName();
		long[] scheduleBefore = scheduleState(fixture.task());
		AtomicReference<Boolean> mutationCalled = new AtomicReference<>(false);
		AtomicReference<TaskCommandResult> result = new AtomicReference<>();

		SwingUtilities.invokeAndWait(() -> result.set(TaskCommandGateway.executeScheduleEdit(fixture.cache(), intent,
			fixture.sheet(), () -> { mutationCalled.set(true); return true; })));

		assertEquals(TaskCommandResult.Status.LOCKED, result.get().status());
		assertFalse(mutationCalled.get(), "lock rejection must happen before the mutation callback");
		assertArrayEquals(scheduleBefore, scheduleState(fixture.task()), "lock rejection must preserve schedule and constraints");
		assertEquals(canUndoBefore, fixture.project().getUndoController().canUndo());
		assertEquals(undoNameBefore, fixture.project().getUndoController().getUndoPresentationName());
	}

	private TaskScheduleEditIntent currentProgressIntent(Fixture fixture) {
		RevisionedProjectionIndex projection = fixture.cache().getVisibleNodes().getProjectionIndex();
		Schedule schedule = (Schedule) fixture.task();
		return new TaskScheduleEditIntent(taskRowFor(projection, fixture.task()), projection.topologyRevision(),
			TaskScheduleEditIntent.Operation.PROGRESS, schedule.getStart(), schedule.getEnd(),
			schedule.getCompletedThrough(), schedule.getStart(), schedule.getEnd(), fixture.task().getConstraintType(),
			fixture.task().getConstraintDate(), schedule.getStart(), schedule.getEnd(), schedule.getCompletedThrough() + 1);
	}

	private long[] scheduleState(Task task) {
		Schedule schedule = (Schedule) task;
		return new long[] { schedule.getStart(), schedule.getEnd(), schedule.getCompletedThrough(),
			task.getActualStart(), task.getConstraintType(), task.getConstraintDate() };
	}

	private List<String> undoNames(Project project) {
		List<String> editNames = project.getUndoController().getEditNames();
		return editNames == null ? List.of() : List.copyOf(editNames);
	}

	private static final class RejectingCollaborationSession extends CollaborationSession {
		private RejectingCollaborationSession(Project project) {
			super(project, new java.io.File(System.getProperty("java.io.tmpdir"),
				"microproject-schedule-lock-test.mpo").getAbsolutePath(), "test-user");
		}

		@Override
		public Task tryAcquireTasks(Iterable<Task> tasks) {
			return tasks.iterator().next();
		}

		@Override
		public String describeLockOwner(Task task) {
			return "test-user";
		}
	}

	@Test
	void keyThatDoesNotBelongToTheCurrentProjectionIsPruned() throws Exception {
		Fixture fixture = createFixture();
		RevisionedProjectionIndex projection = fixture.cache().getVisibleNodes().getProjectionIndex();
		TaskCommandResult result = TaskCommandGateway.execute(fixture.model(),
			new TaskFieldEditIntent(new ProjectTaskKey(fixture.project(), fixture.project().getUniqueId(), fixture.task().getUniqueId() + 1000),
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
