/*
 * MIT License
 * Copyright (c) 2026 microProject
 */
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

class AbstractDialogContentViewportTest {
	@Test
	void constrainedViewportKeepsContentAtPreferredSizeAndScrollable() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			JPanel content = new JPanel(null);
			content.setPreferredSize(new Dimension(500, 700));
			JCheckBox checkbox = new JCheckBox("Resource mapping");
			checkbox.setBounds(20, 650, 200, 30);
			content.add(checkbox);

			JScrollPane viewport = AbstractDialog.createContentViewport(content);
			viewport.setSize(300, 220);
			viewport.doLayout();
			viewport.getViewport().doLayout();

			assertTrue(content.getWidth() >= content.getPreferredSize().width);
			assertTrue(content.getHeight() >= content.getPreferredSize().height);
			Rectangle control = checkbox.getBounds();
			assertTrue(control.x >= 0 && control.y >= 0
				&& control.x + control.width <= content.getWidth()
				&& control.y + control.height <= content.getHeight());
			assertTrue(viewport.getVerticalScrollBar().getMaximum()
				> viewport.getVerticalScrollBar().getVisibleAmount(),
				"content beyond the available work area must remain reachable by scrolling");
		});
	}
}
