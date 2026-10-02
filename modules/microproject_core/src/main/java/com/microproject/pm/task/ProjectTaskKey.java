/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.task;

import java.io.Serializable;
import java.util.Optional;

/** Stable identity for a task projected from its owning project. */
public record ProjectTaskKey(long owningProjectId, long taskUniqueId) implements Serializable {
	private static final long serialVersionUID = 1L;

	public ProjectTaskKey {
		if (owningProjectId <= 0L)
			throw new IllegalArgumentException("owningProjectId must be positive");
		if (taskUniqueId <= 0L)
			throw new IllegalArgumentException("taskUniqueId must be positive");
	}

	/** Returns empty until the owning project and task have durable identifiers. */
	public static Optional<ProjectTaskKey> from(Task task) {
		if (task == null || task.getUniqueId() <= 0L)
			return Optional.empty();
		Project owner = task.getOwningProject();
		long projectId = owner == null ? task.getProjectId() : owner.getUniqueId();
		return projectId <= 0L
				? Optional.empty()
				: Optional.of(new ProjectTaskKey(projectId, task.getUniqueId()));
	}
}
