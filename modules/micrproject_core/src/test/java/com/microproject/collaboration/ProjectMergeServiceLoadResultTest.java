/*
 * MIT License
 * Copyright (c) 2026 microProject
 */
package com.microproject.collaboration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.AccessDeniedException;

import org.junit.jupiter.api.Test;

class ProjectMergeServiceLoadResultTest {
	@Test
	void accessDeniedFilesystemFailuresRemainDistinctFromInvalidProjectContent() {
		assertEquals(ProjectMergeService.LoadStatus.ACCESS_DENIED,
			ProjectMergeService.loadFailureStatus(new AccessDeniedException("locked.mpo")));
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
}
