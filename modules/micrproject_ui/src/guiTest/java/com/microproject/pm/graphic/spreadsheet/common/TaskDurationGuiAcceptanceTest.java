/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.spreadsheet.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.AWTEvent;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.JFrame;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.field.Field;
import com.microproject.graphic.configuration.SpreadSheetCategories;
import com.microproject.grouping.core.Node;
import com.microproject.options.CalendarOption;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetModel;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetUtils;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.testsupport.GuiAcceptanceSupport;

/** Non-headless regression coverage for the visible task duration edit flow. */
class TaskDurationGuiAcceptanceTest {
	private JFrame frame;
	private DialogCapture dialogs;

	@AfterEach
	void closeWindow() throws Exception {
		if (dialogs != null)
			dialogs.close();
		if (frame != null) {
			SwingUtilities.invokeAndWait(() -> {
				frame.dispose();
				frame = null;
			});
		}
	}

	@Test
	void f2DurationEditCommitsThroughTheVisibleSpreadsheetWithoutUnexpectedDialog() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture();
		dialogs = new DialogCapture();
		dialogs.open();
		showFixture(fixture);

		Robot robot = new Robot();
		robot.setAutoDelay(40);
		try {
			activateFixtureWindow(fixture);
			clickCell(robot, fixture);
			GuiAcceptanceSupport.await(() -> frame.isFocused() && fixture.sheet.isFocusOwner(),
				"the Robot click did not leave the spreadsheet in the active fixture window");
			assertEquals(fixture.durationColumn, fixture.sheet.getSelectedColumn());
			assertEquals(fixture.durationColumn, fixture.sheet.getSelection().getActiveColumn());
			assertNull(fixture.sheet.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
				.get(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0)), "ETable must not intercept the global F2 shortcut");
			assertNull(fixture.sheet.getInputMap(JComponent.WHEN_FOCUSED)
				.get(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0)), "the spreadsheet itself must not intercept the global F2 shortcut");

			// Dispatch the F2 KeyEvent through Swing's focused-component route. Calling the root-pane action directly would
			// bypass the ETable ancestor InputMap that caused issue #415, so it
			// cannot prove that the duplicate shortcut route stays removed.
			dispatchF2(fixture.sheet);
			GuiAcceptanceSupport.await(() -> Boolean.TRUE.equals(fixture.sheet.getClientProperty("gui.edit.started")),
				"F2 did not reach the root-pane EditField action");
			GuiAcceptanceSupport.await(fixture.sheet::isEditing, "F2 did not start editing");
			GuiAcceptanceSupport.await(() -> fixture.sheet.getEditorComponent() != null && fixture.sheet.getEditorComponent().isFocusOwner(), "editor did not receive focus");
			assertEquals(fixture.durationColumn, fixture.sheet.getEditingColumn(), "F2 must edit the selected duration column");
			SwingUtilities.invokeAndWait(() -> {
				JTextComponent editor = (JTextComponent) fixture.sheet.getEditorComponent();
				editor.setText("3");
				assertTrue(fixture.sheet.getCellEditor().stopCellEditing(), "duration editor rejected valid text");
			});
			GuiAcceptanceSupport.await(() -> !fixture.sheet.isEditing(), "Enter did not commit the edit");

			assertEquals(3L * CalendarOption.getInstance().getMillisPerDay(), fixture.task.getRawDuration());
			assertTrue(dialogs.messages().isEmpty(), "unexpected dialog: " + dialogs.messages());
		} catch (AssertionError | RuntimeException failure) {
			captureFailure(robot, failure);
			throw failure;
		}
	}

	private void showFixture(Fixture fixture) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			frame = new JFrame("Task duration GUI acceptance");
			JComponent rootPane = frame.getRootPane();
			rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "EditField");
			ActionMap actions = rootPane.getActionMap();
			actions.put("EditField", new AbstractAction() {
				@Override public void actionPerformed(java.awt.event.ActionEvent event) {
					fixture.sheet.putClientProperty("gui.edit.started", fixture.sheet.editActiveCell());
				}
			});
			frame.add(new JScrollPane(fixture.sheet));
			frame.setPreferredSize(new Dimension(900, 420));
			frame.pack();
			frame.setLocationByPlatform(true);
			frame.setAlwaysOnTop(true);
			frame.setVisible(true);
		});
	}

	private void activateFixtureWindow(Fixture fixture) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			frame.toFront();
			frame.requestFocus();
			fixture.sheet.requestFocusInWindow();
		});
		GuiAcceptanceSupport.await(fixture.sheet::isFocusOwner, "spreadsheet did not receive focus");
	}

	private static Fixture createFixture() throws Exception {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("gui-duration-edit-test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		NormalTask task = project.createScriptedTask();
		task.setName("Duration edit");
		task.setDuration(CalendarOption.getInstance().getMillisPerDay());
		final Fixture[] fixture = new Fixture[1];
		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet sheet = new SpreadSheet();
			sheet.setSpreadSheetCategory(SpreadSheetCategories.taskSpreadsheetCategory);
			NodeModelCache cache = NodeModelCacheFactory.getInstance().createFilteredCache(
				NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel()), "gui-duration-edit-test", null);
			SpreadSheetUtils.setFieldsAndContext(sheet, cache, SpreadSheetCategories.taskSpreadsheetCategory, "Spreadsheet.Task.entry", true);
			Node taskNode = (Node) cache.getModel().search(task);
			int taskRow = ((SpreadSheetModel) sheet.getModel()).findGraphicNodeRow(cache.getGraphicNode(taskNode));
			fixture[0] = new Fixture(sheet, task, taskRow, findDurationColumn(sheet));
		});
		return fixture[0];
	}

	private static int findDurationColumn(SpreadSheet sheet) {
		SpreadSheetModel model = (SpreadSheetModel) sheet.getModel();
		for (int modelColumn = 0; modelColumn < model.getColumnCount(); modelColumn++) {
			Field field = model.getFieldInColumn(modelColumn);
			if (field != null && "Field.duration".equals(field.getId()))
				return sheet.convertColumnIndexToView(modelColumn);
		}
		throw new IllegalArgumentException("Missing duration column");
	}

	private static void clickCell(Robot robot, Fixture fixture) throws Exception {
		final Rectangle[] cell = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> {
			fixture.sheet.changeSelection(fixture.taskRow, fixture.durationColumn, false, false);
			cell[0] = fixture.sheet.getCellRect(fixture.taskRow, fixture.durationColumn, true);
			cell[0].setLocation(SwingUtilities.convertPoint(fixture.sheet, cell[0].getLocation(), null));
		});
		robot.mouseMove(cell[0].x + cell[0].width / 2, cell[0].y + cell[0].height / 2);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
	}

	private static void dispatchF2(SpreadSheet sheet) throws Exception {
		SwingUtilities.invokeAndWait(() -> KeyboardFocusManager.getCurrentKeyboardFocusManager().dispatchEvent(
			new KeyEvent(sheet, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_F2, KeyEvent.CHAR_UNDEFINED)));
	}

	private static void captureFailure(Robot robot, Throwable failure) {
		try {
			Path directory = Path.of(System.getProperty("micrproject.gui.artifacts.dir", "build/guiTest-artifacts"));
			Files.createDirectories(directory);
			Rectangle bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
			BufferedImage screenshot = robot.createScreenCapture(bounds);
			ImageIO.write(screenshot, "png", directory.resolve("task-duration-edit-failure.png").toFile());
		} catch (IOException captureFailure) {
			failure.addSuppressed(captureFailure);
		}
	}

	private record Fixture(SpreadSheet sheet, NormalTask task, int taskRow, int durationColumn) { }

	private static final class DialogCapture implements AWTEventListener {
		private final List<String> messages = new ArrayList<>();
		void open() { Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.WINDOW_EVENT_MASK); }
		void close() { Toolkit.getDefaultToolkit().removeAWTEventListener(this); }
		List<String> messages() { synchronized (messages) { return List.copyOf(messages); } }
		@Override public void eventDispatched(AWTEvent event) {
			if (event instanceof WindowEvent windowEvent && windowEvent.getID() == WindowEvent.WINDOW_OPENED && windowEvent.getWindow() instanceof Dialog dialog) {
				synchronized (messages) { messages.add(dialog.getTitle()); }
			}
		}
	}

}
