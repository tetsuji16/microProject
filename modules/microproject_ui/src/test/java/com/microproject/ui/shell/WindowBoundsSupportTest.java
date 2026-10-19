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

	@Test
	void restoreTransitionCannotReplaceLastNormalBoundsWithMaximizedGeometry() {
		Rectangle normal = new Rectangle(120, 90, 900, 650);
		Rectangle maximized = new Rectangle(0, 0, 1280, 720);
		WindowBoundsSupport.NormalBoundsSnapshot snapshot =
			new WindowBoundsSupport.NormalBoundsSnapshot(normal, false);

		snapshot.stateChanged(true);
		snapshot.geometryChanged(true, maximized);
		snapshot.stateChanged(false);
		snapshot.geometryChanged(false, maximized);

		assertEquals(normal, snapshot.settled(false, maximized));
	}

	@Test
	void normalBoundsCanChangeAfterRestoreHasSettled() {
		Rectangle normal = new Rectangle(120, 90, 900, 650);
		Rectangle restored = new Rectangle(130, 100, 910, 660);
		Rectangle maximized = new Rectangle(0, 0, 1280, 720);
		WindowBoundsSupport.NormalBoundsSnapshot snapshot =
			new WindowBoundsSupport.NormalBoundsSnapshot(normal, false);

		snapshot.stateChanged(true);
		snapshot.stateChanged(false);
		assertEquals(normal, snapshot.settled(false, maximized));
		snapshot.geometryChanged(false, restored);

		assertEquals(null, snapshot.settled(false, restored));
		snapshot.stateChanged(true);
		snapshot.stateChanged(false);
		assertEquals(restored, snapshot.settled(false, maximized));
	}
}
