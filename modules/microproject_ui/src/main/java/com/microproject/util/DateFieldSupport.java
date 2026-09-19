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
