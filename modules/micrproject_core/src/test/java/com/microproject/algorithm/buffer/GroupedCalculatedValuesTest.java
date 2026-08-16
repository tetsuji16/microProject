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
package com.microproject.algorithm.buffer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GroupedCalculatedValuesTest {
	@Test
	void unionSortsDatesAndAddsValuesForMatchingDates() {
		GroupedCalculatedValues first = values(point(10L, 1.0), point(30L, 3.0));
		GroupedCalculatedValues second = values(point(20L, 2.0), point(30L, 4.0), point(40L, 5.0));

		GroupedCalculatedValues result = GroupedCalculatedValues.union(first, second);

		assertEquals(4, result.size());
		assertPoint(result, 0, 10L, 1.0);
		assertPoint(result, 1, 20L, 2.0);
		assertPoint(result, 2, 30L, 7.0);
		assertPoint(result, 3, 40L, 5.0);
	}

	@Test
	void unionDoesNotShareMutablePointsWithInputs() {
		GroupedCalculatedValues first = values(point(10L, 1.0));
		GroupedCalculatedValues second = values(point(20L, 2.0));

		GroupedCalculatedValues result = GroupedCalculatedValues.union(first, second);
		result.setValue(0, 99.0);
		first.setValue(0, 77.0);

		assertEquals(77.0, first.getUnscaledValue(0));
		assertEquals(99.0, result.getUnscaledValue(0));
		assertEquals(2.0, second.getUnscaledValue(0));
		assertEquals(2.0, result.getUnscaledValue(1));
	}

	@Test
	void unionHandlesEmptyInputs() {
		GroupedCalculatedValues populated = values(point(10L, 1.0));

		GroupedCalculatedValues result = GroupedCalculatedValues.union(new GroupedCalculatedValues(), populated);

		assertEquals(1, result.size());
		assertPoint(result, 0, 10L, 1.0);
		result.setValue(0, 5.0);
		assertEquals(1.0, populated.getUnscaledValue(0));
	}

	private static GroupedCalculatedValues values(Point... points) {
		GroupedCalculatedValues values = new GroupedCalculatedValues();
		for (int index = 0; index < points.length; index++) {
			Point point = points[index];
			values.set(index, point.getDate(), point.getDate(), point.getValue(), null);
		}
		return values;
	}

	private static Point point(long date, double value) {
		return new Point(date, value);
	}

	private static void assertPoint(GroupedCalculatedValues values, int index, long date, double value) {
		assertEquals(date, values.getDate(index));
		assertEquals(value, values.getUnscaledValue(index));
	}
}
