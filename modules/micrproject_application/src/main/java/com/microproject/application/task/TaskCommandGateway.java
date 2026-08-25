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

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.swing.undo.CompoundEdit;

import com.microproject.configuration.Configuration;
import com.microproject.association.InvalidAssociationException;
import com.microproject.field.Field;
import com.microproject.field.FieldContext;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.ProjectTaskKey;
import com.microproject.pm.task.SubProj;
import com.microproject.pm.task.Task;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.HasDependencies;
import com.microproject.pm.scheduling.Schedule;
import com.microproject.pm.scheduling.ScheduleInterval;
import com.microproject.pm.scheduling.ScheduleService;
import com.microproject.transaction.DomainChangeSet;
import com.microproject.transaction.ModelTransaction;
import com.microproject.undo.ModelFieldEdit;
import com.microproject.undo.DependencyCreationEdit;
import com.microproject.undo.DependencySetFieldsEdit;
import com.microproject.undo.NodeRelocationEdit;
import com.microproject.undo.NodeIndentEdit;
import com.microproject.undo.ScheduleEdit;
import com.microproject.undo.TaskConstraintEdit;
import com.microproject.undo.NodePasteEdit;
import com.microproject.undo.FieldEdit;
import com.microproject.undo.SplitEdit;

/** Headless per-project use-case gateway. */
public final class TaskCommandGateway {
	private final Project project;
	private final TaskAuthorizationPort authorization;

	public TaskCommandGateway(Project project) {
		this(project, (key, commandType) -> project != null && project.getCollaborationSession() == null
				? TaskAuthorizationPort.Decision.ALLOWED : TaskAuthorizationPort.Decision.LOCK_DENIED);
	}

	public TaskCommandGateway(Project project, TaskAuthorizationPort authorization) {
		this.project = Objects.requireNonNull(project, "project");
		this.authorization = Objects.requireNonNull(authorization, "authorization");
	}

	public TaskCommandResult editField(TaskFieldEditCommand command) {
		Task task = resolve(command.taskKey());
		Project owner = task == null ? null : task.getOwningProject();
		if (owner == null) return TaskCommandResult.of(TaskCommandResult.Status.NOT_FOUND);
		return owner.getDomainChangeJournal().write(() -> editFieldLocked(command));
	}

	private TaskCommandResult editFieldLocked(TaskFieldEditCommand command) {
		Task task = resolve(command.taskKey());
		if (task == null)
			return TaskCommandResult.of(TaskCommandResult.Status.NOT_FOUND);
		Project owner = task.getOwningProject();
		if (project.isReadOnly() || task.isReadOnly() || owner == null || owner.isReadOnly())
			return TaskCommandResult.of(TaskCommandResult.Status.READ_ONLY);
		TaskAuthorizationPort.Decision decision = authorization.authorize(command.taskKey(), TaskCommandType.EDIT_FIELD);
		if (decision == TaskAuthorizationPort.Decision.LOCK_DENIED)
			return TaskCommandResult.of(TaskCommandResult.Status.LOCK_DENIED);
		if (decision == TaskAuthorizationPort.Decision.READ_ONLY)
			return TaskCommandResult.of(TaskCommandResult.Status.READ_ONLY);
		Field field = Configuration.getFieldFromId(command.fieldId());
		NodeModel model = owner.getTaskModel();
		Node node = model.search(task);
		if (field == null || node == null)
			return TaskCommandResult.of(TaskCommandResult.Status.INVALID);
		FieldContext context = command.fieldContext() == null ? new FieldContext() : command.fieldContext();
		Object original = field.getValue(node, model, context);
		if (!Objects.deepEquals(original, command.expectedValue()))
			return TaskCommandResult.of(TaskCommandResult.Status.CONFLICT);
		if (Objects.deepEquals(original, command.proposedValue()))
			return TaskCommandResult.of(TaskCommandResult.Status.NO_OP);
		if (field.isReadOnly(node, model, context))
			return TaskCommandResult.of(TaskCommandResult.Status.READ_ONLY);
		boolean projectDirty = owner.isDirty();
		boolean taskDirty = task.isDirty();
		ProjectScheduleBackup scheduleBackup = isScheduleField(command.fieldId()) ? captureSchedule(owner) : null;
		ModelTransaction<Object> transaction = ModelTransaction.<Object>builder()
				.captureRollback(() -> () -> {
					if (scheduleBackup != null) restoreSchedule(owner, scheduleBackup);
					else field.setValue(node, model, this, original, context);
					task.setDirty(taskDirty);
					owner.setDirty(projectDirty);
				})
				.apply(() -> {
					field.setValue(node, model, this, command.proposedValue(), context);
					Object applied = field.getValue(node, model, context);
					return Objects.deepEquals(original, applied)
							? ModelTransaction.Mutation.noOp(applied)
							: ModelTransaction.Mutation.changed(applied);
				})
				.invariant(() -> resolve(command.taskKey()) == task)
				.commitUndo(applied -> {
					if (owner.getUndoController() != null)
						owner.getUndoController().commitEdit(
								new ModelFieldEdit(model, field, node, this, applied, original, context));
				})
				.changes(applied -> new DomainChangeSet.Draft(UUID.randomUUID(), DomainChangeSet.Origin.COMMAND,
						Set.of(command.taskKey()), Set.of(command.fieldId()), DomainChangeSet.TopologyImpact.NONE,
						isScheduleField(command.fieldId()), false))
				.onRecoveryFailure(failure -> owner.setReadOnly(true))
				.build();
		return map(transaction.execute(owner.getDomainChangeJournal()));
	}

