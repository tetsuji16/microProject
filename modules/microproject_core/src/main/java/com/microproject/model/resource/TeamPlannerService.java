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
package com.microproject.model.resource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;

import com.microproject.model.assignment.Assignment;
import com.microproject.model.assignment.AssignmentService;
import com.microproject.model.task.NormalTask;
import com.microproject.model.task.Project;
import com.microproject.model.task.ProjectHierarchyQueries;
import com.microproject.model.task.Task;
import com.microproject.model.time.HasStartAndEnd;
import com.microproject.schedule.ConstraintType;
import com.microproject.schedule.ScheduleService;
import com.microproject.undo.UndoController;
import static com.microproject.util.IdentityCollections.containsIdentity;

/** Model operations used by the interactive Team Planner timeline. */
public final class TeamPlannerService {
	private static final int MAX_SHIFT_DAYS = 400;
	private static final long ONE_DAY = 24L * 60L * 60L * 1000L;

	public record Slot(Task task, Resource resource, Assignment assignment, long start, long end,
		double units, boolean overallocated) {
	}

	public List<Slot> slots(Project project) {
		List<Slot> slots = collectSlots(project);
		slots.sort(Comparator.comparing((Slot value) -> displayName(value.resource()))
			.thenComparingLong(Slot::start).thenComparingLong(value -> value.task().getId()));
		return List.copyOf(slots);
	}

	/** Returns overallocated tasks in the active project's outline order. */
	public List<Task> overallocatedTasks(Project project) {
		Objects.requireNonNull(project, "project");
		Set<Task> overallocated = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
		for (Slot slot : collectSlots(project)) {
			if (slot.overallocated() && slot.task().getOwningProject() == project)
				overallocated.add(slot.task());
		}
		return ProjectHierarchyQueries.outline(project).stream().filter(overallocated::contains).toList();
	}

	private List<Slot> collectSlots(Project project) {
		Objects.requireNonNull(project, "project");
		List<Project> sourceProjects = projectsFor(project);
		long expectedTaskCount = 0L;
		for (Project sourceProject : sourceProjects) {
			expectedTaskCount += sourceProject.getTaskList().size();
		}
		int initialCapacity = (int) Math.min(expectedTaskCount, Integer.MAX_VALUE - 8L);
		List<Slot> slots = new ArrayList<>(initialCapacity);
		Set<Task> seenTasks = java.util.Collections.newSetFromMap(new IdentityHashMap<Task, Boolean>());
		for (Project sourceProject : sourceProjects) {
			for (Task task : ProjectHierarchyQueries.outline(sourceProject)) {
				if (!seenTasks.add(task) || !(task instanceof NormalTask normalTask) || task.isSummary()) {
					continue;
				}
				for (Object value : normalTask.getAssignments()) {
					Assignment assignment = (Assignment) value;
					Resource resource = assignment.getResource();
					boolean overloaded = isOverallocated(resource, assignment);
					slots.add(new Slot(task, resource, assignment, assignment.getStart(), assignment.getEnd(),
						assignment.getUnits(), overloaded));
				}
			}
		}
		return slots;
	}

	/** Returns the current project plus every document connected to its resource pool. */
	private static List<Project> projectsFor(Project project) {
		ResourcePool pool = project.getResourcePool();
		if (pool == null)
			return List.of(project);
		List<Project> poolProjects = pool.getProjects();
		if (poolProjects.isEmpty()) return List.of(project);
		List<Project> projects = new ArrayList<Project>(poolProjects.size() + 1);
		projects.add(project);
		for (Project candidate : poolProjects)
			if (candidate != null && !containsIdentity(projects, candidate))
				projects.add(candidate);
		return projects;
	}


	public void reschedule(Task task, long newStart, Object eventSource) {
		Objects.requireNonNull(task, "task");
		if (task.isReadOnly() || task.inProgress()) {
			throw new IllegalArgumentException("The selected task cannot be rescheduled");
		}
		ScheduleService.getInstance().setConstraint(eventSource, task, ConstraintType.Kind.SNET, newStart,
			task.getOwningProject().getUndoController().getEditSupport());
		task.setDirty(true);
		task.getOwningProject().recalculate();
	}

	/** Whether any assignment of the task is overallocated at its current schedule. */
	public boolean isOverallocated(Task task) {
		Objects.requireNonNull(task, "task");
		if (!(task instanceof NormalTask normalTask))
			return false;
		for (Object value : normalTask.getAssignments()) {
			Assignment assignment = (Assignment) value;
			if (isOverallocated(assignment.getResource(), assignment))
				return true;
		}
		return false;
	}

