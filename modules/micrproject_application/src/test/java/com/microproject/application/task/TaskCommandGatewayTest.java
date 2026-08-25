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
package com.microproject.application.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.microproject.field.FieldContext;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.scheduling.ScheduleService;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.ProjectTaskKey;
import com.microproject.transaction.DomainChangeSet;
import com.microproject.undo.DataFactoryUndoController;

class TaskCommandGatewayTest {
	@Test
	void fieldEditCommitsOneRevisionAndOneUndo() {
		Fixture fixture = fixture("gateway-edit");
		TaskCommandGateway gateway = new TaskCommandGateway(fixture.project);

		TaskCommandResult result = gateway.editField(command(fixture.task, "Before", "After"));

		assertEquals(TaskCommandResult.Status.COMMITTED, result.status(), String.valueOf(result.failure()));
		assertEquals("After", fixture.task.getName());
		assertEquals(1L, fixture.project.getDomainChangeJournal().revision());
		assertTrue(fixture.undo.canUndo());
		fixture.undo.undo();
		assertEquals("Before", fixture.task.getName());
		assertEquals(2L, fixture.project.getDomainChangeJournal().revision());
		fixture.undo.redo();
		assertEquals("After", fixture.task.getName());
		assertEquals(3L, fixture.project.getDomainChangeJournal().revision());
	}

	@Test
	void staleFieldDraftReturnsConflictWithoutMutationRevisionOrUndo() {
		Fixture fixture = fixture("gateway-conflict");

		TaskCommandResult result = new TaskCommandGateway(fixture.project)
				.editField(command(fixture.task, "Stale", "After"));

		assertEquals(TaskCommandResult.Status.CONFLICT, result.status());
		assertEquals("Before", fixture.task.getName());
		assertEquals(0L, fixture.project.getDomainChangeJournal().revision());
		assertFalse(fixture.undo.canUndo());
	}

	@Test
	void lockDenialHasNoDomainSideEffects() {
		Fixture fixture = fixture("gateway-lock");
		TaskAuthorizationPort denied = (key, type) -> TaskAuthorizationPort.Decision.LOCK_DENIED;

		TaskCommandResult result = new TaskCommandGateway(fixture.project, denied)
				.editField(command(fixture.task, "Before", "After"));

		assertEquals(TaskCommandResult.Status.LOCK_DENIED, result.status());
		assertEquals("Before", fixture.task.getName());
		assertEquals(0L, fixture.project.getDomainChangeJournal().revision());
		assertFalse(fixture.undo.canUndo());
	}

	@Test
	void identicalValueIsANoOp() {
		Fixture fixture = fixture("gateway-noop");

		TaskCommandResult result = new TaskCommandGateway(fixture.project)
				.editField(command(fixture.task, "Before", "Before"));

		assertEquals(TaskCommandResult.Status.NO_OP, result.status());
		assertEquals(0L, fixture.project.getDomainChangeJournal().revision());
		assertFalse(fixture.undo.canUndo());
	}

	@Test
	void dependencyCommitIsAtomicAndUndoable() {
		Fixture fixture = fixture("gateway-dependency");
		NormalTask successor = addTask(fixture.project, "Successor");
		TaskCommandGateway gateway = new TaskCommandGateway(fixture.project);

		TaskCommandResult result = gateway.createDependency(new TaskDependencyCommand(
				ProjectTaskKey.from(fixture.task).orElseThrow(), ProjectTaskKey.from(successor).orElseThrow(),
				DependencyType.FS, 0L, 0L));

		assertEquals(TaskCommandResult.Status.COMMITTED, result.status());
		assertEquals(1L, fixture.project.getDomainChangeJournal().revision());
		assertTrue(fixture.task.getSuccessorList().findRight(successor) != null);
		fixture.undo.undo();
		assertTrue(fixture.task.getSuccessorList().findRight(successor) == null);
		assertEquals(2L, fixture.project.getDomainChangeJournal().revision());
		fixture.undo.redo();
		assertTrue(fixture.task.getSuccessorList().findRight(successor) != null);
		assertEquals(3L, fixture.project.getDomainChangeJournal().revision());
	}

