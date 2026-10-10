/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.spreadsheet.command;

import java.awt.Component;
import java.util.Objects;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

import com.microproject.undo.EditSupport;

import com.microproject.pm.graphic.collaboration.CollaborationHelper;
import com.microproject.grouping.core.Node;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.field.FieldParseException;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.scheduling.Schedule;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.pm.graphic.model.cache.ProjectionRowKey;
import com.microproject.pm.graphic.model.cache.RevisionedProjectionIndex;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetModel;
import com.microproject.association.InvalidAssociationException;
import com.microproject.util.ClassUtils;

/** Resolves task edits against the current projection before entering the canonical field/Undo path. */
public final class TaskCommandGateway {
	private TaskCommandGateway() {
	}

	/** Resolves visible Gantt/Network endpoints to stable task keys before using the shared dependency command. */
	public static TaskCommandResult createDependency(NodeModelCache cache, GraphicNode startNode,
			GraphicNode endNode, Object eventSource) throws InvalidAssociationException {
		return createDependency(cache, startNode, endNode, eventSource, null);
	}

	public static TaskCommandResult createDependency(NodeModelCache cache, GraphicNode startNode,
			GraphicNode endNode, Object eventSource, Component dialogParent) throws InvalidAssociationException {
		if (cache == null || startNode == null || endNode == null || startNode.getNode() == null
				|| endNode.getNode() == null || !(startNode.getNode().getImpl() instanceof Task)
				|| !(endNode.getNode().getImpl() instanceof Task))
			throw new InvalidAssociationException("Dependencies require two task rows");
		RevisionedProjectionIndex projection = cache.getVisibleNodes().getProjectionIndex();
		int startRow = projection.rowForNode(startNode);
		int endRow = projection.rowForNode(endNode);
		if (startRow < 0 || endRow < 0 || !(projection.keyAt(startRow) instanceof ProjectionRowKey.TaskRow startKey)
				|| !(projection.keyAt(endRow) instanceof ProjectionRowKey.TaskRow endKey))
			throw new InvalidAssociationException("Dependency tasks are not in the active projection");
		if (startKey.taskKey().equals(endKey.taskKey()))
			throw new InvalidAssociationException("A task cannot depend on itself");
		TaskCommandResult result = execute(cache, new TaskDependencyIntent(TaskDependencyIntent.Operation.LINK,
			List.of(startKey, endKey), projection.topologyRevision(), null), eventSource, dialogParent);
		if (result.status() == TaskCommandResult.Status.LOCKED)
			return result;
		if (result.status() != TaskCommandResult.Status.CHANGED && result.status() != TaskCommandResult.Status.NO_CHANGE)
			throw new InvalidAssociationException("Unable to create task dependency: " + result.reason());
		return result;
	}

	public static TaskCommandResult execute(SpreadSheetModel sheetModel, TaskFieldEditIntent intent,
			Object eventSource) throws FieldParseException {
		Objects.requireNonNull(sheetModel, "sheetModel");
		Objects.requireNonNull(intent, "intent");
		if (sheetModel.getCache() == null)
			return new TaskCommandResult(TaskCommandResult.Status.MISSING_TASK, "missing-task-projection");

		RevisionedProjectionIndex projection = sheetModel.getCache().getVisibleNodes().getProjectionIndex();
		if (projection.topologyRevision() != intent.projectionRevision())
			return new TaskCommandResult(TaskCommandResult.Status.STALE_PROJECTION, "projection-revision-changed");

		ProjectionRowKey.TaskRow taskRow = new ProjectionRowKey.TaskRow(intent.taskKey(), intent.occurrence());
		int modelRow = projection.rowForKey(taskRow);
		if (modelRow < 0)
			return new TaskCommandResult(TaskCommandResult.Status.MISSING_TASK, "task-not-visible");
		Node node = projection.nodeAt(modelRow).getNode();
		if (node == null || node.isVoid() || !(node.getImpl() instanceof Task))
			return new TaskCommandResult(TaskCommandResult.Status.MISSING_TASK, "task-not-editable");

		Object currentValue = intent.field().getValue(node, sheetModel.getCache().getWalkersModel(),
			sheetModel.getFieldContext());
		if (!Objects.deepEquals(currentValue, intent.expectedValue()))
			return new TaskCommandResult(TaskCommandResult.Status.STALE_VALUE, "field-value-changed");
		if (intent.value() != null && Objects.deepEquals(currentValue, intent.value()))
			return TaskCommandResult.of(TaskCommandResult.Status.NO_CHANGE);

		sheetModel.getCache().getModel().setFieldValue(intent.field(), node, eventSource, intent.value(),
			sheetModel.getFieldContext(), NodeModel.NORMAL);
		return TaskCommandResult.of(TaskCommandResult.Status.CHANGED);
	}

