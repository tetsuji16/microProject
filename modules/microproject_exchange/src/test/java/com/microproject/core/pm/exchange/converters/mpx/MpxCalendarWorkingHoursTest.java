package com.microproject.core.pm.exchange.converters.mpx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.BitSet;

import org.junit.jupiter.api.Test;

import net.sf.mpxj.DateRange;
import net.sf.mpxj.Day;
import net.sf.mpxj.ProjectCalendar;
import net.sf.mpxj.ProjectCalendarHours;
import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.RecurringData;
import net.sf.mpxj.RecurrenceType;

import com.microproject.server.data.MPXConverter;
import com.microproject.pm.calendar.CalendarService;
import com.microproject.pm.calendar.WorkDay;
import com.microproject.pm.calendar.WorkingCalendar;
import com.microproject.pm.calendar.CalendarRecurrence;
import com.microproject.util.DateTime;

class MpxCalendarWorkingHoursTest {
	/**
	 * Verifies rule conversion against MPXJ's expanded dates. Microsoft's Project
	 * XML Exception schema defines MonthDay, Month, Occurrences and recurrence
	 * type (https://learn.microsoft.com/en-us/office-project/xml-data-interchange/exception-element?view=project-client-2016);
	 * Microsoft's calendar workflow documents the recurrence controls
	 * (https://support.microsoft.com/en-us/project/add-a-holiday-to-the-project-calendar).
	 * Invalid-month-day handling is a document-derived rule cross-checked with
	 * MPXJ, not a measured MSP-client result.
	 */
	@Test
	void recurrencePatternsRoundTripWithoutChangingMspOccurrenceDates() {
		long start = day(2024, Calendar.JANUARY, 1);
		BitSet weekdays = new BitSet(8);
		weekdays.set(1); // Monday
		weekdays.set(3); // Wednesday
		List<CalendarRecurrence> rules = List.of(
			CalendarRecurrence.daily(start, 2, false, CalendarRecurrence.EndMode.AFTER_OCCURRENCES, 0L, 5),
			CalendarRecurrence.weekly(start, 2, weekdays, CalendarRecurrence.EndMode.AFTER_OCCURRENCES, 0L, 6),
			CalendarRecurrence.monthly(start, 1, true, 1, -1, 5,
				CalendarRecurrence.EndMode.AFTER_OCCURRENCES, 0L, 5),
			CalendarRecurrence.yearly(start, 1, false, Calendar.FEBRUARY + 1, 29, 1, 1,
				CalendarRecurrence.EndMode.AFTER_OCCURRENCES, 0L, 4));
		MpxCalendarRecurrenceConverter converter = new MpxCalendarRecurrenceConverter();
		for (CalendarRecurrence rule : rules) {
			RecurringData mspRule = converter.to(rule);
			assertEquals(rule.occurrenceDates().size(), mspRule.getDates().length,
				"MPXJ/MSP and domain rule must agree on occurrence count for " + rule.getPattern());
			CalendarRecurrence imported = converter.from(mspRule);
			assertEquals(rule.occurrenceDates(), imported.occurrenceDates(),
				"MPXJ import must preserve occurrence dates for " + rule.getPattern());
		}
	}

