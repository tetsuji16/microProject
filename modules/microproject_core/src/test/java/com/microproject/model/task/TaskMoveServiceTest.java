/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.model.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.options.CalendarOption;
import com.microproject.model.resource.ResourcePool;
import com.microproject.schedule.ConstraintType;
import com.microproject.undo.DataFactoryUndoController;

class TaskMoveServiceTest {
	@Test
	void autoScheduledTaskGetsStartNoEarlierThanConstraintAtTheShiftedStart() {
		Project project = project();
		NormalTask task = task(project, "Auto");
		project.recalculate();
		long start = task.getStart();
		ConstraintType.Kind initialKind = task.getConstraintTypeKind();
		long initialConstraintDate = task.getConstraintDate();
		long expected = project.getWorkCalendar().add(start, day() * 5L, false);

		var result = new TaskMoveService().moveByWorkingDays(project, List.of(task), TaskMoveService.WEEK);

		assertEquals(1, result.changedCount());
		assertEquals(ConstraintType.Kind.SNET, task.getConstraintTypeKind());
		assertEquals(expected, task.getConstraintDate());
		assertEquals(expected, task.getStart());
		assertFalse(task.isManuallyScheduled());

		result.change().undo();
		assertEquals(start, task.getStart());
		assertEquals(initialKind, task.getConstraintTypeKind());
		assertEquals(initialConstraintDate, task.getConstraintDate());
		result.change().redo();
		assertEquals(expected, task.getStart());
	}

	@Test
	void manuallyScheduledTaskShiftsStartAndFinishAndKeepsDuration() {
		Project project = project();
		NormalTask task = task(project, "Manual");
		ConstraintType.Kind initialKind = task.getConstraintTypeKind();
		long start = project.getWorkCalendar().add(project.getStart(), day() * 3L, false);
		long finish = project.getWorkCalendar().add(start, day() * 2L, false);
		task.setManualDates(start, finish);
		project.recalculate();
		long duration = task.getEffectiveWorkCalendar().compare(task.getEnd(), task.getStart(), false);

		var result = new TaskMoveService().moveByWorkingDays(project, List.of(task), TaskMoveService.DAY);

		assertEquals(1, result.changedCount());
		assertTrue(task.isManuallyScheduled());
		assertEquals(project.getWorkCalendar().add(start, day(), false), task.getStart());
		assertEquals(duration, task.getEffectiveWorkCalendar().compare(task.getEnd(), task.getStart(), false));
		assertEquals(initialKind, task.getConstraintTypeKind());

		result.change().undo();
		assertEquals(start, task.getStart());
		assertEquals(finish, task.getEnd());
	}

	@Test
	void movingBackShiftsEarlierAndZeroOrSummaryTargetsChangeNothing() {
		Project project = project();
		NormalTask task = task(project, "Back");
		long start = project.getWorkCalendar().add(project.getStart(), day() * 10L, false);
		task.setManualDates(start, project.getWorkCalendar().add(start, day(), false));
		project.recalculate();

		var back = new TaskMoveService().moveByWorkingDays(project, List.of(task), -TaskMoveService.WEEK);
		assertEquals(1, back.changedCount());
		assertEquals(project.getWorkCalendar().add(start, -day() * 5L, false), task.getStart());

		var none = new TaskMoveService().moveByWorkingDays(project, List.of(task), 0);
		assertFalse(none.change().hasChanged());
		assertEquals(0, new TaskMoveService().moveByWorkingDays(project, List.of(), 1).changedCount());
	}

	private static Project project() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("move-test", undo), undo);
		project.initialize(false, false);
		return project;
	}

	private static NormalTask task(Project project, String name) {
		NormalTask task = new NormalTask(project);
		task.setName(name);
		project.connectTask(task);
		project.getSchedulingAlgorithm().addObject(task);
		task.getCurrentSchedule().setStart(project.getStart());
		task.setDuration(day());
		return task;
	}

	private static long day() {
		return CalendarOption.getInstance().getMillisPerDay();
	}
}
