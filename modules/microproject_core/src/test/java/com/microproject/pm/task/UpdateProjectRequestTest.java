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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import com.microproject.command.UpdateProjectCommand;
import com.microproject.datatype.Duration;
import com.microproject.field.FieldContext;
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
		AtomicInteger publishedScheduleEvents = new AtomicInteger();
		AtomicLong revisionObservedByScheduleListener = new AtomicLong(-1L);
		project.addScheduleListener(event -> {
			publishedScheduleEvents.incrementAndGet();
			revisionObservedByScheduleListener.set(project.getMutationState().revision());
		});
		long revisionBeforeUpdate = project.getMutationState().revision();
		command.execute(java.util.List.of(task), editSupport);
		assertEquals(revisionBeforeUpdate + 1, project.getMutationState().revision(),
			"one committed Update Project publishes one project revision");
		assertEquals(revisionBeforeUpdate + 1, revisionObservedByScheduleListener.get(),
			"schedule observers see the committed revision before publication");
		assertTrue(publishedScheduleEvents.get() > 0, "a committed Update Project publishes its schedule changes");
		assertTrue(command.affectedTaskIds().contains(task.getUniqueId()));
		assertEquals(originalStatusDate, project.getStatusDate(), "the command date must not overwrite Status Date");
		assertNotEquals(0D, task.getPercentComplete());
		undoManager.undo();
		assertEquals(revisionBeforeUpdate + 2, project.getMutationState().revision(),
			"Undo publishes one additional project revision");
		assertEquals(revisionBeforeUpdate + 2, revisionObservedByScheduleListener.get());
		assertEquals(0D, task.getPercentComplete(), "one undo restores task progress");
		assertEquals(originalStatusDate, project.getStatusDate());
		undoManager.redo();
		assertEquals(revisionBeforeUpdate + 3, project.getMutationState().revision(),
			"Redo publishes one additional project revision");
		assertEquals(revisionBeforeUpdate + 3, revisionObservedByScheduleListener.get());
		assertNotEquals(0D, task.getPercentComplete(), "one redo reapplies task progress");
		assertEquals(originalStatusDate, project.getStatusDate());
	}

	/**
	 * Document-derived acceptance decision (not an empirical MSP run): the Update
	 * Work command records a task complete through its scheduled finish; a separate
	 * project Status Date remains unchanged. Microsoft sources:
	 * https://support.microsoft.com/en-us/project/update-work-on-a-project-to-show-actual-progress
	 * https://support.microsoft.com/en-us/project/project-status-date
	 * https://support.microsoft.com/en-us/project/actual-finish-task-field
	 */
	@Test
	void completingPartiallyWorkedAssignedTaskSetsExactActualsAndUndoRestoresThem() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("update-actuals", undo), undo);
		project.initialize(false, false);
		NormalTask task = project.createScriptedTask();
		task.setDuration(com.microproject.options.CalendarOption.getInstance().getMillisPerDay() * 4L);
		var resource = project.getResourcePool().createScriptedResource();
		var assignment = com.microproject.pm.assignment.AssignmentService.getInstance()
			.newAssignment(task, resource, 1.0d, 0L, this);
		assignment.setWork(com.microproject.options.CalendarOption.getInstance().getMillisPerDay() * 4L, null);
		project.recalculate();
		task.setPercentComplete(0.5d);
		project.recalculate();
		project.setStatusDate(task.getStart());
		long fixedStatusDate = project.getStatusDate();
		long updateDate = task.getEnd() + 86_400_000L;
		TaskSnapshotValues before = capture(task);
		long assignmentActualWorkBefore = assignment.getActualWork(null);
		long assignmentRemainingWorkBefore = assignment.getRemainingWork();
		assertTrue(assignmentActualWorkBefore > 0L);
		assertTrue(assignmentRemainingWorkBefore > 0L);

		EditSupport edits = new EditSupport();
		UndoManager manager = new UndoManager();
		edits.addEditListener(manager::addEdit);
		new UpdateProjectCommand(project, new UpdateProjectRequest(updateDate, true, false, false))
			.execute(java.util.List.of(task), edits);
		assertEquals(1D, task.getPercentComplete(), 0.00001D);
		assertEquals(task.getStart(), task.getActualStart());
		assertEquals(task.getEnd(), task.getActualFinish());
		assertEquals(task.getWork(null), task.getActualWork(null));
		assertEquals(0L, task.getRemainingWork());
		assertEquals(1D, assignment.getPercentComplete(), 0.00001D);
		assertEquals(task.getActualStart(), assignment.getActualStart());
		assertEquals(task.getActualFinish(), assignment.getActualFinish());
		assertEquals(assignment.getWork(null), assignment.getActualWork(null));
		assertEquals(0L, assignment.getRemainingWork());
		assertEquals(task.getActualWork(null), assignment.getActualWork(null),
			"single-assignment task actual work must reconcile with its assignment total");
		assertEquals(fixedStatusDate, project.getStatusDate(), "through-date must not replace project Status Date");
		TaskSnapshotValues complete = capture(task);

		manager.undo();
		assertTaskValues(before, task);
		assertEquals(assignmentActualWorkBefore, assignment.getActualWork(null));
		assertEquals(assignmentRemainingWorkBefore, assignment.getRemainingWork());
		manager.redo();
		assertTaskValues(complete, task);
		assertEquals(task.getActualWork(null), assignment.getActualWork(null));
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
	void explicitTaskSheetProgressRollsUpByTaskDurationInLinkedSummary() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("task-progress-summary", undo), undo);
		project.initialize(false, false);
		Node summaryNode = project.createLocalTaskNode(null);
		Task summary = (Task) summaryNode.getImpl();
		Task predecessor = (Task) project.createLocalTaskNode(summaryNode).getImpl();
		Task successor = (Task) project.createLocalTaskNode(summaryNode).getImpl();
		long day = com.microproject.options.CalendarOption.getInstance().getMillisPerDay();
		predecessor.setDuration(3L * day);
		successor.setDuration(day);
		FieldContext context = new FieldContext();
		context.setTaskSheetUpdate(true);

		assertTrue(TaskSheetScheduleWorkflow.applyTaskPercentComplete(predecessor,
			TaskSheetScheduleWorkflow.PERCENT_COMPLETE_FIELD_ID, context, Double.valueOf(0.25D)));
		assertTrue(TaskSheetScheduleWorkflow.applyTaskPercentComplete(successor,
			TaskSheetScheduleWorkflow.PERCENT_COMPLETE_FIELD_ID, context, Double.valueOf(1.0D)));

		assertEquals(0.25D, predecessor.getPercentComplete(), 0.00001D);
		assertEquals(1.0D, successor.getPercentComplete(), 0.00001D);
		double planned = Duration.millis(predecessor.getDuration()) + Duration.millis(successor.getDuration());
		double expected = (Duration.millis(predecessor.getDuration()) * predecessor.getPercentComplete()
			+ Duration.millis(successor.getDuration()) * successor.getPercentComplete()) / planned;
		assertEquals(expected, summary.getPercentComplete(), 0.00001D,
			"an explicit task-duration percentage must roll up over the linked summary using task duration");
		assertEquals(0.4375D, summary.getPercentComplete(), 0.00001D,
			"three days at 25% plus one day at 100% must weight to 43.75%");
	}

	@Test
	void nonFiniteTaskSheetProgressIsRejectedBeforeScheduleMutation() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("invalid-task-progress", undo), undo);
		project.initialize(false, false);
		NormalTask task = (NormalTask) project.createLocalTaskNode(null).getImpl();
		long day = com.microproject.options.CalendarOption.getInstance().getMillisPerDay();
		task.setDuration(4L * day);
		FieldContext context = new FieldContext();
		context.setTaskSheetUpdate(true);
		assertTrue(TaskSheetScheduleWorkflow.applyTaskPercentComplete(task,
			TaskSheetScheduleWorkflow.PERCENT_COMPLETE_FIELD_ID, context, Double.valueOf(0.25D)));
		long actualDuration = task.getActualDuration();

		assertThrows(IllegalArgumentException.class, () -> TaskSheetScheduleWorkflow.applyTaskPercentComplete(task,
			TaskSheetScheduleWorkflow.PERCENT_COMPLETE_FIELD_ID, context, Double.valueOf(Double.NaN)));
		assertEquals(0.25D, task.getPercentComplete(), 0.00001D);
		assertEquals(actualDuration, task.getActualDuration(),
			"invalid task progress must not partially mutate the schedule before rejection");
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
		long revisionBeforeFailure = project.getMutationState().revision();
		EditSupport editSupport = new EditSupport();
		UndoManager undoManager = new UndoManager();
		editSupport.addEditListener(undoManager::addEdit);
		AtomicInteger publishedScheduleEvents = new AtomicInteger();
		AtomicInteger publishedObjectEvents = new AtomicInteger();
		project.addScheduleListener(event -> publishedScheduleEvents.incrementAndGet());
		project.addObjectListener(event -> publishedObjectEvents.incrementAndGet());
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
		assertEquals(0, publishedScheduleEvents.get(),
			"a rolled-back Update Project must not publish transient schedule state");
		assertEquals(0, publishedObjectEvents.get(),
			"a rolled-back Update Project must not publish transient object state");
		assertEquals(revisionBeforeFailure, project.getMutationState().revision(),
			"a rolled-back update must not advance the project revision");
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
		AtomicInteger publishedScheduleEvents = new AtomicInteger();
		project.addScheduleListener(event -> publishedScheduleEvents.incrementAndGet());
		long revisionBeforeCommitFailure = project.getMutationState().revision();
		UpdateProjectCommand command = new UpdateProjectCommand(project,
			new UpdateProjectRequest(task.getEnd() + 86_400_000L, true, false, false));

		IllegalStateException failure = assertThrows(IllegalStateException.class,
			() -> command.execute(java.util.List.of(task), editSupport));

		assertEquals(com.microproject.transaction.ModelTransaction.Stage.COMMIT,
			((com.microproject.transaction.ModelTransaction.ModelTransactionException) failure.getCause()).failedStage());
		assertEquals(1D, task.getPercentComplete(), 0.00001D);
		assertTrue(command.affectedTaskIds().contains(task.getUniqueId()));
		assertTrue(undoManager.canUndo(), "an earlier listener may already have accepted the edit");
		assertTrue(publishedScheduleEvents.get() > 0,
			"a commit-stage publication failure must still flush the committed model's schedule events");
		assertEquals(revisionBeforeCommitFailure + 1, project.getMutationState().revision(),
			"a commit publication failure does not roll back the applied model revision");
	}
}
