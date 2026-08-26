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

import com.microproject.application.task.TaskCommands.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.microproject.field.FieldContext;
import com.microproject.configuration.Configuration;
import com.microproject.document.ObjectEvent;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.assignment.AssignmentService;
import com.microproject.options.CalendarOption;
import com.microproject.pm.scheduling.ConstraintType;
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
	void undoObserverSeesCommittedRevision() {
		Fixture fixture = fixture("gateway-observer-order");
		java.util.concurrent.atomic.AtomicLong observedRevision = new java.util.concurrent.atomic.AtomicLong(-1L);
		fixture.undo.addUndoStateListener(event -> {
			if (event.cause() == com.microproject.undo.UndoStateEvent.Cause.EDIT_ADDED)
				observedRevision.set(fixture.project.getDomainChangeJournal().revision());
		});

		new TaskCommandGateway(fixture.project).editField(command(fixture.task, "Before", "After"));

		assertEquals(1L, observedRevision.get());
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
		TaskAuthorizationPort denied = (keys, type) -> TaskAuthorizationPort.fixed(TaskAuthorizationPort.Decision.LOCK_DENIED);

		TaskCommandResult result = new TaskCommandGateway(fixture.project, denied)
				.editField(command(fixture.task, "Before", "After"));

		assertEquals(TaskCommandResult.Status.LOCK_DENIED, result.status());
		assertEquals("Before", fixture.task.getName());
		assertEquals(0L, fixture.project.getDomainChangeJournal().revision());
		assertFalse(fixture.undo.canUndo());
	}

	@Test
	void lockLossAtCommitRollsBackWithoutRevisionOrUndo() {
		Fixture fixture = fixture("gateway-lock-loss");
		TaskAuthorizationPort expiresAtCommit = new TaskAuthorizationPort() {
			@Override public AuthorizationLease acquire(Set<ProjectTaskKey> keys, TaskCommandType type) {
				return new AuthorizationLease() {
					@Override public Decision decision() { return Decision.ALLOWED; }
					@Override public boolean validateAtCommit() { return false; }
				};
			}
		};

		TaskCommandResult result = new TaskCommandGateway(fixture.project, expiresAtCommit)
				.editField(command(fixture.task, "Before", "After"));

		assertEquals(TaskCommandResult.Status.LOCK_DENIED, result.status());
		assertEquals("Before", fixture.task.getName());
		assertEquals(0L, fixture.project.getDomainChangeJournal().revision());
		assertFalse(fixture.project.isDirty());
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
	void dependencyDeleteIsAtomicAndUndoable() {
		Fixture fixture = fixture("gateway-dependency-delete");
		NormalTask successor = addTask(fixture.project, "Successor");
		TaskCommandGateway gateway = new TaskCommandGateway(fixture.project);
		gateway.createDependency(new TaskDependencyCommand(ProjectTaskKey.from(fixture.task).orElseThrow(),
				ProjectTaskKey.from(successor).orElseThrow(), DependencyType.FS, 0L, 0L));
		fixture.undo.clear();

		TaskCommandResult result = gateway.deleteDependency(new TaskDependencyDeleteCommand(
				ProjectTaskKey.from(fixture.task).orElseThrow(), ProjectTaskKey.from(successor).orElseThrow(),
				0L, DependencyType.FS, 1L));

		assertEquals(TaskCommandResult.Status.COMMITTED, result.status(), String.valueOf(result.failure()));
		assertTrue(fixture.task.getSuccessorList().findRight(successor) == null);
		assertEquals(2L, fixture.project.getDomainChangeJournal().revision());
		fixture.undo.undo();
		assertTrue(fixture.task.getSuccessorList().findRight(successor) != null);
	}

	@Test
	void dependencyBatchIsOneRevisionAndOneUndo() {
		Fixture fixture = fixture("gateway-dependency-batch");
		NormalTask second = addTask(fixture.project, "Second");
		NormalTask third = addTask(fixture.project, "Third");
		List<ProjectTaskKey> keys = List.of(ProjectTaskKey.from(fixture.task).orElseThrow(),
				ProjectTaskKey.from(second).orElseThrow(), ProjectTaskKey.from(third).orElseThrow());
		fixture.undo.clear();

		TaskCommandResult linked = new TaskCommandGateway(fixture.project).changeDependencies(
				new TaskDependencyBatchCommand(TaskDependencyBatchCommand.Operation.LINK, keys, 0L));

		assertEquals(TaskCommandResult.Status.COMMITTED, linked.status(), String.valueOf(linked.failure()));
		assertTrue(fixture.task.getSuccessorList().findRight(second) != null);
		assertTrue(second.getSuccessorList().findRight(third) != null);
		assertEquals(1L, fixture.project.getDomainChangeJournal().revision());
		fixture.undo.undo();
		assertTrue(fixture.task.getSuccessorList().findRight(second) == null);
		assertTrue(second.getSuccessorList().findRight(third) == null);
		assertEquals(2L, fixture.project.getDomainChangeJournal().revision());
	}

	@Test
	void dependencyBatchUnlinkRemovesEveryIncidentEdgeAtomically() {
		Fixture fixture = fixture("gateway-dependency-unlink-batch");
		NormalTask middle = addTask(fixture.project, "Middle");
		NormalTask last = addTask(fixture.project, "Last");
		TaskCommandGateway gateway = new TaskCommandGateway(fixture.project);
		List<ProjectTaskKey> all = List.of(ProjectTaskKey.from(fixture.task).orElseThrow(),
				ProjectTaskKey.from(middle).orElseThrow(), ProjectTaskKey.from(last).orElseThrow());
		gateway.changeDependencies(new TaskDependencyBatchCommand(TaskDependencyBatchCommand.Operation.LINK, all, 0L));
		fixture.undo.clear();

		TaskCommandResult unlinked = gateway.changeDependencies(new TaskDependencyBatchCommand(
				TaskDependencyBatchCommand.Operation.UNLINK, List.of(ProjectTaskKey.from(middle).orElseThrow()), 1L));

		assertEquals(TaskCommandResult.Status.COMMITTED, unlinked.status(), String.valueOf(unlinked.failure()));
		assertTrue(fixture.task.getSuccessorList().findRight(middle) == null);
		assertTrue(middle.getSuccessorList().findRight(last) == null);
		assertEquals(2L, fixture.project.getDomainChangeJournal().revision());
		fixture.undo.undo();
		assertTrue(fixture.task.getSuccessorList().findRight(middle) != null);
		assertTrue(middle.getSuccessorList().findRight(last) != null);
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
	void taskDeleteCommitsOneCapturedLegacyUndo() {
		Fixture fixture = fixture("gateway-create-delete");
		TaskCommandGateway gateway = new TaskCommandGateway(fixture.project);
		fixture.undo.clear();
		ProjectTaskKey key = ProjectTaskKey.from(fixture.task).orElseThrow();
		TaskCommandResult deleted = gateway.deleteTasks(new TaskDeleteCommand(List.of(key),
				fixture.project.getDomainChangeJournal().revision()));
		assertEquals(TaskCommandResult.Status.COMMITTED, deleted.status(), String.valueOf(deleted.failure()));
		assertTrue(fixture.project.getTaskModel().search(fixture.task) == null);
		fixture.undo.undo();
		assertTrue(fixture.project.getTaskModel().search(fixture.task) != null);
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
	void hierarchyIndentAuthorizesSelectedTaskAndParentCandidateTogether() {
		Fixture fixture = fixture("gateway-indent-authorization");
		NormalTask second = addTask(fixture.project, "Second");
		java.util.concurrent.atomic.AtomicReference<Set<ProjectTaskKey>> authorized =
				new java.util.concurrent.atomic.AtomicReference<>();
		TaskAuthorizationPort port = (keys, type) -> {
			authorized.set(keys);
			return TaskAuthorizationPort.fixed(TaskAuthorizationPort.Decision.ALLOWED);
		};

		TaskCommandResult result = new TaskCommandGateway(fixture.project, port).indentHierarchy(
				new TaskHierarchyIndentCommand(List.of(ProjectTaskKey.from(second).orElseThrow()), 1, 0L));

		assertEquals(TaskCommandResult.Status.COMMITTED, result.status(), String.valueOf(result.failure()));
		assertEquals(Set.of(ProjectTaskKey.from(fixture.task).orElseThrow(),
				ProjectTaskKey.from(second).orElseThrow()), authorized.get());
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

	@Test
	void multiCellEditCommitsOneRevisionAndOneUndo() {
		Fixture fixture = fixture("gateway-field-batch");
		NormalTask second = addTask(fixture.project, "Second");
		fixture.undo.clear();
		long beforeRevision = fixture.project.getDomainChangeJournal().revision();

		TaskCommandResult result = new TaskCommandGateway(fixture.project).editFields(
				new TaskFieldBatchEditCommand(List.of(
						command(fixture.task, "Before", "First changed"),
						command(second, "Second", "Second changed")), beforeRevision));

		assertEquals(TaskCommandResult.Status.COMMITTED, result.status(), String.valueOf(result.failure()));
		assertEquals("First changed", fixture.task.getName());
		assertEquals("Second changed", second.getName());
		assertEquals(beforeRevision + 1L, fixture.project.getDomainChangeJournal().revision());
		fixture.undo.undo();
		assertEquals("Before", fixture.task.getName());
		assertEquals("Second", second.getName());
		assertEquals(beforeRevision + 2L, fixture.project.getDomainChangeJournal().revision());
	}

	@Test
	void multiCellSetterFailureRollsBackEveryEarlierCellWithoutRevisionOrUndo() {
		Fixture fixture = fixture("gateway-field-batch-rollback");
		Node node = fixture.project.getTaskModel().search(fixture.task);
		var duration = Configuration.getFieldFromId("Field.duration");
		Object originalDuration = duration.getValue(node, fixture.project.getTaskModel(), new FieldContext());

		TaskCommandResult result = new TaskCommandGateway(fixture.project).editFields(
				new TaskFieldBatchEditCommand(List.of(
						command(fixture.task, "Before", "Must roll back"),
						new TaskFieldEditCommand(ProjectTaskKey.from(fixture.task).orElseThrow(), "Field.duration",
								originalDuration, "not a duration", new FieldContext())), 0L));

		assertEquals(TaskCommandResult.Status.FAILED, result.status());
		assertEquals("Before", fixture.task.getName());
		assertEquals(originalDuration, duration.getValue(node, fixture.project.getTaskModel(), new FieldContext()));
		assertEquals(0L, fixture.project.getDomainChangeJournal().revision());
		assertFalse(fixture.undo.canUndo());
		assertFalse(fixture.project.isDirty());
	}

	@Test
	void timelineMoveCommitsResourceAndStartAsOneAuthorizedRevisionAndUndo() {
		Fixture fixture = fixture("gateway-timeline");
		fixture.task.setManuallyScheduled(false);
		ResourceImpl first = fixture.project.getResourcePool().newResourceInstance();
		ResourceImpl second = fixture.project.getResourcePool().newResourceInstance();
		var assignment = AssignmentService.getInstance().newAssignment(fixture.task, first, 1D, 0L, this);
		assignment.setRequestDemandType(2);
		long originalStart = fixture.task.getStart();
		long newStart = fixture.project.getWorkCalendar().add(originalStart,
				CalendarOption.getInstance().getMillisPerDay(), false);
		fixture.undo.clear();
		long revision = fixture.project.getDomainChangeJournal().revision();
		java.util.concurrent.atomic.AtomicLong observed = new java.util.concurrent.atomic.AtomicLong(-1L);
		fixture.undo.addUndoStateListener(event -> observed.set(fixture.project.getDomainChangeJournal().revision()));

		TaskCommandResult result = new TaskCommandGateway(fixture.project).moveTimeline(new TaskTimelineMoveCommand(
				ProjectTaskKey.from(fixture.task).orElseThrow(), assignment.getUniqueId(), first.getUniqueId(),
				second.getUniqueId(), originalStart, newStart, revision));

		assertEquals(TaskCommandResult.Status.COMMITTED, result.status(), String.valueOf(result.failure()));
		assertEquals(revision + 1L, observed.get(), "Undo observers must see the committed revision");
		assertEquals(ConstraintType.SNET, fixture.task.getConstraintType());
		var moved = fixture.task.findAssignment(second);
		assertTrue(moved != null);
		assertEquals(2, moved.getRequestDemandType(), "replacement must retain assignment-specific fields");
		fixture.undo.undo();
		assertEquals(ConstraintType.ASAP, fixture.task.getConstraintType());
		assertTrue(fixture.task.findAssignment(first) == assignment, "Undo must restore the original assignment identity");
		fixture.undo.redo();
		assertTrue(fixture.task.findAssignment(second) == moved, "Redo must restore the replacement assignment identity");
	}

	@Test
	void timelineMoveRollsBackWhenAuthorizationExpiresAtCommit() {
		Fixture fixture = fixture("gateway-timeline-lock");
		fixture.task.setManuallyScheduled(false);
		ResourceImpl first = fixture.project.getResourcePool().newResourceInstance();
		ResourceImpl second = fixture.project.getResourcePool().newResourceInstance();
		var assignment = AssignmentService.getInstance().newAssignment(fixture.task, first, 1D, 0L, this);
		long originalStart = fixture.task.getStart();
		TaskAuthorizationPort expires = (keys, type) -> new TaskAuthorizationPort.AuthorizationLease() {
			@Override public TaskAuthorizationPort.Decision decision() { return TaskAuthorizationPort.Decision.ALLOWED; }
			@Override public boolean validateAtCommit() { return false; }
		};
		fixture.undo.clear();

		TaskCommandResult result = new TaskCommandGateway(fixture.project, expires).moveTimeline(
				new TaskTimelineMoveCommand(ProjectTaskKey.from(fixture.task).orElseThrow(), assignment.getUniqueId(),
						first.getUniqueId(), second.getUniqueId(), originalStart, originalStart + 1L, 0L));

		assertEquals(TaskCommandResult.Status.LOCK_DENIED, result.status());
		assertEquals(originalStart, fixture.task.getStart());
		assertEquals(0L, fixture.project.getDomainChangeJournal().revision());
		assertFalse(fixture.undo.canUndo());
		assertTrue(fixture.task.getAssignments().stream().map(com.microproject.pm.assignment.Assignment.class::cast)
				.anyMatch(value -> value.getResource() == first));
	}

	@Test
	void timelineMoveRestoresTheOriginalAssignmentWhenADeleteListenerFails() {
		Fixture fixture = fixture("gateway-timeline-listener-failure");
		ResourceImpl first = fixture.project.getResourcePool().newResourceInstance();
		ResourceImpl second = fixture.project.getResourcePool().newResourceInstance();
		var original = AssignmentService.getInstance().newAssignment(fixture.task, first, 1D, 0L, this);
		fixture.undo.clear();
		java.util.concurrent.atomic.AtomicBoolean failOnce = new java.util.concurrent.atomic.AtomicBoolean(true);
		ObjectEvent.Listener listener = event -> {
			if (event.isDelete() && event.getObject() == original && failOnce.getAndSet(false))
				throw new IllegalStateException("injected listener failure");
		};
		fixture.project.addObjectListener(listener);
		try {
			TaskCommandResult result = new TaskCommandGateway(fixture.project).moveTimeline(
					new TaskTimelineMoveCommand(ProjectTaskKey.from(fixture.task).orElseThrow(), original.getUniqueId(),
							first.getUniqueId(), second.getUniqueId(), fixture.task.getStart(), null, 0L));
			assertEquals(TaskCommandResult.Status.FAILED, result.status());
			assertTrue(fixture.task.findAssignment(first) == original);
			assertTrue(fixture.task.findAssignment(second) == null);
			assertEquals(0L, fixture.project.getDomainChangeJournal().revision());
			assertFalse(fixture.undo.canUndo());
		} finally {
			fixture.project.removeObjectListener(listener);
		}
	}

	@Test
	void timelineMoveRejectsAnAlreadyAssignedTargetWithoutMutation() {
		Fixture fixture = fixture("gateway-timeline-collision");
		ResourceImpl first = fixture.project.getResourcePool().newResourceInstance();
		ResourceImpl second = fixture.project.getResourcePool().newResourceInstance();
		var firstAssignment = AssignmentService.getInstance().newAssignment(fixture.task, first, 1D, 0L, this);
		AssignmentService.getInstance().newAssignment(fixture.task, second, 1D, 0L, this);
		fixture.undo.clear();

		TaskCommandResult result = new TaskCommandGateway(fixture.project).moveTimeline(new TaskTimelineMoveCommand(
				ProjectTaskKey.from(fixture.task).orElseThrow(), firstAssignment.getUniqueId(), first.getUniqueId(),
				second.getUniqueId(), fixture.task.getStart(), null, 0L));

		assertEquals(TaskCommandResult.Status.CONFLICT, result.status());
		assertTrue(fixture.task.findAssignment(first) != null);
		assertTrue(fixture.task.findAssignment(second) != null);
		assertEquals(0L, fixture.project.getDomainChangeJournal().revision());
		assertFalse(fixture.undo.canUndo());
	}

	@Test
	void timelineMoveToUnassignedRequiresTheLastRealAssignmentAndSupportsUndoRedo() {
		Fixture fixture = fixture("gateway-timeline-unassigned");
		ResourceImpl first = fixture.project.getResourcePool().newResourceInstance();
		ResourceImpl second = fixture.project.getResourcePool().newResourceInstance();
		var firstAssignment = AssignmentService.getInstance().newAssignment(fixture.task, first, 1D, 0L, this);
		AssignmentService.getInstance().newAssignment(fixture.task, second, 1D, 0L, this);
		fixture.undo.clear();
		TaskCommandGateway gateway = new TaskCommandGateway(fixture.project);

		TaskCommandResult collision = gateway.moveTimeline(new TaskTimelineMoveCommand(
				ProjectTaskKey.from(fixture.task).orElseThrow(), firstAssignment.getUniqueId(), first.getUniqueId(),
				ResourceImpl.getUnassignedInstance().getUniqueId(), fixture.task.getStart(), null, 0L));

		assertEquals(TaskCommandResult.Status.CONFLICT, collision.status());
		assertTrue(fixture.task.findAssignment(first) != null);
		AssignmentService.getInstance().remove(fixture.task.findAssignment(second), this, false);
		fixture.undo.clear();
		TaskCommandResult committed = gateway.moveTimeline(new TaskTimelineMoveCommand(
				ProjectTaskKey.from(fixture.task).orElseThrow(), firstAssignment.getUniqueId(), first.getUniqueId(),
				ResourceImpl.getUnassignedInstance().getUniqueId(), fixture.task.getStart(), null, 0L));

		assertEquals(TaskCommandResult.Status.COMMITTED, committed.status(), String.valueOf(committed.failure()));
		assertTrue(fixture.task.findAssignment(ResourceImpl.getUnassignedInstance()) != null);
		fixture.undo.undo();
		assertTrue(fixture.task.findAssignment(first) != null);
		fixture.undo.redo();
		assertTrue(fixture.task.findAssignment(ResourceImpl.getUnassignedInstance()) != null);
	}

	@Test
	void assignmentBatchUsesOneRevisionAndOneUndoBoundary() {
		Fixture fixture = fixture("gateway-assignment-batch");
		ResourceImpl resource = fixture.project.getResourcePool().newResourceInstance();
		NormalTask second = addTask(fixture.project, "Second");
		fixture.undo.clear();
		TaskCommandGateway gateway = new TaskCommandGateway(fixture.project);
		List<ProjectTaskKey> tasks = List.of(ProjectTaskKey.from(fixture.task).orElseThrow(),
				ProjectTaskKey.from(second).orElseThrow());

		TaskCommandResult result = gateway.changeAssignments(new TaskAssignmentBatchCommand(
				tasks, List.of(resource.getUniqueId()), List.of(), 1D, 0L));

		assertEquals(TaskCommandResult.Status.COMMITTED, result.status(), String.valueOf(result.failure()));
		assertEquals(1L, fixture.project.getDomainChangeJournal().revision());
		assertTrue(fixture.task.findAssignment(resource) != null);
		assertTrue(second.findAssignment(resource) != null);
		fixture.undo.undo();
		assertTrue(fixture.task.findAssignment(resource) == null);
		assertTrue(second.findAssignment(resource) == null);
		fixture.undo.redo();
		assertTrue(fixture.task.findAssignment(resource) != null);
		assertTrue(second.findAssignment(resource) != null);
		fixture.undo.clear();
		long beforeNoOp = fixture.project.getDomainChangeJournal().revision();
		TaskCommandResult noOp = gateway.changeAssignments(new TaskAssignmentBatchCommand(
				tasks, List.of(resource.getUniqueId()), List.of(), 1D, beforeNoOp));
		assertEquals(TaskCommandResult.Status.NO_OP, noOp.status());
		assertEquals(beforeNoOp, fixture.project.getDomainChangeJournal().revision());
		assertFalse(fixture.undo.canUndo());

		TaskCommandResult removed = gateway.changeAssignments(new TaskAssignmentBatchCommand(
				tasks, List.of(), List.of(resource.getUniqueId()), 0D,
				fixture.project.getDomainChangeJournal().revision()));
		assertEquals(TaskCommandResult.Status.COMMITTED, removed.status(), String.valueOf(removed.failure()));
		assertTrue(fixture.task.findAssignment(resource) == null);
		assertTrue(second.findAssignment(resource) == null);
		fixture.undo.undo();
		assertTrue(fixture.task.findAssignment(resource) != null);
		assertTrue(second.findAssignment(resource) != null);
		fixture.undo.redo();
		assertTrue(fixture.task.findAssignment(resource) == null);
		assertTrue(second.findAssignment(resource) == null);
	}

	@Test
	void assignmentReplacementRollsBackAsOneDeltaWhenAuthorizationExpires() {
		Fixture fixture = fixture("gateway-assignment-replace-lock");
		ResourceImpl before = fixture.project.getResourcePool().newResourceInstance();
		ResourceImpl after = fixture.project.getResourcePool().newResourceInstance();
		var original = AssignmentService.getInstance().newAssignment(fixture.task, before, 0.75D, 123L, this);
		fixture.undo.clear();
		TaskAuthorizationPort expires = (keys, type) -> new TaskAuthorizationPort.AuthorizationLease() {
			@Override public TaskAuthorizationPort.Decision decision() { return TaskAuthorizationPort.Decision.ALLOWED; }
			@Override public boolean validateAtCommit() { return false; }
		};

		TaskCommandResult result = new TaskCommandGateway(fixture.project, expires).changeAssignments(
				new TaskAssignmentBatchCommand(List.of(ProjectTaskKey.from(fixture.task).orElseThrow()),
						List.of(after.getUniqueId()), List.of(before.getUniqueId()), 1D, 0L));

		assertEquals(TaskCommandResult.Status.LOCK_DENIED, result.status());
		assertTrue(fixture.task.findAssignment(before) == original, "rollback must restore assignment identity and fields");
		assertTrue(fixture.task.findAssignment(after) == null);
		assertEquals(0L, fixture.project.getDomainChangeJournal().revision());
		assertFalse(fixture.undo.canUndo());
	}

	@Test
	void assignmentDeltaRollsBackWhenAnObjectListenerFailsAfterDeletion() {
		Fixture fixture = fixture("gateway-assignment-listener-failure");
		ResourceImpl before = fixture.project.getResourcePool().newResourceInstance();
		ResourceImpl after = fixture.project.getResourcePool().newResourceInstance();
		var original = AssignmentService.getInstance().newAssignment(fixture.task, before, 1D, 0L, this);
		fixture.undo.clear();
		java.util.concurrent.atomic.AtomicBoolean failOnce = new java.util.concurrent.atomic.AtomicBoolean(true);
		ObjectEvent.Listener listener = event -> {
			if (event.isDelete() && event.getObject() == original && failOnce.getAndSet(false))
				throw new IllegalStateException("injected listener failure");
		};
		fixture.project.addObjectListener(listener);
		try {
			TaskCommandResult result = new TaskCommandGateway(fixture.project).changeAssignments(
					new TaskAssignmentBatchCommand(List.of(ProjectTaskKey.from(fixture.task).orElseThrow()),
							List.of(after.getUniqueId()), List.of(before.getUniqueId()), 1D, 0L));
			assertEquals(TaskCommandResult.Status.FAILED, result.status());
			assertTrue(fixture.task.findAssignment(before) == original);
			assertTrue(fixture.task.findAssignment(after) == null);
			assertEquals(0L, fixture.project.getDomainChangeJournal().revision());
			assertFalse(fixture.undo.canUndo());
		} finally {
			fixture.project.removeObjectListener(listener);
		}
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
