/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.task;

import java.util.List;
import java.util.Objects;

/** Small, deterministic core operations used by the MSP status commands. */
public final class TaskProgressService {
	/** Changes the project status date and returns its reversible model change. */
	public Result setStatusDate(Project project, long date) {
		Objects.requireNonNull(project, "project");
		if (date <= 0L) throw new IllegalArgumentException("Status date must be positive");
		long before = project.isStatusDateSet() ? project.getStatusDate() : 0L;
		project.setStatusDate(date);
		long after = project.getStatusDate();
		return new Result(0, after, statusDateChange(project, before, after));
	}

	/** Clears the explicit status date, matching MSP's NA entry in the dialog. */
	public Result clearStatusDate(Project project) {
		Objects.requireNonNull(project, "project");
		long before = project.isStatusDateSet() ? project.getStatusDate() : 0L;
		project.clearStatusDate();
		return new Result(0, project.getStatusDate(), statusDateChange(project, before, 0L));
	}

	private static ReversibleModelChange statusDateChange(Project project, long before, long after) {
		if (before == after) return ReversibleModelChange.unchanged();
		return ReversibleModelChange.changed(() -> restoreStatusDate(project, before),
			() -> restoreStatusDate(project, after));
	}

	private static void restoreStatusDate(Project project, long value) {
		project.restoreStatusDate(value);
	}

	/** Marks selected tasks on track through the project's status date. */
	public Result markOnTrack(Project project, List<? extends Task> tasks) {
		Objects.requireNonNull(project, "project");
		Objects.requireNonNull(tasks, "tasks");
		long statusDate = project.getStatusDate();
		List<Task> selected = tasks.stream().filter(Objects::nonNull).map(task -> (Task) task)
			.filter(task -> !task.isWbsParent()).toList();
		double[] before = selected.stream().mapToDouble(Task::getPercentComplete).toArray();
		return applyProgressBatch(project, selected, statusDate, task -> {
			long start = task.getStart();
			long end = task.getEnd();
			double target;
			if (end <= start) {
				target = statusDate >= end ? 1D : 0D;
			} else if (statusDate <= start) {
				target = 0D;
			} else if (statusDate >= end) {
				target = 1D;
			} else {
				long scheduledDuration = task.getEffectiveWorkCalendar().compare(end, start, false);
				long scheduledThroughDate = task.getEffectiveWorkCalendar().compare(statusDate, start, false);
				target = scheduledDuration <= 0L ? 0D
					: Math.max(0D, Math.min(1D, (double) scheduledThroughDate / scheduledDuration));
			}
			return target;
		}, before);
	}

	/**
	 * Sets the selected leaf tasks to a fixed percent complete (Task &gt; Schedule
	 * 0/25/50/75/100% Complete). Summary tasks are skipped because their progress
	 * is derived from their subtasks, matching {@link #markOnTrack}.
	 *
	 * @param fraction progress as a fraction in [0, 1]
	 */
	public Result setPercentComplete(Project project, List<? extends Task> tasks, double fraction) {
		Objects.requireNonNull(project, "project");
		Objects.requireNonNull(tasks, "tasks");
		if (Double.isNaN(fraction) || fraction < 0D || fraction > 1D)
			throw new IllegalArgumentException("Percent complete must be within 0..1");
		List<Task> selected = tasks.stream().filter(Objects::nonNull).map(task -> (Task) task)
			.filter(task -> !task.isWbsParent()).toList();
		double[] before = selected.stream().mapToDouble(Task::getPercentComplete).toArray();
		return applyProgressBatch(project, selected, project.isStatusDateSet() ? project.getStatusDate() : 0L,
			task -> fraction, before);
	}

	private static Result applyProgressBatch(Project project, List<Task> selected, long statusDate,
		java.util.function.ToDoubleFunction<Task> targetForTask, double[] before) {
		ProjectTaskSnapshot snapshot = ProjectTaskSnapshot.capture(project);
		Object[] taskDetails = new Object[selected.size()];
		boolean[] taskDirty = new boolean[selected.size()];
		Double[] importedOverrides = new Double[selected.size()];
		for (int i = 0; i < selected.size(); i++) {
			Task task = selected.get(i);
			taskDetails[i] = task.backupDetail();
			taskDirty[i] = task.isDirty();
			if (task instanceof NormalTask normal) importedOverrides[i] = normal.getImportedPercentCompleteOverride();
		}
		int[] changed = { 0 };
		project.runAtomicModelCommand(markApplied -> {
			try {
				for (Task task : selected) {
					double target = targetForTask.applyAsDouble(task);
					if (Double.compare(task.getPercentComplete(), target) != 0) {
						markApplied.run();
						setProgress(task, target);
						changed[0]++;
					}
				}
			} catch (RuntimeException | Error failure) {
				try {
					snapshot.restore(project, TaskProgressService.class);
				} catch (RuntimeException | Error compensationFailure) {
					failure.addSuppressed(compensationFailure);
				}
				for (int i = 0; i < selected.size(); i++) {
					Task task = selected.get(i);
					try {
						task.restoreDetail(TaskProgressService.class, taskDetails[i], true);
						if (task instanceof NormalTask normal) normal.restoreImportedPercentCompleteOverride(importedOverrides[i]);
						task.setDirty(taskDirty[i]);
					} catch (RuntimeException | Error compensationFailure) { failure.addSuppressed(compensationFailure); }
				}
				throw failure;
			}
			return null;
		}, ignored -> changed[0] > 0, ignored -> null);
		double[] after = selected.stream().mapToDouble(Task::getPercentComplete).toArray();
		ReversibleModelChange change = changed[0] == 0 ? ReversibleModelChange.unchanged()
			: ReversibleModelChange.changed(() -> restoreProgress(selected, before),
				() -> restoreProgress(selected, after));
		return new Result(changed[0], statusDate, change);
	}

	private static void restoreProgress(List<Task> tasks, double[] values) {
		for (int i = 0; i < tasks.size(); i++) setProgress(tasks.get(i), values[i]);
	}

	private static void setProgress(Task task, double value) {
		// Keep the explicit value for task-level schedules while also updating
		// actual duration/assignment state in NormalTask for MPO persistence.
		if (task instanceof NormalTask normal)
			normal.setImportedPercentComplete(value);
		else
			task.setPercentComplete(value);
	}

	public record Result(int changedCount, long statusDate, ReversibleModelChange change) { }
}
