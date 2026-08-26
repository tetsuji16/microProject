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

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.microproject.configuration.Configuration;
import com.microproject.association.InvalidAssociationException;
import com.microproject.field.Field;
import com.microproject.field.FieldContext;
import com.microproject.field.FieldParseException;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.ProjectTaskKey;
import com.microproject.pm.task.Task;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.HasDependencies;
import com.microproject.pm.scheduling.Schedule;
import com.microproject.pm.scheduling.ScheduleInterval;
import com.microproject.pm.scheduling.ScheduleService;
import com.microproject.pm.scheduling.ConstraintType;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.AssignmentService;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.task.NormalTask;
import com.microproject.transaction.DomainChangeSet;
import com.microproject.transaction.ModelTransaction;
import com.microproject.undo.ModelFieldEdit;
import com.microproject.undo.AtomicCompoundEdit;
import com.microproject.undo.DependencyCreationEdit;
import com.microproject.undo.DependencySetFieldsEdit;
import com.microproject.undo.DependencyDeletionEdit;
import com.microproject.undo.NodeRelocationEdit;
import com.microproject.undo.NodeIndentEdit;
import com.microproject.undo.ScheduleEdit;
import com.microproject.undo.TaskConstraintEdit;
import com.microproject.undo.NodePasteEdit;
import com.microproject.undo.FieldEdit;
import com.microproject.undo.SplitEdit;
import com.microproject.undo.UndoController;
import javax.swing.undo.UndoableEdit;
import javax.swing.undo.AbstractUndoableEdit;

/** Headless per-project use-case gateway. */
public final class TaskCommandGateway {
	private final Project project;
	private final TaskAuthorizationPort authorization;

	public TaskCommandGateway(Project project) {
		this(project, (keys, commandType) -> TaskAuthorizationPort.fixed(
				project != null && project.getCollaborationSession() == null
						? TaskAuthorizationPort.Decision.ALLOWED : TaskAuthorizationPort.Decision.LOCK_DENIED));
	}

	public TaskCommandGateway(Project project, TaskAuthorizationPort authorization) {
		this.project = Objects.requireNonNull(project, "project");
		this.authorization = Objects.requireNonNull(authorization, "authorization");
	}

	public TaskCommandResult editField(TaskFieldEditCommand command) {
		Task task = resolve(command.taskKey());
		Project owner = task == null ? null : task.getOwningProject();
		if (owner == null) return TaskCommandResult.of(TaskCommandResult.Status.NOT_FOUND);
		return owner.getDomainChangeJournal().write(() -> editFieldsLocked(
				new TaskFieldBatchEditCommand(List.of(command), owner.getDomainChangeJournal().revision()), owner));
	}

	public TaskCommandResult editFields(TaskFieldBatchEditCommand command) {
		if (command == null || command.edits().isEmpty()) return TaskCommandResult.of(TaskCommandResult.Status.NO_OP);
		Task first = resolve(command.edits().get(0).taskKey());
		Project owner = first == null ? null : first.getOwningProject();
		if (owner == null) return TaskCommandResult.of(TaskCommandResult.Status.NOT_FOUND);
		return owner.getDomainChangeJournal().write(() -> editFieldsLocked(command, owner));
	}

	private TaskCommandResult editFieldsLocked(TaskFieldBatchEditCommand command, Project ownerProject) {
		if (ownerProject.getDomainChangeJournal().revision() != command.expectedDomainRevision())
			return TaskCommandResult.of(TaskCommandResult.Status.CONFLICT);
		record Target(TaskFieldEditCommand command, Task task, NodeModel model, Node node,
				Field field, FieldContext context, Object original, boolean taskDirty) { }
		List<Target> targets = new ArrayList<>(command.edits().size());
		Set<ProjectTaskKey> affectedTasks = new java.util.LinkedHashSet<>();
		Set<String> affectedFields = new java.util.LinkedHashSet<>();
		boolean scheduleAffected = false;
		for (TaskFieldEditCommand edit : command.edits()) {
			Task task = resolve(edit.taskKey());
			Project owner = task == null ? null : task.getOwningProject();
			if (task == null || owner == null) return TaskCommandResult.of(TaskCommandResult.Status.NOT_FOUND);
			if (owner != ownerProject) return TaskCommandResult.of(TaskCommandResult.Status.INVALID);
			if (ownerProject.isReadOnly() || task.isReadOnly()) return TaskCommandResult.of(TaskCommandResult.Status.READ_ONLY);
			Field field = Configuration.getFieldFromId(edit.fieldId());
			NodeModel model = owner.getTaskModel();
			Node node = model.search(task);
			FieldContext context = edit.fieldContext() == null ? new FieldContext() : edit.fieldContext();
			if (field == null || node == null) return TaskCommandResult.of(TaskCommandResult.Status.INVALID);
			Object original = field.getValue(node, model, context);
			if (!Objects.deepEquals(original, edit.expectedValue()))
				return TaskCommandResult.of(TaskCommandResult.Status.CONFLICT);
			if (field.isReadOnly(node, model, context))
				return TaskCommandResult.of(TaskCommandResult.Status.READ_ONLY);
			if (Objects.deepEquals(original, edit.proposedValue())) continue;
			targets.add(new Target(edit, task, model, node, field, context, original, task.isDirty()));
			affectedTasks.add(edit.taskKey());
			affectedFields.add(edit.fieldId());
			scheduleAffected |= isScheduleField(edit.fieldId());
		}
		if (targets.isEmpty()) return TaskCommandResult.of(TaskCommandResult.Status.NO_OP);
		AuthorizationGuard authorizationGuard = new AuthorizationGuard(affectedTasks, TaskCommandType.EDIT_FIELD);
		boolean projectDirty = ownerProject.isDirty();
		ProjectScheduleBackup scheduleBackup = scheduleAffected ? captureSchedule(ownerProject) : null;
		List<Object> appliedValues = new ArrayList<>(targets.size());
		final boolean scheduleCascade = scheduleAffected;
		ModelTransaction<List<Object>> transaction = TaskCommandGateway.<List<Object>>authorizedBuilder(ownerProject, authorizationGuard)
				.captureRollback(() -> () -> {
					for (int index = targets.size() - 1; index >= 0; index--) {
						Target target = targets.get(index);
						target.field().setValue(target.node(), target.model(), this, target.original(), target.context());
						target.task().setDirty(target.taskDirty());
					}
					if (scheduleBackup != null) restoreSchedule(ownerProject, scheduleBackup);
					ownerProject.setDirty(projectDirty);
				})
				.apply(() -> {
					boolean changed = false;
					appliedValues.clear();
					for (Target target : targets) {
						setProposed(target.field(), target.node(), target.model(), target.context(),
								target.command().proposedValue());
						Object applied = target.field().getValue(target.node(), target.model(), target.context());
						appliedValues.add(applied);
						changed |= !Objects.deepEquals(target.original(), applied);
					}
					return changed ? ModelTransaction.Mutation.changed(List.copyOf(appliedValues))
							: ModelTransaction.Mutation.noOp(List.copyOf(appliedValues));
				})
				.invariant(() -> {
					for (int index = 0; index < targets.size(); index++)
						if (!Objects.deepEquals(appliedValues.get(index),
								targets.get(index).field().getValue(targets.get(index).node(),
										targets.get(index).model(), targets.get(index).context()))) return false;
					return true;
				})
				.commitUndo(values -> {
					if (ownerProject.getUndoController() == null) return;
					AtomicCompoundEdit compound = new AtomicCompoundEdit();
					for (int index = 0; index < targets.size(); index++) {
						Target target = targets.get(index);
						compound.addEdit(new ModelFieldEdit(target.model(), target.field(), target.node(), this,
								values.get(index), target.original(), target.context()));
					}
					compound.end();
					ownerProject.getUndoController().commitEdit(compound);
				})
				.changes(values -> new DomainChangeSet.Draft(UUID.randomUUID(), DomainChangeSet.Origin.COMMAND,
						Set.copyOf(affectedTasks), Set.copyOf(affectedFields), DomainChangeSet.TopologyImpact.NONE,
						scheduleCascade, false))
				.build();
		return executeAuthorized(transaction, authorizationGuard, ownerProject);
	}

