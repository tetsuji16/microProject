/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.ui.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Rectangle;

import org.junit.jupiter.api.Test;

class WindowBoundsSupportTest {
	@Test
	void movesWindowInsideUsableAreaWithoutResizingIt() {
		Rectangle original = new Rectangle(87, 87, 900, 650);

		Rectangle fitted = WindowBoundsSupport.fittedBounds(original, new Rectangle(0, 0, 1280, 672));

		assertEquals(new Rectangle(87, 22, 900, 650), fitted);
		assertEquals(new Rectangle(87, 87, 900, 650), original, "bounds calculation must not mutate the caller's rectangle");
	}

	@Test
	void shrinksOversizedWindowAndKeepsItWithinOffsetMonitorWorkArea() {
		Rectangle fitted = WindowBoundsSupport.fittedBounds(
			new Rectangle(500, 600, 1200, 900), new Rectangle(1920, 40, 1280, 680));

		assertEquals(new Rectangle(1920, 40, 1200, 680), fitted);
	}

	@Test
	void leavesWindowAloneWhenAlreadyInsideUsableArea() {
		Rectangle original = new Rectangle(240, 60, 900, 650);

		assertEquals(original, WindowBoundsSupport.fittedBounds(original, new Rectangle(0, 0, 1280, 720)));
	}
}
