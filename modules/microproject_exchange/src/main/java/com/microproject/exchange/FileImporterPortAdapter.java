/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.exchange;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Supplier;

import com.microproject.pm.task.Project;
import com.microproject.port.ExportPort;
import com.microproject.port.ImportPort;
import com.microproject.port.PortDiagnostic;
import com.microproject.port.PortResult;

/** Bridges the legacy FileImporter contract to the typed core ports. */
public final class FileImporterPortAdapter implements ImportPort, ExportPort {
	private final String formatKey;
	private final Supplier<? extends FileImporter> factory;

	public FileImporterPortAdapter(String formatKey, Supplier<? extends FileImporter> factory) {
		this.formatKey = Objects.requireNonNull(formatKey, "formatKey");
		this.factory = Objects.requireNonNull(factory, "factory");
	}

	@Override public String formatKey() { return formatKey; }

	@Override
	public PortResult<Project> importProject(ImportRequest request) {
		Objects.requireNonNull(request, "request");
		try {
			FileImporter importer = Objects.requireNonNull(factory.get(), "importer");
			importer.setFileName(request.source().toString());
			importer.setProjectFactory(request.projectFactory());
			// Native POD import is file-oriented because it supports the legacy
			// serialized payload and XML recovery fallback.  Keep that behavior
			// behind the typed port; MSP/MPOF adapters remain stream-oriented.
			if (importer instanceof LocalFileImporter) {
				importer.importFile();
				return PortResult.success(Objects.requireNonNull(importer.getProject(),
						"POD importer produced no project"));
			}
			try (InputStream input = Files.newInputStream(request.source())) {
				return PortResult.success(importer.loadProject(input));
			}
		} catch (java.io.IOException exception) {
			return failure("IMPORT_IO", exception, request.source());
		} catch (Exception exception) {
			return failure("IMPORT_FAILED", exception, request.source());
		}
	}

	@Override
	public PortResult<Path> exportProject(Project project, ExportRequest request) {
		Objects.requireNonNull(project, "project");
		Objects.requireNonNull(request, "request");
		try {
			Path parent = request.target().getParent();
			if (parent == null) throw new java.io.IOException("Export target has no parent directory");
			Files.createDirectories(parent);
			FileImporter importer = Objects.requireNonNull(factory.get(), "importer");
			importer.setFileName(request.target().toString());
			importer.setProject(project);
			importer.exportFile();
			return PortResult.success(request.target());
		} catch (java.io.IOException exception) {
			return failure("EXPORT_IO", exception, request.target());
		} catch (Exception exception) {
			return failure("EXPORT_FAILED", exception, request.target());
		}
	}

	private static <T> PortResult<T> failure(String code, Exception exception, Path location) {
		String message = exception.getMessage();
		if (message == null || message.isBlank()) message = exception.getClass().getSimpleName();
		return PortResult.failure(new PortDiagnostic(PortDiagnostic.Severity.ERROR, code, message,
				location == null ? "" : location.toString()));
	}
}
