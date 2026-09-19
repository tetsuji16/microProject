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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Calendar;
import java.util.Date;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import org.junit.jupiter.api.Test;

import com.microproject.field.Field;
import com.microproject.options.CalendarOption;
import com.microproject.options.EditOption;
import com.microproject.util.DateTime;

class DateFieldSupportTest {
	@Test
	void defaultDateUsesStartAndEndFieldRules() {
		Field startField = new Field();
		startField.setStartValue(true);
		Field endField = new Field();
		endField.setEndValue(true);

		Date startDate = DateFieldSupport.defaultDateFor(startField);
		Date endDate = DateFieldSupport.defaultDateFor(endField);

		assertEquals(new Date(CalendarOption.getInstance().makeValidStart(DateTime.midnightToday(), true)), startDate);
		assertEquals(new Date(CalendarOption.getInstance().makeValidEnd(DateTime.midnightToday(), true)), endDate);
	}

	@Test
	void referenceDateScansPreviousRowsForLastRealDate() {
		Date firstDate = DateTime.calendarInstance(2026, Calendar.JUNE, 3).getTime();
		Date secondDate = DateTime.calendarInstance(2026, Calendar.JUNE, 5).getTime();
		DefaultTableModel model = new DefaultTableModel(
			new Object[][] {
				{ firstDate },
				{ null },
				{ secondDate },
			},
			new Object[] { "Date" }
		);
		JTable table = new JTable(model);

		Date reference = DateFieldSupport.referenceDateFromPreviousRows(table, 2, 0);

		assertEquals(firstDate, reference);
		assertNull(DateFieldSupport.referenceDateFromPreviousRows(table, 0, 0));
	}

	@Test
	void dateFormatSelectionReturnsNonNullFormat() {
		Field field = new Field();
		field.setDateOnly(true);

		assertNotNull(DateFieldSupport.dateFormatFor(field));
		assertNotNull(DateFieldSupport.dateFormatFor(null));
	}

	@Test
	void annotationTextUsesDateOnlyShorteningRules() {
		Calendar calendar = DateTime.calendarInstance(2026, Calendar.JULY, 4);
		calendar.set(Calendar.HOUR_OF_DAY, 15);
		calendar.set(Calendar.MINUTE, 30);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		Date date = calendar.getTime();
		Field dateOnlyField = new Field();
		dateOnlyField.setDateOnly(true);
		Field dateTimeField = new Field();

		String shortText = DateFieldSupport.annotationTextFor(date, dateOnlyField);
		String fullText = DateFieldSupport.annotationTextFor(date, dateTimeField);
		String expectedShortText = EditOption.getInstance().getShortDateFormat().format(date);
		int slash = expectedShortText.lastIndexOf('/');
		if (slash > 0) {
			expectedShortText = expectedShortText.substring(0, slash);
		}

		assertEquals(expectedShortText, shortText);
		assertEquals(EditOption.getInstance().getDateFormat().format(date), fullText);
	}

	@Test
	void annotationTextPassesThroughNonDateValues() {
		assertEquals("alpha", DateFieldSupport.annotationTextFor("alpha", null));
	}
}
