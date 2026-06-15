package com.projectlibre1.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.junit.jupiter.api.Test;

import com.projectlibre1.pm.scheduling.Schedule;
import com.projectlibre1.pm.task.TaskSpecificFields;

class GanttProgressTest {
	@Test
	void ratioUsesPercentWorkCompleteForLeafTasks() {
		assertEquals(0.10d, GanttProgress.ratioForObject(taskSpecificSchedule(0.44d, 0.10d, false)), 0.00001d);
	}

	@Test
	void ratioUsesPercentWorkCompleteForSummaryTasks() {
		assertEquals(0.20d, GanttProgress.ratioForObject(taskSpecificSchedule(0.44d, 0.20d, true)), 0.00001d);
	}

	@Test
	void startedStateUsesResolvedProgressRatio() {
		assertTrue(GanttProgress.hasVisibleProgress(taskSpecificSchedule(0.0d, 0.10d, false)));
		assertFalse(GanttProgress.hasVisibleProgress(taskSpecificSchedule(0.0d, 0.0d, false)));
	}

	@Test
	void progressDateUsesResolvedProgressRatio() {
		assertEquals(20L, GanttProgress.progressDate(0L, 100L, 0.20d, 0L));
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
				GanttProgressTest.class.getClassLoader(),
				new Class<?>[] { Schedule.class, TaskSpecificFields.class },
				handler);
	}
}
