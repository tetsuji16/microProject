/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.dialog.calendar;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

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

	@Test
	void calendarAddsAlternativeCalendarLabelsWithoutChangingPrimaryDates() {
		Locale previousFormatLocale = Locale.getDefault(Locale.Category.FORMAT);
		try {
			Locale.setDefault(Locale.Category.FORMAT,
				Locale.forLanguageTag("en-US-u-ca-islamic-umalqura"));
			class InspectableCalendarView extends CalendarView {
				String monthTitle(long millis, String primary) {
					return getCalendarMonthTitle(millis, primary);
				}
				String dayLabel(long millis) {
					return getSecondaryDayLabel(millis);
				}
			}
			InspectableCalendarView calendar = new InspectableCalendarView();
			GregorianCalendar gregorianDate = new GregorianCalendar(2024, Calendar.MARCH, 11);
			long dateMillis = gregorianDate.getTimeInMillis();

			assertTrue(calendar.monthTitle(dateMillis, "March 2024").startsWith("March 2024 ("),
				"the Gregorian month remains primary and the selected calendar is appended");
			assertTrue(!calendar.dayLabel(dateMillis).isEmpty(),
				"a differing alternative-calendar day number is available for secondary rendering");
			gregorianDate.setTimeInMillis(dateMillis);
			assertTrue(gregorianDate.get(Calendar.YEAR) == 2024
					&& gregorianDate.get(Calendar.MONTH) == Calendar.MARCH
					&& gregorianDate.get(Calendar.DAY_OF_MONTH) == 11,
				"the primary Gregorian date remains unchanged");
		} finally {
			Locale.setDefault(Locale.Category.FORMAT, previousFormatLocale);
		}
	}
}
