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
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
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
import javax.swing.WindowConstants;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.microproject.graphic.configuration.CellStyles;
import com.microproject.init.Init;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetModel;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.session.SessionFactory;
import com.microproject.job.JobQueue;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.testsupport.GuiRobot;
import com.microproject.ui.ribbon.RibbonDisplayMode;
import com.microproject.ui.ribbon.RibbonDisplayPreferences;
import com.microproject.util.Alert;
import com.microproject.util.Environment;
import com.microproject.util.SwingFileChooserProvider;
import com.microproject.util.UiServices;
import com.microproject.ui.util.SwingAlertPresenter;
import com.microproject.ui.util.SwingUiDispatcher;
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
		selectFirstTask(robot, frame);
		int rowsBefore = onEdt(() -> outlineRows(project).size());
		robot.keyPress(KeyEvent.VK_INSERT);
		robot.keyRelease(KeyEvent.VK_INSERT);
		GuiAcceptanceSupport.await(() -> outlineCountUnchecked(project) == rowsBefore + 1,
			"physical Insert on the imported POD did not add a row");
		GuiAcceptanceSupport.await(() -> dirtyUnchecked(project), "the POD insertion did not mark the project dirty");

		clickCommand(robot, "RibbonSaveProjectAs", "saveAs");
		GuiAcceptanceSupport.await(() -> Files.isRegularFile(mpo) && !dirtyUnchecked(project),
			"Save As did not publish and commit the MPO");
		assertEquals(mpo.toString(), project.getFileName(), "Save As must associate the project with the MPO target");
		List<String> rowsAfterSave = onEdt(() -> outlineRows(project));
		Files.writeString(workspace.resolve("expected-rows.txt"), encodeRows(rowsAfterSave), StandardCharsets.UTF_8);
		Files.writeString(workspace.resolve("phase-one.pid"), Long.toString(ProcessHandle.current().pid()), StandardCharsets.UTF_8);

		closeProject(robot, mpo);
		Files.writeString(workspace.resolve("phase-one.complete"), "passed", StandardCharsets.UTF_8);
		// The GUI test fork owns the application window. Its clean exit after this
		// test provides the process boundary for phase two; title-bar close calls
		// System.exit and would terminate Gradle's test worker before JUnit reports.
		assertTrue(window.isDisplayable(), "the GUI worker must remain alive to report phase one");
	}

	@Test
	void phaseTwoStartsInANewJvmAndEditsTheSavedMpo() throws Exception {
		assumeDesktop();
		workspace = workspace();
		Path mpo = workspace.resolve("fresh-process.mpo");
		Path expectedRowsFile = workspace.resolve("expected-rows.txt");
		assertTrue(Files.isRegularFile(mpo), "phase one must leave the MPO for the fresh JVM");
		assertTrue(Files.isRegularFile(expectedRowsFile), "phase one must persist its expected task outline");
		long phaseOnePid = Long.parseLong(Files.readString(workspace.resolve("phase-one.pid"), StandardCharsets.UTF_8));
		assertTrue(phaseOnePid != ProcessHandle.current().pid(), "phase two must run in a fresh GUI test JVM");

		createWindow("microProject — #967 fresh process reopen phase");
		installChooser(mpo, mpo);
		Robot robot = robot();
		openThroughBackstage(robot);
		DocumentFrame frame = awaitFrame(mpo);
		Project project = frame.getProject();
		List<String> phaseOneRows = decodeRows(Files.readAllLines(expectedRowsFile, StandardCharsets.UTF_8));
		assertEquals(phaseOneRows, onEdt(() -> outlineRows(project)), "fresh-JVM reopen must restore the POD-derived MPO rows");

		selectFirstTask(robot, frame);
		int rowsBefore = onEdt(() -> outlineRows(project).size());
		robot.keyPress(KeyEvent.VK_INSERT);
		robot.keyRelease(KeyEvent.VK_INSERT);
		GuiAcceptanceSupport.await(() -> outlineCountUnchecked(project) == rowsBefore + 1,
			"a fresh-JVM MPO must accept a physical Insert");
		GuiAcceptanceSupport.await(() -> dirtyUnchecked(project), "the fresh-JVM insertion must make the MPO dirty");
		clickCommand(robot, "RibbonSaveProject", null);
		GuiAcceptanceSupport.await(() -> !dirtyUnchecked(project), "the fresh-JVM edit did not save");
		List<String> finalRows = onEdt(() -> outlineRows(project));

		closeProject(robot, mpo);
		installChooser(mpo, mpo);
		openThroughBackstage(robot);
		DocumentFrame reloaded = awaitFrame(mpo);
		assertEquals(finalRows, onEdt(() -> outlineRows(reloaded.getProject())),
			"the edit made after the process restart must survive a subsequent save and reload");
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
		GuiAcceptanceSupport.await(() -> window.isShowing(), "GUI acceptance window did not show");
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
		GuiAcceptanceSupport.await(() -> named(window, "officeBackstageView") != null, "File tab did not open Backstage");
		clickNamed(robot, "officeBackstageNav-open");
		clickNamed(robot, "officeBackstageCommand-RibbonOpenProject");
	}

	private void clickCommand(Robot robot, String commandId, String page) throws Exception {
		clickTab(robot, "File", "ファイル");
		GuiAcceptanceSupport.await(() -> named(window, "officeBackstageView") != null, "File tab did not open Backstage");
		if (page != null) clickNamed(robot, "officeBackstageNav-" + page);
		clickNamed(robot, "officeBackstageCommand-" + commandId);
	}

	private void clickTab(Robot robot, String english, String japanese) throws Exception {
		Component tab = findText(window.getRibbonPanel(), english, japanese);
		assertNotNull(tab, "File tab is missing");
		click(robot, tab);
	}

	private void clickNamed(Robot robot, String name) throws Exception {
		GuiAcceptanceSupport.await(() -> named(window, name) instanceof AbstractButton button && button.isShowing() && button.isEnabled(),
			"Backstage command is not visible and enabled: " + name);
		click(robot, named(window, name));
	}

	private DocumentFrame awaitFrame(Path path) throws Exception {
		GuiAcceptanceSupport.await(() -> manager.findFrameForProjectFile(path.toString()) != null,
			"File/Open did not publish " + path.getFileName());
		return manager.findFrameForProjectFile(path.toString());
	}

	private void selectFirstTask(Robot robot, DocumentFrame frame) throws Exception {
		SpreadSheet sheet = frame.getTopSpreadSheet();
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
		assertTrue(selected.getUniqueId() > 0, "selection must resolve to a stable persisted task identity");
	}

	private void closeProject(Robot robot, Path path) throws Exception {
		clickCommand(robot, "RibbonCloseProject", null);
		GuiAcceptanceSupport.await(() -> manager.findFrameForProjectFile(path.toString()) == null,
			"project did not close cleanly");
	}

	private void activateWindow(Robot robot) throws Exception {
		SwingUtilities.invokeAndWait(() -> window.toFront());
		GuiAcceptanceSupport.await(() -> window.isActive(), "application window did not activate");
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
			var node = iterator.next();
			if (node != root) rows.add(node.getImpl() instanceof Task task ? task.getName() : "");
		}
		while (!rows.isEmpty() && rows.getLast().isEmpty()) rows.removeLast();
		return rows;
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

