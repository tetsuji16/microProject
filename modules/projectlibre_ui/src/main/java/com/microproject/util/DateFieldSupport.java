package com.microproject.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

import javax.swing.JTable;

import com.microproject.field.Field;
import com.microproject.field.FieldConverter;
import com.microproject.options.CalendarOption;
import com.microproject.options.EditOption;

/**
 * Shared date-input helpers used by spreadsheet and dialog editors.
 */
public final class DateFieldSupport {
	private DateFieldSupport() {
	}

	public static DateFormat dateFormatFor(Field field) {
		if (field != null && field.isDateOnly()) {
			return EditOption.getInstance().getShortDateFormat();
		}
		return EditOption.getInstance().getDateFormat();
	}

	public static Date defaultDateFor(Field field) {
		long date = DateTime.midnightToday();
		if (field != null) {
			if (field.isStartValue()) {
				date = CalendarOption.getInstance().makeValidStart(date, true);
			} else if (field.isEndValue()) {
				date = CalendarOption.getInstance().makeValidEnd(date, true);
			}
		}
		return new Date(date);
	}

	public static Date parseYearless(String text, DateFormat fallbackFormat, Date referenceDate) throws ParseException {
		return YearlessDateInputParser.parse(text, fallbackFormat, referenceDate);
	}

	public static Date referenceDateFromPreviousRows(JTable table, int editingRow, int editingColumn) {
		if (table == null || editingRow <= 0 || editingColumn < 0) {
			return null;
		}
		for (int row = editingRow - 1; row >= 0; row--) {
			Object candidate = table.getValueAt(row, editingColumn);
			if (candidate instanceof Date date && !DateTime.getZeroDate().equals(date)) {
				return date;
			}
		}
		return null;
	}

	public static String annotationTextFor(Object value, Field field) {
		if (value == null) {
			return null;
		}
		if (value instanceof Date date) {
			return annotationTextFor(date, field);
		}
		return FieldConverter.toString(value, value.getClass(), null);
	}

	public static String annotationTextFor(Date value, Field field) {
		if (value == null) {
			return null;
		}
		DateFormat format = dateFormatFor(field);
		String text = format.format(value);
		if (field == null || field.isDateOnly()) {
			return shortenShortDateText(text);
		}
		return text;
	}

	private static String shortenShortDateText(String text) {
		if (text == null) {
			return null;
		}
		int slash = text.lastIndexOf('/');
		if (slash > 0) {
			return text.substring(0, slash);
		}
		return text;
	}
}
