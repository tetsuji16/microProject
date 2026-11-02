/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

import com.microproject.collaboration.CollaborationSession;
import com.microproject.collaboration.CollaborationUserIdentity;
import com.microproject.exchange.MpoFileImporter;
import com.microproject.graphic.configuration.CellStyles;
import com.microproject.init.Init;
import com.microproject.job.JobQueue;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetModel;
import com.microproject.pm.graphic.spreadsheet.renderer.NameCellComponent;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.session.SessionFactory;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.testsupport.GuiRobot;
import com.microproject.ui.ribbon.RibbonDisplayMode;
import com.microproject.ui.ribbon.RibbonDisplayPreferences;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.Alert;
import com.microproject.util.Environment;
import com.microproject.util.SwingFileChooserProvider;
import com.microproject.util.UiDispatch;
import com.microproject.util.UiServices;
import com.microproject.ui.util.SwingAlertPresenter;
import com.microproject.ui.util.SwingUiDispatcher;
import com.microproject.temporary.TemporaryRoot;
import com.microproject.workspace.WorkspaceSetting;

/** Verifies that a physical Save reports a shared-workspace conflict and still saves the MPO. */
@ResourceLock(Resources.SYSTEM_PROPERTIES)
class WorkspaceConflictSaveGuiAcceptanceTest {
	private MainRibbonFrame window;
	private GraphicManager manager;
	private Project project;
	private CollaborationSession peerSession;
	private boolean previousRibbon;
	private boolean previousNewLook;
	private boolean previousClientSide;
	private UiServices.FileChooserProvider previousChooser;
	private JobQueue previousJobQueue;
	private RibbonDisplayMode previousRibbonMode;
	private String previousTemporaryRoot;
	private boolean environmentCaptured;

	@Test
	void physicalSaveReportsWorkspaceConflictAndRetainsProjectData(@TempDir Path tempDirectory) throws Exception {
		assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		captureEnvironment(tempDirectory);
		Path mpo = tempDirectory.resolve("workspace-conflict.mpo");
		writeMpo(mpo);
		createWindow("microProject — workspace conflict save acceptance");
		installOpenChooser(mpo);
		Robot robot = new GuiRobot();
		robot.setAutoDelay(45);
		openMpoThroughBackstage(robot);
		DocumentFrame frame = awaitDocument(mpo);
		project = frame.getProject();
		assertNotNull(project.getCollaborationSession(), "opening an MPO must initialize its collaboration workspace session");
		assertFalse(project.getCollaborationSession().hasWorkspaceConflictNotice(),
			"the initial workspace baseline must not already have a conflict");

		peerSession = new CollaborationSession(null, mpo.toString(), CollaborationUserIdentity.currentUserKey());
		assertEquals(null, peerSession.loadWorkspace(), "both session baselines must start without a saved workspace");
		assertTrue(peerSession.saveWorkspace(new ConflictWorkspace("peer-session-layout")),
			"the peer session must publish a distinct workspace after the UI session captured its baseline");
		peerSession = null;

		assertFalse(onEdt(project::needsSaving), "the opened MPO fixture must begin clean before the user edit");
		Task task = onEdt(() -> project.getTaskList().stream()
			.filter(candidate -> "workspace conflict task".equals(candidate.getName())).findFirst().orElseThrow());
		String savedTaskName = "workspace conflict task saved after conflict";
		editTaskName(robot, frame, task, savedTaskName);
		GuiAcceptanceSupport.await(() -> onEdtUnchecked(project::needsSaving),
			"the physical task-name edit must make the Save operation eligible");
		clickFilePageCommand(robot, "save");
		String savedWarning = "The project was saved, but workspace recovery remains unresolved. Both workspace copies were preserved.";
		GuiAcceptanceSupport.await(() -> onEdtUnchecked(() -> !project.needsSaving()
			&& containsText(frame.statusBar, savedWarning)),
			"physical Save did not finish the MPO write and surface the unresolved workspace conflict in the status bar");
		assertFalse(onEdt(project::needsSaving), "workspace conflict must not leave the MPO edit unsaved");

		Project reopened;
		try (var input = Files.newInputStream(mpo)) {
			reopened = new MpoFileImporter().loadProject(input);
		}
		assertEquals("Workspace conflict fixture", reopened.getName(), "the real Save route must retain the MPO project");
		Task persistedTask = reopened.getTaskList().stream().filter(candidate -> savedTaskName.equals(candidate.getName()))
			.findFirst().orElseThrow(() -> new AssertionError("MPO save lost its original task data"));
		assertTrue(persistedTask instanceof NormalTask);
	}

