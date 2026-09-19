/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.exchange;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;

class MpoFormatVersionTest {
	@Test
	void currentReaderAcceptsAdditiveMinorVersionsAndRejectsOtherMajors() throws Exception {
		assertTrue(MpoFormatVersion.parse("1.0").isReadableBy(MpoFormatVersion.CURRENT));
		assertTrue(MpoFormatVersion.parse("1.99").isReadableBy(MpoFormatVersion.CURRENT));
		assertFalse(MpoFormatVersion.parse("2.0").isReadableBy(MpoFormatVersion.CURRENT));
	}

	@Test
	void malformedVersionsCannotSilentlyChangeCompatibility() {
		assertThrows(IOException.class, () -> MpoFormatVersion.parse("1"));
		assertThrows(IOException.class, () -> MpoFormatVersion.parse("1.-1"));
	}
}
