/*
 * MIT License
 * Copyright (c) 2026 microProject
 */
package com.microproject.pm.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WorkRangeTest {
	@Test
	void constructorPreservesOvertimeFlag() throws WorkRangeException {
		assertTrue(new WorkRange(8, 10, true).isOvertime());
		assertFalse(new WorkRange(8, 10, false).isOvertime());
	}

	@Test
	void clonePreservesRangeAndOvertimeWithoutSharingMutableState() throws WorkRangeException {
		WorkRange original = new WorkRange(8, 10, true);
		WorkRange clone = original.clone();

		assertEquals(original.getStart(), clone.getStart());
		assertEquals(original.getEnd(), clone.getEnd());
		assertTrue(clone.isOvertime());
		clone.setOvertime(false);
		assertTrue(original.isOvertime());
	}
}
