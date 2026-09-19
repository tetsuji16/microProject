/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.collaboration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Acceptance coverage for issue #546's bounded JVM lock registry contract. */
class JvmLockRegistryAcceptanceTest {
	@TempDir Path temp;

	@Test
	void reclaimsMoreThan512CanonicalPathsWithoutUnboundedGrowth() throws Exception {
		int maximum = 0;
		for (int index = 0; index < 513; index++) {
			final int pathIndex = index;
			Path project = temp.resolve("tenant-" + pathIndex).resolve("nested").resolve("plan.mpo");
			Files.createDirectories(project.getParent());
			new CollaborationMetadataStore(project.toFile()).mutate(metadata ->
				metadata.setProjectFingerprint("fingerprint-" + pathIndex));
			maximum = Math.max(maximum, CollaborationMetadataStore.jvmLockRegistrySizeForTests());
		}
		assertEquals(0, CollaborationMetadataStore.jvmLockRegistrySizeForTests());
		assertTrue(maximum <= 1, "released canonical paths must not accumulate entries");
	}

	@Test
	void releasesEntryAfterFailureAndInterruption() throws Exception {
		Path project = Files.createTempFile(temp, "failure-", ".mpo");
		CollaborationMetadataStore store = new CollaborationMetadataStore(project.toFile());
		assertThrows(RuntimeException.class, () -> store.withLockedMetadata(new CollaborationMetadataStore.MetadataCallback<Void>() {
			@Override public Void execute(CollaborationMetadataStore.Metadata metadata) {
				throw new IllegalStateException("injected failure");
			}
		}));
		assertEquals(0, CollaborationMetadataStore.jvmLockRegistrySizeForTests());

		AtomicReference<Throwable> failure = new AtomicReference<>();
		Thread interrupted = new Thread(() -> {
			try {
				store.withLockedMetadata(new CollaborationMetadataStore.MetadataCallback<Void>() {
					@Override public Void execute(CollaborationMetadataStore.Metadata metadata) {
					if (Thread.currentThread().isInterrupted())
						throw new IllegalStateException("interrupted callback");
					return null;
					}
				});
			} catch (Throwable t) {
				failure.set(t);
			}
		});
		interrupted.start();
		interrupted.interrupt();
		interrupted.join(5000);
		assertTrue(!interrupted.isAlive());
		assertNotNull(failure.get());
		assertEquals(0, CollaborationMetadataStore.jvmLockRegistrySizeForTests());
	}

	@Test
	void concurrentSameCanonicalPathUsesOneMonitor() throws Exception {
		Path project = Files.createTempFile(temp, "contended-", ".mpo");
		CollaborationMetadataStore first = new CollaborationMetadataStore(project.toFile());
		CollaborationMetadataStore second = new CollaborationMetadataStore(project.toFile().getCanonicalFile());
		CountDownLatch entered = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		AtomicReference<Object> firstMonitor = new AtomicReference<>();
		AtomicReference<Object> secondMonitor = new AtomicReference<>();
		Thread owner = new Thread(() -> first.withLockedMetadata(new CollaborationMetadataStore.MetadataCallback<Void>() {
			@Override public Void execute(CollaborationMetadataStore.Metadata metadata) {
			firstMonitor.set(CollaborationMetadataStore.jvmLockMonitorForTests(project.toFile()));
			entered.countDown();
			try {
				release.await(5, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException("owner interrupted", e);
			}
			return null;
			}
		}));
		Thread waiter = new Thread(() -> second.withLockedMetadata(new CollaborationMetadataStore.MetadataCallback<Void>() {
			@Override public Void execute(CollaborationMetadataStore.Metadata metadata) {
			secondMonitor.set(CollaborationMetadataStore.jvmLockMonitorForTests(project.toFile()));
			return null;
			}
		}));
		owner.start();
		assertTrue(entered.await(5, TimeUnit.SECONDS));
		waiter.start();
		Thread.sleep(50);
		release.countDown();
		owner.join(5000);
		waiter.join(5000);
		assertSame(firstMonitor.get(), secondMonitor.get(), "canonical path must share one JVM monitor");
		assertEquals(0, CollaborationMetadataStore.jvmLockRegistrySizeForTests());
	}
}
