package com.projectlibre1.algorithm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

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
}
