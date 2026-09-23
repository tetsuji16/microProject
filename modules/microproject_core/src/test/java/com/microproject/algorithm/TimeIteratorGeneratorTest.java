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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;

import org.junit.jupiter.api.Test;

import com.microproject.timescale.TimeIterator;
import com.microproject.timescale.TimeScale;

class TimeIteratorGeneratorTest {
	private static final long HOUR = 60L * 60L * 1000L;

	@Test
	void exposesAndAdvancesIntervalsUntilTheIteratorIsExhausted() {
		TimeIterator timeIterator = new TimeIterator(0, 2 * HOUR, hourlyScale(), -1);
		TimeIteratorGenerator generator = TimeIteratorGenerator.getInstance(timeIterator);

		Object firstInterval = generator.current();
		assertSame(firstInterval, generator.current());
		assertEquals(0, generator.getIndex());
		assertEquals(0, generator.currentStart());
		assertEquals(HOUR, generator.currentEnd());
		assertEquals(0, generator.getStart());
		assertEquals(2 * HOUR, generator.getEnd());
		assertTrue(generator.hasNext());

		assertFalse(generator.evaluate(null));
		assertEquals(1, generator.getIndex());
		assertEquals(HOUR, generator.currentStart());
		assertEquals(2 * HOUR, generator.currentEnd());
		assertFalse(generator.hasNext());
	}

	private static TimeScale hourlyScale() {
		TimeScale scale = new TimeScale();
		scale.setCalendarField1(Calendar.HOUR_OF_DAY);
		scale.setNumber1(1);
		scale.setCalendarField2(Calendar.DAY_OF_MONTH);
		scale.setNumber2(1);
		scale.setPattern1("HH");
		scale.setPattern2("yyyy-MM-dd");
		return scale;
	}
}
