/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.collaboration;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

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
}
