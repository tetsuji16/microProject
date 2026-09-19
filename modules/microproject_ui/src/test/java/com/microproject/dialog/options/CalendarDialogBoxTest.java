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
package com.microproject.dialog.options;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.microproject.options.CalendarOption;

class CalendarDialogBoxTest {
	@Test
	void applyFormToDefaultCopiesTheEditedCalendarSettings() {
		CalendarOption defaultOption = CalendarOption.getDefaultInstance();
		double originalHoursPerDay = defaultOption.getHoursPerDay();
		double originalHoursPerWeek = defaultOption.getHoursPerWeek();
		double originalDaysPerMonth = defaultOption.getDaysPerMonth();
		boolean originalShowTimeInDates = defaultOption.isShowTimeInDates();
		int originalDefaultStartHour = defaultOption.getDefaultStartHour();
		int originalDefaultEndHour = defaultOption.getDefaultEndHour();

		try {
			CalendarOption sourceOption = CalendarOption.getNewInstance();
			sourceOption.setHoursPerDay(6.5);
			sourceOption.setHoursPerWeek(32.0);
			sourceOption.setDaysPerMonth(18.0);
			sourceOption.setShowTimeInDates(true);
			sourceOption.setDefaultStartHour(9);
			sourceOption.setDefaultEndHour(16);

			CalendarDialogBox.Form form = new CalendarDialogBox.Form(sourceOption);
			CalendarDialogBox.applyFormToDefault(form);

			assertEquals(6.5, defaultOption.getHoursPerDay());
			assertEquals(32.0, defaultOption.getHoursPerWeek());
			assertEquals(18.0, defaultOption.getDaysPerMonth());
			assertTrue(defaultOption.isShowTimeInDates());
			assertEquals(9, defaultOption.getDefaultStartHour());
			assertEquals(16, defaultOption.getDefaultEndHour());
		} finally {
			defaultOption.setHoursPerDay(originalHoursPerDay);
			defaultOption.setHoursPerWeek(originalHoursPerWeek);
			defaultOption.setDaysPerMonth(originalDaysPerMonth);
			defaultOption.setShowTimeInDates(originalShowTimeInDates);
			defaultOption.setDefaultStartHour(originalDefaultStartHour);
			defaultOption.setDefaultEndHour(originalDefaultEndHour);
		}
	}
}
