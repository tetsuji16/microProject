/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.menu;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JComponent;
import javax.swing.InputMap;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetModel;
import com.microproject.pm.graphic.frames.DocumentFrame;
import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.pm.graphic.frames.MainRibbonFrame;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.Environment;
import com.microproject.testsupport.GuiAcceptanceSupport;

/** Verifies document-root shortcuts remain the sole physical route with the classic menu installed. */
class ClassicMenuRootPaneShortcutGuiAcceptanceTest {
	private MainRibbonFrame window;
	private GraphicManager manager;
	private boolean previousRibbonUi;
	private boolean previousNewLook;

	@AfterEach
	void closeWindow() throws Exception {
		if (manager != null) SwingUtilities.invokeAndWait(manager::cleanUp);
		if (window != null) SwingUtilities.invokeAndWait(window::dispose);
		Environment.setRibbonUI(previousRibbonUi);
		Environment.setNewLook(previousNewLook);
	}

	@Test
	void physicalIndentShortcutMutatesOnceWithClassicDocumentMenuAndUndoRedo() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(false);
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("Classic menu shortcut", undo), undo);
		project.initialize(false, false);
		NormalTask parent = project.createScriptedTask();
		parent.setName("Parent");
		NormalTask target = project.createScriptedTask();
		target.setName("Target");
		undo.clear();
		DocumentFrame[] document = new DocumentFrame[1];
		SpreadSheet[] sheet = new SpreadSheet[1];
		SwingUtilities.invokeAndWait(() -> {
			window = new MainRibbonFrame("microProject — classic menu shortcut acceptance", null);
			manager = new GraphicManager(window);
			window.setGraphicManager(manager);
			manager.initView();
			InputMap documentShortcuts = window.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
			MenuManager menuManager = manager.getMenuManager();
			menuManager.setDocumentShortcuts(documentShortcuts);
			JMenuBar classicDocumentMenu = new JMenuBar();
			JMenu projectMenu = new JMenu("Project");
			JMenuItem newProject = menuManager.menuFactory.createJMenuItem("RibbonNewProject");
			assertNull(newProject.getAccelerator(), "classic menu must leave the document-root Ctrl+N binding as its sole shortcut owner");
			projectMenu.add(newProject);
			JMenuItem indent = menuManager.menuFactory.createJMenuItem("RibbonIndent");
			projectMenu.add(indent);
			classicDocumentMenu.add(projectMenu);
			window.setJMenuBar(classicDocumentMenu);
			document[0] = manager.addProjectFrame(project);
			window.setSize(1000, 620);
			window.setLocationByPlatform(true);
			window.setAlwaysOnTop(true);
			window.setVisible(true);
		});
		GuiAcceptanceSupport.await(() -> window.isShowing() && document[0].isShowing(),
			"classic-menu document view did not become visible");
		SwingUtilities.invokeAndWait(() -> sheet[0] = document[0].getActiveSpreadSheet());
		GuiAcceptanceSupport.await(() -> sheet[0] != null && sheet[0].isShowing(),
			"classic-menu task spreadsheet did not become visible");
		JMenuBar[] menuBar = new JMenuBar[1];
		SwingUtilities.invokeAndWait(() -> menuBar[0] = window.getJMenuBar());
		assertNotNull(menuBar[0], "classic document menu bar was not installed");
		assertTrue(menuBar[0].getMenuCount() > 0, "classic document menu bar has no menus");

		int[] targetCell = {-1, -1};
		SwingUtilities.invokeAndWait(() -> {
			SpreadSheetModel model = (SpreadSheetModel) sheet[0].getModel();
			for (int row = 0; row < model.getRowCount(); row++) {
				if (model.getNodeForDisplayRow(row) != null && model.getNodeForDisplayRow(row).getImpl() == target) {
					targetCell[0] = row;
					break;
				}
			}
			for (int column = 0; column < sheet[0].getColumnCount(); column++) {
				if (sheet[0].isNameFieldColumn(column)) targetCell[1] = column;
			}
		});
		assertTrue(targetCell[0] >= 0 && targetCell[1] >= 0, "target Name cell was not present in the document view");
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		Rectangle cell = sheet[0].getCellRect(targetCell[0], targetCell[1], true);
		Point origin = sheet[0].getLocationOnScreen();
		robot.mouseMove(origin.x + cell.x + Math.max(2, cell.width / 2), origin.y + cell.y + Math.max(2, cell.height / 2));
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		GuiAcceptanceSupport.await(sheet[0]::isFocusOwner, "physical task selection did not focus the document spreadsheet");
		assertSame(target, manager.getCurrentFrame().getSelectedImpls(false).get(0), "physical selection did not select the target task");

		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_PERIOD);
		robot.keyRelease(KeyEvent.VK_PERIOD);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		GuiAcceptanceSupport.await(() -> target.getWbsParentTask() == parent,
			"physical Ctrl+. did not apply the document-root Indent shortcut");
		assertTrue(undo.canUndo(), "Indent did not create its single undoable edit");
		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Z);
		GuiAcceptanceSupport.await(() -> target.getWbsParentTask() == null, "one Ctrl+Z did not restore the original hierarchy");
		assertFalse(undo.canUndo(), "one physical shortcut created more than one undoable mutation");
		press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Y);
		GuiAcceptanceSupport.await(() -> target.getWbsParentTask() == parent, "one Ctrl+Y did not restore the Indent mutation");
		assertSame(parent, target.getWbsParentTask());
	}

	private static void press(Robot robot, int... keys) {
		for (int key : keys) robot.keyPress(key);
		for (int index = keys.length - 1; index >= 0; index--) robot.keyRelease(keys[index]);
	}
}
