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
package com.microproject.core.time;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DefaultTimeIntervalsTest {
	@Test
	void settingStartOnEmptyCollectionCreatesAnOpenEndedInterval() {
		DefaultTimeIntervals intervals = new DefaultTimeIntervals();

		intervals.setStart(12);

		assertEquals(1, intervals.size());
		assertEquals(12, intervals.getStart());
		assertEquals(-1, intervals.getEnd());
	}

	@Test
	void settingEndOnEmptyCollectionCreatesAnOpenStartedInterval() {
		DefaultTimeIntervals intervals = new DefaultTimeIntervals();

		intervals.setEnd(34);

		assertEquals(1, intervals.size());
		assertEquals(-1, intervals.getStart());
		assertEquals(34, intervals.getEnd());
	}

	@Test
	void settingBothBoundsOnEmptyCollectionKeepsTheNewIntervalInTheSet() {
		DefaultTimeIntervals intervals = new DefaultTimeIntervals();

		intervals.setStart(12);
		intervals.setEnd(34);

		assertEquals(1, intervals.size());
		assertEquals(12, intervals.getStart());
		assertEquals(34, intervals.getEnd());
		assertEquals(new DefaultTimeInterval(12, 34), intervals.getIntervals().iterator().next());
	}
}
