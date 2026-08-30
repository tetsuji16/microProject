/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.spreadsheet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.microproject.field.Field;
import com.microproject.graphic.configuration.SpreadSheetCategories;
import com.microproject.grouping.core.Node;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.graphic.spreadsheet.renderer.NameCellComponent;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.undo.DataFactoryUndoController;

/** GUI-NC-12: visible task-name text editing must only change the targeted cell. */
class TaskTextInputGuiAcceptanceTest {
	private JFrame frame;

	@AfterEach
	void closeWindow() throws Exception {
		if (frame != null) {
			SwingUtilities.invokeAndWait(() -> {
				frame.dispose();
				frame = null;
			});
		}
	}

	@ParameterizedTest(name = "task name input [{0}]")
	@NullAndEmptySource
	@ValueSource(strings = { "日本語タスク 長文入力 1234567890" })
	void robotTaskNameInputKeepsTextInTargetCellOnly(String input) throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture();
		showFixture(fixture);
		Robot robot = new Robot();
		robot.setAutoDelay(40);
		activateWindow(fixture);
		clickCell(robot, fixture);
		GuiAcceptanceSupport.await(() -> fixture.sheet.isFocusOwner(), "spreadsheet did not receive focus");

		// Use the same root-pane F2 route as the production shortcut, then commit through the visible editor.
		SwingUtilities.invokeAndWait(() -> {
			fixture.sheet.getRootPane().getActionMap().get("EditField").actionPerformed(null);
			JTextComponent editor = (JTextComponent) ((NameCellComponent) fixture.sheet.getEditorComponent()).getTextComponent();
			assertTrue(fixture.sheet.isEditing(), "F2 must start task-name editing");
			editor.setText(input == null ? "" : input);
			assertTrue(fixture.sheet.getCellEditor().stopCellEditing(), "task-name editor rejected input");
		});
		GuiAcceptanceSupport.await(() -> !fixture.sheet.isEditing(), "task-name edit did not commit");

		assertEquals(input == null ? "" : input, fixture.task.getName());
		assertEquals("Untouched", fixture.otherTask.getName(), "editing one row must not change its neighbor");
	}

	private void showFixture(Fixture fixture) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			frame = new JFrame("Task text input GUI acceptance");
			JComponent root = frame.getRootPane();
			root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "EditField");
			ActionMap actions = root.getActionMap();
			actions.put("EditField", new AbstractAction() {
				@Override public void actionPerformed(java.awt.event.ActionEvent event) { fixture.sheet.editActiveCell(); }
			});
			frame.add(new JScrollPane(fixture.sheet));
			frame.setPreferredSize(new Dimension(900, 420));
			frame.pack();
			frame.setLocationByPlatform(true);
			frame.setAlwaysOnTop(true);
			frame.setVisible(true);
		});
	}

	private void activateWindow(Fixture fixture) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			frame.toFront();
			frame.requestFocus();
			fixture.sheet.requestFocusInWindow();
		});
	}

	private static void clickCell(Robot robot, Fixture fixture) throws Exception {
		final Rectangle[] cell = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> {
			fixture.sheet.changeSelection(fixture.taskRow, fixture.nameColumn, false, false);
			Rectangle bounds = fixture.sheet.getCellRect(fixture.taskRow, fixture.nameColumn, true);
			Point location = fixture.sheet.getLocationOnScreen();
			cell[0] = new Rectangle(location.x + bounds.x, location.y + bounds.y, bounds.width, bounds.height);
		});
		robot.mouseMove(cell[0].x + cell[0].width / 2, cell[0].y + cell[0].height / 2);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
	}

	private static Fixture createFixture() throws Exception {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("gui-text-input-test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		NormalTask task = project.createScriptedTask();
		task.setName("Original");
		NormalTask otherTask = project.createScriptedTask();
		otherTask.setName("Untouched");
		final Fixture[] result = new Fixture[1];
		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet sheet = new SpreadSheet();
			sheet.setSpreadSheetCategory(SpreadSheetCategories.taskSpreadsheetCategory);
			NodeModelCache cache = NodeModelCacheFactory.getInstance().createFilteredCache(
				NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel()), "gui-text-input-test", null);
			SpreadSheetUtils.setFieldsAndContext(sheet, cache, SpreadSheetCategories.taskSpreadsheetCategory, "Spreadsheet.Task.entry", true);
			Node taskNode = (Node) cache.getModel().search(task);
			int taskRow = ((SpreadSheetModel) sheet.getModel()).findGraphicNodeRow(cache.getGraphicNode(taskNode));
			int nameColumn = findNameColumn(sheet);
			result[0] = new Fixture(sheet, task, otherTask, taskRow, nameColumn);
		});
		return result[0];
	}

	private static int findNameColumn(SpreadSheet sheet) {
		SpreadSheetModel model = (SpreadSheetModel) sheet.getModel();
		for (int modelColumn = 0; modelColumn < model.getColumnCount(); modelColumn++) {
			Field field = model.getFieldInColumn(modelColumn);
			if (field != null && field.isNameField())
				return sheet.convertColumnIndexToView(modelColumn);
		}
		throw new IllegalArgumentException("Missing task name column");
	}

	private record Fixture(SpreadSheet sheet, NormalTask task, NormalTask otherTask, int taskRow, int nameColumn) { }
}
