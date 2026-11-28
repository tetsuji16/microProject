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
package com.microproject.node.summaries;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

import org.apache.commons.collections4.BidiMap;
import org.junit.jupiter.api.Test;

import com.microproject.document.Document;
import com.microproject.field.Field;
import com.microproject.field.FieldContext;
import com.microproject.node.Node;
import com.microproject.node.NodeException;
import com.microproject.node.NodeVisitor;
import com.microproject.node.model.WalkersNodeModel;

class SummaryVisitorFactoryTest {
	@Test
	void numericMinimumIgnoresNonNumbersAndKeepsTheLowestNumber() {
		NumericMinimum minimum = new NumericMinimum();

		minimum.addToSummary("ignored");
		minimum.addToSummary(Double.valueOf(8.5D));
		minimum.addToSummary(Integer.valueOf(3));

		assertEquals(Double.valueOf(3.0D), minimum.getSummary());
	}

	@Test
	void summaryMapsExposeTypedImmutableEntries() {
		BidiMap<String, Integer> flags = SummaryVisitorFactory.getMap(Boolean.class, false);

		assertNotNull(flags.getKey(SummaryNames.OR));
		assertThrows(UnsupportedOperationException.class,
			() -> flags.put("custom", Integer.valueOf(99)));
	}

	@Test
	void summaryNameLookupPreservesKnownAndUnknownValues() {
		assertEquals(SummaryNames.OR, SummaryVisitorFactory.getSummaryId("OR"));
		assertEquals(SummaryNames.NONE, SummaryVisitorFactory.getSummaryId("unknown"));
	}

	@Test
	void thisSummaryUsesCurrentNodeValueEvenForSummaryRows() {
		TestNode parent = new TestNode("parent");
		TestNode child = new TestNode("child");
		parent.addChild(child);

		TestWalkersNodeModel model = new TestWalkersNodeModel();
		model.setChildren(parent, Collections.singletonList(child));
		model.setSummary(parent, true);
		model.setSummary(child, false);

		NodeWalker visitor = assertInstanceOf(
			NodeWalker.class,
			SummaryVisitorFactory.getInstance(SummaryNames.THIS, String.class, false));
		visitor.setField(new MapField(mapOf(parent.getImpl(), "parent-value", child.getImpl(), "child-value")));
		visitor.setContext(new FieldContext());
		visitor.setNodeModel(model);
		visitor.setNode(parent);

		assertEquals("parent-value", visitor.getSummary());
	}

	@Test
	void countNonsummariesCountsOnlyNonSummaryDescendants() {
		TestNode parent = new TestNode("parent");
		TestNode summaryChild = new TestNode("summaryChild");
		TestNode leafOne = new TestNode("leafOne");
		TestNode leafTwo = new TestNode("leafTwo");
		parent.addChild(summaryChild);
		parent.addChild(leafTwo);
		summaryChild.addChild(leafOne);

		TestWalkersNodeModel model = new TestWalkersNodeModel();
		model.setChildren(parent, listOf(summaryChild, leafTwo));
		model.setChildren(summaryChild, Collections.singletonList(leafOne));
		model.setChildren(leafOne, Collections.emptyList());
		model.setChildren(leafTwo, Collections.emptyList());
		model.setSummary(parent, true);
		model.setSummary(summaryChild, true);
		model.setSummary(leafOne, false);
		model.setSummary(leafTwo, false);

		NodeWalker visitor = assertInstanceOf(
			NodeWalker.class,
			SummaryVisitorFactory.getInstance(SummaryNames.COUNT_NONSUMMARIES, Double.class, false));
		visitor.setField(new MapField(mapOf(leafOne.getImpl(), 1, leafTwo.getImpl(), 2)));
		visitor.setContext(new FieldContext());
		visitor.setNodeModel(model);
		visitor.setNode(parent);

		assertEquals(Integer.valueOf(2), visitor.getSummary());
	}

	@Test
	void maximumKeepsTheGreatestValueAndIgnoresNulls() {
		Maximum maximum = new Maximum();
		assertEquals(null, maximum.getSummary());

		maximum.addToSummary(null);
		maximum.addToSummary("b");
		maximum.addToSummary("a");
		maximum.addToSummary("c");
		maximum.addToSummary("c");

		assertEquals("c", maximum.getSummary());
	}

