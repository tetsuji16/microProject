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
		} finally {
			preferences.setUserName(originalName);
			preferences.setShowRowLines(originalRows);
			preferences.setFontFamily(originalFamily);
			preferences.setFontSize(originalSize);
			preferences.setGridLineColor(originalGridColor);
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
