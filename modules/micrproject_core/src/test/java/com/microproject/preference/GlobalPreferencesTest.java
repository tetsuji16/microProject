/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.preference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GlobalPreferencesTest {
	@Test
	void userSettingsAreNormalizedAndExposeStableDefaults() {
		GlobalPreferences preferences = new GlobalPreferences();
		String originalName = preferences.getUserName();
		boolean originalRows = preferences.isShowRowLines();
		String originalFamily = preferences.getFontFamily();
		int originalSize = preferences.getFontSize();
		Integer originalGridColor = preferences.getGridLineColor();
		String originalGanttBarText = preferences.getDefaultGanttBarText();
		String originalGanttBarTextPosition = preferences.getDefaultGanttBarTextPosition();
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
			preferences.setDefaultGanttBarText(GlobalPreferences.GANTT_BAR_TEXT_TASK_NAME);
			assertEquals(GlobalPreferences.GANTT_BAR_TEXT_TASK_NAME, preferences.getDefaultGanttBarText());
			preferences.setDefaultGanttBarText("unsupported");
			assertEquals(GlobalPreferences.GANTT_BAR_TEXT_RESOURCE_NAMES, preferences.getDefaultGanttBarText());
			preferences.setDefaultGanttBarTextPosition(GlobalPreferences.GANTT_BAR_TEXT_POSITION_LEFT);
			assertEquals(GlobalPreferences.GANTT_BAR_TEXT_POSITION_LEFT, preferences.getDefaultGanttBarTextPosition());
			preferences.setDefaultGanttBarTextPosition("unsupported");
			assertEquals(GlobalPreferences.GANTT_BAR_TEXT_POSITION_AUTO, preferences.getDefaultGanttBarTextPosition());
		} finally {
			preferences.setUserName(originalName);
			preferences.setShowRowLines(originalRows);
			preferences.setFontFamily(originalFamily);
			preferences.setFontSize(originalSize);
			preferences.setGridLineColor(originalGridColor);
			preferences.setDefaultGanttBarText(originalGanttBarText);
			preferences.setDefaultGanttBarTextPosition(originalGanttBarTextPosition);
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
