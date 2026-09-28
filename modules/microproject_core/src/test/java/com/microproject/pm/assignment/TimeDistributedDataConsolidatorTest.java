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
	void aggregatesValuesAndRetainsLaborOnlyFiltering() {
		StubData labor = new StubData(true, 11, 3, 8, 2.5);
		StubData nonLabor = new StubData(false, 17, 5, 12, 4.5);

		assertEquals(7.0, TimeDistributedDataConsolidator.acwp(10, 20, List.of(labor, nonLabor)));
		assertEquals(7.0, TimeDistributedDataConsolidator.bac(10, 20, List.of(labor, nonLabor)));
		assertEquals(7.0, TimeDistributedDataConsolidator.bcwp(10, 20, List.of(labor, nonLabor)));
		assertEquals(7.0, TimeDistributedDataConsolidator.bcws(10, 20, List.of(labor, nonLabor)));
		assertEquals(7.0, TimeDistributedDataConsolidator.baselineCost(10, 20, List.of(labor, nonLabor)));
		assertEquals(11, TimeDistributedDataConsolidator.work(10, 20, List.of(labor, nonLabor), true));
		assertEquals(3, TimeDistributedDataConsolidator.actualWork(10, 20, List.of(labor, nonLabor), true));
		assertEquals(20, TimeDistributedDataConsolidator.remainingWork(10, 20, List.of(labor, nonLabor), false));
		assertEquals(0, TimeDistributedDataConsolidator.work(10, 20, List.of(), false));
	}

	private record StubData(boolean labor, long workValue, long actualWorkValue,
		long remainingWorkValue, double acwpValue)
		implements HasTimeDistributedData, EarnedValueValues {
		@Override public boolean isLabor() { return labor; }
		@Override public void buildReverseQuery(ReverseQuery reverseQuery) { }
		@Override public void forEachWorkingInterval(java.util.function.Consumer<Object> visitor,
			boolean mergeWorking, WorkCalendar workCalendar) { }
		@Override public double cost(long start, long end) { return 0; }
		@Override public double actualCost(long start, long end) { return 0; }
		@Override public double actualFixedCost(long start, long end) { return 0; }
		@Override public double fixedCost(long start, long end) { return 0; }
		@Override public double baselineCost(long start, long end) { return acwpValue; }
		@Override public long work(long start, long end) { return workValue; }
		@Override public long baselineWork(long start, long end) { return 0; }
		@Override public long actualWork(long start, long end) { return actualWorkValue; }
		@Override public long remainingWork(long start, long end) { return remainingWorkValue; }
		@Override public java.util.Collection<?> childrenToRollup() { return List.of(); }
		@Override public double acwp(long start, long end) { return acwpValue; }
		@Override public double bac(long start, long end) { return acwpValue; }
		@Override public double bcwp(long start, long end) { return acwpValue; }
		@Override public double bcws(long start, long end) { return acwpValue; }
	}
}
