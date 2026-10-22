/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetModel;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.testsupport.WindowAcceptanceAssertions;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.Environment;

/** GUI-MSP-WINDOW-07: document-root shortcuts mutate only the active project. */
class TwoDocumentShortcutIsolationGuiAcceptanceTest {
	private MainRibbonFrame window;
	private GraphicManager manager;
	private boolean previousRibbonUi;
	private boolean previousNewLook;

	@AfterEach
	void closeWindows() throws Exception {
		if (manager != null) SwingUtilities.invokeAndWait(manager::cleanUp);
		for (java.awt.Window candidate : java.awt.Window.getWindows()) {
			if (candidate instanceof MainRibbonFrame frame && frame != window)
				SwingUtilities.invokeAndWait(frame::dispose);
		}
		if (window != null) SwingUtilities.invokeAndWait(window::dispose);
		Environment.setRibbonUI(previousRibbonUi);
		Environment.setNewLook(previousNewLook);
	}

	@Test
	void physicalCtrlDeleteAndUndoStayWithEachActiveDocument() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		Project[] projects = { project("Shortcut Alpha"), project("Shortcut Beta") };
		NormalTask[] tasks = { task(projects[0], "Alpha task"), task(projects[1], "Beta task") };
		DocumentFrame[] documents = new DocumentFrame[2];
		SpreadSheet[] sheets = new SpreadSheet[2];
		int[][] cells = new int[2][2];
		SwingUtilities.invokeAndWait(() -> {
			window = new MainRibbonFrame("Two-document shortcut isolation", null);
			manager = new GraphicManager(window);
			window.setGraphicManager(manager);
			manager.initView();
			documents[0] = manager.addProjectFrame(projects[0]);
			window.setSize(1000, 650);
			window.setLocation(40, 40);
			window.setVisible(true);
			documents[1] = manager.addProjectFrame(projects[1]);
		});
		GuiAcceptanceSupport.await(() -> window.isShowing() && documents[0].isShowing()
				&& documents[1].isShowing(), "both project documents did not become visible");
		MainRibbonFrame[] secondary = java.util.Arrays.stream(java.awt.Window.getWindows())
			.filter(candidate -> candidate instanceof MainRibbonFrame && candidate != window && candidate.isShowing())
			.map(MainRibbonFrame.class::cast).toArray(MainRibbonFrame[]::new);
		assertEquals(1, secondary.length, "expected exactly one independent secondary project window");
		SwingUtilities.invokeAndWait(() -> {
			// Keep both physical targets in the same usable screen area. The active
			// document is raised before each real Robot click so the other window
			// cannot cover its task row on narrow desktops.
			secondary[0].setBounds(window.getBounds());
			for (int i = 0; i < 2; i++) {
				sheets[i] = documents[i].getActiveSpreadSheet();
				cells[i][0] = findTaskRow(sheets[i], tasks[i]);
				cells[i][1] = findNameColumn(sheets[i]);
				assertTrue(cells[i][0] >= 0 && cells[i][1] >= 0, "task Name cell must be visible in document " + i);
			}
		});

		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		selectAndClear(robot, 1, documents, sheets, cells);
		GuiAcceptanceSupport.await(() -> "".equals(tasks[1].getName()), "active secondary shortcut did not clear Beta");
		assertEquals("Alpha task", tasks[0].getName(), "secondary shortcut changed the inactive primary model");
		assertSame(documents[1], manager.getCurrentFrame(), "secondary window must own the active document");
		assertSelectedTask(sheets[1], cells[1][0], tasks[1]);
		robot.keyPress(KeyEvent.VK_CONTROL); robot.keyPress(KeyEvent.VK_Z);
		robot.keyRelease(KeyEvent.VK_Z); robot.keyRelease(KeyEvent.VK_CONTROL);
		GuiAcceptanceSupport.await(() -> "Beta task".equals(tasks[1].getName()), "secondary Ctrl+Z did not undo Beta clear");
		assertEquals("Alpha task", tasks[0].getName(), "secondary Ctrl+Z changed the inactive primary model");
		robot.keyPress(KeyEvent.VK_CONTROL); robot.keyPress(KeyEvent.VK_Y);
		robot.keyRelease(KeyEvent.VK_Y); robot.keyRelease(KeyEvent.VK_CONTROL);
		GuiAcceptanceSupport.await(() -> "".equals(tasks[1].getName()), "secondary Ctrl+Y did not redo Beta clear");

