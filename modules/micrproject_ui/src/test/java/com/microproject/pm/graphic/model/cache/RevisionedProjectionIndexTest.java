/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package com.microproject.pm.graphic.model.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.pm.graphic.gantt.Gantt;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.grouping.core.Node;

class RevisionedProjectionIndexTest {
	@Test
	void durableKeySurvivesRowDeletionAndRevisionAdvances() {
		Project project = createProject("projection-key");
		NormalTask first = createTask(project, "First");
		NormalTask second = createTask(project, "Second");
		ViewNodeModelCache cache = createViewCache(project, "projection-key-view");
		onEdt(cache::update);

		GraphicNode firstNode = (GraphicNode) cache.getGraphicNode(project.getTaskModel().search(first));
		GraphicNode secondNode = (GraphicNode) cache.getGraphicNode(project.getTaskModel().search(second));
		RevisionedProjectionIndex.Snapshot before = cache.getProjectionSnapshot();
		ProjectionRowKey secondKey = before.keyAt(before.rowOf(secondNode));
		int oldRow = before.rowOf(secondNode);

		onEdt(() -> {
			cache.deleteNodes(List.of(firstNode.getNode()));
			cache.update();
		});

		RevisionedProjectionIndex.Snapshot after = cache.getProjectionSnapshot();
		assertTrue(after.topologyRevision() > before.topologyRevision());
		assertEquals(secondKey, after.keyAt(after.rowOf(secondNode)));
		assertNotEquals(oldRow, after.rowOf(secondNode));
		assertEquals(after.rowOf(secondNode), cache.getRowAt(secondKey));
	}

	@Test
	void closingOneViewDoesNotCloseSharedReferenceOrOtherView() {
		Project project = createProject("projection-lifetime");
		createTask(project, "Existing");
		ReferenceNodeModelCache reference = NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel());
		ViewNodeModelCache first = (ViewNodeModelCache) NodeModelCacheFactory.getInstance()
				.createFilteredCache(reference, "same-name-view", null);
		ViewNodeModelCache second = (ViewNodeModelCache) NodeModelCacheFactory.getInstance()
				.createFilteredCache(reference, "same-name-view", null);
		onEdt(first::update);
		onEdt(second::update);

		first.close();
		first.close();
		createTask(project, "After close");
		onEdt(second::update);

