/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.task;

import com.microproject.pm.dependency.Dependency;
import com.microproject.transaction.ModelTransaction;
import com.microproject.transaction.ProjectMutationState;
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

		ProjectTaskSnapshot[] before = new ProjectTaskSnapshot[1];
		ProjectTaskSnapshot[] after = new ProjectTaskSnapshot[1];
		try (ProjectMutationState.Scope mutation = project.getMutationState().begin()) {
			project.beginEventBatch();
			try {
				ModelTransaction.execute(new ModelTransaction.Steps<ProjectTaskSnapshot, Integer>() {
					@Override public void validate() { }
					@Override public void authorize() {
						if (project.isReadOnly()) throw new IllegalStateException("Cannot respect links in a read-only project");
					}
					@Override public ProjectTaskSnapshot capture() {
						before[0] = snapshotCapture.capture(project);
						return before[0];
					}
					@Override public ModelTransaction.Mutation<Integer> apply() {
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
						return ModelTransaction.Mutation.changed(eligible.size());
					}
					@Override public void schedule() { }
					@Override public void checkInvariants() {
						for (Task task : eligible)
							if (task.isManuallyScheduled() != before[0].taskStates().get(task).manuallyScheduled())
								throw new IllegalStateException("Task scheduling mode changed");
						after[0] = snapshotCapture.capture(project);
					}
					@Override public void restore(ProjectTaskSnapshot snapshot) { snapshot.restore(project, RespectLinksService.this); }
					@Override public void commit(Integer count) { }
				});
				Project.PendingEventBatch pending = project.commitEventBatch(mutation, true);
				project.publishEventBatch(pending);
			} catch (ModelTransaction.ModelTransactionException failure) {
				try { project.endEventBatch(false); }
				catch (Throwable closeFailure) { failure.addSuppressed(closeFailure); }
				throw new IllegalStateException("Respect Links failed during " + failure.failedStage() + ": "
					+ (failure.getCause() == null ? "unknown" : failure.getCause().getMessage()), failure);
			} catch (RuntimeException | Error failure) {
				try { project.endEventBatch(false); }
				catch (Throwable closeFailure) { failure.addSuppressed(closeFailure); }
				throw failure;
			}
		}
		return new Result(eligible.size(), ReversibleModelChange.changed(
			() -> restore(project, before[0]), () -> restore(project, after[0])));
	}

	private static boolean hasActivePredecessor(Task task) {
		for (Object value : task.getPredecessorList()) {
			Dependency dependency = (Dependency) value;
			Task predecessor = (Task) dependency.getPredecessor();
			if (!dependency.isDisabled() && !predecessor.isInactiveTask() && !task.isInactiveTask()) return true;
		}
		return false;
	}

	private static void restore(Project project, ProjectTaskSnapshot snapshot) {
		try (ProjectMutationState.Scope mutation = project.getMutationState().begin()) {
			project.beginEventBatch();
			Project.PendingEventBatch pending;
			try {
				snapshot.restore(project, RespectLinksService.class);
				project.setDirty(true);
				pending = project.commitEventBatch(mutation, true);
			} catch (RuntimeException | Error failure) {
				try { project.endEventBatch(false); }
				catch (Throwable closeFailure) { failure.addSuppressed(closeFailure); }
				throw failure;
			}
			project.publishEventBatch(pending);
		}
	}

	public record Result(int movedCount, ReversibleModelChange change) { }
}
