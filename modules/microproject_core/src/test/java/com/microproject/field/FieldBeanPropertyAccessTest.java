/*
 * MIT License
 * Copyright (c) 2026 microProject
 */
package com.microproject.field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class FieldBeanPropertyAccessTest {
	@Test
	void readsSimplePropertiesForValuesAndReferences() {
		Field field = new Field();
		field.setProperty("name");
		field.setReferencedObjectProperty("task");
		field.setReferencedIdProperty("uniqueId");
		TaskReference reference = new TaskReference();

		assertEquals("Task name", field.getValueFromProperty(reference));
		assertSame(reference.task, field.getReferencedObject(reference));
		assertEquals(42L, field.getReferencedId(reference));
	}

	@Test
	void missingOrNonSimplePropertiesKeepReturningNull() {
		Field field = new Field();
		field.setProperty("nested.name");
		field.setReferencedObjectProperty("missing");
		field.setReferencedIdProperty("nested.uniqueId");
		TaskReference reference = new TaskReference();

		assertNull(field.getValueFromProperty(reference));
		assertNull(field.getReferencedObject(reference));
		assertNull(field.getReferencedId(reference));
	}

	public static final class TaskReference {
		private final Task task = new Task();

		public String getName() {
			return "Task name";
		}

		public Task getTask() {
			return task;
		}

		public Long getUniqueId() {
			return 42L;
		}
	}

	public static final class Task {
	}
}
