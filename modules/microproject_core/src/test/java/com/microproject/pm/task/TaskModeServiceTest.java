/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.task;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.pm.resource.ResourcePool;
import com.microproject.undo.DataFactoryUndoController;

class TaskModeServiceTest {
	@Test
	void compensatesEarlierTasksWhenLaterModeApplicationFails() throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project firstProject = Project.createProject(ResourcePool.createRourcePool("first", undo), undo);
		firstProject.initialize(false, false);
		NormalTask first = firstProject.createScriptedTask();
		firstProject.recalculate();
		long firstStart = first.getStart();
		long firstFinish = first.getEnd();
		Project secondProject = Project.createProject(ResourcePool.createRourcePool("second", undo), undo);
		secondProject.initialize(false, false);
		NormalTask second = secondProject.createScriptedTask();
		secondProject.recalculate();
		java.lang.reflect.Field algorithm = Project.class.getDeclaredField("schedulingAlgorithm");
		algorithm.setAccessible(true);
		algorithm.set(secondProject, null);
		long secondStart = second.getStart();
		long secondFinish = second.getEnd();
		boolean firstDirty = first.isDirty();
		boolean secondDirty = second.isDirty();
		boolean projectDirty = firstProject.isDirty();
		boolean secondProjectDirty = secondProject.isDirty();

		NullPointerException failure = assertThrows(NullPointerException.class,
			() -> new TaskModeService().apply(List.of(first, second), TaskModeService.Mode.MANUAL));
		assertFalse(first.isManuallyScheduled());
		assertFalse(second.isManuallyScheduled());
		assertTrue(first.manualStart == 0L && first.manualFinish == 0L);
		assertTrue(second.manualStart == 0L && second.manualFinish == 0L);
		assertTrue(first.getStart() == firstStart && first.getEnd() == firstFinish);
		assertTrue(second.getStart() == secondStart && second.getEnd() == secondFinish);
		assertTrue(first.isDirty() == firstDirty && second.isDirty() == secondDirty);
		assertTrue(firstProject.isDirty() == projectDirty);
		assertTrue(secondProject.isDirty() == secondProjectDirty);
	}

	@Test
	void appliesManualModeToSelectionAsOneReversibleChange() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("mode", undo), undo);
		project.initialize(false, false);
		NormalTask first = project.createScriptedTask();
		NormalTask second = project.createScriptedTask();
		TaskModeService service = new TaskModeService();

		assertFalse(first.isManuallyScheduled());
		assertFalse(second.isManuallyScheduled());
		TaskModeService.Result result = service.apply(List.of(first, second), TaskModeService.Mode.MANUAL);
		assertTrue(result.manual());
		assertTrue(first.isManuallyScheduled() && second.isManuallyScheduled());
		result.change().undo();
		assertFalse(first.isManuallyScheduled() || second.isManuallyScheduled());
		result.change().redo();
		assertTrue(first.isManuallyScheduled() && second.isManuallyScheduled());
	}
}
