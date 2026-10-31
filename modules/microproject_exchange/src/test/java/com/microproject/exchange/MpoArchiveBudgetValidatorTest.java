/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.exchange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.OutputStream;
import java.util.List;
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

	/** The ZipFile and ZipInputStream routes share one accounting object; every limit must trip on both. */
	@Test
	void everyLimitRejectsOnBothFileAndStreamRoutes() throws Exception {
		assertBothRoutes("decompressed-size-limit", archive("payload", "123456789"), new MpoArchiveBudgetValidator.Budget(8, 5, 2));
		assertBothRoutes("decompressed-size-limit", archive("a", "1234", "b", "12345"), new MpoArchiveBudgetValidator.Budget(8, 5, 2));
		assertBothRoutes("entry-count-limit", archiveWithEntries(3), new MpoArchiveBudgetValidator.Budget(100, 2, 3));
		assertBothRoutes("path-depth-limit", archive("a/b/c/d", "x"), new MpoArchiveBudgetValidator.Budget(100, 5, 2));
		assertBothRoutes("invalid-entry-path", archive("../escape", "x"), new MpoArchiveBudgetValidator.Budget(100, 5, 5));
		assertBothRoutes("invalid-entry-path", archive("/absolute", "x"), new MpoArchiveBudgetValidator.Budget(100, 5, 5));
	}

	@Test
	void limitsAreInclusiveOnBothRoutes() throws Exception {
		Path archive = archive("a", "1234", "b", "1234");
		MpoArchiveBudgetValidator.Budget exact = new MpoArchiveBudgetValidator.Budget(8, 2, 1);
		for (MpoArchiveBudgetValidator.Result result : List.of(MpoArchiveBudgetValidator.validate(archive, exact),
				MpoArchiveBudgetValidator.validate(Files.readAllBytes(archive), exact))) {
			assertTrue(result.valid(), result.reason());
			assertEquals(8, result.decompressedBytes());
			assertEquals(2, result.entries());
			assertEquals(1, result.observedDepth());
		}
	}

	private static void assertBothRoutes(String reason, Path archive, MpoArchiveBudgetValidator.Budget budget)
			throws Exception {
		MpoArchiveBudgetValidator.Result file = MpoArchiveBudgetValidator.validate(archive, budget);
		MpoArchiveBudgetValidator.Result stream = MpoArchiveBudgetValidator.validate(Files.readAllBytes(archive), budget);
		assertFalse(file.valid());
		assertFalse(stream.valid());
		assertEquals(reason, file.reason(), "file route");
		assertEquals(reason, stream.reason(), "stream route");
		assertEquals(file, stream, "both routes report identical measurements");
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
