package com.projectlibre1.dialog;

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
		String source = source("projectlibre_ui/src/com/projectlibre1/dialog/AbstractDialog.java");

		assertTrue(source.contains("KeyEvent.VK_ESCAPE"));
		assertTrue(source.contains("rootPane.registerKeyboardAction(escapeListener"));
	}

	@Test
	void graphicManagerRoutesChoosersThroughPopupDialogSupport() throws Exception {
		String source = source("projectlibre_ui/src/com/projectlibre1/pm/graphic/frames/GraphicManager.java");

		assertTrue(source.contains("PopupDialogSupport.showOptionDialog("));
		assertTrue(source.contains("PopupDialogSupport.showConfirmDialog("));
		assertFalse(source.contains("JOptionPane.showOptionDialog("));
		assertFalse(source.contains("JOptionPane.showConfirmDialog("));
	}

	@Test
	void startupAndTipDialogsHaveEscapeCoverageHooks() throws Exception {
		String mainSource = source("projectlibre_ui/src/com/projectlibre1/pm/graphic/gantt/Main.java");
		String tipSource = source("projectlibre_ui/src/com/projectlibre1/dialog/TipOfTheDay.java");

		assertTrue(mainSource.contains("PopupDialogSupport.showMessageDialog("));
		assertFalse(mainSource.contains("JOptionPane.showMessageDialog("));
		assertTrue(tipSource.contains("PopupDialogSupport.bindEscapeToDispose"));
		assertTrue(tipSource.contains("AWTEvent.WINDOW_EVENT_MASK"));
	}

	private static String source(String relativePath) throws IOException {
		Path path = Path.of(relativePath);
		if (!Files.exists(path)) {
			path = Path.of("..").resolve(relativePath).normalize();
		}
		return Files.readString(path, StandardCharsets.UTF_8);
	}
}