	public TaskCommandResult createDependency(TaskDependencyCommand command) {
		return project.getDomainChangeJournal().write(() -> createDependencyLocked(command));
	}

	public TaskCommandResult updateDependency(TaskDependencyUpdateCommand command) {
		return project.getDomainChangeJournal().write(() -> updateDependencyLocked(command));
	}

	private TaskCommandResult updateDependencyLocked(TaskDependencyUpdateCommand command) {
		if (project.getDomainChangeJournal().revision() != command.expectedDomainRevision())
			return TaskCommandResult.of(TaskCommandResult.Status.CONFLICT);
		Task predecessor = resolve(command.predecessor());
		Task successor = resolve(command.successor());
		Dependency dependency = predecessor == null || successor == null ? null : findDependency(predecessor, successor);
		if (dependency == null) return TaskCommandResult.of(TaskCommandResult.Status.NOT_FOUND);
		if (project.isReadOnly() || predecessor.isReadOnly() || successor.isReadOnly())
			return TaskCommandResult.of(TaskCommandResult.Status.READ_ONLY);
		for (ProjectTaskKey key : List.of(command.predecessor(), command.successor())) {
			TaskAuthorizationPort.Decision decision = authorization.authorize(key, TaskCommandType.UPDATE_DEPENDENCY);
			if (decision == TaskAuthorizationPort.Decision.LOCK_DENIED)
				return TaskCommandResult.of(TaskCommandResult.Status.LOCK_DENIED);
			if (decision == TaskAuthorizationPort.Decision.READ_ONLY)
				return TaskCommandResult.of(TaskCommandResult.Status.READ_ONLY);
		}
		if (dependency.getLag() != command.expectedLag() || dependency.getDependencyType() != command.expectedType())
			return TaskCommandResult.of(TaskCommandResult.Status.CONFLICT);
		if (command.expectedLag() == command.proposedLag() && command.expectedType() == command.proposedType())
			return TaskCommandResult.of(TaskCommandResult.Status.NO_OP);
		boolean dependencyDirty = dependency.isDirty();
		boolean projectDirty = project.isDirty();
		ProjectScheduleBackup scheduleBackup = captureSchedule(project);
		ModelTransaction<Dependency> transaction = ModelTransaction.<Dependency>builder()
				.captureRollback(() -> () -> {
					DependencyService.getInstance().setFields(dependency, command.expectedLag(),
							command.expectedType(), this, false);
					restoreSchedule(project, scheduleBackup);
					dependency.setDirty(dependencyDirty);
					project.setDirty(projectDirty);
				})
				.apply(() -> {
					DependencyService.getInstance().setFields(dependency, command.proposedLag(),
							command.proposedType(), this, false);
					DependencyService.getInstance().update(dependency, this);
					return ModelTransaction.Mutation.changed(dependency);
				})
				.invariant(() -> dependency.getLag() == command.proposedLag()
						&& dependency.getDependencyType() == command.proposedType())
				.commitUndo(value -> {
					if (project.getUndoController() != null)
						project.getUndoController().commitEdit(new DependencySetFieldsEdit(value,
								command.expectedLag(), command.expectedType(), this));
				})
				.changes(value -> new DomainChangeSet.Draft(UUID.randomUUID(), DomainChangeSet.Origin.COMMAND,
						Set.of(command.predecessor(), command.successor()), Set.of(),
						DomainChangeSet.TopologyImpact.NONE, true, true))
				.onRecoveryFailure(failure -> project.setReadOnly(true))
				.build();
		return map(transaction.execute(project.getDomainChangeJournal()));
	}

