/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.task;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import com.microproject.grouping.core.Node;
import com.microproject.pm.resource.ResourcePool;
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
}
