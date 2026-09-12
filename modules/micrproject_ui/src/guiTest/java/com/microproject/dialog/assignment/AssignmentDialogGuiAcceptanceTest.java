/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.dialog.assignment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ResourceBundle;

import javax.swing.AbstractButton;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.collaboration.CollaborationSession;
import com.microproject.grouping.core.Node;
import com.microproject.options.CalendarOption;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.AssignmentEntry;
import com.microproject.pm.assignment.AssignmentService;
import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.pm.graphic.frames.MainRibbonFrame;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetModel;
import com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheetModel;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.Environment;

/** GUI-MSP-ASSIGNMENT-01: Assign Resources has one lock-aware mutation route. */
class AssignmentDialogGuiAcceptanceTest {
	private MainRibbonFrame window;
	private GraphicManager manager;
	private boolean previousRibbonUi;
	private boolean previousNewLook;

	@AfterEach
	void closeWindow() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			for (Window candidate : Window.getWindows()) {
				if (candidate instanceof AssignmentDialog || candidate instanceof MainRibbonFrame)
					candidate.dispose();
			}
			if (manager != null)
				manager.cleanUp();
		});
		Environment.setRibbonUI(previousRibbonUi);
		Environment.setNewLook(previousNewLook);
	}

	@Test
	void robotAssignRouteRejectsLockedTaskThenAssignsAndUndoes() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);

		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("assignment-dialog-acceptance", undo), undo);
		project.initialize(false, false);
		NormalTask task = project.createScriptedTask();
		task.setName("Assignment dialog task");
		Resource resource = project.getResourcePool().createScriptedResource();
		resource.setName("Assignment dialog resource");
		showProject(project);
		GuiAcceptanceSupport.await(() -> window.isShowing() && manager.getCurrentFrame() != null
				&& manager.getCurrentFrame().getActiveSpreadSheet() != null,
			"assignment acceptance project did not become visible");

		Robot robot = new Robot();
		robot.setAutoDelay(40);
		SpreadSheet taskSheet = manager.getCurrentFrame().getActiveSpreadSheet();
		click(robot, cellBounds(taskSheet, rowForTask(taskSheet, task), nameColumn(taskSheet)));
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame().getSelectedImpls(false).contains(task),
			"Robot click did not select the assignment task");
		AbstractButton taskTab = buttonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
			.getString("TaskRibbonTask.title"));
		click(robot, bounds(taskTab));
		AbstractButton assignResources = buttonByCommand("RibbonAssignResources");
		GuiAcceptanceSupport.await(assignResources::isEnabled, "Assign Resources remained disabled for a selected task");
		click(robot, bounds(assignResources));
		AssignmentDialog dialog = awaitDialog();
		SpreadSheet resourceSheet = dialog.spreadSheetPane.getSpreadSheet();
		int resourceRow = rowForResource(resourceSheet, resource);
		click(robot, cellBounds(resourceSheet, resourceRow, nameColumn(resourceSheet)));
		GuiAcceptanceSupport.await(() -> dialog.getSelectedResources().contains(resource),
			"Robot click did not select the replacement resource in Assign Resources");

		project.setCollaborationSession(new RejectingCollaborationSession(project));
		click(robot, bounds(dialog.assignButton));
		robot.waitForIdle();
		assertNull(task.findAssignment(resource), "a rejected collaboration lock must leave the task unchanged");

		project.setCollaborationSession(null);
		click(robot, bounds(dialog.assignButton));
		GuiAcceptanceSupport.await(() -> task.findAssignment(resource) != null,
			"Assign Resources did not create the assignment after lock acceptance");
		assertSame(task, task.findAssignment(resource).getTask());

		SwingUtilities.invokeAndWait(dialog::dispose);
		activateWindow(robot);
		click(robot, cellBounds(taskSheet, rowForTask(taskSheet, task), nameColumn(taskSheet)));
		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Z);
		GuiAcceptanceSupport.await(() -> task.findAssignment(resource) == null,
			"Ctrl+Z did not remove the assignment created by Assign Resources");
		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Y);
		GuiAcceptanceSupport.await(() -> task.findAssignment(resource) != null,
			"Ctrl+Y did not restore the assignment created by Assign Resources");
	}

	/** GUI-MSP-ASSIGNMENT-02: Replace preserves actual work and moves remaining work. */
	@Test
	void robotReplaceWithActualWorkPreservesActualsAndSupportsUndoRedo() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);

		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("assignment-replace-acceptance", undo), undo);
		project.initialize(false, false);
		NormalTask task = project.createScriptedTask();
		task.setName("Assignment replace task");
		task.setDuration(2L * CalendarOption.getInstance().getMillisPerDay());
		Resource original = project.getResourcePool().createScriptedResource();
		original.setName("Original assignment resource");
		Resource replacement = project.getResourcePool().createScriptedResource();
		replacement.setName("Replacement assignment resource");
		Assignment source = AssignmentService.getInstance().newAssignment(task, original, 1.0D, 0L, this);
		source.setActualWork(source.getWork(null) / 2L, null);
		long actualWork = source.getActualWork(null);
		long remainingWork = source.getRemainingWork();
		showProject(project);
		GuiAcceptanceSupport.await(() -> window.isShowing() && manager.getCurrentFrame() != null
				&& manager.getCurrentFrame().getActiveSpreadSheet() != null,
			"replacement acceptance project did not become visible");

		Robot robot = new Robot();
		robot.setAutoDelay(40);
		SpreadSheet taskSheet = manager.getCurrentFrame().getActiveSpreadSheet();
		click(robot, cellBounds(taskSheet, rowForTask(taskSheet, task), nameColumn(taskSheet)));
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame().getSelectedImpls(false).contains(task),
			"Robot click did not select the replacement task");
		click(robot, bounds(buttonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
			.getString("TaskRibbonTask.title"))));
		AbstractButton assignResources = buttonByCommand("RibbonAssignResources");
		GuiAcceptanceSupport.await(assignResources::isEnabled, "Assign Resources remained disabled for the replacement task");
		click(robot, bounds(assignResources));
		AssignmentDialog assignmentDialog = awaitDialog();
		SpreadSheet assignmentSheet = assignmentDialog.spreadSheetPane.getSpreadSheet();
		int originalRow = rowForResource(assignmentSheet, original);
		click(robot, cellBounds(assignmentSheet, originalRow, nameColumn(assignmentSheet)));
		GuiAcceptanceSupport.await(() -> assignmentDialog.getSelectedResources().contains(original),
			"Robot click did not select the original assignment resource");
		click(robot, bounds(assignmentDialog.replaceButton));
		ReplaceAssignmentDialog replaceDialog = awaitReplaceDialog();
		SpreadSheet replacementSheet = replaceDialog.spreadSheetPane.getSpreadSheet();
		click(robot, cellBounds(replacementSheet, rowForResource(replacementSheet, replacement), nameColumn(replacementSheet)));
		GuiAcceptanceSupport.await(() -> replaceDialog.getSelectedResources().contains(replacement),
			"Robot click did not select the replacement resource");
		click(robot, bounds(buttonByText(replaceDialog, com.microproject.strings.Messages.getString("ButtonText.OK"))));

		GuiAcceptanceSupport.await(() -> task.findAssignment(replacement) != null,
			"Replace did not create the replacement assignment");
		Assignment replacementAssignment = task.findAssignment(replacement);
		assertSame(source, task.findAssignment(original), "actual work must retain the original assignment");
		assertEquals(actualWork, source.getActualWork(null));
		assertEquals(0L, source.getRemainingWork());
		assertNotNull(replacementAssignment);
		assertEquals(0L, replacementAssignment.getActualWork(null));
		assertEquals(remainingWork, replacementAssignment.getRemainingWork());

		press(robot, KeyEvent.VK_ESCAPE);
		GuiAcceptanceSupport.await(() -> !assignmentDialog.isShowing(),
			"Escape did not close the modeless Assign Resources dialog before undo");
		activateWindow(robot);
		click(robot, cellBounds(taskSheet, rowForTask(taskSheet, task), nameColumn(taskSheet)));
		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Z);
		GuiAcceptanceSupport.await(() -> task.findAssignment(replacement) == null,
			"Ctrl+Z did not remove the replacement assignment");
		assertSame(source, task.findAssignment(original));
		assertEquals(actualWork, source.getActualWork(null));
		assertEquals(remainingWork, source.getRemainingWork());
		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Y);
		GuiAcceptanceSupport.await(() -> task.findAssignment(replacement) != null,
			"Ctrl+Y did not restore the replacement assignment");
		assertSame(source, task.findAssignment(original));
		assertEquals(actualWork, source.getActualWork(null));
		assertEquals(0L, source.getRemainingWork());
		assertEquals(remainingWork, task.findAssignment(replacement).getRemainingWork());
	}

	private void showProject(Project project) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			window = new MainRibbonFrame("microProject — Assignment Resources acceptance", null, null);
			manager = new GraphicManager(window);
			window.setGraphicManager(manager);
			manager.initView();
			manager.addProjectFrame(project);
			window.setSize(1200, 700);
			window.setLocationByPlatform(true);
			window.setAlwaysOnTop(true);
			window.setVisible(true);
			window.toFront();
			window.requestFocus();
		});
	}

	private AssignmentDialog awaitDialog() throws Exception {
		final AssignmentDialog[] dialog = new AssignmentDialog[1];
		GuiAcceptanceSupport.await(() -> {
			for (Window candidate : Window.getWindows()) {
				if (candidate instanceof AssignmentDialog assignmentDialog && assignmentDialog.isShowing()
						&& assignmentDialog.isActive()) {
					dialog[0] = assignmentDialog;
					return true;
				}
			}
			return false;
		}, "Ribbon Assign Resources did not open the assignment dialog");
		return dialog[0];
	}

	private ReplaceAssignmentDialog awaitReplaceDialog() throws Exception {
		final ReplaceAssignmentDialog[] dialog = new ReplaceAssignmentDialog[1];
		GuiAcceptanceSupport.await(() -> {
			for (Window candidate : Window.getWindows()) {
				if (candidate instanceof ReplaceAssignmentDialog replaceDialog && replaceDialog.isShowing()
						&& replaceDialog.isActive()) {
					dialog[0] = replaceDialog;
					return true;
				}
			}
			return false;
		}, "Replace did not open its resource-selection dialog");
		return dialog[0];
	}

	private AbstractButton buttonByCommand(String command) throws Exception {
		final AbstractButton[] button = new AbstractButton[1];
		SwingUtilities.invokeAndWait(() -> button[0] = com.microproject.menu.testsupport.UiComponentWalker.flatten(window).stream()
			.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
			.filter(AbstractButton::isShowing).filter(value -> command.equals(value.getActionCommand()))
			.findFirst().orElseThrow(() -> new AssertionError("Visible command not found: " + command)));
		return button[0];
	}

	private AbstractButton buttonByText(String text) throws Exception {
		return buttonByText(window, text);
	}

	private AbstractButton buttonByText(java.awt.Component root, String text) throws Exception {
		final AbstractButton[] button = new AbstractButton[1];
		SwingUtilities.invokeAndWait(() -> button[0] = com.microproject.menu.testsupport.UiComponentWalker.flatten(root).stream()
			.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
			.filter(AbstractButton::isShowing).filter(value -> text.equals(value.getText()))
			.findFirst().orElseThrow(() -> new AssertionError("Visible button not found: " + text)));
		return button[0];
	}

	private static int rowForTask(SpreadSheet sheet, NormalTask task) {
		CommonSpreadSheetModel model = (CommonSpreadSheetModel) sheet.getModel();
		for (int row = 0; row < sheet.getRowCount(); row++)
			if (model.getNode(row) != null && model.getNode(row).getNode().getImpl() == task)
				return row;
		throw new AssertionError("Task is absent from the task spreadsheet");
	}

	private static int rowForResource(SpreadSheet sheet, Resource resource) {
		SpreadSheetModel model = (SpreadSheetModel) sheet.getModel();
		for (int row = 0; row < sheet.getRowCount(); row++) {
			Node node = model.getNode(row).getNode();
			if (node != null && node.getImpl() instanceof AssignmentEntry entry && entry.getResource() == resource)
				return row;
		}
		throw new AssertionError("Resource is absent from the assignment spreadsheet");
	}

	private static int nameColumn(SpreadSheet sheet) {
		for (int column = 0; column < sheet.getColumnCount(); column++)
			if (sheet.getColumnName(column).toLowerCase(java.util.Locale.ROOT).contains("name"))
				return column;
		return 0;
	}

	private static Rectangle cellBounds(SpreadSheet sheet, int row, int column) throws Exception {
		final Rectangle[] result = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> {
			Rectangle cell = sheet.getCellRect(row, column, true);
			sheet.scrollRectToVisible(cell);
			cell = sheet.getCellRect(row, column, true);
			Point point = sheet.getLocationOnScreen();
			result[0] = new Rectangle(point.x + cell.x, point.y + cell.y, cell.width, cell.height);
		});
		return result[0];
	}

	private static Rectangle bounds(java.awt.Component component) throws Exception {
		final Rectangle[] result = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> result[0] = new Rectangle(component.getLocationOnScreen(), component.getSize()));
		return result[0];
	}

	private static void click(Robot robot, Rectangle bounds) {
		robot.mouseMove(bounds.x + Math.max(3, bounds.width / 2), bounds.y + Math.max(3, bounds.height / 2));
		robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
	}

	private void activateWindow(Robot robot) throws Exception {
		click(robot, bounds(window));
		GuiAcceptanceSupport.await(window::isActive, "main window did not become active for undo");
	}

	private static void press(Robot robot, int... keys) {
		for (int key : keys) robot.keyPress(key);
		for (int index = keys.length - 1; index >= 0; index--) robot.keyRelease(keys[index]);
	}

	private static final class RejectingCollaborationSession extends CollaborationSession {
		RejectingCollaborationSession(Project project) {
			super(project, new File(System.getProperty("java.io.tmpdir"), "assignment-dialog-lock.mpo").getAbsolutePath(),
				"assignment-dialog-test");
		}

		@Override
		public boolean tryLockTasks(Iterable<Task> tasks, java.awt.Component parent, String actionLabel) {
			return false;
		}
	}
}
