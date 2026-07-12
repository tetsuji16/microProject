package com.projectlibre1.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Calendar;
import java.util.Date;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import org.junit.jupiter.api.Test;

import com.projectlibre1.field.Field;
import com.projectlibre1.options.CalendarOption;
import com.projectlibre1.options.EditOption;
import com.projectlibre1.util.DateTime;

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
