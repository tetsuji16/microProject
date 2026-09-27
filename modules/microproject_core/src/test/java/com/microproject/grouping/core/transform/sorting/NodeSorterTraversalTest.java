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
package com.microproject.grouping.core.transform.sorting;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.grouping.core.transform.HierarchicObject;

class NodeSorterTraversalTest {
	@Test
	void exposesTypedComparatorForNodeValues() {
		Comparator<Object> comparator = new NodeSorter();

		assertEquals(0, comparator.compare(new Object(), new Object()));
	}

	@Test
	void sortsEachHierarchyLevelInEncounterOrder() {
		TestNode group = new TestNode("group", new ArrayList<>(List.of(new TestNode("c"), new TestNode("b"))));
		List<TestNode> roots = new ArrayList<>(List.of(new TestNode("z"), group, new TestNode("a")));
		Comparator<TestNode> byName = Comparator.comparing(TestNode::name);

		new NodeSorter().sortList(roots, byName, true);

		assertEquals(List.of("a", "group", "z"), roots.stream().map(TestNode::name).toList());
		assertEquals(List.of("b", "c"), group.getChildren().stream().map(TestNode::name).toList());
	}

	private record TestNode(String name, List<TestNode> children) implements HierarchicObject {
		private TestNode(String name) {
			this(name, new ArrayList<>());
		}

		@Override
		public List<TestNode> getChildren() {
			return children;
		}
	}
}
