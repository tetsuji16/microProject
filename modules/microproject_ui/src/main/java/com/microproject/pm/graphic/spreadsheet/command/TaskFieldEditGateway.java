/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.spreadsheet.command;

import java.util.Objects;

import com.microproject.field.FieldParseException;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.pm.graphic.model.cache.ProjectionRowKey;
import com.microproject.pm.graphic.model.cache.RevisionedProjectionIndex;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetModel;
import com.microproject.pm.task.Task;

/** Resolves task edits against the current projection before entering the canonical field/Undo path. */
public final class TaskFieldEditGateway {
	private TaskFieldEditGateway() {
	}

	public static TaskFieldEditResult execute(SpreadSheetModel sheetModel, TaskFieldEditIntent intent,
			Object eventSource) throws FieldParseException {
		Objects.requireNonNull(sheetModel, "sheetModel");
		Objects.requireNonNull(intent, "intent");
		if (sheetModel.getCache() == null)
			return new TaskFieldEditResult(TaskFieldEditResult.Status.MISSING_TASK, "missing-task-projection");

		RevisionedProjectionIndex projection = sheetModel.getCache().getVisibleNodes().getProjectionIndex();
		if (projection.topologyRevision() != intent.projectionRevision())
			return new TaskFieldEditResult(TaskFieldEditResult.Status.STALE_PROJECTION, "projection-revision-changed");

		ProjectionRowKey.TaskRow taskRow = new ProjectionRowKey.TaskRow(intent.taskKey(), intent.occurrence());
		int modelRow = projection.rowForKey(taskRow);
		if (modelRow < 0)
			return new TaskFieldEditResult(TaskFieldEditResult.Status.MISSING_TASK, "task-not-visible");
		Node node = projection.nodeAt(modelRow).getNode();
		if (node == null || node.isVoid() || !(node.getImpl() instanceof Task))
			return new TaskFieldEditResult(TaskFieldEditResult.Status.MISSING_TASK, "task-not-editable");

		Object currentValue = intent.field().getValue(node, sheetModel.getCache().getWalkersModel(),
			sheetModel.getFieldContext());
		if (!Objects.deepEquals(currentValue, intent.expectedValue()))
			return new TaskFieldEditResult(TaskFieldEditResult.Status.STALE_VALUE, "field-value-changed");
		if (intent.value() != null && Objects.deepEquals(currentValue, intent.value()))
			return TaskFieldEditResult.of(TaskFieldEditResult.Status.NO_CHANGE);

		sheetModel.getCache().getModel().setFieldValue(intent.field(), node, eventSource, intent.value(),
			sheetModel.getFieldContext(), NodeModel.NORMAL);
		return TaskFieldEditResult.of(TaskFieldEditResult.Status.CHANGED);
	}
}
