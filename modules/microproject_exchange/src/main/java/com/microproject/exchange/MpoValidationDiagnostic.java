/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.exchange;

import java.util.Objects;

/** A stable, machine-readable validation finding for UI/logging adapters. */
public record MpoValidationDiagnostic(Severity severity, String code, String message,
		String entryName) {
	public MpoValidationDiagnostic {
		severity = Objects.requireNonNull(severity, "severity");
		code = Objects.requireNonNull(code, "code");
		message = Objects.requireNonNull(message, "message");
		entryName = entryName == null ? "" : entryName;
	}

	public enum Severity {
		WARNING, ERROR
	}
}
