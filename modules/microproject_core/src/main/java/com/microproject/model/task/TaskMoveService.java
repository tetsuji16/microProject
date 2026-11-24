/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.model.task;

import java.util.List;
import java.util.Objects;

import com.microproject.options.CalendarOption;
import com.microproject.model.calendar.WorkCalendar;
import com.microproject.schedule.ConstraintType;

/**
 * Task &gt; Move &gt; forward/back by a number of working days. A manually scheduled task keeps its
 * duration and its start and finish are shifted; an automatically scheduled task gets a
 * Start No Earlier Than constraint at the shifted start, as Microsoft Project does. Summary
 * tasks are skipped because their dates are derived from their subtasks.
 */
public final class TaskMoveService {
	/** Working days of one Move menu step: a day, a week (5 working days) and four weeks. */
	public static final int DAY = 1;
	public static final int WEEK = 5;
	public static final int FOUR_WEEKS = 20;

	/**
	 * @param days working days to move; positive moves later, negative earlier
	 */
	public Result moveByWorkingDays(Project project, List<? extends Task> tasks, int days) {
		Objects.requireNonNull(project, "project");
		Objects.requireNonNull(tasks, "tasks");
		List<Task> selected = tasks.stream().filter(Objects::nonNull).map(task -> (Task) task)
			.filter(task -> !task.isWbsParent() && !task.isInactiveTask()).toList();
		if (days == 0 || selected.isEmpty())
			return new Result(0, ReversibleModelChange.unchanged());
		long delta = (long) days * CalendarOption.getInstance().getMillisPerDay();
		ProjectTaskSnapshot before = ProjectTaskSnapshot.capture(project);
		long[] startsBefore = selected.stream().mapToLong(Task::getStart).toArray();
		int[] constraintsBefore = selected.stream().mapToInt(Task::getConstraintType).toArray();
		try {
			for (Task task : selected) {
				WorkCalendar calendar = task.getEffectiveWorkCalendar();
				long start = task.getStart();
				long newStart = calendar.adjustInsideCalendar(calendar.add(start, delta, false), false);
				if (newStart <= 0L || newStart == start)
					continue;
				if (task.isManuallyScheduled()) {
					long duration = Math.max(0L, calendar.compare(task.getEnd(), start, false));
					task.setManualDates(newStart, calendar.add(newStart, duration, true));
				} else {
					task.setScheduleConstraint(ConstraintType.Kind.SNET, newStart);
				}
			}
			project.recalculate();
		} catch (RuntimeException | Error failure) {
			try {
				restore(project, before, selected, constraintsBefore);
			} catch (RuntimeException | Error compensationFailure) {
				failure.addSuppressed(compensationFailure);
			}
			throw failure;
		}
		int changed = 0;
		for (int i = 0; i < selected.size(); i++)
			if (selected.get(i).getStart() != startsBefore[i])
				changed++;
		if (changed == 0)
			return new Result(0, ReversibleModelChange.unchanged());
		ProjectTaskSnapshot after = ProjectTaskSnapshot.capture(project);
		int[] constraintsAfter = selected.stream().mapToInt(Task::getConstraintType).toArray();
		return new Result(changed, ReversibleModelChange.changed(
			() -> restore(project, before, selected, constraintsBefore),
			() -> restore(project, after, selected, constraintsAfter)));
	}

	/** The task snapshot does not carry the constraint type, so it is put back before the snapshot recalculates. */
	private static void restore(Project project, ProjectTaskSnapshot snapshot, List<Task> tasks, int[] constraintTypes) {
		for (int i = 0; i < tasks.size(); i++)
			tasks.get(i).constraintType = constraintTypes[i];
		snapshot.restore(project, TaskMoveService.class);
	}

	public record Result(int changedCount, ReversibleModelChange change) { }
}
