/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.testsupport.scenario;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.AbstractButton;
import javax.swing.JPopupMenu;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microproject.field.Field;
import com.microproject.model.task.Project;
import com.microproject.model.task.Task;
import com.microproject.ui.frame.DocumentFrame;
import com.microproject.ui.testsupport.GuiAcceptanceSupport;
import com.microproject.ui.testsupport.GuiProjectWindow;
import com.microproject.ui.testsupport.GuiRunRecorder;
import com.microproject.ui.testsupport.RibbonGuiSupport;
import com.microproject.ui.testsupport.WindowAcceptanceAssertions;
import com.microproject.ui.view.gantt.Gantt;
import com.microproject.ui.view.spreadsheet.SpreadSheet;
import com.microproject.ui.view.spreadsheet.SpreadSheetModel;
import com.microproject.ui.view.views.GanttView;

/**
 * Opens a {@link GuiScenario}'s fixture in a real main window and runs its steps with the Robot.
 * Every step is recorded with {@link GuiRunRecorder#step}, followed by a state snapshot. A step
 * that does not hold, or that leaves a new {@code uncaught} or {@code log-severe} event behind,
 * ends the run with a {@link GuiScenarioFailure}.
 */
public final class GuiScenarioRunner {
	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final String RIBBON_TAB_PREFIX = "microProjectRibbonTab-";

	private final GuiScenario scenario;
	private final Path workDirectory;
	private GuiProjectWindow window;
	private Robot robot;
	private long lastClickNanos;

	private GuiScenarioRunner(GuiScenario scenario, Path workDirectory) {
		this.scenario = scenario;
		this.workDirectory = workDirectory;
	}

	/** Parses {@code json} and runs it. */
	public static void run(String json) throws Exception {
		run(GuiScenario.parse(json));
	}

	/** Runs {@code scenario}; throws {@link GuiScenarioFailure} at the first step that does not hold. */
	public static void run(GuiScenario scenario) throws Exception {
		Path directory = Files.createTempDirectory("microproject-gui-scenario-");
		try {
			new GuiScenarioRunner(scenario, directory).execute();
		} finally {
			deleteTree(directory);
		}
	}

	private void execute() throws Exception {
		Project project;
		try {
			project = GuiScenarioFixtures.build(scenario.fixture(), workDirectory);
		} catch (Exception | AssertionError failure) {
			throw new GuiScenarioFailure(scenario.name(), 0, scenario.fixture().toString(), "the fixture opens",
				String.valueOf(failure), failure);
		}
		try (GuiProjectWindow opened = GuiProjectWindow.open(project)) {
			window = opened;
			robot = opened.robot();
			GuiRunRecorder.attach(opened.manager());
			List<JsonNode> steps = scenario.steps();
			for (int index = 0; index < steps.size(); index++) runStep(index + 1, steps.get(index));
		}
	}

	private void runStep(int number, JsonNode step) throws Exception {
		String kind = GuiScenario.kind(step);
		JsonNode argument = GuiScenario.argument(step);
		GuiRunRecorder.Session recorder = GuiRunRecorder.current();
		int errorsBefore = errorCount(recorder);
		long started = System.nanoTime();
		Throwable failure = null;
		try {
			perform(kind, argument);
			GuiAcceptanceSupport.awaitInputAndEventQueueIdle(robot, "scenario step " + number);
		} catch (StepMismatch mismatch) {
			failure = new GuiScenarioFailure(scenario.name(), number, step.toString(), mismatch.expected, mismatch.actual, null);
		} catch (Exception | AssertionError error) {
			failure = new GuiScenarioFailure(scenario.name(), number, step.toString(), "the " + kind + " step completes",
				String.valueOf(error), error);
		}
		Map<String, Object> attributes = new LinkedHashMap<>();
		attributes.put("scenario", scenario.name());
		attributes.put("index", number);
		attributes.put("kind", kind);
		attributes.put("args", MAPPER.convertValue(argument, Object.class));
		attributes.put("durationMillis", (System.nanoTime() - started) / 1_000_000L);
		if (failure != null) attributes.put("failure", String.valueOf(failure.getMessage()));
		GuiRunRecorder.step(kind, attributes);
		GuiRunRecorder.snapshot("step-" + number + "-" + kind);
		if (failure != null) throw (GuiScenarioFailure) failure;
		int errorsAfter = errorCount(recorder);
		if (errorsAfter > errorsBefore) {
			throw new GuiScenarioFailure(scenario.name(), number, step.toString(),
				errorsBefore + " uncaught or log-severe events", String.valueOf(errorsAfter), null);
		}
	}

