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
package com.microproject.server.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

/**
 * Issue #177: AssignmentData, LinkData and DataObjectImpl override equals() and
 * now implement hashCode() consistently, so they are safe to use in HashMaps /
 * HashSets (IncrementalData stores AssignmentData and LinkData in sets keyed by
 * their equality semantics).
 */
public class DataObjectEqualsHashCodeTest {

	@Test
	public void assignmentDataEqualsImpliesSameHashCode() {
		AssignmentData a = new AssignmentData();
		AssignmentData b = new AssignmentData();
		a.setUniqueId(42L);
		b.setUniqueId(42L);
		a.setTaskId(7L);
		b.setTaskId(7L);
		a.setSnapshotId(1);
		b.setSnapshotId(1);

		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());

		// snapshotId is part of equality, so a different snapshot is not equal
		b.setSnapshotId(2);
		assertNotEquals(a, b);
	}

	@Test
	public void linkDataEqualsImpliesSameHashCode() {
		LinkData a = new LinkData();
		LinkData b = new LinkData();
		a.setUniqueId(9L);
		b.setUniqueId(9L);
		a.setPredecessorId(5L);
		b.setPredecessorId(5L);

		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());

		b.setPredecessorId(6L);
		assertNotEquals(a, b);
	}

	@Test
	public void dataObjectImplEqualsImpliesSameHashCode() {
		DataObjectImpl a = new DataObjectImpl();
		DataObjectImpl b = new DataObjectImpl();
		a.setId(3L);
		b.setId(3L);
		a.setUniqueId(3L);
		b.setUniqueId(3L);

		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());

		b.setId(4L);
		assertNotEquals(a, b);
	}
}
