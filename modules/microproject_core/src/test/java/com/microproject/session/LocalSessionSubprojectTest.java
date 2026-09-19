/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class LocalSessionSubprojectTest {
	@Test
	void registersAnUnopenedProjectFileForMasterProjectInsertion() throws Exception {
		Path projectFile = Files.createTempFile("master-subproject-", ".pod");
		try {
			LocalSession session = new LocalSession();
			long id = session.registerProjectFile(projectFile.toString());

			assertTrue(id > 0L);
			assertTrue(session.projectExists(id));
			assertEquals(projectFile.toRealPath().toString(), session.getProjectFile(id));
		} finally {
			Files.deleteIfExists(projectFile);
		}
	}
}
