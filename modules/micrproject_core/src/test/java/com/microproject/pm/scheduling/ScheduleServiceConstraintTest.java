package com.microproject.pm.scheduling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.microproject.options.CalendarOption;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.scheduling.ScheduleInterval;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

class ScheduleServiceConstraintTest {
	@Test
	void setConstraintUpdatesTypeDateAndUndoRedo() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		NormalTask task = new NormalTask(project);
		project.connectTask(task);

		long targetDate = task.getEffectiveWorkCalendar().add(task.getStart(), CalendarOption.getInstance().getMillisPerDay(), false);

		boolean changed = ScheduleService.getInstance().setConstraint(this, task, ConstraintType.SNET, targetDate, undoController.getEditSupport());

		assertTrue(changed);
		assertEquals(ConstraintType.SNET, task.getConstraintType());
		assertEquals(targetDate, task.getConstraintDate());

		undoController.undo();
		assertEquals(ConstraintType.ASAP, task.getConstraintType());
		assertEquals(0L, task.getConstraintDate());

		undoController.redo();
		assertEquals(ConstraintType.SNET, task.getConstraintType());
		assertEquals(targetDate, task.getConstraintDate());
	}

	@Test
	void movingMilestoneKeepsZeroDurationAndAllowsStartConstraint() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		NormalTask task = new NormalTask(project);
		project.connectTask(task);
		task.setDuration(0L);

		long day = CalendarOption.getInstance().getMillisPerDay();
		long originalStart = task.getStart();
		long originalEnd = task.getEnd();
		long movedStart = task.getEffectiveWorkCalendar().add(originalStart, day, false);
		ScheduleInterval originalInterval = new ScheduleInterval(originalStart, originalEnd);

		ScheduleService.getInstance().setInterval(this, task, movedStart, movedStart, originalInterval, undoController.getEditSupport());
		ScheduleService.getInstance().setConstraint(this, task, ConstraintType.SNET, movedStart, undoController.getEditSupport());

		assertTrue(task.isMilestone());
		assertEquals(0L, task.getDuration());
		assertEquals(movedStart, task.getStart());
		assertEquals(movedStart, task.getConstraintDate());
		assertEquals(ConstraintType.SNET, task.getConstraintType());
	}

	@Test
	void resizingAssignmentlessTaskFromEndKeepsStartAndMovesFinish() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		NormalTask task = new NormalTask(project);
		project.connectTask(task);

		long day = CalendarOption.getInstance().getMillisPerDay();
		long start = task.getStart();
		task.setEnd(task.getEffectiveWorkCalendar().add(start, day, false));
		long originalEnd = task.getEnd();
		long movedEnd = CalendarOption.getInstance().makeValidEnd(task.getEffectiveWorkCalendar().add(originalEnd, day, false), true);

		ScheduleService.getInstance().setInterval(this, task, start, movedEnd, new ScheduleInterval(start, originalEnd), undoController.getEditSupport());

		assertEquals(start, task.getStart());
		assertEquals(movedEnd, task.getEnd());
		assertTrue(task.getDuration() > 0L);
	}

	@Test
	void assignmentlessTaskCanMoveAgainAfterConstraintUpdate() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		NormalTask task = new NormalTask(project);
		project.connectTask(task);

		long day = CalendarOption.getInstance().getMillisPerDay();
		long firstStart = task.getStart();
		long firstEnd = task.getEnd();
		long secondStart = task.getEffectiveWorkCalendar().add(firstStart, day, false);
		long secondEnd = task.getEffectiveWorkCalendar().add(firstEnd, day, false);

		ScheduleService.getInstance().setInterval(this, task, secondStart, secondEnd, new ScheduleInterval(firstStart, firstEnd), undoController.getEditSupport());
		ScheduleService.getInstance().setConstraint(this, task, ConstraintType.SNET, task.getStart(), undoController.getEditSupport());

		long thirdStart = task.getEffectiveWorkCalendar().add(task.getStart(), day, false);
		long thirdEnd = task.getEffectiveWorkCalendar().add(task.getEnd(), day, false);
		ScheduleService.getInstance().setInterval(this, task, thirdStart, thirdEnd, new ScheduleInterval(task.getStart(), task.getEnd()), undoController.getEditSupport());
		ScheduleService.getInstance().setConstraint(this, task, ConstraintType.SNET, task.getStart(), undoController.getEditSupport());

		assertEquals(thirdStart, task.getStart());
		assertEquals(thirdEnd, task.getEnd());
		assertEquals(ConstraintType.SNET, task.getConstraintType());
		assertEquals(thirdStart, task.getConstraintDate());
	}

	@Test
	void unchangedConstraintAndIntervalDoNotCreateUndoEdits() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		NormalTask task = new NormalTask(project);
		project.connectTask(task);

		ScheduleInterval originalInterval = new ScheduleInterval(task.getStart(), task.getEnd());
		boolean intervalChanged = ScheduleService.getInstance().setInterval(this, task, task.getStart(), task.getEnd(), originalInterval, undoController.getEditSupport());
		boolean changed = ScheduleService.getInstance().setConstraint(this, task, task.getConstraintType(), task.getConstraintDate(), undoController.getEditSupport());

		assertFalse(intervalChanged);
		assertFalse(changed);
		assertEquals(task.getStart(), originalInterval.getStart());
		assertEquals(task.getEnd(), originalInterval.getEnd());
		assertTrue(!undoController.canUndo());
	}
}
