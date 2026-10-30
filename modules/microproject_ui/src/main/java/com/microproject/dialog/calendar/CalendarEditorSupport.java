/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.dialog.calendar;

import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;

import com.microproject.util.DateTime;

/** Date and time-of-day helpers shared by the calendar exception and work-week editors. */
final class CalendarEditorSupport {
	private CalendarEditorSupport() {
	}

	static JSpinner dateSpinner(Date value) {
		JSpinner spinner = new JSpinner(new SpinnerDateModel(value, null, null, Calendar.DAY_OF_MONTH));
		spinner.setEditor(new JSpinner.DateEditor(spinner, "yyyy-MM-dd"));
		return spinner;
	}

	static String formatTime(long value) {
		GregorianCalendar calendar = DateTime.calendarInstance();
		calendar.setTimeInMillis(value);
		return "%02d:%02d".formatted(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));
	}

	static long timeValue(String value) {
		LocalTime time = LocalTime.parse(value);
		GregorianCalendar calendar = DateTime.calendarInstance();
		calendar.clear();
		calendar.set(1970, Calendar.JANUARY, 1, time.getHour(), time.getMinute());
		return calendar.getTimeInMillis();
	}
}
