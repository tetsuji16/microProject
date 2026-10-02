/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.microproject.pm.resource.ResourcePool;
import com.microproject.undo.DataFactoryUndoController;

class ProjectTaskKeyTest {
	@Test
	void sameTaskUniqueIdInDifferentProjectsProducesDifferentKeys() {
		Project firstProject = createProject(101L);
		Project secondProject = createProject(202L);
		Task firstTask = createTask(firstProject, 7L);
		Task secondTask = createTask(secondProject, 7L);

		ProjectTaskKey firstKey = ProjectTaskKey.from(firstTask).orElseThrow();
		ProjectTaskKey secondKey = ProjectTaskKey.from(secondTask).orElseThrow();

		assertEquals(new ProjectTaskKey(101L, 7L), firstKey);
		assertEquals(new ProjectTaskKey(202L, 7L), secondKey);
		assertNotEquals(firstKey, secondKey);
	}

	@Test
	void missingDurableIdsDoNotCreateTransientKeys() {
		Project project = createProject(0L);
		Task task = createTask(project, 0L);

		assertTrue(ProjectTaskKey.from(null).isEmpty());
		assertTrue(ProjectTaskKey.from(task).isEmpty());
	}

	private static Project createProject(long uniqueId) {
		var undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("task-key", undo), undo);
		project.setUniqueId(uniqueId);
		project.initialize(false, false);
		return project;
	}

	private static Task createTask(Project project, long uniqueId) {
		Task task = project.createScriptedTask();
		task.setUniqueId(uniqueId);
		task.setProjectId(project.getUniqueId());
		project.connectTask(task);
		project.getTaskOutlines().addToAll(task, null);
		return task;
	}
}
