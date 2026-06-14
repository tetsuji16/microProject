package com.projectlibre1.pm.task;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Iterator;

import org.junit.jupiter.api.Test;

import com.projectlibre1.pm.assignment.Assignment;
import com.projectlibre1.pm.resource.ResourcePool;
import com.projectlibre1.undo.DataFactoryUndoController;

class NormalTaskPercentCompleteTest {
	@Test
	void settingPercentCompleteSynchronizesAssignmentPercentages() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		NormalTask task = new NormalTask(project);
		project.connectTask(task);

		Assignment assignment = firstAssignment(task);
		assignment.setPercentComplete(0.2d);
		assertEquals(0.2d, task.getPercentComplete(), 0.00001d);

		task.setPercentComplete(1.0d);

		assertEquals(1.0d, assignment.getPercentComplete(), 0.00001d);
		assertEquals(1.0d, task.getPercentComplete(), 0.00001d);
	}

	@Test
	void settingPercentCompleteDoesNotChangePlannedBarBounds() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		NormalTask task = new NormalTask(project);
		project.connectTask(task);

		long originalStart = task.getStart();
		long originalEnd = task.getEnd();
		long originalDuration = task.getDuration();

		task.setPercentComplete(0.1d);
		task.setPercentComplete(1.0d);

		assertEquals(originalStart, task.getStart());
		assertEquals(originalEnd, task.getEnd());
		assertEquals(originalDuration, task.getDuration());
	}

	private Assignment firstAssignment(NormalTask task) {
		Iterator iterator = task.getAssignments().iterator();
		return (Assignment) iterator.next();
	}
}
