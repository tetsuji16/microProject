/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.awt.GraphicsConfiguration;
import java.awt.IllegalComponentStateException;
import java.awt.AWTEvent;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.prefs.Preferences;
import java.time.Duration;
import java.time.Instant;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.function.BooleanSupplier;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.formdev.flatlaf.ui.FlatNativeWindowsLibrary;
import com.microproject.dialog.AbstractDialog;
import com.microproject.dialog.AboutDialog;
import com.microproject.dialog.HelpDialog;
import com.microproject.dialog.LocaleDialog;
import com.microproject.dialog.ProjectDialog;
import com.microproject.dialog.ProjectInformationDialog;
import com.microproject.dialog.PreferencesDialogBox;
import com.microproject.job.JobQueue;
import com.microproject.exchange.MpoFileImporter;
import com.microproject.init.Init;
import com.microproject.menu.MenuActionConstants;
import com.microproject.pm.ccpm.CriticalChainBufferHistory;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.pm.graphic.frames.workspace.NamedFrame;
import com.microproject.preference.ConfigurationFile;
import com.microproject.session.SessionFactory;
import com.microproject.strings.Messages;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.testsupport.NativeChooserProbeCompletion;
import com.microproject.testsupport.RibbonGuiButton;
import com.microproject.testsupport.RibbonGuiSupport;
import com.microproject.testsupport.DialogLayoutAssertions;
import com.microproject.testsupport.DialogScreenshotArtifacts;
import com.microproject.ui.ribbon.RibbonDisplayMode;
import com.microproject.ui.ribbon.RibbonDisplayPreferences;
import com.microproject.util.Environment;
import com.microproject.util.Alert;
import com.microproject.ui.util.SwingAlertPresenter;
import com.microproject.util.UiDispatch;
import com.microproject.util.SwingFileChooserProvider;
import com.microproject.util.UiServices;
import com.microproject.ui.util.SwingUiDispatcher;
import com.microproject.undo.DataFactoryUndoController;

/**
 * Verifies the real File-ribbon command pipeline rather than a recording
 * ActionMap.  A dispatch-only test can pass while the production action is
 * disabled, throws, or returns before showing its dialog.
 */
class RibbonExternalCommandGuiAcceptanceTest {
	private static final Duration NATIVE_CHOOSER_READINESS_BUDGET = Duration.ofSeconds(30);
	private MainRibbonFrame window;
	private GraphicManager manager;
	private boolean previousRibbonUi;
	private boolean previousNewLook;
	private boolean previousStandalone;
	private boolean previousClientSide;
	private UiServices.FileChooserProvider previousChooser;
	private String previousSystemChooserProperty;
	private WindowTrace saveWindowTrace;
	private JobQueue previousJobQueue;
	private Path legacyPod;
	private Path importedMpp;
	private Path mpoSaveTarget;
	private Path mpoSavedCopy;
	private Path mppSaveDirectory;
	private Path restartFirstProject;
	private Path restartSecondProject;
	private Preferences localePreferences;
	private String previousLocalePreference;
	private Locale previousDefaultLocale;
	private Locale previousFormatLocale;
	private Locale previousDisplayLocale;
	private RibbonDisplayMode previousRibbonDisplayMode;

	@BeforeEach
	void showRibbonCommandsForAcceptance() {
		previousRibbonDisplayMode = RibbonDisplayPreferences.load();
		RibbonDisplayPreferences.save(RibbonDisplayMode.ALWAYS_SHOW);
	}

	@AfterEach
	void closeWindow() throws Exception {
		if (saveWindowTrace != null) saveWindowTrace.close();
		if (manager != null) SwingUtilities.invokeAndWait(manager::cleanUp);
		if (window != null) SwingUtilities.invokeAndWait(window::dispose);
		Alert.setPresenter(null);
		Environment.setRibbonUI(previousRibbonUi);
		Environment.setNewLook(previousNewLook);
		Environment.setStandAlone(previousStandalone);
		Environment.setClientSide(previousClientSide);
		UiServices.setFileChooserProvider(previousChooser);
		if (previousSystemChooserProperty == null)
			System.clearProperty("flatlaf.useSystemFileChooser");
		else
			System.setProperty("flatlaf.useSystemFileChooser", previousSystemChooserProperty);
		if (previousJobQueue != null || manager != null)
			SessionFactory.getInstance().setJobQueue(previousJobQueue);
		if (legacyPod != null) Files.deleteIfExists(legacyPod);
		if (mppSaveDirectory != null && Files.exists(mppSaveDirectory)) {
			List<Path> cleanupPaths;
			// This directory belongs only to this fixture; do not follow symlinks.
			try (var paths = Files.walk(mppSaveDirectory)) {
				cleanupPaths = paths.sorted(java.util.Comparator.reverseOrder()).toList();
			}
			// Close the directory stream before deleting files on Windows.
			for (Path path : cleanupPaths) Files.deleteIfExists(path);
		}
		if (restartFirstProject != null) Files.deleteIfExists(restartFirstProject);
		if (restartSecondProject != null) Files.deleteIfExists(restartSecondProject);
		if (localePreferences != null) {
			if (previousLocalePreference == null) localePreferences.remove("locale");
			else localePreferences.put("locale", previousLocalePreference);
			localePreferences.flush();
			Locale.setDefault(previousDefaultLocale);
			Locale.setDefault(Locale.Category.FORMAT, previousFormatLocale);
			Locale.setDefault(Locale.Category.DISPLAY, previousDisplayLocale);
			Messages.reset();
		}
		for (Window open : Window.getWindows()) {
			if (open instanceof ProjectDialog || open instanceof LocaleDialog
				|| open instanceof HelpDialog || open instanceof AboutDialog) {
				open.dispose();
			}
		}
		if (previousRibbonDisplayMode != null) {
			RibbonDisplayPreferences.save(previousRibbonDisplayMode);
			previousRibbonDisplayMode = null;
		}
	}

