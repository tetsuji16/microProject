/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.document;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
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
import com.microproject.model.assignment.Assignment;
import com.microproject.association.Association;
import com.microproject.model.resource.ResourcePool;
import com.microproject.model.task.NormalTask;
import com.microproject.model.task.Project;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.undo.NodeUndoInfo;

class ObjectEventManagerTest {
	@Test
	@Timeout(value = 5, unit = TimeUnit.SECONDS)
	void deferredEventsArePublishedAsOrderedFacts() {
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
	void nestedRollbackDiscardsFacts() {
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
	void listenerFailurePropagatesAndLaterEventsStartEmpty() {
		Object source = new Object();
		Object object = new Object();
		NodeUndoInfo info = new NodeUndoInfo(true);
		Field field = new Field();
		ObjectEvent event = ObjectEvent.getInstance(source, object, ObjectEvent.UPDATE, info);
		event.setField(field);
		event.setPublicationRevision(7L);
		ObjectEventManager manager = new ObjectEventManager();
		manager.addListener(ignored -> {
			throw new IllegalStateException("listener failure");
		});

		assertThrows(IllegalStateException.class, () -> manager.fire(event));

		// The failed event keeps its facts for any caller that still holds it.
		assertSame(source, event.getSource());
		assertSame(object, event.getObject());
		assertSame(info, event.getInfo());
		assertSame(field, event.getField());

		Object nextSource = new Object();
		ObjectEvent next = ObjectEvent.getInstance(nextSource);
		assertNotSame(event, next);
		assertSame(nextSource, next.getSource());
		assertNull(next.getField());
		assertNull(next.getInfo());
		assertNull(next.getObject());
		assertEquals(ObjectEvent.UPDATE, next.getType());
		assertEquals(-1L, next.getPublicationRevision());
	}

	@Test
	void eachInstanceIsANewEventEvenWhileAnotherIsBeingDispatched() {
		ObjectEventManager manager = new ObjectEventManager();
		List<ObjectEvent> nested = new ArrayList<>();
		ObjectEvent outer = ObjectEvent.getInstance(this, new Object(), ObjectEvent.UPDATE, null);
		manager.addListener(event -> {
			if (event == outer) {
				ObjectEvent inner = ObjectEvent.getInstance(this, new Object(), ObjectEvent.CREATE, null);
				nested.add(inner);
				manager.fire(inner);
			}
		});

		manager.fire(outer);

		assertEquals(1, nested.size());
		assertNotSame(outer, nested.get(0));
		assertEquals(ObjectEvent.UPDATE, outer.getType(), "dispatching a nested event must not rewrite the outer one");
		assertEquals(ObjectEvent.CREATE, nested.get(0).getType());
	}

	private record CapturedEvent(Object source, Object object, int type,
		NodeUndoInfo info, Field field, long revision) { }
}
