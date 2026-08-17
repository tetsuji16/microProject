package com.microproject.pm.graphic.gantt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.microproject.options.CalculationOption;
import com.microproject.options.CalendarOption;
import com.microproject.pm.criticalpath.TaskSchedule;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

/**
 * Regression test for #206: tasks on the critical path must be visually
 * distinguished with the palette's critical color (MS Project renders them red),
 * while non-critical tasks keep their status color.
 */
class GanttCriticalTaskColorTest {
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
	void criticalTaskResolvesToCriticalColor() throws Exception {
		NormalTask critical = createTask();
		setScheduleWindows(critical, 0L, 0L);
		assertTrue(critical.isCritical(), "zero total slack must mark the task critical");

		GanttRenderer renderer = new GanttRenderer();
		GanttRenderer.DisplayedBarColors colors = renderer.resolveDisplayedBarColors(critical);

		int expected = renderer.getPalette().getCriticalTaskColor().getRGB() & 0x00FFFFFF;
		assertEquals(expected, colors.middleRgb(), "critical task bar must use the critical color");
	}

	@Test
	void nonCriticalTaskKeepsStatusColor() throws Exception {
		NormalTask normal = createTask();
		long day = CalendarOption.getInstance().getMillisPerDay();
		setScheduleWindows(normal, day, day);
		assertFalse(normal.isCritical(), "one day of total slack must not be critical at threshold 0");

		GanttRenderer renderer = new GanttRenderer();
		GanttRenderer.DisplayedBarColors colors = renderer.resolveDisplayedBarColors(normal);

		int critical = renderer.getPalette().getCriticalTaskColor().getRGB() & 0x00FFFFFF;
		assertNotEquals(critical, colors.middleRgb(), "non-critical task must not use the critical color");
	}

	private static NormalTask createTask() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("critical-color-test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		NormalTask task = new NormalTask(project);
		task.setName("Task");
		project.connectTask(task);
		return task;
	}

	private static void setScheduleWindows(NormalTask task, long startSlack, long finishSlack) {
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
