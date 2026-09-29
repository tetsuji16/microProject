/*
 * MIT License
 * Copyright (c) 2026 microProject
 */
package com.microproject.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assumptions;
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

	@Test
	void comparesAnExistingWindowsFileThroughItsGeneratedShortPath() throws Exception {
		Assumptions.assumeTrue(File.separatorChar == '\\', "Windows short-path aliases are only available on Windows.");
		Path directory = Files.createTempDirectory("microProjectLongIdentityPath-");
		Path projectFile = Files.createTempFile(directory, "canonical-window-", ".mpo");
		Process shortPathProcess = new ProcessBuilder("cmd.exe", "/d", "/c",
			"for %I in (\"" + projectFile + "\") do @echo %~sI").redirectErrorStream(true).start();
		try {
			assertTrue(shortPathProcess.waitFor(10, TimeUnit.SECONDS), "Windows short-path lookup timed out");
			String shortPath = new String(shortPathProcess.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
			Assumptions.assumeTrue(shortPath.contains("~"), "8.3 short paths are disabled on this volume");
			assertTrue(FilePathUtils.sameFileIdentity(projectFile.toString(), shortPath),
				"the existing file's long and 8.3 paths must resolve to one identity: " + projectFile + " / " + shortPath);
		} finally {
			shortPathProcess.destroyForcibly();
			Files.deleteIfExists(projectFile);
			Files.deleteIfExists(directory);
		}
	}
}
