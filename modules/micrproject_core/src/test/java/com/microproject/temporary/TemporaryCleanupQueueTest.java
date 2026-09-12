/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.temporary;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class TemporaryCleanupQueueTest {
	@Test
	void retryRemovesAQueuedTemporaryFileAndItsMarker() throws Exception {
		Path root = Files.createTempDirectory("temporary-cleanup-queue-");
		try {
			Path temporary = Files.createFile(root.resolve("plan.mpo.123.tmp"));
			assertTrue(TemporaryCleanupQueue.deleteOrEnqueue(temporary));
			assertFalse(Files.exists(temporary));
			// Simulate a marker left by an earlier process.
			Files.createFile(temporary);
			Files.writeString(root.resolve("plan.mpo.123.tmp.delete"), "retryAt=now\n");
			TemporaryCleanupQueue.retry(root);
			assertFalse(Files.exists(temporary));
			assertFalse(Files.exists(root.resolve("plan.mpo.123.tmp.delete")));
		} finally {
			try (var paths = Files.walk(root)) {
				paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
					try { Files.deleteIfExists(path); } catch (java.io.IOException ignored) { }
				});
			}
		}
	}

	@Test
	void retryDoesNotDeleteUnrecognizedDeleteMarkers() throws Exception {
		Path root = Files.createTempDirectory("temporary-cleanup-safe-");
		try {
			Path unrelated = Files.createFile(root.resolve("user-file"));
			Path marker = Files.createFile(root.resolve("user-file.delete"));
			TemporaryCleanupQueue.retry(root);
			assertTrue(Files.exists(unrelated));
			assertTrue(Files.exists(marker));
		} finally {
			try (var paths = Files.walk(root)) {
				paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
					try { Files.deleteIfExists(path); } catch (java.io.IOException ignored) { }
				});
			}
		}
	}
}
