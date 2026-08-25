/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.application.task;

import com.microproject.pm.task.ProjectTaskKey;

public record TaskDependencyUpdateCommand(ProjectTaskKey predecessor, ProjectTaskKey successor,
		long expectedLag, int expectedType, long proposedLag, int proposedType, long expectedDomainRevision) {
	public TaskDependencyUpdateCommand {
		if (predecessor == null || successor == null) throw new IllegalArgumentException("endpoints are required");
		if (expectedDomainRevision < 0L) throw new IllegalArgumentException("revision must not be negative");
	}
}
