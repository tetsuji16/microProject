/*
 * MIT License
 * Copyright (c) 2026 microProject
 */
package com.microproject.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class FilePathUtilsTest {
	@Test
	void canonicalizesWindowsSeparatorsAndDotSegments() {
		assertEquals("C:\\projects\\plan.mpp",
			FilePathUtils.canonicalPath("C:/projects/./draft/../plan.mpp"));
	}

	@Test
	void resolvesRelativeWindowsPathOnTheSameDrive() {
		assertEquals("child.mpo",
			FilePathUtils.relativePath("C:\\projects\\master.mpo", "C:\\projects\\child.mpo"));
		assertEquals("..\\child\\child.mpo",
			FilePathUtils.relativePath("C:\\projects\\master\\master.mpo", "C:\\projects\\child\\child.mpo"));
	}

	@Test
	void comparesWindowsProjectPathIdentityIndependentOfSeparatorDotSegmentsAndCase() {
		assertTrue(FilePathUtils.sameFileIdentity("C:/Plans/./Draft/../Plan.mpo", "c:\\plans\\plan.mpo"));
		assertFalse(FilePathUtils.sameFileIdentity("C:/Plans/Plan.mpo", "C:/Plans/Other.mpo"));
	}

	@Test
	void comparesHostPathsCanonicallyAndRejectsMissingIdentities() {
		Path projectFile = Path.of("plans", "project.mpo").toAbsolutePath();
		assertTrue(FilePathUtils.sameFileIdentity(projectFile.toString(),
			projectFile.getParent().resolve(".").resolve(projectFile.getFileName()).toString()));
		assertFalse(FilePathUtils.sameFileIdentity(null, projectFile.toString()));
		assertFalse(FilePathUtils.sameFileIdentity(" ", projectFile.toString()));
	}
}
