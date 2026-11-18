/* MIT License — Copyright (c) 2026 microProject */
package com.microproject.collaboration;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * The collaboration sidecar is written beside the project file, so the repository
 * ignore rules must cover the current name. They used to list only the legacy
 * {@code .projectlibre-sync} files, which left every new sidecar untracked in a
 * fresh clone (#999).
 */
class CollaborationSidecarIgnoreTest {

	@Test
	void sidecarAndLockUseTheCurrentName() {
		File project = new File("plan.mpo");
		File sidecar = CollaborationMetadataStore.buildSidecarFile(project);
		assertEquals("plan.microproject-sync.json", sidecar.getName());
		assertEquals("plan.microproject-sync.lock", CollaborationMetadataStore.buildLockFile(sidecar).getName());
	}

	@Test
	void repositoryIgnoresTheCurrentAndLegacySidecarNames() throws IOException {
		Path root = repositoryRoot();
		assumeTrue(root != null, "the repository root is not reachable from this working directory");
		List<String> rules = Files.readAllLines(root.resolve(".gitignore"));
		assertTrue(rules.contains("/samples/*.microproject-sync.json"),
			"the committed .gitignore must ignore the current sidecar name");
		assertTrue(rules.contains("/samples/*.microproject-sync.lock"),
			"the committed .gitignore must ignore the current sidecar lock name");
		assertTrue(rules.contains("/samples/*.projectlibre-sync.json"),
			"sidecars left by an older release must stay ignored");
		assertTrue(rules.contains("/samples/*.projectlibre-sync.lock"),
			"sidecar locks left by an older release must stay ignored");
	}

	/** Walks up from the test working directory to the checkout that holds the ignore file. */
	private static Path repositoryRoot() {
		Path directory = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
		while (directory != null) {
			if (Files.isRegularFile(directory.resolve(".gitignore"))
					&& (Files.isRegularFile(directory.resolve("settings.gradle"))
						|| Files.isRegularFile(directory.resolve("settings.gradle.kts")))) {
				return directory;
			}
			directory = directory.getParent();
		}
		return null;
	}
}
