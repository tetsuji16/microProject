/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.model.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.undo.DataFactoryUndoController;

class RevisionedProjectionIndexTest {
	@Test
	void eachViewResolvesItsOwnRowForTheSameTaskIdentities() {
		Project project = project();
		Task firstTask = task(project, 11L);
		Task secondTask = task(project, 12L);
		GraphicNode first = graphicNode(project, firstTask);
		GraphicNode second = graphicNode(project, secondTask);

		RevisionedProjectionIndex firstView = RevisionedProjectionIndex.create(List.of(first, second), null);
		RevisionedProjectionIndex secondView = RevisionedProjectionIndex.create(List.of(second, first), null);

		assertEquals(0, firstView.rowForKey(firstView.keyAt(0)));
		assertEquals(1, firstView.rowForKey(firstView.keyAt(1)));
		assertEquals(0, secondView.rowForKey(secondView.keyAt(0)));
		assertEquals(1, secondView.rowForKey(secondView.keyAt(1)));
		assertNotEquals(firstView.rowForKey(firstView.keyAt(0)), secondView.rowForKey(firstView.keyAt(0)));
	}

	@Test
	void taskKeyRemainsStableWhenAnEarlierRowIsRemoved() {
		Project project = project();
		Task removedTask = task(project, 21L);
		Task retainedTask = task(project, 22L);
		GraphicNode removed = graphicNode(project, removedTask);
		GraphicNode retained = graphicNode(project, retainedTask);
		RevisionedProjectionIndex before = RevisionedProjectionIndex.create(List.of(removed, retained), null);
		ProjectionRowKey retainedKey = before.keyAt(1);

		RevisionedProjectionIndex after = RevisionedProjectionIndex.create(List.of(retained), before);

		assertEquals(0, after.rowForKey(retainedKey));
		assertEquals(before.topologyRevision() + 1, after.topologyRevision());
	}

	@Test
	void topologyRevisionTracksStableKeyOrderAndSnapshotIsImmutable() {
		Project project = project();
		Task task = task(project, 31L);
		GraphicNode node = graphicNode(project, task);
		RevisionedProjectionIndex initial = RevisionedProjectionIndex.create(List.of(node), null);
		RevisionedProjectionIndex equivalent = RevisionedProjectionIndex.create(
				List.of(graphicNode(project, task)), initial);
		ArrayList<GraphicNode> source = new ArrayList<>(List.of(node));
		RevisionedProjectionIndex snapshot = RevisionedProjectionIndex.create(source, equivalent);
		source.clear();

		assertEquals(initial.topologyRevision(), equivalent.topologyRevision());
		assertEquals(1, snapshot.size());
		assertThrows(UnsupportedOperationException.class, () -> snapshot.nodes().clear());
	}

	@Test
	void repeatedGraphicNodeOccurrencesReceiveDistinctKeysAndRows() {
		Project project = project();
		GraphicNode node = graphicNode(project, task(project, 41L));
		RevisionedProjectionIndex index = RevisionedProjectionIndex.create(List.of(node, node), null);

		assertNotEquals(index.keyAt(0), index.keyAt(1));
		assertEquals(List.of(0, 1), index.rowsForNode(node));
		assertEquals(0, index.rowForNode(node));
	}

	private static Project project() {
		var undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("projection-index", undo), undo);
		project.setUniqueId(901L);
		project.initialize(false, false);
		return project;
	}

	private static Task task(Project project, long uniqueId) {
		Task task = project.createScriptedTask();
		task.setUniqueId(uniqueId);
		task.setProjectId(project.getUniqueId());
		project.connectTask(task);
		project.getTaskOutlines().addToAll(task, null);
		return task;
	}

	private static GraphicNode graphicNode(Project project, Task task) {
		return new GraphicNode(project.getTaskModel().search(task), 0);
	}
}
