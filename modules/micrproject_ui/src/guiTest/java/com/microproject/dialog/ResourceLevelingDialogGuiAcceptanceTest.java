/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.swing.AbstractButton;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.pm.assignment.AssignmentService;
import com.microproject.options.CalendarOption;
import com.microproject.pm.task.Project;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.task.NormalTask;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.testsupport.GuiAcceptanceSupport;

/** GUI acceptance coverage for the same-resource conflict preview workflow. */
class ResourceLevelingDialogGuiAcceptanceTest {
	@AfterEach
	void closeDialogs() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			for (Window window : Window.getWindows())
				if (window instanceof ResourceLevelingDialogBox) window.dispose();
		});
	}

	@Test
	void sameResourceConflictShowsPreviewChangesAfterRobotClick() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Project project = newConflictProject();
		SwingUtilities.invokeLater(() -> ResourceLevelingDialogBox.getInstance(null, project).setVisible(true));
		GuiAcceptanceSupport.await(() -> findDialog() != null, "Resource leveling dialog did not open");
		ResourceLevelingDialogBox dialog = findDialog();
		AbstractButton preview = findButton(dialog, UsabilityStrings.text("leveling.preview"));
		assertTrue(preview != null && preview.isShowing(), "Preview button must be visible");

		Rectangle bounds = new Rectangle();
		SwingUtilities.invokeAndWait(() -> {
			java.awt.Point location = preview.getLocationOnScreen();
			bounds.setBounds(location.x, location.y, preview.getWidth(), preview.getHeight());
		});
		Robot robot = new Robot();
		robot.setAutoDelay(40);
		robot.mouseMove(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
		robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);

		GuiAcceptanceSupport.await(() -> findTable(dialog) != null && findTable(dialog).getRowCount() > 0,
			"same-resource conflict preview must show at least one proposed change");
		capture(robot, dialog);
	}

	private static ResourceLevelingDialogBox findDialog() {
		for (Window window : Window.getWindows())
			if (window instanceof ResourceLevelingDialogBox dialog && dialog.isVisible()) return dialog;
		return null;
	}

	private static AbstractButton findButton(java.awt.Container container, String text) {
		for (Component child : container.getComponents()) {
			if (child instanceof AbstractButton button && text.equals(button.getText())) return button;
			if (child instanceof java.awt.Container nested) {
				AbstractButton button = findButton(nested, text);
				if (button != null) return button;
			}
		}
		return null;
	}

	private static JTable findTable(java.awt.Container container) {
		for (Component child : container.getComponents()) {
			if (child instanceof JTable table) return table;
			if (child instanceof java.awt.Container nested) {
				JTable table = findTable(nested);
				if (table != null) return table;
			}
		}
		return null;
	}

	private static Project newConflictProject() throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("resource-leveling-gui", undo);
		pool.setLocal(true);
		Project project = Project.createProject(pool, undo);
		project.setName("Resource leveling GUI acceptance");
		project.initialize(false, false);
		NormalTask first = new NormalTask(project);
		first.setName("Conflict A");
		project.connectTask(first);
		project.getSchedulingAlgorithm().addObject(first);
		first.getCurrentSchedule().setStart(project.getStart());
		first.setDuration(CalendarOption.getInstance().getMillisPerDay());
		NormalTask second = new NormalTask(project);
		second.setName("Conflict B");
		project.connectTask(second);
		project.getSchedulingAlgorithm().addObject(second);
		second.getCurrentSchedule().setStart(project.getStart());
		second.setDuration(CalendarOption.getInstance().getMillisPerDay());
		ResourceImpl resource = pool.newResourceInstance();
		resource.setName("Shared resource");
		AssignmentService.getInstance().newAssignment(first, resource, 1.0, 0L, ResourceLevelingDialogGuiAcceptanceTest.class);
		AssignmentService.getInstance().newAssignment(second, resource, 1.0, 0L, ResourceLevelingDialogGuiAcceptanceTest.class);
		project.recalculate();
		return project;
	}

	private static void capture(Robot robot, ResourceLevelingDialogBox dialog) throws Exception {
		Rectangle bounds = new Rectangle();
		SwingUtilities.invokeAndWait(() -> {
			dialog.toFront();
			dialog.requestFocus();
			java.awt.Point location = dialog.getRootPane().getLocationOnScreen();
			bounds.setBounds(location.x, location.y, dialog.getRootPane().getWidth(), dialog.getRootPane().getHeight());
		});
		Path directory = Path.of(System.getProperty("micrproject.gui.artifacts.dir", "build/guiTest-artifacts"));
		Files.createDirectories(directory);
		javax.imageio.ImageIO.write(robot.createScreenCapture(bounds), "png",
			directory.resolve("resource-leveling-conflict-preview.png").toFile());
	}
}
