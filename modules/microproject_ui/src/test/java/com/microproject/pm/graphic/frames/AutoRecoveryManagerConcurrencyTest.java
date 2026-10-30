/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

import org.junit.jupiter.api.Test;

import com.microproject.application.AutoRecoveryStore;
import com.microproject.pm.task.ProjectFactory;

class AutoRecoveryManagerConcurrencyTest {
	@Test
	void recoveryIntervalIsBoundedBeforeSwingTimerConversion() {
		assertEquals(TimeUnit.MINUTES.toMillis(AutoRecoveryManager.MINIMUM_INTERVAL_MINUTES),
			AutoRecoveryManager.recoveryDelayMillis(Integer.MIN_VALUE));
		assertEquals(TimeUnit.MINUTES.toMillis(AutoRecoveryManager.MAXIMUM_INTERVAL_MINUTES),
			AutoRecoveryManager.recoveryDelayMillis(Integer.MAX_VALUE));
	}

	@Test
	void recoveryIntervalPreferenceIsClampedAndReloaded() throws Exception {
		Path recoveryDirectory = Files.createTempDirectory("auto-recovery-test-");
		Preferences preferences = Preferences.userRoot().node("microproject-test/" + System.nanoTime());
		preferences.putBoolean(AutoRecoveryManager.ENABLED_PREFERENCE, false);
		AutoRecoveryManager manager = new AutoRecoveryManager(ProjectFactory.createInstance(), null,
			new AutoRecoveryStore(recoveryDirectory), preferences);
		try {
			manager.setIntervalMinutes(Integer.MAX_VALUE);
			assertEquals(AutoRecoveryManager.MAXIMUM_INTERVAL_MINUTES, manager.getIntervalMinutes());
			assertEquals(AutoRecoveryManager.MAXIMUM_INTERVAL_MINUTES,
				preferences.getInt(AutoRecoveryManager.INTERVAL_MINUTES_PREFERENCE, -1));
		} finally {
			manager.stop();
		}

		AutoRecoveryManager reloaded = new AutoRecoveryManager(ProjectFactory.createInstance(), null,
			new AutoRecoveryStore(recoveryDirectory), preferences);
		try {
			assertEquals(AutoRecoveryManager.MAXIMUM_INTERVAL_MINUTES, reloaded.getIntervalMinutes());
			reloaded.setIntervalMinutes(Integer.MIN_VALUE);
			assertEquals(AutoRecoveryManager.MINIMUM_INTERVAL_MINUTES, reloaded.getIntervalMinutes());
		} finally {
			reloaded.stop();
			preferences.removeNode();
			Files.deleteIfExists(recoveryDirectory.resolve(".recovery.lock"));
			Files.deleteIfExists(recoveryDirectory);
		}
	}

	@Test
	void onlyOneRecoverySaveClaimWinsAndCompletionReleasesIt() throws Exception {
		Path recoveryDirectory = Files.createTempDirectory("auto-recovery-test-");
		Preferences preferences = Preferences.userRoot().node("microproject-test/" + System.nanoTime());
		preferences.putBoolean(AutoRecoveryManager.ENABLED_PREFERENCE, false);
		AutoRecoveryManager manager = new AutoRecoveryManager(ProjectFactory.createInstance(), null,
			new AutoRecoveryStore(recoveryDirectory), preferences);

		int workers = 16;
		long projectId = 42L;
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(workers);
		try {
			List<java.util.concurrent.Future<Boolean>> claims = new ArrayList<>();
			for (int i = 0; i < workers; i++) {
				claims.add(executor.submit(() -> {
					start.await();
					return manager.beginSave(projectId);
				}));
			}
			start.countDown();
			long successfulClaims = 0;
			for (var claim : claims) {
				if (claim.get(5, TimeUnit.SECONDS)) successfulClaims++;
			}
			assertEquals(1, successfulClaims);

			manager.completeSave(projectId);
			assertTrue(manager.beginSave(projectId), "completion must allow the next recovery save");
			manager.completeSave(projectId);
		} finally {
			executor.shutdownNow();
			manager.stop();
			preferences.removeNode();
			Files.deleteIfExists(recoveryDirectory.resolve(".recovery.lock"));
			Files.deleteIfExists(recoveryDirectory);
		}
	}

