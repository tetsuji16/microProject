/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.spreadsheet.command;

import java.util.Objects;

import com.microproject.pm.graphic.model.cache.ProjectionRowKey;

/** Stable, optimistic snapshot for a schedule mutation initiated by a task view. */
public record TaskScheduleEditIntent(ProjectionRowKey.TaskRow task, long projectionRevision,
		Operation operation, long expectedScheduleStart, long expectedScheduleEnd,
		long expectedCompletedThrough, long expectedIntervalStart, long expectedIntervalEnd,
		int expectedConstraintType, long expectedConstraintDate, long requestedStart,
		long requestedEnd, long requestedValue) {
	public enum Operation { MOVE, RESIZE_START, RESIZE_END, PROGRESS, SPLIT }

	public TaskScheduleEditIntent {
		Objects.requireNonNull(task, "task");
		Objects.requireNonNull(operation, "operation");
		if (projectionRevision < 0L)
			throw new IllegalArgumentException("projectionRevision must not be negative");
	}
}
