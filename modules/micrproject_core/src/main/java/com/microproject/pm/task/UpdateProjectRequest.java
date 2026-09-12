/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.task;

/** Immutable, validated input captured by the Update Project dialog. */
public record UpdateProjectRequest(long statusDate, boolean updateWorkAsCompleteThrough,
	boolean setFractionalPercentComplete, boolean entireProject) {
	public UpdateProjectRequest {
		if (statusDate <= 0L) throw new IllegalArgumentException("statusDate must be positive");
	}
}
