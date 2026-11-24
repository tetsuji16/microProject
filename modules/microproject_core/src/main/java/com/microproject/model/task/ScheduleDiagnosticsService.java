/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package com.microproject.model.task;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.microproject.model.calendar.WorkCalendar;
import com.microproject.datatype.Duration;
import com.microproject.model.dependency.Dependency;
import com.microproject.model.dependency.DependencyType;
import com.microproject.schedule.ConstraintType;

/** Explains scheduling state in user-oriented terms without changing the schedule. */
public final class ScheduleDiagnosticsService {
	public enum Severity { INFO, WARNING, ERROR }
	public enum Type {
		INACTIVE, MANUAL, DEPENDENCY_CONFLICT, MISSED_DEADLINE, NEGATIVE_SLACK, HARD_CONSTRAINT,
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
		if (hasDependencyConflict(task)) {
			result.add(new Issue(Type.DEPENDENCY_CONFLICT, Severity.ERROR, "Dates conflict with a predecessor",
				"The task starts or finishes before an enabled predecessor link permits.",
				"Switch the task to automatic scheduling, or revise its dates, link, or lag."));
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
		if (isHardConstraint(task.getConstraintTypeKind()) && !task.isInactiveTask()) {
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
		for (var slot : project == null ? List.<com.microproject.model.resource.TeamPlannerService.Slot>of()
				: new com.microproject.model.resource.TeamPlannerService().slots(project)) {
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

	/**
	 * Task Inspector warning: a manually scheduled, active, non-summary task whose dates conflict with an
	 * enabled predecessor link or violate a start/finish date constraint.
	 */
	public static boolean hasWarning(Task task) {
		Objects.requireNonNull(task, "task");
		if (!isInspectable(task))
			return false;
		return hasDependencyConflict(task) || violatesConstraint(task);
	}

	/**
	 * Task Inspector suggestion: a manually scheduled task without a warning whose enabled predecessor links
	 * (and start constraint) would all permit an earlier start than its current one.
	 */
	public static boolean hasSuggestion(Task task) {
		Objects.requireNonNull(task, "task");
		if (!isInspectable(task) || hasWarning(task))
			return false;
		boolean linked = false;
		for (var value : task.getPredecessorList()) {
			Dependency dependency = (Dependency) value;
			if (dependency.isDisabled() || ((Task) dependency.getPredecessor()).isInactiveTask())
				continue;
			linked = true;
			DependencyBoundary boundary = requiredBoundary(dependency);
			long current = boundary.finish() ? task.getEnd() : task.getStart();
			if (boundary.requiredDate() >= current)
				return false;
		}
		if (!linked)
			return false;
		ConstraintType.Kind kind = task.getConstraintTypeKind();
		long date = task.getConstraintDate();
		if ((kind == ConstraintType.Kind.SNET || kind == ConstraintType.Kind.MSO) && date > 0L && date >= task.getStart())
			return false;
		return true;
	}

	/** Warning indicator shown to the user; hidden when the task ignores warnings. */
	public static boolean showsWarning(Task task) {
		return !task.isIgnoreWarnings() && hasWarning(task);
	}

	/** Suggestion indicator shown to the user; hidden when the task ignores warnings. */
	public static boolean showsSuggestion(Task task) {
		return !task.isIgnoreWarnings() && hasSuggestion(task);
	}

	private static boolean isInspectable(Task task) {
		return task.isManuallyScheduled() && !task.isInactiveTask() && !task.isSummary();
	}

	private static boolean violatesConstraint(Task task) {
		ConstraintType.Kind kind = task.getConstraintTypeKind();
		long date = task.getConstraintDate();
		if (kind == null || date <= 0L)
			return false;
		return switch (kind) {
			case SNET, MSO -> task.getStart() < date;
			case FNLT, MFO -> task.getEnd() > date;
			default -> false;
		};
	}

	/** Returns whether any enabled incoming link is contradicted by the task's current dates. */
	public static boolean hasDependencyConflict(Task task) {
		return hasDependencyConflict(task, true);
	}

	/** Checks stored dates for link violations even when automatic scheduling would usually hide them. */
	static boolean hasCurrentDependencyConflict(Task task) {
		return hasDependencyConflict(task, false);
	}

	private static boolean hasDependencyConflict(Task task, boolean requireManualScheduling) {
		Objects.requireNonNull(task, "task");
		for (var value : task.getPredecessorList()) {
			Dependency dependency = (Dependency) value;
			if (violates(dependency, requireManualScheduling))
				return true;
		}
		return false;
	}

	/** Returns the date boundary imposed on a successor by one enabled incoming link. */
	public static DependencyBoundary requiredBoundary(Dependency dependency) {
		Objects.requireNonNull(dependency, "dependency");
		Task predecessor = (Task) dependency.getPredecessor();
		Task successor = (Task) dependency.getSuccessor();
		WorkCalendar calendar = dependency.getEffectiveWorkCalendar();
		return switch (dependency.getDependencyKind()) {
			case FS -> new DependencyBoundary(false, requiredDate(calendar, predecessor.getEnd(), dependency, successor));
			case SS -> new DependencyBoundary(false, requiredDate(calendar, predecessor.getStart(), dependency, successor));
			case FF -> new DependencyBoundary(true, requiredDate(calendar, predecessor.getEnd(), dependency, successor));
			case SF -> new DependencyBoundary(true, requiredDate(calendar, predecessor.getStart(), dependency, successor));
		};
	}

	public record DependencyBoundary(boolean finish, long requiredDate) { }

	private static boolean violates(Dependency dependency, boolean requireManualScheduling) {
		if (dependency.isDisabled())
			return false;
		Task predecessor = (Task) dependency.getPredecessor();
		Task successor = (Task) dependency.getSuccessor();
		// Auto-scheduled tasks are resolved by the scheduling engine; a user-facing
		// conflict is meaningful only when manually entered dates override that result.
		if (requireManualScheduling && !successor.isManuallyScheduled())
			return false;
		if (predecessor.isInactiveTask() || successor.isInactiveTask())
			return false;

		long requiredDate;
		long scheduledDate;
		DependencyBoundary boundary = requiredBoundary(dependency);
		requiredDate = boundary.requiredDate();
		scheduledDate = boundary.finish() ? successor.getEnd() : successor.getStart();
		return scheduledDate < requiredDate;
	}

	private static long requiredDate(WorkCalendar calendar, long predecessorDate, Dependency dependency, Task successor) {
		long lag = dependency.getLeadValue();
		return Duration.millis(lag) == 0L ? predecessorDate
			: calendar.add(predecessorDate, lag, successor.isMilestone());
	}


	private static boolean isHardConstraint(ConstraintType.Kind type) {
		return type != ConstraintType.Kind.ASAP && type != ConstraintType.Kind.ALAP;
	}
}
