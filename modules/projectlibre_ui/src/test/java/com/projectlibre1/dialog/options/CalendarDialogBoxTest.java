package com.projectlibre1.dialog.options;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.projectlibre1.options.CalendarOption;

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
