/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.spreadsheet.command;

import java.util.Objects;

import com.microproject.field.Field;
import com.microproject.pm.task.ProjectTaskKey;

/** A parsed spreadsheet edit addressed to one durable task row in one projection revision. */
public record TaskFieldEditIntent(ProjectTaskKey taskKey, int occurrence, long projectionRevision,
		Field field, Object expectedValue, Object value) {
	public TaskFieldEditIntent {
		Objects.requireNonNull(taskKey, "taskKey");
		Objects.requireNonNull(field, "field");
		if (occurrence < 0)
			throw new IllegalArgumentException("occurrence must not be negative");
		if (projectionRevision < 0L)
			throw new IllegalArgumentException("projectionRevision must not be negative");
	}
}
