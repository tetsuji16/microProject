/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.views;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.graphic.configuration.SpreadSheetCategories;
import com.microproject.pm.graphic.gantt.Gantt;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetUtils;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.FlatUiSupport;

/** Verifies that the visible task-table/Gantt pair shares one grid-style path. */
class TaskTableGanttGridGuiAcceptanceTest {
	private JFrame frame;
	private Gantt gantt;

	@AfterEach
	void closeWindow() throws Exception {
		if (frame != null) {
			SwingUtilities.invokeAndWait(() -> {
				frame.dispose();
				frame = null;
			});
		}
		if (gantt != null) {
			gantt.cleanUp();
			gantt = null;
		}
	}

	@Test
	void visibleTaskTableAndGanttRemainLaidOutWhenTheirSharedGridStyleChanges() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture();
		showFixture(fixture);

		Robot robot = new Robot();
		robot.setAutoDelay(40);
		SwingUtilities.invokeAndWait(() -> {
			frame.toFront();
			frame.requestFocus();
		});
		GuiAcceptanceSupport.await(() -> fixture.sheet.isShowing() && fixture.gantt.isShowing(), "task table or Gantt was not visible");
		captureVisibleLayout(robot);

		SwingUtilities.invokeAndWait(() -> {
			assertTrue(fixture.sheet.getWidth() > 200 && fixture.sheet.getHeight() > 150, "task table layout collapsed");
			assertTrue(fixture.gantt.getWidth() > 200 && fixture.gantt.getHeight() > 150, "Gantt layout collapsed");

			TaskGanttSyncSupport.applySpreadsheetGridStyle(fixture.sheet, fixture.gantt, false, FlatUiSupport.tableGridColor());
			assertFalse(fixture.sheet.getShowHorizontalLines());
			assertFalse(fixture.sheet.getShowVerticalLines());
			assertFalse(fixture.sheet.getRowHeader().getShowHorizontalLines());
			assertFalse(fixture.gantt.isGridLinesVisible());

			TaskGanttSyncSupport.applySpreadsheetGridStyle(fixture.sheet, fixture.gantt, true, FlatUiSupport.tableGridColor());
			assertTrue(fixture.sheet.getShowHorizontalLines());
			assertTrue(fixture.sheet.getShowVerticalLines());
			assertTrue(fixture.sheet.getRowHeader().getShowHorizontalLines());
			assertTrue(fixture.gantt.isGridLinesVisible());
		});
	}

	private void captureVisibleLayout(Robot robot) throws Exception {
		Rectangle[] bounds = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> bounds[0] = new Rectangle(frame.getLocationOnScreen(), frame.getSize()));
		BufferedImage screenshot = robot.createScreenCapture(bounds[0]);
		Path directory = Path.of(System.getProperty("micrproject.gui.artifacts.dir", "build/guiTest-artifacts"));
		Files.createDirectories(directory);
		ImageIO.write(screenshot, "png", directory.resolve("task-table-gantt-grid.png").toFile());
		assertTrue(screenshot.getWidth() > 400 && screenshot.getHeight() > 300, "captured layout is unexpectedly small");
	}

	private void showFixture(Fixture fixture) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			frame = new JFrame("Task table and Gantt GUI acceptance");
			JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				new JScrollPane(fixture.sheet), new JScrollPane(fixture.gantt));
			splitPane.setResizeWeight(0.45);
			frame.add(splitPane);
			frame.setPreferredSize(new Dimension(1100, 520));
			frame.pack();
			frame.setLocationByPlatform(true);
			frame.setAlwaysOnTop(true);
			frame.setVisible(true);
		});
	}

	private Fixture createFixture() throws Exception {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("gui-task-gantt-grid-test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		project.createScriptedTask().setName("Visible task");
		Fixture[] fixture = new Fixture[1];
		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet sheet = new SpreadSheet();
			sheet.setSpreadSheetCategory(SpreadSheetCategories.taskSpreadsheetCategory);
			NodeModelCache cache = NodeModelCacheFactory.getInstance().createFilteredCache(
				NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel()), "gui-task-gantt-grid-test", null);
			SpreadSheetUtils.setFieldsAndContext(sheet, cache, SpreadSheetCategories.taskSpreadsheetCategory, "Spreadsheet.Task.entry", true);
			gantt = new Gantt(project, "Gantt");
			gantt.setCache(cache);
			fixture[0] = new Fixture(sheet, gantt);
		});
		return fixture[0];
	}

	private record Fixture(SpreadSheet sheet, Gantt gantt) { }
}
