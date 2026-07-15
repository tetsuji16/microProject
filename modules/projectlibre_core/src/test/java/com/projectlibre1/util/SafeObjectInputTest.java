package com.projectlibre1.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InvalidClassException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.junit.jupiter.api.Test;

class SafeObjectInputTest {
	@Test
	void acceptsProjectLibreAndJdkValueTypes() throws Exception {
		AllowedPayload expected = new AllowedPayload("project", new int[] { 1, 2, 3 });

		try (var input = SafeObjectInput.create(new ByteArrayInputStream(serialize(expected)))) {
			AllowedPayload actual = (AllowedPayload) input.readObject();
			assertEquals(expected.name, actual.name);
			assertArrayEquals(expected.values, actual.values);
		}
	}

	@Test
	void rejectsClassesOutsideThePersistenceAllowList() throws Exception {
		byte[] bytes = serialize(new File("untrusted"));

		try (var input = SafeObjectInput.create(new ByteArrayInputStream(bytes))) {
			assertThrows(InvalidClassException.class, input::readObject);
		}
	}

	@Test
	void rejectsOversizedArraysBeforeAllocatingThemOnRead() throws Exception {
		byte[] bytes = serialize(new byte[(int) SafeObjectInput.MAX_ARRAY_LENGTH + 1]);

		try (var input = SafeObjectInput.create(new ByteArrayInputStream(bytes))) {
			assertThrows(InvalidClassException.class, input::readObject);
		}
	}

	private static byte[] serialize(Object value) throws Exception {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
			output.writeObject(value);
		}
		return bytes.toByteArray();
	}

	private static final class AllowedPayload implements Serializable {
		private static final long serialVersionUID = 1L;
		private final String name;
		private final int[] values;

		private AllowedPayload(String name, int[] values) {
			this.name = name;
			this.values = values;
		}
	}
}
