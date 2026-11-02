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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
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

		String savedName = "MPO data retained after workspace conflict";
		SwingUtilities.invokeAndWait(() -> project.setName(savedName));
		GuiAcceptanceSupport.await(() -> onEdtUnchecked(project::needsSaving),
			"the project edit must make the physical Save operation eligible");
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
		assertEquals(savedName, reopened.getName(), "the real Save route must persist project data despite workspace conflict");
		Task persistedTask = reopened.getTaskList().stream().filter(task -> "workspace conflict task".equals(task.getName()))
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