	public TaskCommandResult createDependency(TaskDependencyCommand command) {
		return project.getDomainChangeJournal().write(() -> createDependencyLocked(command));
	}

	public TaskCommandResult updateDependency(TaskDependencyUpdateCommand command) {
		return project.getDomainChangeJournal().write(() -> updateDependencyLocked(command));
	}

	public TaskCommandResult deleteDependency(TaskDependencyDeleteCommand command) {
		return project.getDomainChangeJournal().write(() -> deleteDependencyLocked(command));
	}

	public TaskCommandResult changeDependencies(TaskDependencyBatchCommand command) {
		return project.getDomainChangeJournal().write(() -> changeDependenciesLocked(command));
	}

	private TaskCommandResult changeDependenciesLocked(TaskDependencyBatchCommand command) {
		if (project.getDomainChangeJournal().revision() != command.expectedDomainRevision())
			return TaskCommandResult.of(TaskCommandResult.Status.CONFLICT);
		if (command.tasks().size() < (command.operation() == TaskDependencyBatchCommand.Operation.LINK ? 2 : 1))
			return TaskCommandResult.of(TaskCommandResult.Status.NO_OP);
		List<Task> tasks = new ArrayList<>(command.tasks().size());
		for (ProjectTaskKey key : command.tasks()) {
			Task task = resolve(key);
			if (task == null) return TaskCommandResult.of(TaskCommandResult.Status.NOT_FOUND);
			if (task.isReadOnly() || project.isReadOnly()) return TaskCommandResult.of(TaskCommandResult.Status.READ_ONLY);
			tasks.add(task);
		}
		Set<ProjectTaskKey> affected = new java.util.LinkedHashSet<>(command.tasks());
		if (command.operation() == TaskDependencyBatchCommand.Operation.UNLINK) {
			for (Task task : tasks) {
				for (Object value : task.getPredecessorList())
					addTaskKey(affected, ((Dependency) value).getPredecessor());
				for (Object value : task.getSuccessorList())
					addTaskKey(affected, ((Dependency) value).getSuccessor());
			}
		}
		AuthorizationGuard guard = new AuthorizationGuard(Set.copyOf(affected), TaskCommandType.BATCH_DEPENDENCY);
		boolean projectDirty = project.isDirty();
		ProjectScheduleBackup scheduleBackup = captureSchedule(project);
		UndoableEdit[] captured = new UndoableEdit[1];
		ModelTransaction<List<Task>> transaction = TaskCommandGateway.<List<Task>>authorizedBuilder(project, guard)
				.captureRollback(() -> () -> {
					if (captured[0] != null && captured[0].canUndo()) captured[0].undo();
					restoreSchedule(project, scheduleBackup);
					project.setDirty(projectDirty);
				})
				.apply(() -> {
					UndoController.EditCapture capture = project.getUndoController().captureEdits();
					try {
						if (command.operation() == TaskDependencyBatchCommand.Operation.LINK)
							DependencyService.getInstance().connect(tasks, this, null);
						else
							DependencyService.getInstance().removeAnyDependencies(tasks, this);
					} finally {
						capture.close();
						captured[0] = capture.edit();
					}
					return captured[0] == null ? ModelTransaction.Mutation.noOp(List.copyOf(tasks))
							: ModelTransaction.Mutation.changed(List.copyOf(tasks));
				})
				.invariant(() -> command.operation() == TaskDependencyBatchCommand.Operation.LINK
						? adjacentDependenciesExist(tasks) : tasks.stream().noneMatch(TaskCommandGateway::hasDependencies))
				.commitUndo(value -> project.getUndoController().commitEdit(captured[0]))
				.changes(value -> new DomainChangeSet.Draft(UUID.randomUUID(), DomainChangeSet.Origin.COMMAND,
						Set.copyOf(affected), Set.of(), DomainChangeSet.TopologyImpact.NONE, true, true))
				.build();
		return executeAuthorized(transaction, guard, project);
	}

