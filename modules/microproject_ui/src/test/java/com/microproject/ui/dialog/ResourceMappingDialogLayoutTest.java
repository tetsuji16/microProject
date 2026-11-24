/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.dialog;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.Locale;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;

import org.junit.jupiter.api.Test;

import com.microproject.persistence.ResourceMappingForm;
import com.microproject.i18n.Messages;
import com.microproject.ui.testsupport.HeadlessDialogPanels;
import com.microproject.ui.testsupport.HeadlessLayoutAssertions;

class ResourceMappingDialogLayoutTest {
	@Test
	void bodyKeepsLocalizedTextControlsAtPreferredHeight() throws Exception {
		Locale original = Locale.getDefault();
		try {
			for (Locale locale : new Locale[] { Locale.ENGLISH, Locale.JAPANESE }) {
				Locale.setDefault(locale);
				Messages.reset();
				ResourceMappingDialog dialog = HeadlessDialogPanels.allocate(ResourceMappingDialog.class);
				ResourceMappingForm form = new ResourceMappingForm() {
					@Override public boolean execute() { return true; }
				};
				form.setImportedResources(List.of());
				form.setResources(new ArrayList<>());
				dialog.setForm(form);
				JComponent panel = dialog.createContentPanel();
				panel.setSize(panel.getPreferredSize());
				assertDoesNotThrow(() -> HeadlessLayoutAssertions.assertTextControlsAtPreferredHeight(panel,
					"Resource Mapping dialog locale=" + locale));
			}
		} finally {
			Locale.setDefault(original);
			Messages.reset();
		}
	}
}
