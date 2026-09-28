/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.task;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.pm.resource.ResourcePool;
import com.microproject.undo.DataFactoryUndoController;

class TaskModeServiceTest {
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
