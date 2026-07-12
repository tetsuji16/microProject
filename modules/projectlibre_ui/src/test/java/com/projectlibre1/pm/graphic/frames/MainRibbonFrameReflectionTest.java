package com.projectlibre1.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Arrays;

import javax.swing.JFrame;

import org.junit.jupiter.api.Test;

class MainRibbonFrameReflectionTest {
	@Test
	void mainRibbonFrameIsNowAJFrameBackedWindow() {
		assertEquals(JFrame.class, MainRibbonFrame.class.getSuperclass());
	}

	@Test
	void mainRibbonFrameExposesRibbonPanelStorage() {
		assertTrue(Arrays.stream(MainRibbonFrame.class.getDeclaredMethods())
			.anyMatch(method -> method.getName().equals("getRibbonPanel") && method.getParameterCount() == 0));
		assertTrue(Arrays.stream(MainRibbonFrame.class.getDeclaredMethods())
			.anyMatch(method -> method.getName().equals("setRibbonPanel") && method.getParameterCount() == 1));
	}

	@Test
	void mainRibbonFrameNoLongerKeepsSeparateApplicationMenuIconField() {
		assertFalse(Arrays.stream(MainRibbonFrame.class.getDeclaredFields())
			.map(Field::getName)
			.anyMatch("appMenuIcon"::equals));
	}
}
