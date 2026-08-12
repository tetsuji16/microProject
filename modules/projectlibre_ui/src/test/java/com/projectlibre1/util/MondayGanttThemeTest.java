package com.projectlibre1.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.junit.jupiter.api.Test;

import com.projectlibre1.graphic.configuration.BarFormat;
import com.projectlibre1.pm.resource.ResourcePool;
import com.projectlibre1.pm.scheduling.Schedule;
import com.projectlibre1.pm.task.NormalTask;
import com.projectlibre1.pm.task.Project;
import com.projectlibre1.undo.DataFactoryUndoController;

class MondayGanttThemeTest {
	@Test
	void statusColorUsesDoneForCompletedSchedules() {
		assertEquals(MondayGanttTheme.DONE, MondayGanttTheme.statusColor(schedule(1.0d), null));
	}

	@Test
	void statusColorUsesNeutralGrayForNotStartedSchedules() {
		assertEquals(MondayGanttTheme.NOT_STARTED, MondayGanttTheme.statusColor(schedule(0.0d), null));
	}

	@Test
	void statusColorUsesWorkingColorForPartialProgress() {
		assertEquals(MondayGanttTheme.WORKING_ON_IT, MondayGanttTheme.statusColor(schedule(0.44d), null));
	}

	@Test
	void statusColorUsesPercentCompleteForLeafTasks() {
		NormalTask task = createTask();
		task.setPercentComplete(0.44d);
		task.setPercentWorkComplete(0.0d);
		assertEquals(MondayGanttTheme.WORKING_ON_IT, MondayGanttTheme.statusColor(task, task));

		task.setPercentComplete(1.0d);
		assertEquals(MondayGanttTheme.DONE, MondayGanttTheme.statusColor(task, task));
	}

	@Test
	void statusColorUsesPercentCompleteForSummaryTasks() {
		Schedule summary = taskSpecificSchedule(0.44d, 0.0d, true);
		assertEquals(MondayGanttTheme.WORKING_ON_IT, MondayGanttTheme.statusColor(summary, summary));

		summary = taskSpecificSchedule(1.0d, 0.44d, true);
		assertEquals(MondayGanttTheme.DONE, MondayGanttTheme.statusColor(summary, summary));
	}

	@Test
	void criticalAccentUsesNeutralDarkInsteadOfAlertRed() {
		assertEquals(MondayGanttTheme.criticalAccent(), MondayGanttTheme.accentColor(barFormat("Bar.critical"), MondayGanttTheme.NOT_STARTED));
	}

	@Test
	void summaryAccentUsesNeutralDarkInsteadOfLegacyPurple() {
		assertEquals(MondayGanttTheme.criticalAccent(), MondayGanttTheme.accentColor(barFormat("Bar.summary"), MondayGanttTheme.WORKING_ON_IT));
	}

	@Test
	void baselineAccentRemainsDedicatedBaselineGray() {
		assertEquals(MondayGanttTheme.BASELINE, MondayGanttTheme.accentColor(barFormat("Bar.baseline"), MondayGanttTheme.WORKING_ON_IT));
	}

	@Test
	void milestoneAccentTracksStatusColor() {
		assertEquals(MondayGanttTheme.shade(MondayGanttTheme.DONE, 0.18f), MondayGanttTheme.accentColor(barFormat("Bar.milestone"), MondayGanttTheme.DONE));
	}

	@Test
	void nullScheduleDoesNotDefaultToAlertState() {
		assertEquals(MondayGanttTheme.GROUP_A, MondayGanttTheme.statusColor(null, null));
	}

	private static Schedule schedule(final double percentComplete) {
		return taskSpecificSchedule(percentComplete, 0.0d, false);
	}

	private static Schedule taskSpecificSchedule(final double percentComplete, final double percentWorkComplete, final boolean wbsParent) {
		InvocationHandler handler = new InvocationHandler() {
			public Object invoke(Object proxy, Method method, Object[] args) {
				String name = method.getName();
				if ("getPercentComplete".equals(name))
					return Double.valueOf(percentComplete);
				if ("getPercentWorkComplete".equals(name))
					return Double.valueOf(percentWorkComplete);
				if ("isWbsParent".equals(name))
					return Boolean.valueOf(wbsParent);
				if ("equals".equals(name))
					return Boolean.valueOf(proxy == args[0]);
				if ("hashCode".equals(name))
					return Integer.valueOf(System.identityHashCode(proxy));
				if ("toString".equals(name))
					return "ScheduleProxy[" + percentComplete + "]";
				Class<?> returnType = method.getReturnType();
				if (returnType == Boolean.TYPE)
					return Boolean.FALSE;
				if (returnType == Integer.TYPE)
					return Integer.valueOf(0);
				if (returnType == Long.TYPE)
					return Long.valueOf(0L);
				if (returnType == Double.TYPE)
					return Double.valueOf(0.0d);
				return null;
			}
		};
		return (Schedule) Proxy.newProxyInstance(
				MondayGanttThemeTest.class.getClassLoader(),
				new Class<?>[] { Schedule.class, com.projectlibre1.pm.task.TaskSpecificFields.class },
				handler);
	}

	private static BarFormat barFormat(String id) {
		BarFormat format = new BarFormat();
		format.setId(id);
		return format;
	}

	private static NormalTask createTask() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		NormalTask task = new NormalTask(project);
		project.connectTask(task);
		return task;
	}
}
