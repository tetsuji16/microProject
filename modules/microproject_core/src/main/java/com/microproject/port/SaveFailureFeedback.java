/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.port;

/** Supplies a safe, actionable alert message for a save failure with preserved data. */
public interface SaveFailureFeedback {
	String userFacingMessage();
}