	/** Applies move/row-drag edits only after resolving every task occurrence in the captured projection. */
	public static boolean canExecute(SpreadSheet sheet, TaskHierarchyEditIntent intent) {
		PreparedHierarchyEdit prepared = prepare(sheet, intent);
		return prepared.rejection() == null && prepared.possible();
	}

	public static TaskCommandResult execute(SpreadSheet sheet, TaskHierarchyEditIntent intent) {
		PreparedHierarchyEdit prepared = prepare(sheet, intent);
		if (prepared.rejection() != null)
			return prepared.rejection();
		if (!prepared.possible())
			return new TaskCommandResult(TaskCommandResult.Status.REJECTED, "hierarchy-transition-rejected");
		List<Node> locks = new ArrayList<>(prepared.nodes());
		if (prepared.anchor() != null && !locks.contains(prepared.anchor()))
			locks.add(prepared.anchor());
		if (!CollaborationHelper.tryLockNodes(null, locks, sheet, "move task"))
			return new TaskCommandResult(TaskCommandResult.Status.LOCKED, "collaboration-lock-denied");
		boolean changed = intent.operation() == TaskHierarchyEditIntent.Operation.MOVE
			? prepared.cache().moveNodes(prepared.graphicNodes(), intent.direction())
			: prepared.cache().relocateNodes(prepared.graphicNodes(), prepared.anchor(), intent.after());
		return TaskCommandResult.of(changed ? TaskCommandResult.Status.CHANGED : TaskCommandResult.Status.REJECTED);
	}

	/** Validates and applies one task-row paste against the captured projection and selection. */
	public static TaskCommandResult execute(SpreadSheet sheet, TaskPasteIntent intent) {
		Objects.requireNonNull(sheet, "sheet");
		Objects.requireNonNull(intent, "intent");
		if (!(sheet.getModel() instanceof SpreadSheetModel sheetModel) || sheetModel.getCache() == null
				|| !(sheetModel.getCache().getModel().getDataFactory() instanceof Project project))
			return new TaskCommandResult(TaskCommandResult.Status.INVALID_INTENT, "task-paste-requires-project-model");
		if (project.isReadOnly())
			return new TaskCommandResult(TaskCommandResult.Status.REJECTED, "document-read-only");

		RevisionedProjectionIndex projection = sheetModel.getCache().getVisibleNodes().getProjectionIndex();
		if (projection.topologyRevision() != intent.projectionRevision())
			return new TaskCommandResult(TaskCommandResult.Status.STALE_PROJECTION, "projection-revision-changed");

		Set<Node> uniqueRoots = Collections.newSetFromMap(new IdentityHashMap<>());
		for (Node root : intent.copiedRoots()) {
			if (!uniqueRoots.add(root) || (!root.isVoid() && !(root.getImpl() instanceof Task)))
				return new TaskCommandResult(TaskCommandResult.Status.INVALID_INTENT, "invalid-task-paste-batch");
		}

		List<Node> selectedNodes = new ArrayList<>(intent.selectedRows().size());
		for (ProjectionRowKey rowKey : intent.selectedRows()) {
			int row = projection.rowForKey(rowKey);
			if (row < 0)
				return new TaskCommandResult(TaskCommandResult.Status.MISSING_TASK, "paste-anchor-not-visible");
			GraphicNode graphicNode = projection.nodeAt(row);
			Node node = graphicNode == null ? null : graphicNode.getNode();
			if (node == null || node.isVoid() && !(rowKey instanceof ProjectionRowKey.SyntheticRow))
				return new TaskCommandResult(TaskCommandResult.Status.MISSING_TASK, "paste-anchor-not-editable");
			selectedNodes.add(node);
		}
		if (!selectedNodes.isEmpty() && !CollaborationHelper.tryLockNodes(null, selectedNodes, sheet, "paste"))
			return new TaskCommandResult(TaskCommandResult.Status.LOCKED, "collaboration-lock-denied");

		Node anchor = selectedNodes.isEmpty() ? null : selectedNodes.getFirst();
		Node parent = anchor == null ? null : (Node) anchor.getParent();
		int position = anchor == null || parent == null ? 0 : ((com.microproject.grouping.core.NodeBridge) parent).getIndex(anchor);
		boolean pasted = sheetModel.getCache().pasteNodes(parent, new ArrayList<>(intent.copiedRoots()), position);
		return TaskCommandResult.of(pasted ? TaskCommandResult.Status.CHANGED : TaskCommandResult.Status.REJECTED);
	}

