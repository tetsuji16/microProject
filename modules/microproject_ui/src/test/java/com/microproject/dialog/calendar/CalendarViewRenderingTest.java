/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.dialog.calendar;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.junit.jupiter.api.Test;

class CalendarViewRenderingTest {
	@Test
	void calendarUsesHiDpiTextRendering() {
		CalendarView calendar = new CalendarView(0L);

		assertTrue(calendar.getAntialiased(),
				"the legacy month view must opt into antialiased text for HiDPI displays");
	}

	@Test
	void calendarKeepsOneReadableMonthWhenItsContainerIsLarge() {
		GregorianCalendar expectedMonth = new GregorianCalendar(2026, Calendar.SEPTEMBER, 1);
		CalendarView calendar = new CalendarView(expectedMonth.getTimeInMillis());
		calendar.setSize(665, 420);

		GregorianCalendar lastVisible = new GregorianCalendar();
		lastVisible.setTimeInMillis(calendar.getLastDisplayedDate());
		assertTrue(lastVisible.get(Calendar.YEAR) == 2026
				&& lastVisible.get(Calendar.MONTH) == Calendar.SEPTEMBER,
			"a large dialog must not shrink calendar text by packing extra months");
	}

	@Test
	void calendarUsesOfficeStyleWeekdayColors() {
		assertTrue(com.microproject.util.FlatUiSupport.errorForeground().equals(CalendarView.weekDayColor(Calendar.SUNDAY)));
		assertTrue(com.microproject.util.FlatUiSupport.accentColor().equals(CalendarView.weekDayColor(Calendar.SATURDAY)));
		assertTrue(com.microproject.util.FlatUiSupport.labelForeground().equals(CalendarView.weekDayColor(Calendar.MONDAY)));
	}
}
