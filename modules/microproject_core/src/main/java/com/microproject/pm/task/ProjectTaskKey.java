/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.task;

import java.util.Objects;
import java.util.Optional;

/** Runtime identity for a task within its loaded document scope. */
public final class ProjectTaskKey {
	private final long owningProjectId;
	private final long taskUniqueId;
	private final Project projectScope;

	public ProjectTaskKey(Project projectScope, long owningProjectId, long taskUniqueId) {
		if (projectScope == null)
			throw new IllegalArgumentException("projectScope must not be null");
		if (!isAssignedId(owningProjectId))
			throw new IllegalArgumentException("owningProjectId must be assigned");
		if (!isAssignedId(taskUniqueId))
			throw new IllegalArgumentException("taskUniqueId must be assigned");
		this.owningProjectId = owningProjectId;
		this.taskUniqueId = taskUniqueId;
		this.projectScope = projectScope;
	}

	/** LocalSession IDs are negative; zero and the HasUniqueIdImpl -1 sentinel are unassigned. */
	public static boolean isAssignedId(long id) {
		return id != 0L && id != -1L;
	}

	public long owningProjectId() {
		return owningProjectId;
	}

	public long taskUniqueId() {
		return taskUniqueId;
	}

	public Project projectScope() {
		return projectScope;
	}

	/** Returns empty until the owning project and task have assigned runtime identifiers. */
	public static Optional<ProjectTaskKey> from(Task task) {
		if (task == null || !isAssignedId(task.getUniqueId()))
			return Optional.empty();
		Project owningProject = task.getOwningProject();
		Project projectScope = owningProject == null ? task.getProject() : owningProject;
		long projectId = owningProject == null ? task.getProjectId() : owningProject.getUniqueId();
		return !isAssignedId(projectId) || projectScope == null
				? Optional.empty()
				: Optional.of(new ProjectTaskKey(projectScope, projectId, task.getUniqueId()));
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof ProjectTaskKey key) || owningProjectId != key.owningProjectId
				|| taskUniqueId != key.taskUniqueId)
			return false;
		return projectScope == key.projectScope;
	}

	@Override
	public int hashCode() {
		return Objects.hash(owningProjectId, taskUniqueId, System.identityHashCode(projectScope));
	}

	@Override
	public String toString() {
		return "ProjectTaskKey[owningProjectId=" + owningProjectId + ", taskUniqueId=" + taskUniqueId + "]";
	}
}
