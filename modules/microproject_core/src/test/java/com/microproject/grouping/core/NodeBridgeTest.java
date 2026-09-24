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
package com.microproject.grouping.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ListIterator;
import java.util.NoSuchElementException;

import javax.swing.tree.TreeNode;

import org.junit.jupiter.api.Test;

class NodeBridgeTest {
	@Test
	void emptyChildrenIteratorFollowsListIteratorContract() {
		Node node = NodeFactory.getInstance().createNode(new Object());
		ListIterator<TreeNode> iterator = node.childrenIterator();
		TreeNode child = NodeFactory.getInstance().createNode(new Object());

		assertFalse(iterator.hasNext());
		assertFalse(iterator.hasPrevious());
		assertEquals(0, iterator.nextIndex());
		assertEquals(-1, iterator.previousIndex());
		assertThrows(NoSuchElementException.class, iterator::next);
		assertThrows(NoSuchElementException.class, iterator::previous);
		assertThrows(IllegalStateException.class, iterator::remove);
		assertThrows(UnsupportedOperationException.class, () -> iterator.add(child));
		assertThrows(IllegalStateException.class, () -> iterator.set(child));
		assertThrows(IndexOutOfBoundsException.class, () -> node.childrenIterator(1));
	}
}
