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
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.JTabbedPane;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.dialog.TaskInformationDialog;
import com.microproject.field.Field;
import com.microproject.graphic.configuration.SpreadSheetCategories;
import com.microproject.grouping.core.Node;
import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.pm.graphic.frames.DocumentFrame;
import com.microproject.pm.graphic.frames.workspace.FrameHolder;
import com.microproject.pm.graphic.frames.workspace.FrameManager;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.strings.Messages;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.undo.DataFactoryUndoController;

/** Non-headless coverage for the task-information dialog opened from a task-table double-click. */
class TaskInformationGuiAcceptanceTest {
	private TestFrame frame;
	private GraphicManager previousGraphicManager;

	@AfterEach
	void closeWindow() throws Exception {
		if (frame != null) {
			SwingUtilities.invokeAndWait(() -> {
				for (Window window : Window.getWindows()) {
					if (window instanceof TaskInformationDialog)
						window.dispose();
				}
				GraphicManager.getGraphicManagers().remove(frame.manager);
				frame.dispose();
				frame = null;
			});
			restoreLastGraphicManager(previousGraphicManager);
		}
	}

	@Test
	void taskNameDoubleClickOpensTaskInformationForTheClickedTask() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture();
		showFixture(fixture);

		Robot robot = new Robot();
		robot.setAutoDelay(60);
		activateFixtureWindow(fixture);
		doubleClickTaskName(robot, fixture);

