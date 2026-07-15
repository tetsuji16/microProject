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
		String projectSource = source("modules/projectlibre_ui/src/main/java/com/projectlibre1/pm/graphic/views/ProjectView.java");
		String resourceSource = source("modules/projectlibre_ui/src/main/java/com/projectlibre1/pm/graphic/views/ResourceView.java");

		assertTrue(projectSource.contains("SpreadsheetViewSupport.getProjectFields()"));
		assertTrue(projectSource.contains("SpreadsheetViewSupport.cleanup(spreadSheet)"));
		assertFalse(projectSource.contains("TODO don't hardcode"));

		assertTrue(resourceSource.contains("SpreadsheetViewSupport.getResourceFields()"));
		assertTrue(resourceSource.contains("SpreadsheetViewSupport.cleanup(spreadSheet)"));
		assertFalse(resourceSource.contains("TODO don't hardcode"));
	}

	@Test
	void ganttViewUsesSharedTaskFieldLookupAndCleanupSupport() throws Exception {
		String ganttSource = source("modules/projectlibre_ui/src/main/java/com/projectlibre1/pm/graphic/views/GanttView.java");

		assertTrue(ganttSource.contains("SpreadsheetViewSupport.resolveTaskFields(project.getFieldArray())"));
		assertTrue(ganttSource.contains("SpreadsheetViewSupport.getTaskFields(name)"));
		assertTrue(ganttSource.contains("SpreadsheetViewSupport.cleanup(spreadSheet)"));
		assertFalse(ganttSource.contains("Dictionary.get(spreadsheetCategory"));
		assertFalse(ganttSource.contains("private SpreadSheetFieldArray resolveFieldArray()"));
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
