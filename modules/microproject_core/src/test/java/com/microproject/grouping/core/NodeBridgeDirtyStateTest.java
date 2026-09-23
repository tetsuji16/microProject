/*
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
 */
package com.microproject.grouping.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.microproject.server.data.DataObject;

class NodeBridgeDirtyStateTest {
	@Test
	void dirtyStateDelegatesOnlyForDataObjects() {
		MutableDataObject dataObject = new MutableDataObject();
		Node dataNode = NodeFactory.getInstance().createNode(dataObject);
		Node otherNode = NodeFactory.getInstance().createNode(new Object());

		assertFalse(dataNode.isDirty());
		dataNode.setDirty(true);
		assertTrue(dataNode.isDirty());
		dataNode.setDirty(false);
		assertFalse(dataNode.isDirty());

		assertFalse(otherNode.isDirty());
		otherNode.setDirty(true);
		assertFalse(otherNode.isDirty());

		Node validLazyParent = NodeFactory.getInstance().createNode(new TestLazyParent(true));
		Node invalidLazyParent = NodeFactory.getInstance().createNode(new TestLazyParent(false));
		assertTrue(((NodeBridge) validLazyParent).isValidLazyParent());
		assertFalse(((NodeBridge) invalidLazyParent).isValidLazyParent());
		assertFalse(((NodeBridge) otherNode).isValidLazyParent());
	}

	private record TestLazyParent(boolean isValid) implements LazyParent {
		@Override
		public boolean isDataFetched() {
			return false;
		}

		@Override
		public boolean fetchData(Node node) {
			return false;
		}
	}

	private static final class MutableDataObject implements DataObject {
		private boolean dirty;

		@Override
		public String getName() {
			return "test";
		}

		@Override
		public void setName(String name) {
		}

		@Override
		public long getUniqueId() {
			return 1L;
		}

		@Override
		public void setUniqueId(long id) {
		}

		@Override
		public boolean isDirty() {
			return dirty;
		}

		@Override
		public void setDirty(boolean dirty) {
			this.dirty = dirty;
		}
	}
}
