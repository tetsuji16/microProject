/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.JList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.exchange.MpoFileImporter;
import com.microproject.application.RecentProjectStore;
import com.microproject.pm.graphic.frames.workspace.FrameManager;
import com.microproject.pm.graphic.frames.workspace.FrameManager.WindowArrangement;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.job.JobQueue;
import com.microproject.session.SessionFactory;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.Environment;
import com.microproject.util.FilePathUtils;
import com.microproject.util.UiDispatch;
import com.microproject.util.UiServices;
import com.microproject.util.SwingFileChooserProvider;
import com.formdev.flatlaf.ui.FlatNativeWindowsLibrary;
import com.microproject.ui.util.SwingUiDispatcher;

/** GUI-MSP-OPEN-01: one Open command expands a multiple-file selection into independent project documents. */
class MultipleLocalProjectOpenGuiAcceptanceTest {
	private MainRibbonFrame window;
	private GraphicManager manager;
	private Path firstFile;
	private Path secondFile;
	private Path existingFile;
	private Path fixtureDirectory;
	private UiServices.FileChooserProvider previousChooser;
	private boolean previousStandalone;
	private boolean previousClientSide;
	private boolean previousRibbonUi;
	private boolean previousNewLook;
	private String previousSystemChooserProperty;
	private Robot nativeChooserRobot;
	private AtomicBoolean nativeChooserReturned;
	private JobQueue previousJobQueue;
	private boolean previousJobQueueCaptured;

	@AfterEach
	void cleanUp() throws Exception {
		UiServices.setFileChooserProvider(previousChooser);
		if (nativeChooserRobot != null && nativeChooserReturned != null && !nativeChooserReturned.get()) {
			nativeChooserRobot.keyPress(java.awt.event.KeyEvent.VK_ESCAPE);
			nativeChooserRobot.keyRelease(java.awt.event.KeyEvent.VK_ESCAPE);
			nativeChooserRobot.waitForIdle();
		}
		if (previousSystemChooserProperty == null) System.clearProperty("flatlaf.useSystemFileChooser");
		else System.setProperty("flatlaf.useSystemFileChooser", previousSystemChooserProperty);
		if (window != null)
			GuiAcceptanceSupport.runOnEdtWithTimeout(window::dispose, "Multiple-project main window disposal");
		if (manager != null)
			GuiAcceptanceSupport.runOnEdtWithTimeout(manager::cleanUp, "Multiple-project manager cleanup");
		deleteMpoSidecars(firstFile);
		deleteMpoSidecars(secondFile);
		if (firstFile != null) Files.deleteIfExists(firstFile);
		if (secondFile != null) Files.deleteIfExists(secondFile);
		deleteMpoSidecars(existingFile);
		if (existingFile != null) Files.deleteIfExists(existingFile);
		if (fixtureDirectory != null) {
			for (int attempt = 0; attempt < 5; attempt++) {
				try (var children = Files.list(fixtureDirectory)) {
					for (Path child : children.toList()) Files.deleteIfExists(child);
				}
				try {
					Files.deleteIfExists(fixtureDirectory);
					break;
				} catch (java.nio.file.DirectoryNotEmptyException lateSidecar) {
					Thread.sleep(100);
				}
			}
		}
		if (previousJobQueueCaptured) SessionFactory.getInstance().setJobQueue(previousJobQueue);
		Environment.setStandAlone(previousStandalone);
		Environment.setClientSide(previousClientSide);
		Environment.setRibbonUI(previousRibbonUi);
		Environment.setNewLook(previousNewLook);
	}