	/** Validates the captured task projection and schedule values before a Gantt edit runs. */
	public static TaskCommandResult executeScheduleEdit(NodeModelCache cache, TaskScheduleEditIntent intent,
			Component lockParent, BooleanSupplier mutation) {
		Objects.requireNonNull(cache, "cache");
		Objects.requireNonNull(intent, "intent");
		Objects.requireNonNull(mutation, "mutation");
		if (!(cache.getModel().getDataFactory() instanceof Project project))
			return new TaskCommandResult(TaskCommandResult.Status.INVALID_INTENT, "schedule-edit-requires-project-model");
		if (project.isReadOnly())
			return new TaskCommandResult(TaskCommandResult.Status.REJECTED, "document-read-only");
		RevisionedProjectionIndex projection = cache.getVisibleNodes().getProjectionIndex();
		if (projection.topologyRevision() != intent.projectionRevision())
			return new TaskCommandResult(TaskCommandResult.Status.STALE_PROJECTION, "projection-revision-changed");
		int row = projection.rowForKey(intent.task());
		if (row < 0)
			return new TaskCommandResult(TaskCommandResult.Status.MISSING_TASK, "task-not-visible");
		GraphicNode graphicNode = projection.nodeAt(row);
		Node node = graphicNode == null ? null : graphicNode.getNode();
		if (node == null || node.isVoid() || !(node.getImpl() instanceof Task task)
				|| !(node.getImpl() instanceof Schedule schedule))
			return new TaskCommandResult(TaskCommandResult.Status.MISSING_TASK, "task-not-editable");
		if (schedule.getStart() != intent.expectedScheduleStart()
				|| schedule.getEnd() != intent.expectedScheduleEnd()
				|| schedule.getCompletedThrough() != intent.expectedCompletedThrough()
				|| task.getConstraintType() != intent.expectedConstraintType()
				|| task.getConstraintDate() != intent.expectedConstraintDate())
			return new TaskCommandResult(TaskCommandResult.Status.STALE_VALUE, "schedule-value-changed");
		if ((intent.operation() == TaskScheduleEditIntent.Operation.MOVE
				|| intent.operation() == TaskScheduleEditIntent.Operation.RESIZE_START
				|| intent.operation() == TaskScheduleEditIntent.Operation.RESIZE_END)
				&& !containsScheduleInterval(schedule, intent.expectedIntervalStart(), intent.expectedIntervalEnd(),
					intent.expectedScheduleStart(), intent.expectedScheduleEnd()))
			return new TaskCommandResult(TaskCommandResult.Status.STALE_VALUE, "schedule-interval-changed");
		if (intent.operation() == TaskScheduleEditIntent.Operation.SPLIT
				&& (intent.requestedValue() < intent.expectedCompletedThrough()
					|| !containsSchedulePoint(schedule, intent.requestedValue())))
			return new TaskCommandResult(TaskCommandResult.Status.STALE_VALUE, "split-point-outside-current-work-interval");
		if (intent.operation() == TaskScheduleEditIntent.Operation.PROGRESS
				&& intent.requestedValue() == intent.expectedCompletedThrough())
			return TaskCommandResult.of(TaskCommandResult.Status.NO_CHANGE);
		if (!CollaborationHelper.tryLockNodes(null, List.of(node), lockParent, "edit"))
			return new TaskCommandResult(TaskCommandResult.Status.LOCKED, "collaboration-lock-denied");
		return TaskCommandResult.of(mutation.getAsBoolean()
			? TaskCommandResult.Status.CHANGED : TaskCommandResult.Status.NO_CHANGE);
	}

