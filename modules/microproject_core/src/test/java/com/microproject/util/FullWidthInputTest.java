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
package com.microproject.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.text.ParsePosition;
import java.util.Date;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.microproject.datatype.DurationFormat;
import com.microproject.datatype.PercentFormat;

class FullWidthInputTest {
	@Test
	void convertsFullWidthAsciiRangeOnly() {
		assertEquals("1.5d 10%", FullWidthInput.normalize("１．５ｄ　１０％"));
		assertEquals("2026/11/12", FullWidthInput.normalize("２０２６／１１／１２"));
		assertEquals("-1,000", FullWidthInput.normalize("－１，０００"));
		assertEquals("3日 5時間 週 ｱｲ", FullWidthInput.normalize("3日 5時間 週 ｱｲ"));
		assertEquals("カタカナ", FullWidthInput.normalize("カタカナ"));
		assertEquals("￥", FullWidthInput.normalize("￥"));
	}

	@Test
	void isNullSafeAndReturnsSameInstanceWhenUnchanged() {
		assertNull(FullWidthInput.normalize(null));
		String plain = "3d";
		assertEquals(plain, FullWidthInput.normalize(plain));
	}

	@Test
	void durationParsesFullWidthLikeHalfWidth() {
		DurationFormat format = (DurationFormat) DurationFormat.getInstance();
		assertEquals(parse(format, "3d"), parse(format, "３ｄ"));
		assertEquals(parse(format, "5h"), parse(format, "５ｈ"));
		assertEquals(parse(format, "1.5d"), parse(format, "１．５ｄ"));
		assertEquals(parse(format, "2w"), parse(format, "２ｗ"));
	}

	@Test
	void percentParsesFullWidth() throws Exception {
		assertEquals(((Number) PercentFormat.getInstance().parseObject("10%")).doubleValue(),
			((Number) PercentFormat.getInstance().parseObject("１０％")).doubleValue());
	}

	@Test
	void specialDateParsesFullWidth() {
		java.text.DateFormat format = SpecialDateFormat.getSpecialInstance();
		Date half = format.parse("2026/11/12 9:00", new ParsePosition(0));
		Date full = format.parse("２０２６／１１／１２　９：００", new ParsePosition(0));
		assertEquals(half, full);
	}

	private static Object parse(DurationFormat format, String text) {
		Object result = format.parseObject(text, new ParsePosition(0));
		if (result == null)
			throw new AssertionError("not parsed: " + text);
		return result;
	}
}
