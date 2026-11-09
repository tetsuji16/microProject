/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.Locale;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.strings.Messages;
import com.microproject.testsupport.HeadlessDialogPanels;
import com.microproject.testsupport.HeadlessLayoutAssertions;

class BaselineDialogLayoutTest {
	@Test
	void baselineBodyKeepsLocalizedTextControlsAtPreferredHeight() throws Exception {
		Locale original = Locale.getDefault();
		try {
			for (Locale locale : new Locale[] { Locale.ENGLISH, Locale.JAPANESE }) {
				Locale.setDefault(locale);
				Messages.reset();
				BaselineDialog dialog = HeadlessDialogPanels.allocate(BaselineDialog.class);
				JComponent panel = dialog.createContentPanel();
				panel.setSize(panel.getPreferredSize());
				assertDoesNotThrow(() -> HeadlessLayoutAssertions.assertTextControlsAtPreferredHeight(panel,
					"Baseline dialog locale=" + locale));
			}
		} finally {
			Locale.setDefault(original);
			Messages.reset();
			SwingUtilities.invokeAndWait(() -> { });
		}
	}
}
