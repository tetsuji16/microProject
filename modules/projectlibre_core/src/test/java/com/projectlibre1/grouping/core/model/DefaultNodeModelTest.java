package com.projectlibre1.grouping.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import com.projectlibre1.field.Field;
import com.projectlibre1.field.FieldContext;
import com.projectlibre1.field.FieldParseException;
import com.projectlibre1.grouping.core.Node;
import com.projectlibre1.grouping.core.NodeFactory;
import com.projectlibre1.grouping.core.event.HierarchyEvent;
import com.projectlibre1.grouping.core.event.HierarchyListener;
import com.projectlibre1.pm.dependency.Dependency;
import com.projectlibre1.pm.dependency.DependencyService;
import com.projectlibre1.pm.dependency.DependencyType;
import com.projectlibre1.pm.resource.ResourcePool;
import com.projectlibre1.pm.task.NormalTask;
import com.projectlibre1.pm.task.Project;
import com.projectlibre1.undo.DataFactoryUndoController;

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

		public com.projectlibre1.undo.DataFactoryUndoController getUndoController() {
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
