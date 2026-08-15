package com.microproject.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class PopupDialogUsageAuditTest {
	@Test
	void alertRoutesAllJOptionPaneCallsThroughPopupDialogSupport() throws Exception {
		String source = source("modules/projectlibre_core/src/main/java/com/projectlibre1/util/Alert.java");

		assertTrue(source.contains("PopupDialogSupport.showMessageDialog"));
		assertTrue(source.contains("PopupDialogSupport.showConfirmDialog"));
		assertTrue(source.contains("PopupDialogSupport.bindEscapeToOptionPane"));
		assertFalse(source.contains("JOptionPane.showMessageDialog("));
		assertFalse(source.contains("JOptionPane.showConfirmDialog("));
	}

	@Test
	void collaborationSessionUsesPopupDialogSupportForConflictResolution() throws Exception {
		String source = source("modules/projectlibre_core/src/main/java/com/projectlibre1/collaboration/CollaborationSession.java");

		assertTrue(source.contains("PopupDialogSupport.showOptionDialog("));
		assertFalse(source.contains("JOptionPane.showOptionDialog("));
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
