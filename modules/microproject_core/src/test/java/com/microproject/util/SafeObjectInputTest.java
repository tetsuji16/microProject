/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
 * Copyright (c) 2026 microProject
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package com.microproject.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InvalidClassException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.TimeZone;
import java.util.concurrent.locks.ReentrantLock;

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
	void acceptsLegacyPodTimeZonesAndLocks() throws Exception {
		Object[] expected = { TimeZone.getTimeZone("Asia/Tokyo"), new ReentrantLock() };

		try (var input = SafeObjectInput.create(new ByteArrayInputStream(serialize(expected)))) {
			Object[] actual = (Object[]) input.readObject();
			assertEquals(expected[0], actual[0]);
			assertEquals(ReentrantLock.class, actual[1].getClass());
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
