/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.temporary;

import java.nio.file.Path;

/** Resolves the per-user directory that holds microProject temporary workspaces. */
public final class TemporaryRoot {
	/** System property that overrides the default temporary root. */
	public static final String ROOT_PROPERTY = "microproject.temporary-root";

	private TemporaryRoot() {
	}

	/** Returns the override property, then {@code %LOCALAPPDATA%}, then the user-home fallback. */
	public static Path defaultRoot() {
		return resolve(System.getProperty(ROOT_PROPERTY), System.getenv("LOCALAPPDATA"),
			System.getProperty("user.home", "."));
	}

	static Path resolve(String override, String localAppData, String userHome) {
		if (override != null && !override.isBlank()) return Path.of(override);
		if (localAppData != null && !localAppData.isBlank()) return Path.of(localAppData, "microProject", "temp");
		return Path.of(userHome, "AppData", "Local", "microProject", "temp");
	}
}
