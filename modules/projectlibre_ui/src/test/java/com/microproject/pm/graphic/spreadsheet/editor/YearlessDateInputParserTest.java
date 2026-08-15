package com.microproject.pm.graphic.spreadsheet.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.junit.jupiter.api.Test;

import com.microproject.util.DateTime;
import com.microproject.util.YearlessDateInputParser;
class YearlessDateInputParserTest {
	@Test
	void yearlessDateAdvancesToNextLaterYear() throws Exception {
		Date reference = DateTime.calendarInstance(2020, Calendar.DECEMBER, 1).getTime();
		Date parsed = YearlessDateInputParser.parse("2/2", new SimpleDateFormat("yyyy/MM/dd"), reference);

		assertEquals(DateTime.calendarInstance(2021, Calendar.FEBRUARY, 2).getTime(), parsed);
	}

	@Test
	void yearlessDateKeepsSameYearWhenAlreadyLater() throws Exception {
		Date reference = DateTime.calendarInstance(2020, Calendar.DECEMBER, 1).getTime();
		Date parsed = YearlessDateInputParser.parse("12/2", new SimpleDateFormat("yyyy/MM/dd"), reference);

		assertEquals(DateTime.calendarInstance(2020, Calendar.DECEMBER, 2).getTime(), parsed);
	}
}
