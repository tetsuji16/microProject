/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.util;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.chrono.Chronology;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoField;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Formats a secondary calendar date while keeping the ISO date authoritative. */
public final class AlternativeCalendarDisplay {
	private static final Pattern WINDOWS_CALENDAR_VALUE = Pattern.compile(
		"(?im)^\\s*iCalendarType\\s+REG_\\w+\\s+(\\d+)\\s*$");
	private static final Chronology WINDOWS_CALENDAR = readWindowsCalendar();

	private AlternativeCalendarDisplay() {
	}

	public static String monthLabel(YearMonth month, Locale locale) {
		String isoLabel = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale));
		String alternateLabel = companionMonth(month, locale);
		return alternateLabel.isEmpty() ? isoLabel : isoLabel + " (" + alternateLabel + ")";
	}

	/** Returns the selected chronology's month label, or an empty string for ISO-only locales. */
	public static String companionMonth(YearMonth month, Locale locale) {
		Chronology chronology = chronology(locale);
		if (IsoChronology.INSTANCE.equals(chronology)) return "";
		try {
			return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
				.withLocale(locale).withChronology(chronology).format(month.atDay(1));
		} catch (DateTimeException exception) {
			return "";
		}
	}

	/** Returns the selected chronology's day number, or an empty string for ISO-only locales. */
	public static String companionDay(LocalDate isoDate, Locale locale) {
		Chronology chronology = chronology(locale);
		if (IsoChronology.INSTANCE.equals(chronology))
			return "";
		try {
			return Integer.toString(chronology.date(isoDate).get(ChronoField.DAY_OF_MONTH));
		} catch (DateTimeException exception) {
			return "";
		}
	}

	private static Chronology chronology(Locale locale) {
		try {
			Chronology selected = Chronology.ofLocale(locale);
			if (!IsoChronology.INSTANCE.equals(selected))
				return selected;
		} catch (DateTimeException exception) {
			// An unsupported locale calendar may still have a supported Windows selection.
		}
		return WINDOWS_CALENDAR == null ? IsoChronology.INSTANCE : WINDOWS_CALENDAR;
	}

	static Chronology chronologyForWindowsCalendarId(String calendarId) {
		String chronologyId = switch (calendarId == null ? "" : calendarId) {
			case "3" -> "Japanese";
			case "4" -> "Minguo";
			case "7" -> "ThaiBuddhist";
			case "23" -> "Hijrah-umalqura";
			default -> null;
		};
		if (chronologyId == null)
			return null;
		try {
			return Chronology.of(chronologyId);
		} catch (DateTimeException exception) {
			return null;
		}
	}

	static String parseWindowsCalendarId(String commandOutput) {
		Matcher matcher = WINDOWS_CALENDAR_VALUE.matcher(commandOutput == null ? "" : commandOutput);
		return matcher.find() ? matcher.group(1) : null;
	}

	private static Chronology readWindowsCalendar() {
		if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("windows"))
			return null;
		try {
			Process process = new ProcessBuilder("reg.exe", "query", "HKCU\\Control Panel\\International",
				"/v", "iCalendarType").redirectErrorStream(true).start();
			if (!process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)) {
				process.destroyForcibly();
				return null;
			}
			if (process.exitValue() != 0)
				return null;
			String output = new String(process.getInputStream().readAllBytes(), Charset.defaultCharset());
			return chronologyForWindowsCalendarId(parseWindowsCalendarId(output));
		} catch (IOException exception) {
			return null;
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			return null;
		}
	}
}
