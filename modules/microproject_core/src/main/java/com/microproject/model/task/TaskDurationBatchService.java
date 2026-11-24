/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.model.task;

import java.util.Objects;

/**
 * Applies duration changes inside a larger project mutation that already owns
 * a whole-project rollback snapshot.
 *
 * <p>This service deliberately does not create its own rollback or undo record.
 * The caller must hold the project's mutation scope, retain the supplied
 * snapshot, and restore that snapshot if any part of the surrounding mutation
 * fails. Dependent tasks are dirtied and recalculated once in {@link #finish()}.
 */
public final class TaskDurationBatchService {
	private final Project project;
	private final ProjectTaskSnapshot rollbackOwner;
	private int appliedCount;
	private boolean finished;

	public TaskDurationBatchService(Project project, ProjectTaskSnapshot rollbackOwner) {
		this.project = Objects.requireNonNull(project, "project");
		this.rollbackOwner = Objects.requireNonNull(rollbackOwner, "rollbackOwner");
		if (!project.getMutationState().hasActiveScopeForCurrentThread())
			throw new IllegalStateException("A duration batch requires an active mutation scope for its project");
		if (project.isReadOnly())
			throw new IllegalStateException("Cannot change durations in a read-only project");
		if (!rollbackOwner.belongsTo(project))
			throw new IllegalArgumentException("Rollback snapshot does not belong to the project");
	}

	/** Applies one task's duration without making a per-task project snapshot. */
	public void apply(NormalTask task, long duration) {
		if (finished) throw new IllegalStateException("Duration batch is already finished");
		Objects.requireNonNull(task, "task");
		if (task.isReadOnly() || task.getOwningProject() != project || !rollbackOwner.owns(project, task)
				|| !containsIdentity(project.getTaskList(), task))
			throw new IllegalArgumentException("Duration batch task must be writable and covered by its rollback snapshot");
		task.applyDurationForBatch(duration);
		appliedCount++;
	}

	private static boolean containsIdentity(Iterable<Task> tasks, Task target) {
		for (Task task : tasks) if (task == target) return true;
		return false;
	}

	/** Marks the project dirty and runs one scheduling pass after all fields have been applied. */
	public int finish() {
		if (finished) throw new IllegalStateException("Duration batch is already finished");
		finished = true;
		if (appliedCount == 0) return 0;
		for (Task task : project.getTaskList()) task.markTaskAsNeedingRecalculation();
		project.recalculate();
		return appliedCount;
	}
}
