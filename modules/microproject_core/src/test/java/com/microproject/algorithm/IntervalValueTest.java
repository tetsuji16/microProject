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
package com.microproject.algorithm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IntervalValueTest {
	private static final long DAY = 24L * 60L * 60L * 1000L;

	@Test
	void keepsTheIntervalAndTheValueItWasCreatedWith() {
		IntervalValue value = IntervalValue.getInstance(1000L, 5000L, 2.5d);

		assertEquals(1000L, value.getStart());
		assertEquals(5000L, value.getEnd());
		assertEquals(2.5d, value.getValue());
	}

	@Test
	void descriptionMentionsTheStartTheEndAndTheValue() {
		IntervalValue value = IntervalValue.getInstance(0L, DAY, 7.0d);
		String description = value.toString();

		assertTrue(description.contains("value: 7"), "the value must appear: " + description);
		// The notation is not fixed; only that both endpoints are represented.
		assertNotEquals(description, IntervalValue.getInstance(60_000L, DAY, 7.0d).toString(),
				"the start must appear: " + description);
		assertNotEquals(description, IntervalValue.getInstance(0L, DAY + 60_000L, 7.0d).toString(),
				"the end must appear: " + description);
	}
}
