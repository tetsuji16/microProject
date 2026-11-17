/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.microproject.pm.resource.ResourcePool;
import com.microproject.undo.DataFactoryUndoController;

class ProjectTaskSnapshotOwnershipTest {
	@Test
	void snapshotRetainsDistinctSameNameTasksByIdentity() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("snapshot ownership", undo), undo);
		project.initialize(false, false);
		NormalTask first = project.createScriptedTask();
		NormalTask second = project.createScriptedTask();
		first.setName("Duplicate");
		second.setName("Duplicate");

		ProjectTaskSnapshot snapshot = ProjectTaskSnapshot.capture(project);

		assertEquals(2, snapshot.taskStates().size());
		assertTrue(snapshot.owns(project, first));
		assertTrue(snapshot.owns(project, second));
	}
}