	/** GUI-RIBBON-FILE-02: File/Open must reach the legacy importer, not MPO zip validation. */
	@Test
	void robotOpensLegacyPodFromTheFileRibbon() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
			"A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		previousStandalone = Environment.getStandAlone();
		previousClientSide = Environment.isClientSide();
		previousChooser = UiServices.getFileChooserProvider();
		Environment.setStandAlone(true);
		Environment.setClientSide(true);
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		legacyPod = Files.createTempFile("ribbon-legacy-sample-", ".pod");
		Files.copy(Path.of(System.getProperty("microproject.project.dir"), "samples", "June_1_sample.pod"), legacyPod,
			java.nio.file.StandardCopyOption.REPLACE_EXISTING);
		createWindow("microProject — Legacy sample File/Open acceptance");
		UiServices.setFileChooserProvider(new UiServices.FileChooserProvider() {
			@Override public String chooseFileName(boolean save, String selectedFileName, Object parent) { return null; }
			@Override public List<String> chooseFileNames(boolean save, String selectedFileName, Object parent) {
				return save ? List.of() : List.of(legacyPod.toString());
			}
		});
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		clickBackstageOpenTile(robot);
		GuiAcceptanceSupport.await(() -> manager.findFrameForProjectFile(legacyPod.toString()) != null,
			"File/Open rejected the selected legacy POD before its importer ran");
		assertTrue(manager.findFrameForProjectFile(legacyPod.toString()).getProject() != null,
			"File/Open registered a legacy POD frame without a project model");
	}

	/** GUI-MPO-SAVE-01: ordinary Save of an imported MPP asks for an MPO target and preserves its source. */
	@Test
	void robotSavesImportedMppAsMpoAndCancellationOrOverwriteRefusalKeepsSource() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
			"A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		previousStandalone = Environment.getStandAlone();
		previousClientSide = Environment.isClientSide();
		previousChooser = UiServices.getFileChooserProvider();
		previousSystemChooserProperty = System.getProperty("flatlaf.useSystemFileChooser");
		Environment.setStandAlone(true);
		Environment.setClientSide(true);
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		System.setProperty("flatlaf.useSystemFileChooser", "false");
		Alert.setPresenter(new SwingAlertPresenter());
		SwingFileChooserProvider swingChooser = new SwingFileChooserProvider();
		mppSaveDirectory = Files.createTempDirectory("mpp-save-as-mpo-");
		importedMpp = mppSaveDirectory.resolve("Imported sample.mpp");
		mpoSaveTarget = mppSaveDirectory.resolve("Imported sample.mpo");
		mpoSavedCopy = mppSaveDirectory.resolve("Saved project.mpo");
		Path sample = Path.of(System.getProperty("microproject.project.dir"), "modules", "microproject_exchange", "testdata", "New Product.mpp");
		Files.copy(sample, importedMpp);
		AtomicReference<Path> openPath = new AtomicReference<>(importedMpp);
		byte[] originalMppBytes = Files.readAllBytes(importedMpp);
		byte[] existingMpoBytes = "existing MPO must survive a declined overwrite".getBytes(java.nio.charset.StandardCharsets.UTF_8);
		Files.write(mpoSaveTarget, existingMpoBytes);
		createWindow("microProject — imported MPP Save acceptance");
		saveWindowTrace = new WindowTrace();
		saveWindowTrace.open();
		UiServices.setFileChooserProvider(new UiServices.FileChooserProvider() {
			@Override public String chooseFileName(boolean save, String selectedFileName, Object parent) {
				return swingChooser.chooseFileName(save, selectedFileName, parent);
			}
			@Override public List<String> chooseFileNames(boolean save, String selectedFileName, Object parent) {
				return save ? List.of() : List.of(openPath.get().toString());
			}
		});
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		activateWindowForRobot(robot);
		clickBackstageOpenTile(robot);
		GuiAcceptanceSupport.await(() -> manager.findFrameForProjectFile(importedMpp.toString()) != null,
			"File/Open did not import the MPP fixture");
		Project project = manager.findFrameForProjectFile(importedMpp.toString()).getProject();
		String originalAssociation = project.getFileName();
		java.util.Iterator<Task> taskIterator = project.getTaskOutlineIterator();
		assertTrue(taskIterator.hasNext(), "MPP fixture must contain an editable task");
		Task editedTask = taskIterator.next();
		List<String> expectedAssignmentBaselines = assignmentBaselineSemantics(project);
		SwingUtilities.invokeAndWait(() -> {
			editedTask.setManuallyScheduled(true);
		});
		assertTrue(project.needsSaving(), "manual schedule change must make ordinary Save eligible");
		List<String> expectedTaskSemantics = taskSemantics(project);

		activateWindowForRobot(robot);
		click(robot, findCommandButton(window.getRibbonPanel(), "RibbonSaveProject", robot));
		GuiAcceptanceSupport.await(() -> visibleFileChooserDialog() != null,
			"ordinary Save of an MPP import did not open Save As");
		JFileChooser chooser = findFileChooser(visibleFileChooserDialog());
		assertEquals(mpoSaveTarget.getFileName().toString(), chooser.getSelectedFile().getName(),
			"Save As must suggest the MPO sibling for an imported MPP");
		robot.keyPress(java.awt.event.KeyEvent.VK_ESCAPE);
		robot.keyRelease(java.awt.event.KeyEvent.VK_ESCAPE);
		GuiAcceptanceSupport.await(() -> visibleFileChooserDialog() == null, "Save As Escape did not cancel");
		assertEquals(originalAssociation, project.getFileName(), "cancellation must preserve the source association");
		assertTrue(project.needsSaving(), "cancellation must preserve the dirty state");
		assertArrayEquals(originalMppBytes, Files.readAllBytes(importedMpp), "cancellation must leave MPP source bytes untouched");
		assertArrayEquals(existingMpoBytes, Files.readAllBytes(mpoSaveTarget), "cancellation must preserve an existing MPO target");

		activateWindowForRobot(robot);
		click(robot, findCommandButton(window.getRibbonPanel(), "RibbonSaveProject", robot));
		GuiAcceptanceSupport.await(() -> visibleFileChooserDialog() != null,
			"ordinary Save did not reopen Save As after cancellation");
		Window saveDialog = visibleFileChooserDialog();
		JFileChooser saveChooser = findFileChooser(saveDialog);
		assertEquals(mpoSaveTarget.getFileName().toString(), saveChooser.getSelectedFile().getName(),
			"retry must retain the suggested MPO target");
		assertTrue(selectFileChooserSaveButton(saveDialog) != null, "Save As chooser has no visible Save button");
		click(robot, selectFileChooserSaveButton(saveDialog));
		GuiAcceptanceSupport.await(() -> visibleOptionDialog() != null, "existing MPO target did not trigger overwrite confirmation");
		robot.keyPress(java.awt.event.KeyEvent.VK_ESCAPE);
		robot.keyRelease(java.awt.event.KeyEvent.VK_ESCAPE);
		GuiAcceptanceSupport.await(() -> visibleOptionDialog() == null,
			"overwrite refusal dialog did not close");
		assertArrayEquals(existingMpoBytes, Files.readAllBytes(mpoSaveTarget), "declined overwrite must preserve the existing MPO");
		assertEquals(originalAssociation, project.getFileName(), "declined overwrite must preserve the source association");
		assertTrue(project.needsSaving(), "declined overwrite must preserve the dirty state");

		GuiAcceptanceSupport.await(() -> visibleFileChooserDialog() != null,
			"declining overwrite must leave Save As open for another target");
		saveDialog = visibleFileChooserDialog();
		JTextField fileNameField = findFileChooserNameField(saveDialog);
		click(robot, fileNameField);
		GuiAcceptanceSupport.await(() -> isFocusOwnerOnEdt(fileNameField),
			"Save As filename field did not acquire keyboard focus after its physical click");
		robot.keyPress(java.awt.event.KeyEvent.VK_CONTROL);
		robot.keyPress(java.awt.event.KeyEvent.VK_A);
		robot.keyRelease(java.awt.event.KeyEvent.VK_A);
		robot.keyRelease(java.awt.event.KeyEvent.VK_CONTROL);
		type(robot, mpoSavedCopy.getFileName().toString());
		GuiAcceptanceSupport.awaitInputAndEventQueueIdle(robot, "Save As filename entry");
		assertEquals(mpoSavedCopy.getFileName().toString(), textOnEdt(fileNameField),
			"physical filename entry must be present in the chooser field before approval");
		AbstractButton finalApproveButton = selectFileChooserSaveButton(saveDialog);
		String preApproveState = saveAsChooserDiagnostics(saveDialog, finalApproveButton);
		java.util.concurrent.atomic.AtomicBoolean approveActionFired = new java.util.concurrent.atomic.AtomicBoolean();
		finalApproveButton.addActionListener(event -> approveActionFired.set(true));
		System.err.println("=== Save As pre-approve diagnostic ===\n" + preApproveState);
		captureNativeChooserScreen(robot, "mpp-save-as-before-final-approve.png");
		click(robot, finalApproveButton);
		try {
			GuiAcceptanceSupport.await(() -> visibleFileChooserDialog() == null && Files.isRegularFile(mpoSavedCopy),
				"selected MPO copy was not written");
		} catch (AssertionError failure) {
			throw new AssertionError(failure.getMessage() + "\nPre-approve chooser state:\n" + preApproveState
				+ "\nApprove action fired=" + approveActionFired.get()
				+ "\nPost-approve chooser state:\n" + saveAsChooserDiagnostics(saveDialog, finalApproveButton)
				+ "\n" + saveWindowTrace.snapshot(), failure);
		}
		GuiAcceptanceSupport.await(() -> mpoSavedCopy.toString().equals(project.getFileName()) && !project.needsSaving(),
			"successful MPO save did not update association and clear dirty state");
		assertArrayEquals(originalMppBytes, Files.readAllBytes(importedMpp), "MPO save must leave the MPP source untouched");
		com.microproject.exchange.MpoFileImporter reader = new com.microproject.exchange.MpoFileImporter();
		reader.setFileName(mpoSavedCopy.toString());
		reader.importFile();
		assertEquals(expectedTaskSemantics, taskSemantics(reader.getProject()),
			"MPO reopen must preserve imported task names and schedule dates");
		assertEquals(expectedAssignmentBaselines, assignmentBaselineSemantics(reader.getProject()),
			"MPO reopen must preserve imported assignment baseline scalar values");

		Path scalarXmlFixture = Path.of(System.getProperty("microproject.project.dir"), "modules",
			"microproject_exchange", "testdata", "Assignment Baseline Scalars.xml");
		Path scalarXml = mppSaveDirectory.resolve("assignment-baseline-scalars.xml");
		Path scalarMpo = mppSaveDirectory.resolve("assignment-baseline-scalars.mpo");
		Files.copy(scalarXmlFixture, scalarXml);
		click(robot, findCommandButton(window.getRibbonPanel(), "RibbonCloseProject", robot));
		GuiAcceptanceSupport.await(() -> manager.findFrameForProjectFile(mpoSavedCopy.toString()) == null,
			"saved MPP import did not close before the separate MSPDI fixture route");
		openPath.set(scalarXml);
		activateWindowForRobot(robot);
		clickBackstageOpenTile(robot);
		GuiAcceptanceSupport.await(() -> manager.findFrameForProjectFile(scalarXml.toString()) != null,
			"File/Open did not import the fixed MSPDI assignment-baseline fixture");
		Project scalarProject = manager.findFrameForProjectFile(scalarXml.toString()).getProject();
		Task baselineTask = scalarProject.getTaskList().stream()
			.filter(candidate -> "Assignment baseline task".equals(candidate.getName())).findFirst().orElseThrow();
		assertTrue(baselineTask instanceof com.microproject.pm.task.NormalTask);
		com.microproject.pm.task.NormalTask normalBaselineTask = (com.microproject.pm.task.NormalTask) baselineTask;
		assertEquals("Baseline test resource", ((com.microproject.pm.assignment.Assignment)
			normalBaselineTask.getAssignments().iterator().next()).getResource().getName());
		assertEquals(null, normalBaselineTask.getSnapshot(Integer.valueOf(0)),
			"assignment-only baseline import must not create a task baseline");
		com.microproject.pm.assignment.Assignment scalarAssignment =
			(com.microproject.pm.assignment.Assignment) normalBaselineTask.getAssignments().iterator().next();
		assertEquals(Instant.parse("2026-10-05T17:00:00Z").toEpochMilli(),
			scalarAssignment.getBaselineValues(0).start());
		assertEquals(null, scalarAssignment.getBaselineValues(0).finish(), "slot 0 Finish is intentionally missing");
		assertEquals(Long.valueOf(8L * 60L * 60L * 1000L), scalarAssignment.getBaselineValues(0).work());
		assertEquals(Long.valueOf(0L), scalarAssignment.getBaselineValues(9).work());
		assertEquals(Instant.parse("2026-10-07T17:00:00Z").toEpochMilli(),
			scalarAssignment.getBaselineValues(10).start());
		assertEquals(Instant.parse("2026-10-08T17:00:00Z").toEpochMilli(),
			scalarAssignment.getBaselineValues(10).finish());
		assertEquals(Long.valueOf(4L * 60L * 60L * 1000L), scalarAssignment.getBaselineValues(10).work());
		DocumentFrame scalarFrame = manager.findFrameForProjectFile(scalarXml.toString());
		SwingUtilities.invokeAndWait(() -> {
			baselineTask.setName("Edited assignment baseline task");
			baselineTask.setManuallyScheduled(true);
		});
		assertTrue(scalarProject.needsSaving(), "real baseline-project edit must make ordinary Save eligible");
		List<String> expectedScalarBaselines = assignmentBaselineSemantics(scalarProject);
		saveWindowTrace = new WindowTrace();
		saveWindowTrace.open();
		activateWindowForRobot(robot);
		SwingUtilities.invokeAndWait(() -> manager.getFrameManager().activateFrame(scalarFrame));
		try {
			GuiAcceptanceSupport.await(() -> manager.getCurrentFrame() == scalarFrame
					&& scalarFrame.getProject() == scalarProject
					&& manager.getAction("SaveProjectAs").isEnabled(),
				"the scalar XML document did not become active with Save As enabled");
		} catch (AssertionError failure) {
			DocumentFrame activeFrame = manager.getCurrentFrame();
			boolean saveAsEnabled = manager.getAction("SaveProjectAs").isEnabled();
			throw new AssertionError(failure.getMessage() + " (expectedFrame=" + scalarFrame
				+ ", expectedProject=" + (scalarFrame == null ? null : scalarFrame.getProject())
				+ ", registeredFrames=" + manager.getFrameManager().getAllFrames()
				+ ", activeFrame=" + activeFrame + ", activeProject="
				+ (activeFrame == null ? null : activeFrame.getProject()) + ", SaveAsEnabled=" + saveAsEnabled + ")", failure);
		}
		AbstractButton scalarSaveAs = findCommandButton(window.getRibbonPanel(), "RibbonSaveProjectAs", robot);
		assertTrue(scalarSaveAs.isShowing() && scalarSaveAs.isEnabled(),
			"Backstage Save As must be enabled and visible for the active scalar XML project");
		click(robot, scalarSaveAs);
		awaitWithWindowTrace(() -> visibleFileChooserDialog() != null,
			"Save As of an MSPDI import did not open the file chooser");
		Window scalarChooserWindow = visibleFileChooserDialog();
		awaitWithWindowTrace(() -> scalarChooserWindow.isActive()
			&& KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner() != null
			&& SwingUtilities.getWindowAncestor(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner())
				== scalarChooserWindow,
			"Save As chooser did not own keyboard focus");
		JFileChooser scalarChooser = findFileChooser(visibleFileChooserDialog());
		assertEquals(scalarMpo.getFileName().toString(), scalarChooser.getSelectedFile().getName(),
			"Save As must default an imported MSPDI XML project to the native MPO format");
		click(robot, selectFileChooserSaveButton(visibleFileChooserDialog()));
		awaitWithWindowTrace(() -> scalarMpo.toFile().isFile()
			&& scalarMpo.toString().equals(scalarProject.getFileName()) && !scalarProject.needsSaving(),
			"MSPDI assignment baseline project was not saved as MPO");
		MpoFileImporter scalarReader = new MpoFileImporter();
		scalarReader.setFileName(scalarMpo.toString());
		scalarReader.importFile();
		Project scalarReopened = scalarReader.getProject();
		com.microproject.pm.task.NormalTask reopenedBaselineTask = (com.microproject.pm.task.NormalTask)
			scalarReopened.getTaskList().stream()
				.filter(candidate -> "Edited assignment baseline task".equals(candidate.getName())).findFirst().orElseThrow();
		assertEquals(expectedScalarBaselines, assignmentBaselineSemantics(scalarReopened),
			"MPO reopen must preserve physical MSPDI assignment baseline values");
		com.microproject.pm.assignment.Assignment reopenedScalarAssignment =
			(com.microproject.pm.assignment.Assignment) reopenedBaselineTask.getAssignments().iterator().next();
		assertEquals(scalarAssignment.getBaselineValues(0), reopenedScalarAssignment.getBaselineValues(0));
		assertEquals(scalarAssignment.getBaselineValues(9), reopenedScalarAssignment.getBaselineValues(9));
		assertEquals(scalarAssignment.getBaselineValues(10), reopenedScalarAssignment.getBaselineValues(10));
	}

	/** GUI-RIBBON-FILE-03: a persisted CCPM fever-chart history survives the physical File/Open route. */
	@Test
	void robotOpensCcpmHistorySampleFromTheFileRibbon() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
			"A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		previousStandalone = Environment.getStandAlone();
		previousClientSide = Environment.isClientSide();
		previousChooser = UiServices.getFileChooserProvider();
		Environment.setStandAlone(true);
		Environment.setClientSide(true);
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		Path historySample = Path.of(System.getProperty("microproject.project.dir"), "samples",
			"CCPM 標準システム導入 20タスク（履歴付き）.mpo");
		assertTrue(Files.isRegularFile(historySample), "checked-in CCPM history sample is missing");
		createWindow("microProject — CCPM history File/Open acceptance");
		UiServices.setFileChooserProvider(new UiServices.FileChooserProvider() {
			@Override public String chooseFileName(boolean save, String selectedFileName, Object parent) { return null; }
			@Override public List<String> chooseFileNames(boolean save, String selectedFileName, Object parent) {
				return save ? List.of() : List.of(historySample.toString());
			}
		});
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		clickBackstageOpenTile(robot);
		GuiAcceptanceSupport.await(() -> manager.findFrameForProjectFile(historySample.toString()) != null,
			"File/Open did not register the selected CCPM history sample");
		Project loaded = manager.findFrameForProjectFile(historySample.toString()).getProject();
		CriticalChainBufferHistory history = loaded.findTransientDocumentState(CriticalChainBufferHistory.class);
		assertTrue(history != null && history.points().size() == 4,
			"File/Open must restore all four persisted CCPM buffer observations");
		assertEquals(50D, history.points().get(2).progressPercent(), 0.00001D,
			"File/Open did not retain the intermediate CCPM history point");
	}

	@Test
	void robotInvokesRealFileRibbonCommandsAndOpensTheirDialogs() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
			"A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		previousStandalone = Environment.getStandAlone();
		previousClientSide = Environment.isClientSide();
		Environment.setStandAlone(true);
		Environment.setClientSide(true);
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);

		SwingUtilities.invokeAndWait(() -> {
			window = new MainRibbonFrame("microProject — real ribbon command acceptance", null);
			manager = new GraphicManager(window);
			window.setGraphicManager(manager);
			manager.initView();
			SessionFactory.getInstance().setJobQueue(manager.getJobQueue());
			manager.setConnected(true);
			window.setSize(1200, 700);
			window.setLocationByPlatform(true);
			window.setAlwaysOnTop(true);
			window.setVisible(true);
		});
		GuiAcceptanceSupport.await(() -> window.isShowing(), "real ribbon window did not become visible");

		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		clickAndClose(robot, "RibbonNewProject", ProjectDialog.class);
		clickAndDispose(robot, "RibbonBackstageOptions", PreferencesDialogBox.class);
		clickAndClose(robot, "RibbonLocale", LocaleDialog.class);
		AbstractButton helpTab = findRibbonTab(window.getRibbonPanel(), "Help", "ヘルプ");
		click(robot, helpTab);
		robot.waitForIdle();
		GuiAcceptanceSupport.await(helpTab::isSelected, "Help ribbon tab did not become selected");
		clickAndClose(robot, "RibbonProjectLibreDocumentation", HelpDialog.class);
		clickAndClose(robot, "RibbonAboutProjectLibre", AboutDialog.class);
	}

	@Test
	void robotOpensBackstageInfoForAnActiveProject() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
			"A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		previousStandalone = Environment.getStandAlone();
		previousClientSide = Environment.isClientSide();
		Environment.setStandAlone(true);
		Environment.setClientSide(true);
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		createStartedWindow("microProject — Backstage Info acceptance");
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("backstage-info-gui", undo);
		pool.setLocal(true);
		Project project = Project.createProject(pool, undo);
		project.setName("Backstage Info GUI");
		SwingUtilities.invokeAndWait(() -> manager.addProjectFrame(project));
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame() != null
			&& manager.getCurrentFrame().getProject() == project,
			"Backstage Info project was not registered as the active document");
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		activateWindowForRobot(robot);
		click(robot, findCommandButton(window.getRibbonPanel(), "RibbonBackstageProjectInformation", robot));
		GuiAcceptanceSupport.await(() -> visibleDialog(ProjectInformationDialog.class) != null,
			"File > Info did not open Project Information for the active project");
		Window dialog = visibleDialog(ProjectInformationDialog.class);
		assertDialogBodyIsRendered(dialog, "RibbonBackstageProjectInformation");
		SwingUtilities.invokeAndWait(dialog::dispose);
	}

	/** #398: physical File/Open Escape and Cancel both cancel the real chooser without changing documents. */
	@Test
	void robotEscapeAndCancelCloseTheFileOpenChooserWithoutOpeningAProject() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
			"A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		previousStandalone = Environment.getStandAlone();
		previousClientSide = Environment.isClientSide();
		previousChooser = UiServices.getFileChooserProvider();
		previousSystemChooserProperty = System.getProperty("flatlaf.useSystemFileChooser");
		Environment.setStandAlone(true);
		Environment.setClientSide(true);
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		System.setProperty("flatlaf.useSystemFileChooser", "false");
		SwingFileChooserProvider swingChooser = new SwingFileChooserProvider();
		AtomicBoolean chooserCallStarted = new AtomicBoolean();
		AtomicBoolean chooserCallReturned = new AtomicBoolean();
		UiServices.setFileChooserProvider(new UiServices.FileChooserProvider() {
			@Override public String chooseFileName(boolean save, String selectedFileName, Object parent) {
				chooserCallStarted.set(true);
				try { return swingChooser.chooseFileName(save, selectedFileName, parent); }
				finally { chooserCallReturned.set(true); }
			}
			@Override public List<String> chooseFileNames(boolean save, String selectedFileName, Object parent) {
				chooserCallStarted.set(true);
				try { return swingChooser.chooseFileNames(save, selectedFileName, parent); }
				finally { chooserCallReturned.set(true); }
			}
		});

		createWindow("microProject — File/Open cancellation acceptance (#398)");
		saveWindowTrace = new WindowTrace();
		saveWindowTrace.open();
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		activateWindowForRobot(robot);
		int documentsBefore = manager.getFrameManager().getAllFrames().size();

		clickBackstageOpenTile(robot);
		awaitWithWindowTrace(() -> visibleFileChooserDialog() != null,
			"File/Open did not show the Swing file chooser; providerStarted=" + chooserCallStarted.get()
				+ ", providerReturned=" + chooserCallReturned.get());
		Window escapeChooser = visibleFileChooserDialog();
		GuiAcceptanceSupport.await(() -> {
			Component owner = java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
			return owner != null && SwingUtilities.getWindowAncestor(owner) == escapeChooser;
		}, "File/Open did not acquire keyboard focus");
		robot.keyPress(java.awt.event.KeyEvent.VK_ESCAPE);
		robot.keyRelease(java.awt.event.KeyEvent.VK_ESCAPE);
		GuiAcceptanceSupport.await(() -> visibleFileChooserDialog() == null,
			"Escape did not cancel File/Open");
		assertEquals(documentsBefore, manager.getFrameManager().getAllFrames().size(),
			"Escape cancellation must not open a project");

		clickBackstageOpenTile(robot);
		GuiAcceptanceSupport.await(() -> visibleFileChooserDialog() != null,
			"File/Open did not reopen after Escape cancellation");
		Window cancelChooser = visibleFileChooserDialog();
		AbstractButton cancel = findFileChooserCancelButton(cancelChooser);
		assertTrue(cancel != null && cancel.isShowing(), "File/Open chooser has no visible Cancel button");
		click(robot, cancel);
		GuiAcceptanceSupport.await(() -> visibleFileChooserDialog() == null,
			"Physical Cancel did not close File/Open");
		assertEquals(documentsBefore, manager.getFrameManager().getAllFrames().size(),
			"Cancel must not open a project");
	}

	/** #398: exercise Escape against the default Windows native chooser, not only FlatLaf's Swing fallback. */
	@Test
	void robotEscapeCancelsDefaultWindowsFileOpenChooserWithoutOpeningAProject() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
			"A desktop session is required for native chooser acceptance coverage.");
		assertTrue(FlatNativeWindowsLibrary.isLoaded(),
			"The Windows native chooser acceptance route requires FlatLaf's loaded Windows native library.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		previousStandalone = Environment.getStandAlone();
		previousClientSide = Environment.isClientSide();
		previousChooser = UiServices.getFileChooserProvider();
		previousSystemChooserProperty = System.getProperty("flatlaf.useSystemFileChooser");
		System.clearProperty("flatlaf.useSystemFileChooser");
		Environment.setStandAlone(true);
		Environment.setClientSide(true);
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);

		createWindow("microProject — native File/Open Escape acceptance (#398)");
		AtomicBoolean chooserCallStarted = new AtomicBoolean();
		AtomicBoolean chooserCallReturned = new AtomicBoolean();
		AtomicBoolean chooserCancelled = new AtomicBoolean();
		AtomicReference<Throwable> chooserFailure = new AtomicReference<>();
		AtomicReference<Instant> chooserProviderStartedAt = new AtomicReference<>();
		AtomicReference<Instant> chooserProviderReturnedAt = new AtomicReference<>();
		AtomicReference<String> chooserOwnerAtProviderEntry = new AtomicReference<>("not-entered");
		AtomicReference<Window> chooserOwnerWindow = new AtomicReference<>();
		SwingFileChooserProvider nativeProvider = new SwingFileChooserProvider();
		UiServices.setFileChooserProvider(new UiServices.FileChooserProvider() {
			@Override
			public String chooseFileName(boolean save, String selectedFileName, Object parent) {
				chooserProviderStartedAt.set(Instant.now());
				chooserOwnerAtProviderEntry.set(describeChooserOwner(parent));
				chooserOwnerWindow.set(resolveChooserOwner(parent));
				chooserCallStarted.set(true);
				try {
					String selected = nativeProvider.chooseFileName(save, selectedFileName, parent);
					chooserCancelled.set(selected == null);
					return selected;
				} catch (RuntimeException | Error failure) {
					chooserFailure.set(failure);
					throw failure;
				} finally {
					chooserProviderReturnedAt.set(Instant.now());
					chooserCallReturned.set(true);
				}
			}

			@Override
			public List<String> chooseFileNames(boolean save, String selectedFileName, Object parent) {
				chooserProviderStartedAt.set(Instant.now());
				chooserOwnerAtProviderEntry.set(describeChooserOwner(parent));
				chooserOwnerWindow.set(resolveChooserOwner(parent));
				chooserCallStarted.set(true);
				try {
					List<String> selected = nativeProvider.chooseFileNames(save, selectedFileName, parent);
					chooserCancelled.set(selected.isEmpty());
					return selected;
				} catch (RuntimeException | Error failure) {
					chooserFailure.set(failure);
					throw failure;
				} finally {
					chooserProviderReturnedAt.set(Instant.now());
					chooserCallReturned.set(true);
				}
			}
		});

		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		activateWindowForRobot(robot);
		int documentsBefore = manager.getFrameManager().getAllFrames().size();
		verifyNativeChooserEscape(robot, () -> pressCtrlO(robot), chooserCallStarted, chooserCallReturned,
			chooserCancelled, chooserFailure, "Ctrl+O", chooserProviderStartedAt, chooserProviderReturnedAt,
			chooserOwnerAtProviderEntry, chooserOwnerWindow);
		assertEquals(documentsBefore, manager.getFrameManager().getAllFrames().size(),
			"native Ctrl+O cancellation must not open a project");
		verifyNativeChooserEscape(robot,
			() -> clickBackstageOpenTile(robot),
			chooserCallStarted, chooserCallReturned,
			chooserCancelled, chooserFailure, "Ribbon Open", chooserProviderStartedAt, chooserProviderReturnedAt,
			chooserOwnerAtProviderEntry, chooserOwnerWindow);
		assertEquals(documentsBefore, manager.getFrameManager().getAllFrames().size(),
			"native Ribbon Open cancellation must not open a project");
		verifyNativeChooserCancelButton(robot,
			() -> clickBackstageOpenTile(robot), chooserCallStarted,
			chooserCallReturned, chooserCancelled, chooserFailure, "Ribbon Open", chooserProviderStartedAt,
			chooserProviderReturnedAt, chooserOwnerAtProviderEntry, chooserOwnerWindow);
		assertEquals(documentsBefore, manager.getFrameManager().getAllFrames().size(),
			"native Ribbon Open Cancel must not open a project");
	}

	@Test
	void nativeChooserOverlayDetectionAcceptsAWhiteDialogWithVisibleChrome() {
		BufferedImage before = new BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB);
		BufferedImage after = new BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB);
		for (int y = 0; y < before.getHeight(); y++) {
			for (int x = 0; x < before.getWidth(); x++) {
				before.setRGB(x, y, Color.WHITE.getRGB());
				after.setRGB(x, y, Color.WHITE.getRGB());
			}
		}
		// A native chooser can have a white body over a white application. Its
		// title bar and frame still provide a large, connected desktop overlay.
		for (int x = 30; x < 370; x++) {
			after.setRGB(x, 20, Color.GRAY.getRGB());
			after.setRGB(x, 45, Color.LIGHT_GRAY.getRGB());
			after.setRGB(x, 279, Color.GRAY.getRGB());
		}
		for (int y = 20; y < 280; y++) {
			after.setRGB(30, y, Color.GRAY.getRGB());
			after.setRGB(369, y, Color.GRAY.getRGB());
		}
		assertFalse(visibleDialogOverlayBounds(before, after).isEmpty(),
			"visible native chooser chrome must count even when its white body matches the application");
	}

	@Test
	void localeDialogPersistsLanguageAndCountryAndTheirFormattersFollowSelection() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
			"A desktop session is required for Robot acceptance coverage.");
		previousLocalePreference = null;
		localePreferences = Preferences.userNodeForPackage(ConfigurationFile.class);
		previousLocalePreference = localePreferences.get("locale", null);
		previousDefaultLocale = Locale.getDefault();
		previousFormatLocale = Locale.getDefault(Locale.Category.FORMAT);
		previousDisplayLocale = Locale.getDefault(Locale.Category.DISPLAY);
		localePreferences.put("locale", "default");
		localePreferences.flush();
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		previousStandalone = Environment.getStandAlone();
		previousClientSide = Environment.isClientSide();
		Environment.setStandAlone(true);
		Environment.setClientSide(true);
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);

		Init.initialize();
		createWindow("microProject — locale preference acceptance");
		LocaleDialog dialog = LocaleDialog.getInstance(manager);
		SwingUtilities.invokeLater(dialog::doModal);
		GuiAcceptanceSupport.await(dialog::isShowing, "Locale dialog did not open");
		DialogLayoutAssertions.assertTextControlsAtPreferredHeight(dialog, "Locale Settings dialog");
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		JComboBox<?>[] selectors = new JComboBox<?>[2];
		SwingUtilities.invokeAndWait(() -> {
			selectors[0] = findComboWithItem(dialog, "de");
			selectors[1] = findComboWithItem(dialog, "Germany");
		});
		click(robot, selectors[0]);
		type(robot, "de");
		robot.keyPress(java.awt.event.KeyEvent.VK_ENTER);
		robot.keyRelease(java.awt.event.KeyEvent.VK_ENTER);
		click(robot, selectors[1]);
		type(robot, "Germany");
		robot.keyPress(java.awt.event.KeyEvent.VK_ENTER);
		robot.keyRelease(java.awt.event.KeyEvent.VK_ENTER);
		SwingUtilities.invokeAndWait(() -> {
			assertEquals("de", selectors[0].getSelectedItem());
			assertEquals("Germany", String.valueOf(selectors[1].getSelectedItem()));
		});
		click(robot, findButton(dialog, Messages.getString("ButtonText.OK")));
		GuiAcceptanceSupport.await(() -> !dialog.isShowing(), "Locale dialog did not commit and close");

		assertEquals("de_DE", localePreferences.get("locale", "missing"),
			"Locale dialog did not persist its selected language and country");
		Locale.setDefault(Locale.GERMANY);
		assertEquals("1234,50 €", com.microproject.datatype.Money.normalCurrencyFormat(1234.5, false));
		assertTrue(com.microproject.util.DateTime.utcDateFormatInstance()
			.format(new java.util.Date(1767312000000L)).startsWith("02.01."));
	}

	/** MSP documents returning from a report by switching back to View > Gantt Chart. */
	@Test
	void robotReturnsFromReportToGanttThroughTheDocumentedViewRoute() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
			"A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		previousStandalone = Environment.getStandAlone();
		Environment.setStandAlone(true);
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);

		createWindow("microProject — Report to Gantt navigation acceptance");
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("report-navigation-gui", undo);
		pool.setLocal(true);
		Project project = Project.createProject(pool, undo);
		project.setName("Report navigation GUI");
		SwingUtilities.invokeAndWait(() -> manager.addProjectFrame(project));
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame() != null
			&& manager.getCurrentFrame().getProject() == project, "report navigation project did not open");

		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		activateWindowForRobot(robot);
		AbstractButton reportTab = findRibbonTab(window.getRibbonPanel(), "Report", "レポート");
		assertTrue(reportTab.isShowing(), "Report ribbon tab must be physically visible");
		click(robot, reportTab);
		robot.waitForIdle();
		GuiAcceptanceSupport.await(reportTab::isSelected, "Report ribbon tab did not become selected");
		AbstractButton report = RibbonGuiSupport.findVisibleOrExpand(robot, window.getRibbonPanel(), "RibbonReport");
		assertTrue(report.isShowing() && report.isEnabled(), "RibbonReport must be visible and enabled before its physical click");
		assertEquals(window, SwingUtilities.getWindowAncestor(RibbonGuiButton.component(report)),
			"RibbonReport must belong to the active document shell");
		click(robot, report);
		robot.waitForIdle();
		GuiAcceptanceSupport.await(() -> MenuActionConstants.ACTION_REPORT.equals(manager.getTopViewId()),
			"Report command did not activate the report view");

		AbstractButton viewTab = findRibbonTab(window.getRibbonPanel(), "View", "ビュー");
		click(robot, viewTab);
		robot.waitForIdle();
		GuiAcceptanceSupport.await(viewTab::isSelected, "View ribbon tab did not become selected");
		AbstractButton gantt = RibbonGuiSupport.findVisibleOrExpand(robot, window.getRibbonPanel(), "RibbonGantt");
		assertTrue(gantt.isShowing() && gantt.isEnabled(),
			"View > Gantt Chart must remain an available return route from Report");
		click(robot, gantt);
		GuiAcceptanceSupport.await(() -> MenuActionConstants.ACTION_GANTT.equals(manager.getTopViewId()),
			"View > Gantt Chart did not return from Report to the task schedule");
	}

	/**
	 * GUI-RIBBON-FILE-01: New is not accepted until its physical OK route has
	 * created a visible document with the entered model name.  This catches the
	 * historic EDT failure before the dialog as well as the later silent failure
	 * between dialog submission and document-frame registration.
	 */
	@Test
	void robotCreatesNewLocalProjectFromTheFileRibbon() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
			"A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		previousStandalone = Environment.getStandAlone();
		previousClientSide = Environment.isClientSide();
		Environment.setStandAlone(true);
		Environment.setClientSide(true);
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);

		createStartedWindow("microProject — New project creation acceptance");
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		activateWindowForRobot(robot);
		robot.delay(500);
		// Windows may consume a command click while transferring foreground
		// activation. Repeat the same physical route a bounded number of times;
		// every attempt still requires the real dialog and document registration.
		for (int attempt = 0; attempt < 3 && visibleDialog(ProjectDialog.class) == null; attempt++) {
			if (attempt > 0) activateWindowForRobot(robot);
			AbstractButton newButton = findCommandButton(window.getRibbonPanel(), "RibbonNewProject", robot);
			assertTrue(newButton.isShowing(), "RibbonNewProject must be physically showing on attempt " + (attempt + 1));
			assertTrue(newButton.isEnabled(), "RibbonNewProject must be enabled on attempt " + (attempt + 1));
			click(robot, newButton);
			robot.waitForIdle();
			try {
				GuiAcceptanceSupport.await(() -> visibleDialog(ProjectDialog.class) != null,
					"New did not show the project dialog on attempt " + (attempt + 1));
			} catch (AssertionError ignored) {
				// Keep the retry physical and bounded; the final assertion below
				// remains release-blocking if the command never opens its dialog.
			}
		}
		assertTrue(visibleDialog(ProjectDialog.class) != null,
			"New did not show the project dialog after physical foreground retries");
		ProjectDialog dialog = (ProjectDialog) visibleDialog(ProjectDialog.class);
		JTextField name = findProjectNameField(dialog);
		String expectedName = "Robot File Ribbon New Project";
		SwingUtilities.invokeAndWait(() -> {
			dialog.toFront();
			dialog.requestFocus();
		});
		robot.delay(200);
		click(robot, name);
		robot.waitForIdle();
		// A dialog activated from a foreground-retry can consume the first child
		// click while transferring native focus; repeat the physical field click
		// before sending the transaction keystrokes.
		click(robot, name);
		robot.keyPress(java.awt.event.KeyEvent.VK_CONTROL);
		robot.keyPress(java.awt.event.KeyEvent.VK_A);
		robot.keyRelease(java.awt.event.KeyEvent.VK_A);
		robot.keyRelease(java.awt.event.KeyEvent.VK_CONTROL);
		type(robot, expectedName);
		AbstractButton ok = findButton(dialog, Messages.getString("ButtonText.OK"));
		click(robot, ok);
		try {
			GuiAcceptanceSupport.await(() -> manager.getCurrentFrame() != null,
				"New accepted the dialog but did not register a document frame");
		} catch (AssertionError firstAttempt) {
			SwingUtilities.invokeAndWait(() -> {
				dialog.toFront();
				dialog.requestFocus();
				dialog.setAlwaysOnTop(true);
			});
			Point dialogLocation = dialog.getLocationOnScreen();
			robot.mouseMove(dialogLocation.x + Math.max(8, dialog.getWidth() / 2), dialogLocation.y + 8);
			robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
			robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
			robot.delay(150);
			click(robot, ok);
			GuiAcceptanceSupport.await(() -> manager.getCurrentFrame() != null,
				"New did not register a document frame after the physical OK retry");
		}
		Project created = manager.getCurrentFrame().getProject();
		assertEquals(expectedName, created.getName(), "New did not apply the dialog name to the created project");
		assertTrue(manager.getFrameManager().getAllFrames().contains(manager.getCurrentFrame()),
			"New did not expose the created project through the frame manager");
	}

	/** Changing locale through the real ribbon must restart into the same native window. */
	@Test
	void localeChangeRestartsThroughStartupFactoryAndKeepsWindowUsable() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
			"A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		previousStandalone = Environment.getStandAlone();
		previousClientSide = Environment.isClientSide();
		Environment.setStandAlone(true);
		Environment.setClientSide(true);
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		previousDefaultLocale = Locale.getDefault();
		previousFormatLocale = Locale.getDefault(Locale.Category.FORMAT);
		previousDisplayLocale = Locale.getDefault(Locale.Category.DISPLAY);
		localePreferences = Preferences.userNodeForPackage(ConfigurationFile.class);
		previousLocalePreference = localePreferences.get("locale", null);
		localePreferences.put("locale", "default");
		localePreferences.flush();
		Init.initialize();

		createStartedWindow("microProject — locale restart acceptance");
		GraphicManager initialManager = manager;
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		click(robot, findCommandButton(window.getRibbonPanel(), "RibbonLocale", robot));
		GuiAcceptanceSupport.await(() -> visibleDialog(LocaleDialog.class) != null,
			"Locale dialog did not open from the ribbon");
		LocaleDialog dialog = (LocaleDialog) visibleDialog(LocaleDialog.class);
		JComboBox<?>[] selectors = new JComboBox<?>[2];
		SwingUtilities.invokeAndWait(() -> {
			selectors[0] = findComboWithItem(dialog, "de");
			selectors[1] = findComboWithItem(dialog, "Germany");
		});
		click(robot, selectors[0]);
		type(robot, "de");
		robot.keyPress(java.awt.event.KeyEvent.VK_ENTER);
		robot.keyRelease(java.awt.event.KeyEvent.VK_ENTER);
		click(robot, selectors[1]);
		type(robot, "Germany");
		robot.keyPress(java.awt.event.KeyEvent.VK_ENTER);
		robot.keyRelease(java.awt.event.KeyEvent.VK_ENTER);
		SwingUtilities.invokeAndWait(() -> {
			assertEquals("de", selectors[0].getSelectedItem());
			assertEquals("Germany", String.valueOf(selectors[1].getSelectedItem()));
		});
		String okText = Messages.getString("ButtonText.OK");
		click(robot, findButton(dialog, okText));
		GuiAcceptanceSupport.await(() -> !dialog.isShowing(), "Locale dialog did not close after confirmation");
		GuiAcceptanceSupport.await(() -> window.getGraphicManager() != null
			&& window.getGraphicManager() != initialManager,
			"Locale change did not install a new GraphicManager through StartupFactory");
		manager = window.getGraphicManager();
		AbstractButton[] restoredTaskTab = new AbstractButton[1];
		String taskTabTitle = java.util.ResourceBundle.getBundle("com.microproject.menu.menu")
			.getString("TaskRibbonTask.title");
		GuiAcceptanceSupport.await(() -> {
			try {
				restoredTaskTab[0] = findRibbonTab(window.getRibbonPanel(), taskTabTitle);
				return restoredTaskTab[0].isShowing();
			} catch (AssertionError notLaidOutYet) {
				return false;
			}
		}, "Restarted Swing ribbon did not expose its Task tab");
		AbstractButton taskTab = restoredTaskTab[0];
		activateWindowForRobot(robot);
		click(robot, taskTab);
		GuiAcceptanceSupport.await(taskTab::isSelected, "Restarted application did not restore the Task ribbon tab");
		AbstractButton restoredTaskInformation = RibbonGuiSupport.findVisibleOrExpand(robot, window.getRibbonPanel(), "RibbonTaskInformation");
		assertTrue(window.isShowing() && manager.getFrameManager() != null && restoredTaskInformation.isShowing(),
			"Restarted application did not restore an interactive ribbon in the existing visible window");
		assertEquals("de_DE", localePreferences.get("locale", "missing"),
			"Locale restart did not preserve the selected preference");
		assertTrue(manager.getContainer() == window,
			"Restart replaced the native window instead of rebuilding its application UI");
	}

	@Test
	void startupFactoryRestartRestoresBothOpenMpoDocuments() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
			"A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		previousStandalone = Environment.getStandAlone();
		previousClientSide = Environment.isClientSide();
		Environment.setStandAlone(true);
		Environment.setClientSide(true);
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		UiDispatch.setDispatcher(new SwingUiDispatcher());
		restartFirstProject = Files.createTempFile("restart-workspace-alpha-", ".mpo");
		restartSecondProject = Files.createTempFile("restart-workspace-beta-", ".mpo");
		writeMpoProject(restartFirstProject, "Restart workspace Alpha");
		writeMpoProject(restartSecondProject, "Restart workspace Beta");
		createWindow("microProject — workspace restart acceptance");
		SwingUtilities.invokeAndWait(() -> manager.openLocalProjectsSequentially(new String[] {
			restartFirstProject.toString(), restartSecondProject.toString() }));
		GuiAcceptanceSupport.await(() -> manager.getFrameManager().getAllFrames().size() == 2,
			"Workspace fixture did not open two MPO documents");
		DocumentFrame firstFrame = manager.findFrameForProjectFile(restartFirstProject.toString());
		SwingUtilities.invokeAndWait(() -> firstFrame.activateView(MenuActionConstants.ACTION_NETWORK));
		assertEquals(MenuActionConstants.ACTION_NETWORK, firstFrame.getTopViewId(),
			"workspace fixture must start with a non-default view to detect restore being overwritten");

		ApplicationStartupFactory startup = new ApplicationStartupFactory(new HashMap<>());
		SwingUtilities.invokeAndWait(() -> manager = startup.restart(manager));
		GuiAcceptanceSupport.await(() -> manager.getFrameManager() != null
			&& manager.getFrameManager().getAllFrames().size() == 2
			&& manager.findFrameForProjectFile(restartFirstProject.toString()) != null
			&& manager.findFrameForProjectFile(restartSecondProject.toString()) != null
			&& GraphicManager.getLastWorkspace() == null,
			"StartupFactory restart did not restore both open MPO documents");
		assertEquals(MenuActionConstants.ACTION_NETWORK,
			manager.findFrameForProjectFile(restartFirstProject.toString()).getTopViewId(),
			"the restored workspace view must not be overwritten with the initial Gantt view");
		assertTrue(window.isShowing(), "Workspace restart hid or replaced the native window");
		assertTrue(manager.getContainer() == window,
			"Workspace restart attached the restored documents to a different native window");
		SwingUtilities.invokeAndWait(() -> {
			for (Object frame : new ArrayList<>(manager.getFrameManager().getAllFrames()))
				manager.getFrameManager().removeFrame((NamedFrame) frame);
			manager.encodeWorkspace();
		});
	}

	private static void writeMpoProject(Path target, String name) throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool(name + " pool", undo), undo);
		project.initialize(false, false);
		project.setName(name);
		MpoFileImporter exporter = new MpoFileImporter();
		exporter.setProject(project);
		exporter.setFileName(target.toString());
		exporter.exportFile();
	}

	private void createWindow(String title) throws Exception {
		previousJobQueue = SessionFactory.getInstance().getJobQueue();
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
		GuiAcceptanceSupport.await(() -> window.isShowing(), "real ribbon window did not become visible");
	}

	private void verifyNativeChooserEscape(Robot robot, GuiCommand openCommand, AtomicBoolean chooserCallStarted,
			AtomicBoolean chooserCallReturned, AtomicBoolean chooserCancelled, AtomicReference<Throwable> chooserFailure,
			String route, AtomicReference<Instant> providerStartedAt, AtomicReference<Instant> providerReturnedAt,
			AtomicReference<String> ownerAtProviderEntry, AtomicReference<Window> ownerWindowAtProviderEntry) throws Exception {
		// Native Windows choosers are external top-level windows. Do not place the
		// owner above them in the test window's z-order while exercising this route.
		activateWindowForRobot(robot, false);
		chooserCallStarted.set(false);
		chooserCallReturned.set(false);
		chooserCancelled.set(false);
		chooserFailure.set(null);
		providerStartedAt.set(null);
		providerReturnedAt.set(null);
		ownerAtProviderEntry.set("not-entered");
		ownerWindowAtProviderEntry.set(null);
		BufferedImage beforeOpen = captureScreen(robot);
		String windowsBeforeOpen = snapshotNativeChooserWindows();
		captureNativeChooserScreen(beforeOpen, "issue-398-native-" + route.toLowerCase(Locale.ROOT).replace(' ', '-')
			+ "-before-open.png");
		PreparedNativeChooserProbe probe = null;
		Instant chooserInvokedAt = null;
		try {
			probe = PreparedNativeChooserProbe.start(route);
			probe.awaitPrepared();
			openCommand.run();
			chooserInvokedAt = probe.startReadinessDeadline();
			GuiAcceptanceSupport.await(chooserCallStarted::get,
				route + " did not enter the default Windows native chooser provider");
			requireNativeCancelControl(awaitNativeChooserSnapshot(probe, route), route);
			assertFalse(chooserCallReturned.get(), route + " chooser must remain modal until physical Escape");
			String artifactName = "issue-398-native-" + route.toLowerCase(Locale.ROOT).replace(' ', '-')
				+ "-before-escape.png";
			captureNativeChooserScreen(robot, artifactName);
			logNativeChooserForegroundAtKey(route, captureNativeWindowState(route + "-escape-key"));
			robot.keyPress(java.awt.event.KeyEvent.VK_ESCAPE);
			try {
				robot.keyRelease(java.awt.event.KeyEvent.VK_ESCAPE);
			} finally {
				robot.waitForIdle();
			}
			GuiAcceptanceSupport.await(chooserCallReturned::get,
				route + " Escape did not return from the default Windows file chooser");
			assertTrue(chooserCancelled.get(), route + " Escape must cancel the default Windows file chooser");
		} catch (Exception | Error failure) {
			captureNativeChooserFailureDiagnostics(route, robot, beforeOpen, chooserCallStarted, chooserCallReturned,
				chooserCancelled, chooserFailure, windowsBeforeOpen, chooserInvokedAt == null ? Instant.now() : chooserInvokedAt,
				failure, providerStartedAt, providerReturnedAt, ownerAtProviderEntry, ownerWindowAtProviderEntry.get(), probe);
			throw failure;
		} finally {
			if (probe != null) probe.close();
			if (chooserCallStarted.get() && !chooserCallReturned.get()) {
				robot.keyPress(java.awt.event.KeyEvent.VK_ESCAPE);
				try {
					robot.keyRelease(java.awt.event.KeyEvent.VK_ESCAPE);
				} finally {
					robot.waitForIdle();
				}
			}
		}
	}

	private void verifyNativeChooserCancelButton(Robot robot, GuiCommand openCommand,
			AtomicBoolean chooserCallStarted, AtomicBoolean chooserCallReturned,
			AtomicBoolean chooserCancelled, AtomicReference<Throwable> chooserFailure, String route,
			AtomicReference<Instant> providerStartedAt, AtomicReference<Instant> providerReturnedAt,
			AtomicReference<String> ownerAtProviderEntry, AtomicReference<Window> ownerWindowAtProviderEntry) throws Exception {
		activateWindowForRobot(robot, false);
		chooserCallStarted.set(false);
		chooserCallReturned.set(false);
		chooserCancelled.set(false);
		chooserFailure.set(null);
		providerStartedAt.set(null);
		providerReturnedAt.set(null);
		ownerAtProviderEntry.set("not-entered");
		ownerWindowAtProviderEntry.set(null);
		BufferedImage beforeOpen = captureScreen(robot);
		String windowsBeforeOpen = snapshotNativeChooserWindows();
		captureNativeChooserScreen(beforeOpen, "issue-398-native-" + route.toLowerCase(Locale.ROOT).replace(' ', '-')
			+ "-before-open.png");
		PreparedNativeChooserProbe probe = null;
		Instant chooserInvokedAt = null;
		try {
			probe = PreparedNativeChooserProbe.start(route + "-cancel");
			probe.awaitPrepared();
			openCommand.run();
			chooserInvokedAt = probe.startReadinessDeadline();
			GuiAcceptanceSupport.await(chooserCallStarted::get,
				route + " did not enter the default Windows native chooser provider for Cancel acceptance");
			NativeCancelControl initialCancel = requireNativeCancelControl(awaitNativeChooserSnapshot(probe, route), route);
			assertFalse(chooserCallReturned.get(), route + " native chooser returned before physical Cancel");
			clickNativePoint(robot, centerX(initialCancel.dialogBounds),
				initialCancel.dialogBounds.y + Math.max(12, Math.min(18, initialCancel.dialogBounds.height / 12)));
			robot.delay(150); // allow foreground activation and native window geometry to settle

			// Re-query the Win32 child control after activation; the overlay-derived
			// bounds may have been stale if activating the title bar moved the dialog.
			NativeCancelControl activeCancel = requireNativeCancelControl(
				captureNativeWindowState(route + "-cancel-activated"), route);
			assertEquals(initialCancel.dialogHandle, activeCancel.dialogHandle,
				route + " native chooser dialog identity must remain stable through activation");
			captureNativeChooserScreen(robot, "issue-398-native-ribbon-open-before-cancel.png");
			java.awt.Point cancelPoint = nativePointToRobot(activeCancel.cancelBounds,
				captureNativeWindowState(route + "-cancel-coordinate-map"));
			clickNativePoint(robot, cancelPoint.x, cancelPoint.y);
			GuiAcceptanceSupport.await(chooserCallReturned::get,
				route + " physical click on Win32 IDCANCEL did not return from the provider");
			assertTrue(chooserCancelled.get(), route + " native chooser Cancel must report cancellation");
			captureNativeChooserScreen(robot, "issue-398-native-ribbon-after-cancel.png");
		} catch (Exception | Error failure) {
			captureNativeChooserFailureDiagnostics(route + "-cancel", robot, beforeOpen, chooserCallStarted,
				chooserCallReturned, chooserCancelled, chooserFailure, windowsBeforeOpen,
				chooserInvokedAt == null ? Instant.now() : chooserInvokedAt, failure, providerStartedAt,
				providerReturnedAt, ownerAtProviderEntry, ownerWindowAtProviderEntry.get(), probe);
			throw failure;
		} finally {
			if (probe != null) probe.close();
			if (chooserCallStarted.get() && !chooserCallReturned.get()) {
				robot.keyPress(java.awt.event.KeyEvent.VK_ESCAPE);
				try {
					robot.keyRelease(java.awt.event.KeyEvent.VK_ESCAPE);
				} finally {
					robot.waitForIdle();
				}
			}
		}
	}

	private static void clickNativePoint(Robot robot, int x, int y) {
		robot.mouseMove(x, y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		try {
			robot.waitForIdle();
		} finally {
			robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		}
		robot.waitForIdle();
	}

	private static int centerX(Rectangle bounds) {
		return bounds.x + bounds.width / 2;
	}

	private static int centerY(Rectangle bounds) {
		return bounds.y + bounds.height / 2;
	}

	static NativeCancelControl parseNativeCancelControl(String snapshot, String route, long targetPid) {
		List<String> ownedVisibleDialogs = new ArrayList<>();
		List<NativeCancelControl> matching = new ArrayList<>();
		String foregroundLine = null;
		boolean ready = false;
		for (String line : snapshot.split("\\R")) {
			if (line.startsWith("foreground ")) foregroundLine = line;
			if (line.equals("nativeChooserReady=True")) ready = true;
			if (line.startsWith("ownedDialog ")) {
				Map<String, String> dialog = parseFields(line);
				if (Long.toString(targetPid).equals(dialog.get("pid"))
						&& "#32770".equals(dialog.get("class"))
						&& Boolean.parseBoolean(dialog.get("visible")) && Boolean.parseBoolean(dialog.get("enabled"))
						&& Long.toString(targetPid).equals(dialog.get("ownerPid"))
						&& parseBounds(dialog.get("bounds")) != null) ownedVisibleDialogs.add(line);
				continue;
			}
			if (!line.startsWith("cancelControl ")) continue;
			Map<String, String> fields = parseFields(line);
			if (!Long.toString(targetPid).equals(fields.get("pid"))) continue;
			if (!"#32770".equals(fields.get("dialogClass")) || !Boolean.parseBoolean(fields.get("dialogOwnedByTarget"))
					|| !"2".equals(fields.get("controlId")) || !"Button".equals(fields.get("class"))
					|| !Boolean.parseBoolean(fields.get("dialogVisible")) || !Boolean.parseBoolean(fields.get("dialogEnabled"))
					|| !Boolean.parseBoolean(fields.get("visible")) || !Boolean.parseBoolean(fields.get("enabled"))) continue;
			Rectangle controlBounds = parseBounds(fields.get("bounds"));
			Rectangle dialogBounds = parseBounds(fields.get("dialogBounds"));
			if (controlBounds == null || dialogBounds == null || !dialogBounds.contains(controlBounds)) continue;
			matching.add(new NativeCancelControl(fields.get("dialog"), dialogBounds, controlBounds));
		}
		assertEquals(1, ownedVisibleDialogs.size(),
			route + " must expose exactly one visible, enabled, directly owned #32770 file dialog; snapshot=" + snapshot);
		assertEquals(1, matching.size(),
			route + " must expose exactly one visible, enabled direct IDCANCEL (2) Button in the owned #32770 dialog; snapshot="
				+ snapshot);
		NativeCancelControl control = matching.get(0);
		Map<String, String> foreground = parseFields(foregroundLine);
		String foregroundPid = foreground.get("pid");
		String foregroundHandle = foreground.get("hwnd");
		boolean foregroundInDialog = control.dialogHandle.equalsIgnoreCase(foregroundHandle)
			|| snapshot.lines().anyMatch(line -> line.startsWith("nativeChild ")
				&& control.dialogHandle.equalsIgnoreCase(parseFields(line).get("dialog"))
				&& Long.toString(targetPid).equals(parseFields(line).get("pid"))
				&& Boolean.parseBoolean(parseFields(line).get("visible"))
				&& Boolean.parseBoolean(parseFields(line).get("enabled"))
				&& foregroundHandle != null
				&& foregroundHandle.equalsIgnoreCase(parseFields(line).get("hwnd")));
		assertTrue(ready && Long.toString(targetPid).equals(foregroundPid) && foregroundInDialog,
			route + " native chooser must be the foreground dialog (or own foreground child); snapshot=" + snapshot);
		return control;
	}

	private static Map<String, String> parseFields(String line) {
		Map<String, String> fields = new HashMap<>();
		if (line == null) return fields;
		for (String field : line.split(" ")) {
			int equals = field.indexOf('=');
			if (equals > 0) fields.put(field.substring(0, equals), field.substring(equals + 1));
		}
		return fields;
	}

	private static java.awt.Point nativePointToRobot(Rectangle nativeControlBounds, String nativeSnapshot) {
		String displayLine = nativeSnapshot.lines().filter(line -> line.startsWith("display primaryBounds=") )
			.findFirst().orElseThrow(() -> new AssertionError("Native desktop snapshot omitted primary display bounds"));
		Rectangle nativeScreen = parseBounds(displayLine.substring("display primaryBounds=".length()));
		if (nativeScreen == null) throw new AssertionError("Invalid Win32 primary display bounds: " + displayLine);
		GraphicsConfiguration graphics = GraphicsEnvironment.getLocalGraphicsEnvironment()
			.getDefaultScreenDevice().getDefaultConfiguration();
		Rectangle logicalScreen = graphics.getBounds();
		if (logicalScreen.width <= 0 || logicalScreen.height <= 0 || !nativeScreen.contains(nativeControlBounds))
			throw new AssertionError("Invalid display geometry: AWT logical bounds=" + logicalScreen
				+ "; Win32 primary bounds=" + nativeScreen + "; native control=" + nativeControlBounds);
		double scaleX = (double) nativeScreen.width / logicalScreen.width;
		double scaleY = (double) nativeScreen.height / logicalScreen.height;
		if (!Double.isFinite(scaleX) || !Double.isFinite(scaleY) || scaleX <= 0
				|| Math.abs(scaleX - scaleY) > Math.max(scaleX, scaleY) * 0.01)
			throw new AssertionError("Cannot map Win32 chooser coordinates to AWT Robot coordinates: native="
				+ nativeScreen + ", logical=" + logicalScreen + ", scales=" + scaleX + "," + scaleY);
		int nativeX = nativeControlBounds.x + nativeControlBounds.width / 2;
		int nativeY = nativeControlBounds.y + nativeControlBounds.height / 2;
		int robotX = logicalScreen.x + (int) Math.round((nativeX - nativeScreen.x) / scaleX);
		int robotY = logicalScreen.y + (int) Math.round((nativeY - nativeScreen.y) / scaleY);
		if (!logicalScreen.contains(robotX, robotY))
			throw new AssertionError("Mapped IDCANCEL point is outside AWT screen: point=" + robotX + "," + robotY
				+ ", logical=" + logicalScreen + ", native=" + nativeScreen);
		System.err.println("Native IDCANCEL coordinate map: nativeScreen=" + nativeScreen + " logicalScreen="
			+ logicalScreen + " scale=" + scaleX + "," + scaleY + " nativePoint=" + nativeX + "," + nativeY
			+ " robotPoint=" + robotX + "," + robotY);
		return new java.awt.Point(robotX, robotY);
	}

	private static void logNativeChooserForegroundAtKey(String route, String snapshot) {
		String foregroundLine = snapshot.lines().filter(line -> line.startsWith("foreground ")).findFirst().orElse("foreground unavailable");
		Map<String, String> foreground = parseFields(foregroundLine);
		boolean chooserForeground = snapshot.lines().anyMatch(line -> line.equals("nativeChooserReady=True"));
		System.err.println("Native chooser key-send focus: route=" + route + " chooserForeground=" + chooserForeground
			+ " foregroundHwnd=" + foreground.get("hwnd") + " foregroundPid=" + foreground.get("pid")
			+ " foregroundTitle=" + foreground.get("title") + "; snapshot follows\n" + snapshot);
	}

	private static NativeCancelControl requireNativeCancelControl(String snapshot, String route) {
		return parseNativeCancelControl(snapshot, route, ProcessHandle.current().pid());
	}

	private static String awaitNativeChooserSnapshot(PreparedNativeChooserProbe probe, String route) throws IOException {
		try {
			NativeChooserProbeCompletion.Result result = probe.awaitReadiness();
			if (result.status() == NativeChooserProbeCompletion.Status.COMPLETE) return result.output();
			if (result.status() == NativeChooserProbeCompletion.Status.PROCESS_FAILED)
				throw new IOException("Native chooser probe exited " + result.exitCode() + "; output=" + result.output());
			if (result.status() == NativeChooserProbeCompletion.Status.READY_BUT_INCOMPLETE)
				throw new IOException(route + " chooser was ready within the " + NATIVE_CHOOSER_READINESS_BUDGET.toSeconds()
					+ "-second native-dialog readiness budget but the probe snapshot did not finish its bounded 2-second serialization drain; output="
					+ result.output());
			throw new IOException(route + " native chooser readiness was not observed within the "
				+ NATIVE_CHOOSER_READINESS_BUDGET.toSeconds() + "-second native-dialog readiness budget; output="
				+ result.output());
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
			throw new IOException("Native chooser probe interrupted", interrupted);
		}
	}

	private static String readNativeChooserProbeOutput(Path output) {
		try {
			return Files.exists(output) ? Files.readString(output) : "(no output)";
		} catch (IOException failure) {
			return "(unable to read probe output: " + failure + ")";
		}
	}

	private static Rectangle parseBounds(String value) {
		if (value == null || "unavailable".equals(value)) return null;
		String[] coordinates = value.split(",");
		if (coordinates.length != 4) return null;
		try {
			int x = Integer.parseInt(coordinates[0]);
			int y = Integer.parseInt(coordinates[1]);
			int width = Integer.parseInt(coordinates[2]);
			int height = Integer.parseInt(coordinates[3]);
			if (width <= 0 || height <= 0) return null;
			return new Rectangle(x, y, width, height);
		} catch (NumberFormatException invalidBounds) {
			return null;
		}
	}

	private static final class PreparedNativeChooserProbe implements AutoCloseable {
		private final Process process;
		private final Path output;
		private final Path trigger;
		private final Path lifecycleLog;
		private long deadlineNanos;
		private Instant deadlineUtc;
		private boolean triggered;
		private boolean retained;

		private PreparedNativeChooserProbe(Process process, Path output, Path trigger, Path lifecycleLog) {
			this.process = process;
			this.output = output;
			this.trigger = trigger;
			this.lifecycleLog = lifecycleLog;
		}

		static PreparedNativeChooserProbe start(String route) throws IOException {
			Path directory = guiArtifactDirectory();
			Files.createDirectories(directory);
			String name = "issue-398-native-" + route.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-]", "-");
			Path output = directory.resolve(name + "-ready-windows.txt");
			Path trigger = directory.resolve(name + "-trigger.txt");
			Path lifecycleLog = directory.resolve(name + "-window-events.txt");
			Files.deleteIfExists(output);
			Files.deleteIfExists(trigger);
			Files.deleteIfExists(lifecycleLog);
			Path script = Path.of(System.getProperty("microproject.project.dir", ".")).toAbsolutePath()
				.resolve(".github").resolve("scripts").resolve("capture-gui-desktop-state.ps1");
			Process process = new ProcessBuilder("powershell.exe", "-NoLogo", "-NoProfile", "-NonInteractive",
				"-WindowStyle", "Hidden", "-ExecutionPolicy", "Bypass", "-File", script.toString(), "-TargetPid",
				Long.toString(ProcessHandle.current().pid()), "-PrepareNativeChooser", "-ProbeTriggerFile", trigger.toString(),
				"-LifecycleLogFile", lifecycleLog.toString()).redirectErrorStream(true).redirectOutput(output.toFile()).start();
			return new PreparedNativeChooserProbe(process, output, trigger, lifecycleLog);
		}

		void awaitPrepared() throws IOException {
			try {
				NativeChooserProbeCompletion.awaitPrepared(process, () -> readNativeChooserProbeOutput(output),
					java.time.Duration.ofSeconds(15));
			} catch (InterruptedException interrupted) {
				Thread.currentThread().interrupt();
				throw new IOException("Interrupted while preparing native chooser observer", interrupted);
			}
		}

		Instant startReadinessDeadline() throws IOException {
			if (triggered) throw new IllegalStateException("Native chooser probe already triggered");
			long deadlineEpochMillis = System.currentTimeMillis() + NATIVE_CHOOSER_READINESS_BUDGET.toMillis();
			deadlineNanos = System.nanoTime() + NATIVE_CHOOSER_READINESS_BUDGET.toNanos();
			deadlineUtc = Instant.ofEpochMilli(deadlineEpochMillis);
			String request = "callId=" + Long.toUnsignedString(System.nanoTime()) + "\ndeadlineUtcTicks="
				+ (621355968000000000L + deadlineEpochMillis * 10_000L) + "\n";
			Path temporary = trigger.resolveSibling(trigger.getFileName() + ".tmp");
			Files.writeString(temporary, request, java.nio.charset.StandardCharsets.UTF_8);
			try {
				Files.move(temporary, trigger, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
			} catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
				Files.move(temporary, trigger);
			}
			triggered = true;
			return Instant.now();
		}

		NativeChooserProbeCompletion.Result awaitReadiness() throws IOException, InterruptedException {
			if (!triggered) throw new IllegalStateException("Native chooser probe has not been triggered");
			return NativeChooserProbeCompletion.await(process, () -> readNativeChooserProbeOutput(output), deadlineNanos,
				deadlineUtc, java.time.Duration.ofSeconds(2), java.time.Duration.ofMillis(250));
		}

		String stopAndRead() throws IOException {
			String events = Files.exists(lifecycleLog) ? Files.readString(lifecycleLog) : "(no event log)";
			if (events.length() > 32 * 1024) events = events.substring(0, 32 * 1024) + "\n[truncated at 32 KiB]";
			retained = true;
			return "path=" + lifecycleLog + " processExit=" + (process.isAlive() ? "running" : process.exitValue())
				+ "\n" + events + "\nobserverOutput=" + readNativeChooserProbeOutput(output);
		}

		@Override
		public void close() {
			try {
				if (process.isAlive()) {
					process.destroyForcibly();
					process.waitFor(1, java.util.concurrent.TimeUnit.SECONDS);
				}
				if (!retained) {
					Files.deleteIfExists(lifecycleLog);
					Files.deleteIfExists(output);
				}
				Files.deleteIfExists(trigger);
			} catch (Exception ignored) {
				process.destroyForcibly();
			}
		}
	}

	static final class NativeCancelControl {
		final String dialogHandle;
		final Rectangle dialogBounds;
		final Rectangle cancelBounds;

		private NativeCancelControl(String dialogHandle, Rectangle dialogBounds, Rectangle cancelBounds) {
			this.dialogHandle = dialogHandle;
			this.dialogBounds = dialogBounds;
			this.cancelBounds = cancelBounds;
		}
	}

	private static Path guiArtifactDirectory() {
		return Path.of(System.getProperty("microproject.gui.artifacts.dir", "build/reports/guiTest-artifacts"));
	}

	private static void captureNativeChooserFailureDiagnostics(String route, Robot robot, BufferedImage beforeOpen,
			AtomicBoolean chooserCallStarted, AtomicBoolean chooserCallReturned, AtomicBoolean chooserCancelled,
			AtomicReference<Throwable> chooserFailure, String windowsBeforeOpen, Instant chooserInvokedAt,
			Throwable originalFailure, AtomicReference<Instant> providerStartedAt,
			AtomicReference<Instant> providerReturnedAt, AtomicReference<String> ownerAtProviderEntry, Window owner,
			PreparedNativeChooserProbe probe) {
		Instant capturedAt = Instant.now();
		String diagnostic = "Native chooser failure snapshot route=" + route + " capturedUtc=" + capturedAt
			+ " chooserInvokedUtc=" + chooserInvokedAt
			+ " elapsedMs=" + Duration.between(chooserInvokedAt, capturedAt).toMillis()
			+ " providerStartedUtc=" + providerStartedAt.get()
			+ " providerReturnedUtc=" + providerReturnedAt.get()
			+ " ownerAtProviderEntry=" + ownerAtProviderEntry.get()
			+ " ownerHwndSource=targetPidWin32SnapshotMatchByTitle"
			+ " ownerAfterFailure=" + describeChooserOwner(owner)
			+ " state=" + describeNativeChooserState(chooserCallStarted, chooserCallReturned, chooserCancelled,
				chooserFailure, windowsBeforeOpen);
		System.err.println(diagnostic);
		originalFailure.addSuppressed(new AssertionError(diagnostic));
		try {
			String events = probe == null ? "windowEventStatus=unavailable reason=probe-not-started" : probe.stopAndRead();
			originalFailure.addSuppressed(new AssertionError("Native chooser HWND lifecycle timeline:\n" + events));
		} catch (Exception captureFailure) {
			originalFailure.addSuppressed(captureFailure);
		}
		try {
			String dump = captureNativeChooserThreadDump(route, chooserInvokedAt);
			originalFailure.addSuppressed(new AssertionError("Full JVM thread dump saved and printed: " + dump));
		} catch (Exception captureFailure) {
			originalFailure.addSuppressed(captureFailure);
		}
		try {
			String routeName = route.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-]", "-");
			captureNativeChooserScreen(captureScreen(robot), "issue-398-native-" + routeName + "-failure.png");
		} catch (Exception captureFailure) {
			originalFailure.addSuppressed(captureFailure);
		}
		try {
			String nativeWindows = captureNativeWindowState(route + "-failure");
			originalFailure.addSuppressed(new AssertionError("Native Win32 desktop snapshot:\n" + nativeWindows));
		} catch (Exception captureFailure) {
			originalFailure.addSuppressed(captureFailure);
		}
		try {
			String nativeStack = captureNativeChooserOsThreadStack(chooserCallStarted.get(), chooserCallReturned.get());
			originalFailure.addSuppressed(new AssertionError("Native chooser OS-thread stack:\n" + nativeStack));
		} catch (Exception captureFailure) {
			originalFailure.addSuppressed(captureFailure);
		}
	}

	private static String captureNativeChooserOsThreadStack(boolean chooserStarted, boolean chooserReturned) throws IOException {
		Path output = guiArtifactDirectory().resolve("issue-398-native-os-thread-stack.txt");
		Files.createDirectories(output.getParent());
		if (!chooserStarted || chooserReturned) {
			String skipped = "nativeStackStatus=skipped chooserStarted=" + chooserStarted + " chooserReturned=" + chooserReturned;
			Files.writeString(output, skipped + System.lineSeparator());
			return skipped;
		}
		String cdb = "C:\\Program Files (x86)\\Windows Kits\\10\\Debuggers\\x64\\cdb.exe";
		if (!Files.isRegularFile(Path.of(cdb))) {
			String unavailable = "nativeStackStatus=unavailable reason=cdb-not-installed expected=" + cdb;
			Files.writeString(output, unavailable + System.lineSeparator());
			return unavailable;
		}
		long pid = ProcessHandle.current().pid();
		Path jcmd = Path.of(System.getProperty("java.home"), "bin", "jcmd.exe");
		if (!Files.isRegularFile(jcmd)) {
			String unavailable = "nativeStackStatus=unavailable reason=jcmd-not-found path=" + jcmd;
			Files.writeString(output, unavailable + System.lineSeparator());
			return unavailable;
		}
		Path jcmdOutput = guiArtifactDirectory().resolve("issue-398-native-jcmd-threads.txt");
		Process dumpProcess = new ProcessBuilder(jcmd.toString(), Long.toString(pid), "Thread.print", "-l")
			.redirectErrorStream(true).redirectOutput(jcmdOutput.toFile()).start();
		String javaDump;
		try {
			if (!dumpProcess.waitFor(3, java.util.concurrent.TimeUnit.SECONDS)) {
				dumpProcess.destroyForcibly();
				Files.deleteIfExists(jcmdOutput);
				String unavailable = "nativeStackStatus=unavailable reason=jcmd-timeout";
				Files.writeString(output, unavailable + System.lineSeparator());
				return unavailable;
			}
			javaDump = Files.readString(jcmdOutput, java.nio.charset.StandardCharsets.UTF_8);
			Files.deleteIfExists(jcmdOutput);
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
			Files.deleteIfExists(jcmdOutput);
			throw new IOException("Interrupted while locating the native chooser thread", interrupted);
		}
		java.util.regex.Matcher match = java.util.regex.Pattern
			.compile("(?m)^\\\"FlatLaf SystemFileChooser\\\"[^\\r\\n]*?\\[(\\d+)\\]").matcher(javaDump);
		if (!match.find()) {
			String unavailable = "nativeStackStatus=unavailable reason=chooser-os-tid-not-reported-by-jcmd";
			Files.writeString(output, unavailable + System.lineSeparator());
			return unavailable;
		}
		long tid = Long.parseLong(match.group(1));
		String command = "~~[0x" + Long.toHexString(tid) + "]s; k 20; qd";
		Process debugger = new ProcessBuilder(cdb, "-p", Long.toString(pid), "-c", command)
			.redirectErrorStream(true).redirectOutput(output.toFile()).start();
		try {
			if (!debugger.waitFor(8, java.util.concurrent.TimeUnit.SECONDS)) {
				debugger.destroyForcibly();
				debugger.waitFor(1, java.util.concurrent.TimeUnit.SECONDS);
				Files.writeString(output, "nativeStackStatus=unavailable reason=cdb-timeout pid=" + pid + " tid=" + tid
					+ System.lineSeparator(), java.nio.charset.StandardCharsets.UTF_8);
			} else if (debugger.exitValue() != 0) {
				Files.writeString(output, "nativeStackStatus=unavailable reason=cdb-exit-" + debugger.exitValue()
					+ " pid=" + pid + " tid=" + tid + System.lineSeparator(), java.nio.charset.StandardCharsets.UTF_8,
					java.nio.file.StandardOpenOption.APPEND);
			}
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
			debugger.destroyForcibly();
			throw new IOException("Interrupted while capturing native chooser thread", interrupted);
		}
		String stack = Files.readString(output, java.nio.charset.StandardCharsets.UTF_8);
		if (stack.length() > 64 * 1024) stack = stack.substring(0, 64 * 1024) + "\n[truncated at 64 KiB]";
		Files.writeString(output, stack, java.nio.charset.StandardCharsets.UTF_8);
		return "path=" + output + " pid=" + pid + " tid=" + tid + " bytes=" + stack.length() + "\n" + stack;
	}

	private static Window resolveChooserOwner(Object parent) {
		Component component = parent instanceof Component ? (Component) parent : null;
		return parent instanceof Window ? (Window) parent
			: component == null ? null : SwingUtilities.getWindowAncestor(component);
	}

	private static String describeChooserOwner(Object parent) {
		Window owner = resolveChooserOwner(parent);
		if (owner == null)
			return "parent=" + (parent == null ? "null" : parent.getClass().getName()) + ",window=none";
		return "parent=" + (parent == null ? "null" : parent.getClass().getName())
			+ ",window=" + owner.getClass().getName()
			+ "@" + Integer.toHexString(System.identityHashCode(owner))
			+ ",title=" + (owner instanceof Frame frame ? frame.getTitle() : "")
			+ ",displayable=" + owner.isDisplayable() + ",showing=" + owner.isShowing()
			+ ",active=" + owner.isActive() + ",focused=" + owner.isFocused()
			+ ",bounds=" + owner.getBounds()
			+ ",providerThread=" + Thread.currentThread().getName() + "#" + Thread.currentThread().getId();
	}

	private static String captureNativeChooserThreadDump(String route, Instant chooserInvokedAt) throws IOException {
		String routeName = route.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-]", "-");
		Path output = guiArtifactDirectory().resolve("issue-398-native-" + routeName + "-threads.txt");
		Files.createDirectories(output.getParent());
		ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
		ThreadInfo[] threads = threadBean.dumpAllThreads(true, true);
		Instant capturedAt = Instant.now();
		StringBuilder dump = new StringBuilder("Native chooser failure thread dump; route=")
			.append(route).append("; chooserInvokedUtc=").append(chooserInvokedAt)
			.append("; capturedUtc=").append(capturedAt)
			.append("; elapsedMs=").append(Duration.between(chooserInvokedAt, capturedAt).toMillis())
			.append("; threadCount=").append(threads.length).append(System.lineSeparator());
		for (ThreadInfo thread : threads) {
			dump.append('"').append(thread.getThreadName()).append("\" id=").append(thread.getThreadId())
				.append(" state=").append(thread.getThreadState()).append(" lock=").append(thread.getLockInfo())
				.append(" lockOwner=").append(thread.getLockOwnerName()).append('/')
				.append(thread.getLockOwnerId()).append(System.lineSeparator());
			for (StackTraceElement frame : thread.getStackTrace())
				dump.append("\tat ").append(frame).append(System.lineSeparator());
			dump.append("lockedMonitors=").append(java.util.Arrays.toString(thread.getLockedMonitors()))
				.append(System.lineSeparator()).append("lockedSynchronizers=")
				.append(java.util.Arrays.toString(thread.getLockedSynchronizers())).append(System.lineSeparator());
		}
		Files.writeString(output, dump.toString());
		System.err.println("=== Native chooser thread dump: " + output + " ===");
		System.err.print(dump);
		return output + " (" + threads.length + " threads)";
	}

	private static String captureNativeWindowState(String route) throws IOException {
		String routeName = route.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-]", "-");
		Path projectDirectory = Path.of(System.getProperty("microproject.project.dir", ".")).toAbsolutePath();
		Path script = projectDirectory.resolve(".github").resolve("scripts").resolve("capture-gui-desktop-state.ps1");
		Path output = guiArtifactDirectory().resolve("issue-398-native-" + routeName + "-windows.txt");
		Files.createDirectories(output.getParent());
		Process process = new ProcessBuilder("powershell.exe", "-NoLogo", "-NoProfile", "-NonInteractive", "-WindowStyle",
			"Hidden", "-ExecutionPolicy", "Bypass", "-File", script.toString(), "-TargetPid",
			Long.toString(ProcessHandle.current().pid()))
			.redirectErrorStream(true).redirectOutput(output.toFile()).start();
		try {
			if (!process.waitFor(8, java.util.concurrent.TimeUnit.SECONDS)) {
				process.destroyForcibly();
				process.waitFor(1, java.util.concurrent.TimeUnit.SECONDS);
				String partial = Files.exists(output) ? Files.readString(output) : "(no output)";
				System.err.println("Native desktop HWND capture timed out after 8s; output=" + output + "\n" + partial);
				throw new IOException("Native desktop HWND capture timed out after 8s; output=" + output);
			}
			String captured = Files.readString(output);
			System.err.println("=== Native desktop HWND capture: " + output + " (exit=" + process.exitValue() + ") ===");
			System.err.print(captured);
			if (process.exitValue() != 0)
				throw new IOException("Native desktop HWND capture exited " + process.exitValue() + "; output=" + output);
			return captured;
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
			process.destroyForcibly();
			System.err.println("Native desktop HWND capture interrupted; output=" + output);
			throw new IOException("Native desktop HWND capture interrupted; output=" + output, interrupted);
		}
	}

	private static void captureNativeChooserScreen(Robot robot, String artifactName) throws IOException {
		captureNativeChooserScreen(captureScreen(robot), artifactName);
	}

	private static void captureNativeChooserScreen(BufferedImage image, String artifactName) throws IOException {
		Path output = Path.of("build", "reports", "guiTest-artifacts", artifactName);
		Files.createDirectories(output.getParent());
		ImageIO.write(image, "png", output.toFile());
	}

	private static BufferedImage captureScreen(Robot robot) {
		Rectangle bounds = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
		return robot.createScreenCapture(bounds);
	}

	private static String describeDialogOverlay(BufferedImage before, BufferedImage after) {
		if (before.getWidth() != after.getWidth() || before.getHeight() != after.getHeight())
			return "overlay screenshot dimensions changed from " + before.getWidth() + "x" + before.getHeight()
				+ " to " + after.getWidth() + "x" + after.getHeight();
		int changedSamples = 0;
		int minX = before.getWidth();
		int minY = before.getHeight();
		int maxX = -1;
		int maxY = -1;
		for (int y = 20; y < before.getHeight(); y += 4) {
			for (int x = 20; x < before.getWidth(); x += 4) {
				if (!hasPixelDifferenceInSampleBlock(before, after, x, y, 4)) continue;
				changedSamples++;
				minX = Math.min(minX, x);
				minY = Math.min(minY, y);
				maxX = Math.max(maxX, x);
				maxY = Math.max(maxY, y);
			}
		}
		return "overlay samples=" + changedSamples + ", bounds="
			+ (maxX < 0 ? "none" : minX + "," + minY + " " + (maxX - minX + 1) + "x" + (maxY - minY + 1))
			+ ", minimum=80 samples and " + (before.getWidth() * 0.30) + "x" + (before.getHeight() * 0.20)
			+ " span on " + before.getWidth() + "x" + before.getHeight() + " desktop";
	}

	private static Rectangle visibleDialogOverlayBounds(BufferedImage before, BufferedImage after) {
		if (before.getWidth() != after.getWidth() || before.getHeight() != after.getHeight())
			return new Rectangle();
		int changedSamples = 0;
		int minX = before.getWidth();
		int minY = before.getHeight();
		int maxX = -1;
		int maxY = -1;
		for (int y = 20; y < before.getHeight(); y += 4) {
			for (int x = 20; x < before.getWidth(); x += 4) {
				if (!hasPixelDifferenceInSampleBlock(before, after, x, y, 4))
					continue;
				changedSamples++;
				minX = Math.min(minX, x);
				minY = Math.min(minY, y);
				maxX = Math.max(maxX, x);
				maxY = Math.max(maxY, y);
			}
		}
		if (changedSamples < 80 || maxX - minX < before.getWidth() * 0.30
				|| maxY - minY < before.getHeight() * 0.20)
			return new Rectangle();
		return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
	}

	private static String snapshotNativeChooserWindows() throws Exception {
		String[] snapshot = new String[1];
		SwingUtilities.invokeAndWait(() -> snapshot[0] = describeNativeChooserWindows());
		return snapshot[0];
	}

	private static String describeNativeChooserState(AtomicBoolean started, AtomicBoolean returned,
			AtomicBoolean cancelled, AtomicReference<Throwable> failure, String windowsBeforeOpen) {
		String error = failure.get() == null ? "none"
			: failure.get().getClass().getName() + ": " + failure.get().getMessage();
		return "native provider started=" + started.get() + ", returned=" + returned.get()
			+ ", cancelled=" + cancelled.get() + ", error=" + error + "; windows before open="
			+ windowsBeforeOpen + "; windows after failure=" + describeNativeChooserWindows();
	}

	private static String describeNativeChooserWindows() {
		StringBuilder state = new StringBuilder("AWT windows=[");
		boolean first = true;
		// The post-failure snapshot reads AWT window getters directly on the test
		// thread: the native modal chooser may have entered a secondary loop, so
		// invokeAndWait here could block the diagnosis.
		for (Window candidate : Window.getWindows()) {
			if (!candidate.isShowing() && !candidate.isActive() && !candidate.isFocused()) continue;
			if (!first) state.append("; ");
			first = false;
			state.append(candidate.getClass().getSimpleName())
				.append("{showing=").append(candidate.isShowing())
				.append(",active=").append(candidate.isActive())
				.append(",focused=").append(candidate.isFocused())
				.append(",modal=").append(candidate instanceof java.awt.Dialog dialog && dialog.isModal())
				.append(",bounds=").append(candidate.getBounds()).append('}');
		}
		Window activeWindow = java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
		Window focusedWindow = java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
		Component focusOwner = java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
		state.append("], activeWindow=").append(activeWindow == null ? "none" : activeWindow.getClass().getName())
			.append(", focusedWindow=").append(focusedWindow == null ? "none" : focusedWindow.getClass().getName())
			.append(", focusOwner=").append(focusOwner == null ? "none" : focusOwner.getClass().getName());
		return state.toString();
	}

			private static boolean hasPixelDifferenceInSampleBlock(BufferedImage before, BufferedImage after,
			int x, int y, int blockSize) {
		for (int sampleY = y; sampleY < Math.min(y + blockSize, before.getHeight()); sampleY++) {
			for (int sampleX = x; sampleX < Math.min(x + blockSize, before.getWidth()); sampleX++) {
				if (before.getRGB(sampleX, sampleY) != after.getRGB(sampleX, sampleY))
					return true;
			}
		}
		return false;
	}

	private static void pressCtrlO(Robot robot) {
		robot.keyPress(java.awt.event.KeyEvent.VK_CONTROL);
		robot.keyPress(java.awt.event.KeyEvent.VK_O);
		robot.keyRelease(java.awt.event.KeyEvent.VK_O);
		robot.keyRelease(java.awt.event.KeyEvent.VK_CONTROL);
	}

	@FunctionalInterface
	private interface GuiCommand {
		void run() throws Exception;
	}

	/**
	 * Starts the same standalone factory path as the desktop launcher.  Building
	 * a GraphicManager directly cannot prove that startup restored the connected
	 * command state before the File ribbon is shown.
	 */
	private void createStartedWindow(String title) throws Exception {
		previousJobQueue = SessionFactory.getInstance().getJobQueue();
		SwingUtilities.invokeAndWait(() -> {
			window = new MainRibbonFrame(title, null);
			manager = new ApplicationStartupFactory(new HashMap<>()).instanceFromNewSession(window, false);
			window.setGraphicManager(manager);
			window.setAlwaysOnTop(true);
			if (!window.isShowing()) {
				window.setSize(1200, 700);
				window.setLocationByPlatform(true);
				window.setAlwaysOnTop(true);
				window.setVisible(true);
			}
		});
		GuiAcceptanceSupport.await(() -> window.isShowing(), "startup ribbon window did not become visible");
	}

	private void clickAndClose(Robot robot, String commandId, Class<? extends Window> dialogType) throws Exception {
		AbstractButton button = findCommandButton(window.getRibbonPanel(), commandId, robot);
		assertTrue(button.isShowing(), commandId + " is not physically visible");
		assertTrue(button.isEnabled(), commandId + " is disabled in the real application state");
		click(robot, button);
		GuiAcceptanceSupport.await(() -> visibleDialog(dialogType) != null,
			commandId + " did not open " + dialogType.getSimpleName());
		Window dialog = visibleDialog(dialogType);
		assertDialogBodyIsRendered(dialog, commandId);
		if (dialog instanceof LocaleDialog)
			DialogLayoutAssertions.assertTextControlsAtPreferredHeight(dialog, "Locale Settings dialog (#590 body image 4)");
		if (dialog instanceof HelpDialog)
			DialogLayoutAssertions.assertTextControlsAtPreferredHeight(dialog, "Help dialog (#590 body image 3)");
		if (dialog instanceof LocaleDialog)
			DialogScreenshotArtifacts.capture((java.awt.Dialog) dialog, "issue-590-locale-settings");
		if (dialog instanceof HelpDialog)
			DialogScreenshotArtifacts.capture((java.awt.Dialog) dialog, "issue-590-help");
		clickCancel(robot, dialog);
		GuiAcceptanceSupport.await(() -> visibleDialog(dialogType) == null,
			commandId + " did not close its dialog through the physical Cancel route");
	}

	private static void clickCancel(Robot robot, Window dialog) throws Exception {
		assertTrue(dialog instanceof AbstractDialog, "Expected an AbstractDialog: " + dialog);
		SwingUtilities.invokeAndWait(() -> {
			dialog.toFront();
			dialog.requestFocus();
		});
		robot.keyPress(java.awt.event.KeyEvent.VK_ESCAPE);
		robot.keyRelease(java.awt.event.KeyEvent.VK_ESCAPE);
	}

	private static void click(Robot robot, Component component) throws Exception {
		Point[] screenLocation = new Point[1];
		int[] size = new int[2];
		SwingUtilities.invokeAndWait(() -> {
			assertTrue(component.isShowing(), "Cannot physically click a component that is no longer showing: " + component);
			screenLocation[0] = component.getLocationOnScreen();
			size[0] = component.getWidth();
			size[1] = component.getHeight();
		});
		Point point = screenLocation[0];
		robot.mouseMove(point.x + size[0] / 2, point.y + size[1] / 2);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
	}

	private static void assertDialogBodyIsRendered(Window dialog, String commandId) {
		assertTrue(dialog.getWidth() > 240 && dialog.getHeight() > 120,
			commandId + " opened only a title strip or zero-sized dialog: " + dialog.getSize());
		assertTrue(hasVisibleSizedChild(dialog),
			commandId + " dialog has no visible sized body component: " + dialog.getClass().getName());
	}

	private static boolean hasVisibleSizedChild(Component root) {
		if (!(root instanceof Container container)) return false;
		for (Component child : container.getComponents()) {
			if (!child.isVisible() || child.getWidth() <= 0 || child.getHeight() <= 0) continue;
			if (!(child instanceof javax.swing.JRootPane) && !(child instanceof javax.swing.JMenuBar)) return true;
			if (hasVisibleSizedChild(child)) return true;
		}
		return false;
	}

	/**
	 * Windows may give the first child-control click to an inactive top-level
	 * window merely to activate it.  Physically activate the test window first so
	 * the following assertion exercises the command click itself, not foreground
	 * activation policy of the desktop hosting Gradle.
	 */
	private void activateWindowForRobot(Robot robot) throws Exception {
		activateWindowForRobot(robot, true);
	}

	private void activateWindowForRobot(Robot robot, boolean alwaysOnTop) throws Exception {
		GuiAcceptanceSupport.activateAndAwaitRenderedWindow(window, window.getRibbonPanel(), robot, alwaysOnTop,
			"ribbon application window before the next physical command");
	}

	private static void type(Robot robot, String text) {
		for (char character : text.toCharArray()) {
			int keyCode = java.awt.event.KeyEvent.getExtendedKeyCodeForChar(character);
			if (Character.isUpperCase(character)) robot.keyPress(java.awt.event.KeyEvent.VK_SHIFT);
			robot.keyPress(keyCode);
			robot.keyRelease(keyCode);
			if (Character.isUpperCase(character)) robot.keyRelease(java.awt.event.KeyEvent.VK_SHIFT);
		}
	}

	private static Window visibleDialog(Class<? extends Window> type) {
		for (Window window : Window.getWindows()) {
			if (type.isInstance(window) && window.isShowing()) return window;
		}
		return null;
	}

	private static AbstractButton findCommandButton(Component root, String commandId) {
		AbstractButton match = null;
		for (Component component : flatten(root)) {
			if (component instanceof AbstractButton button
				&& commandId.equals(button.getActionCommand()) && button.isShowing()
				&& isInsideTopLevelContent(component)
				&& (match == null || component.getWidth() * component.getHeight() > match.getWidth() * match.getHeight())) {
				match = button;
			}
		}
		if (match != null) return match;
		throw new AssertionError("Ribbon command is not present: " + commandId);
	}

	/** Follow the user-visible File tab, Open page, and Open tile before waiting for a chooser. */
	private void clickBackstageOpenTile(Robot robot) throws Exception {
		Window owner = SwingUtilities.getWindowAncestor(window.getRibbonPanel());
		if (!(owner instanceof javax.swing.JFrame frame))
			throw new AssertionError("File/Open route has no application frame");
		if (findNamedOnEdt(frame, "officeBackstageOverlay") == null) {
			click(robot, findRibbonTab(window.getRibbonPanel(), "File", "ファイル"));
			GuiAcceptanceSupport.await(() -> findNamedOnEdt(frame, "officeBackstageView") != null,
				"File tab did not open Backstage before File/Open");
		}
		Component openPage = findNamedOnEdt(frame, "officeBackstageNav-open");
		if (!(openPage instanceof AbstractButton button))
			throw new AssertionError("Backstage navigation is missing the Open page");
		click(robot, button);
		String tileName = "officeBackstageCommand-RibbonOpenProject";
		GuiAcceptanceSupport.await(() -> {
			Component tile = findNamedOnEdt(frame, tileName);
			return tile instanceof AbstractButton && isEnabledAndShowingOnEdt(tile);
		}, "Backstage Open page did not expose its enabled Open tile");
		Component tile = findNamedOnEdt(frame, tileName);
		if (!(tile instanceof AbstractButton) || !isShowingOnEdt(tile))
			throw new AssertionError("Backstage Open tile is not physically visible");
		click(robot, tile);
	}

	private AbstractButton findCommandButton(Component root, String commandId, Robot robot) throws Exception {
		if (!Set.of("RibbonNewProject", "RibbonNewMasterProject", "RibbonOpenProject", "RibbonRecentProjects",
			"RibbonSaveProject", "RibbonSaveProjectAs", "RibbonSaveMpoAs", "RibbonCloseProject",
			"RibbonBackstageProjectInformation", "RibbonBackstageOptions",
			"RibbonImportProject", "RibbonExportProject", "RibbonPrint", "RibbonPrintPreview", "RibbonPDF",
			"RibbonLocale").contains(commandId)) {
			return findCommandButton(root, commandId);
		}
		Window owner = SwingUtilities.getWindowAncestor(root);
		if (!(owner instanceof javax.swing.JFrame frame))
			throw new AssertionError("File command route has no application frame: " + commandId);
		String targetName = switch (commandId) {
			case "RibbonSaveProject" -> "officeBackstageNav-save";
			case "RibbonCloseProject" -> "officeBackstageNav-close";
			default -> "officeBackstageCommand-" + commandId;
		};
		if (findNamedOnEdt(frame, "officeBackstageOverlay") == null) {
			click(robot, findRibbonTab(root, "File", "ファイル"));
			GuiAcceptanceSupport.await(() -> findNamedOnEdt(frame, "officeBackstageView") != null,
				"File tab did not open Backstage for command " + commandId);
		}
		String pageId = backstagePageFor(commandId);
		if (pageId != null) {
			Component pageButton = findNamedOnEdt(frame, "officeBackstageNav-" + pageId);
			if (!(pageButton instanceof AbstractButton button))
				throw new AssertionError("Backstage navigation is missing destination " + pageId);
			click(robot, button);
			robot.waitForIdle();
		}
		GuiAcceptanceSupport.await(() -> {
			Component target = findNamedOnEdt(frame, targetName);
			return target instanceof AbstractButton && isEnabledAndShowingOnEdt(target);
		}, "Backstage destination did not expose an enabled, visible command " + commandId);
		Component target = findNamedOnEdt(frame, targetName);
		if (!(target instanceof AbstractButton button) || !isShowingOnEdt(button)) {
			throw new AssertionError("Backstage command is not physically visible: " + commandId);
		}
		return button;
	}

	private void clickAndDispose(Robot robot, String commandId, Class<? extends Window> dialogType) throws Exception {
		AbstractButton button = findCommandButton(window.getRibbonPanel(), commandId, robot);
		assertTrue(isShowingOnEdt(button), commandId + " is not physically visible");
		assertTrue(button.isEnabled(), commandId + " is disabled in the real application state");
		click(robot, button);
		GuiAcceptanceSupport.await(() -> visibleDialog(dialogType) != null,
			commandId + " did not open " + dialogType.getSimpleName());
		Window dialog = visibleDialog(dialogType);
		assertDialogBodyIsRendered(dialog, commandId);
		SwingUtilities.invokeAndWait(dialog::dispose);
		GuiAcceptanceSupport.await(() -> visibleDialog(dialogType) == null,
			commandId + " dialog did not close after the acceptance action");
	}

	private static String backstagePageFor(String commandId) {
		return switch (commandId) {
			case "RibbonNewProject", "RibbonNewMasterProject" -> "new";
			case "RibbonOpenProject", "RibbonRecentProjects" -> "open";
			case "RibbonBackstageProjectInformation" -> "info";
			case "RibbonBackstageOptions", "RibbonLocale" -> "options";
			case "RibbonSaveProject" -> null;
			case "RibbonSaveProjectAs", "RibbonSaveMpoAs" -> "saveAs";
			case "RibbonPrint", "RibbonPrintPreview", "RibbonPDF" -> "print";
			case "RibbonImportProject", "RibbonExportProject" -> "export";
			case "RibbonCloseProject" -> null;
			default -> throw new IllegalArgumentException("Not a Backstage command: " + commandId);
		};
	}

	private static Component findNamed(Component root, String name) {
		if (name.equals(root.getName())) return root;
		if (root instanceof Container container) {
			for (Component child : container.getComponents()) {
				Component found = findNamed(child, name);
				if (found != null) return found;
			}
		}
		return null;
	}

	private static Component findNamedShowing(Component root, String name) {
		if (name.equals(root.getName()) && root.isShowing()) return root;
		if (root instanceof Container container) {
			for (Component child : container.getComponents()) {
				Component found = findNamedShowing(child, name);
				if (found != null) return found;
			}
		}
		return null;
	}

	private static Component findNamedOnEdt(Window window, String name) {
		Component[] match = new Component[1];
		try {
			SwingUtilities.invokeAndWait(() -> {
			Component root = SwingUtilities.getRootPane(window);
			Component visible = findNamedShowing(root, name);
			match[0] = visible != null ? visible : findNamed(root, name);
		});
		} catch (Exception exception) {
			throw new AssertionError("Could not inspect Backstage component " + name + " on the EDT", exception);
		}
		return match[0];
	}

	private static boolean isEnabledAndShowingOnEdt(Component component) {
		boolean[] state = new boolean[1];
		try {
			SwingUtilities.invokeAndWait(() -> state[0] = component.isShowing()
				&& (!(component instanceof AbstractButton button) || button.isEnabled()));
		} catch (Exception exception) {
			throw new AssertionError("Could not inspect Backstage command state on the EDT", exception);
		}
		return state[0];
	}

	private static boolean isShowingOnEdt(Component component) {
		boolean[] showing = new boolean[1];
		try {
			SwingUtilities.invokeAndWait(() -> showing[0] = component.isShowing());
		} catch (Exception exception) {
			throw new AssertionError("Could not inspect Backstage visibility on the EDT", exception);
		}
		return showing[0];
	}

	private static boolean isFocusOwnerOnEdt(Component component) {
		boolean[] focused = new boolean[1];
		try {
			SwingUtilities.invokeAndWait(() -> focused[0] = component.isFocusOwner());
		} catch (Exception exception) {
			throw new AssertionError("Could not inspect file chooser focus on the EDT", exception);
		}
		return focused[0];
	}

	private static String textOnEdt(JTextField field) {
		String[] text = new String[1];
		try {
			SwingUtilities.invokeAndWait(() -> text[0] = field.getText());
		} catch (Exception exception) {
			throw new AssertionError("Could not inspect file chooser text on the EDT", exception);
		}
		return text[0];
	}

	private static boolean isInsideTopLevelContent(Component component) {
		Window owner = SwingUtilities.getWindowAncestor(component);
		if (owner == null || !(owner instanceof javax.swing.JFrame frame)) return true;
		try {
			java.awt.Point content = frame.getContentPane().getLocationOnScreen();
			java.awt.Point button = component.getLocationOnScreen();
			return button.x >= content.x && button.y >= content.y
				&& button.x < content.x + frame.getContentPane().getWidth()
				&& button.y < content.y + frame.getContentPane().getHeight();
		} catch (IllegalComponentStateException ignored) {
			return false;
		}
	}

	private static AbstractButton findRibbonTab(Component root, String... titles) {
		for (Component component : flatten(root)) {
			if (component instanceof AbstractButton button && button.isShowing()) {
				for (String title : titles) {
					if (title.equals(button.getText())) return button;
				}
			}
		}
		throw new AssertionError("Requested ribbon tab is not physically visible: " + String.join(", ", titles));
	}

	private static AbstractButton findButton(Component root, String text) {
		for (Component component : flatten(root)) {
			if (component instanceof AbstractButton button && text.equals(button.getText()) && button.isShowing())
				return button;
		}
		throw new AssertionError("Visible dialog button is not present: " + text);
	}

	private static JTextField findProjectNameField(ProjectDialog dialog) {
		for (Component component : flatten(dialog)) {
			if (component instanceof JTextField field && field.isShowing()) return field;
		}
		throw new AssertionError("New Project dialog does not expose a visible project-name field");
	}

	private static Window visibleFileChooserDialog() {
		for (Window candidate : Window.getWindows()) {
			if (candidate.isShowing() && flatten(candidate).stream().anyMatch(JFileChooser.class::isInstance))
				return candidate;
		}
		return null;
	}

	private static Window visibleOptionDialog() {
		for (Window candidate : Window.getWindows()) {
			if (candidate.isShowing() && flatten(candidate).stream().anyMatch(javax.swing.JOptionPane.class::isInstance))
				return candidate;
		}
		return null;
	}

	private static JTextField findFileChooserNameField(Window chooserWindow) {
		for (Component component : flatten(chooserWindow)) {
			if (component instanceof JTextField field && field.isShowing()) return field;
		}
		throw new AssertionError("Visible save dialog has no filename field");
	}

	private static List<String> taskSemantics(Project project) {
		List<String> summary = new ArrayList<>();
		for (java.util.Iterator<Task> tasks = project.getTaskOutlineIterator(); tasks.hasNext();) {
			Task task = tasks.next();
			summary.add(task.getName() + "|" + task.getCurrentSchedule().getStart()
				+ "|" + task.getCurrentSchedule().getFinish());
		}
		return summary;
	}

	private static List<String> assignmentBaselineSemantics(Project project) {
		List<String> summary = new ArrayList<>();
		for (Task task : project.getTaskList()) {
			if (!(task instanceof com.microproject.pm.task.NormalTask normalTask)) continue;
			for (Object value : normalTask.getAssignments()) {
				com.microproject.pm.assignment.Assignment assignment = (com.microproject.pm.assignment.Assignment) value;
				for (int slot = 0; slot <= 10; slot++) {
					com.microproject.pm.assignment.Assignment.BaselineValues baseline = assignment.getBaselineValues(slot);
					if (baseline != null && (baseline.start() != null || baseline.finish() != null
							|| (baseline.work() != null && baseline.work().longValue() != 0L)))
						summary.add(task.getName() + "|" + assignment.getResource().getName() + "|" + slot
							+ "|" + baseline.start() + "|" + baseline.finish() + "|" + baseline.work());
				}
			}
		}
		return summary;
	}

	private void awaitWithWindowTrace(BooleanSupplier condition, String message) throws Exception {
		try {
			GuiAcceptanceSupport.await(condition, message);
		} catch (AssertionError failure) {
			throw new AssertionError(failure.getMessage() + "\n" + saveWindowTrace.snapshot(), failure);
		}
	}

	/** Captures modal ownership and focus from AWT window events on the EDT for failure diagnostics. */
	private static final class WindowTrace implements AWTEventListener {
		private final List<String> events = java.util.Collections.synchronizedList(new ArrayList<>());

		void open() {
			Toolkit.getDefaultToolkit().addAWTEventListener(this,
				AWTEvent.WINDOW_EVENT_MASK | AWTEvent.WINDOW_FOCUS_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK);
		}

		void close() {
			Toolkit.getDefaultToolkit().removeAWTEventListener(this);
		}

		@Override public void eventDispatched(AWTEvent event) {
			if (event instanceof WindowEvent windowEvent) {
				Window changed = windowEvent.getWindow();
				events.add(windowEvent.getID() + " " + describeWindow(changed) + " | " + describeAllWindows());
			} else if (event instanceof MouseEvent mouseEvent
					&& (mouseEvent.getID() == MouseEvent.MOUSE_PRESSED || mouseEvent.getID() == MouseEvent.MOUSE_RELEASED)) {
				Component source = mouseEvent.getComponent();
				Point screen = new Point(mouseEvent.getX(), mouseEvent.getY());
				try { SwingUtilities.convertPointToScreen(screen, source); }
				catch (IllegalComponentStateException ignored) { }
				String target = source instanceof AbstractButton button ? describeButton(button)
					: source.getClass().getSimpleName() + "[bounds=" + source.getBounds() + "]";
				events.add("mouse " + mouseEvent.getID() + " screen=" + screen + " target=" + target);
			}
		}

		String snapshot() {
			synchronized (events) {
				return "AWT modal/focus trace (last 20 events):\n  "
					+ String.join("\n  ", events.subList(Math.max(0, events.size() - 20), events.size()));
			}
		}

		private static String describeAllWindows() {
			List<String> descriptions = new ArrayList<>();
			for (Window candidate : Window.getWindows())
				descriptions.add(describeWindow(candidate));
			KeyboardFocusManager focus = KeyboardFocusManager.getCurrentKeyboardFocusManager();
			return "windows=" + descriptions + ", active=" + describeWindow(focus.getActiveWindow())
				+ ", focused=" + describeWindow(focus.getFocusedWindow()) + ", focusOwner="
				+ (focus.getFocusOwner() == null ? "none" : focus.getFocusOwner().getClass().getName());
		}

		private static String describeWindow(Window candidate) {
			if (candidate == null) return "none";
			String title = candidate instanceof Frame frame ? frame.getTitle()
				: candidate instanceof Dialog dialog ? dialog.getTitle() : "";
			Window owner = candidate.getOwner();
			String ownerTitle = owner instanceof Frame frame ? frame.getTitle()
				: owner instanceof Dialog dialog ? dialog.getTitle() : "";
			StringBuilder details = new StringBuilder(candidate.getClass().getSimpleName())
				.append("{title='").append(title).append("',visible=").append(candidate.isVisible())
				.append(",showing=").append(candidate.isShowing()).append(",active=").append(candidate.isActive())
				.append(",focused=").append(candidate.isFocused()).append(",modal=")
				.append(candidate instanceof Dialog dialog && dialog.isModal())
				.append(",owner=").append(owner == null ? "none" : owner.getClass().getSimpleName() + "['" + ownerTitle + "']")
				.append(",bounds=").append(candidate.getBounds());
			if (candidate instanceof JDialog dialog) {
				List<String> text = new ArrayList<>();
				for (Component component : flatten(dialog)) {
					if (component instanceof JLabel label && label.getText() != null && !label.getText().isBlank())
						text.add(label.getText().trim());
					if (component instanceof JFileChooser chooser && chooser.getSelectedFile() != null)
						text.add("selectedFile=" + chooser.getSelectedFile());
				}
				details.append(",content=").append(text.stream().limit(12).toList());
			}
			return details.append('}').toString();
		}
	}

	private static JFileChooser findFileChooser(Window chooserWindow) {
		return flatten(chooserWindow).stream().filter(JFileChooser.class::isInstance)
			.map(JFileChooser.class::cast).findFirst()
			.orElseThrow(() -> new AssertionError("Visible save dialog has no JFileChooser"));
	}

	private static AbstractButton selectFileChooserSaveButton(Window chooserWindow) {
		JFileChooser chooser = findFileChooser(chooserWindow);
		Set<String> saveLabels = new java.util.HashSet<>();
		if (chooser.getApproveButtonText() != null) saveLabels.add(chooser.getApproveButtonText());
		String swingSaveLabel = UIManager.getString("FileChooser.saveButtonText");
		if (swingSaveLabel != null) saveLabels.add(swingSaveLabel);
		saveLabels.add(Messages.getString("Text.Save"));
		List<AbstractButton> candidates = new ArrayList<>();
		for (Component component : flatten(chooserWindow)) {
			if (component instanceof AbstractButton button && button.isShowing()
					&& ("ApproveSelection".equals(button.getActionCommand())
						|| saveLabels.contains(button.getText())))
				candidates.add(button);
		}
		if (candidates.size() != 1)
			throw new AssertionError("Expected exactly one visible Save As approve button, found " + candidates.size()
				+ "; candidates=" + candidates.stream().map(RibbonExternalCommandGuiAcceptanceTest::describeButton)
					.toList() + "; all buttons=" + flatten(chooserWindow).stream()
					.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
					.map(RibbonExternalCommandGuiAcceptanceTest::describeButton).toList());
		return candidates.get(0);
	}

	private static String saveAsChooserDiagnostics(Window chooserWindow, AbstractButton approveButton) throws Exception {
		AtomicReference<String> snapshot = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> {
			JFileChooser chooser = findFileChooser(chooserWindow);
			JTextField nameField = findFileChooserNameField(chooserWindow);
			java.awt.Container viewport = SwingUtilities.getAncestorOfClass(javax.swing.JViewport.class, approveButton);
			String viewportState = viewport instanceof javax.swing.JViewport view
				? "viewportBounds=" + safeScreenBounds(view)
					+ ", viewPosition=" + view.getViewPosition() + ", viewSize=" + view.getViewSize()
				: "viewport=none";
			KeyboardFocusManager focus = KeyboardFocusManager.getCurrentKeyboardFocusManager();
			snapshot.set("dialog=" + WindowTrace.describeWindow(chooserWindow)
				+ "\nchooser.selectedFile=" + chooser.getSelectedFile()
				+ ", currentDirectory=" + chooser.getCurrentDirectory()
				+ ", nameFieldText='" + nameField.getText() + "', nameFieldBounds=" + nameField.getBounds()
				+ ", nameFieldFocus=" + nameField.isFocusOwner()
				+ "\napprove=" + describeButton(approveButton)
				+ ", approveScreenBounds=" + safeScreenBounds(approveButton)
				+ ", " + viewportState
				+ "\nactive=" + WindowTrace.describeWindow(focus.getActiveWindow())
				+ ", focused=" + WindowTrace.describeWindow(focus.getFocusedWindow())
				+ ", focusOwner=" + (focus.getFocusOwner() == null ? "none"
					: focus.getFocusOwner().getClass().getName() + "[" + focus.getFocusOwner().getName() + "]"));
		});
		return snapshot.get();
	}

	private static String safeScreenBounds(Component component) {
		try { return new Rectangle(component.getLocationOnScreen(), component.getSize()).toString(); }
		catch (IllegalComponentStateException notShowing) { return "unavailable(" + notShowing.getMessage() + ")"; }
	}

	private static String describeButton(AbstractButton button) {
		String location;
		try {
			location = button.getLocationOnScreen().toString();
		} catch (IllegalComponentStateException notShowing) {
			location = "unavailable";
		}
		return "{" + button.getClass().getSimpleName() + ",text='" + button.getText()
			+ "',action='" + button.getActionCommand() + "',showing=" + button.isShowing()
			+ ",enabled=" + button.isEnabled() + ",bounds=" + button.getBounds()
			+ ",screenLocation=" + location + "}";
	}

	private static AbstractButton findFileChooserCancelButton(Window chooserWindow) {
		for (Component component : flatten(chooserWindow)) {
			if (!(component instanceof AbstractButton button)) continue;
			String action = button.getActionCommand();
			String text = button.getText();
			if ((action != null && action.toLowerCase(Locale.ROOT).contains("cancel"))
					|| (text != null && List.of("cancel", "キャンセル", "取消", "abbrechen", "annuler", "anuluj")
						.contains(text.trim().toLowerCase(Locale.ROOT))))
				return button;
		}
		return null;
	}


	private static JComboBox<?> findComboWithItem(Component root, String item) {
		for (Component component : flatten(root)) {
			if (component instanceof JComboBox<?> combo && findComboItem(combo, item) != null)
				return combo;
		}
		throw new AssertionError("Visible dialog does not expose locale item: " + item);
	}

	private static Object findComboItem(JComboBox<?> combo, String item) {
		for (int index = 0; index < combo.getItemCount(); index++) {
			Object candidate = combo.getItemAt(index);
			if (item.equals(String.valueOf(candidate))) return candidate;
		}
		return null;
	}

	private static List<Component> flatten(Component root) {
		List<Component> result = new ArrayList<>();
		result.add(root);
		if (root instanceof Container container) {
			for (Component child : container.getComponents()) result.addAll(flatten(child));
		}
		return result;
	}
}
