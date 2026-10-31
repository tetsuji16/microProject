/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.task;

import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.Map;

/** Captures the project-wide task state needed to roll back a schedule mutation. */
public final class ProjectTaskSnapshot {
	private final Map<Task, TaskState> tasks;
	private final Project.DirtyState projectDirtyState;

	private ProjectTaskSnapshot(Project project) {
		Map<Task, TaskState> result = new LinkedHashMap<>();
		for (Task task : project.getTaskList()) {
			Double override = task instanceof NormalTask normal
				? normal.getImportedPercentCompleteOverride() : null;
			result.put(task, new TaskState(task.backupDetail(), override, task.isInactiveTask(), task.isDirty(),
				task.isManuallyScheduled(), task.getManualStart(), task.getManualFinish(), task.getLevelingDelay(), task.getLevelingManualDateState()));
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
			if (task instanceof NormalTask normal)
				normal.restoreImportedPercentCompleteOverride(state.importedPercentCompleteOverride());
		}
		project.recalculate();
		for (Map.Entry<Task, TaskState> entry : tasks.entrySet())
			entry.getKey().setDirty(entry.getValue().dirty());
		project.restoreDirtyState(projectDirtyState);
	}

	public Map<Task, TaskState> taskStates() { return tasks; }

	public record TaskState(Object detail, Double importedPercentCompleteOverride, boolean inactiveTask, boolean dirty,
		boolean manuallyScheduled, long manualStart, long manualFinish, long levelingDelay,
		Task.LevelingManualDateState levelingManualDateState) { }
}
