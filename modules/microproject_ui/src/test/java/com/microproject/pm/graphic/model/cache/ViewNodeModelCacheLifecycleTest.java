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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.grouping.core.Node;
import com.microproject.pm.graphic.graph.GraphModel;
import com.microproject.pm.graphic.network.Network;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

class ViewNodeModelCacheLifecycleTest {
	@Test
	void allBoundViewsInstallTheSameModelRevisionBeforeCacheListenersRun() throws Exception {
		Project project = createProject();
		NormalTask task = createTask(project, "Revision snapshot");
		ReferenceNodeModelCache reference = NodeModelCacheFactory.createTaskNodeModelCache(project,
				project.getTaskModel());
		ViewNodeModelCache first = (ViewNodeModelCache) NodeModelCacheFactory.getInstance()
				.createFilteredCache(reference, "revision-first", null);
		ViewNodeModelCache second = (ViewNodeModelCache) NodeModelCacheFactory.getInstance()
				.createFilteredCache(reference, "revision-second", null);
		AtomicBoolean listenerSawCoherentInstall = new AtomicBoolean();
		first.addNodeModelListener(event -> {
			assertTrue(SwingUtilities.isEventDispatchThread());
			assertEquals(project.getMutationState().revision(), first.getVisibleNodes().getProjectionIndex().modelRevision());
			assertEquals(first.getVisibleNodes().getProjectionIndex().modelRevision(),
					second.getVisibleNodes().getProjectionIndex().modelRevision());
			listenerSawCoherentInstall.set(true);
		});

		SwingUtilities.invokeAndWait(() -> {
			GraphicNode graphicNode = reference.getGraphicNode(project.getTaskModel().search(task));
			graphicNode.setDirty(true);
		});
		Thread publisher = new Thread(() -> project.fireScheduleChanged("background-test",
				com.microproject.pm.scheduling.ScheduleEvent.SCHEDULE));
		publisher.start();
		publisher.join();
		SwingUtilities.invokeAndWait(() -> { });

		assertTrue(listenerSawCoherentInstall.get(), "the changed node must notify after both views install");
		first.close();
		second.close();
		reference.close();
	}

	@Test
	void queuedOffEdtRefreshIsDiscardedAfterTheReferenceCacheCloses() throws Exception {
		Project project = createProject();
		AtomicBoolean closed = new AtomicBoolean();
		CountDownLatch callbackReturned = new CountDownLatch(1);
		SwingUtilities.invokeAndWait(() -> {
			ReferenceNodeModelCache reference = NodeModelCacheFactory.createTaskNodeModelCache(project,
					project.getTaskModel());
			assertTrue(reference.nodeCache.getCacheSize() > 0);
			Thread publisher = new Thread(() -> {
				project.fireScheduleChanged("background-test", com.microproject.pm.scheduling.ScheduleEvent.SCHEDULE);
				callbackReturned.countDown();
			});
			publisher.start();
			try {
				assertTrue(callbackReturned.await(2, TimeUnit.SECONDS));
			} catch (InterruptedException failure) {
				Thread.currentThread().interrupt();
				throw new AssertionError(failure);
			}
			reference.close();
			closed.set(true);
		});
		SwingUtilities.invokeAndWait(() -> assertTrue(closed.get()));
	}

	@Test
	void olderPublicationDoesNotReinstallOrNotifyAfterANewerRevision() throws Exception {
		Project project = createProject();
		NormalTask task = createTask(project, "Stale revision");
		ReferenceNodeModelCache reference = NodeModelCacheFactory.createTaskNodeModelCache(project,
				project.getTaskModel());
		ViewNodeModelCache view = (ViewNodeModelCache) NodeModelCacheFactory.getInstance()
				.createFilteredCache(reference, "stale-revision", null);
		AtomicInteger notifications = new AtomicInteger();
		view.addNodeModelListener(event -> notifications.incrementAndGet());
		for (int i = 0; i < 2; i++) {
			try (var mutation = project.getMutationState().begin()) {
				mutation.commit(true);
			}
		}
		SwingUtilities.invokeAndWait(() -> {
			reference.update();
			notifications.set(0);
			GraphicNode node = reference.getGraphicNode(project.getTaskModel().search(task));
			node.setDirty(true);
			com.microproject.pm.scheduling.ScheduleEvent stale = new com.microproject.pm.scheduling.ScheduleEvent(
				"stale-publication", com.microproject.pm.scheduling.ScheduleEvent.SCHEDULE);
			stale.setPublicationRevision(1L);
			reference.scheduleChanged(stale);
			assertEquals(2L, view.getVisibleNodes().getProjectionIndex().modelRevision());
		});
		assertEquals(0, notifications.get(), "an already-installed older revision is dropped without a redundant notify");
		view.close();
		reference.close();
	}

