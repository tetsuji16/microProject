package com.projectlibre1.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AutoRecoveryStoreTest {
	@TempDir
	Path temporaryDirectory;

	@Test
	void recordsListsAndDiscardsCompletedSnapshot() throws Exception {
		AutoRecoveryStore store = new AutoRecoveryStore(temporaryDirectory.resolve("recovery"));
		Path snapshot = store.snapshotPath(42L);
		Files.writeString(snapshot, "complete project");
		Instant savedAt = Instant.parse("2026-08-12T01:02:03Z");
		store.recordCompletedSnapshot(42L, "Launch", null, savedAt);

		var entries = store.listRecoverable();
		assertEquals(1, entries.size());
		assertEquals("Launch", entries.getFirst().displayName());
		assertEquals(savedAt, entries.getFirst().savedAt());

		store.discard(42L);
		assertTrue(store.listRecoverable().isEmpty());
		assertFalse(Files.exists(snapshot));
	}

	@Test
	void onlyOffersNamedProjectWhenRecoveryIsNewerThanOriginal() throws Exception {
		AutoRecoveryStore store = new AutoRecoveryStore(temporaryDirectory.resolve("recovery"));
		Path original = temporaryDirectory.resolve("plan.pod");
		Files.writeString(original, "saved");
		Path snapshot = store.snapshotPath(7L);
		Files.writeString(snapshot, "recovery");
		store.recordCompletedSnapshot(7L, "Plan", original.toString(), Instant.now());

		Files.setLastModifiedTime(snapshot, FileTime.from(Instant.parse("2026-01-01T00:00:00Z")));
		Files.setLastModifiedTime(original, FileTime.from(Instant.parse("2026-01-02T00:00:00Z")));
		assertTrue(store.listRecoverable().isEmpty());

		Files.setLastModifiedTime(snapshot, FileTime.from(Instant.parse("2026-01-03T00:00:00Z")));
		assertEquals(1, store.listRecoverable().size());
	}

	@Test
	void cleanupRemovesExpiredSnapshotAndMetadata() throws Exception {
		AutoRecoveryStore store = new AutoRecoveryStore(temporaryDirectory.resolve("recovery"));
		Path snapshot = store.snapshotPath(9L);
		Files.writeString(snapshot, "recovery");
		store.recordCompletedSnapshot(9L, "Old", null, Instant.parse("2025-01-01T00:00:00Z"));
		for (Path file : Files.list(snapshot.getParent()).toList()) {
			Files.setLastModifiedTime(file, FileTime.from(Instant.parse("2025-01-01T00:00:00Z")));
		}

		store.cleanup(Instant.parse("2026-01-20T00:00:00Z"), Duration.ofDays(14));
		assertTrue(Files.list(snapshot.getParent()).findAny().isEmpty());
	}
}