	private TaskCommandResult deleteDependencyLocked(TaskDependencyDeleteCommand command) {
		if (project.getDomainChangeJournal().revision() != command.expectedDomainRevision())
			return TaskCommandResult.of(TaskCommandResult.Status.CONFLICT);
		Task predecessor = resolve(command.predecessor());
		Task successor = resolve(command.successor());
		Dependency dependency = predecessor == null || successor == null ? null : findDependency(predecessor, successor);
		if (dependency == null) return TaskCommandResult.of(TaskCommandResult.Status.NOT_FOUND);
		if (project.isReadOnly() || predecessor.isReadOnly() || successor.isReadOnly())
			return TaskCommandResult.of(TaskCommandResult.Status.READ_ONLY);
		if (dependency.getLag() != command.expectedLag() || dependency.getDependencyType() != command.expectedType())
			return TaskCommandResult.of(TaskCommandResult.Status.CONFLICT);
		Set<ProjectTaskKey> keys = Set.of(command.predecessor(), command.successor());
		AuthorizationGuard guard = new AuthorizationGuard(keys, TaskCommandType.DELETE_DEPENDENCY);
		boolean projectDirty = project.isDirty();
		boolean dependencyDirty = dependency.isDirty();
		ProjectScheduleBackup scheduleBackup = captureSchedule(project);
		ModelTransaction<Dependency> transaction = TaskCommandGateway.<Dependency>authorizedBuilder(project, guard)
				.captureRollback(() -> () -> {
					if (findDependency(predecessor, successor) == null) DependencyService.getInstance().connect(dependency, this);
					restoreSchedule(project, scheduleBackup);
					dependency.setDirty(dependencyDirty);
					project.setDirty(projectDirty);
				})
				.apply(() -> {
					DependencyService.getInstance().remove(dependency, this, false);
					return ModelTransaction.Mutation.changed(dependency);
				})
				.invariant(() -> findDependency(predecessor, successor) == null)
				.commitUndo(value -> {
					if (project.getUndoController() != null)
						project.getUndoController().commitEdit(new DependencyDeletionEdit(value, this));
				})
				.changes(value -> new DomainChangeSet.Draft(UUID.randomUUID(), DomainChangeSet.Origin.COMMAND,
						keys, Set.of(), DomainChangeSet.TopologyImpact.NONE, true, true))
				.build();
		return executeAuthorized(transaction, guard, project);
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
		Set<ProjectTaskKey> authorizationKeys = Set.of(command.predecessor(), command.successor());
		AuthorizationGuard authorizationGuard = new AuthorizationGuard(authorizationKeys, TaskCommandType.UPDATE_DEPENDENCY);
		if (dependency.getLag() != command.expectedLag() || dependency.getDependencyType() != command.expectedType())
			return TaskCommandResult.of(TaskCommandResult.Status.CONFLICT);
		if (command.expectedLag() == command.proposedLag() && command.expectedType() == command.proposedType())
			return TaskCommandResult.of(TaskCommandResult.Status.NO_OP);
		boolean dependencyDirty = dependency.isDirty();
		boolean projectDirty = project.isDirty();
		ProjectScheduleBackup scheduleBackup = captureSchedule(project);
		ModelTransaction<Dependency> transaction = TaskCommandGateway.<Dependency>authorizedBuilder(project, authorizationGuard)
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
				.build();
		return executeAuthorized(transaction, authorizationGuard, project);
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
		Set<ProjectTaskKey> authorizationKeys = Set.of(command.predecessor(), command.successor());
		AuthorizationGuard authorizationGuard = new AuthorizationGuard(authorizationKeys, TaskCommandType.CREATE_DEPENDENCY);
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
		ModelTransaction<Dependency> transaction = TaskCommandGateway.<Dependency>authorizedBuilder(project, authorizationGuard)
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
				.build();
		return executeAuthorized(transaction, authorizationGuard, project);
	}

	public TaskCommandResult moveHierarchy(TaskHierarchyMoveCommand command) {
		return project.getDomainChangeJournal().write(() -> moveHierarchyLocked(command));
	}