	private TaskCommandResult createDependencyLocked(TaskDependencyCommand command) {
		if (project.getDomainChangeJournal().revision() != command.expectedDomainRevision())
			return TaskCommandResult.of(TaskCommandResult.Status.CONFLICT);
		Task predecessor = resolve(command.predecessor());
		Task successor = resolve(command.successor());
		if (predecessor == null || successor == null)
			return TaskCommandResult.of(TaskCommandResult.Status.NOT_FOUND);
		if (predecessor.isReadOnly() || successor.isReadOnly() || project.isReadOnly())
			return TaskCommandResult.of(TaskCommandResult.Status.READ_ONLY);
		TaskAuthorizationPort.Decision predecessorDecision = authorization.authorize(
				command.predecessor(), TaskCommandType.CREATE_DEPENDENCY);
		TaskAuthorizationPort.Decision successorDecision = authorization.authorize(
				command.successor(), TaskCommandType.CREATE_DEPENDENCY);
		if (predecessorDecision == TaskAuthorizationPort.Decision.LOCK_DENIED
				|| successorDecision == TaskAuthorizationPort.Decision.LOCK_DENIED)
			return TaskCommandResult.of(TaskCommandResult.Status.LOCK_DENIED);
		if (predecessorDecision == TaskAuthorizationPort.Decision.READ_ONLY
				|| successorDecision == TaskAuthorizationPort.Decision.READ_ONLY)
			return TaskCommandResult.of(TaskCommandResult.Status.READ_ONLY);
		Dependency existing = findDependency(predecessor, successor);
		if (existing != null) {
			return existing.getDependencyType() == command.dependencyType() && existing.getLag() == command.lag()
					? TaskCommandResult.of(TaskCommandResult.Status.NO_OP)
					: TaskCommandResult.of(TaskCommandResult.Status.CONFLICT);
		}
		boolean projectDirty = project.isDirty();
		boolean predecessorDirty = predecessor.isDirty();
		boolean successorDirty = successor.isDirty();
		ProjectScheduleBackup scheduleBackup = captureSchedule(project);
		Dependency[] created = new Dependency[1];
		ModelTransaction<Dependency> transaction = ModelTransaction.<Dependency>builder()
				.captureRollback(() -> () -> {
					if (created[0] != null)
						DependencyService.getInstance().remove(created[0], TaskCommandGateway.this, false);
					restoreSchedule(project, scheduleBackup);
					predecessor.setDirty(predecessorDirty);
					successor.setDirty(successorDirty);
					project.setDirty(projectDirty);
				})
				.apply(() -> {
					created[0] = DependencyService.getInstance().newDependency(
							(HasDependencies) predecessor, (HasDependencies) successor, command.dependencyType(),
							command.lag(), this, false);
					return ModelTransaction.Mutation.changed(created[0]);
				})
				.invariant(() -> findDependency(predecessor, successor) != null)
				.commitUndo(dependency -> {
					if (project.getUndoController() != null)
						project.getUndoController().commitEdit(new DependencyCreationEdit(dependency, this));
				})
				.changes(dependency -> new DomainChangeSet.Draft(UUID.randomUUID(), DomainChangeSet.Origin.COMMAND,
						Set.of(command.predecessor(), command.successor()), Set.of(), DomainChangeSet.TopologyImpact.NONE,
						true, true))
				.onRecoveryFailure(failure -> project.setReadOnly(true))
				.build();
		return map(transaction.execute(project.getDomainChangeJournal()));
	}

	public TaskCommandResult moveHierarchy(TaskHierarchyMoveCommand command) {
		return project.getDomainChangeJournal().write(() -> moveHierarchyLocked(command));
	}

	private TaskCommandResult moveHierarchyLocked(TaskHierarchyMoveCommand command) {
		if (project.getDomainChangeJournal().revision() != command.expectedDomainRevision())
			return TaskCommandResult.of(TaskCommandResult.Status.CONFLICT);
		List<Task> tasks = new ArrayList<>();
		List<Node> nodes = new ArrayList<>();
		for (ProjectTaskKey key : command.tasks()) {
			Task task = resolve(key);
			Node node = task == null ? null : project.getTaskModel().search(task);
			if (task == null || node == null)
				return TaskCommandResult.of(TaskCommandResult.Status.NOT_FOUND);
			if (project.isReadOnly() || task.isReadOnly())
				return TaskCommandResult.of(TaskCommandResult.Status.READ_ONLY);
			TaskAuthorizationPort.Decision decision = authorization.authorize(key, TaskCommandType.MOVE);
			if (decision == TaskAuthorizationPort.Decision.LOCK_DENIED)
				return TaskCommandResult.of(TaskCommandResult.Status.LOCK_DENIED);
			if (decision == TaskAuthorizationPort.Decision.READ_ONLY)
				return TaskCommandResult.of(TaskCommandResult.Status.READ_ONLY);
			tasks.add(task);
			nodes.add(node);
		}
		NodeModel model = project.getTaskModel();
		if (!isContiguousSiblingBlock(nodes)) return TaskCommandResult.of(TaskCommandResult.Status.INVALID);
		if (!model.canMoveSelectedNodes(nodes, command.direction()))
			return TaskCommandResult.of(TaskCommandResult.Status.NO_OP);
		Node parent = (Node) nodes.get(0).getParent();
		int beforeIndex = parent.getIndex(nodes.get(0));
		int targetIndex = command.direction() < 0 ? beforeIndex - 1 : beforeIndex + 1;
		boolean projectDirty = project.isDirty();
		boolean[] dirty = new boolean[tasks.size()];
		for (int index = 0; index < tasks.size(); index++) dirty[index] = tasks.get(index).isDirty();
		ModelTransaction<List<Node>> transaction = ModelTransaction.<List<Node>>builder()
				.captureRollback(() -> () -> {
					model.relocate(nodes, parent, beforeIndex, NodeModel.EVENT);
					for (int index = 0; index < tasks.size(); index++) tasks.get(index).setDirty(dirty[index]);
					project.setDirty(projectDirty);
				})
				.apply(() -> model.relocate(nodes, parent, targetIndex, NodeModel.EVENT)
						? ModelTransaction.Mutation.changed(List.copyOf(nodes))
						: ModelTransaction.Mutation.noOp(List.copyOf(nodes)))
				.invariant(() -> nodes.get(0).getParent() == parent && parent.getIndex(nodes.get(0)) != beforeIndex)
				.commitUndo(moved -> {
					if (project.getUndoController() != null)
						project.getUndoController().commitEdit(new NodeRelocationEdit(
								model, moved, parent, beforeIndex, parent, parent.getIndex(moved.get(0))));
				})
				.changes(moved -> new DomainChangeSet.Draft(UUID.randomUUID(), DomainChangeSet.Origin.COMMAND,
						Set.copyOf(command.tasks()), Set.of(), DomainChangeSet.TopologyImpact.ROWS, false, false))
				.onRecoveryFailure(failure -> project.setReadOnly(true))
				.build();
		return map(transaction.execute(project.getDomainChangeJournal()));
	}

