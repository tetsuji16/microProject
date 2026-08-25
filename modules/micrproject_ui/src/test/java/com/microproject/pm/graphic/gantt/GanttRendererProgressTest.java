/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
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
package com.microproject.pm.graphic.gantt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.configuration.Dictionary;
import com.microproject.graphic.configuration.BarFormat;
import com.microproject.graphic.configuration.shape.PredefinedShape;
import com.microproject.pm.scheduling.Schedule;
import com.microproject.pm.scheduling.ScheduleInterval;
import com.microproject.pm.task.TaskSpecificFields;
import com.microproject.strings.Messages;
import com.microproject.util.GanttProgress;

class GanttRendererProgressTest {
	@Test
	void summaryBarUsesMicrosoftProjectStyleDropEnds() {
		BarFormat summary = (BarFormat)Dictionary.get(BarFormat.category, Messages.getString("Bar.summary"));

		assertNotNull(summary);
		assertNotNull(summary.getStart());
		assertNotNull(summary.getEnd());
		assertEquals(PredefinedShape.PENTAGON_DOWN, summary.getStart().getShape());
		assertEquals(PredefinedShape.PENTAGON_DOWN, summary.getEnd().getShape());
	}

	@Test
	void deadlineUsesMicrosoftProjectStyleArrow() {
		BarFormat deadline = (BarFormat)Dictionary.get(BarFormat.category, Messages.getString("Bar.deadline"));

		assertNotNull(deadline);
		assertNotNull(deadline.getStart());
		assertEquals(PredefinedShape.ARROW_DOWN, deadline.getStart().getShape());
	}

	@Test
	void ganttBarSupportClassifiesTaskAndBaselineBars() {
		com.microproject.graphic.configuration.BarFormat task = new com.microproject.graphic.configuration.BarFormat();
		task.setId("Bar.task");
		com.microproject.graphic.configuration.BarFormat baseline = new com.microproject.graphic.configuration.BarFormat();
		baseline.setId("Bar.baseline");

		assertTrue(GanttBarSupport.shouldUseModernCapsuleBar(task));
		assertTrue(GanttBarSupport.shouldUsePlannedEnvelopeInterval(task));
		assertTrue(GanttBarSupport.shouldPreserveSplitIntervals(task));
		assertTrue(GanttBarSupport.isBaselineBarFormat(baseline));
		assertTrue(GanttBarSupport.isIndividuallyFormattable(task));
		assertFalse(GanttBarSupport.isIndividuallyFormattable(baseline));
	}

	@Test
	void progressRatioUsesSchedulePercentForSummaryOverlay() {
		assertEquals(0.44d, GanttBarSupport.progressRatioForSchedule(schedule(0.44d)), 0.00001d);
	}

	@Test
	void progressRatioUsesPercentCompleteForTasks() {
		assertEquals(0.44d, GanttProgress.ratioForObject(taskSpecificSchedule(0.44d, 0.10d)), 0.00001d);
	}

	@Test
	void progressRatioUsesPercentCompleteForSummaryTasks() {
		assertEquals(0.44d, GanttProgress.ratioForObject(summaryTaskSchedule(0.44d, 0.10d)), 0.00001d);
	}

	@Test
	void assignmentBarsCanPaintWorkProgress() {
		com.microproject.graphic.configuration.BarFormat assignment = new com.microproject.graphic.configuration.BarFormat();
		assignment.setId("Bar.assignment");
		assignment.setMain(true);
		assertTrue(GanttRendererSupport.shouldPaintProgressOverlay(schedule(0.50d), assignment));
	}

	@Test
	void progressRatioClampsOutOfRangeValues() {
		assertEquals(0.0d, GanttBarSupport.progressRatioForSchedule(schedule(-0.20d)), 0.00001d);
		assertEquals(1.0d, GanttBarSupport.progressRatioForSchedule(schedule(1.50d)), 0.00001d);
	}

	@Test
	void progressRatioFallsBackToZeroWhenScheduleIsMissing() {
		assertEquals(0.0d, GanttBarSupport.progressRatioForSchedule(null), 0.00001d);
	}

	@Test
	void mergeIntervalsForDisplayReturnsSingleEnvelopeBar() {
		ScheduleInterval merged = GanttBarSupport.mergeIntervalsForDisplay(List.of(
				new ScheduleInterval(30L, 50L),
				new ScheduleInterval(10L, 20L),
				new ScheduleInterval(70L, 90L)));
		assertEquals(10L, merged.getStart());
		assertEquals(90L, merged.getEnd());
	}

	@Test
	void mergeIntervalsForDisplayReturnsNullWhenEmpty() {
		assertNull(GanttBarSupport.mergeIntervalsForDisplay(List.of()));
	}

	@Test
	void splitTaskKeepsItsSectionsInsteadOfUsingThePlannedEnvelope() {
		BarFormat task = barFormat("Bar.task");
		List<ScheduleInterval> intervals = GanttBarSupport.displayIntervals(task, List.of(
				new ScheduleInterval(10L, 30L),
				new ScheduleInterval(50L, 90L)), new ScheduleInterval(10L, 90L));

		assertEquals(2, intervals.size());
		assertInterval(intervals.get(0), 10L, 30L);
		assertInterval(intervals.get(1), 50L, 90L);
	}

