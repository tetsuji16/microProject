package com.microproject.grouping.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.NodeFactory;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.transaction.MultipleTransactionManager;
import com.microproject.undo.DataFactoryUndoController;

/** A paste that fails while registering a descendant must leave no partial hierarchy, task or Undo state. */
class TaskPasteRollbackTest {
	private static final class FailingTask extends NormalTask {
		private static final long serialVersionUID = 1L;
		private boolean armed;
		FailingTask(Project project) { super(project); }
		void arm() { armed = true; }
		@Override public void markTaskAsNeedingRecalculation() {
			if (armed) { armed = false; throw new IllegalStateException("injected paste failure"); } // one-shot
			super.markTaskAsNeedingRecalculation();
		}
	}

	@Test
	void failureWhileRegisteringALaterTaskRestoresHierarchyTasksTransactionAndUndo() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("paste-rollback", undoController), undoController);
		project.initialize(false, false);
		DefaultNodeModel model = (DefaultNodeModel) project.getTaskModel();
		model.getHierarchy().setNbEndVoidNodes(0);
		Node root = (Node) model.getHierarchy().getRoot();
		NormalTask existing = project.createScriptedTask();
		existing.setName("Existing");
		undoController.clear();

		NormalTask first = new NormalTask(project);
		first.setName("Pasted first");
		FailingTask second = new FailingTask(project);
		second.setName("Pasted second");
		second.arm();
		Node firstNode = NodeFactory.getInstance().createNode(first);
		Node secondNode = NodeFactory.getInstance().createNode(second);

		List<Task> tasksBefore = List.copyOf(project.getTaskList());
		int childrenBefore = root.getChildCount();
		int depthBefore = MultipleTransactionManager.depth;

		assertThrows(RuntimeException.class,
			() -> model.paste(root, List.of(firstNode, secondNode), 0, NodeModel.NORMAL | NodeModel.UNDO));

		assertEquals(childrenBefore, root.getChildCount(), "no pasted root may remain attached");
		assertEquals(tasksBefore, project.getTaskList(), "tasks registered before the failure must be removed");
		assertNull(firstNode.getParent());
		assertNull(secondNode.getParent());
		assertEquals(depthBefore, MultipleTransactionManager.depth, "the multiple-transaction bracket must be balanced");
		assertFalse(undoController.canUndo(), "a failed paste must not leave an Undo entry");
	}

	@Test
	void listenerFailureAfterTheInsertionWasAnnouncedDoesNotSilentlyDetachThePastedRows() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("paste-commit", undoController), undoController);
		project.initialize(false, false);
		DefaultNodeModel model = (DefaultNodeModel) project.getTaskModel();
		model.getHierarchy().setNbEndVoidNodes(0);
		Node root = (Node) model.getHierarchy().getRoot();
		NormalTask pasted = new NormalTask(project);
		pasted.setName("Pasted");
		Node pastedNode = NodeFactory.getInstance().createNode(pasted);
		model.getHierarchy().addHierarchyListener(new com.microproject.grouping.core.event.HierarchyListener() {
			@Override public void nodesChanged(com.microproject.grouping.core.event.HierarchyEvent e) { }
			@Override public void nodesInserted(com.microproject.grouping.core.event.HierarchyEvent e) {
				throw new IllegalStateException("injected observer failure");
			}
			@Override public void nodesRemoved(com.microproject.grouping.core.event.HierarchyEvent e) { }
			@Override public void structureChanged(com.microproject.grouping.core.event.HierarchyEvent e) { }
		});
		int depthBefore = MultipleTransactionManager.depth;

		assertThrows(RuntimeException.class,
			() -> model.paste(root, List.of(pastedNode), 0, NodeModel.NORMAL | NodeModel.UNDO));

		// Observers were already told about the insertion, so the committed rows and task stay in place.
		assertEquals(root, pastedNode.getParent());
		assertEquals(true, project.getTaskList().contains(pasted));
		assertEquals(depthBefore, MultipleTransactionManager.depth);
	}
}
