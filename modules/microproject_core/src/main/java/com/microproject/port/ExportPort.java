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

	/**
	 * @param markSaved when true the export is the project's save: the project is
	 *        marked saved only if it still equals what was written, otherwise the
	 *        result fails and the project stays modified.
	 */
	record ExportRequest(Path target, boolean markSaved) {
		public ExportRequest {
			target = Objects.requireNonNull(target, "target").toAbsolutePath().normalize();
		}

		public ExportRequest(Path target) {
			this(target, false);
		}
	}
}
