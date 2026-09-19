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
package com.microproject.field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.jupiter.api.Test;	import com.microproject.pm.time.Interval;
	import com.microproject.pm.time.MutableInterval;


/**
 * Issue #166: createIntervalField used to discard the interval-configured
 * FieldContext and set an empty one, which would make getLabel() throw a
 * NullPointerException (specialFieldContext.getInterval() == null).
 */
class FieldIntervalLabelTest {

	@Test
	void intervalFieldUsesTheConfiguredInterval() {
		Interval interval = new MutableInterval(1612137600000L, 1612224000000L); // 2021-02-01 (default TZ)
		Field field = Field.createIntervalField(new Field(), interval);

		String label = field.getLabel();
		assertNotNull(label);
		// getLabel() renders the day-of-week of the interval start
		String expected = new SimpleDateFormat("E").format(new Date(interval.getStart()));
		assertEquals(expected, label);
	}
}
