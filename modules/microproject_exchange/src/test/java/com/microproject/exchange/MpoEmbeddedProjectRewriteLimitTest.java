/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.exchange;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Rewriting an extracted embedded project must not read unbounded entries into memory. */
class MpoEmbeddedProjectRewriteLimitTest {

	@Test
	void entryLargerThanTheEntryLimitIsRejected(@TempDir Path directory) throws Exception {
		Path archive = directory.resolve("child.mpo");
		try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
			zip.putNextEntry(new ZipEntry("payload/big.bin"));
			writeZeros(zip, 65L * 1024L * 1024L);
			zip.closeEntry();
		}

		IOException failure = assertThrows(IOException.class,
			() -> MpoEmbeddedProjects.rewriteExtractedProjectReferences(archive, Map.of()));

		assertTrue(failure.getMessage().contains("exceeds"), failure.getMessage());
	}

	@Test
	void tooManyEntriesAreRejected(@TempDir Path directory) throws Exception {
		Path archive = directory.resolve("many.mpo");
		try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
			for (int index = 0; index <= MpoArchiveBudgetValidator.DEFAULT_BUDGET.maxEntries(); index++) {
				zip.putNextEntry(new ZipEntry("payload/" + index + ".txt"));
				zip.closeEntry();
			}
		}

		IOException failure = assertThrows(IOException.class,
			() -> MpoEmbeddedProjects.rewriteExtractedProjectReferences(archive, Map.of()));

		assertTrue(failure.getMessage().contains("entries"), failure.getMessage());
	}

	private static void writeZeros(OutputStream out, long count) throws IOException {
		byte[] block = new byte[64 * 1024];
		for (long written = 0; written < count; written += block.length)
			out.write(block);
	}
}
