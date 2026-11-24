/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.ribbon;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.AbstractButton;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import com.microproject.ui.menu.testsupport.UiComponentWalker;
import com.microproject.ui.frame.GraphicManager;
import com.microproject.ui.frame.MainRibbonFrame;
import com.microproject.ui.view.spreadsheet.SpreadSheet;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.ui.dialog.ResourceInformationDialog;
import com.microproject.job.JobQueue;
import com.microproject.session.SessionFactory;
import com.microproject.exchange.MpoFileImporter;
import com.microproject.ui.testsupport.GuiAcceptanceSupport;
import com.microproject.ui.testsupport.RibbonGuiSupport;
import com.microproject.ui.testsupport.WindowAcceptanceAssertions;
import com.microproject.ui.ribbon.RibbonDisplayMode;
import com.microproject.ui.ribbon.RibbonDisplayPreferences;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.Environment;
import com.microproject.util.UiServices;
import com.microproject.ui.util.SwingFileChooserProvider;
import com.microproject.ribbon.RibbonCommandResult;

/**
 * A real main window with a small in-memory project, for acceptance tests that click ribbon
 * commands with the Robot. The project lives in memory, so nothing under {@code samples} is
 * touched. The set of commands under test is read from {@code menuInternal.properties}, not
 * copied into the tests.
 */
final class RibbonCommandGuiFixture implements AutoCloseable {
	private static final String RIBBON_RESOURCE = "/com/microproject/ui/menu/menuInternal.properties";
	private static final Properties RIBBON = loadRibbonProperties();

	final MainRibbonFrame window;
	final GraphicManager manager;
	final Robot robot;
	private final boolean previousRibbonUi;
	private final boolean previousNewLook;
	private final RibbonDisplayMode previousDisplayMode;
	private Path savedFixtureDirectory;
	private final UiServices.FileChooserProvider previousFileChooser;
	private final String previousSystemChooserProperty;
	private final AtomicInteger chooserFileNameCalls;
	private final AtomicInteger chooserFileNamesCalls;
	private final JobQueue previousSessionJobQueue;
	private String lastPhysicalButtonClick = "none";

	private RibbonCommandGuiFixture(MainRibbonFrame window, GraphicManager manager, Robot robot,
		boolean ribbonUi, boolean newLook, RibbonDisplayMode displayMode,
		UiServices.FileChooserProvider previousFileChooser, String previousSystemChooserProperty,
		AtomicInteger chooserFileNameCalls, AtomicInteger chooserFileNamesCalls, JobQueue previousSessionJobQueue) {
		this.window = window;
		this.manager = manager;
		this.robot = robot;
		this.previousRibbonUi = ribbonUi;
		this.previousNewLook = newLook;
		this.previousDisplayMode = displayMode;
		this.previousFileChooser = previousFileChooser;
		this.previousSystemChooserProperty = previousSystemChooserProperty;
		this.chooserFileNameCalls = chooserFileNameCalls;
		this.chooserFileNamesCalls = chooserFileNamesCalls;
		this.previousSessionJobQueue = previousSessionJobQueue;
	}

