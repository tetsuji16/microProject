/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.spreadsheet.command;

import java.util.List;
import java.util.Objects;

import com.microproject.pm.graphic.model.cache.ProjectionRowKey;

/** A hierarchy gesture addressed to task occurrences in one visible projection. */
public record TaskHierarchyEditIntent(Operation operation, List<ProjectionRowKey.TaskRow> tasks,
		long projectionRevision, ProjectionRowKey.TaskRow anchor, boolean after, int direction) {
	public enum Operation {
		MOVE,
		RELOCATE
	}

	public TaskHierarchyEditIntent {
		Objects.requireNonNull(operation, "operation");
		tasks = tasks == null ? List.of() : List.copyOf(tasks);
		if (tasks.isEmpty() || tasks.stream().anyMatch(Objects::isNull))
			throw new IllegalArgumentException("task rows are required");
		if (projectionRevision < 0L)
			throw new IllegalArgumentException("projectionRevision must not be negative");
		if (operation == Operation.MOVE && (direction != -1 && direction != 1))
			throw new IllegalArgumentException("move direction must be -1 or 1");
		if (operation == Operation.MOVE && (anchor != null || after))
			throw new IllegalArgumentException("move intent cannot have relocation parameters");
		if (operation == Operation.RELOCATE) {
			Objects.requireNonNull(anchor, "anchor");
			if (direction != 0)
				throw new IllegalArgumentException("relocation direction must be zero");
		}
	}
}
