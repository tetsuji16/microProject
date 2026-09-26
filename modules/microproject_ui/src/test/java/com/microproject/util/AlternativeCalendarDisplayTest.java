/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.chrono.Chronology;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoField;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class AlternativeCalendarDisplayTest {
	@Test
	void isoLocaleKeepsTheExistingSingleCalendarLabel() {
		Locale locale = Locale.US;

		assertEquals("January 2026", AlternativeCalendarDisplay.monthLabel(YearMonth.of(2026, 1), locale));
		assertEquals("", AlternativeCalendarDisplay.companionDay(LocalDate.of(2026, 1, 12), locale));
	}

	@Test
	void japaneseCalendarAddsEraAwareMonthAndDayLabels() {
		Locale locale = Locale.forLanguageTag("ja-JP-u-ca-japanese");
		YearMonth may2019 = YearMonth.of(2019, 5);

		assertTrue(AlternativeCalendarDisplay.monthLabel(may2019, locale).contains("令和"));
		assertEquals("1", AlternativeCalendarDisplay.companionDay(LocalDate.of(2019, 5, 1), locale));
	}

	@Test
	void unsupportedCalendarExtensionFallsBackToGregorianLabels() {
		Locale locale = Locale.forLanguageTag("en-US-u-ca-notreal");

		assertEquals("January 2026", AlternativeCalendarDisplay.monthLabel(YearMonth.of(2026, 1), locale));
		assertEquals("", AlternativeCalendarDisplay.companionDay(LocalDate.of(2026, 1, 12), locale));
	}

	@Test
	void hijrahCalendarAddsItsOwnMonthAndDayLabelsWithoutChangingIsoDates() {
		Locale locale = Locale.forLanguageTag("en-US-u-ca-islamic-umalqura");
		LocalDate isoDate = LocalDate.of(2026, 1, 12);
		Chronology chronology = Chronology.ofLocale(locale);
		String expectedDay = Integer.toString(chronology.date(isoDate).get(ChronoField.DAY_OF_MONTH));
		String expectedMonth = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
			.withLocale(locale).withChronology(chronology).format(YearMonth.of(2026, 1).atDay(1));

		assertEquals(expectedDay, AlternativeCalendarDisplay.companionDay(isoDate, locale));
		assertFalse(AlternativeCalendarDisplay.monthLabel(YearMonth.of(2026, 1), locale).equals("January 2026"));
		assertTrue(AlternativeCalendarDisplay.monthLabel(YearMonth.of(2026, 1), locale).contains(expectedMonth));
	}
}