	@Test
	void recurringCalendarExceptionRetainsRuleThroughImportAndExport() {
		ProjectFile file = new ProjectFile();
		ProjectCalendar mspCalendar = file.addCalendar();
		mspCalendar.setName("Recurring exception calendar");
		RecurringData recurrence = new RecurringData();
		recurrence.setRecurrenceType(RecurrenceType.YEARLY);
		recurrence.setStartDate(mspDate(day(2024, Calendar.JANUARY, 1)));
		recurrence.setOccurrences(3);
		recurrence.setMonthNumber(Calendar.JANUARY + 1);
		recurrence.setDayNumber(1);
		var exception = mspCalendar.addCalendarException(recurrence);
		exception.setName("Annual shutdown");

		WorkingCalendar baseline = WorkingCalendar.getStandardBasedInstance();
		WorkingCalendar imported = WorkingCalendar.getStandardBasedInstance();
		new MpxCalendarConverter().from(mspCalendar, imported, new MpxImportState());
		var rule = imported.getRecurringExceptions().getFirst();
		assertEquals(0, imported.getExceptionDays().length,
			"generated schedule occurrences must not be exported as flattened one-off exceptions");
		assertEquals("Annual shutdown", rule.getTemplate().getDescription());
		assertEquals(List.of(day(2024, Calendar.JANUARY, 1), day(2025, Calendar.JANUARY, 1),
			day(2026, Calendar.JANUARY, 1)), rule.getRecurrence().occurrenceDates());
		assertFalse(CalendarService.getInstance().getDay(imported, day(2025, Calendar.JANUARY, 1)).isWorking());
		assertEquals(CalendarService.getInstance().getDay(baseline, day(2027, Calendar.JANUARY, 4)).isWorking(),
			CalendarService.getInstance().getDay(imported, day(2027, Calendar.JANUARY, 4)).isWorking(),
			"the exception must stop affecting the calendar after its third occurrence");

		ProjectFile exportedFile = new ProjectFile();
		ProjectCalendar exported = exportedFile.addCalendar();
		MPXConverter.toMpxCalendar(imported, exported);
		var savedRule = exported.getCalendarExceptions().stream()
			.filter(value -> value.getRecurring() != null).findFirst().orElseThrow();
		assertEquals("Annual shutdown", savedRule.getName());
		assertEquals(3, savedRule.getRecurring().getOccurrences());
		assertEquals(RecurrenceType.YEARLY, savedRule.getRecurring().getRecurrenceType());
		assertEquals(3, savedRule.getRecurring().getDates().length);
	}

	@Test
	void importsEveryMspWorkingRangeWithoutCollapsingToTheDefaultDay() {
		ProjectCalendarHours mspHours = new ProjectCalendarHours();
		mspHours.add(new DateRange(localTime(7), localTime(11)));
		mspHours.add(new DateRange(localTime(12), localTime(16)));

		var hours = new MpxRangeConverter().from(mspHours);

		assertEquals(2, hours.getIntervals().stream().filter(java.util.Objects::nonNull).count());
		assertEquals(8L * 60L * 60L * 1000L, hours.getDuration());
	}

	@Test
	void importsNamedCalendarExceptionWithItsFullDateRange() {
		ProjectFile file = new ProjectFile();
		ProjectCalendar mspCalendar = file.addCalendar();
		mspCalendar.setName("Named exception calendar");
		long start = day(2024, Calendar.JUNE, 4);
		long end = day(2024, Calendar.JUNE, 6);
		Date sourceStart = mspDate(start);
		Date sourceEnd = mspDate(end);
		var mspException = mspCalendar.addCalendarException(sourceStart, sourceEnd);
		mspException.setName("Company shutdown");

		WorkingCalendar imported = WorkingCalendar.getStandardBasedInstance();
		new MpxCalendarConverter().from(mspCalendar, imported, new MpxImportState());

		WorkDay named = Arrays.stream(imported.getExceptionDays())
			.filter(exception -> "Company shutdown".equals(exception.getDescription()))
			.findFirst().orElseThrow();
		assertEquals(start, DateTime.dayFloor(named.getStart()),
			"raw MSP exception date=" + mspException.getFromDate().getTime() + ", imported=" + named.getStart());
		assertEquals(end, DateTime.dayFloor(named.getEnd()),
			"raw MSP exception end=" + mspException.getToDate().getTime() + ", imported=" + named.getEnd());
		assertFalse(CalendarService.getInstance().getDay(imported, day(2024, Calendar.JUNE, 5)).isWorking());
	}

