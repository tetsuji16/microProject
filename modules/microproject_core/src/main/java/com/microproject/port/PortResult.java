/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.port;

import java.util.List;
import java.util.Objects;

/** Immutable result shared by application orchestration and exchange adapters. */
public record PortResult<T>(T value, List<PortDiagnostic> diagnostics) {
	public PortResult {
		diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
	}

	public boolean succeeded() {
		return diagnostics.stream().noneMatch(diagnostic -> diagnostic.severity() == PortDiagnostic.Severity.ERROR);
	}

	public static <T> PortResult<T> success(T value) {
		return new PortResult<>(value, List.of());
	}

	public static <T> PortResult<T> failure(PortDiagnostic diagnostic) {
		return new PortResult<>(null, List.of(Objects.requireNonNull(diagnostic, "diagnostic")));
	}
}
