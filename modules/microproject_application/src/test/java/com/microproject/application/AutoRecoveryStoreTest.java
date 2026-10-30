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
	void usesProductRecoveryDirectoryAndRetainsUnresolvedLegacySnapshots() throws Exception {
		Path home = temporaryDirectory.resolve("home");
		Path appData = temporaryDirectory.resolve("appData");
		assertEquals(appData.resolve("microProject/recovery/1.recovery.pod").toAbsolutePath(),
			AutoRecoveryStore.forUserDirectories(home, appData).snapshotPath(1));
		assertEquals(home.resolve(".microproject/recovery/1.recovery.pod").toAbsolutePath(),
			AutoRecoveryStore.forUserDirectories(home, null).snapshotPath(1));
		Path legacyDirectory = appData.resolve("ProjectLibre/recovery");
		AutoRecoveryStore legacy = new AutoRecoveryStore(legacyDirectory);
		Files.writeString(legacy.snapshotPath(2), "legacy project");
		legacy.recordCompletedSnapshot(2, "Old plan", null, Instant.now());
		AutoRecoveryStore selected = AutoRecoveryStore.forUserDirectories(home, appData);
		assertEquals(legacyDirectory.resolve("2.recovery.pod").toAbsolutePath(), selected.listRecoverable().getFirst().snapshot());
		selected.discardAll();
		assertEquals(appData.resolve("microProject/recovery/3.recovery.pod").toAbsolutePath(),
			AutoRecoveryStore.forUserDirectories(home, appData).snapshotPath(3));
	}

	@Test
	void unpublishedOrFailedGenerationPreservesPreviousSnapshot() throws Exception {
		AutoRecoveryStore store = new AutoRecoveryStore(temporaryDirectory.resolve("recovery"));
		Path first = store.newSnapshotPath(42L, true);
		Files.writeString(first, "complete first generation");
		store.recordCompletedSnapshot(42L, "Plan", null, Instant.now(), first);
		Path incomplete = store.newSnapshotPath(42L, true);
		Files.writeString(incomplete, "partial");
		assertEquals(first, store.listRecoverable().getFirst().snapshot());
		assertEquals("complete first generation", Files.readString(first));
		Files.delete(incomplete);
		org.junit.jupiter.api.Assertions.assertThrows(java.io.IOException.class,
			() -> store.recordCompletedSnapshot(42L, "Plan", null, Instant.now(), incomplete));
		assertEquals(first, store.listRecoverable().getFirst().snapshot());
		store.discard(42L);
		assertFalse(Files.exists(first));
	}

	@Test
	void cleanupRemovesExpiredUnpublishedGeneration() throws Exception {
		AutoRecoveryStore store = new AutoRecoveryStore(temporaryDirectory.resolve("recovery"));
		Path orphan = store.newSnapshotPath(42L, true);
		Files.writeString(orphan, "partial");
		Files.setLastModifiedTime(orphan, FileTime.from(Instant.parse("2025-01-01T00:00:00Z")));
		store.cleanup(Instant.parse("2026-01-01T00:00:00Z"), Duration.ofDays(14));
		assertFalse(Files.exists(orphan));
	}

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
	void offersLegacyRecoveryEvenWhenOriginalIsNewer() throws Exception {
		AutoRecoveryStore store = new AutoRecoveryStore(temporaryDirectory.resolve("recovery"));
		Path original = temporaryDirectory.resolve("plan.pod");
		Files.writeString(original, "saved");
		Path snapshot = store.snapshotPath(7L);
		Files.writeString(snapshot, "recovery");
		store.recordCompletedSnapshot(7L, "Plan", original.toString(), Instant.now());

		Files.setLastModifiedTime(snapshot, FileTime.from(Instant.parse("2026-01-01T00:00:00Z")));
		Files.setLastModifiedTime(original, FileTime.from(Instant.parse("2026-01-02T00:00:00Z")));
		assertEquals(1, store.listRecoverable().size());

		Files.setLastModifiedTime(snapshot, FileTime.from(Instant.parse("2026-01-03T00:00:00Z")));
		assertEquals(1, store.listRecoverable().size());
	}

	@Test
	void cleanupRetainsOldSnapshotReferencedByRecentMetadata() throws Exception {
		AutoRecoveryStore store = new AutoRecoveryStore(temporaryDirectory.resolve("recovery"));
		Path snapshot = store.newSnapshotPath(9L, true);
		Files.writeString(snapshot, "complete generation");
		store.recordCompletedSnapshot(9L, "Plan", null, Instant.now(), snapshot);
		Files.setLastModifiedTime(snapshot, FileTime.from(Instant.parse("2025-01-01T00:00:00Z")));
		store.cleanup(Instant.now(), Duration.ofDays(14));
		assertEquals("complete generation", Files.readString(snapshot));
		assertEquals(snapshot, store.listRecoverable().getFirst().snapshot());
	}

	@Test
	void cleanupFailsClosedForRecentMalformedMetadata() throws Exception {
		AutoRecoveryStore store = new AutoRecoveryStore(temporaryDirectory.resolve("recovery"));
		Path snapshot = store.newSnapshotPath(9L, true);
		Files.writeString(snapshot, "complete generation");
		Files.setLastModifiedTime(snapshot, FileTime.from(Instant.parse("2025-01-01T00:00:00Z")));
		Files.writeString(snapshot.getParent().resolve("9.recovery.properties"), "projectId=broken\n");
		store.cleanup(Instant.now(), Duration.ofDays(14));
		assertTrue(Files.exists(snapshot));
	}

	@Test
	void cleanupPreservesUnrelatedFilesAndOldMalformedMetadata() throws Exception {
		AutoRecoveryStore store = new AutoRecoveryStore(temporaryDirectory.resolve("recovery"));
		Path orphan = store.newSnapshotPath(42L, true);
		Files.writeString(orphan, "orphan");
		Path unrelated = orphan.resolveSibling("notes.txt");
		Files.writeString(unrelated, "keep");
		Path malformed = orphan.resolveSibling("9.recovery.properties");
		Files.writeString(malformed, "projectId=broken\n");
		FileTime old = FileTime.from(Instant.parse("2025-01-01T00:00:00Z"));
		Files.setLastModifiedTime(orphan, old);
		Files.setLastModifiedTime(unrelated, old);
		Files.setLastModifiedTime(malformed, old);
		Path protectedSnapshot = store.newSnapshotPath(9L, true);
		Files.writeString(protectedSnapshot, "protected");
		Files.setLastModifiedTime(protectedSnapshot, old);
		store.cleanup(Instant.now(), Duration.ofDays(14));
		assertFalse(Files.exists(orphan));
		assertTrue(Files.exists(malformed));
		assertTrue(Files.exists(protectedSnapshot));
		assertEquals("keep", Files.readString(unrelated));
	}

	@Test
	void cleanupProtectsBothIdsWhenMetadataIdentityDisagrees() throws Exception {
		AutoRecoveryStore store = new AutoRecoveryStore(temporaryDirectory.resolve("recovery"));
		Path first = store.newSnapshotPath(9L, true);
		Path second = store.newSnapshotPath(10L, true);
		Files.writeString(first, "first");
		Files.writeString(second, "second");
		store.recordCompletedSnapshot(10L, "Plan", null, Instant.now(), second);
		Path metadata = store.listRecoverable().getFirst().metadata();
		Path mismatched = second.resolveSibling("9.recovery.properties");
		Files.move(metadata, mismatched);
		FileTime old = FileTime.from(Instant.parse("2025-01-01T00:00:00Z"));
		Files.setLastModifiedTime(first, old);
		Files.setLastModifiedTime(second, old);
		Files.setLastModifiedTime(mismatched, old);
		store.cleanup(Instant.now(), Duration.ofDays(14));
		assertTrue(Files.exists(first));
		assertTrue(Files.exists(second));
		assertTrue(Files.exists(mismatched));
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
		assertTrue(Files.list(snapshot.getParent()).allMatch(path -> path.getFileName().toString().equals(".recovery.lock")));
	}

	@Test
	void publicationWaitsForAnotherInstanceLock() throws Exception {
		AutoRecoveryStore store = new AutoRecoveryStore(temporaryDirectory.resolve("locked"));
		Path snapshot = store.newSnapshotPath(17L, true);
		Files.writeString(snapshot, "complete");
		var executor = java.util.concurrent.Executors.newSingleThreadExecutor();
		try (var channel = java.nio.channels.FileChannel.open(snapshot.getParent().resolve(".recovery.lock"),
				java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.WRITE);
			var lock = channel.lock()) {
			var started = new java.util.concurrent.CountDownLatch(1);
			var publication = executor.submit(() -> { started.countDown(); store.recordCompletedSnapshot(17L, "Plan", null, Instant.now(), snapshot); return null; });
			assertTrue(started.await(5, java.util.concurrent.TimeUnit.SECONDS));
			org.junit.jupiter.api.Assertions.assertThrows(java.util.concurrent.TimeoutException.class, () -> publication.get(100, java.util.concurrent.TimeUnit.MILLISECONDS));
			lock.release();
			publication.get(5, java.util.concurrent.TimeUnit.SECONDS);
			assertEquals(snapshot, store.listRecoverable().getFirst().snapshot());
		} finally { executor.shutdownNow(); }
	}

	@Test
	void orphanCleanupFailsClosedWhenMalformedManifestMayReferenceAnotherProject() throws Exception {
		AutoRecoveryStore store = new AutoRecoveryStore(temporaryDirectory.resolve("unknown-reference"));
		Path snapshot = store.newSnapshotPath(2, true); Files.writeString(snapshot, "important");
		Files.setLastModifiedTime(snapshot, FileTime.from(Instant.parse("2020-01-01T00:00:00Z")));
		Path malformed = snapshot.resolveSibling("1.recovery.properties");
		Files.writeString(malformed, "projectId=invalid\nsnapshotFile=" + snapshot.getFileName() + "\nsavedAt=2020-01-01T00:00:00Z\n");
		store.cleanupOrphanGenerations(Instant.now(), Duration.ofDays(14));
		assertTrue(Files.exists(snapshot));
		assertTrue(Files.exists(malformed));
		assertEquals(AutoRecoveryStore.MetadataIssueKind.MALFORMED, store.scanRecoverable().issues().getFirst().kind());
	}

	@Test
	void legacyAliasProtectsReferencedGenerationDuringSeriesCleanup() throws Exception {
		AutoRecoveryStore store = new AutoRecoveryStore(temporaryDirectory.resolve("legacy-alias"));
		String series = java.util.UUID.randomUUID().toString();
		Path old = store.newSnapshotPath(81, true); Files.writeString(old, "old");
		store.recordCompletedSnapshot(81, "Plan", null, Instant.now(), old, series);
		Path oldMetadata = store.listRecoverable().getFirst().metadata();
		Path alias = old.resolveSibling("81.recovery.properties");
		Files.copy(oldMetadata, alias);
		Path next = store.newSnapshotPath(81, true); Files.writeString(next, "new");
		store.recordCompletedSnapshot(81, "Plan", null, Instant.now(), next, series);
		assertTrue(Files.exists(oldMetadata));
		assertTrue(Files.exists(alias));
		assertTrue(Files.exists(old));
		assertEquals(3, store.listRecoverable().size());
		store.discardGeneration(81, old);
		assertTrue(Files.exists(old));
	}

	@Test
	void legacyMetadataProjectIdMismatchIsMalformedAndPreserved() throws Exception {
		AutoRecoveryStore store = new AutoRecoveryStore(temporaryDirectory.resolve("wrong-identity"));
		Path snapshot = store.snapshotPath(83); Files.writeString(snapshot, "recovery");
		store.recordCompletedSnapshot(83, "Plan", null, Instant.now(), snapshot);
		Path correct = snapshot.resolveSibling("83.recovery.properties");
		Path wrong = snapshot.resolveSibling("82.recovery.properties");
		Files.move(correct, wrong);
		var scan = store.scanRecoverable();
		assertTrue(scan.entries().isEmpty());
		assertEquals(AutoRecoveryStore.MetadataIssueKind.MALFORMED, scan.issues().getFirst().kind());
		store.cleanupOrphanGenerations(Instant.now().plus(Duration.ofDays(30)), Duration.ofDays(14));
		assertTrue(Files.exists(snapshot));
		assertTrue(Files.exists(wrong));
	}

	@Test
	void malformedOldManifestCannotDeleteNewlyPublishedGeneration() throws Exception {
		AutoRecoveryStore store = new AutoRecoveryStore(temporaryDirectory.resolve("bad-generation"));
		String series = java.util.UUID.randomUUID().toString();
		Path old = store.newSnapshotPath(80, true); Files.writeString(old, "old");
		store.recordCompletedSnapshot(80, "Plan", null, Instant.now(), old, series);
		Path oldMetadata = store.listRecoverable().getFirst().metadata();
		Path next = store.newSnapshotPath(80, true); Files.writeString(next, "new");
		var properties = new java.util.Properties();
		try (var input = Files.newInputStream(oldMetadata)) { properties.load(input); }
		properties.setProperty("snapshotFile", next.getFileName().toString());
		try (var output = Files.newOutputStream(oldMetadata)) { properties.store(output, "corrupt"); }
		store.recordCompletedSnapshot(80, "Plan", null, Instant.now(), next, series);
		assertTrue(Files.exists(oldMetadata));
		assertTrue(Files.exists(old));
		assertTrue(Files.exists(next));
		var scan = store.scanRecoverable();
		assertEquals(1, scan.issues().size());
		assertEquals(AutoRecoveryStore.MetadataIssueKind.MALFORMED, scan.issues().getFirst().kind());
		assertEquals(next, scan.entries().getFirst().snapshot());
	}

	@Test
	void publicationPrunesOnlyItsOwnUnleasedSeriesAndKeepsOtherInstances() throws Exception {
		AutoRecoveryStore first = new AutoRecoveryStore(temporaryDirectory.resolve("series"));
		AutoRecoveryStore second = new AutoRecoveryStore(temporaryDirectory.resolve("series"));
		String seriesA = java.util.UUID.randomUUID().toString();
		String seriesB = java.util.UUID.randomUUID().toString();
		Path a1 = first.newSnapshotPath(77, true);
		Path b1 = second.newSnapshotPath(77, true);
		Files.writeString(a1, "a1"); Files.writeString(b1, "b1");
		first.recordCompletedSnapshot(77, "A", null, Instant.now(), a1, seriesA);
		second.recordCompletedSnapshot(77, "B", null, Instant.now(), b1, seriesB);
		Path a2 = first.newSnapshotPath(77, true);
		Files.writeString(a2, "a2");
		first.recordCompletedSnapshot(77, "A", null, Instant.now(), a2, seriesA);
		assertFalse(Files.exists(a1));
		assertEquals(java.util.Set.of(a2, b1), first.listRecoverable().stream().map(AutoRecoveryStore.Entry::snapshot).collect(java.util.stream.Collectors.toSet()));
		try (var lease = first.tryOffer(first.listRecoverable().stream().filter(entry -> entry.snapshot().equals(a2)).findFirst().orElseThrow())) {
			Path a3 = first.newSnapshotPath(77, true); Files.writeString(a3, "a3");
			first.recordCompletedSnapshot(77, "A", null, Instant.now(), a3, seriesA);
			assertTrue(Files.exists(a2));
			assertTrue(Files.exists(b1));
		}
	}

	@Test
	void independentGenerationIsOfferedEvenWhenOriginalIsNewer() throws Exception {
		AutoRecoveryStore store = new AutoRecoveryStore(temporaryDirectory.resolve("newer-original"));
		Path original = temporaryDirectory.resolve("original.mpo"); Files.writeString(original, "saved by other process");
		Path snapshot = store.newSnapshotPath(78, true); Files.writeString(snapshot, "unsaved changes");
		Files.setLastModifiedTime(snapshot, FileTime.from(Instant.parse("2020-01-01T00:00:00Z")));
		store.recordCompletedSnapshot(78, "Plan", original.toString(), Instant.now(), snapshot, java.util.UUID.randomUUID().toString());
		assertEquals(snapshot, store.listRecoverable().getFirst().snapshot());
	}

	@Test
	void orphanCleanupRespectsLeaseAndReclaimsAfterRelease() throws Exception {
		AutoRecoveryStore store = new AutoRecoveryStore(temporaryDirectory.resolve("orphans"));
		Path old = store.newSnapshotPath(19L, true);
		Files.writeString(old, "old");
		store.recordCompletedSnapshot(19L, "Plan", null, Instant.now(), old);
		var lease = store.tryOffer(store.listRecoverable().getFirst());
		Path newer = store.newSnapshotPath(19L, true);
		Files.writeString(newer, "new");
		store.recordCompletedSnapshot(19L, "Plan", null, Instant.now(), newer);
		Files.setLastModifiedTime(old, FileTime.from(Instant.parse("2020-01-01T00:00:00Z")));
		store.cleanupOrphanGenerations(Instant.now(), Duration.ofDays(14));
		assertTrue(Files.exists(old));
		lease.close();
		assertTrue(Files.exists(old));
		assertEquals(2, store.listRecoverable().size());
		assertTrue(Files.exists(newer));
		Path orphan = store.newSnapshotPath(20L, true);
		Files.writeString(orphan, "orphan");
		Files.setLastModifiedTime(orphan, FileTime.from(Instant.parse("2020-01-01T00:00:00Z")));
		store.cleanupOrphanGenerations(Instant.now(), Duration.ofDays(14));
		assertFalse(Files.exists(orphan));
	}

	@Test
	void offerLeasePreservesSnapshotAndCancellationLeavesItRecoverable() throws Exception {
		AutoRecoveryStore first = new AutoRecoveryStore(temporaryDirectory.resolve("lease"));
		AutoRecoveryStore second = new AutoRecoveryStore(temporaryDirectory.resolve("lease"));
		Path old = first.newSnapshotPath(18L, true);
		Files.writeString(old, "old");
		first.recordCompletedSnapshot(18L, "Plan", null, Instant.now(), old);
		var entry = first.listRecoverable().getFirst();
		try (var lease = first.tryOffer(entry)) {
			org.junit.jupiter.api.Assertions.assertNotNull(lease);
			org.junit.jupiter.api.Assertions.assertNull(second.tryOffer(entry));
			Path newer = second.newSnapshotPath(18L, true);
			Files.writeString(newer, "new");
			second.recordCompletedSnapshot(18L, "Plan", null, Instant.now(), newer);
			second.discardGeneration(18L, old);
			assertTrue(Files.exists(old));
			lease.complete();
			assertEquals(newer, second.listRecoverable().getFirst().snapshot());
		}
		var replacement = second.listRecoverable().getFirst();
		try (var ignored = second.tryOffer(replacement)) { org.junit.jupiter.api.Assertions.assertNotNull(ignored); }
		assertEquals(replacement, first.listRecoverable().getFirst());
	}

	@Test
	void staleClaimDoesNotConsumeNewGenerationAndOnlyOneInstanceClaimsIt() throws Exception {
		AutoRecoveryStore first = new AutoRecoveryStore(temporaryDirectory.resolve("shared"));
		AutoRecoveryStore second = new AutoRecoveryStore(temporaryDirectory.resolve("shared"));
		Path old = first.newSnapshotPath(11L, true);
		Files.writeString(old, "old");
		first.recordCompletedSnapshot(11L, "Plan", null, Instant.now(), old);
		AutoRecoveryStore.Entry stale = first.listRecoverable().getFirst();
		Path newer = first.newSnapshotPath(11L, true);
		Files.writeString(newer, "new");
		first.recordCompletedSnapshot(11L, "Plan", null, Instant.now(), newer);
		assertEquals(2, first.listRecoverable().size());
		AutoRecoveryStore.Entry current = first.listRecoverable().stream().filter(entry -> entry.snapshot().equals(newer)).findFirst().orElseThrow();
		var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
		try {
			var start = new java.util.concurrent.CountDownLatch(1);
			var a = executor.submit(() -> { start.await(); return first.claimOffered(current); });
			var b = executor.submit(() -> { start.await(); return second.claimOffered(current); });
			start.countDown();
			assertTrue(a.get() ^ b.get());
		} finally { executor.shutdownNow(); }
		assertTrue(first.listRecoverable().stream().anyMatch(entry -> entry.equals(stale)));
		first.discardGeneration(11L, old);
		assertTrue(Files.exists(newer));
		assertTrue(Files.exists(current.metadata()));
	}

	@Test
	void legacyOfferedFlagDoesNotHideUndecidedRecovery() throws Exception {
		AutoRecoveryStore store = new AutoRecoveryStore(temporaryDirectory.resolve("legacy-offered"));
		Path snapshot = store.snapshotPath(22); Files.writeString(snapshot, "recovery");
		store.recordCompletedSnapshot(22, "Plan", null, Instant.now());
		Path metadata = store.listRecoverable().getFirst().metadata();
		var properties = new java.util.Properties();
		try (var input = Files.newInputStream(metadata)) { properties.load(input); }
		properties.setProperty("offered", "true");
		try (var output = Files.newOutputStream(metadata)) { properties.store(output, "legacy"); }
		assertEquals(snapshot, store.listRecoverable().getFirst().snapshot());
		try (var lease = store.tryOffer(store.listRecoverable().getFirst())) { lease.complete(); }
		assertTrue(store.listRecoverable().isEmpty());
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