	public TaskCommandResult relocateHierarchy(TaskHierarchyRelocateCommand command) {
		return project.getDomainChangeJournal().write(() -> relocateHierarchyLocked(command));
	}

	private TaskCommandResult relocateHierarchyLocked(TaskHierarchyRelocateCommand command) {
		if (project.getDomainChangeJournal().revision() != command.expectedDomainRevision())
			return TaskCommandResult.of(TaskCommandResult.Status.CONFLICT);
		List<Task> tasks = new ArrayList<>();
		List<Node> nodes = new ArrayList<>();
		for (ProjectTaskKey key : command.tasks()) {
			Task task = resolve(key);
			Node node = task == null ? null : project.getTaskModel().search(task);
			if (node == null) return TaskCommandResult.of(TaskCommandResult.Status.NOT_FOUND);
			TaskCommandResult rejected = authorizeTask(task, key, TaskCommandType.MOVE);
			if (rejected != null) return rejected;
			tasks.add(task); nodes.add(node);
		}
		Task parentTask = command.parent() == null ? null : resolve(command.parent());
		if (command.parent() != null && parentTask == null) return TaskCommandResult.of(TaskCommandResult.Status.NOT_FOUND);
		NodeModel model = project.getTaskModel();
		if (!isContiguousSiblingBlock(nodes)) return TaskCommandResult.of(TaskCommandResult.Status.INVALID);
		Node destination = parentTask == null ? (Node) model.getRoot() : model.search(parentTask);
		if (!model.canRelocate(nodes, destination, command.position())) return TaskCommandResult.of(TaskCommandResult.Status.NO_OP);
		Node sourceParent = (Node) nodes.get(0).getParent();
		int beforeIndex = sourceParent.getIndex(nodes.get(0));
		boolean projectDirty = project.isDirty();
		boolean[] dirty = new boolean[tasks.size()];
		for (int index = 0; index < tasks.size(); index++) dirty[index] = tasks.get(index).isDirty();
		ModelTransaction<List<Node>> transaction = ModelTransaction.<List<Node>>builder()
				.captureRollback(() -> () -> {
					model.relocate(nodes, sourceParent, beforeIndex, NodeModel.EVENT);
					for (int index = 0; index < tasks.size(); index++) tasks.get(index).setDirty(dirty[index]);
					project.setDirty(projectDirty);
				})
				.apply(() -> model.relocate(nodes, destination, command.position(), NodeModel.EVENT)
						? ModelTransaction.Mutation.changed(List.copyOf(nodes)) : ModelTransaction.Mutation.noOp(List.copyOf(nodes)))
				.invariant(() -> nodes.get(0).getParent() == destination)
				.commitUndo(moved -> {
					if (project.getUndoController() != null)
						project.getUndoController().commitEdit(new NodeRelocationEdit(model, moved,
								sourceParent, beforeIndex, destination, destination.getIndex(moved.get(0))));
				})
				.changes(moved -> new DomainChangeSet.Draft(UUID.randomUUID(), DomainChangeSet.Origin.COMMAND,
						Set.copyOf(command.tasks()), Set.of(), DomainChangeSet.TopologyImpact.ROWS, false, false))
				.onRecoveryFailure(failure -> project.setReadOnly(true)).build();
		return map(transaction.execute(project.getDomainChangeJournal()));
	}

	public TaskCommandResult indentHierarchy(TaskHierarchyIndentCommand command) {
		return project.getDomainChangeJournal().write(() -> indentHierarchyLocked(command));
	}

