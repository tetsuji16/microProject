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
package com.microproject.grouping.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

import com.microproject.field.Field;
import com.microproject.field.FieldContext;
import com.microproject.field.FieldParseException;
import com.microproject.configuration.Configuration;
import com.microproject.datatype.Rate;
import com.microproject.document.ObjectEvent;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.NodeFactory;
import com.microproject.grouping.core.transform.sorting.NodeSorter;
import com.microproject.grouping.core.hierarchy.AbstractMutableNodeHierarchy;
import com.microproject.grouping.core.event.HierarchyEvent;
import com.microproject.grouping.core.event.HierarchyListener;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.AssignmentService;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.undo.UndoController;

class DefaultNodeModelTest {
	@Test
	void implementationComparatorUsesIdentityForSearchKeys() {
		String implementation = new String("implementation");
		Node node = NodeFactory.getInstance().createNode(implementation);
		java.util.Comparator<Object> comparator = new DefaultNodeModel.ImplComparator();

		assertEquals(0, comparator.compare(node, implementation));
		assertEquals(1, comparator.compare(node, new String("implementation")));

		DefaultNodeModel model = new DefaultNodeModel(new StubDataFactory());
		model.add((Node) model.getHierarchy().getRoot(), node, NodeModel.SILENT);
		assertSame(node, model.search(implementation, comparator));
	}

	@Test
	void replaceImplFiresChangeEventWithoutInsertOrRemove() {
		DefaultNodeModel model = new DefaultNodeModel(new StubDataFactory());
		model.getHierarchy().setNbEndVoidNodes(0);
		CapturingHierarchyListener listener = new CapturingHierarchyListener();
		model.getHierarchy().addHierarchyListener(listener);

		Node node = NodeFactory.getInstance().createNode(new Object());
		model.add((Node) model.getHierarchy().getRoot(), node, NodeModel.SILENT);
		listener.reset();

		model.replaceImpl(node, new Object(), this, NodeModel.NORMAL);

		assertEquals(1, listener.changedCount);
		assertEquals(0, listener.insertedCount);
		assertEquals(0, listener.removedCount);
	}

	@Test
	void removeFiresRemoveEventWithoutInsertOrChange() {
		DefaultNodeModel model = new DefaultNodeModel(new StubDataFactory());
		model.getHierarchy().setNbEndVoidNodes(0);
		CapturingHierarchyListener listener = new CapturingHierarchyListener();
		model.getHierarchy().addHierarchyListener(listener);

		Node node = NodeFactory.getInstance().createNode(new Object());
		model.add((Node) model.getHierarchy().getRoot(), node, NodeModel.SILENT);
		listener.reset();

		model.remove(node, NodeModel.NORMAL);

		assertEquals(0, listener.changedCount);
		assertEquals(0, listener.insertedCount);
		assertEquals(1, listener.removedCount);
	}

	@Test
	void hierarchyListenersRetainRegistrationOrderAndLastRegistrationRemoval() {
		DefaultNodeModel model = new DefaultNodeModel(new StubDataFactory());
		HierarchyListener first = new CapturingHierarchyListener();
		HierarchyListener second = new CapturingHierarchyListener();
		model.getHierarchy().addHierarchyListener(first);
		model.getHierarchy().addHierarchyListener(second);

		assertEquals(List.of(first, second), List.of(model.getHierarchy().getHierarchyListeners()));

		model.getHierarchy().removeHierarchyListener(first);

		assertEquals(List.of(second), List.of(model.getHierarchy().getHierarchyListeners()));
	}

	@Test
	void searchTracksAddedReplacedAndRemovedNodes() {
		DefaultNodeModel model = new DefaultNodeModel(new StubDataFactory());
		model.getHierarchy().setNbEndVoidNodes(0);

		Object initialImpl = new Object();
		Node node = NodeFactory.getInstance().createNode(initialImpl);
		model.add((Node) model.getHierarchy().getRoot(), node, NodeModel.SILENT);

		assertSame(node, model.search(initialImpl));

		Object replacementImpl = new Object();
		model.replaceImpl(node, replacementImpl, this, NodeModel.NORMAL);

		assertNull(model.search(initialImpl));
		assertSame(node, model.search(replacementImpl));

		model.remove(node, NodeModel.NORMAL);

		assertNull(model.search(replacementImpl));
	}

	@Test
	void hierarchyIndexUsesDepthFirstChildOrder() {
		DefaultNodeModel model = new DefaultNodeModel(new StubDataFactory());
		model.getHierarchy().setNbEndVoidNodes(0);
		Node root = (Node) model.getHierarchy().getRoot();
		Node parent = NodeFactory.getInstance().createNode(new Object());
		Node child = NodeFactory.getInstance().createNode(new Object());
		Node sibling = NodeFactory.getInstance().createNode(new Object());
		model.add(root, parent, NodeModel.SILENT);
		model.add(parent, child, NodeModel.SILENT);
		model.add(root, sibling, NodeModel.SILENT);

		assertSame(parent, child.getParent());
		assertSame(child, parent.getChildAt(0));
		assertEquals(0, model.getHierarchy().getIndexOfNode(root, false));
		assertEquals(1, model.getHierarchy().getIndexOfNode(parent, false));
		assertEquals(2, model.getHierarchy().getIndexOfNode(child, false));
		assertEquals(3, model.getHierarchy().getIndexOfNode(sibling, false));
	}

	@Test
	void extractNodeListReturnsDepthFirstTypedNodes() {
		DefaultNodeModel model = new DefaultNodeModel(new StubDataFactory());
		model.getHierarchy().setNbEndVoidNodes(0);
		Node root = (Node) model.getHierarchy().getRoot();
		Node parent = NodeFactory.getInstance().createNode(new Object());
		Node child = NodeFactory.getInstance().createNode(new Object());
		Node sibling = NodeFactory.getInstance().createNode(new Object());
		model.add(root, parent, NodeModel.SILENT);
		model.add(parent, child, NodeModel.SILENT);
		model.add(root, sibling, NodeModel.SILENT);

		List<Node> nodes = NodeModelUtil.extractNodeList(model, root);

		assertEquals(List.of(root, parent, child, sibling), nodes);
	}

	@Test
	void hierarchyToListKeepsNodeAndImplementationModesDistinct() {
		DefaultNodeModel model = new DefaultNodeModel(new StubDataFactory());
		model.getHierarchy().setNbEndVoidNodes(0);
		Node root = (Node) model.getHierarchy().getRoot();
		Object implementation = new Object();
		Node node = NodeFactory.getInstance().createNode(implementation);
		model.add(root, node, NodeModel.SILENT);

		List<?> nodes = model.getHierarchy().toList(true, null);
		List<?> implementations = model.getHierarchy().toList(false, null);

		assertTrue(nodes.contains(node));
		assertTrue(implementations.contains(implementation));
	}

