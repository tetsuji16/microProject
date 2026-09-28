/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package com.microproject.pm.assignment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.algorithm.ReverseQuery;
import com.microproject.pm.calendar.WorkCalendar;
import com.microproject.pm.costing.EarnedValueValues;

class TimeDistributedDataConsolidatorTest {
	@Test
	void sumsEachTimeDistributedMetricAndRetainsLaborOnlyFiltering() {
		StubData labor = new StubData(true, 11, 3, 8, 13, 1.25, 2.25, 3.25, 4.25, 5.25,
			6.25, 7.25, 8.25, 9.25);
		StubData nonLabor = new StubData(false, 17, 5, 12, 19, 14.5, 15.5, 16.5, 17.5,
			18.5, 19.5, 20.5, 21.5, 22.5);
		List<StubData> values = List.of(labor, nonLabor);

		assertEquals(25.75, TimeDistributedDataConsolidator.acwp(10, 20, values));
		assertEquals(27.75, TimeDistributedDataConsolidator.bac(10, 20, values));
		assertEquals(29.75, TimeDistributedDataConsolidator.bcwp(10, 20, values));
		assertEquals(31.75, TimeDistributedDataConsolidator.bcws(10, 20, values));
		assertEquals(15.75, TimeDistributedDataConsolidator.cost(10, 20, values));
		assertEquals(17.75, TimeDistributedDataConsolidator.actualCost(10, 20, values));
		assertEquals(19.75, TimeDistributedDataConsolidator.actualFixedCost(10, 20, values));
		assertEquals(21.75, TimeDistributedDataConsolidator.fixedCost(10, 20, values));
		assertEquals(23.75, TimeDistributedDataConsolidator.baselineCost(10, 20, values));
		assertEquals(11, TimeDistributedDataConsolidator.work(10, 20, values, true));
		assertEquals(28, TimeDistributedDataConsolidator.work(10, 20, values, false));
		assertEquals(3, TimeDistributedDataConsolidator.actualWork(10, 20, values, true));
		assertEquals(8, TimeDistributedDataConsolidator.actualWork(10, 20, values, false));
		assertEquals(20, TimeDistributedDataConsolidator.remainingWork(10, 20, values, false));
		assertEquals(8, TimeDistributedDataConsolidator.remainingWork(10, 20, values, true));
		assertEquals(13, TimeDistributedDataConsolidator.baselineWork(10, 20, values, true));
		assertEquals(32, TimeDistributedDataConsolidator.baselineWork(10, 20, values, false));
		assertEquals(0.0, TimeDistributedDataConsolidator.cost(10, 20, List.of()));
		assertEquals(0, TimeDistributedDataConsolidator.work(10, 20, List.of(), false));
	}

	private record StubData(boolean labor, long workValue, long actualWorkValue,
		long remainingWorkValue, long baselineWorkValue, double costValue, double actualCostValue,
		double actualFixedCostValue, double fixedCostValue, double baselineCostValue,
		double acwpValue, double bacValue, double bcwpValue, double bcwsValue)
		implements HasTimeDistributedData, EarnedValueValues {
		@Override public boolean isLabor() { return labor; }
		@Override public void buildReverseQuery(ReverseQuery reverseQuery) { }
		@Override public void forEachWorkingInterval(java.util.function.Consumer<Object> visitor,
			boolean mergeWorking, WorkCalendar workCalendar) { }
		@Override public double cost(long start, long end) { return costValue; }
		@Override public double actualCost(long start, long end) { return actualCostValue; }
		@Override public double actualFixedCost(long start, long end) { return actualFixedCostValue; }
		@Override public double fixedCost(long start, long end) { return fixedCostValue; }
		@Override public double baselineCost(long start, long end) { return baselineCostValue; }
		@Override public long work(long start, long end) { return workValue; }
		@Override public long baselineWork(long start, long end) { return baselineWorkValue; }
		@Override public long actualWork(long start, long end) { return actualWorkValue; }
		@Override public long remainingWork(long start, long end) { return remainingWorkValue; }
		@Override public java.util.Collection<?> childrenToRollup() { return List.of(); }
		@Override public double acwp(long start, long end) { return acwpValue; }
		@Override public double bac(long start, long end) { return bacValue; }
		@Override public double bcwp(long start, long end) { return bcwpValue; }
		@Override public double bcws(long start, long end) { return bcwsValue; }
	}
}
