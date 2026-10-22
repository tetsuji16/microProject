/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.AssignmentService;
import com.microproject.util.DateTime;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.grouping.core.Node;

class TaskProgressServiceTest {
	@Test
	void markOnTrackRollsBackWholeBatchWhenLaterTaskUpdateFails() {
		assertProgressBatchFailure(true);
	}

	@Test
	void setPercentCompleteRollsBackWholeBatchWhenLaterTaskUpdateFails() {
		assertProgressBatchFailure(false);
	}

	private void assertProgressBatchFailure(boolean markOnTrack) {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("atomic-progress", undo), undo);
		project.initialize(false, false);
		NormalTask first = project.createScriptedTask();
		first.setImportedPercentComplete(0.25D);
		if (markOnTrack) project.setStatusDate(first.getEnd() + 7L * 86_400_000L);
		long firstStart = first.getStart();
		long firstEnd = first.getEnd();
		var firstAssignment = (Assignment) first.getAssignments().get(0);
		double assignmentPercent = firstAssignment.getPercentComplete();
		long assignmentWork = firstAssignment.getWork(null);
		long assignmentActualWork = firstAssignment.getActualWork(null);
		long assignmentRemainingWork = firstAssignment.getRemainingWork();
		var baselineValues = firstAssignment.getBaselineValues(0);
		Long assignmentBaselineWork = baselineValues == null ? null : baselineValues.work();
		double firstProgress = first.getPercentComplete();
		boolean firstDirty = first.isDirty();
		AtomicBoolean failOnce = new AtomicBoolean(false);
		RuntimeException injected = new IllegalStateException("injected later progress failure");
		NormalTask failing = new NormalTask(project) {
			@Override public void setImportedPercentComplete(double value) {
				super.setImportedPercentComplete(value);
				if (failOnce.compareAndSet(true, false)) throw injected;
			}
		};
		failing.setImportedPercentComplete(0.10D);
		failOnce.set(true);
		Assignment failingAssignment = (Assignment) failing.getAssignments().get(0);
		double failingAssignmentPercent = failingAssignment.getPercentComplete();
		long failingAssignmentWork = failingAssignment.getWork(null);
		long failingAssignmentActualWork = failingAssignment.getActualWork(null);
		long failingAssignmentRemainingWork = failingAssignment.getRemainingWork();
		var failingBaseline = failingAssignment.getBaselineValues(0);
		Long failingBaselineWork = failingBaseline == null ? null : failingBaseline.work();
		long failingStart = failing.getStart();
		long failingEnd = failing.getEnd();
		boolean failingDirty = failing.isDirty();
		first.setName("undo checkpoint");
		undo.undo();
		List<String> undoNames = undo.getEditNames();
		String redoName = undo.getRedoName();
		boolean canUndo = undo.canUndo();
		boolean canRedo = undo.canRedo();
		long revision = project.getMutationState().revision();
		boolean projectDirty = project.isDirty();
		AtomicInteger objectEvents = new AtomicInteger();
		AtomicInteger scheduleEvents = new AtomicInteger();
		project.addObjectListener(event -> objectEvents.incrementAndGet());
		project.addScheduleListener(event -> scheduleEvents.incrementAndGet());
		TaskProgressService service = new TaskProgressService();

