/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.views;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.configuration.Dictionary;
import com.microproject.exchange.MpoFileImporter;
import com.microproject.graphic.configuration.BarStyles;
import com.microproject.graphic.configuration.SpreadSheetCategories;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.NodeFactory;
import com.microproject.pm.graphic.gantt.Gantt;
import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.pm.graphic.frames.MainRibbonFrame;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetModel;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetUtils;
import com.microproject.pm.graphic.timescale.CoordinatesConverter;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.DefaultSubProj;
import com.microproject.pm.task.DefaultSubprojectHandler;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.ProjectFactory;
import com.microproject.pm.task.SubProj;
import com.microproject.pm.task.Task;
import com.microproject.session.SessionFactory;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.Environment;
import com.microproject.util.UiServices;

/**
 * GUI-MSP-01: loads a real MPO child, persists the master link, and renders the
 * child tasks in the master's consolidated Gantt.
 */
class MasterSubprojectMpoGuiAcceptanceTest {
	private JFrame frame;
	private MainRibbonFrame applicationWindow;
	private GraphicManager graphicManager;
	private Gantt gantt;
	private SpreadSheet sheet;
	private boolean previousClientSide;
	private boolean previousStandalone;
	private boolean previousRibbonUi;
	private boolean previousNewLook;
	private UiServices.FileChooserProvider previousChooser;

	@AfterEach
	void closeWindow() throws Exception {
		if (graphicManager != null)
			SwingUtilities.invokeAndWait(() -> graphicManager.cleanUp());
		if (applicationWindow != null)
			SwingUtilities.invokeAndWait(() -> applicationWindow.dispose());
		if (frame != null)
			SwingUtilities.invokeAndWait(() -> frame.dispose());
		if (gantt != null)
			gantt.cleanUp();
		Environment.setClientSide(previousClientSide);
		Environment.setStandAlone(previousStandalone);
		Environment.setRibbonUI(previousRibbonUi);
		Environment.setNewLook(previousNewLook);
		UiServices.setFileChooserProvider(previousChooser);
	}

