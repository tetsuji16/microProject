/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.graphic.gantt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Rectangle;

import org.junit.jupiter.api.Test;

class GanttRendererVisibilityTest {
	@Test
	void clipsAreBoundedToVisibleRowsBeforeCreatingAnIterator() {
		assertEquals(0, GanttRenderer.clampVisibleRow(-1, 10));
		assertEquals(2, GanttRenderer.clampVisibleRow(2, 10));
		assertEquals(10, GanttRenderer.clampVisibleRow(15, 10));
		assertEquals(0, GanttRenderer.clampVisibleRow(-3, 0));
	}


	@Test
	void skipsDependencyWhoseEndpointRowsAreOutsideThePaintClip() {
		Rectangle clip = new Rectangle(0, 100, 800, 120);

		assertFalse(GanttRenderer.isDependencyPotentiallyVisible(0, 1, 24, clip));
		assertTrue(GanttRenderer.isDependencyPotentiallyVisible(5, 7, 24, clip));
	}

	@Test
	void keepsLinksThatTouchTheExpandedClipBoundary() {
		Rectangle clip = new Rectangle(0, 100, 800, 120);

		assertTrue(GanttRenderer.isDependencyPotentiallyVisible(3, 3, 24, clip));
		assertTrue(GanttRenderer.isDependencyPotentiallyVisible(9, 9, 24, clip));
	}
}