		RuntimeException failure = assertThrows(RuntimeException.class, () -> {
			if (markOnTrack) service.markOnTrack(project, List.of(first, failing));
			else service.setPercentComplete(project, List.of(first, failing), 0.75D);
		});
		assertSame(injected, failure);
		assertEquals(firstProgress, first.getPercentComplete(), 0.00001D);
		assertEquals(0.10D, failing.getPercentComplete(), 0.00001D);
		assertEquals(firstStart, first.getStart());
		assertEquals(firstEnd, first.getEnd());
		assertEquals(failingStart, failing.getStart());
		assertEquals(failingEnd, failing.getEnd());
		assertEquals(firstDirty, first.isDirty());
		assertEquals(failingDirty, failing.isDirty());
		assertEquals(assignmentPercent, firstAssignment.getPercentComplete(), 0.00001D);
		assertEquals(assignmentWork, firstAssignment.getWork(null));
		assertEquals(assignmentActualWork, firstAssignment.getActualWork(null));
		assertEquals(assignmentRemainingWork, firstAssignment.getRemainingWork());
		var restoredBaseline = firstAssignment.getBaselineValues(0);
		assertEquals(assignmentBaselineWork, restoredBaseline == null ? null : restoredBaseline.work());
		assertEquals(failingAssignmentPercent, failingAssignment.getPercentComplete(), 0.00001D);
		assertEquals(failingAssignmentWork, failingAssignment.getWork(null));
		assertEquals(failingAssignmentActualWork, failingAssignment.getActualWork(null));
		assertEquals(failingAssignmentRemainingWork, failingAssignment.getRemainingWork());
		var restoredFailingBaseline = failingAssignment.getBaselineValues(0);
		assertEquals(failingBaselineWork, restoredFailingBaseline == null ? null : restoredFailingBaseline.work());
		assertEquals(projectDirty, project.isDirty());
		assertEquals(revision, project.getMutationState().revision());
		assertEquals(canUndo, undo.canUndo());
		assertEquals(canRedo, undo.canRedo());
		assertEquals(undoNames, undo.getEditNames());
		assertEquals(redoName, undo.getRedoName());
		assertEquals(0, objectEvents.get());
		assertEquals(0, scheduleEvents.get());
	}

	@Test
	void persistedStatusDateRestoresExactlyAndRejectsOutOfRangeValues() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("restore-status", undo), undo);
		project.initialize(false, false);
		project.setStatusDate(project.getStartDate() + 4L * 24L * 60L * 60L * 1000L);
		long persistedStatusDate = project.getStatusDate();

		project.restoreStatusDate(0L);
		assertFalse(project.isStatusDateSet());
		project.restoreStatusDate(persistedStatusDate);
		assertTrue(project.isStatusDateSet());
		assertEquals(persistedStatusDate, project.getStatusDate(),
			"Restoring persisted state must not normalize a canonical status date a second time");
		assertThrows(IllegalArgumentException.class, () -> project.restoreStatusDate(-1L));
		assertEquals(persistedStatusDate, project.getStatusDate(), "Invalid restore must leave state unchanged");
		long firstUnsupportedInstant = DateTime.midnightNextDay(DateTime.getMaxDate().getTime());
		assertThrows(IllegalArgumentException.class, () -> project.restoreStatusDate(firstUnsupportedInstant));
		assertEquals(persistedStatusDate, project.getStatusDate(), "Out-of-range restore must leave state unchanged");
	}

	@Test
	void statusDateAndMarkOnTrackAreUndoableAndBounded() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("progress", undo), undo);
		project.initialize(false, false);
		NormalTask task = project.createScriptedTask();
		// Project normalizes a status date to the last usable instant in its
		// calendar. Use a later date so the task is unambiguously on track even
		// when its finish falls on a non-working boundary.
		long date = task.getEnd() + 7L * 24L * 60L * 60L * 1000L;
		TaskProgressService service = new TaskProgressService();

		assertFalse(project.isStatusDateSet());
		ReversibleModelChange statusDateChange = service.setStatusDate(project, date).change();
		assertTrue(project.isStatusDateSet());
		long explicitStatusDate = project.getStatusDate();
		ReversibleModelChange progressChange = service.markOnTrack(project, List.of(task)).change();
		assertEquals(1D, task.getPercentComplete(), 0.00001D,
				() -> "status=" + project.getStatusDate() + " start=" + task.getStart() + " end=" + task.getEnd());
		progressChange.undo();
		assertEquals(0D, task.getPercentComplete(), 0.00001D);
		statusDateChange.undo();
		assertFalse(project.isStatusDateSet());
		statusDateChange.redo();
		assertTrue(project.isStatusDateSet());
		assertEquals(explicitStatusDate, project.getStatusDate());
		for (int cycle = 0; cycle < 3; cycle++) {
			statusDateChange.undo();
			assertFalse(project.isStatusDateSet());
			statusDateChange.redo();
			assertEquals(explicitStatusDate, project.getStatusDate(),
				"Undo/Redo must restore the exact explicit status date on every cycle");
		}
		progressChange.redo();
		assertEquals(1D, task.getPercentComplete(), 0.00001D);
	}

	@Test
	void setPercentCompleteIsUndoableBoundedAndSkipsSummaries() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("percent", undo), undo);
		project.initialize(false, false);
		NormalTask first = project.createScriptedTask();
		NormalTask second = project.createScriptedTask();
		TaskProgressService service = new TaskProgressService();

		for (double fraction : new double[] { 0.25D, 0.5D, 0.75D, 1D }) {
			TaskProgressService.Result result = service.setPercentComplete(project, List.of(first, second), fraction);
			assertEquals(2, result.changedCount());
			assertEquals(fraction, first.getPercentComplete(), 0.00001D);
			assertEquals(fraction, second.getPercentComplete(), 0.00001D);
			result.change().undo();
			assertEquals(0D, first.getPercentComplete(), 0.00001D);
			assertEquals(0D, second.getPercentComplete(), 0.00001D);
			result.change().redo();
			assertEquals(fraction, first.getPercentComplete(), 0.00001D);
			result.change().undo();
		}
		assertFalse(service.setPercentComplete(project, List.of(first), 0D).change().hasChanged(),
			"0% on an unstarted task is a no-op");
		assertThrows(IllegalArgumentException.class, () -> service.setPercentComplete(project, List.of(first), 1.01D));
		assertThrows(IllegalArgumentException.class, () -> service.setPercentComplete(project, List.of(first), -0.01D));
	}

	@Test
	void clearingStatusDateIsUndoableAndRestoresTheExplicitDate() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("clear-status", undo), undo);
		project.initialize(false, false);
		TaskProgressService service = new TaskProgressService();
		long date = project.getStartDate() + 3L * 24L * 60L * 60L * 1000L;
		service.setStatusDate(project, date);
		long explicit = project.getStatusDate();

		ReversibleModelChange change = service.clearStatusDate(project).change();
		assertFalse(project.isStatusDateSet());
		change.undo();
		assertTrue(project.isStatusDateSet());
		assertEquals(explicit, project.getStatusDate());
		change.redo();
		assertFalse(project.isStatusDateSet());
	}

	@Test
	void rejectsInvalidStatusDateAndClampsProgressAtScheduleBoundaries() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("boundaries", undo), undo);
		project.initialize(false, false);
		NormalTask task = project.createScriptedTask();
		TaskProgressService service = new TaskProgressService();
		assertThrows(IllegalArgumentException.class,
				() -> service.setStatusDate(project, 0L));

		service.setStatusDate(project, task.getStart() - 86_400_000L);
		service.markOnTrack(project, List.of(task));
		assertEquals(0D, task.getPercentComplete(), 0.00001D);
		service.setStatusDate(project, task.getEnd());
		service.markOnTrack(project, List.of(task));
		assertEquals(1D, task.getPercentComplete(), 0.00001D);
		NormalTask milestone = project.createScriptedTask();
		milestone.setDuration(0L);
		service.setStatusDate(project, milestone.getEnd() + 86_400_000L);
		service.markOnTrack(project, List.of(milestone));
		org.junit.jupiter.api.Assertions.assertTrue(milestone.getPercentComplete() >= 0D
				&& milestone.getPercentComplete() <= 1D,
				"milestone progress must remain within MSP bounds");
	}

	@Test
	void markOnTrackUsesWorkingCalendarInsteadOfElapsedWallClockTime() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("calendar-progress", undo), undo);
		project.initialize(false, false);
		NormalTask task = project.createScriptedTask();
		long start = task.getStart();
		long end = task.getEnd();
		task.setImportedPercentComplete(0.25D);
		long statusDate = start + (end - start) / 2L;
		project.setStatusDate(statusDate);
		long normalizedStatusDate = project.getStatusDate();
		long scheduledDuration = task.getEffectiveWorkCalendar().compare(end, start, false);
		long scheduledElapsed = task.getEffectiveWorkCalendar().compare(normalizedStatusDate, start, false);
		double expected = Math.max(0D, Math.min(1D, (double) scheduledElapsed / scheduledDuration));

		ReversibleModelChange change = new TaskProgressService().markOnTrack(project, List.of(task)).change();
		assertEquals(expected, task.getPercentComplete(), 0.00001D,
				"Scheduled completion must count task-calendar working time, not weekends or nonworking hours");
		change.undo();
		assertEquals(0.25D, task.getPercentComplete(), 0.00001D,
			"Undo must restore the exact pre-existing task-level progress");
		change.redo();
		assertEquals(expected, task.getPercentComplete(), 0.00001D);
	}

	@Test
	void markOnTrackPreservesImportedAssignmentProgressAndWorkAcrossUndoRedo() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("assigned-progress", undo), undo);
		project.initialize(false, false);
		NormalTask task = project.createScriptedTask();
		var resource = project.getResourcePool().newResourceInstance();
		Assignment assignment = AssignmentService.getInstance().newAssignment(task, resource, 1.0D, 0L, this);
		long totalWork = assignment.getWork(null);
		assignment.setActualWork(totalWork / 4L, null);
		task.setImportedPercentComplete(0.10D);
		double assignmentPercent = assignment.getPercentComplete();
		long actualWork = assignment.getActualWork(null);
		long remainingWork = assignment.getRemainingWork();
		long statusDate = task.getStart() + (task.getEnd() - task.getStart()) / 2L;
		project.setStatusDate(statusDate);
		long normalizedStatusDate = project.getStatusDate();
		long scheduledDuration = task.getEffectiveWorkCalendar().compare(task.getEnd(), task.getStart(), false);
		long scheduledElapsed = task.getEffectiveWorkCalendar().compare(normalizedStatusDate, task.getStart(), false);
		double expected = Math.max(0D, Math.min(1D, (double) scheduledElapsed / scheduledDuration));

		ReversibleModelChange change = new TaskProgressService().markOnTrack(project, List.of(task)).change();
		assertEquals(expected, task.getPercentComplete(), 0.00001D);
		assertEquals(assignmentPercent, assignment.getPercentComplete(), 0.00001D);
		assertEquals(actualWork, assignment.getActualWork(null));
		assertEquals(remainingWork, assignment.getRemainingWork());
		change.undo();
		assertEquals(0.10D, task.getPercentComplete(), 0.00001D);
		assertEquals(assignmentPercent, assignment.getPercentComplete(), 0.00001D);
		assertEquals(actualWork, assignment.getActualWork(null));
		assertEquals(remainingWork, assignment.getRemainingWork());
		change.redo();
		assertEquals(expected, task.getPercentComplete(), 0.00001D);
		assertEquals(assignmentPercent, assignment.getPercentComplete(), 0.00001D);
		assertEquals(actualWork, assignment.getActualWork(null));
		assertEquals(remainingWork, assignment.getRemainingWork());
	}

	@Test
	void markOnTrackDoesNotWriteSummaryProgressDirectly() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("summary-progress", undo), undo);
		project.initialize(false, false);
		Node summaryNode = project.createLocalTaskNode(null);
		Task summary = (Task) summaryNode.getImpl();
		Task child = (Task) project.createLocalTaskNode(summaryNode).getImpl();
		project.recalculate();
		project.setStatusDate(child.getEnd() + 86_400_000L);

		TaskProgressService.Result result = new TaskProgressService().markOnTrack(project,
			List.of(summary, child));

		assertEquals(1, result.changedCount(), "only the schedulable leaf task is explicitly updated");
		assertEquals(1D, child.getPercentComplete(), 0.00001D);
		assertEquals(child.getPercentComplete(), summary.getPercentComplete(), 0.00001D,
			"summary progress is derived from its child, not directly written by Mark on Track");
		result.change().undo();
		assertEquals(0D, child.getPercentComplete(), 0.00001D);
		assertEquals(child.getPercentComplete(), summary.getPercentComplete(), 0.00001D,
			"Undo restores the child and therefore its derived summary progress");
		result.change().redo();
		assertEquals(1D, child.getPercentComplete(), 0.00001D);
		assertEquals(child.getPercentComplete(), summary.getPercentComplete(), 0.00001D);
	}
}