		GuiAcceptanceSupport.await(() -> findTaskInformationDialog() != null,
			"double-clicking the task name did not open Task Information");
		TaskInformationDialog dialog = findTaskInformationDialog();
		assertTrue(dialog.isVisible());
		assertEquals(Messages.getString("TaskInformationDialog.TaskInformation") + " - " + fixture.task.getId(), dialog.getTitle());
		assertAllTabsFitTheirViewport(dialog);
		captureDialog(robot, dialog);
	}

	private static void assertAllTabsFitTheirViewport(TaskInformationDialog dialog) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			JTabbedPane tabs = findTabbedPane(dialog.getContentPane());
			assertTrue(tabs != null, "Task Information must expose its tabbed form");
			for (int index = 0; index < tabs.getTabCount(); index++) {
				tabs.setSelectedIndex(index);
				JComponent tab = (JComponent)tabs.getComponentAt(index);
				assertTrue(tab.getPreferredSize().height <= tabs.getHeight() || tab instanceof JScrollPane,
					"tab " + tabs.getTitleAt(index) + " is clipped without a scrollable viewport");
			}
			JScrollPane general = (JScrollPane)tabs.getComponentAt(0);
			java.awt.Container view = (java.awt.Container)general.getViewport().getView();
			for (java.awt.Component child : view.getComponents()) {
				if (!child.isVisible())
					continue;
				assertTrue(child.getHeight() >= child.getPreferredSize().height,
					"general tab component is vertically clipped: " + child.getClass().getSimpleName());
			}
		});
	}

	private static JTabbedPane findTabbedPane(java.awt.Container container) {
		for (java.awt.Component child : container.getComponents()) {
			if (child instanceof JTabbedPane tabs)
				return tabs;
			if (child instanceof java.awt.Container nested) {
				JTabbedPane tabs = findTabbedPane(nested);
				if (tabs != null)
					return tabs;
			}
		}
		return null;
	}

	private static void captureDialog(Robot robot, TaskInformationDialog dialog) throws Exception {
		Rectangle[] bounds = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> {
			JTabbedPane tabs = findTabbedPane(dialog.getContentPane());
			tabs.setSelectedIndex(0);
			((JScrollPane)tabs.getComponentAt(0)).getVerticalScrollBar().setValue(0);
			bounds[0] = new Rectangle(dialog.getRootPane().getLocationOnScreen(), dialog.getRootPane().getSize());
		});
		// Let the tab selection and scrollbar update paint before capturing; an
		// immediate capture can otherwise retain the previously selected tab.
		robot.waitForIdle();
		BufferedImage screenshot = robot.createScreenCapture(bounds[0]);
		Path directory = Path.of(System.getProperty("micrproject.gui.artifacts.dir", "build/guiTest-artifacts"));
		Files.createDirectories(directory);
		ImageIO.write(screenshot, "png", directory.resolve("task-information-all-tabs.png").toFile());
	}

	private void showFixture(Fixture fixture) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			previousGraphicManager = lastGraphicManager();
			frame = new TestFrame();
			frame.manager.setDocumentFrame(new DocumentFrame(frame.manager, fixture.task.getOwningProject(), "gui-task-information-test"));
			frame.add(new JScrollPane(fixture.sheet));
			frame.setPreferredSize(new Dimension(900, 420));
			frame.pack();
			frame.setLocationByPlatform(true);
			frame.setAlwaysOnTop(true);
			frame.setVisible(true);
		});
	}

	private static GraphicManager lastGraphicManager() {
		try {
			java.lang.reflect.Field field = GraphicManager.class.getDeclaredField("lastGraphicManager");
			field.setAccessible(true);
			return (GraphicManager) field.get(null);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError("could not preserve the prior graphic manager", e);
		}
	}

	private static void restoreLastGraphicManager(GraphicManager manager) {
		try {
			java.lang.reflect.Field field = GraphicManager.class.getDeclaredField("lastGraphicManager");
			field.setAccessible(true);
			field.set(null, manager);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError("could not restore the prior graphic manager", e);
		}
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
		ResourcePool resourcePool = ResourcePool.createRourcePool("gui-task-information-test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		NormalTask task = project.createScriptedTask();
		task.setName("Task Information acceptance");
		final Fixture[] fixture = new Fixture[1];
		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet sheet = new SpreadSheet();
			sheet.setSpreadSheetCategory(SpreadSheetCategories.taskSpreadsheetCategory);
			NodeModelCache cache = NodeModelCacheFactory.getInstance().createFilteredCache(
				NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel()), "gui-task-information-test", null);
			SpreadSheetUtils.setFieldsAndContext(sheet, cache, SpreadSheetCategories.taskSpreadsheetCategory, "Spreadsheet.Task.entry", true);
			Node taskNode = (Node) cache.getModel().search(task);
			int taskRow = ((SpreadSheetModel) sheet.getModel()).findGraphicNodeRow(cache.getGraphicNode(taskNode));
			fixture[0] = new Fixture(sheet, task, taskRow, findNameColumn(sheet));
		});
		return fixture[0];
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

	private static void doubleClickTaskName(Robot robot, Fixture fixture) throws Exception {
		Rectangle cell = cellOnScreen(fixture.sheet, fixture.taskRow, fixture.nameColumn);
		int x = cell.x + cell.width / 2;
		int y = cell.y + cell.height / 2;
		robot.mouseMove(x, y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
	}

	private static Rectangle cellOnScreen(SpreadSheet sheet, int row, int column) throws Exception {
		final Rectangle[] result = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> {
			Rectangle bounds = sheet.getCellRect(row, column, true);
			Point location = sheet.getLocationOnScreen();
			result[0] = new Rectangle(location.x + bounds.x, location.y + bounds.y, bounds.width, bounds.height);
		});
		return result[0];
	}

	private static TaskInformationDialog findTaskInformationDialog() {
		for (Window window : Window.getWindows()) {
			if (window instanceof TaskInformationDialog dialog && dialog.isVisible())
				return dialog;
		}
		return null;
	}

	private record Fixture(SpreadSheet sheet, NormalTask task, int taskRow, int nameColumn) { }

	private static final class TestFrame extends JFrame implements FrameHolder {
		private static final long serialVersionUID = 1L;
		private TestGraphicManager manager;

		TestFrame() {
			super("Task Information GUI acceptance");
			manager = new TestGraphicManager(this);
			manager.getMenuManager();
		}

		@Override public FrameManager getFrameManager() { return null; }
		@Override public GraphicManager getGraphicManager() { return manager; }
		@Override public void setGraphicManager(GraphicManager manager) { this.manager = (TestGraphicManager) manager; }
	}

	private static final class TestGraphicManager extends GraphicManager {
		private DocumentFrame documentFrame;
		TestGraphicManager(TestFrame frame) { super(frame); }
		void setDocumentFrame(DocumentFrame documentFrame) { this.documentFrame = documentFrame; }
		@Override public DocumentFrame getCurrentFrame() { return documentFrame; }
	}
}
