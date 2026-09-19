package com.microproject.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.microproject.strings.Messages;

/**
 * Issue #186: toAppletVersion used Integer.parseInt on each dot-separated
 * segment, so a version suffix such as "-beta" threw NumberFormatException.
 */
class VersionParseTest {

	@Test
	void toAppletVersionToleratesNonNumericSegments() {
		assertDoesNotThrow(() -> VersionUtils.toAppletVersion("1.2.3-beta"));
		assertDoesNotThrow(() -> Messages.toAppletVersion("1.2.3-beta"));
	}

	@Test
	void toAppletVersionKeepsNumericBehavior() {
		assertEquals("1.2.3.4", VersionUtils.toAppletVersion("1.2.3.4"));
		assertEquals("1.2.3.4", Messages.toAppletVersion("1.2.3.4"));
	}
}
