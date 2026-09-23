/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.document;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.microproject.field.Field;
import com.microproject.undo.NodeUndoInfo;

class ObjectEventManagerTest {
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
