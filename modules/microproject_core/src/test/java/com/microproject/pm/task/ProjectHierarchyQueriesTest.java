/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.task;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import com.microproject.datatype.Duration;
import com.microproject.datatype.DurationDefaults;
import com.microproject.datatype.TimeUnit;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.grouping.core.Node;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.DateTime;

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
	void setLocalParentCanOutdentAnExistingTask() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("local-parent", undo), undo);
		project.initialize(false, false);
		NormalTask parent = project.createScriptedTask();
		NormalTask child = project.createScriptedTask();

		project.setLocalParent(child, parent);
		assertSame(parent, child.getWbsParentTask());

		project.setLocalParent(child, null);
		assertNull(child.getWbsParentTask());
		assertTrue(parent.getWbsChildrenTasks().isEmpty(),
			"outdenting the last child must clear the old parent's WBS children snapshot");
	}

	@Test
	void summaryOutdentAndUndoRedoKeepTheDerivedLeafScheduleValid() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("summary-outdent", undo), undo);
		project.initialize(false, false);
		project.setDurationDefaults(new DurationDefaults(360.0d, 1800.0d, 20.0d,
			TimeUnit.HOURS, TimeUnit.HOURS));
		NormalTask parent = project.createScriptedTask();
		parent.setName("Summary parent");
		NormalTask child = project.createScriptedTask();
		child.setName("Six-hour child");
		long start = project.getEffectiveWorkCalendar().adjustInsideCalendar(DateTime.midnightToday(), false);
		child.getCurrentSchedule().setStart(start);
		child.setDuration(Duration.getInstance(6.0d, TimeUnit.HOURS, project.getDurationDefaults()));
		child.setPercentComplete(1.0d);
		assertEquals(6L * 60L * 60L * 1000L, Duration.millis(child.getRawDuration()), "initial child duration");
		assertEquals(6L * 60L * 60L * 1000L, child.calculateRollupSpan().getDuration(),
			"child span start=" + child.getStart() + " finish=" + child.getEnd());
		Node childNode = project.getTaskModel().search(child);

		int undoableEvent = NodeModel.EVENT | NodeModel.UNDO;
		project.getTaskModel().indent(List.of(childNode), 1, undoableEvent);
		assertSame(parent, child.getWbsParentTask());
		assertEquals(6L * 60L * 60L * 1000L, parent.calculateRollupSpan().getDuration(),
			"child raw=" + Duration.millis(child.getRawDuration()) + " start=" + child.getStart() + " end=" + child.getEnd());
		assertEquals(1.0d, parent.getPercentComplete(), 0.00001d);
		undo.clear(); // Keep the structural undo below focused on outdenting the child.
		project.getTaskModel().indent(List.of(childNode), -1, undoableEvent);
		assertNull(child.getWbsParentTask());
		assertTrue(parent.getWbsChildrenTasks().isEmpty());
		assertValidLeafSchedule(parent, 6L * 60L * 60L * 1000L, "outdent");
		assertEquals(1.0d, parent.getPercentComplete(), 0.00001d);

		undo.undo();
		assertSame(parent, child.getWbsParentTask());
		undo.redo();
		assertNull(child.getWbsParentTask());
		assertValidLeafSchedule(parent, 6L * 60L * 60L * 1000L, "redo outdent");
	}

	@Test
	void setLocalParentOutdentUsesTheHierarchyEventTransitionOnce() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("summary-local-parent", undo), undo);
		project.initialize(false, false);
		project.setDurationDefaults(new DurationDefaults(360.0d, 1800.0d, 20.0d,
			TimeUnit.HOURS, TimeUnit.HOURS));
		NormalTask parent = project.createScriptedTask();
		NormalTask child = project.createScriptedTask();
		long start = project.getEffectiveWorkCalendar().adjustInsideCalendar(DateTime.midnightToday(), false);
		child.getCurrentSchedule().setStart(start);
		child.setDuration(Duration.getInstance(6.0d, TimeUnit.HOURS, project.getDurationDefaults()));
		child.setPercentComplete(1.0d);
		AtomicBoolean leafStateWasValidWhenPublished = new AtomicBoolean(true);
		project.addScheduleListener(event -> {
			if ("SCHEDULE".equals(event.getType()) && !parent.isWbsParent()
					&& (parent.getEnd() < parent.getStart()
						|| Duration.millis(parent.getRawDuration()) != 6L * 60L * 60L * 1000L))
				leafStateWasValidWhenPublished.set(false);
		});

		project.setLocalParent(child, parent);
		assertSame(parent, child.getWbsParentTask());
		assertEquals(6L * 60L * 60L * 1000L, parent.calculateRollupSpan().getDuration());
		undo.clear();
		project.setLocalParent(child, null);
		assertNull(child.getWbsParentTask());
		assertTrue(parent.getWbsChildrenTasks().isEmpty());
		assertValidLeafSchedule(parent, 6L * 60L * 60L * 1000L, "setLocalParent outdent");
		assertEquals(1.0d, parent.getPercentComplete(), 0.00001d);
		assertTrue(leafStateWasValidWhenPublished.get(), "schedule listeners must see only the normalized leaf schedule");
		undo.undo();
		assertSame(parent, child.getWbsParentTask());
		assertEquals(6L * 60L * 60L * 1000L, parent.calculateRollupSpan().getDuration(), "undo outdent");
		undo.redo();
		assertNull(child.getWbsParentTask());
		assertValidLeafSchedule(parent, 6L * 60L * 60L * 1000L, "redo outdent");
	}

	private static void assertValidLeafSchedule(NormalTask task, long expectedDuration, String phase) {
		assertFalse(task.isWbsParent());
		assertTrue(task.getEnd() >= task.getStart(), phase);
		assertEquals(expectedDuration, Duration.millis(task.getRawDuration()), phase + " start=" + task.getStart()
			+ " end=" + task.getEnd() + " raw=" + task.getRawDuration());
	}

	@Test
	void unassignedProgressKeepsPlannedFinishAcrossHierarchyChanges() {
		for (double percentComplete : List.of(0.0d, 0.1d, 0.5d, 0.99d, 1.0d)) {
			DataFactoryUndoController undo = new DataFactoryUndoController();
			Project project = Project.createProject(ResourcePool.createRourcePool("progress-reparent", undo), undo);
			project.initialize(false, false);
			project.setDurationDefaults(new DurationDefaults(360.0d, 1800.0d, 20.0d,
				TimeUnit.HOURS, TimeUnit.HOURS));
			NormalTask parent = project.createScriptedTask();
			NormalTask child = project.createScriptedTask();
			long start = project.getEffectiveWorkCalendar().adjustInsideCalendar(DateTime.midnightToday(), false);
			child.getCurrentSchedule().setStart(start);
			child.setDuration(Duration.getInstance(6.0d, TimeUnit.HOURS, project.getDurationDefaults()));
			child.setPercentComplete(percentComplete);
			Node childNode = project.getTaskModel().search(child);
			int event = NodeModel.EVENT;

			project.getTaskModel().indent(List.of(childNode), 1, event);
			assertEquals(6L * 60L * 60L * 1000L, parent.calculateRollupSpan().getDuration(),
				"summary rollup at progress " + percentComplete);
			project.getTaskModel().indent(List.of(childNode), -1, event);
			assertValidLeafSchedule(parent, 6L * 60L * 60L * 1000L, "outdent at progress " + percentComplete);
			assertEquals(percentComplete, parent.getPercentComplete(), 0.0001d,
				"leaf progress after outdent " + percentComplete);
		}
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
