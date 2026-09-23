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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class IntervalGeneratorContractTest {
	@Test
	void rangeGeneratorExposesItsCurrentInterval() {
		RangeIntervalGenerator generator = RangeIntervalGenerator.getInstance(10L, 20L);

		assertSame(generator, generator.current());
		assertEquals(10L, generator.currentStart());
		assertEquals(20L, generator.currentEnd());
	}

	@Test
	void instantGeneratorExposesItsCurrentInterval() {
		InstantIntervalGenerator generator = InstantIntervalGenerator.getInstance(10L);

		assertSame(generator, generator.current());
		assertEquals(0L, generator.currentStart());
		assertEquals(10L, generator.currentEnd());
	}

	@Test
	void evaluatesOnlyUniqueEarliestEndingGenerator() {
		StubIntervalGenerator earliest = new StubIntervalGenerator(10L, true);
		StubIntervalGenerator later = new StubIntervalGenerator(20L, true);
		IntervalGeneratorSet generators = IntervalGeneratorSet.getInstance(List.of(earliest, later));

		assertTrue(generators.evaluate(new Object()));

		assertEquals(1, earliest.evaluationCount);
		assertEquals(0, later.evaluationCount);
	}

	@Test
	void evaluatesEveryGeneratorTiedAtEarliestEndWithoutShortCircuiting() {
		StubIntervalGenerator first = new StubIntervalGenerator(10L, false);
		StubIntervalGenerator tied = new StubIntervalGenerator(10L, true);
		StubIntervalGenerator later = new StubIntervalGenerator(20L, true);
		IntervalGeneratorSet generators = IntervalGeneratorSet.getInstance(List.of(first, tied, later));

		assertFalse(generators.evaluate(new Object()));

		assertEquals(1, first.evaluationCount);
		assertEquals(1, tied.evaluationCount);
		assertEquals(0, later.evaluationCount);
	}

	@Test
	void acceptsGeneratorEndingAtMaximumLongValue() {
		StubIntervalGenerator unbounded = new StubIntervalGenerator(Long.MAX_VALUE, true);
		IntervalGeneratorSet generators = IntervalGeneratorSet.getInstance(List.of(unbounded));

		assertTrue(generators.hasNext());
		assertTrue(generators.evaluate(new Object()));
		assertEquals(1, unbounded.evaluationCount);
	}

	private static final class StubIntervalGenerator implements IntervalGenerator {
		private final long end;
		private final boolean evaluationResult;
		private int evaluationCount;

		private StubIntervalGenerator(long end, boolean evaluationResult) {
			this.end = end;
			this.evaluationResult = evaluationResult;
		}

		@Override
		public Object current() {
			return this;
		}

		@Override
		public long currentEnd() {
			return end;
		}

		@Override
		public long currentStart() {
			return 0;
		}

		@Override
		public boolean isCurrentActive() {
			return true;
		}

		@Override
		public boolean hasNext() {
			return true;
		}

		@Override
		public boolean canBeShared() {
			return true;
		}

		@Override
		public boolean evaluate(Object value) {
			evaluationCount++;
			return evaluationResult;
		}
	}
}
