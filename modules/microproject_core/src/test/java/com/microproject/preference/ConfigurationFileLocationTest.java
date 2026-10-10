/* MIT License — Copyright (c) 2026 microProject */
package com.microproject.preference;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigurationFileLocationTest {
	@TempDir Path home;

	@Test void canonicalSettingsOverrideLegacyWithoutModifyingThem() throws Exception {
		Path legacy = home.resolve(".projectlibre/projectlibre.conf");
		Files.createDirectories(legacy.getParent());
		Files.writeString(legacy, "locale=ja");
		assertEquals(legacy.toFile(), ConfigurationFile.findConfigurationFile(home.toFile(), "microproject.conf"));
		Path canonical = home.resolve(".microproject/microproject.conf");
		Files.createDirectories(canonical.getParent());
		Files.writeString(canonical, "locale=en");
		assertEquals(canonical.toFile(), ConfigurationFile.findConfigurationFile(home.toFile(), "microproject.conf"));
		assertEquals("locale=ja", Files.readString(legacy));
	}

	@Test void emptyCanonicalDirectoryDoesNotHideLegacyRuntimeConfiguration() throws Exception {
		Files.createDirectories(home.resolve(".microproject"));
		Path legacy = home.resolve("ProjectLibre/run.conf");
		Files.createDirectories(legacy.getParent());
		Files.writeString(legacy, "memory=512");
		assertEquals(legacy.toFile(), ConfigurationFile.findConfigurationFile(home.toFile(), "run.conf"));
		assertNull(ConfigurationFile.findConfigurationFile(home.toFile(), "missing.conf"));
	}
}
