/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The Timeline Format settings and their workspace round trip. */
class TimelineViewSettingsTest {
	@Test
	void textLinesAreClampedAndDateRangeNeedsAnOrderedPair() {
		TimelineViewSettings settings = new TimelineViewSettings();
		settings.setTextLines(99);
		assertEquals(TimelineViewSettings.MAX_TEXT_LINES, settings.getTextLines());
		settings.setTextLines(-5);
		assertEquals(TimelineViewSettings.MIN_TEXT_LINES, settings.getTextLines());
		assertFalse(settings.hasDateRange());
		settings.setDateRange(1_000L, 5_000L);
		assertTrue(settings.hasDateRange());
	}

	@Test
	void everyOptionSurvivesASaveAndRestore() {
		TimelineViewSettings settings = new TimelineViewSettings();
		settings.setShowToday(!settings.isShowToday());
		settings.setShowTimescale(!settings.isShowTimescale());
		settings.setShowTaskDates(!settings.isShowTaskDates());
		settings.setShowProgress(!settings.isShowProgress());
		settings.setShowPanZoom(!settings.isShowPanZoom());
		settings.setShowOverlapped(!settings.isShowOverlapped());
		settings.setExpanded(!settings.isExpanded());
		settings.setBarLabels(!settings.isBarLabels());
		settings.setTextLines(3);
		settings.setDateRange(1_000L, 5_000L);
		settings.setCallout(7L, true);
		int bar = settings.addBar();
		settings.setBarOf(8L, bar);

		TimelineViewSettings restored = new TimelineViewSettings();
		restored.restoreSettings(settings.saveSettings());

		assertEquals(settings.isShowToday(), restored.isShowToday());
		assertEquals(settings.isShowTimescale(), restored.isShowTimescale());
		assertEquals(settings.isShowTaskDates(), restored.isShowTaskDates());
		assertEquals(settings.isShowProgress(), restored.isShowProgress());
		assertEquals(settings.isShowPanZoom(), restored.isShowPanZoom());
		assertEquals(settings.isShowOverlapped(), restored.isShowOverlapped());
		assertEquals(settings.isExpanded(), restored.isExpanded());
		assertEquals(settings.isBarLabels(), restored.isBarLabels());
		assertEquals(3, restored.getTextLines());
		assertTrue(restored.hasDateRange());
		assertEquals(1_000L, restored.getRangeStart());
		assertEquals(5_000L, restored.getRangeEnd());
		assertTrue(restored.isCallout(7L));
		assertEquals(settings.getBarCount(), restored.getBarCount());
		assertEquals(bar, restored.barOf(8L));
	}

	@Test
	void barCountIsCappedAtTheSameLimitForCreationAndRestore() {
		TimelineViewSettings settings = new TimelineViewSettings();
		for (int i = 0; i < TimelineViewSettings.MAX_BARS + 5; i++) settings.addBar();
		assertEquals(TimelineViewSettings.MAX_BARS, settings.getBarCount());
	}
}
