/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
	void legendAndTooltipsIdentifyGregorianAsTheWorkingDate() {
		LocalDate date = LocalDate.of(2026, 10, 18);
		String english = AlternativeCalendarDisplay.calendarLegend(Locale.US);
		String japanese = AlternativeCalendarDisplay.calendarLegend(Locale.JAPAN);
		assertTrue(english.contains("Gregorian calendar only"));
		assertTrue(english.contains("working-time edits use Gregorian dates"));
		assertTrue(japanese.contains("グレゴリオ暦のみ"));
		assertTrue(AlternativeCalendarDisplay.dayTooltip(date, Locale.US).contains("Gregorian date:"));
		assertTrue(AlternativeCalendarDisplay.dayTooltip(date, Locale.JAPAN).contains("グレゴリオ暦:"));
	}

	@Test
	void activeAlternativeCalendarIsNamedInLegendAndFullDateTooltip() {
		Locale japaneseCalendar = Locale.forLanguageTag("ja-JP-u-ca-japanese");
		Locale englishCalendar = Locale.forLanguageTag("en-US-u-ca-islamic-umalqura");
		LocalDate date = LocalDate.of(2026, 10, 18);
		String legend = AlternativeCalendarDisplay.calendarLegend(japaneseCalendar);
		String tooltip = AlternativeCalendarDisplay.dayTooltip(date, japaneseCalendar);
		assertTrue(legend.contains("小さな数字"));
		assertTrue(legend.contains(Chronology.ofLocale(japaneseCalendar)
			.getDisplayName(java.time.format.TextStyle.FULL, japaneseCalendar)));
		assertTrue(tooltip.contains("グレゴリオ暦:"));
		assertTrue(tooltip.contains("選択中の別暦:"));
		assertTrue(tooltip.contains("令和"));
		assertTrue(legend.contains(Chronology.ofLocale(japaneseCalendar)
			.getDisplayName(java.time.format.TextStyle.FULL, japaneseCalendar)));
		assertTrue(legend.contains("言語/地域設定"));
		String englishLegend = AlternativeCalendarDisplay.calendarLegend(englishCalendar);
		assertTrue(englishLegend.contains(Chronology.ofLocale(englishCalendar)
			.getDisplayName(java.time.format.TextStyle.FULL, englishCalendar)));
		assertTrue(englishLegend.contains("locale settings"));
		assertTrue(AlternativeCalendarDisplay.dayTooltip(date, englishCalendar).contains("Selected calendar:"));
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
	void windowsCalendarIdentifiersMapOnlyToEquivalentChronologies() {
		assertEquals("Japanese", AlternativeCalendarDisplay.chronologyForWindowsCalendarId("3").getId());
		assertEquals("Minguo", AlternativeCalendarDisplay.chronologyForWindowsCalendarId("4").getId());
		assertEquals("ThaiBuddhist", AlternativeCalendarDisplay.chronologyForWindowsCalendarId("7").getId());
		assertEquals("Hijrah-umalqura", AlternativeCalendarDisplay.chronologyForWindowsCalendarId("23").getId());
		assertNull(AlternativeCalendarDisplay.chronologyForWindowsCalendarId("6"));
		assertNull(AlternativeCalendarDisplay.chronologyForWindowsCalendarId("8"));
	}

	@Test
	void parsesCurrentWindowsCalendarValueWithoutAssumingTheRegistryDefault() {
		String output = "HKEY_CURRENT_USER\\Control Panel\\International\n"
			+ "    iCalendarType    REG_SZ    23\n";

		assertEquals("23", AlternativeCalendarDisplay.parseWindowsCalendarId(output));
		assertNull(AlternativeCalendarDisplay.parseWindowsCalendarId("iCalendarType REG_SZ invalid"));
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

	@Test
	void hijrahLabelsFollowGregorianDatesAcrossLeapDayMonthAndYearBoundaries() {
		Locale locale = Locale.forLanguageTag("en-US-u-ca-islamic-umalqura");
		Chronology chronology = Chronology.ofLocale(locale);
		for (LocalDate isoDate : new LocalDate[] {
			LocalDate.of(2024, 2, 28), LocalDate.of(2024, 2, 29), LocalDate.of(2024, 3, 1),
			LocalDate.of(2026, 12, 31), LocalDate.of(2027, 1, 1)
		}) {
			var alternateDate = chronology.date(isoDate);
			assertEquals(Integer.toString(alternateDate.get(ChronoField.DAY_OF_MONTH)),
				AlternativeCalendarDisplay.companionDay(isoDate, locale));
			String expectedMonth = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
				.withLocale(locale).withChronology(chronology).format(YearMonth.from(isoDate).atDay(1));
			assertEquals(expectedMonth, AlternativeCalendarDisplay.companionMonth(YearMonth.from(isoDate), locale));
		}
	}
}
