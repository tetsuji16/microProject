/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.task;

import java.util.List;
import java.util.Objects;

/** Applies the Ignore Warnings task field and Show Ignored Problems as undoable changes. */
public final class TaskWarningsService {
	/** Sets Ignore Warnings for the selected tasks. */
	public Result setIgnoreWarnings(Project project, List<? extends Task> tasks, boolean ignore) {
		Objects.requireNonNull(project, "project");
		Objects.requireNonNull(tasks, "tasks");
		List<Task> selected = tasks.stream().filter(Objects::nonNull).map(task -> (Task) task).distinct().toList();
		List<Task> projectTasks = project.getTaskList();
		for (Task task : selected) {
			if (task.getOwningProject() != project || task.isReadOnly())
				throw new IllegalArgumentException("Selected tasks must be writable tasks from one project");
			if (!projectTasks.contains(task)) throw new IllegalArgumentException("Selected task is not part of the project");
		}
		return apply(project, selected.stream().filter(task -> task.isIgnoreWarnings() != ignore).toList(), ignore,
			"Ignore warnings");
	}

	/** Clears Ignore Warnings on every task in the project that has it set. */
	public Result showIgnoredProblems(Project project) {
		Objects.requireNonNull(project, "project");
		List<Task> ignored = project.getTaskList().stream()
			.filter(Task::isIgnoreWarnings).toList();
		return apply(project, ignored, false, "Show ignored problems");
	}

	private Result apply(Project project, List<Task> changing, boolean ignore, String label) {
		if (changing.isEmpty()) return new Result(0, ReversibleModelChange.unchanged());
		if (project.isReadOnly()) throw new IllegalStateException("Cannot change warnings in a read-only project");
		ReversibleModelChange change = ProjectTaskMutation.run(project, ProjectTaskSnapshot::capture,
			this, TaskWarningsService.class, label, new ProjectTaskMutation.Operation() {
				@Override public void authorize() {
					if (project.isReadOnly()) throw new IllegalStateException("Cannot change warnings in a read-only project");
				}
				@Override public void apply() {
					for (Task task : changing) task.setIgnoreWarnings(ignore);
				}
				@Override public void schedule() { }
				@Override public void checkInvariants(ProjectTaskSnapshot before) {
					for (Task task : changing)
						if (task.isIgnoreWarnings() != ignore) throw new IllegalStateException("Ignore Warnings did not apply");
				}
			});
		return new Result(changing.size(), change);
	}

	public record Result(int changedCount, ReversibleModelChange change) { }
}
