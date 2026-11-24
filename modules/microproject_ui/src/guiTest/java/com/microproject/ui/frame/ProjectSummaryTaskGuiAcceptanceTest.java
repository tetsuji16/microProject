/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.frame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;
import javax.swing.Action;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.exchange.MpoFileImporter;
import com.microproject.node.ProjectSummaryRow;
import com.microproject.ui.menu.MenuActionConstants;
import com.microproject.ui.view.gantt.Gantt;
import com.microproject.ui.view.graph.GraphZone;
import com.microproject.ui.view.model.cache.GraphicNode;
import com.microproject.ui.view.model.transform.NodeCacheTransformer;
import com.microproject.ui.view.spreadsheet.SpreadSheet;
import com.microproject.ui.view.spreadsheet.SpreadSheetModel;
import com.microproject.model.resource.ResourcePool;
import com.microproject.model.task.NormalTask;
import com.microproject.model.task.Project;
import com.microproject.ui.testsupport.GuiAcceptanceSupport;
import com.microproject.ui.testsupport.GuiRobot;
import com.microproject.undo.DataFactoryUndoController;

/**
 * Physical desktop evidence for the issue #1284 project-summary row.
 * A headless test cannot confirm the visible summary bar hit target or that
 * Robot selection and the spreadsheet's physical F2/Ctrl+Z/Ctrl+Y routes work.
 */
class ProjectSummaryTaskGuiAcceptanceTest {
	private MainRibbonFrame window;
	private GraphicManager manager;
	private GraphicManager previousGraphicManager;

