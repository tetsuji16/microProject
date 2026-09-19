/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.exchange;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LocalFileImporterTest {
	@Test
	void legacyProjectDataDetectionHandlesANullExceptionMessage() {
		assertFalse(LocalFileImporter.isLegacyProjectDataFailure(new ClassNotFoundException()));
	}

	@Test
	void legacyProjectDataDetectionAcceptsOnlyTheKnownLegacyType() {
		assertTrue(LocalFileImporter.isLegacyProjectDataFailure(
				new ClassNotFoundException("com.projity.server.data.ProjectData")));
		assertFalse(LocalFileImporter.isLegacyProjectDataFailure(
				new ClassNotFoundException("another.type")));
	}
}
