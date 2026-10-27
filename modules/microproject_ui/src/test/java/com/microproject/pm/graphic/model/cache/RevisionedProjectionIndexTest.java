/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.model.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

import com.microproject.exchange.LocalFileImporter;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.NodeFactory;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.ProjectTaskKey;
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

		VisibleNodes firstView = new VisibleNodes("first-view", ignored -> { });
		firstView.getElements().addAll(List.of(first, second));
		firstView.publishProjectionIndex();
		VisibleNodes secondView = new VisibleNodes("second-view", ignored -> { });
		secondView.getElements().addAll(List.of(second, first));
		secondView.publishProjectionIndex();
		ProjectionRowKey firstTaskKey = firstView.getProjectionIndex().keyAt(0);

		assertEquals(0, firstView.getRow(first));
		assertEquals(1, firstView.getRow(second));
		assertEquals(1, secondView.getProjectionIndex().rowForKey(firstTaskKey));
		assertEquals(0, secondView.getRow(second));
		assertEquals(1, secondView.getRow(first));
		assertNotEquals(firstView.getRow(first), secondView.getRow(first));
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
	void localSessionTaskIdsUseStableTaskRowsInsteadOfSyntheticRows() {
		Project project = project();
		project.setUniqueId(-901L);
		Task task = task(project, -902L);

		RevisionedProjectionIndex projection = RevisionedProjectionIndex.create(
			List.of(graphicNode(project, task)), null);

		assertTrue(projection.keyAt(0) instanceof ProjectionRowKey.TaskRow);
		ProjectTaskKey key = ((ProjectionRowKey.TaskRow) projection.keyAt(0)).taskKey();
		assertEquals(-901L, key.owningProjectId());
		assertEquals(-902L, key.taskUniqueId());
		assertEquals(project, key.projectScope());
	}

	@Test
	void sameRuntimeIdsInDifferentProjectInstancesHaveDistinctRows() {
		Project firstProject = project();
		Project secondProject = project();
		firstProject.setUniqueId(-901L);
		secondProject.setUniqueId(-901L);
		GraphicNode first = graphicNode(firstProject, task(firstProject, -902L));
		GraphicNode second = graphicNode(secondProject, task(secondProject, -902L));
		RevisionedProjectionIndex projection = RevisionedProjectionIndex.create(List.of(first, second), null);

		assertNotEquals(projection.keyAt(0), projection.keyAt(1));
		assertEquals(0, projection.rowForKey(projection.keyAt(0)));
		assertEquals(1, projection.rowForKey(projection.keyAt(1)));
	}

	@Test
	void podRoundTrippedExternalTaskWithoutOwningProjectStillGetsDocumentScopedRow() throws Exception {
		var localUndo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("external-task-projection", localUndo), localUndo);
		project.initialize(false, false);
		var externalUndo = new DataFactoryUndoController();
		Project external = Project.createProject(ResourcePool.createRourcePool("external-task-source", externalUndo), externalUndo);
		external.initialize(false, false);
		NormalTask successor = (NormalTask) project.createLocalTaskNode(null).getImpl();
		NormalTask externalTask = (NormalTask) external.createLocalTaskNode(null).getImpl();
		externalTask.setName("External predecessor");
		DependencyService.getInstance().newDependency(externalTask, successor, DependencyType.Kind.FS.code(), 0L, this);
		File sample = Files.createTempFile("external-task-projection", ".pod").toFile();
		sample.deleteOnExit();
		LocalFileImporter exporter = new LocalFileImporter();
		exporter.setFileName(sample.getAbsolutePath());
		exporter.setProject(project);
		exporter.exportFile();
		LocalFileImporter importer = new LocalFileImporter();
		importer.setFileName(sample.getAbsolutePath());
		importer.importFile();
		Project restored = importer.getProject();
		Task restoredExternal = restored.getTaskList().stream().filter(Task::isExternal).findFirst().orElseThrow();
		assertNull(restoredExternal.getOwningProject(), "Serializer external-task branch leaves owningProject unset");
		assertEquals(restored, restoredExternal.getProject(), "external placeholder belongs to the loaded document scope");
		ProjectTaskKey taskKey = ProjectTaskKey.from(restoredExternal).orElseThrow();
		assertEquals(restored, taskKey.projectScope());
		assertEquals(external.getUniqueId(), taskKey.owningProjectId());
		ProjectionRowKey key = ProjectionRowKey.forNode(new GraphicNode(
			NodeFactory.getInstance().createNode(restoredExternal), 0), 0);
		assertTrue(key instanceof ProjectionRowKey.TaskRow,
			"external POD task must not fall back to SyntheticRow");
		assertEquals(taskKey, ((ProjectionRowKey.TaskRow) key).taskKey());
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
	void projectionSnapshotCarriesTheModelRevisionItWasBuiltFrom() {
		Project project = project();
		GraphicNode node = graphicNode(project, task(project, 32L));
		RevisionedProjectionIndex previous = RevisionedProjectionIndex.create(List.of(node), null, 4L);
		RevisionedProjectionIndex next = RevisionedProjectionIndex.create(List.of(node), previous, 5L);

		assertEquals(4L, previous.modelRevision());
		assertEquals(5L, next.modelRevision());
		assertEquals(previous.topologyRevision(), next.topologyRevision(),
			"a model revision can advance without changing the visible row topology");
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

	@Test
	void syntheticRowsKeepNodeIdentityAndOccurrenceAcrossReorder() {
		Node firstSyntheticNode = NodeFactory.getInstance().createVoidNode();
		Node secondSyntheticNode = NodeFactory.getInstance().createVoidNode();
		GraphicNode first = new GraphicNode(firstSyntheticNode, 0);
		GraphicNode repeatedFirst = new GraphicNode(firstSyntheticNode, 1);
		GraphicNode second = new GraphicNode(secondSyntheticNode, 0);
		RevisionedProjectionIndex before = RevisionedProjectionIndex.create(
				List.of(first, repeatedFirst, second), null);
		ProjectionRowKey firstOccurrence = before.keyAt(0);
		ProjectionRowKey secondOccurrence = before.keyAt(1);
		ProjectionRowKey otherNode = before.keyAt(2);

		RevisionedProjectionIndex after = RevisionedProjectionIndex.create(
				List.of(second, first, repeatedFirst), before);

		assertNotEquals(firstOccurrence, secondOccurrence);
		assertNotEquals(firstOccurrence, otherNode);
		assertEquals(1, after.rowForKey(firstOccurrence));
		assertEquals(2, after.rowForKey(secondOccurrence));
		assertEquals(0, after.rowForKey(otherNode));
		assertEquals(before.topologyRevision() + 1, after.topologyRevision());
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
