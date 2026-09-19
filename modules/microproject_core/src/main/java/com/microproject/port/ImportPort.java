/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.port;

import java.nio.file.Path;
import java.util.Objects;

import com.microproject.pm.task.Project;
import com.microproject.pm.task.ProjectFactory;

/** Core-owned import boundary; concrete file formats live in exchange. */
public interface ImportPort {
	String formatKey();
	PortResult<Project> importProject(ImportRequest request);

	record ImportRequest(Path source, ProjectFactory projectFactory) {
		public ImportRequest {
			source = Objects.requireNonNull(source, "source").toAbsolutePath().normalize();
			projectFactory = Objects.requireNonNull(projectFactory, "projectFactory");
		}
	}
}
