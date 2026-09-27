/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.task;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import com.microproject.grouping.core.Node;
import com.microproject.graphic.configuration.GraphicConfiguration;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.snapshot.DataSnapshot;
import com.microproject.pm.snapshot.Snapshottable;
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

	@Test
	void taskOutlineIteratorReturnsTasksAndRejectsReadsAfterExhaustion() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("iterator", undo), undo);
		project.initialize(false, false);
		NormalTask task = project.createScriptedTask();

		Iterator<Task> iterator = project.getTaskOutlineIterator();
		assertTrue(iterator.hasNext());
		assertSame(task, iterator.next());
		assertFalse(iterator.hasNext());
		assertThrows(NoSuchElementException.class, iterator::next);
	}

	@Test
	void rootNodesContainsTheOutlineNodeForEachRootTask() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("root-nodes", undo), undo);
		project.initialize(false, false);
		Task task = project.createScriptedTask();

		List<Node> roots = project.getRootNodes(List.of(task));

		assertEquals(1, roots.size());
		assertSame(task, roots.getFirst().getImpl());
	}

	@Test
	void wbsChildrenTasksReturnsImplementationsFromNodeCache() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("wbs-children", undo), undo);
		project.initialize(false, false);
		NormalTask parent = project.createScriptedTask();
		NormalTask child = project.createScriptedTask();
		Node childNode = project.getTaskModel().search(child);
		parent.setWbsChildrenNodes(List.of(childNode));

		assertEquals(List.of(child), parent.getWbsChildrenTasks());
	}

	@Test
	void rowHeightTracksIntegerBaselineIndexes() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("row-height", undo), undo);
		project.initialize(false, false);
		NormalTask task = project.createScriptedTask();
		task.setSnapshot(Snapshottable.BASELINE_2, new MarkerSnapshot());
		TreeSet<Integer> baselines = new TreeSet<>();

		assertEquals(GraphicConfiguration.getInstance().getRowHeight()
			+ 3 * GraphicConfiguration.getInstance().getBaselineHeight(), project.getRowHeight(baselines));
		assertEquals(new TreeSet<>(List.of(2)), baselines);
	}

	private record MarkerSnapshot() implements DataSnapshot { }

	@Test
	void descendantsReturnsDepthFirstTasksInStableOrder() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("descendants", undo), undo);
		project.initialize(false, false);
		NormalTask root = project.createScriptedTask();
		NormalTask firstChild = project.createScriptedTask();
		NormalTask grandchild = project.createScriptedTask();
		NormalTask secondChild = project.createScriptedTask();
		root.setWbsChildrenNodes(List.of(project.getTaskModel().search(firstChild), project.getTaskModel().search(secondChild)));
		firstChild.setWbsChildrenNodes(List.of(project.getTaskModel().search(grandchild)));

		assertEquals(List.of(firstChild, grandchild, secondChild), ProjectHierarchyQueries.descendants(root));
	}
}
