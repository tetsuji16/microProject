/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.spreadsheet.command;

import java.util.List;

/** Outcome of resolving and applying one stable-key task command. */
public record TaskCommandResult(Status status, String reason, String commandId,
		List<Long> selectedTaskIds, List<Long> affectedTaskIds) {
	public TaskCommandResult(Status status, String reason) {
		this(status, reason, "", List.of(), List.of());
	}
	public enum Status {
		CHANGED,
		NO_CHANGE,
		STALE_PROJECTION,
		STALE_VALUE,
		MISSING_TASK,
		INVALID_INTENT,
		LOCKED,
		REJECTED
	}

	public TaskCommandResult {
		if (status == null)
			throw new IllegalArgumentException("status must not be null");
		reason = reason == null ? "" : reason;
		commandId = commandId == null ? "" : commandId;
		selectedTaskIds = selectedTaskIds == null ? List.of() : List.copyOf(selectedTaskIds);
		affectedTaskIds = affectedTaskIds == null ? List.of() : List.copyOf(affectedTaskIds);
	}

	public static TaskCommandResult of(Status status) {
		return new TaskCommandResult(status, "");
	}
}
