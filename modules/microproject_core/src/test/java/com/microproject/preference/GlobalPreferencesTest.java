/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.preference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Date;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import com.microproject.options.CalendarOption;
import com.microproject.options.EditOption;

class GlobalPreferencesTest {
	@Test
	void userSettingsAreNormalizedAndExposeStableDefaults() {
		GlobalPreferences preferences = new GlobalPreferences();
		String originalName = preferences.getUserName();
		boolean originalRows = preferences.isShowRowLines();
		String originalFamily = preferences.getFontFamily();
		int originalSize = preferences.getFontSize();
		Integer originalGridColor = preferences.getGridLineColor();
		Integer originalBarColor = preferences.getDefaultGanttBarColor();
		String originalMilestoneShape = preferences.getDefaultMilestoneShape();
		String originalGanttBarText = preferences.getDefaultGanttBarText();
		String originalGanttBarTextPosition = preferences.getDefaultGanttBarTextPosition();
		boolean originalDarkTheme = preferences.isDarkTheme();
		String originalDatePattern = preferences.getDatePattern();
		String originalDateTimePattern = preferences.getDateTimePattern();
		String originalCurrencyCode = preferences.getCurrencyCode();
		boolean originalShowTime = CalendarOption.getInstance().isShowTimeInDates();
		Locale originalLocale = Locale.getDefault();
		String originalActiveCurrencyCode = com.microproject.datatype.Money.getPreferredCurrencyCode();
		try {
			preferences.setUserName("  editor  ");
			preferences.setShowRowLines(false);
			preferences.setFontFamily("  SansSerif ");
			preferences.setFontSize(100);
			assertEquals("editor", preferences.getUserName());
			assertFalse(preferences.isShowRowLines());
			assertEquals("SansSerif", preferences.getFontFamily());
			assertEquals(32, preferences.getFontSize());
			preferences.setFontSize(1);
			assertEquals(8, preferences.getFontSize());
			preferences.setFontFamily(null);
			assertEquals("", preferences.getFontFamily());
			preferences.setGridLineColor(Integer.valueOf(0x12345678));
			assertEquals(Integer.valueOf(0x345678), preferences.getGridLineColor());
			preferences.setGridLineColor(null);
			assertEquals(null, preferences.getGridLineColor());
			preferences.setDefaultGanttBarColor(Integer.valueOf(0xAA123456));
			assertEquals(Integer.valueOf(0x123456), preferences.getDefaultGanttBarColor());
			preferences.setDefaultGanttBarColor(null);
			assertEquals(null, preferences.getDefaultGanttBarColor());
			preferences.setDefaultMilestoneShape("DIAMOND");
			assertEquals("DIAMOND", new GlobalPreferences().getDefaultMilestoneShape());
			preferences.setDefaultMilestoneShape("unsupported");
			assertEquals(null, preferences.getDefaultMilestoneShape());
			preferences.setDefaultGanttBarText(GlobalPreferences.GANTT_BAR_TEXT_TASK_NAME);
			assertEquals(GlobalPreferences.GANTT_BAR_TEXT_TASK_NAME, preferences.getDefaultGanttBarText());
			preferences.setDefaultGanttBarText("unsupported");
			assertEquals(GlobalPreferences.GANTT_BAR_TEXT_RESOURCE_NAMES, preferences.getDefaultGanttBarText());
			preferences.setDefaultGanttBarTextPosition(GlobalPreferences.GANTT_BAR_TEXT_POSITION_LEFT);
			assertEquals(GlobalPreferences.GANTT_BAR_TEXT_POSITION_LEFT, preferences.getDefaultGanttBarTextPosition());
			preferences.setDefaultGanttBarTextPosition("unsupported");
			assertEquals(GlobalPreferences.GANTT_BAR_TEXT_POSITION_AUTO, preferences.getDefaultGanttBarTextPosition());
			preferences.setDarkTheme(!originalDarkTheme);
			assertEquals(!originalDarkTheme, preferences.isDarkTheme());
			preferences.setDatePattern("yyyy-MM-dd");
			preferences.setDateTimePattern("yyyy-MM-dd HH:mm");
			assertEquals("yyyy-MM-dd", new GlobalPreferences().getDatePattern());
			assertEquals("yyyy-MM-dd HH:mm", new GlobalPreferences().getDateTimePattern());
			assertFalse(GlobalPreferences.isValidDatePattern("yyyy-MM-dd '"));
			assertThrows(IllegalArgumentException.class, () -> preferences.setDatePattern("yyyy-MM-dd '"));
			preferences.setCurrencyCode("eur");
			assertEquals("EUR", new GlobalPreferences().getCurrencyCode());
			assertFalse(GlobalPreferences.isValidCurrencyCode("NOT-A-CURRENCY"));
			assertThrows(IllegalArgumentException.class, () -> preferences.setCurrencyCode("NOT-A-CURRENCY"));
			Locale.setDefault(Locale.US);
			preferences.applyFormatPreferences();
			CalendarOption.getInstance().setShowTimeInDates(false);
			assertEquals("1970-01-01", EditOption.getInstance().getDateFormat().format(new Date(0)));
			CalendarOption.getInstance().setShowTimeInDates(true);
			assertEquals("1970-01-01 00:00", EditOption.getInstance().getDateFormat().format(new Date(0)));
		} finally {
			preferences.setUserName(originalName);
			preferences.setShowRowLines(originalRows);
			preferences.setFontFamily(originalFamily);
			preferences.setFontSize(originalSize);
			preferences.setGridLineColor(originalGridColor);
			preferences.setDefaultGanttBarColor(originalBarColor);
			preferences.setDefaultMilestoneShape(originalMilestoneShape);
			preferences.setDefaultGanttBarText(originalGanttBarText);
			preferences.setDefaultGanttBarTextPosition(originalGanttBarTextPosition);
			preferences.setDarkTheme(originalDarkTheme);
			preferences.setDatePattern(originalDatePattern);
			preferences.setDateTimePattern(originalDateTimePattern);
			preferences.setCurrencyCode(originalCurrencyCode);
			CalendarOption.getInstance().setShowTimeInDates(originalShowTime);
			Locale.setDefault(originalLocale);
			preferences.applyFormatPreferences();
			com.microproject.datatype.Money.setPreferredCurrencyCode(originalActiveCurrencyCode);
		}
		assertTrue(preferences.getFontSize() >= 0);
	}

	@Test
	void resourceFilterPreferenceUsesRequestedValue() {
		GlobalPreferences preferences = new GlobalPreferences();
		boolean original = preferences.isShowProjectResourcesOnly();
		try {
			preferences.setShowProjectResourcesOnly(!original);
			assertEquals(!original, preferences.isShowProjectResourcesOnly());
		} finally {
			preferences.setShowProjectResourcesOnly(original);
		}
	}
}
