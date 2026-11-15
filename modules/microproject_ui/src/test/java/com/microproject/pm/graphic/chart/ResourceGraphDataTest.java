/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.chart;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.microproject.pm.graphic.chart.ResourceGraphData.ValueKind;
import com.microproject.pm.graphic.chart.ResourceGraphSettings.GraphType;
import com.microproject.pm.graphic.chart.ResourceGraphSettings.Item;

/** The series each Graph option of the Resource Graph Format tab plots, on three intervals of plain numbers. */
class ResourceGraphDataTest {
	private static final double H = 3_600_000D;
	private static final double DELTA = 1e-9;
	private static final long[] DATES = { 0L, 1L, 2L };
	private static final double[] TOTAL = { 8 * H, 12 * H, 4 * H };
	private static final double[] SELECTED = { 4 * H, 6 * H, 0D };
	private static final double[] AVAILABILITY = { 8 * H, 8 * H, 8 * H };
	private static final double[] COST = { 100D, 200D, 50D };
	private static final double[] SELECTED_COST = { 40D, 60D, 0D };
	private static final double UNIT = 8 * H;

	private static ResourceGraphData compute(GraphType type, boolean stack) {
		return ResourceGraphData.compute(type, DATES, TOTAL, SELECTED, AVAILABILITY, COST, SELECTED_COST, UNIT, stack);
	}

	private static void assertY(double[] expected, ResourceGraphData data, Item item) {
		ResourceGraphData.Series series = data.getSeries(item);
		assertNotNull(series, item.name());
		assertArrayEquals(expected, series.y(), DELTA, item.name());
	}

	@Test
	void peakUnitsPlotsTheUnitsEachIntervalNeedsAgainstTheAvailableUnits() {
		ResourceGraphData data = compute(GraphType.PEAK_UNITS, false);
		assertEquals(ValueKind.UNITS, data.getKind());
		assertY(new double[] { 1D, 1.5D, 0.5D }, data, Item.ALLOCATED_FILTERED);
		assertY(new double[] { 0.5D, 0.75D, 0D }, data, Item.ALLOCATED_RESOURCE);
		assertY(new double[] { 0.5D }, data, Item.OVERALLOCATED_FILTERED);
		assertArrayEquals(new double[] { 1D }, data.getSeries(Item.OVERALLOCATED_FILTERED).x(), DELTA);
		assertY(new double[] { 0.5D }, data, Item.OVERALLOCATED_RESOURCE);
		assertArrayEquals(new double[] { 0, 1, 1, 1, 1, 1 }, data.getAvailabilityY(), DELTA);
		assertArrayEquals(new double[] { 0, 0, 1, 1, 2, 2 }, data.getAvailabilityX(), DELTA);
	}

	@Test
	void workPlotsHoursPerInterval() {
		ResourceGraphData data = compute(GraphType.WORK, false);
		assertEquals(ValueKind.HOURS, data.getKind());
		assertY(new double[] { 8, 12, 4 }, data, Item.ALLOCATED_FILTERED);
		assertY(new double[] { 4, 6, 0 }, data, Item.ALLOCATED_RESOURCE);
		assertY(new double[] { 4 }, data, Item.OVERALLOCATED_FILTERED);
		assertArrayEquals(new double[] { 0, 8, 8, 8, 8, 8 }, data.getAvailabilityY(), DELTA);
	}

	@Test
	void cumulativeWorkAddsUpTheIntervals() {
		ResourceGraphData data = compute(GraphType.CUMULATIVE_WORK, false);
		assertEquals(ValueKind.HOURS, data.getKind());
		assertY(new double[] { 8, 20, 24 }, data, Item.ALLOCATED_FILTERED);
		assertY(new double[] { 4, 10, 10 }, data, Item.ALLOCATED_RESOURCE);
		assertY(new double[] { 4, 4 }, data, Item.OVERALLOCATED_FILTERED);
		assertArrayEquals(new double[] { 0, 8, 8, 16, 16, 24 }, data.getAvailabilityY(), DELTA);
	}

	@Test
	void overallocationPlotsOnlyTheWorkAboveTheAvailability() {
		ResourceGraphData data = compute(GraphType.OVERALLOCATION, false);
		assertEquals(ValueKind.HOURS, data.getKind());
		assertNull(data.getSeries(Item.ALLOCATED_FILTERED));
		assertNull(data.getSeries(Item.ALLOCATED_RESOURCE));
		assertY(new double[] { 4 }, data, Item.OVERALLOCATED_FILTERED);
		assertY(new double[] { 4 }, data, Item.OVERALLOCATED_RESOURCE);
		assertEquals(0, data.getAvailabilityX().length);
	}

