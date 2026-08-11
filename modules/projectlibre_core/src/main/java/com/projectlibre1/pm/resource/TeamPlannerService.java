package com.projectlibre1.pm.resource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import com.projectlibre1.pm.assignment.Assignment;
import com.projectlibre1.pm.assignment.AssignmentService;
import com.projectlibre1.pm.task.NormalTask;
import com.projectlibre1.pm.task.Project;
import com.projectlibre1.pm.task.Task;
import com.projectlibre1.pm.scheduling.ConstraintType;
import com.projectlibre1.pm.scheduling.ScheduleService;

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