	@Test
	void hierarchyTraversalsPreservePreorderAndLevelOrder() {
		DefaultNodeModel model = new DefaultNodeModel(new StubDataFactory());
		model.getHierarchy().setNbEndVoidNodes(0);
		Node root = (Node) model.getHierarchy().getRoot();
		Node first = NodeFactory.getInstance().createNode(new Object());
		Node second = NodeFactory.getInstance().createNode(new Object());
		Node firstChild = NodeFactory.getInstance().createNode(new Object());
		Node secondChild = NodeFactory.getInstance().createNode(new Object());
		model.add(root, first, NodeModel.SILENT);
		model.add(root, second, NodeModel.SILENT);
		model.add(first, firstChild, NodeModel.SILENT);
		model.add(second, secondChild, NodeModel.SILENT);
		List<Object> preorder = new ArrayList<>();
		List<Object> levelOrder = new ArrayList<>();
		List<Node> iteratorOrder = new ArrayList<>();

		model.getHierarchy().visitAll(root, true, preorder::add);
		model.getHierarchy().visitAllLevelOrder(root, false, levelOrder::add);
		Iterator<Node> nodeIterator = model.iterator();
		nodeIterator.forEachRemaining(iteratorOrder::add);

		assertNodeOrder(preorder, first, firstChild, second, secondChild);
		assertNodeOrder(levelOrder, first, second, firstChild, secondChild);
		assertEquals(List.of(root, first, firstChild, second, secondChild), iteratorOrder);
		assertThrows(UnsupportedOperationException.class, () -> model.iterator().remove());
	}

	@Test
	void hierarchyNavigationMovesInPreorderAndSkipsVirtualRoot() {
		DefaultNodeModel model = new DefaultNodeModel(new StubDataFactory());
		model.getHierarchy().setNbEndVoidNodes(0);
		Node root = (Node) model.getHierarchy().getRoot();
		Node firstParent = NodeFactory.getInstance().createNode(new Object());
		Node firstChild = NodeFactory.getInstance().createNode(new Object());
		Node lastChild = NodeFactory.getInstance().createNode(new Object());
		Node nextParent = NodeFactory.getInstance().createNode(new Object());
		model.add(root, firstParent, NodeModel.SILENT);
		model.add(firstParent, firstChild, NodeModel.SILENT);
		model.add(firstParent, lastChild, NodeModel.SILENT);
		model.add(root, nextParent, NodeModel.SILENT);

		assertSame(firstChild, model.getHierarchy().getNext(firstParent));
		assertSame(nextParent, model.getHierarchy().getNext(lastChild));
		assertSame(lastChild, model.getHierarchy().getPrevious(nextParent));
		assertSame(firstParent, model.getHierarchy().getPrevious(firstChild));
		assertNull(model.getHierarchy().getPrevious(firstParent));
		assertNull(model.getHierarchy().getNext(nextParent));
	}

	@Test
	void shallowHierarchyIteratorHonorsDepthAndRootOptions() {
		DefaultNodeModel model = new DefaultNodeModel(new StubDataFactory());
		model.getHierarchy().setNbEndVoidNodes(0);
		Node root = (Node) model.getHierarchy().getRoot();
		Node first = NodeFactory.getInstance().createNode(new Object());
		Node second = NodeFactory.getInstance().createNode(new Object());
		Node grandchild = NodeFactory.getInstance().createNode(new Object());
		model.add(root, first, NodeModel.SILENT);
		model.add(root, second, NodeModel.SILENT);
		model.add(first, grandchild, NodeModel.SILENT);

		Iterator<Node> includingRoot = model.shallowIterator(1, true);
		assertSame(root, includingRoot.next());
		assertSame(first, includingRoot.next());
		assertSame(second, includingRoot.next());
		assertFalse(includingRoot.hasNext());
		assertThrows(NoSuchElementException.class, includingRoot::next);

		Iterator<Node> excludingRoot = model.shallowIterator(2, false);
		assertSame(first, excludingRoot.next());
		assertSame(grandchild, excludingRoot.next());
		assertSame(second, excludingRoot.next());
		assertFalse(excludingRoot.hasNext());
		assertThrows(NoSuchElementException.class, excludingRoot::next);

		Iterator<Node> excludingOnlyRoot = model.shallowIterator(0, false);
		assertFalse(excludingOnlyRoot.hasNext());
		assertThrows(NoSuchElementException.class, excludingOnlyRoot::next);
	}

	@Test
	void hierarchyDumpPreservesDepthFirstIndentationAndNewlines() {
		DefaultNodeModel model = new DefaultNodeModel(new StubDataFactory());
		model.getHierarchy().setNbEndVoidNodes(0);
		Node root = (Node) model.getHierarchy().getRoot();
		Node first = NodeFactory.getInstance().createNode("first");
		Node child = NodeFactory.getInstance().createNode("child");
		Node second = NodeFactory.getInstance().createNode("second");
		model.add(root, first, NodeModel.SILENT);
		model.add(first, child, NodeModel.SILENT);
		model.add(root, second, NodeModel.SILENT);
		StringBuffer buffer = new StringBuffer();

		((AbstractMutableNodeHierarchy) model.getHierarchy()).dump(buffer);

		assertEquals("-->first\n---->child\n-->second\n", buffer.toString());
	}

	private static void assertNodeOrder(List<Object> actual, Node... expected) {
		assertEquals(expected.length, actual.size());
		for (int i = 0; i < expected.length; i++)
			assertSame(expected[i], actual.get(i));
	}

	@Test
	void deletionUndoRedoRestoresASelectedParentAndItsChildOnce() {
		UndoController undoController = new UndoController();
		DefaultNodeModel model = new DefaultNodeModel(new StubDataFactory());
		model.setUndoController(undoController);
		model.getHierarchy().setNbEndVoidNodes(0);

		Node root = (Node) model.getHierarchy().getRoot();
		Node parent = NodeFactory.getInstance().createVoidNode();
		Node child = NodeFactory.getInstance().createVoidNode();
		model.add(root, parent, NodeModel.SILENT);
		model.add(parent, child, NodeModel.SILENT);
		undoController.clear();

		model.remove(Arrays.asList(parent, child), NodeModel.NORMAL);

		assertTrue(undoController.canUndo());
		assertEquals(0, root.getChildCount());
		undoController.undo();
		assertSame(parent, root.getChildAt(0));
		assertSame(child, parent.getChildAt(0));
		assertTrue(undoController.canRedo());
		undoController.redo();
		assertEquals(0, root.getChildCount());
	}

