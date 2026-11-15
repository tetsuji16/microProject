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

import javax.swing.AbstractButton;
import javax.swing.SwingUtilities;

import com.microproject.menu.testsupport.UiComponentWalker;
import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.pm.graphic.frames.MainRibbonFrame;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.testsupport.RibbonGuiSupport;
import com.microproject.testsupport.WindowAcceptanceAssertions;
import com.microproject.ui.ribbon.RibbonDisplayMode;
import com.microproject.ui.ribbon.RibbonDisplayPreferences;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.Environment;

/**
 * A real main window with a small in-memory project, for acceptance tests that click ribbon
 * commands with the Robot. The project lives in memory, so nothing under {@code samples} is
 * touched. The set of commands under test is read from {@code menuInternal.properties}, not
 * copied into the tests.
 */
final class RibbonCommandGuiFixture implements AutoCloseable {
	private static final String RIBBON_RESOURCE = "/com/microproject/menu/menuInternal.properties";
	private static final Properties RIBBON = loadRibbonProperties();

	final MainRibbonFrame window;
	final GraphicManager manager;
	final Robot robot;
	private final boolean previousRibbonUi;
	private final boolean previousNewLook;
	private final RibbonDisplayMode previousDisplayMode;

	private RibbonCommandGuiFixture(MainRibbonFrame window, GraphicManager manager, Robot robot,
		boolean ribbonUi, boolean newLook, RibbonDisplayMode displayMode) {
		this.window = window;
		this.manager = manager;
		this.robot = robot;
		this.previousRibbonUi = ribbonUi;
		this.previousNewLook = newLook;
		this.previousDisplayMode = displayMode;
	}

	/** Opens the main window on a project with {@code taskCount} tasks and two resources. */
	static RibbonCommandGuiFixture open(int taskCount) throws Exception {
		boolean ribbonUi = Environment.isRibbonUI();
		boolean newLook = Environment.isNewLook();
		RibbonDisplayMode displayMode = RibbonDisplayPreferences.load();
		RibbonDisplayPreferences.save(RibbonDisplayMode.ALWAYS_SHOW);
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
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		RibbonCommandGuiFixture fixture = new RibbonCommandGuiFixture(frame[0], manager[0], robot, ribbonUi, newLook, displayMode);
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
		RibbonDisplayPreferences.save(previousDisplayMode);
		Environment.setRibbonUI(previousRibbonUi);
		Environment.setNewLook(previousNewLook);
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

	void click(Component component) throws Exception {
		WindowAcceptanceAssertions.clickComponent(robot, component);
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

	SpreadSheet activeSheet() throws Exception {
		return onEdt(() -> manager.getCurrentFrame().getActiveSpreadSheet());
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
			pressKey(java.awt.event.KeyEvent.VK_ESCAPE);
			try {
				GuiAcceptanceSupport.await(() -> !showing(opened), "Escape did not close " + opened.getClass().getSimpleName());
			} catch (AssertionError stillOpen) {
				SwingUtilities.invokeAndWait(opened::dispose);
				GuiAcceptanceSupport.await(() -> !showing(opened), opened.getClass().getSimpleName() + " stayed open after dispose");
			}
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
