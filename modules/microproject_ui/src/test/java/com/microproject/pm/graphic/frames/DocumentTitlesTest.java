/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

class DocumentTitlesTest {
	@Test
	void modifiedMarkerIsSuppressedWhileSavingAndRestoredAfterwards() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("title", undo), undo);
		project.initialize(false, false);
		project.setName("Plan");
		project.markSaved();
		assertFalse(DocumentTitles.windowTitle(project, null, false).endsWith(" *"));

		project.setDirty(true);
		assertTrue(DocumentTitles.windowTitle(project, null, false).endsWith(" - Plan *"));
		assertTrue(DocumentTitles.windowTitle(project, null, true).endsWith(" - Plan"));

		project.markSaved();
		assertFalse(DocumentTitles.windowTitle(project, null, false).endsWith(" *"));
	}

	@Test
	void unnamedProjectUsesFallbackThenPlaceholder() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("title2", undo), undo);
		project.initialize(false, false);
		project.setName("");
		assertTrue(DocumentTitles.windowTitle(project, "Frame", true).endsWith(" - Frame"));
		assertTrue(DocumentTitles.windowTitle(project, " ", true).endsWith(" - (unnamed project)"));
	}
}
