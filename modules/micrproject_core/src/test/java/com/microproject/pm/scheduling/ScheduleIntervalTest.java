package com.microproject.pm.scheduling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import org.junit.jupiter.api.Test;

class ScheduleIntervalTest {
	@Test
	void clonePreservesIntervalState() {
		ScheduleInterval interval = new ScheduleInterval(10L, 20L);
		interval.setMinimumStart(12L);

		ScheduleInterval clone = (ScheduleInterval) interval.clone();

		assertNotSame(interval, clone);
		assertEquals(10L, clone.getStart());
		assertEquals(20L, clone.getEnd());
		assertEquals(12L, clone.getMinimumStart());
	}
}
