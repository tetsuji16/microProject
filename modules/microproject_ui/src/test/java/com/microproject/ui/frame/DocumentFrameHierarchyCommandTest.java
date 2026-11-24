/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.ui.frame;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import com.microproject.node.Node;
import com.microproject.model.resource.ResourcePool;
import com.microproject.model.task.Project;
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
