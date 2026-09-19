/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.server.data;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

class MSPDISerializerAtomicSaveTest {
	@TempDir
	Path temporaryDirectory;

	@Test
	void successfulSaveUsesSiblingTempAndLeavesNoTemporaryFile() throws Exception {
		MSPDISerializer serializer = writingSerializer("new-content");
		Path target = temporaryDirectory.resolve("plan.xml");
		Files.writeString(target, "old-content", StandardCharsets.UTF_8);

		assertTrue(serializer.saveProject(testProject(), target.toString()));
		assertArrayEquals("new-content".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(target));
		assertNoTemporarySibling(target);
	}

	@Test
	void failedSavePreservesOriginalAndCleansTemporaryFile() throws Exception {
		MSPDISerializer serializer = new MSPDISerializer() {
			@Override
			public boolean saveProject(Project project, OutputStream output) {
				write(output, "partial");
				return false;
			}
		};
		Path target = temporaryDirectory.resolve("plan.xml");
		byte[] original = "original-content".getBytes(StandardCharsets.UTF_8);
		Files.write(target, original);

		assertFalse(serializer.saveProject(testProject(), target.toString()));
		assertArrayEquals(original, Files.readAllBytes(target));
		assertNoTemporarySibling(target);
	}

	private static MSPDISerializer writingSerializer(String content) {
		return new MSPDISerializer() {
			@Override
			public boolean saveProject(Project project, OutputStream output) {
				write(output, content);
				return true;
			}
		};
	}

	private static void write(OutputStream output, String content) {
		try {
			output.write(content.getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}

	private static Project testProject() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		return Project.createProject(ResourcePool.createRourcePool("mspdi-atomic-save", undo), undo);
	}

	private static void assertNoTemporarySibling(Path target) throws IOException {
		try (var siblings = Files.list(target.getParent())) {
			assertTrue(siblings.noneMatch(path -> path.getFileName().toString().startsWith(target.getFileName() + ".")
					&& path.getFileName().toString().endsWith(".tmp")));
		}
	}
}