	private TaskCommandResult indentHierarchyLocked(TaskHierarchyIndentCommand command) {
		if (project.getDomainChangeJournal().revision() != command.expectedDomainRevision())
			return TaskCommandResult.of(TaskCommandResult.Status.CONFLICT);
		List<Task> tasks = new ArrayList<>();
		List<Node> nodes = new ArrayList<>();
		for (ProjectTaskKey key : command.tasks()) {
			Task task = resolve(key);
			Node node = task == null ? null : project.getTaskModel().search(task);
			if (node == null) return TaskCommandResult.of(TaskCommandResult.Status.NOT_FOUND);
			TaskCommandResult rejected = authorizeTask(task, key, TaskCommandType.INDENT);
			if (rejected != null) return rejected;
			tasks.add(task);
			nodes.add(node);
		}
		NodeModel model = project.getTaskModel();
		List<NodeIndentEdit.Position> before = hierarchyPositions(model);
		boolean projectDirty = project.isDirty();
		boolean[] dirty = new boolean[tasks.size()];
		for (int index = 0; index < tasks.size(); index++) dirty[index] = tasks.get(index).isDirty();
		@SuppressWarnings("unchecked")
		List<Node>[] changedNodes = new List[] { List.of() };
		@SuppressWarnings("unchecked")
		List<NodeIndentEdit.Position>[] after = new List[] { List.of() };
		ModelTransaction<List<Node>> transaction = ModelTransaction.<List<Node>>builder()
				.captureRollback(() -> () -> {
					restorePositions(model, before);
					for (int index = 0; index < tasks.size(); index++) tasks.get(index).setDirty(dirty[index]);
					project.setDirty(projectDirty);
				})
				.apply(() -> {
					model.getHierarchy().indent(nodes, command.deltaLevel(), model, NodeModel.EVENT);
					after[0] = hierarchyPositions(model);
					changedNodes[0] = changedNodes(before, after[0]);
					return changedNodes[0].isEmpty() ? ModelTransaction.Mutation.noOp(List.copyOf(nodes))
							: ModelTransaction.Mutation.changed(List.copyOf(changedNodes[0]));
				})
				.invariant(() -> !changedNodes[0].isEmpty())
				.commitUndo(changed -> {
					if (project.getUndoController() != null)
						project.getUndoController().commitEdit(new NodeIndentEdit(model, changed,
								command.deltaLevel(), selectPositions(before, changed), selectPositions(after[0], changed)));
				})
				.changes(changed -> new DomainChangeSet.Draft(UUID.randomUUID(), DomainChangeSet.Origin.COMMAND,
						Set.copyOf(command.tasks()), Set.of(), DomainChangeSet.TopologyImpact.ROWS, false, false))
				.onRecoveryFailure(failure -> project.setReadOnly(true)).build();
		return map(transaction.execute(project.getDomainChangeJournal()));
	}

	public TaskCommandResult dragSchedule(TaskScheduleDragCommand command) {
		return project.getDomainChangeJournal().write(() -> dragScheduleLocked(command));
	}

	private TaskCommandResult dragScheduleLocked(TaskScheduleDragCommand command) {
		if (project.getDomainChangeJournal().revision() != command.expectedDomainRevision())
			return TaskCommandResult.of(TaskCommandResult.Status.CONFLICT);
		Task task = resolve(command.task());
		if (task == null) return TaskCommandResult.of(TaskCommandResult.Status.NOT_FOUND);
		if (project.isReadOnly() || task.isReadOnly()) return TaskCommandResult.of(TaskCommandResult.Status.READ_ONLY);
		TaskAuthorizationPort.Decision decision = authorization.authorize(command.task(), TaskCommandType.DRAG_SCHEDULE);
		if (decision == TaskAuthorizationPort.Decision.LOCK_DENIED) return TaskCommandResult.of(TaskCommandResult.Status.LOCK_DENIED);
		if (decision == TaskAuthorizationPort.Decision.READ_ONLY) return TaskCommandResult.of(TaskCommandResult.Status.READ_ONLY);
		Schedule schedule = task;
		List<ScheduleInterval> intervals = new ArrayList<>();
		ScheduleService.getInstance().consumeIntervals(schedule,
				interval -> intervals.add(new ScheduleInterval(interval.getStart(), interval.getEnd())));
		if (command.intervalIndex() >= intervals.size()) return TaskCommandResult.of(TaskCommandResult.Status.CONFLICT);
		ScheduleInterval expected = intervals.get(command.intervalIndex());
		if (expected.getStart() != command.expectedStart() || expected.getEnd() != command.expectedEnd()
				|| task.getConstraintType() != command.expectedConstraintType()
				|| task.getConstraintDate() != command.expectedConstraintDate())
			return TaskCommandResult.of(TaskCommandResult.Status.CONFLICT);
		if (command.expectedStart() == command.proposedStart() && command.expectedEnd() == command.proposedEnd())
			return TaskCommandResult.of(TaskCommandResult.Status.NO_OP);
		Object backup = schedule.backupDetail();
		ProjectScheduleBackup projectScheduleBackup = captureSchedule(project);
		boolean projectDirty = project.isDirty();
		boolean taskDirty = task.isDirty();
		ScheduleInterval liveInterval = findInterval(schedule, command.intervalIndex());
		if (liveInterval == null) return TaskCommandResult.of(TaskCommandResult.Status.CONFLICT);
		ModelTransaction<ScheduleInterval> transaction = ModelTransaction.<ScheduleInterval>builder()
				.captureRollback(() -> () -> {
					restoreSchedule(project, projectScheduleBackup);
					task.setScheduleConstraint(command.expectedConstraintType(), command.expectedConstraintDate());
					task.setDirty(taskDirty);
					project.setDirty(projectDirty);
				})
				.apply(() -> {
					if (command.updateConstraint())
						task.setScheduleConstraint(command.proposedConstraintType(), command.proposedStart());
					boolean changed = ScheduleService.getInstance().setInterval(this, schedule,
							command.proposedStart(), command.proposedEnd(), liveInterval, null);
					if (changed && command.updateConstraint())
						task.setScheduleConstraint(command.proposedConstraintType(), task.getStart());
					return changed ? ModelTransaction.Mutation.changed(liveInterval)
							: ModelTransaction.Mutation.noOp(liveInterval);
				})
				.invariant(() -> task.getStart() <= task.getEnd())
				.commitUndo(interval -> {
					if (project.getUndoController() == null) return;
					CompoundEdit compound = new CompoundEdit();
					compound.addEdit(new ScheduleEdit(schedule, backup, command.proposedStart(),
							command.proposedEnd(), interval, false, this));
					if (command.updateConstraint())
						compound.addEdit(new TaskConstraintEdit(task, command.expectedConstraintType(),
								command.expectedConstraintDate(), command.proposedConstraintType(), task.getStart(), this));
					compound.end();
					project.getUndoController().commitEdit(compound);
				})
				.changes(interval -> new DomainChangeSet.Draft(UUID.randomUUID(), DomainChangeSet.Origin.COMMAND,
						Set.of(command.task()), Set.of("Field.start", "Field.finish", "Field.constraintType",
								"Field.constraintDate"), DomainChangeSet.TopologyImpact.NONE, true, false))
				.onRecoveryFailure(failure -> project.setReadOnly(true))
				.build();
		return map(transaction.execute(project.getDomainChangeJournal()));
	}

