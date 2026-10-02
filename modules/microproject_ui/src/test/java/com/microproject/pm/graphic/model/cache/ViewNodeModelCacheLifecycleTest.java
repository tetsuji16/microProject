/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.model.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.microproject.grouping.core.Node;
import com.microproject.pm.graphic.graph.GraphModel;
import com.microproject.pm.graphic.graph.event.GraphListener;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

class ViewNodeModelCacheLifecycleTest {
	@Test
	void closingOneViewIsIdempotentAndLeavesSharedReferenceAndOtherViewLive() {
		Project project = createProject();
		ReferenceNodeModelCache reference = NodeModelCacheFactory.createTaskNodeModelCache(project,
				project.getTaskModel());
		ViewNodeModelCache closedView = (ViewNodeModelCache) NodeModelCacheFactory.getInstance()
				.createFilteredCache(reference, "closed-view", null);
		ViewNodeModelCache liveView = (ViewNodeModelCache) NodeModelCacheFactory.getInstance()
				.createFilteredCache(reference, "live-view", null);
		int initialLiveSize = liveView.getSize();

		closedView.close();
		closedView.close();

		assertEquals(0, closedView.getVisibleNodes().getNodeModelListeners().length);
		assertFalse(reference.nodeCache.getVisibleElements().contains(closedView.getVisibleNodes()));
		assertTrue(reference.nodeCache.getVisibleElements().contains(liveView.getVisibleNodes()));
		assertSame(project.getTaskModel(), reference.getModel());

		NormalTask task = createTask(project, "Live after close");
		reference.update();
		Node taskNode = project.getTaskModel().search(task);

		assertTrue(liveView.getSize() > initialLiveSize,
				"an open view sharing the reference cache must receive later model updates");
		assertTrue(liveView.getGraphicNode(taskNode) != null);
		assertEquals(0, closedView.getSize(), "a closed view must not be repopulated by a late cache event");

		liveView.close();
		reference.close();
	}

	@Test
	void graphCleanupDetachesItsCacheListenerWithoutClosingTheSharedCache() {
		Project project = createProject();
		ReferenceNodeModelCache reference = NodeModelCacheFactory.createTaskNodeModelCache(project,
				project.getTaskModel());
		ViewNodeModelCache view = (ViewNodeModelCache) NodeModelCacheFactory.getInstance()
				.createFilteredCache(reference, "graph-view", null);
		GraphModel graphModel = new GraphModel(project, "graph-lifecycle-test");
		GraphListener listener = event -> {};
		graphModel.addGraphListener(listener);
		graphModel.setCache(view);
		assertTrue(Arrays.asList(view.getNodeModelListeners()).contains(graphModel));

		graphModel.close();
		graphModel.close();

		assertFalse(Arrays.asList(view.getNodeModelListeners()).contains(graphModel));
		assertSame(project.getTaskModel(), reference.getModel(),
				"graph teardown releases only its listener and does not close the document cache");
		view.close();
		reference.close();
	}

	@Test
	void defaultViewOwnsAndClosesItsReferenceCache() {
		Project project = createProject();
		ViewNodeModelCache ownedView = (ViewNodeModelCache) NodeModelCacheFactory.getInstance()
				.createDefaultCache(project.getTaskModel(), project, NodeModelCache.TASK_TYPE,
						"owned-view", null);
		ReferenceNodeModelCache ownedReference = ownedView.getReference();

		assertTrue(ownedReference.nodeCache.getCacheSize() > 0);
		ownedView.close();
		ownedView.close();
		ownedReference.update();

		assertEquals(0, ownedReference.nodeCache.getCacheSize(),
				"a closed owned reference cache must not rebuild on a late update");
		assertEquals(0, ownedView.getSize());
	}

	private static Project createProject() {
		var undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("cache-lifecycle", undo), undo);
		project.initialize(false, false);
		return project;
	}

	private static NormalTask createTask(Project project, String name) {
		NormalTask task = project.createScriptedTask();
		task.setName(name);
		project.connectTask(task);
		project.getTaskOutlines().addToAll(task, null);
		return task;
	}

}
