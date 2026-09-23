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

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.microproject.util.ClassUtils;

class DurationComparisonTest {
	@Test
	void typedNaturalOrderMatchesTheRegisteredComparator() {
		Duration shorter = new Duration(Duration.getInstance(1.25, TimeUnit.HOURS));
		Duration longer = new Duration(Duration.getInstance(1.5, TimeUnit.HOURS));

		assertTrue(shorter.compareTo(longer) < 0);
		assertTrue(longer.compareTo(shorter) > 0);
		assertTrue(ClassUtils.getComparator(Duration.class).compare(shorter, longer) < 0);
		Work shorterWork = new Work(shorter.getEncodedMillis());
		Work longerWork = new Work(longer.getEncodedMillis());
		assertTrue(ClassUtils.getComparator(Work.class).compare(shorterWork, longerWork) < 0);
	}
}