	public TaskCommandResult paste(TaskPasteCommand command) {
		return project.getDomainChangeJournal().write(() -> pasteLocked(command));
	}

	private TaskCommandResult pasteLocked(TaskPasteCommand command) {
		if (project.getDomainChangeJournal().revision() != command.expectedDomainRevision())
			return TaskCommandResult.of(TaskCommandResult.Status.CONFLICT);
		Task parentTask = command.parent() == null ? null : resolve(command.parent());
		if (command.parent() != null && parentTask == null)
			return TaskCommandResult.of(TaskCommandResult.Status.NOT_FOUND);
		if (project.isReadOnly() || parentTask != null && parentTask.isReadOnly())
			return TaskCommandResult.of(TaskCommandResult.Status.READ_ONLY);
		if (parentTask != null) {
			TaskAuthorizationPort.Decision decision = authorization.authorize(command.parent(), TaskCommandType.PASTE);
			if (decision == TaskAuthorizationPort.Decision.LOCK_DENIED) return TaskCommandResult.of(TaskCommandResult.Status.LOCK_DENIED);
			if (decision == TaskAuthorizationPort.Decision.READ_ONLY) return TaskCommandResult.of(TaskCommandResult.Status.READ_ONLY);
		}
		NodeModel model = project.getTaskModel();
		Node parent = parentTask == null ? (Node) model.getRoot() : model.search(parentTask);
		if (parent == null || command.position() > parent.getChildCount())
			return TaskCommandResult.of(TaskCommandResult.Status.CONFLICT);
		List<Node> nodes = new ArrayList<>(command.detachedNodes());
		for (Node node : nodes)
			if (node == null || node.getParent() != null) return TaskCommandResult.of(TaskCommandResult.Status.INVALID);
		boolean projectDirty = project.isDirty();
		ModelTransaction<List<Node>> transaction = ModelTransaction.<List<Node>>builder()
				.captureRollback(() -> () -> {
					List<Node> attached = nodes.stream().filter(node -> node.getParent() != null).toList();
					if (!attached.isEmpty()) model.remove(attached, NodeModel.EVENT);
					project.setDirty(projectDirty);
				})
				.apply(() -> {
					model.paste(parent, nodes, command.position(), NodeModel.EVENT);
					return ModelTransaction.Mutation.changed(List.copyOf(nodes));
				})
				.invariant(() -> nodes.stream().allMatch(node -> node.getParent() == parent))
				.commitUndo(pasted -> {
					if (project.getUndoController() != null)
						project.getUndoController().commitEdit(
								new NodePasteEdit(model, parent, pasted, command.position()));
				})
				.changes(pasted -> new DomainChangeSet.Draft(UUID.randomUUID(), DomainChangeSet.Origin.COMMAND,
						pasted.stream().map(Node::getImpl).filter(Task.class::isInstance).map(Task.class::cast)
								.map(ProjectTaskKey::from).flatMap(java.util.Optional::stream).collect(java.util.stream.Collectors.toSet()),
						Set.of(), DomainChangeSet.TopologyImpact.FULL_PROJECTION_INVALIDATION, true, true))
				.onRecoveryFailure(failure -> project.setReadOnly(true))
				.build();
		return map(transaction.execute(project.getDomainChangeJournal()));
	}

	public TaskCommandResult updateProgress(TaskProgressCommand command) {
		return project.getDomainChangeJournal().write(() -> updateProgressLocked(command));
	}

