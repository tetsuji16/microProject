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
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;

import com.microproject.pm.calendar.WorkRangeException;
import com.microproject.pm.calendar.WorkingHours;
import com.microproject.strings.Messages;
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

	/**
	 * Reads the from/to slot fields of one working day.  Blank pairs are skipped; a half-filled
	 * pair, an unparsable or non-increasing range, or overlapping slots are rejected, and at least
	 * one slot is required.
	 */
	static WorkingHours workingHours(JTextField[] from, JTextField[] to) {
		WorkingHours hours = new WorkingHours();
		int count = 0;
		LocalTime previousFinish = null;
		try {
			for (int slot = 0; slot < from.length; slot++) {
				String begin = from[slot].getText().trim();
				String finish = to[slot].getText().trim();
				if (begin.isEmpty() && finish.isEmpty()) continue;
				if (begin.isEmpty() || finish.isEmpty()) throw new IllegalArgumentException(
					Messages.getString("ChangeWorkingTimeDialogBox.WorkWeekTimePairRequired"));
				LocalTime startTime = LocalTime.parse(begin);
				LocalTime finishTime = LocalTime.parse(finish);
				if (!finishTime.isAfter(startTime) || (previousFinish != null && startTime.isBefore(previousFinish)))
					throw new IllegalArgumentException(Messages.getString("ChangeWorkingTimeDialogBox.WorkWeekInvalidTime"));
				hours.setInterval(count++, timeValue(begin), timeValue(finish));
				previousFinish = finishTime;
			}
		} catch (WorkRangeException | java.time.format.DateTimeParseException invalid) {
			throw new IllegalArgumentException(Messages.getString("ChangeWorkingTimeDialogBox.WorkWeekInvalidTime"), invalid);
		}
		if (count == 0) throw new IllegalArgumentException(
			Messages.getString("ChangeWorkingTimeDialogBox.WorkWeekTimeRequired"));
		return hours;
	}
}
