/*******************************************************************************
 * MIT License
 *
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
package com.microproject.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.io.ObjectStreamClass;
import java.lang.reflect.Constructor;

import org.junit.jupiter.api.Test;

class CircularDependencyExceptionSerializationTest {
	private static final long SERIAL_VERSION_UID = -5477943199083401004L;

	@Test
	void retainsSerializedClassNameUidAndConstructorSignatures() throws Exception {
		assertEquals("com.microproject.configuration.CircularDependencyException",
				CircularDependencyException.class.getName());
		assertEquals(SERIAL_VERSION_UID,
				ObjectStreamClass.lookup(CircularDependencyException.class).getSerialVersionUID());
		assertNotNull(CircularDependencyException.class.getConstructor());
		assertNotNull(CircularDependencyException.class.getConstructor(String.class));
		assertNotNull(CircularDependencyException.class.getConstructor(Throwable.class));
		assertNotNull(CircularDependencyException.class.getConstructor(String.class, Throwable.class));
	}

	@Test
	void allFourConstructorsRetainMessageAndCauseAcrossSerialization() throws Exception {
		IOException cause = new IOException("cause detail");
		assertRestores(new CircularDependencyException(), null, null);
		assertRestores(new CircularDependencyException("explicit message"), "explicit message", null);
		assertRestores(new CircularDependencyException(cause), cause.toString(), cause);
		assertRestores(new CircularDependencyException("wrapped message", cause), "wrapped message", cause);
	}

	private static void assertRestores(CircularDependencyException value, String expectedMessage,
			Throwable expectedCause) throws Exception {
		CircularDependencyException restored = roundTrip(value);
		assertEquals(expectedMessage, restored.getMessage());
		if (expectedCause == null) {
			assertNull(restored.getCause());
		} else {
			assertNotNull(restored.getCause());
			assertEquals(expectedCause.getClass(), restored.getCause().getClass());
			assertEquals(expectedCause.getMessage(), restored.getCause().getMessage());
		}
	}

	private static CircularDependencyException roundTrip(Serializable value) throws Exception {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
			output.writeObject(value);
		}
		try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
			return (CircularDependencyException) input.readObject();
		}
	}
}
