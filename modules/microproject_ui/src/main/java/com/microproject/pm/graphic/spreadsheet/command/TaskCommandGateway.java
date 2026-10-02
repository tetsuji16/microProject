/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.spreadsheet.command;

import java.util.Objects;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import com.microproject.pm.graphic.collaboration.CollaborationHelper;
import com.microproject.grouping.core.Node;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.field.FieldParseException;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.pm.graphic.model.cache.ProjectionRowKey;
import com.microproject.pm.graphic.model.cache.RevisionedProjectionIndex;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetModel;
import com.microproject.pm.task.Task;
import com.microproject.pm.task.Project;

/** Resolves task edits against the current projection before entering the canonical field/Undo path. */
public final class TaskCommandGateway {
	private TaskCommandGateway() {
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
