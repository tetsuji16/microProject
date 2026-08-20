/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.task;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.microproject.pm.resource.ResourcePool;
import com.microproject.undo.DataFactoryUndoController;

class TaskVisibilityStateTest {
	@Test
	void hiddenStateDefaultsToVisibleAndCopiesToAnotherTask() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("visibility", undo), undo);
		NormalTask source = new NormalTask(project);
		NormalTask target = new NormalTask(project);

		assertFalse(source.isHiddenTask());
		source.setHiddenTask(true);
		source.cloneTo(target);

		assertTrue(source.isHiddenTask());
		assertTrue(target.isHiddenTask());
	}
}
