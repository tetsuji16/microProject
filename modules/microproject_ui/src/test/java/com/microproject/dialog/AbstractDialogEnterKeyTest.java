package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.junit.jupiter.api.Test;

/** Windows/Office rule: Enter presses the focused push button, otherwise the dialog's default (OK). */
class AbstractDialogEnterKeyTest {
	@Test
	void enterTargetsOnlyAnEnabledFocusedPushButtonInsideTheDialog() {
		JPanel dialog = new JPanel();
		JButton cancel = new JButton("Cancel");
		JButton disabled = new JButton("Disabled");
		disabled.setEnabled(false);
		JCheckBox option = new JCheckBox("Option");
		JTextField text = new JTextField();
		dialog.add(cancel);
		dialog.add(disabled);
		dialog.add(option);
		dialog.add(text);

		assertSame(cancel, AbstractDialog.focusedButtonForEnter(cancel, dialog), "focused Cancel takes Enter");
		assertNull(AbstractDialog.focusedButtonForEnter(disabled, dialog), "a disabled button falls back to OK");
		assertNull(AbstractDialog.focusedButtonForEnter(option, dialog), "check boxes do not take Enter");
		assertNull(AbstractDialog.focusedButtonForEnter(text, dialog), "a single-line field falls back to OK");
		assertNull(AbstractDialog.focusedButtonForEnter(new JButton("Elsewhere"), dialog), "foreign focus falls back to OK");
		assertNull(AbstractDialog.focusedButtonForEnter(null, dialog));
	}
}
