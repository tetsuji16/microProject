/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.graphic.spreadsheet.command;

import java.util.List;
import java.util.Objects;

import com.microproject.grouping.core.Node;
import com.microproject.pm.graphic.model.cache.ProjectionRowKey;

/** A task-row paste addressed to the selection snapshot in one visible projection. */
public record TaskPasteIntent(List<Node> copiedRoots, List<ProjectionRowKey> selectedRows,
		long projectionRevision) {
	public TaskPasteIntent {
		copiedRoots = copiedRoots == null ? List.of() : List.copyOf(copiedRoots);
		selectedRows = selectedRows == null ? List.of() : List.copyOf(selectedRows);
		if (copiedRoots.isEmpty() || copiedRoots.stream().anyMatch(Objects::isNull))
			throw new IllegalArgumentException("copied task roots are required");
		if (selectedRows.stream().anyMatch(Objects::isNull))
			throw new IllegalArgumentException("selected row keys cannot contain null");
		if (projectionRevision < 0L)
			throw new IllegalArgumentException("projectionRevision must not be negative");
	}
}
