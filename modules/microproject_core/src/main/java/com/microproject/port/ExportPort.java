/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.port;

import java.nio.file.Path;
import java.util.Objects;

import com.microproject.pm.task.Project;

/** Core-owned export boundary; concrete file formats live in exchange. */
public interface ExportPort {
	String formatKey();
	PortResult<Path> exportProject(Project project, ExportRequest request);

	record ExportRequest(Path target) {
		public ExportRequest {
			target = Objects.requireNonNull(target, "target").toAbsolutePath().normalize();
		}
	}
}
