/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.dialog.assignment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.awt.Color;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.swing.AbstractButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.microproject.collaboration.CollaborationSession;
import com.microproject.datatype.Rate;
import com.microproject.exchange.MpoFileImporter;
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
import com.microproject.testsupport.RibbonGuiButton;
import com.microproject.ui.ribbon.RibbonDisplayMode;
import com.microproject.ui.ribbon.RibbonDisplayPreferences;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.ui.util.SwingAlertPresenter;
import com.microproject.util.Alert;
import com.microproject.util.Environment;
import com.microproject.util.FlatUiSupport;

/** GUI-MSP-ASSIGNMENT-01: Assign Resources has one lock-aware mutation route. */
class AssignmentDialogGuiAcceptanceTest {
	private MainRibbonFrame window;
	private GraphicManager manager;
	private boolean previousRibbonUi;
	private boolean previousNewLook;
	private boolean previousClientSide;
	private boolean previousBatchMode;
	private RibbonDisplayMode previousRibbonDisplayMode;

	@BeforeEach
	void prepareRibbonDisplay() {
		previousRibbonDisplayMode = RibbonDisplayPreferences.load();
		previousClientSide = Environment.isClientSide();
		previousBatchMode = Environment.isBatchMode();
		RibbonDisplayPreferences.save(RibbonDisplayMode.ALWAYS_SHOW);
	}

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
		Environment.setClientSide(previousClientSide);
		Environment.setBatchMode(previousBatchMode);
		RibbonDisplayPreferences.save(previousRibbonDisplayMode);
		Alert.setPresenter(null);
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

		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(40);
		SpreadSheet taskSheet = manager.getCurrentFrame().getActiveSpreadSheet();
		click(robot, cellBounds(taskSheet, rowForTask(taskSheet, task), nameColumn(taskSheet)));
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame().getSelectedImpls(false).contains(task),
			"Robot click did not select the assignment task");
		AbstractButton taskTab = buttonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
			.getString("TaskRibbonTask.title"));
		click(robot, bounds(taskTab));
		AbstractButton assignResources = com.microproject.testsupport.RibbonGuiSupport.findVisibleOrExpand(robot, window, "RibbonAssignResources");
		GuiAcceptanceSupport.await(assignResources::isEnabled, "Assign Resources remained disabled for a selected task");
		click(robot, bounds(assignResources));
		AssignmentDialog dialog = awaitDialog();
		SpreadSheet resourceSheet = dialog.spreadSheetPane.getSpreadSheet();
		int resourceRow = rowForResource(resourceSheet, resource);
		click(robot, cellBounds(resourceSheet, resourceRow, nameColumn(resourceSheet)));
		GuiAcceptanceSupport.await(() -> dialog.getSelectedResources().contains(resource),
			"Robot click did not select the replacement resource in Assign Resources");

		Alert.setPresenter(new SwingAlertPresenter());
		Environment.setClientSide(true);
		Environment.setBatchMode(false);
		assertTrue(Alert.allowPopups(), "the physical lock-warning journey requires the production popup gate");
		project.setCollaborationSession(new RejectingCollaborationSession(project));
		click(robot, bounds(dialog.assignButton));
		robot.waitForIdle();
		assertNull(task.findAssignment(resource), "a rejected collaboration lock must leave the task unchanged");
		acknowledgeLockWarning(robot, dialog, task);
		assertTrue(dialog.isShowing(), "the rejected assignment warning must return to the still-open Assign Resources dialog");
		assertNull(task.findAssignment(resource), "acknowledging the lock warning must not assign the resource");

		project.setCollaborationSession(null);
		click(robot, bounds(dialog.assignButton));
		GuiAcceptanceSupport.await(() -> task.findAssignment(resource) != null,
			"Assign Resources did not create the assignment after lock acceptance");
		assertSame(task, task.findAssignment(resource).getTask());
		Color[] assignmentStatusBackground = new Color[1];
		SwingUtilities.invokeAndWait(() -> assignmentStatusBackground[0] = resourceSheet.prepareRenderer(
			resourceSheet.getCellRenderer(resourceRow, nameColumn(resourceSheet)), resourceRow, nameColumn(resourceSheet)).getBackground());
		assertEquals(FlatUiSupport.assignmentCompleteBackground(), assignmentStatusBackground[0],
			"fully assigned resources should use the muted theme status color");

		SwingUtilities.invokeAndWait(dialog::dispose);
		activateWindow(robot);
		click(robot, bounds(assignResources));
		AssignmentDialog reopenedDialog = awaitDialog();
		assertNotSame(dialog, reopenedDialog, "disposed assignment dialogs must be recreated");
		SwingUtilities.invokeAndWait(reopenedDialog::dispose);
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
		original.setStandardRate(new Rate(20.0D, com.microproject.datatype.TimeUnit.HOURS));
		Resource replacement = project.getResourcePool().createScriptedResource();
		replacement.setName("Replacement assignment resource");
		replacement.setStandardRate(new Rate(80.0D, com.microproject.datatype.TimeUnit.HOURS));
		Assignment source = AssignmentService.getInstance().newAssignment(task, original, 1.0D, 0L, this);
		source.setActualWork(source.getWork(null) / 2L, null);
		long actualWork = source.getActualWork(null);
		long remainingWork = source.getRemainingWork();
		assertTaskAssignmentTotals(task, "initial assignment");
		showProject(project);
		GuiAcceptanceSupport.await(() -> window.isShowing() && manager.getCurrentFrame() != null
				&& manager.getCurrentFrame().getActiveSpreadSheet() != null,
			"replacement acceptance project did not become visible");

		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(40);
		SpreadSheet taskSheet = manager.getCurrentFrame().getActiveSpreadSheet();
		click(robot, cellBounds(taskSheet, rowForTask(taskSheet, task), nameColumn(taskSheet)));
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame().getSelectedImpls(false).contains(task),
			"Robot click did not select the replacement task");
		click(robot, bounds(buttonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
			.getString("TaskRibbonTask.title"))));
		AbstractButton assignResources = com.microproject.testsupport.RibbonGuiSupport.findVisibleOrExpand(robot, window, "RibbonAssignResources");
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
		assertSame(assignmentDialog, replaceDialog.getOwner(), "Replace must return keyboard focus to its calling assignment dialog");
		SpreadSheet replacementSheet = replaceDialog.spreadSheetPane.getSpreadSheet();
		click(robot, cellBounds(replacementSheet, rowForResource(replacementSheet, replacement), nameColumn(replacementSheet)));
		GuiAcceptanceSupport.await(() -> replaceDialog.getSelectedResources().contains(replacement),
			"Robot click did not select the replacement resource");
		click(robot, bounds(buttonByText(replaceDialog, com.microproject.strings.Messages.getString("ButtonText.OK"))));

		GuiAcceptanceSupport.await(() -> task.findAssignment(replacement) != null,
			"Replace did not create the replacement assignment");
		GuiAcceptanceSupport.await(() -> source.getRemainingWork() == 0L,
			"Replace did not finish transferring the source remaining work");
		Assignment replacementAssignment = task.findAssignment(replacement);
		assertSame(source, task.findAssignment(original), "actual work must retain the original assignment");
		assertEquals(actualWork, source.getActualWork(null));
		assertEquals(0L, source.getRemainingWork());
		assertNotNull(replacementAssignment);
		assertEquals(0L, replacementAssignment.getActualWork(null));
		assertEquals(remainingWork, replacementAssignment.getRemainingWork());
		assertTaskAssignmentTotals(task, "after Replace");

		try {
			GuiAcceptanceSupport.await(() -> hasKeyboardFocusWithin(assignmentDialog),
				"the modeless Assign Resources dialog did not regain active keyboard focus after Replace closed");
		} catch (AssertionError failure) {
			KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
			failure.addSuppressed(new AssertionError("Focus diagnostics after Replace closed: "
				+ "activeWindow=" + describeWindow(focusManager.getActiveWindow())
				+ ", focusedWindow=" + describeWindow(focusManager.getFocusedWindow())
				+ ", focusOwner=" + describeComponent(focusManager.getFocusOwner())
				+ ", assignmentDialog=" + describeWindow(assignmentDialog)
				+ ", assignmentIsActive=" + assignmentDialog.isActive()
				+ ", assignmentIsFocused=" + assignmentDialog.isFocused()
				+ ", assignmentFocusOwner=" + describeComponent(assignmentDialog.getFocusOwner())
				+ ", replaceDialog=" + describeWindow(replaceDialog)
				+ ", replaceIsVisible=" + replaceDialog.isVisible()
				+ ", replaceOwner=" + describeWindow(replaceDialog.getOwner())));
			throw failure;
		}
		press(robot, KeyEvent.VK_ESCAPE);
		GuiAcceptanceSupport.await(() -> !assignmentDialog.isShowing(),
			"Escape did not close the modeless Assign Resources dialog before undo");
		activateWindow(robot);
		click(robot, cellBounds(taskSheet, rowForTask(taskSheet, task), nameColumn(taskSheet)));
		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Z);
		try {
			GuiAcceptanceSupport.await(() -> task.findAssignment(replacement) == null,
				"Ctrl+Z did not remove the replacement assignment");
		} catch (AssertionError failure) {
			failure.addSuppressed(new AssertionError(assignmentUndoDiagnostics(project, task, source, replacement)));
			throw failure;
		}
		GuiAcceptanceSupport.await(() -> source.getRemainingWork() == remainingWork,
			"Ctrl+Z did not restore the source remaining work");
		assertSame(source, task.findAssignment(original));
		assertEquals(actualWork, source.getActualWork(null));
		assertEquals(remainingWork, source.getRemainingWork());
		assertTaskAssignmentTotals(task, "after physical Undo");
		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Y);
		try {
			GuiAcceptanceSupport.await(() -> task.findAssignment(replacement) != null
					&& source.getRemainingWork() == 0L,
				"Ctrl+Y did not restore the replacement assignment and source remaining work");
		} catch (AssertionError failure) {
			failure.addSuppressed(new AssertionError(assignmentRedoDiagnostics(project, task, source, replacement)));
			throw failure;
		}
		assertSame(source, task.findAssignment(original));
		assertEquals(actualWork, source.getActualWork(null));
		assertEquals(0L, source.getRemainingWork());
		assertEquals(remainingWork, task.findAssignment(replacement).getRemainingWork());
		assertTaskAssignmentTotals(task, "after physical Redo");

		ByteArrayOutputStream saved = new ByteArrayOutputStream();
		MpoFileImporter importer = new MpoFileImporter();
		assertTrue(importer.saveProject(project, saved), "MPO save rejected the redone replacement state");
		Project reloaded = importer.loadProject(new ByteArrayInputStream(saved.toByteArray()));
		NormalTask reloadedTask = null;
		for (Object value : reloaded.getTaskList()) {
			if (value instanceof NormalTask candidate && "Assignment replace task".equals(candidate.getName())) {
				reloadedTask = candidate;
				break;
			}
		}
		assertNotNull(reloadedTask, "MPO reload lost the task identified by its stable name");
		Resource reloadedOriginal = null;
		Resource reloadedReplacement = null;
		for (Object value : reloaded.getResourcePool().getResourceList()) {
			if (value instanceof Resource candidate) {
				if ("Original assignment resource".equals(candidate.getName())) reloadedOriginal = candidate;
				if ("Replacement assignment resource".equals(candidate.getName())) reloadedReplacement = candidate;
			}
		}
		assertNotNull(reloadedOriginal, "MPO reload lost the original resource identified by its stable name");
		assertNotNull(reloadedReplacement, "MPO reload lost the replacement resource identified by its stable name");
		Assignment reloadedSource = reloadedTask.findAssignment(reloadedOriginal);
		Assignment reloadedReplacementAssignment = reloadedTask.findAssignment(reloadedReplacement);
		assertNotNull(reloadedSource, "MPO reload lost the original task-resource assignment association");
		assertNotNull(reloadedReplacementAssignment, "MPO reload lost the replacement task-resource assignment association");
		assertEquals(2, reloadedTask.getAssignments().size(), "MPO reload changed the task's exact resource assignment set");
		assertSame(reloadedTask, reloadedSource.getTask(), "reloaded source assignment must point to the reloaded task");
		assertSame(reloadedOriginal, reloadedSource.getResource(), "reloaded source assignment must point to the named original resource");
		assertSame(reloadedTask, reloadedReplacementAssignment.getTask(), "reloaded replacement assignment must point to the reloaded task");
		assertSame(reloadedReplacement, reloadedReplacementAssignment.getResource(),
			"reloaded replacement assignment must point to the named replacement resource");
		assertEquals(actualWork, reloadedSource.getActualWork(null), "MPO reload changed source actual work");
		assertEquals(0L, reloadedSource.getRemainingWork(), "MPO reload restored source remaining work");
		assertEquals(0L, reloadedReplacementAssignment.getActualWork(null), "MPO reload added actual work to replacement");
		assertEquals(remainingWork, reloadedReplacementAssignment.getRemainingWork(),
			"MPO reload changed replacement remaining work");
		assertTaskAssignmentTotals(reloadedTask, "after MPO reload");
	}

	private static void assertTaskAssignmentTotals(NormalTask task, String state) {
		long assignmentWork = 0L;
		long assignmentActualWork = 0L;
		long assignmentRemainingWork = 0L;
		double assignmentCost = 0.0D;
		for (Object value : task.getAssignments()) {
			Assignment assignment = (Assignment) value;
			assignmentWork += assignment.getWork(null);
			assignmentActualWork += assignment.getActualWork(null);
			assignmentRemainingWork += assignment.getRemainingWork(null);
			assignmentCost += assignment.getCost(null);
		}
		assertEquals(assignmentWork, task.getWork(null), state + ": task Work must equal assignment Work totals");
		assertEquals(assignmentActualWork, task.getActualWork(null), state + ": task Actual Work must equal assignment Actual Work totals");
		assertEquals(assignmentRemainingWork, task.getRemainingWork(null), state + ": task Remaining Work must equal assignment Remaining Work totals");
		assertEquals(assignmentCost, task.getCost(null), 0.01D, state + ": task Cost must equal assignment Cost totals");
	}

	private String assignmentRedoDiagnostics(Project project, NormalTask task, Assignment source,
			Resource replacementResource) throws Exception {
		CompletableFuture<String> result = new CompletableFuture<>();
		SwingUtilities.invokeLater(() -> {
			try {
			var frame = manager.getCurrentFrame();
			var frameUndo = frame == null ? null : frame.getUndoController();
			Assignment replacement = task.findAssignment(replacementResource);
			KeyboardFocusManager focus = KeyboardFocusManager.getCurrentKeyboardFocusManager();
			result.complete("documentActive=" + manager.isDocumentActive()
					+ ", frameActive=" + (frame != null && frame.isActive())
					+ ", frameProjectMatches=" + (frame != null && frame.getProject() == project)
					+ ", frameUndoMatchesProject=" + (frameUndo == project.getUndoController())
					+ ", canUndo=" + project.getUndoController().canUndo()
					+ ", canRedo=" + project.getUndoController().canRedo()
					+ ", pendingHistory=" + project.getUndoController().getEditSupport().hasPendingHistory()
					+ ", undoName=" + project.getUndoController().getUndoName()
					+ ", redoName=" + project.getUndoController().getRedoName()
					+ ", editNames=" + project.getUndoController().getEditNames()
					+ ", activeWindowClass=" + (focus.getActiveWindow() == null ? "null"
							: focus.getActiveWindow().getClass().getName())
					+ ", focusOwnerClass=" + (focus.getFocusOwner() == null ? "null"
							: focus.getFocusOwner().getClass().getName())
					+ ", source=" + describeAssignment(source)
					+ ", replacement=" + describeAssignment(replacement));
				} catch (Throwable failure) {
					result.completeExceptionally(failure);
				}
		});
		try {
			return "EDT snapshot=" + result.get(2, TimeUnit.SECONDS);
		} catch (TimeoutException timeout) {
			return "EDT snapshot timed out after 2 seconds; thread state=" + blockedThreadDiagnostics();
		}
	}

	private String assignmentUndoDiagnostics(Project project, NormalTask task, Assignment source,
			Resource replacementResource) throws Exception {
		return "Undo failure; " + assignmentRedoDiagnostics(project, task, source, replacementResource);
	}

	private static String blockedThreadDiagnostics() {
		ThreadMXBean threads = ManagementFactory.getThreadMXBean();
		long[] ids = threads.getAllThreadIds();
		ThreadInfo[] infos = threads.getThreadInfo(ids, 12);
		StringBuilder result = new StringBuilder();
		int included = 0;
		for (ThreadInfo info : infos) {
			if (info == null) continue;
			String name = info.getThreadName();
			if (!(name.startsWith("AWT-EventQueue") || name.startsWith("Test worker")
					|| name.startsWith("AWT-Windows") || info.getThreadState() == Thread.State.BLOCKED)) continue;
			if (included++ >= 8) break;
			result.append("\n").append(name).append(" state=").append(info.getThreadState())
				.append(" lock=").append(info.getLockName())
				.append(" lockOwnerId=").append(info.getLockOwnerId())
				.append(" lockOwner=").append(info.getLockOwnerName());
			for (StackTraceElement frame : info.getStackTrace())
				result.append("\n  at ").append(frame);
		}
		return result.length() == 0 ? "no EDT/test/blocked thread snapshot available" : result.toString();
	}

	private static String describeAssignment(Assignment assignment) {
		if (assignment == null) return "null";
		return "id=" + assignment.getUniqueId()
				+ "/resource=" + assignment.getResource().getUniqueId()
				+ "/work=" + assignment.getWork(null)
				+ "/actual=" + assignment.getActualWork(null)
				+ "/remaining=" + assignment.getRemainingWork();
	}

	private void showProject(Project project) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			window = new MainRibbonFrame("microProject — Assignment Resources acceptance", null);
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

	private void acknowledgeLockWarning(Robot robot, AssignmentDialog owner, NormalTask task) throws Exception {
		final JDialog[] warning = new JDialog[1];
		final AbstractButton[] acknowledge = new AbstractButton[1];
		final String[] message = new String[1];
		final String[] buttonState = new String[1];
		GuiAcceptanceSupport.await(() -> {
			try {
				SwingUtilities.invokeAndWait(() -> {
					for (Window candidate : Window.getWindows()) {
						if (!(candidate instanceof JDialog dialog) || !dialog.isShowing() || dialog.getOwner() != owner) continue;
						for (Component component : com.microproject.menu.testsupport.UiComponentWalker.flatten(dialog)) {
							if (component instanceof JOptionPane optionPane) {
								Object value = optionPane.getMessage();
								message[0] = value == null ? "null" : value.toString();
								warning[0] = dialog;
								AbstractButton defaultButton = dialog.getRootPane().getDefaultButton();
								acknowledge[0] = com.microproject.menu.testsupport.UiComponentWalker.flatten(dialog).stream()
									.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
									.filter(button -> button.isShowing() && "OK".equals(button.getText()))
									.findFirst().orElse(null);
								buttonState[0] = "default=" + describeButton(defaultButton)
										+ ", visibleAcknowledge=" + describeButton(acknowledge[0]);
								return;
							}
						}
					}
				});
				return warning[0] != null && acknowledge[0] != null;
			} catch (Exception exception) {
				throw new AssertionError("Could not inspect the lock warning dialog", exception);
			}
		}, "lock rejection warning did not open with a visible OK button under the assignment dialog; owner="
				+ describeWindow(owner) + "; buttons=" + buttonState[0]);
		assertTrue(message[0].contains(task.getName()) && message[0].contains("locked by"),
			"unexpected lock warning text: " + message[0]);
		assertTrue(owner.isShowing(), "assignment dialog closed while its lock warning was open");
		click(robot, bounds(acknowledge[0]));
		robot.waitForIdle();
		GuiAcceptanceSupport.await(() -> !warning[0].isShowing(), "physical acknowledgement did not close the lock warning");
	}

	private static String describeButton(AbstractButton button) {
		return button == null ? "null" : "text=" + button.getText() + "/showing=" + button.isShowing()
				+ "/visible=" + button.isVisible() + "/enabled=" + button.isEnabled();
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

	private AbstractButton buttonByText(String text) throws Exception {
		return buttonByText(window, text);
	}

	private AbstractButton buttonByText(java.awt.Component root, String text) throws Exception {
		final AbstractButton[] button = new AbstractButton[1];
		SwingUtilities.invokeAndWait(() -> button[0] = com.microproject.menu.testsupport.UiComponentWalker.flatten(root).stream()
			.filter(component -> component instanceof AbstractButton || component instanceof org.pushingpixels.flamingo.api.common.AbstractCommandButton)
			.map(RibbonGuiButton::adapt)
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

	private static boolean hasKeyboardFocusWithin(Window dialog) {
		if (!dialog.isActive() || KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow() != dialog)
			return false;
		Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
		return focusOwner != null && (focusOwner == dialog || SwingUtilities.isDescendingFrom(focusOwner, dialog));
	}

	private static String describeWindow(Window window) {
		if (window == null) return "null";
		return window.getClass().getName() + "[showing=" + window.isShowing()
			+ ",active=" + window.isActive() + ",focused=" + window.isFocused()
			+ ",owner=" + (window.getOwner() == null ? "null" : window.getOwner().getClass().getName()) + "]";
	}

	private static String describeComponent(Component component) {
		if (component == null) return "null";
		return component.getClass().getName() + "[name=" + component.getName()
			+ ",focusOwner=" + component.isFocusOwner()
			+ ",window=" + describeWindow(javax.swing.SwingUtilities.getWindowAncestor(component)) + "]";
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
		public Task tryAcquireTasks(Iterable<Task> tasks) {
			if (tasks == null) return null;
			java.util.Iterator<Task> iterator = tasks.iterator();
			return iterator.hasNext() ? iterator.next() : null;
		}
	}
}
