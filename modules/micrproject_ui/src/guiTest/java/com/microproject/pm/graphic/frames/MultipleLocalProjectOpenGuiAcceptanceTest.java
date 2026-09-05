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
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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
import com.microproject.session.SessionFactory;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.Environment;
import com.microproject.util.UiServices;

/** GUI-MSP-OPEN-01: one Open command expands a multiple-file selection into independent project documents. */
class MultipleLocalProjectOpenGuiAcceptanceTest {
	private MainRibbonFrame window;
	private GraphicManager manager;
	private Path firstFile;
	private Path secondFile;
	private UiServices.FileChooserProvider previousChooser;
	private boolean previousStandalone;
	private boolean previousClientSide;
	private boolean previousRibbonUi;
	private boolean previousNewLook;

	@AfterEach
	void cleanUp() throws Exception {
		UiServices.setFileChooserProvider(previousChooser);
		if (manager != null)
			SwingUtilities.invokeAndWait(() -> manager.cleanUp());
		if (window != null)
			SwingUtilities.invokeAndWait(() -> window.dispose());
		if (firstFile != null) Files.deleteIfExists(firstFile);
		if (secondFile != null) Files.deleteIfExists(secondFile);
		Environment.setStandAlone(previousStandalone);
		Environment.setClientSide(previousClientSide);
		Environment.setRibbonUI(previousRibbonUi);
		Environment.setNewLook(previousNewLook);
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
		firstFile = Files.createTempFile("msp-open-alpha-", ".mpo");
		secondFile = Files.createTempFile("msp-open-beta-", ".mpo");
		writeProject(firstFile, "Multiple Open Alpha");
		writeProject(secondFile, "Multiple Open Beta");
		UiServices.setFileChooserProvider(new UiServices.FileChooserProvider() {
			@Override public String chooseFileName(boolean save, String selectedFileName, Object parent) { return null; }
			@Override public List<String> chooseFileNames(boolean save, String selectedFileName, Object parent) {
				return save ? List.of() : List.of(firstFile.toString(), secondFile.toString());
			}
		});

		SwingUtilities.invokeAndWait(() -> {
			window = new MainRibbonFrame("microProject — Multiple local project Open GUI acceptance", null, null);
			manager = new GraphicManager(window);
			window.setGraphicManager(manager);
			manager.initView();
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
		capture(new Robot(), "msp-multiple-local-project-open.png");
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
			window = new MainRibbonFrame("microProject — command-line project Open GUI acceptance", null, null);
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
			startup.doStartupAction(manager, 0L, startup.projectUrls, false, false);
		});
		FrameManager frames = manager.getFrameManager();
		GuiAcceptanceSupport.await(() -> frames.getAllFrames().size() == 2,
				"a command-line project list did not create two project documents");
		assertTrue(manager.findFrameForProjectFile(firstFile.toString()) != null, () -> "opened files: " + openedFileNames(frames));
		assertTrue(manager.findFrameForProjectFile(secondFile.toString()) != null, () -> "opened files: " + openedFileNames(frames));
		GuiAcceptanceSupport.await(() -> usesSeparateDesktopWindows(),
				"command-line projects were not presented as separate desktop windows");
		SwingUtilities.invokeAndWait(() -> frames.arrangeAll(WindowArrangement.TILE));
		capture(new Robot(), "msp-command-line-multiple-project-open.png");
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
			window = new MainRibbonFrame("microProject — Recent project GUI acceptance", null, null);
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
		capture(new Robot(), "msp-recent-project-keeps-existing-document.png");
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
			window = new MainRibbonFrame("microProject — file-drop project Open GUI acceptance", null, null);
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
		capture(new Robot(), "msp-file-drop-multiple-project-open.png");
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
		Rectangle bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		BufferedImage image = robot.createScreenCapture(bounds);
		Path artifact = Path.of(System.getProperty("micrproject.gui.artifacts.dir", "build/guiTest-artifacts"),
				artifactName);
		Files.createDirectories(artifact.getParent());
		ImageIO.write(image, "png", artifact.toFile());
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
}