	@Test
	void percentAllocationIsAFractionOfTheAvailability() {
		ResourceGraphData data = compute(GraphType.PERCENT_ALLOCATION, false);
		assertEquals(ValueKind.UNITS, data.getKind());
		assertY(new double[] { 1D, 1.5D, 0.5D }, data, Item.ALLOCATED_FILTERED);
		assertY(new double[] { 0.5D, 0.75D, 0D }, data, Item.ALLOCATED_RESOURCE);
		assertY(new double[] { 0.5D }, data, Item.OVERALLOCATED_FILTERED);
		assertArrayEquals(new double[] { 0, 1, 1, 1, 1, 1 }, data.getAvailabilityY(), DELTA);
	}

	@Test
	void percentAllocationWithoutAvailabilityFallsBackToOneFullTimeUnit() {
		ResourceGraphData data = ResourceGraphData.compute(GraphType.PERCENT_ALLOCATION, new long[] { 0L }, new double[] { 4 * H },
			new double[0], new double[] { 0D }, null, null, UNIT, false);
		assertY(new double[] { 0.5D }, data, Item.ALLOCATED_FILTERED);
		assertEquals(0, data.getSeries(Item.OVERALLOCATED_FILTERED).x().length);
	}

	@Test
	void remainingAvailabilityIsWhatTheAssignedWorkLeavesFree() {
		ResourceGraphData data = compute(GraphType.REMAINING_AVAILABILITY, false);
		assertEquals(ValueKind.HOURS, data.getKind());
		assertY(new double[] { 0, 0, 4 }, data, Item.ALLOCATED_FILTERED);
		assertY(new double[] { 4, 2, 8 }, data, Item.ALLOCATED_RESOURCE);
		assertNull(data.getSeries(Item.OVERALLOCATED_FILTERED));
	}

	@Test
	void costAndCumulativeCostPlotMoney() {
		ResourceGraphData cost = compute(GraphType.COST, false);
		assertEquals(ValueKind.MONEY, cost.getKind());
		assertY(new double[] { 100, 200, 50 }, cost, Item.ALLOCATED_FILTERED);
		assertY(new double[] { 40, 60, 0 }, cost, Item.ALLOCATED_RESOURCE);
		ResourceGraphData cumulative = compute(GraphType.CUMULATIVE_COST, false);
		assertEquals(ValueKind.MONEY, cumulative.getKind());
		assertY(new double[] { 100, 300, 350 }, cumulative, Item.ALLOCATED_FILTERED);
		assertY(new double[] { 40, 100, 100 }, cumulative, Item.ALLOCATED_RESOURCE);
	}

	@Test
	void workAvailabilityAndUnitAvailabilityPlotTheCapacity() {
		ResourceGraphData work = compute(GraphType.WORK_AVAILABILITY, false);
		assertEquals(ValueKind.HOURS, work.getKind());
		assertY(new double[] { 8, 8, 8 }, work, Item.ALLOCATED_FILTERED);
		ResourceGraphData units = compute(GraphType.UNIT_AVAILABILITY, false);
		assertEquals(ValueKind.UNITS, units.getKind());
		assertY(new double[] { 1, 1, 1 }, units, Item.ALLOCATED_FILTERED);
	}

	@Test
	void stackingDrawsTheSelectedPortionOnTopOfTheRest() {
		ResourceGraphData data = compute(GraphType.WORK, true);
		ResourceGraphData.Series resource = data.getSeries(Item.ALLOCATED_RESOURCE);
		assertArrayEquals(new double[] { 8, 12, 4 }, resource.y(), DELTA);
		assertArrayEquals(new double[] { 4, 6, 0 }, resource.raw(), DELTA);
		assertY(new double[] { 4, 6, 4 }, data, Item.ALLOCATED_FILTERED);
		assertArrayEquals(new double[] { 8, 12, 4 }, data.getSeries(Item.ALLOCATED_FILTERED).raw(), DELTA);
	}

	@Test
	void everyGraphTypeComputesOnEmptyAndMissingInput() {
		for (GraphType type : GraphType.values()) {
			ResourceGraphData empty = ResourceGraphData.compute(type, new long[0], null, null, null, null, null, 0D, false);
			assertEquals(type, empty.getType());
			for (ResourceGraphData.Series series : empty.getSeries())
				assertTrue(series.isEmpty(), type + " " + series.item());
			assertEquals(0, empty.getAvailabilityX().length, type.name());
			assertEquals(GraphType.PEAK_UNITS,
				ResourceGraphData.compute(null, DATES, TOTAL, SELECTED, AVAILABILITY, COST, SELECTED_COST, UNIT, false).getType());
		}
	}
}
