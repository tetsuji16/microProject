/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.exchange;

import java.nio.file.Path;

import com.microproject.pm.task.Project;
import com.microproject.port.ProjectArtifactLifecyclePort;
import com.microproject.session.LocalSession;

/** Owns temporary extraction resources created while opening MPO archives. */
public final class MpoProjectArtifactLifecycleAdapter implements ProjectArtifactLifecyclePort {
	@Override
	public String formatKey() {
		return LocalSession.MPO_PROJECT_IMPORTER;
	}

	@Override
	public void setWorkspaceRoot(Path root) {
		MpoFileImporter.setExtractionWorkspaceRoot(root);
	}

	@Override
	public void close(Project project) {
		MpoExtractionOwnershipRegistry.close(project);
	}

	@Override
	public void closeAll() {
		MpoExtractionOwnershipRegistry.closeAll();
	}
}
