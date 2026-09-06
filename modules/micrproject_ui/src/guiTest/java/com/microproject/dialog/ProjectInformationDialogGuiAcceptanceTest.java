/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.InputEvent;

import javax.swing.AbstractButton;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.undo.DataFactoryUndoController;

/** Physical layout regression for Project > Information. */
class ProjectInformationDialogGuiAcceptanceTest {
	private ProjectInformationDialog dialog;

	@AfterEach
	void closeDialog() throws Exception {
		if (dialog != null) SwingUtilities.invokeAndWait(dialog::dispose);
	}

	@Test
	void projectInformationShowsAllTabsAndButtonsAfterResize() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for layout acceptance coverage.");
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("project-information-gui", undo);
		pool.setLocal(true);
		Project project = Project.createProject(pool, undo);
		project.setName("Project information GUI");
		dialog = ProjectInformationDialog.getInstance(null, project);
		SwingUtilities.invokeLater(() -> { dialog.pack(); dialog.setVisible(true); });
		GuiAcceptanceSupport.await(() -> dialog.isShowing(), "Project information dialog did not open");
		assertTrue(findTabs(dialog) != null && findTabs(dialog).getTabCount() == 3,
			"Project information must show General, Statistics, and Notes tabs");
		assertTrue(findButton(dialog, "Close") != null || findButton(dialog, "閉じる") != null,
			"Project information must expose a visible close button");

		Robot robot = new Robot();
		robot.setAutoDelay(30);
		SwingUtilities.invokeAndWait(() -> dialog.setSize(dialog.getWidth() + 180, dialog.getHeight() + 80));
		robot.delay(250);
		assertTrue(findTabs(dialog).isShowing() && findTabs(dialog).getWidth() > 0 && findTabs(dialog).getHeight() > 0,
			"Project information content must remain visible after resize");
	}

	private static JTabbedPane findTabs(java.awt.Container root) {
		for (Component child : root.getComponents()) {
			if (child instanceof JTabbedPane tabs) return tabs;
			if (child instanceof java.awt.Container nested) {
				JTabbedPane tabs = findTabs(nested);
				if (tabs != null) return tabs;
			}
		}
		return null;
	}

	private static AbstractButton findButton(java.awt.Container root, String text) {
		for (Component child : root.getComponents()) {
			if (child instanceof AbstractButton button && text.equals(button.getText()) && button.isShowing()) return button;
			if (child instanceof java.awt.Container nested) {
				AbstractButton button = findButton(nested, text);
				if (button != null) return button;
			}
		}
		return null;
	}
}
