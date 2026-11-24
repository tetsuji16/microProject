/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.model.task;

import java.util.IdentityHashMap;
import java.util.Collections;
import java.util.Map;

/** Captures the project-wide task state needed to roll back a schedule mutation. */
public final class ProjectTaskSnapshot {
	private final Project project;
	private final Map<Task, TaskState> tasks;
	private final Project.DirtyState projectDirtyState;

	private ProjectTaskSnapshot(Project project) {
		this.project = project;
		Map<Task, TaskState> result = new IdentityHashMap<>();
		for (Task task : project.getTaskList()) {
			Double override = task instanceof NormalTask normal
				? normal.getImportedPercentCompleteOverride() : null;
			result.put(task, new TaskState(task.backupDetail(), override, task.isInactiveTask(), task.isDirty(),
				task.isManuallyScheduled(), task.getManualStart(), task.getManualFinish(), task.getLevelingDelay(), task.getLevelingManualDateState(), task.isIgnoreWarnings(),
				task.durationPinnedByScheduleEdit));
		}
		tasks = Collections.unmodifiableMap(result);
		projectDirtyState = project.captureDirtyState();
	}

	public static ProjectTaskSnapshot capture(Project project) {
		return new ProjectTaskSnapshot(java.util.Objects.requireNonNull(project, "project"));
	}

	public void restore(Project project, Object eventSource) {
		for (Map.Entry<Task, TaskState> entry : tasks.entrySet()) {
			Task task = entry.getKey();
			TaskState state = entry.getValue();
			if (task.isInactiveTask() != state.inactiveTask()) task.setInactiveTask(state.inactiveTask());
			task.restoreDetail(eventSource, state.detail(), true);
			task.restoreManualSchedulingState(state.manuallyScheduled(), state.manualStart(), state.manualFinish());
			task.setLevelingDelay(state.levelingDelay());
			task.setLevelingManualDateState(state.levelingManualDateState());
			task.setIgnoreWarnings(state.ignoreWarnings());
			task.durationPinnedByScheduleEdit = state.durationPinnedByScheduleEdit();
			if (task instanceof NormalTask normal)
				normal.restoreImportedPercentCompleteOverride(state.importedPercentCompleteOverride());
		}
		project.recalculate();
		for (Map.Entry<Task, TaskState> entry : tasks.entrySet())
			entry.getKey().setDirty(entry.getValue().dirty());
		project.restoreDirtyState(projectDirtyState);
	}

	public Map<Task, TaskState> taskStates() { return tasks; }

	/** Whether this snapshot belongs to the specified project. */
	public boolean belongsTo(Project owner) {
		return project == owner;
	}

	/** Whether this snapshot can roll back the supplied task in the specified project. */
	public boolean owns(Project owner, Task task) {
		return project == owner && tasks.containsKey(task);
	}

	public record TaskState(Object detail, Double importedPercentCompleteOverride, boolean inactiveTask, boolean dirty,
		boolean manuallyScheduled, long manualStart, long manualFinish, long levelingDelay,
		Task.LevelingManualDateState levelingManualDateState, boolean ignoreWarnings,
		boolean durationPinnedByScheduleEdit) { }
}
