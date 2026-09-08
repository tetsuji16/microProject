/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.ribbon;

import java.util.Objects;

/** Immutable input delivered when a user activates a ribbon command. */
public record RibbonCommandInvocation(String commandId, Origin origin, Object source) {
	public enum Origin {
		RIBBON_BUTTON,
		QUICK_ACCESS,
		OVERFLOW_POPUP
	}

	public RibbonCommandInvocation {
		Objects.requireNonNull(commandId, "commandId");
		Objects.requireNonNull(origin, "origin");
	}
}
