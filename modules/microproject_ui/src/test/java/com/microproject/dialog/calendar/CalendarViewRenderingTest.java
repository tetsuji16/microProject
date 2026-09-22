/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.dialog.calendar;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.util.Calendar;

import org.junit.jupiter.api.Test;

class CalendarViewRenderingTest {
	@Test
	void calendarUsesHiDpiTextRendering() {
		CalendarView calendar = new CalendarView(0L);

		assertTrue(calendar.getAntialiased(),
				"the legacy month view must opt into antialiased text for HiDPI displays");
	}

	@Test
	void calendarUsesOfficeStyleWeekdayColors() {
		assertTrue(Color.RED.equals(CalendarView.weekDayColor(Calendar.SUNDAY)));
		assertTrue(Color.BLUE.equals(CalendarView.weekDayColor(Calendar.SATURDAY)));
		assertTrue(Color.BLACK.equals(CalendarView.weekDayColor(Calendar.MONDAY)));
	}
}
