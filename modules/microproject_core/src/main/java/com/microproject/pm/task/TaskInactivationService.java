/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.task;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** Applies the task-level Active field transition and returns an Undo change. */
public final class TaskInactivationService {
	private final Consumer<Task> afterTaskTransition;

	public TaskInactivationService() {
		this(task -> { });
	}

	TaskInactivationService(Consumer<Task> afterTaskTransition) {
		this.afterTaskTransition = Objects.requireNonNull(afterTaskTransition, "afterTaskTransition");
	}

	public Result inactivate(List<? extends Task> tasks) {
		return setInactive(tasks, true);
	}

	/** Toggles the selected set: all-inactive selections reactivate; all other selections become inactive. */
	public Result toggle(List<? extends Task> tasks) {
		Objects.requireNonNull(tasks, "tasks");
		List<Task> selected = tasks.stream().filter(Objects::nonNull).map(task -> (Task) task).distinct().toList();
		if (selected.isEmpty()) return new Result(0, ReversibleModelChange.unchanged());
		boolean inactive = !selected.stream().allMatch(Task::isInactiveTask);
		return setInactive(selected, inactive);
	}

	/** Sets the Active field for selected tasks and captures one reversible change. */
	public Result setInactive(List<? extends Task> tasks, boolean inactive) {
		Objects.requireNonNull(tasks, "tasks");
		List<Task> selected = tasks.stream().filter(Objects::nonNull).map(task -> (Task) task).distinct().toList();
		if (selected.isEmpty()) return new Result(0, ReversibleModelChange.unchanged());
		Project project = Objects.requireNonNull(selected.get(0).getOwningProject(), "task project");
		for (Task task : selected)
			if (task.getOwningProject() != project || task.isReadOnly())
				throw new IllegalArgumentException("Selected tasks must be writable tasks from one project");
		if (project.isReadOnly()) throw new IllegalStateException("Cannot inactivate tasks in a read-only project");
		List<Task> projectTasks = project.getTaskList();
		for (Task task : selected)
			if (!projectTasks.contains(task)) throw new IllegalArgumentException("Selected task is not part of the project");
		long changed = selected.stream().filter(task -> task.isInactiveTask() != inactive).count();
		if (changed == 0) return new Result(0, ReversibleModelChange.unchanged());

		ReversibleModelChange change = ProjectTaskMutation.run(project, ProjectTaskSnapshot::capture,
			this, TaskInactivationService.class, "Inactivate", new ProjectTaskMutation.Operation() {
				@Override public void authorize() {
					if (project.isReadOnly()) throw new IllegalStateException("Cannot inactivate tasks in a read-only project");
				}
				@Override public void apply() {
					for (Task task : selected) if (task.isInactiveTask() != inactive) {
						task.setInactiveTask(inactive);
						afterTaskTransition.accept(task);
					}
				}
				@Override public void schedule() { project.recalculate(); }
				@Override public void checkInvariants(ProjectTaskSnapshot before) {
					for (Task task : selected)
						if (task.isInactiveTask() != inactive) throw new IllegalStateException("Active state did not apply");
				}
			});
		return new Result((int) changed, change);
	}

	public record Result(int changedCount, ReversibleModelChange change) { }
}
