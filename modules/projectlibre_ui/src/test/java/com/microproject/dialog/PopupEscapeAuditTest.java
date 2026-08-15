package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class PopupEscapeAuditTest {
	@Test
	void abstractDialogStillBindsEscapeAtTheBaseClass() throws Exception {
		String source = source("modules/projectlibre_ui/src/main/java/com/projectlibre1/dialog/AbstractDialog.java");

		assertTrue(source.contains("KeyEvent.VK_ESCAPE"));
		assertTrue(source.contains("rootPane.registerKeyboardAction(escapeListener"));
	}

	@Test
	void graphicManagerRoutesChoosersThroughPopupDialogSupport() throws Exception {
		String source = source("modules/projectlibre_ui/src/main/java/com/projectlibre1/pm/graphic/frames/GraphicManager.java");

		assertTrue(source.contains("PopupDialogSupport.showOptionDialog("));
		assertTrue(source.contains("PopupDialogSupport.showConfirmDialog("));
		assertFalse(source.contains("JOptionPane.showOptionDialog("));
		assertFalse(source.contains("JOptionPane.showConfirmDialog("));
	}

	@Test
	void startupAndTipDialogsHaveEscapeCoverageHooks() throws Exception {
		String mainSource = source("modules/projectlibre_ui/src/main/java/com/projectlibre1/pm/graphic/gantt/Main.java");

		assertTrue(mainSource.contains("PopupDialogSupport.showMessageDialog("));
		assertFalse(mainSource.contains("JOptionPane.showMessageDialog("));
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
