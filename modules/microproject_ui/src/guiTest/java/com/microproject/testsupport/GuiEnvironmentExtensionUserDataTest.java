/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.testsupport;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GuiEnvironmentExtensionUserDataTest {
	@TempDir
	Path temp;

	@Test
	void clearsDataInsideTheBuildDirectory() throws Exception {
		Path userData = temp.resolve("build/localAppData");
		Path snapshot = snapshotIn(userData);

		GuiEnvironmentExtension.clearIsolatedUserData(userData, temp.resolve("build"));

		assertFalse(Files.exists(snapshot));
		assertTrue(Files.isDirectory(userData));
	}

	@Test
	void neverClearsADirectoryOutsideTheBuild() throws Exception {
		Path userData = temp.resolve("profile/AppData/Local");
		Path snapshot = snapshotIn(userData);

		GuiEnvironmentExtension.clearIsolatedUserData(userData, temp.resolve("build"));

		assertTrue(Files.exists(snapshot));
	}

	@Test
	void neverClearsTheBuildDirectoryItself() throws Exception {
		Path build = temp.resolve("build");
		Path snapshot = snapshotIn(build);

		GuiEnvironmentExtension.clearIsolatedUserData(build, build);

		assertTrue(Files.exists(snapshot));
	}

	private static Path snapshotIn(Path userData) throws Exception {
		Path snapshot = userData.resolve("microProject/recovery/project.mpo");
		Files.createDirectories(snapshot.getParent());
		return Files.writeString(snapshot, "snapshot");
	}
}
