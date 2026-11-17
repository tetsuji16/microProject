/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractButton;
import javax.swing.JFormattedTextField;
import javax.swing.JSpinner;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.AssignmentService;
import com.microproject.pm.graphic.frames.DocumentFrame;
import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.pm.graphic.frames.MainRibbonFrame;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.testsupport.GuiJourney;
import com.microproject.testsupport.GuiJourney.InputMechanism;
import com.microproject.undo.DataFactoryUndoController;

/** Verifies the real selected-assignment ribbon route, edit, undo and cancellation. */
class AssignmentInformationGuiAcceptanceTest {
	private MainRibbonFrame frame;
	private GraphicManager manager;
	private Assignment assignment;

	@AfterEach
	void closeWindows() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			if (manager != null) manager.cleanUp();
			if (frame != null) frame.dispose();
		});
	}

	@Test
	@GuiJourney(InputMechanism.RIBBON_CLICK)
	void selectedAssignmentInformationEditsAndCancelsThroughPhysicalRibbonRoute() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Robot acceptance requires a desktop session.");
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("assignment-information-gui", undo);
		Project project = Project.createProject(pool, undo);
		project.initialize(false, false);
		NormalTask task = project.createScriptedTask();
		task.setName("Selected assignment task");
		Resource resource = pool.createScriptedResource();
		resource.setName("Selected assignment resource");
		assignment = AssignmentService.getInstance().newAssignment(task, resource, 1.0, 0L, this);
		assignment.setNotes("Existing assignment note");
		project.getUndoController().clear();

		SwingUtilities.invokeAndWait(() -> {
			frame = new MainRibbonFrame("Assignment Information acceptance", null);
			manager = new GraphicManager(frame);
			frame.setGraphicManager(manager);
			manager.initView();
			manager.addProjectFrame(project);
			frame.setSize(1280, 760);
			frame.setLocationByPlatform(true);
			frame.setVisible(true);
			manager.getCurrentFrame().activateView("TaskUsageDetail");
		});
		GuiAcceptanceSupport.await(() -> frame.isShowing() && manager.getCurrentFrame() != null
			&& "TaskUsageDetail".equals(manager.getRibbonContextViewId()), "Task Usage view did not activate");
		DocumentFrame document = manager.getCurrentFrame();
		SpreadSheet sheet = document.getActiveSpreadSheet();
		assertNotNull(sheet);
		int selectedRow = -1;
		for (int row = 0; row < sheet.getRowCount(); row++) {
			var node = sheet.getNodeAtRow(row);
			if (node != null && node.getImpl() == assignment) selectedRow = row;
		}
		assertTrue(selectedRow >= 0, "Task Usage must expose the actual assignment row");

		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(35);
		click(robot, sheet, selectedRow);
		GuiAcceptanceSupport.await(() -> sheet.getSelectedNodes().size() == 1
			&& sheet.getSelectedNodes().get(0).getImpl() == assignment, "Robot must select the intended assignment row");
		clickText(robot, frame.getRibbonPanel(), "Task Usage Format", "タスク使用状況の書式");
		AbstractButton info = commandButton(frame.getRibbonPanel(), "RibbonAssignmentInformation");
		GuiAcceptanceSupport.await(info::isEnabled, "Assignment Information must enable for a selected assignment");
		click(robot, info);
		AssignmentInformationDialog dialog = awaitDialog();
		JSpinner spinner = (JSpinner) named(dialog, "assignmentUnits");
		JFormattedTextField text = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
		click(robot, text);
		robot.keyPress(KeyEvent.VK_CONTROL); robot.keyPress(KeyEvent.VK_A); robot.keyRelease(KeyEvent.VK_A); robot.keyRelease(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_5); robot.keyRelease(KeyEvent.VK_5);
		robot.keyPress(KeyEvent.VK_0); robot.keyRelease(KeyEvent.VK_0);
		AbstractButton bold = (AbstractButton) named(dialog, "assignmentNotesBold");
		click(robot, bold);
		Component notes = named(dialog, "assignmentNotes");
		click(robot, notes);
		robot.keyPress(KeyEvent.VK_CONTROL); robot.keyPress(KeyEvent.VK_A); robot.keyRelease(KeyEvent.VK_A); robot.keyRelease(KeyEvent.VK_CONTROL);
		for (char key : "Formatted note".toCharArray()) type(robot, key);
		clickText(robot, dialog, "OK", "OK");
		awaitClosed(dialog, "OK must close Assignment Information");
		assertEquals(0.5, assignment.getUnits());
		assertTrue(assignment.getNotes().contains("\\b"), "the Notes editor must commit formatted RTF");
		projectUndo(project);
		assertEquals(1.0, assignment.getUnits(), "one Undo must restore the prior assignment units");
		assertEquals("Existing assignment note", assignment.getNotes());

		click(robot, info);
		dialog = awaitDialog();
		spinner = (JSpinner) named(dialog, "assignmentUnits");
		text = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
		click(robot, text);
		robot.keyPress(KeyEvent.VK_CONTROL); robot.keyPress(KeyEvent.VK_A); robot.keyRelease(KeyEvent.VK_A); robot.keyRelease(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_2); robot.keyRelease(KeyEvent.VK_2);
		clickText(robot, dialog, "Cancel", "キャンセル");
		awaitClosed(dialog, "Cancel must close Assignment Information");
		assertEquals(1.0, assignment.getUnits(), "Cancel must discard uncommitted values");
		assertEquals("Existing assignment note", assignment.getNotes());
	}

	private static void projectUndo(Project project) throws Exception {
		SwingUtilities.invokeAndWait(() -> project.getUndoController().undo());
	}

	private static AssignmentInformationDialog awaitDialog() throws Exception {
		GuiAcceptanceSupport.await(() -> visibleDialog() != null, "Assignment Information dialog did not open");
		return visibleDialog();
	}

	private static void awaitClosed(AssignmentInformationDialog dialog, String message) throws Exception {
		GuiAcceptanceSupport.await(() -> !dialog.isShowing(), message);
	}

	private static AssignmentInformationDialog visibleDialog() {
		for (Window window : Window.getWindows())
			if (window instanceof AssignmentInformationDialog dialog && dialog.isShowing()) return dialog;
		return null;
	}

	private static Component named(Component root, String name) {
		if (name.equals(root.getName())) return root;
		if (root instanceof java.awt.Container container)
			for (Component child : container.getComponents()) {
				Component found = named(child, name);
				if (found != null) return found;
			}
		return null;
	}

	private static AbstractButton commandButton(Component root, String command) {
		if (root instanceof AbstractButton button && command.equals(button.getActionCommand()) && button.isShowing()) return button;
		if (root instanceof java.awt.Container container)
			for (Component child : container.getComponents()) {
				AbstractButton found = commandButton(child, command);
				if (found != null) return found;
			}
		throw new AssertionError("Visible ribbon command not found: " + command);
	}

	private static void clickText(Robot robot, Component root, String... texts) {
		for (String text : texts) {
			Component target = findText(root, text);
			if (target != null) { click(robot, target); return; }
		}
		throw new AssertionError("Visible text control not found: " + java.util.Arrays.toString(texts));
	}

	private static Component findText(Component root, String text) {
		if (root instanceof AbstractButton button && text.equals(button.getText()) && button.isShowing()) return button;
		if (root instanceof java.awt.Container container)
			for (Component child : container.getComponents()) {
				Component found = findText(child, text);
				if (found != null) return found;
			}
		return null;
	}

	private static void click(Robot robot, Component component) {
		try {
			java.awt.Point point = component.getLocationOnScreen();
			robot.mouseMove(point.x + component.getWidth() / 2, point.y + component.getHeight() / 2);
			robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
			robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
			robot.waitForIdle();
		} catch (java.awt.IllegalComponentStateException failure) {
			throw new AssertionError("Physical target is not showing: " + component, failure);
		}
	}

	private static void click(Robot robot, SpreadSheet sheet, int row) {
		java.awt.Rectangle cell = sheet.getCellRect(row, 0, true);
		java.awt.Point point = sheet.getLocationOnScreen();
		robot.mouseMove(point.x + cell.x + Math.max(8, cell.width / 2), point.y + cell.y + cell.height / 2);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
	}

	private static void type(Robot robot, char value) {
		int key = KeyEvent.getExtendedKeyCodeForChar(value);
		robot.keyPress(key); robot.keyRelease(key);
	}
}
