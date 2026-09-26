/*
 * MIT License
 * Copyright (c) 2026 microProject
 */
package com.microproject.dialog.util;

import static org.junit.jupiter.api.Assertions.assertSame;

import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.JTextField;

import org.junit.jupiter.api.Test;

class FieldVerifierTest {
	@Test
	void spinnerEditorResolvesToItsValueHoldingSpinner() {
		JSpinner spinner = new JSpinner();
		JTextField editor = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();

		assertSame(spinner, FieldVerifier.valueHoldingComponent(editor));
	}

	@Test
	void componentWithoutAParentRemainsTheValueHoldingComponent() {
		JComponent component = new JTextField();

		assertSame(component, FieldVerifier.valueHoldingComponent(component));
	}
}
