/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.graphic.model.cache.ViewNodeModelCache;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

class DocumentReferenceCachesTest {
	@Test
	void closingOneFrameOwnerCannotInvalidateAnotherOwnerForTheSameProject() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("two-frame-cache", undo), undo);
		project.initialize(false, false);
		var commands = new com.microproject.application.task.TaskCommandGateway(project);
		DocumentReferenceCaches first = new DocumentReferenceCaches(project, commands);
		DocumentReferenceCaches second = new DocumentReferenceCaches(project, commands);
		assertNotSame(first.task(), second.task());
		ViewNodeModelCache survivingView = (ViewNodeModelCache)NodeModelCacheFactory.getInstance()
				.createFilteredCache(second.task(), "surviving-view", null);

		first.close();
		first.close();
		survivingView.update();

		assertTrue(survivingView.getProjectionSnapshot().topologyRevision() >= 0L);
		survivingView.close();
		second.close();
	}
}