	@Test
	void publicationsCompletingOutOfOrderInstallOnlyTheNewestRevisionOnce() throws Exception {
		for (boolean newestFirst : new boolean[] { true, false }) {
			Project project = createProject();
			NormalTask task = createTask(project, "Out of order " + newestFirst);
			ReferenceNodeModelCache reference = NodeModelCacheFactory.createTaskNodeModelCache(project,
					project.getTaskModel());
			ViewNodeModelCache view = (ViewNodeModelCache) NodeModelCacheFactory.getInstance()
					.createFilteredCache(reference, "out-of-order-" + newestFirst, null);
			AtomicInteger notifications = new AtomicInteger();
			view.addNodeModelListener(event -> notifications.incrementAndGet());
			long older = project.getMutationState().revision();
			try (var mutation = project.getMutationState().begin()) {
				mutation.commit(true);
			}
			long newer = project.getMutationState().revision();
			assertTrue(newer > older);
			SwingUtilities.invokeAndWait(() -> {
				reference.getGraphicNode(project.getTaskModel().search(task)).setDirty(true);
				notifications.set(0);
			});
			// Both publications are produced off the EDT and queued in the given completion order.
			long[] order = newestFirst ? new long[] { newer, older } : new long[] { older, newer };
			for (long revision : order) {
				com.microproject.pm.scheduling.ScheduleEvent event = new com.microproject.pm.scheduling.ScheduleEvent(
						"out-of-order", com.microproject.pm.scheduling.ScheduleEvent.SCHEDULE);
				event.setPublicationRevision(revision);
				Thread publisher = new Thread(() -> reference.scheduleChanged(event));
				publisher.start();
				publisher.join();
			}
			SwingUtilities.invokeAndWait(() -> { });
			assertEquals(newer, view.getVisibleNodes().getProjectionIndex().modelRevision(),
					"only the newest revision may be installed");
			assertEquals(1, notifications.get(), "the superseded publication must not install or notify again");
			view.close();
			reference.close();
		}
	}

	@Test
	void structuralChangesKeepTableAndGanttProjectionsOnTheSameStableRows() throws Exception {
		Project project = createProject();
		NormalTask first = createTask(project, "First");
		NormalTask second = createTask(project, "Second");
		ReferenceNodeModelCache reference = NodeModelCacheFactory.createTaskNodeModelCache(project,
				project.getTaskModel());
		ViewNodeModelCache table = (ViewNodeModelCache) NodeModelCacheFactory.getInstance()
				.createFilteredCache(reference, "structure-table", null);
		ViewNodeModelCache gantt = (ViewNodeModelCache) NodeModelCacheFactory.getInstance()
				.createFilteredCache(reference, "structure-gantt", null);
		var model = (com.microproject.grouping.core.model.DefaultNodeModel) project.getTaskModel();
		SwingUtilities.invokeAndWait(() -> {
			reference.update();
			assertProjectionsAgree(table, gantt, "First", "Second");
			NormalTask inserted = createTask(project, "Inserted");
			reference.update();
			assertProjectionsAgree(table, gantt, "First", "Second", "Inserted");
			model.indent(java.util.List.of(model.search(second)), 1,
					com.microproject.grouping.core.model.NodeModel.SILENT);
			reference.update();
			assertProjectionsAgree(table, gantt, "First", "Second", "Inserted");
			model.remove(model.search(inserted), com.microproject.grouping.core.model.NodeModel.SILENT);
			reference.update();
			assertProjectionsAgree(table, gantt, "First", "Second");
		});
		table.close();
		gantt.close();
		reference.close();
	}

	private static void assertProjectionsAgree(ViewNodeModelCache table, ViewNodeModelCache gantt,
			String... expectedTaskNames) {
		RevisionedProjectionIndex left = table.getVisibleNodes().getProjectionIndex();
		RevisionedProjectionIndex right = gantt.getVisibleNodes().getProjectionIndex();
		java.util.List<String> names = new java.util.ArrayList<>();
		for (GraphicNode node : left.nodes())
			if (node.getNode().getImpl() instanceof NormalTask task && task.getName() != null
					&& Arrays.asList(expectedTaskNames).contains(task.getName()))
				names.add(task.getName());
		assertEquals(Arrays.asList(expectedTaskNames), names);
		assertEquals(left.rowKeys(), right.rowKeys(), "table and Gantt must show the same stable task ids in order");
		assertEquals(left.modelRevision(), right.modelRevision());
		assertEquals(left.topologyRevision(), right.topologyRevision());
	}

	@Test
	void queuedRefreshFromAnOldBindingIsDiscardedAfterRebind() throws Exception {
		Project project = createProject();
		ReferenceNodeModelCache reference = NodeModelCacheFactory.createTaskNodeModelCache(project,
				project.getTaskModel());
		ViewNodeModelCache view = (ViewNodeModelCache) NodeModelCacheFactory.getInstance()
				.createFilteredCache(reference, "rebound-view", null);
		AtomicInteger notifications = new AtomicInteger();
		view.addNodeModelListener(event -> notifications.incrementAndGet());
		CountDownLatch callbackReturned = new CountDownLatch(1);
		int[] afterRebind = new int[1];
		SwingUtilities.invokeAndWait(() -> {
			Thread publisher = new Thread(() -> {
				project.fireScheduleChanged("old-binding", com.microproject.pm.scheduling.ScheduleEvent.SCHEDULE);
				callbackReturned.countDown();
			});
			publisher.start();
			try {
				assertTrue(callbackReturned.await(2, TimeUnit.SECONDS));
			} catch (InterruptedException failure) {
				Thread.currentThread().interrupt();
				throw new AssertionError(failure);
			}
			reference.setModel(project.getTaskModel());
			afterRebind[0] = notifications.get();
		});
		SwingUtilities.invokeAndWait(() -> { });
		assertEquals(afterRebind[0], notifications.get(), "queued work from the old binding must not run after rebind");
		view.close();
		reference.close();
	}

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
		Network graph = new Network(project, "graph-lifecycle-test");
		GraphModel graphModel = graph.getModel();
		graph.setCache(view);
		assertTrue(Arrays.asList(view.getNodeModelListeners()).contains(graphModel));
		assertTrue(Arrays.asList(graphModel.getGraphListeners()).contains(graph));

		graph.cleanUp();
		graph.cleanUp();

		assertFalse(Arrays.asList(view.getNodeModelListeners()).contains(graphModel));
		assertFalse(Arrays.asList(graphModel.getGraphListeners()).contains(graph));
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
