/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TemporaryWorkspaceTest {
	@TempDir
	Path temporaryDirectory;

	@Test
	void createsManifestInsideDedicatedRootAndCloseRemovesArtifact() throws Exception {
		Path root = temporaryDirectory.resolve("microproject-temp");
		TemporaryWorkspace workspace = TemporaryWorkspace.open(root, Duration.ofDays(7));
		TemporaryWorkspace.TempArtifact artifact = workspace.createArtifact("mpo", ".part",
				Map.of("purpose", "embedded-project"));

		assertEquals(root.toAbsolutePath().normalize(), workspace.root());
		assertTrue(Files.isRegularFile(artifact.path()));
		assertTrue(Files.isRegularFile(artifact.manifest()));
		String manifest = Files.readString(artifact.manifest());
		assertTrue(manifest.contains("createdAt=" + artifact.createdAt() + "\n"));
		assertTrue(manifest.contains("purpose=embedded-project\n"));
		assertFalse(Files.exists(artifact.path().resolveSibling(artifact.manifest().getFileName() + ".part")));

		workspace.close();

		assertFalse(Files.exists(artifact.path()));
		assertFalse(Files.exists(artifact.manifest()));
	}

	@Test
	void rejectsPathSeparatorsAndTraversalComponents() throws Exception {
		try (TemporaryWorkspace workspace = TemporaryWorkspace.open(temporaryDirectory.resolve("root"), Duration.ofDays(7))) {
			assertThrows(IllegalArgumentException.class,
					() -> workspace.createArtifact("../outside", ".tmp", Map.of()));
			assertThrows(IllegalArgumentException.class,
					() -> workspace.createArtifact("nested\\outside", ".tmp", Map.of()));
			assertThrows(IllegalArgumentException.class,
					() -> workspace.createArtifact("safe", "../outside", Map.of()));
		}
	}

	@Test
	void startupCleanupHonorsTtlAndLeavesUnmanagedFilesAlone() throws Exception {
		Path root = temporaryDirectory.resolve("root");
		Files.createDirectories(root);
		Path stale = Files.createFile(root.resolve("microProject-temp-old.tmp"));
		Path staleManifest = Files.createFile(root.resolve("microProject-temp-old.tmp.manifest"));
		Path fresh = Files.createFile(root.resolve("microProject-temp-fresh.tmp"));
		Path unrelated = Files.createFile(root.resolve("other.tmp"));
		Instant now = Instant.parse("2026-09-12T00:00:00Z");
		Files.setLastModifiedTime(stale, FileTime.from(now.minus(Duration.ofDays(8))));
		Files.setLastModifiedTime(staleManifest, FileTime.from(now.minus(Duration.ofDays(8))));
		Files.setLastModifiedTime(fresh, FileTime.from(now.minus(Duration.ofDays(1))));

		TemporaryWorkspace.cleanupStale(root, now, Duration.ofDays(7));

		assertFalse(Files.exists(stale));
		assertFalse(Files.exists(staleManifest));
		assertTrue(Files.exists(fresh));
		assertTrue(Files.exists(unrelated));
	}

	@Test
	void failedDeletionCreatesMarkerAndNextCleanupRetriesIt() throws Exception {
		Path root = temporaryDirectory.resolve("root");
		Files.createDirectories(root);
		Path blocked = Files.createDirectory(root.resolve("microProject-temp-blocked.tmp"));
		Files.writeString(blocked.resolve("child"), "keep directory non-empty");
		Instant now = Instant.parse("2026-09-12T00:00:00Z");
		Files.setLastModifiedTime(blocked, FileTime.from(now.minus(Duration.ofDays(8))));

		TemporaryWorkspace.cleanupStale(root, now, Duration.ofDays(7));

		Path marker = root.resolve("microProject-temp-blocked.tmp.delete");
		assertTrue(Files.exists(blocked));
		assertTrue(Files.exists(marker));
		Files.delete(blocked.resolve("child"));
		TemporaryWorkspace.cleanupStale(root, now, Duration.ofDays(7));

		assertFalse(Files.exists(blocked));
		assertFalse(Files.exists(marker));
	}

	@Test
	void startupCleanupRemovesStaleMpoExtractionTree() throws Exception {
		Path root = temporaryDirectory.resolve("root");
		Files.createDirectories(root);
		Path extraction = Files.createDirectory(root.resolve("microProject-temp-mpof-orphan-1"));
		Files.writeString(extraction.resolve("child.mpo"), "stale");
		Path manifest = extraction.resolveSibling(extraction.getFileName() + ".manifest");
		Instant now = Instant.parse("2026-09-12T00:00:00Z");
		Files.writeString(manifest, mpofManifest(now.minus(Duration.ofDays(8)), "closed", 1L));
		Files.setLastModifiedTime(extraction, FileTime.from(now));

		TemporaryWorkspace.cleanupStale(root, now, Duration.ofDays(7));

		assertFalse(Files.exists(extraction));
		assertFalse(Files.exists(manifest));
	}

	@Test
	void startupCleanupNeverDeletesMpoExtractionWithMissingOrInvalidManifest() throws Exception {
		Path root = temporaryDirectory.resolve("root");
		Files.createDirectories(root);
		Instant now = Instant.parse("2026-09-12T00:00:00Z");
		for (String suffix : new String[] { "missing", "invalid-purpose", "invalid-created-at" }) {
			Path extraction = Files.createDirectory(root.resolve("microProject-temp-mpof-" + suffix));
			Files.writeString(extraction.resolve("child.mpo"), "retain");
			if (!"missing".equals(suffix)) {
				String manifest = "invalid-purpose".equals(suffix)
						? mpofManifest(now.minus(Duration.ofDays(8)), "closed", 1L).replace("purpose=mpof-extraction", "purpose=other")
						: "createdAt=not-a-timestamp\npurpose=mpof-extraction\n";
				Files.writeString(extraction.resolveSibling(extraction.getFileName() + ".manifest"), manifest);
			}
		}

		TemporaryWorkspace.cleanupStale(root, now, Duration.ofDays(7));

		assertTrue(Files.exists(root.resolve("microProject-temp-mpof-missing")));
		assertTrue(Files.exists(root.resolve("microProject-temp-mpof-invalid-purpose")));
		assertTrue(Files.exists(root.resolve("microProject-temp-mpof-invalid-created-at")));
	}

	@Test
	void startupCleanupDoesNotDeleteAnOpenExtractionFromThisProcess() throws Exception {
		Path root = temporaryDirectory.resolve("root");
		Files.createDirectories(root);
		Path extraction = Files.createDirectory(root.resolve("microProject-temp-mpof-current"));
		Files.writeString(extraction.resolve("child.mpo"), "retain");
		Path manifest = extraction.resolveSibling(extraction.getFileName() + ".manifest");
		Instant now = Instant.parse("2026-09-12T00:00:00Z");
		Files.writeString(manifest, mpofManifest(now.minus(Duration.ofDays(8)), "open",
				ProcessHandle.current().pid()));

		TemporaryWorkspace.cleanupStale(root, now, Duration.ofDays(7));

		assertTrue(Files.exists(extraction));
		assertTrue(Files.exists(manifest));
	}

	@Test
	void startupCleanupRemovesExpiredOpenExtractionFromAnotherProcess() throws Exception {
		Path root = temporaryDirectory.resolve("root");
		Files.createDirectories(root);
		Path extraction = Files.createDirectory(root.resolve("microProject-temp-mpof-previous-process"));
		Files.writeString(extraction.resolve("child.mpo"), "stale");
		Path manifest = extraction.resolveSibling(extraction.getFileName() + ".manifest");
		Instant now = Instant.parse("2026-09-12T00:00:00Z");
		Files.writeString(manifest, mpofManifest(now.minus(Duration.ofDays(8)), "open",
				ProcessHandle.current().pid() + 1L));

		TemporaryWorkspace.cleanupStale(root, now, Duration.ofDays(7));

		assertFalse(Files.exists(extraction));
		assertFalse(Files.exists(manifest));
	}

	@Test
	void startupCleanupLeavesRetryMarkerForUnmanagedMpoExtraction() throws Exception {
		Path root = temporaryDirectory.resolve("root");
		Files.createDirectories(root);
		Path extraction = Files.createDirectory(root.resolve("microProject-temp-mpof-retry"));
		Files.writeString(extraction.resolve("child.mpo"), "retain");
		Path manifest = extraction.resolveSibling(extraction.getFileName() + ".manifest");
		Files.writeString(manifest, "purpose=other\ncreatedAt=2026-09-01T00:00:00Z\n");
		Path marker = extraction.resolveSibling(extraction.getFileName() + ".delete");
		Files.writeString(marker, "retryAt=2026-09-12T00:00:00Z\n");

		TemporaryWorkspace.cleanupStale(root, Instant.parse("2026-09-12T00:00:00Z"), Duration.ofDays(7));

		assertTrue(Files.exists(extraction));
		assertTrue(Files.exists(manifest));
		assertTrue(Files.exists(marker));
	}

	@Test
	void startupCleanupRemovesExpiredMpoFManifestRetryAfterTreeGone() throws Exception {
		Path root = temporaryDirectory.resolve("root");
		Files.createDirectories(root);
		Path manifest = root.resolve("microProject-temp-mpof-closed.manifest");
		Instant now = Instant.parse("2026-09-12T00:00:00Z");
		Files.writeString(manifest, mpofManifest(now.minus(Duration.ofDays(8)), "closed", 1L));
		Path marker = root.resolve("microProject-temp-mpof-closed.manifest.delete");
		Files.writeString(marker, "retryAt=" + now + "\n");

		TemporaryWorkspace.cleanupStale(root, now, Duration.ofDays(7));

		assertFalse(Files.exists(manifest));
		assertFalse(Files.exists(marker));
	}

	@Test
	void workspaceIsThreadSafeForIndependentArtifacts() throws Exception {
		try (TemporaryWorkspace workspace = TemporaryWorkspace.open(temporaryDirectory.resolve("root"), Duration.ofDays(7))) {
			try (var executor = java.util.concurrent.Executors.newFixedThreadPool(4)) {
				var tasks = java.util.stream.IntStream.range(0, 16)
						.mapToObj(index -> executor.submit(() -> workspace.createArtifact("parallel-" + index, ".tmp", Map.of())))
						.toList();
				for (var task : tasks) assertTrue(Files.isRegularFile(task.get().path()));
			}
		}
	}

	private static String mpofManifest(Instant createdAt, String state, long processId) {
		Instant processStart = ProcessHandle.current().info().startInstant().orElseThrow();
		return "createdAt=" + createdAt + "\n"
				+ "purpose=mpof-extraction\n"
				+ "instanceId=4f5d2af5-6d5d-4b26-8f7b-7e4f4f9f9f30\n"
				+ "processId=" + processId + "\n"
				+ "processStart=" + processStart + "\n"
				+ "state=" + state + "\n";
	}
}
