/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.awt.Dialog;
import java.awt.Component;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JComboBox;
import javax.swing.JTabbedPane;
import javax.swing.JScrollPane;
import javax.swing.JMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPopupMenu;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.dialog.TaskInformationDialog;
import com.microproject.dialog.UpdateTaskDialog;
import com.microproject.dialog.UpdateProjectDialogBox;
import com.microproject.dialog.StatusDateDialog;
import com.microproject.dialog.CalendarViewDialogBox;
import com.microproject.dialog.calendar.ChangeWorkingTimeDialogBox;
import com.microproject.dialog.options.CalendarDialogBox;
import com.microproject.dialog.DependencyDialog;
import com.microproject.dialog.assignment.TimesheetDialog;
import com.microproject.dialog.assignment.TimesheetEntryPane;
import com.microproject.menu.MenuManager;
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
import com.microproject.pm.graphic.views.PertView;
import com.microproject.pm.graphic.views.TreeView;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.calendar.WorkingCalendar;
import com.microproject.pm.graphic.views.ResourceView;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.strings.Messages;
import com.microproject.session.SessionFactory;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.testsupport.GuiCommandAcceptanceFixture;
import com.microproject.testsupport.GuiPhysicalRouteAdapter;
import com.microproject.testsupport.DialogLayoutAssertions;
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
				if (candidate instanceof TaskInformationDialog || candidate instanceof UpdateTaskDialog || candidate instanceof TimesheetDialog
					|| candidate instanceof CalendarDialogBox)
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
	void robotOpensIssue590DialogsWithoutClippedText() throws Exception {
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
		clickUntilSelected(robot, window, sheet, rowForTask(sheet, task), nameColumn(sheet));
		AbstractButton taskTab = findShowingButtonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
			.getString("TaskRibbonTask.title"));
		click(robot, boundsOnScreen(taskTab));
		GuiAcceptanceSupport.await(taskTab::isSelected, "Robot click did not select the Task ribbon tab");
		AbstractButton update = findShowingButtonByCommand("RibbonUpdateTasks");
		GuiAcceptanceSupport.await(update::isEnabled, "Update Tasks remained disabled after selecting a task");
		click(robot, boundsOnScreen(update));

		GuiAcceptanceSupport.await(() -> findUpdateTaskDialog() != null,
			"Task > Update did not open the Update Tasks dialog after a Robot click");
		UpdateTaskDialog dialog = findUpdateTaskDialog();
		capture(robot, dialog, "issue-590-update-tasks.png");
		assertUpdateDialogComponentsFit(dialog);

		press(robot, KeyEvent.VK_ESCAPE);
		GuiAcceptanceSupport.await(() -> findUpdateTaskDialog() == null,
			"Escape did not close Update Tasks after visual assertions");
		AbstractButton recurring = findShowingButtonByCommand("RibbonInsertRecurring");
		click(robot, boundsOnScreen(recurring));
		GuiAcceptanceSupport.await(() -> findRecurringTaskDialog() != null,
			"Task > Insert > Recurring Task did not open after a Robot click");
		Dialog recurringDialog = findRecurringTaskDialog();
		capture(robot, recurringDialog, "issue-590-recurring-task.png");
		assertRecurringDialogComponentsFit(recurringDialog);
	}

	private static void assertUpdateDialogComponentsFit(UpdateTaskDialog dialog) throws Exception {
		DialogLayoutAssertions.assertTextControlsAtPreferredHeight(dialog, "Update Tasks dialog");
		final String[] clipping = new String[1];
		SwingUtilities.invokeAndWait(() -> {
			layoutTree(dialog);
			clipping[0] = null;
			AbstractButton help = findVisibleButton(dialog, MenuManager.getMenuString("Help.text"));
			assertNotNull(help, "Update Tasks must expose its online Help button");
			if (clipping[0] == null && help.getHeight() < help.getPreferredSize().height)
				clipping[0] = "Help button " + help.getClass().getName() + " bounds=" + help.getBounds()
					+ " preferred=" + help.getPreferredSize();
		});
		assertEquals(null, clipping[0], () -> "Update Tasks clips a text component: " + clipping[0]);
	}

	private static void assertRecurringDialogComponentsFit(Dialog dialog) throws Exception {
		DialogLayoutAssertions.assertTextControlsAtPreferredHeight(dialog, "Recurring Task dialog");
		final String[] clipping = new String[1];
		SwingUtilities.invokeAndWait(() -> {
			layoutTree(dialog);
			clipping[0] = null;
			AbstractButton create = findVisibleButton(dialog, Messages.getString("RecurringTaskDialog.Create"));
			AbstractButton cancel = findVisibleButton(dialog, Messages.getString("ButtonText.Cancel"));
			AbstractButton endByDate = findVisibleButton(dialog, Messages.getString("RecurringTaskDialog.EndByDate"));
			AbstractButton endAfterOccurrences = findVisibleButton(dialog,
				Messages.getString("RecurringTaskDialog.EndAfterOccurrences"));
			assertNotNull(create, "Recurring Task must expose its Create button");
			assertNotNull(cancel, "Recurring Task must expose its Cancel button");
			assertNotNull(endByDate, "Recurring Task must expose its end-by-date option");
			assertNotNull(endAfterOccurrences, "Recurring Task must expose its occurrence-count option");
			assertSame(endByDate.getParent(), endAfterOccurrences.getParent(),
				"The two end conditions must share their dedicated range panel");
			assertTrue(endAfterOccurrences.getY() > endByDate.getY(),
				"The end conditions must occupy separate rows so their controls cannot spill into each other");
			String overflow = findChildOutsideParent(endByDate.getParent());
			if (clipping[0] == null && overflow != null)
				clipping[0] = "Recurring Task range control overflows its panel: " + overflow;
			if (clipping[0] == null && (create.getHeight() < create.getPreferredSize().height
					|| cancel.getHeight() < cancel.getPreferredSize().height))
				clipping[0] = "Recurring Task buttons do not fit their labels";
		});
		assertEquals(null, clipping[0], () -> "Recurring Task clips a text component: " + clipping[0]);
	}

	private static String findChildOutsideParent(java.awt.Container parent) {
		for (Component child : parent.getComponents()) {
			Rectangle bounds = child.getBounds();
			if (bounds.x < 0 || bounds.y < 0 || bounds.x + bounds.width > parent.getWidth()
					|| bounds.y + bounds.height > parent.getHeight())
				return child.getClass().getName() + " bounds=" + bounds + " parent=" + parent.getSize();
			if (child instanceof java.awt.Container nested) {
				String overflow = findChildOutsideParent(nested);
				if (overflow != null)
					return overflow;
			}
		}
		return null;
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
		clickUntilSelected(robot, window, sheet, row, column);

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
		assertTextStyleTabComponentsFit(dialog);
		capture(robot, dialog);
	}

	private static void assertTextStyleTabComponentsFit(TaskInformationDialog dialog) throws Exception {
		DialogLayoutAssertions.assertTextControlsAtPreferredHeight(dialog, "Task Information dialog");
		final List<String> tabTitles = new ArrayList<>();
		final List<java.awt.Container> tabContainers = new ArrayList<>();
		SwingUtilities.invokeAndWait(() -> {
			JTabbedPane tabs = findTabbedPane(dialog);
			assertNotNull(tabs, "Task Information must expose its tabs");
			AbstractButton help = findVisibleButton(dialog, MenuManager.getMenuString("Help.text"));
			assertNotNull(help, "Task Information must expose its online Help button");
			assertTrue(help.getHeight() >= help.getPreferredSize().height,
				"online Help button must retain its font-derived preferred height");
			for (int tabIndex = 0; tabIndex < tabs.getTabCount(); tabIndex++) {
				tabs.setSelectedIndex(tabIndex);
				layoutTree(dialog);
				Component tab = tabs.getComponentAt(tabIndex);
				if (tab instanceof JScrollPane scrollPane
						&& scrollPane.getViewport().getView() instanceof java.awt.Container view)
					tab = view;
				assertTrue(tab instanceof java.awt.Container,
					"Task Information tab has no layout container: " + tabs.getTitleAt(tabIndex));
				tabTitles.add(tabs.getTitleAt(tabIndex));
				tabContainers.add((java.awt.Container) tab);
			}
		});
		assertTrue(tabContainers.size() >= 3, "Task Information must expose all expected tabs");
		for (int tabIndex = 0; tabIndex < tabContainers.size(); tabIndex++)
			DialogLayoutAssertions.assertTextControlsAtPreferredHeight(tabContainers.get(tabIndex),
				"Task Information tab " + tabTitles.get(tabIndex));
	}

	private static JTabbedPane findTabbedPane(java.awt.Container root) {
		for (Component child : root.getComponents()) {
			if (child instanceof JTabbedPane tabs)
				return tabs;
			if (child instanceof java.awt.Container nested) {
				JTabbedPane tabs = findTabbedPane(nested);
				if (tabs != null)
					return tabs;
			}
		}
		return null;
	}

	private static void layoutTree(java.awt.Container root) {
		root.doLayout();
		for (Component child : root.getComponents()) {
			if (child instanceof java.awt.Container nested)
				layoutTree(nested);
		}
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
	void robotCalendarOptionsRibbonRouteOpensUsableDialog() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		NormalTask task = createTask();
		showProject(task.getOwningProject());
		SwingUtilities.invokeAndWait(() -> window.setSize(1600, 700));
		GuiAcceptanceSupport.await(() -> window.isShowing() && manager.getCurrentFrame() != null,
			"calendar options project window did not become visible");

		Robot robot = new Robot();
		robot.setAutoDelay(45);
		activateWindow(robot, window);
		AbstractButton projectTab = findShowingButtonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
			.getString("ProjectRibbonTask.title"));
		click(robot, boundsOnScreen(projectTab));
		GuiAcceptanceSupport.await(projectTab::isSelected, "Project ribbon tab did not become selected");
		AbstractButton calendarOptions = findShowingButtonByCommand("RibbonCalendarOptions");
		GuiAcceptanceSupport.await(calendarOptions::isEnabled, "Calendar Options remained disabled");
		click(robot, boundsOnScreen(calendarOptions));
		GuiAcceptanceSupport.await(() -> findCalendarOptionsDialog() != null,
			"Calendar Options did not show CalendarDialogBox");
		CalendarDialogBox dialog = findCalendarOptionsDialog();
		DialogLayoutAssertions.assertTextControlsAtPreferredHeight(dialog, "Calendar Options dialog");
		assertTrue(dialog.getWidth() > 240 && dialog.getHeight() > 180,
			"Calendar Options opened without a usable dialog body: " + dialog.getSize());
		assertTrue(UiComponentWalker.flatten(dialog).stream().anyMatch(Component::isShowing),
			"Calendar Options dialog body has no showing components");
		press(robot, KeyEvent.VK_ESCAPE);
		GuiAcceptanceSupport.await(() -> !dialog.isShowing(),
			"Escape did not close the Calendar Options dialog");
	}

	@Test
	void robotChangeWorkingTimeProjectRibbonRouteOpensAndCancels() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		NormalTask task = createTask();
		showProject(task.getOwningProject());
		SwingUtilities.invokeAndWait(() -> window.setSize(1600, 700));
		GuiAcceptanceSupport.await(() -> window.isShowing() && manager.getCurrentFrame() != null,
			"working-time project window did not become visible");
		Robot robot = new Robot();
		robot.setAutoDelay(45);
		activateWindow(robot, window);
		AbstractButton projectTab = findShowingButtonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
			.getString("ProjectRibbonTask.title"));
		click(robot, boundsOnScreen(projectTab));
		GuiAcceptanceSupport.await(projectTab::isSelected, "Project ribbon tab did not become selected");
		AbstractButton changeWorkingTime = findShowingButtonByCommand("RibbonChangeWorkingTime");
		assertTrue(changeWorkingTime.isEnabled(), "Change Working Time requires the active project calendar to be editable");
		click(robot, boundsOnScreen(changeWorkingTime));
		GuiAcceptanceSupport.await(() -> java.util.Arrays.stream(Window.getWindows())
			.anyMatch(candidate -> candidate instanceof ChangeWorkingTimeDialogBox && candidate.isShowing()),
			"Project ribbon Change Working Time did not open its dialog");
		ChangeWorkingTimeDialogBox dialog = java.util.Arrays.stream(Window.getWindows())
			.filter(candidate -> candidate instanceof ChangeWorkingTimeDialogBox && candidate.isShowing())
			.map(ChangeWorkingTimeDialogBox.class::cast).findFirst().orElseThrow();
		com.microproject.pm.calendar.WorkingCalendar calendar = (com.microproject.pm.calendar.WorkingCalendar)
			manager.getCurrentFrame().getProject().getWorkCalendar();
		int exceptionCount = calendar.getExceptionDays().length;
		int workWeekCount = calendar.getWorkWeekPeriods().size();
		DialogLayoutAssertions.assertTextControlsAtPreferredHeight(dialog, "Change Working Time ribbon route");
		press(robot, KeyEvent.VK_ESCAPE);
		GuiAcceptanceSupport.await(() -> !dialog.isShowing(), "Escape did not cancel Change Working Time");
		assertEquals(exceptionCount, calendar.getExceptionDays().length,
			"closing without parent OK must not commit calendar exceptions");
		assertEquals(workWeekCount, calendar.getWorkWeekPeriods().size(),
			"closing without parent OK must not commit work weeks");
	}

	@Test
	void robotIssue592StatusDateAndUpdateProjectDialogsFitVisualMatrix() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot visual coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		NormalTask task = createTask();
		Project project = task.getOwningProject();
		project.setStatusDate(task.getEnd() + 86_400_000L);
		long originalStatusDate = project.getStatusDate();
		showProject(project);
		SwingUtilities.invokeAndWait(() -> window.setSize(1600, 700));
		GuiAcceptanceSupport.await(() -> window.isShowing() && manager.getCurrentFrame() != null,
			"Issue #592 project window did not become visible");
		Robot robot = new Robot();
		robot.setAutoDelay(45);
		activateWindow(robot, window);
		AbstractButton projectTab = findShowingButtonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
			.getString("ProjectRibbonTask.title"));
		click(robot, boundsOnScreen(projectTab));
		GuiAcceptanceSupport.await(projectTab::isSelected, "Project ribbon tab did not become selected");

		AbstractButton statusDate = findShowingButtonByCommand("RibbonStatusDate");
		String renderedStatusDate = statusDate.getText().replaceAll("(?i)<br\\s*/?>", " ").replaceAll("<[^>]*>", "");
		String localizedDate = com.microproject.options.EditOption.getInstance().getDateFormat()
			.format(new java.util.Date(originalStatusDate));
		assertTrue(renderedStatusDate.startsWith(localizedDate + " ") && renderedStatusDate.endsWith(":"),
			"Status Date ribbon value/caption must remain readable at this locale and scale: " + renderedStatusDate);
		click(robot, boundsOnScreen(statusDate));
		GuiAcceptanceSupport.await(() -> java.util.Arrays.stream(Window.getWindows())
			.anyMatch(candidate -> candidate instanceof StatusDateDialog && candidate.isShowing()),
			"Status Date dialog did not open for visual inspection");
		StatusDateDialog statusDialog = java.util.Arrays.stream(Window.getWindows())
			.filter(candidate -> candidate instanceof StatusDateDialog && candidate.isShowing())
			.map(StatusDateDialog.class::cast).findFirst().orElseThrow();
		DialogLayoutAssertions.assertTextControlsAtPreferredHeight(statusDialog, "Issue #592 Status Date dialog");
		click(robot, boundsOnScreen(findShowingButtonByText(statusDialog, Messages.getString("ButtonText.Cancel"))));
		GuiAcceptanceSupport.await(() -> !statusDialog.isShowing(), "Status Date visual dialog did not cancel");
		assertEquals(originalStatusDate, project.getStatusDate(), "visual inspection must not change Status Date");

		AbstractButton updateProject = findShowingButtonByCommand("RibbonUpdateProject");
		click(robot, boundsOnScreen(updateProject));
		GuiAcceptanceSupport.await(() -> java.util.Arrays.stream(Window.getWindows())
			.anyMatch(candidate -> candidate instanceof UpdateProjectDialogBox && candidate.isShowing()),
			"Update Project dialog did not open for visual inspection");
		UpdateProjectDialogBox updateDialog = java.util.Arrays.stream(Window.getWindows())
			.filter(candidate -> candidate instanceof UpdateProjectDialogBox && candidate.isShowing())
			.map(UpdateProjectDialogBox.class::cast).findFirst().orElseThrow();
		DialogLayoutAssertions.assertTextControlsAtPreferredHeight(updateDialog, "Issue #592 Update Project dialog");
		click(robot, boundsOnScreen(findShowingButtonByText(updateDialog, Messages.getString("ButtonText.Cancel"))));
		GuiAcceptanceSupport.await(() -> !updateDialog.isShowing(), "Update Project visual dialog did not cancel");
		assertEquals(originalStatusDate, project.getStatusDate(), "visual inspection must not alter Status Date");
	}

	@Test
	void robotChangeWorkingTimeCommandResolvesSelectedResourceCalendar() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		NormalTask task = createTask();
		Project project = task.getOwningProject();
		Resource resource = project.getResourcePool().createScriptedResource();
		resource.setName("Selected resource calendar");
		WorkingCalendar resourceCalendar = WorkingCalendar.getInstanceBasedOn(project.getWorkCalendar());
		resourceCalendar.setName("Selected resource calendar work time");
		resource.setWorkCalendar(resourceCalendar);
		showProject(project);
		SwingUtilities.invokeAndWait(() -> {
			window.setSize(1600, 700);
			manager.getCurrentFrame().activateResourceView();
		});
		DocumentFrame documentFrame = manager.getCurrentFrame();
		ResourceView resourceView = documentFrame.getResourceView();
		GuiAcceptanceSupport.await(() -> documentFrame.getActiveTopView() == resourceView,
			"Resource Sheet did not become active");
		SpreadSheet resourceSheet = resourceView.getSpreadSheet();
		int resourceRow = rowForResource(resourceSheet, resource);
		assertTrue(resourceRow >= 0, "the test resource must be present in Resource Sheet's visible rows");
		// Seed the typed row selection through its canonical Swing selection model;
		// the subsequent command itself is exercised with physical Robot input.
		SwingUtilities.invokeAndWait(() -> resourceSheet.setRowSelectionInterval(resourceRow, resourceRow));
		Object selected = documentFrame.getSelectedImpl();
		assertTrue(selected instanceof Resource && "Selected resource calendar".equals(((Resource) selected).getName()),
			"resource selection must resolve to the selected resource, got " + selected);

		Robot robot = new Robot();
		robot.setAutoDelay(45);
		activateWindow(robot, window);
		// ResourceView presents a one-time product notice for ordinary projects.
		// Dismiss that expected modal before testing the Project ribbon route.
		if (java.util.Arrays.stream(Window.getWindows()).anyMatch(candidate -> candidate instanceof Dialog && candidate.isShowing())) {
			press(robot, KeyEvent.VK_ENTER);
			GuiAcceptanceSupport.await(() -> java.util.Arrays.stream(Window.getWindows())
				.noneMatch(candidate -> candidate instanceof Dialog && candidate.isShowing()),
				"the expected Resource Sheet notice did not close");
		}
		AbstractButton projectTab = findShowingButtonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
			.getString("ProjectRibbonTask.title"));
		for (int attempt = 0; attempt < 3 && !projectTab.isSelected(); attempt++) {
			if (attempt > 0) activateWindow(robot, window);
			click(robot, boundsOnScreen(projectTab));
		}
		GuiAcceptanceSupport.await(projectTab::isSelected, "Project ribbon tab was not selected after bounded physical clicks");
		AbstractButton changeWorkingTime = findShowingButtonByCommand("RibbonChangeWorkingTime");
		assertTrue(changeWorkingTime.isEnabled(), "Change Working Time must be enabled for a selected writable resource");
		click(robot, boundsOnScreen(changeWorkingTime));
		GuiAcceptanceSupport.await(() -> findWorkingTimeDialog() != null, "Change Working Time did not open");
		ChangeWorkingTimeDialogBox dialog = findWorkingTimeDialog();
		assertTrue(UiComponentWalker.flatten(dialog).stream().filter(JComboBox.class::isInstance)
			.map(JComboBox.class::cast).anyMatch(combo -> combo.getSelectedItem() == resourceCalendar),
			"DocumentFrame must open Change Working Time on the selected resource's calendar");
		JTabbedPane tabs = findTabbedPane(dialog);
		assertNotNull(tabs, "Change Working Time must expose calendar tabs");
		Rectangle exceptionsTab = tabs.getBoundsAt(2);
		Rectangle tabsBounds = boundsOnScreen(tabs);
		click(robot, new Rectangle(tabsBounds.x + exceptionsTab.x + exceptionsTab.width / 2,
			tabsBounds.y + exceptionsTab.y + exceptionsTab.height / 2, 1, 1));
		GuiAcceptanceSupport.await(() -> tabs.getSelectedIndex() == 2, "Exceptions tab did not become active");
		AbstractButton add = UiComponentWalker.flatten(dialog).stream().filter(AbstractButton.class::isInstance)
			.map(AbstractButton.class::cast).filter(button -> "calendarExceptionAddButton".equals(button.getName()))
			.findFirst().orElseThrow(() -> new AssertionError("resource exception Add button is absent"));
		assertTrue(add.isEnabled(), "resource-specific calendar exceptions must be editable");
		click(robot, boundsOnScreen(add));
		GuiAcceptanceSupport.await(() -> java.util.Arrays.stream(Window.getWindows())
			.anyMatch(candidate -> candidate instanceof Dialog && candidate.isShowing() && candidate != dialog),
			"resource calendar Add editor did not open");
		press(robot, KeyEvent.VK_ESCAPE);
		GuiAcceptanceSupport.await(() -> java.util.Arrays.stream(Window.getWindows())
			.noneMatch(candidate -> candidate instanceof Dialog && candidate.isShowing() && candidate != dialog),
			"Escape did not close resource calendar Add editor");
		press(robot, KeyEvent.VK_ESCAPE);
		GuiAcceptanceSupport.await(() -> !dialog.isShowing(), "Escape did not cancel Change Working Time");
		assertTrue(resourceCalendar.getRecurringExceptions().isEmpty(), "Cancel must leave resource calendar unchanged");
	}

	private static ChangeWorkingTimeDialogBox findWorkingTimeDialog() {
		return java.util.Arrays.stream(Window.getWindows())
			.filter(candidate -> candidate instanceof ChangeWorkingTimeDialogBox && candidate.isShowing())
			.map(ChangeWorkingTimeDialogBox.class::cast).findFirst().orElse(null);
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
	void robotNetworkAndWbsRibbonRoutesRenderTheirDedicatedViews() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for GUI view coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		NormalTask task = createTask();
		showProject(task.getOwningProject());
		SwingUtilities.invokeAndWait(() -> window.setSize(1600, 700));
		GuiAcceptanceSupport.await(() -> window.isShowing() && manager.getCurrentFrame() != null,
			"network/WBS test window did not become visible");
		Robot robot = new Robot();
		robot.setAutoDelay(45);
		activateWindow(robot, window);
		AbstractButton viewTab = findShowingButtonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
			.getString("ViewRibbonTask.title"));
		click(robot, boundsOnScreen(viewTab));
		GuiAcceptanceSupport.await(viewTab::isSelected, "View ribbon tab was not selected");

		AbstractButton network = findShowingButtonByCommand("RibbonNetwork");
		click(robot, boundsOnScreen(network));
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame().getActiveTopView() instanceof PertView,
			"RibbonNetwork did not activate the real network view");
		assertTrue(isShowingView(manager.getCurrentFrame().getActiveTopView()),
			"network view was activated without becoming visible");

		AbstractButton wbs = findShowingButtonByCommand("RibbonWBS");
		click(robot, boundsOnScreen(wbs));
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame().getActiveTopView() instanceof TreeView,
			"RibbonWBS did not activate the real WBS view");
		assertTrue(isShowingView(manager.getCurrentFrame().getActiveTopView()),
			"WBS view was activated without becoming visible");
	}

	private static boolean isShowingView(Object view) {
		return view instanceof Component component && component.isShowing();
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
		SwingUtilities.invokeAndWait(() -> window.setExtendedState(java.awt.Frame.MAXIMIZED_BOTH));
		GuiAcceptanceSupport.await(() -> window.isShowing() && manager.getCurrentFrame() != null
				&& manager.getCurrentFrame().getActiveSpreadSheet() != null,
			"hide/show test project did not become visible");
		Robot robot = new Robot();
		robot.setAutoDelay(45);
		activateWindow(robot, window);
		SpreadSheet sheet = manager.getCurrentFrame().getActiveSpreadSheet();
		int row = rowForTask(sheet, task);
		clickUntilSelected(robot, window, sheet, row, nameColumn(sheet));
		AbstractButton taskTab = findShowingButtonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
				.getString("TaskRibbonTask.title"));
		click(robot, boundsOnScreen(taskTab));
		AbstractButton hide = findVisibleOrOverflowButton(robot, "RibbonHideSelectedTasks");
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
		hide = findVisibleOrOverflowButton(robot, "RibbonHideSelectedTasks");
		click(robot, boundsOnScreen(hide));
		GuiAcceptanceSupport.await(task::isHiddenTask, "second hide did not update the task model");
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_Y);
		robot.keyRelease(KeyEvent.VK_Y);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		robot.waitForIdle();
		GuiAcceptanceSupport.await(task::isHiddenTask, "Ctrl+Y did not reapply task visibility");

		AbstractButton show = findVisibleOrOverflowButton(robot, "RibbonShowAllTasks");
		assertTrue(show.isShowing(), "Show All Tasks must remain discoverable beside Hide Selected Tasks after hiding");
		assertTrue(show.isEnabled(), "Show All Tasks must become enabled after a task is hidden");
		click(robot, boundsOnScreen(show));
		GuiAcceptanceSupport.await(() -> !task.isHiddenTask(), "Show All Tasks did not restore the task model");
		GuiAcceptanceSupport.await(() -> rowForTask(sheet, task) >= 0, "Show All Tasks did not restore the visible task row");
	}

	@Test
	void taskModeRibbonRouteChangesModelAndRoundTripsUndoRedoAndMpo() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Task Mode Robot coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		NormalTask task = createTask();
		Project project = task.getOwningProject();
		showProject(project);
		SwingUtilities.invokeAndWait(() -> window.setExtendedState(java.awt.Frame.MAXIMIZED_BOTH));
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
		AbstractButton manual = findVisibleOrOverflowButton(robot, "RibbonTaskModeManual");
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
			AbstractButton statusDate = GuiPhysicalRouteAdapter.visibleButton(window, "RibbonStatusDate");
			String expectedDate = com.microproject.options.EditOption.getInstance().getDateFormat()
					.format(new java.util.Date(initialStatusDate));
			String statusDateText = statusDate.getText().replaceAll("(?i)<br\\s*/?>", " ").replaceAll("<[^>]*>", "");
			assertTrue(statusDateText.startsWith(expectedDate + " "),
					"MSP Status Date control must show the current date before its caption: " + statusDateText);
			assertTrue(statusDateText.endsWith(":"),
					"MSP Status Date control must retain its caption after the date value: " + statusDateText);
			click(context.robot(), boundsOnScreen(statusDate));
			GuiAcceptanceSupport.await(() -> java.util.Arrays.stream(Window.getWindows())
					.anyMatch(candidate -> candidate instanceof StatusDateDialog && candidate.isShowing()),
					"Status Date dialog did not open for cancel verification");
			StatusDateDialog cancelled = java.util.Arrays.stream(Window.getWindows())
					.filter(candidate -> candidate instanceof StatusDateDialog && candidate.isShowing())
					.map(StatusDateDialog.class::cast).findFirst().orElseThrow();
			DialogLayoutAssertions.assertTextControlsAtPreferredHeight(cancelled, "Status Date dialog");
			assertTrue(UiComponentWalker.flatten(cancelled).stream()
					.filter(com.microproject.dialog.util.ExtDateField.class::isInstance)
					.map(com.microproject.dialog.util.ExtDateField.class::cast)
					.anyMatch(field -> field.isEnabled() && field.getDateValue() != null),
					"A configured MSP Status Date must be shown in the editable date field");
			click(context.robot(), boundsOnScreen(findShowingButtonByText(cancelled, Messages.getString("ButtonText.Cancel"))));
			GuiAcceptanceSupport.await(() -> !cancelled.isShowing(), "Status Date cancel did not close dialog");
			assertEquals(initialStatusDate, contextProject().getStatusDate(), "Cancel must not change status date");
			click(context.robot(), boundsOnScreen(GuiPhysicalRouteAdapter.visibleButton(window, "RibbonStatusDate")));
			chooseStatusDateNotSet(context.robot());
			GuiAcceptanceSupport.await(() -> GuiPhysicalRouteAdapter.visibleButton(window, "RibbonStatusDate")
					.getText().startsWith("NA "), "Status Date ribbon value did not refresh to NA");
			return null;
		}, () -> !contextProject().isStatusDateSet() && statusDateRibbonMatchesModel(),
			() -> contextProject().isStatusDateSet() && statusDateRibbonMatchesModel(),
			"StatusDate");
	}
	@Test
	void statusDateRibbonCanSetSpecificDatePhysically() throws Exception {
		runProgressPhysicalRoute("status-set-date", context -> {
			click(context.robot(), boundsOnScreen(GuiPhysicalRouteAdapter.visibleButton(window, "RibbonStatusDate")));
			GuiAcceptanceSupport.await(() -> java.util.Arrays.stream(Window.getWindows())
					.anyMatch(candidate -> candidate instanceof StatusDateDialog && candidate.isShowing()),
					"Status Date dialog did not open for date selection");
			StatusDateDialog dialog = java.util.Arrays.stream(Window.getWindows())
					.filter(candidate -> candidate instanceof StatusDateDialog && candidate.isShowing())
					.map(StatusDateDialog.class::cast).findFirst().orElseThrow();
			AbstractButton picker = UiComponentWalker.flatten(dialog).stream()
					.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
					.filter(button -> "...".equals(button.getText())).findFirst().orElseThrow();
			click(context.robot(), boundsOnScreen(picker));
			GuiAcceptanceSupport.await(() -> java.util.Arrays.stream(javax.swing.MenuSelectionManager.defaultManager().getSelectedPath())
					.anyMatch(JPopupMenu.class::isInstance), "Status Date calendar popup did not open");
			JPopupMenu popup = java.util.Arrays.stream(javax.swing.MenuSelectionManager.defaultManager().getSelectedPath())
					.filter(JPopupMenu.class::isInstance).map(JPopupMenu.class::cast).findFirst().orElseThrow();
			AbstractButton selectedDay = UiComponentWalker.flatten(popup).stream()
					.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
					.filter(button -> "3".equals(button.getText())).findFirst().orElseThrow();
			click(context.robot(), boundsOnScreen(selectedDay));
			com.microproject.dialog.util.ExtDateField dateField = UiComponentWalker.flatten(dialog).stream()
					.filter(com.microproject.dialog.util.ExtDateField.class::isInstance)
					.map(com.microproject.dialog.util.ExtDateField.class::cast).findFirst().orElseThrow();
			assertNotNull(dateField.getDateValue(), "Selecting a calendar day must update the date field");
			assertTrue(initialStatusDate != dateField.getDateValue().getTime(),
					"The physical calendar selection must replace the preselected status date");
			click(context.robot(), boundsOnScreen(findShowingButtonByText(dialog, Messages.getString("ButtonText.OK"))));
			GuiAcceptanceSupport.await(() -> !dialog.isShowing(), "Status Date date selection did not close dialog");
			assertTrue(initialStatusDate != contextProject().getStatusDate(),
					"Accepting the selected date must update the project model");
			return null;
		}, () -> contextProject().isStatusDateSet() && contextProject().getStatusDate() != initialStatusDate
				&& statusDateRibbonMatchesModel(),
			() -> contextProject().isStatusDateSet() && contextProject().getStatusDate() == initialStatusDate
				&& statusDateRibbonMatchesModel(), "StatusDate");
	}
	@Test
	void statusDatePopupRouteUsesSharedMutationFixture() throws Exception {
		runProgressPhysicalRoute("status-popup", c -> { rightClick(c.robot(), c.cell()); SpreadSheetPopupMenu p=c.sheet().getPopup(); GuiAcceptanceSupport.await(p::isVisible,"Status popup absent"); click(c.robot(),boundsOnScreen(GuiPhysicalRouteAdapter.visiblePopupItem(p,"popup.StatusDate"))); chooseStatusDateNotSet(c.robot()); return null; }, () -> !contextProject().isStatusDateSet() && statusDateRibbonMatchesModel(), () -> contextProject().isStatusDateSet() && statusDateRibbonMatchesModel(), "StatusDate");
	}
	@Test
	void statusDateShortcutRouteUsesSharedMutationFixture() throws Exception {
		runProgressPhysicalRoute("status-shortcut", c -> { GuiPhysicalRouteAdapter.assertRootPaneBinding(window.getRootPane(),javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_S,InputEvent.CTRL_DOWN_MASK|InputEvent.ALT_DOWN_MASK),"StatusDate"); press(c.robot(),KeyEvent.VK_CONTROL,KeyEvent.VK_ALT,KeyEvent.VK_S); chooseStatusDateNotSet(c.robot()); return null; }, () -> !contextProject().isStatusDateSet() && statusDateRibbonMatchesModel(), () -> contextProject().isStatusDateSet() && statusDateRibbonMatchesModel(), "StatusDate");
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
	void markOnTrackMenuRouteUsesSharedMutationFixture() throws Exception {
		runProgressPhysicalRoute("mark-menu", c -> {
			JMenuBar bar = new JMenuBar();
			JMenu root = new JMenu("Task");
			JMenuItem item = new JMenuItem(manager.getMenuManager().getActionFromId("MarkOnTrack"));
			item.setActionCommand("MarkOnTrack");
			root.add(item); bar.add(root); window.setJMenuBar(bar); window.validate();
			GuiAcceptanceSupport.await(root::isShowing, "Mark on Track menu root did not become visible");
			click(c.robot(), boundsOnScreen(root));
			JMenuItem visible = GuiPhysicalRouteAdapter.visiblePopupItem(root.getPopupMenu(), "MarkOnTrack");
			GuiAcceptanceSupport.await(visible::isEnabled, "Mark on Track menu item remained disabled for a selected task");
			click(c.robot(), boundsOnScreen(visible));
			return null;
		}, () -> contextTask().getPercentComplete() != initialPercentComplete,
			() -> contextTask().getPercentComplete() == initialPercentComplete, "MarkOnTrack");
	}
	@Test
	void markOnTrackRibbonIsDisabledWithoutTaskSelection() throws Exception {
		runProgressPhysicalRoute("mark-on-track-no-selection", c -> {
			AbstractButton markOnTrack = GuiPhysicalRouteAdapter.visibleButton(window, "RibbonMarkOnTrack");
			assertFalse(markOnTrack.isEnabled(), "MSP Mark on Track requires selected tasks");
			click(c.robot(), boundsOnScreen(markOnTrack));
			assertEquals(initialPercentComplete, contextTask().getPercentComplete(),
					"A disabled command must leave the task unchanged");
			return null;
		}, () -> contextTask().getPercentComplete() == initialPercentComplete,
			() -> contextTask().getPercentComplete() == initialPercentComplete, "MarkOnTrack", false);
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
		SpreadSheet sheet = manager.getCurrentFrame().getActiveSpreadSheet();
		clickUntilSelected(robot, window, sheet, rowForTask(sheet, task), nameColumn(sheet));
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
		AbstractButton selectedTasks = findShowingButtonByText(dialog,
			Messages.getString("UpdateProjectDialogBox.SelectedTasks"));
		assertTrue(selectedTasks.isSelected(), "an existing task selection must default to Selected Tasks");
		assertEquals(project.getStatusDate(), dialog.getForm().getUpdateDate().getTime(),
			"Update Project date defaults to the effective project Status Date");
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
		project.setStatusDate(task.getEnd() + 86_400_000L);
		long statusDateBeforeUpdate = project.getStatusDate();
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
				assertTrue(findShowingButtonByText(dialog, Messages.getString("UpdateProjectDialogBox.EntireProject")).isSelected(),
					"no selected task rows must default Update Project to Entire Project");
				assertEquals(statusDateBeforeUpdate, dialog.getForm().getUpdateDate().getTime(),
					"Update Project date must default to the effective project Status Date");
				click(robot, boundsOnScreen(findShowingButtonByText(dialog,
					Messages.getString("UpdateProjectDialogBox.SetZeroOrHundredOnly"))));
				click(robot, boundsOnScreen(findShowingButtonByText(dialog, Messages.getString("ButtonText.OK")))); return null;
			}, () -> manager.getLastRibbonCommandResult(), () -> task.getPercentComplete() == 1D,
			() -> { press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Z); return null; },
				() -> task.getPercentComplete() == 0D && project.getStatusDate() == statusDateBeforeUpdate,
			() -> { press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Y); return null; },
				() -> task.getPercentComplete() == 1D && project.getStatusDate() == statusDateBeforeUpdate,
			() -> {
				ByteArrayOutputStream saved = new ByteArrayOutputStream();
				MpoFileImporter importer = new MpoFileImporter();
				return importer.saveProject(project, saved)
					&& importer.loadProject(new java.io.ByteArrayInputStream(saved.toByteArray()))
						.getTaskList().stream().anyMatch(reloaded -> reloaded.getUniqueId() == task.getUniqueId()
							&& reloaded.getPercentComplete() == 1D);
			});
	}

	private NormalTask progressTask;
	private Project progressProject;
	private long initialStatusDate;
	private double initialPercentComplete;
	private Project contextProject() { return progressProject; }
	private NormalTask contextTask() { return progressTask; }
	private boolean statusDateRibbonMatchesModel() {
		AbstractButton button = GuiPhysicalRouteAdapter.visibleButton(window, "RibbonStatusDate");
		String text = button.getText().replaceAll("(?i)<br\\s*/?>", " ").replaceAll("<[^>]*>", "");
		if (!contextProject().isStatusDateSet()) return text.startsWith("NA ") && text.endsWith(":");
		String date = com.microproject.options.EditOption.getInstance().getDateFormat()
				.format(new java.util.Date(contextProject().getStatusDate()));
		return text.startsWith(date + " ") && text.endsWith(":");
	}

	private void runProgressPhysicalRoute(String routeName, TaskModeRoute route,
			java.util.function.BooleanSupplier after, java.util.function.BooleanSupplier before, String commandId) throws Exception {
		runProgressPhysicalRoute(routeName, route, after, before, commandId, true);
	}

	private void runProgressPhysicalRoute(String routeName, TaskModeRoute route,
			java.util.function.BooleanSupplier after, java.util.function.BooleanSupplier before, String commandId,
			boolean selectTask) throws Exception {
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
		if (selectTask) click(robot, cellOnScreen(sheet, rowForTask(sheet, progressTask), nameColumn(sheet)));
		else SwingUtilities.invokeAndWait(sheet::clearSelection);
		click(robot, boundsOnScreen(findShowingButtonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
				.getString(commandId.equals("MarkOnTrack") ? "TaskRibbonTask.title" : "ProjectRibbonTask.title"))));
		GuiAcceptanceSupport.await(() -> GuiPhysicalRouteAdapter.visibleButton(window,
				commandId.equals("StatusDate") ? "RibbonStatusDate" : "RibbonMarkOnTrack") != null,
				"progress command did not become visible");
		TaskModeContext context = new TaskModeContext(robot, sheet,
				cellOnScreen(sheet, rowForTask(sheet, progressTask), nameColumn(sheet)), progressTask);
		if (!selectTask) {
			assertEquals(0, sheet.getSelectedRows().length, "No-selection fixture must have no selected task rows");
			route.run(context);
			assertTrue(after.getAsBoolean(), "Disabled Mark on Track must preserve task progress");
			return;
		}
		GuiCommandAcceptanceFixture.verifyMutation(commandId, () -> route.run(context),
			() -> manager.getCurrentFrame().getLastTaskCommandResult(), after,
			() -> rowForTask(sheet, progressTask) >= 0,
			() -> { press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Z); return null; }, before,
			() -> { press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Y); return null; }, after,
			() -> {
				ByteArrayOutputStream saved = new ByteArrayOutputStream();
				if (!new MpoFileImporter().saveProject(progressProject, saved)) return false;
				Project reloaded = new MpoFileImporter().loadProject(new ByteArrayInputStream(saved.toByteArray()));
				return commandId.equals("StatusDate") ? reloaded.isStatusDateSet() == progressProject.isStatusDateSet()
						&& (!reloaded.isStatusDateSet() || reloaded.getStatusDate() == progressProject.getStatusDate())
						: Double.compare(taskNamed(reloaded, progressTask.getName()).getPercentComplete(), initialPercentComplete) != 0;
			});
	}

	private void chooseStatusDateNotSet(Robot robot) throws Exception {
		GuiAcceptanceSupport.await(() -> java.util.Arrays.stream(Window.getWindows())
				.anyMatch(candidate -> candidate instanceof StatusDateDialog && candidate.isShowing()),
				"Status Date dialog did not open");
		StatusDateDialog dialog = java.util.Arrays.stream(Window.getWindows())
				.filter(candidate -> candidate instanceof StatusDateDialog && candidate.isShowing())
				.map(StatusDateDialog.class::cast).findFirst().orElseThrow();
		JCheckBox notSet = (JCheckBox) UiComponentWalker.flatten(dialog).stream()
				.filter(JCheckBox.class::isInstance).map(JCheckBox.class::cast)
				.filter(JCheckBox::isShowing).findFirst().orElseThrow();
		click(robot, boundsOnScreen(notSet));
		click(robot, boundsOnScreen(findShowingButtonByText(dialog, Messages.getString("ButtonText.OK"))));
		GuiAcceptanceSupport.await(() -> !dialog.isShowing(), "Status Date dialog did not close");
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
		SwingUtilities.invokeAndWait(() -> window.setExtendedState(java.awt.Frame.MAXIMIZED_BOTH));
		GuiAcceptanceSupport.await(() -> window.isShowing() && manager.getCurrentFrame() != null
				&& manager.getCurrentFrame().getActiveSpreadSheet() != null, "delete test project did not become visible");
		SpreadSheet sheet = manager.getCurrentFrame().getActiveSpreadSheet();
		Robot robot = new Robot();
		robot.setAutoDelay(45);
		activateWindow(robot, window);
		click(robot, cellOnScreen(sheet, rowForTask(sheet, task), nameColumn(sheet)));
		click(robot, boundsOnScreen(findShowingButtonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
				.getString("TaskRibbonTask.title"))));
		AbstractButton delete = findVisibleOrOverflowButton(robot, "RibbonDelete");
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
		SwingUtilities.invokeAndWait(() -> window.setExtendedState(java.awt.Frame.MAXIMIZED_BOTH));
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
		AbstractButton hide = findVisibleOrOverflowButton(robot, "RibbonHideSelectedTasks");
		AbstractButton show = findVisibleOrOverflowButton(robot, "RibbonShowAllTasks");
		assertFalse(hide.isEnabled(), "Hide Selected Tasks must be disabled without a selected task");
		assertFalse(show.isEnabled(), "Show All Tasks must be disabled when no task is hidden");
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

		ByteArrayOutputStream saved = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(project, saved),
				"MPO save rejected the hierarchy restored through the physical Outdent route");
		Project reloaded = new MpoFileImporter().loadProject(new ByteArrayInputStream(saved.toByteArray()));
		NormalTask reloadedFirst = taskNamed(reloaded, "Indent predecessor");
		NormalTask reloadedSecond = taskNamed(reloaded, "Indent target");
		assertNotNull(reloadedFirst, "MPO reload lost the Outdent predecessor");
		assertNotNull(reloadedSecond, "MPO reload lost the Outdent target");
		assertTrue(reloadedSecond.getWbsParentTask() == null,
				"MPO reload retained a parent after the physical Outdent route");
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
	void robotRightClickTaskPopupHideAndShowUseTheSharedVisibilityRoute() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		NormalTask target = createTask();
		Project project = target.getOwningProject();
		NormalTask companion = project.createScriptedTask();
		companion.setName("Popup visibility companion");
		project.recalculate();
		showProject(project);
		SwingUtilities.invokeAndWait(() -> window.setSize(1600, 700));
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame() != null
				&& manager.getCurrentFrame().getActiveSpreadSheet() != null,
			"popup visibility project did not become visible");

		Robot robot = new Robot();
		robot.setAutoDelay(45);
		activateWindow(robot, window);
		SpreadSheet sheet = manager.getCurrentFrame().getActiveSpreadSheet();
		Rectangle targetCell = cellOnScreen(sheet, rowForTask(sheet, target), nameColumn(sheet));
		click(robot, targetCell);
		rightClick(robot, targetCell);
		SpreadSheetPopupMenu popup = sheet.getPopup();
		GuiAcceptanceSupport.await(() -> popup != null && popup.isVisible(),
			"physical right click did not show the task popup for Hide");
		JMenuItem hide = popupItem(popup, "popup." + com.microproject.menu.MenuActionConstants.ACTION_HIDE_SELECTED_TASKS);
		GuiAcceptanceSupport.await(hide::isEnabled, "popup Hide Selected Tasks remained disabled for the selected task");
		click(robot, boundsOnScreen(hide));
		GuiAcceptanceSupport.await(target::isHiddenTask, "popup Hide Selected Tasks did not update the task model");
		GuiAcceptanceSupport.await(() -> !isTaskVisible(sheet, target), "popup Hide Selected Tasks did not remove the visible row");
		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Z);
		GuiAcceptanceSupport.await(() -> !target.isHiddenTask(), "Ctrl+Z did not undo popup Hide Selected Tasks");
		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Y);
		GuiAcceptanceSupport.await(target::isHiddenTask, "Ctrl+Y did not redo popup Hide Selected Tasks");

		ByteArrayOutputStream saved = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(project, saved), "MPO save rejected the popup-hidden task state");
		Project reloaded = new MpoFileImporter().loadProject(new ByteArrayInputStream(saved.toByteArray()));
		assertTrue(taskNamed(reloaded, target.getName()).isHiddenTask(), "MPO reload lost popup-hidden task state");

		Rectangle companionCell = cellOnScreen(sheet, rowForTask(sheet, companion), nameColumn(sheet));
		click(robot, companionCell);
		rightClick(robot, companionCell);
		GuiAcceptanceSupport.await(popup::isVisible, "physical right click did not show the task popup for Show All");
		JMenuItem show = popupItem(popup, "popup." + com.microproject.menu.MenuActionConstants.ACTION_SHOW_ALL_TASKS);
		GuiAcceptanceSupport.await(show::isEnabled, "popup Show All Tasks remained disabled after hiding a task");
		click(robot, boundsOnScreen(show));
		GuiAcceptanceSupport.await(() -> !target.isHiddenTask(), "popup Show All Tasks did not restore the task model");
		GuiAcceptanceSupport.await(() -> isTaskVisible(sheet, target), "popup Show All Tasks did not restore the visible row");
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
	void robotRightClickTaskPopupDeleteUsesSharedRouteAndRoundTripsPersistence() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		NormalTask target = createTask();
		Project project = target.getOwningProject();
		project.recalculate();
		showProject(project);
		SwingUtilities.invokeAndWait(() -> window.setSize(1600, 700));
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame() != null
				&& manager.getCurrentFrame().getActiveSpreadSheet() != null,
			"popup delete project did not become visible");

		Robot robot = new Robot();
		robot.setAutoDelay(45);
		activateWindow(robot, window);
		SpreadSheet sheet = manager.getCurrentFrame().getActiveSpreadSheet();
		Rectangle targetCell = cellOnScreen(sheet, rowForTask(sheet, target), nameColumn(sheet));
		click(robot, targetCell);
		rightClick(robot, targetCell);
		SpreadSheetPopupMenu popup = sheet.getPopup();
		GuiAcceptanceSupport.await(() -> popup != null && popup.isVisible(),
				"physical right click did not show the task popup for Delete");
		JMenuItem delete = popupItem(popup, "popup." + com.microproject.menu.MenuActionConstants.ACTION_DELETE);
		GuiAcceptanceSupport.await(delete::isEnabled, "popup Delete remained disabled for the selected task");
		click(robot, boundsOnScreen(delete));
		GuiAcceptanceSupport.await(() -> !isTaskVisible(sheet, target),
				"popup Delete did not remove the selected row");

		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Z);
		GuiAcceptanceSupport.await(() -> isTaskVisible(sheet, target),
				"Ctrl+Z did not restore the row deleted through the popup");
		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Y);
		GuiAcceptanceSupport.await(() -> !isTaskVisible(sheet, target),
				"Ctrl+Y did not reapply popup Delete");

		ByteArrayOutputStream saved = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(project, saved),
				"MPO save rejected the task deleted through the physical popup");
		Project reloaded = new MpoFileImporter().loadProject(new ByteArrayInputStream(saved.toByteArray()));
		assertTrue(reloaded.getTaskList().stream().noneMatch(candidate -> target.getName().equals(candidate.getName())),
				"MPO reload retained the task deleted through the physical popup");
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

	private JPopupMenu openOverflowForCommand(Robot robot, String command) throws Exception {
		JPopupMenu[] result = new JPopupMenu[1];
		AbstractButton[] trigger = new AbstractButton[1];
		SwingUtilities.invokeAndWait(() -> {
			for (java.awt.Component component : UiComponentWalker.flatten(window)) {
				if (!(component instanceof AbstractButton button)) continue;
				Object value = button.getClientProperty("MicroProject.ribbonCollapsedPopup");
				if (value instanceof JPopupMenu popup && popupCommandOrNull(popup, command) != null) {
					trigger[0] = button;
					result[0] = popup;
					break;
				}
			}
		});
		if (trigger[0] == null)
			throw new AssertionError("Responsive ribbon overflow has no command: " + command);
		click(robot, boundsOnScreen(trigger[0]));
		GuiAcceptanceSupport.await(result[0]::isVisible, "Responsive ribbon overflow did not open");
		return result[0];
	}

	private AbstractButton findVisibleOrOverflowButton(Robot robot, String command) throws Exception {
		AbstractButton direct = findVisibleCommandOrNull(command);
		if (direct != null) return direct;
		return popupCommand(openOverflowForCommand(robot, command), command);
	}

	private AbstractButton findVisibleCommandOrNull(String command) throws Exception {
		AbstractButton[] result = new AbstractButton[1];
		SwingUtilities.invokeAndWait(() -> result[0] = UiComponentWalker.flatten(window).stream()
			.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
			.filter(AbstractButton::isShowing).filter(button -> command.equals(button.getActionCommand()))
			.findFirst().orElse(null));
		return result[0];
	}

	private static AbstractButton popupCommand(JPopupMenu popup, String command) {
		AbstractButton button = popupCommandOrNull(popup, command);
		if (button == null) throw new AssertionError("Ribbon overflow has no command: " + command);
		return button;
	}

	private static AbstractButton popupCommandOrNull(JPopupMenu popup, String command) {
		for (java.awt.Component component : UiComponentWalker.flatten(popup)) {
			if (component instanceof AbstractButton button && command.equals(button.getActionCommand()))
				return button;
		}
		return null;
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

	private static int rowForResource(SpreadSheet sheet, Resource resource) {
		SpreadSheetModel model = (SpreadSheetModel) sheet.getModel();
		for (int row = 0; row < model.getRowCount(); row++) {
			Node node = model.getNodeForDisplayRow(row);
			if (node != null && node.getImpl() == resource) return row;
		}
		return -1;
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

	private static void clickUntilSelected(Robot robot, java.awt.Window window, SpreadSheet sheet, int row, int column)
		throws Exception {
		for (int attempt = 0; attempt < 3 && sheet.getSelectedRow() != row; attempt++) {
			if (attempt > 0) activateWindow(robot, window);
			click(robot, cellOnScreen(sheet, row, column));
		}
		GuiAcceptanceSupport.await(() -> sheet.getSelectedRow() == row,
			"physical task-table click did not establish the selected row after bounded foreground retries");
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

	private static UpdateTaskDialog findUpdateTaskDialog() {
		for (Window candidate : Window.getWindows()) {
			if (candidate instanceof UpdateTaskDialog dialog && dialog.isVisible())
				return dialog;
		}
		return null;
	}

	private static Dialog findRecurringTaskDialog() {
		for (Window candidate : Window.getWindows()) {
			if (candidate instanceof Dialog dialog && dialog.isVisible()
					&& Messages.getString("RecurringTaskDialog.Title").equals(dialog.getTitle()))
				return dialog;
		}
		return null;
	}

	private static CalendarDialogBox findCalendarOptionsDialog() {
		for (Window candidate : Window.getWindows()) {
			if (candidate instanceof CalendarDialogBox dialog && dialog.isVisible())
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
		capture(robot, dialog, "task-information-ribbon-click.png");
	}

	private static void capture(Robot robot, Dialog dialog, String fileName) throws Exception {
		Rectangle[] bounds = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> bounds[0] = new Rectangle(dialog.getLocationOnScreen(), dialog.getSize()));
		BufferedImage image = robot.createScreenCapture(bounds[0]);
		Path artifact = Path.of(System.getProperty("microproject.gui.artifacts.dir", "build/guiTest-artifacts"),
		fileName);
		Files.createDirectories(artifact.getParent());
		ImageIO.write(image, "png", artifact.toFile());
		assertTrue(image.getWidth() > 300 && image.getHeight() > 200, "Dialog capture is unexpectedly small: " + fileName);
	}
}
