/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.task;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.microproject.pm.resource.ResourcePool;
import com.microproject.undo.DataFactoryUndoController;

class ProjectDirtyStateTest {
	private static Project newProject() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("dirty", undo), undo);
		project.initialize(false, false);
		return project;
	}

	@Test
	void restoreReturnsBothFlagsToTheCapturedState() {
		Project project = newProject();
		project.markSaved();
		assertFalse(project.needsSaving());
		Project.DirtyState clean = project.captureDirtyState();

		project.setDirty(true);
		assertTrue(project.needsSaving());
		project.restoreDirtyState(clean);
		assertFalse(project.isDirty());
		assertFalse(project.needsSaving(), "setDirty(false) alone would have left the group flag set");

		project.setDirty(true);
		Project.DirtyState edited = project.captureDirtyState();
		project.markSaved();
		assertFalse(project.needsSaving());
		project.restoreDirtyState(edited);
		assertTrue(project.needsSaving());
	}
}
