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
		net.sf.mpxj.TimeUnit[] mpxjUnits = {
			net.sf.mpxj.TimeUnit.MINUTES,
			net.sf.mpxj.TimeUnit.HOURS,
			net.sf.mpxj.TimeUnit.DAYS,
			net.sf.mpxj.TimeUnit.WEEKS,
			net.sf.mpxj.TimeUnit.MONTHS,
			net.sf.mpxj.TimeUnit.PERCENT,
			net.sf.mpxj.TimeUnit.YEARS,
			net.sf.mpxj.TimeUnit.ELAPSED_MINUTES,
			net.sf.mpxj.TimeUnit.ELAPSED_HOURS,
			net.sf.mpxj.TimeUnit.ELAPSED_DAYS,
			net.sf.mpxj.TimeUnit.ELAPSED_WEEKS,
			net.sf.mpxj.TimeUnit.ELAPSED_MONTHS,
			net.sf.mpxj.TimeUnit.ELAPSED_YEARS,
			net.sf.mpxj.TimeUnit.ELAPSED_PERCENT
		};

		for (int i = 0; i < engineUnits.length; i++) {
			net.sf.mpxj.Duration converted = MPXConverter.toMPXDuration(Duration.getInstance(1, engineUnits[i]));
			assertEquals(mpxjUnits[i], converted.getUnits(), "engine time unit " + engineUnits[i]);
		}
	}
}
