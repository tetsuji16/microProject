package com.microproject.pm.graphic.gantt;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GanttSelectionGeometrySupportTest {
	@Test
	void milestoneSelectionUsesTheLargerOfShapeHeightAndSelectionSquare() {
		assertEquals(94.0d, GanttSelectionGeometrySupport.milestoneSelectionStart(100.0d, 8.0d, 12.0d), 0.00001d);
		assertEquals(106.0d, GanttSelectionGeometrySupport.milestoneSelectionEnd(100.0d, 8.0d, 12.0d), 0.00001d);
	}

	@Test
	void milestoneSelectionUsesShapeHeightWhenItExceedsSelectionSquare() {
		assertEquals(90.0d, GanttSelectionGeometrySupport.milestoneSelectionStart(100.0d, 20.0d, 12.0d), 0.00001d);
		assertEquals(110.0d, GanttSelectionGeometrySupport.milestoneSelectionEnd(100.0d, 20.0d, 12.0d), 0.00001d);
	}
}
