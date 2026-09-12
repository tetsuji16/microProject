/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.exchange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MpoArchiveBudgetValidatorTest {
	@TempDir Path temporaryDirectory;

	@Test
	void acceptsArchiveWithinAllBudgetsAndReportsMeasurements() throws Exception {
		Path archive = archive("content/project.xml", "abc", "manifest.xml", "ok");
		MpoArchiveBudgetValidator.Result result = MpoArchiveBudgetValidator.validate(archive,
				new MpoArchiveBudgetValidator.Budget(20, 3, 2));
		assertTrue(result.valid());
		assertEquals(5, result.decompressedBytes());
		assertEquals(2, result.entries());
		assertEquals(2, result.observedDepth());
	}

	@Test
	void rejectsDecompressedSizeEntryCountAndTraversalDepth() throws Exception {
		Path archive = archive("payload", "123456789");
		assertFalse(MpoArchiveBudgetValidator.validate(archive, new MpoArchiveBudgetValidator.Budget(8, 5, 2)).valid());
		assertEquals("entry-count-limit", MpoArchiveBudgetValidator.validate(archiveWithEntries(3),
				new MpoArchiveBudgetValidator.Budget(100, 2, 3)).reason());
		Path traversal = archive("../escape", "x");
		assertEquals("invalid-entry-path", MpoArchiveBudgetValidator.validate(traversal,
				new MpoArchiveBudgetValidator.Budget(100, 5, 5)).reason());
		Path deep = archive("a/b/c/d", "x");
		assertEquals("path-depth-limit", MpoArchiveBudgetValidator.validate(deep,
				new MpoArchiveBudgetValidator.Budget(100, 5, 2)).reason());
	}

	private Path archive(String... entries) throws Exception {
		Path archive = Files.createTempFile(temporaryDirectory, "budget-", ".mpo");
		try (OutputStream output = Files.newOutputStream(archive); ZipOutputStream zip = new ZipOutputStream(output)) {
			for (int index = 0; index < entries.length; index += 2) {
				zip.putNextEntry(new ZipEntry(entries[index]));
				zip.write(entries[index + 1].getBytes(java.nio.charset.StandardCharsets.UTF_8));
				zip.closeEntry();
			}
		}
		return archive;
	}

	private Path archiveWithEntries(int count) throws Exception {
		Path archive = Files.createTempFile(temporaryDirectory, "budget-", ".mpo");
		try (OutputStream output = Files.newOutputStream(archive); ZipOutputStream zip = new ZipOutputStream(output)) {
			for (int index = 0; index < count; index++) {
				zip.putNextEntry(new ZipEntry("entry-" + index));
				zip.write('x');
				zip.closeEntry();
			}
		}
		return archive;
	}
}
