/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.microproject.command.UpdateProjectCommand;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.grouping.core.Node;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.undo.EditSupport;
import javax.swing.undo.UndoManager;

class UpdateProjectRequestTest {
	@Test
	void requestIsImmutableAndRejectsInvalidDate() {
		UpdateProjectRequest request = new UpdateProjectRequest(42L, true, false, true);
		assertEquals(42L, request.statusDate());
		assertThrows(IllegalArgumentException.class,
			() -> new UpdateProjectRequest(0L, false, false, false));
	}

	@Test
	void commandUsesTypedRequestAndReportsOnlyTasksActuallyChanged() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("update", undo), undo);
		project.initialize(false, false);
		NormalTask task = project.createScriptedTask();
		UpdateProjectRequest request = new UpdateProjectRequest(task.getEnd() + 86_400_000L,
			true, false, false);
		long originalStatusDate = project.getStatusDate();
		UpdateProjectCommand command = new UpdateProjectCommand(project, request);
		assertSame(project, command.getDocument());
		EditSupport editSupport = new EditSupport();
		UndoManager undoManager = new UndoManager();
		editSupport.addEditListener(undoManager::addEdit);
		command.execute(java.util.List.of(task), editSupport);
		assertTrue(command.affectedTaskIds().contains(task.getUniqueId()));
		assertEquals(originalStatusDate, project.getStatusDate(), "the command date must not overwrite Status Date");
		assertNotEquals(0D, task.getPercentComplete());
		undoManager.undo();
		assertEquals(0D, task.getPercentComplete(), "one undo restores task progress");
		assertEquals(originalStatusDate, project.getStatusDate());
		undoManager.redo();
		assertNotEquals(0D, task.getPercentComplete(), "one redo reapplies task progress");
		assertEquals(originalStatusDate, project.getStatusDate());
	}

	@Test
	void selectedAndEntireProjectUpdatesRespectLinkedSummaryHierarchyAndUndo() throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("update-hierarchy", undo), undo);
		project.initialize(false, false);
		Node summaryNode = project.createLocalTaskNode(null);
		Task summary = (Task) summaryNode.getImpl();
		Task predecessor = (Task) project.createLocalTaskNode(summaryNode).getImpl();
		Task successor = (Task) project.createLocalTaskNode(summaryNode).getImpl();
		DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.FS, 0L, this);
		project.recalculate();
		long originalStatusDate = project.getStatusDate();
		long updateDate = successor.getEnd() + 2L * 86_400_000L;
		TaskSnapshotValues predecessorBeforeSelected = capture(predecessor);
		TaskSnapshotValues successorBeforeSelected = capture(successor);
		TaskSnapshotValues summaryBeforeSelected = capture(summary);
		UpdateProjectCommand selected = new UpdateProjectCommand(project,
			new UpdateProjectRequest(updateDate, true, false, false));
		EditSupport edits = new EditSupport();
		UndoManager manager = new UndoManager();
		edits.addEditListener(manager::addEdit);

		selected.execute(java.util.List.of(predecessor), edits);
		assertEquals(java.util.List.of(predecessor.getUniqueId()), selected.affectedTaskIds(),
			"Selected Tasks scope must not update a linked but unselected successor or its summary row");
		assertEquals(1D, predecessor.getPercentComplete(), 0.00001D);
		assertEquals(0D, successor.getPercentComplete(), 0.00001D);
		assertEquals(originalStatusDate, project.getStatusDate(), "through-date is not the project Status Date");
		TaskSnapshotValues predecessorAfterSelected = capture(predecessor);
		TaskSnapshotValues successorAfterSelected = capture(successor);
		TaskSnapshotValues summaryAfterSelected = capture(summary);
		manager.undo();
		assertEquals(0D, predecessor.getPercentComplete(), 0.00001D);
		assertEquals(0D, successor.getPercentComplete(), 0.00001D);
		assertTaskValues(predecessorBeforeSelected, predecessor);
		assertTaskValues(successorBeforeSelected, successor);
		assertTaskValues(summaryBeforeSelected, summary);
		manager.redo();
		assertEquals(1D, predecessor.getPercentComplete(), 0.00001D);
		assertEquals(0D, successor.getPercentComplete(), 0.00001D);
		assertTaskValues(predecessorAfterSelected, predecessor);
		assertTaskValues(successorAfterSelected, successor);
		assertTaskValues(summaryAfterSelected, summary);
		for (int cycle = 0; cycle < 3; cycle++) {
			manager.undo();
			assertTaskValues(predecessorBeforeSelected, predecessor);
			assertTaskValues(successorBeforeSelected, successor);
			assertTaskValues(summaryBeforeSelected, summary);
			manager.redo();
			assertTaskValues(predecessorAfterSelected, predecessor);
			assertTaskValues(successorAfterSelected, successor);
			assertTaskValues(summaryAfterSelected, summary);
		}
		manager.undo();
		project.recalculate();
		assertTaskValues(predecessorBeforeSelected, predecessor);
		assertTaskValues(successorBeforeSelected, successor);
		assertTaskValues(summaryBeforeSelected, summary);
		manager.redo();
		project.recalculate();
		assertTaskValues(predecessorAfterSelected, predecessor);
		assertTaskValues(successorAfterSelected, successor);
		assertTaskValues(summaryAfterSelected, summary);

		TaskSnapshotValues predecessorBeforeEntire = capture(predecessor);
		TaskSnapshotValues successorBeforeEntire = capture(successor);
		TaskSnapshotValues summaryBeforeEntire = capture(summary);
		UpdateProjectCommand entire = new UpdateProjectCommand(project,
			new UpdateProjectRequest(updateDate, true, false, false));
		entire.execute(project.getTaskList(), edits);
		assertEquals(java.util.Set.of(predecessor.getUniqueId(), successor.getUniqueId()),
			new java.util.HashSet<>(entire.affectedTaskIds()),
			"Entire Project updates both linked leaf tasks while summary completion remains derived");
		assertEquals(1D, predecessor.getPercentComplete(), 0.00001D);
		assertEquals(1D, successor.getPercentComplete(), 0.00001D);
		assertEquals(1D, summary.getPercentComplete(), 0.00001D);
		assertEquals(originalStatusDate, project.getStatusDate());
		TaskSnapshotValues predecessorAfterEntire = capture(predecessor);
		TaskSnapshotValues successorAfterEntire = capture(successor);
		TaskSnapshotValues summaryAfterEntire = capture(summary);
		manager.undo();
		assertEquals(0D, successor.getPercentComplete(), 0.00001D,
			"one Undo restores only the second transaction and all dependent task state");
		assertEquals(1D, predecessor.getPercentComplete());
		assertTaskValues(predecessorBeforeEntire, predecessor);
		assertTaskValues(successorBeforeEntire, successor);
		assertTaskValues(summaryBeforeEntire, summary);
		manager.redo();
		assertEquals(1D, successor.getPercentComplete());
		assertTaskValues(predecessorAfterEntire, predecessor);
		assertTaskValues(successorAfterEntire, successor);
		assertTaskValues(summaryAfterEntire, summary);
		assertEquals(originalStatusDate, project.getStatusDate());
		for (int cycle = 0; cycle < 3; cycle++) {
			manager.undo();
			assertTaskValues(predecessorBeforeEntire, predecessor);
			assertTaskValues(successorBeforeEntire, successor);
			assertTaskValues(summaryBeforeEntire, summary);
			manager.redo();
			assertTaskValues(predecessorAfterEntire, predecessor);
			assertTaskValues(successorAfterEntire, successor);
			assertTaskValues(summaryAfterEntire, summary);
		}
		manager.undo();
		project.recalculate();
		assertTaskValues(predecessorBeforeEntire, predecessor);
		assertTaskValues(successorBeforeEntire, successor);
		assertTaskValues(summaryBeforeEntire, summary);
		manager.redo();
		project.recalculate();
		assertTaskValues(predecessorAfterEntire, predecessor);
		assertTaskValues(successorAfterEntire, successor);
		assertTaskValues(summaryAfterEntire, summary);
	}

	@Test
	void partialUpdateFailureRestoresTaskValuesAndDoesNotPublishUndo() throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("update-rollback", undo), undo);
		project.initialize(false, false);
		Node summaryNode = project.createLocalTaskNode(null);
		Task summary = (Task) summaryNode.getImpl();
		NormalTask first = (NormalTask) project.createLocalTaskNode(summaryNode).getImpl();
		NormalTask second = (NormalTask) project.createLocalTaskNode(summaryNode).getImpl();
		DependencyService.getInstance().newDependency(first, second, DependencyType.FS, 0L, this);
		project.recalculate();
		first.setDirty(false);
		second.setDirty(false);
		project.setDirty(false);
		project.setGroupDirty(false);
		long updateDate = Math.max(first.getEnd(), second.getEnd()) + 86_400_000L;
		TaskSnapshotValues firstBefore = capture(first);
		TaskSnapshotValues secondBefore = capture(second);
		TaskSnapshotValues summaryBefore = capture(summary);
		Double firstOverride = first.getImportedPercentCompleteOverride();
		Double secondOverride = second.getImportedPercentCompleteOverride();
		boolean projectDirty = project.isDirty();
		boolean projectGroupDirty = project.isGroupDirty();
		EditSupport editSupport = new EditSupport();
		UndoManager undoManager = new UndoManager();
		editSupport.addEditListener(undoManager::addEdit);
		UpdateProjectCommand command = new UpdateProjectCommand(project,
			new UpdateProjectRequest(updateDate, true, false, false)) {
			@Override protected void applyToTask(Task task) {
				super.applyToTask(task);
				if (task == second)
					throw new IllegalStateException("injected failure after second task mutation");
			}
		};

		IllegalStateException failure = assertThrows(IllegalStateException.class,
			() -> command.execute(java.util.List.of(first, second), editSupport));

		assertTrue(failure.getCause() instanceof com.microproject.transaction.ModelTransaction.ModelTransactionException);
		assertEquals(com.microproject.transaction.ModelTransaction.Stage.APPLY,
			((com.microproject.transaction.ModelTransaction.ModelTransactionException) failure.getCause()).failedStage());
		assertTaskValues(firstBefore, first);
		assertTaskValues(secondBefore, second);
		assertTaskValues(summaryBefore, summary);
		assertEquals(firstOverride, first.getImportedPercentCompleteOverride());
		assertEquals(secondOverride, second.getImportedPercentCompleteOverride());
		assertEquals(projectDirty, project.isDirty());
		assertEquals(projectGroupDirty, project.isGroupDirty());
		assertTrue(command.affectedTaskIds().isEmpty());
		assertNull(first.getImportedPercentCompleteOverride());
		assertNull(second.getImportedPercentCompleteOverride());
		assertFalse(undoManager.canUndo(), "failed mutations must not publish an Undo edit");
	}

	private static TaskSnapshotValues capture(Task task) {
		NormalTask normal = task instanceof NormalTask normalTask ? normalTask : null;
		return new TaskSnapshotValues(task.getStart(), task.getEnd(), task.getDuration(), task.getStop(),
			task.getWindowEarlyStart(), task.getWindowEarlyFinish(), task.getWindowLateStart(), task.getWindowLateFinish(),
			task.getActualStart(), task.getActualFinish(), task.getActualDuration(),
			normal == null ? null : Long.valueOf(normal.getActualWork(null)),
			normal == null ? null : Long.valueOf(normal.getRemainingWork()), task.getPercentComplete(), task.isDirty());
	}

	private static void assertTaskValues(TaskSnapshotValues expected, Task task) {
		assertEquals(expected.start(), task.getStart());
		assertEquals(expected.end(), task.getEnd());
		assertEquals(expected.duration(), task.getDuration());
		assertEquals(expected.stop(), task.getStop());
		assertEquals(expected.windowEarlyStart(), task.getWindowEarlyStart());
		assertEquals(expected.windowEarlyFinish(), task.getWindowEarlyFinish());
		assertEquals(expected.windowLateStart(), task.getWindowLateStart());
		assertEquals(expected.windowLateFinish(), task.getWindowLateFinish());
		assertEquals(expected.actualStart(), task.getActualStart());
		assertEquals(expected.actualFinish(), task.getActualFinish());
		assertEquals(expected.actualDuration(), task.getActualDuration());
		if (expected.actualWork() != null) {
			NormalTask normal = (NormalTask) task;
			assertEquals(expected.actualWork(), Long.valueOf(normal.getActualWork(null)));
			assertEquals(expected.remainingWork(), Long.valueOf(normal.getRemainingWork()));
		}
		assertEquals(expected.percentComplete(), task.getPercentComplete(), 0.00001D);
		assertEquals(expected.dirty(), task.isDirty());
	}

	private record TaskSnapshotValues(long start, long end, long duration, long stop,
		long windowEarlyStart, long windowEarlyFinish, long windowLateStart, long windowLateFinish,
		long actualStart,
		long actualFinish, long actualDuration, Long actualWork, Long remainingWork,
		double percentComplete, boolean dirty) { }

	@Test
	void rejectsForeignTaskBeforeMutatingAnyProjectTask() {
		DataFactoryUndoController firstUndo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("update-owner", firstUndo), firstUndo);
		project.initialize(false, false);
		NormalTask local = project.createScriptedTask();
		DataFactoryUndoController secondUndo = new DataFactoryUndoController();
		Project otherProject = Project.createProject(ResourcePool.createRourcePool("update-foreign", secondUndo), secondUndo);
		otherProject.initialize(false, false);
		NormalTask foreign = otherProject.createScriptedTask();
		local.setDirty(false);
		foreign.setDirty(false);
		double localProgress = local.getPercentComplete();
		double foreignProgress = foreign.getPercentComplete();
		EditSupport editSupport = new EditSupport();
		UndoManager undoManager = new UndoManager();
		editSupport.addEditListener(undoManager::addEdit);
		UpdateProjectCommand command = new UpdateProjectCommand(project,
			new UpdateProjectRequest(Math.max(local.getEnd(), foreign.getEnd()) + 86_400_000L,
				true, false, false));

		IllegalStateException failure = assertThrows(IllegalStateException.class,
			() -> command.execute(java.util.List.of(local, foreign), editSupport));

		assertEquals(com.microproject.transaction.ModelTransaction.Stage.VALIDATE,
			((com.microproject.transaction.ModelTransaction.ModelTransactionException) failure.getCause()).failedStage());
		assertEquals(localProgress, local.getPercentComplete(), 0.00001D);
		assertEquals(foreignProgress, foreign.getPercentComplete(), 0.00001D);
		assertFalse(local.isDirty());
		assertFalse(foreign.isDirty());
		assertTrue(command.affectedTaskIds().isEmpty());
		assertFalse(undoManager.canUndo());
	}

	@Test
	void undoPublicationFailureIsReportedWithoutRevertingPossiblyPublishedEdit() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("update-publish-failure", undo), undo);
		project.initialize(false, false);
		NormalTask task = project.createScriptedTask();
		EditSupport editSupport = new EditSupport();
		UndoManager undoManager = new UndoManager();
		editSupport.addEditListener(undoManager::addEdit);
		editSupport.addEditListener(edit -> { throw new IllegalStateException("injected listener failure"); });
		UpdateProjectCommand command = new UpdateProjectCommand(project,
			new UpdateProjectRequest(task.getEnd() + 86_400_000L, true, false, false));

		IllegalStateException failure = assertThrows(IllegalStateException.class,
			() -> command.execute(java.util.List.of(task), editSupport));

		assertEquals(com.microproject.transaction.ModelTransaction.Stage.COMMIT,
			((com.microproject.transaction.ModelTransaction.ModelTransactionException) failure.getCause()).failedStage());
		assertEquals(1D, task.getPercentComplete(), 0.00001D);
		assertTrue(command.affectedTaskIds().contains(task.getUniqueId()));
		assertTrue(undoManager.canUndo(), "an earlier listener may already have accepted the edit");
	}
}