	private static boolean containsScheduleInterval(Schedule schedule, long expectedStart, long expectedEnd,
			long expectedScheduleStart, long expectedScheduleEnd) {
		boolean[] found = { false };
		schedule.consumeIntervals(interval -> {
			if (interval.getStart() == expectedStart
					&& (interval.getEnd() == expectedEnd
						|| (expectedEnd >= interval.getEnd() && expectedStart == expectedScheduleStart
							&& interval.getEnd() == expectedScheduleEnd)))
				found[0] = true;
		});
		return found[0];
	}

	private static boolean containsSchedulePoint(Schedule schedule, long point) {
		boolean[] found = { false };
		schedule.consumeIntervals(interval -> {
			if (point >= interval.getStart() && point < interval.getEnd())
				found[0] = true;
		});
		return found[0];
	}

	/** Resolves a stable task selection before creating or removing dependencies. */
	public static TaskCommandResult execute(SpreadSheet sheet, TaskDependencyIntent intent, Object eventSource)
			throws InvalidAssociationException {
		Objects.requireNonNull(sheet, "sheet");
		if (!(sheet.getModel() instanceof SpreadSheetModel sheetModel) || sheetModel.getCache() == null)
			return new TaskCommandResult(TaskCommandResult.Status.INVALID_INTENT, "task-dependency-requires-project-model");
		return execute(sheetModel.getCache(), intent, eventSource, sheet);
	}

	/** Applies dependency edits from any task projection through the same validated command path. */
	public static TaskCommandResult execute(NodeModelCache cache, TaskDependencyIntent intent, Object eventSource)
			throws InvalidAssociationException {
		return execute(cache, intent, eventSource, null);
	}

