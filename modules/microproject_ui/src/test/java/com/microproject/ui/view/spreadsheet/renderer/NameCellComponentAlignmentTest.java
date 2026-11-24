/*
 * Copyright (c) 2026 microProject
 * Licensed under the MIT License.
 */
package com.microproject.ui.view.spreadsheet.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

class NameCellComponentAlignmentTest {
	@Test
	void nameLabelFillsTheWidthLeftAfterIndentAndIcon() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			JLabel label = new JLabel("Short");
			NameCellComponent cell = new NameCellComponent(label);
			cell.init();
			cell.setText("Short");
			cell.setSize(new Dimension(400, 20));
			cell.doLayout();

			assertTrue(label.getWidth() > label.getPreferredSize().width + 100,
				"the label must grow with the column so its alignment is visible, width=" + label.getWidth());
			assertEquals(400 - cell.getInsets().right, label.getX() + label.getWidth());

			label.setHorizontalAlignment(SwingConstants.RIGHT);
			assertEquals(SwingConstants.RIGHT, label.getHorizontalAlignment());
		});
	}
}
