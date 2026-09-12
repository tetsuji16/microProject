/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.task;

import java.util.List;
import java.util.Objects;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoableEditSupport;

/** Applies MSP-style manual/automatic scheduling as one undoable model edit. */
public final class TaskModeService {
	public enum Mode { MANUAL, AUTOMATIC }

	public Result apply(List<? extends Task> tasks, Mode mode, UndoableEditSupport edits) {
		Objects.requireNonNull(tasks, "tasks");
		Objects.requireNonNull(mode, "mode");
		List<Task> selected = tasks.stream().filter(Objects::nonNull).map(task -> (Task) task).toList();
		boolean[] before = new boolean[selected.size()];
		for (int i = 0; i < before.length; i++) before[i] = selected.get(i).isManuallyScheduled();
		boolean target = mode == Mode.MANUAL;
		for (Task task : selected) task.setManuallyScheduled(target);
		if (edits != null && !selected.isEmpty()) edits.postEdit(new AbstractUndoableEdit() {
			private static final long serialVersionUID = 1L;
			@Override public void undo() { super.undo(); restore(before); }
			@Override public void redo() { super.redo(); restoreTarget(target); }
			private void restore(boolean[] values) { for (int i = 0; i < selected.size(); i++) selected.get(i).setManuallyScheduled(values[i]); }
			private void restoreTarget(boolean value) { for (Task task : selected) task.setManuallyScheduled(value); }
		});
		return new Result(selected.size(), target);
	}

	public record Result(int affectedCount, boolean manual) { }
}
