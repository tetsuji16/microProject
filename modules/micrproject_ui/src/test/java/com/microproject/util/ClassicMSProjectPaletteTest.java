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
