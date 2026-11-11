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
package com.microproject.server.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.microproject.datatype.Duration;
import com.microproject.datatype.TimeUnit;

class MpxConverterTimeUnitTest {
	@Test
	void mapsEveryEngineTimeUnitToItsMpxjCounterpart() {
		int[] engineUnits = {
			TimeUnit.MINUTES,
			TimeUnit.HOURS,
			TimeUnit.DAYS,
			TimeUnit.WEEKS,
			TimeUnit.MONTHS,
			TimeUnit.PERCENT,
			TimeUnit.YEARS,
			TimeUnit.ELAPSED_MINUTES,
			TimeUnit.ELAPSED_HOURS,
			TimeUnit.ELAPSED_DAYS,
			TimeUnit.ELAPSED_WEEKS,
			TimeUnit.ELAPSED_MONTHS,
			TimeUnit.ELAPSED_YEARS,
			TimeUnit.ELAPSED_PERCENT
		};
		org.mpxj.TimeUnit[] mpxjUnits = {
			org.mpxj.TimeUnit.MINUTES,
			org.mpxj.TimeUnit.HOURS,
			org.mpxj.TimeUnit.DAYS,
			org.mpxj.TimeUnit.WEEKS,
			org.mpxj.TimeUnit.MONTHS,
			org.mpxj.TimeUnit.PERCENT,
			org.mpxj.TimeUnit.YEARS,
			org.mpxj.TimeUnit.ELAPSED_MINUTES,
			org.mpxj.TimeUnit.ELAPSED_HOURS,
			org.mpxj.TimeUnit.ELAPSED_DAYS,
			org.mpxj.TimeUnit.ELAPSED_WEEKS,
			org.mpxj.TimeUnit.ELAPSED_MONTHS,
			org.mpxj.TimeUnit.ELAPSED_YEARS,
			org.mpxj.TimeUnit.ELAPSED_PERCENT
		};

		for (int i = 0; i < engineUnits.length; i++) {
			org.mpxj.Duration converted = MPXConverter.toMPXDuration(Duration.getInstance(1, engineUnits[i]));
			assertEquals(mpxjUnits[i], converted.getUnits(), "engine time unit " + engineUnits[i]);
		}
	}
}
