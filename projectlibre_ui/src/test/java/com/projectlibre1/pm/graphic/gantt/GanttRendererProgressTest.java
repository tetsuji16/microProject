package com.projectlibre1.pm.graphic.gantt;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.junit.jupiter.api.Test;

import com.projectlibre1.pm.scheduling.Schedule;

class GanttRendererProgressTest {
	@Test
	void progressRatioUsesSchedulePercentForSummaryOverlay() {
		assertEquals(0.44d, GanttRenderer.progressRatioForSchedule(schedule(0.44d)), 0.00001d);
	}

	@Test
	void progressRatioClampsOutOfRangeValues() {
		assertEquals(0.0d, GanttRenderer.progressRatioForSchedule(schedule(-0.20d)), 0.00001d);
		assertEquals(1.0d, GanttRenderer.progressRatioForSchedule(schedule(1.50d)), 0.00001d);
	}

	@Test
	void progressRatioFallsBackToZeroWhenScheduleIsMissing() {
		assertEquals(0.0d, GanttRenderer.progressRatioForSchedule(null), 0.00001d);
	}

	private static Schedule schedule(final double percentComplete) {
		InvocationHandler handler = new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) {
				String name = method.getName();
				if ("getPercentComplete".equals(name))
					return Double.valueOf(percentComplete);
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
				GanttRendererProgressTest.class.getClassLoader(),
				new Class<?>[] { Schedule.class },
				handler);
	}
}