	private static int errorCount(GuiRunRecorder.Session recorder) {
		return recorder.count("uncaught") + recorder.count("log-severe");
	}

	private void perform(String kind, JsonNode argument) throws Exception {
		switch (kind) {
			case "select" -> select(argument);
			case "command" -> command(argument.asText());
			case "key" -> key(argument.asText());
			case "edit" -> edit(argument);
			case "scrollGantt" -> scrollGantt(argument.get("date").asText());
			case "dialog" -> dialog(argument);
			case "expect" -> expect(argument);
			case "saveReload" -> saveReload(argument.asText());
			case "snapshot" -> GuiRunRecorder.snapshot(argument.asText());
			default -> throw new IllegalArgumentException("unknown step kind: " + kind);
		}
	}

	/** A step whose check did not hold; {@link #runStep} adds the scenario name and step number. */
	private static final class StepMismatch extends RuntimeException {
		private static final long serialVersionUID = 1L;
		private final String expected;
		private final String actual;

		StepMismatch(String expected, String actual) {
			super("expected " + expected + ", actual " + actual);
			this.expected = expected;
			this.actual = actual;
		}
	}

	// ---- steps ----

	private void select(JsonNode argument) throws Exception {
		List<Integer> rows = new ArrayList<>();
		if (argument.has("rows")) {
			for (JsonNode row : argument.get("rows")) rows.add(row.asInt() - 1);
		} else {
			for (JsonNode name : argument.get("tasks")) rows.add(rowOfTask(name.asText()));
		}
		for (int index = 0; index < rows.size(); index++) {
			clickCell(rows.get(index), 1, index > 0);
		}
		GuiAcceptanceSupport.await(() -> {
			try {
				List<Integer> selected = onEdt(() -> {
					List<Integer> result = new ArrayList<>();
					for (int row : sheet().getSelectedRows()) result.add(row);
					return result;
				});
				return selected.containsAll(rows) && selected.size() == rows.size();
			} catch (Exception failure) {
				throw new AssertionError(failure);
			}
		}, "the clicks did not select table rows " + rows);
	}

	private int rowOfTask(String name) throws Exception {
		List<Task> rows = onEdt(() -> GuiScenarioExpectations.tableTasks(window.manager().getCurrentFrame()));
		for (int row = 0; row < rows.size(); row++) {
			if (rows.get(row) != null && name.equals(rows.get(row).getName())) return row;
		}
		throw new AssertionError("no table row shows task \"" + name + "\"");
	}

	private void command(String commandId) throws Exception {
		AbstractButton button = findRibbonButton(commandId);
		if (!onEdt(button::isEnabled)) {
			GuiAcceptanceSupport.await(() -> {
				try {
					return onEdt(button::isEnabled);
				} catch (Exception failure) {
					throw new AssertionError(failure);
				}
			}, "ribbon command " + commandId + " is disabled");
		}
		spaceFromLastClick();
		if (onEdt(() -> SwingUtilities.getAncestorOfClass(JPopupMenu.class, button) != null)) {
			WindowAcceptanceAssertions.clickComponent(robot, button);
		} else {
			RibbonGuiSupport.clickSplitPrimary(robot, button);
		}
		lastClickNanos = System.nanoTime();
	}

	/** Finds the button on the selected ribbon tab, or on the first tab that shows it. */
	private AbstractButton findRibbonButton(String commandId) throws Exception {
		try {
			return RibbonGuiSupport.findVisibleOrExpand(robot, window.window(), commandId);
		} catch (AssertionError notOnThisTab) {
			// try the other tabs below
		}
		for (AbstractButton tab : onEdt(this::ribbonTabs)) {
			WindowAcceptanceAssertions.clickComponent(robot, tab);
			GuiAcceptanceSupport.awaitInputAndEventQueueIdle(robot, "ribbon tab switch");
			try {
				return RibbonGuiSupport.findVisibleOrExpand(robot, window.window(), commandId);
			} catch (AssertionError notOnThisTab) {
				// keep looking
			}
		}
		throw new AssertionError("ribbon command " + commandId + " was not found on any ribbon tab");
	}

