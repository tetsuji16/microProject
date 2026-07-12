package com.projectlibre1.algorithm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class QueryTest {
	@Test
	void executeReturnsTheIntervalsVisitedByTheQuery() {
		IntervalGenerator[] intervals = Query.getInstance()
				.groupBy(RangeIntervalGenerator.getInstance(10L, 20L))
				.execute();

		assertNotNull(intervals);
		assertEquals(1, intervals.length);
		assertEquals(10L, intervals[0].currentStart());
		assertEquals(20L, intervals[0].currentEnd());
	}
}
