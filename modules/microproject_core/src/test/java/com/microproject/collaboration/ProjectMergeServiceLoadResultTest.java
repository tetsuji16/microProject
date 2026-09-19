/*
 * MIT License
 * Copyright (c) 2026 microProject
 */
package com.microproject.collaboration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ProjectMergeServiceLoadResultTest {
	@Test
	void embeddedXmlIsStreamedWhenSeparatorCrossesTheReadBuffer() throws Exception {
		byte[] separator = "@@@@@@@@@@ProjectLibreSeparator_MSXML@@@@@@@@@@".getBytes(StandardCharsets.UTF_8);
		byte[] prefix = new byte[64 * 1024 - separator.length + 3];
		Arrays.fill(prefix, (byte) 'x');
		byte[] xml = "<Project><Tasks/></Project>".getBytes(StandardCharsets.UTF_8);
		Path file = Files.createTempFile("embedded-pod-", ".pod");
		try {
			byte[] pod = new byte[prefix.length + separator.length + xml.length];
			System.arraycopy(prefix, 0, pod, 0, prefix.length);
			System.arraycopy(separator, 0, pod, prefix.length, separator.length);
			System.arraycopy(xml, 0, pod, prefix.length + separator.length, xml.length);
			Files.write(file, pod);

			try (InputStream embedded = new ProjectMergeService().openEmbeddedPodXml(file.toString())) {
				assertArrayEquals(xml, embedded.readAllBytes());
			}
		} finally {
			Files.deleteIfExists(file);
		}
	}

	@Test
	void podWithoutSeparatorFallsBackWithoutReturningAnInputStream() throws Exception {
		Path file = Files.createTempFile("serialized-pod-", ".pod");
		try {
			Files.writeString(file, "serialized payload without embedded XML");
			assertTrue(new ProjectMergeService().openEmbeddedPodXml(file.toString()) == null);
		} finally {
			Files.deleteIfExists(file);
		}
	}

	@Test
	void accessDeniedFilesystemFailuresRemainDistinctFromInvalidProjectContent() {
		assertEquals(ProjectMergeService.LoadStatus.ACCESS_DENIED,
			ProjectMergeService.loadFailureStatus(new AccessDeniedException("locked.mpo")));
	}

	@Test
	void filesystemFailureClassificationKeepsMissingAndTransientFailuresDistinct() {
		assertEquals(ProjectMergeService.LoadStatus.NOT_FOUND,
			ProjectMergeService.loadFailureStatus(new NoSuchFileException("disappeared.mpo")));
		assertEquals(ProjectMergeService.LoadStatus.TRANSIENT_FAILURE,
			ProjectMergeService.loadFailureStatus(new IOException("file is still being written")));
	}

	@Test
	void importerFormatErrorsWrappedAsIoExceptionAreInvalidFiles() {
		assertEquals(ProjectMergeService.LoadStatus.INVALID_FILE,
			ProjectMergeService.loadFailureStatus(new IOException(
				"An MPOF file must contain META-INF/manifest.xml and content.xml.")));
	}

	@Test
	void brokenMpoContainerIsReportedAsAnInvalidProjectFile() throws Exception {
		Path file = Files.createTempFile("invalid-external-project-", ".mpo");
		try {
			Files.writeString(file, "not a ZIP archive");
			ProjectMergeService.ExternalProjectLoadResult result = new ProjectMergeService()
				.loadExternalProjectResult(file.toString());

			assertEquals(ProjectMergeService.LoadStatus.INVALID_FILE, result.getStatus());
			assertFalse(result.isSuccess());
			assertFalse(result.getCause() == null);
		} finally {
			Files.deleteIfExists(file);
		}
	}

	@Test
	void missingExternalProjectIsNotReportedAsSuccess() {
		ProjectMergeService.ExternalProjectLoadResult result = new ProjectMergeService()
			.loadExternalProjectResult("does-not-exist-" + System.nanoTime() + ".xml");

		assertEquals(ProjectMergeService.LoadStatus.NOT_FOUND, result.getStatus());
		assertFalse(result.isSuccess());
	}

	@Test
	void malformedExternalProjectIncludesFailureCause() throws Exception {
		Path file = Files.createTempFile("microproject-invalid-", ".xml");
		try {
			Files.writeString(file, "not a project file");
			ProjectMergeService.ExternalProjectLoadResult result = new ProjectMergeService()
				.loadExternalProjectResult(file.toString());

			assertEquals(ProjectMergeService.LoadStatus.INVALID_FILE, result.getStatus());
			assertFalse(result.isSuccess());
			assertFalse(result.getCause() == null);
		} finally {
			Files.deleteIfExists(file);
		}
	}

	@Test
	void deletedTaskCheckExposesLoadFailureInsteadOfReturningAnAmbiguousEmptySet() throws Exception {
		ProjectMergeService.DeletedTasksResult result = new ProjectMergeService().findDeletedTasksResult(
			"missing-external-project-" + System.nanoTime() + ".xml", Set.of(1L));

		assertEquals(ProjectMergeService.LoadStatus.NOT_FOUND, result.getLoadStatus());
		assertFalse(result.getDeletedTaskIds().contains(1L));
		assertTrue(result.hasLoadFailure());
	}
}
