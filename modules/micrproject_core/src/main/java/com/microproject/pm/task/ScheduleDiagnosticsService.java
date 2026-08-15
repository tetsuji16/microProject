package com.microproject.pm.task;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.microproject.pm.scheduling.ConstraintType;

/** Explains scheduling state in user-oriented terms without changing the schedule. */
public final class ScheduleDiagnosticsService {
	public enum Severity { INFO, WARNING, ERROR }
	public enum Type {
		INACTIVE, MANUAL, MISSED_DEADLINE, NEGATIVE_SLACK, HARD_CONSTRAINT,
		LATE_INCOMPLETE_WORK, OVERALLOCATED_RESOURCE, NO_ISSUES
	}
	public record Issue(Type type, Severity severity, String summary, String cause, String recommendation) { }

	public List<Issue> diagnose(Task task) {
		Objects.requireNonNull(task, "task");
		List<Issue> result = new ArrayList<>();
		if (task.isInactiveTask()) {
			result.add(new Issue(Type.INACTIVE, Severity.INFO, "Task is inactive",
				"Inactive tasks are retained for comparison but do not drive dependencies or rollups.",
				"Activate the task when this work becomes part of the committed plan."));
		}
		if (task.isManuallyScheduled()) {
			result.add(new Issue(Type.MANUAL, Severity.INFO, "Dates are manually scheduled",
				"Automatic dependency and calendar calculations cannot move this task.",
				"Switch to automatic scheduling if links should determine its dates."));
		}
		if (task.getDeadline() > 0L && task.getEnd() > task.getDeadline() && !task.isInactiveTask()) {
			result.add(new Issue(Type.MISSED_DEADLINE, Severity.ERROR, "Finish is after the deadline",
				"The scheduled finish exceeds the task deadline.",
				"Shorten the work, add capacity, revise links, or move the deadline."));
		}
		if (task.getTotalSlack() < 0L && !task.isInactiveTask()) {
			result.add(new Issue(Type.NEGATIVE_SLACK, Severity.ERROR, "Task has negative total slack",
				"Constraints, dependencies, or resource delays require more time than the plan permits.",
				"Inspect predecessor links and constraints, then remove or relax the limiting condition."));
		}
		if (isHardConstraint(task.getConstraintType()) && !task.isInactiveTask()) {
			result.add(new Issue(Type.HARD_CONSTRAINT, Severity.WARNING, "Task has a date constraint",
				"A non-flexible constraint can override normal dependency scheduling.",
				"Use an ASAP/ALAP constraint when the date is not contractually fixed."));
		}
		Project project = task.getOwningProject();
		long statusDate = project == null ? 0L : project.getStatusDate();
		if (statusDate > 0L && task.getEnd() < statusDate && !task.isComplete() && !task.isInactiveTask()) {
			result.add(new Issue(Type.LATE_INCOMPLETE_WORK, Severity.WARNING, "Incomplete work is before the status date",
				"The task is not complete although its scheduled finish is in the past.",
				"Update actual progress or reschedule the remaining work after the status date."));
		}
		for (var slot : project == null ? List.<com.microproject.pm.resource.TeamPlannerService.Slot>of()
				: new com.microproject.pm.resource.TeamPlannerService().slots(project)) {
			if (slot.task() == task && slot.overallocated()) {
				result.add(new Issue(Type.OVERALLOCATED_RESOURCE, Severity.WARNING, "Assigned resource is overallocated",
					"Concurrent assignments exceed the resource's maximum units.",
					"Reassign the task, change assignment units, or run resource leveling."));
				break;
			}
		}
		if (result.isEmpty()) {
			result.add(new Issue(Type.NO_ISSUES, Severity.INFO, "No scheduling issues detected",
				"The current dates are consistent with the inspected scheduling rules.",
				"No action is required."));
		}
		return List.copyOf(result);
	}

	private static boolean isHardConstraint(int type) {
		return type != ConstraintType.ASAP && type != ConstraintType.ALAP;
	}
}