		assertTrue(second.getSize() >= 2);
		assertTrue(second.getProjectionSnapshot().topologyRevision() > 0L);
	}

	@Test
	void unchangedRefreshDoesNotInventANewTopologyRevision() {
		Project project = createProject("projection-revision");
		createTask(project, "Existing");
		ViewNodeModelCache cache = createViewCache(project, "projection-revision-view");
		onEdt(cache::update);
		long revision = cache.getProjectionSnapshot().topologyRevision();

		onEdt(cache::update);

		assertEquals(revision, cache.getProjectionSnapshot().topologyRevision());
	}

	@Test
	void domainRevisionCanAdvanceWithoutInventingATopologyRevision() {
		RevisionedProjectionIndex index = new RevisionedProjectionIndex();
		RevisionedProjectionIndex.Snapshot before = index.refresh(List.of(), 3L);

		RevisionedProjectionIndex.Snapshot after = index.refresh(List.of(), 4L);

		assertEquals(4L, after.domainRevision());
		assertEquals(before.topologyRevision(), after.topologyRevision());
	}

	@Test
	void cleanedUpGraphUnsubscribesFromTheViewCache() {
		Project project = createProject("graph-lifetime");
		ViewNodeModelCache cache = createViewCache(project, "graph-lifetime-view");
		int listenersBefore = cache.getNodeModelListeners().length;
		Gantt gantt = new Gantt(project, "Gantt");
		gantt.setCache(cache);
		assertEquals(listenersBefore + 1, cache.getNodeModelListeners().length);

		gantt.cleanUp();

		assertEquals(listenersBefore, cache.getNodeModelListeners().length);
		cache.close();
	}

	@Test
	void twoViewIndexesCanProjectTheSameNodesInOppositeOrders() {
		Project project = createProject("projection-isolation");
		NormalTask first = createTask(project, "First");
		NormalTask second = createTask(project, "Second");
		ViewNodeModelCache cache = createViewCache(project, "projection-isolation-source");
		onEdt(cache::update);
		GraphicNode firstNode = (GraphicNode) cache.getGraphicNode(project.getTaskModel().search(first));
		GraphicNode secondNode = (GraphicNode) cache.getGraphicNode(project.getTaskModel().search(second));
		RevisionedProjectionIndex standard = new RevisionedProjectionIndex();
		RevisionedProjectionIndex tracking = new RevisionedProjectionIndex();

		RevisionedProjectionIndex.Snapshot standardSnapshot = standard.refresh(List.of(firstNode, secondNode));
		RevisionedProjectionIndex.Snapshot trackingSnapshot = tracking.refresh(List.of(secondNode, firstNode));

		assertEquals(0, standardSnapshot.rowOf(firstNode));
		assertEquals(1, standardSnapshot.rowOf(secondNode));
		assertEquals(1, trackingSnapshot.rowOf(firstNode));
		assertEquals(0, trackingSnapshot.rowOf(secondNode));
		assertEquals(0, standardSnapshot.rowOf(firstNode));
		cache.close();
	}

	@Test
	void collapseStateIsIndependentBetweenTwoRealViewCaches() {
		Project project = createProject("collapse-isolation");
		NormalTask parentTask = createTask(project, "Parent");
		NormalTask childTask = createTask(project, "Child");
		Node childNode = project.getTaskModel().search(childTask);
		project.getTaskModel().getHierarchy().indent(List.of(childNode), 1, project.getTaskModel(),
				com.microproject.grouping.core.model.NodeModel.EVENT);
		ReferenceNodeModelCache reference = NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel());
		ViewNodeModelCache standard = (ViewNodeModelCache) NodeModelCacheFactory.getInstance()
				.createFilteredCache(reference, "same-view-name", null);
		ViewNodeModelCache tracking = (ViewNodeModelCache) NodeModelCacheFactory.getInstance()
				.createFilteredCache(reference, "same-view-name", null);
		onEdt(standard::update);
		onEdt(tracking::update);
		GraphicNode parent = (GraphicNode) standard.getGraphicNode(project.getTaskModel().search(parentTask));
		GraphicNode child = (GraphicNode) standard.getGraphicNode(childNode);

		onEdt(() -> standard.changeCollapsedState(parent));

		assertTrue(standard.isCollapsed(parent));
		assertEquals(-1, standard.getRowAt(child));
		assertTrue(!tracking.isCollapsed(parent));
		assertTrue(tracking.getRowAt(child) >= 0);
		standard.close();
		tracking.close();
		reference.close();
	}

	@Test
	void finalValueSnapshotContainsNoMutableDomainOrGraphicHandles() {
		Project project = createProject("value-snapshot");
		createTask(project, "Captured");
		ViewNodeModelCache cache = createViewCache(project, "value-snapshot-view");
		onEdt(cache::update);

		TaskProjectionSnapshot snapshot = cache.getTaskProjectionSnapshot();
		ViewNodeModelCache.InstalledProjectionSnapshot installed = cache.getInstalledProjectionSnapshot();
		assertSame(snapshot, installed.values());
		assertSame(cache.getProjectionSnapshot(), installed.topology());
		assertEquals(cache.getProjectionSnapshot().domainRevision(), snapshot.domainRevision());
		assertEquals(cache.getProjectionSnapshot().topologyRevision(), snapshot.topologyRevision());
		assertTrue(snapshot.rows().stream().noneMatch(row -> row.key() == null));
		for (var component : TaskProjectionSnapshot.Row.class.getRecordComponents()) {
			Class<?> type = component.getType();
			assertTrue(type != GraphicNode.class && type != Node.class && !Task.class.isAssignableFrom(type),
					"mutable handle in value snapshot: " + component.getName());
		}
		cache.close();
	}

	private static ViewNodeModelCache createViewCache(Project project, String name) {
		ReferenceNodeModelCache reference = NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel());
		return (ViewNodeModelCache) NodeModelCacheFactory.getInstance().createFilteredCache(reference, name, null);
	}

	private static void onEdt(Runnable action) {
		try {
			if (SwingUtilities.isEventDispatchThread()) action.run();
			else SwingUtilities.invokeAndWait(action);
		} catch (Exception failure) {
			throw new AssertionError(failure);
		}
	}

	private static Project createProject(String name) {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool(name, undo), undo);
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
