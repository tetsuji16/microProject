/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.task;

import java.util.List;
import java.util.Objects;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoableEditSupport;

/** Small, deterministic core operations used by the MSP status commands. */
public final class TaskProgressService {
	/** Changes the project status date and records the change as one edit. */
	public Result setStatusDate(Project project, long date, UndoableEditSupport edits) {
		Objects.requireNonNull(project, "project");
		if (date <= 0L) throw new IllegalArgumentException("Status date must be positive");
		long before = project.isStatusDateSet() ? project.getStatusDate() : 0L;
		project.setStatusDate(date);
		long after = project.getStatusDate();
		postStatusDateEdit(project, before, after, edits);
		return new Result(0, after);
	}

	/** Clears the explicit status date, matching MSP's NA entry in the dialog. */
	public Result clearStatusDate(Project project, UndoableEditSupport edits) {
		Objects.requireNonNull(project, "project");
		long before = project.isStatusDateSet() ? project.getStatusDate() : 0L;
		project.clearStatusDate();
		postStatusDateEdit(project, before, 0L, edits);
		return new Result(0, project.getStatusDate());
	}

	private static void postStatusDateEdit(Project project, long before, long after, UndoableEditSupport edits) {
		if (edits == null || before == after) return;
		edits.postEdit(new AbstractUndoableEdit() {
			private static final long serialVersionUID = 1L;
			@Override public void undo() { super.undo(); restore(before); }
			@Override public void redo() { super.redo(); restore(after); }
			private void restore(long value) { if (value == 0L) project.clearStatusDate(); else project.setStatusDate(value); }
		});
	}

	/** Marks selected tasks on track through the project's status date. */
	public Result markOnTrack(Project project, List<? extends Task> tasks, UndoableEditSupport edits) {
		Objects.requireNonNull(project, "project");
		Objects.requireNonNull(tasks, "tasks");
		long statusDate = project.getStatusDate();
		List<Task> selected = tasks.stream().filter(Objects::nonNull).map(task -> (Task) task)
			.filter(task -> !task.isWbsParent()).toList();
		double[] before = selected.stream().mapToDouble(Task::getPercentComplete).toArray();
		int changed = 0;
		for (Task task : selected) {
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
			if (Double.compare(task.getPercentComplete(), target) != 0) {
				setProgress(task, target);
				changed++;
			}
		}
		if (edits != null && changed > 0) edits.postEdit(new AbstractUndoableEdit() {
			private static final long serialVersionUID = 1L;
			@Override public void undo() { super.undo(); restore(before); }
			@Override public void redo() { super.redo(); restoreCurrent(); }
			private void restore(double[] values) { for (int i = 0; i < selected.size(); i++) setProgress(selected.get(i), values[i]); }
			private void restoreCurrent() { for (Task task : selected) {
				long start = task.getStart(), end = task.getEnd();
				double value;
				if (end <= start) value = statusDate >= end ? 1D : 0D;
				else if (statusDate <= start) value = 0D;
				else if (statusDate >= end) value = 1D;
				else {
					long duration = task.getEffectiveWorkCalendar().compare(end, start, false);
					long elapsed = task.getEffectiveWorkCalendar().compare(statusDate, start, false);
					value = duration <= 0L ? 0D : Math.max(0D, Math.min(1D, (double) elapsed / duration));
				}
				setProgress(task, value);
			} }
		});
		return new Result(changed, statusDate);
	}

	private static void setProgress(Task task, double value) {
		// Keep the explicit value for task-level schedules while also updating
		// actual duration/assignment state in NormalTask for MPO persistence.
		if (task instanceof NormalTask normal)
			normal.setImportedPercentComplete(value);
		else
			task.setPercentComplete(value);
	}

	public record Result(int changedCount, long statusDate) { }
}
