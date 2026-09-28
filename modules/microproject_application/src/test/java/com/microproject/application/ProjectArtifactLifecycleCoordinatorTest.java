/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.application;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.microproject.pm.task.Project;
import com.microproject.port.PortRegistry;
import com.microproject.port.ProjectArtifactLifecyclePort;

class ProjectArtifactLifecycleCoordinatorTest {
	@Test
	void routesWorkspaceAndCloseLifecycleToTheMatchingFormatPort() {
		PortRegistry registry = new PortRegistry();
		RecordingPort port = new RecordingPort();
		registry.registerArtifactLifecycle(port);
		ProjectArtifactLifecycleCoordinator coordinator = new ProjectArtifactLifecycleCoordinator(registry);
		Path workspace = Path.of("test-workspace");
		Project project = null;

		assertTrue(coordinator.setWorkspaceRoot("mpo", workspace));
		assertTrue(coordinator.close("mpo", project));
		assertTrue(coordinator.closeAll("mpo"));

		assertEquals(workspace, port.workspaceRoot);
		assertTrue(port.closeCalled);
		assertTrue(port.closedAll);
	}

	@Test
	void missingOrBlankFormatIsReportedWithoutCallingAnyLifecyclePort() {
		ProjectArtifactLifecycleCoordinator coordinator = new ProjectArtifactLifecycleCoordinator(new PortRegistry());

		assertFalse(coordinator.setWorkspaceRoot("missing", Path.of("unused")));
		assertFalse(coordinator.close("", null));
		assertFalse(coordinator.closeAll(null));
	}

	private static final class RecordingPort implements ProjectArtifactLifecyclePort {
		private Path workspaceRoot;
		private boolean closeCalled;
		private boolean closedAll;

		@Override public String formatKey() { return "mpo"; }
		@Override public void setWorkspaceRoot(Path root) { workspaceRoot = root; }
		@Override public void close(Project project) { closeCalled = true; }
		@Override public void closeAll() { closedAll = true; }
	}
}
