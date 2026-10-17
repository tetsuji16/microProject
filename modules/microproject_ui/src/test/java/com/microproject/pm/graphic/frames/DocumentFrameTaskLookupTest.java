/*
 * MIT License
 * Copyright (c) 2026 microProject
 */
package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

class DocumentFrameTaskLookupTest {
	@Test
	void lookupFindsLaterTaskAfterLeafWithNullChildren() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("inspector-task-lookup", undo), undo);
		project.initialize(false, false);
		NormalTask first = project.createScriptedTask();
		NormalTask target = project.createScriptedTask();

		assertNull(project.getTaskModel().search(first).getChildren(), "fixture must include a leaf before the target");
		assertSame(project.getTaskModel().search(target), DocumentFrame.findTaskNode(project, target));
	}
}
