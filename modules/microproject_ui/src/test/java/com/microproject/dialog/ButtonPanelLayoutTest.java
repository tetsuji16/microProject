/*
 * MIT License
 * Copyright (c) 2026 microProject
 */
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.List;

import javax.swing.JButton;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

class ButtonPanelLayoutTest {
	@Test
	void wrapsButtonsWithinConstrainedPanelBoundsWithoutOverlap() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			ButtonPanel panel = new ButtonPanel();
			List<JButton> buttons = List.of(
				new JButton("OK"), new JButton("Cancel"), new JButton("Help"), new JButton("Apply"));
			buttons.forEach(panel::addButton);
			int oneRowHeight = panel.getPreferredSize().height;
			panel.setSize(190, oneRowHeight);
			Dimension wrappedPreferredSize = panel.getPreferredSize();
			assertTrue(wrappedPreferredSize.height > oneRowHeight,
				"preferred height must grow when the constrained width wraps buttons");
			panel.setSize(190, wrappedPreferredSize.height);
			panel.doLayout();

			boolean multipleRows = false;
			for (int i = 0; i < buttons.size(); i++) {
				Rectangle bounds = buttons.get(i).getBounds();
				assertTrue(bounds.x >= 0 && bounds.y >= 0
					&& bounds.x + bounds.width <= panel.getWidth()
					&& bounds.y + bounds.height <= panel.getHeight(),
					"button must remain inside its panel: " + buttons.get(i).getText());
				for (int j = i + 1; j < buttons.size(); j++) {
					assertFalse(bounds.intersects(buttons.get(j).getBounds()),
						"buttons must not overlap");
					multipleRows |= bounds.y != buttons.get(j).getY();
				}
			}
			assertTrue(multipleRows, "constrained width must wrap buttons onto multiple rows");
		});
	}

	@Test
	void keepsButtonsOnOneRowAtPreferredWidth() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			ButtonPanel panel = new ButtonPanel();
			List<JButton> buttons = List.of(
				new JButton("OK"), new JButton("Cancel"), new JButton("Help"), new JButton("Apply"));
			buttons.forEach(panel::addButton);
			Dimension preferredSize = panel.getPreferredSize();
			panel.setSize(preferredSize);
			panel.doLayout();

			int y = buttons.get(0).getY();
			for (JButton button : buttons) {
				assertEquals(y, button.getY(), "preferred width should keep buttons on one row");
				assertTrue(button.getBounds().y + button.getHeight() <= panel.getHeight());
			}
		});
	}
}