	private List<AbstractButton> ribbonTabs() {
		List<AbstractButton> tabs = new ArrayList<>();
		collect(window.window(), tabs);
		return tabs;
	}

	private static void collect(Component component, List<AbstractButton> tabs) {
		if (component instanceof AbstractButton button && button.isShowing() && button.getName() != null
			&& button.getName().startsWith(RIBBON_TAB_PREFIX)) tabs.add(button);
		if (component instanceof java.awt.Container container) {
			for (Component child : container.getComponents()) collect(child, tabs);
		}
	}

	private void key(String keyStrokeText) throws Exception {
		KeyStroke stroke = KeyStroke.getKeyStroke(keyStrokeText);
		if (stroke == null) throw new IllegalArgumentException("not a KeyStroke: " + keyStrokeText);
		dismissPopups();
		focusSheetUnlessDialogShows();
		int modifiers = stroke.getModifiers();
		List<Integer> held = new ArrayList<>();
		if ((modifiers & InputEvent.CTRL_DOWN_MASK) != 0) held.add(KeyEvent.VK_CONTROL);
		if ((modifiers & InputEvent.SHIFT_DOWN_MASK) != 0) held.add(KeyEvent.VK_SHIFT);
		if ((modifiers & InputEvent.ALT_DOWN_MASK) != 0) held.add(KeyEvent.VK_ALT);
		for (int code : held) robot.keyPress(code);
		robot.keyPress(stroke.getKeyCode());
		robot.keyRelease(stroke.getKeyCode());
		for (int index = held.size() - 1; index >= 0; index--) robot.keyRelease(held.get(index));
	}

	/** A ribbon overflow popup can stay open after its command ran; Escape closes it like a user would. */
	private void dismissPopups() throws Exception {
		for (int attempt = 0; attempt < 3; attempt++) {
			boolean open = onEdt(() -> {
				Component owner = java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
				java.awt.Window ancestor = owner == null ? null : SwingUtilities.getWindowAncestor(owner);
				return ancestor != null && ancestor.getClass().getName().startsWith("javax.swing.Popup");
			});
			if (!open) return;
			robot.keyPress(KeyEvent.VK_ESCAPE);
			robot.keyRelease(KeyEvent.VK_ESCAPE);
			GuiAcceptanceSupport.awaitInputAndEventQueueIdle(robot, "closing a ribbon popup");
		}
		// Escape did not close it: hide the stale popup window directly.
		onEdt(() -> {
			Component owner = java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
			java.awt.Window ancestor = owner == null ? null : SwingUtilities.getWindowAncestor(owner);
			if (ancestor != null && ancestor.getClass().getName().startsWith("javax.swing.Popup")) ancestor.setVisible(false);
			return null;
		});
	}

	private void focusSheetUnlessDialogShows() throws Exception {
		if (onEdt(() -> GuiScenarioExpectations.modalDialogTitle() != null)) return;
		onEdt(() -> {
			SpreadSheet sheet = sheet();
			if (!sheet.isFocusOwner()) sheet.requestFocusInWindow();
			return null;
		});
		GuiAcceptanceSupport.await(() -> {
			try {
				return onEdt(() -> sheet().isFocusOwner());
			} catch (Exception failure) {
				throw new AssertionError(failure);
			}
		}, "the task table did not take the keyboard focus; focus owner is " + onEdtQuietly(() -> {
			Component owner = java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
			return owner == null ? "none" : owner.getClass().getName() + " in " + SwingUtilities.getWindowAncestor(owner);
		}));
	}

