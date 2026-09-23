/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.pm.resource.ResourcePool;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.grouping.core.Node;

class TaskProgressServiceTest {
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
		service.setStatusDate(project, date, undo.getEditSupport());
		assertTrue(project.isStatusDateSet());
		service.markOnTrack(project, List.of(task), undo.getEditSupport());
		assertEquals(1D, task.getPercentComplete(), 0.00001D,
				() -> "status=" + project.getStatusDate() + " start=" + task.getStart() + " end=" + task.getEnd());
		undo.undo();
		assertEquals(0D, task.getPercentComplete(), 0.00001D);
		undo.undo();
		assertFalse(project.isStatusDateSet());
		undo.redo();
		assertTrue(project.isStatusDateSet());
		undo.redo();
		assertEquals(1D, task.getPercentComplete(), 0.00001D);
	}

	@Test
	void clearingStatusDateIsUndoableAndRestoresTheExplicitDate() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("clear-status", undo), undo);
		project.initialize(false, false);
		TaskProgressService service = new TaskProgressService();
		long date = project.getStartDate() + 3L * 24L * 60L * 60L * 1000L;
		service.setStatusDate(project, date, undo.getEditSupport());
		long explicit = project.getStatusDate();

		service.clearStatusDate(project, undo.getEditSupport());
		assertFalse(project.isStatusDateSet());
		undo.undo();
		assertTrue(project.isStatusDateSet());
		assertEquals(explicit, project.getStatusDate());
		undo.redo();
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
				() -> service.setStatusDate(project, 0L, undo.getEditSupport()));

		service.setStatusDate(project, task.getStart() - 86_400_000L, undo.getEditSupport());
		service.markOnTrack(project, List.of(task), undo.getEditSupport());
		assertEquals(0D, task.getPercentComplete(), 0.00001D);
		service.setStatusDate(project, task.getEnd(), undo.getEditSupport());
		service.markOnTrack(project, List.of(task), undo.getEditSupport());
		assertEquals(1D, task.getPercentComplete(), 0.00001D);
		NormalTask milestone = project.createScriptedTask();
		milestone.setDuration(0L);
		service.setStatusDate(project, milestone.getEnd() + 86_400_000L, undo.getEditSupport());
		service.markOnTrack(project, List.of(milestone), undo.getEditSupport());
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
		long statusDate = start + (end - start) / 2L;
		project.setStatusDate(statusDate);
		long normalizedStatusDate = project.getStatusDate();
		long scheduledDuration = task.getEffectiveWorkCalendar().compare(end, start, false);
		long scheduledElapsed = task.getEffectiveWorkCalendar().compare(normalizedStatusDate, start, false);
		double expected = Math.max(0D, Math.min(1D, (double) scheduledElapsed / scheduledDuration));

		new TaskProgressService().markOnTrack(project, List.of(task), undo.getEditSupport());
		assertEquals(expected, task.getPercentComplete(), 0.00001D,
				"Scheduled completion must count task-calendar working time, not weekends or nonworking hours");
		undo.undo();
		assertEquals(0D, task.getPercentComplete(), 0.00001D);
		undo.redo();
		assertEquals(expected, task.getPercentComplete(), 0.00001D);
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
			List.of(summary, child), undo.getEditSupport());

		assertEquals(1, result.changedCount(), "only the schedulable leaf task is explicitly updated");
		assertEquals(1D, child.getPercentComplete(), 0.00001D);
		assertEquals(child.getPercentComplete(), summary.getPercentComplete(), 0.00001D,
			"summary progress is derived from its child, not directly written by Mark on Track");
		undo.undo();
		assertEquals(0D, child.getPercentComplete(), 0.00001D);
		assertEquals(child.getPercentComplete(), summary.getPercentComplete(), 0.00001D,
			"Undo restores the child and therefore its derived summary progress");
		undo.redo();
		assertEquals(1D, child.getPercentComplete(), 0.00001D);
		assertEquals(child.getPercentComplete(), summary.getPercentComplete(), 0.00001D);
	}
}