	@Test
	void staleDependencyGestureDoesNotMutateDirtyUndoOrRevision() {
		Fixture fixture = fixture("gateway-stale-dependency");
		NormalTask successor = addTask(fixture.project, "Successor");
		fixture.project.getDomainChangeJournal().recordLegacy(DomainChangeSet.Origin.LEGACY);
		fixture.undo.clear();
		fixture.project.setDirty(false);

		TaskCommandResult result = new TaskCommandGateway(fixture.project).createDependency(new TaskDependencyCommand(
				ProjectTaskKey.from(fixture.task).orElseThrow(), ProjectTaskKey.from(successor).orElseThrow(),
				DependencyType.FS, 0L, 0L));

		assertEquals(TaskCommandResult.Status.CONFLICT, result.status());
		assertTrue(fixture.task.getSuccessorList().findRight(successor) == null);
		assertEquals(1L, fixture.project.getDomainChangeJournal().revision());
		assertFalse(fixture.project.isDirty());
		assertFalse(fixture.undo.canUndo());
	}

	@Test
	void concurrentCommandsForOneProjectAreSerializedAndOnlyOneRevisionCommits() throws Exception {
		Fixture fixture = fixture("gateway-concurrent");
		NormalTask successor = addTask(fixture.project, "Successor");
		TaskDependencyCommand command = new TaskDependencyCommand(ProjectTaskKey.from(fixture.task).orElseThrow(),
				ProjectTaskKey.from(successor).orElseThrow(), DependencyType.FS, 0L, 0L);
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		try (var executor = Executors.newFixedThreadPool(2)) {
			var first = executor.submit(() -> { ready.countDown(); start.await(); return new TaskCommandGateway(fixture.project).createDependency(command); });
			var second = executor.submit(() -> { ready.countDown(); start.await(); return new TaskCommandGateway(fixture.project).createDependency(command); });
			assertTrue(ready.await(5, TimeUnit.SECONDS));
			start.countDown();
			List<TaskCommandResult.Status> statuses = List.of(first.get(5, TimeUnit.SECONDS).status(),
					second.get(5, TimeUnit.SECONDS).status());
			assertEquals(1L, statuses.stream().filter(TaskCommandResult.Status.COMMITTED::equals).count());
			assertEquals(1L, fixture.project.getDomainChangeJournal().revision());
			assertTrue(fixture.task.getSuccessorList().findRight(successor) != null);
		}
	}

	@Test
	void hierarchyMovePreservesIdentityAndIsOneUndoTransaction() {
		Fixture fixture = fixture("gateway-move");
		NormalTask second = addTask(fixture.project, "Second");
		NormalTask third = addTask(fixture.project, "Third");
		fixture.undo.clear();
		fixture.project.setDirty(false);
		Object parent = fixture.project.getTaskModel().search(second).getParent();

		TaskCommandResult result = new TaskCommandGateway(fixture.project).moveHierarchy(new TaskHierarchyMoveCommand(
				List.of(ProjectTaskKey.from(second).orElseThrow()), -1, 0L));

		assertEquals(TaskCommandResult.Status.COMMITTED, result.status());
		assertEquals(0, ((com.microproject.grouping.core.Node) parent).getIndex(fixture.project.getTaskModel().search(second)));
		assertEquals(1L, fixture.project.getDomainChangeJournal().revision());
		fixture.undo.undo();
		assertEquals(1, ((com.microproject.grouping.core.Node) parent).getIndex(fixture.project.getTaskModel().search(second)));
		assertEquals(2L, fixture.project.getDomainChangeJournal().revision());
		assertTrue(fixture.project.getTaskModel().search(third) != null);
	}

