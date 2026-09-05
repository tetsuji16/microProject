/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.awt.Dialog;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.dialog.TaskInformationDialog;
import com.microproject.dialog.assignment.TimesheetDialog;
import com.microproject.dialog.assignment.TimesheetEntryPane;
import com.microproject.exchange.MpoFileImporter;
import com.microproject.field.Field;
import com.microproject.grouping.core.Node;
import com.microproject.job.JobQueue;
import com.microproject.menu.testsupport.UiComponentWalker;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetModel;
import com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheetModel;
import com.microproject.pm.graphic.views.UsageDetailView;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.strings.Messages;
import com.microproject.session.SessionFactory;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.Environment;

/**
 * Non-headless regression coverage for issue #330.
 *
 * <p>This deliberately exercises the shipped ribbon route rather than invoking
 * {@code RibbonTaskInformationAction} directly: Robot selects a real task-table
 * cell, opens the Task tab, and clicks the visible Information command. The
 * observable result is the real Task Information dialog.</p>
 */
class TaskInformationRibbonGuiAcceptanceTest {
	private MainRibbonFrame window;
	private GraphicManager manager;
	private boolean previousRibbonUi;
	private boolean previousNewLook;
	private String previousUiDebug;
	private JobQueue previousJobQueue;

	@AfterEach
	void closeWindow() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			for (Window candidate : Window.getWindows()) {
				if (candidate instanceof TaskInformationDialog || candidate instanceof TimesheetDialog)
					candidate.dispose();
			}
			if (manager != null)
				manager.cleanUp();
			for (Window candidate : Window.getWindows()) {
				if (candidate instanceof MainRibbonFrame)
					candidate.dispose();
			}
		});
		Environment.setRibbonUI(previousRibbonUi);
		Environment.setNewLook(previousNewLook);
		if (previousUiDebug == null)
			System.clearProperty("microproject.ui.debug");
		else
			System.setProperty("microproject.ui.debug", previousUiDebug);
		SessionFactory.getInstance().setJobQueue(previousJobQueue);
	}

	@Test
	void robotClickOnTaskPropertiesInformationOpensTaskInformation() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		previousUiDebug = System.getProperty("microproject.ui.debug");
		System.setProperty("microproject.ui.debug", "true");
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);

		NormalTask task = createTask();
		showProject(task.getOwningProject());
		SwingUtilities.invokeAndWait(() -> window.setSize(1600, 700));
		GuiAcceptanceSupport.await(() -> window.isShowing() && manager.getCurrentFrame() != null
				&& manager.getCurrentFrame().getActiveSpreadSheet() != null,
			"full ribbon task window did not become visible");

		Robot robot = new Robot();
		robot.setAutoDelay(45);
		activateWindow(robot, window);
		SpreadSheet sheet = manager.getCurrentFrame().getActiveSpreadSheet();
		int row = rowForTask(sheet, task);
		int column = nameColumn(sheet);
		click(robot, cellOnScreen(sheet, row, column));
		GuiAcceptanceSupport.await(() -> sheet.getSelectedRow() == row,
			"Robot click did not select the task-table row");

		AbstractButton taskTab = findShowingButtonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
			.getString("TaskRibbonTask.title"));
		click(robot, boundsOnScreen(taskTab));
		GuiAcceptanceSupport.await(taskTab::isSelected, "Robot click did not select the Task ribbon tab");
		AbstractButton information = findShowingButtonByCommand("RibbonTaskInformation");
		GuiAcceptanceSupport.await(information::isEnabled,
			"Task Properties > Information remained disabled after selecting a task");
		assertTrue(information.getAction().getClass().getName().contains("UiButtonDiagnostics"),
			"Debug mode must instrument the physical ribbon button action");
		click(robot, boundsOnScreen(information));

		GuiAcceptanceSupport.await(() -> findTaskInformationDialog() != null,
			"Task Properties > Information did not open Task Information after a Robot click");
		TaskInformationDialog dialog = findTaskInformationDialog();
		assertEquals(Messages.getString("TaskInformationDialog.TaskInformation") + " - " + task.getId(), dialog.getTitle());
		capture(robot, dialog);
	}

	@Test
	void usageDetailViewRouteConstructsWithoutRuntimeFailure() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for GUI view coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		NormalTask task = createTask();
		showProject(task.getOwningProject());
		GuiAcceptanceSupport.await(() -> window.isShowing() && manager.getCurrentFrame() != null,
			"usage-detail window did not become visible");
		SwingUtilities.invokeAndWait(() -> {
			DocumentFrame frame = manager.getCurrentFrame();
			assertTrue(frame.activateView(GraphicManager.ACTION_TASK_USAGE_DETAIL));
			assertTrue(frame.getActiveTopView() instanceof UsageDetailView);
			UsageDetailView taskUsage = frame.getTaskUsageDetailView();
			assertNotNull(taskUsage.getSpreadSheet(), "task usage left spreadsheet was not initialized");
			assertNotNull(taskUsage.getTimeSpreadSheet(), "task usage time spreadsheet was not initialized");
			assertTrue(taskUsage.getSpreadSheet().getModel().getRowCount() >= 0);
			assertTrue(frame.activateView(GraphicManager.ACTION_RESOURCE_USAGE_DETAIL));
			assertTrue(frame.getActiveTopView() instanceof UsageDetailView);
			UsageDetailView resourceUsage = frame.getResourceUsageDetailView();
			assertNotNull(resourceUsage.getSpreadSheet(), "resource usage left spreadsheet was not initialized");
			assertNotNull(resourceUsage.getTimeSpreadSheet(), "resource usage time spreadsheet was not initialized");
			assertTrue(resourceUsage.getSpreadSheet().getModel().getRowCount() >= 0);
		});
	}

	@Test
	void timesheetRouteConstructsItsDedicatedSpreadsheet() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for GUI view coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		NormalTask task = createTask();
		showProject(task.getOwningProject());
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame() != null
				&& manager.getCurrentFrame().getActiveSpreadSheet() != null,
			"timesheet test project did not become active");
		SwingUtilities.invokeAndWait(() -> manager.showTimesheetDialog(manager.getCurrentFrame()));
		GuiAcceptanceSupport.await(() -> java.util.Arrays.stream(Window.getWindows())
				.filter(TimesheetDialog.class::isInstance)
				.map(TimesheetDialog.class::cast)
				.anyMatch(dialog -> dialog.isShowing() && dialog.getSpreadSheetPane() != null
						&& dialog.getSpreadSheetPane().getSpreadSheet() != null),
			"RibbonTimesheet did not construct its dedicated spreadsheet");
		TimesheetDialog dialog = java.util.Arrays.stream(Window.getWindows())
			.filter(TimesheetDialog.class::isInstance)
			.map(TimesheetDialog.class::cast)
			.filter(TimesheetDialog::isShowing)
			.findFirst().orElseThrow();
		TimesheetEntryPane pane = dialog.getSpreadSheetPane();
		assertNotNull(pane.getSpreadSheet().getModel(), "timesheet spreadsheet model was not initialized");
	}

	@Test
	void hideAndShowSelectedTaskThroughRibbonRoundTripsWithUndoRedo() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Assumptions.assumeTrue(guiScale() <= 1.0d,
			"Ribbon mutation sweep requires a full-width desktop; high-DPI layout is covered by the dedicated visual matrix.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		NormalTask task = createTask();
		showProject(task.getOwningProject());
		SwingUtilities.invokeAndWait(() -> window.setSize(1600, 700));
		GuiAcceptanceSupport.await(() -> window.isShowing() && manager.getCurrentFrame() != null
				&& manager.getCurrentFrame().getActiveSpreadSheet() != null,
			"hide/show test project did not become visible");
		Robot robot = new Robot();
		robot.setAutoDelay(45);
		activateWindow(robot, window);
		SpreadSheet sheet = manager.getCurrentFrame().getActiveSpreadSheet();
		int row = rowForTask(sheet, task);
		click(robot, cellOnScreen(sheet, row, nameColumn(sheet)));
		AbstractButton taskTab = findShowingButtonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
				.getString("TaskRibbonTask.title"));
		click(robot, boundsOnScreen(taskTab));
		AbstractButton hide = findShowingButtonByCommand("RibbonHideSelectedTasks");
		GuiAcceptanceSupport.await(hide::isEnabled, "Hide Selected Tasks remained disabled after selection");
		click(robot, boundsOnScreen(hide));
		GuiAcceptanceSupport.await(task::isHiddenTask, "Hide Selected Tasks did not update the task model");
		GuiAcceptanceSupport.await(() -> !isTaskVisible(sheet, task),
				"hidden task remained visible in the task sheet");
		ByteArrayOutputStream hiddenSnapshot = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(task.getOwningProject(), hiddenSnapshot),
				"MPO save did not accept the hidden task state");
		Project hiddenReload = new MpoFileImporter().loadProject(
				new ByteArrayInputStream(hiddenSnapshot.toByteArray()));
		assertTrue(taskNamed(hiddenReload, task.getName()).isHiddenTask(),
				"MPO reload lost the hidden task state");

		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_Z);
		robot.keyRelease(KeyEvent.VK_Z);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		robot.waitForIdle();
		GuiAcceptanceSupport.await(() -> !task.isHiddenTask(), "Ctrl+Z did not restore task visibility");
		GuiAcceptanceSupport.await(() -> rowForTask(sheet, task) >= 0, "Ctrl+Z did not restore the visible task row");

		click(robot, cellOnScreen(sheet, rowForTask(sheet, task), nameColumn(sheet)));
		click(robot, boundsOnScreen(hide));
		GuiAcceptanceSupport.await(task::isHiddenTask, "second hide did not update the task model");
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_Y);
		robot.keyRelease(KeyEvent.VK_Y);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		robot.waitForIdle();
		GuiAcceptanceSupport.await(task::isHiddenTask, "Ctrl+Y did not reapply task visibility");

		AbstractButton show = findShowingButtonByCommand("RibbonShowAllTasks");
		assertTrue(show.isShowing(), "Show All Tasks must remain discoverable beside Hide Selected Tasks after hiding");
		assertTrue(show.isEnabled(), "Show All Tasks must become enabled after a task is hidden");
		click(robot, boundsOnScreen(show));
		GuiAcceptanceSupport.await(() -> !task.isHiddenTask(), "Show All Tasks did not restore the task model");
		GuiAcceptanceSupport.await(() -> rowForTask(sheet, task) >= 0, "Show All Tasks did not restore the visible task row");
	}

	private static double guiScale() {
		try {
			String configured = System.getProperty("sun.java2d.uiScale");
			if (configured != null)
				return Double.parseDouble(configured);
		} catch (NumberFormatException ignored) {
			// Fall through to the active device transform.
		}
		return GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
			.getDefaultConfiguration().getDefaultTransform().getScaleX();
	}

	@Test
	void mutationCommandsAreDisabledWithoutTaskSelection() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		NormalTask task = createTask();
		showProject(task.getOwningProject());
		SwingUtilities.invokeAndWait(() -> window.setSize(1600, 700));
		GuiAcceptanceSupport.await(() -> window.isShowing() && manager.getCurrentFrame() != null
				&& manager.getCurrentFrame().getActiveSpreadSheet() != null,
			"no-selection test project did not become visible");
		SpreadSheet sheet = manager.getCurrentFrame().getActiveSpreadSheet();
		SwingUtilities.invokeAndWait(sheet::clearSelection);
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame().getSelectedImpls(false).isEmpty(),
			"test fixture did not reach the no-selection state");

		Robot robot = new Robot();
		robot.setAutoDelay(45);
		activateWindow(robot, window);
		AbstractButton taskTab = findShowingButtonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
				.getString("TaskRibbonTask.title"));
		click(robot, boundsOnScreen(taskTab));
		GuiAcceptanceSupport.await(taskTab::isSelected, "Robot click did not select the Task ribbon tab");
		assertFalse(findShowingButtonByCommand("RibbonLink").isEnabled(),
			"Link must be disabled without two selected tasks");
		assertFalse(findShowingButtonByCommand("RibbonUnlink").isEnabled(),
			"Unlink must be disabled without a selected task");
		assertFalse(findShowingButtonByCommand("RibbonIndent").isEnabled(),
			"Indent must be disabled without a selected task");
		assertFalse(findShowingButtonByCommand("RibbonOutdent").isEnabled(),
			"Outdent must be disabled without a selected task");
		assertFalse(findShowingButtonByCommand("RibbonCollapse").isEnabled(),
			"Collapse must be disabled without a selected task");
		assertFalse(findShowingButtonByCommand("RibbonHideSelectedTasks").isEnabled(),
			"Hide Selected Tasks must be disabled without a selected task");
		assertFalse(findShowingButtonByCommand("RibbonShowAllTasks").isEnabled(),
			"Show All Tasks must be disabled when no task is hidden");
	}

	@Test
	void collapseAndExpandSelectedSummaryChangesVisibleRows() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("ribbon-outline-acceptance", undo), undo);
		project.initialize(false, false);
		Node parentNode = project.createLocalTaskNode(null);
		NormalTask parent = (NormalTask) parentNode.getImpl();
		parent.setName("Outline parent");
		Node childNode = project.createLocalTaskNode(parentNode);
		NormalTask child = (NormalTask) childNode.getImpl();
		child.setName("Outline child");
		project.recalculate();
		showProject(project);
		SwingUtilities.invokeAndWait(() -> window.setSize(1600, 700));
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame() != null
				&& manager.getCurrentFrame().getActiveSpreadSheet() != null,
			"outline test project did not become visible");
		Robot robot = new Robot();
		robot.setAutoDelay(45);
		SpreadSheet sheet = manager.getCurrentFrame().getActiveSpreadSheet();
		click(robot, cellOnScreen(sheet, rowForTask(sheet, parent), nameColumn(sheet)));
		AbstractButton taskTab = findShowingButtonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
				.getString("TaskRibbonTask.title"));
		click(robot, boundsOnScreen(taskTab));
		AbstractButton collapse = findShowingButtonByCommand("RibbonCollapse");
		GuiAcceptanceSupport.await(collapse::isEnabled, "Collapse remained disabled for a selected summary");
		click(robot, boundsOnScreen(collapse));
		GuiAcceptanceSupport.await(() -> !isTaskVisible(sheet, child), "Collapse did not hide the child row");

		AbstractButton expand = findShowingButtonByCommand("RibbonExpand");
		click(robot, boundsOnScreen(expand));
		GuiAcceptanceSupport.await(() -> isTaskVisible(sheet, child), "Expand did not restore the child row");
	}

	@Test
	void indentAndOutdentSelectedTaskThroughRibbonRoundTripsHierarchy() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("ribbon-indent-acceptance", undo), undo);
		project.initialize(false, false);
		Node firstNode = project.createLocalTaskNode(null);
		NormalTask first = (NormalTask) firstNode.getImpl();
		first.setName("Indent predecessor");
		Node secondNode = project.createLocalTaskNode(null);
		NormalTask second = (NormalTask) secondNode.getImpl();
		second.setName("Indent target");
		project.recalculate();
		showProject(project);
		SwingUtilities.invokeAndWait(() -> window.setSize(1600, 700));
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame() != null
				&& manager.getCurrentFrame().getActiveSpreadSheet() != null,
			"indent test project did not become visible");
		Robot robot = new Robot();
		robot.setAutoDelay(45);
		SpreadSheet sheet = manager.getCurrentFrame().getActiveSpreadSheet();
		click(robot, cellOnScreen(sheet, rowForTask(sheet, second), nameColumn(sheet)));
		AbstractButton taskTab = findShowingButtonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
				.getString("TaskRibbonTask.title"));
		click(robot, boundsOnScreen(taskTab));
		AbstractButton indent = findShowingButtonByCommand("RibbonIndent");
		GuiAcceptanceSupport.await(indent::isEnabled, "Indent remained disabled for the selected task");
		click(robot, boundsOnScreen(indent));
		GuiAcceptanceSupport.await(() -> second.getWbsParentTask() == first,
				"Indent did not make the selected task a child of its predecessor");
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame().getSelectedImpls(false).contains(second),
				"Indent did not preserve the selected task after hierarchy refresh");
		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Z);
		GuiAcceptanceSupport.await(() -> second.getWbsParentTask() == null,
				"Ctrl+Z did not restore the selected task to the top level after Indent");
		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Y);
		GuiAcceptanceSupport.await(() -> second.getWbsParentTask() == first,
				"Ctrl+Y did not reapply the selected task hierarchy after Indent");

		AbstractButton outdent = findShowingButtonByCommand("RibbonOutdent");
		GuiAcceptanceSupport.await(outdent::isEnabled, "Outdent became disabled after Indent");
		click(robot, boundsOnScreen(outdent));
		GuiAcceptanceSupport.await(() -> second.getWbsParentTask() == null,
				"Outdent did not restore the selected task to the top level");
	}

	@Test
	void robotNameCellShortcutsFollowMicrosoftSheetNavigationSemantics() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("robot-shortcut-acceptance", undo), undo);
		project.initialize(false, false);
		Node predecessorNode = project.createLocalTaskNode(null);
		NormalTask predecessor = (NormalTask) predecessorNode.getImpl();
		predecessor.setName("Shortcut predecessor");
		Node targetNode = project.createLocalTaskNode(null);
		NormalTask target = (NormalTask) targetNode.getImpl();
		target.setName("Shortcut target");
		Node outdentNode = project.createLocalTaskNode(predecessorNode);
		NormalTask outdentTarget = (NormalTask) outdentNode.getImpl();
		outdentTarget.setName("Shortcut outdent target");
		project.recalculate();
		showProject(project);
		SwingUtilities.invokeAndWait(() -> window.setSize(1600, 700));
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame() != null
				&& manager.getCurrentFrame().getActiveSpreadSheet() != null,
			"shortcut test project did not become visible");
		Robot robot = new Robot();
		robot.setAutoDelay(45);
		activateWindow(robot, window);
		SpreadSheet sheet = manager.getCurrentFrame().getActiveSpreadSheet();
		int nameColumn = nameColumn(sheet);
		SwingUtilities.invokeAndWait(sheet::requestFocusInWindow);
		GuiAcceptanceSupport.await(sheet::isFocusOwner, "shortcut spreadsheet did not accept focus before Robot input");

		// MSP: Tab indents the current name row; Shift+Tab outdents it.
		click(robot, cellOnScreen(sheet, rowForTask(sheet, target), nameColumn));
		GuiAcceptanceSupport.await(sheet::isFocusOwner, "name-cell click did not give focus to the spreadsheet");
		press(robot, KeyEvent.VK_F2);
		GuiAcceptanceSupport.await(sheet::isEditing, "F2 did not enter name-cell editing");
		press(robot, KeyEvent.VK_TAB);
		GuiAcceptanceSupport.await(() -> target.getWbsParentTask() == predecessor,
				"Robot Tab did not indent the selected name row");
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame().getSelectedImpls(false).contains(target),
				"Robot Tab lost the selected task");
		click(robot, cellOnScreen(sheet, rowForTask(sheet, outdentTarget), nameColumn));
		GuiAcceptanceSupport.await(sheet::isFocusOwner, "second name-cell click did not give focus to the spreadsheet");
		press(robot, KeyEvent.VK_F2);
		GuiAcceptanceSupport.await(sheet::isEditing, "F2 did not enter the second name-cell edit");
		press(robot, KeyEvent.VK_SHIFT, KeyEvent.VK_TAB);
		GuiAcceptanceSupport.await(() -> outdentTarget.getWbsParentTask() == null,
				"Robot Shift+Tab did not outdent the selected name row");

		// Ctrl+Up/Down navigates to the first/last visible task row, matching MSP.
		click(robot, cellOnScreen(sheet, rowForTask(sheet, predecessor), nameColumn));
		GuiAcceptanceSupport.await(sheet::isFocusOwner, "navigation name-cell click did not give focus to the spreadsheet");
		press(robot, KeyEvent.VK_F2);
		GuiAcceptanceSupport.await(sheet::isEditing, "F2 did not enter the navigation name-cell edit");
		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_DOWN);
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame().getSelectedImpls(false).contains(outdentTarget),
				"Robot Ctrl+Down did not move to the last visible task row");
		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_UP);
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame().getSelectedImpls(false).contains(predecessor),
				"Robot Ctrl+Up did not move to the first visible task row");
	}

	@Test
	void linkAndUnlinkSelectedTasksThroughRibbonRoundTripsDependency() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("ribbon-link-acceptance", undo), undo);
		project.initialize(false, false);
		NormalTask predecessor = project.createScriptedTask();
		predecessor.setName("Link predecessor");
		NormalTask successor = project.createScriptedTask();
		successor.setName("Link successor");
		project.recalculate();
		showProject(project);
		SwingUtilities.invokeAndWait(() -> window.setSize(1600, 700));
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame() != null
				&& manager.getCurrentFrame().getActiveSpreadSheet() != null,
			"link test project did not become visible");
		Robot robot = new Robot();
		robot.setAutoDelay(45);
		SpreadSheet sheet = manager.getCurrentFrame().getActiveSpreadSheet();
		click(robot, cellOnScreen(sheet, rowForTask(sheet, predecessor), nameColumn(sheet)));
		robot.keyPress(KeyEvent.VK_SHIFT);
		click(robot, cellOnScreen(sheet, rowForTask(sheet, successor), nameColumn(sheet)));
		robot.keyRelease(KeyEvent.VK_SHIFT);
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame().getSelectedImpls(false).contains(predecessor)
				&& manager.getCurrentFrame().getSelectedImpls(false).contains(successor),
			"Shift-click did not preserve both selected tasks");
		AbstractButton taskTab = findShowingButtonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
				.getString("TaskRibbonTask.title"));
		click(robot, boundsOnScreen(taskTab));
		AbstractButton link = findShowingButtonByCommand("RibbonLink");
		GuiAcceptanceSupport.await(link::isEnabled, "Link remained disabled for two selected tasks");
		click(robot, boundsOnScreen(link));
		GuiAcceptanceSupport.await(() -> successor.getPredecessorList().size() == 1,
				"Link did not create a dependency between the selected tasks");
		ByteArrayOutputStream saved = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(project, saved),
				"MPO save did not accept the project after the physical Link command");
		Project reloaded = new MpoFileImporter().loadProject(new ByteArrayInputStream(saved.toByteArray()));
		NormalTask reloadedPredecessor = taskNamed(reloaded, "Link predecessor");
		NormalTask reloadedSuccessor = taskNamed(reloaded, "Link successor");
		assertEquals(1, reloadedSuccessor.getPredecessorList().size(),
				"MPO reload lost the dependency created through the ribbon");
		Dependency reloadedDependency = (Dependency) reloadedSuccessor.getPredecessorList().iterator().next();
		assertEquals("Link predecessor", ((Task) reloadedDependency.getPredecessor()).getName(),
				"MPO reload connected the successor to the wrong predecessor");
		assertTrue(reloadedSuccessor.getWbsParentTask() == null && reloadedPredecessor.getWbsParentTask() == null,
				"MPO reload changed the top-level hierarchy of linked tasks");

		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_Z);
		robot.keyRelease(KeyEvent.VK_Z);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		robot.waitForIdle();
		GuiAcceptanceSupport.await(() -> successor.getPredecessorList().isEmpty(), "Ctrl+Z did not undo the link");
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_Y);
		robot.keyRelease(KeyEvent.VK_Y);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		robot.waitForIdle();
		GuiAcceptanceSupport.await(() -> successor.getPredecessorList().size() == 1, "Ctrl+Y did not redo the link");

		AbstractButton unlink = findShowingButtonByCommand("RibbonUnlink");
		GuiAcceptanceSupport.await(unlink::isEnabled, "Unlink became disabled after link creation");
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame().getSelectedImpls(false).contains(predecessor)
				&& manager.getCurrentFrame().getSelectedImpls(false).contains(successor),
			"Undo/Redo did not preserve both selected tasks for Unlink");
		click(robot, boundsOnScreen(unlink));
		GuiAcceptanceSupport.await(() -> successor.getPredecessorList().isEmpty(),
				"Unlink did not remove the dependency between the selected tasks");
	}

	@Test
	void secondaryDocumentWindowUsesTheSameRibbonShell() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for window coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		NormalTask first = createTask();
		NormalTask second = createTask();
		second.getOwningProject().setName("secondary-window-second");
		second.getOwningProject().setUniqueId(first.getOwningProject().getUniqueId() + 1);
		first.getOwningProject().setFileName("secondary-window-first.mpo");
		second.getOwningProject().setFileName("secondary-window-second.mpo");
		showProject(first.getOwningProject());
		SwingUtilities.invokeAndWait(() -> manager.addProjectFrame(second.getOwningProject()));
		assertTrue(manager.getFrameManager().getAllFrames().size() >= 2,
			"second document was not registered: firstId=" + first.getOwningProject().getUniqueId()
				+ " secondId=" + second.getOwningProject().getUniqueId()
				+ " sameProject=" + (first.getOwningProject() == second.getOwningProject())
				+ " equals=" + first.getOwningProject().equals(second.getOwningProject()));
		GuiAcceptanceSupport.await(() -> java.util.Arrays.stream(Window.getWindows())
			.filter(candidate -> candidate instanceof MainRibbonFrame && candidate != window)
			.map(MainRibbonFrame.class::cast)
			.anyMatch(candidate -> candidate.isShowing() && candidate.getRibbonPanel() != null
				&& candidate.getTitle().contains("secondary-window-second")),
			"secondary document window did not install the ribbon shell");
		second.getOwningProject().setDirty(false);
		second.getOwningProject().setGroupDirty(false);
		previousJobQueue = SessionFactory.getInstance().getJobQueue();
		if (previousJobQueue == null)
			SessionFactory.getInstance().setJobQueue(new JobQueue("gui-acceptance", false));
		MainRibbonFrame secondary = java.util.Arrays.stream(Window.getWindows())
			.filter(candidate -> candidate instanceof MainRibbonFrame && candidate != window)
			.map(MainRibbonFrame.class::cast)
			.filter(candidate -> candidate.isShowing() && candidate.getTitle().contains("secondary-window-second"))
			.findFirst().orElseThrow(() -> new AssertionError("secondary window disappeared"));
		secondary.dispatchEvent(new java.awt.event.WindowEvent(secondary,
			java.awt.event.WindowEvent.WINDOW_CLOSING));
		GuiAcceptanceSupport.await(() -> !secondary.isShowing(),
			"secondary document window did not close from its title-bar close action");
	}

	private static NormalTask createTask() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("ribbon-task-information", undo);
		Project project = Project.createProject(resourcePool, undo);
		project.initialize(false, false);
		NormalTask task = project.createScriptedTask();
		task.setName("Ribbon information acceptance");
		return task;
	}

	private static NormalTask taskNamed(Project project, String name) {
		for (Task task : project.getTaskList()) {
			if (name.equals(task.getName()))
				return (NormalTask) task;
		}
		throw new AssertionError("Reloaded task is absent: " + name);
	}

	private void showProject(Project project) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			for (Window candidate : Window.getWindows()) {
				if (candidate instanceof Dialog && candidate.isShowing())
					candidate.dispose();
				if (candidate instanceof MainRibbonFrame)
					candidate.dispose();
			}
			window = new MainRibbonFrame("microProject — Task Information ribbon acceptance", null, null);
			manager = new GraphicManager(window);
			window.setGraphicManager(manager);
			manager.initView();
			manager.addProjectFrame(project);
			window.setSize(1120, 700);
			window.setLocationByPlatform(true);
			window.setAlwaysOnTop(true);
			window.setVisible(true);
			window.toFront();
			window.requestFocus();
		});
	}

	private AbstractButton findShowingButtonByText(String text) throws Exception {
		AbstractButton[] result = new AbstractButton[1];
		SwingUtilities.invokeAndWait(() -> result[0] = UiComponentWalker.flatten(window).stream()
			.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
			.filter(AbstractButton::isShowing).filter(button -> text.equals(button.getText()))
			.findFirst().orElseThrow(() -> new AssertionError("Visible ribbon button not found: " + text)));
		return result[0];
	}

	private AbstractButton findShowingButtonByCommand(String command) throws Exception {
		AbstractButton[] result = new AbstractButton[1];
		SwingUtilities.invokeAndWait(() -> result[0] = UiComponentWalker.flatten(window).stream()
			.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
			.filter(AbstractButton::isShowing).filter(button -> command.equals(button.getActionCommand()))
			.findFirst().orElseThrow(() -> new AssertionError("Visible ribbon command not found: " + command
				+ " visibleCommands=" + UiComponentWalker.flatten(window).stream()
					.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
					.filter(AbstractButton::isShowing).map(AbstractButton::getActionCommand)
					.filter(java.util.Objects::nonNull).collect(Collectors.joining(",")))));
		return result[0];
	}

	private static int rowForTask(SpreadSheet sheet, NormalTask task) {
		CommonSpreadSheetModel model = (CommonSpreadSheetModel) sheet.getModel();
		for (int row = 0; row < sheet.getRowCount(); row++) {
			if (model.getNode(row) != null && model.getNode(row).getNode().getImpl() == task)
				return row;
		}
		throw new AssertionError("Task is absent from the visible spreadsheet");
	}

	private static boolean isTaskVisible(SpreadSheet sheet, NormalTask task) {
		CommonSpreadSheetModel model = (CommonSpreadSheetModel) sheet.getModel();
		for (int row = 0; row < sheet.getRowCount(); row++) {
			if (model.getNode(row) != null && model.getNode(row).getNode() != null
					&& model.getNode(row).getNode().getImpl() == task)
				return true;
		}
		return false;
	}

	private static int nameColumn(SpreadSheet sheet) {
		SpreadSheetModel model = (SpreadSheetModel) sheet.getModel();
		for (int modelColumn = 0; modelColumn < model.getColumnCount(); modelColumn++) {
			Field field = model.getFieldInColumn(modelColumn);
			if (field != null && field.isNameField())
				return sheet.convertColumnIndexToView(modelColumn);
		}
		throw new AssertionError("Task-name column is absent from the visible spreadsheet");
	}

	private static Rectangle cellOnScreen(SpreadSheet sheet, int row, int column) throws Exception {
		Rectangle[] result = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> {
			Rectangle cell = sheet.getCellRect(row, column, true);
			Point location = sheet.getLocationOnScreen();
			result[0] = new Rectangle(location.x + cell.x, location.y + cell.y, cell.width, cell.height);
		});
		return result[0];
	}

	private static Rectangle boundsOnScreen(java.awt.Component component) throws Exception {
		Rectangle[] result = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> result[0] = new Rectangle(component.getLocationOnScreen(), component.getSize()));
		return result[0];
	}

	private static void click(Robot robot, Rectangle bounds) {
		robot.mouseMove(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
		robot.delay(150);
	}

	private static void activateWindow(Robot robot, java.awt.Window window) throws Exception {
		Rectangle bounds = boundsOnScreen(window);
		robot.mouseMove(bounds.x + Math.min(40, bounds.width / 2), bounds.y + 12);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
	}

	private static void press(Robot robot, int... keys) {
		for (int key : keys)
			robot.keyPress(key);
		for (int index = keys.length - 1; index >= 0; index--)
			robot.keyRelease(keys[index]);
		robot.waitForIdle();
	}

	private static TaskInformationDialog findTaskInformationDialog() {
		for (Window candidate : Window.getWindows()) {
			if (candidate instanceof TaskInformationDialog dialog && dialog.isVisible())
				return dialog;
		}
		return null;
	}

	private static void capture(Robot robot, TaskInformationDialog dialog) throws Exception {
		Rectangle[] bounds = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> bounds[0] = new Rectangle(dialog.getLocationOnScreen(), dialog.getSize()));
		BufferedImage image = robot.createScreenCapture(bounds[0]);
		Path artifact = Path.of(System.getProperty("micrproject.gui.artifacts.dir", "build/guiTest-artifacts"),
			"task-information-ribbon-click.png");
		Files.createDirectories(artifact.getParent());
		ImageIO.write(image, "png", artifact.toFile());
		assertTrue(image.getWidth() > 300 && image.getHeight() > 200, "Task Information capture is unexpectedly small");
	}
}
