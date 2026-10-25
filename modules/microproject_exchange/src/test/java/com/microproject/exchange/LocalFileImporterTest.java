/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.exchange;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.security.MessageDigest;

import org.junit.jupiter.api.Test;


class LocalFileImporterTest {
	@Test
	void legacyProjectDataDetectionHandlesANullExceptionMessage() {
		assertFalse(LocalFileImporter.isLegacyProjectDataFailure(new ClassNotFoundException()));
	}

	@Test
	void legacyProjectDataDetectionAcceptsOnlyTheKnownLegacyType() {
		assertTrue(LocalFileImporter.isLegacyProjectDataFailure(
				new ClassNotFoundException("com.projity.server.data.ProjectData")));
		assertFalse(LocalFileImporter.isLegacyProjectDataFailure(
				new ClassNotFoundException("another.type")));
	}

	@Test
	void malformedTruncatedAndUnsafePodPayloadsAreRejectedWithoutPublicationOrMutation() throws Exception {
		byte[] unsafe = serialize(new URL("https://example.invalid/unsafe"));
		byte[] truncated = java.util.Arrays.copyOf(unsafe, 5);
		for (byte[] payload : new byte[][] { truncated, unsafe }) {
			File source = Files.createTempFile("invalid-pod-boundary-", ".pod").toFile();
			try {
				Files.write(source.toPath(), payload);
				byte[] before = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(source.toPath()));
				LocalFileImporter importer = new LocalFileImporter();
				importer.setFileName(source.getAbsolutePath());
				assertThrows(IOException.class, importer::importFile,
					"invalid serialized data must surface a typed import failure");
				assertNull(importer.getProject(), "failed deserialization must not publish a partial project");
				assertTrue(java.util.Arrays.equals(before,
					MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(source.toPath()))),
					"import must not mutate source bytes");
			} finally {
				Files.deleteIfExists(source.toPath());
			}
		}
	}

	@Test
	void missingPodIsRejectedWithoutPublishingAProject() throws Exception {
		var directory = Files.createTempDirectory("missing-pod-");
		try {
			File missing = directory.resolve("absent.pod").toFile();
			LocalFileImporter importer = new LocalFileImporter();
			importer.setFileName(missing.getAbsolutePath());
			assertThrows(FileNotFoundException.class, importer::importFile);
			assertNull(importer.getProject());
		} finally {
			Files.deleteIfExists(directory);
		}
	}

	private static byte[] serialize(Object value) throws Exception {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
			output.writeObject(value);
		}
		return bytes.toByteArray();
	}

}