	@Test
	void minimumKeepsTheSmallestValueAndIgnoresNulls() {
		Minimum minimum = new Minimum();
		minimum.addToSummary(null);
		minimum.addToSummary(Integer.valueOf(5));
		minimum.addToSummary(Integer.valueOf(2));
		minimum.addToSummary(Integer.valueOf(9));

		assertEquals(Integer.valueOf(2), minimum.getSummary());
	}

	@Test
	void boundaryVisitorsCompareDatesChronologically() {
		Date early = new Date(1_000_000L);
		Date late = new Date(2_000_000L);

		Maximum maximum = new Maximum();
		maximum.addToSummary(early);
		maximum.addToSummary(late);
		assertSame(late, maximum.getSummary());

		Minimum minimum = new Minimum();
		minimum.addToSummary(late);
		minimum.addToSummary(early);
		assertSame(early, minimum.getSummary());
	}

	@Test
	void boundaryVisitorsResetToNoSummary() {
		Maximum maximum = new Maximum();
		maximum.addToSummary("b");
		maximum.reset();

		assertEquals(null, maximum.getSummary());
	}

	@Test
	void sameSummaryKeepsEqualValuesAndClearsOnMismatch() {
		Same same = new Same();
		assertEquals(null, same.getSummary());

		same.addToSummary("a");
		same.addToSummary("a");
		assertEquals("a", same.getSummary());

		same.addToSummary("b");
		assertEquals(null, same.getSummary());
		same.addToSummary("a");
		assertEquals(null, same.getSummary(), "a cleared summary stays cleared until reset");

		same.reset();
		same.addToSummary("b");
		assertEquals("b", same.getSummary());
	}

	@Test
	void sameSummaryStaysNullWhenTheFirstValueIsNull() {
		Same same = new Same();
		same.addToSummary(null);
		same.addToSummary(null);
		same.addToSummary("a");

		assertEquals(null, same.getSummary());
	}

	@Test
	void concatTextSummaryJoinsValuesWithSemicolons() {
		ConcatTextSummaryVisitor concat = new ConcatTextSummaryVisitor();
		concat.reset();
		assertEquals("", concat.getSummary());

		concat.addToSummary("a");
		concat.addToSummary("b");

		assertEquals("a;b;", concat.getSummary());
	}

	@Test
	void sumAddsNumbersAsDoubles() {
		Sum sum = new Sum();
		sum.reset();
		sum.addToSummary(Integer.valueOf(1));
		sum.addToSummary(Double.valueOf(2.5D));

		assertEquals(Double.valueOf(3.5D), sum.getSummary());
	}

	@Test
	void sumRejectsNullBecauseCallersFilterNullsBeforeSummarizing() {
		Sum sum = new Sum();
		sum.reset();

		assertThrows(NullPointerException.class, () -> sum.addToSummary(null));
	}

	@Test
	void summaryIdsKeepTheirStoredIntegers() {
		assertEquals(0, SummaryNames.MAXIMUM);
		assertEquals(0, SummaryNames.OR);
		assertEquals(1, SummaryNames.MINIMUM);
		assertEquals(1, SummaryNames.AND);
		assertEquals(2, SummaryNames.COUNT_ALL);
		assertEquals(3, SummaryNames.SUM);
		assertEquals(4, SummaryNames.AVERAGE);
		assertEquals(5, SummaryNames.AVERAGE_FIRST_SUBLEVEL);
		assertEquals(6, SummaryNames.COUNT_FIRST_SUBLEVEL);
		assertEquals(7, SummaryNames.COUNT_NONSUMMARIES);
		assertEquals(8, SummaryNames.SAME);
		assertEquals(-1, SummaryNames.NONE);
		assertEquals(-2, SummaryNames.LIST);
		assertEquals(-3, SummaryNames.CUSTOM);
		assertEquals(-4, SummaryNames.THIS);
	}

