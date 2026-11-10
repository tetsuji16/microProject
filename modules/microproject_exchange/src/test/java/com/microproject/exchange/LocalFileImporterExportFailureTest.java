/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.exchange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

/** A POD export that cannot complete must fail the save and leave the existing target alone. */
class LocalFileImporterExportFailureTest {

	@Test
	void failedReplaceIsReportedToTheCallerAndKeepsTheTarget(@TempDir Path directory) throws Exception {
		// A non-empty directory in the target's place cannot be replaced by the exported file.
		Path target = Files.createDirectory(directory.resolve("plan.pod"));
		Path keep = Files.writeString(target.resolve("keep.txt"), "ORIGINAL", StandardCharsets.UTF_8);
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("export-failure", undo), undo);
		project.initialize(false, false);
		LocalFileImporter exporter = new LocalFileImporter();
		exporter.setFileName(target.toString());
		exporter.setProject(project);

		LocalFileImporter.PodSaveException failure = assertThrows(LocalFileImporter.PodSaveException.class,
			exporter::exportFile);

		assertTrue(failure.userFacingMessage().contains(directory.toString()), failure.userFacingMessage());
		assertEquals("ORIGINAL", Files.readString(keep, StandardCharsets.UTF_8));
	}

	@Test
	void exportToMissingFolderThrows(@TempDir Path directory) {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("export-missing", undo), undo);
		project.initialize(false, false);
		LocalFileImporter exporter = new LocalFileImporter();
		exporter.setFileName(directory.resolve("missing").resolve("plan.pod").toString());
		exporter.setProject(project);

		assertThrows(java.io.IOException.class, exporter::exportFile);
	}
}
