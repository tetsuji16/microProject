/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.collaboration;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;

class CollaborationMetadataStoreTest {
	@Test
	void onlyMpoFilesAreCollaborationCandidates() {
		assertTrue(CollaborationMetadataStore.isMpoCollaborationCandidate("plan.mpo"));
		assertTrue(CollaborationMetadataStore.isMpoCollaborationCandidate("plan.MPO"));
		assertFalse(CollaborationMetadataStore.isMpoCollaborationCandidate("plan.pod"));
		assertFalse(CollaborationMetadataStore.isMpoCollaborationCandidate("plan.xml"));
		assertFalse(CollaborationMetadataStore.isMpoCollaborationCandidate("plan.xlsx"));
	}

	@Test
	void malformedSidecarIsNeverReplacedDuringCloudSync() throws Exception {
		Path project = Files.createTempFile("mpo-collaboration", ".mpo");
		Path sidecar = CollaborationMetadataStore.buildSidecarFile(project.toFile()).toPath();
		byte[] partial = "{\"schemaVersion\":".getBytes(StandardCharsets.UTF_8);
		Files.write(sidecar, partial);

		CollaborationMetadataStore store = new CollaborationMetadataStore(project.toFile());
		assertThrows(RuntimeException.class, () -> store.mutate(metadata -> metadata.setProjectFingerprint("must-not-write")));
		assertArrayEquals(partial, Files.readAllBytes(sidecar));
	}

	@Test
	void lockFileUsesTheStableCompatibilityName() throws Exception {
		Path project = Files.createTempFile("mpo-collaboration", ".mpo");
		Path sidecar = CollaborationMetadataStore.buildSidecarFile(project.toFile()).toPath();

		assertTrue(sidecar.getFileName().toString().endsWith(".projectlibre-sync.json"));
		assertTrue(CollaborationMetadataStore.buildLockFile(sidecar.toFile()).getName()
				.endsWith(".projectlibre-sync.lock"));
	}

