package com.microproject.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;

import org.junit.jupiter.api.Test;

/**
 * Issue #185: Environment.isWindows() was hardcoded to always return true.
 * Verify it now reflects the actual operating system.
 */
class EnvironmentTest {

	@Test
	void isWindowsMatchesTheActualOsNameProperty() {
		String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
		assertEquals(osName.startsWith("windows"), Environment.isWindows());
	}

	@Test
	void macOsIsNotReportedAsWindows() {
		String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
		if (osName.startsWith("mac")) {
			assertEquals(false, Environment.isWindows());
		}
	}
}
