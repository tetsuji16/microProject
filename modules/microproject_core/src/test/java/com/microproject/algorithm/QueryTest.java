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
package com.microproject.algorithm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

	@Test
	void emptySelectFromClauseCompletesWithoutDereferencingAMissingGenerator() {
		IntervalGenerator[] intervals = Query.getInstance()
				.selectFrom(SelectFrom.getInstance())
				.execute();

		assertEquals(1, intervals.length);
	}

	@Test
	void whereInRangeIntersectsAnExistingRangeAndRejectsBackwardsRanges() {
		SelectFrom selectFrom = SelectFrom.getInstance()
				.whereInRange(10L, 30L)
				.whereInRange(15L, 25L);

		assertTrue(selectFrom.wherePredicate instanceof DateInRangePredicate);
		DateInRangePredicate range = (DateInRangePredicate) selectFrom.wherePredicate;
		assertEquals(15L, range.getStart());
		assertEquals(25L, range.getEnd());

		selectFrom.whereInRange(26L, 24L);

		assertSame(org.apache.commons.collections.functors.FalsePredicate.INSTANCE, selectFrom.wherePredicate);
	}
}
