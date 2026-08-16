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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.field.Field;
import com.microproject.field.FieldContext;
import com.microproject.field.FieldParseException;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.NodeFactory;
import com.microproject.grouping.core.event.HierarchyEvent;
import com.microproject.grouping.core.event.HierarchyListener;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.undo.UndoController;

class DefaultNodeModelTest {
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

		List children = model.getHierarchy().getChildren(root);
		assertEquals(4, children.size());
		assertTrue(children.get(0) != children.get(1));
		assertTrue(children.get(1) != children.get(2));
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
		List children = new ArrayList();
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

		List children = model.getHierarchy().getChildren(root);
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

		List previousChildren = model.getHierarchy().getChildren(previous);
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
		public void setSubprojectFieldValues(java.util.Map values) { }
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
