package com.projectlibre1.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.reflect.Field;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.pushingpixels.flamingo.api.ribbon.JRibbonFrame;

class MainRibbonFrameReflectionTest {
	@Test
	void mainRibbonFrameUsesJRibbonFrameApplicationIconImplementation() throws Exception {
		assertEquals(JRibbonFrame.class,
			MainRibbonFrame.class.getMethod("getApplicationIcon").getDeclaringClass());

		assertFalse(Arrays.stream(MainRibbonFrame.class.getDeclaredMethods())
			.anyMatch(method -> method.getName().equals("getApplicationIcon") && method.getParameterCount() == 0));
	}

	@Test
	void mainRibbonFrameNoLongerKeepsSeparateApplicationMenuIconField() {
		assertFalse(Arrays.stream(MainRibbonFrame.class.getDeclaredFields())
			.map(Field::getName)
			.anyMatch("appMenuIcon"::equals));
	}
}