	@Test
	void configuredSummaryNamesResolveToTheirIntegers() {
		assertEquals(SummaryNames.MAXIMUM, SummaryVisitorFactory.getSummaryId("Maximum"));
		assertEquals(SummaryNames.MINIMUM, SummaryVisitorFactory.getSummaryId("Minimum"));
		assertEquals(SummaryNames.SAME, SummaryVisitorFactory.getSummaryId("Same"));
		assertEquals(SummaryNames.SUM, SummaryVisitorFactory.getSummaryId("Sum"));
		assertEquals(SummaryNames.AVERAGE, SummaryVisitorFactory.getSummaryId("Average"));
		assertEquals(SummaryNames.AVERAGE_FIRST_SUBLEVEL, SummaryVisitorFactory.getSummaryId("AverageFirstSublevel"));
		assertEquals(SummaryNames.COUNT_ALL, SummaryVisitorFactory.getSummaryId("CountAll"));
		assertEquals(SummaryNames.COUNT_FIRST_SUBLEVEL, SummaryVisitorFactory.getSummaryId("CountFirstSublevel"));
		assertEquals(SummaryNames.COUNT_NONSUMMARIES, SummaryVisitorFactory.getSummaryId("CountNonsummaries"));
		assertEquals(SummaryNames.LIST, SummaryVisitorFactory.getSummaryId("List"));
		assertEquals(SummaryNames.NONE, SummaryVisitorFactory.getSummaryId("None"));
		assertEquals(SummaryNames.THIS, SummaryVisitorFactory.getSummaryId("This"));
		assertEquals(SummaryNames.OR, SummaryVisitorFactory.getSummaryId("OR"));
		assertEquals(SummaryNames.AND, SummaryVisitorFactory.getSummaryId("AND"));
	}

	@Test
	void factorySelectsAVisitorForEveryConfiguredSummaryId() {
		assertInstanceOf(LeafWalker.class, SummaryVisitorFactory.getInstance(SummaryNames.MAXIMUM, Double.class, false));
		assertInstanceOf(LeafWalker.class, SummaryVisitorFactory.getInstance(SummaryNames.MINIMUM, Date.class, false));
		assertInstanceOf(LeafWalker.class, SummaryVisitorFactory.getInstance(SummaryNames.SUM, Double.class, false));
		assertInstanceOf(LeafWalker.class, SummaryVisitorFactory.getInstance(SummaryNames.AVERAGE, Double.class, false));
		assertInstanceOf(LeafWalker.class, SummaryVisitorFactory.getInstance(SummaryNames.LIST, String.class, false));
		assertInstanceOf(DeepChildWalker.class,
			SummaryVisitorFactory.getInstance(SummaryNames.COUNT_ALL, Double.class, false));
		assertInstanceOf(ShallowChildWalker.class,
			SummaryVisitorFactory.getInstance(SummaryNames.COUNT_FIRST_SUBLEVEL, Double.class, false));
		assertInstanceOf(ShallowChildWalker.class, SummaryVisitorFactory.getInstance(SummaryNames.SAME, Double.class, false));
		assertInstanceOf(LeafWalker.class, SummaryVisitorFactory.getInstance(SummaryNames.OR, Boolean.class, false));
		assertInstanceOf(LeafWalker.class, SummaryVisitorFactory.getInstance(SummaryNames.AND, Boolean.class, false));
		assertEquals(null, SummaryVisitorFactory.getInstance(SummaryNames.NONE, Double.class, false).getSummary());
	}

	private static Map<Object, Object> mapOf(Object key1, Object value1, Object key2, Object value2) {
		Map<Object, Object> values = new HashMap<Object, Object>();
		values.put(key1, value1);
		values.put(key2, value2);
		return values;
	}

	private static Map<Object, Object> mapOf(Object key1, Object value1, Object key2, Object value2, Object key3, Object value3) {
		Map<Object, Object> values = mapOf(key1, value1, key2, value2);
		values.put(key3, value3);
		return values;
	}

	private static List<Node> listOf(Node first, Node second) {
		List<Node> result = new ArrayList<Node>(2);
		result.add(first);
		result.add(second);
		return result;
	}

	private static final class MapField extends Field {
		private final Map<Object, Object> values;

		private MapField(Map<Object, Object> values) {
			this.values = values;
		}

		public Object getValue(Object object, FieldContext context) {
			return values.get(object);
		}
	}

