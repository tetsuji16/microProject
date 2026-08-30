/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.application.RecentProjectStore;
import com.microproject.menu.MenuActionMapSupport;
import com.microproject.menu.MenuManager;

/** Visible regression coverage for Welcome-dialog recent project rows. */
class WelcomeDialogGuiAcceptanceTest {
	private WelcomeDialog dialog;
	private Path projectFile;

	@AfterEach
	void closeDialogAndRemoveRecentEntry() throws Exception {
		if (dialog != null) {
			SwingUtilities.invokeAndWait(() -> {
				dialog.dispose();
				dialog = null;
			});
		}
		if (projectFile != null) {
			new RecentProjectStore().remove(projectFile);
			Files.deleteIfExists(projectFile);
		}
	}

	@Test
	void welcomeDialogVisiblyShowsExistingRecentProjectAndOmitsDeletedEntries() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for GUI acceptance coverage.");
		projectFile = Files.createTempFile("micrproject-welcome-recent-", ".mpo");
		RecentProjectStore store = new RecentProjectStore();
		store.recordOpened(projectFile.toString());

		SwingUtilities.invokeAndWait(() -> {
			dialog = WelcomeDialog.getRecentProjectsInstance(null,
				MenuManager.getInstance(MenuActionMapSupport.noopActionMap()));
			dialog.setModal(false);
			dialog.pack();
			dialog.setLocationByPlatform(true);
			dialog.setVisible(true);
		});

		Robot robot = new Robot();
		robot.setAutoDelay(30);
		SwingUtilities.invokeAndWait(() -> {
			int entryIndex = findEntryIndex();
			assertTrue(entryIndex >= 0, "the visible welcome list must contain the recorded project");
			JLabel row = (JLabel) dialog.recentProjects.getCellRenderer().getListCellRendererComponent(
				dialog.recentProjects, dialog.recentProjects.getModel().getElementAt(entryIndex), entryIndex, true, true);
			assertTrue(row.getText().contains(projectFile.getFileName().toString()),
				"the visible welcome row must retain the recent project name");
		});
		capture(robot);

		Files.delete(projectFile);
		SwingUtilities.invokeAndWait(() -> {
			dialog.dispose();
			dialog = WelcomeDialog.getRecentProjectsInstance(null,
				MenuManager.getInstance(MenuActionMapSupport.noopActionMap()));
			dialog.setModal(false);
			dialog.pack();
			assertEquals(-1, findEntryIndex(),
				"deleted project paths must not remain in the visible recent-project list");
		});
	}

	private int findEntryIndex() {
		for (int index = 0; index < dialog.recentProjects.getModel().getSize(); index++) {
			if (projectFile.toAbsolutePath().equals(dialog.recentProjects.getModel().getElementAt(index).path()))
				return index;
		}
		return -1;
	}

	private void capture(Robot robot) throws Exception {
		Rectangle[] bounds = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> bounds[0] = new Rectangle(dialog.getRootPane().getLocationOnScreen(), dialog.getRootPane().getSize()));
		Path directory = Path.of(System.getProperty("micrproject.gui.artifacts.dir", "build/guiTest-artifacts"));
		Files.createDirectories(directory);
		javax.imageio.ImageIO.write(robot.createScreenCapture(bounds[0]), "png",
			directory.resolve("welcome-recent-project.png").toFile());
	}
}