	/** GUI-MSP-OPEN-05: the real Windows native chooser returns both selected MPO files without replacing an open project. */
	@Test
	void nativeFileOpenChooserSelectsTwoProjectsAndKeepsExistingProject() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Assumptions.assumeTrue(FlatNativeWindowsLibrary.isLoaded(), "The real Windows native chooser requires FlatLaf's Windows native library.");
		previousChooser = UiServices.getFileChooserProvider();
		previousSystemChooserProperty = System.getProperty("flatlaf.useSystemFileChooser");
		previousStandalone = Environment.getStandAlone();
		previousClientSide = Environment.isClientSide();
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		System.clearProperty("flatlaf.useSystemFileChooser");
		Environment.setStandAlone(true);
		Environment.setClientSide(true);
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		UiDispatch.setDispatcher(new SwingUiDispatcher());
		previousJobQueue = SessionFactory.getInstance().getJobQueue();
		previousJobQueueCaptured = true;
		fixtureDirectory = Files.createTempDirectory("msp-native-multi-open-");
		firstFile = fixtureDirectory.resolve("Native Alpha.mpo");
		secondFile = fixtureDirectory.resolve("Native Beta.mpo");
		existingFile = fixtureDirectory.resolve("Native Existing.mpo");
		writeProjectWithTask(firstFile, "Native Multi Alpha");
		writeProjectWithTask(secondFile, "Native Multi Beta");
		writeProject(existingFile, "Native Existing Project");
		AtomicBoolean chooserStarted = new AtomicBoolean();
		nativeChooserReturned = new AtomicBoolean();
		AtomicBoolean chooserReturned = nativeChooserReturned;
		AtomicReference<List<String>> nativeSelection = new AtomicReference<>(List.of());
		SwingFileChooserProvider nativeProvider = new SwingFileChooserProvider();
		SwingUtilities.invokeAndWait(() -> {
			window = new MainRibbonFrame("microProject — native multi-file Open GUI acceptance", null);
			manager = new GraphicManager(window);
			window.setGraphicManager(manager);
			manager.initView();
			SessionFactory.getInstance().setJobQueue(manager.getJobQueue());
			window.setSize(1020, 620);
			window.setLocationByPlatform(true);
			window.setAlwaysOnTop(true);
			window.setVisible(true);
			manager.openLocalProjectsSequentially(new String[] { existingFile.toString() });
		});
		DocumentFrame existing = manager.findFrameForProjectFile(existingFile.toString());
		GuiAcceptanceSupport.await(() -> manager.findFrameForProjectFile(existingFile.toString()) != null,
			"the existing project did not open before native File/Open");
		existing = manager.findFrameForProjectFile(existingFile.toString());
		Project existingProject = existing.getProject();
		existingProject.setDirty(true);
		UiServices.setFileChooserProvider(new UiServices.FileChooserProvider() {
			@Override public String chooseFileName(boolean save, String selectedFileName, Object parent) { return null; }
			@Override public List<String> chooseFileNames(boolean save, String selectedFileName, Object parent) {
				chooserStarted.set(true);
				try {
					List<String> selected = nativeProvider.chooseFileNames(save, selectedFileName, parent);
					nativeSelection.set(selected == null ? List.of() : List.copyOf(selected));
					return selected;
				} finally { chooserReturned.set(true); }
			}
		});
		Robot robot = new com.microproject.testsupport.GuiRobot();
		nativeChooserRobot = robot;
		robot.setAutoDelay(45);
		activateMainWindow(robot);
		clickFileOpen(robot);
		GuiAcceptanceSupport.await(chooserStarted::get, "the native File/Open chooser was not invoked");
		robot.delay(700);
		pasteNativeChooserText(robot, "\"" + firstFile + "\" \"" + secondFile + "\"");
		robot.keyPress(java.awt.event.KeyEvent.VK_ENTER);
		robot.keyRelease(java.awt.event.KeyEvent.VK_ENTER);
		robot.waitForIdle();
		GuiAcceptanceSupport.await(chooserReturned::get, "the native multi-file chooser did not return");
		List<String> selectedPaths = nativeSelection.get();
		assertEquals(2, selectedPaths.size(), "the native chooser must return a two-file selection");
		assertTrue(selectedPaths.stream().anyMatch(path -> FilePathUtils.sameFileIdentity(path, firstFile.toString())),
			"native chooser selection omitted Alpha; expected " + firstFile + ", actual " + selectedPaths);
		assertTrue(selectedPaths.stream().anyMatch(path -> FilePathUtils.sameFileIdentity(path, secondFile.toString())),
			"native chooser selection omitted Beta; expected " + secondFile + ", actual " + selectedPaths);
		FrameManager frames = manager.getFrameManager();
		GuiAcceptanceSupport.await(() -> frames.getAllFrames().size() == 3,
			"native multi-file Open did not add two independent projects");
		assertTrue(manager.findFrameForProjectFile(existingFile.toString()) != null, "native multi-file Open replaced the existing project");
		assertTrue(existingProject.needsSaving(), "native multi-file Open changed the existing project's dirty state");
		assertProjectTitleAndTask(firstFile, "Native Multi Alpha");
		assertProjectTitleAndTask(secondFile, "Native Multi Beta");
		GuiAcceptanceSupport.await(this::usesSeparateDesktopWindows,
			"the two selected projects were not presented in separate desktop windows");
		assertEquals(3, frames.getAllFrames().size(), "the existing project must remain open alongside both selected projects");
	}

	private void activateMainWindow(Robot robot) throws Exception {
		java.awt.Point point = window.getLocationOnScreen();
		robot.mouseMove(point.x + window.getWidth() / 2, point.y + 30);
		robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
	}

	private void clickFileOpen(Robot robot) throws Exception {
		Component fileTab = findByTextOnEdt(window.getRibbonPanel(), "File", "ファイル");
		click(robot, fileTab);
		Component openPage = awaitNamed(window, "officeBackstageNav-open", "File tab did not expose Backstage Open");
		click(robot, openPage);
		Component openTile = awaitNamed(window, "officeBackstageCommand-RibbonOpenProject",
			"Backstage Open page did not expose the Open tile");
		click(robot, openTile);
	}

	private static void pasteNativeChooserText(Robot robot, String value) throws Exception {
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(value), null);
		robot.keyPress(java.awt.event.KeyEvent.VK_CONTROL);
		robot.keyPress(java.awt.event.KeyEvent.VK_A);
		robot.keyRelease(java.awt.event.KeyEvent.VK_A);
		robot.keyRelease(java.awt.event.KeyEvent.VK_CONTROL);
		robot.keyPress(java.awt.event.KeyEvent.VK_ALT);
		robot.keyPress(java.awt.event.KeyEvent.VK_N);
		robot.keyRelease(java.awt.event.KeyEvent.VK_N);
		robot.keyRelease(java.awt.event.KeyEvent.VK_ALT);
		robot.keyPress(java.awt.event.KeyEvent.VK_CONTROL);
		robot.keyPress(java.awt.event.KeyEvent.VK_V);
		robot.keyRelease(java.awt.event.KeyEvent.VK_V);
		robot.keyRelease(java.awt.event.KeyEvent.VK_CONTROL);
	}

	private static Component findByName(Component root, String name) {
		if (name.equals(root.getName())) return root;
		if (root instanceof java.awt.Container container) for (Component child : container.getComponents()) {
			Component result = findByName(child, name);
			if (result != null) return result;
		}
		return null;
	}

	private static Component findByNameOnEdt(Component root, String name) throws Exception {
		Component[] result = new Component[1];
		SwingUtilities.invokeAndWait(() -> result[0] = findByName(root, name));
		return result[0];
	}

	private static Component awaitNamed(Component root, String name, String message) throws Exception {
		long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
		Component component;
		do {
			component = findByNameOnEdt(root, name);
			if (component != null && component.isShowing()) return component;
			Thread.sleep(50);
		} while (System.nanoTime() < deadline);
		throw new AssertionError(message);
	}

	private static Component findByTextOnEdt(Component root, String... labels) throws Exception {
		Component[] result = new Component[1];
		SwingUtilities.invokeAndWait(() -> result[0] = findByText(root, labels));
		return result[0];
	}

	private static Component findByText(Component root, String... labels) {
		if (root instanceof javax.swing.AbstractButton button)
			for (String label : labels) if (label.equals(button.getText())) return button;
		if (root instanceof java.awt.Container container) for (Component child : container.getComponents()) {
			Component result = findByText(child, labels);
			if (result != null) return result;
		}
		return null;
	}

	private static void click(Robot robot, Component component) throws Exception {
		if (component == null) throw new AssertionError("Required visible Open control was not found");
		java.awt.Point location = component.getLocationOnScreen();
		robot.mouseMove(location.x + component.getWidth() / 2, location.y + component.getHeight() / 2);
		robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
	}

	private void assertProjectTitleAndTask(Path file, String expectedName) {
		DocumentFrame frame = manager.findFrameForProjectFile(file.toString());
		assertTrue(frame != null, "project file was not registered: " + file);
		assertEquals(expectedName, frame.getProject().getName());
		assertTrue(frame.getProject().getTaskList().stream().anyMatch(task -> expectedName.equals(task.getName())),
			"loaded project does not contain its fixture task: " + expectedName);
	}

	@Test
	void oneGuiOpenSelectionCreatesTwoIndependentMpoDocuments() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		previousChooser = UiServices.getFileChooserProvider();
		previousStandalone = Environment.getStandAlone();
		previousClientSide = Environment.isClientSide();
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setStandAlone(true);
		Environment.setClientSide(true);
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		UiDispatch.setDispatcher(new SwingUiDispatcher());
		firstFile = Files.createTempFile("msp-open-alpha-", ".mpo");
		secondFile = Files.createTempFile("msp-open-beta-", ".mpo");
		writeProject(firstFile, "Multiple Open Alpha");
		writeProject(secondFile, "Multiple Open Beta");
		SwingUtilities.invokeAndWait(() -> {
			window = new MainRibbonFrame("microProject — Multiple local project Open GUI acceptance", null);
			manager = new GraphicManager(window);
			window.setGraphicManager(manager);
			manager.initView();
			// FlatLafSupport installs the native chooser during initView(). Install
			// the deterministic provider afterwards, otherwise the Robot test opens
			// a real Windows file dialog and blocks the EDT indefinitely.
			UiServices.setFileChooserProvider(new UiServices.FileChooserProvider() {
				@Override public String chooseFileName(boolean save, String selectedFileName, Object parent) { return null; }
				@Override public List<String> chooseFileNames(boolean save, String selectedFileName, Object parent) {
					return save ? List.of() : List.of(firstFile.toString(), secondFile.toString());
				}
			});
			SessionFactory.getInstance().setJobQueue(manager.getJobQueue());
			window.setSize(1020, 620);
			window.setLocationByPlatform(true);
			window.setAlwaysOnTop(true);
			window.setVisible(true);
			manager.openLocalProject();
		});
		FrameManager frames = manager.getFrameManager();
		GuiAcceptanceSupport.await(() -> frames.getAllFrames().size() == 2,
				"a single multiple-file Open selection did not create two project documents");
		assertTrue(manager.findFrameForProjectFile(firstFile.toString()) != null, () -> "opened files: " + openedFileNames(frames));
		assertTrue(manager.findFrameForProjectFile(secondFile.toString()) != null, () -> "opened files: " + openedFileNames(frames));
		GuiAcceptanceSupport.await(() -> usesSeparateDesktopWindows(),
				"two selected projects were not presented as separate desktop windows");
		SwingUtilities.invokeAndWait(() -> frames.arrangeAll(WindowArrangement.TILE));
		capture(new com.microproject.testsupport.GuiRobot(), "msp-multiple-local-project-open.png");
	}

	/** GUI-MSP-OPEN-02: command-line startup must register every supplied project, not just argv[0]. */
	@Test
	void commandLineProjectListCreatesTwoIndependentMpoDocuments() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		previousChooser = UiServices.getFileChooserProvider();
		previousStandalone = Environment.getStandAlone();
		previousClientSide = Environment.isClientSide();
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setStandAlone(true);
		Environment.setClientSide(true);
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		firstFile = Files.createTempFile("msp-command-line-alpha-", ".mpo");
		secondFile = Files.createTempFile("msp-command-line-beta-", ".mpo");
		writeProject(firstFile, "Command line Alpha");
		writeProject(secondFile, "Command line Beta");

		SwingUtilities.invokeAndWait(() -> {
			window = new MainRibbonFrame("microProject — command-line project Open GUI acceptance", null);
			manager = new GraphicManager(window);
			window.setGraphicManager(manager);
			manager.initView();
			SessionFactory.getInstance().setJobQueue(manager.getJobQueue());
			window.setSize(1020, 620);
			window.setLocationByPlatform(true);
			window.setAlwaysOnTop(true);
			window.setVisible(true);
			ApplicationStartupFactory startup = new ApplicationStartupFactory(new String[] {
				"--fileNames", firstFile.toString(), secondFile.toString() });
			startup.doStartupAction(manager, startup.projectUrls, false);
		});
		FrameManager frames = manager.getFrameManager();
		GuiAcceptanceSupport.await(() -> frames.getAllFrames().size() == 2,
				"a command-line project list did not create two project documents");
		assertTrue(manager.findFrameForProjectFile(firstFile.toString()) != null, () -> "opened files: " + openedFileNames(frames));
		assertTrue(manager.findFrameForProjectFile(secondFile.toString()) != null, () -> "opened files: " + openedFileNames(frames));
		GuiAcceptanceSupport.await(() -> usesSeparateDesktopWindows(),
				"command-line projects were not presented as separate desktop windows");
		SwingUtilities.invokeAndWait(() -> frames.arrangeAll(WindowArrangement.TILE));
		capture(new com.microproject.testsupport.GuiRobot(), "msp-command-line-multiple-project-open.png");
	}

	/** GUI-MSP-OPEN-04: selecting a recent project adds it without replacing a dirty document. */
	@Test
	void recentProjectSelectionKeepsExistingDirtyDocumentOpen() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		previousChooser = UiServices.getFileChooserProvider();
		previousStandalone = Environment.getStandAlone();
		previousClientSide = Environment.isClientSide();
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setStandAlone(true);
		Environment.setClientSide(true);
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		firstFile = Files.createTempFile("msp-recent-existing-", ".mpo");
		secondFile = Files.createTempFile("msp-recent-selected-", ".mpo");
		writeProject(firstFile, "Recent Existing A");
		writeProject(secondFile, "Recent Selected B");
		new RecentProjectStore().recordOpened(secondFile.toString());

		SwingUtilities.invokeAndWait(() -> {
			window = new MainRibbonFrame("microProject — Recent project GUI acceptance", null);
			manager = new GraphicManager(window);
			window.setGraphicManager(manager);
			manager.initView();
			SessionFactory.getInstance().setJobQueue(manager.getJobQueue());
			window.setSize(1020, 620);
			window.setLocationByPlatform(true);
			window.setAlwaysOnTop(true);
			window.setVisible(true);
			manager.openLocalProjectsSequentially(new String[] { firstFile.toString() });
		});
		FrameManager frames = manager.getFrameManager();
		GuiAcceptanceSupport.await(() -> manager.findFrameForProjectFile(firstFile.toString()) != null,
				"the existing project did not open before Recent selection");
		DocumentFrame existing = manager.findFrameForProjectFile(firstFile.toString());
		Project existingProject = existing.getProject();
		existingProject.setDirty(true);

		SwingUtilities.invokeLater(() -> manager.new RecentProjectsAction().actionPerformed(null));
		GuiAcceptanceSupport.await(() -> findVisibleListForPath(secondFile) != null,
				"Recent Projects did not show the recorded project");
		JList<?> recentList = findVisibleListForPath(secondFile);
		SwingUtilities.invokeAndWait(() -> {
			recentList.setSelectedValue(new RecentProjectStore().entries().stream()
				.filter(entry -> entry.path().equals(secondFile.toAbsolutePath())).findFirst().orElseThrow(), true);
			recentList.dispatchEvent(new java.awt.event.KeyEvent(recentList,
				java.awt.event.KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0,
				java.awt.event.KeyEvent.VK_ENTER, '\n'));
		});
		GuiAcceptanceSupport.await(() -> manager.findFrameForProjectFile(secondFile.toString()) != null,
				"Recent selection did not open the selected project");
		assertTrue(manager.findFrameForProjectFile(firstFile.toString()) != null,
				"Recent selection must not close the existing project");
		assertTrue(existingProject.needsSaving(), "Recent selection must preserve the existing dirty state");
		assertEquals(2, frames.getAllFrames().size(), "Recent selection must add a document frame");
		SwingUtilities.invokeAndWait(() -> frames.arrangeAll(WindowArrangement.TILE));
		capture(new com.microproject.testsupport.GuiRobot(), "msp-recent-project-keeps-existing-document.png");
	}

	private static JList<?> findVisibleListForPath(Path path) {
		for (Window window : Window.getWindows()) {
			if (!window.isShowing()) continue;
			JList<?> list = findList(window);
			if (list != null) for (int i = 0; i < list.getModel().getSize(); i++) {
				Object value = list.getModel().getElementAt(i);
				if (value instanceof RecentProjectStore.Entry entry && entry.path().equals(path.toAbsolutePath())) return list;
			}
		}
		return null;
	}

	private static JList<?> findList(java.awt.Container container) {
		for (Component component : container.getComponents()) {
			if (component instanceof JList<?> list) return list;
			if (component instanceof java.awt.Container child) {
				JList<?> list = findList(child);
				if (list != null) return list;
			}
		}
		return null;
	}

	/** GUI-MSP-OPEN-03: desktop file drops use the same independent-document registration route. */
	@Test
	void fileDropCreatesTwoIndependentMpoDocuments() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		previousChooser = UiServices.getFileChooserProvider();
		previousStandalone = Environment.getStandAlone();
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setStandAlone(true);
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		firstFile = Files.createTempFile("msp-file-drop-alpha-", ".mpo");
		secondFile = Files.createTempFile("msp-file-drop-beta-", ".mpo");
		writeProject(firstFile, "File drop Alpha");
		writeProject(secondFile, "File drop Beta");
		boolean[] accepted = new boolean[1];

		SwingUtilities.invokeAndWait(() -> {
			window = new MainRibbonFrame("microProject — file-drop project Open GUI acceptance", null);
			manager = new GraphicManager(window);
			window.setGraphicManager(manager);
			manager.initView();
			SessionFactory.getInstance().setJobQueue(manager.getJobQueue());
			window.setSize(1020, 620);
			window.setLocationByPlatform(true);
			window.setAlwaysOnTop(true);
			window.setVisible(true);
			TransferHandler handler = window.getRootPane().getTransferHandler();
			accepted[0] = handler != null && handler.importData(window.getRootPane(), fileListTransferable(firstFile, secondFile));
		});
		assertTrue(accepted[0], "the main desktop window did not accept the file-list drop");
		FrameManager frames = manager.getFrameManager();
		GuiAcceptanceSupport.await(() -> frames.getAllFrames().size() == 2,
				"a two-file desktop drop did not create two project documents");
		assertTrue(manager.findFrameForProjectFile(firstFile.toString()) != null, () -> "opened files: " + openedFileNames(frames));
		assertTrue(manager.findFrameForProjectFile(secondFile.toString()) != null, () -> "opened files: " + openedFileNames(frames));
		GuiAcceptanceSupport.await(() -> usesSeparateDesktopWindows(),
				"file-drop projects were not presented as separate desktop windows");
		SwingUtilities.invokeAndWait(() -> frames.arrangeAll(WindowArrangement.TILE));
		capture(new com.microproject.testsupport.GuiRobot(), "msp-file-drop-multiple-project-open.png");
	}

	private static List<String> openedFileNames(FrameManager frames) {
		List<String> names = new ArrayList<>();
		for (Object value : frames.getAllFrames()) {
			if (value instanceof DocumentFrame frame)
				names.add(frame.getProject().getFileName());
		}
		return names;
	}

	private static Transferable fileListTransferable(Path... files) {
		List<java.io.File> values = new ArrayList<>();
		for (Path file : files)
			values.add(file.toFile());
		return new Transferable() {
			@Override public DataFlavor[] getTransferDataFlavors() { return new DataFlavor[] { DataFlavor.javaFileListFlavor }; }
			@Override public boolean isDataFlavorSupported(DataFlavor flavor) { return DataFlavor.javaFileListFlavor.equals(flavor); }
			@Override public Object getTransferData(DataFlavor flavor) {
				if (!isDataFlavorSupported(flavor)) throw new IllegalArgumentException("Unsupported flavor: " + flavor);
				return values;
			}
		};
	}

	private void capture(Robot robot, String artifactName) throws Exception {
		robot.waitForIdle();
		robot.delay(750);
		Path artifactDirectory = Path.of(System.getProperty("microproject.gui.artifacts.dir", "build/guiTest-artifacts"));
		Files.createDirectories(artifactDirectory);
		Rectangle bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		BufferedImage image = robot.createScreenCapture(bounds);
		for (Path projectFile : List.of(firstFile, secondFile)) {
			DocumentFrame frame = manager.findFrameForProjectFile(projectFile.toString());
			Window documentWindow = SwingUtilities.getWindowAncestor(frame);
			assertTrue(documentWindow != null && hasRenderedWorkAreaPixels(image, bounds, documentWindow),
				"A tiled document window must have a rendered work area: " + projectFile.getFileName());
		}
		Path artifact = artifactDirectory.resolve(artifactName);
		Files.createDirectories(artifact.getParent());
		ImageIO.write(image, "png", artifact.toFile());
	}

	private static boolean hasRenderedWorkAreaPixels(BufferedImage image, Rectangle captureBounds, Window window) {
		java.awt.Point location = window.getLocationOnScreen();
		int x0 = Math.max(0, location.x - captureBounds.x + 12);
		int y0 = Math.max(0, location.y - captureBounds.y + 170);
		int x1 = Math.min(image.getWidth(), location.x - captureBounds.x + window.getWidth() - 12);
		int y1 = Math.min(image.getHeight(), location.y - captureBounds.y + window.getHeight() - 20);
		int renderedPixels = 0;
		for (int y = y0; y < y1; y += 3) for (int x = x0; x < x1; x += 3) {
			int rgb = image.getRGB(x, y);
			if (((rgb >>> 16) & 0xff) < 220 || ((rgb >>> 8) & 0xff) < 220 || (rgb & 0xff) < 220)
				renderedPixels++;
		}
		return renderedPixels > 100;
	}

	private boolean usesSeparateDesktopWindows() {
		DocumentFrame first = manager.findFrameForProjectFile(firstFile.toString());
		DocumentFrame second = manager.findFrameForProjectFile(secondFile.toString());
		Window firstWindow = first == null ? null : SwingUtilities.getWindowAncestor(first);
		Window secondWindow = second == null ? null : SwingUtilities.getWindowAncestor(second);
		return firstWindow != null && secondWindow != null && firstWindow != secondWindow
				&& firstWindow.isVisible() && secondWindow.isVisible();
	}

	private static void writeProject(Path target, String name) throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool(name + " pool", undo), undo);
		project.initialize(false, false);
		project.setName(name);
		MpoFileImporter exporter = new MpoFileImporter();
		exporter.setProject(project);
		exporter.setFileName(target.toString());
		exporter.exportFile();
	}

	private static void writeProjectWithTask(Path target, String name) throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool(name + " pool", undo), undo);
		project.initialize(false, false);
		project.setName(name);
		project.createScriptedTask().setName(name);
		MpoFileImporter exporter = new MpoFileImporter();
		exporter.setProject(project);
		exporter.setFileName(target.toString());
		exporter.exportFile();
	}

	private static void deleteMpoSidecars(Path file) throws Exception {
		if (file == null) return;
		Files.deleteIfExists(Path.of(file + ".lock"));
		String filename = file.getFileName().toString();
		int extension = filename.lastIndexOf('.');
		Path syncMetadata = file.resolveSibling(filename.substring(0, extension) + ".projectlibre-sync.json");
		Files.deleteIfExists(syncMetadata);
		Files.deleteIfExists(Path.of(syncMetadata + ".lock"));
	}
}