	private TaskCommandResult updateProgressLocked(TaskProgressCommand command) {
		if (project.getDomainChangeJournal().revision() != command.expectedDomainRevision())
			return TaskCommandResult.of(TaskCommandResult.Status.CONFLICT);
		Task task = resolve(command.task());
		if (task == null) return TaskCommandResult.of(TaskCommandResult.Status.NOT_FOUND);
		TaskCommandResult rejected = authorizeTask(task, command.task(), TaskCommandType.DRAG_SCHEDULE);
		if (rejected != null) return rejected;
		Schedule schedule = task;
		long current = ScheduleService.getInstance().getCompleted(schedule);
		if (current != command.expectedCompleted()) return TaskCommandResult.of(TaskCommandResult.Status.CONFLICT);
		long proposed = Math.max(schedule.getStart(), Math.min(command.proposedCompleted(), schedule.getEnd()));
		if (current == proposed) return TaskCommandResult.of(TaskCommandResult.Status.NO_OP);
		Object backup = schedule.backupDetail();
		ProjectScheduleBackup projectScheduleBackup = captureSchedule(project);
		Field field = ScheduleService.getCompletedField();
		Object oldValue = field.getValue(schedule);
		if (oldValue == null) oldValue = Long.valueOf(schedule.getActualStart());
		Object committedOldValue = oldValue;
		boolean projectDirty = project.isDirty();
		boolean taskDirty = task.isDirty();
		ModelTransaction<Long> transaction = ModelTransaction.<Long>builder()
				.captureRollback(() -> () -> { restoreSchedule(project, projectScheduleBackup); task.setDirty(taskDirty); project.setDirty(projectDirty); })
				.apply(() -> {
					boolean changed = ScheduleService.getInstance().setCompleted(this, schedule, proposed, null);
					long applied = ScheduleService.getInstance().getCompleted(schedule);
					return changed && applied != current ? ModelTransaction.Mutation.changed(applied) : ModelTransaction.Mutation.noOp(applied);
				})
				.invariant(() -> schedule.getStart() <= schedule.getEnd())
				.commitUndo(value -> {
					if (project.getUndoController() != null)
						project.getUndoController().commitEdit(
								new FieldEdit(field, schedule, Long.valueOf(value), committedOldValue, this, null));
				})
				.changes(value -> new DomainChangeSet.Draft(UUID.randomUUID(), DomainChangeSet.Origin.COMMAND,
						Set.of(command.task()), Set.of("Field.stop"), DomainChangeSet.TopologyImpact.NONE, true, false))
				.onRecoveryFailure(failure -> project.setReadOnly(true)).build();
		return map(transaction.execute(project.getDomainChangeJournal()));
	}

	public TaskCommandResult split(TaskSplitCommand command) {
		return project.getDomainChangeJournal().write(() -> splitLocked(command));
	}

	private TaskCommandResult splitLocked(TaskSplitCommand command) {
		if (project.getDomainChangeJournal().revision() != command.expectedDomainRevision())
			return TaskCommandResult.of(TaskCommandResult.Status.CONFLICT);
		Task task = resolve(command.task());
		if (task == null) return TaskCommandResult.of(TaskCommandResult.Status.NOT_FOUND);
		TaskCommandResult rejected = authorizeTask(task, command.task(), TaskCommandType.DRAG_SCHEDULE);
		if (rejected != null) return rejected;
		Schedule schedule = task;
		if (schedule.getStart() != command.expectedStart() || schedule.getEnd() != command.expectedEnd())
			return TaskCommandResult.of(TaskCommandResult.Status.CONFLICT);
		List<String> beforeIntervals = intervalSignature(schedule);
		Object backup = schedule.backupDetail();
		ProjectScheduleBackup projectScheduleBackup = captureSchedule(project);
		boolean projectDirty = project.isDirty();
		boolean taskDirty = task.isDirty();
		ModelTransaction<Long> transaction = ModelTransaction.<Long>builder()
				.captureRollback(() -> () -> { restoreSchedule(project, projectScheduleBackup); task.setDirty(taskDirty); project.setDirty(projectDirty); })
				.apply(() -> {
					ScheduleService.getInstance().split(this, schedule, command.splitAt(), command.splitAt(), null);
					return beforeIntervals.equals(intervalSignature(schedule))
							? ModelTransaction.Mutation.noOp(command.splitAt())
							: ModelTransaction.Mutation.changed(command.splitAt());
				})
				.commitUndo(value -> {
					if (project.getUndoController() != null)
						project.getUndoController().commitEdit(new SplitEdit(schedule, backup, value, value, this));
				})
				.changes(value -> new DomainChangeSet.Draft(UUID.randomUUID(), DomainChangeSet.Origin.COMMAND,
						Set.of(command.task()), Set.of("Field.start", "Field.finish"), DomainChangeSet.TopologyImpact.NONE, true, false))
				.onRecoveryFailure(failure -> project.setReadOnly(true)).build();
		return map(transaction.execute(project.getDomainChangeJournal()));
	}

	private TaskCommandResult authorizeTask(Task task, ProjectTaskKey key, TaskCommandType type) {
		if (project.isReadOnly() || task.isReadOnly()) return TaskCommandResult.of(TaskCommandResult.Status.READ_ONLY);
		TaskAuthorizationPort.Decision decision = authorization.authorize(key, type);
		if (decision == TaskAuthorizationPort.Decision.LOCK_DENIED) return TaskCommandResult.of(TaskCommandResult.Status.LOCK_DENIED);
		if (decision == TaskAuthorizationPort.Decision.READ_ONLY) return TaskCommandResult.of(TaskCommandResult.Status.READ_ONLY);
		return null;
	}

	private static List<String> intervalSignature(Schedule schedule) {
		List<String> result = new ArrayList<>();
		ScheduleService.getInstance().consumeIntervals(schedule,
				interval -> result.add(interval.getStart() + ":" + interval.getEnd()));
		return List.copyOf(result);
	}

