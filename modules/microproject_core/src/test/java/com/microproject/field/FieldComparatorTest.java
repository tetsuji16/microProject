/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class FieldComparatorTest {
	@Test
	void ascendingComparatorIsFieldAndDescendingReversesArguments() {
		Field field = new Field() {
			@Override
			public int compare(Object left, Object right) {
				return Integer.compare((Integer) left, (Integer) right);
			}
		};

		assertSame(field, field.getComparator(true));
		assertEquals(-1, field.getComparator(false).compare(2, 1));
	}
}
