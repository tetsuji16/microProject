/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.ArrayList;
import java.util.Locale;

import javax.swing.JComponent;

import org.junit.jupiter.api.Test;

import com.microproject.strings.Messages;
import com.microproject.testsupport.HeadlessDialogPanels;
import com.microproject.testsupport.HeadlessLayoutAssertions;

class ResourceInformationLayoutTest {
	@Test
	void generalPanelKeepsLocalizedTextControlsAtPreferredHeight() throws Exception {
		Locale original = Locale.getDefault();
		try {
			for (Locale locale : new Locale[] { Locale.ENGLISH, Locale.JAPANESE }) {
				Locale.setDefault(locale);
				Messages.reset();
				ResourceInformationDialog dialog = HeadlessDialogPanels.allocate(ResourceInformationDialog.class);
				HeadlessDialogPanels.set(dialog, "maps", new ArrayList<>());
				JComponent panel = dialog.createGeneralPanel();
				panel.setSize(panel.getPreferredSize());
				assertDoesNotThrow(() -> HeadlessLayoutAssertions.assertTextControlsAtPreferredHeight(panel,
					"Resource Information General locale=" + locale));
			}
		} finally {
			Locale.setDefault(original);
			Messages.reset();
		}
	}
}
