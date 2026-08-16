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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Paint;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.junit.jupiter.api.Test;

import com.microproject.pm.scheduling.Schedule;
import com.microproject.pm.task.TaskSpecificFields;

class ClassicMSProjectPaletteTest {
	@Test
	void statusColorFollowsClassicMsProjectStates() {
		ClassicMSProjectPalette palette = new ClassicMSProjectPalette();

		assertEquals(new Color(0x70, 0xAD, 0x47), palette.getStatusColor(schedule(1.0d), null));
		assertEquals(new Color(0xC9, 0xD8, 0xEA), palette.getStatusColor(schedule(0.0d), null));
		assertEquals(new Color(0x5B, 0x9B, 0xD5), palette.getStatusColor(schedule(0.42d), null));
	}

	@Test
	void createBarPaintUsesReadableGradientsForBarsAndBackgroundLayers() {
		ClassicMSProjectPalette palette = new ClassicMSProjectPalette();
		Paint fillPaint = palette.createBarPaint(new Color(0x5B, 0x9B, 0xD5), new Rectangle2D.Double(0, 0, 80, 12), false, false);
		Paint backgroundPaint = palette.createBarPaint(new Color(0x5B, 0x9B, 0xD5), new Rectangle2D.Double(0, 0, 80, 12), true, false);

		GradientPaint fillGradient = assertInstanceOf(GradientPaint.class, fillPaint);
		GradientPaint backgroundGradient = assertInstanceOf(GradientPaint.class, backgroundPaint);
		assertEquals(new Color(0x5B, 0x9B, 0xD5), palette.getTaskBar(new Color(0x5B, 0x9B, 0xD5)));
		assertTrue(fillGradient.getColor1().getRed() >= fillGradient.getColor2().getRed() - 40);
		assertTrue(backgroundGradient.getColor1().getAlpha() < 255);
		assertTrue(backgroundGradient.getColor2().getAlpha() < 255);
	}

	private static Schedule schedule(final double percentComplete) {
		InvocationHandler handler = new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) {
				String name = method.getName();
				if ("getPercentComplete".equals(name)) {
					return Double.valueOf(percentComplete);
				}
				if ("equals".equals(name)) {
					return Boolean.valueOf(proxy == args[0]);
				}
				if ("hashCode".equals(name)) {
					return Integer.valueOf(System.identityHashCode(proxy));
				}
				Class<?> returnType = method.getReturnType();
				if (returnType == Boolean.TYPE) {
					return Boolean.FALSE;
				}
				if (returnType == Integer.TYPE) {
					return Integer.valueOf(0);
				}
				if (returnType == Long.TYPE) {
					return Long.valueOf(0L);
				}
				if (returnType == Double.TYPE) {
					return Double.valueOf(0.0d);
				}
				return null;
			}
		};
		return (Schedule) Proxy.newProxyInstance(
				ClassicMSProjectPaletteTest.class.getClassLoader(),
				new Class<?>[] { Schedule.class, TaskSpecificFields.class },
				handler);
	}
}
