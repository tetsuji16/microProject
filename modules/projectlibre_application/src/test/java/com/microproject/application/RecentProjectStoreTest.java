package com.microproject.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.util.List;
import java.util.UUID;
import java.util.prefs.Preferences;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RecentProjectStoreTest {
	@TempDir java.nio.file.Path temp;
	@Test void recordsPinsAndRestoresExistingSessionFiles() throws Exception {
		Preferences prefs = Preferences.userRoot().node("projectlibre-test-" + UUID.randomUUID());
		try {
			RecentProjectStore store = new RecentProjectStore(prefs); var file = Files.createFile(temp.resolve("plan.pod"));
			store.recordOpened(file.toString()); store.setPinned(file, true); store.saveSession(List.of(file.toString(), temp.resolve("missing.pod").toString()));
			assertTrue(store.entries().getFirst().pinned()); assertEquals(List.of(file.toAbsolutePath()), store.session());
		} finally { prefs.removeNode(); }
	}
}