	/**
	 * Reschedules like {@link #reschedule}, then moves the task later one day at a time until none
	 * of its assignments is overallocated. When no free slot exists within {@code MAX_SHIFT_DAYS}
	 * the original constraint and start are restored and an {@link IllegalArgumentException} is thrown.
	 */
	public void reschedulePreventingOverallocation(Task task, long newStart, Object eventSource) {
		reschedulePreventingOverallocation(task, newStart, () -> isOverallocated(task), eventSource);
	}

	private void reschedulePreventingOverallocation(Task task, long newStart, BooleanSupplier conflict,
			Object eventSource) {
		int originalConstraint = task.getConstraintType();
		long originalConstraintDate = task.getConstraintDate();
		long candidate = newStart;
		for (int day = 0; day <= MAX_SHIFT_DAYS; day++) {
			reschedule(task, candidate, eventSource);
			if (!conflict.getAsBoolean())
				return;
			candidate += ONE_DAY;
		}
		// Restore the original constraint (an ASAP task must not stay SNET), then the original start.
		ScheduleService.getInstance().setConstraint(eventSource, task, originalConstraint, originalConstraintDate,
			task.getOwningProject().getUndoController().getEditSupport());
		task.setDirty(true);
		task.getOwningProject().recalculate();
		throw new IllegalArgumentException("No slot without overallocation was found for the task");
	}

	/** Tasks without any resource assignment, in outline order of the active project. */
	public List<Task> unassignedTasks(Project project) {
		Objects.requireNonNull(project, "project");
		List<Task> result = new ArrayList<>();
		for (Task task : ProjectHierarchyQueries.outline(project)) {
			if (task instanceof NormalTask normalTask && !task.isSummary() && !task.isInactiveTask()
				&& normalTask.getAssignments().isEmpty())
				result.add(task);
		}
		return List.copyOf(result);
	}

	/** Manually scheduled tasks that have no start or finish yet. */
	public List<Task> unscheduledTasks(Project project) {
		Objects.requireNonNull(project, "project");
		List<Task> result = new ArrayList<>();
		for (Task task : ProjectHierarchyQueries.outline(project)) {
			if (!task.isSummary() && task.isManuallyScheduled() && (task.getStart() <= 0L || task.getEnd() <= 0L))
				result.add(task);
		}
		return List.copyOf(result);
	}

	/**
	 * Applies one Team Planner drag as a single undo step: an optional reassignment to {@code target}
	 * ({@code null} keeps the resource), then an optional reschedule to {@code newStart} ({@code null}
	 * keeps the dates). With {@code preventOverallocation} the task moves later until it no longer
	 * overallocates; a resource-only drag is rescheduled only when the new resource is overallocated.
	 * When the drag cannot be applied every edit is undone, so the original assignment, task detail and
	 * constraint come back, and the {@link IllegalArgumentException} is rethrown.
	 *
	 * @return the assignment that now carries the dragged work
	 */
	public Assignment applyDrag(Assignment assignment, Resource target, Long newStart, boolean preventOverallocation,
			Object eventSource) {
		Objects.requireNonNull(assignment, "assignment");
		Task task = assignment.getTask();
		UndoController undo = task.getOwningProject().getUndoController();
		// A bounded slot search can post hundreds of edits; keep them all so rollback and the single-step
		// merge never depend on edits the undo limit would otherwise evict.
		int limit = undo.getLimit();
		undo.setLimit(-1);
		UndoController.Session session = undo.beginSession();
		UndoController.enterDialogEditScope();
		boolean applied = false;
		try {
			Assignment result = target == null || target == assignment.getResource()
				? assignment : reassign(assignment, target, eventSource);
			if (newStart != null) {
				if (preventOverallocation) reschedulePreventingOverallocation(task, newStart, eventSource);
				else reschedule(task, newStart, eventSource);
			} else if (result != assignment && preventOverallocation && isOverallocated(result.getResource(), result)) {
				// Only the moved assignment's destination decides a resource-only drag; other resources of
				// the task that are already overallocated must not move its dates.
				reschedulePreventingOverallocation(task, task.getStart(),
					() -> isOverallocated(result.getResource(), result), eventSource);
			}
			applied = true;
			return result;
		} finally {
			UndoController.exitDialogEditScope();
			try {
				if (applied) {
					session.commit();
				} else {
					session.rollback();
					task.setDirty(true);
					task.getOwningProject().recalculate();
				}
			} finally {
				undo.setLimit(limit);
			}
		}
	}

