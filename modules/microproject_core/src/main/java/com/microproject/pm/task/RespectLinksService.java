/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.task;

import com.microproject.pm.dependency.Dependency;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;

/** Moves selected manually scheduled tasks to the dates determined by their incoming links. */
public final class RespectLinksService {
	@FunctionalInterface interface SnapshotCapture { ProjectTaskSnapshot capture(Project project); }
	private final SnapshotCapture snapshotCapture;

	public RespectLinksService() { this(ProjectTaskSnapshot::capture); }
	RespectLinksService(SnapshotCapture snapshotCapture) {
		this.snapshotCapture = Objects.requireNonNull(snapshotCapture, "snapshotCapture");
	}

	public Result apply(Project project, List<? extends Task> selected) {
		Objects.requireNonNull(project, "project");
		Objects.requireNonNull(selected, "selected");
		var projectTasks = Collections.newSetFromMap(new IdentityHashMap<Task, Boolean>());
		projectTasks.addAll(project.getTaskList());
		var unique = Collections.newSetFromMap(new IdentityHashMap<Task, Boolean>());
		List<Task> eligible = new ArrayList<>();
		for (Task task : selected) {
			if (task == null || !projectTasks.contains(task))
				throw new IllegalArgumentException("Every selected task must belong to the project");
			if (!unique.add(task)) throw new IllegalArgumentException("Selected tasks must be unique");
			if (!task.isReadOnly() && !task.isSummary() && hasActivePredecessor(task)
					&& ScheduleDiagnosticsService.hasCurrentDependencyConflict(task)) eligible.add(task);
		}
		if (eligible.isEmpty()) return new Result(0, ReversibleModelChange.unchanged());
		if (project.isReadOnly()) throw new IllegalStateException("Cannot respect links in a read-only project");

		ReversibleModelChange change = ProjectTaskMutation.run(project, snapshotCapture::capture,
			this, RespectLinksService.class, "Respect Links", new ProjectTaskMutation.Operation() {
				@Override public void authorize() {
					if (project.isReadOnly()) throw new IllegalStateException("Cannot respect links in a read-only project");
				}
				@Override public void apply() {
					List<Task> manual = eligible.stream().filter(Task::isManuallyScheduled).toList();
					for (Task task : manual) task.setManuallyScheduledWithoutRecalculation(false);
					for (Task task : eligible) if (!task.isManuallyScheduled()) task.markTaskAsNeedingRecalculation();
					project.recalculate();
					for (Task task : manual) task.setManuallyScheduledWithoutRecalculation(true);
					if (!manual.isEmpty()) project.recalculate();
					for (Task task : eligible) {
						if (ScheduleDiagnosticsService.hasCurrentDependencyConflict(task))
							throw new IllegalStateException("A task constraint conflicts with an incoming dependency: "
								+ task.getName() + " start=" + task.getStart() + " finish=" + task.getEnd()
								+ " constraint=" + task.getConstraintTypeKind());
					}
				}
				@Override public void checkInvariants(ProjectTaskSnapshot before) {
					for (Task task : eligible)
						if (task.isManuallyScheduled() != before.taskStates().get(task).manuallyScheduled())
							throw new IllegalStateException("Task scheduling mode changed");
				}
			});
		return new Result(eligible.size(), change);
	}

	private static boolean hasActivePredecessor(Task task) {
		for (Object value : task.getPredecessorList()) {
			Dependency dependency = (Dependency) value;
			Task predecessor = (Task) dependency.getPredecessor();
			if (!dependency.isDisabled() && !predecessor.isInactiveTask() && !task.isInactiveTask()) return true;
		}
		return false;
	}

	public record Result(int movedCount, ReversibleModelChange change) { }
}
