/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.util;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.chrono.Chronology;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoField;
import java.util.Locale;

/** Formats a secondary calendar date while keeping the ISO date authoritative. */
public final class AlternativeCalendarDisplay {
	private AlternativeCalendarDisplay() {
	}

	public static String monthLabel(YearMonth month, Locale locale) {
		Chronology chronology = chronology(locale);
		String isoLabel = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale));
		if (IsoChronology.INSTANCE.equals(chronology))
			return isoLabel;

		try {
			String alternateLabel = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
				.withLocale(locale).withChronology(chronology).format(month.atDay(1));
			return isoLabel + " (" + alternateLabel + ")";
		} catch (DateTimeException exception) {
			return isoLabel;
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
			return Chronology.ofLocale(locale);
		} catch (DateTimeException exception) {
			// Unsupported OS calendar extensions leave the established ISO display intact.
			return IsoChronology.INSTANCE;
		}
	}
}
