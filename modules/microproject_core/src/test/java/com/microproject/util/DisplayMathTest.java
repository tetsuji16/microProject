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
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.pm.scheduling.Schedule;
import com.microproject.pm.scheduling.ScheduleInterval;

class DisplayMathTest {
	@Test
	void clampProgressRatioUsesSchedulePercentComplete() {
		assertEquals(0.44d, DisplayMath.clampProgressRatio(schedule(0.44d)), 0.00001d);
	}

	@Test
	void clampProgressRatioClampsOutOfRangeValues() {
		assertEquals(0.0d, DisplayMath.clampProgressRatio(schedule(-0.20d)), 0.00001d);
		assertEquals(1.0d, DisplayMath.clampProgressRatio(schedule(1.50d)), 0.00001d);
	}

	@Test
	void clampProgressRatioFallsBackToZeroWhenScheduleIsMissing() {
		assertEquals(0.0d, DisplayMath.clampProgressRatio(null), 0.00001d);
	}

	@Test
	void progressWidthIsProportionalToRatio() {
		assertEquals(50.0d, DisplayMath.progressWidth(100.0d, 0.5d), 0.00001d);
		assertEquals(10.0d, DisplayMath.progressWidth(100.0d, 0.1d), 0.00001d);
	}

	@Test
	void mergeIntervalsReturnsSingleEnvelopeBar() {
		ScheduleInterval merged = DisplayMath.mergeIntervals(List.of(
				new ScheduleInterval(30L, 50L),
				new ScheduleInterval(10L, 20L),
				new ScheduleInterval(70L, 90L)));
		assertEquals(10L, merged.getStart());
		assertEquals(90L, merged.getEnd());
	}

	@Test
	void mergeIntervalsReturnsNullWhenEmpty() {
		assertNull(DisplayMath.mergeIntervals(List.of()));
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
				DisplayMathTest.class.getClassLoader(),
				new Class<?>[] { Schedule.class },
				handler);
	}
}