	@Test
	void deletionOfASubprojectTaskKeepsTheUndoHistory() {
		Project project = createProject();
		DataFactoryUndoController undoController = project.getUndoController();
		DefaultNodeModel model = (DefaultNodeModel) project.getTaskModel();
		model.getHierarchy().setNbEndVoidNodes(0);
		TestSubProj subprojectTask = new TestSubProj();
		Node root = (Node) model.getHierarchy().getRoot();
		Node node = NodeFactory.getInstance().createNode(subprojectTask);
		model.add(root, node, NodeModel.SILENT);
		undoController.clear();

		model.remove(node, NodeModel.NORMAL);

		assertTrue(undoController.canUndo());
		undoController.undo();
		assertSame(node, root.getChildAt(0));
		undoController.redo();
		assertFalse(undoController.canRedo());
		assertEquals(0, root.getChildCount());
	}

	@Test
	void redoCancelsASubprojectRestoreWaitingForCloseCompletion() {
		Project parentProject = createProject();
		Project subproject = createProject();
		subproject.setUniqueId(987654321L);
		DefaultNodeModel model = new DefaultNodeModel(new StubDataFactory());
		model.getHierarchy().setNbEndVoidNodes(0);
		Node root = (Node) model.getHierarchy().getRoot();
		Node node = NodeFactory.getInstance().createNode(new TestSubProj(subproject));
		model.add(root, node, NodeModel.SILENT);
		DefaultNodeModel.RemovalSnapshot snapshot = DefaultNodeModel.RemovalSnapshot.capture(Arrays.asList(node));
		DefaultNodeModel.RemovalSnapshot.SubprojectState state = snapshot.getSubprojects().get(0);
		com.microproject.pm.task.ProjectFactory factory = com.microproject.pm.task.ProjectFactory.getInstance();

		factory.addClosingProject(subproject.getUniqueId());
		state.restoreAfterClose(parentProject);
		state.cancelRestore();
		factory.removeClosingProject(subproject.getUniqueId());

		assertNull(factory.findFromId(subproject.getUniqueId()));
	}

	@Test
	void replaceImplAndSetFieldValueKeepsReplacementSearchableDuringValidation() throws Exception {
		SearchAwareDataFactory factory = new SearchAwareDataFactory();
		DefaultNodeModel model = new DefaultNodeModel(factory);
		model.getHierarchy().setNbEndVoidNodes(0);

		Node node = NodeFactory.getInstance().createVoidNode();
		model.add((Node) model.getHierarchy().getRoot(), node, NodeModel.SILENT);

		Object replacementImpl = new Object();
		TrackingField field = new TrackingField();

		model.replaceImplAndSetFieldValue(node, null, replacementImpl, field, this, "Task A", null, NodeModel.NORMAL);

		assertSame(node, factory.nodeSeenDuringValidation);
		assertSame(node, model.search(replacementImpl));
		assertSame(replacementImpl, field.oldImplSeen);
		assertEquals("Task A", field.valueSeen);
	}

	@Test
	void replaceImplAndSetFieldValueRestoresOriginalSearchIndexWhenFieldSetFails() {
		DefaultNodeModel model = new DefaultNodeModel(new StubDataFactory());
		model.getHierarchy().setNbEndVoidNodes(0);

		Object originalImpl = new Object();
		Node node = NodeFactory.getInstance().createNode(originalImpl);
		model.add((Node) model.getHierarchy().getRoot(), node, NodeModel.SILENT);

		Object replacementImpl = new Object();
		FailingField field = new FailingField();

		assertThrows(FieldParseException.class,
				() -> model.replaceImplAndSetFieldValue(node, null, replacementImpl, field, this, "Task A", null,
						NodeModel.NORMAL));

		assertSame(node, model.search(originalImpl));
		assertNull(model.search(replacementImpl));
		assertSame(originalImpl, node.getImpl());
	}

	@Test
	void pasteCreatesDistinctVoidNodesWhenPaddingToRequestedPosition() {
		DefaultNodeModel model = new DefaultNodeModel(new StubDataFactory());
		model.getHierarchy().setNbEndVoidNodes(0);

		Node root = (Node) model.getHierarchy().getRoot();
		Node pastedNode = NodeFactory.getInstance().createVoidNode();

		model.paste(root, Arrays.asList(pastedNode), 3, NodeModel.SILENT);

		List<Node> children = model.getHierarchy().getChildren(root);
		assertEquals(4, children.size());
		assertTrue(children.get(0) != children.get(1));
		assertTrue(children.get(1) != children.get(2));
	}

	@Test
	void pasteUndoRedoRestoresThePastedNodeAtItsOriginalPosition() {
		UndoController undoController = new UndoController();
		DefaultNodeModel model = new DefaultNodeModel(new StubDataFactory());
		model.setUndoController(undoController);
		model.getHierarchy().setNbEndVoidNodes(0);
		Node root = (Node) model.getHierarchy().getRoot();
		Node existing = NodeFactory.getInstance().createNode(new Object());
		Node pasted = NodeFactory.getInstance().createVoidNode();
		model.add(root, existing, NodeModel.SILENT);
		undoController.clear();

		model.paste(root, List.of(pasted), 0, NodeModel.NORMAL);

		assertSame(pasted, root.getChildAt(0));
		assertSame(existing, root.getChildAt(1));
		undoController.undo();
		assertEquals(1, root.getChildCount());
		assertSame(existing, root.getChildAt(0));
		undoController.redo();
		assertEquals(2, root.getChildCount());
		assertSame(pasted, root.getChildAt(0));
		assertSame(existing, root.getChildAt(1));
	}

