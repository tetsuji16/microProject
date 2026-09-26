/*
 * MIT License
 * Copyright (c) 2026 microProject
 */
package com.microproject.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
	}
}
