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
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;

/** Model operations used by the interactive Team Planner timeline. */
public final class TeamPlannerService {
	public record Slot(Task task, String taskName, Resource resource, Assignment assignment, long start, long end,
		double units, boolean overallocated, long domainRevision) {
	}

	public List<Slot> slots(Project project) {
		Objects.requireNonNull(project, "project");
		return project.getDomainChangeJournal().read(() -> slotsLocked(project));
	}

	private List<Slot> slotsLocked(Project project) {
		List<Slot> slots = new ArrayList<>();
		long revision = project.getDomainChangeJournal().revision();
		for (var iterator = project.getTaskOutlineIterator(); iterator.hasNext();) {
			Task task = (Task) iterator.next();
			if (!(task instanceof NormalTask normalTask) || task.isSummary()) {
				continue;
			}
			for (Object value : normalTask.getAssignments()) {
				Assignment assignment = (Assignment) value;
				Resource resource = assignment.getResource();
				boolean overloaded = isOverallocated(resource, assignment);
				slots.add(new Slot(task, task.getName(), resource, assignment, assignment.getStart(), assignment.getEnd(),
					assignment.getUnits(), overloaded, revision));
			}
		}
		slots.sort(Comparator.comparing((Slot value) -> displayName(value.resource()))
			.thenComparingLong(Slot::start).thenComparingLong(value -> value.task().getId()));
		return List.copyOf(slots);
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
