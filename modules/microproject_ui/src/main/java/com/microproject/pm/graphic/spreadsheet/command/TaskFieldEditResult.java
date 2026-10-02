/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.spreadsheet.command;

/** Outcome of resolving and applying one stable-key task field edit. */
public record TaskFieldEditResult(Status status, String reason) {
	public enum Status {
		CHANGED,
		NO_CHANGE,
		STALE_PROJECTION,
		STALE_VALUE,
		MISSING_TASK
	}

	public TaskFieldEditResult {
		if (status == null)
			throw new IllegalArgumentException("status must not be null");
		reason = reason == null ? "" : reason;
	}

	public static TaskFieldEditResult of(Status status) {
		return new TaskFieldEditResult(status, "");
	}
}