	@AfterEach
	void closeWindow() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			if (manager != null) manager.cleanUp();
			if (window != null) window.dispose();
		});
		assertSame(previousGraphicManager, GraphicManager.getInstance(),
			"cleanup restores the previous process-wide graphic manager");
	}

	@Test
	void visibleProjectSummarySupportsGanttCommandsEditingUndoRedoAndMpo() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("gui-project-summary", undo), undo);
		project.initialize(false, false);
		project.setName("Project Summary GUI Fixture");
		NormalTask first = project.createScriptedTask();
		first.setName("Summary fixture task A");
		first.getCurrentSchedule().setStart(project.getStart());
		first.setDuration(com.microproject.options.CalendarOption.getInstance().getMillisPerDay() * 3);
		NormalTask second = project.createScriptedTask();
		second.setName("Summary fixture task B");
		second.getCurrentSchedule().setStart(first.getEnd());
		second.setDuration(com.microproject.options.CalendarOption.getInstance().getMillisPerDay() * 2);
		project.recalculate();

		AtomicReference<DocumentFrame> documentRef = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> {
			previousGraphicManager = GraphicManager.getInstance();
			window = new MainRibbonFrame("Project Summary Task GUI acceptance", null);
			manager = new GraphicManager(window);
			window.setGraphicManager(manager);
			manager.initView();
			manager.addProjectFrame(project);
			window.setSize(new Dimension(1180, 760));
			window.setVisible(true);
			DocumentFrame document = manager.getFrameForProject(project);
			document.activateGanttView();
			NodeCacheTransformer transformer = (NodeCacheTransformer) document.getGanttView().getCache()
				.getVisibleNodes().getTransformer();
			assertFalse(transformer.isShowProjectSummaryTask(), "the production default remains off");
			transformer.setShowProjectSummaryTask(true); // fixture-only: no user-facing control is introduced
			document.getGanttView().getCache().update();
			documentRef.set(document);
		});

		DocumentFrame document = documentRef.get();
		SpreadSheet sheet = document.getTopSpreadSheet();
		Gantt gantt = document.getGanttView().getGantt();
		GuiRobot robot = new GuiRobot();
		robot.setAutoDelay(45);
		GuiAcceptanceSupport.activateAndAwaitRenderedWindow(window, window.getContentPane(), robot, true,
			"project summary task window");
		GuiAcceptanceSupport.await(() -> sheet.getRowCount() >= 3 && gantt.isShowing(),
			"the task sheet and Gantt did not show the summary row and its tasks");
		SwingUtilities.invokeAndWait(() -> assertTrue(gantt.scrollToDate(project.getStart()),
			"the visible Gantt must be hosted in its horizontal viewport"));
		robot.waitForIdle();

		GraphicNode summary = document.getGanttView().getCache().getVisibleNodes().getElementAt(0);
		assertTrue(summary.getNode() instanceof ProjectSummaryRow);
		assertEquals(project, summary.getNode().getImpl());
		assertEquals(project.getStart(), summary.getStart());
		assertEquals(project.getEnd(), summary.getEnd());
		assertTrue(summary.getGanttShapeHeight() > 0.0d, "the Gantt renderer must create visible summary-bar geometry");
		AtomicReference<GraphZone> hit = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> {
			long interior = project.getStart() + (project.getEnd() - project.getStart()) / 2;
			double x = gantt.getCoord().toX(interior);
			double y = gantt.getRowHeight() / 2.0d;
			assertTrue(gantt.getVisibleRect().contains((int) x, (int) y), "the project bar interior must be onscreen");
			hit.set(gantt.getUI().getNodeAt(x, y));
		});
		assertNotNull(hit.get(), "the onscreen Gantt summary bar must respond to hit testing");
		assertSame(summary, hit.get().getObject());

		int nameColumn = findNameColumn(sheet);
		clickCell(robot, sheet, 0, nameColumn);
		GuiAcceptanceSupport.await(() -> sheet.getSelectedNodes().stream()
			.anyMatch(node -> node instanceof ProjectSummaryRow), "physical click did not select the synthetic project row");
		assertTrue(document.hasProjectSummaryRowSelected());
		assertActionDisabled(manager, MenuActionConstants.ACTION_DELETE);
		assertActionDisabled(manager, MenuActionConstants.ACTION_INDENT);
		assertActionDisabled(manager, MenuActionConstants.ACTION_OUTDENT);
		assertActionDisabled(manager, MenuActionConstants.ACTION_LINK);
		var link = document.routeTaskCommand(com.microproject.ribbon.CommandId.LINK);
		assertEquals(com.microproject.ribbon.RibbonCommandResult.Status.REJECTED, link.status());
		assertEquals("project-summary-row-selected", link.reason());
		assertTrue(second.getPredecessorList().isEmpty(), "rejected link must not mutate task dependencies");

		// F2 is the spreadsheet's physical task-name editing route.
		robot.keyPress(KeyEvent.VK_F2);
		robot.keyRelease(KeyEvent.VK_F2);
		GuiAcceptanceSupport.await(sheet::isEditing, "F2 did not open the selected summary name editor");
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_A);
		robot.keyRelease(KeyEvent.VK_A);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		typeText(robot, "Robot Project Summary");
		robot.keyPress(KeyEvent.VK_ENTER);
		robot.keyRelease(KeyEvent.VK_ENTER);
		GuiAcceptanceSupport.await(() -> "Robot Project Summary".equals(project.getName()),
			"typing into the project summary name cell did not rename the project");
		assertTrue(project.isDirty(), "the project name edit must mark the document dirty");

		pressShortcut(robot, KeyEvent.VK_Z);
		GuiAcceptanceSupport.await(() -> !"Robot Project Summary".equals(project.getName()),
			"Ctrl+Z did not undo the project summary rename");
		pressShortcut(robot, KeyEvent.VK_Y);
		GuiAcceptanceSupport.await(() -> "Robot Project Summary".equals(project.getName()),
			"Ctrl+Y did not redo the project summary rename");
		ByteArrayOutputStream saved = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(project, saved), "MPO save must succeed after the physical rename");
		Project reopened = new MpoFileImporter().loadProject(new ByteArrayInputStream(saved.toByteArray()));
		assertEquals("Robot Project Summary", reopened.getName(), "the renamed project must survive MPO reload");

		Path artifacts = Path.of(System.getProperty("microproject.gui.artifacts.dir", "build/guiTest-artifacts"));
		Files.createDirectories(artifacts);
		Rectangle bounds = new Rectangle(window.getLocationOnScreen(), window.getSize());
		ImageIO.write(robot.createScreenCapture(bounds), "png", artifacts.resolve("project-summary-task.png").toFile());
	}

	private static int findNameColumn(SpreadSheet sheet) {
		SpreadSheetModel model = (SpreadSheetModel) sheet.getModel();
		for (int column = 0; column < model.getColumnCount(); column++) {
			var field = model.getFieldInColumn(column);
			if (field != null && "Field.name".equals(field.getId())) return sheet.convertColumnIndexToView(column);
		}
		throw new IllegalArgumentException("Missing project summary name column");
	}

	private static void assertActionDisabled(GraphicManager manager, String actionId) {
		Action action = manager.getMenuManager().getActionFromId(actionId);
		assertNotNull(action, "missing task command action: " + actionId);
		assertFalse(action.isEnabled(), "command must be disabled with the project summary row selected: " + actionId);
	}

	private static void clickCell(Robot robot, SpreadSheet sheet, int row, int column) throws Exception {
		Rectangle[] cell = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> {
			Rectangle bounds = sheet.getCellRect(row, column, true);
			var location = sheet.getLocationOnScreen();
			cell[0] = new Rectangle(location.x + bounds.x, location.y + bounds.y, bounds.width, bounds.height);
		});
		robot.mouseMove(cell[0].x + cell[0].width / 2, cell[0].y + cell[0].height / 2);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
	}

	private static void pressShortcut(Robot robot, int key) throws Exception {
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(key);
		robot.keyRelease(key);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		robot.waitForIdle();
	}

	private static void typeText(Robot robot, String text) {
		for (char character : text.toCharArray()) {
			if (character == ' ') {
				robot.keyPress(KeyEvent.VK_SPACE);
				robot.keyRelease(KeyEvent.VK_SPACE);
			} else {
				boolean shifted = Character.isUpperCase(character);
				if (shifted) robot.keyPress(KeyEvent.VK_SHIFT);
				int key = KeyEvent.getExtendedKeyCodeForChar(character);
				if (key == KeyEvent.VK_UNDEFINED)
					throw new IllegalArgumentException("Cannot type character: " + character);
				robot.keyPress(key);
				robot.keyRelease(key);
				if (shifted) robot.keyRelease(KeyEvent.VK_SHIFT);
			}
		}
	}
}
