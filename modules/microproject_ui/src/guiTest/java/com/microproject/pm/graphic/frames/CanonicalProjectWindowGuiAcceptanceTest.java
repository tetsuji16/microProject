/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.pm.graphic.frames.workspace.FrameManager;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetModel;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.NormalTask;
import com.microproject.options.CalendarOption;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.Environment;

/** GUI-MSP-WINDOW-06: canonical aliases focus an existing document instead of opening a duplicate. */
class CanonicalProjectWindowGuiAcceptanceTest {
	private MainRibbonFrame window;
	private FrameManager frameManager;
	private GraphicManager graphicManager;
	private Path file;
	private boolean previousRibbonUi;
	private boolean previousNewLook;

	@AfterEach
	void closeWindow() throws Exception {
		if (graphicManager != null) SwingUtilities.invokeAndWait(() -> graphicManager.cleanUp());
		if (window != null) SwingUtilities.invokeAndWait(() -> window.dispose());
		if (file != null) Files.deleteIfExists(file);
		Environment.setRibbonUI(previousRibbonUi);
		Environment.setNewLook(previousNewLook);
	}

	@Test
	void robotFocusesTheExistingFrameForAnEquivalentCanonicalPath() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		file = Files.createTempFile("canonical-window-", ".mpo");
		Project first = project("Canonical project", file.toRealPath().toString());
		Project alias = project("Duplicate object must not create a frame", file.getParent().resolve(".").resolve(file.getFileName()).toString());
		DocumentFrame[] frames = new DocumentFrame[2];
		GraphicManager[] managers = new GraphicManager[1];
		int[] existingFrameCount = new int[1];
		SwingUtilities.invokeAndWait(() -> {
			window = new MainRibbonFrame("microProject — Canonical project-window GUI acceptance", null);
			GraphicManager manager = new GraphicManager(window);
			graphicManager = manager;
			managers[0] = manager;
			window.setGraphicManager(manager);
			manager.initView();
			frameManager = manager.getFrameManager();
			existingFrameCount[0] = frameManager.getAllFrames().size();
			frames[0] = manager.addProjectFrame(first);
			frames[1] = manager.addProjectFrame(alias);
			window.setSize(920, 560);
			window.setLocationByPlatform(true);
			window.setAlwaysOnTop(true);
			window.setVisible(true);
		});
		GuiAcceptanceSupport.await(() -> window.isShowing() && frames[0].isShowing(), "canonical project frame did not become visible");
		assertSame(frames[0], frames[1], "equivalent project paths created separate document frames: first="
				+ frames[0].getProject().getFileName() + ", alias=" + alias.getFileName());
		assertSame(frames[0], managers[0].getCurrentFrame());
		assertEquals(existingFrameCount[0] + 1, frameManager.getAllFrames().size());
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(40);
		capture(robot);
	}

	@Test
	void fullRibbonWindowShowsAnEmptyTaskBetweenTwoTasks() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		file = Files.createTempFile("empty-middle-task-", ".mpo");
		Project project = project("Empty middle task", file.toRealPath().toString());
		// Match issue #451: the empty row is inserted between rows 9 and 10.
		NormalTask[] tasks = new NormalTask[15];
		long[] durationsInDays = {10, 10, 10, 10, 10, 10, 4, 10, 1, 0, 70, 10, 10, 10, 10};
		for (int index = 1; index <= tasks.length; index++) {
			NormalTask task = project.createScriptedTask();
			task.setName(switch (index) {
				case 7 -> "Obtain building permit";
				case 8 -> "Submit preliminary plans";
				case 9 -> "Submit monthly reports";
				case 10 -> "";
				case 11 -> "Long Lead Procurement";
				default -> "Task " + index;
			});
			task.getCurrentSchedule().setStart(project.getStart());
			task.setDuration(durationsInDays[index - 1] * CalendarOption.getInstance().getMillisPerDay());
			tasks[index - 1] = task;
			if (index > 1 && index != 10) {
				int predecessorIndex = index == 11 ? 9 : index - 1;
				DependencyService.getInstance().newDependency(tasks[predecessorIndex - 1], task,
					DependencyType.FS, 0L, project);
			}
		}
		project.recalculate();
		assertTrue(tasks[0] != null && !tasks[0].isManuallyScheduled(), "new tasks default to automatic scheduling");
		assertEquals(4 * CalendarOption.getInstance().getMillisPerDay(), tasks[6].getDuration());
		assertEquals(70 * CalendarOption.getInstance().getMillisPerDay(), tasks[10].getDuration());
		assertEquals(1, tasks[8].getPredecessorList().size());
		assertEquals(0, tasks[9].getPredecessorList().size(), "the inserted empty row must not own a dependency");
		assertEquals(1, tasks[10].getPredecessorList().size());
		SwingUtilities.invokeAndWait(() -> {
			window = new MainRibbonFrame("microProject — empty middle task GUI acceptance", null);
			graphicManager = new GraphicManager(window);
			window.setGraphicManager(graphicManager);
			graphicManager.initView();
			graphicManager.addProjectFrame(project);
			window.setSize(1120, 700);
			window.setLocationByPlatform(true);
			window.setAlwaysOnTop(true);
			window.setVisible(true);
		});
		GuiAcceptanceSupport.await(() -> window.isShowing() && graphicManager.getCurrentFrame() != null,
			"full ribbon project window did not become visible");
		SwingUtilities.invokeAndWait(() -> {
			var frame = graphicManager.getCurrentFrame();
			for (int i = 0; i < 3; i++) frame.doZoomIn();
			if (frame != null && frame.getActiveSpreadSheet() != null) {
				frame.getActiveSpreadSheet().getColumnModel().getColumn(2).setPreferredWidth(95);
				frame.getActiveSpreadSheet().getColumnModel().getColumn(2).setWidth(95);
				int lastColumn = frame.getActiveSpreadSheet().getColumnModel().getColumnCount() - 1;
				frame.getActiveSpreadSheet().getColumnModel().getColumn(lastColumn).setPreferredWidth(75);
				frame.getActiveSpreadSheet().getColumnModel().getColumn(lastColumn).setWidth(75);
			}
		});
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(40);
		robot.delay(700);
		capture(robot, "issue451-matched-dependencies-v2.png");
	}

	@Test
	void robotCtrlDeleteClearsSelectedFieldWithoutDeletingItsTaskAndSupportsUndoRedo() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		Project project = project("Ctrl Delete", null);
		NormalTask task = project.createScriptedTask();
		task.setName("Clear with Ctrl Delete");
		DocumentFrame[] document = new DocumentFrame[1];
		SpreadSheet[] sheets = new SpreadSheet[1];
		int[] taskCell = new int[2];
		SwingUtilities.invokeAndWait(() -> {
			window = new MainRibbonFrame("microProject — Ctrl Delete GUI acceptance", null);
			graphicManager = new GraphicManager(window);
			window.setGraphicManager(graphicManager);
			graphicManager.initView();
			document[0] = graphicManager.addProjectFrame(project);
			window.setSize(920, 560);
			window.setLocationByPlatform(true);
			window.setAlwaysOnTop(true);
			window.setVisible(true);
		});
		GuiAcceptanceSupport.await(() -> window.isShowing() && document[0].isShowing(), "project document did not become visible");
		SwingUtilities.invokeAndWait(() -> {
			sheets[0] = document[0].getActiveSpreadSheet();
			taskCell[0] = findTaskRow(sheets[0], task);
			taskCell[1] = findNameColumn(sheets[0]);
		});
		GuiAcceptanceSupport.await(() -> sheets[0] != null && sheets[0].isShowing(), "task spreadsheet did not become visible");
		assertTrue(taskCell[0] >= 0 && taskCell[1] >= 0, "fixture task Name cell was not visible");
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(40);
		Rectangle[] bounds = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> bounds[0] = sheets[0].getCellRect(taskCell[0], taskCell[1], true));
		java.awt.Point point = sheets[0].getLocationOnScreen();
		robot.mouseMove(point.x + bounds[0].x + Math.max(2, bounds[0].width / 2),
			point.y + bounds[0].y + Math.max(2, bounds[0].height / 2));
		robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
		// A task-table click intentionally selects the full task row. Narrow the
		// spreadsheet selection back to the clicked field before exercising the
		// physical global shortcut, matching MSP's "selected field" contract.
		SwingUtilities.invokeAndWait(() -> {
			sheets[0].changeSelection(taskCell[0], taskCell[1], false, false);
			sheets[0].requestFocusInWindow();
		});
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_DELETE);
		robot.keyRelease(KeyEvent.VK_DELETE);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		GuiAcceptanceSupport.await(() -> "".equals(task.getName()), "physical Ctrl+Delete did not clear the selected Name field");
		SwingUtilities.invokeAndWait(() -> {
			assertEquals(task, ((SpreadSheetModel) sheets[0].getModel()).getNodeForDisplayRow(taskCell[0]).getImpl());
			assertEquals("", sheets[0].getValueAt(taskCell[0], taskCell[1]), "rendered cell was not cleared");
		});
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_Z);
		robot.keyRelease(KeyEvent.VK_Z);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		GuiAcceptanceSupport.await(() -> "Clear with Ctrl Delete".equals(task.getName()), "Ctrl+Z did not restore cleared value");
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_Y);
		robot.keyRelease(KeyEvent.VK_Y);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		GuiAcceptanceSupport.await(() -> "".equals(task.getName()), "Ctrl+Y did not reapply clear");
	}

	@Test
	void robotConvertReconvertsOrReportsUnsupportedImeOnBothSpreadsheetRoutes() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		// Environment-dependent oracle (#991): a desktop with a Japanese IME must really
		// reconvert; without one the request fails and the text must survive unchanged.
		// Other platforms use the JDK input context and need their own oracle.
		Assumptions.assumeTrue(System.getProperty("os.name", "").startsWith("Windows"),
			"These oracles apply to the Windows IMM32 reconversion route.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		Project project = project("IME reconversion", null);
		NormalTask task = project.createScriptedTask();
		task.setName(RECONVERSION_TEXT);
		DocumentFrame[] document = new DocumentFrame[1];
		SpreadSheet[] sheets = new SpreadSheet[1];
		int[] cell = new int[2];
		SwingUtilities.invokeAndWait(() -> {
			window = new MainRibbonFrame("microProject — IME reconversion GUI acceptance", null);
			graphicManager = new GraphicManager(window);
			window.setGraphicManager(graphicManager);
			graphicManager.initView();
			document[0] = graphicManager.addProjectFrame(project);
			window.setSize(920, 560);
			window.setLocationByPlatform(true);
			window.setAlwaysOnTop(true);
			window.setVisible(true);
		});
		GuiAcceptanceSupport.await(() -> window.isShowing() && document[0].isShowing(), "project document did not become visible");
		SwingUtilities.invokeAndWait(() -> {
			sheets[0] = document[0].getActiveSpreadSheet();
			cell[0] = findTaskRow(sheets[0], task);
			cell[1] = findNameColumn(sheets[0]);
		});
		GuiAcceptanceSupport.await(() -> sheets[0].isShowing(), "task spreadsheet did not become visible");
		assertTrue(cell[0] >= 0 && cell[1] >= 0, "fixture task Name cell was not visible");
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		Rectangle[] bounds = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> bounds[0] = sheets[0].getCellRect(cell[0], cell[1], true));
		java.awt.Point origin = sheets[0].getLocationOnScreen();
		robot.mouseMove(origin.x + bounds[0].x + Math.max(2, bounds[0].width / 2), origin.y + bounds[0].y + bounds[0].height / 2);
		robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
		robot.keyPress(KeyEvent.VK_F2);
		robot.keyRelease(KeyEvent.VK_F2);
		GuiAcceptanceSupport.await(() -> sheets[0].isEditing(), "physical F2 did not start cell editing");
		robot.keyPress(KeyEvent.VK_HOME);
		robot.keyRelease(KeyEvent.VK_HOME);
		robot.keyPress(KeyEvent.VK_SHIFT);
		robot.keyPress(KeyEvent.VK_END);
		robot.keyRelease(KeyEvent.VK_END);
		robot.keyRelease(KeyEvent.VK_SHIFT);
		GuiAcceptanceSupport.await(() -> {
			Component focus = java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
			return focus instanceof JTextField field && RECONVERSION_TEXT.equals(field.getSelectedText());
		}, "could not physically select the existing Japanese text in the active spreadsheet editor");
		if (com.microproject.pm.graphic.spreadsheet.editor.ImeTextInputSupport.isJapaneseImeActive()) {
			assertImeReconversionJourney(robot, task, sheets[0], cell, bounds[0]);
			return;
		}
		robot.keyPress(KeyEvent.VK_CONVERT);
		robot.keyRelease(KeyEvent.VK_CONVERT);
		String expected = com.microproject.dialog.UsabilityStrings.text("spreadsheet.imeReconversionUnsupported");
		GuiAcceptanceSupport.await(() -> expected.equals(statusMessage(document[0])), "editor-route unsupported IME feedback did not appear in the document status bar");
		assertUnsupportedReconversionKeptText(task, sheets[0], cell, "editor route");

		// Route 2: Convert on a selected, non-editing cell starts editing, selects the
		// committed text and must reach the same reconversion outcome.
		robot.keyPress(KeyEvent.VK_ESCAPE);
		robot.keyRelease(KeyEvent.VK_ESCAPE);
		GuiAcceptanceSupport.await(() -> !sheets[0].isEditing(), "physical Escape did not end cell editing");
		SwingUtilities.invokeAndWait(() -> document[0].showStatusMessage(""));
		robot.keyPress(KeyEvent.VK_CONVERT);
		robot.keyRelease(KeyEvent.VK_CONVERT);
		GuiAcceptanceSupport.await(() -> sheets[0].isEditing(), "physical Convert on a selected cell did not start editing");
		GuiAcceptanceSupport.await(() -> expected.equals(statusMessage(document[0])), "cell-route unsupported IME feedback did not appear in the document status bar");
		assertUnsupportedReconversionKeptText(task, sheets[0], cell, "selected-cell route");
		robot.keyPress(KeyEvent.VK_ESCAPE);
		robot.keyRelease(KeyEvent.VK_ESCAPE);
		GuiAcceptanceSupport.await(() -> !sheets[0].isEditing(), "physical Escape did not end cell editing");
		SwingUtilities.invokeAndWait(() -> assertEquals(RECONVERSION_TEXT, task.getName(), "cancelling after unsupported reconversion changed the model"));
	}

	private static final String RECONVERSION_TEXT = "東京";

	private static void assertImeReconversionJourney(Robot robot, NormalTask task, SpreadSheet sheet, int[] cell, Rectangle bounds) throws Exception {
		java.awt.im.InputContext context = sheet.getInputContext();
		boolean wasOpen = context.isCompositionEnabled();
		try {
			// A closed IME must open: Convert is an explicit request.
			context.setCompositionEnabled(false);
			// Route 1: the editor's selection, cancelled and then committed.
			pressKey(robot, KeyEvent.VK_CONVERT);
			awaitComposition(true, "editor route: Convert did not start a composition");
			cancelReconversion(robot);
			assertEditorAndModelText(task, sheet, cell, RECONVERSION_TEXT, "editor route cancel");
			pressKey(robot, KeyEvent.VK_HOME);
			robot.keyPress(KeyEvent.VK_SHIFT);
			pressKey(robot, KeyEvent.VK_END);
			robot.keyRelease(KeyEvent.VK_SHIFT);
			pressKey(robot, KeyEvent.VK_CONVERT);
			assertReconversionCommitsWithUndo(robot, task, sheet, cell, "editor route");

			// Route 2: Convert on a selected, non-editing cell, cancelled and then committed.
			java.awt.Point origin = sheet.getLocationOnScreen();
			robot.mouseMove(origin.x + bounds.x + Math.max(2, bounds.width / 2), origin.y + bounds.y + bounds.height / 2);
			robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
			robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
			robot.delay(600);
			pressKey(robot, KeyEvent.VK_CONVERT);
			GuiAcceptanceSupport.await(sheet::isEditing, "cell route: Convert did not start editing");
			awaitComposition(true, "cell route: Convert did not start a composition");
			cancelReconversion(robot);
			assertEditorAndModelText(task, sheet, cell, RECONVERSION_TEXT, "cell route cancel");
			pressKey(robot, KeyEvent.VK_ESCAPE);
			GuiAcceptanceSupport.await(() -> !sheet.isEditing(), "physical Escape did not end cell editing");
			pressKey(robot, KeyEvent.VK_CONVERT);
			GuiAcceptanceSupport.await(sheet::isEditing, "cell route: second Convert did not start editing");
			assertReconversionCommitsWithUndo(robot, task, sheet, cell, "cell route");
		} finally {
			context.setCompositionEnabled(wasOpen);
		}
	}

	/** Selects the next candidate, commits it, and proves model, rendered value and Undo/Redo. */
	private static void assertReconversionCommitsWithUndo(Robot robot, NormalTask task, SpreadSheet sheet, int[] cell, String route) throws Exception {
		awaitComposition(true, route + ": Convert did not start a composition");
		pressKey(robot, KeyEvent.VK_SPACE);
		robot.delay(500);
		pressKey(robot, KeyEvent.VK_ENTER);
		awaitComposition(false, route + ": Enter did not commit the composition");
		String[] committed = new String[1];
		SwingUtilities.invokeAndWait(() -> committed[0] = ((JTextField) java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner()).getText());
		assertTrue(!committed[0].isEmpty() && !RECONVERSION_TEXT.equals(committed[0]), route + ": the next candidate was not committed: " + committed[0]);
		pressKey(robot, KeyEvent.VK_ENTER);
		GuiAcceptanceSupport.await(() -> !sheet.isEditing(), route + ": Enter did not end cell editing");
		GuiAcceptanceSupport.await(() -> committed[0].equals(task.getName()), route + ": the committed candidate did not reach the model");
		SwingUtilities.invokeAndWait(() -> assertEquals(committed[0], sheet.getValueAt(cell[0], cell[1]), route + ": rendered value differs"));
		pressCtrl(robot, KeyEvent.VK_Z);
		GuiAcceptanceSupport.await(() -> RECONVERSION_TEXT.equals(task.getName()), route + ": Undo did not restore the text");
		pressCtrl(robot, KeyEvent.VK_Y);
		GuiAcceptanceSupport.await(() -> committed[0].equals(task.getName()), route + ": Redo did not reapply the candidate");
		pressCtrl(robot, KeyEvent.VK_Z);
		GuiAcceptanceSupport.await(() -> RECONVERSION_TEXT.equals(task.getName()), route + ": second Undo did not restore the text");
	}

	/** The number of Escapes an IME needs depends on its state; one too many would cancel the cell edit. */
	private static void cancelReconversion(Robot robot) throws Exception {
		for (int attempt = 0; attempt < 6 && isEditorComposing(); attempt++) {
			pressKey(robot, KeyEvent.VK_ESCAPE);
			robot.delay(400);
		}
		awaitComposition(false, "Escape did not end the composition");
	}

	private static void awaitComposition(boolean active, String failure) throws Exception {
		GuiAcceptanceSupport.await(() -> isEditorComposing() == active, failure);
	}

	private static boolean isEditorComposing() {
		boolean[] composing = new boolean[1];
		try {
			SwingUtilities.invokeAndWait(() -> composing[0] = java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner() instanceof JTextField field
				&& com.microproject.pm.graphic.spreadsheet.editor.ImeTextInputSupport.isCompositionActive(field));
		} catch (InterruptedException | java.lang.reflect.InvocationTargetException failure) {
			throw new AssertionError(failure);
		}
		return composing[0];
	}

	private static void assertEditorAndModelText(NormalTask task, SpreadSheet sheet, int[] cell, String expected, String route) throws Exception {
		GuiAcceptanceSupport.await(() -> java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner() instanceof JTextField field
			&& expected.equals(field.getText()), route + ": the editor did not keep the original text");
		SwingUtilities.invokeAndWait(() -> {
			assertEquals(expected, task.getName(), route + ": the model changed");
			assertEquals(expected, sheet.getValueAt(cell[0], cell[1]), route + ": the rendered cell changed");
		});
	}

	private static void pressKey(Robot robot, int key) {
		robot.keyPress(key);
		robot.keyRelease(key);
	}

	private static void pressCtrl(Robot robot, int key) {
		robot.keyPress(KeyEvent.VK_CONTROL);
		pressKey(robot, key);
		robot.keyRelease(KeyEvent.VK_CONTROL);
	}

	private static void assertUnsupportedReconversionKeptText(NormalTask task, SpreadSheet sheet, int[] cell, String route) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			Component focus = java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
			assertTrue(focus instanceof JTextField, route + ": editor focus was lost after unsupported reconversion");
			JTextField field = (JTextField) focus;
			assertEquals(RECONVERSION_TEXT, field.getText(), route + ": unsupported reconversion changed the edited text");
			assertEquals(RECONVERSION_TEXT, field.getSelectedText(), route + ": unsupported reconversion lost the selection");
			assertEquals(RECONVERSION_TEXT, task.getName(), route + ": unsupported reconversion changed the committed model value");
			assertEquals(RECONVERSION_TEXT, sheet.getValueAt(cell[0], cell[1]), route + ": unsupported reconversion changed the rendered cell");
		});
	}

	private static String statusMessage(DocumentFrame frame) {
		Component[] components = frame.statusBar.getComponents();
		return components.length > 2 && components[2] instanceof JLabel label ? label.getText() : "";
	}

	private static int findTaskRow(SpreadSheet sheet, NormalTask task) {
		SpreadSheetModel model = (SpreadSheetModel) sheet.getModel();
		for (int row = 0; row < model.getRowCount(); row++) {
			if (model.getNodeForDisplayRow(row) != null && model.getNodeForDisplayRow(row).getImpl() == task)
				return row;
		}
		return -1;
	}

	private static int findNameColumn(SpreadSheet sheet) {
		for (int column = 0; column < sheet.getColumnCount(); column++) {
			if (sheet.isNameFieldColumn(column))
				return column;
		}
		return -1;
	}

	private void capture(Robot robot) throws Exception {
		capture(robot, "canonical-project-window-no-duplicate.png");
	}

	private void capture(Robot robot, String fileName) throws Exception {
		Rectangle[] bounds = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> bounds[0] = new Rectangle(window.getLocationOnScreen(), window.getSize()));
		BufferedImage image = robot.createScreenCapture(bounds[0]);
		Path artifact = Path.of(System.getProperty("microproject.gui.artifacts.dir", "build/guiTest-artifacts"), fileName);
		Files.createDirectories(artifact.getParent());
		ImageIO.write(image, "png", artifact.toFile());
		assertTrue(image.getWidth() > 400 && image.getHeight() > 300);
	}

	private static Project project(String name, String fileName) {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool(name, undo), undo);
		project.initialize(false, false);
		project.setName(name);
		project.setFileName(fileName);
		return project;
	}
}
