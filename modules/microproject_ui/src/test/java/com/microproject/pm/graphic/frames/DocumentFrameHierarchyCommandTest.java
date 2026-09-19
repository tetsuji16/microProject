/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import com.microproject.grouping.core.Node;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

class DocumentFrameHierarchyCommandTest {
	@Test
	void rootTaskIsRejectedForOutdentInsteadOfReportingAChange() {
		var undo = new DataFactoryUndoController();
		var project = Project.createProject(ResourcePool.createRourcePool("outdent", undo), undo);
		project.initialize(false, false);
		Node rootTask = project.createLocalTaskNode(null);

		assertFalse(DocumentFrame.canOutdent(rootTask));
	}
}