	private static final class TestWalkersNodeModel implements WalkersNodeModel {
		private final Map<Node, List<Node>> childrenByNode = new HashMap<Node, List<Node>>();
		private final Map<Node, Boolean> summaryByNode = new HashMap<Node, Boolean>();

		void setChildren(Node parent, List<Node> children) {
			childrenByNode.put(parent, children);
		}

		void setSummary(Node node, boolean summary) {
			summaryByNode.put(node, Boolean.valueOf(summary));
		}

		public List getChildren(Node parent) {
			List<Node> children = childrenByNode.get(parent);
			return children == null ? Collections.emptyList() : children;
		}

		public Node getParent(Node child) {
			return null;
		}

		public Node search(Object key) {
			return null;
		}

		public boolean isSummary(Node node) {
			Boolean summary = summaryByNode.get(node);
			return summary != null && summary.booleanValue();
		}

		public Document getDocument() {
			return null;
		}
	}

	private static final class TestNode implements Node {
		private final Object impl;
		private final List<MutableTreeNode> children = new ArrayList<MutableTreeNode>();
		private MutableTreeNode parent;
		private boolean virtual;
		private boolean voidNode;
		private boolean root;
		private boolean dirty;
		private int subprojectLevel;

		private TestNode(Object impl) {
			this.impl = impl;
		}

		void addChild(TestNode child) {
			children.add(child);
			child.parent = this;
		}

		public void accept(NodeVisitor visitor) {
			visitor.accept(this);
		}

		public Class<?> getType() throws NodeException {
			return impl == null ? Object.class : impl.getClass();
		}

		public boolean isVirtual() {
			return virtual;
		}

		public void setVirtual(boolean virtual) {
			this.virtual = virtual;
		}

		public boolean isVoid() {
			return voidNode;
		}

		public void setVoid(boolean voidNode) {
			this.voidNode = voidNode;
		}

		public boolean isRoot() {
			return root;
		}

		public void setRoot(boolean root) {
			this.root = root;
		}

		public boolean hasNumber() {
			return false;
		}

		public Object getImpl() {
			return impl;
		}

		public void setImpl(Object imp) {
			throw new UnsupportedOperationException();
		}

		public ListIterator childrenIterator() {
			return children.listIterator();
		}

		public ListIterator childrenIterator(int i) {
			return children.listIterator(i);
		}

		public List getChildren() {
			return children;
		}

		public void add(MutableTreeNode node) {
			children.add(node);
		}

		public DefaultMutableTreeNode getPreviousSibling() {
			return null;
		}

		public DefaultMutableTreeNode getNextSibling() {
			return null;
		}

		public boolean isIndentable(int value) {
			return false;
		}

		public boolean isLazyParent() {
			return false;
		}

		public boolean canBeChildOf(Node parent) {
			return true;
		}

		public boolean isDirty() {
			return dirty;
		}

		public void setDirty(boolean dirty) {
			this.dirty = dirty;
		}

		public int getSubprojectLevel() {
			return subprojectLevel;
		}

		public void setSubprojectLevel(int subprojectLevel) {
			this.subprojectLevel = subprojectLevel;
		}

		public boolean isInSubproject() {
			return subprojectLevel > 0;
		}

		public TreeNode getChildAt(int childIndex) {
			return (TreeNode)children.get(childIndex);
		}

		public int getChildCount() {
			return children.size();
		}

		public TreeNode getParent() {
			return (TreeNode)parent;
		}

		public int getIndex(TreeNode node) {
			return children.indexOf(node);
		}

		public boolean getAllowsChildren() {
			return true;
		}

		public boolean isLeaf() {
			return children.isEmpty();
		}

		public Enumeration children() {
			return Collections.enumeration(children);
		}

		public void insert(MutableTreeNode child, int index) {
			children.add(index, child);
		}

		public void remove(int index) {
			children.remove(index);
		}

		public void remove(MutableTreeNode node) {
			children.remove(node);
		}

		public void setUserObject(Object object) {
		}

		public void removeFromParent() {
			if (parent != null) {
				parent.remove(this);
			}
		}

		public void setParent(MutableTreeNode newParent) {
			this.parent = newParent;
		}
	}
}
