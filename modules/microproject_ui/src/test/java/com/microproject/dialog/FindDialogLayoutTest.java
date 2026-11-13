/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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
	@Test
	void findButtonsUseCurrentDesignWithoutLegacyIcons() throws Exception {
		Locale original = Locale.getDefault();
		try {
			Locale.setDefault(Locale.ENGLISH);
			Messages.reset();
			FindDialog dialog = HeadlessDialogPanels.allocate(FindDialog.class);
			dialog.search = new JTextField(30);
			dialog.combo = new JComboBox<>();
			dialog.caseSensitive = new JCheckBox(Messages.getString("FindDialog.MatchCase"));
			dialog.createContentPanel();
			javax.swing.JPanel buttons = dialog.createButtonPanel();
			assertNotNull(dialog.next, "Find Next button must exist");
			assertNull(dialog.next.getIcon(), "Find Next must not use a legacy GIF icon");
			assertNotNull(dialog.direction, "Search direction combo must exist");
			assertEquals(0, dialog.direction.getSelectedIndex(), "Default direction must be Down");
			for (java.awt.Component c : buttons.getComponents()) {
				if (c instanceof javax.swing.JButton b)
					assertNull(b.getIcon(), "Dialog buttons must not carry legacy icons");
			}
		} finally {
			Locale.setDefault(original);
			Messages.reset();
		}
	}

}
