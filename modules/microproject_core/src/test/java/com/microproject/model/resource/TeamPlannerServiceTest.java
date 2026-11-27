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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.microproject.options.CalendarOption;
import com.microproject.model.assignment.AssignmentService;
import com.microproject.schedule.ConstraintType;
import com.microproject.model.task.NormalTask;
import com.microproject.model.task.Project;
import com.microproject.undo.DataFactoryUndoController;

class TeamPlannerServiceTest {
	@Test void detectsConcurrentOverloadButExcludesInactiveWork() {
		Fixture fixture = fixture(); NormalTask first = task(fixture.project, "First"); NormalTask second = task(fixture.project, "Second");
		AssignmentService.getInstance().newAssignment(first, fixture.first, 1D, 0L, this);
		AssignmentService.getInstance().newAssignment(second, fixture.first, 1D, 0L, this);
		TeamPlannerService service = new TeamPlannerService();
		assertTrue(service.slots(fixture.project).stream().anyMatch(TeamPlannerService.Slot::overallocated));
		assertEquals(List.of(first, second), service.overallocatedTasks(fixture.project),
			"navigation candidates must follow the active project's outline order");
		second.setInactiveTask(true);
		assertFalse(service.slots(fixture.project).stream().anyMatch(TeamPlannerService.Slot::overallocated));
		assertTrue(service.overallocatedTasks(fixture.project).isEmpty(),
			"inactive work must not remain a Next Overallocation target");
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

	@Test void resourceOnlyDragToAFreeResourceKeepsTheScheduleUnderPreventOverallocation() {
		Fixture fixture = fixture(); NormalTask task = task(fixture.project, "Hand over");
		var assignment = AssignmentService.getInstance().newAssignment(task, fixture.first, 1D, 0L, this);
		int constraint = task.getConstraintType(); long start = task.getStart();
		var moved = new TeamPlannerService().applyDrag(assignment, fixture.second, null, true, this);
		assertEquals(fixture.second, moved.getResource());
		assertEquals(constraint, task.getConstraintType(), "no conflict, so the task must not be pinned with SNET");
		assertEquals(start, task.getStart());
	}

	@Test void resourceOnlyDragIgnoresAnotherResourceThatIsAlreadyOverallocated() {
		Fixture fixture = fixture(); NormalTask task = task(fixture.project, "Two resources"); NormalTask other = task(fixture.project, "Other");
		ResourceImpl busy = fixture.project.getResourcePool().newResourceInstance(); busy.setName("Engineer C");
		ResourceImpl free = fixture.project.getResourcePool().newResourceInstance(); free.setName("Engineer D");
		var moving = AssignmentService.getInstance().newAssignment(task, fixture.first, 1D, 0L, this);
		AssignmentService.getInstance().newAssignment(task, busy, 1D, 0L, this);
		AssignmentService.getInstance().newAssignment(other, busy, 1D, 0L, this);
		TeamPlannerService service = new TeamPlannerService();
		assertTrue(service.isOverallocated(task), "fixture: C is overallocated by the other task");
		int constraint = task.getConstraintType(); long start = task.getStart();
		var moved = service.applyDrag(moving, free, null, true, this);
		assertEquals(free, moved.getResource());
		assertEquals(constraint, task.getConstraintType(), "the free destination must not move the task's dates");
		assertEquals(start, task.getStart());
	}

	@Test void rejectedDragWithAnExhaustiveSearchRestoresEverythingBeyondTheUndoLimit() {
		Fixture fixture = fixture(); NormalTask task = task(fixture.project, "Move me");
		// A 100% assignment always overloads a 50% resource, so the slot search runs all 401 candidates.
		fixture.second.setMaximumUnits(0.5D);
		var assignment = AssignmentService.getInstance().newAssignment(task, fixture.first, 1D, 0L, this);
		int constraint = task.getConstraintType(); long start = task.getStart();
		int limit = fixture.project.getUndoController().getLimit();
		assertThrows(IllegalArgumentException.class,
			() -> new TeamPlannerService().applyDrag(assignment, fixture.second, null, true, this));
		assertSame(assignment, task.findAssignment(fixture.first), "the reassignment must be undone even after 400 candidates");
		assertEquals(null, task.findAssignment(fixture.second));
		assertEquals(constraint, task.getConstraintType());
		assertEquals(start, task.getStart());
		assertEquals(limit, fixture.project.getUndoController().getLimit(), "the undo limit must be restored");
	}

	@Test void failedDragRestoresTheOriginalAssignmentAndConstraint() {
		Fixture fixture = fixture(); NormalTask task = task(fixture.project, "In progress");
		var assignment = AssignmentService.getInstance().newAssignment(task, fixture.first, 0.5D, 0L, this);
		task.setPercentComplete(0.5D);
		int constraint = task.getConstraintType();
		long newStart = fixture.project.getWorkCalendar().add(task.getStart(), day() * 2L, false);
		assertThrows(IllegalArgumentException.class,
			() -> new TeamPlannerService().applyDrag(assignment, fixture.second, newStart, false, this));
		assertSame(assignment, task.findAssignment(fixture.first), "the original assignment object must come back");
		assertSame(assignment, fixture.first.findAssignment(task));
		assertEquals(null, task.findAssignment(fixture.second));
		assertEquals(0.5D, assignment.getUnits(), 1e-9);
		assertEquals(constraint, task.getConstraintType());
	}

	@Test void sharedPoolOverloadIncludesAssignmentsFromAnotherProject() {
		Fixture owner = fixture();
		Fixture sharer = fixture();
		owner.first.setUniqueId(9001L);
		sharer.first.setUniqueId(9001L);
		NormalTask ownerTask = task(owner.project, "Owner task");
		NormalTask sharerTask = task(sharer.project, "Sharer task");
		sharerTask.setStart(ownerTask.getStart());
		AssignmentService.getInstance().newAssignment(ownerTask, owner.first, 1D, 0L, this);
		var sharerAssignment = AssignmentService.getInstance().newAssignment(sharerTask, sharer.first, 1D, 0L, this);
		assertEquals(1, owner.first.getAssignments().size());
		assertEquals(1, sharer.first.getAssignments().size());

		SharedResourcePoolService.getInstance().share(sharer.project, owner.project,
			SharedResourcePoolService.ConflictPolicy.POOL_TAKES_PRECEDENCE);

		var slots = new TeamPlannerService().slots(owner.project);
		assertEquals(2, slots.size(), "The pool view must include each connected client's assignment");
		assertSame(owner.first, sharerAssignment.getResource());
		assertEquals(2, owner.first.getAssignments().size(), "the canonical resource keeps both project assignments");
		assertTrue(slots.stream().allMatch(TeamPlannerService.Slot::overallocated), slots.toString());
		assertTrue(slots.stream().anyMatch(slot -> slot.task() == ownerTask));
		assertTrue(slots.stream().anyMatch(slot -> slot.task() == sharerTask));
	}

	@Test void overlapAfterTheAssignmentStartIsStillAConflict() {
		Fixture fixture = fixture(); NormalTask early = task(fixture.project, "Early"); NormalTask late = task(fixture.project, "Late");
		late.setStart(early.getStart() + 3_600_000L);
		AssignmentService.getInstance().newAssignment(early, fixture.first, 1D, 0L, this);
		AssignmentService.getInstance().newAssignment(late, fixture.first, 1D, 0L, this);
		assertTrue(new TeamPlannerService().slots(fixture.project).stream()
			.filter(slot -> slot.task() == early).allMatch(TeamPlannerService.Slot::overallocated),
			"the earlier assignment overlaps the later one after its own start");
	}

	@ParameterizedTest
	@ValueSource(strings = {"2026-11-23", "2026-11-24", "2026-11-25", "2026-11-26", "2026-11-27"})
	void splitAssignmentGapDoesNotCountAsResourceOverload(String projectStartDate) {
		Fixture fixture = fixture();
		long projectStart = CalendarOption.getInstance().makeValidStart(
			LocalDate.parse(projectStartDate).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(), true);
		fixture.project.setStart(projectStart); fixture.project.setStartConstraint(projectStart);
		NormalTask splitTask = task(fixture.project, "Split");
		splitTask.setDuration(day() * 5L);
		var split = AssignmentService.getInstance().newAssignment(splitTask, fixture.first, 0.75D, 0L, this);
		long gapStart = split.getStart() + 2L * 3_600_000L;
		long gapEnd = fixture.project.getWorkCalendar().add(gapStart, day() * 2L, false);
		split.split(this, gapStart, gapEnd);
		NormalTask gapTask = task(fixture.project, "Only in gap");
		gapTask.setStart(gapStart);
		gapTask.setDuration(day() - 2L * 3_600_000L);
		AssignmentService.getInstance().newAssignment(gapTask, fixture.first, 0.75D, 0L, this);

		assertFalse(TeamPlannerService.isOverallocated(fixture.first),
			"work in an assignment split gap must not be added to the other assignment's units");
		assertFalse(new TeamPlannerService().isOverallocated(splitTask),
			"a candidate must not be reported overallocated when the overlap exists only in its split gap");
	}

	private Fixture fixture() {
		DataFactoryUndoController undo = new DataFactoryUndoController(); ResourcePool pool = ResourcePool.createRourcePool("team-test", undo);
		Project project = Project.createProject(pool, undo); project.initialize(false, false); project.setFileName("C:/team-planner-fixtures/" + System.nanoTime() + ".mpo");
		// A fixed Monday keeps the fixtures independent of the weekday the tests run on (#1451).
		long start = CalendarOption.getInstance().makeValidStart(LocalDate.of(2026, 11, 23).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(), true);
		project.setStart(start); project.setStartConstraint(start);
		ResourceImpl first = pool.newResourceInstance(); first.setName("Engineer A"); ResourceImpl second = pool.newResourceInstance(); second.setName("Engineer B");
		return new Fixture(project, first, second);
	}
	private NormalTask task(Project project, String name) { NormalTask task = (NormalTask) project.createLocalTaskNode(null).getImpl(); task.setName(name); task.getCurrentSchedule().setStart(project.getStart()); task.setDuration(day()); return task; }
	private long day() { return CalendarOption.getInstance().getMillisPerDay(); }
	private record Fixture(Project project, ResourceImpl first, ResourceImpl second) { }
}
