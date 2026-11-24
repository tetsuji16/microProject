/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.util.ResourceBundle;

import javax.swing.AbstractButton;
import javax.swing.JSpinner;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.model.assignment.Assignment;
import com.microproject.model.assignment.AssignmentService;
import com.microproject.ui.frame.DocumentFrame;
import com.microproject.ui.frame.GraphicManager;
import com.microproject.ui.frame.MainRibbonFrame;
import com.microproject.ui.view.spreadsheet.SpreadSheet;
import com.microproject.ui.view.views.UsageDetailView;
import com.microproject.model.resource.ResourcePool;
import com.microproject.model.task.NormalTask;
import com.microproject.model.task.Project;
import com.microproject.ui.testsupport.GuiAcceptanceSupport;
import com.microproject.ui.testsupport.RibbonGuiSupport;
import com.microproject.undo.DataFactoryUndoController;

/** Verifies the real selected-assignment ribbon route, edit, undo and cancellation. */
class AssignmentInformationGuiAcceptanceTest {

	@Test
	void selectedAssignmentInformationEditsAndCancelsThroughPhysicalRibbonRoute() throws Exception {
		Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(), "Robot acceptance requires a desktop session.");
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("assignment-info-gui", undo), undo);
		project.initialize(false, false);
		var resource = project.getResourcePool().newResourceInstance();
		resource.setName("Resource 1");
		NormalTask task = project.createScriptedTask();
		task.setName("Task 1");
		project.connectTask(task);
		Assignment assignment = AssignmentService.getInstance().newAssignment(task, resource, 1.0, 0L, null);
		assignment.forceUnits(1.0);
		assignment.setWorkContourTypeKind(com.microproject.model.assignment.contour.ContourTypes.Kind.FLAT);
		assignment.setNotes("Original notes");

		MainRibbonFrame frame = null;
		GraphicManager manager = null;
		try {
			MainRibbonFrame[] frameRef = new MainRibbonFrame[1];
			GraphicManager[] managerRef = new GraphicManager[1];
			javax.swing.SwingUtilities.invokeAndWait(() -> {
				frameRef[0] = new MainRibbonFrame("Assignment Information acceptance", null);
				managerRef[0] = new GraphicManager(frameRef[0]);
				frameRef[0].setGraphicManager(managerRef[0]);
				managerRef[0].initView();
				managerRef[0].addProjectFrame(project);
				frameRef[0].setSize(1280, 760);
				frameRef[0].setLocationByPlatform(true);
				frameRef[0].setVisible(true);
			});
			frame = frameRef[0];
			manager = managerRef[0];
			GuiAcceptanceSupport.await(() -> frameRef[0].isShowing() && managerRef[0].getCurrentFrame() != null, "Project window did not become visible");

			Robot robot = new com.microproject.ui.testsupport.GuiRobot();
			robot.setAutoDelay(35);
			// Activate Task Usage view by clicking the ribbon button
			AbstractButton viewTab = GuiAcceptanceSupport.findShowingButtonByText(frame,
				java.util.ResourceBundle.getBundle("com.microproject.ui.menu.menu").getString("ViewRibbonTask.title"));
			click(robot, viewTab);
			GuiAcceptanceSupport.await(() -> viewTab.isSelected(), "View tab click did not select the View ribbon");
			AbstractButton taskUsageButton = RibbonGuiSupport.findVisibleOrExpand(robot, frame, "RibbonTaskUsageDetail");
			click(robot, taskUsageButton);
			GuiAcceptanceSupport.await(() -> "TaskUsageDetail".equals(managerRef[0].getRibbonContextViewId())
				&& managerRef[0].getCurrentFrame().getActiveTopView() instanceof UsageDetailView, "Task Usage view did not activate");
			// Get the spreadsheet from the UsageDetailView
			UsageDetailView usageView = (UsageDetailView) manager.getCurrentFrame().getActiveTopView();
			GuiAcceptanceSupport.await(() -> usageView.getSpreadSheet() != null && usageView.getSpreadSheet().isShowing(), "SpreadSheet must be showing");
			SpreadSheet sheet = usageView.getSpreadSheet();
			assertNotNull(sheet);
			int selectedRow = -1;
			for (int row = 0; row < sheet.getRowCount(); row++) {
				if (sheet.getNodeAtRow(row).getImpl() == assignment) {
					selectedRow = row;
					break;
				}
			}
			assertTrue(selectedRow >= 0, "Task Usage must expose the actual assignment row");

			click(robot, sheet, selectedRow);
			GuiAcceptanceSupport.await(() -> sheet.getSelectedNodes().size() == 1
				&& sheet.getSelectedNodes().get(0).getImpl() == assignment, "Robot must select the intended assignment row");
			// Activate Task ribbon tab (where Assignment Information button is located)
			AbstractButton taskTab = GuiAcceptanceSupport.findShowingButtonByText(frame,
				ResourceBundle.getBundle("com.microproject.ui.menu.menu").getString("TaskRibbonTask.title"));
			click(robot, taskTab);
			GuiAcceptanceSupport.await(() -> taskTab.isSelected(), "Task tab click did not select the Task ribbon");
			robot.waitForIdle();
			Thread.sleep(300);
			AbstractButton info = RibbonGuiSupport.findVisibleOrExpand(robot, frame, "RibbonAssignmentInformation");
			if (info == null) throw new AssertionError("Visible ribbon command not found: RibbonAssignmentInformation");
			GuiAcceptanceSupport.await(info::isEnabled, "Assignment Information must enable for a selected assignment");
			GuiAcceptanceSupport.await(() -> info.isShowing(), "Assignment Information button must be showing");
			click(robot, info);
			AssignmentInformationDialog dialog = awaitDialog();
			JSpinner spinner = (JSpinner) named(dialog, "assignmentUnits");
			spinner.setValue(200.0); // 200%
			clickOk(dialog);
			GuiAcceptanceSupport.await(() -> !dialog.isShowing(), "Dialog must close after OK");
			assertEquals(2.0, assignment.getUnits(), 0.001, "Units must be 200% after edit");
			// Undo must revert both fields in one step
			robot.keyPress(java.awt.event.KeyEvent.VK_CONTROL);
			robot.keyPress(java.awt.event.KeyEvent.VK_Z);
			robot.keyRelease(java.awt.event.KeyEvent.VK_Z);
			robot.keyRelease(java.awt.event.KeyEvent.VK_CONTROL);
			robot.waitForIdle();
			GuiAcceptanceSupport.await(() -> assignment.getUnits() == 1.0, "Undo must revert units to original");
			assertEquals(1.0, assignment.getUnits(), 0.001, "Undo reverts units");

			// Second assignment row for cancel test
			NormalTask task2 = project.createScriptedTask();
			task2.setName("Task 2");
			project.connectTask(task2);
			Assignment assignment2 = AssignmentService.getInstance().newAssignment(task2, resource, 0.5, 0L, null);
			assignment2.forceUnits(0.5);
			assignment2.setWorkContourTypeKind(com.microproject.model.assignment.contour.ContourTypes.Kind.FLAT);
			assignment2.setNotes("Cancel notes");

			// Re-select assignment2
			int selectedRow2 = -1;
			for (int row = 0; row < sheet.getRowCount(); row++) {
				if (sheet.getNodeAtRow(row).getImpl() == assignment2) {
					selectedRow2 = row;
					break;
				}
			}
			assertTrue(selectedRow2 >= 0);
			click(robot, sheet, selectedRow2);
			GuiAcceptanceSupport.await(() -> sheet.getSelectedNodes().size() == 1
				&& sheet.getSelectedNodes().get(0).getImpl() == assignment2, "Robot must select the second assignment row");
			// Click Assignment Information again
			AbstractButton info2 = RibbonGuiSupport.findVisibleOrExpand(robot, frame, "RibbonAssignmentInformation");
			click(robot, info2);
			AssignmentInformationDialog dialog2 = awaitDialog();
			JSpinner spinner2 = (JSpinner) named(dialog2, "assignmentUnits");
			spinner2.setValue(300.0); // 300%
			clickCancel(dialog2);
			GuiAcceptanceSupport.await(() -> !dialog2.isShowing(), "Dialog must close after Cancel");
			assertEquals(0.5, assignment2.getUnits(), 0.001, "Cancel must leave units unchanged");
		} finally {
			GraphicManager cleanupManager = manager;
			MainRibbonFrame cleanupFrame = frame;
			javax.swing.SwingUtilities.invokeAndWait(() -> {
				if (cleanupManager != null) cleanupManager.cleanUp();
				if (cleanupFrame != null) cleanupFrame.dispose();
			});
		}
	}

	private static AssignmentInformationDialog awaitDialog() throws Exception {
		AssignmentInformationDialog[] found = new AssignmentInformationDialog[1];
		GuiAcceptanceSupport.await(() -> {
			try {
				javax.swing.SwingUtilities.invokeAndWait(() -> {
					for (Window window : Window.getWindows())
						if (window instanceof AssignmentInformationDialog dialog && dialog.isShowing()) found[0] = dialog;
				});
			} catch (Exception failure) { throw new RuntimeException(failure); }
			return found[0] != null;
		}, "AssignmentInformationDialog must appear");
		return found[0];
	}

	private static <T> T named(Container root, String name) {
		for (Component c : root.getComponents()) {
			if (name.equals(c.getName())) return (T) c;
			if (c instanceof Container) {
				T found = named((Container) c, name);
				if (found != null) return found;
			}
		}
		return null;
	}

	private static void click(Robot robot, Component target) {
		robot.waitForIdle();
		java.awt.Point p = target.getLocationOnScreen();
		robot.mouseMove(p.x + target.getWidth() / 2, p.y + target.getHeight() / 2);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
	}

	private static void click(Robot robot, SpreadSheet sheet, int row) {
		java.awt.Rectangle cell = sheet.getCellRect(row, 0, true);
		java.awt.Point p = sheet.getLocationOnScreen();
		robot.waitForIdle();
		robot.mouseMove(p.x + cell.x + cell.width / 2, p.y + cell.y + cell.height / 2);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
	}

	private static void clickOk(AssignmentInformationDialog dialog) {
		Component ok = named(dialog.getContentPane(), "ok");
		if (ok instanceof AbstractButton) {
			((AbstractButton) ok).doClick();
		}
	}

	private static void clickCancel(AssignmentInformationDialog dialog) {
		Component cancel = named(dialog.getContentPane(), "cancel");
		if (cancel instanceof AbstractButton) {
			((AbstractButton) cancel).doClick();
		}
	}
}