	private static boolean isContiguousSiblingBlock(List<Node> nodes) {
		if (nodes == null || nodes.isEmpty() || !(nodes.get(0).getParent() instanceof Node parent)) return false;
		List<Integer> indexes = new ArrayList<>(nodes.size());
		for (Node node : nodes) {
			if (node.getParent() != parent) return false;
			indexes.add(Integer.valueOf(parent.getIndex(node)));
		}
		indexes.sort(Integer::compareTo);
		for (int index = 1; index < indexes.size(); index++)
			if (indexes.get(index).intValue() != indexes.get(index - 1).intValue() + 1) return false;
		return true;
	}

	private record ProjectScheduleBackup(Object projectDetail, Map<Task, Object> taskDetails) { }

	private static ProjectScheduleBackup captureSchedule(Project project) {
		Map<Task, Object> details = new IdentityHashMap<>();
		for (Task task : project.getTasks()) details.put(task, task.backupDetail());
		return new ProjectScheduleBackup(project.backupDetail(), Collections.unmodifiableMap(details));
	}

	private static void restoreSchedule(Project project, ProjectScheduleBackup backup) {
		if (backup == null) return;
		for (Map.Entry<Task, Object> entry : backup.taskDetails().entrySet())
			entry.getKey().restoreDetail(TaskCommandGateway.class, entry.getValue(), false);
		project.restoreDetail(TaskCommandGateway.class, backup.projectDetail(), false);
	}

	private static List<NodeIndentEdit.Position> hierarchyPositions(NodeModel model) {
		List<NodeIndentEdit.Position> positions = new ArrayList<>();
		for (var iterator = model.getHierarchy().iterator(); iterator.hasNext();) {
			Object value = iterator.next();
			if (!(value instanceof Node node) || !(node.getParent() instanceof Node parent)) continue;
			positions.add(new NodeIndentEdit.Position(parent, node, parent.getIndex(node)));
		}
		return List.copyOf(positions);
	}

	private static List<Node> changedNodes(List<NodeIndentEdit.Position> before,
			List<NodeIndentEdit.Position> after) {
		var beforeByNode = new IdentityHashMap<Node, NodeIndentEdit.Position>();
		for (NodeIndentEdit.Position position : before) beforeByNode.put(position.node, position);
		List<Node> changed = new ArrayList<>();
		for (NodeIndentEdit.Position position : after) {
			NodeIndentEdit.Position original = beforeByNode.get(position.node);
			if (original != null && (original.parent != position.parent || original.index != position.index))
				changed.add(position.node);
		}
		return List.copyOf(changed);
	}

	private static List<NodeIndentEdit.Position> selectPositions(List<NodeIndentEdit.Position> positions,
			List<Node> nodes) {
		Set<Node> selected = Collections.newSetFromMap(new IdentityHashMap<>());
		selected.addAll(nodes);
		return positions.stream().filter(position -> selected.contains(position.node)).toList();
	}

	private static void restorePositions(NodeModel model, List<NodeIndentEdit.Position> positions) {
		for (NodeIndentEdit.Position position : positions) {
			if (position.node.getParent() == position.parent && position.parent.getIndex(position.node) == position.index)
				continue;
			model.getHierarchy().add(position.parent, List.of(position.node),
					Math.max(0, Math.min(position.index, position.parent.getChildCount())), NodeModel.EVENT);
		}
	}

	private static ScheduleInterval findInterval(Schedule schedule, int wanted) {
		List<ScheduleInterval> intervals = new ArrayList<>();
		ScheduleService.getInstance().consumeIntervals(schedule, intervals::add);
		return wanted < intervals.size() ? intervals.get(wanted) : null;
	}

	private Task resolve(ProjectTaskKey key) {
		return resolve(project, key, Collections.newSetFromMap(new IdentityHashMap<>()));
	}

	private static Dependency findDependency(Task predecessor, Task successor) {
		return (Dependency) predecessor.getSuccessorList().findRight(successor);
	}

	private static Task resolve(Project current, ProjectTaskKey key, Set<Project> visited) {
		if (current == null || !visited.add(current))
			return null;
		for (Task task : current.getTasks()) {
			if (task.getUniqueId() == key.taskUniqueId() && ProjectTaskKey.from(task).filter(key::equals).isPresent())
				return task;
			if (task instanceof SubProj subproject) {
				Task nested = resolve(subproject.getSubproject(), key, visited);
				if (nested != null)
					return nested;
			}
		}
		return null;
	}

	private static boolean isScheduleField(String fieldId) {
		return fieldId != null && (fieldId.contains("start") || fieldId.contains("finish")
				|| fieldId.contains("duration") || fieldId.contains("complete"));
	}

	private static TaskCommandResult map(ModelTransaction.Result<?> result) {
		TaskCommandResult.Status status = switch (result.status()) {
			case COMMITTED -> TaskCommandResult.Status.COMMITTED;
			case NO_OP -> TaskCommandResult.Status.NO_OP;
			case VALIDATION_FAILED -> TaskCommandResult.Status.INVALID;
			case AUTHORIZATION_FAILED -> TaskCommandResult.Status.LOCK_DENIED;
			case FAILED -> TaskCommandResult.Status.FAILED;
			case RECOVERY_REQUIRED -> TaskCommandResult.Status.RECOVERY_REQUIRED;
		};
		return new TaskCommandResult(status, result.changeSet(), result.failure());
	}
}