	@AfterEach
	void restoreEnvironment() throws Exception {
		if (project != null && project.getCollaborationSession() != null) {
			project.getCollaborationSession().stop();
			project.setCollaborationSession(null);
		}
		if (peerSession != null) peerSession.stop();
		if (manager != null) SwingUtilities.invokeAndWait(manager::cleanUp);
		if (window != null) SwingUtilities.invokeAndWait(window::dispose);
		if (environmentCaptured) {
			Alert.setPresenter(null);
			Environment.setRibbonUI(previousRibbon);
			Environment.setNewLook(previousNewLook);
			Environment.setClientSide(previousClientSide);
			UiServices.setFileChooserProvider(previousChooser);
			SessionFactory.getInstance().setJobQueue(previousJobQueue);
			RibbonDisplayPreferences.save(previousRibbonMode);
			UiDispatch.setDispatcher(null);
			if (previousTemporaryRoot == null) System.clearProperty(TemporaryRoot.ROOT_PROPERTY);
			else System.setProperty(TemporaryRoot.ROOT_PROPERTY, previousTemporaryRoot);
		}
	}

	private void captureEnvironment(Path tempDirectory) {
		previousRibbon = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		previousClientSide = Environment.isClientSide();
		previousChooser = UiServices.getFileChooserProvider();
		previousJobQueue = SessionFactory.getInstance().getJobQueue();
		previousRibbonMode = RibbonDisplayPreferences.load();
		previousTemporaryRoot = System.getProperty(TemporaryRoot.ROOT_PROPERTY);
		System.setProperty(TemporaryRoot.ROOT_PROPERTY, tempDirectory.resolve("isolated-temp").toString());
		environmentCaptured = true;
		Environment.setClientSide(true);
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		UiDispatch.setDispatcher(new SwingUiDispatcher());
		Alert.setPresenter(new SwingAlertPresenter());
		RibbonDisplayPreferences.save(RibbonDisplayMode.ALWAYS_SHOW);
	}

