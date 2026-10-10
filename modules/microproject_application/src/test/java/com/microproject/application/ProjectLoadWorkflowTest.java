/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
 * Copyright (c) 2026 microProject
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package com.microproject.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.microproject.session.LocalSession;
import com.microproject.session.LoadOptions;

class ProjectLoadWorkflowTest {
	@TempDir
	Path temporaryDirectory;

	@Test
	void preparesLoadOptionsForMpoCollaborationFile() {
		LoadOptions options = ProjectLoadWorkflow.prepareLoadOptions("sample.MPO", "alice");

		assertEquals("sample.MPO", options.getFileName());
		assertEquals(LocalSession.MPO_PROJECT_IMPORTER, options.getImporter());
		assertTrue(options.isCollaborationEnabled());
		assertEquals("alice", options.getCollaborationUserKey());
		assertTrue(options.getSidecarFileName().endsWith(".projectlibre-sync.json"));
	}

	@Test
	void legacyAndExchangeFormatsDoNotEnableCollaboration() {
		for (String fileName : new String[] { "sample.pod", "sample.POD", "sample.xml", "sample.xlsx" }) {
			LoadOptions options = ProjectLoadWorkflow.prepareLoadOptions(fileName, "alice");
			assertFalse(options.isCollaborationEnabled(), fileName);
			assertEquals(null, options.getSidecarFileName(), fileName);
			if (fileName.toLowerCase(java.util.Locale.ROOT).endsWith(".pod")) {
				assertEquals(LocalSession.LOCAL_PROJECT_IMPORTER, options.getImporter(), fileName);
			}
		}
	}

	@Test
	void retiredServerModeFlagDoesNotSelectServerImporter() {
		for (boolean localOnlySession : new boolean[] { true, false }) {
			LoadOptions options = ProjectLoadWorkflow.prepareLoadOptions("sample.pod", localOnlySession, "alice");
			assertEquals(LocalSession.LOCAL_PROJECT_IMPORTER, options.getImporter());
		}
	}

	@Test
	void preparesLoadOptionsForMicrosoftFile() {
		LoadOptions options = ProjectLoadWorkflow.prepareLoadOptions("sample.mpp", "alice");

		assertEquals("sample.mpp", options.getFileName());
		assertEquals(LocalSession.MICROSOFT_PROJECT_IMPORTER, options.getImporter());
		assertFalse(options.isCollaborationEnabled());
	}

	@Test
	void standalonePreflightValidatesOnlyMpoArchiveContainers() throws IOException {
		assertNull(ProjectLoadWorkflow.preflightStandaloneFile(temporaryDirectory.resolve("missing.xml").toString()));
		assertInstanceOf(NoSuchFileException.class,
			ProjectLoadWorkflow.preflightStandaloneFile(temporaryDirectory.resolve("missing.mpo").toString()));

		Path corruptArchive = temporaryDirectory.resolve("corrupt.MPO");
		Files.writeString(corruptArchive, "not a zip archive");
		assertInstanceOf(ZipException.class, ProjectLoadWorkflow.preflightStandaloneFile(corruptArchive.toString()));

		Path validArchive = temporaryDirectory.resolve("valid.mpo");
		try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(validArchive))) {
			output.putNextEntry(new ZipEntry("project.xml"));
			output.write("<project/>".getBytes(java.nio.charset.StandardCharsets.UTF_8));
			output.closeEntry();
		}
		assertNull(ProjectLoadWorkflow.preflightStandaloneFile(validArchive.toString()));
	}
}
