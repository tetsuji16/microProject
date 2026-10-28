/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.Window;
import java.util.ResourceBundle;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;
import javax.swing.AbstractButton;
import javax.swing.UIManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.pm.graphic.views.ResourceView;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.ProjectFactory;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.testsupport.RibbonGuiSupport;
import com.microproject.testsupport.WindowAcceptanceAssertions;
import com.microproject.menu.testsupport.UiComponentWalker;
import com.microproject.ui.ribbon.ModernRibbonPanel;
import com.microproject.ui.ribbon.RibbonDisplayMode;
import com.microproject.ui.ribbon.RibbonDisplayPreferences;
import com.microproject.ui.diagnostics.UiButtonDiagnostics;
import com.microproject.ui.util.SwingAlertPresenter;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.Environment;
import com.microproject.util.UiDispatch;
import com.microproject.session.SessionFactory;
import com.microproject.ui.util.SwingUiDispatcher;

/** GUI regression coverage for issue #461. */
class ResourcePoolInitialViewGuiAcceptanceTest {
	private MainRibbonFrame window;
	private GraphicManager manager;
	private boolean previousRibbonUi;
	private boolean previousNewLook;
	private boolean previousClientSide;
	private boolean previousBatchMode;
	private RibbonDisplayMode previousRibbonDisplayMode;
	private String previousUiDebug;
	private Level previousDiagnosticsLevel;
	private Level previousProjectFactoryLevel;
	private Level previousAlertLevel;
	private com.microproject.job.JobQueue previousJobQueue;
	private Handler diagnosticsHandler;
	private final StringBuffer diagnostics = new StringBuffer();

	@AfterEach
	void closeWindow() throws Exception {
		UiDispatch.setDispatcher(null);
		SwingUtilities.invokeAndWait(() -> {
			if (manager != null)
				manager.cleanUp();
			for (Window candidate : Window.getWindows())
				if (candidate == window)
					candidate.dispose();
		});
		if (previousJobQueue != null || manager != null)
			SessionFactory.getInstance().setJobQueue(previousJobQueue);
		Environment.setRibbonUI(previousRibbonUi);
		Environment.setNewLook(previousNewLook);
		Environment.setClientSide(previousClientSide);
		Environment.setBatchMode(previousBatchMode);
		com.microproject.util.Alert.setPresenter(null);
		if (previousRibbonDisplayMode != null) RibbonDisplayPreferences.save(previousRibbonDisplayMode);
		if (diagnosticsHandler != null) {
			Logger logger = Logger.getLogger(UiButtonDiagnostics.class.getName());
			logger.removeHandler(diagnosticsHandler);
			logger.setLevel(previousDiagnosticsLevel);
			Logger projectFactoryLogger = Logger.getLogger(ProjectFactory.class.getName());
			Logger alertLogger = Logger.getLogger(com.microproject.util.Alert.class.getName());
			projectFactoryLogger.removeHandler(diagnosticsHandler);
			projectFactoryLogger.setLevel(previousProjectFactoryLevel);
			alertLogger.removeHandler(diagnosticsHandler);
			alertLogger.setLevel(previousAlertLevel);
		}
		if (previousUiDebug != null) System.setProperty("microproject.ui.debug", previousUiDebug);
		else if (diagnosticsHandler != null) System.clearProperty("microproject.ui.debug");
	}

	@Test
	void masterResourcePoolOpensOnResourceSheetInsteadOfGantt() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
				"A desktop session is required for GUI acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("issue-461-resource-pool", undo);
		pool.setLocal(true);
		pool.setMaster(true);
		Project project = Project.createProject(pool, undo);
		project.setResourcePoolProject(true);
		project.initialize(false, false);

		SwingUtilities.invokeAndWait(() -> {
			window = new MainRibbonFrame("issue-461-resource-pool", null);
			manager = new GraphicManager(window);
			window.setGraphicManager(manager);
			manager.initView();
			manager.addProjectFrame(project);
			window.setSize(1120, 700);
			window.setVisible(true);
		});

