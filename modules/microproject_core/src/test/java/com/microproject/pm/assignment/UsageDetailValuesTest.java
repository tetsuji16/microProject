/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.assignment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.microproject.field.FieldContext;
import com.microproject.options.CalendarOption;
import com.microproject.pm.availability.Availability;
import com.microproject.pm.calendar.WorkDay;
import com.microproject.pm.calendar.WorkingCalendar;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.time.HasStartAndEnd;
import com.microproject.undo.DataFactoryUndoController;

class UsageDetailValuesTest {
	private static final long DAY = CalendarOption.getInstance().getMillisPerDay();

	@Test
	void overallocationIsWorkAboveAvailabilityAndNeverNegative() {
		assertEquals(30L, UsageDetailValues.overallocation(130L, 100L));
		assertEquals(0L, UsageDetailValues.overallocation(100L, 100L));
		assertEquals(0L, UsageDetailValues.overallocation(40L, 100L));
	}

	@Test
	void remainingAvailabilityIsAvailabilityMinusWorkAndNeverBelowZero() {
		assertEquals(60L, UsageDetailValues.remainingAvailability(40L, 100L));
		assertEquals(0L, UsageDetailValues.remainingAvailability(100L, 100L));
		assertEquals(0L, UsageDetailValues.remainingAvailability(130L, 100L));
	}

	@Test
	void availableWorkFollowsTheAvailabilityTableAndMatchesTheAssignmentAvailability() throws Exception {
		Project project = createProject();
		NormalTask task = createTask(project);
		task.setDuration(2L * DAY);
		ResourceImpl resource = project.getResourcePool().newResourceInstance();
		resource.setWorkCalendar(project.getWorkCalendar());
		Assignment assignment = AssignmentService.getInstance().newAssignment(task, resource, 1D, 0L, this);
		long split = assignment.getEffectiveWorkCalendar().add(assignment.getStart(), DAY, false);
		Availability halfRate = (Availability) resource.getAvailabilityTable().newValueObject(split);
		halfRate.setMaximumUnits(0.5D);

		assertEquals(assignment.getResourceAvailability(),
			UsageDetailValues.availableWork(resource, assignment.getStart(), assignment.getEnd()));
		assertEquals(0L, UsageDetailValues.availableWork(resource, assignment.getEnd(), assignment.getStart()));
	}

	@Test
		void resourceWithoutCalendarUsesTheProjectCalendarNotTheTaskCalendar() {
			Project project = createProject();
			// Set project start to a fixed Monday (2026-11-23) to ensure the test period
			// spans multiple working days where the project calendar (Mon-Fri) and task
			// calendar (Mon, Tue, Fri; Wed/Thu non-working) differ.
			long mondayStart = 1795430400000L; // Mon Nov 23 2026 00:00:00 UTC
			project.setStart(mondayStart);
			project.recalculate();

			NormalTask task = createTask(project);
			task.setDuration(2L * DAY);
			WorkingCalendar taskCalendar = WorkingCalendar.getStandardBasedInstance();
			taskCalendar.setWeekDay(java.util.Calendar.WEDNESDAY - 1, new WorkDay());
			taskCalendar.setWeekDay(java.util.Calendar.THURSDAY - 1, new WorkDay());
			task.setWorkCalendar(taskCalendar);
			ResourceImpl resource = project.getResourcePool().newResourceInstance();
			resource.setWorkCalendar(null);
			Assignment assignment = AssignmentService.getInstance().newAssignment(task, resource, 1D, 0L, this);
			long start = assignment.getStart();
			long end = start + 7L * DAY;
			double units = ((Availability) resource.getAvailabilityTable().getList().iterator().next()).getMaximumUnits();

			long projectWork = Math.round(project.getEffectiveWorkCalendar().compare(end, start, false) * units);
			long taskWork = Math.round(taskCalendar.compare(end, start, false) * units);

			assertTrue(projectWork > 0L && projectWork != taskWork, "the task calendar must differ from the project calendar");
			assertEquals(projectWork, UsageDetailValues.availableWork(resource, start, end));
		}

