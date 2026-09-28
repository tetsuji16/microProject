/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.task;

import java.util.List;
import java.util.Objects;

/** Applies MSP-style manual/automatic scheduling and returns its reversible model change. */
public final class TaskModeService {
	public enum Mode { MANUAL, AUTOMATIC }

	public Result apply(List<? extends Task> tasks, Mode mode) {
		Objects.requireNonNull(tasks, "tasks");
		Objects.requireNonNull(mode, "mode");
		List<Task> selected = tasks.stream().filter(Objects::nonNull).map(task -> (Task) task).toList();
		boolean[] before = new boolean[selected.size()];
		for (int i = 0; i < before.length; i++) before[i] = selected.get(i).isManuallyScheduled();
		boolean target = mode == Mode.MANUAL;
		boolean changed = false;
		for (boolean value : before) changed |= value != target;
		for (Task task : selected) task.setManuallyScheduled(target);
		ReversibleModelChange change = changed
			? ReversibleModelChange.changed(() -> restore(selected, before), () -> restoreTarget(selected, target))
			: ReversibleModelChange.unchanged();
		return new Result(selected.size(), target, change);
	}

	private static void restore(List<Task> tasks, boolean[] values) {
		for (int i = 0; i < tasks.size(); i++) tasks.get(i).setManuallyScheduled(values[i]);
	}

	private static void restoreTarget(List<Task> tasks, boolean value) {
		for (Task task : tasks) task.setManuallyScheduled(value);
	}

	public record Result(int affectedCount, boolean manual, ReversibleModelChange change) { }
}