		selectAndClear(robot, 0, documents, sheets, cells);
		GuiAcceptanceSupport.await(() -> "".equals(tasks[0].getName()), "active primary shortcut did not clear Alpha");
		assertEquals("", tasks[1].getName(), "primary shortcut unexpectedly changed the inactive secondary model");
		assertSame(documents[0], manager.getCurrentFrame(), "primary window must own the active document after switching back");
		assertSelectedTask(sheets[0], cells[0][0], tasks[0]);
		assertSelectedTask(sheets[1], cells[1][0], tasks[1]);
		robot.keyPress(KeyEvent.VK_CONTROL); robot.keyPress(KeyEvent.VK_Z);
		robot.keyRelease(KeyEvent.VK_Z); robot.keyRelease(KeyEvent.VK_CONTROL);
		GuiAcceptanceSupport.await(() -> "Alpha task".equals(tasks[0].getName()), "primary Ctrl+Z did not undo Alpha clear");
		assertEquals("", tasks[1].getName(), "primary Ctrl+Z changed the inactive secondary model");
		robot.keyPress(KeyEvent.VK_CONTROL); robot.keyPress(KeyEvent.VK_Y);
		robot.keyRelease(KeyEvent.VK_Y); robot.keyRelease(KeyEvent.VK_CONTROL);
		GuiAcceptanceSupport.await(() -> "".equals(tasks[0].getName()), "primary Ctrl+Y did not redo Alpha clear");
		assertSelectedTask(sheets[0], cells[0][0], tasks[0]);
		assertSelectedTask(sheets[1], cells[1][0], tasks[1]);
	}

	private void selectAndClear(Robot robot, int index, DocumentFrame[] documents, SpreadSheet[] sheets, int[][] cells) throws Exception {
		Rectangle[] cell = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> {
			manager.activateDocumentWindow(documents[index]);
			java.awt.Window owner = SwingUtilities.getWindowAncestor(documents[index]);
			owner.setVisible(true);
			owner.toFront();
			owner.requestFocus();
			cell[0] = sheets[index].getCellRect(cells[index][0], cells[index][1], true);
		});
		java.awt.Window owner = SwingUtilities.getWindowAncestor(documents[index]);
		GuiAcceptanceSupport.await(() -> owner.isActive() && manager.getCurrentFrame() == documents[index],
			"requested document window did not become active before its physical task click");
		WindowAcceptanceAssertions.assertWithinUsableWorkArea((javax.swing.JFrame) owner,
			"document window " + documents[index].getTitle());
		Point origin = sheets[index].getLocationOnScreen();
		Point target = new Point(origin.x + cell[0].x + Math.max(2, cell[0].width / 2),
			origin.y + cell[0].y + Math.max(2, cell[0].height / 2));
		Rectangle ownerBounds = new Rectangle(owner.getLocationOnScreen(), owner.getSize());
		assertTrue(ownerBounds.contains(target), "physical task hit point must be inside its document window: point=" + target
			+ ", window=" + ownerBounds + ", document=" + documents[index].getTitle());
		robot.mouseMove(target.x, target.y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame() == documents[index], "physical task selection did not activate document " + index);
		GuiAcceptanceSupport.await(() -> sheets[index].getSelectedRow() == cells[index][0], "physical click did not select the intended task in document " + index);
		SwingUtilities.invokeAndWait(() -> {
			sheets[index].changeSelection(cells[index][0], cells[index][1], false, false);
			sheets[index].requestFocusInWindow();
		});
		robot.keyPress(KeyEvent.VK_CONTROL); robot.keyPress(KeyEvent.VK_DELETE);
		robot.keyRelease(KeyEvent.VK_DELETE); robot.keyRelease(KeyEvent.VK_CONTROL);
	}

	private static void assertSelectedTask(SpreadSheet sheet, int row, NormalTask expected) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			assertEquals(row, sheet.getSelectedRow(), "document must retain its own selected task row");
			assertSame(expected, ((SpreadSheetModel) sheet.getModel()).getNodeForDisplayRow(sheet.getSelectedRow()).getImpl(),
				"selected row must still resolve to its own task");
		});
	}

	private static int findTaskRow(SpreadSheet sheet, NormalTask task) {
		SpreadSheetModel model = (SpreadSheetModel) sheet.getModel();
		for (int row = 0; row < model.getRowCount(); row++)
			if (model.getNodeForDisplayRow(row) != null && model.getNodeForDisplayRow(row).getImpl() == task) return row;
		return -1;
	}

	private static int findNameColumn(SpreadSheet sheet) {
		for (int column = 0; column < sheet.getColumnCount(); column++) if (sheet.isNameFieldColumn(column)) return column;
		return -1;
	}

	private static Project project(String name) {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool(name, undo), undo);
		project.initialize(false, false);
		project.setName(name);
		project.setFileName(name.replace(' ', '-') + ".mpo");
		return project;
	}

	private static NormalTask task(Project project, String name) {
		NormalTask task = project.createScriptedTask();
		task.setName(name);
		return task;
	}
}
