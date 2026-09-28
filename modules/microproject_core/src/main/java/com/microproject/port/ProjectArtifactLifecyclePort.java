/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.port;

import java.nio.file.Path;

import com.microproject.pm.task.Project;

/** Lifecycle operations for temporary artifacts owned by a project format. */
public interface ProjectArtifactLifecyclePort {
	String formatKey();

	void setWorkspaceRoot(Path root);

	void close(Project project);

	void closeAll();
}
