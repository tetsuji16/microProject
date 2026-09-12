/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.ribbon;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Stable identifiers for commands that have more than one presentation route.
 *
 * <p>The legacy menu layer still exposes string action ids.  Keeping the
 * conversion here makes the route boundary explicit while preserving those
 * ids for old menu definitions and saved workspaces.</p>
 */
public enum CommandId {
	INSERT("InsertTask"),
	DELETE("Delete"),
	CUT("Cut"),
	COPY("Copy"),
	PASTE("Paste"),
	PASTE_INSERT("PasteInsert"),
	LINK("Link"),
	UNLINK("Unlink"),
	INDENT("Indent"),
	OUTDENT("Outdent"),
	EXPAND("Expand"),
	COLLAPSE("Collapse"),
	TASK_MODE_MANUAL("TaskModeManual"),
	TASK_MODE_AUTOMATIC("TaskModeAutomatic"),
	STATUS_DATE("StatusDate"),
	MARK_ON_TRACK("MarkOnTrack"),
	UPDATE_PROJECT("UpdateProject");

	private static final Map<String, CommandId> BY_ACTION_ID = Arrays.stream(values())
		.collect(Collectors.toUnmodifiableMap(CommandId::actionId, Function.identity()));

	private final String actionId;

	CommandId(String actionId) {
		this.actionId = actionId;
	}

	/** The action id used by the legacy menu and root-pane action maps. */
	public String actionId() {
		return actionId;
	}

	/** Resolves a legacy action id without exposing a nullable result. */
	public static CommandId fromActionId(String actionId) {
		Objects.requireNonNull(actionId, "actionId");
		CommandId command = BY_ACTION_ID.get(actionId);
		if (command == null)
			throw new IllegalArgumentException("Unsupported routed command: " + actionId);
		return command;
	}
}
