/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.awt.IllegalComponentStateException;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.prefs.Preferences;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
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
import com.microproject.pm.graphic.frames.workspace.NamedFrame;
import com.microproject.preference.ConfigurationFile;
import com.microproject.session.SessionFactory;
import com.microproject.strings.Messages;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.testsupport.RibbonGuiButton;
import com.microproject.testsupport.RibbonGuiSupport;
import com.microproject.testsupport.DialogLayoutAssertions;
import com.microproject.util.Environment;
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
	private MainRibbonFrame window;
	private GraphicManager manager;
	private boolean previousRibbonUi;
	private boolean previousNewLook;
	private boolean previousStandalone;
	private boolean previousClientSide;
	private UiServices.FileChooserProvider previousChooser;
	private String previousSystemChooserProperty;
	private JobQueue previousJobQueue;
	private Path legacyPod;
	private Path restartFirstProject;
	private Path restartSecondProject;
	private Preferences localePreferences;
	private String previousLocalePreference;
	private Locale previousDefaultLocale;
	private Locale previousFormatLocale;
	private Locale previousDisplayLocale;

	@AfterEach
	void closeWindow() throws Exception {
		if (manager != null) SwingUtilities.invokeAndWait(manager::cleanUp);
		if (window != null) SwingUtilities.invokeAndWait(window::dispose);
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
		AbstractButton openButton = findCommandButton(window.getRibbonPanel(), "RibbonOpenProject", robot);
		click(robot, openButton);
		GuiAcceptanceSupport.await(() -> manager.findFrameForProjectFile(legacyPod.toString()) != null,
			"File/Open rejected the selected legacy POD before its importer ran");
		assertTrue(manager.findFrameForProjectFile(legacyPod.toString()).getProject() != null,
			"File/Open registered a legacy POD frame without a project model");
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
		click(robot, findCommandButton(window.getRibbonPanel(), "RibbonOpenProject", robot));
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
		UiServices.setFileChooserProvider(new SwingFileChooserProvider());

		createWindow("microProject — File/Open cancellation acceptance (#398)");
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		activateWindowForRobot(robot);
		AbstractButton open = findCommandButton(window.getRibbonPanel(), "RibbonOpenProject", robot);
		int documentsBefore = manager.getFrameManager().getAllFrames().size();

		click(robot, open);
		GuiAcceptanceSupport.await(() -> visibleFileChooserDialog() != null,
			"File/Open did not show the Swing file chooser");
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

		click(robot, open);
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
		SwingFileChooserProvider nativeProvider = new SwingFileChooserProvider();
		UiServices.setFileChooserProvider(new UiServices.FileChooserProvider() {
			@Override
			public String chooseFileName(boolean save, String selectedFileName, Object parent) {
				chooserCallStarted.set(true);
				try {
					String selected = nativeProvider.chooseFileName(save, selectedFileName, parent);
					chooserCancelled.set(selected == null);
					return selected;
				} finally {
					chooserCallReturned.set(true);
				}
			}

			@Override
			public List<String> chooseFileNames(boolean save, String selectedFileName, Object parent) {
				chooserCallStarted.set(true);
				try {
					List<String> selected = nativeProvider.chooseFileNames(save, selectedFileName, parent);
					chooserCancelled.set(selected.isEmpty());
					return selected;
				} finally {
					chooserCallReturned.set(true);
				}
			}
		});

		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		activateWindowForRobot(robot);
		int documentsBefore = manager.getFrameManager().getAllFrames().size();
		verifyNativeChooserEscape(robot, () -> pressCtrlO(robot), chooserCallStarted, chooserCallReturned,
			chooserCancelled, "Ctrl+O");
		assertEquals(documentsBefore, manager.getFrameManager().getAllFrames().size(),
			"native Ctrl+O cancellation must not open a project");
		verifyNativeChooserEscape(robot,
			() -> click(robot, findCommandButton(window.getRibbonPanel(), "RibbonOpenProject", robot)),
			chooserCallStarted, chooserCallReturned,
			chooserCancelled, "Ribbon Open");
		assertEquals(documentsBefore, manager.getFrameManager().getAllFrames().size(),
			"native Ribbon Open cancellation must not open a project");
		verifyNativeChooserCancelButton(robot,
			() -> click(robot, findCommandButton(window.getRibbonPanel(), "RibbonOpenProject", robot)), chooserCallStarted,
			chooserCallReturned, chooserCancelled, "Ribbon Open");
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
			AtomicBoolean chooserCallReturned, AtomicBoolean chooserCancelled, String route) throws Exception {
		// Native Windows choosers are external top-level windows. Do not place the
		// owner above them in the test window's z-order while exercising this route.
		activateWindowForRobot(robot, false);
		chooserCallStarted.set(false);
		chooserCallReturned.set(false);
		chooserCancelled.set(false);
		BufferedImage beforeOpen = captureScreen(robot);
		openCommand.run();
		try {
			GuiAcceptanceSupport.await(chooserCallStarted::get,
				route + " did not enter the default Windows native chooser provider");
			GuiAcceptanceSupport.await(() -> !visibleDialogOverlayBounds(beforeOpen, captureScreen(robot)).isEmpty(),
				route + " called the native provider but no visible chooser appeared on the desktop");
			assertFalse(chooserCallReturned.get(), route + " chooser must remain modal until physical Escape");
			String artifactName = "issue-398-native-" + route.toLowerCase(Locale.ROOT).replace(' ', '-')
				+ "-before-escape.png";
			captureNativeChooserScreen(robot, artifactName);
			robot.keyPress(java.awt.event.KeyEvent.VK_ESCAPE);
			robot.keyRelease(java.awt.event.KeyEvent.VK_ESCAPE);
		} finally {
			if (chooserCallStarted.get() && !chooserCallReturned.get()) {
				robot.keyPress(java.awt.event.KeyEvent.VK_ESCAPE);
				robot.keyRelease(java.awt.event.KeyEvent.VK_ESCAPE);
			}
		}
		GuiAcceptanceSupport.await(chooserCallReturned::get,
			route + " Escape did not return from the default Windows file chooser");
		assertTrue(chooserCancelled.get(), route + " Escape must cancel the default Windows file chooser");
	}

	private static void captureNativeChooserScreen(Robot robot, String artifactName) throws IOException {
		BufferedImage image = captureScreen(robot);
		Path output = Path.of("build", "reports", "guiTest-artifacts", artifactName);
		Files.createDirectories(output.getParent());
		ImageIO.write(image, "png", output.toFile());
	}

	private static BufferedImage captureScreen(Robot robot) {
		Rectangle bounds = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
		return robot.createScreenCapture(bounds);
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

	private void verifyNativeChooserCancelButton(Robot robot, GuiCommand openCommand,
			AtomicBoolean chooserCallStarted, AtomicBoolean chooserCallReturned,
			AtomicBoolean chooserCancelled, String route) throws Exception {
		activateWindowForRobot(robot, false);
		chooserCallStarted.set(false);
		chooserCallReturned.set(false);
		chooserCancelled.set(false);
		BufferedImage beforeOpen = captureScreen(robot);
		openCommand.run();
		try {
			GuiAcceptanceSupport.await(chooserCallStarted::get,
				route + " did not enter the default Windows native chooser provider for Cancel acceptance");
			Rectangle[] chooserBounds = new Rectangle[1];
			GuiAcceptanceSupport.await(() -> {
				chooserBounds[0] = visibleDialogOverlayBounds(beforeOpen, captureScreen(robot));
				return !chooserBounds[0].isEmpty();
			}, route + " did not show a visible Windows chooser before physical Cancel");
			assertFalse(chooserCallReturned.get(), route + " native chooser returned before physical Cancel");
			captureNativeChooserScreen(robot, "issue-398-native-ribbon-open-before-cancel.png");
			Rectangle bounds = chooserBounds[0];
			// The native dialog can be visible while another always-on-top test
			// window still owns foreground activation. Activate the chooser through
			// its title bar first so the following click reaches its Cancel control.
			robot.mouseMove(bounds.x + bounds.width / 2, bounds.y + Math.max(12, bounds.height / 30));
			robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
			robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
			robot.delay(150);
			int cancelX = bounds.x + bounds.width - Math.round(bounds.width * 0.06f);
			int cancelY = bounds.y + bounds.height - Math.round(bounds.height * 0.055f);
			robot.mouseMove(cancelX, cancelY);
			robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
			robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
			robot.waitForIdle();
			GuiAcceptanceSupport.await(chooserCallReturned::get,
				route + " physical click on native chooser Cancel did not return from the provider");
		} finally {
			if (chooserCallStarted.get() && !chooserCallReturned.get()) {
				robot.keyPress(java.awt.event.KeyEvent.VK_ESCAPE);
				robot.keyRelease(java.awt.event.KeyEvent.VK_ESCAPE);
				robot.waitForIdle();
			}
		}
		assertTrue(chooserCancelled.get(), route + " native chooser Cancel must report cancellation");
		// The native provider returning with an empty selection is the reliable
		// close signal. A desktop before/after pixel diff also includes unrelated
		// repaint/focus changes and can falsely report a full-screen chooser.
		captureNativeChooserScreen(robot, "issue-398-native-ribbon-after-cancel.png");
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
		var point = component.getLocationOnScreen();
		robot.mouseMove(point.x + component.getWidth() / 2, point.y + component.getHeight() / 2);
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
		SwingUtilities.invokeAndWait(() -> {
			window.toFront();
			window.requestFocus();
			window.setAlwaysOnTop(alwaysOnTop);
		});
		Point location = window.getLocationOnScreen();
		// The Alt transition grants a foreground activation opportunity on
		// Windows when the test worker was launched behind another desktop window.
		robot.keyPress(java.awt.event.KeyEvent.VK_ALT);
		robot.keyRelease(java.awt.event.KeyEvent.VK_ALT);
		robot.mouseMove(location.x + Math.max(8, window.getWidth() / 2), location.y + 8);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		// Foreground activation is controlled by Windows and can remain false in a
		// Gradle-launched desktop even after the physical title-bar click.  The
		// subsequent command click is the acceptance boundary; requiring the native
		// isActive bit here made the test fail before exercising that route.
		robot.delay(250);
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
			case "RibbonLocale" -> "officeBackstageNav-locale";
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
			return target != null;
		}, "Backstage destination did not expose command " + commandId);
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
			case "RibbonBackstageOptions" -> "options";
			case "RibbonSaveProject" -> null;
			case "RibbonSaveProjectAs", "RibbonSaveMpoAs" -> "saveAs";
			case "RibbonPrint", "RibbonPrintPreview", "RibbonPDF" -> "print";
			case "RibbonImportProject", "RibbonExportProject" -> "export";
			case "RibbonCloseProject", "RibbonLocale" -> null;
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

	private static Component findNamedOnEdt(Window window, String name) {
		Component[] match = new Component[1];
		try {
			SwingUtilities.invokeAndWait(() -> match[0] = findNamed(SwingUtilities.getRootPane(window), name));
		} catch (Exception exception) {
			throw new AssertionError("Could not inspect Backstage component " + name + " on the EDT", exception);
		}
		return match[0];
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