	private TaskCommandResult moveHierarchyLocked(TaskHierarchyMoveCommand command) {
		if (project.getDomainChangeJournal().revision() != command.expectedDomainRevision())
			return TaskCommandResult.of(TaskCommandResult.Status.CONFLICT);
		List<Node> nodes = new ArrayList<>();
		for (ProjectTaskKey key : command.tasks()) {
			Task task = resolve(key);
			Node node = task == null ? null : project.getTaskModel().search(task);
			if (task == null || node == null)
				return TaskCommandResult.of(TaskCommandResult.Status.NOT_FOUND);
			if (project.isReadOnly() || task.isReadOnly())
				return TaskCommandResult.of(TaskCommandResult.Status.READ_ONLY);
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
		ProjectScheduleBackup scheduleBackup = captureSchedule(project);
		Set<ProjectTaskKey> affected = hierarchyKeys(nodes, parent);
		AuthorizationGuard authorizationGuard = new AuthorizationGuard(affected, TaskCommandType.MOVE);
		ModelTransaction<List<Node>> transaction = TaskCommandGateway.<List<Node>>authorizedBuilder(project, authorizationGuard)
				.captureRollback(() -> () -> {
					model.relocate(nodes, parent, beforeIndex, NodeModel.EVENT);
					restoreSchedule(project, scheduleBackup);
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
						affected, Set.of(), DomainChangeSet.TopologyImpact.ROWS, true, false))
				.build();
		return executeAuthorized(transaction, authorizationGuard, project);
	}

	public TaskCommandResult relocateHierarchy(TaskHierarchyRelocateCommand command) {
		return project.getDomainChangeJournal().write(() -> relocateHierarchyLocked(command));
	}

	private TaskCommandResult relocateHierarchyLocked(TaskHierarchyRelocateCommand command) {
		if (project.getDomainChangeJournal().revision() != command.expectedDomainRevision())
			return TaskCommandResult.of(TaskCommandResult.Status.CONFLICT);
		List<Node> nodes = new ArrayList<>();
		for (ProjectTaskKey key : command.tasks()) {
			Task task = resolve(key);
			Node node = task == null ? null : project.getTaskModel().search(task);
			if (node == null) return TaskCommandResult.of(TaskCommandResult.Status.NOT_FOUND);
			if (project.isReadOnly() || task.isReadOnly()) return TaskCommandResult.of(TaskCommandResult.Status.READ_ONLY);
			nodes.add(node);
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
		ProjectScheduleBackup scheduleBackup = captureSchedule(project);
		Set<ProjectTaskKey> affected = hierarchyKeys(nodes, sourceParent, destination);
		AuthorizationGuard authorizationGuard = new AuthorizationGuard(affected, TaskCommandType.MOVE);
		ModelTransaction<List<Node>> transaction = TaskCommandGateway.<List<Node>>authorizedBuilder(project, authorizationGuard)
				.captureRollback(() -> () -> {
					model.relocate(nodes, sourceParent, beforeIndex, NodeModel.EVENT);
					restoreSchedule(project, scheduleBackup);
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
						affected, Set.of(), DomainChangeSet.TopologyImpact.ROWS, true, false))
				.build();
		return executeAuthorized(transaction, authorizationGuard, project);
	}

	public TaskCommandResult indentHierarchy(TaskHierarchyIndentCommand command) {
		return project.getDomainChangeJournal().write(() -> indentHierarchyLocked(command));
	}

	private TaskCommandResult indentHierarchyLocked(TaskHierarchyIndentCommand command) {
		if (project.getDomainChangeJournal().revision() != command.expectedDomainRevision())
			return TaskCommandResult.of(TaskCommandResult.Status.CONFLICT);
		List<Node> nodes = new ArrayList<>();
		for (ProjectTaskKey key : command.tasks()) {
			Task task = resolve(key);
			Node node = task == null ? null : project.getTaskModel().search(task);
			if (node == null) return TaskCommandResult.of(TaskCommandResult.Status.NOT_FOUND);
			if (project.isReadOnly() || task.isReadOnly()) return TaskCommandResult.of(TaskCommandResult.Status.READ_ONLY);
			nodes.add(node);
		}
		NodeModel model = project.getTaskModel();
		List<NodeIndentEdit.Position> before = hierarchyPositions(model);
		boolean projectDirty = project.isDirty();
		ProjectScheduleBackup scheduleBackup = captureSchedule(project);
		Node parent = nodes.isEmpty() ? null : (Node) nodes.get(0).getParent();
		int firstIndex = parent == null ? -1 : parent.getIndex(nodes.get(0));
		Node previousSibling = firstIndex > 0 ? (Node) parent.getChildAt(firstIndex - 1) : null;
		Set<ProjectTaskKey> affected = hierarchyKeys(nodes, parent, previousSibling);
		@SuppressWarnings("unchecked")
		List<Node>[] changedNodes = new List[] { List.of() };
		@SuppressWarnings("unchecked")
		List<NodeIndentEdit.Position>[] after = new List[] { List.of() };
		AuthorizationGuard authorizationGuard = new AuthorizationGuard(affected, TaskCommandType.INDENT);
		ModelTransaction<List<Node>> transaction = TaskCommandGateway.<List<Node>>authorizedBuilder(project, authorizationGuard)
				.captureRollback(() -> () -> {
					restorePositions(model, before);
					restoreSchedule(project, scheduleBackup);
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
						affected, Set.of(), DomainChangeSet.TopologyImpact.ROWS, true, false))
				.build();
		return executeAuthorized(transaction, authorizationGuard, project);
	}

	public TaskCommandResult dragSchedule(TaskScheduleDragCommand command) {
		return project.getDomainChangeJournal().write(() -> dragScheduleLocked(command));
	}

	public TaskCommandResult moveTimeline(TaskTimelineMoveCommand command) {
		return project.getDomainChangeJournal().write(() -> moveTimelineLocked(command));
	}

	public TaskCommandResult changeAssignments(TaskAssignmentBatchCommand command) {
		return project.getDomainChangeJournal().write(() -> changeAssignmentsLocked(command));
	}

	private TaskCommandResult changeAssignmentsLocked(TaskAssignmentBatchCommand command) {
		if (project.getDomainChangeJournal().revision() != command.expectedDomainRevision())
			return TaskCommandResult.of(TaskCommandResult.Status.CONFLICT);
		List<NormalTask> tasks = new ArrayList<>(command.tasks().size());
		for (ProjectTaskKey key : command.tasks()) {
			Task task = resolve(key);
			if (!(task instanceof NormalTask normalTask)) return TaskCommandResult.of(TaskCommandResult.Status.NOT_FOUND);
			if (project.isReadOnly() || task.isReadOnly() || !task.isAssignable())
				return TaskCommandResult.of(TaskCommandResult.Status.READ_ONLY);
			tasks.add(normalTask);
		}
		List<Resource> assignResources = resolveResources(command.assignResourceUniqueIds());
		List<Resource> removeResources = resolveResources(command.removeResourceUniqueIds());
		if (assignResources == null || removeResources == null)
			return TaskCommandResult.of(TaskCommandResult.Status.NOT_FOUND);
		List<Assignment> removals = new ArrayList<>();
		record MissingAssignment(NormalTask task, Resource resource) { }
		List<MissingAssignment> missing = new ArrayList<>();
		for (NormalTask task : tasks) {
			for (Resource resource : assignResources)
				if (task.findAssignment(resource) == null) missing.add(new MissingAssignment(task, resource));
			for (Resource resource : removeResources) {
				Assignment existing = task.findAssignment(resource);
				if (existing != null) removals.add(existing);
			}
		}
		if (missing.isEmpty() && removals.isEmpty()) return TaskCommandResult.of(TaskCommandResult.Status.NO_OP);

		ProjectScheduleBackup scheduleBackup = captureSchedule(project);
		AssignmentDeltaEdit assignmentEdit = new AssignmentDeltaEdit(project);
		removals.forEach(assignmentEdit::recordRemoval);
		AuthorizationGuard guard = new AuthorizationGuard(Set.copyOf(command.tasks()), TaskCommandType.CHANGE_ASSIGNMENTS);
		ModelTransaction<List<NormalTask>> transaction = TaskCommandGateway.<List<NormalTask>>authorizedBuilder(project, guard)
				.captureRollback(() -> () -> {
					assignmentEdit.restoreBeforeCommit();
					restoreSchedule(project, scheduleBackup);
				})
				.apply(() -> {
					for (Assignment assignment : removals)
						AssignmentService.getInstance().remove(assignment, this, false);
					try {
						AssignmentService.getInstance().newAssignments(tasks, assignResources, command.units(), 0L, this, false);
					} finally {
						for (MissingAssignment target : missing) {
							Assignment assigned = target.task().findAssignment(target.resource());
							if (assigned != null) assignmentEdit.recordAddition(assigned);
						}
					}
					if (assignmentEdit.additionCount() != missing.size())
						throw new IllegalStateException("Assignment was not connected");
					project.recalculate();
					return ModelTransaction.Mutation.changed(List.copyOf(tasks));
				})
				.invariant(() -> tasks.stream().allMatch(task ->
						assignResources.stream().allMatch(resource -> task.findAssignment(resource) != null)
						&& removeResources.stream().allMatch(resource -> task.findAssignment(resource) == null)))
				.commitUndo(value -> project.getUndoController().commitEdit(assignmentEdit))
				.changes(value -> new DomainChangeSet.Draft(UUID.randomUUID(), DomainChangeSet.Origin.COMMAND,
						Set.copyOf(command.tasks()), Set.of("Field.resourceNames"),
						DomainChangeSet.TopologyImpact.NONE, true, false))
				.build();
		return executeAuthorized(transaction, guard, project);
	}

	private TaskCommandResult moveTimelineLocked(TaskTimelineMoveCommand command) {
		if (project.getDomainChangeJournal().revision() != command.expectedDomainRevision())
			return TaskCommandResult.of(TaskCommandResult.Status.CONFLICT);
		Task task = resolve(command.task());
		if (task == null) return TaskCommandResult.of(TaskCommandResult.Status.NOT_FOUND);
		if (project.isReadOnly() || task.isReadOnly() || command.proposedStart() != null && task.inProgress())
			return TaskCommandResult.of(TaskCommandResult.Status.READ_ONLY);
		Assignment assignment = command.assignmentUniqueId() == null ? null : findAssignment(task, command.assignmentUniqueId());
		if (command.assignmentUniqueId() != null && (assignment == null
				|| assignment.getResource().getUniqueId() != command.expectedResourceUniqueId().longValue()))
			return TaskCommandResult.of(TaskCommandResult.Status.CONFLICT);
		Resource target = command.targetResourceUniqueId() == null ? null : findResource(command.targetResourceUniqueId());
		if (command.targetResourceUniqueId() != null && target == null)
			return TaskCommandResult.of(TaskCommandResult.Status.NOT_FOUND);
		boolean changeResource = assignment != null && assignment.getResource() != target;
		NormalTask normalTask = assignment == null ? null : (NormalTask) task;
		boolean targetIsUnassigned = target == ResourceImpl.getUnassignedInstance();
		if (changeResource && normalTask.findAssignment(target) != null)
			return TaskCommandResult.of(TaskCommandResult.Status.CONFLICT);
		if (changeResource && targetIsUnassigned && normalTask.getRealAssignments().size() != 1)
			return TaskCommandResult.of(TaskCommandResult.Status.CONFLICT);
		boolean changeStart = command.proposedStart() != null && command.proposedStart().longValue() != task.getStart();
		boolean manualStart = changeStart && task.isManuallyScheduled();
		long originalStart = task.getStart();
		long originalEnd = task.getEnd();
		int originalConstraintType = task.getConstraintType();
		long originalConstraintDate = task.getConstraintDate();
		if (task.getStart() != command.expectedStart()) return TaskCommandResult.of(TaskCommandResult.Status.CONFLICT);
		if (!changeResource && !changeStart) return TaskCommandResult.of(TaskCommandResult.Status.NO_OP);

		UndoableEdit[] captured = new UndoableEdit[1];
		ProjectScheduleBackup scheduleBackup = captureSchedule(project);
		boolean projectDirty = project.isDirty();
		AssignmentDeltaEdit assignmentEdit = changeResource ? new AssignmentDeltaEdit(project) : null;
		if (assignmentEdit != null) assignmentEdit.recordRemoval(assignment);
		AuthorizationGuard guard = new AuthorizationGuard(Set.of(command.task()), TaskCommandType.MOVE_TIMELINE);
		ModelTransaction<Task> transaction = TaskCommandGateway.<Task>authorizedBuilder(project, guard)
				.captureRollback(() -> () -> {
					if (captured[0] != null && captured[0].canUndo()) captured[0].undo();
					if (assignmentEdit != null) assignmentEdit.restoreBeforeCommit();
					restoreSchedule(project, scheduleBackup);
					task.setScheduleConstraint(originalConstraintType, originalConstraintDate);
					project.setDirty(projectDirty);
				})
				.apply(() -> {
					UndoController.EditCapture capture = project.getUndoController().captureEdits();
					try {
						if (assignmentEdit != null) {
							AssignmentService.getInstance().remove(assignment, this, false);
							if (!targetIsUnassigned) {
								try {
									AssignmentService.getInstance().newAssignment(normalTask, target,
											assignment.getUnits(), assignment.getDelay(), this, false);
								} finally {
									Assignment added = normalTask.findAssignment(target);
									if (added != null) {
										added.usePropertiesOf(assignment);
										assignmentEdit.recordAddition(added);
									}
								}
							}
						}
						if (manualStart)
							task.setManualDates(command.proposedStart().longValue(),
									command.proposedStart().longValue() + Math.max(0L, originalEnd - originalStart));
						else if (changeStart)
							ScheduleService.getInstance().setConstraint(this, task, ConstraintType.SNET,
									command.proposedStart().longValue(), project.getUndoController().getEditSupport());
						task.setDirty(true);
						project.recalculate();
					} finally {
						capture.close();
						captured[0] = capture.edit();
					}
					return captured[0] == null && !manualStart && assignmentEdit == null ? ModelTransaction.Mutation.noOp(task)
							: ModelTransaction.Mutation.changed(task);
				})
				.invariant(() -> task.getStart() <= task.getEnd()
						&& (!changeResource || normalTask.findAssignment(target) != null))
				.commitUndo(value -> {
					UndoableEdit manual = manualStart ? new ManualDatesEdit(task, originalStart, originalEnd,
							task.getStart(), task.getEnd(), project) : null;
					project.getUndoController().commitEdit(combineEdits(assignmentEdit, captured[0], manual));
				})
				.changes(value -> new DomainChangeSet.Draft(UUID.randomUUID(), DomainChangeSet.Origin.COMMAND,
						Set.of(command.task()), Set.of("Field.resourceNames", "Field.start", "Field.finish"),
						DomainChangeSet.TopologyImpact.NONE, true, false))
				.build();
		return executeAuthorized(transaction, guard, project);
	}

	private TaskCommandResult dragScheduleLocked(TaskScheduleDragCommand command) {
		if (project.getDomainChangeJournal().revision() != command.expectedDomainRevision())
			return TaskCommandResult.of(TaskCommandResult.Status.CONFLICT);
		Task task = resolve(command.task());
		if (task == null) return TaskCommandResult.of(TaskCommandResult.Status.NOT_FOUND);
		if (project.isReadOnly() || task.isReadOnly()) return TaskCommandResult.of(TaskCommandResult.Status.READ_ONLY);
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
		ScheduleInterval liveInterval = findInterval(schedule, command.intervalIndex());
		if (liveInterval == null) return TaskCommandResult.of(TaskCommandResult.Status.CONFLICT);
		AuthorizationGuard authorizationGuard = new AuthorizationGuard(Set.of(command.task()), TaskCommandType.DRAG_SCHEDULE);
		ModelTransaction<ScheduleInterval> transaction = TaskCommandGateway.<ScheduleInterval>authorizedBuilder(project, authorizationGuard)
				.captureRollback(() -> () -> {
					restoreSchedule(project, projectScheduleBackup);
					task.setScheduleConstraint(command.expectedConstraintType(), command.expectedConstraintDate());
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
					AtomicCompoundEdit compound = new AtomicCompoundEdit();
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
				.build();
		return executeAuthorized(transaction, authorizationGuard, project);
	}

	public TaskCommandResult paste(TaskPasteCommand command) {
		return project.getDomainChangeJournal().write(() -> pasteLocked(command));
	}

	public TaskCommandResult deleteTasks(TaskDeleteCommand command) {
		return project.getDomainChangeJournal().write(() -> deleteTasksLocked(command));
	}

	private TaskCommandResult deleteTasksLocked(TaskDeleteCommand command) {
		if (command == null || command.tasks().isEmpty()) return TaskCommandResult.of(TaskCommandResult.Status.NO_OP);
		if (project.getDomainChangeJournal().revision() != command.expectedDomainRevision())
			return TaskCommandResult.of(TaskCommandResult.Status.CONFLICT);
		List<Node> nodes = new ArrayList<>(command.tasks().size());
		List<Task> tasks = new ArrayList<>(command.tasks().size());
		for (ProjectTaskKey key : command.tasks()) {
			Task task = resolve(key);
			Node node = task == null ? null : project.getTaskModel().search(task);
			if (node == null) return TaskCommandResult.of(TaskCommandResult.Status.NOT_FOUND);
			if (project.isReadOnly() || task.isReadOnly()) return TaskCommandResult.of(TaskCommandResult.Status.READ_ONLY);
			nodes.add(node);
			tasks.add(task);
		}
		Set<ProjectTaskKey> affected = new java.util.LinkedHashSet<>(hierarchyKeys(nodes));
		for (ProjectTaskKey key : List.copyOf(affected)) {
			Task affectedTask = resolve(key);
			if (affectedTask == null) continue;
			for (Object value : affectedTask.getPredecessorList())
				addTaskKey(affected, ((Dependency) value).getPredecessor());
			for (Object value : affectedTask.getSuccessorList())
				addTaskKey(affected, ((Dependency) value).getSuccessor());
		}
		Set<ProjectTaskKey> affectedKeys = Set.copyOf(affected);
		AuthorizationGuard guard = new AuthorizationGuard(affectedKeys, TaskCommandType.DELETE_TASK);
		UndoableEdit[] capturedEdit = new UndoableEdit[1];
		boolean projectDirty = project.isDirty();
		ProjectScheduleBackup scheduleBackup = captureSchedule(project);
		ModelTransaction<List<Node>> transaction = TaskCommandGateway.<List<Node>>authorizedBuilder(project, guard)
				.captureRollback(() -> () -> {
					if (capturedEdit[0] != null && capturedEdit[0].canUndo()) capturedEdit[0].undo();
					restoreSchedule(project, scheduleBackup);
					project.setDirty(projectDirty);
				})
				.apply(() -> {
					UndoController.EditCapture capture = project.getUndoController().captureEdits();
					try {
						project.getTaskModel().remove(nodes, NodeModel.NORMAL);
					} finally {
						capture.close();
						capturedEdit[0] = capture.edit();
					}
					return capturedEdit[0] == null ? ModelTransaction.Mutation.noOp(List.copyOf(nodes))
							: ModelTransaction.Mutation.changed(List.copyOf(nodes));
				})
				.invariant(() -> tasks.stream().allMatch(task -> project.getTaskModel().search(task) == null))
				.commitUndo(value -> project.getUndoController().commitEdit(capturedEdit[0]))
				.changes(value -> new DomainChangeSet.Draft(UUID.randomUUID(), DomainChangeSet.Origin.COMMAND,
						affectedKeys, Set.of(), DomainChangeSet.TopologyImpact.ROWS, true, true))
				.build();
		return executeAuthorized(transaction, guard, project);
	}

	private TaskCommandResult pasteLocked(TaskPasteCommand command) {
		if (project.getDomainChangeJournal().revision() != command.expectedDomainRevision())
			return TaskCommandResult.of(TaskCommandResult.Status.CONFLICT);
		Task parentTask = command.parent() == null ? null : resolve(command.parent());
		if (command.parent() != null && parentTask == null)
			return TaskCommandResult.of(TaskCommandResult.Status.NOT_FOUND);
		if (project.isReadOnly() || parentTask != null && parentTask.isReadOnly())
			return TaskCommandResult.of(TaskCommandResult.Status.READ_ONLY);
		NodeModel model = project.getTaskModel();
		Node parent = parentTask == null ? (Node) model.getRoot() : model.search(parentTask);
		if (parent == null || command.position() > parent.getChildCount())
			return TaskCommandResult.of(TaskCommandResult.Status.CONFLICT);
		List<Node> nodes = new ArrayList<>(command.detachedNodes());
		for (Node node : nodes)
			if (node == null || node.getParent() != null) return TaskCommandResult.of(TaskCommandResult.Status.INVALID);
		boolean projectDirty = project.isDirty();
		Set<ProjectTaskKey> authorizationKeys = hierarchyKeys(List.of(), parent);
		AuthorizationGuard authorizationGuard = new AuthorizationGuard(authorizationKeys, TaskCommandType.PASTE);
		ModelTransaction<List<Node>> transaction = TaskCommandGateway.<List<Node>>authorizedBuilder(project, authorizationGuard)
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
				.build();
		return executeAuthorized(transaction, authorizationGuard, project);
	}

	public TaskCommandResult updateProgress(TaskProgressCommand command) {
		return project.getDomainChangeJournal().write(() -> updateProgressLocked(command));
	}

	private TaskCommandResult updateProgressLocked(TaskProgressCommand command) {
		if (project.getDomainChangeJournal().revision() != command.expectedDomainRevision())
			return TaskCommandResult.of(TaskCommandResult.Status.CONFLICT);
		Task task = resolve(command.task());
		if (task == null) return TaskCommandResult.of(TaskCommandResult.Status.NOT_FOUND);
		if (project.isReadOnly() || task.isReadOnly()) return TaskCommandResult.of(TaskCommandResult.Status.READ_ONLY);
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
		AuthorizationGuard authorizationGuard = new AuthorizationGuard(Set.of(command.task()), TaskCommandType.DRAG_SCHEDULE);
		ModelTransaction<Long> transaction = TaskCommandGateway.<Long>authorizedBuilder(project, authorizationGuard)
				.captureRollback(() -> () -> { restoreSchedule(project, projectScheduleBackup); project.setDirty(projectDirty); })
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
				.build();
		return executeAuthorized(transaction, authorizationGuard, project);
	}

	public TaskCommandResult split(TaskSplitCommand command) {
		return project.getDomainChangeJournal().write(() -> splitLocked(command));
	}

	private TaskCommandResult splitLocked(TaskSplitCommand command) {
		if (project.getDomainChangeJournal().revision() != command.expectedDomainRevision())
			return TaskCommandResult.of(TaskCommandResult.Status.CONFLICT);
		Task task = resolve(command.task());
		if (task == null) return TaskCommandResult.of(TaskCommandResult.Status.NOT_FOUND);
		if (project.isReadOnly() || task.isReadOnly()) return TaskCommandResult.of(TaskCommandResult.Status.READ_ONLY);
		Schedule schedule = task;
		if (schedule.getStart() != command.expectedStart() || schedule.getEnd() != command.expectedEnd())
			return TaskCommandResult.of(TaskCommandResult.Status.CONFLICT);
		List<String> beforeIntervals = intervalSignature(schedule);
		Object backup = schedule.backupDetail();
		ProjectScheduleBackup projectScheduleBackup = captureSchedule(project);
		boolean projectDirty = project.isDirty();
		AuthorizationGuard authorizationGuard = new AuthorizationGuard(Set.of(command.task()), TaskCommandType.DRAG_SCHEDULE);
		ModelTransaction<Long> transaction = TaskCommandGateway.<Long>authorizedBuilder(project, authorizationGuard)
				.captureRollback(() -> () -> { restoreSchedule(project, projectScheduleBackup); project.setDirty(projectDirty); })
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
				.build();
		return executeAuthorized(transaction, authorizationGuard, project);
	}

	private TaskCommandResult executeAuthorized(ModelTransaction<?> transaction,
			AuthorizationGuard guard, Project owner) {
		UndoController undo = owner.getUndoController();
		try (guard; UndoController.StateNotificationScope ignored = undo == null ? null : undo.deferStateNotifications()) {
			ModelTransaction.Result<?> result = transaction.execute(owner.getDomainChangeJournal());
			if (result.status() == ModelTransaction.Status.AUTHORIZATION_FAILED
					&& guard.decision() == TaskAuthorizationPort.Decision.READ_ONLY)
				return TaskCommandResult.of(TaskCommandResult.Status.READ_ONLY);
			return map(result);
		}
	}

	private final class AuthorizationGuard implements AutoCloseable {
		private final Set<ProjectTaskKey> keys;
		private final TaskCommandType commandType;
		private TaskAuthorizationPort.AuthorizationLease lease;
		private TaskAuthorizationPort.Decision decision = TaskAuthorizationPort.Decision.LOCK_DENIED;

		private AuthorizationGuard(Set<ProjectTaskKey> keys, TaskCommandType commandType) {
			this.keys = Set.copyOf(keys);
			this.commandType = commandType;
		}

		private boolean acquire() {
			lease = authorization.acquire(keys, commandType);
			decision = lease.decision();
			return decision == TaskAuthorizationPort.Decision.ALLOWED;
		}

		private boolean validateAtCommit() {
			return lease != null && lease.validateAtCommit();
		}

		private TaskAuthorizationPort.Decision decision() { return decision; }

		@Override public void close() {
			if (lease != null) lease.close();
		}
	}

	private static List<String> intervalSignature(Schedule schedule) {
		List<String> result = new ArrayList<>();
		ScheduleService.getInstance().consumeIntervals(schedule,
				interval -> result.add(interval.getStart() + ":" + interval.getEnd()));
		return List.copyOf(result);
	}

	private static <T> ModelTransaction.Builder<T> authorizedBuilder(Project owner, AuthorizationGuard guard) {
		return ModelTransaction.<T>builder()
				.authorize(guard::acquire)
				.commitAuthorization(guard::validateAtCommit)
				.onRecoveryFailure(failure -> owner.setReadOnly(true));
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

	private record ProjectScheduleBackup(Object projectDetail, boolean projectDirty,
			Map<Task, Object> taskDetails, Map<Task, Boolean> dirtyStates) { }

	private static ProjectScheduleBackup captureSchedule(Project project) {
		Map<Task, Object> details = new IdentityHashMap<>();
		Map<Task, Boolean> dirtyStates = new IdentityHashMap<>();
		for (Task task : project.getTasks()) {
			details.put(task, task.backupDetail());
			dirtyStates.put(task, task.isDirty());
		}
		return new ProjectScheduleBackup(project.backupDetail(), project.isDirty(), Collections.unmodifiableMap(details),
				Collections.unmodifiableMap(dirtyStates));
	}

	private static void restoreSchedule(Project project, ProjectScheduleBackup backup) {
		if (backup == null) return;
		for (Map.Entry<Task, Object> entry : backup.taskDetails().entrySet())
			entry.getKey().restoreDetail(TaskCommandGateway.class, entry.getValue(), false);
		project.restoreDetail(TaskCommandGateway.class, backup.projectDetail(), false);
		backup.dirtyStates().forEach(Task::setDirty);
		project.setDirty(backup.projectDirty());
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

	private static Set<ProjectTaskKey> hierarchyKeys(List<Node> moved, Node... context) {
		Set<ProjectTaskKey> keys = new java.util.LinkedHashSet<>();
		Set<Node> visited = Collections.newSetFromMap(new IdentityHashMap<>());
		for (Node node : moved) addHierarchySubtreeKeys(node, keys, visited);
		for (Node node : context) {
			for (Node current = node; current != null && visited.add(current);
					current = current.getParent() instanceof Node parent ? parent : null)
				addNodeKey(current, keys);
		}
		return Set.copyOf(keys);
	}

	private static void addHierarchySubtreeKeys(Node node, Set<ProjectTaskKey> keys, Set<Node> visited) {
		if (node == null || !visited.add(node)) return;
		addNodeKey(node, keys);
		for (int index = 0; index < node.getChildCount(); index++)
			addHierarchySubtreeKeys((Node) node.getChildAt(index), keys, visited);
	}

	private static void addNodeKey(Node node, Set<ProjectTaskKey> keys) {
		if (node != null && node.getImpl() instanceof Task task) ProjectTaskKey.from(task).ifPresent(keys::add);
	}

	private static ScheduleInterval findInterval(Schedule schedule, int wanted) {
		List<ScheduleInterval> intervals = new ArrayList<>();
		ScheduleService.getInstance().consumeIntervals(schedule, intervals::add);
		return wanted < intervals.size() ? intervals.get(wanted) : null;
	}

	private Task resolve(ProjectTaskKey key) {
		return ProjectTaskKey.resolve(project, key).orElse(null);
	}

	private static Assignment findAssignment(Task task, long uniqueId) {
		if (!(task instanceof NormalTask normalTask)) return null;
		return normalTask.getAssignments().stream().map(Assignment.class::cast)
				.filter(value -> value.getUniqueId() == uniqueId).findFirst().orElse(null);
	}

	private Resource findResource(long uniqueId) {
		Resource unassigned = ResourceImpl.getUnassignedInstance();
		if (unassigned.getUniqueId() == uniqueId) return unassigned;
		return project.getResourcePool().getResourceList().stream()
				.filter(value -> value.getUniqueId() == uniqueId).findFirst().orElse(null);
	}

	private List<Resource> resolveResources(List<Long> uniqueIds) {
		List<Resource> resources = new ArrayList<>(uniqueIds.size());
		for (long uniqueId : uniqueIds) {
			Resource resource = findResource(uniqueId);
			if (resource == null) return null;
			resources.add(resource);
		}
		return List.copyOf(resources);
	}

	private static Dependency findDependency(Task predecessor, Task successor) {
		return (Dependency) predecessor.getSuccessorList().findRight(successor);
	}

	private static boolean adjacentDependenciesExist(List<Task> tasks) {
		for (int index = 0; index + 1 < tasks.size(); index++)
			if (findDependency(tasks.get(index), tasks.get(index + 1)) == null) return false;
		return true;
	}

	private static boolean hasDependencies(Task task) {
		return !task.getPredecessorList().isEmpty() || !task.getSuccessorList().isEmpty();
	}

	private static void addTaskKey(Set<ProjectTaskKey> keys, Object endpoint) {
		if (endpoint instanceof Task task) ProjectTaskKey.from(task).ifPresent(keys::add);
	}

	private static boolean isScheduleField(String fieldId) {
		return fieldId != null && (fieldId.contains("start") || fieldId.contains("finish")
				|| fieldId.contains("duration") || fieldId.contains("complete"));
	}

	private static UndoableEdit combineEdits(UndoableEdit... edits) {
		List<UndoableEdit> present = java.util.Arrays.stream(edits).filter(Objects::nonNull).toList();
		if (present.isEmpty()) throw new IllegalStateException("A committed mutation requires an Undo edit");
		if (present.size() == 1) return present.get(0);
		AtomicCompoundEdit compound = new AtomicCompoundEdit();
		present.forEach(compound::addEdit);
		compound.end();
		return compound;
	}

	private static final class AssignmentDeltaEdit extends AbstractUndoableEdit {
		private static final long serialVersionUID = 1L;
		private final List<Assignment> removals = new ArrayList<>();
		private final List<Assignment> additions = new ArrayList<>();
		private final Project project;

		AssignmentDeltaEdit(Project project) { this.project = project; }
		void recordRemoval(Assignment assignment) { if (!removals.contains(assignment)) removals.add(assignment); }
		void recordAddition(Assignment assignment) { if (!additions.contains(assignment)) additions.add(assignment); }
		int additionCount() { return additions.size(); }
		void restoreBeforeCommit() { restoreBefore(); project.recalculate(); }
		@Override public void undo() {
			super.undo();
			restoreBefore();
			project.recalculate();
		}
		@Override public void redo() {
			super.redo();
			for (int index = removals.size() - 1; index >= 0; index--) removeIfConnected(removals.get(index));
			for (Assignment assignment : additions) connectIfMissing(assignment);
			project.recalculate();
		}
		private void restoreBefore() {
			for (int index = additions.size() - 1; index >= 0; index--) removeIfConnected(additions.get(index));
			for (Assignment assignment : removals) connectIfMissing(assignment);
		}
		private void connectIfMissing(Assignment assignment) {
			if (((NormalTask) assignment.getTask()).findAssignment(assignment.getResource()) == null)
				AssignmentService.getInstance().connect(assignment, this, false);
		}
		private void removeIfConnected(Assignment assignment) {
			Assignment current = ((NormalTask) assignment.getTask()).findAssignment(assignment.getResource());
			if (current != null) AssignmentService.getInstance().remove(current, this, false);
		}
	}

	private static final class ManualDatesEdit extends AbstractUndoableEdit {
		private static final long serialVersionUID = 1L;
		private final Task task;
		private final long beforeStart;
		private final long beforeEnd;
		private final long afterStart;
		private final long afterEnd;
		private final Project project;

		ManualDatesEdit(Task task, long beforeStart, long beforeEnd, long afterStart, long afterEnd, Project project) {
			this.task = task;
			this.beforeStart = beforeStart;
			this.beforeEnd = beforeEnd;
			this.afterStart = afterStart;
			this.afterEnd = afterEnd;
			this.project = project;
		}

		@Override public void undo() { super.undo(); apply(beforeStart, beforeEnd); }
		@Override public void redo() { super.redo(); apply(afterStart, afterEnd); }
		private void apply(long start, long end) { task.setManualDates(start, end); project.recalculate(); }
	}

	private void setProposed(Field field, Node node, NodeModel model, FieldContext context, Object proposed)
			throws FieldParseException {
		if (proposed instanceof TaskFieldEditCommand.Text text)
			field.setText(node, model, text.value(), context);
		else
			field.setValue(node, model, this, proposed, context);
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
