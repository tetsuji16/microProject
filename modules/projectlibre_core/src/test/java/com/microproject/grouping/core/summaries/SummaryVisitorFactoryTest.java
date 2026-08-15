package com.microproject.grouping.core.summaries;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

import org.junit.jupiter.api.Test;

import com.microproject.document.Document;
import com.microproject.field.Field;
import com.microproject.field.FieldContext;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.NodeException;
import com.microproject.grouping.core.NodeVisitor;
import com.microproject.grouping.core.model.WalkersNodeModel;

class SummaryVisitorFactoryTest {
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

		public Class getType() throws NodeException {
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
