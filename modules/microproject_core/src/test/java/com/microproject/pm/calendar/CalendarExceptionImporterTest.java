/*******************************************************************************
 * MIT License
 *
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
package com.microproject.pm.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.StringReader;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;

import org.junit.jupiter.api.Test;

class CalendarExceptionImporterTest {
	@Test
	void readsCsvHolidayDatesFromTheFirstColumn() throws Exception {
		assertEquals(Set.of(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 11)),
			CalendarExceptionImporter.readNonWorkingDates(new StringReader("""
				Date,Name
				2026-01-01,New Year
				2/11/2026,National Foundation Day
				""")));
	}

	@Test
	void readsOutlookIcalendarAllDayEventsAndExpandsDateRanges() throws Exception {
		String calendar = """
			BEGIN:VCALENDAR
			BEGIN:VEVENT
			DTSTART;VALUE=DATE:20260810
			DTEND;VALUE=DATE:20260813
			SUMMARY:Summer leave
			END:VEVENT
			END:VCALENDAR
			""";
		assertEquals(Set.of(LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 11), LocalDate.of(2026, 8, 12)),
			CalendarExceptionImporter.readNonWorkingDates(new StringReader(calendar)));
	}

	@Test
	void importedDatesBecomeCalendarNonWorkingExceptions() {
		WorkingCalendar calendar = CalendarService.getInstance().getDefaultInstance().makeScratchCopy();
		LocalDate holiday = LocalDate.of(2026, 5, 6);
		ZoneId zone = ZoneId.systemDefault();

		assertEquals(1, CalendarExceptionImporter.applyNonWorkingDates(calendar, Set.of(holiday), zone));
		assertFalse(CalendarService.getInstance().getDay(calendar,
			holiday.atStartOfDay(zone).toInstant().toEpochMilli()).isWorking());
	}
}
