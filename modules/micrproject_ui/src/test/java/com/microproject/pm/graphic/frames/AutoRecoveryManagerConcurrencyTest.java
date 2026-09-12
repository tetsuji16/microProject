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
			Files.deleteIfExists(recoveryDirectory);
		}
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
			Files.deleteIfExists(recoveryDirectory);
		}
	}
}
