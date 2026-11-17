/*******************************************************************************
 * MIT License
 *
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
package com.microproject.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.junit.jupiter.api.Test;

import com.microproject.pm.scheduling.Schedule;
import com.microproject.pm.task.ProgressLineSettings;
import com.microproject.pm.task.TaskSpecificFields;

class GanttProgressTest {
	@Test
	void ratioUsesPercentCompleteForLeafTasks() {
		assertEquals(0.44d, GanttProgress.ratioForObject(taskSpecificSchedule(0.44d, 0.10d, false)), 0.00001d);
	}

	@Test
	void ratioUsesPercentCompleteForSummaryTasks() {
		assertEquals(0.44d, GanttProgress.ratioForObject(taskSpecificSchedule(0.44d, 0.20d, true)), 0.00001d);
	}

	@Test
	void startedStateUsesResolvedProgressRatio() {
		assertTrue(GanttProgress.hasVisibleProgress(taskSpecificSchedule(0.10d, 0.0d, false)));
		assertFalse(GanttProgress.hasVisibleProgress(taskSpecificSchedule(0.0d, 0.0d, false)));
	}

	@Test
	void progressLineUsesCalendarAwareCompleteThroughDate() {
		Schedule task = taskSpecificSchedule(0.50d, 0.0d, false, 45L, 10L, 90L);
		assertEquals(45L, GanttProgress.progressLineDate(task, 50L));
	}

	@Test
	void progressLineKeepsFutureAndAlreadyCompletedTasksOnStatusDate() {
		assertEquals(50L, GanttProgress.progressLineDate(
				taskSpecificSchedule(0.0d, 0.0d, false, 60L, 60L, 90L), 50L));
		assertEquals(50L, GanttProgress.progressLineDate(
				taskSpecificSchedule(1.0d, 1.0d, false, 40L, 10L, 40L), 50L));
	}

	@Test
	void recurringProgressLinesUseConfiguredCalendarIntervalAndIncludeReferenceLine() {
		ProgressLineSettings settings = new ProgressLineSettings(true,
			ProgressLineSettings.ReferenceDate.CURRENT_DATE, true, 2, ProgressLineSettings.IntervalUnit.DAY);
		long start = 86_400_000L * 10L;
		long end = start + 5L * 86_400_000L;
		var dates = GanttProgress.progressLineReferenceDates(start, end, start + 86_400_000L, settings);
		assertEquals(java.util.List.of(start + 86_400_000L, start, start + 2L * 86_400_000L,
			start + 4L * 86_400_000L), dates);
	}

	@Test
	void recurringProgressLinesUseCalendarWeekAndMonthIntervals() {
		java.util.Calendar startCalendar = DateTime.calendarInstance();
		startCalendar.clear();
		startCalendar.set(2026, java.util.Calendar.JANUARY, 31);
		long start = startCalendar.getTimeInMillis();
		java.util.Calendar expectedCalendar = (java.util.Calendar) startCalendar.clone();
		java.util.List<Long> expectedWeeks = new java.util.ArrayList<>();
		for (int i = 0; i < 5; i++) {
			expectedWeeks.add(Long.valueOf(expectedCalendar.getTimeInMillis()));
			expectedCalendar.add(java.util.Calendar.WEEK_OF_YEAR, 1);
		}
		assertEquals(expectedWeeks, GanttProgress.progressLineReferenceDates(start,
			expectedWeeks.getLast().longValue(), -1L,
			new ProgressLineSettings(false, ProgressLineSettings.ReferenceDate.STATUS_DATE,
				true, 1, ProgressLineSettings.IntervalUnit.WEEK)));

		java.util.List<Long> expectedMonths = new java.util.ArrayList<>();
		for (int i = 0; i < 5; i++) {
			java.util.Calendar monthDate = (java.util.Calendar) startCalendar.clone();
			monthDate.add(java.util.Calendar.MONTH, i);
			expectedMonths.add(Long.valueOf(monthDate.getTimeInMillis()));
		}
		assertEquals(expectedMonths, GanttProgress.progressLineReferenceDates(start,
			expectedMonths.getLast().longValue(), -1L,
			new ProgressLineSettings(false, ProgressLineSettings.ReferenceDate.STATUS_DATE,
				true, 1, ProgressLineSettings.IntervalUnit.MONTH)));
	}

	@Test
	void recurringViewportStartingBetweenIntervalsDoesNotDiscardFollowingWeekOrMonthDates() {
		java.util.Calendar startCalendar = DateTime.calendarInstance();
		startCalendar.clear();
		startCalendar.set(2026, java.util.Calendar.JANUARY, 31);
		long start = startCalendar.getTimeInMillis();
		java.util.Calendar weekVisibleStartCalendar = (java.util.Calendar) startCalendar.clone();
		weekVisibleStartCalendar.add(java.util.Calendar.DAY_OF_MONTH, 2);
		long weekVisibleStart = weekVisibleStartCalendar.getTimeInMillis();
		java.util.Calendar weekVisibleEndCalendar = (java.util.Calendar) startCalendar.clone();
		weekVisibleEndCalendar.add(java.util.Calendar.DAY_OF_MONTH, 22);
		java.util.List<Long> expectedWeeks = new java.util.ArrayList<>();
		java.util.Calendar expectedWeek = (java.util.Calendar) startCalendar.clone();
		for (int index = 0; index < 4; index++) {
			expectedWeek.add(java.util.Calendar.WEEK_OF_YEAR, 1);
			if (expectedWeek.getTimeInMillis() >= weekVisibleStart
				&& expectedWeek.getTimeInMillis() <= weekVisibleEndCalendar.getTimeInMillis())
				expectedWeeks.add(Long.valueOf(expectedWeek.getTimeInMillis()));
		}
		var weekly = new ProgressLineSettings(false, ProgressLineSettings.ReferenceDate.STATUS_DATE,
			true, 1, ProgressLineSettings.IntervalUnit.WEEK);
		assertEquals(expectedWeeks, GanttProgress.progressLineReferenceDates(start,
			weekVisibleEndCalendar.getTimeInMillis(), -1L, weekly, weekVisibleStart,
			weekVisibleEndCalendar.getTimeInMillis()));

		java.util.Calendar monthVisibleStartCalendar = (java.util.Calendar) startCalendar.clone();
		monthVisibleStartCalendar.set(java.util.Calendar.MONTH, java.util.Calendar.FEBRUARY);
		monthVisibleStartCalendar.set(java.util.Calendar.DAY_OF_MONTH, 1);
		java.util.Calendar monthVisibleEndCalendar = (java.util.Calendar) startCalendar.clone();
		monthVisibleEndCalendar.set(java.util.Calendar.MONTH, java.util.Calendar.APRIL);
		monthVisibleEndCalendar.set(java.util.Calendar.DAY_OF_MONTH, 30);
		java.util.List<Long> expectedMonths = new java.util.ArrayList<>();
		for (int index = 1; index <= 3; index++) {
			java.util.Calendar monthDate = (java.util.Calendar) startCalendar.clone();
			monthDate.add(java.util.Calendar.MONTH, index);
			if (monthDate.getTimeInMillis() >= monthVisibleStartCalendar.getTimeInMillis()
				&& monthDate.getTimeInMillis() <= monthVisibleEndCalendar.getTimeInMillis())
				expectedMonths.add(Long.valueOf(monthDate.getTimeInMillis()));
		}
		var monthly = new ProgressLineSettings(false, ProgressLineSettings.ReferenceDate.STATUS_DATE,
			true, 1, ProgressLineSettings.IntervalUnit.MONTH);
		assertEquals(expectedMonths, GanttProgress.progressLineReferenceDates(start,
			monthVisibleEndCalendar.getTimeInMillis(), -1L, monthly,
			monthVisibleStartCalendar.getTimeInMillis(), monthVisibleEndCalendar.getTimeInMillis()));
	}

	@Test
	void recurringProgressLinesHaveNoTenThousandDateCapAndRespectVisibleBounds() {
		java.util.Calendar startCalendar = DateTime.calendarInstance();
		startCalendar.clear();
		startCalendar.set(2000, java.util.Calendar.JANUARY, 1);
		long projectStart = startCalendar.getTimeInMillis();
		java.util.Calendar endCalendar = (java.util.Calendar) startCalendar.clone();
		endCalendar.add(java.util.Calendar.DAY_OF_MONTH, 10_001);
		long projectEnd = endCalendar.getTimeInMillis();
		ProgressLineSettings recurringDaily = new ProgressLineSettings(false,
			ProgressLineSettings.ReferenceDate.STATUS_DATE, true, 1, ProgressLineSettings.IntervalUnit.DAY);
		assertEquals(10_002, GanttProgress.progressLineReferenceDates(projectStart, projectEnd, 0L, recurringDaily).size(),
			"long projects must not silently lose recurring lines after an arbitrary count");

		java.util.Calendar visibleStartCalendar = (java.util.Calendar) startCalendar.clone();
		visibleStartCalendar.add(java.util.Calendar.DAY_OF_MONTH, 10_000);
		long visibleStart = visibleStartCalendar.getTimeInMillis();
		java.util.Calendar visibleEndCalendar = (java.util.Calendar) visibleStartCalendar.clone();
		visibleEndCalendar.add(java.util.Calendar.DAY_OF_MONTH, 4);
		long visibleEnd = visibleEndCalendar.getTimeInMillis();
		assertEquals(java.util.List.of(visibleStart, visibleStart + 86_400_000L),
			GanttProgress.progressLineReferenceDates(projectStart, projectEnd, 0L, recurringDaily, visibleStart, visibleEnd));
	}

	@Test
	void dailyProgressLinesStayCalendarAlignedAcrossDstTransitionDates() {
		java.util.Calendar startCalendar = DateTime.calendarInstance(); // Project dates use GMT, so local DST cannot shift the interval.
		startCalendar.clear();
		startCalendar.set(2026, java.util.Calendar.MARCH, 7);
		long start = startCalendar.getTimeInMillis();
		java.util.Calendar endCalendar = (java.util.Calendar) startCalendar.clone();
		endCalendar.add(java.util.Calendar.DAY_OF_MONTH, 4);
		var dates = GanttProgress.progressLineReferenceDates(start, endCalendar.getTimeInMillis(), 0L,
			new ProgressLineSettings(false, ProgressLineSettings.ReferenceDate.STATUS_DATE,
				true, 1, ProgressLineSettings.IntervalUnit.DAY));
		assertEquals(5, dates.size());
		for (int index = 0; index < dates.size(); index++)
			assertEquals(start + index * 86_400_000L, dates.get(index).longValue(), "GMT daily cadence must remain 24 hours");
	}

	private static Schedule taskSpecificSchedule(final double percentComplete, final double percentWorkComplete, final boolean wbsParent) {
		return taskSpecificSchedule(percentComplete, percentWorkComplete, wbsParent, 20L, 0L, 100L);
	}

	private static Schedule taskSpecificSchedule(final double percentComplete, final double percentWorkComplete,
			final boolean wbsParent, final long completedThrough, final long start, final long end) {
		InvocationHandler handler = new InvocationHandler() {
			public Object invoke(Object proxy, Method method, Object[] args) {
				String name = method.getName();
				if ("getPercentComplete".equals(name))
					return Double.valueOf(percentComplete);
				if ("getPercentWorkComplete".equals(name))
					return Double.valueOf(percentWorkComplete);
				if ("isWbsParent".equals(name))
					return Boolean.valueOf(wbsParent);
				if ("getCompletedThrough".equals(name))
					return Long.valueOf(completedThrough);
				if ("getStart".equals(name))
					return Long.valueOf(start);
				if ("getEnd".equals(name))
					return Long.valueOf(end);
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
