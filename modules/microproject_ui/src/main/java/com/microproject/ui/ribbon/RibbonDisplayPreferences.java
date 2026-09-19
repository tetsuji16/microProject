/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.ribbon;

import java.util.prefs.Preferences;

/** User-scoped storage for chrome choices; this is intentionally not project data. */
public final class RibbonDisplayPreferences {
	private static final String DISPLAY_MODE_KEY = "ribbonDisplayMode";

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
}
