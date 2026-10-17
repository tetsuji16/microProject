/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.Dialog;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JComboBox;
import javax.swing.JTabbedPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPopupMenu;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.JTable;
import javax.swing.text.JTextComponent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.microproject.dialog.BaselineDialog;
import com.microproject.dialog.TaskInspectorPanel;
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
import com.microproject.ui.ribbon.RibbonDisplayMode;
import com.microproject.ui.ribbon.RibbonDisplayPreferences;
import com.microproject.ribbon.RibbonCommandResult;
import com.microproject.ui.diagnostics.UiButtonDiagnostics;
import com.microproject.exchange.MpoFileImporter;
import com.microproject.collaboration.CollaborationSession;
import com.microproject.field.Field;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.transform.CommonTransformFactory;
import com.microproject.grouping.core.transform.sorting.TaskSortSpec;
import com.microproject.grouping.core.transform.ViewConfiguration;
import com.microproject.grouping.core.transform.ViewTransformer;
import com.microproject.job.JobQueue;
import com.microproject.job.Job;
import com.microproject.menu.testsupport.UiComponentWalker;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.AssignmentService;
import com.microproject.pm.graphic.gantt.Gantt;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetModel;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetPopupMenu;
import com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheetModel;
import com.microproject.pm.graphic.views.GanttView;
import com.microproject.pm.graphic.views.UsageDetailView;
import com.microproject.pm.graphic.views.PertView;
import com.microproject.pm.graphic.views.TreeView;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.resource.Resource;
import com.microproject.options.CalendarOption;
import com.microproject.pm.calendar.WorkingCalendar;
import com.microproject.pm.graphic.views.ResourceView;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.ProjectFactory;
import com.microproject.pm.task.Task;
import com.microproject.pm.snapshot.Snapshottable;
import com.microproject.strings.Messages;
import com.microproject.session.SessionFactory;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.testsupport.WindowAcceptanceAssertions;
import com.microproject.testsupport.RibbonGuiButton;
import com.microproject.testsupport.RibbonGuiSupport;
import com.microproject.testsupport.GuiCommandAcceptanceFixture;
import com.microproject.testsupport.GuiPhysicalRouteAdapter;
import com.microproject.testsupport.DialogLayoutAssertions;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.Environment;
import com.microproject.util.Alert;
import com.microproject.util.UiDispatch;
import com.microproject.ui.util.SwingAlertPresenter;

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
	private boolean previousClientSide;
	private boolean previousBatchMode;
	private String previousUiDebug;
	private static String previousQueueTrace;
	private static final ConcurrentLinkedQueue<String> queueLifecycleMessages = new ConcurrentLinkedQueue<>();
	private static final Logger[] queueLifecycleLoggers = {
		Logger.getLogger(GraphicManager.class.getName()),
		Logger.getLogger(SessionFactory.class.getName())
	};
	private static final Level[] previousQueueLoggerLevels = new Level[queueLifecycleLoggers.length];
	private static final Handler queueLifecycleHandler = new Handler() {
		@Override public void publish(LogRecord record) {
			if (record != null && record.getMessage() != null)
				queueLifecycleMessages.add(record.getMessage());
		}
		@Override public void flush() { }
		@Override public void close() { }
	};
	private JobQueue previousJobQueue;
	private Locale previousFormatLocale;
	private RibbonDisplayMode previousShellDisplayMode;
	private Logger diagnosticsLogger;
	private Handler diagnosticsHandler;
	private Level previousDiagnosticsLevel;
	private ViewConfiguration taskSortViewToRestore;
	private TaskSortSpec previousTaskSortSpec;
	private final ConcurrentLinkedQueue<String> diagnosticMessages = new ConcurrentLinkedQueue<>();
	private volatile boolean failUpdateTasksRoute;

	@BeforeAll
	static void enableQueueLifecycleTraceForClass() {
		previousQueueTrace = System.getProperty("microproject.session.traceQueueLifecycle");
		System.setProperty("microproject.session.traceQueueLifecycle", "true");
		queueLifecycleHandler.setLevel(Level.ALL);
		for (int index = 0; index < queueLifecycleLoggers.length; index++) {
			Logger logger = queueLifecycleLoggers[index];
			previousQueueLoggerLevels[index] = logger.getLevel();
			logger.setLevel(Level.ALL);
			logger.addHandler(queueLifecycleHandler);
		}
	}

	@AfterAll
	static void restoreQueueLifecycleTraceForClass() {
		for (int index = 0; index < queueLifecycleLoggers.length; index++) {
			Logger logger = queueLifecycleLoggers[index];
			logger.removeHandler(queueLifecycleHandler);
			logger.setLevel(previousQueueLoggerLevels[index]);
		}
		queueLifecycleMessages.stream()
			.filter(message -> message.startsWith("SESSION_QUEUE_CLEAR") || message.startsWith("EXTERNAL_COMMAND_START"))
			.forEach(message -> System.out.println("QUEUE_LIFECYCLE_TRACE " + message));
		if (previousQueueTrace == null)
			System.clearProperty("microproject.session.traceQueueLifecycle");
		else
			System.setProperty("microproject.session.traceQueueLifecycle", previousQueueTrace);
	}

	@BeforeEach
	void captureClientSide() {
		previousJobQueue = SessionFactory.getInstance().getJobQueue();
		previousClientSide = Environment.isClientSide();
		previousBatchMode = Environment.isBatchMode();
		previousShellDisplayMode = RibbonDisplayPreferences.load();
		RibbonDisplayPreferences.save(RibbonDisplayMode.ALWAYS_SHOW);
	}

	@AfterEach
	void closeWindow() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			for (Window candidate : Window.getWindows()) {
				if (candidate instanceof TaskInformationDialog || candidate instanceof UpdateTaskDialog || candidate instanceof TimesheetDialog
					|| candidate instanceof CalendarDialogBox)
					candidate.dispose();
			}
			if (taskSortViewToRestore != null && previousTaskSortSpec != null)
				taskSortViewToRestore.getTransform().setTaskSortSpec(previousTaskSortSpec);
			taskSortViewToRestore = null;
			previousTaskSortSpec = null;
			if (manager != null)
				manager.cleanUp();
			for (Window candidate : Window.getWindows()) {
				if (candidate instanceof MainRibbonFrame)
					candidate.dispose();
			}
		});
		if (previousShellDisplayMode != null)
			RibbonDisplayPreferences.save(previousShellDisplayMode);
		if (diagnosticsLogger != null && diagnosticsHandler != null)
			diagnosticsLogger.removeHandler(diagnosticsHandler);
		if (diagnosticsLogger != null)
			diagnosticsLogger.setLevel(previousDiagnosticsLevel);
		Environment.setRibbonUI(previousRibbonUi);
		Environment.setNewLook(previousNewLook);
		Environment.setClientSide(previousClientSide);
		Environment.setBatchMode(previousBatchMode);
		Alert.setPresenter(null);
		if (previousUiDebug == null)
			System.clearProperty("microproject.ui.debug");
		else
			System.setProperty("microproject.ui.debug", previousUiDebug);
		SessionFactory.getInstance().setJobQueue(previousJobQueue);
		if (previousFormatLocale != null)
			Locale.setDefault(Locale.Category.FORMAT, previousFormatLocale);
	}

	/** Leveling that adjusts individual assignments stores its delay on the assignments; the task start moves with it. */
	private static long levelingDelay(com.microproject.pm.task.Task task) {
		long total = com.microproject.datatype.Duration.millis(task.getLevelingDelay());
		if (task instanceof NormalTask normal) for (Object value : normal.getAssignments())
			total += com.microproject.datatype.Duration.millis(((com.microproject.pm.assignment.Assignment) value).getLevelingDelay());
		return total;
	}

	@Test
	void robotSaveShowsMpoRecoveryCopyAfterLockFailure() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		previousClientSide = Environment.isClientSide();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		Environment.setClientSide(true);
		Alert.setPresenter(new SwingAlertPresenter());

		NormalTask task = createTask();
		Project project = task.getOwningProject();
		Path target = Files.createTempFile("mpo-save-feedback-", ".mpo");
		Path lockPath = Path.of(target + ".lock");
		Path recoveryCopy = null;
		Project[] cleanupProject = new Project[1];
		Dialog[] alert = new Dialog[1];
		AbstractButton[] staleSaveButton = new AbstractButton[1];
		javax.swing.Action[] staleSaveAction = new javax.swing.Action[1];
		boolean[] recoverySaveCompleted = new boolean[1];
		try {
			MpoFileImporter importer = new MpoFileImporter();
			try (OutputStream output = Files.newOutputStream(target)) {
				assertTrue(importer.saveProject(project, output), "the initial MPO fixture must save");
			}
			byte[] originalArchive = Files.readAllBytes(target);
			project = new MpoFileImporter().loadProject(new ByteArrayInputStream(originalArchive));
			task = taskNamed(project, "Ribbon information acceptance");
			long taskId = task.getUniqueId();
			project.setFileName(target.toString());
			task.setName("Edit preserved in recovery copy");
			Files.createDirectory(lockPath);
			Project openedProject = project;
			cleanupProject[0] = openedProject;
			ProjectFactory.getInstance().addProject(openedProject, false, false);
			showProject(openedProject);
			JobQueue saveQueue = manager.getJobQueue();
			SessionFactory.getInstance().setJobQueue(saveQueue);
			assertSame(saveQueue, SessionFactory.getInstance().getJobQueue(),
				"the File/Save route must start with the fixture's active job queue");
			assertSame(saveQueue, SessionFactory.getInstance().getLocalSession().getJobQueue(),
				"the local save session must use the fixture's active job queue");
			GuiAcceptanceSupport.await(() -> manager.getCurrentFrame() != null
				&& manager.getCurrentFrame().getProject() == openedProject, "the MPO project did not open");

			Robot robot = new com.microproject.testsupport.GuiRobot();
			robot.setAutoDelay(45);
			activateWindow(robot, window);
			AbstractButton fileTab = findShowingButtonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
				.getString("FileRibbonTask.title"));
			clickAndAwaitSelected(robot, fileTab, "Robot click did not select the File ribbon tab");
			GuiAcceptanceSupport.await(fileTab::isSelected, "Robot click did not select the File ribbon tab");
			AbstractButton save = findShowingButtonByCommand("RibbonSaveProject");
			staleSaveButton[0] = save;
			staleSaveAction[0] = manager.getAction(com.microproject.menu.MenuActionConstants.ACTION_SAVE_PROJECT);
			assertNotNull(staleSaveAction[0], "the visible Save command must resolve to its canonical action for teardown testing");
			assertTrue(save.isEnabled(), "Save must be enabled for the edited MPO project");
			ConcurrentLinkedQueue<String> saveActionEvents = new ConcurrentLinkedQueue<>();
			save.addActionListener(event -> saveActionEvents.add(Instant.now()
				+ " factoryQueue=" + queueIdentity(SessionFactory.getInstance().getJobQueue())
				+ " localQueue=" + queueIdentity(SessionFactory.getInstance().getLocalSession().getJobQueue())));
			click(robot, save);

			GuiAcceptanceSupport.await(() -> {
				for (Window candidate : Window.getWindows())
					if (candidate instanceof Dialog dialog && dialog.isShowing()
						&& UiComponentWalker.flatten(dialog).stream().filter(JLabel.class::isInstance)
							.map(JLabel.class::cast).anyMatch(label -> label.getText() != null
								&& label.getText().contains(target.getFileName() + ".conflict-"))) {
						alert[0] = dialog;
						return true;
					}
				return false;
			}, "the physical Save route did not display the recovery-copy path");
			assertArrayEquals(originalArchive, Files.readAllBytes(target), "a failed save must leave the shared archive untouched");
			assertSame(saveQueue, SessionFactory.getInstance().getJobQueue(),
				"the session queue must remain attached through the Save recovery result");
			assertSame(saveQueue, SessionFactory.getInstance().getLocalSession().getJobQueue(),
				"the local save session queue must remain attached through the Save recovery result");
			assertEquals("saveProject", manager.getLastRibbonCommandResult().commandId(),
				"the physical Save route must reach its canonical ribbon command");
			assertFalse(manager.getLastRibbonCommandResult().status() == RibbonCommandResult.Status.FAILED,
				"the physical Save command must not fail before producing its recovery result");
			try (var files = Files.list(target.getParent())) {
				recoveryCopy = files.filter(path -> path.getFileName().toString().startsWith(target.getFileName() + ".conflict-"))
					.filter(path -> path.getFileName().toString().endsWith(".mpo"))
					.findFirst().orElseThrow(() -> new AssertionError("the alert named a recovery copy that was not written"));
			}
			Project recovered;
			try (var input = Files.newInputStream(recoveryCopy)) {
				recovered = new MpoFileImporter().loadProject(input);
			}
			assertEquals("Edit preserved in recovery copy", recovered.findByUniqueId(taskId).getName(),
				"the recovery copy must retain the user's unsaved edit");
			robot.keyPress(KeyEvent.VK_ENTER);
			robot.keyRelease(KeyEvent.VK_ENTER);
			GuiAcceptanceSupport.await(() -> !alert[0].isShowing(), "the recovery alert did not close after Enter");
			GuiAcceptanceSupport.awaitInputAndEventQueueIdle(robot, "Save recovery alert dismissal");
			assertEquals(1, saveActionEvents.size(),
				"the Save button must invoke the shared command exactly once; events=" + saveActionEvents);
			assertTrue(saveActionEvents.peek().contains("factoryQueue=" + queueIdentity(saveQueue)
				+ " localQueue=" + queueIdentity(saveQueue)),
				"the Save button action must observe the attached queue; events=" + saveActionEvents);
			recoverySaveCompleted[0] = true;
		} finally {
			if (alert[0] != null && alert[0].isShowing())
				SwingUtilities.invokeAndWait(alert[0]::dispose);
			if (cleanupProject[0] != null) {
				ProjectFactory factory = ProjectFactory.getInstance();
				// Removing a project changes Swing-backed portfolio models. Keep that
				// mutation on the EDT, as production close routes do, while retaining a
				// unique name so JobQueue duplicate suppression cannot discard cleanup.
				UiDispatch.setDispatcher(new com.microproject.ui.util.SwingUiDispatcher());
				Job removal = factory.getRemoveProjectJob(cleanupProject[0], false, false, true);
				if (removal != null) {
					CountDownLatch removalComplete = new CountDownLatch(1);
					removal.setName("gui-acceptance-remove-" + System.nanoTime());
					removal.addCompletionRunnable(removalComplete::countDown);
					SessionFactory.getInstance().getSession(cleanupProject[0].isLocal()).schedule(removal);
					assertTrue(removalComplete.await(30, TimeUnit.SECONDS),
						"the project removal job did not finish before the GUI manager closed");
				}
				GuiAcceptanceSupport.await(() -> factory.getPortfolio().getNodeModel().search(cleanupProject[0]) == null,
					"the test project did not leave the portfolio before the GUI manager closed");
			}
			if (recoverySaveCompleted[0] && staleSaveButton[0] != null && manager != null) {
				GraphicManager closedManager = manager;
				SwingUtilities.invokeAndWait(() -> {
					closedManager.cleanUp();
					window.dispose();
				});
				manager = null;
				SessionFactory.getInstance().setJobQueue(null);
				// The retained component is no longer showing and may be disabled, so
				// doClick() can silently skip its listener. Deliver the retained
				// canonical action directly to model a late event already queued before
				// window teardown; this is an integration lifecycle check, not a second
				// physical click.
				SwingUtilities.invokeAndWait(() -> staleSaveAction[0].actionPerformed(
					new java.awt.event.ActionEvent(staleSaveButton[0], java.awt.event.ActionEvent.ACTION_PERFORMED,
						staleSaveButton[0].getActionCommand())));
				assertEquals(RibbonCommandResult.Status.REJECTED, closedManager.getLastRibbonCommandResult().status(),
					"a late Save action from a closed window must be rejected before reaching the shared session");
				assertEquals("manager-closed", closedManager.getLastRibbonCommandResult().reason());
				String lateSaveTrace = queueLifecycleMessages.stream()
					.filter(message -> message.startsWith("EXTERNAL_COMMAND_START id=saveProject"))
					.reduce((first, second) -> second).orElse("");
				assertTrue(lateSaveTrace.contains("cleanedUp=true"),
					"the retained action must reach the closed manager guard; trace=" + lateSaveTrace);
				assertNull(SessionFactory.getInstance().getJobQueue(),
					"a stale Save action must not republish a closed manager's queue");
				assertNull(SessionFactory.getInstance().getLocalSession().getJobQueue(),
					"a stale Save action must not install a replacement queue on the shared session");
			}
			if (recoveryCopy != null) Files.deleteIfExists(recoveryCopy);
			Files.deleteIfExists(lockPath);
			Files.deleteIfExists(target);
		}
	}

	@Test
	void robotOpensIssue590DialogsWithoutClippedText() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		previousUiDebug = System.getProperty("microproject.ui.debug");
		System.setProperty("microproject.ui.debug", "true");
		diagnosticsLogger = Logger.getLogger("com.microproject.ui.diagnostics.UiButtonDiagnostics");
		previousDiagnosticsLevel = diagnosticsLogger.getLevel();
		diagnosticsLogger.setLevel(Level.FINE);
		diagnosticsHandler = new Handler() {
			@Override public void publish(LogRecord record) { diagnosticMessages.add(record.getMessage()); }
			@Override public void flush() { }
			@Override public void close() { }
		};
		diagnosticsLogger.addHandler(diagnosticsHandler);
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);

		NormalTask task = createTask();
		showProject(task.getOwningProject());
		SwingUtilities.invokeAndWait(() -> window.setSize(1600, 700));
		GuiAcceptanceSupport.await(() -> window.isShowing() && manager.getCurrentFrame() != null
				&& manager.getCurrentFrame().getActiveSpreadSheet() != null,
			"full ribbon task window did not become visible");

		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		activateWindow(robot, window);
		SpreadSheet sheet = manager.getCurrentFrame().getActiveSpreadSheet();
		clickUntilSelected(robot, window, sheet, rowForTask(sheet, task), nameColumn(sheet));
		AbstractButton taskTab = findShowingButtonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
			.getString("TaskRibbonTask.title"));
		clickAndAwaitSelected(robot, taskTab, "Robot click did not select the Task ribbon tab");
		GuiAcceptanceSupport.await(taskTab::isSelected, "Robot click did not select the Task ribbon tab");
		AbstractButton update = RibbonGuiSupport.findVisibleOrExpand(robot, window, "RibbonUpdateTasks");
		GuiAcceptanceSupport.await(update::isEnabled, "Update Tasks remained disabled after selecting a task");
		click(robot, update);

		GuiAcceptanceSupport.await(() -> findUpdateTaskDialog() != null,
			"Task > Update did not open the Update Tasks dialog after a Robot click");
		long taskStartBeforeCancel = task.getStart();
		long taskEndBeforeCancel = task.getEnd();
		long taskDurationBeforeCancel = task.getDuration();
		double taskProgressBeforeCancel = task.getPercentComplete();
		boolean taskDirtyBeforeCancel = task.isDirty();
		boolean projectDirtyBeforeCancel = task.getOwningProject().needsSaving();
		boolean canUndoBeforeCancel = task.getOwningProject().getUndoController().canUndo();
		UpdateTaskDialog dialog = findUpdateTaskDialog();
		capture(robot, dialog, "issue-590-update-tasks.png");
		assertUpdateDialogComponentsFit(dialog);

		press(robot, KeyEvent.VK_ESCAPE);
		GuiAcceptanceSupport.await(() -> findUpdateTaskDialog() == null,
			"Escape did not close Update Tasks after visual assertions");
		GuiAcceptanceSupport.await(() -> diagnosticContains("UI_COMMAND_RESULT id=RibbonUpdateTasks reason=dialog-cancelled"),
			"closing the Update Tasks dialog with Escape must be recorded as an intentional cancel outcome");
		assertFalse(diagnosticContains("UI_COMMAND_FAILURE id=RibbonUpdateTasks reason=no-observable-state-change"),
			"an observed modal cancel with unchanged project state is not a silent command failure");
		click(robot, update);
		GuiAcceptanceSupport.await(() -> findUpdateTaskDialog() != null,
			"Update Tasks could not be reopened after Cancel");
		press(robot, KeyEvent.VK_ESCAPE);
		GuiAcceptanceSupport.await(() -> findUpdateTaskDialog() == null,
			"the reopened Update Tasks dialog did not close through Cancel");
		GuiAcceptanceSupport.await(() -> diagnosticCount("UI_COMMAND_RESULT id=RibbonUpdateTasks reason=dialog-cancelled") >= 2,
			"each completed modal Cancel must be observed, including a reopened dialog");
		assertEquals(taskStartBeforeCancel, task.getStart());
		assertEquals(taskEndBeforeCancel, task.getEnd());
		assertEquals(taskDurationBeforeCancel, task.getDuration());
		assertEquals(taskProgressBeforeCancel, task.getPercentComplete(), 0.00001D);
		assertEquals(taskDirtyBeforeCancel, task.isDirty());
		assertEquals(projectDirtyBeforeCancel, task.getOwningProject().needsSaving());
		assertEquals(canUndoBeforeCancel, task.getOwningProject().getUndoController().canUndo(),
			"cancelling both dialog instances must not add Undo work");

		failUpdateTasksRoute = true;
		click(robot, update);
		GuiAcceptanceSupport.await(() -> ribbonCommandResult(update) != null
				&& ribbonCommandResult(update).status() == RibbonCommandResult.Status.FAILED
				&& diagnosticContains("UI_COMMAND_FAILURE id=RibbonUpdateTasks reason=command-failed"),
			"a real exception from the routed command must remain visible in the semantic result and diagnostics");
		assertTrue(diagnosticContains("commandReason=IllegalStateException"),
			"the true command failure cause must remain in the diagnostic outcome");
		assertEquals(taskStartBeforeCancel, task.getStart());
		assertEquals(taskEndBeforeCancel, task.getEnd());
		assertEquals(taskDurationBeforeCancel, task.getDuration());
		assertEquals(taskProgressBeforeCancel, task.getPercentComplete(), 0.00001D);
		assertEquals(taskDirtyBeforeCancel, task.isDirty());
		assertEquals(projectDirtyBeforeCancel, task.getOwningProject().needsSaving());
		assertEquals(canUndoBeforeCancel, task.getOwningProject().getUndoController().canUndo(),
			"a rejected route failure must preserve model state and Undo history");
		click(robot, update);
		GuiAcceptanceSupport.await(() -> findUpdateTaskDialog() != null,
			"Update Tasks did not recover after the injected command failure");
		press(robot, KeyEvent.VK_ESCAPE);
		GuiAcceptanceSupport.await(() -> findUpdateTaskDialog() == null,
			"recovered Update Tasks did not close through Cancel");

		AbstractButton recurring = findShowingButtonByCommand("RibbonInsertRecurring");
		click(robot, recurring);
		GuiAcceptanceSupport.await(() -> findRecurringTaskDialog() != null,
			"Task > Insert > Recurring Task did not open after a Robot click");
		Dialog recurringDialog = findRecurringTaskDialog();
		capture(robot, recurringDialog, "issue-590-recurring-task.png");
		assertRecurringDialogComponentsFit(recurringDialog);
	}

	private static RibbonCommandResult ribbonCommandResult(AbstractButton button) {
		javax.swing.Action action = button.getAction();
		Object result = action == null ? null : action.getValue(RibbonCommandResult.RESULT_ACTION_PROPERTY);
		if (!(result instanceof RibbonCommandResult) && action != null) {
			javax.swing.Action delegate = UiButtonDiagnostics.unwrapAction(action);
			result = delegate == null ? null : delegate.getValue(RibbonCommandResult.RESULT_ACTION_PROPERTY);
		}
		return result instanceof RibbonCommandResult commandResult ? commandResult : null;
	}

	private boolean diagnosticContains(String fragment) {
		return diagnosticMessages.stream().anyMatch(message -> message.contains(fragment));
	}

	private long diagnosticCount(String fragment) {
		return diagnosticMessages.stream().filter(message -> message.contains(fragment)).count();
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
		Resource assignmentResource = task.getProject().getResourcePool().createScriptedResource();
		assignmentResource.setName("Task information assigned resource");
		AssignmentService.getInstance().newAssignment(task, assignmentResource, 1D, 0L, this);
		showProject(task.getOwningProject());
		SwingUtilities.invokeAndWait(() -> window.setSize(1600, 700));
		GuiAcceptanceSupport.await(() -> window.isShowing() && manager.getCurrentFrame() != null
				&& manager.getCurrentFrame().getActiveSpreadSheet() != null,
			"full ribbon task window did not become visible");

		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		activateWindow(robot, window);
		SpreadSheet sheet = manager.getCurrentFrame().getActiveSpreadSheet();
		int row = rowForTask(sheet, task);
		int column = nameColumn(sheet);
		clickUntilSelected(robot, window, sheet, row, column);

		AbstractButton taskTab = findShowingButtonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
			.getString("TaskRibbonTask.title"));
		clickAndAwaitSelected(robot, taskTab, "Robot click did not select the Task ribbon tab");
		GuiAcceptanceSupport.await(taskTab::isSelected, "Robot click did not select the Task ribbon tab");
		AbstractButton information = findShowingButtonByCommand("RibbonTaskInformation");
		GuiAcceptanceSupport.await(information::isEnabled,
			"Task Properties > Information remained disabled after selecting a task");
		RibbonCommandResult previousOutcome = manager.getLastRibbonCommandResult();
		click(robot, information);

		GuiAcceptanceSupport.await(() -> findTaskInformationDialog() != null,
			"Task Properties > Information did not open Task Information after a Robot click");
		assertRibbonOutcome(manager, previousOutcome, RibbonCommandResult.Status.DISPATCHED, task.getUniqueId());
		TaskInformationDialog dialog = findTaskInformationDialog();
		assertEquals(Messages.getString("TaskInformationDialog.TaskInformation") + " - " + task.getId(), dialog.getTitle());
		assertTextStyleTabComponentsFit(dialog);
		assertTabsUseAvailableScreenHeight(dialog);
		assertTaskAssignmentShowsResource(robot, dialog, assignmentResource.getName());
		capture(robot, dialog);
	}

	private static void assertTaskAssignmentShowsResource(Robot robot, TaskInformationDialog dialog,
			String expectedResourceName) throws Exception {
		JTabbedPane tabs = findTabbedPane(dialog);
		int resourcesTab = tabs.indexOfTab(Messages.getString("TaskInformationDialog.Resources")); //$NON-NLS-1$
		assertTrue(resourcesTab >= 0, "Task Information must expose the Resources tab");
		Rectangle tabsBounds = boundsOnScreen(tabs);
		Rectangle tabBounds = tabs.getBoundsAt(resourcesTab);
		click(robot, new Rectangle(tabsBounds.x + tabBounds.x, tabsBounds.y + tabBounds.y,
				tabBounds.width, tabBounds.height));
		GuiAcceptanceSupport.await(() -> tabs.getSelectedIndex() == resourcesTab,
				"Robot click did not select Task Information's Resources tab");
		SpreadSheet assignments = findSpreadSheet(tabs.getComponentAt(resourcesTab));
		assertNotNull(assignments, "Task Information Resources tab must contain its assignment spreadsheet");
		assertEquals(UsageDetailView.resourceAssignmentSpreadsheetCategory, assignments.getSpreadSheetCategory());
		assertTrue(assignments.getRowCount() > 0, "Task Information must show the task's resource assignment");
		JScrollPane assignmentViewport = enclosingScrollPane(assignments);
		assertNotNull(assignmentViewport, "Task Information assignment spreadsheet must have a scroll viewport");
		JComponent resourcesView = (JComponent) ((JScrollPane) tabs.getComponentAt(resourcesTab))
			.getViewport().getView();
		Rectangle assignmentBounds = SwingUtilities.convertRectangle(assignmentViewport.getParent(),
			assignmentViewport.getBounds(), resourcesView);
		assertTrue(assignmentBounds.height * 2 >= resourcesView.getHeight(),
			"Task Information Resources tab must give its growable assignment table most of the viewport; table="
				+ assignmentBounds + ", tabContent=" + resourcesView.getBounds());
		SpreadSheetModel model = (SpreadSheetModel) assignments.getModel();
		String displayedResourceName = null;
		for (int viewColumn = 0; viewColumn < assignments.getColumnModel().getColumnCount(); viewColumn++) {
			int modelColumn = assignments.convertColumnIndexToModel(viewColumn);
			Field field = model.getFieldInColumn(modelColumn);
			if (field != null && field.isNameField()) {
				displayedResourceName = String.valueOf(model.getValueAt(0, modelColumn));
				break;
			}
		}
		assertEquals(expectedResourceName, displayedResourceName,
				"Task Information must show the assigned resource name");
	}

	private static SpreadSheet findSpreadSheet(Component component) {
		if (component instanceof SpreadSheet sheet)
			return sheet;
		if (component instanceof java.awt.Container container)
			for (Component child : container.getComponents()) {
				SpreadSheet sheet = findSpreadSheet(child);
				if (sheet != null)
					return sheet;
			}
		return null;
	}

	private static JScrollPane enclosingScrollPane(Component component) {
		for (java.awt.Container parent = component.getParent(); parent != null; parent = parent.getParent())
			if (parent instanceof JScrollPane scrollPane)
				return scrollPane;
		return null;
	}

	private static void assertTabsUseAvailableScreenHeight(TaskInformationDialog dialog) throws Exception {
		GraphicsConfiguration configuration = dialog.getGraphicsConfiguration();
		assertNotNull(configuration, "Task Information must belong to a display configuration");
		Rectangle monitor = configuration.getBounds();
		Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(configuration);
		Rectangle usable = new Rectangle(monitor.x + insets.left, monitor.y + insets.top,
				monitor.width - insets.left - insets.right, monitor.height - insets.top - insets.bottom);
		SwingUtilities.invokeAndWait(() -> {
			assertTrue(usable.contains(dialog.getBounds()), () -> "Task Information dialog must fit the usable monitor bounds; dialog="
					+ dialog.getBounds() + ", usable=" + usable);
			JTabbedPane tabs = findTabbedPane(dialog);
			for (int tabIndex = 0; tabIndex < tabs.getTabCount(); tabIndex++) {
				tabs.setSelectedIndex(tabIndex);
				layoutTree(dialog);
				JScrollPane tab = (JScrollPane) tabs.getComponentAt(tabIndex);
				int maxViewportHeight = Math.max(120, usable.height - 160);
				int preferredContentHeight = tab.getViewport().getView().getPreferredSize().height;
				if (preferredContentHeight <= maxViewportHeight)
					assertFalse(tab.getVerticalScrollBar().isVisible(),
							"Tab " + tabs.getTitleAt(tabIndex)
								+ " should not scroll when its content fits within the available screen height");
				else
					assertTrue(tab.getVerticalScrollBar().isVisible(),
							"Tab " + tabs.getTitleAt(tabIndex)
								+ " should remain scrollable when its content exceeds the available screen height");
			}
			tabs.setSelectedIndex(0);
			layoutTree(dialog);
			JScrollPane generalTab = (JScrollPane) tabs.getComponentAt(0);
			Component generalContent = generalTab.getViewport().getView();
			Rectangle nameLabelBounds = componentBounds(generalContent,
				Messages.getString("Field.name") + ":"); //$NON-NLS-1$
			assertNotNull(nameLabelBounds, "Task Information General tab must expose its task name label");
			int expectedTopAllowance = ((JComponent) generalContent).getInsets().top
				+ nameLabelBounds.height;
			assertTrue(nameLabelBounds.y <= expectedTopAllowance,
				"Task Information General tab must keep its first form row near the viewport top; top gap="
					+ nameLabelBounds.y + "px, content=" + generalContent.getBounds() + ", viewport="
					+ generalTab.getViewport().getExtentSize() + ", allowed=" + expectedTopAllowance);
		});
	}

	private static Rectangle componentBounds(Component root, String labelText) {
		if (root instanceof JLabel label && labelText.equals(label.getText()))
			return new Rectangle(0, 0, label.getWidth(), label.getHeight());
		if (!(root instanceof java.awt.Container container))
			return null;
		for (Component child : container.getComponents()) {
			if (!child.isVisible())
				continue;
			Rectangle bounds = componentBounds(child, labelText);
			if (bounds != null) {
				bounds.translate(child.getX(), child.getY());
				return bounds;
			}
		}
		return null;
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
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		activateWindow(robot, window);
		AbstractButton viewTab = findShowingButtonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
				.getString("ViewRibbonTask.title"));
		clickAndAwaitSelected(robot, viewTab, "Robot click did not select the View ribbon tab");
		AbstractButton taskUsageButton = findShowingButtonByCommand("RibbonTaskUsageDetail");
		click(robot, taskUsageButton);
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame().getActiveTopView() instanceof UsageDetailView,
			"Task Usage did not become the active view after a Robot click");
		UsageDetailView taskUsage = (UsageDetailView)manager.getCurrentFrame().getActiveTopView();
		assertNotNull(taskUsage.getSpreadSheet(), "task usage left spreadsheet was not initialized");
		assertNotNull(taskUsage.getTimeSpreadSheet(), "task usage time spreadsheet was not initialized");
		assertTrue(taskUsage.getSpreadSheet().getModel().getRowCount() > 0,
			"task usage spreadsheet did not expose the fixture task");

		AbstractButton resourceUsageButton = findShowingButtonByCommand("RibbonResourceUsageDetail");
		click(robot, resourceUsageButton);
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

		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		activateWindow(robot, window);
		AbstractButton viewTab = findShowingButtonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
				.getString("ViewRibbonTask.title"));
		clickAndAwaitSelected(robot, viewTab, "Robot click did not select the View ribbon tab");

		SwingUtilities.invokeAndWait(() -> manager.getCurrentFrame().activateView(com.microproject.menu.MenuActionConstants.ACTION_HISTOGRAM));
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame().getActiveBottomView() != null
			&& manager.getCurrentFrame().getMainView().getBottomComponent() != null,
			"Histogram bottom view did not become visible");
		assertTrue(manager.getCurrentFrame().getMainView().getBottomComponent().isShowing(),
			"Histogram bottom component must be physically visible");

		AbstractButton details = findShowingButtonByCommand("RibbonDetails");
		click(robot, details);
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame().getActiveBottomView() == null
			&& manager.getCurrentFrame().getMainView().getBottomComponent() == null,
			"Details did not close the bottom view after a Robot click");
		GuiAcceptanceSupport.await(() -> !details.isSelected(),
			"Details remained selected after closing the bottom view");

		click(robot, details);
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame().getActiveBottomView() != null
			&& manager.getCurrentFrame().getMainView().getBottomComponent() != null,
			"Details did not restore the last bottom view after a Robot click");
		GuiAcceptanceSupport.await(details::isSelected,
			"Details was not selected after restoring the bottom view");

		click(robot, details);
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

		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		activateWindow(robot, window);
		AbstractButton projectTab = findShowingButtonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
			.getString("ProjectRibbonTask.title"));
		clickAndAwaitSelected(robot, projectTab, "Robot click did not select the Project ribbon tab");
		AbstractButton calendarOptions = findShowingButtonByCommand("RibbonCalendarOptions");
		GuiAcceptanceSupport.await(calendarOptions::isEnabled, "Calendar Options remained disabled");
		click(robot, calendarOptions);
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
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		activateWindow(robot, window);
		AbstractButton projectTab = findShowingButtonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
			.getString("ProjectRibbonTask.title"));
		clickAndAwaitSelected(robot, projectTab, "Robot click did not select the Project ribbon tab");
		GuiAcceptanceSupport.await(projectTab::isSelected, "Project ribbon tab did not become selected");
		AbstractButton changeWorkingTime = findShowingButtonByCommand("RibbonChangeWorkingTime");
		assertTrue(changeWorkingTime.isEnabled(), "Change Working Time requires the active project calendar to be editable");
		click(robot, changeWorkingTime);
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
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		activateWindow(robot, window);
		AbstractButton projectTab = findShowingButtonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
			.getString("ProjectRibbonTask.title"));
		clickAndAwaitSelected(robot, projectTab, "Project ribbon tab did not become selected");

		AbstractButton statusDate = RibbonGuiSupport.findVisibleOrExpand(robot, window, "RibbonStatusDate");
		String renderedStatusDate = statusDate.getText().replaceAll("(?i)<br\\s*/?>", " ").replaceAll("<[^>]*>", "");
		String localizedDate = com.microproject.options.EditOption.getInstance().getDateFormat()
			.format(new java.util.Date(originalStatusDate));
		assertTrue(renderedStatusDate.startsWith(localizedDate + " ") && renderedStatusDate.endsWith(":"),
			"Status Date ribbon value/caption must remain readable at this locale and scale: " + renderedStatusDate);
		click(robot, statusDate);
		GuiAcceptanceSupport.await(() -> java.util.Arrays.stream(Window.getWindows())
			.anyMatch(candidate -> candidate instanceof StatusDateDialog && candidate.isShowing()),
			"Status Date dialog did not open for visual inspection");
		StatusDateDialog statusDialog = java.util.Arrays.stream(Window.getWindows())
			.filter(candidate -> candidate instanceof StatusDateDialog && candidate.isShowing())
			.map(StatusDateDialog.class::cast).findFirst().orElseThrow();
		DialogLayoutAssertions.assertTextControlsAtPreferredHeight(statusDialog, "Issue #592 Status Date dialog");
		click(robot, findShowingButtonByText(statusDialog, Messages.getString("ButtonText.Cancel")));
		GuiAcceptanceSupport.await(() -> !statusDialog.isShowing(), "Status Date visual dialog did not cancel");
		assertEquals(originalStatusDate, project.getStatusDate(), "visual inspection must not change Status Date");

		AbstractButton updateProject = RibbonGuiSupport.findVisibleOrExpand(robot, window, "RibbonUpdateProject");
		click(robot, updateProject);
		GuiAcceptanceSupport.await(() -> java.util.Arrays.stream(Window.getWindows())
			.anyMatch(candidate -> candidate instanceof UpdateProjectDialogBox && candidate.isShowing()),
			"Update Project dialog did not open for visual inspection");
		UpdateProjectDialogBox updateDialog = java.util.Arrays.stream(Window.getWindows())
			.filter(candidate -> candidate instanceof UpdateProjectDialogBox && candidate.isShowing())
			.map(UpdateProjectDialogBox.class::cast).findFirst().orElseThrow();
		DialogLayoutAssertions.assertTextControlsAtPreferredHeight(updateDialog, "Issue #592 Update Project dialog");
		click(robot, findShowingButtonByText(updateDialog, Messages.getString("ButtonText.Cancel")));
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

		Robot robot = new com.microproject.testsupport.GuiRobot();
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
			clickAndAwaitSelected(robot, projectTab, "Robot click did not select the Project ribbon tab");
		}
		GuiAcceptanceSupport.await(projectTab::isSelected, "Project ribbon tab was not selected after bounded physical clicks");
		AbstractButton changeWorkingTime = findShowingButtonByCommand("RibbonChangeWorkingTime");
		assertTrue(changeWorkingTime.isEnabled(), "Change Working Time must be enabled for a selected writable resource");
		click(robot, changeWorkingTime);
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
		click(robot, add);
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
		previousFormatLocale = Locale.getDefault(Locale.Category.FORMAT);
		Locale.setDefault(Locale.Category.FORMAT, Locale.forLanguageTag("ja-JP-u-ca-japanese"));
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		NormalTask task = createTask();
		showProject(task.getOwningProject());
		SwingUtilities.invokeAndWait(() -> window.setSize(1600, 700));
		GuiAcceptanceSupport.await(() -> window.isShowing() && manager.getCurrentFrame() != null,
			"calendar test window did not become visible");

		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		activateWindow(robot, window);
		AbstractButton viewTab = findShowingButtonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
				.getString("ViewRibbonTask.title"));
		clickAndAwaitSelected(robot, viewTab, "Robot click did not select the View ribbon tab");
		GuiAcceptanceSupport.await(viewTab::isSelected, "Robot click did not select the View ribbon tab");
		AbstractButton calendarButton = findShowingButtonByCommand("RibbonCalendarView");
		click(robot, calendarButton);

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
		assertTrue(displayedMonth[0].contains("令和"),
			"the visible calendar month header must include the FORMAT-locale Japanese calendar date");
		AbstractButton[] navigation = UiComponentWalker.flatten(dialog).stream()
			.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
			.filter(AbstractButton::isShowing).toArray(AbstractButton[]::new);
		assertTrue(navigation.length >= 3, "calendar must show previous, today, and next controls");
		click(robot, navigation[0]);
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
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		activateWindow(robot, window);
		AbstractButton viewTab = findShowingButtonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
			.getString("ViewRibbonTask.title"));
		clickAndAwaitSelected(robot, viewTab, "Robot click did not select the View ribbon tab");
		GuiAcceptanceSupport.await(viewTab::isSelected, "View ribbon tab was not selected");

		AbstractButton network = findShowingButtonByCommand("RibbonNetwork");
		click(robot, network);
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame().getActiveTopView() instanceof PertView,
			"RibbonNetwork did not activate the real network view");
		assertTrue(isShowingView(manager.getCurrentFrame().getActiveTopView()),
			"network view was activated without becoming visible");

		AbstractButton wbs = findShowingButtonByCommand("RibbonWBS");
		click(robot, wbs);
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
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		activateWindow(robot, window);
		AbstractButton viewTab = findShowingButtonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
			.getString("ViewRibbonTask.title"));
		clickAndAwaitSelected(robot, viewTab, "Robot click did not select the View ribbon tab for Calendar");
		GuiAcceptanceSupport.await(() -> hasShowingCommand("RibbonCalendarView"),
			"View ribbon did not expose RibbonCalendarView after selecting its tab; " + componentInputDiagnostics(viewTab));
		click(robot, findShowingButtonByCommand("RibbonCalendarView"));
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
		click(robot, remove);
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
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		activateWindow(robot, window);
		AbstractButton resourceTab = findShowingButtonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
				.getString("ResourceRibbonTask.title"));
		clickAndAwaitSelected(robot, resourceTab, "Robot click did not select the Resource ribbon tab");
		AbstractButton timesheet = findShowingButtonByCommand("RibbonTimesheet");
		GuiAcceptanceSupport.await(timesheet::isEnabled, "Timesheet remained disabled on the Resource ribbon");
		click(robot, timesheet);
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
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		activateWindow(robot, window);
		SpreadSheet sheet = manager.getCurrentFrame().getActiveSpreadSheet();
		Gantt gantt = manager.getCurrentFrame().getGanttView().getGantt();
		assertTrue(isTaskVisibleInGantt(gantt, task), "task must start visible in Gantt");
		int row = rowForTask(sheet, task);
		clickUntilSelected(robot, window, sheet, row, nameColumn(sheet));
		AbstractButton taskTab = findShowingButtonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
				.getString("TaskRibbonTask.title"));
		clickAndAwaitSelected(robot, taskTab, "Robot click did not select the Task ribbon tab");
		AbstractButton hide = findVisibleOrOverflowButton(robot, "RibbonHideSelectedTasks");
		GuiAcceptanceSupport.await(hide::isEnabled, "Hide Selected Tasks remained disabled after selection");
		RibbonCommandResult previousHideOutcome = manager.getLastRibbonCommandResult();
		click(robot, hide);
		GuiAcceptanceSupport.await(task::isHiddenTask, "Hide Selected Tasks did not update the task model");
		assertRibbonOutcome(manager, previousHideOutcome, RibbonCommandResult.Status.CHANGED,
			List.of(task.getUniqueId()), List.of(task.getUniqueId()));
		assertEquals(List.of(task.getUniqueId()), manager.getLastRibbonCommandResult().affectedTaskIds(),
			"Hide Selected Tasks semantic result must identify the changed task");
		GuiAcceptanceSupport.await(() -> !isTaskVisible(sheet, task),
				"hidden task remained visible in the task sheet");
		GuiAcceptanceSupport.await(() -> !isTaskVisibleInGantt(gantt, task),
				"hidden task remained visible in Gantt");
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
		GuiAcceptanceSupport.await(() -> isTaskVisibleInGantt(gantt, task), "Ctrl+Z did not restore the visible Gantt row");

		click(robot, cellOnScreen(sheet, rowForTask(sheet, task), nameColumn(sheet)));
		hide = findVisibleOrOverflowButton(robot, "RibbonHideSelectedTasks");
		click(robot, hide);
		GuiAcceptanceSupport.await(task::isHiddenTask, "second hide did not update the task model");
		GuiAcceptanceSupport.await(() -> !isTaskVisibleInGantt(gantt, task), "second Hide did not hide the Gantt row");
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_Y);
		robot.keyRelease(KeyEvent.VK_Y);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		robot.waitForIdle();
		GuiAcceptanceSupport.await(task::isHiddenTask, "Ctrl+Y did not reapply task visibility");
		GuiAcceptanceSupport.await(() -> !isTaskVisibleInGantt(gantt, task), "Ctrl+Y did not rehide the Gantt row");

		AbstractButton show = findVisibleOrOverflowButton(robot, "RibbonShowAllTasks");
		assertTrue(show.isShowing(), "Show All Tasks must remain discoverable beside Hide Selected Tasks after hiding");
		assertTrue(show.isEnabled(), "Show All Tasks must become enabled after a task is hidden");
		RibbonCommandResult previousShowOutcome = manager.getLastRibbonCommandResult();
		click(robot, show);
		GuiAcceptanceSupport.await(() -> !task.isHiddenTask(), "Show All Tasks did not restore the task model");
		GuiAcceptanceSupport.await(() -> rowForTask(sheet, task) >= 0, "Show All Tasks did not restore the visible task row");
		GuiAcceptanceSupport.await(() -> isTaskVisibleInGantt(gantt, task), "Show All Tasks did not restore the visible Gantt row");
		assertRibbonOutcome(manager, previousShowOutcome, RibbonCommandResult.Status.CHANGED,
			List.of(), List.of(task.getUniqueId()));
		ByteArrayOutputStream shownSnapshot = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(task.getOwningProject(), shownSnapshot),
				"MPO save did not accept the project after Show All Tasks");
		Project shownReload = new MpoFileImporter().loadProject(new ByteArrayInputStream(shownSnapshot.toByteArray()));
		assertFalse(taskNamed(shownReload, task.getName()).isHiddenTask(),
				"MPO reload lost the task restored through Show All Tasks");

		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Z);
		GuiAcceptanceSupport.await(task::isHiddenTask, "Ctrl+Z did not undo Show All Tasks");
		GuiAcceptanceSupport.await(() -> !isTaskVisible(sheet, task), "Undo Show All Tasks did not hide the task row");
		GuiAcceptanceSupport.await(() -> !isTaskVisibleInGantt(gantt, task), "Undo Show All Tasks did not hide the Gantt row");
		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Y);
		GuiAcceptanceSupport.await(() -> !task.isHiddenTask(), "Ctrl+Y did not redo Show All Tasks");
		GuiAcceptanceSupport.await(() -> isTaskVisible(sheet, task), "Redo Show All Tasks did not restore the task row");
		GuiAcceptanceSupport.await(() -> isTaskVisibleInGantt(gantt, task), "Redo Show All Tasks did not restore the Gantt row");
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
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		activateWindow(robot, window);
		SpreadSheet sheet = manager.getCurrentFrame().getActiveSpreadSheet();
		click(robot, cellOnScreen(sheet, rowForTask(sheet, task), nameColumn(sheet)));
		click(robot, findShowingButtonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
				.getString("TaskRibbonTask.title")));
		AbstractButton manual = findVisibleOrOverflowButton(robot, "RibbonTaskModeManual");
		GuiAcceptanceSupport.await(manual::isEnabled, "Manual Schedule remained disabled after task selection");
		GuiCommandAcceptanceFixture.verifyMutation("TaskModeManual", () -> { click(robot, manual); return null; },
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
	void inactivateTaskRibbonRouteKeepsTaskVisibleAndSupportsUndoRedoAndMpo() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Inactivate Task Robot coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		NormalTask task = createTaskWithProgress(0.4d);
		Project project = task.getOwningProject();
		showProject(project);
		SwingUtilities.invokeAndWait(() -> window.setExtendedState(java.awt.Frame.MAXIMIZED_BOTH));
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame() != null
				&& manager.getCurrentFrame().getActiveSpreadSheet() != null,
			"inactivate-task project did not become visible");
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		activateWindow(robot, window);
		SpreadSheet sheet = manager.getCurrentFrame().getActiveSpreadSheet();
		assertProgressTaskFixtureRendered(sheet, task, 0.4d);
		click(robot, cellOnScreen(sheet, rowForTask(sheet, task), nameColumn(sheet)));
		clickAndAwaitSelected(robot, findShowingButtonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
				.getString("TaskRibbonTask.title")), "Robot click did not select the Task ribbon tab for Inactivate Task");
		AbstractButton inactivate = findVisibleOrOverflowButton(robot, "RibbonInactivateTask");
		GuiAcceptanceSupport.await(inactivate::isEnabled, "Inactivate remained disabled for the selected active task");
		RibbonCommandResult previousOutcome = manager.getLastRibbonCommandResult();
		click(robot, inactivate);
		GuiAcceptanceSupport.await(task::isInactiveTask, "Inactivate did not update the selected task's Active field");
		GuiAcceptanceSupport.await(() -> isTaskVisible(sheet, task), "Inactivate removed the task record from the task table");
		assertTrue(task.getPercentComplete() == 0.4d, "Inactivate must preserve progress");
		assertRibbonOutcome(manager, previousOutcome, RibbonCommandResult.Status.CHANGED, task.getUniqueId());
		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Z);
		GuiAcceptanceSupport.await(() -> !task.isInactiveTask(), "Undo did not reactivate the task");
		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Y);
		GuiAcceptanceSupport.await(task::isInactiveTask, "Redo did not inactivate the task");
		ByteArrayOutputStream saved = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(project, saved), "MPO save rejected the inactive task");
		Project reloaded = new MpoFileImporter().loadProject(new ByteArrayInputStream(saved.toByteArray()));
		NormalTask reloadedTask = taskNamed(reloaded, task.getName());
		assertTrue(reloadedTask.isInactiveTask(), "MPO reload lost the inactive state");
		assertTrue(reloadedTask.getPercentComplete() == 0.4d, "MPO reload lost inactive task progress");
	}

	@Test
	void respectLinksRibbonRouteMovesTheSelectedManualTaskAndRoundTripsUndoRedoAndMpo() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Respect Links Robot coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("respect-links-ribbon-acceptance", undo), undo);
		project.initialize(false, false);
		NormalTask predecessor = project.createScriptedTask();
		predecessor.setName("Respect Links predecessor");
		NormalTask successor = project.createScriptedTask();
		successor.setName("Respect Links successor");
		long day = CalendarOption.getInstance().getMillisPerDay();
		predecessor.setDuration(day);
		successor.setDuration(day);
		long base = project.getWorkCalendar().adjustInsideCalendar(project.getStart(), false);
		long predecessorStart = project.getWorkCalendar().add(base, 2L * day, false);
		long predecessorFinish = project.getWorkCalendar().add(predecessorStart, day, false);
		long oldSuccessorStart = base;
		long oldSuccessorFinish = project.getWorkCalendar().add(oldSuccessorStart, day, false);
		predecessor.setManualDates(predecessorStart, predecessorFinish);
		successor.setManualDates(oldSuccessorStart, oldSuccessorFinish);
		DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.FS, 0L, this);
		project.recalculate();
		assertTrue(successor.isManuallyScheduled(), "fixture successor must be manually scheduled");
		assertTrue(successor.getStart() < predecessor.getEnd(), "fixture must begin with an FS conflict");
		undo.clear();
		showProject(project);
		SwingUtilities.invokeAndWait(() -> window.setExtendedState(java.awt.Frame.MAXIMIZED_BOTH));
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame() != null
				&& manager.getCurrentFrame().getActiveSpreadSheet() != null,
			"Respect Links project did not become visible");
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		activateWindow(robot, window);
		SpreadSheet sheet = manager.getCurrentFrame().getActiveSpreadSheet();
		click(robot, cellOnScreen(sheet, rowForTask(sheet, successor), nameColumn(sheet)));
		clickAndAwaitSelected(robot, findShowingButtonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
				.getString("TaskRibbonTask.title")), "Robot click did not select the Task ribbon tab for Respect Links");
		AbstractButton respectLinks = findVisibleOrOverflowButton(robot, "RibbonRespectLinks");
		GuiAcceptanceSupport.await(respectLinks::isEnabled, "Respect Links remained disabled for the selected conflicting task");
		RibbonCommandResult previousOutcome = manager.getLastRibbonCommandResult();
		click(robot, respectLinks);
		GuiAcceptanceSupport.await(() -> successor.isManuallyScheduled() && successor.getStart() >= predecessor.getEnd(),
			"Respect Links did not preserve manual mode while moving the successor after its predecessor");
		GuiAcceptanceSupport.await(() -> {
			var cache = manager.getCurrentFrame().getGanttView().getGantt().getModel().getCache();
			for (int row = 0; row < cache.getSize(); row++) {
				Object element = cache.getElementAt(row);
				if (element instanceof GraphicNode graphic && graphic.getNode() != null
						&& graphic.getNode().getImpl() == successor)
					return graphic.getStart() == successor.getStart() && graphic.getEnd() == successor.getEnd();
			}
			return false;
		}, "Gantt projection did not redraw the moved successor dates");
		assertRibbonOutcome(manager, previousOutcome, RibbonCommandResult.Status.CHANGED, successor.getUniqueId());
		long respectedStart = successor.getStart();
		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Z);
		GuiAcceptanceSupport.await(() -> successor.isManuallyScheduled() && successor.getStart() == oldSuccessorStart
				&& successor.getEnd() == oldSuccessorFinish,
			"Undo did not restore the original manually scheduled dates");
		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Y);
		GuiAcceptanceSupport.await(() -> successor.isManuallyScheduled() && successor.getStart() == respectedStart
				&& successor.getStart() >= predecessor.getEnd(),
			"Redo did not restore the link-respecting dates and manual mode");
		ByteArrayOutputStream saved = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(project, saved), "MPO save rejected the respected schedule");
		Project reloaded = new MpoFileImporter().loadProject(new ByteArrayInputStream(saved.toByteArray()));
		NormalTask reloadedSuccessor = taskNamed(reloaded, "Respect Links successor");
		NormalTask reloadedPredecessor = taskNamed(reloaded, "Respect Links predecessor");
		assertTrue(reloadedSuccessor.isManuallyScheduled(), "MPO reload lost manual scheduling mode");
		assertTrue(reloadedSuccessor.getStart() >= reloadedPredecessor.getEnd(), "MPO reload lost the respected FS boundary");
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
		click(context.robot(), item);
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
			click(context.robot(), root);
			JMenuItem item = GuiPhysicalRouteAdapter.visiblePopupItem(root.getPopupMenu(), "TaskModeManual");
			GuiAcceptanceSupport.await(item::isEnabled, "Task Mode menu item remained disabled");
			click(context.robot(), item);
			return null;
		});
	}

	@Test
	void statusDateRibbonRouteUsesSharedMutationFixture() throws Exception {
		runProgressPhysicalRoute("status-date", context -> {
			AbstractButton statusDate = GuiPhysicalRouteAdapter.visibleButton(context.robot(), window, "RibbonStatusDate");
			String expectedDate = com.microproject.options.EditOption.getInstance().getDateFormat()
					.format(new java.util.Date(initialStatusDate));
			String statusDateText = statusDate.getText().replaceAll("(?i)<br\\s*/?>", " ").replaceAll("<[^>]*>", "");
			assertTrue(statusDateText.startsWith(expectedDate + " "),
					"MSP Status Date control must show the current date before its caption: " + statusDateText);
			assertTrue(statusDateText.endsWith(":"),
					"MSP Status Date control must retain its caption after the date value: " + statusDateText);
			click(context.robot(), statusDate);
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
			click(context.robot(), findShowingButtonByText(cancelled, Messages.getString("ButtonText.Cancel")));
			GuiAcceptanceSupport.await(() -> !cancelled.isShowing(), "Status Date cancel did not close dialog");
			assertEquals(initialStatusDate, contextProject().getStatusDate(), "Cancel must not change status date");
			click(context.robot(), GuiPhysicalRouteAdapter.visibleButton(context.robot(), window, "RibbonStatusDate"));
			chooseStatusDateNotSet(context.robot());
			GuiAcceptanceSupport.await(() -> RibbonGuiButton.adapt(RibbonGuiButton.findCommand(window, "RibbonStatusDate"))
					.getText().startsWith("NA "), "Status Date ribbon value did not refresh to NA");
			return null;
		}, () -> !contextProject().isStatusDateSet() && statusDateRibbonMatchesModel(),
			() -> contextProject().isStatusDateSet() && statusDateRibbonMatchesModel(),
			"StatusDate");
	}
	@Test
	void statusDateRibbonCanSetSpecificDatePhysically() throws Exception {
		runProgressPhysicalRoute("status-set-date", context -> {
			click(context.robot(), GuiPhysicalRouteAdapter.visibleButton(context.robot(), window, "RibbonStatusDate"));
			GuiAcceptanceSupport.await(() -> java.util.Arrays.stream(Window.getWindows())
					.anyMatch(candidate -> candidate instanceof StatusDateDialog && candidate.isShowing()),
					"Status Date dialog did not open for date selection");
			StatusDateDialog dialog = java.util.Arrays.stream(Window.getWindows())
					.filter(candidate -> candidate instanceof StatusDateDialog && candidate.isShowing())
					.map(StatusDateDialog.class::cast).findFirst().orElseThrow();
			AbstractButton picker = UiComponentWalker.flatten(dialog).stream()
					.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
					.filter(button -> "...".equals(button.getText())).findFirst().orElseThrow();
			click(context.robot(), picker);
			GuiAcceptanceSupport.await(() -> java.util.Arrays.stream(javax.swing.MenuSelectionManager.defaultManager().getSelectedPath())
					.anyMatch(JPopupMenu.class::isInstance), "Status Date calendar popup did not open");
			JPopupMenu popup = java.util.Arrays.stream(javax.swing.MenuSelectionManager.defaultManager().getSelectedPath())
					.filter(JPopupMenu.class::isInstance).map(JPopupMenu.class::cast).findFirst().orElseThrow();
			AbstractButton selectedDay = UiComponentWalker.flatten(popup).stream()
					.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
					.filter(button -> "3".equals(button.getText())).findFirst().orElseThrow();
			click(context.robot(), selectedDay);
			com.microproject.dialog.util.ExtDateField dateField = UiComponentWalker.flatten(dialog).stream()
					.filter(com.microproject.dialog.util.ExtDateField.class::isInstance)
					.map(com.microproject.dialog.util.ExtDateField.class::cast).findFirst().orElseThrow();
			assertNotNull(dateField.getDateValue(), "Selecting a calendar day must update the date field");
			assertTrue(initialStatusDate != dateField.getDateValue().getTime(),
					"The physical calendar selection must replace the preselected status date");
			click(context.robot(), findShowingButtonByText(dialog, Messages.getString("ButtonText.OK")));
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
		runProgressPhysicalRoute("status-popup", c -> { rightClick(c.robot(), c.cell()); SpreadSheetPopupMenu p=c.sheet().getPopup(); GuiAcceptanceSupport.await(p::isVisible,"Status popup absent"); click(c.robot(), GuiPhysicalRouteAdapter.visiblePopupItem(p,"popup.StatusDate")); chooseStatusDateNotSet(c.robot()); return null; }, () -> !contextProject().isStatusDateSet() && statusDateRibbonMatchesModel(), () -> contextProject().isStatusDateSet() && statusDateRibbonMatchesModel(), "StatusDate");
	}
	@Test
	void statusDateShortcutRouteUsesSharedMutationFixture() throws Exception {
		runProgressPhysicalRoute("status-shortcut", c -> { GuiPhysicalRouteAdapter.assertRootPaneBinding(window.getRootPane(),javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_S,InputEvent.CTRL_DOWN_MASK|InputEvent.ALT_DOWN_MASK),"StatusDate"); press(c.robot(),KeyEvent.VK_CONTROL,KeyEvent.VK_ALT,KeyEvent.VK_S); chooseStatusDateNotSet(c.robot()); return null; }, () -> !contextProject().isStatusDateSet() && statusDateRibbonMatchesModel(), () -> contextProject().isStatusDateSet() && statusDateRibbonMatchesModel(), "StatusDate");
	}

	@Test
	void markOnTrackRibbonRouteUsesSharedMutationFixture() throws Exception {
		runProgressPhysicalRoute("mark-on-track", context -> {
			click(context.robot(), GuiPhysicalRouteAdapter.visibleButton(context.robot(), window,
					"RibbonMarkOnTrack"));
			return null;
		}, () -> contextTask().getPercentComplete() != initialPercentComplete, () -> contextTask().getPercentComplete() == initialPercentComplete,
			"MarkOnTrack");
	}
	@Test
	void markOnTrackPopupRouteUsesSharedMutationFixture() throws Exception {
		runProgressPhysicalRoute("mark-popup", c -> { rightClick(c.robot(), c.cell()); SpreadSheetPopupMenu p=c.sheet().getPopup(); GuiAcceptanceSupport.await(p::isVisible,"Mark popup absent"); click(c.robot(), GuiPhysicalRouteAdapter.visiblePopupItem(p,"popup.MarkOnTrack")); return null; }, () -> contextTask().getPercentComplete()!=initialPercentComplete, () -> contextTask().getPercentComplete()==initialPercentComplete, "MarkOnTrack");
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
			click(c.robot(), root);
			JMenuItem visible = GuiPhysicalRouteAdapter.visiblePopupItem(root.getPopupMenu(), "MarkOnTrack");
			GuiAcceptanceSupport.await(visible::isEnabled, "Mark on Track menu item remained disabled for a selected task");
			click(c.robot(), visible);
			return null;
		}, () -> contextTask().getPercentComplete() != initialPercentComplete,
			() -> contextTask().getPercentComplete() == initialPercentComplete, "MarkOnTrack");
	}
	@Test
	void markOnTrackRibbonIsDisabledWithoutTaskSelection() throws Exception {
		runProgressPhysicalRoute("mark-on-track-no-selection", c -> {
			// Keep the tracking band in overflow regardless of label width differences
			// between supported locales and the desktop's native resolution.
			SwingUtilities.invokeAndWait(() -> { window.setSize(720, 700); window.validate(); });
			AbstractButton markOnTrack = GuiPhysicalRouteAdapter.visibleButton(c.robot(), window, "RibbonMarkOnTrack");
			JPopupMenu[] popupAncestor = new JPopupMenu[1];
			boolean[] popupVisible = new boolean[1];
			boolean[] commandEnabled = new boolean[1];
			int[] actualWindowWidth = new int[1];
			SwingUtilities.invokeAndWait(() -> {
				actualWindowWidth[0] = window.getWidth();
				for (Component current = markOnTrack; current != null; current = current.getParent()) {
					if (current instanceof JPopupMenu popup) {
						popupAncestor[0] = popup;
						popupVisible[0] = popup.isVisible();
						break;
					}
				}
				commandEnabled[0] = markOnTrack.isEnabled();
			});
			assertEquals(720, actualWindowWidth[0], "The responsive fixture must keep its requested width");
			assertTrue(popupAncestor[0] != null && popupVisible[0],
				"The narrow disabled-command fixture must exercise the visible ribbon overflow popup");
			assertFalse(commandEnabled[0], "MSP Mark on Track requires selected tasks");
			clickDisabled(c.robot(), markOnTrack);
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
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		activateWindow(robot, window);
		SpreadSheet sheet = manager.getCurrentFrame().getActiveSpreadSheet();
		clickUntilSelected(robot, window, sheet, rowForTask(sheet, task), nameColumn(sheet));
		click(robot, findShowingButtonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
				.getString("ProjectRibbonTask.title")));
		AbstractButton update = GuiPhysicalRouteAdapter.visibleButton(robot, window, "RibbonUpdateProject");
		GuiAcceptanceSupport.await(update::isEnabled, "Update Project remained disabled");
		click(robot, update);
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
		click(robot, ok);
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
		Robot robot = new com.microproject.testsupport.GuiRobot(); robot.setAutoDelay(45); activateWindow(robot, window);
		JMenuBar bar = new JMenuBar(); JMenu root = new JMenu("Project");
		JMenuItem item = new JMenuItem(manager.getMenuManager().getActionFromId("UpdateProject"));
		item.setActionCommand("UpdateProject"); root.add(item); bar.add(root); window.setJMenuBar(bar); window.validate();
		GuiAcceptanceSupport.await(root::isShowing, "Update Project menu root did not become visible");
		GuiCommandAcceptanceFixture.verifyDeferredDialog("UpdateProject", () -> {
			click(robot, root);
			click(robot, GuiPhysicalRouteAdapter.visiblePopupItem(root.getPopupMenu(), "UpdateProject"));
			return null;
		}, () -> manager.getLastRibbonCommandResult(),
			() -> java.util.Arrays.stream(Window.getWindows()).anyMatch(w -> w instanceof UpdateProjectDialogBox && w.isShowing()),
			() -> {
				UpdateProjectDialogBox dialog = java.util.Arrays.stream(Window.getWindows()).filter(w -> w instanceof UpdateProjectDialogBox && w.isShowing()).map(UpdateProjectDialogBox.class::cast).findFirst().orElseThrow();
				assertTrue(findShowingButtonByText(dialog, Messages.getString("UpdateProjectDialogBox.EntireProject")).isSelected(),
					"no selected task rows must default Update Project to Entire Project");
				assertEquals(statusDateBeforeUpdate, dialog.getForm().getUpdateDate().getTime(),
					"Update Project date must default to the effective project Status Date");
				click(robot, findShowingButtonByText(dialog,
					Messages.getString("UpdateProjectDialogBox.SetZeroOrHundredOnly")));
				click(robot, findShowingButtonByText(dialog, Messages.getString("ButtonText.OK"))); return null;
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
		AbstractButton button = RibbonGuiButton.adapt(RibbonGuiButton.findCommand(window, "RibbonStatusDate"));
		String text = button.getText().replaceAll("(?i)<br\\s*/?>", " ").replaceAll("<[^>]*>", "");
		String value = !contextProject().isStatusDateSet() ? "NA"
				: com.microproject.options.EditOption.getInstance().getDateFormat()
					.format(new java.util.Date(contextProject().getStatusDate()));
		String label = manager.getMenuManager().getStringOrNull("RibbonStatusDate.text");
		String expectedDisplay = value + " " + (label == null ? "Status Date" : label) + ":";
		Object actionName = manager.getMenuManager().getActionFromId("StatusDate").getValue(javax.swing.Action.NAME);
		return text.startsWith(value + " ") && text.endsWith(":")
				&& expectedDisplay.equals(button.getAccessibleContext().getAccessibleName())
				&& expectedDisplay.equals(actionName);
	}

	@Test
	void baselineRibbonRouteRegistersCoreChangeInSwingUndoHistory() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Baseline Robot coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		NormalTask task = createTask();
		Project project = task.getOwningProject();
		NormalTask summary = project.createScriptedTask();
		summary.setName("Baseline roll-up summary");
		var taskModel = project.getTaskModel();
		var summaryNode = taskModel.search(summary);
		assertTrue(taskModel.relocate(List.of(taskModel.search(task)), summaryNode, 0,
			com.microproject.grouping.core.model.NodeModel.SILENT));
		summary.setWbsChildrenNodes(taskModel.getHierarchy().getChildren(summaryNode));
		task.setWbsParent(summary);
		Resource resource = project.getResourcePool().createScriptedResource();
		Assignment assignment = AssignmentService.getInstance().newAssignment(task, resource, 1.0d, 0L, this);
		task.setFixedCost(10.0d);
		summary.setFixedCost(5.0d);
		long day = CalendarOption.getInstance().getMillisPerDay();
		assignment.setWork(day, null);
		project.saveCurrentToSnapshot(Integer.valueOf(Snapshottable.BASELINE), false, List.of(summary));
		assertNull(task.getSnapshot(Snapshottable.BASELINE),
			"The physical overwrite route starts with a new leaf and an already-baselined ancestor");
		showProject(project);
		SwingUtilities.invokeAndWait(() -> window.setSize(1600, 700));
		GuiAcceptanceSupport.await(() -> window.isShowing() && manager.getCurrentFrame() != null
				&& manager.getCurrentFrame().getActiveSpreadSheet() != null,
			"Baseline project did not become visible");
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		activateWindow(robot, window);
		SpreadSheet sheet = manager.getCurrentFrame().getActiveSpreadSheet();
		click(robot, cellOnScreen(sheet, rowForTask(sheet, task), nameColumn(sheet)));
		click(robot, findShowingButtonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
			.getString("ProjectRibbonTask.title")));
		AbstractButton saveBaseline = findShowingButtonByCommand("RibbonSaveBaseline");
		GuiAcceptanceSupport.await(saveBaseline::isEnabled, "Save Baseline remained disabled");
		click(robot, saveBaseline);
		GuiAcceptanceSupport.await(() -> java.util.Arrays.stream(Window.getWindows())
				.anyMatch(candidate -> candidate instanceof BaselineDialog && candidate.isShowing()),
			"Save Baseline dialog did not open");
		BaselineDialog dialog = java.util.Arrays.stream(Window.getWindows())
			.filter(candidate -> candidate instanceof BaselineDialog && candidate.isShowing())
			.map(BaselineDialog.class::cast).findFirst().orElseThrow();
		click(robot, findShowingButtonByText(dialog, Messages.getString("Text.SelectedTasks")));
		ResourceBundle baselineLabels = ResourceBundle.getBundle("com.microproject.dialog.usability");
		click(robot, findShowingButtonByText(dialog,
			baselineLabels.getString("baseline.rollupToSummaryTasks")));
		click(robot, findShowingButtonByText(dialog, Messages.getString("ButtonText.OK")));
		GuiAcceptanceSupport.await(() -> isActiveDialogOwnedBy(dialog, baselineLabels.getString("baseline.overwriteTitle")),
			"A new selected leaf must still confirm overwrite of its already-baselined ancestor");
		Dialog ancestorOverwrite = activeDialogOwnedBy(dialog);
		click(robot, findShowingButtonByText(ancestorOverwrite, baselineLabels.getString("dialog.yes")));
		GuiAcceptanceSupport.await(() -> !dialog.isShowing() && task.getSnapshot(Snapshottable.BASELINE) != null,
			"Confirmed selected-task save did not store the baseline snapshot");
		assertTrue(sheet.getSelectedNodes().contains(taskModel.search(task)),
			"A completed Save Baseline command must preserve the user's selected task row");
		assertTrue(rowForTask(sheet, task) >= 0, "Baseline mutation must keep the task visible");
		long originalBaselineStart = task.getBaselineStart(Snapshottable.BASELINE);
		int originalSummaryBaselineProfileCount = ((com.microproject.pm.task.TaskSnapshot)
			summary.getSnapshot(Snapshottable.BASELINE))
			.captureBaselineProfiles(summary, Snapshottable.BASELINE).size();
		Assignment.BaselineValues originalBaselineValues = assignment.getBaselineValues(Snapshottable.BASELINE);
		assertEquals(Long.valueOf(day), originalBaselineValues.work(),
			"The first save must capture the seeded one-day assignment baseline");
		assertEquals(10.0d, task.getBaselineCost(Snapshottable.BASELINE), 0.001d,
			"The leaf Baseline Cost includes its own saved Fixed Cost");
		assertEquals(15.0d, summary.getBaselineCost(Snapshottable.BASELINE), 0.001d,
			"The summary Baseline Cost includes the leaf total and its own Fixed Cost exactly once");
		long replacementStart = project.getEffectiveWorkCalendar().add(task.getStart(),
			day, false);
		SwingUtilities.invokeAndWait(() -> {
			task.setStart(replacementStart);
			task.setFixedCost(20.0d);
			assignment.setWork(3L * day, null);
			project.recalculate();
		});
		assertEquals(replacementStart, task.getStart(), "Fixture must move the current task before overwrite");
		assertNotEquals(originalBaselineStart, task.getStart(),
			"Replacement current start must differ from the seeded baseline start");
		// The schedule mutation below is fixture setup, not the Save Baseline route.
		// Select the task again so the second dialog exercises the overwrite path
		// with an explicit physical selection after the model refresh.
		click(robot, cellOnScreen(sheet, rowForTask(sheet, task), nameColumn(sheet)));
		long overwrittenBaselineStart = task.getStart();
		long currentAssignmentWork = assignment.getWork(null);
		click(robot, findShowingButtonByCommand("RibbonSaveBaseline"));
		GuiAcceptanceSupport.await(() -> java.util.Arrays.stream(Window.getWindows())
				.anyMatch(candidate -> candidate instanceof BaselineDialog && candidate.isShowing()),
			"Overwrite Save Baseline dialog did not open");
		BaselineDialog overwriteDialog = java.util.Arrays.stream(Window.getWindows())
			.filter(candidate -> candidate instanceof BaselineDialog && candidate.isShowing())
			.map(BaselineDialog.class::cast).findFirst().orElseThrow();
		click(robot, findShowingButtonByText(overwriteDialog, Messages.getString("Text.SelectedTasks")));
		click(robot, findShowingButtonByText(overwriteDialog,
			baselineLabels.getString("baseline.rollupToSummaryTasks")));
		AbstractButton summaryRollupCheckbox = findShowingButtonByText(overwriteDialog,
			baselineLabels.getString("baseline.rollupToSummaryTasks"));
		boolean[] summaryRollupSelected = new boolean[1];
		SwingUtilities.invokeAndWait(() -> summaryRollupSelected[0] = summaryRollupCheckbox.isSelected());
		assertTrue(summaryRollupSelected[0], "The physical summary-roll-up checkbox must remain selected");
		AtomicInteger baselineEventCount = new AtomicInteger();
		com.microproject.pm.scheduling.ScheduleEventListener baselineListener = event -> {
			if (com.microproject.pm.scheduling.ScheduleEvent.BASELINE.equals(event.getType())) baselineEventCount.incrementAndGet();
		};
		AtomicInteger undoEditCount = new AtomicInteger();
		com.microproject.undo.EditListener undoListener = edit -> undoEditCount.incrementAndGet();
		project.addScheduleListener(baselineListener);
		project.getUndoController().getEditSupport().addEditListener(undoListener);
		int eventCountBeforeConfirmation = baselineEventCount.get();
		boolean undoBeforeConfirmation = project.getUndoController().canUndo();
		boolean dirtyBeforeConfirmation = project.needsSaving();
		click(robot, findShowingButtonByText(overwriteDialog, Messages.getString("ButtonText.OK")));
		GuiAcceptanceSupport.await(() -> isActiveDialogOwnedBy(overwriteDialog,
				baselineLabels.getString("baseline.overwriteTitle")),
			"Overwriting an occupied baseline must ask before mutation");
		assertTrue(overwriteDialog.getForm().isRollupToSummaryTasks(),
			"Accepting the dialog binds the physical summary-roll-up choice before overwrite preflight");
		Dialog noConfirmation = activeDialogOwnedBy(overwriteDialog);
		String[] promptUiState = new String[2];
		boolean[] promptOwnedByChooser = new boolean[1];
		SwingUtilities.invokeAndWait(() -> {
			promptUiState[0] = componentText(noConfirmation);
			promptUiState[1] = ((javax.swing.JDialog) noConfirmation).getRootPane().getDefaultButton().getText();
			promptOwnedByChooser[0] = noConfirmation.getOwner() == overwriteDialog
				&& baselineLabels.getString("baseline.overwriteTitle").equals(noConfirmation.getTitle());
		});
		assertTrue(promptOwnedByChooser[0], "The localized overwrite prompt must be owned by its chooser");
		assertTrue(promptUiState[0].contains(baselineLabels.getString("baseline.overwritePrompt")),
			"The confirmation must explain which operation is being confirmed");
		assertEquals(baselineLabels.getString("dialog.no"), promptUiState[1],
			"No must be the confirmation's default button");
		click(robot, findShowingButtonByText(noConfirmation,
			baselineLabels.getString("dialog.no")));
		GuiAcceptanceSupport.await(() -> !isShowingOnEdt(noConfirmation) && isShowingOnEdt(overwriteDialog),
			"No must leave the baseline chooser open");
		assertEquals(Snapshottable.BASELINE, overwriteDialog.getForm().getBaselineNumber(),
			"No must retain the selected baseline slot");
		assertFalse(overwriteDialog.getForm().isEntireProject(), "No must retain the selected task scope");
		assertTrue(overwriteDialog.getForm().isRollupToSummaryTasks(),
			"No must retain the selected summary roll-up option");
		assertEquals(originalBaselineStart, task.getBaselineStart(Snapshottable.BASELINE));
		assertEquals(originalBaselineValues, assignment.getBaselineValues(Snapshottable.BASELINE));
		assertEquals(currentAssignmentWork, assignment.getWork(null));
		assertEquals(eventCountBeforeConfirmation, baselineEventCount.get(), "No must not publish baseline events");
		assertEquals(0, undoEditCount.get(), "No must not publish an undo edit");
		assertEquals(undoBeforeConfirmation, project.getUndoController().canUndo(), "No must not add an Undo edit");
		assertEquals(dirtyBeforeConfirmation, project.needsSaving(), "No must not dirty the project");

		click(robot, findShowingButtonByText(overwriteDialog, Messages.getString("ButtonText.OK")));
		GuiAcceptanceSupport.await(() -> isActiveDialogOwnedBy(overwriteDialog,
				baselineLabels.getString("baseline.overwriteTitle")),
			"The overwrite confirmation did not reopen after No");
		Dialog escapeConfirmation = activeDialogOwnedBy(overwriteDialog);
		press(robot, KeyEvent.VK_ESCAPE);
		GuiAcceptanceSupport.await(() -> !isShowingOnEdt(escapeConfirmation) && isShowingOnEdt(overwriteDialog),
			"Escape must leave the baseline chooser open");
		assertEquals(Snapshottable.BASELINE, overwriteDialog.getForm().getBaselineNumber(),
			"Escape must retain the selected baseline slot");
		assertFalse(overwriteDialog.getForm().isEntireProject(), "Escape must retain the selected task scope");
		assertTrue(overwriteDialog.getForm().isRollupToSummaryTasks(),
			"Escape must retain the selected summary roll-up option");
		assertEquals(originalBaselineStart, task.getBaselineStart(Snapshottable.BASELINE));
		assertEquals(originalBaselineValues, assignment.getBaselineValues(Snapshottable.BASELINE));
		assertEquals(currentAssignmentWork, assignment.getWork(null));
		assertEquals(eventCountBeforeConfirmation, baselineEventCount.get(), "Escape must not publish baseline events");
		assertEquals(0, undoEditCount.get(), "Escape must not publish an undo edit");
		assertEquals(undoBeforeConfirmation, project.getUndoController().canUndo(), "Escape must not add an Undo edit");
		assertEquals(dirtyBeforeConfirmation, project.needsSaving(), "Escape must not dirty the project");

		click(robot, findShowingButtonByText(overwriteDialog, Messages.getString("ButtonText.OK")));
		GuiAcceptanceSupport.await(() -> isActiveDialogOwnedBy(overwriteDialog,
				baselineLabels.getString("baseline.overwriteTitle")),
			"The overwrite confirmation did not reopen after Escape");
		Dialog yesConfirmation = activeDialogOwnedBy(overwriteDialog);
		click(robot, findShowingButtonByText(yesConfirmation,
			baselineLabels.getString("dialog.yes")));
		GuiAcceptanceSupport.await(() -> !overwriteDialog.isShowing()
				&& task.getBaselineStart(Snapshottable.BASELINE) == overwrittenBaselineStart
				&& ((com.microproject.pm.task.TaskSnapshot) summary.getSnapshot(Snapshottable.BASELINE))
					.captureBaselineProfiles(summary, Snapshottable.BASELINE).size() == 1
				&& baselineEventCount.get() == eventCountBeforeConfirmation + 1 && undoEditCount.get() == 1,
			"Save Baseline did not overwrite the task and roll its baseline up to the summary");
		project.removeScheduleListener(baselineListener);
		project.getUndoController().getEditSupport().removeEditListener(undoListener);
		assertEquals(eventCountBeforeConfirmation + 1, baselineEventCount.get(), "Yes must publish one baseline change");
		Assignment.BaselineValues overwrittenBaselineValues = assignment.getBaselineValues(Snapshottable.BASELINE);
		assertEquals(Long.valueOf(3L * day), overwrittenBaselineValues.work(),
			"Overwrite must capture the explicitly seeded three-day assignment work");
		assertEquals(20.0d, task.getBaselineCost(Snapshottable.BASELINE), 0.001d,
			"The overwritten leaf Baseline Cost includes its replacement Fixed Cost");
		assertEquals(25.0d, summary.getBaselineCost(Snapshottable.BASELINE), 0.001d,
			"The rolled-up summary includes descendant Baseline Cost and its own Fixed Cost once");
		assertEquals(1, ((com.microproject.pm.task.TaskSnapshot) summary.getSnapshot(Snapshottable.BASELINE))
			.captureBaselineProfiles(summary, Snapshottable.BASELINE).size(),
			"The summary receives one detached child baseline profile");
		assertNotEquals(originalBaselineValues, overwrittenBaselineValues,
			"Overwrite must produce assignment baseline values distinct from the prior slot");

		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Z);
		GuiAcceptanceSupport.await(() -> task.getBaselineStart(Snapshottable.BASELINE) == originalBaselineStart
				&& ((com.microproject.pm.task.TaskSnapshot) summary.getSnapshot(Snapshottable.BASELINE))
					.captureBaselineProfiles(summary, Snapshottable.BASELINE).size() == originalSummaryBaselineProfileCount
				&& originalBaselineValues.equals(assignment.getBaselineValues(Snapshottable.BASELINE)),
			"Undo did not restore the overwritten task and summary baseline snapshots");
		assertEquals(originalBaselineValues, assignment.getBaselineValues(Snapshottable.BASELINE));
		assertEquals(10.0d, task.getBaselineCost(Snapshottable.BASELINE), 0.001d,
			"Undo restores the leaf total Baseline Cost");
		assertEquals(15.0d, summary.getBaselineCost(Snapshottable.BASELINE), 0.001d,
			"Undo restores the summary total Baseline Cost without rolling Fixed Cost into its own field");
		assertEquals(overwrittenBaselineStart, task.getStart(), "Undo must preserve the current task schedule");
		assertEquals(currentAssignmentWork, assignment.getWork(null), "Undo must preserve current assignment work");
		ByteArrayOutputStream restoredSaved = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(project, restoredSaved), "MPO save must accept the restored baseline");
		Project restoredReload = new MpoFileImporter().loadProject(new ByteArrayInputStream(restoredSaved.toByteArray()));
		NormalTask restoredTask = (NormalTask) taskNamed(restoredReload, task.getName());
		assertEquals(originalBaselineStart, restoredTask.getBaselineStart(Snapshottable.BASELINE));
		assertEquals(10.0d, restoredTask.getBaselineCost(Snapshottable.BASELINE), 0.001d,
			"MPO reopen preserves the restored leaf total Baseline Cost");
		NormalTask restoredSummary = (NormalTask) taskNamed(restoredReload, summary.getName());
		assertEquals(originalSummaryBaselineProfileCount,
			((com.microproject.pm.task.TaskSnapshot) restoredSummary.getSnapshot(Snapshottable.BASELINE))
				.captureBaselineProfiles(restoredSummary, Snapshottable.BASELINE).size(),
			"Undo snapshot MPO reload must restore the summary profile list");
		Assignment restoredAssignment = (Assignment) restoredTask.getAssignments().iterator().next();
		assertEquals(originalBaselineValues, restoredAssignment.getBaselineValues(Snapshottable.BASELINE));
		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Y);
		GuiAcceptanceSupport.await(() -> task.getBaselineStart(Snapshottable.BASELINE) == overwrittenBaselineStart
				&& ((com.microproject.pm.task.TaskSnapshot) summary.getSnapshot(Snapshottable.BASELINE))
					.captureBaselineProfiles(summary, Snapshottable.BASELINE).size() == 1
				&& overwrittenBaselineValues.equals(assignment.getBaselineValues(Snapshottable.BASELINE)),
			"Redo did not restore the replacement task and summary baseline snapshots");
		assertEquals(overwrittenBaselineValues, assignment.getBaselineValues(Snapshottable.BASELINE));
		ByteArrayOutputStream saved = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(project, saved), "MPO save must accept the overwritten baseline");
		Project reloaded = new MpoFileImporter().loadProject(new ByteArrayInputStream(saved.toByteArray()));
		NormalTask reloadedTask = (NormalTask) taskNamed(reloaded, task.getName());
		assertEquals(overwrittenBaselineStart, reloadedTask.getBaselineStart(Snapshottable.BASELINE),
			"MPO reopen must retain the replacement baseline");
		assertEquals(20.0d, reloadedTask.getBaselineCost(Snapshottable.BASELINE), 0.001d,
			"MPO reopen preserves the overwritten leaf total Baseline Cost");
		NormalTask reloadedSummary = (NormalTask) taskNamed(reloaded, summary.getName());
		assertEquals(25.0d, reloadedSummary.getBaselineCost(Snapshottable.BASELINE), 0.001d,
			"MPO reopen preserves the rolled-up summary total Baseline Cost");
		assertEquals(1, ((com.microproject.pm.task.TaskSnapshot) reloadedSummary.getSnapshot(Snapshottable.BASELINE))
			.captureBaselineProfiles(reloadedSummary, Snapshottable.BASELINE).size(),
			"MPO reopen must retain the summary's detached rolled-up profile");
		Assignment reloadedAssignment = (Assignment) reloadedTask.getAssignments().iterator().next();
		assertEquals(overwrittenBaselineValues, reloadedAssignment.getBaselineValues(Snapshottable.BASELINE));
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
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		activateWindow(robot, window);
		SpreadSheet sheet = manager.getCurrentFrame().getActiveSpreadSheet();
		if (selectTask) click(robot, cellOnScreen(sheet, rowForTask(sheet, progressTask), nameColumn(sheet)));
		else SwingUtilities.invokeAndWait(sheet::clearSelection);
		AbstractButton progressTab = findShowingButtonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
				.getString(commandId.equals("MarkOnTrack") ? "TaskRibbonTask.title" : "ProjectRibbonTask.title"));
		clickAndAwaitSelected(robot, progressTab, routeName + " ribbon tab did not become selected");
		SwingUtilities.invokeAndWait(() -> { });
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
		click(robot, notSet);
		click(robot, findShowingButtonByText(dialog, Messages.getString("ButtonText.OK")));
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
		Robot robot = new com.microproject.testsupport.GuiRobot();
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
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		activateWindow(robot, window);
		click(robot, cellOnScreen(sheet, rowForTask(sheet, task), nameColumn(sheet)));
		click(robot, findShowingButtonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
				.getString("TaskRibbonTask.title")));
		AbstractButton delete = findVisibleOrOverflowButton(robot, "RibbonDelete");
		GuiAcceptanceSupport.await(delete::isEnabled, "Delete remained disabled after selecting a task");
		click(robot, delete);
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
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		activateWindow(robot, window);
		click(robot, cellOnScreen(sheet, rowForTask(sheet, source), nameColumn(sheet)));
		click(robot, findShowingButtonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
				.getString("TaskRibbonTask.title")));
		// Selecting a ribbon tab can rebuild the active spreadsheet; reassert the
		// physical task selection before checking the clipboard command state.
		click(robot, cellOnScreen(sheet, rowForTask(sheet, source), nameColumn(sheet)));
		GuiAcceptanceSupport.await(() -> sheet.getSelectedRow() == rowForTask(sheet, source),
			"source task selection was lost while switching to the Task ribbon tab");
		AbstractButton copy = findShowingButtonByCommand("RibbonCopy");
		GuiAcceptanceSupport.await(copy::isEnabled, "Copy remained disabled after selecting source task");
		click(robot, copy);
		click(robot, cellOnScreen(sheet, rowForTask(sheet, target), nameColumn(sheet)));
		AbstractButton paste = findShowingButtonByCommand("RibbonPaste");
		GuiAcceptanceSupport.await(paste::isEnabled, "Paste remained disabled after Copy");
		click(robot, paste);
		assertEquals("Paste", manager.getLastRibbonCommandResult().commandId(),
			"physical Paste must use the canonical ribbon command dispatcher");
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
		click(robot, pasteInsert);
		GuiAcceptanceSupport.await(() -> manager.getLastRibbonCommandResult() != null
			&& "PasteInsert".equals(manager.getLastRibbonCommandResult().commandId()),
			"physical Paste Insert did not reach the canonical CommandId route");

		click(robot, cellOnScreen(sheet, rowForTask(sheet, source), nameColumn(sheet)));
		AbstractButton cut = findShowingButtonByCommand("RibbonCut");
		GuiAcceptanceSupport.await(cut::isEnabled, "Cut remained disabled after selecting source task");
		click(robot, cut);
		// A desktop clipboard provider may defer or reject exportDone; assert the
		// physical Action remains wired and leave model commit coverage to a real
		// clipboard-enabled environment.
		assertEquals("Cut", manager.getLastRibbonCommandResult().commandId(),
			"physical Cut must use the canonical ribbon command dispatcher");
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

		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		activateWindow(robot, window);
		AbstractButton taskTab = findShowingButtonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
				.getString("TaskRibbonTask.title"));
		clickAndAwaitSelected(robot, taskTab, "Robot click did not select the Task ribbon tab");
		GuiAcceptanceSupport.await(taskTab::isSelected, "Robot click did not select the Task ribbon tab");
		assertFalse(findVisibleOrOverflowButton(robot, "RibbonLink").isEnabled(),
			"Link must be disabled without two selected tasks");
		assertFalse(findVisibleOrOverflowButton(robot, "RibbonUnlink").isEnabled(),
			"Unlink must be disabled without a selected task");
		assertFalse(findVisibleOrOverflowButton(robot, "RibbonIndent").isEnabled(),
			"Indent must be disabled without a selected task");
		assertFalse(findVisibleOrOverflowButton(robot, "RibbonOutdent").isEnabled(),
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
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		SpreadSheet sheet = manager.getCurrentFrame().getActiveSpreadSheet();
		Gantt gantt = manager.getCurrentFrame().getGanttView().getGantt();
		undo.clear();
		assertTrue(isTaskVisible(sheet, child), "fixture must start with the child visible in the task table");
		assertTrue(isTaskVisibleInGantt(gantt, parent), "Gantt must show the selected summary before collapse");
		assertTrue(isTaskVisibleInGantt(gantt, child), "Gantt must show the child before collapse");
		assertFalse(undo.canUndo(), "view-state fixture must start with an empty project undo history");
		JTable rowHeader = sheet.getRowHeader();
		RowHeaderTarget parentRowTarget = rowHeaderTargetOnScreen(rowHeader, rowForTask(sheet, parent), false);
		click(robot, new Rectangle(parentRowTarget.screenPoint().x, parentRowTarget.screenPoint().y, 1, 1));
		assertTrue(isTaskVisible(sheet, child), "selecting the summary row must not collapse its children");
		AbstractButton taskTab = findShowingButtonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
				.getString("TaskRibbonTask.title"));
		clickAndAwaitSelected(robot, taskTab, "Robot click did not select the Task ribbon tab");
		AbstractButton collapse = findShowingButtonByCommand("RibbonCollapse");
		GuiAcceptanceSupport.await(collapse::isEnabled, "Collapse remained disabled for a selected summary");
		RibbonCommandResult previousCollapseOutcome = manager.getLastRibbonCommandResult();
		click(robot, collapse);
		GuiAcceptanceSupport.await(() -> !isTaskVisible(sheet, child), "Collapse did not hide the child row");
		GuiAcceptanceSupport.await(() -> !isTaskVisibleInGantt(gantt, child), "Collapse did not hide the child Gantt row");
		assertTrue(isTaskVisibleInGantt(gantt, parent), "Collapse must keep the summary visible in Gantt");
		assertSame(parent, child.getWbsParentTask(), "Collapse must not mutate the project hierarchy");
		assertFalse(undo.canUndo(), "view-only Collapse must not add a project undo edit");
		assertRibbonOutcome(manager, previousCollapseOutcome, RibbonCommandResult.Status.CHANGED, parent.getUniqueId());
		ByteArrayOutputStream collapsedSnapshot = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(project, collapsedSnapshot),
				"MPO save failed after the view-only Collapse operation");
		Project collapsedReload = new MpoFileImporter().loadProject(new ByteArrayInputStream(collapsedSnapshot.toByteArray()));
		NormalTask reloadedParent = taskNamed(collapsedReload, "Outline parent");
		NormalTask reloadedChild = taskNamed(collapsedReload, "Outline child");
		assertNotNull(reloadedParent, "Collapse persistence check lost the summary task");
		assertNotNull(reloadedChild, "Collapse persistence check lost the child task");
		assertSame(reloadedParent, reloadedChild.getWbsParentTask(),
				"view-only Collapse must preserve the hierarchy after MPO save/reload");
		assertFalse(reloadedChild.isHiddenTask(), "view-only Collapse must not persist as a hidden-task mutation");

		AbstractButton expand = findShowingButtonByCommand("RibbonExpand");
		RibbonCommandResult previousExpandOutcome = manager.getLastRibbonCommandResult();
		click(robot, expand);
		GuiAcceptanceSupport.await(() -> isTaskVisible(sheet, child), "Expand did not restore the child row");
		GuiAcceptanceSupport.await(() -> isTaskVisibleInGantt(gantt, child), "Expand did not restore the child Gantt row");
		assertSame(parent, child.getWbsParentTask(), "Expand must not mutate the project hierarchy");
		assertFalse(undo.canUndo(), "view-only Expand must not add a project undo edit");
		assertRibbonOutcome(manager, previousExpandOutcome, RibbonCommandResult.Status.CHANGED, parent.getUniqueId());
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
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		SpreadSheet sheet = manager.getCurrentFrame().getActiveSpreadSheet();
		Gantt gantt = manager.getCurrentFrame().getGanttView().getGantt();
		activateWindow(robot, window);
		clickUntilSelected(robot, window, sheet, rowForTask(sheet, second), nameColumn(sheet));
		AbstractButton taskTab = findShowingButtonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
				.getString("TaskRibbonTask.title"));
		clickAndAwaitSelected(robot, taskTab, "Robot click did not select the Task ribbon tab");
		AbstractButton indent = findVisibleOrOverflowButton(robot, "RibbonIndent");
		GuiAcceptanceSupport.await(indent::isEnabled, "Indent remained disabled for the selected task");
		RibbonCommandResult previousIndentOutcome = manager.getLastRibbonCommandResult();
		click(robot, indent);
		GuiAcceptanceSupport.await(() -> second.getWbsParentTask() == first,
				"Indent did not make the selected task a child of its predecessor");
		GuiAcceptanceSupport.await(() -> rowForTask(sheet, second) >= 0 && isTaskVisibleInGantt(gantt, second),
				"Indent changed hierarchy but lost the task from the table or Gantt projection");
		assertRibbonOutcome(manager, previousIndentOutcome, RibbonCommandResult.Status.CHANGED, second.getUniqueId());
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame().getSelectedImpls(false).contains(second),
				"Indent did not preserve the selected task after hierarchy refresh");
		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Z);
		GuiAcceptanceSupport.await(() -> second.getWbsParentTask() == null,
				"Ctrl+Z did not restore the selected task to the top level after Indent");
		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Y);
		GuiAcceptanceSupport.await(() -> second.getWbsParentTask() == first,
				"Ctrl+Y did not reapply the selected task hierarchy after Indent");

		AbstractButton outdent = findVisibleOrOverflowButton(robot, "RibbonOutdent");
		GuiAcceptanceSupport.await(outdent::isEnabled, "Outdent became disabled after Indent: readOnly="
				+ project.isReadOnly() + " parent=" + (second.getWbsParentTask() == null ? "null" : second.getWbsParentTask().getName()));
		RibbonCommandResult previousOutdentOutcome = manager.getLastRibbonCommandResult();
		click(robot, outdent);
		GuiAcceptanceSupport.await(() -> second.getWbsParentTask() == null,
				"Outdent did not restore the selected task to the top level");
		GuiAcceptanceSupport.await(() -> rowForTask(sheet, second) >= 0 && isTaskVisibleInGantt(gantt, second),
				"Outdent changed hierarchy but lost the task from the table or Gantt projection");
		assertRibbonOutcome(manager, previousOutdentOutcome, RibbonCommandResult.Status.CHANGED, second.getUniqueId());
		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Z);
		GuiAcceptanceSupport.await(() -> second.getWbsParentTask() == first,
				"Ctrl+Z did not undo Outdent");
		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Y);
		GuiAcceptanceSupport.await(() -> second.getWbsParentTask() == null,
				"Ctrl+Y did not redo Outdent");

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

		// A whole-project read-only state disables the Ribbon Action, while the
		// focused-editor shortcut adapter must still reach the same canonical
		// transaction and publish its explicit rejection without a mutation.
		undo.clear();
		project.setReadOnly(true);
		click(robot, cellOnScreen(sheet, rowForTask(sheet, second), nameColumn(sheet)));
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame().getSelectedImpls(false).contains(second),
				"read-only outline test did not select the target task");
		manager.setButtonState(second, project);
		AbstractButton readOnlyIndent = findVisibleOrOverflowButton(robot, "RibbonIndent");
		assertFalse(readOnlyIndent.isEnabled(), "Ribbon Indent must be disabled for a read-only project");
		press(robot, KeyEvent.VK_ALT, KeyEvent.VK_SHIFT, KeyEvent.VK_RIGHT);
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame().getLastTaskCommandResult() != null
				&& manager.getCurrentFrame().getLastTaskCommandResult().status() == RibbonCommandResult.Status.REJECTED,
				"the outline shortcut did not publish rejection for the read-only project");
		assertEquals("document-read-only", manager.getCurrentFrame().getLastTaskCommandResult().reason());
		assertNull(second.getWbsParentTask(), "read-only outline shortcut changed the task hierarchy");
		assertFalse(undo.canUndo(), "read-only outline shortcut created an undo edit");
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

		Robot robot = new com.microproject.testsupport.GuiRobot();
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
		assertSame(manager.getAction(com.microproject.menu.MenuActionConstants.ACTION_INDENT), indent.getAction(),
				"the physical task popup must use the canonical GraphicManager Indent Action");
		click(robot, indent);
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
	void robotTaskPopupInsertAddsSelectedRowCountBelowSelectionAndOneUndoRevertsBatch() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		NormalTask first = createTask();
		Project project = first.getOwningProject();
		Node secondNode = project.createLocalTaskNode(null);
		NormalTask second = (NormalTask) secondNode.getImpl();
		second.setName("Popup multi insert second");
		Node thirdNode = project.createLocalTaskNode(null);
		NormalTask third = (NormalTask) thirdNode.getImpl();
		third.setName("Popup multi insert third");
		project.recalculate();
		showProject(project);
		SwingUtilities.invokeAndWait(() -> window.setSize(1600, 700));
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame() != null
				&& manager.getCurrentFrame().getActiveSpreadSheet() != null,
			"multi insert project did not become visible");

		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		activateWindow(robot, window);
		SpreadSheet sheet = manager.getCurrentFrame().getActiveSpreadSheet();
		Rectangle firstCell = cellOnScreen(sheet, rowForTask(sheet, first), nameColumn(sheet));
		Rectangle secondCell = cellOnScreen(sheet, rowForTask(sheet, second), nameColumn(sheet));
		click(robot, firstCell);
		robot.keyPress(KeyEvent.VK_SHIFT);
		click(robot, secondCell);
		robot.keyRelease(KeyEvent.VK_SHIFT);
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame().getSelectedImpls(false).containsAll(List.of(first, second)),
			"Shift-click did not select both task rows");
		Object root = project.getTaskModel().getRoot();
		int beforeChildren = project.getTaskModel().getChildCount(root);
		int beforeRows = sheet.getRowCount();
		int secondIndex = project.getTaskModel().getIndexOfChild(root, secondNode);
		int thirdRowBefore = rowForTask(sheet, third);
		int secondRowBefore = rowForTask(sheet, second);
		List<Object> beforeOrder = new ArrayList<>();
		for (int index = 0; index < beforeChildren; index++) beforeOrder.add(project.getTaskModel().getChild(root, index));
		project.getUndoController().clear();
		rightClick(robot, secondCell);
		SpreadSheetPopupMenu popup = sheet.getPopup();
		GuiAcceptanceSupport.await(() -> popup != null && popup.isVisible(),
			"physical right click did not show the task popup for multi insert");
		JMenuItem insert = popupItem(popup, "popup." + com.microproject.menu.MenuActionConstants.ACTION_INSERT_TASK);
		GuiAcceptanceSupport.await(insert::isEnabled, "task popup Insert remained disabled for the selected rows");
		click(robot, insert);
		GuiAcceptanceSupport.await(() -> project.getTaskModel().getChildCount(root) == beforeChildren + 2,
			"popup Insert did not add two contiguous model rows");
		GuiAcceptanceSupport.await(() -> sheet.getRowCount() == beforeRows + 2,
			"the task table did not render both inserted rows");
		GuiAcceptanceSupport.await(() -> rowForTask(sheet, third) - rowForTask(sheet, second) == 3,
			"the rendered blank rows are not located directly below the selected range");
		assertEquals(1, thirdRowBefore - secondRowBefore,
			"the fixture tasks must be adjacent before insertion");
		List<Object> afterOrder = new ArrayList<>();
		for (int index = 0; index < project.getTaskModel().getChildCount(root); index++) afterOrder.add(project.getTaskModel().getChild(root, index));
		assertEquals(beforeOrder.subList(0, secondIndex + 1), afterOrder.subList(0, secondIndex + 1));
		assertEquals(beforeOrder.subList(secondIndex + 1, beforeOrder.size()),
			afterOrder.subList(secondIndex + 3, afterOrder.size()),
			"popup Insert must place the complete batch directly below the selected range");
		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Z);
		GuiAcceptanceSupport.await(() -> project.getTaskModel().getChildCount(root) == beforeChildren,
			"one Ctrl+Z did not remove the complete two-row insertion");
		GuiAcceptanceSupport.await(() -> rowForTask(sheet, third) - rowForTask(sheet, second) == 1,
			"the task table did not restore its original row positions after Undo");
		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Y);
		GuiAcceptanceSupport.await(() -> project.getTaskModel().getChildCount(root) == beforeChildren + 2,
			"one Ctrl+Y did not restore the complete two-row insertion");
		GuiAcceptanceSupport.await(() -> rowForTask(sheet, third) - rowForTask(sheet, second) == 3,
			"the task table did not restore the inserted row positions after Redo");
		Node insertedNode = (Node) project.getTaskModel().getChild(root, secondIndex + 1);
		int insertedRow = rowForTask(sheet, second) + 1;
		assertSame(insertedNode, sheet.getNodeAtRow(insertedRow),
			"the visible row immediately after the selected range must be the inserted placeholder Node");
		assertTrue(insertedNode.getImpl() instanceof com.microproject.grouping.core.VoidNodeImpl,
			"the first newly inserted task row must start blank");
		click(robot, cellOnScreen(sheet, insertedRow, nameColumn(sheet)));
		GuiAcceptanceSupport.await(sheet::isFocusOwner, "the new task name cell did not receive focus");
		press(robot, KeyEvent.VK_F2);
		GuiAcceptanceSupport.await(sheet::isEditing, "F2 did not start editing the inserted task name");
		Component editor = sheet.getEditorComponent();
		GuiAcceptanceSupport.await(() -> editor != null
			&& KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner() != null
			&& (KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner() == editor
				|| (editor instanceof java.awt.Container container
					&& SwingUtilities.isDescendingFrom(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner(), container))),
			"the inserted task editor did not receive keyboard focus");
		for (char character : "new beta task".toCharArray()) {
			int keyCode = character == ' ' ? KeyEvent.VK_SPACE : KeyEvent.getExtendedKeyCodeForChar(character);
			if (keyCode == KeyEvent.VK_UNDEFINED) throw new AssertionError("No key code for " + character);
			robot.keyPress(keyCode);
			robot.keyRelease(keyCode);
		}
		robot.waitForIdle();
		press(robot, KeyEvent.VK_ENTER);
		GuiAcceptanceSupport.await(() -> !sheet.isEditing()
			&& sheet.getNodeAtRow(insertedRow) != null
			&& sheet.getNodeAtRow(insertedRow).getImpl() instanceof NormalTask
			&& "new beta task".equals(((NormalTask) sheet.getNodeAtRow(insertedRow).getImpl()).getName()),
			"physical keyboard input did not commit the inserted task name");
		ByteArrayOutputStream insertedSnapshot = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(project, insertedSnapshot),
			"MPO save rejected the newly inserted and named task");
		Project insertedReload = new MpoFileImporter().loadProject(new ByteArrayInputStream(insertedSnapshot.toByteArray()));
		assertEquals("new beta task", taskNamed(insertedReload, "new beta task").getName(),
			"MPO reload lost the task created by physical Insert and keyboard input");
		SwingUtilities.invokeAndWait(sheet::clearSelection);
		int beforeUnselectedInsert = project.getTaskModel().getChildCount(root);
		press(robot, KeyEvent.VK_INSERT);
		GuiAcceptanceSupport.await(() -> project.getTaskModel().getChildCount(root) == beforeUnselectedInsert + 1,
			"Insert with no selected row did not preserve the single-row default");
		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Z);
		GuiAcceptanceSupport.await(() -> project.getTaskModel().getChildCount(root) == beforeUnselectedInsert,
			"one Ctrl+Z did not revert the single-row default insertion");
	}

	@Test
	void robotRightClickTaskPopupHideAndShowUseTheSharedVisibilityRoute() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		previousClientSide = Environment.isClientSide();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		Environment.setClientSide(true);
		Environment.setBatchMode(false);
		assertTrue(Alert.allowPopups(), "the lock rejection acceptance test requires interactive alerts");
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

		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		activateWindow(robot, window);
		SpreadSheet sheet = manager.getCurrentFrame().getActiveSpreadSheet();
		Gantt gantt = manager.getCurrentFrame().getGanttView().getGantt();
		Rectangle targetCell = cellOnScreen(sheet, rowForTask(sheet, target), nameColumn(sheet));
		click(robot, targetCell);
		rightClick(robot, targetCell);
		SpreadSheetPopupMenu popup = sheet.getPopup();
		GuiAcceptanceSupport.await(() -> popup != null && popup.isVisible(),
			"physical right click did not show the task popup for Hide");
		JMenuItem hide = popupItem(popup, "popup." + com.microproject.menu.MenuActionConstants.ACTION_HIDE_SELECTED_TASKS);
		GuiAcceptanceSupport.await(hide::isEnabled, "popup Hide Selected Tasks remained disabled for the selected task");
		Alert.setPresenter(new SwingAlertPresenter());
		project.setCollaborationSession(new CollaborationSession(project,
				System.getProperty("java.io.tmpdir") + "/microproject-popup-visibility-lock.mpo", "visibility-lock-test") {
			@Override
			public Task tryAcquireTasks(Iterable<Task> tasks) {
				if (tasks == null) return null;
				java.util.Iterator<Task> iterator = tasks.iterator();
				return iterator.hasNext() ? iterator.next() : null;
			}

			@Override
			public String describeLockOwner(Task task) {
				return "robot lock test";
			}
		});
		click(robot, hide);
		GuiAcceptanceSupport.await(() -> findDialogContainingText("locked by robot lock test") != null,
				"locked Hide did not report the collaboration rejection to the user");
		Dialog lockWarning = findDialogContainingText("locked by robot lock test");
		assertFalse(target.isHiddenTask(), "a collaborator-locked task must remain unchanged after Hide rejection");
		assertTrue(isTaskVisible(sheet, target) && isTaskVisibleInGantt(gantt, target),
				"a rejected Hide must preserve both table and Gantt rows");
		assertEquals(RibbonCommandResult.Status.REJECTED, manager.getLastRibbonCommandResult().status(),
				"the popup route must publish a rejected semantic command outcome when locking fails");
		assertEquals("lock-failed", manager.getLastRibbonCommandResult().reason());
		press(robot, KeyEvent.VK_ENTER);
		GuiAcceptanceSupport.await(() -> !lockWarning.isShowing(), "the collaboration lock warning did not close after Enter");
		project.setCollaborationSession(null);
		rightClick(robot, targetCell);
		GuiAcceptanceSupport.await(popup::isVisible, "task popup did not reopen after dismissing the lock warning");
		hide = popupItem(popup, "popup." + com.microproject.menu.MenuActionConstants.ACTION_HIDE_SELECTED_TASKS);
		click(robot, hide);
		GuiAcceptanceSupport.await(target::isHiddenTask, "popup Hide Selected Tasks did not update the task model");
		GuiAcceptanceSupport.await(() -> !isTaskVisible(sheet, target), "popup Hide Selected Tasks did not remove the visible row");
		GuiAcceptanceSupport.await(() -> !isTaskVisibleInGantt(gantt, target), "popup Hide Selected Tasks did not remove the Gantt row");
		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Z);
		GuiAcceptanceSupport.await(() -> !target.isHiddenTask(), "Ctrl+Z did not undo popup Hide Selected Tasks");
		GuiAcceptanceSupport.await(() -> isTaskVisible(sheet, target) && isTaskVisibleInGantt(gantt, target),
				"Undo popup Hide did not restore the table and Gantt rows");
		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Y);
		GuiAcceptanceSupport.await(target::isHiddenTask, "Ctrl+Y did not redo popup Hide Selected Tasks");
		GuiAcceptanceSupport.await(() -> !isTaskVisible(sheet, target) && !isTaskVisibleInGantt(gantt, target),
				"Redo popup Hide did not hide the table and Gantt rows");

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
		click(robot, show);
		GuiAcceptanceSupport.await(() -> !target.isHiddenTask(), "popup Show All Tasks did not restore the task model");
		GuiAcceptanceSupport.await(() -> isTaskVisible(sheet, target), "popup Show All Tasks did not restore the visible row");
		GuiAcceptanceSupport.await(() -> isTaskVisibleInGantt(gantt, target), "popup Show All Tasks did not restore the Gantt row");
		ByteArrayOutputStream shownSnapshot = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(project, shownSnapshot), "MPO save rejected popup Show All state");
		Project shownReload = new MpoFileImporter().loadProject(new ByteArrayInputStream(shownSnapshot.toByteArray()));
		assertFalse(taskNamed(shownReload, target.getName()).isHiddenTask(), "MPO reload lost popup Show All state");
		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Z);
		GuiAcceptanceSupport.await(target::isHiddenTask, "Ctrl+Z did not undo popup Show All Tasks");
		GuiAcceptanceSupport.await(() -> !isTaskVisible(sheet, target) && !isTaskVisibleInGantt(gantt, target),
				"Undo popup Show All did not hide the table and Gantt rows");
		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Y);
		GuiAcceptanceSupport.await(() -> !target.isHiddenTask(), "Ctrl+Y did not redo popup Show All Tasks");
		GuiAcceptanceSupport.await(() -> isTaskVisible(sheet, target) && isTaskVisibleInGantt(gantt, target),
				"Redo popup Show All did not restore the table and Gantt rows");
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

		Robot robot = new com.microproject.testsupport.GuiRobot();
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
	void physicalTaskRowHeaderDragReordersStableTasksAndSurvivesUndoAndReload() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		Environment.setClientSide(true);
		Environment.setBatchMode(false);
		Alert.setPresenter(new SwingAlertPresenter());

		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("physical-task-row-drag", undo), undo);
		project.initialize(false, false);
		NormalTask first = project.createScriptedTask();
		first.setName("Row drag first");
		NormalTask second = project.createScriptedTask();
		second.setName("Row drag second");
		NormalTask third = project.createScriptedTask();
		third.setName("Row drag third");
		project.recalculate();
		List<String> originalOrder = taskOrder(project);
		assertEquals(List.of("Row drag first", "Row drag second", "Row drag third"), originalOrder);
		showProject(project);
		SwingUtilities.invokeAndWait(() -> window.setSize(1600, 700));
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame() != null
				&& manager.getCurrentFrame().getActiveSpreadSheet() != null,
			"row-drag test project did not become visible");

		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(40);
		activateWindow(robot, window);
		SpreadSheet sheet = manager.getCurrentFrame().getActiveSpreadSheet();
		int sourceRow = rowForTask(sheet, second);
		int targetRow = rowForTask(sheet, first);
		JTable[] rowHeaderBox = new JTable[1];
		Point[] points = new Point[2];
		int[] dragState = { 0, -1 };
		SwingUtilities.invokeAndWait(() -> {
			rowHeaderBox[0] = sheet.getRowHeader();
			JTable rowHeader = rowHeaderBox[0];
			rowHeader.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
				@Override
				public void mouseDragged(java.awt.event.MouseEvent event) {
					dragState[0]++;
					dragState[1] = rowHeader.rowAtPoint(event.getPoint());
				}
			});
		});
		JTable rowHeader = rowHeaderBox[0];
		GuiAcceptanceSupport.await(() -> sheet.isShowing() && rowHeader.isShowing(),
			"task row header was not visible for the physical move gesture; " + componentInputDiagnostics(rowHeader));
		points[0] = moveToRowHeaderTarget(robot, rowHeader, sourceRow, false,
			"Robot source move did not hit the visible source row");
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		try {
			robot.delay(100);
			GuiAcceptanceSupport.await(() -> {
				boolean[] selected = new boolean[1];
				try {
					SwingUtilities.invokeAndWait(() -> selected[0] = sheet.isRowFullySelected(sourceRow));
				} catch (Exception exception) {
					throw new AssertionError("could not inspect row selection on the EDT", exception);
				}
				return selected[0];
			}, "physical row-header press must select the complete source task row before dragging");
			RowHeaderTarget initialTarget = rowHeaderTargetOnScreen(rowHeader, targetRow, true);
			robot.mouseMove((points[0].x + initialTarget.screenPoint().x) / 2,
				(points[0].y + initialTarget.screenPoint().y) / 2);
			int[] observedDrag = new int[3];
			Point[] targetHover = new Point[1];
			RowHeaderTarget[] currentTarget = new RowHeaderTarget[1];
			long cursorDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
			do {
				currentTarget[0] = rowHeaderTargetOnScreen(rowHeader, targetRow, true);
				points[1] = currentTarget[0].screenPoint();
				robot.mouseMove(points[1].x, points[1].y);
				robot.waitForIdle();
				SwingUtilities.invokeAndWait(() -> {
					observedDrag[0] = dragState[0];
					observedDrag[1] = dragState[1];
					observedDrag[2] = rowHeader.getCursor().getType();
					Point hover = rowHeader.getMousePosition();
					targetHover[0] = hover;
				});
				if (rowHeaderHoverRow(rowHeader) == targetRow && observedDrag[0] > 0
						&& observedDrag[1] == targetRow && observedDrag[2] == Cursor.MOVE_CURSOR)
					break;
				if (System.nanoTime() >= cursorDeadline)
					break;
				Thread.sleep(25);
			} while (true);
			assertTrue(observedDrag[0] > 0, "physical row-header drag must deliver MOUSE_DRAGGED; events="
				+ observedDrag[0] + " source=" + points[0] + " target=" + points[1]
				+ " targetHover=" + targetHover[0] + " " + componentInputDiagnostics(rowHeader));
			assertEquals(targetRow, observedDrag[1], "physical drag must reach the intended target task row; source="
				+ sourceRow + " target=" + targetRow + " lastDragRow=" + observedDrag[1]
				+ " target=" + currentTarget[0]);
			assertEquals(Cursor.MOVE_CURSOR, observedDrag[2], "valid row-header target must show move cursor before release; "
				+ "cursor=" + observedDrag[2] + " dragEvents=" + observedDrag[0] + " dragRow=" + observedDrag[1]
				+ " sourcePoint=" + points[0] + " targetPoint=" + points[1] + " pointer="
				+ java.awt.MouseInfo.getPointerInfo().getLocation() + " target=" + currentTarget[0]);

			assertEquals(targetRow, rowHeaderHoverRow(rowHeader),
				"physical pointer must remain over the target task row before release");
		} finally {
			robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
			robot.waitForIdle();
		}

		GuiAcceptanceSupport.await(() -> visibleMoveConfirmation() != null,
			"physical row-header drag did not ask for its documented move confirmation");
		Dialog confirmation = visibleMoveConfirmation();
		AbstractButton accept = findVisibleButton(confirmation, "OK");
		if (accept == null) accept = findVisibleButton(confirmation, "はい");
		if (accept == null) accept = findVisibleButton(confirmation, "確認");
		assertNotNull(accept, "move confirmation must expose its affirmative button");
		click(robot, accept);
		List<String> movedOrder = List.of("Row drag second", "Row drag first", "Row drag third");
		GuiAcceptanceSupport.await(() -> taskOrder(project).equals(movedOrder),
			"accepted physical task row drag did not reorder the selected stable task");
		assertEquals(0, rowForTask(sheet, second), "the spreadsheet must redraw the moved task at its new row");
		assertEquals(1, rowForTask(sheet, first), "the spreadsheet must redraw the relocation anchor after the moved task");

		ByteArrayOutputStream saved = new ByteArrayOutputStream();
		MpoFileImporter importer = new MpoFileImporter();
		assertTrue(importer.saveProject(project, saved), "row-drag hierarchy mutation must save as MPO");
		Project reopened = importer.loadProject(new ByteArrayInputStream(saved.toByteArray()));
		assertEquals(movedOrder, taskOrder(reopened), "row-drag hierarchy mutation must survive MPO save/reload");
		activateWindow(robot, window);
		SwingUtilities.invokeAndWait(sheet::requestFocusInWindow);
		GuiAcceptanceSupport.await(() -> sheet.isFocusOwner(), "task table did not regain focus for physical Undo/Redo");
		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Z);
		GuiAcceptanceSupport.await(() -> taskOrder(project).equals(originalOrder),
			"one physical Ctrl+Z must restore the exact pre-drag task order");
		assertEquals(0, rowForTask(sheet, first), "Undo must redraw the original task table order");
		assertEquals(1, rowForTask(sheet, second), "Undo must restore the moved task's original row");
		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Y);
		GuiAcceptanceSupport.await(() -> taskOrder(project).equals(movedOrder),
			"one physical Ctrl+Y must reapply the task row relocation");
		assertEquals(0, rowForTask(sheet, second), "Redo must redraw the moved task at its relocated row");
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

		Robot robot = new com.microproject.testsupport.GuiRobot();
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
		click(robot, delete);
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
	void robotNameCellIndentShortcutsRoundTripModelAndPersistence() throws Exception {
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
		Gantt gantt = manager.getCurrentFrame().getGanttView().getGantt();
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		activateWindow(robot, window);
		SpreadSheet sheet = manager.getCurrentFrame().getActiveSpreadSheet();
		int nameColumn = nameColumn(sheet);
		SwingUtilities.invokeAndWait(sheet::requestFocusInWindow);
		GuiAcceptanceSupport.await(sheet::isFocusOwner, "shortcut spreadsheet did not accept focus before Robot input");

		// Microsoft publishes conflicting Project Desktop key directions in these references:
		// https://support.microsoft.com/en-us/accessibility/project/keyboard-shortcuts-for-project
		// https://support.microsoft.com/en-us/project/edit-a-project-in-project-desktop
		// This verifies microProject's existing root-pane binding; the compatibility choice remains open in issue #414.
		click(robot, cellOnScreen(sheet, rowForTask(sheet, target), nameColumn));
		GuiAcceptanceSupport.await(sheet::isFocusOwner, "name-cell click did not give focus to the spreadsheet");
		press(robot, KeyEvent.VK_F2);
		GuiAcceptanceSupport.await(sheet::isEditing, "F2 did not enter name-cell editing");
		press(robot, KeyEvent.VK_ALT, KeyEvent.VK_SHIFT, KeyEvent.VK_RIGHT);
		GuiAcceptanceSupport.await(() -> target.getWbsParentTask() == predecessor,
				"Robot Alt+Shift+Right did not indent the selected name row");
		GuiAcceptanceSupport.await(() -> isTaskVisibleInGantt(gantt, target),
				"Indent shortcut removed the task from the Gantt projection");
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame().getSelectedImpls(false).contains(target),
				"Robot Alt+Shift+Right lost the selected task");
		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Z);
		GuiAcceptanceSupport.await(() -> target.getWbsParentTask() == null,
				"Ctrl+Z did not undo shortcut Indent");
		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Y);
		GuiAcceptanceSupport.await(() -> target.getWbsParentTask() == predecessor,
				"Ctrl+Y did not redo shortcut Indent");
		click(robot, cellOnScreen(sheet, rowForTask(sheet, outdentTarget), nameColumn));
		GuiAcceptanceSupport.await(sheet::isFocusOwner, "second name-cell click did not give focus to the spreadsheet");
		press(robot, KeyEvent.VK_F2);
		GuiAcceptanceSupport.await(sheet::isEditing, "F2 did not enter the second name-cell edit");
		press(robot, KeyEvent.VK_ALT, KeyEvent.VK_SHIFT, KeyEvent.VK_LEFT);
		GuiAcceptanceSupport.await(() -> outdentTarget.getWbsParentTask() == null,
				"Robot Alt+Shift+Left did not outdent the selected name row");
		GuiAcceptanceSupport.await(() -> isTaskVisibleInGantt(gantt, outdentTarget),
				"Outdent shortcut removed the task from the Gantt projection");
		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Z);
		GuiAcceptanceSupport.await(() -> outdentTarget.getWbsParentTask() == predecessor,
				"Ctrl+Z did not undo shortcut Outdent");
		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Y);
		GuiAcceptanceSupport.await(() -> outdentTarget.getWbsParentTask() == null,
				"Ctrl+Y did not redo shortcut Outdent");

		ByteArrayOutputStream saved = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(project, saved),
				"MPO save rejected the hierarchy from the physical indent shortcuts");
		Project reloaded = new MpoFileImporter().loadProject(new ByteArrayInputStream(saved.toByteArray()));
		assertNotNull(taskNamed(reloaded, "Shortcut predecessor"), "MPO reload lost the shortcut predecessor");
		assertSame(taskNamed(reloaded, "Shortcut predecessor"), taskNamed(reloaded, "Shortcut target").getWbsParentTask(),
				"MPO reload lost the hierarchy from Alt+Shift+Right");
		assertTrue(taskNamed(reloaded, "Shortcut outdent target").getWbsParentTask() == null,
				"MPO reload retained the parent removed by Alt+Shift+Left");

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
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		Gantt gantt = manager.getCurrentFrame().getGanttView().getGantt();
		assertEquals(0, gantt.getModel().getCache().getEdgesSize(), "fixture must start without Gantt dependencies");
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
		clickAndAwaitSelected(robot, taskTab, "Robot click did not select the Task ribbon tab");
		assertFalse(findShowingButtonByCommand("RibbonTaskInformation").isEnabled(),
			"Task Information must not stay enabled for a multi-task selection");
		AbstractButton link = findVisibleOrOverflowButton(robot, "RibbonLink");
		GuiAcceptanceSupport.await(link::isEnabled, "Link remained disabled for two selected tasks");
		Environment.setClientSide(true);
		Environment.setBatchMode(false);
		Alert.setPresenter(new SwingAlertPresenter());
		undo.clear();
		project.setCollaborationSession(new CollaborationSession(project,
				System.getProperty("java.io.tmpdir") + "/microproject-ribbon-link-lock.mpo", "link-lock-test") {
			@Override
			public Task tryAcquireTasks(Iterable<Task> tasks) {
				if (tasks == null) return null;
				java.util.Iterator<Task> iterator = tasks.iterator();
				return iterator.hasNext() ? iterator.next() : null;
			}

			@Override
			public String describeLockOwner(Task task) {
				return "robot link lock test";
			}
		});
		click(robot, link);
		GuiAcceptanceSupport.await(() -> findDialogContainingText("locked by robot link lock test") != null,
				"the Ribbon Link route did not show the collaboration lock rejection");
		Dialog lockWarning = findDialogContainingText("locked by robot link lock test");
		assertTrue(successor.getPredecessorList().isEmpty(), "a lock-rejected Ribbon Link must not mutate dependencies");
		assertEquals(0, gantt.getModel().getCache().getEdgesSize(), "a lock-rejected Ribbon Link must not add a Gantt edge");
		assertEquals(RibbonCommandResult.Status.REJECTED, manager.getLastRibbonCommandResult().status(),
				"the Ribbon Link route must publish rejection while its warning remains open");
		assertEquals("lock-failed", manager.getLastRibbonCommandResult().reason());
		assertFalse(undo.canUndo(), "a lock-rejected Ribbon Link must not create an undo edit");
		press(robot, KeyEvent.VK_ENTER);
		GuiAcceptanceSupport.await(() -> !lockWarning.isShowing(), "the Ribbon Link lock warning did not close after Enter");
		project.setCollaborationSession(null);
		GuiAcceptanceSupport.await(link::isEnabled, "Link stayed disabled after dismissing the lock rejection");
		RibbonCommandResult previousLinkOutcome = manager.getLastRibbonCommandResult();
		click(robot, link);
		GuiAcceptanceSupport.await(() -> successor.getPredecessorList().size() == 1,
				"Link did not create a dependency between the selected tasks");
		GuiAcceptanceSupport.await(() -> gantt.getModel().getCache().getEdgesSize() == 1,
				"Link did not create one visible Gantt dependency");
		assertRibbonOutcome(manager, previousLinkOutcome, RibbonCommandResult.Status.CHANGED,
			predecessor.getUniqueId(), successor.getUniqueId());
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
		GuiAcceptanceSupport.await(() -> successor.getPredecessorList().isEmpty()
				&& gantt.getModel().getCache().getEdgesSize() == 0, "Ctrl+Z did not undo the link");
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_Y);
		robot.keyRelease(KeyEvent.VK_Y);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		robot.waitForIdle();
		GuiAcceptanceSupport.await(() -> successor.getPredecessorList().size() == 1
				&& gantt.getModel().getCache().getEdgesSize() == 1, "Ctrl+Y did not redo the link");

		AbstractButton unlink = findVisibleOrOverflowButton(robot, "RibbonUnlink");
		GuiAcceptanceSupport.await(unlink::isEnabled, "Unlink became disabled after link creation");
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame().getSelectedImpls(false).contains(predecessor)
				&& manager.getCurrentFrame().getSelectedImpls(false).contains(successor),
			"Undo/Redo did not preserve both selected tasks for Unlink");
		RibbonCommandResult previousUnlinkOutcome = manager.getLastRibbonCommandResult();
		click(robot, unlink);
		GuiAcceptanceSupport.await(() -> successor.getPredecessorList().isEmpty(),
				"Unlink did not remove the dependency between the selected tasks");
		GuiAcceptanceSupport.await(() -> gantt.getModel().getCache().getEdgesSize() == 0,
				"Unlink did not remove the visible Gantt dependency");
		assertRibbonOutcome(manager, previousUnlinkOutcome, RibbonCommandResult.Status.CHANGED,
			predecessor.getUniqueId(), successor.getUniqueId());
		ByteArrayOutputStream unlinkedSnapshot = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(project, unlinkedSnapshot),
				"MPO save did not accept the project after the physical Unlink command");
		Project unlinkedReload = new MpoFileImporter().loadProject(new ByteArrayInputStream(unlinkedSnapshot.toByteArray()));
		assertEquals(0, taskNamed(unlinkedReload, "Link successor").getPredecessorList().size(),
				"MPO reload restored the dependency removed through the ribbon");
		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Z);
		GuiAcceptanceSupport.await(() -> successor.getPredecessorList().size() == 1
				&& gantt.getModel().getCache().getEdgesSize() == 1, "Ctrl+Z did not undo Unlink");
		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Y);
		GuiAcceptanceSupport.await(() -> successor.getPredecessorList().isEmpty()
				&& gantt.getModel().getCache().getEdgesSize() == 0, "Ctrl+Y did not redo Unlink");
	}

	@Test
	void selectedTaskLevelingUsesTheResourceRibbonAndSupportsUndoRedoAndPersistence() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("ribbon-level-selection", undo);
		pool.setLocal(true);
		Project project = Project.createProject(pool, undo);
		project.initialize(false, false);
		NormalTask fixed = project.createScriptedTask();
		fixed.setName("Unselected capacity anchor");
		NormalTask selected = project.createScriptedTask();
		selected.setName("Selected leveling target");
		Resource shared = pool.newResourceInstance();
		shared.setName("Leveling resource");
		AssignmentService.getInstance().newAssignment(fixed, shared, 1.0, 0L, getClass());
		AssignmentService.getInstance().newAssignment(selected, shared, 1.0, 0L, getClass());
		project.recalculate();
		long originalStart = ((Assignment) selected.getAssignments().iterator().next()).getStart();
		showProject(project);
		SwingUtilities.invokeAndWait(() -> window.setSize(1600, 700));
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame() != null
				&& manager.getCurrentFrame().getActiveSpreadSheet() != null,
			"resource leveling task sheet did not become visible");
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		SpreadSheet sheet = manager.getCurrentFrame().getActiveSpreadSheet();
		click(robot, cellOnScreen(sheet, rowForTask(sheet, selected), nameColumn(sheet)));
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame().getSelectedImpls(false).contains(selected),
			"physical task click did not select the intended leveling target");
		AbstractButton resourceTab = findShowingButtonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
				.getString("ResourceRibbonTask.title"));
		clickAndAwaitSelected(robot, resourceTab, "Robot click did not select the Resource ribbon tab");
		AbstractButton levelSelection = findShowingButtonByCommand("RibbonLevelSelection");
		GuiAcceptanceSupport.await(levelSelection::isEnabled,
			"Level Selection must be enabled for one selected editable task");
		RibbonCommandResult before = manager.getLastRibbonCommandResult();
		click(robot, levelSelection);
		GuiAcceptanceSupport.await(() -> levelingDelay(selected) > 0,
			"Resource > Level Selection did not apply leveling delay to the selected task");
		assertEquals(0L, levelingDelay(fixed), "unselected task must remain a fixed leveling anchor");
		GuiAcceptanceSupport.await(() -> manager.getLastRibbonCommandResult() != before
				&& manager.getLastRibbonCommandResult().status() == RibbonCommandResult.Status.CHANGED,
			"Level Selection did not publish its changed result");
		GuiAcceptanceSupport.await(() -> ((Assignment) selected.getAssignments().iterator().next()).getStart() > originalStart,
			"the selected task's leveled assignment start did not move");
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame().getSelectedImpls(false).contains(selected),
			"Level Selection did not preserve the task selection after ribbon focus transfer");
		ByteArrayOutputStream saved = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(project, saved), "MPO save rejected the leveled project");
		Project reloaded = new MpoFileImporter().loadProject(new ByteArrayInputStream(saved.toByteArray()));
		assertTrue(levelingDelay(taskNamed(reloaded, "Selected leveling target")) > 0,
			"MPO reload lost selected-task leveling delay");
		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Z);
		GuiAcceptanceSupport.await(() -> levelingDelay(selected) == 0L,
			"one Ctrl+Z did not restore the selected task's leveling delay");
		assertEquals(0L, levelingDelay(fixed), "Undo must not mutate the unselected anchor");
		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Y);
		GuiAcceptanceSupport.await(() -> levelingDelay(selected) > 0,
			"one Ctrl+Y did not restore selected-task leveling");
	}

	@Test
	void clearLevelingRibbonScopesPreserveManualSplitsAndRoundTripUndoMpo() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("ribbon-clear-leveling", undo), undo);
		project.initialize(false, false);
		NormalTask selected = project.createScriptedTask();
		selected.setName("Clear leveling selected");
		NormalTask other = project.createScriptedTask();
		other.setName("Clear leveling other");
		long hour = com.microproject.pm.calendar.WorkCalendar.MILLIS_IN_HOUR;
		long workday = com.microproject.options.CalendarOption.getInstance().getMillisPerDay();
		selected.setDuration(workday);
		other.setDuration(workday);
		long selectedGapStart = selected.getStart() + hour;
		selected.splitForLeveling(this, selectedGapStart, selectedGapStart + hour);
		selected.split(this, selectedGapStart, selectedGapStart + hour);
		long otherGapStart = other.getStart() + 2L * hour;
		other.splitForLeveling(this, otherGapStart, otherGapStart + hour);
		selected.setLevelingDelay(com.microproject.datatype.Duration.millis(86_400_000L));
		other.setLevelingDelay(com.microproject.datatype.Duration.millis(172_800_000L));
		assertEquals(1, selected.getLevelingTaskSplitIntervals().size(),
			"fixture must start with one generated split on the selected task");
		assertEquals(1, selected.getManualTaskSplitIntervals().size(),
			"fixture must retain an overlapping manual split as independent ownership");
		assertEquals(1, other.getLevelingTaskSplitIntervals().size(),
			"fixture must start with a generated split on the unselected task");
		undo.clear();
		project.recalculate();
		showProject(project);
		SwingUtilities.invokeAndWait(() -> window.setSize(1600, 700));
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame() != null
				&& manager.getCurrentFrame().getActiveSpreadSheet() != null,
			"Clear Leveling task sheet did not become visible");
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		SpreadSheet sheet = manager.getCurrentFrame().getActiveSpreadSheet();
		click(robot, cellOnScreen(sheet, rowForTask(sheet, selected), nameColumn(sheet)));
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame().getSelectedImpls(false).contains(selected),
			"physical task click did not select the Clear Leveling target");
		clickAndAwaitSelected(robot, findShowingButtonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
				.getString("ResourceRibbonTask.title")), "Robot click did not select the Resource ribbon tab");
		AbstractButton clearSelected = findVisibleOrOverflowButton(robot, "RibbonClearLevelSelection");
		GuiAcceptanceSupport.await(clearSelected::isEnabled,
			"Clear Leveling (Selection) must be enabled for an editable task selection");
		String activeView = manager.getCurrentFrame().getTopViewId();
		click(robot, clearSelected);
		GuiAcceptanceSupport.await(() -> findShowingOptionPane() != null,
			"Clear selected leveling must ask for confirmation before changing the project");
		press(robot, KeyEvent.VK_ESCAPE);
		GuiAcceptanceSupport.await(() -> findShowingOptionPane() == null,
			"Escape did not cancel Clear Leveling confirmation");
		assertEquals(86_400_000L, com.microproject.datatype.Duration.millis(selected.getLevelingDelay()),
			"canceling confirmation must leave selected leveling untouched");
		assertEquals(1, selected.getLevelingTaskSplitIntervals().size());
		assertFalse(undo.canUndo(), "canceling confirmation must not add an Undo edit");
		assertTrue(manager.getCurrentFrame().getSelectedImpls(false).contains(selected),
			"canceling confirmation must retain the physical task selection");
		assertEquals(activeView, manager.getCurrentFrame().getTopViewId(), "canceling confirmation must retain the active view");
		assertEquals(RibbonCommandResult.Status.REJECTED, manager.getLastRibbonCommandResult().status());
		click(robot, clearSelected);
		GuiAcceptanceSupport.await(() -> findShowingOptionPane() != null,
			"Clear selected leveling confirmation did not reopen");
		press(robot, KeyEvent.VK_ENTER);
		GuiAcceptanceSupport.await(() -> com.microproject.datatype.Duration.millis(selected.getLevelingDelay()) == 0L
				&& selected.getLevelingTaskSplitIntervals().isEmpty(),
			"Clear Leveling (Selection) did not clear selected delay and generated split");
		assertEquals(1, selected.getManualTaskSplitIntervals().size(), "manual split must remain after selected clear");
		assertEquals(172_800_000L, com.microproject.datatype.Duration.millis(other.getLevelingDelay()),
			"selected clear must leave other tasks unchanged");
		assertEquals(1, other.getLevelingTaskSplitIntervals().size());
		assertTrue(manager.getCurrentFrame().getSelectedImpls(false).contains(selected),
			"Clear Leveling must preserve the user's task selection");
		assertEquals(activeView, manager.getCurrentFrame().getTopViewId(), "Clear Leveling must preserve the active view");
		assertEquals(List.of(selected.getUniqueId()), manager.getLastRibbonCommandResult().affectedTaskIds());
		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Z);
		GuiAcceptanceSupport.await(() -> com.microproject.datatype.Duration.millis(selected.getLevelingDelay()) == 86_400_000L
				&& selected.getLevelingTaskSplitIntervals().size() == 1,
			"one Ctrl+Z did not restore the selected leveling state");
		assertEquals(1, selected.getManualTaskSplitIntervals().size(), "Undo must retain manual split ownership");
		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Y);
		GuiAcceptanceSupport.await(() -> com.microproject.datatype.Duration.millis(selected.getLevelingDelay()) == 0L
				&& selected.getLevelingTaskSplitIntervals().isEmpty(),
			"one Ctrl+Y did not clear selected leveling again");

		click(robot, findVisibleOrOverflowButton(robot, "RibbonClearLevelAll"));
		GuiAcceptanceSupport.await(() -> findShowingOptionPane() != null,
			"Clear Leveling (All) must ask for confirmation before changing the project");
		press(robot, KeyEvent.VK_ENTER);
		GuiAcceptanceSupport.await(() -> com.microproject.datatype.Duration.millis(other.getLevelingDelay()) == 0L
				&& other.getLevelingTaskSplitIntervals().isEmpty(),
			"Clear Leveling (All) did not clear the remaining generated leveling state");
		ByteArrayOutputStream saved = new ByteArrayOutputStream();
		MpoFileImporter importer = new MpoFileImporter();
		assertTrue(importer.saveProject(project, saved), "MPO save rejected the cleared leveling project");
		Project reloaded = importer.loadProject(new ByteArrayInputStream(saved.toByteArray()));
		assertEquals(0L, com.microproject.datatype.Duration.millis(taskNamed(reloaded, selected.getName()).getLevelingDelay()));
		assertEquals(1, taskNamed(reloaded, selected.getName()).getManualTaskSplitIntervals().size(),
			"MPO reload must preserve the manually owned split after both clear scopes");
		assertTrue(taskNamed(reloaded, selected.getName()).getLevelingTaskSplitIntervals().isEmpty());
		assertTrue(taskNamed(reloaded, other.getName()).getLevelingTaskSplitIntervals().isEmpty());
		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Z);
		GuiAcceptanceSupport.await(() -> com.microproject.datatype.Duration.millis(other.getLevelingDelay()) == 172_800_000L
				&& other.getLevelingTaskSplitIntervals().size() == 1,
			"one Ctrl+Z did not undo Clear Leveling (All)");
		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Y);
		GuiAcceptanceSupport.await(() -> com.microproject.datatype.Duration.millis(other.getLevelingDelay()) == 0L,
			"one Ctrl+Y did not redo Clear Leveling (All)");
	}

	private static JOptionPane findShowingOptionPane() {
		for (Window candidate : Window.getWindows()) {
			if (!candidate.isShowing()) continue;
			JOptionPane[] panes = UiComponentWalker.flatten(candidate).stream()
				.filter(JOptionPane.class::isInstance).map(JOptionPane.class::cast).toArray(JOptionPane[]::new);
			if (panes.length > 0) return panes[0];
		}
		return null;
	}

	@Test
	void physicalTaskInspectorFollowsSelectionAndNavigatesOnlyVisiblePredecessors() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		NormalTask successor = createTask();
		Project project = successor.getOwningProject();
		NormalTask predecessor = project.createScriptedTask();
		predecessor.setName("Inspector predecessor");
		successor.setName("Inspector successor");
		DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.FS, 0L, this);
		project.recalculate();
		showProject(project);
		SwingUtilities.invokeAndWait(() -> window.setSize(1320, 760));
		GuiAcceptanceSupport.await(() -> window.isShowing() && manager.getCurrentFrame() != null
				&& manager.getCurrentFrame().getActiveSpreadSheet() != null,
			"the task Inspector project did not become visible");
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		activateWindow(robot, window);
		SpreadSheet sheet = manager.getCurrentFrame().getActiveSpreadSheet();
		clickUntilSelected(robot, window, sheet, rowForTask(sheet, successor), nameColumn(sheet));
		clickAndAwaitSelected(robot, findShowingButtonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
			.getString("TaskRibbonTask.title")), "Robot click did not select the Task ribbon tab");
		click(robot, findVisibleOrOverflowButton(robot, "RibbonTaskInspector"));
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame().taskInspectorVisible()
				&& manager.getCurrentFrame().getSelectedImpls(false).contains(successor),
			"Inspect Task did not open its pane for the selected task");
		JSplitPane inspectorSplit = manager.getCurrentFrame().getTaskInspectorSplit();
		assertTrue(inspectorSplit.getDividerLocation() > 0, "the Inspector must appear to the left of project information");
		assertTrue(inspectorSplit.getLeftComponent().isVisible(), "the Inspector pane must remain visible");
		inspectorSplit.setDividerLocation(300);
		long predecessorStart = predecessor.getStart();
		long successorStart = successor.getStart();
		boolean dirtyBefore = project.needsSaving();
		boolean undoBefore = project.getUndoController().canUndo();
		AbstractButton predecessorLink = inspectorPredecessorButton("Inspector predecessor");
		assertTrue(inspectorLinkFitsNaturalHeight(predecessorLink),
			"Inspector sections and predecessor controls must keep natural heights in the narrow pane: button="
				+ inspectorButtonBounds(predecessorLink) + "; viewport="
				+ inspectorViewportBounds(SwingUtilities.getAncestorOfClass(javax.swing.JScrollPane.class, predecessorLink) instanceof javax.swing.JScrollPane scrollPane
					? scrollPane.getViewport() : null));
		java.util.concurrent.atomic.AtomicBoolean predecessorRouteInvoked = new java.util.concurrent.atomic.AtomicBoolean();
		predecessorLink.addActionListener(event -> predecessorRouteInvoked.set(true));
		click(robot, predecessorLink);
		GuiAcceptanceSupport.await(predecessorRouteInvoked::get,
			"the physical predecessor-link click did not invoke its route; label=" + predecessorLink.getText());
		try {
			GuiAcceptanceSupport.await(() -> manager.getCurrentFrame().getSelectedImpls(false).contains(predecessor),
				"clicking the visible predecessor did not select its rendered spreadsheet row");
		} catch (AssertionError failure) {
			Node predecessorNode = DocumentFrame.findTaskNode(project, predecessor);
			throw new AssertionError(failure.getMessage() + "; routeInvoked=" + predecessorRouteInvoked.get()
				+ "; " + inspectorNavigationDiagnostics(sheet, predecessor, predecessorNode), failure);
		}
		GuiAcceptanceSupport.await(() -> taskInspectorTitleContains("Inspector predecessor"),
			"the Inspector did not follow the physical predecessor selection");
		assertEquals(predecessorStart, predecessor.getStart());
		assertEquals(successorStart, successor.getStart());
		assertEquals(dirtyBefore, project.needsSaving(), "inspection and navigation must not dirty the project");
		assertEquals(undoBefore, project.getUndoController().canUndo(), "inspection and navigation must not add Undo history");

		clickUntilSelected(robot, window, sheet, rowForTask(sheet, successor), nameColumn(sheet));
		GuiAcceptanceSupport.await(() -> taskInspectorTitleContains("Inspector successor"),
			"the Inspector did not update when selection returned to the successor");
		int[] hiddenCount = new int[1];
		SwingUtilities.invokeAndWait(() -> hiddenCount[0] = TaskVisibilityService.hideTasks(
			project, List.of(predecessor), project.getUndoController()));
		assertEquals(1, hiddenCount[0],
			"the fixture must hide exactly the predecessor row before testing filtered navigation");
		GuiAcceptanceSupport.await(() -> sheet.taskNodeToViewRow(DocumentFrame.findTaskNode(project, predecessor)) < 0,
			"the hidden predecessor must be absent from visible view coordinates");
		int selectionBeforeHiddenClick = sheet.getSelectedRow();
		boolean dirtyBeforeHiddenClick = project.needsSaving();
		boolean undoBeforeHiddenClick = project.getUndoController().canUndo();
		AbstractButton hiddenPredecessorLink = inspectorPredecessorButton("Inspector predecessor");
		java.util.concurrent.atomic.AtomicBoolean hiddenRouteInvoked = new java.util.concurrent.atomic.AtomicBoolean();
		hiddenPredecessorLink.addActionListener(event -> hiddenRouteInvoked.set(true));
		click(robot, hiddenPredecessorLink);
		GuiAcceptanceSupport.await(hiddenRouteInvoked::get,
			"the hidden predecessor click did not invoke its route; button=" + inspectorButtonBounds(hiddenPredecessorLink)
				+ "; viewport=" + inspectorViewportBounds(
					(javax.swing.JViewport) SwingUtilities.getAncestorOfClass(javax.swing.JViewport.class, hiddenPredecessorLink)));
		try {
			GuiAcceptanceSupport.await(() -> taskInspectorTitleEquals(Messages.getString("TaskInspector.PredecessorHidden")),
				"the Inspector must explain when a predecessor is hidden by the current projection");
		} catch (AssertionError failure) {
			throw new AssertionError(failure.getMessage() + "; hiddenRouteInvoked=" + hiddenRouteInvoked.get()
				+ "; button=" + inspectorButtonBounds(hiddenPredecessorLink)
				+ "; " + inspectorNavigationDiagnostics(sheet, predecessor, DocumentFrame.findTaskNode(project, predecessor)), failure);
		}
		assertEquals(selectionBeforeHiddenClick, sheet.getSelectedRow(), "hidden predecessor navigation must preserve selection");
		assertEquals(dirtyBeforeHiddenClick, project.needsSaving(), "hidden navigation must not mutate the project");
		assertEquals(undoBeforeHiddenClick, project.getUndoController().canUndo(), "hidden navigation must not change Undo history");
	}

	private boolean taskInspectorTitleContains(String text) {
		return taskInspectorTitle().contains(text);
	}

	private boolean taskInspectorTitleEquals(String text) {
		return text.equals(taskInspectorTitle());
	}

	private String taskInspectorTitle() {
		String[] title = new String[1];
		try {
			SwingUtilities.invokeAndWait(() -> title[0] = manager.getCurrentFrame().getTaskInspectorPanel().getTitleText());
		} catch (Exception failure) {
			throw new IllegalStateException("could not read Inspector title on the EDT", failure);
		}
		return title[0];
	}

	private String inspectorNavigationDiagnostics(SpreadSheet sheet, Task expected, Node expectedNode) {
		String[] result = new String[1];
		try {
			SwingUtilities.invokeAndWait(() -> {
				int[] modelRows = sheet.nodesToRows(List.of(expectedNode));
				int modelRow = modelRows.length == 0 ? -1 : modelRows[0];
				int viewRow = sheet.taskNodeToViewRow(expectedNode);
				int selectedRow = sheet.getSelectedRow();
				int selectedModelRow = selectedRow < 0 ? -1 : sheet.convertRowIndexToModel(selectedRow);
				String rowSelection = java.util.Arrays.stream(sheet.getSelectedRows())
					.mapToObj(row -> row + "->" + sheet.convertRowIndexToModel(row)).toList().toString();
				String selectedNodes = sheet.getSelectedNodes().stream().map(node -> {
					Object implementation = node.getImpl();
					return (implementation instanceof Task task ? task.getName() : String.valueOf(implementation))
						+ "[node=" + System.identityHashCode(node) + ",impl="
						+ System.identityHashCode(implementation) + "]";
				}).toList().toString();
				String frameSelection = manager.getCurrentFrame().getSelectedImpls(false).stream()
					.map(value -> value instanceof Task task ? task.getName() + "[impl="
						+ System.identityHashCode(value) + "]" : String.valueOf(value)).toList().toString();
				Object cached = ((SpreadSheetModel) sheet.getModel()).getCache().getGraphicNode(expectedNode);
				result[0] = "expected=" + expected.getName() + "[node=" + System.identityHashCode(expectedNode)
					+ ",impl=" + System.identityHashCode(expected) + ",graphic="
					+ System.identityHashCode(cached) + ",modelRow=" + modelRow + ",viewRow=" + viewRow
					+ "]; tableSelectedRows(view->model)=" + rowSelection + ",selectedRow=" + selectedRow
					+ ",selectedModelRow=" + selectedModelRow + ",selectedNodes=" + selectedNodes
					+ ",frameSelectedImpls=" + frameSelection + ",inspectorTitle="
					+ manager.getCurrentFrame().getTaskInspectorPanel().getTitleText();
			});
		} catch (Exception failure) {
			return "inspector navigation diagnostic collection failed: " + failure;
		}
		return result[0];
	}

	private AbstractButton inspectorPredecessorButton(String predecessorName) throws Exception {
		AbstractButton[] found = new AbstractButton[1];
		javax.swing.JViewport[] foundViewport = new javax.swing.JViewport[1];
		SwingUtilities.invokeAndWait(() -> {
			TaskInspectorPanel panel = manager.getCurrentFrame().getTaskInspectorPanel();
			findInspectorButton(panel, predecessorName, found);
			if (found[0] != null) {
				for (java.awt.Container parent = found[0].getParent(); parent != null; parent = parent.getParent()) {
					if (parent instanceof javax.swing.JScrollPane scrollPane) {
						javax.swing.JViewport viewport = scrollPane.getViewport();
						foundViewport[0] = viewport;
						scrollInspectorButtonToCenter(found[0], viewport);
						break;
					}
				}
			}
		});
		if (found[0] == null)
			throw new AssertionError("Inspector predecessor control not found for: " + predecessorName);
		GuiAcceptanceSupport.await(() -> inspectorButtonCenterIsVisible(found[0], foundViewport[0], predecessorName),
			"Inspector predecessor control did not reach the visible scroll viewport: " + predecessorName
				+ "; button=" + inspectorButtonBounds(found[0]) + "; viewport=" + inspectorViewportBounds(foundViewport[0]));
		return found[0];
	}

	private static void scrollInspectorButtonToCenter(AbstractButton button, javax.swing.JViewport viewport) {
		javax.swing.JComponent view = (javax.swing.JComponent) viewport.getView();
		view.validate();
		viewport.validate();
		java.awt.Rectangle bounds = javax.swing.SwingUtilities.convertRectangle(button.getParent(), button.getBounds(), view);
		java.awt.Dimension extent = viewport.getExtentSize();
		int x = Math.max(0, bounds.x + bounds.width / 2 - extent.width / 2);
		int y = Math.max(0, bounds.y + bounds.height / 2 - extent.height / 2);
		int maxX = Math.max(0, view.getWidth() - extent.width);
		int maxY = Math.max(0, view.getHeight() - extent.height);
		viewport.setViewPosition(new java.awt.Point(Math.min(maxX, x), Math.min(maxY, y)));
	}

	private boolean inspectorButtonCenterIsVisible(AbstractButton button, javax.swing.JViewport viewport,
			String predecessorName) {
		boolean[] visible = new boolean[1];
		try {
			SwingUtilities.invokeAndWait(() -> {
				TaskInspectorPanel current = manager.getCurrentFrame().getTaskInspectorPanel();
				if (button == null || viewport == null || !button.isShowing() || !viewport.isShowing()
						|| !javax.swing.SwingUtilities.isDescendingFrom(button, current)) return;
				scrollInspectorButtonToCenter(button, viewport);
				java.awt.Rectangle buttonBounds = new java.awt.Rectangle(button.getLocationOnScreen(), button.getSize());
				java.awt.Rectangle viewportBounds = new java.awt.Rectangle(viewport.getLocationOnScreen(), viewport.getExtentSize());
				visible[0] = viewportBounds.contains(buttonBounds.getCenterX(), buttonBounds.getCenterY());
			});
		} catch (Exception failure) {
			throw new IllegalStateException("could not prepare Inspector predecessor target " + predecessorName, failure);
		}
		return visible[0];
	}

	private static String inspectorButtonBounds(AbstractButton button) {
		return button == null || !button.isShowing() ? "not-showing"
			: new java.awt.Rectangle(button.getLocationOnScreen(), button.getSize()).toString();
	}

	private static String inspectorViewportBounds(javax.swing.JViewport viewport) {
		return viewport == null || !viewport.isShowing() ? "not-showing"
			: new java.awt.Rectangle(viewport.getLocationOnScreen(), viewport.getExtentSize()).toString()
				+ " viewPosition=" + viewport.getViewPosition() + " viewSize=" + viewport.getViewSize();
	}

	private static boolean inspectorLinkFitsNaturalHeight(AbstractButton button) {
		boolean[] fits = new boolean[1];
		try {
			SwingUtilities.invokeAndWait(() -> {
				if (button == null || !button.isShowing()) return;
				javax.swing.JScrollPane scroll = (javax.swing.JScrollPane) SwingUtilities.getAncestorOfClass(
					javax.swing.JScrollPane.class, button);
				fits[0] = scroll != null && button.getHeight() <= 80
					&& button.getHeight() < scroll.getViewport().getExtentSize().height;
			});
		} catch (Exception failure) {
			throw new IllegalStateException("could not inspect Inspector control dimensions", failure);
		}
		return fits[0];
	}

	private static void findInspectorButton(java.awt.Container parent, String expectedPrefix, AbstractButton[] result) {
		for (java.awt.Component component : parent.getComponents()) {
			if (component instanceof AbstractButton button && button.getText() != null
				&& button.getText().startsWith(expectedPrefix + " — ")) {
				result[0] = button;
				return;
			}
			if (component instanceof java.awt.Container nested) {
				findInspectorButton(nested, expectedPrefix, result);
				if (result[0] != null) return;
			}
		}
	}

	@Test
	void physicalTaskSortSupportsAscendingStartAndDescendingFinishForViewAndOutline() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("task-date-sort-acceptance", undo), undo);
		project.initialize(false, false);
		NormalTask later = project.createScriptedTask();
		later.setName("Date sort later");
		NormalTask earlier = project.createScriptedTask();
		earlier.setName("Date sort earlier");
		NormalTask tiedEarlier = project.createScriptedTask();
		tiedEarlier.setName("Date sort tied earlier");
		long day = CalendarOption.getInstance().getMillisPerDay();
		long start = project.getStart();
		later.setDuration(day / 2);
		earlier.setDuration(4 * day);
		tiedEarlier.setDuration(day / 2);
		later.setStart(start + 2 * day);
		earlier.setStart(start + day);
		tiedEarlier.setStart(start + day);
		project.recalculate();
		List<String> originalOutline = taskOrder(project);
		assertEquals(List.of("Date sort later", "Date sort earlier", "Date sort tied earlier"), originalOutline,
			"fixture must begin in outline order rather than date order");
		assertTrue(earlier.getStart() < later.getStart(), "fixture must retain distinct scheduled start dates after recalculation");
		assertEquals(earlier.getStart(), tiedEarlier.getStart(), "fixture must retain an equal-date tie after recalculation");
		long[] originalStarts = { later.getStart(), earlier.getStart(), tiedEarlier.getStart() };
		long[] originalFinishes = { later.getEnd(), earlier.getEnd(), tiedEarlier.getEnd() };
		assertTrue(earlier.getEnd() > later.getEnd() && later.getEnd() > tiedEarlier.getEnd(),
			"fixture must distinguish Finish Date descending from Start Date ordering");

		showProject(project);
		SwingUtilities.invokeAndWait(() -> window.setSize(1120, 700));
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame() != null
				&& manager.getCurrentFrame().getActiveSpreadSheet() != null,
			"task date-sort project did not become visible");
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		activateWindow(robot, window);
		SpreadSheet sheet = manager.getCurrentFrame().getActiveSpreadSheet();
		GanttView ganttView = manager.getCurrentFrame().getGanttView();
		taskSortViewToRestore = ViewConfiguration.getView(manager.getCurrentFrame().getTopViewId());
		previousTaskSortSpec = taskSortViewToRestore.getTransform().getTaskSortSpec();
		assertSame(ganttView.getSpreadSheet(), sheet);
		assertSame(sheet.getCache(), ganttView.getCache(), "task table and Gantt must share the live projection");
		assertEquals(originalOutline, visibleTaskOrder(sheet), "task table must initially follow outline order");

		AbstractButton viewTab = findShowingButtonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
			.getString("ViewRibbonTask.title"));
		clickAndAwaitSelected(robot, viewTab, "Robot click did not select the View ribbon tab");
		click(robot, findShowingButtonByCommand("RibbonChooseSort"));
		JComboBox<?> sorterChoice = findShowingTransformCombo("Sorter.StartDate");
		int dateSorterIndex = -1;
		for (int index = 0; index < sorterChoice.getItemCount(); index++) {
			Object item = sorterChoice.getItemAt(index);
			if (item instanceof CommonTransformFactory factory && "Sorter.StartDate".equals(factory.getId())) {
				dateSorterIndex = index;
				break;
			}
		}
		assertTrue(dateSorterIndex >= 0, "the shared task-view sorter list must expose Start Date");
		GuiAcceptanceSupport.selectComboBoxItem(robot, sorterChoice, dateSorterIndex);
		assertTrue(sorterChoice.getSelectedItem() instanceof CommonTransformFactory selectedSorter
				&& "Sorter.StartDate".equals(selectedSorter.getId()),
			"Robot keyboard selection must choose the Start Date sorter before dialog confirmation");
		Dialog chooser = java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow() instanceof Dialog dialog
			? dialog : null;
		assertNotNull(chooser, "the generic task Sort dialog must remain open after choosing a sorter");
		JComboBox<?> startDirection = findTaskSortDirection(chooser);
		assertEquals(0, startDirection.getSelectedIndex(), "Start Date begins in ascending order");
		click(robot, findShowingButtonByText(chooser, javax.swing.UIManager.getString("OptionPane.okButtonText")));
		GuiAcceptanceSupport.await(() -> visibleTaskOrder(sheet).equals(
			List.of("Date sort earlier", "Date sort tied earlier", "Date sort later")),
			"the physical View > Sort route did not apply the Start Date ordering stably; rows="
				+ visibleTaskOrder(sheet) + ", selectedSorter="
				+ ViewConfiguration.getView(manager.getCurrentFrame().getTopViewId()).getTransform().getUserSorterId());
		GuiAcceptanceSupport.await(() -> "Sorter.StartDate".equals(
			ViewConfiguration.getView(manager.getCurrentFrame().getTopViewId()).getTransform().getUserSorterId()),
			"the active task-view configuration did not retain Start Date as its selected sorter");
		assertEquals(originalOutline, taskOrder(project), "date sorting must remain view-only");
		assertArrayEquals(originalStarts, new long[] { later.getStart(), earlier.getStart(), tiedEarlier.getStart() },
			"date sorting must not change task schedules");
		assertArrayEquals(originalFinishes, new long[] { later.getEnd(), earlier.getEnd(), tiedEarlier.getEnd() },
			"date sorting must not change task finishes");

		// Microsoft Project's Application.Sort documents Ascending1=False for a
		// selected field (https://learn.microsoft.com/en-us/office/vba/api/project.application.sort).
		// Exercise the documented direction through the same task-view route.
		click(robot, findShowingButtonByCommand("RibbonChooseSort"));
		JComboBox<?> finishSorterChoice = findShowingTransformCombo("Sorter.StartDate");
		int finishSorterIndex = -1;
		for (int index = 0; index < finishSorterChoice.getItemCount(); index++) {
			Object item = finishSorterChoice.getItemAt(index);
			if (item instanceof CommonTransformFactory factory && "Sorter.FinishDate".equals(factory.getId())) {
				finishSorterIndex = index;
				break;
			}
		}
		assertTrue(finishSorterIndex >= 0, "the shared task-view sorter list must expose Finish Date");
		GuiAcceptanceSupport.selectComboBoxItem(robot, finishSorterChoice, finishSorterIndex);
		Dialog finishChooser = (Dialog) java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
		JComboBox<?> finishDirection = findTaskSortDirection(finishChooser);
		GuiAcceptanceSupport.selectComboBoxItem(robot, finishDirection, 1);
		assertEquals(1, finishDirection.getSelectedIndex(), "Finish Date must be explicitly set to descending");
		click(robot, findShowingButtonByText(finishChooser, javax.swing.UIManager.getString("OptionPane.okButtonText")));
		List<String> descendingFinishOrder = List.of("Date sort earlier", "Date sort later", "Date sort tied earlier");
		GuiAcceptanceSupport.await(() -> visibleTaskOrder(sheet).equals(descendingFinishOrder),
			"descending Finish Date must order by task finish, independently of Start Date; rows=" + visibleTaskOrder(sheet));
		assertEquals("Sorter.FinishDate", taskSortViewToRestore.getTransform().getUserSorterId());
		assertFalse(taskSortViewToRestore.getTransform().isUserSorterAscending());
		assertEquals(originalOutline, taskOrder(project), "descending view sort must not reorder the task model");
		assertArrayEquals(originalStarts, new long[] { later.getStart(), earlier.getStart(), tiedEarlier.getStart() },
			"descending view sort must not change task starts");
		assertArrayEquals(originalFinishes, new long[] { later.getEnd(), earlier.getEnd(), tiedEarlier.getEnd() },
			"descending view sort must not change task finishes");

		// Microsoft Project's Sort dialog permits a secondary field with its own direction.
		// Keep Outline off is view-only here; this product contract does not rewrite WBS.
		click(robot, findShowingButtonByCommand("RibbonChooseSort"));
		Dialog multiKeyChooser = (Dialog) java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
		JComboBox<?> multiPrimary = findShowingTransformCombo("Sorter.FinishDate");
		int startIndex = -1;
		for (int index = 0; index < multiPrimary.getItemCount(); index++) {
			if (multiPrimary.getItemAt(index) instanceof CommonTransformFactory factory
					&& "Sorter.StartDate".equals(factory.getId())) startIndex = index;
		}
		GuiAcceptanceSupport.selectComboBoxItem(robot, multiPrimary, startIndex);
		JComboBox<?> multiPrimaryDirection = findTaskSortDirection(multiKeyChooser);
		GuiAcceptanceSupport.selectComboBoxItem(robot, multiPrimaryDirection, 0);
		assertEquals(0, multiPrimaryDirection.getSelectedIndex(),
			"the new primary Start Date key must be explicitly ascending after a descending prior sort");
		JComboBox<?> secondField = UiComponentWalker.flatten(multiKeyChooser).stream()
			.filter(JComboBox.class::isInstance).map(JComboBox.class::cast)
			.filter(field -> "microproject.taskSort.secondary".equals(field.getName())).findFirst().orElseThrow();
		int nameIndex = -1;
		for (int index = 0; index < secondField.getItemCount(); index++) {
			if (secondField.getItemAt(index) instanceof CommonTransformFactory factory
					&& "Sorter.Name".equals(factory.getId())) nameIndex = index;
		}
		assertTrue(nameIndex >= 0, "the secondary key must use the shared task sorter catalog");
		GuiAcceptanceSupport.selectComboBoxItem(robot, secondField, nameIndex);
		assertTrue(secondField.getSelectedItem() instanceof CommonTransformFactory selectedName
				&& "Sorter.Name".equals(selectedName.getId()),
			"the physical chooser must select Name as its secondary key; selected="
				+ (secondField.getSelectedItem() instanceof CommonTransformFactory selected
					? selected.getId() : secondField.getSelectedItem())
				+ "; " + GuiAcceptanceSupport.comboState(secondField));
		JComboBox<?> secondDirection = UiComponentWalker.flatten(multiKeyChooser).stream()
			.filter(JComboBox.class::isInstance).map(JComboBox.class::cast)
			.filter(field -> "microproject.taskSort.secondaryDirection".equals(field.getName())).findFirst().orElseThrow();
		GuiAcceptanceSupport.selectComboBoxItem(robot, secondDirection, 1);
		assertEquals(1, secondDirection.getSelectedIndex(), "the secondary Name key must be explicitly descending");
		JCheckBox keepOutline = UiComponentWalker.flatten(multiKeyChooser).stream()
			.filter(JCheckBox.class::isInstance).map(JCheckBox.class::cast)
			.filter(check -> "microproject.taskSort.keepOutline".equals(check.getName())).findFirst().orElseThrow();
		click(robot, keepOutline);
		click(robot, findShowingButtonByText(multiKeyChooser, javax.swing.UIManager.getString("OptionPane.okButtonText")));
		assertEquals(List.of(new TaskSortSpec.Key("Sorter.StartDate", true), new TaskSortSpec.Key("Sorter.Name", false)),
			taskSortViewToRestore.getTransform().getTaskSortSpec().keys(),
			"confirming the dialog must atomically commit both selected keys and directions");
		GuiAcceptanceSupport.await(() -> visibleTaskOrder(sheet).equals(
				List.of("Date sort tied earlier", "Date sort earlier", "Date sort later")),
			"the physical sort route must apply the secondary descending Name key; rows=" + visibleTaskOrder(sheet));
		assertFalse(taskSortViewToRestore.getTransform().isKeepOutline());
		assertEquals(List.of(new TaskSortSpec.Key("Sorter.StartDate", true), new TaskSortSpec.Key("Sorter.Name", false)),
			taskSortViewToRestore.getTransform().getTaskSortSpec().keys());
		assertEquals(originalOutline, taskOrder(project), "flat view sorting must not mutate task outline/WBS order");
		for (NormalTask task : List.of(earlier, tiedEarlier, later)) {
			int sheetRow = rowForTask(sheet, task);
			GraphicNode graphicNode = (GraphicNode) sheet.getCache().getGraphicNode(
					((CommonSpreadSheetModel) sheet.getModel()).getNode(sheetRow).getNode());
			assertEquals(1, sheet.getCache().getLevel(graphicNode), "flat sort rows must have one displayed level");
			int ganttRow = ganttView.getGantt().getCache().getVisibleNodes().getProjectionIndex().rowForNode(graphicNode);
			assertEquals(sheetRow, ganttRow, "flat view sort must keep table and Gantt rows aligned");
		}
		for (NormalTask task : List.of(earlier, tiedEarlier, later)) {
			int sheetRow = rowForTask(sheet, task);
			var graphicNode = sheet.getCache().getGraphicNode(((CommonSpreadSheetModel) sheet.getModel()).getNode(sheetRow).getNode());
			int ganttRow = ganttView.getGantt().getCache().getVisibleNodes().getProjectionIndex().rowForNode((GraphicNode) graphicNode);
			assertEquals(sheetRow, ganttRow, "Gantt must paint the task at the same visible row as its table row");
		}
		List<Long> flatModeIds = List.of(later.getId(), earlier.getId(), tiedEarlier.getId());
		click(robot, findShowingButtonByCommand("RibbonChooseSort"));
		Dialog flatSortChooser = (Dialog) java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
		JCheckBox flatPermanent = UiComponentWalker.flatten(flatSortChooser).stream()
			.filter(JCheckBox.class::isInstance).map(JCheckBox.class::cast)
			.filter(check -> "microproject.taskSort.permanentOrder".equals(check.getName())).findFirst().orElseThrow();
		assertFalse(flatPermanent.isEnabled(), "Keep Outline off must disable permanent ordering and renumbering");
		assertFalse(flatPermanent.isSelected(), "a flat projection cannot retain a permanent-order selection");
		JComboBox<?> reopenedSecondary = UiComponentWalker.flatten(flatSortChooser).stream()
			.filter(JComboBox.class::isInstance).map(JComboBox.class::cast)
			.filter(field -> "microproject.taskSort.secondary".equals(field.getName())).findFirst().orElseThrow();
		assertEquals("Sorter.Name", ((CommonTransformFactory) reopenedSecondary.getSelectedItem()).getId(),
			"reopening the chooser must restore the complete secondary key");
		JComboBox<?> reopenedSecondaryDirection = UiComponentWalker.flatten(flatSortChooser).stream()
			.filter(JComboBox.class::isInstance).map(JComboBox.class::cast)
			.filter(field -> "microproject.taskSort.secondaryDirection".equals(field.getName())).findFirst().orElseThrow();
		assertEquals(1, reopenedSecondaryDirection.getSelectedIndex(),
			"reopening the chooser must restore the secondary key direction");
		JCheckBox reopenedKeepOutline = UiComponentWalker.flatten(flatSortChooser).stream()
			.filter(JCheckBox.class::isInstance).map(JCheckBox.class::cast)
			.filter(check -> "microproject.taskSort.keepOutline".equals(check.getName())).findFirst().orElseThrow();
		assertFalse(reopenedKeepOutline.isSelected(), "reopening the chooser must restore Keep Outline=false");
		clickDisabled(robot, flatPermanent);
		assertFalse(flatPermanent.isSelected(), "a disabled permanent-order option cannot be activated physically");
		click(robot, findShowingButtonByText(flatSortChooser, javax.swing.UIManager.getString("OptionPane.cancelButtonText")));
		GuiAcceptanceSupport.await(() -> !flatSortChooser.isShowing(), "flat sort review dialog did not close");
		assertEquals(originalOutline, taskOrder(project), "flat mode must never mutate WBS order");
		assertEquals(flatModeIds, List.of(later.getId(), earlier.getId(), tiedEarlier.getId()),
			"flat mode must never permanently renumber task IDs");
		click(robot, cellOnScreen(sheet, rowForTask(sheet, tiedEarlier), nameColumn(sheet)));
		GuiAcceptanceSupport.await(() -> ganttView.getGantt().getHighlightedRows()
				.equals(java.util.Set.of(rowForTask(sheet, tiedEarlier))),
			"the rendered Gantt highlight did not follow the sorted task's table row");
		Point selectedRowHeaderPoint = rowHeaderTargetOnScreen(sheet.getRowHeader(), rowForTask(sheet, tiedEarlier), false).screenPoint();
		click(robot, new Rectangle(selectedRowHeaderPoint.x, selectedRowHeaderPoint.y, 1, 1));
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame().getSelectedImpls(false).contains(tiedEarlier),
			"the task row-header selection precondition was not established before permanent sorting");

		List<Long> originalIds = List.of(later.getId(), earlier.getId(), tiedEarlier.getId());
		boolean undoBeforeCancel = project.getUndoController().canUndo();
		boolean dirtyBeforeCancel = project.needsSaving();
		click(robot, findShowingButtonByCommand("RibbonChooseSort"));
		Dialog permanentChooser = (Dialog) java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
		JCheckBox permanentCheck = UiComponentWalker.flatten(permanentChooser).stream()
			.filter(JCheckBox.class::isInstance).map(JCheckBox.class::cast)
			.filter(check -> check.getText().equals(ResourceBundle.getBundle("com.microproject.menu.menu")
				.getString("Sort.PermanentTaskOrder"))).findFirst().orElseThrow();
		JCheckBox keepOutlineForCancel = UiComponentWalker.flatten(permanentChooser).stream()
			.filter(JCheckBox.class::isInstance).map(JCheckBox.class::cast)
			.filter(check -> "microproject.taskSort.keepOutline".equals(check.getName())).findFirst().orElseThrow();
		click(robot, keepOutlineForCancel);
		assertTrue(permanentCheck.isEnabled(), "permanent ordering is available after restoring Keep Outline");
		click(robot, permanentCheck);
		click(robot, findShowingButtonByText(permanentChooser, javax.swing.UIManager.getString("OptionPane.okButtonText")));
		GuiAcceptanceSupport.await(() -> java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow() instanceof Dialog dialog
				&& dialog != permanentChooser, "permanent task sort did not ask for a second confirmation");
		Dialog confirmation = (Dialog) java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
		click(robot, findShowingButtonByText(confirmation, javax.swing.UIManager.getString("OptionPane.noButtonText")));
		GuiAcceptanceSupport.await(() -> !confirmation.isShowing(), "No did not cancel permanent task sorting");
		assertEquals(originalOutline, taskOrder(project), "confirmation cancel must leave task order unchanged");
		assertEquals(originalIds, List.of(later.getId(), earlier.getId(), tiedEarlier.getId()),
			"confirmation cancel must leave task IDs unchanged");
		assertEquals(undoBeforeCancel, project.getUndoController().canUndo(), "confirmation cancel must not add an Undo edit");
		assertEquals(dirtyBeforeCancel, project.needsSaving(), "confirmation cancel must not dirty the project");

		click(robot, findShowingButtonByCommand("RibbonChooseSort"));
		JComboBox<?> permanentSorterAgain = findShowingTransformCombo("Sorter.FinishDate");
		int finishDateIndex = -1;
		for (int index = 0; index < permanentSorterAgain.getItemCount(); index++) {
			Object item = permanentSorterAgain.getItemAt(index);
			if (item instanceof CommonTransformFactory factory && "Sorter.FinishDate".equals(factory.getId())) {
				finishDateIndex = index;
				break;
			}
		}
		assertTrue(finishDateIndex >= 0);
		GuiAcceptanceSupport.selectComboBoxItem(robot, permanentSorterAgain, finishDateIndex);
		Dialog permanentChooserAgain = (Dialog) java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
		GuiAcceptanceSupport.selectComboBoxItem(robot, findTaskSortDirection(permanentChooserAgain), 1);
		assertEquals(1, findTaskSortDirection(permanentChooserAgain).getSelectedIndex(),
			"the permanent sort must use the explicitly selected descending direction");
		JCheckBox permanentCheckAgain = UiComponentWalker.flatten(permanentChooserAgain).stream()
			.filter(JCheckBox.class::isInstance).map(JCheckBox.class::cast)
			.filter(check -> check.getText().equals(ResourceBundle.getBundle("com.microproject.menu.menu")
				.getString("Sort.PermanentTaskOrder"))).findFirst().orElseThrow();
		JCheckBox keepOutlineAgain = UiComponentWalker.flatten(permanentChooserAgain).stream()
			.filter(JCheckBox.class::isInstance).map(JCheckBox.class::cast)
			.filter(check -> "microproject.taskSort.keepOutline".equals(check.getName())).findFirst().orElseThrow();
		click(robot, keepOutlineAgain);
		assertTrue(permanentCheckAgain.isEnabled(), "permanent ordering is enabled after restoring Keep Outline");
		click(robot, permanentCheckAgain);
		click(robot, findShowingButtonByText(permanentChooserAgain, javax.swing.UIManager.getString("OptionPane.okButtonText")));
		GuiAcceptanceSupport.await(() -> java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow() instanceof Dialog dialog
				&& dialog != permanentChooserAgain, "confirmed permanent task sort did not ask before mutation");
		Dialog confirmationAgain = (Dialog) java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
		click(robot, findShowingButtonByText(confirmationAgain,
				javax.swing.UIManager.getString("OptionPane.yesButtonText")));
		GuiAcceptanceSupport.await(() -> taskOrder(project).equals(descendingFinishOrder),
			"confirmed permanent Finish Date descending sort did not change outline order");
		assertTrue(manager.getCurrentFrame().getSelectedImpls(false).contains(tiedEarlier),
			"permanent sorting must preserve selected task identity");
		assertTrue(earlier.getId() < later.getId() && later.getId() < tiedEarlier.getId(),
			"permanent descending task sort must renumber IDs in outline order");
		assertEquals(ViewTransformer.SORTER_NONE_ID,
			ViewConfiguration.getView(manager.getCurrentFrame().getTopViewId()).getTransform().getUserSorterId(),
			"permanent ordering must clear the view-only sorter after applying it to the model");
		for (NormalTask task : List.of(earlier, tiedEarlier, later)) {
			int sheetRow = rowForTask(sheet, task);
			var graphicNode = sheet.getCache().getGraphicNode(((CommonSpreadSheetModel) sheet.getModel()).getNode(sheetRow).getNode());
			int ganttRow = ganttView.getGantt().getCache().getVisibleNodes().getProjectionIndex().rowForNode((GraphicNode) graphicNode);
			assertEquals(sheetRow, ganttRow, "Gantt must paint permanently reordered tasks at their table rows");
		}
		ByteArrayOutputStream permanentMpo = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(project, permanentMpo), "MPO save rejected the permanent task order");
		Project permanentReload = new MpoFileImporter().loadProject(new ByteArrayInputStream(permanentMpo.toByteArray()));
		assertEquals(descendingFinishOrder,
			taskOrder(permanentReload), "MPO reload must preserve permanent task ordering");
		assertEquals(List.of(earlier.getId(), later.getId(), tiedEarlier.getId()),
			List.of(taskNamed(permanentReload, "Date sort earlier").getId(),
				taskNamed(permanentReload, "Date sort later").getId(),
				taskNamed(permanentReload, "Date sort tied earlier").getId()),
			"MPO reload must preserve renumbered task IDs");
		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Z);
		GuiAcceptanceSupport.await(() -> taskOrder(project).equals(originalOutline),
			"one Undo must restore the previous task order");
		assertEquals(originalIds, List.of(later.getId(), earlier.getId(), tiedEarlier.getId()),
			"one Undo must restore the previous task IDs");
		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Y);
		GuiAcceptanceSupport.await(() -> taskOrder(project).equals(descendingFinishOrder),
			"one Redo must restore the permanent task order");
	}

	@Test
	void levelAllUsesEveryProjectResourceWithoutChangingTaskSelectionAndSupportsUndoRedoAndMpo() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("ribbon-level-all", undo);
		pool.setLocal(true);
		Project project = Project.createProject(pool, undo);
		project.initialize(false, false);
		NormalTask anchorOne = project.createScriptedTask();
		anchorOne.setName("Level All resource one anchor");
		NormalTask targetOne = project.createScriptedTask();
		targetOne.setName("Level All resource one target");
		NormalTask anchorTwo = project.createScriptedTask();
		anchorTwo.setName("Level All resource two anchor");
		NormalTask targetTwo = project.createScriptedTask();
		targetTwo.setName("Level All resource two target");
		Resource resourceOne = pool.newResourceInstance();
		resourceOne.setName("Level All resource one");
		Resource resourceTwo = pool.newResourceInstance();
		resourceTwo.setName("Level All resource two");
		AssignmentService.getInstance().newAssignment(anchorOne, resourceOne, 1.0, 0L, getClass());
		AssignmentService.getInstance().newAssignment(targetOne, resourceOne, 1.0, 0L, getClass());
		AssignmentService.getInstance().newAssignment(anchorTwo, resourceTwo, 1.0, 0L, getClass());
		AssignmentService.getInstance().newAssignment(targetTwo, resourceTwo, 1.0, 0L, getClass());
		project.recalculate();
		assertEquals(0L, levelingDelay(targetOne));
		assertEquals(0L, levelingDelay(targetTwo));
		showProject(project);
		SwingUtilities.invokeAndWait(() -> window.setSize(1600, 700));
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame() != null
				&& manager.getCurrentFrame().getActiveSpreadSheet() != null,
			"resource leveling task sheet did not become visible");
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		SpreadSheet sheet = manager.getCurrentFrame().getActiveSpreadSheet();
		click(robot, cellOnScreen(sheet, rowForTask(sheet, anchorOne), nameColumn(sheet)));
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame().getSelectedImpls(false).contains(anchorOne),
			"physical task click did not select the intended Level All anchor");
		AbstractButton resourceTab = findShowingButtonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
				.getString("ResourceRibbonTask.title"));
		clickAndAwaitSelected(robot, resourceTab, "Robot click did not select the Resource ribbon tab");
		AbstractButton levelAll = findShowingButtonByCommand("RibbonLevelAll");
		GuiAcceptanceSupport.await(levelAll::isEnabled,
			"Level All must be enabled for a writable project with a selected task");
		RibbonCommandResult before = manager.getLastRibbonCommandResult();
		click(robot, levelAll);
		GuiAcceptanceSupport.await(() -> levelingDelay(targetOne) > 0L && levelingDelay(targetTwo) > 0L,
			"Resource > Level All did not level eligible tasks on both project resources");
		assertEquals(0L, levelingDelay(anchorOne), "Level All must retain the first resource's anchor");
		assertEquals(0L, levelingDelay(anchorTwo), "Level All must retain the second resource's anchor");
		GuiAcceptanceSupport.await(() -> manager.getLastRibbonCommandResult() != before
				&& manager.getLastRibbonCommandResult().status() == RibbonCommandResult.Status.CHANGED,
			"Level All did not publish its changed result");
		assertTrue(manager.getCurrentFrame().getSelectedImpls(false).contains(anchorOne),
			"Level All must preserve the user's task selection");
		assertFalse(levelAll.isSelected(), "Level All is a momentary command and must not latch");
		ByteArrayOutputStream saved = new ByteArrayOutputStream();
		MpoFileImporter importer = new MpoFileImporter();
		assertTrue(importer.saveProject(project, saved), "MPO save rejected the Level All project");
		Project reloaded = importer.loadProject(new ByteArrayInputStream(saved.toByteArray()));
		assertTrue(levelingDelay(taskNamed(reloaded, "Level All resource one target")) > 0L,
			"MPO reload lost Level All's first resource delay");
		assertTrue(levelingDelay(taskNamed(reloaded, "Level All resource two target")) > 0L,
			"MPO reload lost Level All's second resource delay");
		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Z);
		GuiAcceptanceSupport.await(() -> levelingDelay(targetOne) == 0L && levelingDelay(targetTwo) == 0L,
			"one Ctrl+Z did not undo Level All across every resource");
		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Y);
		GuiAcceptanceSupport.await(() -> levelingDelay(targetOne) > 0L && levelingDelay(targetTwo) > 0L,
			"one Ctrl+Y did not redo Level All across every resource");
	}

	@Test
	void nextOverallocationSelectsNextVisibleTaskAndWrapsWithoutMutatingSchedule() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("ribbon-next-overallocation", undo);
		pool.setLocal(true);
		Project project = Project.createProject(pool, undo);
		project.initialize(false, false);
		NormalTask first = project.createScriptedTask();
		first.setName("Next Overallocation first task");
		NormalTask second = project.createScriptedTask();
		second.setName("Next Overallocation second task");
		Resource resource = pool.newResourceInstance();
		resource.setName("Next Overallocation resource");
		AssignmentService.getInstance().newAssignment(first, resource, 1.0, 0L, getClass());
		AssignmentService.getInstance().newAssignment(second, resource, 1.0, 0L, getClass());
		project.recalculate();
		assertTrue(new com.microproject.pm.resource.TeamPlannerService().overallocatedTasks(project).containsAll(List.of(first, second)),
			"the acceptance fixture must contain two genuinely overallocated tasks");
		long firstStart = first.getStart();
		long firstFinish = first.getEnd();
		long secondStart = second.getStart();
		long secondFinish = second.getEnd();
		showProject(project);
		SwingUtilities.invokeAndWait(() -> window.setSize(1600, 700));
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame() != null
				&& manager.getCurrentFrame().getActiveSpreadSheet() != null,
			"Next Overallocation task sheet did not become visible");
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		SpreadSheet sheet = manager.getCurrentFrame().getActiveSpreadSheet();
		click(robot, cellOnScreen(sheet, rowForTask(sheet, first), nameColumn(sheet)));
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame().getSelectedImpls(false).contains(first),
			"physical task click did not select the first overallocated task");
		AbstractButton resourceTab = findShowingButtonByText(ResourceBundle.getBundle("com.microproject.menu.menu")
			.getString("ResourceRibbonTask.title"));
		clickAndAwaitSelected(robot, resourceTab, "Robot click did not select the Resource ribbon tab");
		AbstractButton next = findShowingButtonByCommand("RibbonNextOverallocation");
		GuiAcceptanceSupport.await(next::isEnabled, "Next Overallocation must be enabled in a task view");
		click(robot, next);
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame().getSelectedImpls(false).contains(second),
			"Next Overallocation did not select the next overallocated task in visible row order");
		assertFalse(next.isSelected(), "Next Overallocation is a momentary navigation command");
		assertTrue(sheet.getVisibleRect().contains(sheet.getCellRect(rowForTask(sheet, second), 0, true)),
			"the destination task row must be visible after navigation");
		click(robot, next);
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame().getSelectedImpls(false).contains(first),
			"Next Overallocation must wrap to the first visible candidate at the end of the list");
		assertEquals(firstStart, first.getStart());
		assertEquals(firstFinish, first.getEnd());
		assertEquals(secondStart, second.getStart());
		assertEquals(secondFinish, second.getEnd());
		SwingUtilities.invokeAndWait(() -> {
			((com.microproject.pm.resource.ResourceImpl) resource).setMaximumUnits(2.0);
			project.recalculate();
		});
		assertTrue(new com.microproject.pm.resource.TeamPlannerService().overallocatedTasks(project).isEmpty(),
			"the empty-result state must have no remaining overallocated task");
		click(robot, next);
		String noCandidateMessage = com.microproject.dialog.UsabilityStrings.text("resource.nextOverallocation.noneVisible");
		GuiAcceptanceSupport.await(() -> findDialogContainingText(noCandidateMessage) != null,
			"an empty result must explain that no visible overallocation remains");
		Dialog noCandidateDialog = findDialogContainingText(noCandidateMessage);
		click(robot, findShowingButtonByText(noCandidateDialog, Messages.getString("ButtonText.OK")));
		assertEquals(firstStart, first.getStart(), "navigation and empty-result feedback must not change schedules");
		assertEquals(secondStart, second.getStart(), "navigation and empty-result feedback must not change schedules");
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
		Robot robot = new com.microproject.testsupport.GuiRobot();
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
		SwingUtilities.invokeAndWait(() -> window.setAlwaysOnTop(false));
		assertFalse(window.isAlwaysOnTop(), "the primary test fixture must not cover the secondary document window");
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
		assertWindowFitsUsableScreen(window, "primary document window");
		assertWindowFitsUsableScreen(secondary, "secondary document window");
		DocumentFrame secondaryDocument = manager.getFrameForProject(second.getOwningProject());
		assertNotNull(secondaryDocument, "secondary project must remain registered before close");
		assertEquals(2, manager.getFrameManager().getAllFrames().size());
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(35);
		activateWindow(robot, secondary);
		GuiAcceptanceSupport.await(secondary::isFocused,
			"physical activation did not bring the secondary document window above the primary window");
		AbstractButton closeButton = WindowAcceptanceAssertions.findFlatLafTitleButton(secondary, "Close");
		clickWithWindowsDpi(robot, secondary, closeButton, boundsOnScreen(closeButton));
		GuiAcceptanceSupport.await(() -> !secondary.isShowing(),
			"Robot title-bar close button did not close the secondary window");
		GuiAcceptanceSupport.await(() -> manager.getFrameForProject(second.getOwningProject()) == null,
			"closed secondary window left its project registered in the frame manager");
		assertTrue(window.isShowing(), "closing the secondary document must leave the primary window visible");
		assertEquals(1, manager.getFrameManager().getAllFrames().size(),
			"closing a secondary project must preserve the primary document frame");
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

	private static NormalTask createTaskWithProgress(double percentComplete) {
		NormalTask task = createTask();
		task.setDuration(CalendarOption.getInstance().getMillisPerDay());
		task.setPercentComplete(percentComplete);
		assertEquals(percentComplete, task.getPercentComplete(), 0.00001d,
			"Progress fixture must use a nonzero-duration task so percent complete is task-level work progress");
		return task;
	}

	private static void assertProgressTaskFixtureRendered(SpreadSheet sheet, NormalTask task, double percentComplete)
			throws Exception {
		GuiAcceptanceSupport.await(() -> sheet.isShowing() && isTaskVisible(sheet, task),
			"Progress fixture task row did not render in the visible task sheet");
		int row = rowForTask(sheet, task);
		Rectangle nameCell = sheet.getCellRect(row, nameColumn(sheet), true);
		assertTrue(sheet.getVisibleRect().contains(nameCell),
			"Progress fixture task name cell must be inside the rendered spreadsheet viewport");
		assertEquals(percentComplete, task.getPercentComplete(), 0.00001d,
			"Progress fixture domain value must be established before the physical command");
	}

	private static NormalTask taskNamed(Project project, String name) {
		for (Task task : project.getTaskList()) {
			if (name.equals(task.getName()))
				return (NormalTask) task;
		}
		throw new AssertionError("Reloaded task is absent: " + name);
	}

	private static String queueIdentity(JobQueue queue) {
		return queue == null ? "null" : queue.getName() + "@" + Integer.toHexString(System.identityHashCode(queue));
	}

	private void showProject(Project project) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			for (Window candidate : Window.getWindows()) {
				if (candidate instanceof Dialog && candidate.isShowing())
					candidate.dispose();
				if (candidate instanceof MainRibbonFrame)
					candidate.dispose();
			}
			window = new MainRibbonFrame("microProject — Task Information ribbon acceptance", null);
			manager = new GraphicManager(window) {
				@Override protected boolean beforeUpdateTasksRoute(DocumentFrame frame) {
					if (failUpdateTasksRoute) {
						failUpdateTasksRoute = false;
						throw new IllegalStateException("injected Update Tasks route failure");
					}
					return super.beforeUpdateTasksRoute(frame);
				}
			};
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

	@Test
	// Microsoft documents one-shot formatting, double-click to continue, and Escape to stop; the exact
	// six task text attributes are a document-derived microProject rule because Project's support page
	// does not enumerate task-format fields. Gantt bar appearance remains a separate per-view style.
	// Source: https://support.microsoft.com/en-us/word/use-the-format-painter
	void formatPainterCopiesTaskTextStyleOnceOrContinuouslyWithUndoAndMpoReload() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("format-painter-acceptance", undo), undo);
		project.initialize(false, false);
		NormalTask source = project.createScriptedTask();
		source.setName("Format source");
		source.setFontFamily("Dialog");
		source.setFontSize(15);
		source.setFontColor(0x2457A6);
		source.setFontBold(true);
		source.setFontItalic(true);
		source.setFontStrikethrough(true);
		NormalTask firstTarget = project.createScriptedTask();
		firstTarget.setName("Format first target");
		NormalTask secondTarget = project.createScriptedTask();
		secondTarget.setName("Format second target");
		NormalTask thirdTarget = project.createScriptedTask();
		thirdTarget.setName("Format third target");
		NormalTask untouched = project.createScriptedTask();
		untouched.setName("Format untouched");
		project.recalculate();
		List<Long> originalIds = List.of(source.getId(), firstTarget.getId(), secondTarget.getId(),
			thirdTarget.getId(), untouched.getId());
		long[] originalDates = { firstTarget.getStart(), firstTarget.getEnd(), secondTarget.getStart(),
			secondTarget.getEnd(), thirdTarget.getStart(), thirdTarget.getEnd(), untouched.getStart(), untouched.getEnd() };
		undo.clear();

		showProject(project);
		SwingUtilities.invokeAndWait(() -> window.setSize(1120, 700));
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame() != null
				&& manager.getCurrentFrame().getActiveSpreadSheet() != null,
			"format-painter project did not become visible");
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		activateWindow(robot, window);
		SpreadSheet sheet = manager.getCurrentFrame().getActiveSpreadSheet();
		clickUntilSelected(robot, window, sheet, rowForTask(sheet, source), 0);
		AbstractButton painter = findShowingButtonByCommand("RibbonFormatPainter");
		assertTrue(painter.isEnabled(), "Format Painter must be enabled for one editable task source");
		robot.keyPress(KeyEvent.VK_SHIFT);
		click(robot, cellOnScreen(sheet, rowForTask(sheet, firstTarget), 0));
		robot.keyRelease(KeyEvent.VK_SHIFT);
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame().getSelectedImpls(false)
				.containsAll(List.of(source, firstTarget)), "Shift-click must create the mixed multi-task selection fixture");
		assertFalse(painter.isEnabled(), "Format Painter must reject a multiple-task source selection");
		// The lead row can already be the source while Shift-click has extended the selection.
		// clickUntilSelected only checks that lead row, so issue an unmodified physical click
		// here to collapse the multi-selection before asserting single-source enablement.
		click(robot, cellOnScreen(sheet, rowForTask(sheet, source), 0));
		GuiAcceptanceSupport.await(() -> sheetHasOnlySelectedTask(sheet, source),
			"an unmodified source-row click did not collapse the Format Painter source selection");
		GuiAcceptanceSupport.await(painter::isEnabled, "Format Painter must re-enable for a single source task");
		click(robot, painter);
		GuiAcceptanceSupport.await(painter::isSelected, "one-shot Format Painter did not enter its armed state");
		clickUntilSelected(robot, window, sheet, rowForTask(sheet, firstTarget), 0);
		GuiAcceptanceSupport.await(() -> firstTarget.isFontBold() && firstTarget.isFontItalic()
				&& firstTarget.isFontStrikethrough() && firstTarget.getFontSize() == 15
				&& java.util.Objects.equals(firstTarget.getFontFamily(), "Dialog")
				&& java.util.Objects.equals(firstTarget.getFontColor(), 0x2457A6),
			"the physical target-row selection did not receive all six source text-style properties");
		GuiAcceptanceSupport.await(() -> !painter.isSelected(), "one-shot Format Painter must disarm after one target selection");
		assertTrue(undo.canUndo(), "one target application must create one undoable mutation");
		assertEquals("Format first target", firstTarget.getName(), "format painting must preserve task data");
		assertArrayEquals(originalDates, new long[] { firstTarget.getStart(), firstTarget.getEnd(), secondTarget.getStart(),
			secondTarget.getEnd(), thirdTarget.getStart(), thirdTarget.getEnd(), untouched.getStart(), untouched.getEnd() },
			"text formatting must not change schedule dates");
		java.awt.Font fallback = new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 12);
		assertEquals(com.microproject.pm.graphic.TaskFontStyle.resolveFont(source, fallback),
			com.microproject.pm.graphic.TaskFontStyle.resolveFont(firstTarget, fallback),
			"the task sheet and Gantt text renderer must resolve the same copied text format");
		java.awt.Component renderedTaskName = renderedTaskNameCell(sheet, firstTarget);
		assertEquals(com.microproject.pm.graphic.TaskFontStyle.resolveFont(firstTarget, renderedTaskName.getFont()),
			renderedTaskName.getFont(), "the rendered task-table name must display the copied font attributes");
		assertEquals(com.microproject.pm.graphic.TaskFontStyle.resolveColor(firstTarget, renderedTaskName.getForeground()),
			renderedTaskName.getForeground(), "the rendered task-table name must display the copied font color");

		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Z);
		GuiAcceptanceSupport.await(() -> !firstTarget.isFontBold() && firstTarget.getFontSize() == 0
				&& firstTarget.getFontColor() == null,
			"Undo did not restore the target's original task text style");
		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Y);
		GuiAcceptanceSupport.await(() -> firstTarget.isFontBold() && firstTarget.getFontSize() == 15,
			"Redo did not restore the copied task text style");

		ByteArrayOutputStream saved = new ByteArrayOutputStream();
		MpoFileImporter importer = new MpoFileImporter();
		assertTrue(importer.saveProject(project, saved), "MPO save rejected the formatted task project");
		Project reloaded = importer.loadProject(new ByteArrayInputStream(saved.toByteArray()));
		Task reloadedFirst = taskNamed(reloaded, "Format first target");
		assertTrue(reloadedFirst.isFontBold() && reloadedFirst.isFontItalic() && reloadedFirst.isFontStrikethrough());
		assertEquals("Dialog", reloadedFirst.getFontFamily());
		assertEquals(15, reloadedFirst.getFontSize());
		assertEquals(Integer.valueOf(0x2457A6), reloadedFirst.getFontColor());

		clickUntilSelected(robot, window, sheet, rowForTask(sheet, source), 0);
		click(robot, painter);
		GuiAcceptanceSupport.await(painter::isSelected, "Format Painter did not arm before continuous mode");
		click(robot, painter);
		GuiAcceptanceSupport.await(painter::isSelected, "second Format Painter activation must promote to continuous mode");
		clickUntilSelected(robot, window, sheet, rowForTask(sheet, secondTarget), 0);
		GuiAcceptanceSupport.await(() -> secondTarget.isFontBold() && secondTarget.getFontSize() == 15,
			"continuous Format Painter did not format its first target");
		assertTrue(painter.isSelected(), "continuous Format Painter must remain armed after a target selection");
		clickUntilSelected(robot, window, sheet, rowForTask(sheet, thirdTarget), 0);
		GuiAcceptanceSupport.await(() -> thirdTarget.isFontBold() && thirdTarget.getFontSize() == 15,
			"continuous Format Painter did not format its next target");
		press(robot, KeyEvent.VK_ESCAPE);
		GuiAcceptanceSupport.await(() -> !painter.isSelected(), "Escape must stop continuous Format Painter mode");
		clickUntilSelected(robot, window, sheet, rowForTask(sheet, untouched), 0);
		assertFalse(untouched.isFontBold() || untouched.isFontItalic() || untouched.isFontStrikethrough()
			|| untouched.getFontSize() != 0 || untouched.getFontFamily() != null || untouched.getFontColor() != null,
			"the target selected after Escape must remain unchanged");
		assertEquals(originalIds, List.of(source.getId(), firstTarget.getId(), secondTarget.getId(),
			thirdTarget.getId(), untouched.getId()), "format painting must not reorder or renumber tasks");
		assertArrayEquals(originalDates, new long[] { firstTarget.getStart(), firstTarget.getEnd(), secondTarget.getStart(),
			secondTarget.getEnd(), thirdTarget.getStart(), thirdTarget.getEnd(), untouched.getStart(), untouched.getEnd() },
			"continuous text formatting must leave all target schedules unchanged");
	}

	private static boolean sheetHasOnlySelectedTask(SpreadSheet sheet, Task expected) {
		boolean[] selected = new boolean[1];
		try {
			SwingUtilities.invokeAndWait(() -> {
				List<Node> nodes = sheet.getSelectedNodes();
				selected[0] = sheet.getSelectedRows().length == 1 && nodes.size() == 1
					&& nodes.getFirst().getImpl() == expected;
			});
		} catch (Exception failure) {
			throw new IllegalStateException("could not inspect single-source task selection", failure);
		}
		return selected[0];
	}

	private AbstractButton findShowingButtonByText(String text) throws Exception {
		return GuiAcceptanceSupport.findShowingButtonByText(window, text);
	}

	private static AbstractButton findShowingButtonByText(java.awt.Container container, String text) throws Exception {
		return GuiAcceptanceSupport.findShowingButtonByText(container, text);
	}

	private AbstractButton findShowingButtonByCommand(String command) throws Exception {
		AbstractButton[] result = new AbstractButton[1];
		SwingUtilities.invokeAndWait(() -> result[0] = UiComponentWalker.flatten(window).stream()
			.filter(AbstractButton.class::isInstance)
			.map(RibbonGuiButton::adapt)
			.filter(AbstractButton::isShowing).filter(button -> command.equals(button.getActionCommand()))
			.findFirst().orElseThrow(() -> new AssertionError("Visible ribbon command not found: " + command
				+ " visibleCommands=" + UiComponentWalker.flatten(window).stream()
					.filter(AbstractButton.class::isInstance)
					.map(RibbonGuiButton::adapt)
					.filter(AbstractButton::isShowing).map(AbstractButton::getActionCommand)
					.filter(java.util.Objects::nonNull).collect(Collectors.joining(",")))));
		return result[0];
	}

	private AbstractButton findVisibleOrOverflowButton(Robot robot, String command) throws Exception {
		return RibbonGuiSupport.findVisibleOrExpand(robot, window, command);
	}

	private static int rowForTask(SpreadSheet sheet, NormalTask task) {
		CommonSpreadSheetModel model = (CommonSpreadSheetModel) sheet.getModel();
		for (int row = 0; row < sheet.getRowCount(); row++) {
			if (model.getNode(row) != null && model.getNode(row).getNode().getImpl() == task)
				return row;
		}
		throw new AssertionError("Task is absent from the visible spreadsheet");
	}

	private static java.awt.Component renderedTaskNameCell(SpreadSheet sheet, Task task) throws Exception {
		java.awt.Component[] rendered = new java.awt.Component[1];
		SwingUtilities.invokeAndWait(() -> {
			int row = rowForTask(sheet, (NormalTask)task);
			int viewRow = sheet.convertRowIndexToView(row);
			int viewColumn = nameColumn(sheet);
			if (viewColumn < 0)
				throw new AssertionError("Task-name field is not visible in the spreadsheet");
			javax.swing.table.TableCellRenderer renderer = sheet.getCellRenderer(viewRow, viewColumn);
			rendered[0] = renderer.getTableCellRendererComponent(sheet, sheet.getValueAt(viewRow, viewColumn),
				false, false, viewRow, viewColumn);
		});
		return rendered[0];
	}

	private static List<String> taskOrder(Project project) {
		List<String> names = new ArrayList<>();
		java.util.Iterator<Task> iterator = project.getTaskOutlineIterator();
		while (iterator.hasNext())
			names.add(iterator.next().getName());
		return names;
	}

	private static List<String> visibleTaskOrder(SpreadSheet sheet) {
		List<String> names = new ArrayList<>();
		CommonSpreadSheetModel model = (CommonSpreadSheetModel) sheet.getModel();
		for (int row = 0; row < sheet.getRowCount(); row++) {
			if (model.getNode(row) != null && model.getNode(row).getNode() != null
					&& model.getNode(row).getNode().getImpl() instanceof Task task)
				names.add(task.getName());
		}
		return names;
	}

	private static JComboBox<?> findShowingTransformCombo(String transformId) throws Exception {
		JComboBox<?>[] result = new JComboBox<?>[1];
		SwingUtilities.invokeAndWait(() -> {
			for (Window candidate : Window.getWindows()) {
				if (!candidate.isShowing()) continue;
				for (Component component : UiComponentWalker.flatten(candidate)) {
					if (!(component instanceof JComboBox<?> combo) || !combo.isShowing()) continue;
					for (int index = 0; index < combo.getItemCount(); index++) {
						Object item = combo.getItemAt(index);
						if (item instanceof CommonTransformFactory factory && transformId.equals(factory.getId())) {
							result[0] = combo;
							return;
						}
					}
				}
			}
		});
		assertNotNull(result[0], "Visible generic Sort dialog combo missing transform " + transformId);
		return result[0];
	}

	private static JComboBox<?> findTaskSortDirection(Dialog dialog) {
		return UiComponentWalker.flatten(dialog).stream()
			.filter(JComboBox.class::isInstance).map(JComboBox.class::cast)
			.filter(combo -> "microproject.taskSort.direction".equals(combo.getName()))
			.findFirst().orElseThrow(() -> new AssertionError("Task sort direction chooser missing"));
	}

	private static Dialog visibleMoveConfirmation() {
		for (Window candidate : Window.getWindows()) {
			if (candidate instanceof Dialog dialog && dialog.isShowing()
					&& (findVisibleButton(dialog, "OK") != null || findVisibleButton(dialog, "はい") != null
							|| findVisibleButton(dialog, "確認") != null))
				return dialog;
		}
		return null;
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

	private static Dialog findDialogContainingText(String expected) {
		for (Window candidate : Window.getWindows()) {
			if (candidate instanceof Dialog dialog && dialog.isShowing()
					&& componentText(dialog).contains(expected))
				return dialog;
		}
		return null;
	}

	private static String componentText(java.awt.Container container) {
		StringBuilder text = new StringBuilder();
		for (Component component : container.getComponents()) {
			if (component instanceof JLabel label) text.append(label.getText());
			if (component instanceof JTextComponent textComponent) text.append(textComponent.getText());
			if (component instanceof JOptionPane optionPane) appendMessageText(optionPane.getMessage(), text);
			if (component instanceof java.awt.Container child) text.append(componentText(child));
		}
		return text.toString().replaceAll("(?i)<br\\s*/?>", " ")
				.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ");
	}

	private static boolean isActiveDialogOwnedBy(Window expectedOwner, String expectedTitle) {
		boolean[] result = new boolean[1];
		try {
			SwingUtilities.invokeAndWait(() -> {
				Window active = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
				result[0] = active instanceof Dialog dialog && dialog.getOwner() == expectedOwner
					&& expectedTitle.equals(dialog.getTitle());
			});
		} catch (Exception exception) {
			throw new AssertionError("Could not inspect the active confirmation on the EDT", exception);
		}
		return result[0];
	}

	private static Dialog activeDialogOwnedBy(Window expectedOwner) throws Exception {
		Dialog[] result = new Dialog[1];
		SwingUtilities.invokeAndWait(() -> {
			Window active = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
			if (active instanceof Dialog dialog && dialog.getOwner() == expectedOwner) result[0] = dialog;
		});
		if (result[0] == null) throw new AssertionError("No active confirmation is owned by the baseline chooser");
		return result[0];
	}

	private static boolean isShowingOnEdt(Window window) {
		boolean[] result = new boolean[1];
		try {
			SwingUtilities.invokeAndWait(() -> result[0] = window.isShowing());
		} catch (Exception exception) {
			throw new AssertionError("Could not inspect the dialog on the EDT", exception);
		}
		return result[0];
	}

	private static void appendMessageText(Object message, StringBuilder text) {
		if (message instanceof Object[] messages) {
			for (Object item : messages) appendMessageText(item, text);
		} else if (message instanceof Component component) {
			if (component instanceof JLabel label) text.append(label.getText());
			if (component instanceof JTextComponent textComponent) text.append(textComponent.getText());
			if (component instanceof java.awt.Container container) text.append(componentText(container));
		} else if (message != null) {
			text.append(message);
		}
	}

	private static boolean isTaskVisibleInGantt(Gantt gantt, NormalTask task) {
		var cache = gantt.getModel().getCache();
		for (int row = 0; row < cache.getSize(); row++) {
			Object element = cache.getElementAt(row);
			if (element instanceof GraphicNode graphicNode && graphicNode.getNode() != null
					&& graphicNode.getNode().getImpl() == task)
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

	private static void assertRibbonOutcome(GraphicManager manager, RibbonCommandResult previousOutcome,
			RibbonCommandResult.Status expectedStatus,
			Long... expectedTaskIds) throws Exception {
		List<Long> expectedIds = List.of(expectedTaskIds);
		assertRibbonOutcome(manager, previousOutcome, expectedStatus, expectedIds, expectedIds);
	}

	private static void assertRibbonOutcome(GraphicManager manager, RibbonCommandResult previousOutcome,
			RibbonCommandResult.Status expectedStatus, List<Long> expectedSelectedTaskIds,
			List<Long> expectedAffectedTaskIds) throws Exception {
		GuiAcceptanceSupport.await(() -> manager.getLastRibbonCommandResult() != null
				&& manager.getLastRibbonCommandResult() != previousOutcome,
			"GraphicManager did not record a new command outcome");
		RibbonCommandResult result = manager.getLastRibbonCommandResult();
		assertEquals(expectedStatus, result.status(), "ribbon command status: " + result);
		assertEquals(expectedSelectedTaskIds, result.selectedTaskIds(), "ribbon command selected task IDs");
		assertEquals(expectedAffectedTaskIds, result.affectedTaskIds(), "ribbon command affected task IDs");
		assertEquals(manager.getCurrentFrame().getTopViewId(), result.activeViewId(),
			"ribbon command outcome must identify the independently observed active view");
	}

	private static Rectangle boundsOnScreen(java.awt.Component component) throws Exception {
		return WindowAcceptanceAssertions.boundsOnScreen(component);
	}

	private static void assertWindowFitsUsableScreen(MainRibbonFrame frame, String description) {
		GraphicsConfiguration configuration = frame.getGraphicsConfiguration();
		Rectangle usable = new Rectangle(configuration.getBounds());
		Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(configuration);
		usable.x += insets.left;
		usable.y += insets.top;
		usable.width -= insets.left + insets.right;
		usable.height -= insets.top + insets.bottom;
		assertTrue(usable.contains(frame.getBounds()), description + " must remain fully visible in the monitor work area; frame="
			+ frame.getBounds() + ", usable=" + usable + ", scale=" + configuration.getDefaultTransform());
	}



	private static void clickAndAwaitSelected(Robot robot, AbstractButton button, String failureMessage) throws Exception {
		if (buttonSelectedOnEdt(button))
			return;
		click(robot, button);
		try {
			GuiAcceptanceSupport.await(() -> buttonSelectedOnEdt(button), failureMessage);
		} catch (AssertionError failure) {
			throw new AssertionError(failure.getMessage() + "; afterClick={"
				+ componentInputDiagnostics(button) + "}; windows={" + visibleWindowInputDiagnostics() + "}", failure);
		}
	}

	private static boolean buttonSelectedOnEdt(AbstractButton button) {
		boolean[] selected = new boolean[1];
		try { SwingUtilities.invokeAndWait(() -> selected[0] = button.isSelected()); }
		catch (Exception exception) { throw new AssertionError("could not inspect ribbon tab on EDT", exception); }
		return selected[0];
	}

	private record RowHeaderTarget(Point screenPoint, Rectangle cell, Rectangle visible) {}

	private static RowHeaderTarget rowHeaderTargetOnScreen(JTable rowHeader, int row, boolean beforeHalf)
			throws Exception {
		RowHeaderTarget[] target = new RowHeaderTarget[1];
		SwingUtilities.invokeAndWait(() -> {
			Rectangle cell = rowHeader.getCellRect(row, 0, true);
			Rectangle visible = cell.intersection(rowHeader.getVisibleRect());
			assertFalse(visible.isEmpty(), "task row cell must intersect the visible row header; cell="
				+ cell + ", visibleRect=" + rowHeader.getVisibleRect() + ", row=" + row);
			int x = visible.x + visible.width / 2;
			int y = beforeHalf
				? visible.y + Math.max(1, visible.height / 4)
				: visible.y + visible.height / 2;
			if (beforeHalf)
				assertTrue(y < cell.y + cell.height / 2, "target must remain in the before half of the task row");
			Point location = rowHeader.getLocationOnScreen();
			target[0] = new RowHeaderTarget(new Point(location.x + x, location.y + y), cell, visible);
		});
		return target[0];
	}

	private static Point moveToRowHeaderTarget(Robot robot, JTable rowHeader, int row, boolean beforeHalf,
			String failureMessage) throws Exception {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);
		RowHeaderTarget[] target = new RowHeaderTarget[1];
		while (System.nanoTime() < deadline) {
			target[0] = rowHeaderTargetOnScreen(rowHeader, row, beforeHalf);
			robot.mouseMove(target[0].screenPoint().x, target[0].screenPoint().y);
			robot.waitForIdle();
			if (rowHeaderHoverRow(rowHeader) == row)
				return target[0].screenPoint();
			Thread.sleep(25);
		}
		throw new AssertionError(failureMessage + "; target=" + target[0] + "; "
			+ componentInputDiagnostics(rowHeader) + "; windows={" + visibleWindowInputDiagnostics() + "}");
	}

	private static Point componentMousePosition(Component component) {
		Point[] local = new Point[1];
		try { SwingUtilities.invokeAndWait(() -> local[0] = component.getMousePosition()); }
		catch (Exception exception) { throw new AssertionError("could not inspect component pointer position on EDT", exception); }
		return local[0];
	}

	private static int rowHeaderHoverRow(JTable rowHeader) {
		int[] row = { -1 };
		try {
			SwingUtilities.invokeAndWait(() -> {
				Point point = rowHeader.getMousePosition();
				if (point != null) row[0] = rowHeader.rowAtPoint(point);
			});
		} catch (Exception exception) {
			throw new AssertionError("could not inspect row header hover on EDT", exception);
		}
		return row[0];
	}

	private static String visibleWindowInputDiagnostics() {
		String[] value = new String[1];
		try {
			SwingUtilities.invokeAndWait(() -> value[0] = java.util.Arrays.stream(Window.getWindows())
				.filter(Window::isShowing)
				.map(candidate -> candidate.getClass().getSimpleName() + "[active=" + candidate.isActive()
					+ ",focused=" + candidate.isFocused() + ",modal=" + (candidate instanceof Dialog dialog
						&& dialog.isModal()) + ",bounds=" + candidate.getBounds() + "]")
				.collect(Collectors.joining("; ")));
		} catch (Exception exception) {
			return "windowSnapshotError=" + exception;
		}
		return value[0];
	}

	private static boolean hasShowingCommand(String command) {
		boolean[] found = new boolean[1];
		try {
			SwingUtilities.invokeAndWait(() -> {
				for (Window candidate : Window.getWindows()) {
					if (!(candidate instanceof MainRibbonFrame frame) || !frame.isShowing()) continue;
					found[0] = UiComponentWalker.flatten(frame).stream().filter(AbstractButton.class::isInstance)
						.map(RibbonGuiButton::adapt).anyMatch(button -> button.isShowing()
							&& command.equals(button.getActionCommand()));
					if (found[0]) break;
				}
			});
		} catch (Exception exception) {
			throw new AssertionError("could not inspect visible ribbon commands", exception);
		}
		return found[0];
	}

	private static String componentInputDiagnostics(Component component) {
		Point[] local = new Point[1];
		Point[] screen = new Point[1];
		java.awt.Dimension[] size = new java.awt.Dimension[1];
		java.awt.Rectangle[] visible = new java.awt.Rectangle[1];
		GraphicsConfiguration[] configuration = new GraphicsConfiguration[1];
		try {
			SwingUtilities.invokeAndWait(() -> {
				local[0] = component.getMousePosition();
				try { screen[0] = component.getLocationOnScreen(); } catch (java.awt.IllegalComponentStateException ignored) { }
				size[0] = component.getSize();
				visible[0] = component instanceof JComponent swing ? swing.getVisibleRect() : null;
				configuration[0] = component.getGraphicsConfiguration();
			});
		} catch (Exception exception) {
			return "componentSnapshotError=" + exception;
		}
		return "component=" + component.getClass().getSimpleName() + ", showing=" + component.isShowing()
			+ ", enabled=" + component.isEnabled() + ", size=" + size[0] + ", visibleRect=" + visible[0]
			+ ", localMouse=" + local[0] + ", locationOnScreen=" + screen[0]
			+ ", pointer=" + java.awt.MouseInfo.getPointerInfo().getLocation()
			+ ", toolkitDpi=" + Toolkit.getDefaultToolkit().getScreenResolution()
			+ ", uiScale=" + System.getProperty("sun.java2d.uiScale", "default")
			+ ", graphicsTransform=" + (configuration[0] == null ? null : configuration[0].getDefaultTransform());
	}

	private static void click(Robot robot, Component component) throws Exception {
		WindowAcceptanceAssertions.clickComponent(robot, component);
	}

	private static void clickDisabled(Robot robot, Component component) throws Exception {
		WindowAcceptanceAssertions.clickDisabledComponent(robot, component);
	}

	private static void click(Robot robot, Rectangle bounds) {
		robot.mouseMove(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
		robot.delay(150);
	}

	private static void clickWithWindowsDpi(Robot robot, MainRibbonFrame frame, AbstractButton button, Rectangle bounds)
		throws Exception {
		double dpiScale = java.awt.Toolkit.getDefaultToolkit().getScreenResolution() / 96.0d;
		Point awtTarget = new Point(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
		Point robotTarget = WindowAcceptanceAssertions.robotPointFor(bounds);
		GraphicsConfiguration configuration = frame.getGraphicsConfiguration();
		Rectangle screenBounds = configuration.getBounds();
		String suffix = (System.getProperty("user.language", "unknown") + "-"
			+ System.getProperty("sun.java2d.uiScale", "default")).replaceAll("[^A-Za-z0-9_.-]", "_");
		String name = "secondary-window-close-" + suffix;
		Path artifactDirectory = Path.of(System.getProperty("microproject.gui.artifacts.dir", "build/reports/guiTest-artifacts"));
		Files.createDirectories(artifactDirectory);
		BufferedImage screenshot = robot.createScreenCapture(screenBounds);
		ImageIO.write(screenshot, "png", artifactDirectory.resolve(name + "-before-click.png").toFile());
		robot.mouseMove(robotTarget.x, robotTarget.y);
		robot.waitForIdle();
		Point actualPointer = java.awt.MouseInfo.getPointerInfo().getLocation();
		String diagnostics = "frameBounds=" + frame.getBounds() + System.lineSeparator()
			+ "frameLocationOnScreen=" + frame.getLocationOnScreen() + System.lineSeparator()
			+ "closeButtonBoundsOnScreen=" + bounds + System.lineSeparator()
			+ "screenBounds=" + screenBounds + System.lineSeparator()
			+ "toolkitDpi=" + Toolkit.getDefaultToolkit().getScreenResolution() + System.lineSeparator()
			+ "graphicsTransform=" + configuration.getDefaultTransform() + System.lineSeparator()
			+ "dpiScaleUsed=" + dpiScale + System.lineSeparator()
			+ "awtTarget=" + awtTarget + System.lineSeparator()
			+ "robotTarget=" + robotTarget + System.lineSeparator()
			+ "pointerAfterMove=" + actualPointer + System.lineSeparator()
			+ "buttonShowing=" + button.isShowing() + System.lineSeparator();
		Files.writeString(artifactDirectory.resolve(name + "-coordinates.txt"), diagnostics);
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
		try {
			return GuiAcceptanceSupport.findShowingButtonByText(container, text);
		} catch (AssertionError missingButton) {
			return null;
		} catch (Exception failure) {
			throw new AssertionError("Unable to inspect visible button for semantic label: " + text, failure);
		}
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
