/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.document;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.field.Field;
import com.microproject.pm.assignment.Assignment;
import com.microproject.association.Association;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.undo.NodeUndoInfo;

class ObjectEventManagerTest {
	@Test
	void assignmentApplicableTaskUpdateIsPropagatedToItsAssignments() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		NormalTask task = new NormalTask(project);
		project.connectTask(task);
		Association assignment = task.getAssignments().iterator().next();
		Field field = new Field();
		field.setClass(Assignment.class);
		List<Object> updatedObjects = new ArrayList<>();
		ObjectEventManager manager = new ObjectEventManager();
		manager.addListener(event -> updatedObjects.add(event.getObject()));

		manager.fireUpdateEvent(this, task, field);

		assertTrue(updatedObjects.contains(task));
		assertTrue(updatedObjects.contains(assignment));
	}

	@Test
	void listenerFailureStillResetsAndRecyclesPooledEvent() {
		Object source = new Object();
		ObjectEvent event = ObjectEvent.getInstance(source, new Object(), ObjectEvent.UPDATE, new NodeUndoInfo(true));
		event.setField(new Field());
		ObjectEventManager manager = new ObjectEventManager();
		manager.addListener(ignored -> {
			throw new IllegalStateException("listener failure");
		});

		assertThrows(IllegalStateException.class, () -> manager.fire(event));

		ObjectEvent reused = ObjectEvent.getInstance(new Object());
		try {
			assertSame(event, reused);
			assertNull(reused.getField());
			assertNull(reused.getInfo());
			assertNull(reused.getObject());
		} finally {
			reused.recycle();
		}
	}
}