	@Test
	void exportsNamedCalendarExceptionWithItsFullDateRange() {
		long start = day(2024, Calendar.JUNE, 4);
		long end = day(2024, Calendar.JUNE, 6);
		WorkingCalendar source = WorkingCalendar.getStandardBasedInstance();
		source.setName("Named exception export");
		source.addOrReplaceException(new WorkDay(start, end, "Company shutdown"));

		ProjectFile file = new ProjectFile();
		ProjectCalendar exported = file.addCalendar();
		MPXConverter.toMpxCalendar(source, exported);

		WorkingCalendar imported = WorkingCalendar.getStandardBasedInstance();
		new MpxCalendarConverter().from(exported, imported, new MpxImportState());
		WorkDay roundTripped = Arrays.stream(imported.getExceptionDays())
			.filter(exception -> "Company shutdown".equals(exception.getDescription()))
			.findFirst().orElseThrow();
		assertEquals(start, DateTime.dayFloor(roundTripped.getStart()));
		assertEquals(end, DateTime.dayFloor(roundTripped.getEnd()));
	}

	@Test
	void datedWorkWeekSurvivesMspImportExportRoundTrip() {
		long start = day(2024, Calendar.JUNE, 3);
		long end = day(2024, Calendar.JUNE, 9);
		ProjectFile file = new ProjectFile();
		ProjectCalendar msp = file.addCalendar();
		var mspWeek = msp.addWorkWeek();
		mspWeek.setName("Summer schedule");
		mspWeek.setDateRange(new DateRange(mspDate(start), mspDate(end)));
		mspWeek.setWorkingDay(Day.getInstance(2), false); // Monday
		ProjectCalendarHours tuesday = mspWeek.addCalendarHours(Day.getInstance(3));
		tuesday.add(new DateRange(localTime(9), localTime(13)));

		WorkingCalendar imported = WorkingCalendar.getStandardBasedInstance();
		new MpxCalendarConverter().from(msp, imported, new MpxImportState());
		assertFalse(CalendarService.getInstance().getDay(imported, day(2024, Calendar.JUNE, 3)).isWorking());
		assertEquals(4L * 60L * 60L * 1000L, CalendarService.getInstance()
			.getDay(imported, day(2024, Calendar.JUNE, 4)).getWorkingHours().getDuration());
		assertEquals("Summer schedule", imported.getEffectiveWorkWeekPeriods().getFirst().getName());

		ProjectFile exportedFile = new ProjectFile();
		ProjectCalendar exported = exportedFile.addCalendar();
		MPXConverter.toMpxCalendar(imported, exported);
		assertEquals(net.sf.mpxj.DayType.NON_WORKING,
			exported.getWorkWeeks().getFirst().getCalendarDayType(Day.getInstance(2)));
		WorkingCalendar roundTripped = WorkingCalendar.getStandardBasedInstance();
		new MpxCalendarConverter().from(exported, roundTripped, new MpxImportState());
		assertFalse(roundTripped.getEffectiveWorkWeekPeriods().getFirst().getWeekDay(1).isWorking());
		assertEquals(start, roundTripped.getEffectiveWorkWeekPeriods().getFirst().getStart());
		assertEquals(end, roundTripped.getEffectiveWorkWeekPeriods().getFirst().getEnd());
		assertFalse(CalendarService.getInstance().getDay(roundTripped, day(2024, Calendar.JUNE, 3)).isWorking());
		assertEquals(4L * 60L * 60L * 1000L, CalendarService.getInstance()
			.getDay(roundTripped, day(2024, Calendar.JUNE, 4)).getWorkingHours().getDuration());
		assertEquals(start, roundTripped.getEffectiveWorkWeekPeriods().getFirst().getStart());
		assertEquals(end, roundTripped.getEffectiveWorkWeekPeriods().getFirst().getEnd());
	}

	private static Date localTime(int hour) {
		java.util.GregorianCalendar calendar = new java.util.GregorianCalendar();
		calendar.clear();
		calendar.set(1970, java.util.Calendar.JANUARY, 1, hour, 0);
		return calendar.getTime();
	}

	private static long day(int year, int month, int dayOfMonth) {
		return DateTime.calendarInstance(year, month, dayOfMonth).getTimeInMillis();
	}

	private static Date mspDate(long utcTime) {
		return new Date(utcTime + Calendar.getInstance().getTimeZone().getOffset(utcTime));
	}

}
