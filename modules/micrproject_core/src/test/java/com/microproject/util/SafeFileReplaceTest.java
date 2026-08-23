/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Regression coverage for the data-safe file replace used by all save paths (issue #354). */
class SafeFileReplaceTest {

	@TempDir
	File dir;

	private File write(File f, String content) throws Exception {
		Files.writeString(f.toPath(), content, StandardCharsets.UTF_8);
		return f;
	}

	private String read(File f) throws Exception {
		return Files.readString(f.toPath(), StandardCharsets.UTF_8);
	}

	@Test
	void replaceMovesTempOntoExistingTargetAndConsumesTemp() throws Exception {
		File target = write(new File(dir, "project.xml"), "ORIGINAL");
		File temp = write(new File(dir, "project_tmp0.xml"), "UPDATED");

		assertTrue(SafeFileReplace.replace(temp, target));
		assertEquals("UPDATED", read(target));
		assertFalse(temp.exists(), "temp file must be consumed by the move");
	}

	@Test
	void replaceCreatesTargetWhenAbsent() throws Exception {
		File target = new File(dir, "new.xml");
		File temp = write(new File(dir, "new_tmp0.xml"), "FRESH");

		assertTrue(SafeFileReplace.replace(temp, target));
		assertEquals("FRESH", read(target));
		assertFalse(temp.exists());
	}

	@Test
	void replacePreservesOriginalWhenMoveFails() throws Exception {
		// A non-existent temp cannot be moved; the helper must report failure
		// and leave the existing target untouched (no silent data loss).
		File target = write(new File(dir, "kept.xml"), "ORIGINAL");
		File ghost = new File(dir, "ghost_tmp0.xml"); // does not exist

		assertFalse(SafeFileReplace.replace(ghost, target));
		assertTrue(target.exists());
		assertEquals("ORIGINAL", read(target));
	}

	@Test
	void replaceSameFileIsNoOp() throws Exception {
		File f = write(new File(dir, "same.xml"), "DATA");
		assertTrue(SafeFileReplace.replace(f, f));
		assertEquals("DATA", read(f));
	}
}
