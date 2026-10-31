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
package com.microproject.dialog.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import javax.swing.JTextField;

import org.junit.jupiter.api.Test;

import com.microproject.pm.calendar.WorkingHours;

/** The single slot validator behind both the exception editor and the work-week editor. */
class CalendarEditorSupportWorkingHoursTest {
	private static JTextField[] fields(String... values) {
		JTextField[] result = new JTextField[values.length];
		for (int i = 0; i < values.length; i++) result[i] = new JTextField(values[i]);
		return result;
	}

	@Test
	void blankSlotsAreSkippedAndValidSlotsAreKept() {
		WorkingHours hours = CalendarEditorSupport.workingHours(fields("08:00", "", "13:00"), fields("12:00", "", "17:00"));
		assertEquals(480, minutes(hours, 0, true));
		assertEquals(1020, minutes(hours, 1, false));
	}

	@Test
	void invalidSlotsAreRejected() {
		assertThrows(IllegalArgumentException.class, () -> CalendarEditorSupport.workingHours(fields("", ""), fields("", "")), "no slot");
		assertThrows(IllegalArgumentException.class, () -> CalendarEditorSupport.workingHours(fields("08:00"), fields("")), "half pair");
		assertThrows(IllegalArgumentException.class, () -> CalendarEditorSupport.workingHours(fields("09:00"), fields("09:00")), "empty range");
		assertThrows(IllegalArgumentException.class, () -> CalendarEditorSupport.workingHours(fields("08:00", "11:00"), fields("12:00", "13:00")), "overlap");
		assertThrows(IllegalArgumentException.class, () -> CalendarEditorSupport.workingHours(fields("8am"), fields("12:00")), "unparsable");
	}

	private static long minutes(WorkingHours hours, int slot, boolean start) {
		var range = hours.getIntervals().get(slot);
		long millis = start ? range.getStart() : range.getEnd();
		java.util.GregorianCalendar calendar = com.microproject.util.DateTime.calendarInstance();
		calendar.setTimeInMillis(millis);
		return calendar.get(java.util.Calendar.HOUR_OF_DAY) * 60L + calendar.get(java.util.Calendar.MINUTE);
	}
}