	private static TaskCommandResult execute(NodeModelCache cache, TaskDependencyIntent intent, Object eventSource,
			Component lockParent) throws InvalidAssociationException {
		Objects.requireNonNull(cache, "cache");
		Objects.requireNonNull(intent, "intent");
		if (!(cache.getModel().getDataFactory() instanceof Project project))
			return new TaskCommandResult(TaskCommandResult.Status.INVALID_INTENT, "task-dependency-requires-project-model");
		if (project.isReadOnly())
			return new TaskCommandResult(TaskCommandResult.Status.REJECTED, "document-read-only");

		RevisionedProjectionIndex projection = cache.getVisibleNodes().getProjectionIndex();
		if (projection.topologyRevision() != intent.projectionRevision())
			return new TaskCommandResult(TaskCommandResult.Status.STALE_PROJECTION, "projection-revision-changed");

		List<Node> selectedNodes = new ArrayList<>(intent.tasks().size());
		List<Task> tasks = new ArrayList<>(intent.tasks().size());
		for (ProjectionRowKey.TaskRow key : intent.tasks()) {
			int row = projection.rowForKey(key);
			if (row < 0)
				return new TaskCommandResult(TaskCommandResult.Status.MISSING_TASK, "task-not-visible");
			GraphicNode graphicNode = projection.nodeAt(row);
			Node node = graphicNode == null ? null : graphicNode.getNode();
			if (node == null || node.isVoid() || !(node.getImpl() instanceof Task task))
				return new TaskCommandResult(TaskCommandResult.Status.MISSING_TASK, "task-not-editable");
			selectedNodes.add(node);
			tasks.add(task);
		}
		DependencyService service = DependencyService.getInstance();
		Dependency selectedDependency = null;
		if (intent.operation() == TaskDependencyIntent.Operation.UNLINK && intent.dependency() != null) {
			selectedDependency = tasks.stream().flatMap(task -> service.getIncidentDependencies(task).stream())
				.filter(intent.dependency()::matches).findFirst().orElse(null);
			if (selectedDependency == null)
				return new TaskCommandResult(TaskCommandResult.Status.MISSING_TASK, "dependency-not-found");
		}
		List<Object> lockTargets = new ArrayList<>(selectedNodes);
		if (selectedDependency != null) {
			if (selectedDependency.getPredecessor() instanceof Task predecessor
					&& tasks.stream().noneMatch(task -> task == predecessor))
				lockTargets.add(predecessor);
			if (selectedDependency.getSuccessor() instanceof Task successor
					&& tasks.stream().noneMatch(task -> task == successor))
				lockTargets.add(successor);
		}
		if (!CollaborationHelper.tryLockNodes(null, lockTargets, lockParent,
			intent.operation() == TaskDependencyIntent.Operation.LINK ? "link" : "unlink"))
			return new TaskCommandResult(TaskCommandResult.Status.LOCKED, "collaboration-lock-denied");

		int before = dependencyCount(tasks);
		int anticipatedEdits = intent.operation() == TaskDependencyIntent.Operation.LINK
			? adjacentLinksToCreate(tasks) : intent.dependency() == null ? before : 1;
		EditSupport undoSupport = beginCompoundUndo(project, anticipatedEdits);
		try {
			if (intent.operation() == TaskDependencyIntent.Operation.LINK) {
				service.connect(tasks, eventSource, null);
			} else if (intent.dependency() != null) {
				service.remove(selectedDependency, eventSource, true);
			} else {
				service.removeAnyDependencies(tasks, eventSource);
			}
		} finally {
			if (undoSupport != null)
				undoSupport.endUpdate();
		}
		int after = dependencyCount(tasks);
		boolean changed = intent.operation() == TaskDependencyIntent.Operation.LINK ? after > before : after < before;
		return TaskCommandResult.of(changed ? TaskCommandResult.Status.CHANGED : TaskCommandResult.Status.NO_CHANGE);
	}

	private static int dependencyCount(List<Task> tasks) {
		Set<Dependency> incident = Collections.newSetFromMap(new IdentityHashMap<>());
		for (Task task : tasks)
			incident.addAll(DependencyService.getInstance().getIncidentDependencies(task));
		return incident.size();
	}

	private static int adjacentLinksToCreate(List<Task> tasks) {
		int count = 0;
		for (int index = 0; index + 1 < tasks.size(); index++) {
			Task predecessor = tasks.get(index);
			Task successor = tasks.get(index + 1);
			if (!ClassUtils.isObjectReadOnly(predecessor) && !ClassUtils.isObjectReadOnly(successor)
					&& successor.getPredecessorList().findLeft(predecessor) == null)
				count++;
		}
		return count;
	}

	private static EditSupport beginCompoundUndo(Project project, int editCount) {
		if (editCount <= 1 || project.getUndoController() == null)
			return null;
		EditSupport undoSupport = project.getUndoController().getEditSupport();
		if (undoSupport != null)
			undoSupport.beginUpdate();
		return undoSupport;
	}

