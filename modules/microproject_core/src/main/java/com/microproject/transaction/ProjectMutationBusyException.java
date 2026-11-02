/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.transaction;

/**
 * Thrown when the Swing event thread cannot obtain the process-wide mutation
 * coordinator within the bounded wait (issue #946). Background threads never
 * use this path; they wait without a time limit.
 */
public final class ProjectMutationBusyException extends IllegalStateException {
	public ProjectMutationBusyException(String message) {
		super(message);
	}

	public ProjectMutationBusyException(String message, Throwable cause) {
		super(message, cause);
	}
}