	@Test
	void unsplitTaskUsesItsFullPlannedEnvelope() {
		BarFormat task = barFormat("Bar.task");
		List<ScheduleInterval> intervals = GanttBarSupport.displayIntervals(task,
				List.of(new ScheduleInterval(12L, 88L)), new ScheduleInterval(10L, 90L));

		assertEquals(1, intervals.size());
		assertInterval(intervals.get(0), 10L, 90L);
	}

	@Test
	void splitSummaryStillUsesOneRollupEnvelope() {
		BarFormat summary = barFormat("Bar.summary");
		List<ScheduleInterval> intervals = GanttBarSupport.displayIntervals(summary, List.of(
				new ScheduleInterval(10L, 30L),
				new ScheduleInterval(50L, 90L)), new ScheduleInterval(10L, 90L));

		assertEquals(1, intervals.size());
		assertInterval(intervals.get(0), 10L, 90L);
	}

	@Test
	void splitTaskGapsBecomeConnectorRanges() {
		List<ScheduleInterval> gaps = GanttBarSupport.splitGaps(List.of(
				new ScheduleInterval(10L, 30L),
				new ScheduleInterval(50L, 90L),
				new ScheduleInterval(100L, 120L)));

		assertEquals(2, gaps.size());
		assertInterval(gaps.get(0), 30L, 50L);
		assertInterval(gaps.get(1), 90L, 100L);
	}

	@Test
	void splitTaskProgressIsAllocatedAcrossSectionsInWorkOrder() {
		List<Double> ratios = GanttBarSupport.progressRatiosForIntervals(List.of(
				new ScheduleInterval(10L, 30L),
				new ScheduleInterval(50L, 90L)), 0.5d);

		assertEquals(1.0d, ratios.get(0), 0.00001d);
		assertEquals(0.25d, ratios.get(1), 0.00001d);
	}

	@Test
	void progressOverlayBoundsOmitOverlayForZeroProgress() {
		assertNull(GanttBarSupport.progressOverlayBounds(10.0d, 20.0d, 100.0d, 8.0d, 0.0d));
	}

	@Test
	void progressOverlayBoundsScaleMiddleProgressProportionally() {
		Rectangle2D bounds = GanttBarSupport.progressOverlayBounds(10.0d, 20.0d, 100.0d, 8.0d, 0.44d);
		assertEquals(10.0d, bounds.getX(), 0.00001d);
		assertEquals(44.0d, bounds.getWidth(), 0.00001d);
		assertEquals(8.0d, bounds.getHeight(), 0.00001d);
	}

	@Test
	void progressOverlayBoundsCoverFullWidthForCompleteProgress() {
		Rectangle2D bounds = GanttBarSupport.progressOverlayBounds(10.0d, 20.0d, 100.0d, 8.0d, 1.0d);
		assertEquals(100.0d, bounds.getWidth(), 0.00001d);
	}

	@Test
	void progressOverlayIncludesDarkMicrosoftProjectStyleIndicator() {
		BufferedImage image = new BufferedImage(60, 20, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		try {
			graphics.setColor(Color.WHITE);
			graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
			GanttRenderer.paintProgressIndicator(graphics, new Rectangle2D.Double(5.0d, 7.0d, 40.0d, 6.0d));
		} finally {
			graphics.dispose();
		}
		assertEquals(Color.BLACK.getRGB(), image.getRGB(20, 10));
	}

	@Test
	void summaryProgressBoundsOmitOverlayForZeroProgress() {
		Rectangle2D summaryBounds = GanttBarSupport.createSummaryBandBounds(10.0d, 20.0d, 100.0d, 12.0d);
		assertNull(GanttBarSupport.summaryProgressBounds(summaryBounds, 0.0d));
	}

	@Test
	void summaryBandUsesSlightlyReducedHeight() {
		Rectangle2D summaryBounds = GanttBarSupport.createSummaryBandBounds(10.0d, 20.0d, 100.0d, 11.0d);
		assertEquals(100.0d, summaryBounds.getWidth(), 0.00001d);
		assertEquals(5.5d, summaryBounds.getHeight(), 0.00001d);
		assertEquals(17.25d, summaryBounds.getY(), 0.00001d);
	}

	@Test
	void summaryProgressBoundsScaleMiddleProgressProportionally() {
		Rectangle2D summaryBounds = GanttBarSupport.createSummaryBandBounds(10.0d, 20.0d, 100.0d, 12.0d);
		Rectangle2D progressBounds = GanttBarSupport.summaryProgressBounds(summaryBounds, 0.44d);
		assertEquals(summaryBounds.getX(), progressBounds.getX(), 0.00001d);
		assertEquals(summaryBounds.getY(), progressBounds.getY(), 0.00001d);
		assertEquals(44.0d, progressBounds.getWidth(), 0.00001d);
		assertEquals(summaryBounds.getHeight(), progressBounds.getHeight(), 0.00001d);
	}

	@Test
	void summaryProgressBoundsCoverFullWidthForCompleteProgress() {
		Rectangle2D summaryBounds = GanttBarSupport.createSummaryBandBounds(10.0d, 20.0d, 100.0d, 12.0d);
		Rectangle2D progressBounds = GanttBarSupport.summaryProgressBounds(summaryBounds, 1.0d);
		assertEquals(summaryBounds.getWidth(), progressBounds.getWidth(), 0.00001d);
	}

	private static BarFormat barFormat(String id) {
		BarFormat format = new BarFormat();
		format.setId(id);
		return format;
	}

	private static void assertInterval(ScheduleInterval interval, long start, long end) {
		assertEquals(start, interval.getStart());
		assertEquals(end, interval.getEnd());
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
