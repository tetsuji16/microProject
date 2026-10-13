/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.document;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

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
	@Timeout(value = 5, unit = TimeUnit.SECONDS)
	void pooledEventsAreReturnedWhileBatchRetainsOrderedFacts() {
		ObjectEventManager manager = new ObjectEventManager();
		List<CapturedEvent> observed = new ArrayList<>();
		manager.addListener(event -> observed.add(new CapturedEvent(event.getSource(), event.getObject(),
			event.getType(), event.getInfo(), event.getField(), event.getPublicationRevision())));
		Object source = new Object();
		List<Object> objects = new ArrayList<>();
		List<NodeUndoInfo> infos = new ArrayList<>();
		List<Field> fields = new ArrayList<>();
		manager.beginBatch();

		for (int index = 0; index < 32; index++) {
			Object object = new Object();
			NodeUndoInfo info = new NodeUndoInfo(index % 2 == 0);
			Field field = new Field();
			objects.add(object);
			infos.add(info);
			fields.add(field);
			ObjectEvent event = ObjectEvent.getInstance(source, object,
				index % 2 == 0 ? ObjectEvent.CREATE : ObjectEvent.UPDATE, info);
			event.setField(field);
			manager.fire(event);
		}
		assertEquals(32, manager.pendingEventCount());

		ObjectEventManager.PendingBatch batch = manager.detachBatch(true);
		manager.publishBatch(batch, 41L);

		assertEquals(32, observed.size());
		for (int index = 0; index < observed.size(); index++) {
			CapturedEvent event = observed.get(index);
			assertSame(source, event.source());
			assertSame(objects.get(index), event.object());
			assertEquals(index % 2 == 0 ? ObjectEvent.CREATE : ObjectEvent.UPDATE, event.type());
			assertSame(infos.get(index), event.info());
			assertSame(fields.get(index), event.field());
			assertEquals(41L, event.revision());
		}
	}

	@Test
	void nestedRollbackDiscardsFactsAndRecyclesEventsImmediately() {
		ObjectEventManager manager = new ObjectEventManager();
		List<Object> observed = new ArrayList<>();
		manager.addListener(event -> observed.add(event.getObject()));
		manager.beginBatch();
		manager.fireCreateEvent(this, new Object());
		manager.beginBatch();
		manager.fireDeleteEvent(this, new Object());
		manager.endBatch(false);
		manager.endBatch(true);

		assertTrue(observed.isEmpty());
		assertEquals(0, manager.pendingEventCount());
	}

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

	private record CapturedEvent(Object source, Object object, int type,
		NodeUndoInfo info, Field field, long revision) { }
}