	/** GUI-MSP-SAVE-02: the explicit Save as MPO command writes and reopens a portable master. */
	@Test
	void explicitSaveAsMpoCommandPersistsAndReloadsMasterChildren() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		previousChooser = UiServices.getFileChooserProvider();
		previousClientSide = Environment.isClientSide();
		previousStandalone = Environment.getStandAlone();
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setClientSide(true);
		Environment.setStandAlone(true);
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		Project child = newProject("Explicit MPO save child");
		Task childTask = (Task) child.createLocalTaskNode(null).getImpl();
		childTask.setName("Child survives explicit MPO save");
		File childFile = File.createTempFile("msp-explicit-save-child-", ".mpo");
		childFile.deleteOnExit();
		persist(child, childFile);
		child.setFileName(childFile.getAbsolutePath());
		Project master = newProject("Explicit MPO save master");
		master.setMaster(true);
		master.setLocal(true);
		File sourceMaster = File.createTempFile("msp-explicit-save-source-", ".mpo");
		sourceMaster.deleteOnExit();
		master.setFileName(sourceMaster.getAbsolutePath());
		DefaultSubProj reference = new DefaultSubProj(master, child.getUniqueId());
		reference.setName("Child link");
		reference.setSubprojectFile(childFile.getAbsolutePath());
		Node referenceNode = NodeFactory.getInstance().createNode(reference);
		master.addToDefaultOutline(null, referenceNode);
		new DefaultSubprojectHandler(master).addSubproject(child, referenceNode, true, false);
		persist(master, sourceMaster);
		File selectedWithoutExtension = File.createTempFile("msp-explicit-save-target-", "");
		selectedWithoutExtension.delete();
		File target = new File(selectedWithoutExtension.getAbsolutePath() + ".mpo");
		target.deleteOnExit();
		UiServices.setFileChooserProvider(new UiServices.FileChooserProvider() {
			@Override public String chooseFileName(boolean save, String selectedFileName, Object parent) {
				return save ? selectedWithoutExtension.getAbsolutePath() : null;
			}
		});
		showRuntimeMaster(sourceMaster);
		GuiAcceptanceSupport.await(() -> graphicManager.findFrameForProjectFile(sourceMaster.getAbsolutePath()) != null,
				"the source master did not open before Save as MPO");
		Project runtimeMaster = graphicManager.findFrameForProjectFile(sourceMaster.getAbsolutePath()).getProject();
		GuiAcceptanceSupport.await(() -> hasTaskNamed(runtimeMaster, "Child survives explicit MPO save"),
				"the source master did not materialize its child before Save as MPO");
		SwingUtilities.invokeAndWait(() -> assertTrue(graphicManager.saveMasterAsMpo(),
				"the explicit MPO save route did not accept the selected target"));
		GuiAcceptanceSupport.await(target::isFile, "Save as MPO did not create the .mpo target");
		Project reloaded = load(target);
		SubProj reloadedChild = findSubproject(reloaded);
		assertNotNull(reloadedChild, "the explicitly saved MPO did not retain the child reference");
		assertTrue(reloadedChild.getSubprojectFile() != null && new File(reloadedChild.getSubprojectFile()).isFile(),
				"the explicitly saved MPO did not restore the embedded child archive");
		capture(new Robot(), "master-explicit-save-as-mpo.png");
	}

	@Test
	void realMpoChildIsVisibleInPersistedReadOnlyMasterGantt() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		previousClientSide = Environment.isClientSide();
		previousStandalone = Environment.getStandAlone();
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setClientSide(true);
		Environment.setStandAlone(true);
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		Project child = newProject("Read-only MPO child");
		Task crossProjectPredecessor = (Task) child.createLocalTaskNode(null).getImpl();
		crossProjectPredecessor.setName("Read-only child task in reopened master");
		File childFile = File.createTempFile("msp-read-only-child-gui-", ".mpo");
		childFile.deleteOnExit();
		persist(child, childFile);
		child.setFileName(childFile.getAbsolutePath());
		Project master = newProject("MPO Master Portfolio");
		master.setMaster(true);
		File masterFile = File.createTempFile("msp-master-gui-", ".mpo");
		masterFile.deleteOnExit();
		master.setFileName(masterFile.getAbsolutePath());
		master.setLocal(true);
		Project writableChild = newProject("Writable MPO child");
		Task writableTask = (Task) writableChild.createLocalTaskNode(null).getImpl();
		writableTask.setName("Writable child edit reflected in master");
		File writableChildFile = File.createTempFile("msp-writable-child-gui-", ".mpo");
		writableChildFile.deleteOnExit();
		persist(writableChild, writableChildFile);
		writableChild.setFileName(writableChildFile.getAbsolutePath());

		DefaultSubProj placeholder = new DefaultSubProj(master, child.getUniqueId());
		placeholder.setName("CCPM child schedule (read-only)");
		placeholder.setSubprojectFile(childFile.getAbsolutePath());
		placeholder.setSubprojectReadOnly(true);
		Node placeholderNode = NodeFactory.getInstance().createNode(placeholder);
		master.addToDefaultOutline(null, placeholderNode);
		child.setFileName(childFile.getAbsolutePath());
		new DefaultSubprojectHandler(master).addSubproject(child, placeholderNode, true, false);
		DefaultSubProj writablePlaceholder = new DefaultSubProj(master, writableChild.getUniqueId());
		writablePlaceholder.setName("Writable child schedule");
		writablePlaceholder.setSubprojectFile(writableChildFile.getAbsolutePath());
		Node writablePlaceholderNode = NodeFactory.getInstance().createNode(writablePlaceholder);
		master.addToDefaultOutline(null, writablePlaceholderNode);
		new DefaultSubprojectHandler(master).addSubproject(writableChild, writablePlaceholderNode, true, false);
		Dependency crossProjectLink = DependencyService.getInstance().newDependency(crossProjectPredecessor, writableTask,
			DependencyType.FS, 0L, master);
		assertTrue(crossProjectLink.isCrossProject(), "master Gantt fixture must contain a real cross-project connector");
		assertTrue(master.getTasks().contains(writableTask), "master must reference writable child tasks directly");

		MpoFileImporter writer = new MpoFileImporter();
		writer.setFileName(masterFile.getAbsolutePath());
		writer.setProject(master);
		writer.exportFile();
		assertTrue(master.getTasks().containsAll(child.getTasks()), "master must contain child tasks for consolidated scheduling");

		showRuntimeMaster(masterFile);
		Robot robot = new Robot();
		GuiAcceptanceSupport.await(() -> graphicManager.findFrameForProjectFile(masterFile.getAbsolutePath()) != null,
				"the normal local-file route did not open the master document");
		Project runtimeMaster = graphicManager.findFrameForProjectFile(masterFile.getAbsolutePath()).getProject();
		GuiAcceptanceSupport.await(() -> hasTaskNamed(runtimeMaster, crossProjectPredecessor.getName()),
				"the normal local-file route did not materialize the first embedded child in the reopened master: "
						+ "master=" + runtimeMaster.isMaster() + ", openedAsSubproject=" + runtimeMaster.isOpenedAsSubproject()
						+ ", states=" + subprojectStates(runtimeMaster));
		dismissReadOnlyWarning(robot);
		GuiAcceptanceSupport.await(() -> hasTaskNamed(runtimeMaster, writableTask.getName()),
				"the normal local-file route did not materialize the writable embedded child after dismissing the read-only notice: "
						+ "states=" + subprojectStates(runtimeMaster));
		assertExpandCollapseKeepsMasterRowsStable(writablePlaceholder.getName(), writableTask.getName());
		capture(robot, "master-subproject-consolidated-gantt.png");
		capture(robot, "master-subproject-cross-project-gantt.png");

		Project reloaded = load(masterFile);
		SubProj reloadedPlaceholder = findSubproject(reloaded);
		assertNotNull(reloadedPlaceholder, "master MPO must retain its subproject placeholder");
		assertNotEquals(childFile.getCanonicalPath(), new File(reloadedPlaceholder.getSubprojectFile()).getCanonicalPath(),
				"a portable MPO must restore its embedded child rather than retaining the original external path");
		assertTrue(new File(reloadedPlaceholder.getSubprojectFile()).isFile(), "embedded child MPO must be extracted on reopen");
		assertTrue(java.util.Arrays.equals(Files.readAllBytes(childFile.toPath()),
				Files.readAllBytes(new File(reloadedPlaceholder.getSubprojectFile()).toPath())),
				"extracted child MPO must match the embedded archive payload");
		assertTrue(((Task) reloadedPlaceholder).isSubprojectReadOnly(), "read-only insertion mode must survive save/reopen");
		assertTrue(hasExtractedSubproject(reloaded, writableChildFile),
				"writable child MPO must be embedded and extracted on reopen");
	}

	private void assertExpandCollapseKeepsMasterRowsStable(String parentName, String childName) throws Exception {
		final int[] counts = new int[2];
		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet visibleSheet = graphicManager.getCurrentFrame().getTopSpreadSheet();
			assertNotNull(visibleSheet, "the opened master must expose a task spreadsheet");
			assertTrue(visibleSheet.getModel() instanceof SpreadSheetModel,
					"the opened master spreadsheet must use the outline model");
			SpreadSheetModel model = (SpreadSheetModel) visibleSheet.getModel();
			int nameColumn = -1;
			for (int column = 0; column < visibleSheet.getColumnCount(); column++) {
				if (visibleSheet.isNameFieldColumn(column)) {
					nameColumn = column;
					break;
				}
			}
			assertTrue(nameColumn >= 0, "the master spreadsheet must expose a task-name column");
			int parentRow = -1;
			for (int row = 0; row < model.getRowCount(); row++) {
				Object value = model.getNode(row).getNode().getImpl();
				if (value instanceof Task task && parentName.equals(task.getName())) {
					parentRow = row;
					break;
				}
			}
			assertTrue(parentRow >= 0, "the embedded child placeholder must be visible before collapse");
			visibleSheet.changeSelection(parentRow, nameColumn, false, false);
			counts[0] = model.getRowCount();
			visibleSheet.executeNameCellCollapseExpand(false);
			counts[1] = model.getRowCount();
			assertTrue(counts[1] < counts[0], "collapsing the embedded child must hide its projected task rows");
			visibleSheet.executeNameCellCollapseExpand(true);
			assertEquals(counts[0], model.getRowCount(), "re-expanding must restore the original row count");
			boolean childVisible = false;
			for (int row = 0; row < model.getRowCount(); row++) {
				Object value = model.getNode(row).getNode().getImpl();
				if (value instanceof Task task && childName.equals(task.getName())) {
					childVisible = true;
					break;
				}
			}
			assertTrue(childVisible, "re-expanding must restore the embedded child task row");
		});
	}

	/** GUI-MSP-WINDOW-04: master, two independent files, and an opened child are all navigable documents. */
	@Test
	void masterTwoIndependentProjectsAndOpenedChildProduceFourDocumentWindows() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		previousClientSide = Environment.isClientSide();
		previousStandalone = Environment.getStandAlone();
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setClientSide(true);
		Environment.setStandAlone(true);
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		Project child = newProject("Window navigation child");
		((Task) child.createLocalTaskNode(null).getImpl()).setName("Child task");
		File childFile = File.createTempFile("msp-window-child-", ".mpo");
		childFile.deleteOnExit();
		persist(child, childFile);
		child.setFileName(childFile.getAbsolutePath());
		Project master = newProject("Window navigation master");
		master.setMaster(true);
		master.setLocal(true);
		File masterFile = File.createTempFile("msp-window-master-", ".mpo");
		masterFile.deleteOnExit();
		master.setFileName(masterFile.getAbsolutePath());
		DefaultSubProj reference = new DefaultSubProj(master, child.getUniqueId());
		reference.setName("Open this child");
		reference.setSubprojectFile(childFile.getAbsolutePath());
		Node referenceNode = NodeFactory.getInstance().createNode(reference);
		master.addToDefaultOutline(null, referenceNode);
		new DefaultSubprojectHandler(master).addSubproject(child, referenceNode, true, false);
		persist(master, masterFile);
		File firstIndependentFile = File.createTempFile("msp-window-independent-one-", ".mpo");
		File secondIndependentFile = File.createTempFile("msp-window-independent-two-", ".mpo");
		firstIndependentFile.deleteOnExit();
		secondIndependentFile.deleteOnExit();
		persist(newProject("Independent one"), firstIndependentFile);
		persist(newProject("Independent two"), secondIndependentFile);

		showRuntimeMaster(masterFile);
		GuiAcceptanceSupport.await(() -> graphicManager.findFrameForProjectFile(masterFile.getAbsolutePath()) != null,
				"the master document did not open");
		Project runtimeMaster = graphicManager.findFrameForProjectFile(masterFile.getAbsolutePath()).getProject();
		GuiAcceptanceSupport.await(() -> findSubproject(runtimeMaster) != null && findSubproject(runtimeMaster).getSubproject() != null,
				"the master child did not materialize before Open Subproject");
		RuntimeGraphicManager manager = (RuntimeGraphicManager) graphicManager;
		manager.openForTest(firstIndependentFile.getAbsolutePath());
		GuiAcceptanceSupport.await(() -> graphicManager.findFrameForProjectFile(firstIndependentFile.getAbsolutePath()) != null,
				"the first independent project did not open");
		manager.openForTest(secondIndependentFile.getAbsolutePath());
		GuiAcceptanceSupport.await(() -> graphicManager.findFrameForProjectFile(secondIndependentFile.getAbsolutePath()) != null,
				"the second independent project did not open");
		boolean[] openedChild = new boolean[1];
		SwingUtilities.invokeAndWait(() -> openedChild[0] = graphicManager.activateSubproject(findSubproject(runtimeMaster)));
		assertTrue(openedChild[0],
				"Open Subproject must promote the linked child to its own document window");
		GuiAcceptanceSupport.await(() -> graphicManager.getFrameManager().getAllFrames().size() == 4,
				"Window navigation must contain master, two independent projects, and the opened child");
		assertNotNull(graphicManager.getFrameForProject(findSubproject(runtimeMaster).getSubproject()),
				"the opened child must have its own DocumentFrame");
		SwingUtilities.invokeAndWait(() -> graphicManager.getFrameManager().arrangeAll(
				com.microproject.pm.graphic.frames.workspace.FrameManager.WindowArrangement.TILE));
		capture(new Robot(), "msp-master-four-window-navigation.png");
	}

	/** GUI-MSP-SAVE-01: saving a clean master persists a dirty linked child. */
	@Test
	void saveActionPersistsDirtyLinkedChildWhenMasterIsClean() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		previousClientSide = Environment.isClientSide();
		previousStandalone = Environment.getStandAlone();
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setClientSide(true);
		Environment.setStandAlone(true);
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		Project child = newProject("Dirty child save source");
		Task childTask = (Task) child.createLocalTaskNode(null).getImpl();
		childTask.setName("Child value before master save");
		File childFile = File.createTempFile("msp-dirty-child-save-", ".mpo");
		persist(child, childFile);
		child.setFileName(childFile.getAbsolutePath());
		Project sibling = newProject("Sibling project must remain separate");
		Task siblingTask = (Task) sibling.createLocalTaskNode(null).getImpl();
		siblingTask.setName("Sibling-only task");
		File siblingFile = File.createTempFile("msp-sibling-save-", ".mpo");
		persist(sibling, siblingFile);
		sibling.setFileName(siblingFile.getAbsolutePath());
		Project master = newProject("Clean master saves dirty child");
		master.setMaster(true);
		master.setLocal(true);
		File masterFile = File.createTempFile("msp-clean-master-save-", ".mpo");
		master.setFileName(masterFile.getAbsolutePath());
		DefaultSubProj reference = new DefaultSubProj(master, child.getUniqueId());
		reference.setName("Dirty child link");
		reference.setSubprojectFile(childFile.getAbsolutePath());
		Node referenceNode = NodeFactory.getInstance().createNode(reference);
		master.addToDefaultOutline(null, referenceNode);
		new DefaultSubprojectHandler(master).addSubproject(child, referenceNode, true, false);
		DefaultSubProj siblingReference = new DefaultSubProj(master, sibling.getUniqueId());
		siblingReference.setName("Sibling link");
		siblingReference.setSubprojectFile(siblingFile.getAbsolutePath());
		Node siblingReferenceNode = NodeFactory.getInstance().createNode(siblingReference);
		master.addToDefaultOutline(null, siblingReferenceNode);
		new DefaultSubprojectHandler(master).addSubproject(sibling, siblingReferenceNode, true, false);
		persist(master, masterFile);

		showRuntimeMaster(masterFile);
		GuiAcceptanceSupport.await(() -> graphicManager.findFrameForProjectFile(masterFile.getAbsolutePath()) != null,
				"the master did not open through the normal local-file route");
		Project runtimeMaster = graphicManager.findFrameForProjectFile(masterFile.getAbsolutePath()).getProject();
		GuiAcceptanceSupport.await(() -> findSubproject(runtimeMaster) != null && findSubproject(runtimeMaster).getSubproject() != null,
				"the writable linked child did not materialize in the open master");
		Project runtimeChild = findSubproject(runtimeMaster).getSubproject();
		assertEquals(new File(findSubproject(runtimeMaster).getSubprojectFile()).getCanonicalPath(),
				new File(runtimeChild.getFileName()).getCanonicalPath(),
				"the loaded child must retain its linked MPO file as its save target");
		Task runtimeTask = firstOrdinaryTask(runtimeChild);
		runtimeTask.setName("Child value saved through master Save");
		assertTrue(hasTaskNamed(runtimeChild, "Child value saved through master Save"),
				"fixture must edit the materialized child before invoking the master Save action");
		runtimeChild.setDirty(true);
		runtimeMaster.setDirty(false);
		runtimeMaster.setGroupDirty(false);
		assertFalse(runtimeMaster.needsSaving(), "fixture must keep the master clean");
		assertTrue(runtimeChild.needsSaving(), "fixture must keep only the linked child dirty");
		SwingUtilities.invokeAndWait(() -> graphicManager.new SaveProjectAction().actionPerformed(null));
		GuiAcceptanceSupport.await(() -> !runtimeChild.needsSaving(), "the master Save action did not persist the dirty child");
		Project savedChild = load(new File(runtimeChild.getFileName()));
		assertTrue(hasTaskNamed(savedChild, "Child value saved through master Save"),
				"the linked child file did not contain the edit made before master Save");
		assertFalse(hasTaskNamed(savedChild, "Sibling-only task"),
				"saving an opened child must not serialize a sibling from the consolidated master outline");
		capture(new Robot(), "master-save-dirty-child.png");
	}

	private void showRuntimeMaster(File masterFile) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			applicationWindow = new MainRibbonFrame("microProject — MPO Master/Sub-project GUI acceptance", null, null);
			RuntimeGraphicManager manager = new RuntimeGraphicManager(applicationWindow);
			graphicManager = manager;
			applicationWindow.setGraphicManager(manager);
			manager.initView();
			SessionFactory.getInstance().setJobQueue(manager.getJobQueue());
			applicationWindow.setSize(1180, 640);
			applicationWindow.setLocationByPlatform(true);
			applicationWindow.setAlwaysOnTop(true);
			applicationWindow.setVisible(true);
			manager.openForTest(masterFile.getAbsolutePath());
		});
	}

	private static void dismissReadOnlyWarning(Robot robot) throws Exception {
		final Dialog[] warning = new Dialog[1];
		GuiAcceptanceSupport.await(() -> {
			for (Window candidate : Window.getWindows())
				if (candidate instanceof Dialog dialog && dialog.isShowing()) {
					warning[0] = dialog;
					return true;
				}
			return false;
		}, "read-only child warning did not appear");
		warning[0].toFront();
		JButton button = findButton(warning[0]);
		assertNotNull(button, "read-only child warning must provide a close button");
		java.awt.Point location = button.getLocationOnScreen();
		robot.mouseMove(location.x + button.getWidth() / 2, location.y + button.getHeight() / 2);
		robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
		GuiAcceptanceSupport.await(() -> !warning[0].isShowing(), "read-only child warning did not dismiss");
	}

	private static JButton findButton(Container container) {
		for (Component component : container.getComponents()) {
			if (component instanceof JButton button)
				return button;
			if (component instanceof Container child) {
				JButton button = findButton(child);
				if (button != null)
					return button;
			}
		}
		return null;
	}

	private void showMasterGantt(Project master, File childFile) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			sheet = new SpreadSheet();
			sheet.setSpreadSheetCategory(SpreadSheetCategories.taskSpreadsheetCategory);
			NodeModelCache cache = NodeModelCacheFactory.getInstance().createFilteredCache(
					NodeModelCacheFactory.createTaskNodeModelCache(master, master.getTaskModel()), "master-subproject-gui", null);
			SpreadSheetUtils.setFieldsAndContext(sheet, cache, SpreadSheetCategories.taskSpreadsheetCategory,
					"Spreadsheet.Task.summary", true);
			gantt = new Gantt(master, "Gantt");
			gantt.setCache(cache);
			gantt.setCoord(new CoordinatesConverter(master));
			gantt.setBarStyles((BarStyles) Dictionary.get(BarStyles.category, "standard"));
			frame = new JFrame("microProject — MPO Master/Sub-project GUI acceptance");
			JLabel evidence = new JLabel("Master: " + master.getName() + "  |  Child: " + childFile.getName()
					+ " (Read-only)  |  Writable child edit reflected  |  Saved and reloaded as MPO", SwingConstants.CENTER);
			JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(sheet), new JScrollPane(gantt));
			split.setResizeWeight(0.46D);
			frame.add(evidence, BorderLayout.NORTH);
			frame.add(split, BorderLayout.CENTER);
			frame.setPreferredSize(new Dimension(1180, 640));
			frame.pack();
			frame.setLocationByPlatform(true);
			frame.setAlwaysOnTop(true);
			frame.setVisible(true);
			gantt.updateSize();
		});
	}

	private void capture(Robot robot, String artifactName) throws Exception {
		Rectangle bounds = new Rectangle();
		SwingUtilities.invokeAndWait(() -> {
			JFrame target = applicationWindow == null ? frame : applicationWindow;
			java.awt.Point point = target.getLocationOnScreen();
			bounds.setBounds(point.x, point.y, target.getWidth(), target.getHeight());
			target.toFront();
		});
		BufferedImage image = robot.createScreenCapture(bounds);
		Path artifact = Path.of(System.getProperty("micrproject.gui.artifacts.dir", "build/guiTest-artifacts"),
				artifactName);
		Files.createDirectories(artifact.getParent());
		ImageIO.write(image, "png", artifact.toFile());
	}

	private static Project newProject(String name) {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool(name, undo), undo);
		project.setName(name);
		project.initialize(false, false);
		return project;
	}

	private static Project loadSample() throws Exception {
		return load(findSample());
	}

	private static Project load(File file) throws Exception {
		MpoFileImporter importer = new MpoFileImporter();
		importer.setFileName(file.getAbsolutePath());
		importer.setProjectFactory(ProjectFactory.getInstance());
		importer.importFile();
		return importer.getProject();
	}

	private static boolean hasTaskNamed(Project project, String taskName) {
		return project.getTasks().stream().anyMatch(task -> taskName.equals(task.getName()));
	}

	private static String subprojectStates(Project project) {
		StringBuilder states = new StringBuilder();
		for (java.util.Iterator<Task> tasks = project.getTaskOutlineIterator(); tasks.hasNext();) {
			Task task = tasks.next();
			if (task instanceof SubProj reference)
				states.append('[').append(reference.getSubprojectFile()).append(':')
						.append(reference.getLoadStatus()).append(':').append(reference.isSubprojectOpen()).append(']');
		}
		return states.toString();
	}

	private static final class RuntimeGraphicManager extends GraphicManager {
		private static final long serialVersionUID = 1L;
		RuntimeGraphicManager(MainRibbonFrame window) { super(window); }
		void openForTest(String fileName) { loadLocalDocument(fileName, false); }
	}

	private static void persist(Project project, File file) throws Exception {
		MpoFileImporter writer = new MpoFileImporter();
		writer.setFileName(file.getAbsolutePath());
		writer.setProject(project);
		writer.exportFile();
	}

	private static File findSample() {
		for (String prefix : new String[] { "samples", "../samples", "../../samples" }) {
			File candidate = Path.of(prefix, "CCPM 標準システム導入 20タスク.mpo").toFile();
			if (candidate.isFile())
				return candidate;
		}
		throw new AssertionError("checked-in CCPM MPO sample is missing");
	}

	private static SubProj findSubproject(Project project) {
		for (java.util.Iterator<Task> tasks = project.getTaskOutlineIterator(); tasks.hasNext();) {
			Task task = tasks.next();
			if (task instanceof SubProj subproject)
				return subproject;
		}
		return null;
	}

	private static Task firstOrdinaryTask(Project project) {
		for (Object value : project.getTasks())
			if (value instanceof Task task && !(task instanceof SubProj))
				return task;
		throw new AssertionError("sample child has no ordinary task for the external-link Gantt fixture");
	}

	private static boolean hasSubprojectPath(Project project, String expectedPath) {
		for (java.util.Iterator<Task> tasks = project.getTaskOutlineIterator(); tasks.hasNext();) {
			Task task = tasks.next();
			if (task instanceof SubProj && expectedPath.equals(task.getSubprojectFile()))
				return true;
		}
		return false;
	}

	private static boolean hasExtractedSubproject(Project project, File source) throws Exception {
		for (java.util.Iterator<Task> tasks = project.getTaskOutlineIterator(); tasks.hasNext();) {
			Task task = tasks.next();
			if (task instanceof SubProj reference && reference.getSubprojectFile() != null) {
				File extracted = new File(reference.getSubprojectFile());
				if (extracted.isFile() && !source.getCanonicalFile().equals(extracted.getCanonicalFile())
						&& java.util.Arrays.equals(Files.readAllBytes(source.toPath()), Files.readAllBytes(extracted.toPath())))
					return true;
			}
		}
		return false;
	}
}
