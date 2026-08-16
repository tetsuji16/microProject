/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
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
package com.microproject.pm.resource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.AssignmentService;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.pm.scheduling.ConstraintType;
import com.microproject.pm.scheduling.ScheduleService;

/** Model operations used by the interactive Team Planner timeline. */
public final class TeamPlannerService {
	public record Slot(Task task, Resource resource, Assignment assignment, long start, long end,
		double units, boolean overallocated) {
	}

	public List<Slot> slots(Project project) {
		Objects.requireNonNull(project, "project");
		List<Slot> slots = new ArrayList<>();
		for (var iterator = project.getTaskOutlineIterator(); iterator.hasNext();) {
			Task task = (Task) iterator.next();
			if (!(task instanceof NormalTask normalTask) || task.isSummary()) {
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
		slots.sort(Comparator.comparing((Slot value) -> displayName(value.resource()))
			.thenComparingLong(Slot::start).thenComparingLong(value -> value.task().getId()));
		return List.copyOf(slots);
	}

	public void reschedule(Task task, long newStart, Object eventSource) {
		Objects.requireNonNull(task, "task");
		if (task.isReadOnly() || task.inProgress()) {
			throw new IllegalArgumentException("The selected task cannot be rescheduled");
		}
		ScheduleService.getInstance().setConstraint(eventSource, task, ConstraintType.SNET, newStart,
			task.getOwningProject().getUndoController().getEditSupport());
		task.setDirty(true);
		task.getOwningProject().recalculate();
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
		if (resource == null || !resource.isLabor() || candidate.getTask().isInactiveTask()) {
			return false;
		}
		double units = 0D;
		long probe = candidate.getStart();
		for (Object value : resource.getAssignments()) {
			Assignment assignment = (Assignment) value;
			if (assignment.getTask().isInactiveTask()) {
				continue;
			}
			if (assignment.getStart() <= probe && assignment.getEnd() > probe) {
				units += Math.max(0D, assignment.getUnits());
			}
		}
		return units > resource.getMaximumUnits() + 0.000001D;
	}

	public static String displayName(Resource resource) {
		if (resource == null || resource == ResourceImpl.getUnassignedInstance()) {
			return "Unassigned";
		}
		String name = resource.getName();
		return name == null || name.isBlank() ? "Unnamed resource" : name;
	}
}
