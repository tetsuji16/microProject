/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.spreadsheet.command;

/** Outcome of resolving and applying one stable-key task field edit. */
public record TaskCommandResult(Status status, String reason) {
	public enum Status {
		CHANGED,
		NO_CHANGE,
		STALE_PROJECTION,
		STALE_VALUE,
		MISSING_TASK
	}

	public TaskCommandResult {
		if (status == null)
			throw new IllegalArgumentException("status must not be null");
		reason = reason == null ? "" : reason;
	}

	public static TaskCommandResult of(Status status) {
		return new TaskCommandResult(status, "");
	}
}
