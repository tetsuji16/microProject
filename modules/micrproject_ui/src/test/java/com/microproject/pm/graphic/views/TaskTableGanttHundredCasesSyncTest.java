package com.microproject.pm.graphic.views;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

class TaskTableGanttHundredCasesSyncTest {
	private record RowHeightCase(Integer[] baselines, int defaultHeight, int baselineHeight, int expected) {}

	@TestFactory
	Stream<DynamicTest> taskTableAndGanttRowHeightCases() {
		List<RowHeightCase> cases = List.of(
			new RowHeightCase(null, 20, 5, 20),
			new RowHeightCase(new Integer[0], 20, 5, 20),
			new RowHeightCase(new Integer[] { 0 }, 20, 5, 25),
			new RowHeightCase(new Integer[] { 1 }, 20, 5, 30),
			new RowHeightCase(new Integer[] { 2 }, 20, 5, 35),
			new RowHeightCase(new Integer[] { 0, 2 }, 20, 5, 35),
			new RowHeightCase(new Integer[] { 1, 3 }, 60, 15, 120),
			new RowHeightCase(new Integer[] { 0, 1, 2, 3 }, 10, 2, 18),
			new RowHeightCase(new Integer[] { 5 }, 24, 3, 42),
			new RowHeightCase(new Integer[] { 0, 5 }, 24, 3, 42),
			new RowHeightCase(new Integer[] { 10 }, 1, 1, 12),
			new RowHeightCase(new Integer[] { 2, 7, 10 }, 1, 1, 12),
			new RowHeightCase(new Integer[] { 0 }, 0, 10, 10),
			new RowHeightCase(new Integer[] { 3 }, 0, 10, 40),
			new RowHeightCase(new Integer[] { 100 }, 25, 0, 25),
			new RowHeightCase(new Integer[] { 0, 100 }, 25, 0, 25),
			new RowHeightCase(new Integer[] { 20 }, 18, 2, 60),
			new RowHeightCase(new Integer[] { 4, 8, 12 }, 30, 4, 82),
			new RowHeightCase(new Integer[] { 31 }, 16, 1, 48),
			new RowHeightCase(new Integer[] { 0, 15, 31 }, 16, 1, 48));
		return IntStream.range(0, cases.size()).mapToObj(index -> DynamicTest.dynamicTest(
			"TC100-V" + String.format("%03d", index + 81), () -> {
				RowHeightCase c = cases.get(index);
				SortedSet<Integer> baselines = c.baselines == null
					? null : new TreeSet<>(Arrays.asList(c.baselines));
				assertEquals(c.expected, TaskGanttSyncSupport.calculateRowHeight(
					baselines, c.defaultHeight, c.baselineHeight));
			}));
	}
}
