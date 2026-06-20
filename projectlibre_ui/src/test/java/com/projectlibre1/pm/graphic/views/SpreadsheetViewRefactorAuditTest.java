package com.projectlibre1.pm.graphic.views;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class SpreadsheetViewRefactorAuditTest {
	@Test
	void projectAndResourceViewsUseSharedFieldLookupSupport() throws Exception {
		String projectSource = source("projectlibre_ui/src/com/projectlibre1/pm/graphic/views/ProjectView.java");
		String resourceSource = source("projectlibre_ui/src/com/projectlibre1/pm/graphic/views/ResourceView.java");

		assertTrue(projectSource.contains("SpreadsheetViewSupport.getProjectFields()"));
		assertTrue(projectSource.contains("SpreadsheetViewSupport.cleanup(spreadSheet)"));
		assertFalse(projectSource.contains("TODO don't hardcode"));

		assertTrue(resourceSource.contains("SpreadsheetViewSupport.getResourceFields()"));
		assertTrue(resourceSource.contains("SpreadsheetViewSupport.cleanup(spreadSheet)"));
		assertFalse(resourceSource.contains("TODO don't hardcode"));
	}

	private static String source(String relativePath) throws IOException {
		Path path = Path.of(relativePath);
		if (!Files.exists(path)) {
			path = Path.of("..").resolve(relativePath).normalize();
		}
		return Files.readString(path, StandardCharsets.UTF_8);
	}
}
