/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.AWTEvent;
import java.awt.Toolkit;
import java.awt.GraphicsEnvironment;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.AWTEventListener;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.swing.AbstractButton;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import javax.swing.WindowConstants;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.microproject.graphic.configuration.CellStyles;
import com.microproject.dialog.TaskInformationDialog;
import com.microproject.init.Init;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetModel;
import com.microproject.pm.graphic.spreadsheet.renderer.NameCellComponent;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.session.SessionFactory;
import com.microproject.job.JobQueue;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.testsupport.GuiRobot;
import com.microproject.testsupport.RibbonGuiSupport;
import com.microproject.ui.ribbon.RibbonDisplayMode;
import com.microproject.ui.ribbon.RibbonDisplayPreferences;
import com.microproject.util.Alert;
import com.microproject.util.Environment;
import com.microproject.util.SwingFileChooserProvider;
import com.microproject.util.UiServices;
import com.microproject.ui.util.SwingAlertPresenter;
import com.microproject.ui.util.SwingUiDispatcher;
import com.microproject.strings.Messages;
import com.microproject.util.UiDispatch;

/** Two separately invoked GUI phases prove an MPO remains interactive across a JVM restart. */
class FreshProcessMpoRestartGuiAcceptanceTest {
	private static final String WORKSPACE_ENV = "MICROPROJECT_ISSUE967_WORKSPACE";
	private static final String DRIVER_REQUIRED = "Run both phases through test-issue-967-fresh-process.ps1 to preserve the JVM boundary.";
	private MainRibbonFrame window;
	private GraphicManager manager;
	private Path workspace;
	private boolean previousRibbon;
	private boolean previousNewLook;
	private boolean previousClientSide;
	private UiServices.FileChooserProvider previousChooser;
	private JobQueue previousJobQueue;
	private RibbonDisplayMode previousRibbonMode;
	private boolean environmentCaptured;

	@Test
	void phaseOneOpensPodInTheUiAddsARowAndSavesAsMpo() throws Exception {
		assumeDesktop();
		workspace = workspace();
		Path source = workspace.resolve("Commercial construction project plan2.pod");
		Path fixture = Path.of(System.getProperty("microproject.project.dir"), "samples", source.getFileName().toString());
		assertEquals("ef3fae19814517d10b225b131c251c6ade27cc9154d5138cf3347479e6b1aab4", sha256(fixture),
			"the reporter's fixed POD fixture must not drift");
		Files.copy(fixture, source);
		Path mpo = workspace.resolve("fresh-process.mpo");
		createWindow("microProject — #967 save phase");
		installChooser(source, mpo);
		Robot robot = robot();

		openThroughBackstage(robot);
		DocumentFrame frame = awaitFrame(source);
		Project project = frame.getProject();
		Task selectedBeforeSave = selectFirstTask(robot, frame);
		long selectedUid = onEdt(selectedBeforeSave::getUniqueId);
		Files.writeString(workspace.resolve("selected-task.uid"), Long.toString(selectedUid),
			StandardCharsets.UTF_8);
		int rowsBefore = onEdt(() -> outlineRows(project).size());
		robot.keyPress(KeyEvent.VK_INSERT);
		robot.keyRelease(KeyEvent.VK_INSERT);
		GuiAcceptanceSupport.await(() -> outlineCountUnchecked(project) == rowsBefore + 1,
			"physical Insert on the imported POD did not add a row");
		GuiAcceptanceSupport.await(() -> dirtyUnchecked(project), "the POD insertion did not mark the project dirty");

		clickCommand(robot, "RibbonSaveProjectAs", "saveAs");
		GuiAcceptanceSupport.await(() -> Files.isRegularFile(mpo) && !dirtyUnchecked(project),
			"Save As did not publish and commit the MPO");
		assertEquals(mpo.toString(), onEdt(project::getFileName), "Save As must associate the project with the MPO target");
		List<String> rowsAfterSave = onEdt(() -> outlineRows(project));
		Files.writeString(workspace.resolve("expected-rows.txt"), encodeRows(rowsAfterSave), StandardCharsets.UTF_8);
		writeNodeDiagnostics("phase-one-node-counts.txt", project);
		Files.writeString(workspace.resolve("phase-one.identity"), ProcessHandle.current().pid() + System.lineSeparator()
			+ ProcessHandle.current().info().startInstant().orElseThrow().toEpochMilli(), StandardCharsets.UTF_8);

		closeProject(robot, mpo);
		Files.writeString(workspace.resolve("phase-one.complete"), "passed", StandardCharsets.UTF_8);
		// The GUI test fork owns the application window. Its clean exit after this
		// test provides the process boundary for phase two; title-bar close calls
		// System.exit and would terminate Gradle's test worker before JUnit reports.
		assertTrue(onEdt(window::isDisplayable), "the GUI worker must remain alive to report phase one");
	}

