/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.spreadsheet;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.BorderLayout;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.graphic.configuration.SpreadSheetCategories;
import com.microproject.grouping.core.NodeFactory;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.DefaultSubProj;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.SubProj;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.undo.DataFactoryUndoController;

/** GUI-MSP-RECOVERY-01: a missing child remains repairable through the visible popup menu. */
class LinkedSubprojectRecoveryGuiAcceptanceTest {
	private JFrame frame;
	private SpreadSheet sheet;

	@AfterEach
	void closeWindow() throws Exception {
		if (frame != null)
			SwingUtilities.invokeAndWait(() -> frame.dispose());
	}

	@Test
	void robotShowsRecoveryActionsForMissingLinkedProject() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		DefaultSubProj reference = createReference("Missing child schedule", "C:/plans/missing-child.mpo", SubProj.LoadStatus.MISSING);
		showRecoveryActions(reference, "missing-linked-child", "linked-subproject-recovery-popup.png");
	}

	@Test
	void robotShowsRecoveryActionsForInvalidLinkedProject() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		DefaultSubProj reference = createReference("Invalid child schedule", "C:/plans/invalid-child.mpo", SubProj.LoadStatus.INVALID);
		showRecoveryActions(reference, "invalid-linked-child", "invalid-linked-subproject-recovery-popup.png");
	}

	private void showRecoveryActions(DefaultSubProj reference, String cacheName, String artifactName) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			Project master = reference.getProject();
			NodeModelCache cache = NodeModelCacheFactory.getInstance().createFilteredCache(
				NodeModelCacheFactory.createTaskNodeModelCache(master, master.getTaskModel()), cacheName, null);
			sheet = new SpreadSheet();
			sheet.setSpreadSheetCategory(SpreadSheetCategories.taskSpreadsheetCategory);
			SpreadSheetUtils.setFieldsAndContext(sheet, cache, SpreadSheetCategories.taskSpreadsheetCategory,
				"Spreadsheet.Task.entry", true);
			cache.update();
			frame = new JFrame("microProject — Linked-project recovery GUI acceptance");
			frame.add(new JScrollPane(sheet), BorderLayout.CENTER);
			frame.setSize(760, 320);
			frame.setLocationByPlatform(true);
			frame.setAlwaysOnTop(true);
			frame.setVisible(true);
		});
		GuiAcceptanceSupport.await(() -> frame.isShowing() && sheet.isShowing(), "missing linked-project spreadsheet did not become visible");
		int row = findRow(reference);
		Rectangle cell = cellBounds(row);
		Robot robot = new Robot();
		robot.setAutoDelay(40);
		// Deliver the platform-independent popup gesture through the same public
		// table route used by both the cell and row-header mouse handlers.  Robot
		// still captures the resulting native Swing popup; synthesizing the event
		// avoids an OS-specific press-vs-release popup-trigger distinction.
		SwingUtilities.invokeAndWait(() -> sheet.showPopupForCell(row, 0, sheet,
			new MouseEvent(sheet, MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(),
				0, cell.x - sheet.getLocationOnScreen().x + Math.max(5, cell.width / 2),
				cell.y - sheet.getLocationOnScreen().y + Math.max(5, cell.height / 2),
				1, true, MouseEvent.BUTTON3)));
		SpreadSheetPopupMenu popup = sheet.getPopup();
		GuiAcceptanceSupport.await(() -> popup.isShowing(), "linked-project recovery popup did not open");
		assertTrue(menuItemIsVisible(popup, "openLinkedProject"));
		assertTrue(menuItemIsVisible(popup, "refreshLinkedProject"));
		assertTrue(menuItemIsVisible(popup, "locateLinkedProject"));
		assertTrue(menuItemIsVisible(popup, "removeLinkedProject"));
		capture(robot, popup, artifactName);
	}

	private DefaultSubProj createReference(String name, String fileName, SubProj.LoadStatus status) {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project master = Project.createProject(ResourcePool.createRourcePool("linked-recovery-master", undo), undo);
		master.initialize(false, false);
		master.setMaster(true);
		DefaultSubProj reference = new DefaultSubProj(master, 99101L);
		reference.setName(name);
		reference.setSubprojectFile(fileName);
		reference.setLoadStatus(status);
		master.connectTask(reference);
		master.addToDefaultOutline(null, NodeFactory.getInstance().createNode(reference));
		return reference;
	}

	private int findRow(DefaultSubProj reference) {
		for (int row = 0; row < sheet.getRowCount(); row++)
			if (sheet.getTaskAtRow(row) == reference)
				return row;
		throw new AssertionError("missing linked-project row was not visible");
	}

	private Rectangle cellBounds(int row) throws Exception {
		Rectangle[] bounds = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> {
			Rectangle cell = sheet.getCellRect(row, 0, true);
			java.awt.Point location = sheet.getLocationOnScreen();
			bounds[0] = new Rectangle(location.x + cell.x, location.y + cell.y, cell.width, cell.height);
		});
		return bounds[0];
	}

	private static boolean menuItemIsVisible(SpreadSheetPopupMenu popup, String name) {
		for (java.awt.Component component : popup.getComponents())
			if (component instanceof javax.swing.JMenuItem item && name.equals(item.getName()))
				return item.isVisible() && item.isEnabled();
		return false;
	}

	private void capture(Robot robot, SpreadSheetPopupMenu popup, String artifactName) throws Exception {
		Rectangle[] bounds = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> {
			java.awt.Point location = frame.getLocationOnScreen();
			Rectangle frameBounds = new Rectangle(location.x, location.y, frame.getWidth(), frame.getHeight());
			java.awt.Point popupLocation = popup.getLocationOnScreen();
			Rectangle popupBounds = new Rectangle(popupLocation.x, popupLocation.y, popup.getWidth(), popup.getHeight());
			bounds[0] = frameBounds.union(popupBounds);
		});
		BufferedImage image = robot.createScreenCapture(bounds[0]);
		Path artifact = Path.of(System.getProperty("micrproject.gui.artifacts.dir", "build/guiTest-artifacts"),
			artifactName);
		Files.createDirectories(artifact.getParent());
		ImageIO.write(image, "png", artifact.toFile());
	}
}