	@Test
	void cumulativeWorkIsTheRunningTotalThroughEachPeriod() {
		Project project = createProject();
		NormalTask task = createTask(project);
		task.setDuration(3L * DAY);
		Assignment assignment = firstAssignment(task);
		long start = assignment.getStart();
		long afterFirstDay = assignment.getEffectiveWorkCalendar().add(start, DAY, false);
		long afterSecondDay = assignment.getEffectiveWorkCalendar().add(start, 2L * DAY, false);
		FieldContext first = period(start, afterFirstDay);
		FieldContext second = period(afterFirstDay, afterSecondDay);
		FieldContext whole = period(start, assignment.getEnd() + DAY);

		long cumulativeFirst = assignment.getCumulativeWork(first);
		long cumulativeSecond = assignment.getCumulativeWork(second);

		assertEquals(assignment.work(start, afterFirstDay), cumulativeFirst);
		assertEquals(cumulativeFirst + assignment.work(afterFirstDay, afterSecondDay), cumulativeSecond);
		assertTrue(cumulativeSecond > cumulativeFirst);
		assertEquals(assignment.work(), assignment.getCumulativeWork(whole));
		// the task row rolls its assignments up
		assertEquals(assignment.work(), task.getCumulativeWork(whole));
	}

	@Test
	void resourceOverallocationAndRemainingAvailabilityPerPeriod() {
		Project project = createProject();
		NormalTask task = createTask(project);
		task.setDuration(2L * DAY);
		ResourceImpl resource = project.getResourcePool().newResourceInstance();
		resource.setWorkCalendar(project.getWorkCalendar());
		Assignment assignment = AssignmentService.getInstance().newAssignment(task, resource, 2D, 0L, this);
		long start = assignment.getStart();
		long end = assignment.getEnd();
		FieldContext period = period(start, end);
		long available = UsageDetailValues.availableWork(resource, start, end);

		long work = resource.work(start, end);
		assertTrue(work > available, "two units of work on one resource exceed its availability");
		assertEquals(work - available, resource.getOverallocation(period));
		assertEquals(0L, resource.getRemainingAvailability(period));

		assignment.forceUnits(0.5D);
		work = resource.work(start, end);
		assertTrue(work < available);
		assertEquals(0L, resource.getOverallocation(period));
		assertEquals(available - work, resource.getRemainingAvailability(period));
	}

	@Test
	void rowsWithoutAvailabilityAndPeriodlessContextsReportZeroOverallocation() {
		Project project = createProject();
		NormalTask task = createTask(project);
		task.setDuration(DAY);
		Assignment assignment = firstAssignment(task);
		FieldContext period = period(assignment.getStart(), assignment.getEnd());

		assertEquals(0L, assignment.getOverallocation(period));
		assertEquals(0L, task.getRemainingAvailability(period));
		assertEquals(0L, assignment.getResource().getOverallocation(null));
	}

	@Test
	void theFieldsAreRegisteredAndReadThroughTheFieldFramework() {
		Project project = createProject();
		NormalTask task = createTask(project);
		task.setDuration(DAY);
		Assignment assignment = firstAssignment(task);
		FieldContext period = period(assignment.getStart(), assignment.getEnd());

		for (String id : java.util.List.of("Field.cumulativeWork", "Field.overallocation", "Field.remainingAvailability")) {
			com.microproject.field.Field field = com.microproject.configuration.Configuration.getFieldFromId(id);
			assertTrue(field != null, id + " is configured");
			assertTrue(field.isVector() && field.isReadOnly(), id + " is a read-only per-period field");
		}
		com.microproject.field.Field cumulative = com.microproject.configuration.Configuration.getFieldFromId("Field.cumulativeWork");
		com.microproject.field.Field work = com.microproject.configuration.Configuration.getFieldFromId("Field.work");
		// the first period starts the assignment, so its running total is its work
		Object expectedWork = work.getValue(assignment, period);
		Object cumulativeValue = cumulative.getValue(assignment, period);
		assertTrue(expectedWork instanceof Number && ((Number) expectedWork).longValue() > 0L, "the period carries work");
		assertEquals(((Number) expectedWork).longValue(), ((Number) cumulativeValue).longValue());
	}

	private static FieldContext period(long start, long end) {
		FieldContext context = new FieldContext();
		context.setInterval(new HasStartAndEnd() {
			public long getStart() {
				return start;
			}
			public long getEnd() {
				return end;
			}
		});
		return context;
	}

	private Project createProject() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("usage-detail-values", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		return project;
	}

	private NormalTask createTask(Project project) {
		NormalTask task = new NormalTask(project);
		project.connectTask(task);
		return task;
	}

	private Assignment firstAssignment(NormalTask task) {
		return (Assignment) task.getAssignments().iterator().next();
	}
}