	@Test
	void phaseTwoStartsInANewJvmAndEditsTheSavedMpo() throws Exception {
		assumeDesktop();
		workspace = workspace();
		Path mpo = workspace.resolve("fresh-process.mpo");
		Path expectedRowsFile = workspace.resolve("expected-rows.txt");
		assertTrue(Files.isRegularFile(mpo), "phase one must leave the MPO for the fresh JVM");
		assertTrue(Files.isRegularFile(expectedRowsFile), "phase one must persist its expected task outline");
		String[] phaseOneIdentity = Files.readString(workspace.resolve("phase-one.identity"), StandardCharsets.UTF_8).split("\\R");
		long phaseOnePid = Long.parseLong(phaseOneIdentity[0]);
		long phaseOneStarted = Long.parseLong(phaseOneIdentity[1]);
		long phaseTwoPid = ProcessHandle.current().pid();
		long phaseTwoStarted = ProcessHandle.current().info().startInstant().orElseThrow().toEpochMilli();
		assertTrue(phaseOnePid != phaseTwoPid || phaseOneStarted != phaseTwoStarted,
			"phase two must run in a fresh GUI test JVM, even if Windows reuses a PID");

		createWindow("microProject — #967 fresh process reopen phase");
		installChooser(mpo, mpo);
		Robot robot = robot();
		openThroughBackstage(robot);
		DocumentFrame frame = awaitFrame(mpo);
		Project project = frame.getProject();
		List<String> phaseOneRows = decodeRows(Files.readAllLines(expectedRowsFile, StandardCharsets.UTF_8));
		writeNodeDiagnostics("phase-two-node-counts.txt", project);
		assertEquals(phaseOneRows, onEdt(() -> outlineRows(project)), "fresh-JVM reopen must restore the POD-derived MPO rows");

		Task selectedAfterRestart = selectFirstTask(robot, frame);
		TaskIdentity selectedIdentity = onEdt(() -> new TaskIdentity(selectedAfterRestart.getUniqueId(),
			selectedAfterRestart.getId(), selectedAfterRestart.getName()));
		long expectedTaskUid = Long.parseLong(Files.readString(workspace.resolve("selected-task.uid"), StandardCharsets.UTF_8));
		assertEquals(expectedTaskUid, selectedIdentity.uniqueId(),
			"the selected stable task identity must survive the process restart");
		openTaskInformation(robot, selectedIdentity);
		int rowsBefore = onEdt(() -> outlineRows(project).size());
		robot.keyPress(KeyEvent.VK_INSERT);
		robot.keyRelease(KeyEvent.VK_INSERT);
		GuiAcceptanceSupport.await(() -> outlineCountUnchecked(project) == rowsBefore + 1,
			"a fresh-JVM MPO must accept a physical Insert");
		GuiAcceptanceSupport.await(() -> dirtyUnchecked(project), "the fresh-JVM insertion must make the MPO dirty");
		SpreadSheet sheet = onEdt(frame::getTopSpreadSheet);
		Task selectedAfterInsert = selectFirstTask(robot, frame);
		assertEquals(selectedIdentity.uniqueId(), onEdt(selectedAfterInsert::getUniqueId),
			"physical Insert must leave the intended existing task identity selected");
		int editedRow = onEdt(() -> rowForTask(sheet, selectedAfterInsert));
		int nameColumn = onEdt(() -> nameColumn(sheet));
		Point nameCell = onEdt(() -> cellCenterOnScreen(sheet, editedRow, nameColumn));
		activateWindow(robot);
		nameCell = onEdt(() -> cellCenterOnScreen(sheet, editedRow, nameColumn));
		robot.mouseMove(nameCell.x, nameCell.y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		GuiAcceptanceSupport.awaitInputAndEventQueueIdle(robot, "physical selection of existing task name cell");
		assertEquals(editedRow, onEdt(sheet::getSelectedRow), "Robot must select the intended task row for renaming");
		Task taskAtSelection = onEdt(() -> selectedTaskAt(sheet, editedRow));
		assertEquals(selectedIdentity.uniqueId(), onEdt(taskAtSelection::getUniqueId),
			"the physical name-cell click must select the intended stable task identity");
		robot.keyPress(KeyEvent.VK_F2);
		robot.keyRelease(KeyEvent.VK_F2);
		GuiAcceptanceSupport.await(() -> onEdtUnchecked(sheet::isEditing), "F2 did not start editing the selected task name");
		Component editor = onEdt(sheet::getEditorComponent);
		assertTrue(editor instanceof JTextComponent, "task name editor must expose physical text input");
		GuiAcceptanceSupport.await(() -> onEdtUnchecked(() -> editor.isFocusOwner()),
			"F2 did not transfer focus to the task name editor");
		StringBuilder inputEvents = new StringBuilder();
		AWTEventListener eventRecorder = event -> {
			if (event instanceof KeyEvent key) inputEvents.append("KEY id=").append(key.getID())
				.append(" code=").append(key.getKeyCode()).append(" char=").append((int) key.getKeyChar())
				.append(" source=").append(key.getSource().getClass().getName())
				.append(" consumed=").append(key.isConsumed()).append(System.lineSeparator());
			else if (event instanceof java.awt.event.InputMethodEvent ime) inputEvents.append("IME id=").append(ime.getID())
				.append(" text=").append(ime.getText()).append(" committed=").append(ime.getCommittedCharacterCount())
				.append(System.lineSeparator());
		};
		Toolkit.getDefaultToolkit().addAWTEventListener(eventRecorder,
			AWTEvent.KEY_EVENT_MASK | AWTEvent.INPUT_METHOD_EVENT_MASK);
		try {
			writeEditDiagnostic("edit-before-typing.txt", project, sheet, editedRow, nameColumn, editor, inputEvents);
			robot.keyPress(KeyEvent.VK_CONTROL);
			robot.keyPress(KeyEvent.VK_A);
			robot.keyRelease(KeyEvent.VK_A);
			robot.keyRelease(KeyEvent.VK_CONTROL);
			for (char character : "issue 967 persisted task".toCharArray()) {
				int keyCode = character == ' ' ? KeyEvent.VK_SPACE : KeyEvent.getExtendedKeyCodeForChar(character);
				if (keyCode == KeyEvent.VK_UNDEFINED) throw new AssertionError("No key code for " + character);
				robot.keyPress(keyCode);
				robot.keyRelease(keyCode);
			}
			robot.waitForIdle();
			assertEquals("issue 967 persisted task", onEdt(() -> ((JTextComponent) editor).getText()),
				"physical multi-character input must replace the existing task name before Enter");
			assertEquals(selectedIdentity.name(), onEdt(selectedAfterRestart::getName),
				"the project model must remain unchanged until the editor commits");
			writeEditDiagnostic("edit-before-enter.txt", project, sheet, editedRow, nameColumn, editor, inputEvents);
			robot.keyPress(KeyEvent.VK_ENTER);
			robot.keyRelease(KeyEvent.VK_ENTER);
			try {
				GuiAcceptanceSupport.await(() -> onEdtUnchecked(() -> !sheet.isEditing()
					&& "issue 967 persisted task".equals(selectedAfterRestart.getName())
					&& "issue 967 persisted task".equals(renderedCellText(sheet, editedRow, nameColumn))),
					"physical multi-character task-name edit did not commit to the model and rendered spreadsheet");
			} finally {
				writeEditDiagnostic("edit-after-enter.txt", project, sheet, editedRow, nameColumn, editor, inputEvents);
			}
		} finally {
			Toolkit.getDefaultToolkit().removeAWTEventListener(eventRecorder);
		}
		assertTrue(onEdt(() -> outlineRows(project).contains("issue 967 persisted task")),
			"physical rename did not update the existing task in the project model");
		clickImmediateBackstagePage(robot, "save");
		GuiAcceptanceSupport.await(() -> !dirtyUnchecked(project), "the fresh-JVM edit did not save");
		List<String> finalRows = onEdt(() -> outlineRows(project));

		closeProject(robot, mpo);
		installChooser(mpo, mpo);
		openThroughBackstage(robot);
		DocumentFrame reloaded = awaitFrame(mpo);
		List<String> reloadedRows = onEdt(() -> outlineRows(reloaded.getProject()));
		assertEquals(finalRows, reloadedRows,
			"the edit made after the process restart must survive a subsequent save and reload");
		assertTrue(reloadedRows.contains("issue 967 persisted task"), "the named task must survive MPO save and reload");
		closeProject(robot, mpo);
		Files.writeString(workspace.resolve("phase-two.complete"), "passed", StandardCharsets.UTF_8);
	}

	@AfterEach
	void disposeApplication() throws Exception {
		if (manager != null) SwingUtilities.invokeAndWait(manager::cleanUp);
		if (window != null) SwingUtilities.invokeAndWait(() -> {
			window.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			window.dispose();
		});
		if (environmentCaptured) {
			Alert.setPresenter(null);
			Environment.setRibbonUI(previousRibbon);
			Environment.setNewLook(previousNewLook);
			Environment.setClientSide(previousClientSide);
			UiServices.setFileChooserProvider(previousChooser);
			SessionFactory.getInstance().setJobQueue(previousJobQueue);
			RibbonDisplayPreferences.save(previousRibbonMode);
		}
	}

	private void assumeDesktop() {
		assumeFalse(GraphicsEnvironment.isHeadless(), "A Windows desktop session is required.");
		assumeTrue(System.getProperty("os.name", "").toLowerCase().contains("windows"),
			"The reporter's reference desktop target is Windows.");
	}

	private Path workspace() throws Exception {
		String configured = System.getenv(WORKSPACE_ENV);
		assumeTrue(configured != null && !configured.isBlank(), DRIVER_REQUIRED);
		Path path = Path.of(configured).toAbsolutePath().normalize();
		Files.createDirectories(path);
		return path;
	}

	private void createWindow(String title) throws Exception {
		previousRibbon = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		previousClientSide = Environment.isClientSide();
		previousChooser = UiServices.getFileChooserProvider();
		previousJobQueue = SessionFactory.getInstance().getJobQueue();
		previousRibbonMode = RibbonDisplayPreferences.load();
		environmentCaptured = true;
		Environment.setClientSide(true);
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		UiDispatch.setDispatcher(new SwingUiDispatcher());
		Alert.setPresenter(new SwingAlertPresenter());
		RibbonDisplayPreferences.save(RibbonDisplayMode.ALWAYS_SHOW);
		Init.initialize();
		assertNotNull(CellStyles.getInstance());
		SwingUtilities.invokeAndWait(() -> {
			window = new MainRibbonFrame(title, null);
			manager = new GraphicManager(window);
			window.setGraphicManager(manager);
			manager.initView();
			SessionFactory.getInstance().setJobQueue(manager.getJobQueue());
			window.setSize(1200, 700);
			window.setLocationByPlatform(true);
			window.setAlwaysOnTop(true);
			window.setVisible(true);
		});
		GuiAcceptanceSupport.await(() -> onEdtUnchecked(window::isShowing), "GUI acceptance window did not show");
	}

	private void installChooser(Path openPath, Path savePath) {
		SwingFileChooserProvider swingChooser = new SwingFileChooserProvider();
		UiServices.setFileChooserProvider(new UiServices.FileChooserProvider() {
			@Override public String chooseFileName(boolean save, String selectedFileName, Object parent) {
				return save ? savePath.toString() : swingChooser.chooseFileName(false, selectedFileName, parent);
			}
			@Override public List<String> chooseFileNames(boolean save, String selectedFileName, Object parent) {
				return save ? List.of() : List.of(openPath.toString());
			}
		});
	}

	private void openThroughBackstage(Robot robot) throws Exception {
		clickTab(robot, "File", "ファイル");
		GuiAcceptanceSupport.await(() -> onEdtUnchecked(() -> named(window, "officeBackstageView") != null), "File tab did not open Backstage");
		clickNamed(robot, "officeBackstageNav-open");
		clickNamed(robot, "officeBackstageCommand-RibbonOpenProject");
	}

	private void clickCommand(Robot robot, String commandId, String page) throws Exception {
		clickTab(robot, "File", "ファイル");
		GuiAcceptanceSupport.await(() -> onEdtUnchecked(() -> named(window, "officeBackstageView") != null), "File tab did not open Backstage");
		if (page != null) clickNamed(robot, "officeBackstageNav-" + page);
		clickNamed(robot, "officeBackstageCommand-" + commandId);
	}

	private void clickImmediateBackstagePage(Robot robot, String page) throws Exception {
		clickTab(robot, "File", "ファイル");
		GuiAcceptanceSupport.await(() -> onEdtUnchecked(() -> named(window, "officeBackstageView") != null),
			"File tab did not open Backstage");
		clickNamed(robot, "officeBackstageNav-" + page);
	}

	private void clickTab(Robot robot, String english, String japanese) throws Exception {
		Component tab = onEdt(() -> findText(window.getRibbonPanel(), english, japanese));
		assertNotNull(tab, "File tab is missing");
		click(robot, tab);
	}

	private void clickNamed(Robot robot, String name) throws Exception {
		GuiAcceptanceSupport.await(() -> onEdtUnchecked(() -> named(window, name) instanceof AbstractButton button && button.isShowing() && button.isEnabled()),
			"Backstage command is not visible and enabled: " + name);
		click(robot, onEdt(() -> named(window, name)));
	}

	private DocumentFrame awaitFrame(Path path) throws Exception {
		GuiAcceptanceSupport.await(() -> initializedFrameForPathUnchecked(path),
			"File/Open did not initialize the task spreadsheet and load task rows for " + path.getFileName());
		return onEdt(() -> manager.findFrameForProjectFile(path.toString()));
	}

	private boolean initializedFrameForPathUnchecked(Path path) {
		return onEdtUnchecked(() -> {
			DocumentFrame frame = manager.findFrameForProjectFile(path.toString());
			if (frame == null || frame.getProject() == null || frame.getTopSpreadSheet() == null) return false;
			SpreadSheet sheet = frame.getTopSpreadSheet();
			SpreadSheetModel model = (SpreadSheetModel) sheet.getModel();
			for (int row = 0; row < sheet.getRowCount(); row++) {
				var node = model.getNodeForDisplayRow(row);
				if (node != null && node.getImpl() instanceof Task task && !task.isSummary()) return true;
			}
			return false;
		});
	}

	private DocumentFrame frameForPathUnchecked(Path path) {
		return onEdtUnchecked(() -> manager.findFrameForProjectFile(path.toString()));
	}

	private Task selectFirstTask(Robot robot, DocumentFrame frame) throws Exception {
		SpreadSheet sheet = onEdt(frame::getTopSpreadSheet);
		int row = onEdt(() -> {
			SpreadSheetModel model = (SpreadSheetModel) sheet.getModel();
			for (int candidate = 0; candidate < sheet.getRowCount(); candidate++) {
				var node = model.getNodeForDisplayRow(candidate);
				if (node != null && node.getImpl() instanceof Task task && !task.isSummary()) return candidate;
			}
			throw new AssertionError("opened MPO has no visible task row");
		});
		Point point = onEdt(() -> {
			sheet.clearSelection();
			sheet.scrollRectToVisible(sheet.getCellRect(row, 1, true));
			Rectangle cell = sheet.getCellRect(row, 1, true);
			Point origin = sheet.getLocationOnScreen();
			return new Point(origin.x + cell.x + Math.max(2, cell.width / 2), origin.y + cell.y + cell.height / 2);
		});
		activateWindow(robot);
		point = onEdt(() -> {
			Rectangle cell = sheet.getCellRect(row, 1, true);
			Point origin = sheet.getLocationOnScreen();
			return new Point(origin.x + cell.x + Math.max(2, cell.width / 2), origin.y + cell.y + cell.height / 2);
		});
		robot.mouseMove(point.x, point.y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		GuiAcceptanceSupport.awaitInputAndEventQueueIdle(robot, "physical selection of first project task");
		assertEquals(row, onEdt(sheet::getSelectedRow), "Robot must select the target task row");
		Task selected = onEdt(() -> {
			var node = ((SpreadSheetModel) sheet.getModel()).getNodeForDisplayRow(sheet.getSelectedRow());
			return node != null && node.getImpl() instanceof Task task && !task.isSummary() ? task : null;
		});
		assertNotNull(selected, "selection must identify a real non-summary task");
		assertTrue(onEdt(selected::getUniqueId) > 0, "selection must resolve to a stable persisted task identity");
		return selected;
	}

	private void openTaskInformation(Robot robot, TaskIdentity selectedTask) throws Exception {
		Component taskTab = onEdt(() -> findText(window.getRibbonPanel(), "Task", "タスク"));
		assertNotNull(taskTab, "Task ribbon tab is missing");
		click(robot, taskTab);
		AbstractButton information = RibbonGuiSupport.findVisibleOrExpand(robot, window, "RibbonTaskInformation");
		GuiAcceptanceSupport.await(information::isEnabled, "Task Information command is not enabled for the selected task");
		click(robot, information);
		GuiAcceptanceSupport.await(FreshProcessMpoRestartGuiAcceptanceTest::taskInformationDialogUnchecked,
			"physical Task Properties > Information route did not open Task Information");
		TaskInformationDialog dialog = onEdt(FreshProcessMpoRestartGuiAcceptanceTest::taskInformationDialog);
		assertNotNull(dialog);
		String expectedTitle = Messages.getString("TaskInformationDialog.TaskInformation") + " - " + selectedTask.id();
		assertEquals(expectedTitle, onEdt(dialog::getTitle), "dialog must describe the selected task identity");
		assertTrue(onEdt(() -> containsText(dialog, selectedTask.name())),
			"Task Information must show the selected task's name");
		robot.keyPress(KeyEvent.VK_ESCAPE);
		robot.keyRelease(KeyEvent.VK_ESCAPE);
		GuiAcceptanceSupport.await(() -> !taskInformationDialogUnchecked(),
			"Task Information must close through the physical Escape route");
	}

	private static TaskInformationDialog taskInformationDialog() {
		for (Window candidate : Window.getWindows())
			if (candidate instanceof TaskInformationDialog dialog && dialog.isShowing()) return dialog;
		return null;
	}

	private static boolean containsText(Component root, String expected) {
		if (root instanceof JTextComponent text && expected.equals(text.getText())) return true;
		if (root instanceof Container container) {
			for (Component child : container.getComponents()) if (containsText(child, expected)) return true;
		}
		return false;
	}

	private static boolean taskInformationDialogUnchecked() {
		return onEdtUnchecked(() -> taskInformationDialog() != null);
	}

	private static int nameColumn(SpreadSheet sheet) {
		SpreadSheetModel model = (SpreadSheetModel) sheet.getModel();
		for (int modelColumn = 0; modelColumn < model.getColumnCount(); modelColumn++) {
			var field = model.getFieldInColumn(modelColumn);
			if (field != null && field.isNameField()) return sheet.convertColumnIndexToView(modelColumn);
		}
		throw new AssertionError("Task-name column is absent from the visible spreadsheet");
	}

	private static int rowForTask(SpreadSheet sheet, Task task) {
		SpreadSheetModel model = (SpreadSheetModel) sheet.getModel();
		for (int row = 0; row < sheet.getRowCount(); row++) {
			var node = model.getNodeForDisplayRow(row);
			if (node != null && node.getImpl() == task) return row;
		}
		throw new AssertionError("Task is absent from the visible spreadsheet: " + task.getUniqueId());
	}

	private static Task selectedTaskAt(SpreadSheet sheet, int row) {
		var node = ((SpreadSheetModel) sheet.getModel()).getNodeForDisplayRow(row);
		return node != null && node.getImpl() instanceof Task task ? task : null;
	}

	private static String renderedCellText(SpreadSheet sheet, int row, int column) {
		Component rendered = sheet.prepareRenderer(sheet.getCellRenderer(row, column), row, column);
		if (rendered instanceof NameCellComponent nameCell) return nameCell.getText();
		if (rendered instanceof javax.swing.JLabel label) return label.getText();
		return rendered.toString();
	}

	private static Point cellCenterOnScreen(SpreadSheet sheet, int row, int column) {
		Rectangle cell = sheet.getCellRect(row, column, true);
		Point origin = sheet.getLocationOnScreen();
		return new Point(origin.x + cell.x + Math.max(2, cell.width / 2), origin.y + cell.y + cell.height / 2);
	}

	private record TaskIdentity(long uniqueId, long id, String name) { }

	private void closeProject(Robot robot, Path path) throws Exception {
		clickImmediateBackstagePage(robot, "close");
		GuiAcceptanceSupport.await(() -> frameForPathUnchecked(path) == null,
			"project did not close cleanly");
	}

	private void activateWindow(Robot robot) throws Exception {
		SwingUtilities.invokeAndWait(() -> window.toFront());
		GuiAcceptanceSupport.await(() -> onEdtUnchecked(window::isActive), "application window did not activate");
		robot.waitForIdle();
	}

	private void click(Robot robot, Component component) throws Exception {
		Point point = onEdt(() -> {
			Rectangle bounds = component.getBounds();
			Point origin = component.getLocationOnScreen();
			return new Point(origin.x + Math.max(2, bounds.width / 2), origin.y + Math.max(2, bounds.height / 2));
		});
		activateWindow(robot);
		point = onEdt(() -> {
			Point origin = component.getLocationOnScreen();
			Rectangle bounds = component.getBounds();
			return new Point(origin.x + Math.max(2, bounds.width / 2), origin.y + Math.max(2, bounds.height / 2));
		});
		robot.mouseMove(point.x, point.y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		GuiAcceptanceSupport.awaitInputAndEventQueueIdle(robot, "physical click " + component.getName());
	}

	private static Robot robot() throws java.awt.AWTException {
		Robot robot = new GuiRobot();
		robot.setAutoDelay(45);
		return robot;
	}

	private static int outlineCountUnchecked(Project project) {
		try { return onEdt(() -> outlineRows(project).size()); }
		catch (Exception failure) { throw new IllegalStateException("Could not read outline on EDT", failure); }
	}

	private static boolean dirtyUnchecked(Project project) {
		try { return onEdt(project::needsSaving); }
		catch (Exception failure) { throw new IllegalStateException("Could not read dirty state on EDT", failure); }
	}

	private static String sha256(Path path) throws Exception {
		return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
	}

	private static List<String> outlineRows(Project project) {
		List<String> rows = new ArrayList<>();
		Object root = project.getTaskOutline().getHierarchy().getRoot();
		for (var iterator = project.getTaskOutline().iterator(); iterator.hasNext();) {
			com.microproject.grouping.core.Node node = (com.microproject.grouping.core.Node) iterator.next();
			if (node == root) continue;
			if (node.isVoid()) rows.add("");
			else if (node.getImpl() instanceof Task task) rows.add(task.getName());
		}
		while (!rows.isEmpty() && rows.getLast().isEmpty()) rows.removeLast();
		return rows;
	}

	private void writeNodeDiagnostics(String fileName, Project project) throws Exception {
		String report = onEdt(() -> {
			java.util.Map<String, Integer> classes = new java.util.TreeMap<>();
			int voidCount = 0;
			int taskCount = 0;
			int otherCount = 0;
			Object root = project.getTaskOutline().getHierarchy().getRoot();
			for (var iterator = project.getTaskOutline().iterator(); iterator.hasNext();) {
				com.microproject.grouping.core.Node node = (com.microproject.grouping.core.Node) iterator.next();
				if (node == root) continue;
				Object impl = node.getImpl();
				classes.merge("isVoid=" + node.isVoid() + "; node=" + node.getClass().getName()
					+ "; impl=" + (impl == null ? "<null>" : impl.getClass().getName()), 1, Integer::sum);
				if (node.isVoid()) voidCount++;
				else if (impl instanceof Task) taskCount++;
				else otherCount++;
			}
			StringBuilder details = new StringBuilder();
			classes.forEach((key, count) -> details.append(key).append(" => ").append(count).append(System.lineSeparator()));
			return "taskRows=" + taskCount + System.lineSeparator() + "voidRows=" + voidCount + System.lineSeparator()
				+ "otherRows=" + otherCount + System.lineSeparator() + details;
		});
		Files.writeString(workspace.resolve(fileName), report, StandardCharsets.UTF_8);
		System.out.println(fileName + System.lineSeparator() + report);
	}

	private void writeEditDiagnostic(String fileName, Project project, SpreadSheet sheet, int row, int column,
			Component editor, StringBuilder inputEvents) throws Exception {
		String state = onEdt(() -> {
			var node = ((SpreadSheetModel) sheet.getModel()).getNodeForDisplayRow(row);
			Object impl = node == null ? null : node.getImpl();
			Object fieldValue = row >= 0 && row < sheet.getRowCount() && column >= 0 && column < sheet.getColumnCount()
				? sheet.getValueAt(row, column) : "<outside-table>";
			Component focus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
			String editorText = editor instanceof JTextComponent text ? text.getText() : "<not-text-component>";
			String selected = sheet.getSelection().getActiveRow() + "," + sheet.getSelection().getActiveColumn();
			List<String> taskNames = new ArrayList<>();
			Object root = project.getTaskOutline().getHierarchy().getRoot();
			for (var iterator = project.getTaskOutline().iterator(); iterator.hasNext();) {
				var outlineNode = iterator.next();
				if (outlineNode != root && outlineNode.getImpl() instanceof Task task) taskNames.add(task.getName());
			}
			String tasks = String.join(" | ", taskNames);
			return "editing=" + sheet.isEditing() + " editRow=" + sheet.getEditingRow() + " editColumn=" + sheet.getEditingColumn()
				+ " selected=" + selected + " targetRow=" + row + " targetColumn=" + column + System.lineSeparator()
				+ "nodeClass=" + (node == null ? "<null>" : node.getClass().getName())
				+ " nodeVoid=" + (node != null && node.isVoid()) + " implClass=" + (impl == null ? "<null>" : impl.getClass().getName())
				+ " implName=" + (impl instanceof Task task ? task.getName() : "<not-task>") + System.lineSeparator()
				+ "fieldValue=" + fieldValue + " editorClass=" + (editor == null ? "<null>" : editor.getClass().getName())
				+ " editorText=" + editorText + " editorFocus=" + (editor != null && editor.isFocusOwner())
				+ " focusOwner=" + (focus == null ? "<null>" : focus.getClass().getName()) + System.lineSeparator()
				+ "projectTasks=" + tasks + System.lineSeparator();
		});
		Files.writeString(workspace.resolve(fileName), state + "inputEvents=" + System.lineSeparator() + inputEvents,
			StandardCharsets.UTF_8);
		System.out.println(fileName + System.lineSeparator() + state + "inputEvents=" + System.lineSeparator() + inputEvents);
	}

	private static String encodeRows(List<String> rows) {
		return rows.stream().map(value -> Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8)))
			.collect(java.util.stream.Collectors.joining(System.lineSeparator(), "", System.lineSeparator()));
	}

	private static List<String> decodeRows(List<String> rows) {
		return rows.stream().map(value -> new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8)).toList();
	}

	private static <T> T onEdt(java.util.concurrent.Callable<T> action) throws Exception {
		java.util.concurrent.FutureTask<T> task = new java.util.concurrent.FutureTask<>(action);
		SwingUtilities.invokeLater(task);
		return task.get(5, TimeUnit.SECONDS);
	}

	private static <T> T onEdtUnchecked(java.util.concurrent.Callable<T> action) {
		try { return onEdt(action); }
		catch (Exception failure) { throw new IllegalStateException("Could not inspect Swing state on EDT", failure); }
	}

	private static Component named(Component root, String name) {
		if (root == null) return null;
		if (name.equals(root.getName())) return root;
		if (root instanceof Container container) {
			for (Component child : container.getComponents()) {
				Component found = named(child, name);
				if (found != null) return found;
			}
		}
		return null;
	}

	private static Component findText(Component root, String... labels) {
		if (root instanceof AbstractButton button) {
			String text = button.getText();
			if (text != null && java.util.Arrays.stream(labels).anyMatch(label -> label.equalsIgnoreCase(text.trim()))) return button;
		}
		if (root instanceof Container container) {
			for (Component child : container.getComponents()) {
				Component found = findText(child, labels);
				if (found != null) return found;
			}
		}
		return null;
	}
}

