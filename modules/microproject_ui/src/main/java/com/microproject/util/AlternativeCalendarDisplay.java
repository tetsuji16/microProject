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
import java.time.format.TextStyle;
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

	public static boolean hasAlternateCalendar(Locale locale) {
		return !IsoChronology.INSTANCE.equals(chronology(locale));
	}

	public static String calendarLegend(Locale locale) {
		if (!hasAlternateCalendar(locale)) {
			if (isJapanese(locale)) {
				return "グレゴリオ暦のみを表示しています。曜日・選択・稼働時間の編集はグレゴリオ暦の日付を基準にします。";
			}
			return "Gregorian calendar only. Weekdays, selection, and working-time edits use Gregorian dates.";
		}
		String calendarName = calendarName(locale);
		String source = localeChronology(locale) != null
			? (isJapanese(locale) ? "言語/地域設定" : "locale settings")
			: (isJapanese(locale) ? "Windows の地域設定" : "Windows regional settings");
		if (isJapanese(locale))
			return "大きな数字はグレゴリオ暦、小さな数字は" + calendarName + "（" + source
				+ "）の日付です。曜日・選択・稼働時間の編集はグレゴリオ暦の日付を基準にします。";
		return "Large dates are Gregorian; small dates use " + calendarName + " (" + source
			+ "). Weekdays, selection, and working-time edits use Gregorian dates.";
	}

	public static String calendarName(Locale locale) {
		Chronology selected = chronology(locale);
		return selected.getId() + " (" + selected.getDisplayName(TextStyle.FULL, locale) + ")";
	}

	public static String dayTooltip(LocalDate date, Locale locale) {
		String gregorian = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale).format(date);
		if (!hasAlternateCalendar(locale))
			return isJapanese(locale) ? "グレゴリオ暦: " + gregorian : "Gregorian date: " + gregorian;
		Chronology selected = chronology(locale);
		String alternate = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)
			.withChronology(selected).format(date);
		return isJapanese(locale) ? "グレゴリオ暦: " + gregorian + " / 選択中の別暦: " + alternate
			: "Gregorian date: " + gregorian + " / Selected calendar: " + alternate;
	}

	private static boolean isJapanese(Locale locale) {
		return locale != null && "ja".equals(locale.getLanguage());
	}

	private static Chronology chronology(Locale locale) {
		Chronology selected = localeChronology(locale);
		if (selected != null) return selected;
		return WINDOWS_CALENDAR == null ? IsoChronology.INSTANCE : WINDOWS_CALENDAR;
	}

	private static Chronology localeChronology(Locale locale) {
		try {
			Chronology selected = Chronology.ofLocale(locale);
			if (!IsoChronology.INSTANCE.equals(selected))
				return selected;
		} catch (DateTimeException exception) {
			// An unsupported locale calendar may still have a supported Windows selection.
		}
		return null;
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
		if (!Environment.isWindows())
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
