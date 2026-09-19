/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.graphic.chart;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ChartModelResourceGraphTest {
	@Test
	void overallocatedSeriesContainsOnlyWorkAboveAvailability() {
		assertEquals(0D, ChartModel.overallocatedAmount(8D, 8D));
		assertEquals(0D, ChartModel.overallocatedAmount(6D, 8D));
		assertEquals(2.5D, ChartModel.overallocatedAmount(10.5D, 8D));
	}
}
