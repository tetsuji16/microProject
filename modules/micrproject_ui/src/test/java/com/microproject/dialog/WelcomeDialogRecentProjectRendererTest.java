/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.application.RecentProjectStore;

class WelcomeDialogRecentProjectRendererTest {
	@Test void rendersTheRecentProjectNameInsteadOfAnEmptyCell() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			RecentProjectStore.Entry entry = new RecentProjectStore.Entry(Path.of("C:/projects/roadmap.pod"), 0L, false, true);
			DefaultListCellRenderer renderer = WelcomeDialog.recentProjectRenderer();
			JLabel label = (JLabel) renderer.getListCellRendererComponent(new JList<>(), entry, 0, true, true);

			assertTrue(label.getText().contains("roadmap.pod"));
			assertTrue(label.getText().contains("projects"));
		});
	}
}
