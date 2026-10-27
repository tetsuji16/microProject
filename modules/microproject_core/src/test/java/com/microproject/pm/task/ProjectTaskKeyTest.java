/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.microproject.pm.resource.ResourcePool;
import com.microproject.undo.DataFactoryUndoController;

class ProjectTaskKeyTest {
	@Test
	void samePositiveTaskUniqueIdInDifferentProjectsProducesDifferentKeys() {
		Project firstProject = createProject(101L);
		Project secondProject = createProject(202L);
		Task firstTask = createTask(firstProject, 7L);
		Task secondTask = createTask(secondProject, 7L);

		ProjectTaskKey firstKey = ProjectTaskKey.from(firstTask).orElseThrow();
		ProjectTaskKey secondKey = ProjectTaskKey.from(secondTask).orElseThrow();

		assertEquals(101L, firstKey.owningProjectId());
		assertEquals(7L, firstKey.taskUniqueId());
		assertEquals(202L, secondKey.owningProjectId());
		assertEquals(7L, secondKey.taskUniqueId());
		assertEquals(firstProject, firstKey.projectScope());
		assertNotEquals(firstKey, secondKey);
	}

	@Test
	void sameTaskUniqueIdInDifferentProjectsProducesDifferentKeys() {
		Project firstProject = createProject(-901L);
		Project secondProject = createProject(-902L);
		Task firstTask = createTask(firstProject, -7L);
		Task secondTask = createTask(secondProject, -7L);

		ProjectTaskKey firstKey = ProjectTaskKey.from(firstTask).orElseThrow();
		ProjectTaskKey secondKey = ProjectTaskKey.from(secondTask).orElseThrow();

		assertEquals(-901L, firstKey.owningProjectId());
		assertEquals(-7L, firstKey.taskUniqueId());
		assertEquals(-902L, secondKey.owningProjectId());
		assertEquals(-7L, secondKey.taskUniqueId());
		assertNotEquals(firstKey, secondKey);
	}

	@Test
	void duplicateRuntimeNumbersRemainScopedToProjectInstancesAndKeyIsNotSerializable() {
		Project firstProject = createProject(-901L);
		Project secondProject = createProject(-901L);
		ProjectTaskKey firstKey = ProjectTaskKey.from(createTask(firstProject, -7L)).orElseThrow();
		ProjectTaskKey secondKey = ProjectTaskKey.from(createTask(secondProject, -7L)).orElseThrow();

		assertNotEquals(firstKey, secondKey);
		assertFalse(java.io.Serializable.class.isAssignableFrom(ProjectTaskKey.class));
		assertThrows(IllegalArgumentException.class, () -> new ProjectTaskKey(null, -901L, -7L));
	}

	@Test
	void missingZeroIdentifiersRemainUnavailable() {
		Project project = createProject(0L);
		Task task = createTask(project, 0L);

		assertFalse(ProjectTaskKey.from(null).isPresent());
		assertFalse(ProjectTaskKey.from(task).isPresent());
		assertThrows(IllegalArgumentException.class, () -> new ProjectTaskKey(project, 0L, -1L));
		assertThrows(IllegalArgumentException.class, () -> new ProjectTaskKey(project, -1L, 0L));

		Project sentinelProject = createProject(-1L);
		Task sentinelTask = createTask(sentinelProject, -7L);
		assertFalse(ProjectTaskKey.from(sentinelTask).isPresent(), "-1 is the unassigned HasUniqueIdImpl sentinel");
		assertThrows(IllegalArgumentException.class, () -> new ProjectTaskKey(sentinelProject, -1L, -7L));
		assertThrows(IllegalArgumentException.class, () -> new ProjectTaskKey(project, -901L, -1L));
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