	private void edit(JsonNode argument) throws Exception {
		int row = argument.has("row") ? argument.get("row").asInt() - 1 : rowOfTask(argument.get("task").asText());
		int column = viewColumnOf(argument.get("field").asText());
		// A double click on a task cell opens Task Information, so the cell is selected with one click
		// and F2 starts the in-cell editor.
		clickCell(row, column, false);
		robot.keyPress(KeyEvent.VK_F2);
		robot.keyRelease(KeyEvent.VK_F2);
		GuiAcceptanceSupport.await(() -> {
			try {
				return onEdt(() -> sheet().isEditing());
			} catch (Exception failure) {
				throw new AssertionError(failure);
			}
		}, "F2 did not start editing the " + argument.get("field").asText() + " cell");
		pressWithModifier(KeyEvent.VK_CONTROL, KeyEvent.VK_A);
		for (char character : argument.get("text").asText().toCharArray()) type(character);
		robot.keyPress(KeyEvent.VK_ENTER);
		robot.keyRelease(KeyEvent.VK_ENTER);
		GuiAcceptanceSupport.await(() -> {
			try {
				return !onEdt(() -> sheet().isEditing());
			} catch (Exception failure) {
				throw new AssertionError(failure);
			}
		}, "the cell editor stayed open after Enter, so it did not accept \"" + argument.get("text").asText() + "\"");
	}

	private void pressWithModifier(int modifier, int key) {
		robot.keyPress(modifier);
		robot.keyPress(key);
		robot.keyRelease(key);
		robot.keyRelease(modifier);
	}

	private void type(char character) {
		int code = KeyEvent.getExtendedKeyCodeForChar(character);
		if (code == KeyEvent.VK_UNDEFINED) throw new IllegalArgumentException("cannot type the character '" + character + "'");
		boolean shift = Character.isUpperCase(character);
		if (shift) robot.keyPress(KeyEvent.VK_SHIFT);
		robot.keyPress(code);
		robot.keyRelease(code);
		if (shift) robot.keyRelease(KeyEvent.VK_SHIFT);
	}

	/** The view column showing {@code fieldId} ({@code Field.duration}; the {@code Field.} prefix is optional). */
	private int viewColumnOf(String fieldId) throws Exception {
		String wanted = fieldId.startsWith("Field.") ? fieldId : "Field." + fieldId;
		return onEdt(() -> {
			SpreadSheet sheet = sheet();
			SpreadSheetModel model = (SpreadSheetModel) sheet.getModel();
			for (int modelColumn = 0; modelColumn < model.getColumnCount(); modelColumn++) {
				Field field = model.getFieldInColumn(modelColumn);
				if (field != null && wanted.equals(field.getId())) return sheet.convertColumnIndexToView(modelColumn);
			}
			throw new AssertionError("the task table has no column for " + wanted);
		});
	}

	private void scrollGantt(String date) throws Exception {
		long millis = GuiScenarioFixtures.millis(date);
		onEdt(() -> {
			DocumentFrame frame = window.manager().getCurrentFrame();
			if (!(frame.getActiveTopView() instanceof GanttView view) || view.getGantt() == null) {
				throw new AssertionError("the Gantt chart is not the active view");
			}
			Gantt gantt = view.getGantt();
			if (!gantt.scrollToDate(millis)) throw new AssertionError("the Gantt chart could not scroll to " + date);
			return null;
		});
		// The chart can lay out again after the scroll; wait until its left position stops moving.
		int[] previous = {Integer.MIN_VALUE};
		GuiAcceptanceSupport.await(() -> {
			try {
				int now = onEdt(() -> {
					Gantt gantt = ((GanttView) window.manager().getCurrentFrame().getActiveTopView()).getGantt();
					return ((JViewport) gantt.getParent()).getViewPosition().x;
				});
				boolean steady = now == previous[0];
				previous[0] = now;
				if (!steady) robot.delay(100);
				return steady;
			} catch (Exception failure) {
				throw new AssertionError(failure);
			}
		}, "the Gantt chart kept moving after scrolling to " + date);
	}

	private void dialog(JsonNode argument) throws Exception {
		AtomicReference<String> title = new AtomicReference<>();
		GuiAcceptanceSupport.await(() -> {
			try {
				title.set(onEdt(GuiScenarioExpectations::modalDialogTitle));
				return title.get() != null;
			} catch (Exception failure) {
				throw new AssertionError(failure);
			}
		}, "no modal dialog is showing");
		if (argument.hasNonNull("title") && (title.get() == null || !title.get().contains(argument.get("title").asText()))) {
			throw new StepMismatch("a dialog titled \"" + argument.get("title").asText() + "\"", "\"" + title.get() + "\"");
		}
		String press = argument.path("press").asText("escape");
		int code = switch (press) {
			case "escape" -> KeyEvent.VK_ESCAPE;
			case "enter" -> KeyEvent.VK_ENTER;
			default -> throw new IllegalArgumentException("dialog press must be escape or enter: " + press);
		};
		robot.keyPress(code);
		robot.keyRelease(code);
		GuiAcceptanceSupport.await(() -> {
			try {
				return onEdt(GuiScenarioExpectations::modalDialogTitle) == null;
			} catch (Exception failure) {
				throw new AssertionError(failure);
			}
		}, "the dialog \"" + title.get() + "\" stayed open after " + press);
	}