	private static PreparedHierarchyEdit prepare(SpreadSheet sheet, TaskHierarchyEditIntent intent) {
		Objects.requireNonNull(sheet, "sheet");
		Objects.requireNonNull(intent, "intent");
		if (intent.tasks().stream().map(ProjectionRowKey.TaskRow::taskKey).distinct().count() != intent.tasks().size())
			return PreparedHierarchyEdit.rejected(new TaskCommandResult(TaskCommandResult.Status.INVALID_INTENT,
				"duplicate-task-identity"));
		if (intent.operation() == TaskHierarchyEditIntent.Operation.RELOCATE
				&& intent.tasks().stream().anyMatch(task -> task.taskKey().equals(intent.anchor().taskKey())))
			return PreparedHierarchyEdit.rejected(new TaskCommandResult(TaskCommandResult.Status.INVALID_INTENT,
				"anchor-is-selected-task"));
		if (!(sheet.getModel() instanceof SpreadSheetModel sheetModel) || sheetModel.getCache() == null)
			return PreparedHierarchyEdit.rejected(new TaskCommandResult(TaskCommandResult.Status.MISSING_TASK,
				"missing-task-projection"));
		NodeModelCache cache = sheetModel.getCache();
		RevisionedProjectionIndex projection = cache.getVisibleNodes().getProjectionIndex();
		if (projection.topologyRevision() != intent.projectionRevision())
			return PreparedHierarchyEdit.rejected(new TaskCommandResult(TaskCommandResult.Status.STALE_PROJECTION,
				"projection-revision-changed"));

		List<GraphicNode> graphicNodes = new ArrayList<>(intent.tasks().size());
		List<Node> nodes = new ArrayList<>(intent.tasks().size());
		for (ProjectionRowKey.TaskRow key : intent.tasks()) {
			int row = projection.rowForKey(key);
			if (row < 0)
				return PreparedHierarchyEdit.rejected(new TaskCommandResult(TaskCommandResult.Status.MISSING_TASK,
					"task-not-visible"));
			GraphicNode graphicNode = projection.nodeAt(row);
			Node node = graphicNode == null ? null : graphicNode.getNode();
			if (node == null || node.isVoid() || !(node.getImpl() instanceof Task))
				return PreparedHierarchyEdit.rejected(new TaskCommandResult(TaskCommandResult.Status.MISSING_TASK,
					"task-not-editable"));
			graphicNodes.add(graphicNode);
			nodes.add(node);
		}

		Node anchor = null;
		if (intent.operation() == TaskHierarchyEditIntent.Operation.RELOCATE) {
			int anchorRow = projection.rowForKey(intent.anchor());
			if (anchorRow < 0)
				return PreparedHierarchyEdit.rejected(new TaskCommandResult(TaskCommandResult.Status.MISSING_TASK,
					"anchor-not-visible"));
			GraphicNode anchorGraphicNode = projection.nodeAt(anchorRow);
			anchor = anchorGraphicNode == null ? null : anchorGraphicNode.getNode();
			if (anchor == null || !(anchor.getImpl() instanceof Task))
				return PreparedHierarchyEdit.rejected(new TaskCommandResult(TaskCommandResult.Status.MISSING_TASK,
					"anchor-not-editable"));
		}

		boolean possible = intent.operation() == TaskHierarchyEditIntent.Operation.MOVE
			? cache.canMoveNodes(graphicNodes, intent.direction())
			: cache.canRelocateNodes(graphicNodes, anchor, intent.after());
		return new PreparedHierarchyEdit(cache, graphicNodes, nodes, anchor, null, possible);
	}

	private record PreparedHierarchyEdit(NodeModelCache cache, List<GraphicNode> graphicNodes, List<Node> nodes,
			Node anchor, TaskCommandResult rejection, boolean possible) {
		private static PreparedHierarchyEdit rejected(TaskCommandResult rejection) {
			return new PreparedHierarchyEdit(null, List.of(), List.of(), null, rejection, false);
		}
	}
}
