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
package com.microproject.timescale;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Calendar;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import com.microproject.i18n.Messages;
import com.microproject.util.DateTime;

class ExtendedDateFormatTest {
	private static final Locale LOCALE = Locale.ENGLISH;

	private static final long FEBRUARY = DateTime.calendarInstance(2026, Calendar.FEBRUARY, 15).getTimeInMillis();
	private static final long MAY = DateTime.calendarInstance(2026, Calendar.MAY, 15).getTimeInMillis();
	private static final long AUGUST = DateTime.calendarInstance(2026, Calendar.AUGUST, 15).getTimeInMillis();
	private static final long NOVEMBER = DateTime.calendarInstance(2026, Calendar.NOVEMBER, 15).getTimeInMillis();

	private static String format(String pattern, long millis) {
		return new ExtendedDateFormat(pattern, LOCALE).format(millis);
	}

	@Test
	void singleQuarterLetterAppendsTheQuarterNumber() {
		String prefix = Messages.getString("Date.Quarter1");
		assertEquals(prefix + "1", format("Q", FEBRUARY));
		assertEquals(prefix + "2", format("Q", MAY));
		assertEquals(prefix + "3", format("Q", AUGUST));
		assertEquals(prefix + "4", format("Q", NOVEMBER));
	}

	@Test
	void doubleQuarterLetterUsesTheLongLabelAndASeparator() {
		String prefix = Messages.getString("Date.Quarter2");
		assertEquals(prefix + " 1", format("QQ", FEBRUARY));
		assertEquals(prefix + " 4", format("QQ", NOVEMBER));
	}

	@Test
	void singleHalfLetterAppendsTheHalfNumber() {
		String prefix = Messages.getString("Date.Half1");
		assertEquals(prefix + "1", format("L", FEBRUARY));
		assertEquals(prefix + "1", format("L", MAY));
		assertEquals(prefix + "2", format("L", AUGUST));
		assertEquals(prefix + "2", format("L", NOVEMBER));
	}

	@Test
	void doubleHalfLetterUsesTheLongLabelAndASeparator() {
		String prefix = Messages.getString("Date.Half2");
		assertEquals(prefix + " 1", format("LL", FEBRUARY));
		assertEquals(prefix + " 2", format("LL", NOVEMBER));
	}

	@Test
	void plainPatternsKeepTheirLocaleFormatting() {
		assertEquals("2026/02/15", format("yyyy/MM/dd", FEBRUARY));
		assertEquals("Feb 2026", format("MMM yyyy", FEBRUARY));
	}

	@Test
	void quarterAndHalfPrefixesCombineWithAPlainPattern() {
		String quarter = Messages.getString("Date.Quarter1");
		String half = Messages.getString("Date.Half1");
		assertEquals(quarter + "3 Aug 2026", format("Q MMM yyyy", AUGUST));
		assertEquals(half + "2 Nov 2026", format("L MMM yyyy", NOVEMBER));
	}
}
