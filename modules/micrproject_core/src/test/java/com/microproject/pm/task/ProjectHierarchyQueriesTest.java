/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.task;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.undo.DataFactoryUndoController;

class ProjectHierarchyQueriesTest {
	@Test
	void nullProjectAndTaskAreSafe() {
		assertTrue(ProjectHierarchyQueries.outline(null).isEmpty());
		assertTrue(ProjectHierarchyQueries.descendants(null).isEmpty());
	}

	@Test
	void outlineReturnsAnImmutableSnapshot() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("hierarchy", undo), undo);
		project.initialize(false, false);
		assertThrows(UnsupportedOperationException.class, () -> ProjectHierarchyQueries.outline(project).add(null));
	}
}
