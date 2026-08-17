package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;

import org.junit.jupiter.api.Test;

/**
 * Issue #186: the --projectId option was parsed with an unguarded
 * Long.parseLong, so a non-numeric value crashed startup with
 * NumberFormatException.
 */
class ApplicationStartupFactoryProjectIdTest {

	@Test
	void malformedProjectIdOptionIsIgnoredInsteadOfCrashingStartup() {
		HashMap<String, Object> opts = new HashMap<>();
		opts.put("projectId", "abc");
		ApplicationStartupFactory factory = assertDoesNotThrow(() -> new ApplicationStartupFactory(opts));
		assertEquals(0L, factory.projectId);
	}

	@Test
	void validProjectIdOptionIsParsed() {
		HashMap<String, Object> opts = new HashMap<>();
		opts.put("projectId", "42");
		ApplicationStartupFactory factory = new ApplicationStartupFactory(opts);
		assertEquals(42L, factory.projectId);
	}
}
