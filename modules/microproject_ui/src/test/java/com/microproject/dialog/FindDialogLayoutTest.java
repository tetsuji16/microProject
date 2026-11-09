/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.Locale;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTextField;

import org.junit.jupiter.api.Test;

import com.microproject.strings.Messages;
import com.microproject.testsupport.HeadlessDialogPanels;
import com.microproject.testsupport.HeadlessLayoutAssertions;

class FindDialogLayoutTest {
	@Test
	void findBodyKeepsLocalizedTextControlsAtPreferredHeight() throws Exception {
		Locale original = Locale.getDefault();
		try {
			for (Locale locale : new Locale[] { Locale.ENGLISH, Locale.JAPANESE }) {
				Locale.setDefault(locale);
				Messages.reset();
				FindDialog dialog = HeadlessDialogPanels.allocate(FindDialog.class);
				dialog.search = new JTextField(30);
				dialog.combo = new JComboBox<>();
				dialog.caseSensitive = new JCheckBox(Messages.getString("FindDialog.MatchCase"));
				JComponent panel = dialog.createContentPanel();
				panel.setSize(panel.getPreferredSize());
				assertDoesNotThrow(() -> HeadlessLayoutAssertions.assertTextControlsAtPreferredHeight(panel,
					"Find dialog locale=" + locale));
			}
		} finally {
			Locale.setDefault(original);
			Messages.reset();
		}
	}
}
