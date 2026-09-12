/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.application;

import java.nio.file.Path;
import java.util.Objects;

import com.microproject.pm.task.Project;
import com.microproject.pm.task.ProjectFactory;
import com.microproject.port.ExportPort;
import com.microproject.port.ImportPort;
import com.microproject.port.PortDiagnostic;
import com.microproject.port.PortRegistry;
import com.microproject.port.PortResult;
import com.microproject.session.LocalSession;

/**
 * Application-layer mediator for typed import/export ports.  UI and model code
 * depend only on this mediator and never construct exchange adapters directly.
 */
public final class ProjectPortCoordinator {
	private final PortRegistry registry;

	public ProjectPortCoordinator() {
		this(LocalSession.getPortRegistry());
	}

	public ProjectPortCoordinator(PortRegistry registry) {
		this.registry = Objects.requireNonNull(registry, "registry");
	}

	public PortResult<Project> importProject(String formatKey, Path source) {
		if (source == null) return failure("IMPORT_ARGUMENT", "Import source is required");
		ImportPort port = lookupImport(formatKey);
		if (port == null) return failure("IMPORT_PROVIDER_MISSING", "No import provider for format: " + formatKey);
		return port.importProject(new ImportPort.ImportRequest(source, ProjectFactory.getInstance()));
	}

	public PortResult<Path> exportProject(String formatKey, Project project, Path target) {
		if (project == null || target == null) return failure("EXPORT_ARGUMENT", "Project and export target are required");
		ExportPort port = lookupExport(formatKey);
		if (port == null) return failure("EXPORT_PROVIDER_MISSING", "No export provider for format: " + formatKey);
		return port.exportProject(project, new ExportPort.ExportRequest(target));
	}

	private ImportPort lookupImport(String key) {
		if (key == null || key.isBlank()) return null;
		return registry.importPort(key);
	}

	private ExportPort lookupExport(String key) {
		if (key == null || key.isBlank()) return null;
		return registry.exportPort(key);
	}

	private static <T> PortResult<T> failure(String code, String message) {
		return PortResult.failure(new PortDiagnostic(PortDiagnostic.Severity.ERROR, code, message, ""));
	}
}