	/** Opens the main window on a project with {@code taskCount} tasks and two resources. */
	static RibbonCommandGuiFixture open(int taskCount) throws Exception {
		boolean ribbonUi = Environment.isRibbonUI();
		boolean newLook = Environment.isNewLook();
		RibbonDisplayMode displayMode = RibbonDisplayPreferences.load();
		UiServices.FileChooserProvider previousFileChooser = UiServices.getFileChooserProvider();
		String previousSystemChooserProperty = System.getProperty("flatlaf.useSystemFileChooser");
		JobQueue previousSessionJobQueue = SessionFactory.getInstance().getLocalSession().getJobQueue();
		RibbonDisplayPreferences.save(RibbonDisplayMode.ALWAYS_SHOW);
		System.setProperty("flatlaf.useSystemFileChooser", "false");
		SwingFileChooserProvider swingChooser = new SwingFileChooserProvider();
		AtomicInteger chooserFileNameCalls = new AtomicInteger();
		AtomicInteger chooserFileNamesCalls = new AtomicInteger();
		UiServices.setFileChooserProvider(new UiServices.FileChooserProvider() {
			@Override public String chooseFileName(boolean save, String selectedFileName, Object parent) {
				chooserFileNameCalls.incrementAndGet();
				return swingChooser.chooseFileName(save, selectedFileName, parent);
			}
			@Override public List<String> chooseFileNames(boolean save, String selectedFileName, Object parent) {
				chooserFileNamesCalls.incrementAndGet();
				return swingChooser.chooseFileNames(save, selectedFileName, parent);
			}
		});
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("ribbon-command-acceptance", undo), undo);
		project.initialize(false, false);
		for (int index = 1; index <= taskCount; index++) {
			project.createScriptedTask().setName("Ribbon acceptance task " + index);
		}
		project.getResourcePool().createScriptedResource().setName("Ribbon acceptance resource 1");
		project.getResourcePool().createScriptedResource().setName("Ribbon acceptance resource 2");

