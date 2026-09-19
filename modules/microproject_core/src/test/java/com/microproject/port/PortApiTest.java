/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.port;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class PortApiTest {
	@Test
	void registryRejectsUnknownAndDuplicatePorts() {
		PortRegistry registry = new PortRegistry();
		ImportPort port = new ImportPort() {
			public String formatKey() { return "pod"; }
			public PortResult<com.microproject.pm.task.Project> importProject(ImportRequest request) { return PortResult.success(null); }
		};
		registry.registerImport(port);
		assertEquals(port, registry.importPort("pod"));
		assertThrows(IllegalStateException.class, () -> registry.registerImport(port));
		assertEquals(null, registry.importPort("unknown"));
	}

	@Test
	void requestAndDiagnosticsAreImmutableAndStructured() {
		ImportPort.ImportRequest request = new ImportPort.ImportRequest(Path.of("plan.pod"),
				com.microproject.pm.task.ProjectFactory.getInstance());
		assertFalse(request.source().isAbsolute() && request.source().toString().isBlank());
		PortResult<Void> result = PortResult.failure(new PortDiagnostic(PortDiagnostic.Severity.ERROR,
				"FORMAT_UNSUPPORTED", "Unsupported format", "plan.xyz"));
		assertFalse(result.succeeded());
		assertThrows(UnsupportedOperationException.class,
				() -> result.diagnostics().add(new PortDiagnostic(PortDiagnostic.Severity.WARNING, "x", "x", "")));
	}
}