	public Assignment reassign(Assignment assignment, Resource target, Object eventSource) {
		Objects.requireNonNull(assignment, "assignment");
		Task task = assignment.getTask();
		if (!(task instanceof NormalTask) || task.isReadOnly()) {
			throw new IllegalArgumentException("The selected assignment cannot be changed");
		}
		Resource normalizedTarget = target == null ? ResourceImpl.getUnassignedInstance() : target;
		if (normalizedTarget.equals(assignment.getResource())) {
			return assignment;
		}
		double units = assignment.getUnits();
		long delay = assignment.getDelay();
		AssignmentService service = AssignmentService.getInstance();
		service.remove(assignment, eventSource, true);
		Assignment replacement = service.newAssignment((NormalTask) task, normalizedTarget, units, delay, eventSource);
		task.setDirty(true);
		task.getOwningProject().recalculate();
		return replacement;
	}

	private static boolean isOverallocated(Resource resource, Assignment candidate) {
		if (resource == null || !resource.isLabor() || candidate.getTask().isInactiveTask()) return false;
		// Capacity changes at assignment starts and after split gaps. Probe each working interval.
		long from = candidate.getStart();
		long to = candidate.getEnd();
		List<Long> probes = new ArrayList<>();
		addWorkingStarts(candidate, probes, from, to);
		for (Object value : resource.getAssignments()) {
			Assignment assignment = (Assignment) value;
			if (!assignment.getTask().isInactiveTask()) addWorkingStarts(assignment, probes, from, to);
		}
		for (long probe : probes) {
			if (!assignmentCovers(candidate, probe)) continue;
			double units = 0D;
			for (Object value : resource.getAssignments()) {
				Assignment assignment = (Assignment) value;
				if (assignmentCovers(assignment, probe)) units += Math.max(0D, assignment.getUnits());
			}
			if (units > resource.getMaximumUnits() + 0.000001D) return true;
		}
		return false;
	}

	/**
	 * Checks whether a resource is overallocated at its current schedule.
	 * Returns true if the total units assigned to the resource at any time point
	 * exceeds its maximum units.
	 */
	public static boolean isOverallocated(Resource resource) {
		Objects.requireNonNull(resource, "resource");
		if (!resource.isLabor()) return false;
		// Probe at every working interval start, including the end of an assignment split gap.
		List<Long> probes = new ArrayList<>();
		for (Object value : resource.getAssignments()) {
			Assignment assignment = (Assignment) value;
			if (!assignment.getTask().isInactiveTask()) addWorkingStarts(assignment, probes);
		}
		if (probes.isEmpty()) return false;
		for (long probe : probes) {
			double units = 0D;
			for (Object value : resource.getAssignments()) {
				Assignment assignment = (Assignment) value;
				if (assignmentCovers(assignment, probe)) units += Math.max(0D, assignment.getUnits());
			}
			if (units > resource.getMaximumUnits() + 0.000001D) return true;
		}
		return false;
	}

	private static void addWorkingStarts(Assignment assignment, List<Long> probes) {
		assignment.forEachWorkingInterval(value -> {
			if (value instanceof HasStartAndEnd interval && interval.getEnd() > interval.getStart())
				probes.add(interval.getStart());
		}, true, assignment.getEffectiveWorkCalendar());
	}

	private static void addWorkingStarts(Assignment assignment, List<Long> probes, long from, long to) {
		assignment.forEachWorkingInterval(value -> {
			if (value instanceof HasStartAndEnd interval && interval.getEnd() > interval.getStart()
					&& interval.getStart() >= from && interval.getStart() < to)
				probes.add(interval.getStart());
		}, true, assignment.getEffectiveWorkCalendar());
	}

	private static boolean assignmentCovers(Assignment assignment, long time) {
		if (assignment.getTask().isInactiveTask()) return false;
		final boolean[] covers = { false };
		assignment.forEachWorkingInterval(value -> {
			if (value instanceof HasStartAndEnd interval && interval.getStart() <= time && interval.getEnd() > time)
				covers[0] = true;
		}, true, assignment.getEffectiveWorkCalendar());
		return covers[0];
	}

	    public static String displayName(Resource resource) {
		if (resource == null || resource == ResourceImpl.getUnassignedInstance()) {
			return "Unassigned";
		}
		String name = resource.getName();
		return name == null || name.isBlank() ? "Unnamed resource" : name;
	}
}
