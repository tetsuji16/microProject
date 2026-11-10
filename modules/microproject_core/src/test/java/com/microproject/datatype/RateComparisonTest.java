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
package com.microproject.datatype;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RateComparisonTest {
	@Test
	void comparisonPreservesFractionalDifferences() {
		assertTrue(new Rate(1.25).compareTo(new Rate(1.75)) < 0);
		assertTrue(new Rate(1.75).compareTo(new Rate(1.25)) > 0);
	}

	@Test
	void comparisonDoesNotOverflowForLargeRates() {
		assertTrue(new Rate(-1.0e20).compareTo(new Rate(1.0e20)) < 0);
		assertTrue(new Rate(1.0e20).compareTo(new Rate(-1.0e20)) > 0);
	}

	@Test
	void ratesWithDifferentTimeUnitsAreNotEqual() {
		Rate perHour = new Rate(1.0, TimeUnit.HOURS);
		Rate perDay = new Rate(1.0, TimeUnit.DAYS);
		assertNotEquals(perHour, perDay);
		assertNotEquals(perDay, perHour);
		assertTrue(perHour.compareTo(perDay) != 0);
		assertEquals(-Integer.signum(perHour.compareTo(perDay)), Integer.signum(perDay.compareTo(perHour)));
	}

	@Test
	void equalRatesHaveEqualHashCodesAndEqualsIsSymmetric() {
		Rate a = new Rate(2.5, TimeUnit.HOURS);
		Rate b = new Rate(2.5, TimeUnit.HOURS);
		assertEquals(a, b);
		assertEquals(b, a);
		assertEquals(a.hashCode(), b.hashCode());
		assertEquals(0, a.compareTo(b));
		assertFalse(a.equals(null));
		assertFalse(a.equals("2.5"));
		assertNotEquals(a, new Rate(3.0, TimeUnit.HOURS));
	}
}
