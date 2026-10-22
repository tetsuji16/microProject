/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.task;

import java.util.ArrayList;
import java.util.List;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

import com.microproject.transaction.ProjectMutationState;

/** Applies MSP-style manual/automatic scheduling and returns its reversible model change. */
public final class TaskModeService {
	public enum Mode { MANUAL, AUTOMATIC }

	public Result apply(List<? extends Task> tasks, Mode mode) {
		Objects.requireNonNull(tasks, "tasks");
		Objects.requireNonNull(mode, "mode");
		List<Task> selected = tasks.stream().filter(Objects::nonNull).map(task -> (Task) task).toList();
		boolean[] before = new boolean[selected.size()];
		long[] manualStarts = new long[selected.size()];
		long[] manualFinishes = new long[selected.size()];
		Map<Project, ProjectTaskSnapshot> projectSnapshots = new IdentityHashMap<>();
		for (int i = 0; i < before.length; i++) {
			Task task = selected.get(i);
			Project project = task.getProject();
			if (project != null) projectSnapshots.computeIfAbsent(project, ProjectTaskSnapshot::capture);
			before[i] = task.isManuallyScheduled();
			manualStarts[i] = task.manualStart;
			manualFinishes[i] = task.manualFinish;
		}
		boolean target = mode == Mode.MANUAL;
		boolean changed = false;
		for (boolean value : before) changed |= value != target;
		List<Project> projects = List.copyOf(projectSnapshots.keySet());
		Map<Project, ProjectMutationState.Scope> mutations = new IdentityHashMap<>();
		for (Project project : projects) {
			mutations.put(project, project.getMutationState().begin());
			project.beginEventBatch();
		}
		int attempted = 0;
		try {
			for (Task task : selected) {
				attempted++;
				task.setManuallyScheduled(target);
			}
		} catch (RuntimeException | Error failure) {
			for (Map.Entry<Project, ProjectTaskSnapshot> entry : projectSnapshots.entrySet()) {
				try {
					entry.getValue().restore(entry.getKey(), this);
				} catch (RuntimeException | Error compensationFailure) {
					failure.addSuppressed(compensationFailure);
				}
			}
			for (int i = attempted - 1; i >= 0; i--) {
				Task task = selected.get(i);
				try {
					task.restoreManualSchedulingState(before[i], manualStarts[i], manualFinishes[i]);
				} catch (RuntimeException | Error compensationFailure) {
					failure.addSuppressed(compensationFailure);
				}
			}
			for (int i = projects.size() - 1; i >= 0; i--) {
				Project project = projects.get(i);
				try {
					project.endEventBatch(false);
				} catch (RuntimeException | Error compensationFailure) {
					failure.addSuppressed(compensationFailure);
				}
				try {
					mutations.get(project).commit(false);
					mutations.get(project).close();
				} catch (RuntimeException | Error compensationFailure) {
					failure.addSuppressed(compensationFailure);
				}
			}
			throw failure;
		}
		List<Project.PendingEventBatch> pendingEvents = new ArrayList<>();
		for (int i = projects.size() - 1; i >= 0; i--) {
			Project project = projects.get(i);
			boolean projectChanged = false;
			for (int taskIndex = 0; taskIndex < selected.size(); taskIndex++)
				if (selected.get(taskIndex).getProject() == project && before[taskIndex] != target) projectChanged = true;
			pendingEvents.add(project.commitEventBatch(mutations.get(project), projectChanged));
		}
		for (int i = 0; i < pendingEvents.size(); i++)
			projects.get(projects.size() - 1 - i).publishEventBatch(pendingEvents.get(i));
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
