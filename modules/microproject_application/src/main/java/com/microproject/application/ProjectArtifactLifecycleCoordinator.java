/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.application;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

import com.microproject.pm.task.Project;
import com.microproject.port.PortRegistry;
import com.microproject.port.ProjectArtifactLifecyclePort;
import com.microproject.session.LocalSession;

/** Coordinates project-format temporary artifact lifecycle operations. */
public final class ProjectArtifactLifecycleCoordinator {
	private final PortRegistry registry;
	private TemporaryWorkspace workspace;

	public ProjectArtifactLifecycleCoordinator() {
		this(LocalSession.getPortRegistry());
	}

	public ProjectArtifactLifecycleCoordinator(PortRegistry registry) {
		this.registry = Objects.requireNonNull(registry, "registry");
	}

	public boolean setWorkspaceRoot(String formatKey, Path root) {
		ProjectArtifactLifecyclePort port = lookup(formatKey);
		if (port == null)
			return false;
		port.setWorkspaceRoot(root);
		return true;
	}

	public boolean close(String formatKey, Project project) {
		ProjectArtifactLifecyclePort port = lookup(formatKey);
		if (port == null)
			return false;
		port.close(project);
		return true;
	}

	public boolean closeAll(String formatKey) {
		ProjectArtifactLifecyclePort port = lookup(formatKey);
		if (port == null)
			return false;
		port.closeAll();
		return true;
	}

	public boolean openDefaultWorkspace(String formatKey) throws IOException {
		return ownWorkspace(formatKey, TemporaryWorkspace.openDefault());
	}

	public boolean ownWorkspace(String formatKey, TemporaryWorkspace workspace) {
		if (this.workspace != null)
			throw new IllegalStateException("A temporary workspace is already open");
		this.workspace = Objects.requireNonNull(workspace, "workspace");
		return setWorkspaceRoot(formatKey, workspace.root());
	}

	public void closeWorkspace() {
		if (workspace == null)
			return;
		workspace.close();
		workspace = null;
	}

	private ProjectArtifactLifecyclePort lookup(String formatKey) {
		if (formatKey == null || formatKey.isBlank())
			return null;
		return registry.artifactLifecycle(formatKey);
	}
}
