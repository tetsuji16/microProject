package com.projectlibre1.pm.graphic.gantt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.geom.Rectangle2D;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.projectlibre1.pm.scheduling.Schedule;
import com.projectlibre1.pm.scheduling.ScheduleInterval;
import com.projectlibre1.pm.task.TaskSpecificFields;

class GanttRendererProgressTest {
	@Test
	void ganttBarSupportClassifiesTaskAndBaselineBars() {
		com.projectlibre1.graphic.configuration.BarFormat task = new com.projectlibre1.graphic.configuration.BarFormat();
		task.setId("Bar.task");
		com.projectlibre1.graphic.configuration.BarFormat baseline = new com.projectlibre1.graphic.configuration.BarFormat();
		baseline.setId("Bar.baseline");

		assertTrue(GanttBarSupport.shouldUseModernCapsuleBar(task));
		assertTrue(GanttBarSupport.shouldUsePlannedEnvelopeInterval(task));
		assertTrue(GanttBarSupport.isBaselineBarFormat(baseline));
	}

	@Test
	void progressRatioUsesSchedulePercentForSummaryOverlay() {
		assertEquals(0.44d, GanttRenderer.progressRatioForSchedule(schedule(0.44d)), 0.00001d);
	}

	@Test
	void progressRatioUsesPercentWorkCompleteForTasks() {
		assertEquals(0.10d, GanttRenderer.progressRatioForObject(taskSpecificSchedule(0.44d, 0.10d)), 0.00001d);
	}

	@Test
	void progressRatioUsesPercentWorkCompleteForSummaryTasks() {
		assertEquals(0.10d, GanttRenderer.progressRatioForObject(summaryTaskSchedule(0.44d, 0.10d)), 0.00001d);
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

	@Test
	void mergeIntervalsForDisplayReturnsSingleEnvelopeBar() {
		ScheduleInterval merged = GanttRenderer.mergeIntervalsForDisplay(List.of(
				new ScheduleInterval(30L, 50L),
				new ScheduleInterval(10L, 20L),
				new ScheduleInterval(70L, 90L)));
		assertEquals(10L, merged.getStart());
		assertEquals(90L, merged.getEnd());
	}

	@Test
	void mergeIntervalsForDisplayReturnsNullWhenEmpty() {
		assertNull(GanttRenderer.mergeIntervalsForDisplay(List.of()));
	}

	@Test
	void progressOverlayBoundsOmitOverlayForZeroProgress() {
		assertNull(GanttRenderer.progressOverlayBounds(10.0d, 20.0d, 100.0d, 8.0d, 0.0d));
	}

	@Test
	void progressOverlayBoundsScaleMiddleProgressProportionally() {
		Rectangle2D bounds = GanttRenderer.progressOverlayBounds(10.0d, 20.0d, 100.0d, 8.0d, 0.44d);
		assertEquals(10.0d, bounds.getX(), 0.00001d);
		assertEquals(44.0d, bounds.getWidth(), 0.00001d);
		assertEquals(8.0d, bounds.getHeight(), 0.00001d);
	}

	@Test
	void progressOverlayBoundsCoverFullWidthForCompleteProgress() {
		Rectangle2D bounds = GanttRenderer.progressOverlayBounds(10.0d, 20.0d, 100.0d, 8.0d, 1.0d);
		assertEquals(100.0d, bounds.getWidth(), 0.00001d);
	}

	@Test
	void summaryProgressBoundsOmitOverlayForZeroProgress() {
		Rectangle2D summaryBounds = GanttRenderer.createSummaryBandBounds(10.0d, 20.0d, 100.0d, 12.0d);
		assertNull(GanttRenderer.summaryProgressBounds(summaryBounds, 0.0d));
	}

	@Test
	void summaryBandUsesSlightlyReducedHeight() {
		Rectangle2D summaryBounds = GanttRenderer.createSummaryBandBounds(10.0d, 20.0d, 100.0d, 11.0d);
		assertEquals(100.0d, summaryBounds.getWidth(), 0.00001d);
		assertEquals(5.5d, summaryBounds.getHeight(), 0.00001d);
		assertEquals(17.25d, summaryBounds.getY(), 0.00001d);
	}

	@Test
	void summaryProgressBoundsScaleMiddleProgressProportionally() {
		Rectangle2D summaryBounds = GanttRenderer.createSummaryBandBounds(10.0d, 20.0d, 100.0d, 12.0d);
		Rectangle2D progressBounds = GanttRenderer.summaryProgressBounds(summaryBounds, 0.44d);
		assertEquals(summaryBounds.getX(), progressBounds.getX(), 0.00001d);
		assertEquals(summaryBounds.getY(), progressBounds.getY(), 0.00001d);
		assertEquals(44.0d, progressBounds.getWidth(), 0.00001d);
		assertEquals(summaryBounds.getHeight(), progressBounds.getHeight(), 0.00001d);
	}

	@Test
	void summaryProgressBoundsCoverFullWidthForCompleteProgress() {
		Rectangle2D summaryBounds = GanttRenderer.createSummaryBandBounds(10.0d, 20.0d, 100.0d, 12.0d);
		Rectangle2D progressBounds = GanttRenderer.summaryProgressBounds(summaryBounds, 1.0d);
		assertEquals(summaryBounds.getWidth(), progressBounds.getWidth(), 0.00001d);
	}

	private static Schedule schedule(final double percentComplete) {
		return scheduleProxy(percentComplete, 0.0d, false, new Class<?>[] { Schedule.class });
	}

	private static Object taskSpecificSchedule(final double percentComplete, final double percentWorkComplete) {
		return scheduleProxy(percentComplete, percentWorkComplete, false, new Class<?>[] { Schedule.class, TaskSpecificFields.class });
	}

	private static Object summaryTaskSchedule(final double percentComplete, final double percentWorkComplete) {
		return scheduleProxy(percentComplete, percentWorkComplete, true, new Class<?>[] { Schedule.class, TaskSpecificFields.class });
	}

	private static Schedule scheduleProxy(final double percentComplete, final double percentWorkComplete, final boolean wbsParent, Class<?>[] interfaces) {
		InvocationHandler handler = new InvocationHandler() {
			@Override
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
				GanttRendererProgressTest.class.getClassLoader(),
				interfaces,
				handler);
	}
}
