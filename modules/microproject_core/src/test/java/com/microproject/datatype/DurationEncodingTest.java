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
package com.microproject.datatype;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.ParsePosition;

import org.junit.jupiter.api.Test;

import com.microproject.field.FieldConverter;

class DurationEncodingTest {
    @Test
    void temporalUnitsRoundTripWithinOneMillisecondPrecision() {
		int[] units = {TimeUnit.MINUTES, TimeUnit.HOURS, TimeUnit.DAYS,
			TimeUnit.ELAPSED_MINUTES,
			TimeUnit.ELAPSED_HOURS, TimeUnit.ELAPSED_DAYS};
        for (int unit : units) {
            long encoded = Duration.getInstance(2.75D, unit);
            assertEquals(unit, Duration.getType(encoded));
            assertEquals(2.75D, Duration.getValue(encoded), 0.000001D);
        }
    }

    @Test
	void workMarkerDoesNotRewriteEncodedUnit() {
        Duration duration = new Duration(Duration.getInstance(3D, TimeUnit.HOURS));
        long encoded = duration.getEncodedMillis();
        duration.setWork(true);
        assertTrue(duration.isWork());
        assertEquals(encoded, duration.getEncodedMillis());
	}

	@Test
	void unitlessDurationUsesTheConfiguredDurationUnit() {
		Duration duration = (Duration) DurationFormat.getInstance().parseObject("3", new ParsePosition(0));
		assertEquals(TimeUnit.DAYS, Duration.getType(duration.getEncodedMillis()));
		assertEquals(3D, Duration.getValue(duration.getEncodedMillis()));
	}

	@Test
	void fieldConversionKeepsTheValueOfAUnitlessDuration() throws Exception {
		Duration duration = (Duration) FieldConverter.convert("3", Duration.class, null);
		assertEquals(3D, Duration.getValue(duration.getEncodedMillis()));
	}
}
