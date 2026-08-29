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

import java.io.IOException;
import java.io.Reader;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/** Imports public-holiday and leave dates as Microsoft Project calendar exceptions. */
public final class CalendarExceptionImporter {
	private static final DateTimeFormatter[] CSV_DATE_FORMATS = {
		DateTimeFormatter.ISO_LOCAL_DATE,
		DateTimeFormatter.ofPattern("uuuu/M/d", Locale.ROOT),
		DateTimeFormatter.ofPattern("M/d/uuuu", Locale.ROOT)
	};

	private CalendarExceptionImporter() {
	}

	/**
	 * Reads either an Outlook-compatible iCalendar stream or a CSV stream whose first
	 * column contains one ISO, yyyy/M/d, or M/d/yyyy date per row.
	 */
	public static Set<LocalDate> readNonWorkingDates(Reader reader) throws IOException {
		String content = readAll(reader);
		Set<LocalDate> dates = content.contains("BEGIN:VCALENDAR") || content.contains("BEGIN:VEVENT")
			? readIcalendar(content)
			: readCsv(content);
		if (dates.isEmpty())
			throw new IOException("The calendar import contains no usable dates");
		return dates;
	}

	/** Applies every imported date as an all-day non-working exception. */
	public static int applyNonWorkingDates(WorkingCalendar calendar, Collection<LocalDate> dates, ZoneId zone) {
		if (calendar == null)
			throw new IllegalArgumentException("calendar must not be null");
		if (zone == null)
			throw new IllegalArgumentException("zone must not be null");
		int applied = 0;
		for (LocalDate date : dates) {
			if (date == null)
				continue;
			CalendarService.getInstance().setDayNonWorking(calendar,
				date.atStartOfDay(zone).toInstant().toEpochMilli());
			applied++;
		}
		return applied;
	}

	private static Set<LocalDate> readCsv(String content) throws IOException {
		Set<LocalDate> dates = new LinkedHashSet<>();
		int lineNumber = 0;
		for (String line : content.split("\\R")) {
			lineNumber++;
			String value = firstCsvValue(line);
			if (value.isEmpty() || value.startsWith("#") || isHeader(value))
				continue;
			try {
				dates.add(parseCsvDate(value));
			} catch (DateTimeParseException exception) {
				throw new IOException("Invalid calendar date on CSV line " + lineNumber + ": " + value, exception);
			}
		}
		return dates;
	}

	private static Set<LocalDate> readIcalendar(String content) throws IOException {
		Set<LocalDate> dates = new LinkedHashSet<>();
		LocalDate start = null;
		LocalDate end = null;
		boolean inEvent = false;
		for (String line : unfold(content).split("\\R")) {
			if ("BEGIN:VEVENT".equals(line)) {
				inEvent = true;
				start = null;
				end = null;
				continue;
			}
			if ("END:VEVENT".equals(line)) {
				if (inEvent && start != null)
					addEventDates(dates, start, end);
				inEvent = false;
				continue;
			}
			if (!inEvent)
				continue;
			int separator = line.indexOf(':');
			if (separator < 0)
				continue;
			String property = line.substring(0, separator);
			String value = line.substring(separator + 1);
			if (property.startsWith("DTSTART"))
				start = parseIcalendarDate(value);
			else if (property.startsWith("DTEND"))
				end = parseIcalendarDate(value);
		}
		return dates;
	}

	private static void addEventDates(Set<LocalDate> dates, LocalDate start, LocalDate end) {
		LocalDate exclusiveEnd = end == null || !end.isAfter(start) ? start.plusDays(1) : end;
		for (LocalDate date = start; date.isBefore(exclusiveEnd); date = date.plusDays(1))
			dates.add(date);
	}

	private static LocalDate parseIcalendarDate(String value) throws IOException {
		String date = value.length() >= 8 ? value.substring(0, 8) : value;
		try {
			return LocalDate.parse(date, DateTimeFormatter.BASIC_ISO_DATE);
		} catch (DateTimeParseException exception) {
			throw new IOException("Unsupported iCalendar date: " + value, exception);
		}
	}

	private static LocalDate parseCsvDate(String value) {
		for (DateTimeFormatter format : CSV_DATE_FORMATS) {
			try {
				return LocalDate.parse(value, format);
			} catch (DateTimeParseException ignored) {
				// Try the next documented input format.
			}
		}
		throw new DateTimeParseException("Unsupported date", value, 0);
	}

	private static String firstCsvValue(String line) {
		String value = line.trim();
		if (value.startsWith("\"") && value.length() > 1) {
			int closingQuote = value.indexOf('\"', 1);
			return (closingQuote < 0 ? value.substring(1) : value.substring(1, closingQuote)).trim();
		}
		int separator = value.indexOf(',');
		return (separator < 0 ? value : value.substring(0, separator)).trim();
	}

	private static boolean isHeader(String value) {
		return "date".equalsIgnoreCase(value) || "start date".equalsIgnoreCase(value);
	}

	private static String unfold(String content) {
		return content.replaceAll("\\R[ \\t]", "");
	}

	private static String readAll(Reader reader) throws IOException {
		StringBuilder content = new StringBuilder();
		char[] buffer = new char[4096];
		for (int count; (count = reader.read(buffer)) >= 0;)
			content.append(buffer, 0, count);
		return content.toString();
	}
}
