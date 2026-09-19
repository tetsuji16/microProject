/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.ribbon;

import java.util.Objects;
import java.util.List;

/** Outcome recorded by the canonical ribbon command route. */
public record RibbonCommandResult(String commandId, Status status, String reason, List<Long> affectedTaskIds,
		String activeViewId) {
	public enum Status {
		/** The input route accepted the command; asynchronous work may still fail. */
		DISPATCHED,
		/** The command completed and changed at least one model value. */
		CHANGED,
		/** The command completed but the model was already in the requested state. */
		NO_CHANGE,
		REJECTED,
		FAILED
	}

	public RibbonCommandResult {
		Objects.requireNonNull(commandId, "commandId");
		Objects.requireNonNull(status, "status");
		reason = reason == null ? "" : reason;
		affectedTaskIds = affectedTaskIds == null ? List.of() : List.copyOf(affectedTaskIds);
		activeViewId = activeViewId == null ? "" : activeViewId;
	}
	public RibbonCommandResult(String commandId, Status status, String reason, List<Long> affectedTaskIds) {
		this(commandId, status, reason, affectedTaskIds, "");
	}
	public RibbonCommandResult(String commandId, Status status, String reason) {
		this(commandId, status, reason, List.of());
	}

	public RibbonCommandResult withActiveView(String viewId) {
		return new RibbonCommandResult(commandId, status, reason, affectedTaskIds, viewId);
	}

	public static RibbonCommandResult dispatched(String commandId) {
		return new RibbonCommandResult(commandId, Status.DISPATCHED, "");
	}
	public static RibbonCommandResult changed(String commandId) {
		return new RibbonCommandResult(commandId, Status.CHANGED, "");
	}
	public static RibbonCommandResult changed(String commandId, List<Long> ids) {
		return new RibbonCommandResult(commandId, Status.CHANGED, "", ids);
	}
	public static RibbonCommandResult noChange(String commandId) {
		return new RibbonCommandResult(commandId, Status.NO_CHANGE, "");
	}
	public static RibbonCommandResult noChange(String commandId, List<Long> ids) {
		return new RibbonCommandResult(commandId, Status.NO_CHANGE, "", ids);
	}

	public static RibbonCommandResult rejected(String commandId, String reason) {
		return new RibbonCommandResult(commandId, Status.REJECTED, reason);
	}

	public static RibbonCommandResult failed(String commandId, Throwable failure) {
		return new RibbonCommandResult(commandId, Status.FAILED,
			failure == null ? "unknown-failure" : failure.getClass().getSimpleName());
	}
}
