package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class FindDialogCleanupAuditTest {
	@Test
	void findDialogRemovesGlobalListenersOnDispose() throws Exception {
		String source = source("modules/micrproject_ui/src/main/java/com/projectlibre1/dialog/FindDialog.java");

		assertTrue(source.contains("public void dispose()"));
		assertTrue(source.contains("DocumentSelectedEvent.removeListener(this);"));
		assertTrue(source.contains("documentFrame.getProject().removeObjectListener(this);"));
		assertFalse(source.contains("private static FindDialog instance = null;"));
		assertFalse(source.contains("TODO set minimum size"));
		assertFalse(source.contains("TODO Auto-generated method stub"));
	}

	private static String source(String relativePath) throws IOException {
		for (Path current = Path.of("").toAbsolutePath(); current != null; current = current.getParent()) {
			Path candidate = current.resolve(relativePath).normalize();
			if (Files.exists(candidate)) {
				return Files.readString(candidate, StandardCharsets.UTF_8);
			}
		}
		throw new java.nio.file.NoSuchFileException(relativePath);
	}
}