	private void expect(JsonNode argument) throws Exception {
		List<GuiScenarioExpectations.Mismatch> mismatches = GuiScenarioExpectations.check(window, argument);
		if (mismatches.isEmpty()) return;
		Map<String, Object> expected = new LinkedHashMap<>();
		Map<String, Object> actual = new LinkedHashMap<>();
		for (GuiScenarioExpectations.Mismatch mismatch : mismatches) {
			expected.put(mismatch.path(), mismatch.expected());
			actual.put(mismatch.path(), mismatch.actual());
		}
		throw new StepMismatch(MAPPER.writeValueAsString(expected), MAPPER.writeValueAsString(actual));
	}

	private void saveReload(String format) throws Exception {
		if (!"mpo".equals(format)) throw new IllegalArgumentException("saveReload supports only \"mpo\": " + format);
		String difference = GuiScenarioExpectations.saveAndReloadDifference(window.project(), workDirectory);
		if (difference != null) {
			throw new StepMismatch("the reloaded project equals the saved one", difference);
		}
	}

	// ---- table access ----

	private SpreadSheet sheet() {
		return window.manager().getCurrentFrame().getActiveSpreadSheet();
	}

	private void clickCell(int row, int column, boolean withControl) throws Exception {
		onEdt(() -> {
			SpreadSheet sheet = sheet();
			sheet.scrollRectToVisible(sheet.getCellRect(row, column, true));
			return null;
		});
		robot.waitForIdle();
		spaceFromLastClick();
		Point point = cellPoint(row, column);
		robot.mouseMove(point.x, point.y);
		if (withControl) robot.keyPress(KeyEvent.VK_CONTROL);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		if (withControl) robot.keyRelease(KeyEvent.VK_CONTROL);
		lastClickNanos = System.nanoTime();
		robot.waitForIdle();
	}

	private Point cellPoint(int row, int column) throws Exception {
		return onEdt(() -> {
			SpreadSheet sheet = sheet();
			sheet.scrollRectToVisible(sheet.getCellRect(row, column, true));
			Rectangle cell = sheet.getCellRect(row, column, true);
			Point location = sheet.getLocationOnScreen();
			return WindowAcceptanceAssertions.robotPointFor(
				new Rectangle(location.x + cell.x, location.y + cell.y, cell.width, cell.height));
		});
	}

	/** Two clicks closer than the platform's double-click interval would act as a double click. */
	private void spaceFromLastClick() {
		Object interval = Toolkit.getDefaultToolkit().getDesktopProperty("awt.multiClickInterval");
		long millis = (interval instanceof Integer value ? value : 500) + 100L;
		long elapsed = (System.nanoTime() - lastClickNanos) / 1_000_000L;
		if (lastClickNanos != 0 && elapsed < millis) robot.delay((int) (millis - elapsed));
	}

	private static String onEdtQuietly(java.util.concurrent.Callable<String> action) {
		try {
			return onEdt(action);
		} catch (Exception failure) {
			return failure.toString();
		}
	}

	private static <T> T onEdt(java.util.concurrent.Callable<T> action) throws Exception {
		AtomicReference<T> result = new AtomicReference<>();
		AtomicReference<Throwable> failure = new AtomicReference<>();
		GuiAcceptanceSupport.runOnEdtWithTimeout(() -> {
			try {
				result.set(action.call());
			} catch (Throwable error) {
				failure.set(error);
			}
		}, "scenario step on the EDT");
		if (failure.get() instanceof Exception exception) throw exception;
		if (failure.get() instanceof Error error) throw error;
		return result.get();
	}

	private static void deleteTree(Path directory) throws IOException {
		try (var paths = Files.walk(directory)) {
			for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
		}
	}
}
