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
package com.microproject.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.junit.jupiter.api.Test;

import com.microproject.pm.scheduling.Schedule;
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
