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
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JSpinner;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.model.assignment.Assignment;
import com.microproject.model.assignment.AssignmentService;
import com.microproject.i18n.Messages;
import com.microproject.ribbon.RibbonCommandResult;
import com.microproject.ui.frame.DocumentFrame;
import com.microproject.ui.frame.GraphicManager;
import com.microproject.ui.frame.MainRibbonFrame;
import com.microproject.ui.view.spreadsheet.SpreadSheet;
import com.microproject.ui.view.views.UsageDetailView;
import com.microproject.model.resource.ResourcePool;
import com.microproject.model.task.NormalTask;
import com.microproject.model.task.Project;
import com.microproject.ui.testsupport.GuiAcceptanceSupport;
import com.microproject.ui.testsupport.GuiJourney;
import com.microproject.ui.testsupport.GuiJourney.InputMechanism;
import com.microproject.ui.testsupport.RibbonGuiSupport;
import com.microproject.undo.DataFactoryUndoController;

/** Verifies the real selected-assignment ribbon route, edit, undo and cancellation. */
class AssignmentInformationGuiAcceptanceTest {

	@Test
	@GuiJourney(InputMechanism.RIBBON_CLICK)
	void selectedAssignmentInformationEditsAndCancelsThroughPhysicalRibbonRoute() throws Exception {
		Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(), "Robot acceptance requires a desktop session.");
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("assignment-info-gui", undo), undo);
		project.initialize(false, false);
		var resource = project.getResourcePool().createScriptedResource();
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
			// Use the contextual Task Usage Format tab that owns this command.
			AbstractButton taskUsageFormatTab = GuiAcceptanceSupport.findShowingButtonByText(frame,
				ResourceBundle.getBundle("com.microproject.ui.menu.menu").getString("TaskUsageFormatRibbonTask.title"));
			click(robot, taskUsageFormatTab);
			GuiAcceptanceSupport.await(taskUsageFormatTab::isSelected, "Task Usage Format tab did not select");
			robot.waitForIdle();
			Thread.sleep(300);
			AbstractButton info = RibbonGuiSupport.findVisibleOrExpand(robot, frame, "RibbonAssignmentInformation");
			if (info == null) throw new AssertionError("Visible ribbon command not found: RibbonAssignmentInformation");
			GuiAcceptanceSupport.await(info::isEnabled, "Assignment Information must enable for a selected assignment");
			GuiAcceptanceSupport.await(() -> info.isShowing(), "Assignment Information button must be showing");
			assertAssignmentSelection(frame, manager, sheet, assignment, info);
			AtomicInteger commandDispatches = new AtomicInteger();
			info.addActionListener(event -> commandDispatches.incrementAndGet());
			click(robot, info);
			if (commandDispatches.get() != 1) captureRouteFailure(frame, manager, sheet, assignment, info);
			assertEquals(1, commandDispatches.get(), "The physical click must dispatch the registered ribbon action");
			AssignmentInformationDialog dialog;
			try {
				dialog = awaitDialog();
			} catch (AssertionError failure) {
				captureRouteFailure(frame, manager, sheet, assignment, info);
				throw failure;
			}
			JSpinner spinner = (JSpinner) named(dialog, "assignmentUnits");
			javax.swing.JFormattedTextField unitsField = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
			click(robot, unitsField);
			robot.keyPress(java.awt.event.KeyEvent.VK_CONTROL);
			robot.keyPress(java.awt.event.KeyEvent.VK_A);
			robot.keyRelease(java.awt.event.KeyEvent.VK_A);
			robot.keyRelease(java.awt.event.KeyEvent.VK_CONTROL);
			for (char key : "200".toCharArray()) robot.keyPress(java.awt.event.KeyEvent.getExtendedKeyCodeForChar(key));
			for (char key : "200".toCharArray()) robot.keyRelease(java.awt.event.KeyEvent.getExtendedKeyCodeForChar(key));
			robot.keyPress(java.awt.event.KeyEvent.VK_ENTER);
			robot.keyRelease(java.awt.event.KeyEvent.VK_ENTER);
			clickOk(robot, dialog);
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

			// Open the same command through Resource Usage Format and cancel without mutation.
			AbstractButton viewTabAgain = GuiAcceptanceSupport.findShowingButtonByText(frame,
				ResourceBundle.getBundle("com.microproject.ui.menu.menu").getString("ViewRibbonTask.title"));
			click(robot, viewTabAgain);
			AbstractButton resourceUsageButton = RibbonGuiSupport.findVisibleOrExpand(robot, frame, "RibbonResourceUsageDetail");
			click(robot, resourceUsageButton);
			GuiAcceptanceSupport.await(() -> "ResourceUsageDetail".equals(managerRef[0].getRibbonContextViewId()),
				"Resource Usage view did not activate");
			UsageDetailView resourceUsageView = (UsageDetailView) manager.getCurrentFrame().getActiveTopView();
			SpreadSheet resourceSheet = resourceUsageView.getSpreadSheet();
			int resourceAssignmentRow = -1;
			for (int row = 0; row < resourceSheet.getRowCount(); row++)
				if (resourceSheet.getNodeAtRow(row).getImpl() == assignment) { resourceAssignmentRow = row; break; }
			assertTrue(resourceAssignmentRow >= 0, "Resource Usage must expose the actual assignment row");
			click(robot, resourceSheet, resourceAssignmentRow);
			GuiAcceptanceSupport.await(() -> resourceSheet.getSelectedNodes().size() == 1
				&& resourceSheet.getSelectedNodes().get(0).getImpl() == assignment,
				"Robot must select the assignment in Resource Usage");
			AbstractButton resourceUsageFormatTab = GuiAcceptanceSupport.findShowingButtonByText(frame,
				ResourceBundle.getBundle("com.microproject.ui.menu.menu").getString("ResourceUsageFormatRibbonTask.title"));
			click(robot, resourceUsageFormatTab);
			GuiAcceptanceSupport.await(resourceUsageFormatTab::isSelected, "Resource Usage Format tab did not select");
			AbstractButton resourceInfo = RibbonGuiSupport.findVisibleOrExpand(robot, frame, "RibbonAssignmentInformation");
			GuiAcceptanceSupport.await(resourceInfo::isEnabled, "Command must enable on Resource Usage Format");
			assertAssignmentSelection(frame, manager, resourceSheet, assignment, resourceInfo);
			click(robot, resourceInfo);
			AssignmentInformationDialog resourceDialog;
			try {
				resourceDialog = awaitDialog();
			} catch (AssertionError failure) {
				captureRouteFailure(frame, manager, resourceSheet, assignment, resourceInfo);
				throw failure;
			}
			robot.keyPress(java.awt.event.KeyEvent.VK_ESCAPE);
			robot.keyRelease(java.awt.event.KeyEvent.VK_ESCAPE);
			GuiAcceptanceSupport.await(() -> !resourceDialog.isShowing(), "Escape must cancel the dialog");
			assertEquals(1.0, assignment.getUnits(), 0.001, "Resource Usage cancel must not mutate the assignment");

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

	private static void assertAssignmentSelection(MainRibbonFrame frame, GraphicManager manager, SpreadSheet sheet,
			Assignment assignment, AbstractButton command) throws Exception {
		Object[] state = new Object[3];
		javax.swing.SwingUtilities.invokeAndWait(() -> {
			state[0] = manager.getCurrentFrame().getActiveTopView();
			state[1] = sheet.getSelectedNodes().size() == 1
				&& sheet.getSelectedNodes().get(0).getImpl() == assignment;
			try {
				var resolver = GraphicManager.class.getDeclaredMethod("informationSelectionSnapshot");
				resolver.setAccessible(true);
				Object selection = resolver.invoke(manager);
				var kind = selection.getClass().getDeclaredMethod("kind");
				var selectedAssignment = selection.getClass().getDeclaredMethod("assignment");
				kind.setAccessible(true);
				selectedAssignment.setAccessible(true);
				state[2] = kind.invoke(selection) + ":" + (selectedAssignment.invoke(selection) == assignment);
			} catch (ReflectiveOperationException failure) {
				throw new RuntimeException(failure);
			}
		});
		if (!(state[0] instanceof UsageDetailView) || !Boolean.TRUE.equals(state[1])
				|| !"ASSIGNMENT:true".equals(state[2])) captureRouteFailure(frame, manager, sheet, assignment, command);
		assertTrue(state[0] instanceof UsageDetailView, "The action resolver must see the active Usage Detail view");
		assertEquals(Boolean.TRUE, state[1], "The actual assignment row must remain selected when the format tab is active");
		assertEquals("ASSIGNMENT:true", state[2], "The action's selection resolver must resolve this exact assignment");
		assertEquals("RibbonAssignmentInformationAction",
			manager.getAction("RibbonAssignmentInformation").getClass().getSimpleName(),
			"The ribbon command must map to its registered document action");
	}

	private static void captureRouteFailure(MainRibbonFrame frame, GraphicManager manager, SpreadSheet sheet,
			Assignment assignment, AbstractButton command) throws Exception {
		Path directory = Path.of(System.getProperty("microproject.gui.artifacts.dir", "build/guiTest-artifacts"));
		Files.createDirectories(directory);
		StringBuilder state = new StringBuilder().append("frameBounds=").append(frame.getBounds()).append('\n')
			.append("commandEnabled=").append(command.isEnabled()).append('\n')
			.append("commandAction=").append(manager.getAction("RibbonAssignmentInformation").getClass().getName()).append('\n');
		javax.swing.SwingUtilities.invokeAndWait(() -> {
			Action buttonAction = command.getAction();
			Object ribbonOutcome = buttonAction == null ? null
				: buttonAction.getValue(RibbonCommandResult.RESULT_ACTION_PROPERTY);
			state.append("buttonAction=").append(buttonAction == null ? "null" : buttonAction.getClass().getName()).append('\n')
				.append("buttonActionListeners=").append(java.util.Arrays.stream(command.getActionListeners())
					.map(listener -> listener.getClass().getName()).toList()).append('\n')
				.append("ribbonOutcome=").append(ribbonOutcome).append('\n');
			state.append("activeTopView=").append(manager.getCurrentFrame().getActiveTopView().getClass().getName()).append('\n')
				.append("selectedNodes=").append(sheet.getSelectedNodes().size()).append('\n')
				.append("selectedAssignment=").append(sheet.getSelectedNodes().size() == 1
					&& sheet.getSelectedNodes().get(0).getImpl() == assignment).append('\n');
			try {
				var resolver = GraphicManager.class.getDeclaredMethod("informationSelectionSnapshot");
				resolver.setAccessible(true);
				Object selection = resolver.invoke(manager);
				var kind = selection.getClass().getDeclaredMethod("kind");
				var selectedAssignment = selection.getClass().getDeclaredMethod("assignment");
				kind.setAccessible(true);
				selectedAssignment.setAccessible(true);
				state.append("selectionResolver=").append(kind.invoke(selection)).append(":")
					.append(selectedAssignment.invoke(selection) == assignment).append('\n');
			} catch (ReflectiveOperationException failure) {
				state.append("selectionResolverFailure=").append(failure).append('\n');
			}
			for (Window window : Window.getWindows()) state.append(window.getClass().getName())
				.append(" showing=").append(window.isShowing()).append(" bounds=").append(window.getBounds()).append('\n');
		});
		Files.writeString(directory.resolve("assignment-information-route-failure.txt"), state);
		Rectangle desktop = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		BufferedImage screenshot = new Robot().createScreenCapture(desktop);
		javax.imageio.ImageIO.write(screenshot, "png", directory.resolve("assignment-information-route-failure.png").toFile());
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

	private static void clickOk(Robot robot, AssignmentInformationDialog dialog) throws Exception {
		click(robot, GuiAcceptanceSupport.findShowingButtonByText(dialog, Messages.getString("ButtonText.OK")));
	}

	private static void clickCancel(AssignmentInformationDialog dialog) {
		Component cancel = named(dialog.getContentPane(), "cancel");
		if (cancel instanceof AbstractButton) {
			((AbstractButton) cancel).doClick();
		}
	}
}
