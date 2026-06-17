package com.projectlibre1.pm.graphic.views.synchro;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Point;

import org.junit.jupiter.api.Test;

class ScrollPaneSynchronizerTest {
	@Test
	void resolveViewportLeftEdgeDateUsesCurrentViewportOffset() {
		double leftEdgeDate = ScrollPaneSynchronizer.resolveViewportLeftEdgeDate(x -> 1_000.0d + x * 2.0d, new Point(125, 0));

		assertEquals(1_250.0d, leftEdgeDate, 0.00001d);
	}

	@Test
	void resolveViewportLeftEdgeDateClampsNegativeViewportOffset() {
		double leftEdgeDate = ScrollPaneSynchronizer.resolveViewportLeftEdgeDate(x -> 500.0d + x, new Point(-25, 0));

		assertEquals(500.0d, leftEdgeDate, 0.00001d);
	}

	@Test
	void restoreViewportXRoundsConvertedCoordinate() {
		assertEquals(384, ScrollPaneSynchronizer.restoreViewportX(t -> t / 2.0d, 767.6d));
	}
}
