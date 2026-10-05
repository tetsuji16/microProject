/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
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
package com.microproject.pm.graphic.views;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

class GanttRowHeightSynchronizationTest {
	private record RowHeightCase(String name, Integer[] baselines, int defaultHeight,
		int baselineHeight, int expected) {}

	@TestFactory
	Stream<DynamicTest> taskTableAndGanttRowHeightCases() {
		List<RowHeightCase> cases = List.of(
			new RowHeightCase("null-baselines", null, 20, 5, 20),
			new RowHeightCase("empty-baselines", new Integer[0], 20, 5, 20),
			new RowHeightCase("first-baseline-row", new Integer[] { 0 }, 20, 5, 25),
			new RowHeightCase("sparse-indices-use-highest-row", new Integer[] { 1, 3 }, 60, 15, 120),
			new RowHeightCase("zero-default-height", new Integer[] { 3 }, 0, 10, 40),
			new RowHeightCase("zero-baseline-height", new Integer[] { 0, 100 }, 25, 0, 25));
		return cases.stream().map(c -> DynamicTest.dynamicTest(c.name, () -> {
			SortedSet<Integer> baselines = c.baselines == null
				? null : new TreeSet<>(Arrays.asList(c.baselines));
			assertEquals(c.expected, TaskGanttSyncSupport.calculateRowHeight(
				baselines, c.defaultHeight, c.baselineHeight));
		}));
	}
}
