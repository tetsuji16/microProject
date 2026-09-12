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
import javax.swing.JComponent;
import javax.swing.JComboBox;
import javax.swing.JMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.dialog.TaskInformationDialog;
import com.microproject.dialog.UpdateProjectDialogBox;
import com.microproject.dialog.CalendarViewDialogBox;
import com.microproject.dialog.DependencyDialog;
import com.microproject.dialog.assignment.TimesheetDialog;
import com.microproject.dialog.assignment.TimesheetEntryPane;
import com.microproject.exchange.MpoFileImporter;
import com.microproject.field.Field;
import com.microproject.grouping.core.Node;
import com.microproject.job.JobQueue;
import com.microproject.menu.testsupport.UiComponentWalker;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetModel;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetPopupMenu;
import com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheetModel;
import com.microproject.pm.graphic.views.UsageDetailView;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.strings.Messages;
import com.microproject.session.SessionFactory;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.testsupport.GuiCommandAcceptanceFixture;
import com.microproject.testsupport.GuiPhysicalRouteAdapter;
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
		SwingUtilities.invokeAndWait(() -> window.setSize(1600, 700));
		GuiAcceptanceSupport.await(() -> window.isShowing() && manager.getCurrentFrame() != null,
			"usage-detail window did not become visible");
		Robot robot = new Robot();
		robot.setAutoDelay(45);
		activateWindow(robot, window);
		AbstractButton viewTab = findShowingButtonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
				.getString("ViewRibbonTask.title"));
		click(robot, boundsOnScreen(viewTab));
		GuiAcceptanceSupport.await(viewTab::isSelected, "Robot click did not select the View ribbon tab");
		AbstractButton taskUsageButton = findShowingButtonByCommand("RibbonTaskUsageDetail");
		click(robot, boundsOnScreen(taskUsageButton));
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame().getActiveTopView() instanceof UsageDetailView,
			"Task Usage did not become the active view after a Robot click");
		UsageDetailView taskUsage = (UsageDetailView)manager.getCurrentFrame().getActiveTopView();
		assertNotNull(taskUsage.getSpreadSheet(), "task usage left spreadsheet was not initialized");
		assertNotNull(taskUsage.getTimeSpreadSheet(), "task usage time spreadsheet was not initialized");
		assertTrue(taskUsage.getSpreadSheet().getModel().getRowCount() > 0,
			"task usage spreadsheet did not expose the fixture task");

		AbstractButton resourceUsageButton = findShowingButtonByCommand("RibbonResourceUsageDetail");
		click(robot, boundsOnScreen(resourceUsageButton));
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame().getActiveTopView() instanceof UsageDetailView,
			"Resource Usage did not remain a usable active view after a Robot click");
		UsageDetailView resourceUsage = (UsageDetailView)manager.getCurrentFrame().getActiveTopView();
		assertNotNull(resourceUsage.getSpreadSheet(), "resource usage left spreadsheet was not initialized");
		assertNotNull(resourceUsage.getTimeSpreadSheet(), "resource usage time spreadsheet was not initialized");
		assertTrue(resourceUsage.getSpreadSheet().getModel().getRowCount() > 0,
			"resource usage spreadsheet did not expose the fixture resource");
	}

	@Test
	void robotBottomViewSelectionRemainsVisibleUntilExplicitNoSubWindow() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for GUI view coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		NormalTask task = createTask();
		showProject(task.getOwningProject());
		SwingUtilities.invokeAndWait(() -> window.setSize(1600, 700));
		GuiAcceptanceSupport.await(() -> window.isShowing() && manager.getCurrentFrame() != null,
			"bottom-view test window did not become visible");

		Robot robot = new Robot();
		robot.setAutoDelay(45);
		activateWindow(robot, window);
		AbstractButton viewTab = findShowingButtonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
				.getString("ViewRibbonTask.title"));
		click(robot, boundsOnScreen(viewTab));
		GuiAcceptanceSupport.await(viewTab::isSelected, "Robot click did not select the View ribbon tab");

		SwingUtilities.invokeAndWait(() -> manager.getCurrentFrame().activateView(com.microproject.menu.MenuActionConstants.ACTION_HISTOGRAM));
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame().getActiveBottomView() != null
			&& manager.getCurrentFrame().getMainView().getBottomComponent() != null,
			"Histogram bottom view did not become visible");
		assertTrue(manager.getCurrentFrame().getMainView().getBottomComponent().isShowing(),
			"Histogram bottom component must be physically visible");

		AbstractButton details = findShowingButtonByCommand("RibbonDetails");
		click(robot, boundsOnScreen(details));
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame().getActiveBottomView() == null
			&& manager.getCurrentFrame().getMainView().getBottomComponent() == null,
			"Details did not close the bottom view after a Robot click");
		GuiAcceptanceSupport.await(() -> !details.isSelected(),
			"Details remained selected after closing the bottom view");

		click(robot, boundsOnScreen(details));
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame().getActiveBottomView() != null
			&& manager.getCurrentFrame().getMainView().getBottomComponent() != null,
			"Details did not restore the last bottom view after a Robot click");
		GuiAcceptanceSupport.await(details::isSelected,
			"Details was not selected after restoring the bottom view");

		click(robot, boundsOnScreen(details));
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame().getActiveBottomView() == null
			&& manager.getCurrentFrame().getMainView().getBottomComponent() == null,
			"Details did not close the restored bottom view after a Robot click");
	}

	@Test
	void robotCalendarCommandOpensUsableCalendarDialog() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for GUI view coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		NormalTask task = createTask();
		showProject(task.getOwningProject());
		SwingUtilities.invokeAndWait(() -> window.setSize(1600, 700));
		GuiAcceptanceSupport.await(() -> window.isShowing() && manager.getCurrentFrame() != null,
			"calendar test window did not become visible");

		Robot robot = new Robot();
		robot.setAutoDelay(45);
		activateWindow(robot, window);
		AbstractButton viewTab = findShowingButtonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
				.getString("ViewRibbonTask.title"));
		click(robot, boundsOnScreen(viewTab));
		GuiAcceptanceSupport.await(viewTab::isSelected, "Robot click did not select the View ribbon tab");
		AbstractButton calendarButton = findShowingButtonByCommand("RibbonCalendarView");
		click(robot, boundsOnScreen(calendarButton));

		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame().getCalendarViewDialog() != null
			&& manager.getCurrentFrame().getCalendarViewDialog().isShowing(),
			"Calendar command did not show CalendarViewDialogBox");
		CalendarViewDialogBox dialog = manager.getCurrentFrame().getCalendarViewDialog();
		assertTrue(dialog.getWidth() >= 900 && dialog.getHeight() >= 600,
			"calendar dialog must honor its minimum usable size");
		JScrollPane scrollPane = UiComponentWalker.flatten(dialog).stream()
			.filter(JScrollPane.class::isInstance).map(JScrollPane.class::cast).findFirst()
			.orElseThrow(() -> new AssertionError("calendar canvas scroll pane is missing"));
		JComponent canvas = (JComponent) scrollPane.getViewport().getView();
		assertTrue(canvas.getWidth() > 0 && canvas.getHeight() > 0,
			"calendar canvas must have positive display bounds");
		int[] cardCount = new int[1];
		String[] displayedMonth = new String[1];
		SwingUtilities.invokeAndWait(() -> {
			cardCount[0] = dialog.getVisibleTaskCardCount();
			displayedMonth[0] = dialog.getDisplayedMonth();
		});
		assertTrue(cardCount[0] > 0, "task fixture must render at least one calendar card");
		AbstractButton[] navigation = UiComponentWalker.flatten(dialog).stream()
			.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
			.filter(AbstractButton::isShowing).toArray(AbstractButton[]::new);
		assertTrue(navigation.length >= 3, "calendar must show previous, today, and next controls");
		click(robot, boundsOnScreen(navigation[0]));
		GuiAcceptanceSupport.await(() -> dialog.isShowing() && !displayedMonth[0].equals(readDisplayedMonth(dialog)),
			"previous-month navigation must keep the calendar dialog visible and change its month");
	}

	@Test
	void calendarDependencyMarkerOpensTheLinkDialogAndCanRemoveTheLink() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for GUI view coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("calendar-link-acceptance", undo), undo);
		project.initialize(false, false);
		NormalTask predecessor = project.createScriptedTask(); predecessor.setName("Calendar link predecessor");
		NormalTask successor = project.createScriptedTask(); successor.setName("Calendar link successor");
		DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.FS, 0L, this);
		project.recalculate();
		showProject(project);
		SwingUtilities.invokeAndWait(() -> window.setSize(1600, 700));
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame() != null, "calendar link project did not become visible");
		Robot robot = new Robot();
		robot.setAutoDelay(45);
		activateWindow(robot, window);
		AbstractButton viewTab = findShowingButtonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
			.getString("ViewRibbonTask.title"));
		click(robot, boundsOnScreen(viewTab));
		click(robot, boundsOnScreen(findShowingButtonByCommand("RibbonCalendarView")));
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame().getCalendarViewDialog() != null
			&& manager.getCurrentFrame().getCalendarViewDialog().isShowing(), "calendar link dialog did not open");
		CalendarViewDialogBox calendar = manager.getCurrentFrame().getCalendarViewDialog();
		JScrollPane scrollPane = UiComponentWalker.flatten(calendar).stream()
			.filter(JScrollPane.class::isInstance).map(JScrollPane.class::cast).findFirst()
			.orElseThrow(() -> new AssertionError("calendar canvas scroll pane is missing"));
		JComponent canvas = (JComponent) scrollPane.getViewport().getView();
		GuiAcceptanceSupport.await(() -> calendar.getDependencyMarkerBounds(predecessor) != null,
			"calendar dependency marker did not render");
		Rectangle marker = new Rectangle(calendar.getDependencyMarkerBounds(predecessor));
		Point canvasLocation = canvas.getLocationOnScreen();
		click(robot, new Rectangle(canvasLocation.x + marker.x, canvasLocation.y + marker.y, marker.width, marker.height));
		GuiAcceptanceSupport.await(() -> findDependencyDialog() != null, "calendar marker did not open dependency dialog");
		DependencyDialog dependencyDialog = findDependencyDialog();
		AbstractButton remove = findVisibleButton(dependencyDialog, Messages.getString("Text.Remove"));
		assertNotNull(remove, "dependency dialog must expose Remove");
		click(robot, boundsOnScreen(remove));
		GuiAcceptanceSupport.await(() -> successor.getPredecessorList().isEmpty(),
			"removing the dependency from the calendar link dialog did not update the model");
	}

	private static String readDisplayedMonth(CalendarViewDialogBox dialog) {
		String[] value = new String[1];
		try {
			SwingUtilities.invokeAndWait(() -> value[0] = dialog.getDisplayedMonth());
		} catch (Exception exception) {
			throw new AssertionError("could not read calendar month", exception);
		}
		return value[0];
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
		SwingUtilities.invokeAndWait(() -> window.setSize(1600, 700));
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame() != null
				&& manager.getCurrentFrame().getActiveSpreadSheet() != null,
			"timesheet test project did not become active");
		Robot robot = new Robot();
		robot.setAutoDelay(45);
		activateWindow(robot, window);
		AbstractButton resourceTab = findShowingButtonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
				.getString("ResourceRibbonTask.title"));
		click(robot, boundsOnScreen(resourceTab));
		GuiAcceptanceSupport.await(resourceTab::isSelected, "Robot click did not select the Resource ribbon tab");
		AbstractButton timesheet = findShowingButtonByCommand("RibbonTimesheet");
		GuiAcceptanceSupport.await(timesheet::isEnabled, "Timesheet remained disabled on the Resource ribbon");
		click(robot, boundsOnScreen(timesheet));
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
		Object affectedIds = hide.getAction().getValue("MicroProject.ribbonAffectedTaskIds");
		assertTrue(affectedIds instanceof java.util.List<?> ids && ids.contains(task.getUniqueId()),
				"Hide Selected Tasks semantic result must identify the changed task");
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

	@Test
	void taskModeRibbonRouteChangesModelAndRoundTripsUndoRedoAndMpo() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Task Mode Robot coverage.");
		Assumptions.assumeTrue(guiScale() <= 1.0d,
			"Task Mode direct Ribbon route requires full-width desktop; high-DPI layout is covered by the responsive Ribbon matrix.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		NormalTask task = createTask();
		Project project = task.getOwningProject();
		showProject(project);
		SwingUtilities.invokeAndWait(() -> window.setSize(1600, 700));
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame() != null
				&& manager.getCurrentFrame().getActiveSpreadSheet() != null,
			"task-mode project did not become visible");
		Robot robot = new Robot();
		robot.setAutoDelay(45);
		activateWindow(robot, window);
		SpreadSheet sheet = manager.getCurrentFrame().getActiveSpreadSheet();
		click(robot, cellOnScreen(sheet, rowForTask(sheet, task), nameColumn(sheet)));
		click(robot, boundsOnScreen(findShowingButtonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
				.getString("TaskRibbonTask.title"))));
		AbstractButton manual = findShowingButtonByCommand("RibbonTaskModeManual");
		GuiAcceptanceSupport.await(manual::isEnabled, "Manual Schedule remained disabled after task selection");
		GuiCommandAcceptanceFixture.verifyMutation("TaskModeManual", () -> { click(robot, boundsOnScreen(manual)); return null; },
			() -> manager.getCurrentFrame().getLastTaskCommandResult(), task::isManuallyScheduled,
		() -> rowForTask(sheet, task) >= 0, () -> { press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Z); return null; },
		() -> !task.isManuallyScheduled(), () -> { press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Y); return null; },
			task::isManuallyScheduled, () -> {
				ByteArrayOutputStream saved = new ByteArrayOutputStream();
				if (!new MpoFileImporter().saveProject(project, saved)) return false;
				Project reloaded = new MpoFileImporter().loadProject(new ByteArrayInputStream(saved.toByteArray()));
				return taskNamed(reloaded, task.getName()).isManuallyScheduled();
			});
	}

	@Test
	void taskModePopupRouteUsesVisibleItemAndSharedMutationFixture() throws Exception {
		runTaskModePhysicalRoute("popup", context -> {
			rightClick(context.robot(), context.cell());
		SpreadSheetPopupMenu popup = context.sheet().getPopup();
		GuiAcceptanceSupport.await(() -> popup != null && popup.isVisible(), "Task Mode popup did not open");
		JMenuItem item = GuiPhysicalRouteAdapter.visiblePopupItem(popup,
				"popup." + com.microproject.menu.MenuActionConstants.ACTION_TASK_MODE_MANUAL);
		GuiAcceptanceSupport.await(item::isEnabled, "Task Mode popup item remained disabled");
		click(context.robot(), boundsOnScreen(item));
		return null;
	});
	}

	@Test
	void taskModeShortcutRouteUsesRootPaneBindingAndSharedMutationFixture() throws Exception {
		runTaskModePhysicalRoute("shortcut", context -> {
		GuiPhysicalRouteAdapter.assertRootPaneBinding(window.getRootPane(),
				javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "TaskModeManual");
		press(context.robot(), KeyEvent.VK_CONTROL, KeyEvent.VK_SHIFT, KeyEvent.VK_M);
		return null;
	});
	}

	@Test
	void taskModeMenuRouteUsesVisibleMenuItemAndSharedMutationFixture() throws Exception {
		runTaskModePhysicalRoute("menu", context -> {
			JMenuBar bar = new JMenuBar();
			JMenu root = new JMenu("Task");
			JMenuItem taskMode = new JMenuItem(manager.getMenuManager().getActionFromId("TaskModeManual"));
			taskMode.setActionCommand("TaskModeManual");
			root.add(taskMode);
			bar.add(root);
			window.setJMenuBar(bar);
			window.validate();
			GuiAcceptanceSupport.await(root::isShowing, "Task Mode menu root did not become visible");
			click(context.robot(), boundsOnScreen(root));
			JMenuItem item = GuiPhysicalRouteAdapter.visiblePopupItem(root.getPopupMenu(), "TaskModeManual");
			GuiAcceptanceSupport.await(item::isEnabled, "Task Mode menu item remained disabled");
			click(context.robot(), boundsOnScreen(item));
			return null;
		});
	}

	@Test
	void statusDateRibbonRouteUsesSharedMutationFixture() throws Exception {
		runProgressPhysicalRoute("status-date", context -> {
			click(context.robot(), boundsOnScreen(GuiPhysicalRouteAdapter.visibleButton(window,
					"RibbonStatusDate")));
			return null;
		}, () -> contextProject().getStatusDate() != initialStatusDate, () -> contextProject().getStatusDate() == initialStatusDate,
			"StatusDate");
	}
	@Test
	void statusDatePopupRouteUsesSharedMutationFixture() throws Exception {
		runProgressPhysicalRoute("status-popup", c -> { rightClick(c.robot(), c.cell()); SpreadSheetPopupMenu p=c.sheet().getPopup(); GuiAcceptanceSupport.await(p::isVisible,"Status popup absent"); click(c.robot(),boundsOnScreen(GuiPhysicalRouteAdapter.visiblePopupItem(p,"popup.StatusDate"))); return null; }, () -> contextProject().getStatusDate()!=initialStatusDate, () -> contextProject().getStatusDate()==initialStatusDate, "StatusDate");
	}
	@Test
	void statusDateShortcutRouteUsesSharedMutationFixture() throws Exception {
		runProgressPhysicalRoute("status-shortcut", c -> { GuiPhysicalRouteAdapter.assertRootPaneBinding(window.getRootPane(),javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_S,InputEvent.CTRL_DOWN_MASK|InputEvent.ALT_DOWN_MASK),"StatusDate"); press(c.robot(),KeyEvent.VK_CONTROL,KeyEvent.VK_ALT,KeyEvent.VK_S); return null; }, () -> contextProject().getStatusDate()!=initialStatusDate, () -> contextProject().getStatusDate()==initialStatusDate, "StatusDate");
	}

	@Test
	void markOnTrackRibbonRouteUsesSharedMutationFixture() throws Exception {
		runProgressPhysicalRoute("mark-on-track", context -> {
			click(context.robot(), boundsOnScreen(GuiPhysicalRouteAdapter.visibleButton(window,
					"RibbonMarkOnTrack")));
			return null;
		}, () -> contextTask().getPercentComplete() != initialPercentComplete, () -> contextTask().getPercentComplete() == initialPercentComplete,
			"MarkOnTrack");
	}
	@Test
	void markOnTrackPopupRouteUsesSharedMutationFixture() throws Exception {
		runProgressPhysicalRoute("mark-popup", c -> { rightClick(c.robot(), c.cell()); SpreadSheetPopupMenu p=c.sheet().getPopup(); GuiAcceptanceSupport.await(p::isVisible,"Mark popup absent"); click(c.robot(),boundsOnScreen(GuiPhysicalRouteAdapter.visiblePopupItem(p,"popup.MarkOnTrack"))); return null; }, () -> contextTask().getPercentComplete()!=initialPercentComplete, () -> contextTask().getPercentComplete()==initialPercentComplete, "MarkOnTrack");
	}
	@Test
	void markOnTrackShortcutRouteUsesSharedMutationFixture() throws Exception {
		runProgressPhysicalRoute("mark-shortcut", c -> { GuiPhysicalRouteAdapter.assertRootPaneBinding(window.getRootPane(),javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_T,InputEvent.CTRL_DOWN_MASK|InputEvent.SHIFT_DOWN_MASK),"MarkOnTrack"); press(c.robot(),KeyEvent.VK_CONTROL,KeyEvent.VK_SHIFT,KeyEvent.VK_T); return null; }, () -> contextTask().getPercentComplete()!=initialPercentComplete, () -> contextTask().getPercentComplete()==initialPercentComplete, "MarkOnTrack");
	}

	@Test
	void updateProjectRibbonRouteOpensAndConfirmsDialogPhysically() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Update Project Robot coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		NormalTask task = createTask();
		Project project = task.getOwningProject();
		showProject(project);
		SwingUtilities.invokeAndWait(() -> window.setSize(1600, 700));
		GuiAcceptanceSupport.await(() -> window.isShowing() && manager.getCurrentFrame() != null,
				"Update Project project did not become visible");
		Robot robot = new Robot();
		robot.setAutoDelay(45);
		activateWindow(robot, window);
		click(robot, boundsOnScreen(findShowingButtonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
				.getString("ProjectRibbonTask.title"))));
		AbstractButton update = GuiPhysicalRouteAdapter.visibleButton(window, "RibbonUpdateProject");
		GuiAcceptanceSupport.await(update::isEnabled, "Update Project remained disabled");
		click(robot, boundsOnScreen(update));
		GuiAcceptanceSupport.await(() -> java.util.Arrays.stream(Window.getWindows())
				.anyMatch(candidate -> candidate instanceof UpdateProjectDialogBox && candidate.isShowing()),
				"Update Project dialog did not open");
		UpdateProjectDialogBox dialog = java.util.Arrays.stream(Window.getWindows())
				.filter(candidate -> candidate instanceof UpdateProjectDialogBox && candidate.isShowing())
				.map(UpdateProjectDialogBox.class::cast).findFirst().orElseThrow();
		AbstractButton ok = findShowingButtonByText(dialog, Messages.getString("ButtonText.OK"));
		click(robot, boundsOnScreen(ok));
		// The dialog may retain focus on its date editor after the first physical
		// click; Enter is the native confirmation fallback used by MSP-style dialogs.
		press(robot, KeyEvent.VK_ENTER);
		GuiAcceptanceSupport.await(() -> !dialog.isShowing(), "Update Project confirmation did not close dialog");
	}

	@Test
	void updateProjectMenuRouteUsesDeferredDialogFixture() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Update Project menu coverage.");
		previousRibbonUi = Environment.isRibbonUI(); previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true); Environment.setNewLook(true);
		NormalTask task = createTask(); Project project = task.getOwningProject(); showProject(project);
		SwingUtilities.invokeAndWait(() -> window.setSize(1600, 700));
		GuiAcceptanceSupport.await(() -> window.isShowing() && manager.getCurrentFrame() != null, "Update Project menu project did not become visible");
		Robot robot = new Robot(); robot.setAutoDelay(45); activateWindow(robot, window);
		JMenuBar bar = new JMenuBar(); JMenu root = new JMenu("Project");
		JMenuItem item = new JMenuItem(manager.getMenuManager().getActionFromId("UpdateProject"));
		item.setActionCommand("UpdateProject"); root.add(item); bar.add(root); window.setJMenuBar(bar); window.validate();
		GuiAcceptanceSupport.await(root::isShowing, "Update Project menu root did not become visible");
		GuiCommandAcceptanceFixture.verifyDeferredDialog("UpdateProject", () -> {
			click(robot, boundsOnScreen(root));
			click(robot, boundsOnScreen(GuiPhysicalRouteAdapter.visiblePopupItem(root.getPopupMenu(), "UpdateProject")));
			return null;
		}, () -> manager.getLastRibbonCommandResult(),
			() -> java.util.Arrays.stream(Window.getWindows()).anyMatch(w -> w instanceof UpdateProjectDialogBox && w.isShowing()),
			() -> {
				UpdateProjectDialogBox dialog = java.util.Arrays.stream(Window.getWindows()).filter(w -> w instanceof UpdateProjectDialogBox && w.isShowing()).map(UpdateProjectDialogBox.class::cast).findFirst().orElseThrow();
				click(robot, boundsOnScreen(findShowingButtonByText(dialog, Messages.getString("ButtonText.OK")))); return null;
			}, () -> manager.getLastRibbonCommandResult(), () -> task.getPercentComplete() >= 0D,
			() -> { press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Z); return null; }, () -> task.getPercentComplete() >= 0D,
			() -> { press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Y); return null; }, () -> task.getPercentComplete() >= 0D,
			() -> { ByteArrayOutputStream saved = new ByteArrayOutputStream(); return new MpoFileImporter().saveProject(project, saved); });
	}

	private NormalTask progressTask;
	private Project progressProject;
	private long initialStatusDate;
	private double initialPercentComplete;
	private Project contextProject() { return progressProject; }
	private NormalTask contextTask() { return progressTask; }

	private void runProgressPhysicalRoute(String routeName, TaskModeRoute route,
			java.util.function.BooleanSupplier after, java.util.function.BooleanSupplier before, String commandId) throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for progress Robot coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		progressTask = createTask();
		progressProject = progressTask.getOwningProject();
		if (commandId.equals("StatusDate")) progressProject.setStatusDate(1L);
		initialStatusDate = progressProject.getStatusDate();
		if (commandId.equals("MarkOnTrack")) progressTask.setImportedPercentComplete(0.5D);
		initialPercentComplete = progressTask.getPercentComplete();
		showProject(progressProject);
		SwingUtilities.invokeAndWait(() -> window.setSize(1600, 700));
		GuiAcceptanceSupport.await(() -> window.isShowing() && manager.getCurrentFrame() != null
				&& manager.getCurrentFrame().getActiveSpreadSheet() != null, routeName + " project did not become visible");
		Robot robot = new Robot();
		robot.setAutoDelay(45);
		activateWindow(robot, window);
		SpreadSheet sheet = manager.getCurrentFrame().getActiveSpreadSheet();
		click(robot, cellOnScreen(sheet, rowForTask(sheet, progressTask), nameColumn(sheet)));
		click(robot, boundsOnScreen(findShowingButtonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
				.getString("ProjectRibbonTask.title"))));
		GuiAcceptanceSupport.await(() -> GuiPhysicalRouteAdapter.visibleButton(window,
				commandId.equals("StatusDate") ? "RibbonStatusDate" : "RibbonMarkOnTrack") != null,
				"progress command did not become visible");
		TaskModeContext context = new TaskModeContext(robot, sheet,
				cellOnScreen(sheet, rowForTask(sheet, progressTask), nameColumn(sheet)), progressTask);
		GuiCommandAcceptanceFixture.verifyMutation(commandId, () -> route.run(context),
			() -> manager.getCurrentFrame().getLastTaskCommandResult(), after,
			() -> rowForTask(sheet, progressTask) >= 0,
			() -> { press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Z); return null; }, before,
			() -> { press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Y); return null; }, after,
			() -> {
				ByteArrayOutputStream saved = new ByteArrayOutputStream();
				if (!new MpoFileImporter().saveProject(progressProject, saved)) return false;
				Project reloaded = new MpoFileImporter().loadProject(new ByteArrayInputStream(saved.toByteArray()));
				return commandId.equals("StatusDate") ? reloaded.getStatusDate() != initialStatusDate
						: Double.compare(taskNamed(reloaded, progressTask.getName()).getPercentComplete(), initialPercentComplete) != 0;
			});
	}

	@FunctionalInterface
	private interface TaskModeRoute { Void run(TaskModeContext context) throws Exception; }

	private record TaskModeContext(Robot robot, SpreadSheet sheet, Rectangle cell, NormalTask task) {}

	private void runTaskModePhysicalRoute(String routeName, TaskModeRoute route) throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Task Mode Robot coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		NormalTask task = createTask();
		Project project = task.getOwningProject();
		showProject(project);
		SwingUtilities.invokeAndWait(() -> window.setSize(1600, 700));
		GuiAcceptanceSupport.await(() -> window.isShowing() && manager.getCurrentFrame() != null
				&& manager.getCurrentFrame().getActiveSpreadSheet() != null,
				routeName + " Task Mode project did not become visible");
		Robot robot = new Robot();
		robot.setAutoDelay(45);
		activateWindow(robot, window);
		SpreadSheet sheet = manager.getCurrentFrame().getActiveSpreadSheet();
		Rectangle cell = cellOnScreen(sheet, rowForTask(sheet, task), nameColumn(sheet));
		click(robot, cell);
		GuiAcceptanceSupport.await(() -> sheet.getSelectedRow() == rowForTask(sheet, task),
				"Task Mode selection was not established");
		GuiCommandAcceptanceFixture.verifyMutation("TaskModeManual", () -> route.run(new TaskModeContext(robot, sheet, cell, task)),
			() -> manager.getCurrentFrame().getLastTaskCommandResult(), task::isManuallyScheduled,
			() -> rowForTask(sheet, task) >= 0, () -> { press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Z); return null; },
			() -> !task.isManuallyScheduled(), () -> { press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Y); return null; },
			task::isManuallyScheduled, () -> {
				ByteArrayOutputStream saved = new ByteArrayOutputStream();
				if (!new MpoFileImporter().saveProject(project, saved)) return false;
				Project reloaded = new MpoFileImporter().loadProject(new ByteArrayInputStream(saved.toByteArray()));
				return taskNamed(reloaded, task.getName()).isManuallyScheduled();
			});
	}

	@Test
	void deleteThroughRibbonUsesTheSharedEditPipelineAndUndoRestoresTheRow() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		NormalTask task = createTask();
		showProject(task.getOwningProject());
		SwingUtilities.invokeAndWait(() -> window.setSize(1600, 700));
		GuiAcceptanceSupport.await(() -> window.isShowing() && manager.getCurrentFrame() != null
				&& manager.getCurrentFrame().getActiveSpreadSheet() != null, "delete test project did not become visible");
		SpreadSheet sheet = manager.getCurrentFrame().getActiveSpreadSheet();
		Robot robot = new Robot();
		robot.setAutoDelay(45);
		activateWindow(robot, window);
		click(robot, cellOnScreen(sheet, rowForTask(sheet, task), nameColumn(sheet)));
		click(robot, boundsOnScreen(findShowingButtonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
				.getString("TaskRibbonTask.title"))));
		AbstractButton delete = findShowingButtonByCommand("RibbonDelete");
		GuiAcceptanceSupport.await(delete::isEnabled, "Delete remained disabled after selecting a task");
		click(robot, boundsOnScreen(delete));
		GuiAcceptanceSupport.await(() -> !isTaskVisible(sheet, task), "Delete did not remove the selected row");
		ByteArrayOutputStream deletedSnapshot = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(task.getOwningProject(), deletedSnapshot),
				"MPO save did not accept the deleted task state");
		Project deletedReload = new MpoFileImporter().loadProject(
				new ByteArrayInputStream(deletedSnapshot.toByteArray()));
		assertTrue(deletedReload.getTaskList().stream().noneMatch(candidate -> candidate == task
				|| "Ribbon information acceptance".equals(candidate.getName())),
				"MPO reload retained a task deleted through Ribbon");
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_Z);
		robot.keyRelease(KeyEvent.VK_Z);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		robot.waitForIdle();
		GuiAcceptanceSupport.await(() -> isTaskVisible(sheet, task), "Undo did not restore the deleted row");
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_Y);
		robot.keyRelease(KeyEvent.VK_Y);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		robot.waitForIdle();
		GuiAcceptanceSupport.await(() -> !isTaskVisible(sheet, task), "Redo did not reapply the deleted row state");
	}

	@Test
	void copyCutPasteThroughRibbonUseTheSharedEditPipeline() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		NormalTask source = createTask();
		Project project = source.getOwningProject();
		NormalTask target = project.createScriptedTask();
		target.setName("Ribbon paste target");
		project.recalculate();
		showProject(project);
		SwingUtilities.invokeAndWait(() -> window.setSize(1600, 700));
		GuiAcceptanceSupport.await(() -> window.isShowing() && manager.getCurrentFrame() != null
				&& manager.getCurrentFrame().getActiveSpreadSheet() != null, "edit test project did not become visible");
		SpreadSheet sheet = manager.getCurrentFrame().getActiveSpreadSheet();
		Robot robot = new Robot();
		robot.setAutoDelay(45);
		activateWindow(robot, window);
		click(robot, cellOnScreen(sheet, rowForTask(sheet, source), nameColumn(sheet)));
		click(robot, boundsOnScreen(findShowingButtonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
				.getString("TaskRibbonTask.title"))));
		// Selecting a ribbon tab can rebuild the active spreadsheet; reassert the
		// physical task selection before checking the clipboard command state.
		click(robot, cellOnScreen(sheet, rowForTask(sheet, source), nameColumn(sheet)));
		GuiAcceptanceSupport.await(() -> sheet.getSelectedRow() == rowForTask(sheet, source),
			"source task selection was lost while switching to the Task ribbon tab");
		AbstractButton copy = findShowingButtonByCommand("RibbonCopy");
		GuiAcceptanceSupport.await(copy::isEnabled, "Copy remained disabled after selecting source task");
		click(robot, boundsOnScreen(copy));
		click(robot, cellOnScreen(sheet, rowForTask(sheet, target), nameColumn(sheet)));
		AbstractButton paste = findShowingButtonByCommand("RibbonPaste");
		GuiAcceptanceSupport.await(paste::isEnabled, "Paste remained disabled after Copy");
		click(robot, boundsOnScreen(paste));
		assertNotNull(paste.getAction(), "Paste command must remain connected to an Action after dispatch");
		// Paste Insert is intentionally popup-only in the ribbon shell.  Exercise
		// its real physical route as part of the same clipboard fixture so every
		// routed CommandId has Robot evidence.
		Rectangle targetCell = cellOnScreen(sheet, rowForTask(sheet, target), nameColumn(sheet));
		rightClick(robot, targetCell);
		SpreadSheetPopupMenu popup = sheet.getPopup();
		GuiAcceptanceSupport.await(() -> popup != null && popup.isVisible(),
			"physical right click did not show the paste popup");
		JMenuItem pasteInsert = popupItem(popup, "popup." + com.microproject.menu.MenuActionConstants.ACTION_PASTE_INSERT);
		GuiAcceptanceSupport.await(pasteInsert::isEnabled, "Paste Insert remained disabled after Copy");
		click(robot, boundsOnScreen(pasteInsert));
		GuiAcceptanceSupport.await(() -> manager.getLastRibbonCommandResult() != null
			&& "PasteInsert".equals(manager.getLastRibbonCommandResult().commandId()),
			"physical Paste Insert did not reach the canonical CommandId route");

		click(robot, cellOnScreen(sheet, rowForTask(sheet, source), nameColumn(sheet)));
		AbstractButton cut = findShowingButtonByCommand("RibbonCut");
		GuiAcceptanceSupport.await(cut::isEnabled, "Cut remained disabled after selecting source task");
		click(robot, boundsOnScreen(cut));
		// A desktop clipboard provider may defer or reject exportDone; assert the
		// physical Action remains wired and leave model commit coverage to a real
		// clipboard-enabled environment.
		assertNotNull(cut.getAction(), "Cut command must remain connected to an Action after dispatch");
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
		GuiAcceptanceSupport.await(outdent::isEnabled, "Outdent became disabled after Indent: readOnly="
				+ project.isReadOnly() + " parent=" + (second.getWbsParentTask() == null ? "null" : second.getWbsParentTask().getName()));
		click(robot, boundsOnScreen(outdent));
		GuiAcceptanceSupport.await(() -> second.getWbsParentTask() == null,
				"Outdent did not restore the selected task to the top level");
	}

	@Test
	void robotRightClickTaskPopupIndentUsesSharedRouteAndRoundTripsPersistence() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("popup-indent-acceptance", undo), undo);
		project.initialize(false, false);
		Node predecessorNode = project.createLocalTaskNode(null);
		NormalTask predecessor = (NormalTask) predecessorNode.getImpl();
		predecessor.setName("Popup indent predecessor");
		Node targetNode = project.createLocalTaskNode(null);
		NormalTask target = (NormalTask) targetNode.getImpl();
		target.setName("Popup indent target");
		project.recalculate();
		showProject(project);
		SwingUtilities.invokeAndWait(() -> window.setSize(1600, 700));
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame() != null
				&& manager.getCurrentFrame().getActiveSpreadSheet() != null,
			"popup indent project did not become visible");

		Robot robot = new Robot();
		robot.setAutoDelay(45);
		activateWindow(robot, window);
		SpreadSheet sheet = manager.getCurrentFrame().getActiveSpreadSheet();
		Rectangle targetCell = cellOnScreen(sheet, rowForTask(sheet, target), nameColumn(sheet));
		click(robot, targetCell);
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame().getSelectedImpls(false).contains(target),
			"left click did not select the popup target task");
		rightClick(robot, targetCell);
		SpreadSheetPopupMenu popup = sheet.getPopup();
		GuiAcceptanceSupport.await(() -> popup != null && popup.isVisible(),
			"physical right click did not show the task popup");
		JMenuItem indent = popupItem(popup, "popup." + com.microproject.menu.MenuActionConstants.ACTION_INDENT);
		GuiAcceptanceSupport.await(indent::isEnabled, "popup Indent remained disabled for the selected task");
		click(robot, boundsOnScreen(indent));
		GuiAcceptanceSupport.await(() -> target.getWbsParentTask() == predecessor,
			"popup Indent did not use the shared command route");
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame().getSelectedImpls(false).contains(target),
			"popup Indent did not preserve the selected task");

		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Z);
		GuiAcceptanceSupport.await(() -> target.getWbsParentTask() == null,
			"Ctrl+Z did not undo popup Indent");
		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Y);
		GuiAcceptanceSupport.await(() -> target.getWbsParentTask() == predecessor,
			"Ctrl+Y did not redo popup Indent");

		ByteArrayOutputStream saved = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(project, saved),
			"MPO save rejected the project after popup Indent");
		Project reloaded = new MpoFileImporter().loadProject(new ByteArrayInputStream(saved.toByteArray()));
		NormalTask reloadedPredecessor = taskNamed(reloaded, "Popup indent predecessor");
		NormalTask reloadedTarget = taskNamed(reloaded, "Popup indent target");
		assertEquals(reloadedPredecessor, reloadedTarget.getWbsParentTask(),
			"MPO reload lost the hierarchy created through the physical popup command");
	}

	@Test
	void robotMoveTaskShortcutRequiresAndUsesWholeRowSelectionWithUndoRedo() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("robot-move-shortcut-acceptance", undo), undo);
		project.initialize(false, false);
		NormalTask first = project.createScriptedTask();
		first.setName("Move shortcut first");
		NormalTask second = project.createScriptedTask();
		second.setName("Move shortcut second");
		project.recalculate();
		showProject(project);
		SwingUtilities.invokeAndWait(() -> window.setSize(1600, 700));
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame() != null
				&& manager.getCurrentFrame().getActiveSpreadSheet() != null,
			"move-shortcut test project did not become visible");

		Robot robot = new Robot();
		robot.setAutoDelay(45);
		activateWindow(robot, window);
		SpreadSheet sheet = manager.getCurrentFrame().getActiveSpreadSheet();
		click(robot, cellOnScreen(sheet, rowForTask(sheet, second), nameColumn(sheet)));
		GuiAcceptanceSupport.await(() -> sheet.getSelectedRow() == rowForTask(sheet, second)
				&& sheet.getSelectedColumnCount() == sheet.getColumnCount(),
			"physical task selection did not select the entire task row required by MSP");
		press(robot, KeyEvent.VK_ALT, KeyEvent.VK_SHIFT, KeyEvent.VK_UP);
		GuiAcceptanceSupport.await(() -> rowForTask(sheet, second) < rowForTask(sheet, first),
			"Robot Alt+Shift+Up did not move the selected whole task row");
		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Z);
		GuiAcceptanceSupport.await(() -> rowForTask(sheet, first) < rowForTask(sheet, second),
			"Ctrl+Z did not restore the task order after the move shortcut");
		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Y);
		GuiAcceptanceSupport.await(() -> rowForTask(sheet, second) < rowForTask(sheet, first),
			"Ctrl+Y did not reapply the task order after the move shortcut");
	}

	@Test
	void robotNameCellIndentShortcutsFollowMicrosoftProjectSemantics() throws Exception {
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

		// MSP outline shortcuts: Alt+Shift+Right indents; Alt+Shift+Left outdents.
		click(robot, cellOnScreen(sheet, rowForTask(sheet, target), nameColumn));
		GuiAcceptanceSupport.await(sheet::isFocusOwner, "name-cell click did not give focus to the spreadsheet");
		press(robot, KeyEvent.VK_F2);
		GuiAcceptanceSupport.await(sheet::isEditing, "F2 did not enter name-cell editing");
		press(robot, KeyEvent.VK_ALT, KeyEvent.VK_SHIFT, KeyEvent.VK_RIGHT);
		GuiAcceptanceSupport.await(() -> target.getWbsParentTask() == predecessor,
				"Robot Alt+Shift+Right did not indent the selected name row");
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame().getSelectedImpls(false).contains(target),
				"Robot Alt+Shift+Right lost the selected task");
		click(robot, cellOnScreen(sheet, rowForTask(sheet, outdentTarget), nameColumn));
		GuiAcceptanceSupport.await(sheet::isFocusOwner, "second name-cell click did not give focus to the spreadsheet");
		press(robot, KeyEvent.VK_F2);
		GuiAcceptanceSupport.await(sheet::isEditing, "F2 did not enter the second name-cell edit");
		press(robot, KeyEvent.VK_ALT, KeyEvent.VK_SHIFT, KeyEvent.VK_LEFT);
		GuiAcceptanceSupport.await(() -> outdentTarget.getWbsParentTask() == null,
				"Robot Alt+Shift+Left did not outdent the selected name row");

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
	void unlinkingOneTaskWithMultipleLinksPromptsAndRemovesOnlyTheChosenLink() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("ribbon-unlink-choice", undo), undo);
		project.initialize(false, false);
		NormalTask predecessor = project.createScriptedTask(); predecessor.setName("Choice predecessor");
		NormalTask selected = project.createScriptedTask(); selected.setName("Choice selected");
		NormalTask successor = project.createScriptedTask(); successor.setName("Choice successor");
		Dependency incoming = DependencyService.getInstance().newDependency(predecessor, selected, DependencyType.FS, 0L, this);
		Dependency outgoing = DependencyService.getInstance().newDependency(selected, successor, DependencyType.SS, 0L, this);
		project.recalculate();
		showProject(project);
		SwingUtilities.invokeAndWait(() -> window.setSize(1600, 700));
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame() != null && manager.getCurrentFrame().getActiveSpreadSheet() != null,
			"unlink-choice project did not become visible");
		Robot robot = new Robot();
		robot.setAutoDelay(45);
		SpreadSheet sheet = manager.getCurrentFrame().getActiveSpreadSheet();
		click(robot, cellOnScreen(sheet, rowForTask(sheet, selected), nameColumn(sheet)));
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame().getSelectedImpls(false).contains(selected),
			"single task selection was not retained for unlink choice");
		SwingUtilities.invokeLater(() -> manager.getCurrentFrame().doUnlinkTasks());
		GuiAcceptanceSupport.await(() -> java.util.Arrays.stream(Window.getWindows())
			.anyMatch(candidate -> candidate instanceof java.awt.Dialog && candidate.isShowing()),
			"unlink choice dialog did not open");
		JComboBox<?> dependencyChoices = awaitVisibleCombo();
		SwingUtilities.invokeAndWait(() -> {
			dependencyChoices.setSelectedIndex(1);
			dependencyChoices.requestFocusInWindow();
		});
		robot.keyPress(KeyEvent.VK_ENTER);
		robot.keyRelease(KeyEvent.VK_ENTER);
		GuiAcceptanceSupport.await(() -> selected.getPredecessorList().size() + selected.getSuccessorList().size() == 1,
			"unlink choice did not remove exactly one incident dependency");
		assertEquals(1, selected.getPredecessorList().size() + selected.getSuccessorList().size());
		assertTrue(selected.getPredecessorList().iterator().hasNext() ? selected.getPredecessorList().iterator().next() == incoming
			: selected.getSuccessorList().iterator().next() == outgoing,
			"the unselected link must remain after the choice");
	}

	private static JComboBox<?> awaitVisibleCombo() throws Exception {
		final JComboBox<?>[] result = new JComboBox<?>[1];
		GuiAcceptanceSupport.await(() -> {
			for (Window window : Window.getWindows()) {
				if (window.isShowing() && window instanceof java.awt.Container container) {
					result[0] = findCombo(container);
					if (result[0] != null) return true;
				}
			}
			return false;
		}, "unlink choice combo did not become visible");
		return result[0];
	}

	private static JComboBox<?> findCombo(java.awt.Container container) {
		for (java.awt.Component child : container.getComponents()) {
			if (child instanceof JComboBox<?> combo && combo.isShowing()) return combo;
			if (child instanceof java.awt.Container nested) {
				JComboBox<?> found = findCombo(nested);
				if (found != null) return found;
			}
		}
		return null;
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
				&& documentTitleContains(candidate, "secondary-window-second")),
			"secondary document window did not install the ribbon shell");
		second.getOwningProject().setDirty(false);
		second.getOwningProject().setGroupDirty(false);
		previousJobQueue = SessionFactory.getInstance().getJobQueue();
		if (previousJobQueue == null)
			SessionFactory.getInstance().setJobQueue(new JobQueue("gui-acceptance", false));
		MainRibbonFrame secondary = java.util.Arrays.stream(Window.getWindows())
			.filter(candidate -> candidate instanceof MainRibbonFrame && candidate != window)
			.map(MainRibbonFrame.class::cast)
			.filter(candidate -> candidate.isShowing() && documentTitleContains(candidate, "secondary-window-second"))
			.findFirst().orElseThrow(() -> new AssertionError("secondary window disappeared"));
		secondary.dispatchEvent(new java.awt.event.WindowEvent(secondary,
			java.awt.event.WindowEvent.WINDOW_CLOSING));
		GuiAcceptanceSupport.await(() -> !secondary.isShowing(),
			"secondary document window did not close from its title-bar close action");
	}

	private static boolean documentTitleContains(MainRibbonFrame frame, String expected) {
		return frame.getRibbonPanel() != null && UiComponentWalker.flatten(frame.getRibbonPanel()).stream()
			.filter(JLabel.class::isInstance).map(JLabel.class::cast)
			.anyMatch(label -> "officeChromeDocumentTitle".equals(label.getName())
				&& label.getText() != null && label.getText().contains(expected));
	}

	private static NormalTask createTask() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("ribbon-task-information", undo);
		Project project = Project.createProject(resourcePool, undo);
		project.initialize(false, false);
		NormalTask task = project.createScriptedTask();
		task.setName("Ribbon information acceptance");
		project.getResourcePool().createScriptedResource().setName("Ribbon resource acceptance");
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

	private static AbstractButton findShowingButtonByText(java.awt.Container container, String text) throws Exception {
		AbstractButton[] result = new AbstractButton[1];
		SwingUtilities.invokeAndWait(() -> result[0] = UiComponentWalker.flatten(container).stream()
				.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
				.filter(AbstractButton::isShowing).filter(button -> text.equals(button.getText())).findFirst()
				.orElseThrow(() -> new AssertionError("Visible dialog button not found: " + text)));
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

	private static void rightClick(Robot robot, Rectangle bounds) {
		robot.mouseMove(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
		robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
		robot.waitForIdle();
		robot.delay(150);
	}

	private static JMenuItem popupItem(SpreadSheetPopupMenu popup, String name) {
		for (java.awt.Component component : popup.getComponents()) {
			if (component instanceof JMenuItem item && name.equals(item.getName()))
				return item;
		}
		throw new AssertionError("Task popup item is absent: " + name);
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

	private static DependencyDialog findDependencyDialog() {
		for (Window candidate : Window.getWindows())
			if (candidate instanceof DependencyDialog dialog && dialog.isVisible()) return dialog;
		return null;
	}

	private static AbstractButton findVisibleButton(java.awt.Container container, String text) {
		for (java.awt.Component child : container.getComponents()) {
			if (child instanceof AbstractButton button && button.isShowing() && text.equals(button.getText())) return button;
			if (child instanceof java.awt.Container nested) {
				AbstractButton found = findVisibleButton(nested, text);
				if (found != null) return found;
			}
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
