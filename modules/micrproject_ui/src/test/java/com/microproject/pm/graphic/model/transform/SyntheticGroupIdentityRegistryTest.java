/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.model.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

class SyntheticGroupIdentityRegistryTest {
	@Test
	void equalLabelsAndReorderedRebuildsRemainDistinctByDurableMembers() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("groups", undo), undo);
		project.initialize(false, false);
		NormalTask first = task(project, "Same");
		NormalTask second = task(project, "Same");
		GraphicNode firstNode = new GraphicNode(project.getTaskModel().search(first), 1);
		GraphicNode secondNode = new GraphicNode(project.getTaskModel().search(second), 1);
		SyntheticGroupIdentityRegistry registry = new SyntheticGroupIdentityRegistry();

		registry.beginGeneration();
		String firstId = registry.resolve(1, "root", List.of(firstNode));
		String secondId = registry.resolve(1, "root", List.of(secondNode));
		assertNotEquals(firstId, secondId);

		registry.beginGeneration();
		assertEquals(secondId, registry.resolve(1, "root", List.of(secondNode)));
		assertEquals(firstId, registry.resolve(1, "root", List.of(firstNode)));
	}

	private static NormalTask task(Project project, String name) {
		NormalTask task = project.createScriptedTask();
		task.setName(name);
		project.connectTask(task);
		project.getTaskOutlines().addToAll(task, null);
		return task;
	}
}
