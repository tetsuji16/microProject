/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.undo.DataFactoryUndoController;

class NormalTaskResourceNamesTest {
	@Test
	void typingAnUnregisteredResourceNameCreatesAndAssignsOneLocalWorkResource() throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("resource-name-entry", undo);
		Project project = Project.createProject(pool, undo);
		project.initialize(false, false);
		NormalTask task = project.createScriptedTask();

		task.setResourceNames("Aiko");

		assertEquals(1, pool.getResourceList().size());
		assertEquals("Aiko", pool.getResourceList().getFirst().getName());
		assertEquals("Aiko", task.getResourceNames());
		assertEquals(1, task.getAssignments().size());
	}

	@Test
	void typingAnExistingResourceNameReusesThatResourceInsteadOfCreatingAnother() throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("resource-name-reuse", undo);
		Project project = Project.createProject(pool, undo);
		project.initialize(false, false);
		Resource resource = pool.newResourceInstance();
		resource.setName("Aiko");
		NormalTask task = project.createScriptedTask();

		task.setResourceNames("Aiko");

		assertEquals(1, pool.getResourceList().size());
		assertSame(resource, ((Assignment) task.getAssignments().getFirst()).getResource());
	}
}
