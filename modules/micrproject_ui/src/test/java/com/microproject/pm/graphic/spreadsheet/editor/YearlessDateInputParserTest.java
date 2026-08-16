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
package com.microproject.pm.graphic.spreadsheet.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.junit.jupiter.api.Test;

import com.microproject.util.DateTime;
import com.microproject.util.YearlessDateInputParser;
class YearlessDateInputParserTest {
	@Test
	void yearlessDateAdvancesToNextLaterYear() throws Exception {
		Date reference = DateTime.calendarInstance(2020, Calendar.DECEMBER, 1).getTime();
		Date parsed = YearlessDateInputParser.parse("2/2", new SimpleDateFormat("yyyy/MM/dd"), reference);

		assertEquals(DateTime.calendarInstance(2021, Calendar.FEBRUARY, 2).getTime(), parsed);
	}

	@Test
	void yearlessDateKeepsSameYearWhenAlreadyLater() throws Exception {
		Date reference = DateTime.calendarInstance(2020, Calendar.DECEMBER, 1).getTime();
		Date parsed = YearlessDateInputParser.parse("12/2", new SimpleDateFormat("yyyy/MM/dd"), reference);

		assertEquals(DateTime.calendarInstance(2020, Calendar.DECEMBER, 2).getTime(), parsed);
	}
}
