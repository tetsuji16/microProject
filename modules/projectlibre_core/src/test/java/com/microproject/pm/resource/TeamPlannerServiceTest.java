package com.microproject.pm.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.microproject.options.CalendarOption;
import com.microproject.pm.assignment.AssignmentService;
import com.microproject.pm.scheduling.ConstraintType;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

class TeamPlannerServiceTest {
	@Test void detectsConcurrentOverloadButExcludesInactiveWork() {
		Fixture fixture = fixture(); NormalTask first = task(fixture.project, "First"); NormalTask second = task(fixture.project, "Second");
		AssignmentService.getInstance().newAssignment(first, fixture.first, 1D, 0L, this);
		AssignmentService.getInstance().newAssignment(second, fixture.first, 1D, 0L, this);
		TeamPlannerService service = new TeamPlannerService();
		assertTrue(service.slots(fixture.project).stream().anyMatch(TeamPlannerService.Slot::overallocated));
		second.setInactiveTask(true);
		assertFalse(service.slots(fixture.project).stream().anyMatch(TeamPlannerService.Slot::overallocated));
	}

	@Test void reschedulesAndReassignsThroughUndoAwareServices() {
		Fixture fixture = fixture(); NormalTask task = task(fixture.project, "Move me");
		var assignment = AssignmentService.getInstance().newAssignment(task, fixture.first, 1D, 0L, this);
		TeamPlannerService service = new TeamPlannerService(); long newStart = fixture.project.getWorkCalendar().add(task.getStart(), day() * 2L, false);
		service.reschedule(task, newStart, this);
		assertEquals(ConstraintType.SNET, task.getConstraintType());
		var replacement = service.reassign(assignment, fixture.second, this);
		assertEquals(fixture.second, replacement.getResource());
	}

	private Fixture fixture() {
		DataFactoryUndoController undo = new DataFactoryUndoController(); ResourcePool pool = ResourcePool.createRourcePool("team-test", undo);
		Project project = Project.createProject(pool, undo); project.initialize(false, false);
		ResourceImpl first = pool.newResourceInstance(); first.setName("Engineer A"); ResourceImpl second = pool.newResourceInstance(); second.setName("Engineer B");
		return new Fixture(project, first, second);
	}
	private NormalTask task(Project project, String name) { NormalTask task = (NormalTask) project.createLocalTaskNode(null).getImpl(); task.setName(name); task.getCurrentSchedule().setStart(project.getStart()); task.setDuration(day()); return task; }
	private long day() { return CalendarOption.getInstance().getMillisPerDay(); }
	private record Fixture(Project project, ResourceImpl first, ResourceImpl second) { }
}
