/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.collaboration;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;

class CollaborationMetadataStoreTest {
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
