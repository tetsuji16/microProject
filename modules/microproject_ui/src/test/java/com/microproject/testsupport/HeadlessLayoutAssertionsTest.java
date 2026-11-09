/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.testsupport;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.junit.jupiter.api.Test;

class HeadlessLayoutAssertionsTest {
	@Test
	void laysOutNestedTextControlsWithoutCreatingAWindow() {
		JPanel root = new JPanel();
		JPanel nested = new JPanel();
		nested.add(new JLabel("Dialog label"));
		root.add(nested);

		assertDoesNotThrow(() -> HeadlessLayoutAssertions.assertTextControlsAtPreferredHeight(root, "headless fixture"));
	}

	@Test
	void rejectsTextControlShorterThanItsPreferredHeight() {
		JPanel root = new JPanel(null);
		JLabel label = new JLabel("Dialog label");
		label.setBounds(0, 0, 100, 1);
		root.setSize(120, 30);
		root.add(label);

		assertThrows(AssertionError.class,
			() -> HeadlessLayoutAssertions.assertTextControlsAtPreferredHeight(root, "short control fixture"));
	}
}