	private void createWindow(String title) throws Exception {
		Init.initialize();
		assertNotNull(CellStyles.getInstance(), "application cell styles must be initialized before creating the window");
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

	private void installOpenChooser(Path mpo) {
		SwingFileChooserProvider fallback = new SwingFileChooserProvider();
		UiServices.setFileChooserProvider(new UiServices.FileChooserProvider() {
			@Override public String chooseFileName(boolean save, String selectedFileName, Object parent) {
				return save ? fallback.chooseFileName(true, selectedFileName, parent) : mpo.toString();
			}
			@Override public List<String> chooseFileNames(boolean save, String selectedFileName, Object parent) {
				return save ? List.of() : List.of(mpo.toString());
			}
		});
	}

	private void openMpoThroughBackstage(Robot robot) throws Exception {
		click(robot, onEdt(() -> findText(window.getRibbonPanel(), "File", "ファイル")));
		clickNamed(robot, "officeBackstageNav-open");
		clickNamed(robot, "officeBackstageCommand-RibbonOpenProject");
	}

	private DocumentFrame awaitDocument(Path mpo) throws Exception {
		GuiAcceptanceSupport.await(() -> onEdtUnchecked(() -> {
			DocumentFrame candidate = manager.findFrameForProjectFile(mpo.toString());
			return candidate != null && candidate.getProject() != null
				&& candidate.getProject().getCollaborationSession() != null;
		}), "File/Open did not initialize the MPO document and its collaboration session");
		return onEdt(() -> manager.findFrameForProjectFile(mpo.toString()));
	}

	private void clickFilePageCommand(Robot robot, String page) throws Exception {
		click(robot, onEdt(() -> findText(window.getRibbonPanel(), "File", "ファイル")));
		clickNamed(robot, "officeBackstageNav-" + page);
	}

	private void editTaskName(Robot robot, DocumentFrame frame, Task task, String value) throws Exception {
		SpreadSheet sheet = onEdt(frame::getTopSpreadSheet);
		int[] cell = onEdt(() -> new int[] { rowForTask(sheet, task), nameColumn(sheet) });
		Point point = onEdt(() -> cellCenterOnScreen(sheet, cell[0], cell[1]));
		SwingUtilities.invokeAndWait(window::toFront);
		GuiAcceptanceSupport.await(() -> onEdtUnchecked(window::isActive), "application window did not activate for task edit");
		point = onEdt(() -> cellCenterOnScreen(sheet, cell[0], cell[1]));
		robot.mouseMove(point.x, point.y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		GuiAcceptanceSupport.awaitInputAndEventQueueIdle(robot, "physical selection of task name cell");
		assertEquals(cell[0], onEdt(sheet::getSelectedRow), "Robot must select the task-name row");
		robot.keyPress(KeyEvent.VK_F2);
		robot.keyRelease(KeyEvent.VK_F2);
		GuiAcceptanceSupport.await(() -> onEdtUnchecked(sheet::isEditing), "physical F2 did not start task-name editing");
		JTextComponent input = onEdt(() -> (JTextComponent) ((NameCellComponent) sheet.getEditorComponent()).getTextComponent());
		GuiAcceptanceSupport.await(() -> onEdtUnchecked(input::isFocusOwner), "task-name editor did not receive focus");
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_A);
		robot.keyRelease(KeyEvent.VK_A);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		for (char character : value.toCharArray()) {
			int keyCode = character == ' ' ? KeyEvent.VK_SPACE : KeyEvent.getExtendedKeyCodeForChar(character);
			if (keyCode == KeyEvent.VK_UNDEFINED) throw new AssertionError("No physical key for character: " + character);
			robot.keyPress(keyCode);
			robot.keyRelease(keyCode);
		}
		robot.waitForIdle();
		assertEquals(value, onEdt(input::getText), "Robot must deliver the complete replacement task name");
		robot.keyPress(KeyEvent.VK_ENTER);
		robot.keyRelease(KeyEvent.VK_ENTER);
		GuiAcceptanceSupport.await(() -> onEdtUnchecked(() -> !sheet.isEditing() && value.equals(task.getName())
			&& value.equals(renderedTaskName(sheet, task))),
			"physical task-name input did not commit to both model and rendered spreadsheet");
	}

	private static int rowForTask(SpreadSheet sheet, Task task) {
		SpreadSheetModel model = (SpreadSheetModel) sheet.getModel();
		for (int row = 0; row < sheet.getRowCount(); row++) {
			var node = model.getNodeForDisplayRow(row);
			if (node != null && node.getImpl() == task) return row;
		}
		throw new AssertionError("Task is not visible in the task spreadsheet: " + task.getUniqueId());
	}

	private static int nameColumn(SpreadSheet sheet) {
		SpreadSheetModel model = (SpreadSheetModel) sheet.getModel();
		for (int modelColumn = 0; modelColumn < model.getColumnCount(); modelColumn++) {
			var field = model.getFieldInColumn(modelColumn);
			if (field != null && field.isNameField()) return sheet.convertColumnIndexToView(modelColumn);
		}
		throw new AssertionError("Task-name column is absent from the visible spreadsheet");
	}

	private static Point cellCenterOnScreen(SpreadSheet sheet, int row, int column) {
		sheet.scrollRectToVisible(sheet.getCellRect(row, column, true));
		Rectangle cell = sheet.getCellRect(row, column, true);
		Point origin = sheet.getLocationOnScreen();
		return new Point(origin.x + cell.x + Math.max(2, cell.width / 2), origin.y + cell.y + cell.height / 2);
	}

	private static String renderedTaskName(SpreadSheet sheet, Task task) {
		int row = rowForTask(sheet, task);
		int column = nameColumn(sheet);
		Component rendered = sheet.prepareRenderer(sheet.getCellRenderer(row, column), row, column);
		if (rendered instanceof NameCellComponent nameCell) return nameCell.getText();
		if (rendered instanceof JLabel label) return label.getText();
		return rendered.toString();
	}

	private void clickNamed(Robot robot, String name) throws Exception {
		GuiAcceptanceSupport.await(() -> onEdtUnchecked(() -> {
			Component component = named(window, name);
			return component instanceof AbstractButton button && button.isShowing() && button.isEnabled();
		}), "Backstage command is not visible and enabled: " + name);
		click(robot, onEdt(() -> named(window, name)));
	}

	private void click(Robot robot, Component component) throws Exception {
		assertNotNull(component, "physical target component is missing");
		SwingUtilities.invokeAndWait(window::toFront);
		GuiAcceptanceSupport.await(() -> onEdtUnchecked(window::isActive), "application window did not activate");
		Point point = onEdt(() -> {
			Rectangle bounds = component.getBounds();
			Point origin = component.getLocationOnScreen();
			return new Point(origin.x + Math.max(2, bounds.width / 2), origin.y + Math.max(2, bounds.height / 2));
		});
		robot.mouseMove(point.x, point.y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		GuiAcceptanceSupport.awaitInputAndEventQueueIdle(robot, "physical click " + component.getName());
	}

	private static boolean containsText(Component root, String expected) {
		if (root instanceof JLabel label && expected.equals(label.getText())) return true;
		if (root instanceof Container container)
			for (Component child : container.getComponents()) if (containsText(child, expected)) return true;
		return false;
	}

	private static Component named(Component root, String name) {
		if (root == null) return null;
		if (name.equals(root.getName())) return root;
		if (root instanceof Container container)
			for (Component child : container.getComponents()) {
				Component found = named(child, name);
				if (found != null) return found;
			}
		return null;
	}

	private static Component findText(Component root, String... labels) {
		if (root instanceof AbstractButton button && button.getText() != null
				&& java.util.Arrays.stream(labels).anyMatch(label -> label.equalsIgnoreCase(button.getText().trim()))) return button;
		if (root instanceof Container container)
			for (Component child : container.getComponents()) {
				Component found = findText(child, labels);
				if (found != null) return found;
			}
		return null;
	}

	private static boolean onEdtUnchecked(Callable<Boolean> action) {
		try { return onEdt(action); }
		catch (Exception failure) { throw new IllegalStateException("Could not inspect Swing state on EDT", failure); }
	}

	private static <T> T onEdt(Callable<T> action) throws Exception {
		FutureTask<T> task = new FutureTask<>(action);
		SwingUtilities.invokeLater(task);
		return task.get(5, TimeUnit.SECONDS);
	}

	private static void writeMpo(Path target) throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project source = Project.createProject(ResourcePool.createRourcePool("workspace-conflict-pool", undo), undo);
		source.initialize(false, false);
		source.setName("Workspace conflict fixture");
		NormalTask task = source.createScriptedTask();
		task.setName("workspace conflict task");
		source.recalculate();
		MpoFileImporter exporter = new MpoFileImporter();
		exporter.setProject(source);
		exporter.setFileName(target.toString());
		exporter.exportFile();
		assertTrue(Files.isRegularFile(target), "fixed MPO fixture must be written for the GUI Open route");
	}

	private record ConflictWorkspace(String marker) implements WorkspaceSetting {
		private static final long serialVersionUID = 1L;
	}
}
