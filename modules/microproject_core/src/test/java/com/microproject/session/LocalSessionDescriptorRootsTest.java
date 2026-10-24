/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.session;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalSessionDescriptorRootsTest {
	@TempDir
	Path temporaryDirectory;

	@Test
	void scansCanonicalHomeFolderBeforeLegacyProjectLibreFolder() throws Exception {
		Path canonical = Files.createDirectories(temporaryDirectory.resolve("microProject"));
		Path legacy = Files.createDirectories(temporaryDirectory.resolve("ProjectLibre"));

		assertEquals(List.of(canonical.toFile(), legacy.toFile()),
			LocalSession.getHomeProjectDescriptorRoots(temporaryDirectory.toFile()));
	}

	@Test
	void ignoresMissingLegacyOrCanonicalHomeFolders() throws Exception {
		Path legacy = Files.createDirectories(temporaryDirectory.resolve("ProjectLibre"));

		assertEquals(List.of(new File(legacy.toString())),
			LocalSession.getHomeProjectDescriptorRoots(temporaryDirectory.toFile()));
	}
}