		DocumentFrame frame = manager.getFrameForProject(project);
		ResourceView expectedView[] = new ResourceView[1];
		SwingUtilities.invokeAndWait(() -> expectedView[0] = frame.getResourceView());
		GuiAcceptanceSupport.await(() -> frame.getActiveTopView() == expectedView[0],
				"a master resource pool did not activate the resource sheet");
		assertSame(expectedView[0], frame.getActiveTopView());
		org.junit.jupiter.api.Assertions.assertInstanceOf(ResourceView.class, frame.getActiveTopView());
	}

	@Test
	void physicalCreateResourcePoolCommandOpensUsableResourceView() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
				"A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		previousClientSide = Environment.isClientSide();
		previousBatchMode = Environment.isBatchMode();
		Environment.setClientSide(true);
		Environment.setBatchMode(false);
		UiDispatch.setDispatcher(new SwingUiDispatcher());
		com.microproject.util.Alert.setPresenter(new SwingAlertPresenter());
		previousRibbonDisplayMode = RibbonDisplayPreferences.load();
		RibbonDisplayPreferences.save(RibbonDisplayMode.ALWAYS_SHOW);
		previousUiDebug = System.getProperty("microproject.ui.debug");
		System.setProperty("microproject.ui.debug", "true");
		Logger diagnosticsLogger = Logger.getLogger(UiButtonDiagnostics.class.getName());
		previousDiagnosticsLevel = diagnosticsLogger.getLevel();
		diagnosticsLogger.setLevel(Level.FINE);
		Logger projectFactoryLogger = Logger.getLogger(ProjectFactory.class.getName());
		previousProjectFactoryLevel = projectFactoryLogger.getLevel();
		projectFactoryLogger.setLevel(Level.FINE);
		Logger alertLogger = Logger.getLogger(com.microproject.util.Alert.class.getName());
		previousAlertLevel = alertLogger.getLevel();
		alertLogger.setLevel(Level.WARNING);
		diagnosticsHandler = new Handler() {
			@Override public void publish(LogRecord record) {
				diagnostics.append(record.getMessage()).append('\n');
				if (record.getThrown() != null) diagnostics.append(record.getThrown()).append('\n');
			}
			@Override public void flush() { }
			@Override public void close() { }
		};
		diagnosticsLogger.addHandler(diagnosticsHandler);
		projectFactoryLogger.addHandler(diagnosticsHandler);
		alertLogger.addHandler(diagnosticsHandler);
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool starterPool = ResourcePool.createRourcePool("issue-461-starter", undo);
		starterPool.setLocal(true);
		Project starterProject = Project.createProject(starterPool, undo);
		starterProject.initialize(false, false);
		SwingUtilities.invokeAndWait(() -> {
			window = new MainRibbonFrame("issue-461-resource-pool-command", null);
			manager = new GraphicManager(window);
			window.setGraphicManager(manager);
			manager.initView();
			previousJobQueue = SessionFactory.getInstance().getJobQueue();
			SessionFactory.getInstance().setJobQueue(manager.getJobQueue());
			manager.addProjectFrame(starterProject);
			window.setSize(1400, 900);
			window.setVisible(true);
		});
		SwingUtilities.invokeAndWait(() -> manager.getFrameForProject(starterProject).activateResourceView());

		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(50);
		String resourceTabText = ResourceBundle.getBundle("com.microproject.menu.menu")
			.getString("ResourceRibbonTask.title");
		AbstractButton resourceTab = GuiAcceptanceSupport.findShowingButtonByText(window, resourceTabText);
		click(robot, resourceTab);
		GuiAcceptanceSupport.await(resourceTab::isSelected, "physical Resource tab click did not select the ribbon tab");
		AbstractButton createButton;
		try {
			createButton = RibbonGuiSupport.findVisibleOrExpand(robot, window.getRibbonPanel(),
				"RibbonCreateResourcePool");
		} catch (AssertionError failure) {
			throw new AssertionError("Could not locate Create Resource Pool. " + ribbonDiagnostics(), failure);
		}
		click(robot, createButton);

		GuiAcceptanceSupport.await(() -> manager.getOpenProjects().stream()
			.anyMatch(project -> project != starterProject && project.isResourcePoolProject()),
			"Create Resource Pool did not register a new master pool document. " + ribbonDiagnostics());
		Dialog notice;
		try {
			notice = awaitVisibleDialog();
		} catch (AssertionError failure) {
			throw new AssertionError("Resource-pool save notice did not become visible; Alert.warn logs: "
				+ diagnostics, failure);
		}
		click(robot, GuiAcceptanceSupport.findShowingButtonByText(notice,
			UIManager.getString("OptionPane.okButtonText")));
		GuiAcceptanceSupport.await(() -> java.util.Arrays.stream(Window.getWindows())
			.noneMatch(candidate -> candidate instanceof Dialog dialog && dialog.isShowing()),
			"resource-pool command left a dialog open after dismissing its notice");

		Project createdProject = manager.getOpenProjects().stream()
			.filter(project -> project != starterProject && project.isResourcePoolProject()).findFirst().orElseThrow();
		DocumentFrame frame = manager.getFrameForProject(createdProject);
		GuiAcceptanceSupport.await(() -> frame != null && frame.getActiveTopView() instanceof ResourceView,
			"new master resource-pool document did not activate ResourceView");
		ResourceView resourceView = (ResourceView) frame.getActiveTopView();
		SpreadSheet sheet = resourceView.getSpreadSheet();
		assertNotNull(sheet, "ResourceView must own its spreadsheet");
		assertNotNull(sheet.getModel(), "resource spreadsheet must have a model");
		assertTrue(sheet.getColumnCount() > 0, "resource spreadsheet must expose usable columns");
		assertTrue(sheet.getRowCount() > 0, "new resource pool must show at least its initial resource row");
		assertFalse(diagnostics.toString().contains("UI_COMMAND_FAILURE id=RibbonCreateResourcePool"),
			"Create Resource Pool command failed: " + diagnostics);
		assertFalse(diagnostics.toString().contains("UI_BUTTON_FAILURE id=RibbonCreateResourcePool"),
			"Create Resource Pool button failed: " + diagnostics);

	}

	@Test
	void physicalMultipleProjectAndResourcePoolWindowsSwitchCommandAndCloseCleanly() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
				"A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		Project first = namedProject("phase4-window-first", false);
		Project second = namedProject("phase4-window-second", false);
		Project pool = namedProject("phase4-window-resource-pool", true);
		SwingUtilities.invokeAndWait(() -> {
			window = new MainRibbonFrame("phase4-window-first", null);
			manager = new GraphicManager(window);
			window.setGraphicManager(manager);
			manager.initView();
			manager.addProjectFrame(first);
			manager.addProjectFrame(second);
			manager.addProjectFrame(pool);
			DocumentFrame firstFrame = manager.getFrameForProject(first);
			window.setWindowCloseAction(() -> {
				manager.closeDocumentWindow(firstFrame);
				window.setVisible(false);
			});
			window.setSize(1200, 760);
			window.setVisible(true);
		});
		GuiAcceptanceSupport.await(() -> manager.getOpenProjects().size() == 3,
			"the two named projects and master resource pool must all be registered");
		java.util.List<MainRibbonFrame> documentWindows = awaitNamedWindows(
			"phase4-window-first", "phase4-window-second", "phase4-window-resource-pool");
		assertEquals(3, documentWindows.size(), "each project must have a native document window");
		MainRibbonFrame firstWindow = findNamedWindow(documentWindows, "phase4-window-first");
		MainRibbonFrame secondWindow = findNamedWindow(documentWindows, "phase4-window-second");
		MainRibbonFrame poolWindow = findNamedWindow(documentWindows, "phase4-window-resource-pool");
		positionNativeWindows(documentWindows);
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(40);
		for (MainRibbonFrame documentWindow : documentWindows) {
			activateNativeWindow(robot, documentWindow);
			GuiAcceptanceSupport.await(documentWindow::isActive,
				"Robot could not switch to " + documentWindow.getTitle());
			Project expectedProject = documentWindow == firstWindow ? first
				: documentWindow == secondWindow ? second : pool;
			DocumentFrame expectedFrame = manager.getFrameForProject(expectedProject);
			GuiAcceptanceSupport.await(() -> manager.getCurrentFrame() == expectedFrame,
				"native focus did not select " + expectedProject.getName());
			assertTrue(documentWindow.getTitle().contains(expectedProject.getName()),
				"each native shell must retain its own project title after switching; expected="
					+ expectedProject.getName() + ", titles=" + documentWindows.stream()
						.map(MainRibbonFrame::getTitle).toList());
			if (expectedProject == pool)
				GuiAcceptanceSupport.await(() -> expectedFrame.getActiveTopView() instanceof ResourceView,
					"switching to the master pool did not retain its ResourceView");
		}
		MainRibbonFrame normalWindow = secondWindow;
		activateNativeWindow(robot, normalWindow);
		GuiAcceptanceSupport.await(normalWindow::isActive, "normal project window did not become active");
		DocumentFrame secondFrame = manager.getFrameForProject(second);
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame() == secondFrame,
			"normal project focus did not update the active document before command dispatch");
		AbstractButton viewTab = GuiAcceptanceSupport.findShowingButtonByText(normalWindow,
			ResourceBundle.getBundle("com.microproject.menu.menu").getString("ViewRibbonTask.title"));
		WindowAcceptanceAssertions.clickComponent(robot, viewTab);
		GuiAcceptanceSupport.await(() -> normalWindow.isFocused() && viewTab.isFocusOwner() && viewTab.isSelected(),
			"physical ribbon-tab click did not establish keyboard focus in the active document window");
		com.microproject.ui.ribbon.RibbonController ribbon = (com.microproject.ui.ribbon.RibbonController)
			normalWindow.getRibbonPanel().getClientProperty(com.microproject.ui.ribbon.RibbonController.CONTEXTUAL_TABS_PROPERTY);
		com.microproject.ui.ribbon.RibbonDisplayMode before = ribbon.getRibbonDisplayMode();
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_F1);
		robot.keyRelease(KeyEvent.VK_F1);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		robot.waitForIdle();
		GuiAcceptanceSupport.await(() -> ribbon.getRibbonDisplayMode() != before,
			"physical Ctrl+F1 did not toggle the active normal project's ribbon");
		assertSame(secondFrame, manager.getCurrentFrame(),
			"the harmless ribbon command must leave the active project stable");

		SwingUtilities.invokeAndWait(() -> {
			for (Project project : java.util.List.of(first, second, pool)) {
				project.setDirty(false);
				project.setGroupDirty(false);
			}
		});
		assertTrue(java.util.List.of(first, second, pool).stream().noneMatch(Project::needsSaving),
			"the physical title-bar close journey must start from clean projects, without save prompts");
		java.util.List<MainRibbonFrame> closeOrder = java.util.List.of(
			secondWindow, poolWindow, firstWindow);
		for (MainRibbonFrame documentWindow : closeOrder) {
			activateNativeWindow(robot, documentWindow);
			WindowAcceptanceAssertions.clickNativeTitleButton(robot, documentWindow, "Close");
			GuiAcceptanceSupport.await(() -> !documentWindow.isShowing(),
				"Robot title-bar close did not close " + documentWindow.getTitle());
		}
		GuiAcceptanceSupport.await(() -> manager.getOpenProjects().isEmpty(),
			"closing all three native windows left project registrations behind");
		assertTrue(documentWindows.stream().noneMatch(MainRibbonFrame::isShowing),
			"closing each document must leave no orphan project windows");
	}

	private static Project namedProject(String name, boolean masterPool) {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool(name, undo);
		resourcePool.setLocal(true);
		resourcePool.setMaster(masterPool);
		Project project = Project.createProject(resourcePool, undo);
		project.setName(name);
		project.setFileName(name + ".mpo");
		project.setResourcePoolProject(masterPool);
		project.initialize(false, false);
		return project;
	}

	private static java.util.List<MainRibbonFrame> awaitNamedWindows(String... names) throws Exception {
		java.util.List<MainRibbonFrame>[] result = new java.util.List[] { java.util.List.of() };
		GuiAcceptanceSupport.await(() -> {
			java.util.List<MainRibbonFrame> found = java.util.Arrays.stream(Window.getWindows())
				.filter(MainRibbonFrame.class::isInstance).map(MainRibbonFrame.class::cast)
				.filter(MainRibbonFrame::isShowing).filter(candidate -> candidate.getRibbonPanel() != null)
				.filter(candidate -> {
					String title = candidate.getRibbonPanel().getComponents().length == 0 ? "" : candidate.getTitle();
					return java.util.Arrays.stream(names).anyMatch(title::contains);
				}).toList();
			if (found.size() == names.length) result[0] = found;
			return found.size() == names.length;
		}, "three named native project windows did not appear");
		return result[0];
	}

	private static MainRibbonFrame findNamedWindow(java.util.List<MainRibbonFrame> windows, String name) {
		return windows.stream().filter(candidate -> candidate.getTitle().contains(name)).findFirst()
			.orElseThrow(() -> new AssertionError("native project window is absent: " + name + "; titles="
				+ windows.stream().map(candidate -> candidate.getTitle() + " showing=" + candidate.isShowing()
					+ " bounds=" + candidate.getBounds()).toList()));
	}

	private void positionNativeWindows(java.util.List<MainRibbonFrame> windows) throws Exception {
		java.awt.Dimension screen = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
		int width = Math.max(480, (screen.width - 30) / 2);
		int height = Math.max(320, Math.min(650, (screen.height - 30) / 2));
		MainRibbonFrame firstWindow = findNamedWindow(windows, "phase4-window-first");
		MainRibbonFrame secondWindow = findNamedWindow(windows, "phase4-window-second");
		MainRibbonFrame poolWindow = findNamedWindow(windows, "phase4-window-resource-pool");
		SwingUtilities.invokeAndWait(() -> {
			firstWindow.setAlwaysOnTop(false);
			secondWindow.setAlwaysOnTop(false);
			poolWindow.setAlwaysOnTop(false);
			firstWindow.setBounds(10, 10, width, height);
			secondWindow.setBounds(width + 20, 10, width, height);
			poolWindow.setBounds(10, height + 20, width, height);
		});
	}

	private static void activateNativeWindow(Robot robot, MainRibbonFrame frame) throws Exception {
		java.awt.Rectangle bounds = WindowAcceptanceAssertions.boundsOnScreen(frame);
		java.awt.Point target = WindowAcceptanceAssertions.robotPointFor(bounds);
		robot.mouseMove(target.x, target.y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
	}

	private static Dialog awaitVisibleDialog() throws Exception {
		Dialog[] result = new Dialog[1];
		GuiAcceptanceSupport.await(() -> {
			for (Window candidate : Window.getWindows())
				if (candidate instanceof Dialog dialog && dialog.isShowing()) {
					result[0] = dialog;
					return true;
				}
			return false;
		}, "resource-pool save notice did not appear");
		return result[0];
	}

	private static void click(Robot robot, Component component) throws Exception {
		Point[] location = new Point[1];
		SwingUtilities.invokeAndWait(() -> location[0] = component.getLocationOnScreen());
		robot.mouseMove(location[0].x + component.getWidth() / 2, location[0].y + component.getHeight() / 2);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
	}

	private String ribbonDiagnostics() {
		StringBuilder result = new StringBuilder("frame=").append(window.getBounds());
		for (Component component : UiComponentWalker.flatten(window.getRibbonPanel())) {
			if (component instanceof AbstractButton button
				&& "RibbonCreateResourcePool".equals(button.getActionCommand())) {
				result.append("; command=").append(button.getClass().getSimpleName())
					.append(" showing=").append(button.isShowing())
					.append(" enabled=").append(button.isEnabled())
					.append(" bounds=").append(safeBounds(button));
			}
			if (component instanceof javax.swing.JComponent swingComponent) {
				Object popup = swingComponent.getClientProperty(ModernRibbonPanel.COLLAPSED_POPUP_PROPERTY);
				if (popup instanceof javax.swing.JPopupMenu menu) {
					for (Component child : UiComponentWalker.flatten(menu)) {
						if (child instanceof AbstractButton button
							&& "RibbonCreateResourcePool".equals(button.getActionCommand())) {
							result.append("; collapsed-command showing=").append(button.isShowing())
								.append(" enabled=").append(button.isEnabled());
						}
					}
				}
			}
		}
		return result.append("; diagnostics=").append(diagnostics).toString();
	}

	private static Rectangle safeBounds(Component component) {
		try { return new Rectangle(component.getLocationOnScreen(), component.getSize()); }
		catch (java.awt.IllegalComponentStateException notShowing) { return component.getBounds(); }
	}
}
