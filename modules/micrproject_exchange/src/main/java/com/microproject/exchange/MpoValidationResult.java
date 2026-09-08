/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.exchange;

/** Immutable result of MPO container preflight validation. */
public record MpoValidationResult(boolean valid, String reason) {
	public MpoValidationResult {
		reason = reason == null ? "" : reason;
	}
}
