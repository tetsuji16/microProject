/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.Locale;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;

import org.junit.jupiter.api.Test;

import com.microproject.strings.Messages;
import com.microproject.testsupport.HeadlessDialogPanels;
import com.microproject.testsupport.HeadlessLayoutAssertions;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

class ProjectInformationLayoutTest {
	@Test
	void allTabsKeepLocalizedTextControlsAtPreferredHeight() throws Exception {
		Locale original = Locale.getDefault();
		try {
			for (Locale locale : new Locale[] { Locale.ENGLISH, Locale.JAPANESE }) {
				Locale.setDefault(locale);
				Messages.reset();
				ProjectInformationDialog dialog = HeadlessDialogPanels.allocate(ProjectInformationDialog.class);
				HeadlessDialogPanels.set(dialog, "maps", new ArrayList<>());
				DataFactoryUndoController undo = new DataFactoryUndoController();
				Project project = Project.createProject(ResourcePool.createRourcePool("headless-project-info", undo), undo);
				HeadlessDialogPanels.set(dialog, "object", project);
				HeadlessDialogPanels.set(dialog, "objectClass", Project.class);
				JComponent panel = dialog.createContentPanel();
				panel.setSize(panel.getPreferredSize());
				JTabbedPane tabs = findTabs(panel);
				assertNotNull(tabs, "Project Information must expose its tab body");
				for (int index = 0; index < tabs.getTabCount(); index++) {
					JComponent tab = (JComponent) tabs.getComponentAt(index);
					tab.setSize(tab.getPreferredSize());
					HeadlessLayoutAssertions.assertTextControlsAtPreferredHeight(tab,
						"Project Information " + tabs.getTitleAt(index) + " locale=" + locale);
				}
			}
		} finally {
			Locale.setDefault(original);
			Messages.reset();
		}
	}

	private static JTabbedPane findTabs(java.awt.Container root) {
		for (java.awt.Component child : root.getComponents()) {
			if (child instanceof JTabbedPane tabs)
				return tabs;
			if (child instanceof java.awt.Container nested) {
				JTabbedPane tabs = findTabs(nested);
				if (tabs != null)
					return tabs;
			}
		}
		return null;
	}
}
