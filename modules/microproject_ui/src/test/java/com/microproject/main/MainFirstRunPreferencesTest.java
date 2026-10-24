/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.main;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import java.util.prefs.Preferences;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MainFirstRunPreferencesTest {
	private Preferences preferences;

	@BeforeEach
	void createIsolatedPreferencesNode() throws Exception {
		preferences = Preferences.userNodeForPackage(Main.class).node("first-run-test-" + UUID.randomUUID());
		preferences.clear();
	}

	@AfterEach
	void removeIsolatedPreferencesNode() throws Exception {
		preferences.removeNode();
	}

	@Test
	void canonicalValueTakesPriorityAndIsNotReplacedAtStartup() {
		preferences.putLong("microprojectFirstRun", 10L);
		preferences.putLong("projectlibreFirstRun", 20L);
		preferences.putLong("projectlibrefirstRun", 30L);

		assertEquals(10L, Main.getFirstRun(preferences, 40L));
		assertEquals(10L, Main.initializeFirstRun(preferences, 50L));
		assertEquals("10", preferences.get("microprojectFirstRun", null));
	}

	@Test
	void legacyReadKeyHasPriorityAndIsMigratedToCanonicalKey() {
		preferences.putLong("projectlibreFirstRun", 20L);
		preferences.putLong("projectlibrefirstRun", 30L);

		assertEquals(20L, Main.initializeFirstRun(preferences, 40L));
		assertEquals("20", preferences.get("microprojectFirstRun", null));
	}

	@Test
	void legacyWriterKeyIsReadWhenTheOtherLegacyKeyIsAbsent() {
		preferences.putLong("projectlibrefirstRun", 30L);

		assertEquals(30L, Main.initializeFirstRun(preferences, 40L));
		assertEquals("30", preferences.get("microprojectFirstRun", null));
	}

	@Test
	void invalidCanonicalValueDoesNotFallBackToLegacyValue() {
		preferences.put("microprojectFirstRun", "invalid");
		preferences.putLong("projectlibreFirstRun", 20L);

		assertEquals(40L, Main.getFirstRun(preferences, 40L));
		assertEquals(50L, Main.initializeFirstRun(preferences, 50L));
		assertEquals("50", preferences.get("microprojectFirstRun", null));
	}

	@Test
	void invalidLegacyValuesFallThroughOrUseTheProvidedDefault() {
		preferences.put("projectlibreFirstRun", "invalid");
		preferences.putLong("projectlibrefirstRun", 30L);

		assertEquals(30L, Main.initializeFirstRun(preferences, 40L));

		preferences.remove("microprojectFirstRun");
		preferences.put("projectlibrefirstRun", "also-invalid");
		preferences.remove("projectlibreFirstRun");
		assertEquals(50L, Main.initializeFirstRun(preferences, 50L));
	}

	@Test
	void runNumberUsesCanonicalValueAndWritesOnlyTheCanonicalKey() {
		preferences.putInt("microprojectRunNumber", 10);
		preferences.putInt("projectlibreRunNumber", 20);

		assertEquals(10, Main.getRunNumber(preferences));
		assertEquals(11, Main.incrementRunNumber(preferences));
		assertEquals("11", preferences.get("microprojectRunNumber", null));
		assertEquals("20", preferences.get("projectlibreRunNumber", null));
	}

	@Test
	void runNumberReadsLegacyValueOnceAndWritesCanonicalKey() {
		preferences.putInt("projectlibreRunNumber", 7);

		assertEquals(8, Main.incrementRunNumber(preferences));
		assertEquals("8", preferences.get("microprojectRunNumber", null));
	}

	@Test
	void invalidCanonicalRunNumberDoesNotFallBackToLegacyValue() {
		preferences.put("microprojectRunNumber", "invalid");
		preferences.putInt("projectlibreRunNumber", 7);

		assertEquals(0, Main.getRunNumber(preferences));
		assertEquals(1, Main.incrementRunNumber(preferences));
		assertEquals("1", preferences.get("microprojectRunNumber", null));
	}
}