	@Test
	void lockMarkerRemainsButActiveOsLockIsReleasedAfterTransaction() throws Exception {
		Path project = Files.createTempFile("mpo-collaboration", ".mpo");
		CollaborationMetadataStore store = new CollaborationMetadataStore(project.toFile());
		store.mutate(metadata -> metadata.setProjectFingerprint("transaction-complete"));

		Path lockPath = CollaborationMetadataStore.buildLockFile(store.getSidecarFile()).toPath();
		assertTrue(Files.isRegularFile(lockPath), "compatibility marker remains after the transaction");
		try (FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.WRITE);
			 FileLock lock = channel.tryLock()) {
			assertNotNull(lock, "the transaction must release its active OS lock");
		}
	}

	@Test
	void cleanupRemovesArtifactsOnlyWhenAllLeasesAreExpired() throws Exception {
		Path project = Files.createTempFile("mpo-collaboration", ".mpo");
		CollaborationMetadataStore store = new CollaborationMetadataStore(project.toFile());
		store.mutate(metadata -> {
			CollaborationMetadataStore.LockRecord lock = new CollaborationMetadataStore.LockRecord();
			lock.setOwnerKey("editor-1");
			lock.setLeaseUntil(System.currentTimeMillis() + 60_000L);
			metadata.getLocks().put("task-1", lock);
		});
		assertFalse(store.removeArtifactsIfUnowned(), "an active lease must block cleanup");
		assertTrue(Files.isRegularFile(store.getSidecarFile().toPath()));

		store.mutate(metadata -> metadata.getLocks().get("task-1")
				.setLeaseUntil(System.currentTimeMillis() - 1L));
		assertTrue(store.removeArtifactsIfUnowned(), "expired leases permit explicit cleanup");
		assertFalse(Files.exists(store.getSidecarFile().toPath()));
		assertFalse(Files.exists(CollaborationMetadataStore
				.buildLockFile(store.getSidecarFile()).toPath()));
	}

	@Test
	void cleanupNeverDeletesMalformedPartialSidecar() throws Exception {
		Path project = Files.createTempFile("mpo-collaboration", ".mpo");
		CollaborationMetadataStore store = new CollaborationMetadataStore(project.toFile());
		Path sidecar = store.getSidecarFile().toPath();
		byte[] partial = "{\"schemaVersion\":".getBytes(StandardCharsets.UTF_8);
		Files.write(sidecar, partial);
		assertFalse(store.removeArtifactsIfUnowned());
		assertArrayEquals(partial, Files.readAllBytes(sidecar));
	}

	@Test
	void jvmLockRegistryReclaimsEntriesAfterSuccessfulTransaction() throws Exception {
		Path project = Files.createTempFile("mpo-collaboration", ".mpo");
		CollaborationMetadataStore store = new CollaborationMetadataStore(project.toFile());
		store.mutate(metadata -> metadata.setProjectFingerprint("released"));
		assertEquals(0, CollaborationMetadataStore.jvmLockRegistrySizeForTests());
	}

	@Test
	void jvmLockRegistryStaysBoundedAcrossManyCanonicalPaths() throws Exception {
		Path root = Files.createTempDirectory("mpo-lock-registry-many-paths-");
		try {
			int maximumObserved = 0;
			for (int index = 0; index < 512; index++) {
				Path project = root.resolve("tenant-" + index).resolve("nested").resolve("plan.mpo");
				Files.createDirectories(project.getParent());
				CollaborationMetadataStore store = new CollaborationMetadataStore(project.toFile());
				String fingerprint = "path-" + index;
				store.mutate(metadata -> metadata.setProjectFingerprint(fingerprint));
				maximumObserved = Math.max(maximumObserved, CollaborationMetadataStore.jvmLockRegistrySizeForTests());
			}
			assertEquals(0, CollaborationMetadataStore.jvmLockRegistrySizeForTests(),
					"completed transactions must release every path entry");
			assertTrue(maximumObserved <= 1,
					"sequential canonical paths must not accumulate JVM lock entries");
		} finally {
			try (java.util.stream.Stream<Path> paths = Files.walk(root)) {
				paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
					try { Files.deleteIfExists(path); } catch (IOException ignored) { }
				});
			}
		}
	}

	@Test
	void jvmLockRegistryReclaimsEntriesWhenCallbackFails() throws Exception {
		Path project = Files.createTempFile("mpo-collaboration", ".mpo");
		CollaborationMetadataStore store = new CollaborationMetadataStore(project.toFile());
		RuntimeException failure = assertThrows(RuntimeException.class,
				() -> store.withLockedMetadata(new CollaborationMetadataStore.MetadataCallback<Void>() {
					@Override public Void execute(CollaborationMetadataStore.Metadata metadata) {
						throw new IllegalStateException("injected callback failure");
					}
				}));
		assertTrue(failure.getCause() instanceof IllegalStateException);
		assertEquals(0, CollaborationMetadataStore.jvmLockRegistrySizeForTests());
	}

	@Test
	void failedTemporaryWriteLeavesExistingSidecarByteForByteUnchanged() throws Exception {
		Path project = Files.createTempFile("mpo-collaboration", ".mpo");
		CollaborationMetadataStore store = new CollaborationMetadataStore(project.toFile());
		store.mutate(metadata -> metadata.setProjectFingerprint("before"));
		Path sidecar = store.getSidecarFile().toPath();
		byte[] original = Files.readAllBytes(sidecar);

		CollaborationMetadataStore failingStore = new CollaborationMetadataStore(project.toFile()) {
			@Override
			protected void writeTempFile(Path temporary, byte[] bytes) throws IOException {
				Files.write(temporary, new byte[] {'{'});
				throw new IOException("injected write failure");
			}
		};
		assertThrows(RuntimeException.class,
				() -> failingStore.mutate(metadata -> metadata.setProjectFingerprint("after")));
		assertArrayEquals(original, Files.readAllBytes(sidecar));
	}

	@Test
	void concurrentStoresPreserveBothUpdates() throws Exception {
		Path project = Files.createTempFile("mpo-collaboration", ".mpo");
		CollaborationMetadataStore first = new CollaborationMetadataStore(project.toFile());
		CollaborationMetadataStore second = new CollaborationMetadataStore(project.toFile());
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<Void> firstUpdate = executor.submit(mutateUser(first, "first"));
			Future<Void> secondUpdate = executor.submit(mutateUser(second, "second"));
			firstUpdate.get();
			secondUpdate.get();
		} finally {
			executor.shutdownNow();
		}

		CollaborationMetadataStore.Metadata metadata = first.load();
		assertEquals("first", metadata.getUsers().get("first").getUserKey());
		assertEquals("second", metadata.getUsers().get("second").getUserKey());
		assertEquals(0, CollaborationMetadataStore.jvmLockRegistrySizeForTests());
	}

	private static Callable<Void> mutateUser(CollaborationMetadataStore store, String userKey) {
		return () -> {
			store.mutate(metadata -> {
				CollaborationMetadataStore.UserRecord user = new CollaborationMetadataStore.UserRecord();
				user.setUserKey(userKey);
				metadata.getUsers().put(userKey, user);
			});
			return null;
		};
	}
}