	@Test
	void claimRemainsHeldUntilQueuedMetadataIsPublished() throws Exception {
		Path directory = Files.createTempDirectory("auto-recovery-publication-");
		Preferences preferences = Preferences.userRoot().node("microproject-test/" + System.nanoTime());
		preferences.putBoolean(AutoRecoveryManager.ENABLED_PREFERENCE, false);
		ExecutorService executor = Executors.newSingleThreadExecutor();
		CountDownLatch unblock = new CountDownLatch(1);
		CountDownLatch entered = new CountDownLatch(1);
		executor.execute(() -> { entered.countDown(); try { unblock.await(); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); } });
		entered.await(5, TimeUnit.SECONDS);
		AutoRecoveryStore store = new AutoRecoveryStore(directory);
		AutoRecoveryManager manager = new AutoRecoveryManager(ProjectFactory.createInstance(), null, store, preferences, executor);
		try {
			long id = 42L;
			Path snapshot = store.newSnapshotPath(id, true);
			Files.writeString(snapshot, "complete");
			assertTrue(manager.beginSave(id));
			CountDownLatch published = new CountDownLatch(1);
			manager.recordSnapshotAsync(id, "Plan", null, snapshot, 0L, () -> { manager.completeSave(id); published.countDown(); });
			org.junit.jupiter.api.Assertions.assertFalse(manager.beginSave(id), "publication still pending");
			unblock.countDown();
			assertTrue(published.await(5, TimeUnit.SECONDS));
			assertEquals(snapshot, store.listRecoverable().getFirst().snapshot());
			assertTrue(manager.beginSave(id));
			manager.completeSave(id);
		} finally {
			unblock.countDown();
			manager.stop();
			store.discardAll();
			preferences.removeNode();
			Files.deleteIfExists(directory.resolve(".recovery.lock"));
			Files.deleteIfExists(directory);
		}
	}

	@Test
	void normalShutdownPreservesRecoveryOwnedByAnotherInstance() throws Exception {
		Path directory = Files.createTempDirectory("auto-recovery-clean-shutdown-");
		Preferences preferences = Preferences.userRoot().node("microproject-test/" + System.nanoTime());
		AutoRecoveryStore store = new AutoRecoveryStore(directory);
		AutoRecoveryManager manager = new AutoRecoveryManager(ProjectFactory.createInstance(), null, store, preferences);
		try {
			Path snapshot = store.newSnapshotPath(42L, true);
			Files.writeString(snapshot, "complete");
			store.recordCompletedSnapshot(42L, "Plan", null, java.time.Instant.now(), snapshot);
			assertEquals(1, store.listRecoverable().size());
			manager.completeNormalShutdown();
			assertEquals(1, store.listRecoverable().size());
			assertTrue(Files.exists(snapshot));
		} finally {
			manager.stop();
			store.discardAll();
			preferences.removeNode();
			Files.deleteIfExists(directory.resolve(".recovery.lock"));
			Files.deleteIfExists(directory);
		}
	}

	@Test
	void normalShutdownPreventsQueuedPublication() throws Exception {
		Path directory = Files.createTempDirectory("auto-recovery-shutdown-");
		Preferences preferences = Preferences.userRoot().node("microproject-test/" + System.nanoTime());
		ExecutorService executor = Executors.newSingleThreadExecutor();
		CountDownLatch unblock = new CountDownLatch(1);
		CountDownLatch entered = new CountDownLatch(1);
		executor.execute(() -> { entered.countDown(); try { unblock.await(); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); } });
		assertTrue(entered.await(5, TimeUnit.SECONDS));
		AutoRecoveryStore store = new AutoRecoveryStore(directory);
		AutoRecoveryManager manager = new AutoRecoveryManager(ProjectFactory.createInstance(), null, store, preferences, executor);
		try {
			Path snapshot = store.newSnapshotPath(42L, true);
			Files.writeString(snapshot, "complete");
			CountDownLatch finished = new CountDownLatch(1);
			manager.recordSnapshotAsync(42L, "Plan", null, snapshot, 0L, finished::countDown);
			manager.completeNormalShutdown();
			unblock.countDown();
			assertTrue(finished.await(5, TimeUnit.SECONDS));
			executor.submit(() -> {}).get(5, TimeUnit.SECONDS);
			assertTrue(store.listRecoverable().isEmpty());
		} finally {
			unblock.countDown();
			manager.stop();
			store.discardAll();
			preferences.removeNode();
			Files.deleteIfExists(directory.resolve(".recovery.lock"));
			Files.deleteIfExists(directory);
		}
	}

	@Test
	void discardReturnsWhileWorkerIsBlockedAndRemovesLateSnapshot() throws Exception {
		Path directory = Files.createTempDirectory("auto-recovery-discard-");
		Preferences preferences = Preferences.userRoot().node("microproject-test/" + System.nanoTime());
		ExecutorService executor = Executors.newSingleThreadExecutor();
		CountDownLatch unblock = new CountDownLatch(1);
		CountDownLatch entered = new CountDownLatch(1);
		executor.execute(() -> { entered.countDown(); try { unblock.await(); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); } });
		assertTrue(entered.await(5, TimeUnit.SECONDS));
		AutoRecoveryStore store = new AutoRecoveryStore(directory);
		AutoRecoveryManager manager = new AutoRecoveryManager(ProjectFactory.createInstance(), null, store, preferences, executor);
		ExecutorService caller = Executors.newSingleThreadExecutor();
		try {
			Path snapshot = store.newSnapshotPath(42L, true);
			caller.submit(() -> manager.discard(42L)).get(2, TimeUnit.SECONDS);
			unblock.countDown();
			executor.submit(() -> {}).get(5, TimeUnit.SECONDS);
			// The export finishes only after discard has completed.
			Files.writeString(snapshot, "late");
			CountDownLatch finished = new CountDownLatch(1);
			manager.recordSnapshotAsync(42L, "Plan", null, snapshot, 0L, finished::countDown);
			assertTrue(finished.await(5, TimeUnit.SECONDS));
			assertTrue(Files.notExists(snapshot));
			assertTrue(store.listRecoverable().isEmpty());
		} finally {
			unblock.countDown();
			caller.shutdownNow();
			manager.stop();
			store.discardAll();
			preferences.removeNode();
			Files.deleteIfExists(directory.resolve(".recovery.lock"));
			Files.deleteIfExists(directory);
		}
	}

	@Test
	void metadataSubmissionFailureReleasesClaim() throws Exception {
		Path directory = Files.createTempDirectory("auto-recovery-rejected-");
		Preferences preferences = Preferences.userRoot().node("microproject-test/" + System.nanoTime());
		preferences.putBoolean(AutoRecoveryManager.ENABLED_PREFERENCE, false);
		ExecutorService executor = Executors.newSingleThreadExecutor();
		executor.shutdown();
		AutoRecoveryManager manager = new AutoRecoveryManager(ProjectFactory.createInstance(), null,
			new AutoRecoveryStore(directory), preferences, executor);
		try {
			assertTrue(manager.beginSave(42L));
			Path late = directory.resolve("unused");
			Files.writeString(late, "late");
			CountDownLatch finished = new CountDownLatch(1);
			manager.recordSnapshotAsync(42L, "Plan", null, late, 0L, () -> { manager.completeSave(42L); finished.countDown(); });
			assertTrue(finished.await(5, TimeUnit.SECONDS));
			assertTrue(Files.notExists(late));
			assertTrue(manager.beginSave(42L), "failed submission must release the claim");
			manager.completeSave(42L);
		} finally {
			manager.stop();
			preferences.removeNode();
			Files.deleteIfExists(directory.resolve(".recovery.lock"));
			Files.deleteIfExists(directory);
		}
	}

	@Test
	void startupQueueRefreshesReplacedGenerationButSkipsBusyLease(@org.junit.jupiter.api.io.TempDir Path directory) throws Exception {
		AutoRecoveryStore store = new AutoRecoveryStore(directory);
		String series = java.util.UUID.randomUUID().toString();
		Path old = store.newSnapshotPath(51, true); Files.writeString(old, "old");
		store.recordCompletedSnapshot(51, "Plan", null, java.time.Instant.now(), old, series);
		var queue = new AutoRecoveryManager.OfferQueue(store);
		Path newer = store.newSnapshotPath(51, true); Files.writeString(newer, "new");
		store.recordCompletedSnapshot(51, "Plan", null, java.time.Instant.now(), newer, series);
		var candidate = queue.next();
		assertEquals(newer, candidate.entry().snapshot());
		try (var lease = candidate.lease()) {
			var busy = new AutoRecoveryManager.OfferQueue(store);
			org.junit.jupiter.api.Assertions.assertNull(busy.next());
		}
		try (var retry = store.tryOffer(store.listRecoverable().getFirst())) {
			org.junit.jupiter.api.Assertions.assertNotNull(retry);
		}
	}

	@Test
	void recoveredDocumentKeepsLeaseUntilExplicitSaveCleanup(@org.junit.jupiter.api.io.TempDir Path directory) throws Exception {
		Preferences preferences = Preferences.userRoot().node("microproject-test/" + System.nanoTime());
		preferences.putBoolean(AutoRecoveryManager.ENABLED_PREFERENCE, false);
		ExecutorService executor = Executors.newSingleThreadExecutor();
		AutoRecoveryStore store = new AutoRecoveryStore(directory);
		AutoRecoveryStore other = new AutoRecoveryStore(directory);
		AutoRecoveryManager manager = new AutoRecoveryManager(ProjectFactory.createInstance(), null, store, preferences, executor);
		try {
			Path snapshot = store.newSnapshotPath(55, true);
			Files.writeString(snapshot, "recovery");
			store.recordCompletedSnapshot(55, "Plan", null, java.time.Instant.now(), snapshot);
			var entry = store.listRecoverable().getFirst();
			var lease = store.tryOffer(entry);
			CountDownLatch completed = new CountDownLatch(1);
			var finish = AutoRecoveryManager.class.getDeclaredMethod("finishOffer", AutoRecoveryStore.OfferLease.class,
				AutoRecoveryStore.Entry.class, AutoRecoveryManager.OfferQueue.class, boolean.class, boolean.class, boolean.class,
				java.util.function.Consumer.class);
			finish.setAccessible(true);
			finish.invoke(manager, lease, entry, new AutoRecoveryManager.OfferQueue(store), true, true, false,
				(java.util.function.Consumer<Boolean>) ignored -> completed.countDown());
			assertTrue(completed.await(5, TimeUnit.SECONDS));
			org.junit.jupiter.api.Assertions.assertNull(other.tryOffer(entry));
			assertTrue(Files.exists(snapshot));
			manager.discard(55);
			executor.submit(() -> {}).get(5, TimeUnit.SECONDS);
			assertTrue(Files.notExists(snapshot));
		} finally { manager.stop(); preferences.removeNode(); }
	}

	@Test
	void failedSavePathCanReleaseClaimAndUnknownCompletionIsHarmless() throws Exception {
		Path recoveryDirectory = Files.createTempDirectory("auto-recovery-test-");
		Preferences preferences = Preferences.userRoot().node("microproject-test/" + System.nanoTime());
		preferences.putBoolean(AutoRecoveryManager.ENABLED_PREFERENCE, false);
		AutoRecoveryManager manager = new AutoRecoveryManager(ProjectFactory.createInstance(), null,
				new AutoRecoveryStore(recoveryDirectory), preferences);
		long projectId = 99L;
		try {
			assertTrue(manager.beginSave(projectId));
			try {
				throw new IllegalStateException("injected save failure");
			} catch (IllegalStateException expected) {
				// Mirrors save()'s failure handler: release in every failure path.
				manager.completeSave(projectId);
			}
			assertTrue(manager.beginSave(projectId));
			manager.completeSave(projectId);
			manager.completeSave(projectId);
			assertTrue(manager.beginSave(projectId), "an extra completion must not poison the claim");
			manager.completeSave(projectId);
		} finally {
			manager.stop();
			preferences.removeNode();
			Files.deleteIfExists(recoveryDirectory.resolve(".recovery.lock"));
			Files.deleteIfExists(recoveryDirectory);
		}
	}
}
