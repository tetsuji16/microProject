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
	private final boolean projectDirty;
	private final boolean projectGroupDirty;

	private ProjectTaskSnapshot(Project project) {
		Map<Task, TaskState> result = new LinkedHashMap<>();
		for (Task task : project.getTaskList()) {
			Double override = task instanceof NormalTask normal
				? normal.getImportedPercentCompleteOverride() : null;
			result.put(task, new TaskState(task.backupDetail(), override, task.isDirty(), task.isManuallyScheduled(),
				task.getManualStart(), task.getManualFinish()));
		}
		tasks = Collections.unmodifiableMap(result);
		projectDirty = project.isDirty();
		projectGroupDirty = project.isGroupDirty();
	}

	public static ProjectTaskSnapshot capture(Project project) {
		return new ProjectTaskSnapshot(java.util.Objects.requireNonNull(project, "project"));
	}

	public void restore(Project project, Object eventSource) {
		for (Map.Entry<Task, TaskState> entry : tasks.entrySet()) {
			Task task = entry.getKey();
			TaskState state = entry.getValue();
			task.restoreDetail(eventSource, state.detail(), true);
			task.restoreManualSchedulingState(state.manuallyScheduled(), state.manualStart(), state.manualFinish());
			if (task instanceof NormalTask normal)
				normal.restoreImportedPercentCompleteOverride(state.importedPercentCompleteOverride());
		}
		project.recalculate();
		for (Map.Entry<Task, TaskState> entry : tasks.entrySet())
			entry.getKey().setDirty(entry.getValue().dirty());
		project.setDirty(projectDirty);
		project.setGroupDirty(projectGroupDirty);
	}

	public Map<Task, TaskState> taskStates() { return tasks; }

	public record TaskState(Object detail, Double importedPercentCompleteOverride, boolean dirty,
		boolean manuallyScheduled, long manualStart, long manualFinish) { }
}