	@Test
	void hierarchyIndentUsesOneRevisionAndRestoresExactParentOnUndo() {
		Fixture fixture = fixture("gateway-indent");
		NormalTask second = addTask(fixture.project, "Second");
		Node firstNode = fixture.project.getTaskModel().search(fixture.task);
		Node secondNode = fixture.project.getTaskModel().search(second);
		Node root = (Node) secondNode.getParent();
		fixture.undo.clear();
		fixture.project.setDirty(false);

		TaskCommandResult result = new TaskCommandGateway(fixture.project).indentHierarchy(
				new TaskHierarchyIndentCommand(List.of(ProjectTaskKey.from(second).orElseThrow()), 1, 0L));

		assertEquals(TaskCommandResult.Status.COMMITTED, result.status(), String.valueOf(result.failure()));
		assertEquals(firstNode, secondNode.getParent());
		assertEquals(1L, fixture.project.getDomainChangeJournal().revision());
		assertTrue(fixture.undo.canUndo());
		fixture.undo.undo();
		assertEquals(root, secondNode.getParent());
		assertEquals(1, root.getIndex(secondNode));
		assertEquals(2L, fixture.project.getDomainChangeJournal().revision());
	}

	@Test
	void pasteIsOneRollbackCapableCommandAndOneUndo() {
		Fixture fixture = fixture("gateway-paste");
		Node original = fixture.project.getTaskModel().search(fixture.task);
		@SuppressWarnings("unchecked")
		List<Node> detached = fixture.project.getTaskModel().copy(List.of(original), NodeModel.SILENT);
		Node root = (Node) fixture.project.getTaskModel().getRoot();
		int before = taskChildCount(root);
		fixture.undo.clear();
		fixture.project.setDirty(false);

		TaskCommandResult result = new TaskCommandGateway(fixture.project).paste(
				new TaskPasteCommand(null, root.getChildCount(), detached, 0L));

		assertEquals(TaskCommandResult.Status.COMMITTED, result.status());
		assertEquals(before + 1, taskChildCount(root));
		assertEquals(1L, fixture.project.getDomainChangeJournal().revision());
		fixture.undo.undo();
		assertEquals(before, taskChildCount(root));
		assertEquals(2L, fixture.project.getDomainChangeJournal().revision());
	}

	@Test
	void staleProgressDragHasNoSideEffects() {
		Fixture fixture = fixture("gateway-progress");
		long expected = ScheduleService.getInstance().getCompleted(fixture.task);

		TaskCommandResult result = new TaskCommandGateway(fixture.project).updateProgress(new TaskProgressCommand(
				ProjectTaskKey.from(fixture.task).orElseThrow(), expected + 1L, expected + 2L, 0L));

		assertEquals(TaskCommandResult.Status.CONFLICT, result.status());
		assertEquals(expected, ScheduleService.getInstance().getCompleted(fixture.task));
		assertEquals(0L, fixture.project.getDomainChangeJournal().revision());
		assertFalse(fixture.undo.canUndo());
	}

	private static TaskFieldEditCommand command(NormalTask task, Object expected, Object proposed) {
		return new TaskFieldEditCommand(ProjectTaskKey.from(task).orElseThrow(), "Field.name", expected, proposed,
				new FieldContext());
	}

	private static Fixture fixture(String name) {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool(name, undo), undo);
		project.initialize(false, false);
		NormalTask task = project.createScriptedTask();
		task.setName("Before");
		project.connectTask(task);
		project.getTaskOutlines().addToAll(task, null);
		undo.clear();
		project.setDirty(false);
		return new Fixture(project, task, undo);
	}

	private static NormalTask addTask(Project project, String name) {
		NormalTask task = project.createScriptedTask();
		task.setName(name);
		project.connectTask(task);
		project.getTaskOutlines().addToAll(task, null);
		return task;
	}

	private static int taskChildCount(Node parent) {
		int count = 0;
		for (int index = 0; index < parent.getChildCount(); index++)
			if (((Node) parent.getChildAt(index)).getImpl() instanceof com.microproject.pm.task.Task) count++;
		return count;
	}

	private record Fixture(Project project, NormalTask task, DataFactoryUndoController undo) {
	}
}
