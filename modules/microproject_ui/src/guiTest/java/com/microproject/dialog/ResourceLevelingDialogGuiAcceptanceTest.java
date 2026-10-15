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
import javax.swing.JComboBox;
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
	void proposedResourcesStayOutOfPreviewUntilRobotEnablesThem() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Project project = newConflictProject();
		SwingUtilities.invokeLater(() -> ResourceLevelingDialogBox.getInstance(null, project).setVisible(true));
		GuiAcceptanceSupport.await(() -> findDialog() != null, "Resource leveling dialog did not open");
		ResourceLevelingDialogBox dialog = findDialog();
		AbstractButton preview = findButton(dialog, UsabilityStrings.text("leveling.preview"));
		AbstractButton includeProposed = findButton(dialog, UsabilityStrings.text("leveling.includeProposed"));
		JComboBox<?> order = findComboBox(dialog, 0);
		JComboBox<?> sensitivity = findComboBox(dialog, 1);
		assertTrue(preview != null && preview.isShowing(), "Preview button must be visible");
		assertTrue(includeProposed != null && includeProposed.isShowing(), "Proposed-resource option must be visible");
		assertTrue(sensitivity != null && sensitivity.isShowing(), "sensitivity period must be visible");
		assertTrue(order != null && isBelow(sensitivity, order),
			"sensitivity must occupy a separate form row below leveling order");
		assertTrue(selectedIndex(sensitivity, 2), "day must be the document-derived product default");
		assertTrue(!isSelected(includeProposed), "Proposed resources must be excluded by default");
		assertTrue(tableRowCount(dialog) == 0, "default preview must leave proposed-resource conflicts untouched");

		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(40);
		click(robot, includeProposed);
		click(robot, preview);

		Rectangle bounds = new Rectangle();
		SwingUtilities.invokeAndWait(() -> {
			java.awt.Point location = preview.getLocationOnScreen();
			bounds.setBounds(location.x, location.y, preview.getWidth(), preview.getHeight());
		});
		GuiAcceptanceSupport.await(() -> findTable(dialog) != null && findTable(dialog).getRowCount() > 0,
			"same-resource conflict preview must show at least one proposed change");
		assertTrue(isSelected(includeProposed), "physical opt-in must stay selected while showing the plan");
		for (int index : new int[] {0, 1}) {
			GuiAcceptanceSupport.selectComboBoxItem(robot, sensitivity, index);
			click(robot, preview);
			GuiAcceptanceSupport.await(() -> tableRowCountUnchecked(dialog) > 0,
				"minute and hour sensitivity must show the same full-day conflict");
		}
		GuiAcceptanceSupport.selectComboBoxItem(robot, sensitivity, 3);
		click(robot, preview);
		GuiAcceptanceSupport.await(() -> tableRowCountUnchecked(dialog) == 0,
			"weekly sensitivity must accept two one-day assignments within weekly capacity");
		GuiAcceptanceSupport.selectComboBoxItem(robot, sensitivity, 2);
		click(robot, preview);
		GuiAcceptanceSupport.await(() -> tableRowCountUnchecked(dialog) > 0,
			"daily sensitivity must detect the full-day conflict");
		capture(robot, dialog);
	}

	private static void click(Robot robot, Component component) throws Exception {
		Rectangle bounds = new Rectangle();
		SwingUtilities.invokeAndWait(() -> {
			java.awt.Point location = component.getLocationOnScreen();
			bounds.setBounds(location.x, location.y, component.getWidth(), component.getHeight());
		});
		robot.mouseMove(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
		robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
	}

	private static boolean isSelected(AbstractButton button) throws Exception {
		boolean[] selected = new boolean[1];
		SwingUtilities.invokeAndWait(() -> selected[0] = button.isSelected());
		return selected[0];
	}

	private static boolean selectedIndex(JComboBox<?> combo, int index) throws Exception {
		boolean[] selected = new boolean[1];
		SwingUtilities.invokeAndWait(() -> selected[0] = combo.getSelectedIndex() == index);
		return selected[0];
	}

	private static boolean isBelow(Component component, Component other) throws Exception {
		boolean[] below = new boolean[1];
		SwingUtilities.invokeAndWait(() -> below[0] = component.getY() >= other.getY() + other.getHeight());
		return below[0];
	}

	private static int tableRowCount(ResourceLevelingDialogBox dialog) throws Exception {
		int[] rowCount = new int[1];
		SwingUtilities.invokeAndWait(() -> rowCount[0] = findTable(dialog).getRowCount());
		return rowCount[0];
	}

	private static int tableRowCountUnchecked(ResourceLevelingDialogBox dialog) {
		try { return tableRowCount(dialog); }
		catch (Exception exception) { throw new IllegalStateException(exception); }
	}

	private static JComboBox<?> findComboBox(java.awt.Container container, int index) {
		int[] visited = {0};
		return findComboBox(container, index, visited);
	}

	private static JComboBox<?> findComboBox(java.awt.Container container, int index, int[] visited) {
		for (Component child : container.getComponents()) {
			if (child instanceof JComboBox<?> combo && visited[0]++ == index) return combo;
			if (child instanceof java.awt.Container nested) {
				JComboBox<?> combo = findComboBox(nested, index, visited);
				if (combo != null) return combo;
			}
		}
		return null;
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
		resource.setBookingTypeKind(com.microproject.pm.resource.BookingType.Kind.PROPOSED);
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
		Path directory = Path.of(System.getProperty("microproject.gui.artifacts.dir", "build/guiTest-artifacts"));
		Files.createDirectories(directory);
		javax.imageio.ImageIO.write(robot.createScreenCapture(bounds), "png",
			directory.resolve("resource-leveling-conflict-preview.png").toFile());
	}
}
