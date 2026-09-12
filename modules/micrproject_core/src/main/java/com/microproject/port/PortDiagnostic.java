/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.port;

import java.util.Objects;

/** Structured, format-neutral diagnostic returned by an import/export port. */
public record PortDiagnostic(Severity severity, String code, String message, String location) {
	public PortDiagnostic {
		severity = Objects.requireNonNull(severity, "severity");
		code = Objects.requireNonNull(code, "code");
		message = Objects.requireNonNull(message, "message");
		location = location == null ? "" : location;
	}

	public enum Severity { WARNING, ERROR }
}
