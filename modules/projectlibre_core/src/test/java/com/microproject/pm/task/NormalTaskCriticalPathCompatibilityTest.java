package com.microproject.pm.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.microproject.options.CalculationOption;
import com.microproject.options.CalendarOption;
import com.microproject.pm.criticalpath.TaskSchedule;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.scheduling.ConstraintType;
import com.microproject.undo.DataFactoryUndoController;

class NormalTaskCriticalPathCompatibilityTest {
	private long originalCriticalSlackThreshold;

	@BeforeEach
	void rememberCalculationOptions() {
		originalCriticalSlackThreshold = CalculationOption.getInstance().getCriticalSlackThreshold();
		CalculationOption.getInstance().setCriticalSlackThreshold(0L);
	}

	@AfterEach
	void restoreCalculationOptions() {
		CalculationOption.getInstance().setCriticalSlackThreshold(originalCriticalSlackThreshold);
	}

	@Test
	void totalSlackUsesTheSmallerOfStartAndFinishSlack() {
		NormalTask task = createTask();
		long day = CalendarOption.getInstance().getMillisPerDay();
		setScheduleWindows(task, day, 2L * day);

		assertEquals(day, task.getTotalSlack());
	}

	@Test
	void configuredSlackThresholdControlsCriticalClassification() {
		NormalTask task = createTask();
		long day = CalendarOption.getInstance().getMillisPerDay();
		setScheduleWindows(task, day, day);

		assertFalse(task.isCritical());

		CalculationOption.getInstance().setCriticalSlackThreshold(day);

		assertTrue(task.isCritical());
	}

	@Test
	void completedTaskStopsBeingCritical() {
		NormalTask task = createTask();
		setScheduleWindows(task, 0L, 0L);
		assertTrue(task.isCritical());

		task.setPercentComplete(1.0d);

		assertFalse(task.isCritical());
	}

	@Test
	void inflexibleConstraintIsCriticalEvenWithPositiveSlack() throws Exception {
		NormalTask task = createTask();
		long day = CalendarOption.getInstance().getMillisPerDay();
		setScheduleWindows(task, day, day);
		task.setConstraintType(ConstraintType.MFO);

		assertTrue(task.isCritical());
	}

	@Test
	void reachingADeadlineMakesTaskCritical() {
		NormalTask task = createTask();
		long day = CalendarOption.getInstance().getMillisPerDay();
		setScheduleWindows(task, day, day);
		task.setDeadline(task.getEnd());

		assertTrue(task.isCritical());
	}

	private NormalTask createTask() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		NormalTask task = new NormalTask(project);
		project.connectTask(task);
		return task;
	}

	private void setScheduleWindows(NormalTask task, long startSlack, long finishSlack) {
		long start = task.getStart();
		long finish = task.getEnd();
		TaskSchedule early = task.getEarlySchedule();
		TaskSchedule late = task.getLateSchedule();
		early.setStart(start);
		early.setFinish(finish);
		late.setStart(task.getEffectiveWorkCalendar().add(start, startSlack, false));
		late.setFinish(task.getEffectiveWorkCalendar().add(finish, finishSlack, false));
	}
}