	@Test
	void addBeforeLinkedListHonorsUndoActionType() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("test", undoController), undoController);
		project.initialize(false, false);
		DefaultNodeModel model = (DefaultNodeModel) project.getTaskModel();
		model.getHierarchy().setNbEndVoidNodes(0);
		undoController.clear();

		Node root = (Node) model.getHierarchy().getRoot();
		Node first = NodeFactory.getInstance().createNode(new Object());
		Node second = NodeFactory.getInstance().createNode(new Object());
		model.add(root, first, NodeModel.SILENT);
		model.add(root, second, NodeModel.SILENT);
		undoController.clear();

		LinkedList siblings = new LinkedList();
		siblings.add(first);
		siblings.add(second);

		model.addBefore(siblings, NodeFactory.getInstance().createNode(new Object()), NodeModel.NORMAL);

		assertTrue(undoController.canUndo());
		undoController.undo();
		assertEquals(2, model.getHierarchy().getChildren(root).size());
	}

	@Test
	void nodeCreationUndoSnapshotsCallerList() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("test", undoController), undoController);
		project.initialize(false, false);
		DefaultNodeModel model = (DefaultNodeModel) project.getTaskModel();
		model.getHierarchy().setNbEndVoidNodes(0);
		undoController.clear();

		Node root = (Node) model.getHierarchy().getRoot();
		List<Node> children = new ArrayList<>();
		children.add(NodeFactory.getInstance().createNode(new Object()));
		children.add(NodeFactory.getInstance().createNode(new Object()));

		model.add(root, children, NodeModel.NORMAL);
		children.clear();

		assertTrue(undoController.canUndo());
		undoController.undo();
		assertEquals(0, model.getHierarchy().getChildren(root).size());
	}

	@Test
	void outdentPreservesVoidNodeOrder() {
		DefaultNodeModel model = new DefaultNodeModel(new StubDataFactory());
		model.getHierarchy().setNbEndVoidNodes(0);

		Node root = (Node) model.getHierarchy().getRoot();
		Node parent = NodeFactory.getInstance().createNode(new Object());
		Node firstVoid = NodeFactory.getInstance().createVoidNode();
		Node secondVoid = NodeFactory.getInstance().createVoidNode();
		Node child = NodeFactory.getInstance().createNode(new Object());

		model.add(root, parent, NodeModel.SILENT);
		model.add(parent, firstVoid, NodeModel.SILENT);
		model.add(parent, secondVoid, NodeModel.SILENT);
		model.add(parent, child, NodeModel.SILENT);

		model.getHierarchy().indent(Arrays.asList(child), -1, model, NodeModel.SILENT);

		List<Node> children = model.getHierarchy().getChildren(root);
		assertSame(parent, children.get(0));
		assertSame(firstVoid, children.get(1));
		assertSame(secondVoid, children.get(2));
		assertSame(child, children.get(3));
	}

	@Test
	void indentPreservesVoidNodeOrder() {
		DefaultNodeModel model = new DefaultNodeModel(new StubDataFactory());
		model.getHierarchy().setNbEndVoidNodes(0);

		Node root = (Node) model.getHierarchy().getRoot();
		Node previous = NodeFactory.getInstance().createNode(new Object());
		Node firstVoid = NodeFactory.getInstance().createVoidNode();
		Node secondVoid = NodeFactory.getInstance().createVoidNode();
		Node child = NodeFactory.getInstance().createNode(new Object());

		model.add(root, previous, NodeModel.SILENT);
		model.add(root, firstVoid, NodeModel.SILENT);
		model.add(root, secondVoid, NodeModel.SILENT);
		model.add(root, child, NodeModel.SILENT);

		model.getHierarchy().indent(Arrays.asList(child), 1, model, NodeModel.SILENT);

		List<Node> previousChildren = model.getHierarchy().getChildren(previous);
		assertSame(firstVoid, previousChildren.get(0));
		assertSame(secondVoid, previousChildren.get(1));
		assertSame(child, previousChildren.get(2));
	}

	@Test
	void newNodeHonorsSilentActionType() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("test", undoController), undoController);
		project.initialize(false, false);
		DefaultNodeModel model = (DefaultNodeModel) project.getTaskModel();
		model.getHierarchy().setNbEndVoidNodes(0);
		undoController.clear();

		Node root = (Node) model.getHierarchy().getRoot();

		model.newNode(root, 0, NodeModel.SILENT);

		assertTrue(model.getHierarchy().getChildren(root).size() > 0);
		assertTrue(!undoController.canUndo());
	}

	@Test
	void newNodeHonorsSilentActionTypeWhenInsertingBeforeExistingChild() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("test", undoController), undoController);
		project.initialize(false, false);
		DefaultNodeModel model = (DefaultNodeModel) project.getTaskModel();
		model.getHierarchy().setNbEndVoidNodes(0);
		undoController.clear();

		Node root = (Node) model.getHierarchy().getRoot();
		Node existing = NodeFactory.getInstance().createVoidNode();
		model.add(root, existing, NodeModel.SILENT);
		undoController.clear();

		model.newNode(root, 0, NodeModel.SILENT);

		assertEquals(2, model.getHierarchy().getChildren(root).size());
		assertTrue(!undoController.canUndo());
	}

	@Test
	void batchNewNodesStayTogetherAndUndoAsOneEdit() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("test", undoController), undoController);
		project.initialize(false, false);
		DefaultNodeModel model = (DefaultNodeModel) project.getTaskModel();
		model.getHierarchy().setNbEndVoidNodes(0);
		Node root = (Node) model.getHierarchy().getRoot();
		Node first = NodeFactory.getInstance().createNode(new Object());
		Node selectedLast = NodeFactory.getInstance().createNode(new Object());
		Node following = NodeFactory.getInstance().createNode(new Object());
		model.add(root, List.of(first, selectedLast, following), NodeModel.SILENT);
		undoController.clear();

		List<Node> inserted = model.newNodes(root, 2, 2, NodeModel.NORMAL);
		assertEquals(List.of(first, selectedLast, inserted.get(0), inserted.get(1), following),
			model.getHierarchy().getChildren(root));
		assertTrue(undoController.canUndo());

		undoController.undo();
		assertEquals(List.of(first, selectedLast, following), model.getHierarchy().getChildren(root));
		assertTrue(!undoController.canUndo(), "one undo must remove the whole batch");
		undoController.redo();
		assertEquals(List.of(first, selectedLast, inserted.get(0), inserted.get(1), following),
			model.getHierarchy().getChildren(root));
	}

	@Test
	void copyRebuildsDependencyBetweenCopiedTasks() throws Exception {
		Project project = createProject();
		NormalTask predecessor = createTask(project, "predecessor");
		NormalTask successor = createTask(project, "successor");
		DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.FS, 0L, this);

		DefaultNodeModel model = (DefaultNodeModel) project.getTaskModel();
		List copiedNodes = model.copy(Arrays.asList(model.search(predecessor), model.search(successor)),
				NodeModel.SILENT);

		NormalTask copiedPredecessor = (NormalTask) ((Node) copiedNodes.get(0)).getImpl();
		NormalTask copiedSuccessor = (NormalTask) ((Node) copiedNodes.get(1)).getImpl();
		Dependency copiedDependency = findDependency(copiedPredecessor, copiedSuccessor);

		assertNotNull(copiedDependency);
		assertSame(copiedPredecessor, copiedDependency.getPredecessor());
		assertSame(copiedSuccessor, copiedDependency.getSuccessor());
	}

	@Test
	void ancestorCheckRejectsParentAndChildInOneMoveSelection() {
		DefaultNodeModel model = new DefaultNodeModel(new StubDataFactory());
		model.getHierarchy().setNbEndVoidNodes(0);
		Node root = (Node) model.getHierarchy().getRoot();
		Node parent = NodeFactory.getInstance().createNode(new Object());
		Node child = NodeFactory.getInstance().createNode(new Object());
		Node sibling = NodeFactory.getInstance().createNode(new Object());
		model.add(root, parent, NodeModel.SILENT);
		model.add(parent, child, NodeModel.SILENT);
		model.add(root, sibling, NodeModel.SILENT);

		assertFalse(model.testAncestorOrDescendant(parent, List.of(child)));
		assertTrue(model.testAncestorOrDescendant(parent, List.of(sibling)));
		assertTrue(model.testAncestorOrDescendant(parent, List.of()));
	}

	@Test
	void addImplCollectionCreatesNodesForEveryImplementationInOrder() {
		DefaultNodeModel model = new DefaultNodeModel(new StubDataFactory());
		model.getHierarchy().setNbEndVoidNodes(0);
		Object firstImplementation = new Object();
		Object secondImplementation = new Object();
		Node root = (Node) model.getHierarchy().getRoot();

		model.addImplCollection(root, List.of(firstImplementation, secondImplementation), NodeModel.SILENT);

		assertSame(firstImplementation, ((Node) root.getChildAt(0)).getImpl());
		assertSame(secondImplementation, ((Node) root.getChildAt(1)).getImpl());
	}

	@Test
	void copyRebuildsAssignmentRowsForCopiedTask() {
		Project project = createProject();
		NormalTask task = createTask(project, "assigned task");
		ResourceImpl resource = project.getResourcePool().newResourceInstance();
		Assignment assignment = AssignmentService.getInstance()
				.newAssignment(task, resource, 1.0D, 0L, this);

		DefaultNodeModel model = (DefaultNodeModel) project.getTaskModel();
		List copiedNodes = model.copy(List.of(model.search(task)), NodeModel.SILENT);
		NormalTask copiedTask = (NormalTask) ((Node) copiedNodes.getFirst()).getImpl();

		assertEquals(1, copiedTask.getAssignments().size());
		Assignment copiedAssignment = (Assignment) copiedTask.getAssignments().getFirst();
		assertNotSame(assignment, copiedAssignment);
		assertSame(resource, copiedAssignment.getResource());
	}

	@Test
	void removingAssignedTaskCleansResourceAssignmentAndUndoState() {
		Project project = createProjectWithoutVoidRows();
		NormalTask task = createTask(project, "assigned task");
		ResourceImpl resource = project.getResourcePool().newResourceInstance();
		Assignment assignment = AssignmentService.getInstance().newAssignment(task, resource, 1.0D, 0L, this);
		DefaultNodeModel model = (DefaultNodeModel) project.getTaskModel();
		Node taskNode = model.search(task);
		project.getUndoController().clear();

		model.remove(taskNode, NodeModel.NORMAL);

		assertNull(model.search(task));
		assertFalse(resource.getAssignments().contains(assignment));
		project.getUndoController().undo();
		assertSame(taskNode, model.search(task));
		assertTrue(task.getAssignments().contains(assignment));
		assertTrue(resource.getAssignments().contains(assignment));
		project.getUndoController().redo();
		assertNull(model.search(task));
		assertFalse(resource.getAssignments().contains(assignment));
	}

	@Test
	void removingAssignmentNodeWithDefaultFilteringKeepsItInOutline() {
		Project project = createProjectWithoutVoidRows();
		NormalTask task = createTask(project, "assigned task");
		ResourceImpl resource = project.getResourcePool().newResourceInstance();
		Assignment assignment = AssignmentService.getInstance()
				.newAssignment(task, resource, 1.0D, 0L, this);
		DefaultNodeModel model = (DefaultNodeModel) project.getTaskModel();
		Node assignmentNode = model.search(assignment);

		assertNotNull(assignmentNode);
		model.remove(assignmentNode, NodeModel.NORMAL);

		assertSame(assignmentNode, model.search(assignment));
		assertSame(assignmentNode.getParent(), model.search(task));
	}

	@Test
	void resourceFieldEditRecalculatesAndPublishesAcrossSharedPoolProjectsWithOneUndo() throws Exception {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		DataFactoryUndoController firstProjectUndo = new DataFactoryUndoController();
		DataFactoryUndoController secondProjectUndo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("shared-resource-field-test", undoController);
		undoController.setDataFactory(pool);
		pool.setLocal(true);
		DefaultNodeModel resourceModel = (DefaultNodeModel) pool.getResourceOutline();
		resourceModel.setUndoController(undoController);
		Project firstProject = Project.createProject(pool, firstProjectUndo);
		Project secondProject = Project.createProject(pool, secondProjectUndo);
		firstProject.initialize(false, false);
		secondProject.initialize(false, false);
		pool.addProject(firstProject);
		pool.addProject(secondProject);
		ResourceImpl resource = (ResourceImpl) pool.createScriptedResource();
		Node resourceNode = resourceModel.search(resource);
		Field standardRate = Configuration.getFieldFromId("Field.standardRate");
		Rate originalRate = resource.getStandardRate();
		List<ObjectEvent> observed = new ArrayList<>();
		boolean[] observerSawCoordinator = { false };
		pool.addObjectListener(event -> {
			if (event.getObject() == resource) {
				observed.add(event);
				observerSawCoordinator[0] |= firstProject.getMutationState().isHeldByCurrentThread();
			}
		});
		undoController.clear();
		long poolRevision = pool.getCoordinationRevision();
		long firstRevision = firstProject.getMutationState().revision();
		long secondRevision = secondProject.getMutationState().revision();

		resourceModel.setFieldValue(standardRate, resourceNode, this, new Rate(125.0D), null, NodeModel.NORMAL);

		assertEquals(125.0D, resource.getStandardRate().getValue());
		assertEquals(poolRevision + 1, pool.getCoordinationRevision());
		assertEquals(firstRevision + 1, firstProject.getMutationState().revision());
		assertEquals(secondRevision + 1, secondProject.getMutationState().revision());
		assertEquals(1, observed.size(), "one actual resource fact is published for the field edit");
		assertEquals(pool.getCoordinationRevision(), observed.getFirst().getPublicationRevision());
		assertFalse(observerSawCoordinator[0], "resource observers run after all project scopes unlock");
		assertTrue(undoController.canUndo());
		assertFalse(firstProjectUndo.canUndo(), "shared-resource edit history belongs to the resource model's controller");
		assertFalse(secondProjectUndo.canUndo(), "member project controllers do not receive a duplicate field edit");

		undoController.undo();
		assertSame(originalRate, resource.getStandardRate());
		assertEquals(poolRevision + 2, pool.getCoordinationRevision());
		assertEquals(firstRevision + 2, firstProject.getMutationState().revision());
		assertEquals(secondRevision + 2, secondProject.getMutationState().revision());
		undoController.redo();
		assertEquals(125.0D, resource.getStandardRate().getValue());
		assertEquals(poolRevision + 3, pool.getCoordinationRevision());
		assertEquals(firstRevision + 3, firstProject.getMutationState().revision());
		assertEquals(secondRevision + 3, secondProject.getMutationState().revision());
		assertEquals(3, observed.size(), "Undo and Redo publish the same actual resource fact");
		undoController.clear();
		long noOpPoolRevision = pool.getCoordinationRevision();
		long noOpFirstRevision = firstProject.getMutationState().revision();
		long noOpSecondRevision = secondProject.getMutationState().revision();
		resourceModel.setFieldValue(standardRate, resourceNode, this, resource.getStandardRate(), null, NodeModel.NORMAL);
		assertEquals(noOpPoolRevision, pool.getCoordinationRevision());
		assertEquals(noOpFirstRevision, firstProject.getMutationState().revision());
		assertEquals(noOpSecondRevision, secondProject.getMutationState().revision());
		assertFalse(undoController.canUndo(), "a semantic no-op creates no history");
		long failurePoolRevision = pool.getCoordinationRevision();
		FailingField failingField = new FailingField();
		failingField.setClass(ResourceImpl.class);
		assertThrows(FieldParseException.class, () -> resourceModel.setFieldValue(
			failingField, resourceNode, this, "invalid", null, NodeModel.NORMAL));
		assertEquals(failurePoolRevision, pool.getCoordinationRevision());
		assertFalse(undoController.canUndo(), "a rejected field parse creates no history");
	}

	@Test
	void copyRebuildsWbsChildrenForCopiedSummaryTask() {
		Project project = createProjectWithoutVoidRows();
		NormalTask parent = createTask(project, "parent");
		NormalTask child = createTask(project, "child");
		DefaultNodeModel model = (DefaultNodeModel) project.getTaskModel();
		Node parentNode = model.search(parent);
		Node childNode = model.search(child);

		assertTrue(model.relocate(List.of(childNode), parentNode, 0, NodeModel.SILENT));
		List copiedNodes = model.copy(List.of(parentNode), NodeModel.SILENT);
		Node copiedParentNode = (Node) copiedNodes.getFirst();
		NormalTask copiedParent = (NormalTask) copiedParentNode.getImpl();
		Node copiedChildNode = (Node) copiedParentNode.getChildAt(0);

		assertSame(copiedChildNode, copiedParent.getWbsChildrenNodes().iterator().next());
	}

	@Test
	void moveSelectedTaskPreservesIdentityUniqueIdDependencyAndUndoRedo() throws Exception {
		Project project = createProjectWithoutVoidRows();
		NormalTask first = createTask(project, "First");
		NormalTask second = createTask(project, "Second");
		NormalTask third = createTask(project, "Third");
		Dependency dependency = DependencyService.getInstance().newDependency(second, third, DependencyType.FS, 0L, this);
		DefaultNodeModel model = (DefaultNodeModel)project.getTaskModel();
		Node firstNode = model.search(first);
		Node secondNode = model.search(second);
		Node thirdNode = model.search(third);
		long secondUniqueId = second.getUniqueId();
		project.getUndoController().clear();

		assertTrue(model.canMoveSelectedNodes(Arrays.asList(secondNode),-1));
		assertTrue(model.moveSelectedNodes(Arrays.asList(secondNode), -1, NodeModel.NORMAL));

		Node root = (Node)model.getHierarchy().getRoot();
		assertSame(secondNode, root.getChildAt(0));
		assertSame(firstNode, root.getChildAt(1));
		assertSame(thirdNode, root.getChildAt(2));
		assertSame(second, secondNode.getImpl());
		assertEquals(secondUniqueId, second.getUniqueId());
		assertSame(dependency, findDependency(second, third));
		assertEquals(1L, second.getId());
		assertEquals(2L, first.getId());

		project.getUndoController().undo();
		assertSame(firstNode, root.getChildAt(0));
		assertSame(secondNode, root.getChildAt(1));
		assertSame(dependency, findDependency(second, third));
		project.getUndoController().redo();
		assertSame(secondNode, root.getChildAt(0));
		assertEquals(secondUniqueId, second.getUniqueId());
	}

	@Test
	void permanentOutlineSortPreservesHierarchyStableTiesAndUndoRedo() throws Exception {
		Project project = createProjectWithoutVoidRows();
		NormalTask parent = createTask(project, "Zulu summary");
		NormalTask nestedZulu = createTask(project, "Zulu child");
		NormalTask nestedAlpha = createTask(project, "Alpha child");
		NormalTask alphaFirst = createTask(project, "Alpha");
		NormalTask alphaSecond = createTask(project, "Alpha");
		DefaultNodeModel model = (DefaultNodeModel) project.getTaskModel();
		Node parentNode = model.search(parent);
		Node nestedZuluNode = model.search(nestedZulu);
		Node nestedAlphaNode = model.search(nestedAlpha);
		Node alphaFirstNode = model.search(alphaFirst);
		Node alphaSecondNode = model.search(alphaSecond);
		assertTrue(model.relocate(List.of(nestedZuluNode, nestedAlphaNode), parentNode, 0, NodeModel.SILENT));
		Dependency dependency = DependencyService.getInstance().newDependency(nestedZulu, alphaFirst,
			DependencyType.FS, 0L, this);
		long revisionBeforeSort = project.getMutationState().revision();
		long[] uniqueIds = { parent.getUniqueId(), nestedZulu.getUniqueId(), nestedAlpha.getUniqueId(),
			alphaFirst.getUniqueId(), alphaSecond.getUniqueId() };
		project.getUndoController().clear();

		NodeSorter byTaskName = new NodeSorter() {
			@Override
			public int compare(Object left, Object right) {
				return ((com.microproject.pm.task.Task) ((Node) left).getImpl()).getName()
					.compareTo(((com.microproject.pm.task.Task) ((Node) right).getImpl()).getName());
			}
		};
		assertTrue(model.sortOutlineBy(byTaskName, NodeModel.NORMAL));
		assertEquals(revisionBeforeSort + 1, project.getMutationState().revision(),
			"a multi-branch permanent sort publishes as one project mutation");

		Node root = (Node) model.getHierarchy().getRoot();
		assertSame(alphaFirstNode, root.getChildAt(0));
		assertSame(alphaSecondNode, root.getChildAt(1), "equal keys retain their prior sibling order");
		assertSame(parentNode, root.getChildAt(2));
		assertSame(nestedAlphaNode, parentNode.getChildAt(0));
		assertSame(nestedZuluNode, parentNode.getChildAt(1));
		assertSame(parentNode, nestedAlphaNode.getParent());
		assertArrayEquals(uniqueIds, new long[] { parent.getUniqueId(), nestedZulu.getUniqueId(), nestedAlpha.getUniqueId(),
			alphaFirst.getUniqueId(), alphaSecond.getUniqueId() });
		assertEquals(List.of(1L, 2L, 3L, 4L, 5L), taskIds(project));
		assertSame(dependency, findDependency(nestedZulu, alphaFirst));

		assertTrue(project.getUndoController().canUndo());
		long revisionBeforeUndo = project.getMutationState().revision();
		project.getUndoController().undo();
		assertEquals(revisionBeforeUndo + 1, project.getMutationState().revision());
		assertSame(parentNode, root.getChildAt(0));
		assertSame(alphaFirstNode, root.getChildAt(1));
		assertSame(alphaSecondNode, root.getChildAt(2));
		assertSame(nestedZuluNode, parentNode.getChildAt(0));
		assertSame(nestedAlphaNode, parentNode.getChildAt(1));
		assertSame(dependency, findDependency(nestedZulu, alphaFirst));
		long revisionBeforeRedo = project.getMutationState().revision();
		project.getUndoController().redo();
		assertEquals(revisionBeforeRedo + 1, project.getMutationState().revision());
		assertSame(alphaFirstNode, root.getChildAt(0));
		assertSame(nestedAlphaNode, parentNode.getChildAt(0));
		assertSame(dependency, findDependency(nestedZulu, alphaFirst));
		project.getUndoController().clear();
		long revisionBeforeNoOp = project.getMutationState().revision();
		assertFalse(model.sortOutlineBy(byTaskName, NodeModel.NORMAL), "an already sorted outline is a true no-op");
		assertEquals(revisionBeforeNoOp, project.getMutationState().revision());
		assertFalse(project.getUndoController().canUndo(), "a no-op must not create an Undo edit");

		NodeSorter failingNestedSort = new NodeSorter() {
			@Override
			public int compare(Object left, Object right) {
				if (((Node) left).getParent() == parentNode)
					throw new IllegalStateException("nested comparison failed before mutation");
				return ((com.microproject.pm.task.Task) ((Node) right).getImpl()).getName()
					.compareTo(((com.microproject.pm.task.Task) ((Node) left).getImpl()).getName());
			}
		};
		assertThrows(IllegalStateException.class, () -> model.sortOutlineBy(failingNestedSort, NodeModel.NORMAL));
		assertSame(alphaFirstNode, root.getChildAt(0), "sort plans are validated before any sibling is moved");
		assertFalse(project.getUndoController().canUndo(), "a failed preflight must not create an Undo edit");
	}

	@Test
	void movingSummaryTaskMovesItsWholeBranch() {
		Project project = createProjectWithoutVoidRows();
		NormalTask summary = createTask(project, "Summary");
		NormalTask child = createTask(project, "Child");
		NormalTask sibling = createTask(project, "Sibling");
		DefaultNodeModel model = (DefaultNodeModel)project.getTaskModel();
		Node summaryNode = model.search(summary);
		Node childNode = model.search(child);
		Node siblingNode = model.search(sibling);
		assertTrue(model.relocate(Arrays.asList(childNode), summaryNode, 0, NodeModel.NORMAL));

		assertTrue(model.moveSelectedNodes(Arrays.asList(summaryNode), 1, NodeModel.NORMAL));

		Node root = (Node)model.getHierarchy().getRoot();
		assertSame(siblingNode, root.getChildAt(0));
		assertSame(summaryNode, root.getChildAt(1));
		assertSame(childNode, summaryNode.getChildAt(0));
		assertSame(summaryNode, childNode.getParent());
		assertEquals(1, child.getOutlineLevel());
	}

	@Test
	void movingContiguousSelectionPreservesItsOrder() {
		Project project = createProjectWithoutVoidRows();
		NormalTask first = createTask(project, "First");
		NormalTask second = createTask(project, "Second");
		NormalTask third = createTask(project, "Third");
		NormalTask fourth = createTask(project, "Fourth");
		DefaultNodeModel model = (DefaultNodeModel)project.getTaskModel();
		Node firstNode = model.search(first);
		Node secondNode = model.search(second);
		Node thirdNode = model.search(third);
		Node fourthNode = model.search(fourth);

		assertTrue(model.moveSelectedNodes(Arrays.asList(secondNode, thirdNode), 1, NodeModel.NORMAL));

		Node root = (Node)model.getHierarchy().getRoot();
		assertSame(firstNode, root.getChildAt(0));
		assertSame(fourthNode, root.getChildAt(1));
		assertSame(secondNode, root.getChildAt(2));
		assertSame(thirdNode, root.getChildAt(3));
		assertFalse(model.moveSelectedNodes(Arrays.asList(firstNode), -1, NodeModel.NORMAL));
		assertFalse(model.moveSelectedNodes(Arrays.asList(secondNode, thirdNode), 1, NodeModel.NORMAL));
		assertFalse(model.moveSelectedNodes(Arrays.asList(firstNode, secondNode), -1, NodeModel.NORMAL));
		assertFalse(model.canMoveSelectedNodes(Arrays.asList(firstNode),-1));
		assertFalse(model.canMoveSelectedNodes(Arrays.asList(secondNode,thirdNode),1));
		assertFalse(model.canRelocate(Arrays.asList(secondNode,thirdNode),root,2));
	}

	@Test
	void indentAndOutdentKeepTaskIdsUniqueAndInDepthFirstOrder() {
		Project project = createProjectWithoutVoidRows();
		NormalTask parent = createTask(project, "Parent");
		NormalTask child = createTask(project, "Child");
		NormalTask sibling = createTask(project, "Sibling");
		long parentUniqueId = parent.getUniqueId();
		long childUniqueId = child.getUniqueId();
		long siblingUniqueId = sibling.getUniqueId();
		DefaultNodeModel model = (DefaultNodeModel) project.getTaskModel();

		long revision = project.getMutationState().revision();
		model.indent(Arrays.asList(model.search(child)), 1, NodeModel.NORMAL);
		assertEquals(revision + 1, project.getMutationState().revision());
		assertEquals(List.of(1L, 2L, 3L), taskIds(project));
		assertEquals(parentUniqueId, parent.getUniqueId());
		assertEquals(childUniqueId, child.getUniqueId());
		assertEquals(siblingUniqueId, sibling.getUniqueId());

		long revisionAfterIndent = project.getMutationState().revision();
		model.indent(Arrays.asList(model.search(child)), 1, NodeModel.NORMAL);
		assertEquals(revisionAfterIndent, project.getMutationState().revision(), "an impossible second indent is a no-op");
		model.indent(Arrays.asList(model.search(child)), -1, NodeModel.NORMAL);
		assertEquals(revisionAfterIndent + 1, project.getMutationState().revision());
		assertEquals(List.of(1L, 2L, 3L), taskIds(project));
		assertEquals(childUniqueId, child.getUniqueId());
	}

	@Test
	void indentUndoAndRedoRestoreOriginalAndIndentedNodePositions() {
		Project project = createProjectWithoutVoidRows();
		NormalTask parent = createTask(project, "Parent");
		NormalTask child = createTask(project, "Child");
		NormalTask sibling = createTask(project, "Sibling");
		DefaultNodeModel model = (DefaultNodeModel)project.getTaskModel();
		Node root = (Node)model.getHierarchy().getRoot();
		Node parentNode = model.search(parent);
		Node childNode = model.search(child);
		Node siblingNode = model.search(sibling);
		project.getUndoController().clear();

		long revisionBeforeIndent = project.getMutationState().revision();
		model.indent(List.of(childNode), 1, NodeModel.NORMAL);
		assertEquals(revisionBeforeIndent + 1, project.getMutationState().revision());
		assertSame(parentNode, childNode.getParent());
		assertSame(siblingNode, root.getChildAt(1));

		long revisionBeforeUndo = project.getMutationState().revision();
		project.getUndoController().undo();
		assertEquals(revisionBeforeUndo + 1, project.getMutationState().revision());
		assertSame(root, childNode.getParent());
		assertSame(parentNode, root.getChildAt(0));
		assertSame(childNode, root.getChildAt(1));
		assertSame(siblingNode, root.getChildAt(2));

		long revisionBeforeRedo = project.getMutationState().revision();
		project.getUndoController().redo();
		assertEquals(revisionBeforeRedo + 1, project.getMutationState().revision());
		assertSame(parentNode, childNode.getParent());
		assertSame(siblingNode, root.getChildAt(1));
	}

	@Test
	void relocateCanMoveTaskToAnotherOutlineParentWithoutChangingUniqueId() {
		Project project = createProjectWithoutVoidRows();
		NormalTask firstSummary = createTask(project, "First summary");
		NormalTask secondSummary = createTask(project, "Second summary");
		NormalTask child = createTask(project, "Child");
		DefaultNodeModel model = (DefaultNodeModel)project.getTaskModel();
		Node firstSummaryNode = model.search(firstSummary);
		Node secondSummaryNode = model.search(secondSummary);
		Node childNode = model.search(child);
		long uniqueId = child.getUniqueId();
		assertTrue(model.relocate(Arrays.asList(childNode), firstSummaryNode, 0, NodeModel.NORMAL));

		assertTrue(model.relocate(Arrays.asList(childNode), secondSummaryNode, 0, NodeModel.NORMAL));

		assertSame(secondSummaryNode, childNode.getParent());
		assertEquals(uniqueId, child.getUniqueId());
		assertEquals(1, child.getOutlineLevel());
	}

	private static class StubDataFactory implements NodeModelDataFactory {
		public Object createUnvalidatedObject(NodeModel nodeModel, Object parent) {
			return new Object();
		}

		public void addUnvalidatedObject(Object object, NodeModel nodeModel, Object parent) {
		}

		public void validateObject(Object newlyCreated, NodeModel nodeModel, Object eventSource,
				Object hierarchyInfo, boolean isNew) {
		}

		public void remove(Object toRemove, NodeModel nodeModel, boolean deep, boolean undo,
				boolean cleanDependencies) {
		}

		public boolean isGroupDirty() {
			return false;
		}

		public void setGroupDirty(boolean isGroupDirty) {
		}

		public com.microproject.undo.DataFactoryUndoController getUndoController() {
			return null;
		}

		public void rollbackUnvalidated(NodeModel nodeModel, Object object) {
		}

		public void initOutline(NodeModel nodeModel) {
		}

		public NodeModelDataFactory getFactoryToUseForChildOfParent(Object impl) {
			return this;
		}

		public boolean containsAssignments() {
			return false;
		}
	}

	private static final class SearchAwareDataFactory extends StubDataFactory {
		private Node nodeSeenDuringValidation;

		@Override
		public void validateObject(Object newlyCreated, NodeModel nodeModel, Object eventSource,
				Object hierarchyInfo, boolean isNew) {
			nodeSeenDuringValidation = nodeModel.search(newlyCreated);
		}
	}

	private static final class TestSubProj implements com.microproject.pm.task.SubProj {
		private final Project subproject;
		private TestSubProj() { this(null); }
		private TestSubProj(Project subproject) { this.subproject = subproject; }
		public Project getSubproject() { return subproject; }
		public boolean isSubprojectOpen() { return false; }
		public boolean isValidAndOpen() { return false; }
		public boolean isWritable() { return false; }
		public long getSubprojectUniqueId() { return 0L; }
		public void setFetching(boolean fetching) { }
		public boolean isValid() { return false; }
		public void setSubprojectFieldValues(java.util.Map<String, Object> values) { }
		public void setSubprojectUniqueId(long subprojectId) { }
		public void setSchedulesFromSubprojectFieldValues() { }
	}

	private static final class TrackingField extends Field {
		private Object oldImplSeen;
		private Object valueSeen;

		@Override
		public boolean setInternalValue(Object object, Object value, FieldContext context) throws FieldParseException {
			oldImplSeen = object;
			valueSeen = value;
			return true;
		}
	}

	private static final class FailingField extends Field {
		@Override
		public boolean setInternalValue(Object object, Object value, FieldContext context) throws FieldParseException {
			throw new FieldParseException("boom");
		}
	}

	private Project createProject() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		project.getTaskModel();
		return project;
	}

	private Project createProjectWithoutVoidRows() {
		Project project = createProject();
		project.getTaskModel().getHierarchy().setNbEndVoidNodes(0);
		project.getTaskModel().getHierarchy().cleanVoidChildren();
		return project;
	}

	private NormalTask createTask(Project project, String name) {
		NormalTask task = new NormalTask(project);
		task.setName(name);
		project.connectTask(task);
		project.getTaskOutlines().addToAll(task, null);
		return task;
	}

	private List<Long> taskIds(Project project) {
		List<Long> ids = new ArrayList<>();
		for (Iterator<?> iterator = project.getTaskOutlineIterator(); iterator.hasNext();)
			ids.add(Long.valueOf(((com.microproject.pm.task.Task) iterator.next()).getId()));
		return ids;
	}

	private Dependency findDependency(NormalTask predecessor, NormalTask successor) {
		Iterator iterator = predecessor.getSuccessorList().iterator();
		while (iterator.hasNext()) {
			Dependency dependency = (Dependency) iterator.next();
			if (dependency.getPredecessor() == predecessor && dependency.getSuccessor() == successor) {
				return dependency;
			}
		}
		return null;
	}

	private static final class CapturingHierarchyListener implements HierarchyListener {
		int changedCount;
		int insertedCount;
		int removedCount;

		public void nodesChanged(HierarchyEvent e) {
			changedCount++;
		}

		public void nodesInserted(HierarchyEvent e) {
			insertedCount++;
		}

		public void nodesRemoved(HierarchyEvent e) {
			removedCount++;
		}

		public void structureChanged(HierarchyEvent e) {
		}

		void reset() {
			changedCount = 0;
			insertedCount = 0;
			removedCount = 0;
		}
	}
}
