/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.ribbon;

import java.util.prefs.Preferences;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** User-scoped storage for chrome choices; this is intentionally not project data. */
public final class RibbonDisplayPreferences {
	private static final String DISPLAY_MODE_KEY = "ribbonDisplayMode";
	private static final String QUICK_ACCESS_VISIBLE_KEY = "ribbonQuickAccessVisible";
	private static final String QUICK_ACCESS_COMMANDS_KEY = "ribbonQuickAccessCommands";
	private static final List<String> DEFAULT_QUICK_ACCESS_COMMANDS = List.of(
		"RibbonTopBarSaveProject", "RibbonTopBarUndo", "RibbonTopBarRedo");

	private RibbonDisplayPreferences() { }

	public static RibbonDisplayMode load() {
		String saved = Preferences.userNodeForPackage(RibbonDisplayPreferences.class)
			.get(DISPLAY_MODE_KEY, RibbonDisplayMode.ALWAYS_SHOW.name());
		try {
			return RibbonDisplayMode.valueOf(saved);
		} catch (IllegalArgumentException invalidValue) {
			return RibbonDisplayMode.ALWAYS_SHOW;
		}
	}

	public static void save(RibbonDisplayMode mode) {
		Preferences.userNodeForPackage(RibbonDisplayPreferences.class).put(DISPLAY_MODE_KEY, mode.name());
	}

	public static boolean loadQuickAccessVisible() {
		return Preferences.userNodeForPackage(RibbonDisplayPreferences.class).getBoolean(QUICK_ACCESS_VISIBLE_KEY, true);
	}

	public static void saveQuickAccessVisible(boolean visible) {
		Preferences.userNodeForPackage(RibbonDisplayPreferences.class).putBoolean(QUICK_ACCESS_VISIBLE_KEY, visible);
	}

	public static List<String> loadQuickAccessCommands() {
		String saved = Preferences.userNodeForPackage(RibbonDisplayPreferences.class)
			.get(QUICK_ACCESS_COMMANDS_KEY, null);
		if (saved == null) return DEFAULT_QUICK_ACCESS_COMMANDS;
		Set<String> allowed = new LinkedHashSet<>(RibbonCommandCatalog.quickAccessCandidates());
		allowed.addAll(DEFAULT_QUICK_ACCESS_COMMANDS);
		LinkedHashSet<String> commands = new LinkedHashSet<>();
		for (String token : saved.split(",", -1)) {
			String id = token.trim();
			if (allowed.contains(id)) commands.add(id);
		}
		return List.copyOf(commands);
	}

	public static List<String> quickAccessCandidateCommands() {
		return RibbonCommandCatalog.quickAccessCandidates();
	}

	public static void saveQuickAccessCommands(List<String> commandIds) {
		Set<String> allowed = new LinkedHashSet<>(RibbonCommandCatalog.quickAccessCandidates());
		allowed.addAll(DEFAULT_QUICK_ACCESS_COMMANDS);
		LinkedHashSet<String> commands = new LinkedHashSet<>();
		for (String id : commandIds) {
			if (id != null && allowed.contains(id)) commands.add(id);
		}
		Preferences.userNodeForPackage(RibbonDisplayPreferences.class)
			.put(QUICK_ACCESS_COMMANDS_KEY, String.join(",", commands));
	}
}
