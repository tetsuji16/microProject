/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The pure decisions behind the Calendar Format tab. */
class CalendarViewSettingsTest {
	@Test
	void bestFitHeightGrowsWithTheBusiestDayAndNeverDropsBelowTheMinimum() {
		assertEquals(CalendarViewSettings.MIN_ROW_HEIGHT, CalendarViewSettings.bestFitRowHeight(0, false));
		int three = CalendarViewSettings.bestFitRowHeight(3, false);
		int six = CalendarViewSettings.bestFitRowHeight(6, false);
		assertEquals(3 * CalendarViewSettings.BAR_PITCH, six - three);
		assertTrue(CalendarViewSettings.bestFitRowHeight(3, true) > three, "the companion day line needs room");
	}

	@Test
	void bestFitRowShowsEveryTaskOfTheBusiestDay() {
		for (int tasks = 1; tasks <= 12; tasks++)
			for (boolean companion : new boolean[] { false, true })
				assertTrue(CalendarViewSettings.visibleTaskCount(CalendarViewSettings.bestFitRowHeight(tasks, companion), companion) >= tasks,
					tasks + " tasks, companion " + companion);
		assertEquals(3, CalendarViewSettings.visibleTaskCount(CalendarViewSettings.DEFAULT_ROW_HEIGHT, false));
		assertEquals(2, CalendarViewSettings.visibleTaskCount(CalendarViewSettings.DEFAULT_ROW_HEIGHT, true));
	}

	@Test
	void barGeometryGrowsWithTheTextSize() {
		int smallHeight = CalendarViewSettings.barHeight(12);
		assertEquals(CalendarViewSettings.BAR_PITCH - 4, smallHeight, "ordinary text keeps the default 19px bar");
		int bigFont = 36 + 9;
		assertTrue(CalendarViewSettings.barHeight(bigFont) >= bigFont + 5, "the bar holds the big text");
		int pitch = CalendarViewSettings.barPitch(bigFont);
		assertTrue(pitch > CalendarViewSettings.BAR_PITCH);
		assertTrue(CalendarViewSettings.bestFitRowHeight(3, false, pitch) > CalendarViewSettings.bestFitRowHeight(3, false));
		for (int tasks = 1; tasks <= 8; tasks++)
			assertTrue(CalendarViewSettings.visibleTaskCount(CalendarViewSettings.bestFitRowHeight(tasks, true, pitch), true, pitch) >= tasks);
		assertTrue(CalendarViewSettings.visibleTaskCount(CalendarViewSettings.DEFAULT_ROW_HEIGHT, false, pitch)
			< CalendarViewSettings.visibleTaskCount(CalendarViewSettings.DEFAULT_ROW_HEIGHT, false));
	}

	@Test
	void settingsKeepOnlyChangedValues() {
		CalendarViewSettings settings = new CalendarViewSettings();
		assertEquals(null, settings.getBarColor(CalendarViewSettings.BarType.CRITICAL));
		settings.setBarColor(CalendarViewSettings.BarType.CRITICAL, 0xC0392B);
		assertEquals(0xC0392B, settings.getBarColor(CalendarViewSettings.BarType.CRITICAL));
		settings.setBarColor(CalendarViewSettings.BarType.CRITICAL, null);
		assertEquals(null, settings.getBarColor(CalendarViewSettings.BarType.CRITICAL));
		settings.setTextStyle(CalendarViewSettings.TextItem.BAR_TEXT, new CalendarViewSettings.TextStyle(14, true));
		assertEquals(14, settings.getTextStyle(CalendarViewSettings.TextItem.BAR_TEXT).size());
		assertEquals(CalendarViewSettings.TextStyle.DEFAULT, settings.getTextStyle(CalendarViewSettings.TextItem.DAY_NUMBERS));
		settings.setRowHeight(10);
		assertEquals(CalendarViewSettings.MIN_ROW_HEIGHT, settings.getRowHeight());
		assertTrue(settings.isAutomaticLayout());
	}
}
