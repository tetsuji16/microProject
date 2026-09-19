/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
 * Copyright (c) 2026 microProject
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package com.microproject.application;

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

	@Test
	void offeredRecoveryIsConsumedWithoutDeletingTheCandidate() throws Exception {
		AutoRecoveryStore store = new AutoRecoveryStore(temporaryDirectory.resolve("recovery"));
		Path snapshot = store.snapshotPath(11L);
		Files.writeString(snapshot, "recovery");
		store.recordCompletedSnapshot(11L, "Plan", null, Instant.now());

		assertEquals(1, store.listRecoverable().size());
		store.markOffered(11L);

		assertTrue(Files.exists(snapshot));
		assertTrue(store.listRecoverable().isEmpty());
	}

	@Test
	void normalShutdownClearsOnlyRecoveryFiles() throws Exception {
		AutoRecoveryStore store = new AutoRecoveryStore(temporaryDirectory.resolve("recovery"));
		Path snapshot = store.snapshotPath(12L);
		Files.writeString(snapshot, "recovery");
		store.recordCompletedSnapshot(12L, "Plan", null, Instant.now());
		Path unrelated = snapshot.resolveSibling("keep.txt");
		Files.writeString(unrelated, "keep");

		store.discardAll();

		assertFalse(Files.exists(snapshot));
		assertFalse(Files.exists(snapshot.resolveSibling("12.recovery.properties")));
		assertTrue(Files.exists(unrelated));
	}

	@Test
	void malformedMetadataIsReturnedAsDiagnosticAndDoesNotHideValidCandidates() throws Exception {
		AutoRecoveryStore store = new AutoRecoveryStore(temporaryDirectory.resolve("recovery"));
		Path validSnapshot = store.snapshotPath(13L);
		Files.writeString(validSnapshot, "valid recovery");
		store.recordCompletedSnapshot(13L, "Valid", null, Instant.now());
		Path malformed = validSnapshot.resolveSibling("14.recovery.properties");
		Files.writeString(malformed, "projectId=not-a-number\nsavedAt=not-an-instant\n");
		Path malformedSnapshot = validSnapshot.resolveSibling("14.recovery.pod");
		Files.writeString(malformedSnapshot, "malformed recovery");

		AutoRecoveryStore.RecoveryScan scan = store.scanRecoverable();

		assertEquals(1, scan.entries().size());
		assertEquals(13L, scan.entries().getFirst().projectId());
		assertEquals(1, scan.issues().size());
		assertEquals(AutoRecoveryStore.MetadataIssueKind.MALFORMED, scan.issues().getFirst().kind());
		assertEquals(malformed, scan.issues().getFirst().metadata());
		assertTrue(scan.issues().getFirst().detail().contains("For input string"));
		assertTrue(scan.hasIssues());

		// The legacy API remains usable and logs the same diagnostic.
		assertEquals(1, store.listRecoverable().size());
	}

	@Test
	void unreadableMetadataIsReturnedAsDiagnostic() throws Exception {
		AutoRecoveryStore store = new AutoRecoveryStore(temporaryDirectory.resolve("recovery"));
		Path metadataDirectory = store.snapshotPath(15L).resolveSibling("15.recovery.properties");
		Files.createDirectory(metadataDirectory);

		AutoRecoveryStore.RecoveryScan scan = store.scanRecoverable();

		assertTrue(scan.entries().isEmpty());
		assertEquals(1, scan.issues().size());
		assertEquals(AutoRecoveryStore.MetadataIssueKind.UNREADABLE, scan.issues().getFirst().kind());
		assertEquals(metadataDirectory, scan.issues().getFirst().metadata());
	}

	@Test
	void recordsAndReloadsMpoSnapshotEvenWhenOriginalUsedLegacyExtension() throws Exception {
		AutoRecoveryStore store = new AutoRecoveryStore(temporaryDirectory.resolve("recovery"));
		Path snapshot = store.snapshotPath(16L, true);
		Files.writeString(snapshot, "mpo recovery");
		store.recordCompletedSnapshot(16L, "CCPM plan", "plan.pod", Instant.now(), snapshot);

		var entries = store.listRecoverable();
		assertEquals(1, entries.size());
		assertEquals(snapshot, entries.getFirst().snapshot());
		assertTrue(Files.exists(snapshot));
		store.discard(16L);
		assertFalse(Files.exists(snapshot));
	}
}
