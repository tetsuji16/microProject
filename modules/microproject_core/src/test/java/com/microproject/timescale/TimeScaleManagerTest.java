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
package com.microproject.timescale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;

import org.junit.jupiter.api.Test;

class TimeScaleManagerTest {
	@Test
	void canZoomInStopsAtDailyScale() {
		TimeScaleManager manager = managerAtScale(2);

		assertFalse(manager.canZoomIn());
	}

	@Test
	void zoomInDoesNotMovePastDailyScale() {
		TimeScaleManager manager = managerAtScale(2);

		assertFalse(manager.zoomIn());
		assertEquals(2, manager.getCurrentScaleIndex());
	}

	@Test
	void zoomInStillWorksFromCoarserScale() {
		TimeScaleManager manager = managerAtScale(3);

		assertTrue(manager.canZoomIn());
		assertTrue(manager.zoomIn());
		assertEquals(2, manager.getCurrentScaleIndex());
	}

	private static TimeScaleManager managerAtScale(int currentScaleIndex) {
		TimeScaleManager manager = new TimeScaleManager();
		manager.addTimeScale(scale(Calendar.HOUR_OF_DAY, 2));
		manager.addTimeScale(scale(Calendar.HOUR_OF_DAY, 6));
		manager.addTimeScale(scale(Calendar.DAY_OF_WEEK, 1));
		manager.addTimeScale(scale(Calendar.DAY_OF_MONTH, 3));
		manager.setDefaultIndex(2);
		manager.setCurrentScaleIndex(currentScaleIndex);
		return manager;
	}

	private static TimeScale scale(int calendarField1, int number1) {
		TimeScale scale = new TimeScale();
		scale.setCalendarField1(calendarField1);
		scale.setNumber1(number1);
		scale.setNormalMinWidth(1);
		return scale;
	}
}
