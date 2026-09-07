/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.ui.ribbon;

import java.util.Objects;

/** Outcome recorded by the canonical ribbon command route. */
public record RibbonCommandResult(String commandId, Status status, String reason) {
	public enum Status {
		COMPLETED,
		REJECTED,
		FAILED
	}

	public RibbonCommandResult {
		Objects.requireNonNull(commandId, "commandId");
		Objects.requireNonNull(status, "status");
		reason = reason == null ? "" : reason;
	}

	public static RibbonCommandResult completed(String commandId) {
		return new RibbonCommandResult(commandId, Status.COMPLETED, "");
	}

	public static RibbonCommandResult rejected(String commandId, String reason) {
		return new RibbonCommandResult(commandId, Status.REJECTED, reason);
	}

	public static RibbonCommandResult failed(String commandId, Throwable failure) {
		return new RibbonCommandResult(commandId, Status.FAILED,
			failure == null ? "unknown-failure" : failure.getClass().getSimpleName());
	}
}