		MainRibbonFrame[] frame = new MainRibbonFrame[1];
		GraphicManager[] manager = new GraphicManager[1];
		SwingUtilities.invokeAndWait(() -> {
			for (Window candidate : Window.getWindows()) {
				if (candidate instanceof MainRibbonFrame) candidate.dispose();
			}
			frame[0] = new MainRibbonFrame("microProject ribbon command acceptance", null);
			manager[0] = new GraphicManager(frame[0]);
			// StartupFactory wires the application's LocalSession to the active manager's queue.
			// A standalone ribbon fixture must follow that same initialization path for file commands.
			SessionFactory.getInstance().setJobQueue(manager[0].getJobQueue());
			frame[0].setGraphicManager(manager[0]);
			manager[0].initView();
			manager[0].addProjectFrame(project);
			frame[0].setSize(1600, 700);
			frame[0].setLocationByPlatform(true);
			frame[0].setAlwaysOnTop(true);
			frame[0].setVisible(true);
			frame[0].toFront();
			frame[0].requestFocus();
		});
		Robot robot = new com.microproject.ui.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		RibbonCommandGuiFixture fixture = new RibbonCommandGuiFixture(frame[0], manager[0], robot, ribbonUi, newLook, displayMode,
			previousFileChooser, previousSystemChooserProperty, chooserFileNameCalls, chooserFileNamesCalls,
			previousSessionJobQueue);
		GuiAcceptanceSupport.await(() -> frame[0].isShowing() && manager[0].getCurrentFrame() != null
			&& manager[0].getCurrentFrame().getActiveSpreadSheet() != null, "the main window did not show its project");
		fixture.activateWindow();
		return fixture;
	}

	@Override
	public void close() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			for (Window candidate : Window.getWindows()) {
				if (candidate != window && candidate.isShowing() && (candidate instanceof Dialog || candidate instanceof Frame)) {
					candidate.dispose();
				}
			}
			manager.cleanUp();
			window.dispose();
		});
		SessionFactory.getInstance().setJobQueue(previousSessionJobQueue);
		RibbonDisplayPreferences.save(previousDisplayMode);
		Environment.setRibbonUI(previousRibbonUi);
		Environment.setNewLook(previousNewLook);
		UiServices.setFileChooserProvider(previousFileChooser);
		if (previousSystemChooserProperty == null) System.clearProperty("flatlaf.useSystemFileChooser");
		else System.setProperty("flatlaf.useSystemFileChooser", previousSystemChooserProperty);
		if (savedFixtureDirectory != null) {
			try (var paths = Files.walk(savedFixtureDirectory)) {
				for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
			}
		}
	}

	// ---- what the resources declare ----

	/** The commands whose {@code <id>.activation} is {@code activation}, in file order. */
	static List<String> commandsWithActivation(String activation) {
		List<String> ids = new ArrayList<>();
		for (String key : sortedKeysInFileOrder()) {
			if (key.endsWith(".activation") && activation.equalsIgnoreCase(RIBBON.getProperty(key).trim())) {
				ids.add(key.substring(0, key.length() - ".activation".length()));
			}
		}
		return ids;
	}

	/** The tabs of the standard ribbon that list {@code commandId} in one of their bands, in tab order. */
	static List<String> tabsContaining(String commandId) {
		List<String> tabs = new ArrayList<>();
		for (String tabId : list("StandardRibbon")) {
			for (String bandId : list(tabId)) {
				for (String token : list(bandId)) {
					if (stripPriority(token).equals(commandId) && !tabs.contains(tabId)) tabs.add(tabId);
				}
			}
		}
		return tabs;
	}

	/** A missing direct button is expected only for contextual or popup-only commands. */
	static boolean isContextualOrPopupOnly(String commandId) {
		List<String> tabs = tabsContaining(commandId);
		if (!tabs.isEmpty()) {
			return tabs.stream().allMatch(tab -> Boolean.parseBoolean(RIBBON.getProperty(tab + ".contextual", "false")));
		}
		return RIBBON.stringPropertyNames().stream().filter(key -> key.endsWith(".items"))
			.anyMatch(key -> list(key).contains(commandId));
	}

	private static List<String> list(String key) {
		String value = RIBBON.getProperty(key);
		return value == null || value.isBlank() ? List.of() : List.of(value.trim().split("\\s+"));
	}

	private static String stripPriority(String token) {
		return token.endsWith(".TOP") || token.endsWith(".LOW") ? token.substring(0, token.length() - 4) : token;
	}

	private static List<String> sortedKeysInFileOrder() {
		// Properties does not keep order; the property file is the source, so read the keys again in order.
		List<String> keys = new ArrayList<>();
		try (InputStream in = RibbonCommandGuiFixture.class.getResourceAsStream(RIBBON_RESOURCE);
			java.io.BufferedReader reader = new java.io.BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				String trimmed = line.strip();
				if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")) continue;
				int end = 0;
				while (end < trimmed.length() && !Character.isWhitespace(trimmed.charAt(end))
					&& trimmed.charAt(end) != '=' && trimmed.charAt(end) != ':') end++;
				String key = trimmed.substring(0, end);
				if (RIBBON.containsKey(key) && !keys.contains(key)) keys.add(key);
			}
		} catch (java.io.IOException failure) {
			throw new IllegalStateException("Cannot read " + RIBBON_RESOURCE, failure);
		}
		return keys;
	}

	private static Properties loadRibbonProperties() {
		Properties properties = new Properties();
		try (InputStream in = RibbonCommandGuiFixture.class.getResourceAsStream(RIBBON_RESOURCE)) {
			assertNotNull(in, RIBBON_RESOURCE + " must be on the classpath");
			properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
		} catch (java.io.IOException failure) {
			throw new IllegalStateException("Cannot read " + RIBBON_RESOURCE, failure);
		}
		return properties;
	}

	// ---- driving the window ----

	<T> T onEdt(Callable<T> action) throws Exception {
		Object[] result = new Object[1];
		SwingUtilities.invokeAndWait(() -> {
			try {
				result[0] = action.call();
			} catch (Exception failure) {
				throw new IllegalStateException(failure);
			}
		});
		@SuppressWarnings("unchecked")
		T value = (T) result[0];
		return value;
	}

	void activateWindow() throws Exception {
		Rectangle bounds = onEdt(() -> window.isShowing() ? new Rectangle(window.getLocationOnScreen(), window.getSize()) : null);
		robot.mouseMove(bounds.x + Math.min(40, bounds.width / 2), bounds.y + 12);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
	}

	/** The showing ribbon tab button of {@code tabId}, or {@code null} while that tab is hidden. */
	AbstractButton tabButton(String tabId) throws Exception {
		return onEdt(() -> UiComponentWalker.flatten(window).stream()
			.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
			.filter(button -> ("microProjectRibbonTab-" + tabId).equals(button.getName()) && button.isShowing())
			.findFirst().orElse(null));
	}

	/** Selects the tab with a real click; false when the tab is not shown (a contextual tab of another view). */
	boolean selectTab(String tabId) throws Exception {
		AbstractButton tab = tabButton(tabId);
		if (tab == null) return false;
		if (!isSelected(tab)) {
			WindowAcceptanceAssertions.clickComponent(robot, tab);
			GuiAcceptanceSupport.await(() -> selected(tab), "the " + tabId + " ribbon tab did not become selected after a click");
		}
		return true;
	}

	/** The visible (or overflow) button of {@code commandId} on the selected tab. */
	AbstractButton command(String commandId) throws Exception {
		return RibbonGuiSupport.findVisibleOrExpand(robot, window, commandId);
	}

	/** Selects the first tab that lists the command and shows it; {@code null} when none is shown. */
	AbstractButton showCommand(String commandId) throws Exception {
		for (String tabId : tabsContaining(commandId)) {
			if (selectTab(tabId)) return command(commandId);
		}
		return null;
	}

	/** Returns to the task Gantt through its visible ribbon command between independent journeys. */
	void restoreGanttView() throws Exception {
		if ("Gantt".equals(manager.getTopViewId())) return;
		AbstractButton gantt = showCommand("RibbonGantt");
		assertNotNull(gantt, "the Gantt command must be available to restore the task sheet");
		if (!isSelected(gantt)) RibbonGuiSupport.clickSplitPrimary(robot, gantt);
		GuiAcceptanceSupport.await(() -> {
			try {
				return "Gantt".equals(manager.getTopViewId()) && activeSheet() != null;
			} catch (Exception failure) {
				throw new AssertionError(failure);
			}
		}, "the physical Gantt command did not restore the task sheet");
	}

	/** Gives commands that require a persisted peer project the real saved-project precondition. */
	void prepareSavedResourcePoolProject() throws Exception {
		Project sharer = manager.getCurrentFrame().getProject();
		ensureSavedDirectory();
		Path sharerPath = savedFixtureDirectory.resolve("sharer.mpo");
		Path poolPath = savedFixtureDirectory.resolve("pool.mpo");
		saveProject(sharer, sharerPath);

		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project pool = Project.createProject(ResourcePool.createRourcePool("ribbon-pool", undo), undo);
		pool.initialize(false, false);
		pool.setName("Ribbon acceptance resource pool");
		pool.getResourcePool().createScriptedResource().setName("Pool resource");
		saveProject(pool, poolPath);
		pool.setFileName(poolPath.toString());

		SwingUtilities.invokeAndWait(() -> {
			manager.addProjectFrame(pool);
			manager.addProjectFrame(sharer);
		});
	}

	void saveCurrentProject() throws Exception {
		Project project = manager.getCurrentFrame().getProject();
		ensureSavedDirectory();
		Path path = savedFixtureDirectory.resolve("current.mpo");
		saveProject(project, path);
	}

	Path reserveSavePath(String fileName) throws Exception {
		ensureSavedDirectory();
		return savedFixtureDirectory.resolve(fileName);
	}

	void routeSaveChooserTo(Path savePath) {
		UiServices.setFileChooserProvider(new UiServices.FileChooserProvider() {
			@Override public String chooseFileName(boolean save, String selectedFileName, Object parent) {
				chooserFileNameCalls.incrementAndGet();
				return save ? savePath.toString() : previousFileChooser.chooseFileName(save, selectedFileName, parent);
			}
			@Override public List<String> chooseFileNames(boolean save, String selectedFileName, Object parent) {
				chooserFileNamesCalls.incrementAndGet();
				return previousFileChooser.chooseFileNames(save, selectedFileName, parent);
			}
		});
	}

	Project loadProject(Path path) throws Exception {
		try (InputStream in = Files.newInputStream(path)) {
			Project loaded = new MpoFileImporter().loadProject(in);
			loaded.setFileName(path.toString());
			return loaded;
		}
	}

	private void ensureSavedDirectory() throws Exception {
		if (savedFixtureDirectory == null) savedFixtureDirectory = Files.createTempDirectory("microproject-ribbon-pool-");
	}

	private static void saveProject(Project project, Path path) throws Exception {
		try (OutputStream out = Files.newOutputStream(path)) {
			new MpoFileImporter().saveProject(project, out);
		}
		project.setFileName(path.toString());
		project.setDirty(false);
	}

	void click(Component component) throws Exception {
		if (component instanceof AbstractButton button) {
			AtomicInteger actionEvents = new AtomicInteger();
			button.addActionListener(event -> actionEvents.incrementAndGet());
			WindowAcceptanceAssertions.clickComponent(robot, component);
			lastPhysicalButtonClick = onEdt(() -> "name=" + button.getName() + ", action="
				+ (button.getAction() == null ? "null" : button.getAction().getClass().getName())
				+ ", eventCount=" + actionEvents.get() + ", enabled=" + button.isEnabled()
				+ ", selected=" + button.isSelected() + ", actionResult=" + actionProperty(button, RibbonCommandResult.RESULT_ACTION_PROPERTY)
				+ ", status=" + actionProperty(button, RibbonCommandResult.STATUS_ACTION_PROPERTY)
				+ ", reason=" + actionProperty(button, RibbonCommandResult.REASON_ACTION_PROPERTY));
			return;
		}
		WindowAcceptanceAssertions.clickComponent(robot, component);
	}

	private static Object actionProperty(AbstractButton button, String key) {
		return button.getAction() == null ? null : button.getAction().getValue(key);
	}

	boolean isSelected(AbstractButton button) throws Exception {
		return onEdt(button::isSelected);
	}

	private boolean selected(AbstractButton button) {
		try {
			return isSelected(button);
		} catch (Exception failure) {
			throw new AssertionError("Cannot read the pressed state of " + button.getName(), failure);
		}
	}

	boolean isEnabled(AbstractButton button) throws Exception {
		return onEdt(button::isEnabled);
	}

	boolean isShowing(Window candidate) throws Exception {
		return onEdt(candidate::isShowing);
	}

	ResourceInformationDialog visibleResourceInformationDialog() throws Exception {
		return onEdt(() -> {
			for (Window candidate : Window.getWindows()) {
				if (candidate instanceof ResourceInformationDialog dialog && dialog.isShowing()) return dialog;
			}
			return null;
		});
	}

	boolean isResourceNotesTabSelected(ResourceInformationDialog dialog) throws Exception {
		return onEdt(() -> UiComponentWalker.flatten(dialog).stream()
			.filter(JTabbedPane.class::isInstance).map(JTabbedPane.class::cast)
			.anyMatch(tabs -> "Notes".equals(tabs.getTitleAt(tabs.getSelectedIndex()))
				|| com.microproject.strings.Messages.getString("ResourceInformationDialog.Notes")
					.equals(tabs.getTitleAt(tabs.getSelectedIndex()))));
	}

	SpreadSheet activeSheet() throws Exception {
		return onEdt(() -> manager.getCurrentFrame().getActiveSpreadSheet());
	}

	String describeActiveSelection() throws Exception {
		return onEdt(() -> {
			SpreadSheet sheet = manager.getCurrentFrame().getActiveSpreadSheet();
			if (sheet == null) return "view=" + manager.getTopViewId() + ", sheet=null";
			return "view=" + manager.getTopViewId() + ", selectedRows=" + java.util.Arrays.toString(sheet.getSelectedRows())
				+ ", selectedNodes=" + sheet.getSelectedNodes().stream()
					.map(node -> node.getImpl().getClass().getName()).toList();
		});
	}

	String describeLastRouteDiagnostics() {
		return "lastPhysicalButton={" + lastPhysicalButtonClick + "}, chooserFileNameCalls=" + chooserFileNameCalls.get()
			+ ", chooserFileNamesCalls=" + chooserFileNamesCalls.get();
	}

	/** Selects {@code row} of the active sheet with a real click on its first text column. */
	void clickRow(int row) throws Exception {
		SpreadSheet sheet = activeSheet();
		for (int attempt = 0; attempt < 3 && sheet.getSelectedRow() != row; attempt++) {
			if (attempt > 0) activateWindow();
			Rectangle cell = cellOnScreen(sheet, row);
			robot.mouseMove(cell.x + cell.width / 2, cell.y + cell.height / 2);
			robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
			robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
			robot.waitForIdle();
		}
		GuiAcceptanceSupport.await(() -> sheet.getSelectedRow() == row,
			"a physical click did not select row " + row + " of the task table");
	}

	private Rectangle cellOnScreen(SpreadSheet sheet, int row) throws Exception {
		return onEdt(() -> {
			// Column 0 is the row indicator; the first data column is the name column.
			int column = Math.min(1, sheet.getColumnCount() - 1);
			Rectangle cell = sheet.getCellRect(row, column, true);
			Point location = sheet.getLocationOnScreen();
			return new Rectangle(WindowAcceptanceAssertions.robotPointFor(
				new Rectangle(location.x + cell.x, location.y + cell.y, cell.width, cell.height)),
				new java.awt.Dimension(1, 1));
		});
	}

	void pressKey(int keyCode) {
		robot.keyPress(keyCode);
		robot.keyRelease(keyCode);
		robot.waitForIdle();
	}

	// ---- windows ----

	/** The windows that are showing now. */
	Set<Window> showingWindows() throws Exception {
		return onEdt(() -> {
			Set<Window> showing = new LinkedHashSet<>();
			for (Window candidate : Window.getWindows()) {
				if (candidate.isShowing()) showing.add(candidate);
			}
			return showing;
		});
	}

	/** A dialog or frame that appeared since {@code before}, or {@code null}. Popup windows do not count. */
	Window newWindow(Set<Window> before) throws Exception {
		return onEdt(() -> {
			for (Window candidate : Window.getWindows()) {
				if (candidate.isShowing() && !before.contains(candidate) && candidate != window
					&& (candidate instanceof Dialog || candidate instanceof Frame)) {
					return candidate;
				}
			}
			return null;
		});
	}

	/** Closes everything that opened since {@code before}: Escape first, the window's own close as the fallback. */
	void closeNewWindows(Set<Window> before) throws Exception {
		for (int round = 0; round < 4; round++) {
			Window opened = newWindow(before);
			if (opened == null) return;
			closeWindow(opened);
		}
	}

	void closeWindow(Window opened) throws Exception {
		pressKey(java.awt.event.KeyEvent.VK_ESCAPE);
		try {
			GuiAcceptanceSupport.await(() -> !showing(opened), "Escape did not close " + opened.getClass().getSimpleName());
		} catch (AssertionError stillOpen) {
			SwingUtilities.invokeAndWait(opened::dispose);
			GuiAcceptanceSupport.await(() -> !showing(opened), opened.getClass().getSimpleName() + " stayed open after dispose");
		}
	}

	private boolean showing(Window candidate) {
		try {
			return onEdt(candidate::isShowing);
		} catch (Exception failure) {
			throw new AssertionError("Cannot read the state of " + candidate.getClass().getSimpleName(), failure);
		}
	}

	Map<String, String> describeWindows(Set<Window> before) throws Exception {
		return onEdt(() -> {
			Map<String, String> windows = new LinkedHashMap<>();
			for (Window candidate : Window.getWindows()) {
				if (candidate.isShowing() && !before.contains(candidate)) {
					windows.put(candidate.getClass().getName(), candidate.getClass().getSimpleName());
				}
			}
			return windows;
		});
	}
}
