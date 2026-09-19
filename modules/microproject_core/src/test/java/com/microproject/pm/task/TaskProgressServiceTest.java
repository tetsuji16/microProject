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
}
