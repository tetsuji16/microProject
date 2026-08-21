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
		boolean originalTaskRowDragAndDrop = preferences.isTaskRowDragAndDropEnabled();
		String originalFamily = preferences.getFontFamily();
		int originalSize = preferences.getFontSize();
		try {
			preferences.setUserName("  editor  ");
			preferences.setShowRowLines(false);
			preferences.setTaskRowDragAndDropEnabled(false);
			preferences.setFontFamily("  SansSerif ");
			preferences.setFontSize(100);
			assertEquals("editor", preferences.getUserName());
			assertFalse(preferences.isShowRowLines());
			assertFalse(preferences.isTaskRowDragAndDropEnabled());
			assertEquals("SansSerif", preferences.getFontFamily());
			assertEquals(32, preferences.getFontSize());
			preferences.setFontSize(1);
			assertEquals(8, preferences.getFontSize());
			preferences.setFontFamily(null);
			assertEquals("", preferences.getFontFamily());
		} finally {
			preferences.setUserName(originalName);
			preferences.setShowRowLines(originalRows);
			preferences.setTaskRowDragAndDropEnabled(originalTaskRowDragAndDrop);
			preferences.setFontFamily(originalFamily);
			preferences.setFontSize(originalSize);
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
