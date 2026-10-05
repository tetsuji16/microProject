/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.ui.ribbon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.prefs.Preferences;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RibbonQuickAccessPreferencesTest {
	private static final String KEY = "ribbonQuickAccessCommands";
	private final Preferences preferences = Preferences.userNodeForPackage(RibbonDisplayPreferences.class);
	private String previousValue;

	@BeforeEach
	void isolateQuickAccessPreferences() {
		previousValue = preferences.get(KEY, null);
		preferences.remove(KEY);
	}

	@AfterEach
	void restoreQuickAccessPreferences() {
		if (previousValue == null) preferences.remove(KEY);
		else preferences.put(KEY, previousValue);
	}

	@Test
	void defaultsToSaveUndoRedoAndPersistsCustomSelectionInOrder() {
		assertEquals(List.of("RibbonTopBarSaveProject", "RibbonTopBarUndo", "RibbonTopBarRedo"),
			RibbonDisplayPreferences.loadQuickAccessCommands());

		RibbonDisplayPreferences.saveQuickAccessCommands(List.of("RibbonFind", "RibbonSaveProject", "RibbonFind"));

		assertEquals(List.of("RibbonFind", "RibbonSaveProject"), RibbonDisplayPreferences.loadQuickAccessCommands());
	}

	@Test
	void ignoresUnknownCommandsAndSupportsAnExplicitEmptyToolbar() {
		preferences.put(KEY, "UnknownCommand,RibbonUndo");
		assertTrue(RibbonDisplayPreferences.loadQuickAccessCommands().isEmpty());

		RibbonDisplayPreferences.saveQuickAccessCommands(List.of());
		assertTrue(RibbonDisplayPreferences.loadQuickAccessCommands().isEmpty());
	}

	@Test
	void resetRestoresTheOriginalSaveUndoRedoConfiguration() {
		RibbonDisplayPreferences.saveQuickAccessCommands(List.of("RibbonFind"));

		RibbonDisplayPreferences.resetQuickAccessCommands();

		assertEquals(List.of("RibbonTopBarSaveProject", "RibbonTopBarUndo", "RibbonTopBarRedo"),
			RibbonDisplayPreferences.loadQuickAccessCommands());
	}

	@Test
	void newlySelectedCommandsAreAppendedWithoutReorderingExistingCommands() {
		assertEquals(List.of("RibbonTopBarRedo", "RibbonTopBarSaveProject", "RibbonFind"),
			RibbonDisplayPreferences.orderQuickAccessSelection(
				List.of("RibbonTopBarRedo", "RibbonTopBarSaveProject", "RibbonTopBarUndo"),
				List.of("RibbonFind", "RibbonTopBarRedo", "RibbonTopBarSaveProject", "RibbonTopBarUndo"),
				java.util.Set.of("RibbonTopBarRedo", "RibbonTopBarSaveProject", "RibbonFind")));
	}
}
