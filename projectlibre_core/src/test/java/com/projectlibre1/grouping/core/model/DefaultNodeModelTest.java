package com.projectlibre1.grouping.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;

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

	private static final class StubDataFactory implements NodeModelDataFactory {
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
