/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.exchange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.microproject.pm.task.Project;

class MpoExtractionLifecycleTest {
	@TempDir Path temporaryDirectory;

	@AfterEach
	void releaseAllOwnership() {
		MpoExtractionOwnershipRegistry.closeAll();
	}

	@Test
	void sessionWritesManagedManifestAndProjectCloseReleasesDirectory() throws Exception {
		Project project = Project.getDummy();
		MpoExtractionSession session = MpoExtractionSession.open(temporaryDirectory);
		Path directory = session.directory();
		session.extract("projects/child.mpo", new byte[] {1, 2, 3});
		Path manifest = directory.resolveSibling(directory.getFileName() + ".manifest");
		assertTrue(Files.isRegularFile(manifest));
		assertTrue(Files.readString(manifest).contains("purpose=mpof-extraction"));

		MpoExtractionOwnershipRegistry.attach(project, session);
		assertEquals(1, MpoExtractionOwnershipRegistry.size());
		assertTrue(MpoExtractionOwnershipRegistry.close(project));
		assertFalse(Files.exists(directory));
		assertFalse(Files.exists(manifest));
		assertEquals(0, MpoExtractionOwnershipRegistry.size());
	}

	@Test
	void replacingSessionClosesReloadedSessionWithoutLeakingFirstDirectory() throws Exception {
		Project project = Project.getDummy();
		MpoExtractionSession first = MpoExtractionSession.open(temporaryDirectory);
		Path firstDirectory = first.directory();
		first.extract("first.mpo", new byte[] {1});
		MpoExtractionOwnershipRegistry.attach(project, first);

		MpoExtractionSession second = MpoExtractionSession.open(temporaryDirectory);
		Path secondDirectory = second.directory();
		MpoExtractionOwnershipRegistry.attach(project, second);
		assertFalse(Files.exists(firstDirectory));
		assertTrue(Files.exists(secondDirectory));

		second.extract("second.mpo", new byte[] {2});
		MpoExtractionOwnershipRegistry.close(project);
		assertFalse(Files.exists(secondDirectory));
		}
}
