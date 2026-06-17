package com.projectlibre1.util;

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
		String source = source("projectlibre_core/src/com/projectlibre1/util/Alert.java");

		assertTrue(source.contains("PopupDialogSupport.showMessageDialog"));
		assertTrue(source.contains("PopupDialogSupport.showConfirmDialog"));
		assertTrue(source.contains("PopupDialogSupport.bindEscapeToOptionPane"));
		assertFalse(source.contains("JOptionPane.showMessageDialog("));
		assertFalse(source.contains("JOptionPane.showConfirmDialog("));
	}

	@Test
	void collaborationSessionUsesPopupDialogSupportForConflictResolution() throws Exception {
		String source = source("projectlibre_core/src/com/projectlibre1/collaboration/CollaborationSession.java");

		assertTrue(source.contains("PopupDialogSupport.showOptionDialog("));
		assertFalse(source.contains("JOptionPane.showOptionDialog("));
	}

	private static String source(String relativePath) throws IOException {
		Path path = Path.of(relativePath);
		if (!Files.exists(path)) {
			path = Path.of("..").resolve(relativePath).normalize();
		}
		return Files.readString(path, StandardCharsets.UTF_8);
	}
}
