/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.graphic.configuration.SpreadSheetCategories;
import com.microproject.configuration.Dictionary;
import com.microproject.graphic.configuration.BarStyles;
import com.microproject.pm.graphic.gantt.Gantt;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetUtils;
import com.microproject.pm.graphic.timescale.CoordinatesConverter;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.options.CalendarOption;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.NormalTask;
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
		robot.delay(500);
		assertTrue(hasRenderedGanttNode(fixture.gantt), "Gantt must render at least one task node");
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

	private static boolean hasRenderedGanttNode(Gantt chart) throws Exception {
		boolean[] rendered = new boolean[1];
		SwingUtilities.invokeAndWait(() -> {
			for (int y = 0; y < Math.min(chart.getHeight(), chart.getRowHeight() * 20) && !rendered[0]; y += 2) {
				for (int x = 0; x < Math.min(chart.getWidth(), 1200) && !rendered[0]; x += 2) {
					rendered[0] = chart.getUI().getNodeAt(x, y) != null;
				}
			}
		});
		return rendered[0];
	}

	@Test
	void twentyMixedTasksRemainAccessibleAfterMouseScrollbarClick() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture(20);
		showFixture(fixture);

		Robot robot = new Robot();
		robot.setAutoDelay(40);
		SwingUtilities.invokeAndWait(() -> {
			frame.toFront();
			frame.requestFocus();
			fixture.sheet.requestFocusInWindow();
		});
		GuiAcceptanceSupport.await(() -> fixture.sheet.isShowing() && fixture.gantt.isShowing(), "20-task table or Gantt was not visible");
		int initialRows = ((com.microproject.pm.graphic.spreadsheet.SpreadSheetModel) fixture.sheet.getModel()).getRowCount();
		assertTrue(initialRows >= 20, "all 20 task rows must be present before scrolling");
		assertEquals(20, fixture.taskCount, "fixture must contain exactly 20 tasks");
		assertEquals(9, fixture.sequentialDependencyCount, "first ten tasks must form one FS chain");
		assertEquals(10, fixture.independentTaskCount, "last ten tasks must remain independent");
		JScrollPane tableScroll = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, fixture.sheet);
		assertTrue(tableScroll != null, "20-task table must be hosted by a scroll pane");
		assertTrue(tableScroll.getVerticalScrollBar().getMaximum() > tableScroll.getVerticalScrollBar().getVisibleAmount(),
			"20-task table must have a scrollable vertical range");
		Rectangle scrollBounds = new Rectangle();
		SwingUtilities.invokeAndWait(() -> {
			Rectangle bounds = tableScroll.getVerticalScrollBar().getBounds();
			java.awt.Point location = tableScroll.getVerticalScrollBar().getLocationOnScreen();
			scrollBounds.setBounds(location.x, location.y, bounds.width, bounds.height);
		});
		robot.mouseMove(scrollBounds.x + scrollBounds.width / 2, scrollBounds.y + scrollBounds.height - 8);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.delay(300);
		GuiAcceptanceSupport.await(() -> tableScroll.getVerticalScrollBar().getValue() > 0,
			"mouse wheel must scroll the 20-task table");
		assertTrue(tableScroll.getVerticalScrollBar().getValue() > 0, "table must remain scrollable with 20 tasks");
	}

	private void captureVisibleLayout(Robot robot) throws Exception {
		Rectangle[] bounds = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> bounds[0] = new Rectangle(frame.getRootPane().getLocationOnScreen(), frame.getRootPane().getSize()));
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
			gantt.updateSize();
		});
	}

	private Fixture createFixture() throws Exception {
		return createFixture(1);
	}

	private Fixture createFixture(int taskCount) throws Exception {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("gui-task-gantt-grid-test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		List<NormalTask> tasks = new ArrayList<>();
		for (int index = 1; index <= taskCount; index++) {
			NormalTask task = project.createScriptedTask();
			task.setName(index <= 10 ? "Sequential " + index : "Independent " + index);
			task.getCurrentSchedule().setStart(project.getStart());
			task.setDuration(CalendarOption.getInstance().getMillisPerDay());
			tasks.add(task);
			if (index > 1 && index <= 10) {
				DependencyService.getInstance().newDependency(tasks.get(index - 2), task, DependencyType.FS, 0L, project);
			}
		}
		project.recalculate();
		int sequentialDependencyCount = (int) tasks.subList(1, Math.min(10, tasks.size())).stream()
			.filter(task -> task.getPredecessorList().size() == 1).count();
		int independentTaskCount = (int) tasks.subList(Math.min(10, tasks.size()), tasks.size()).stream()
			.filter(task -> task.getPredecessorList().isEmpty()).count();
		Fixture[] fixture = new Fixture[1];
		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet sheet = new SpreadSheet();
			sheet.setSpreadSheetCategory(SpreadSheetCategories.taskSpreadsheetCategory);
			NodeModelCache cache = NodeModelCacheFactory.getInstance().createFilteredCache(
				NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel()), "gui-task-gantt-grid-test", null);
			SpreadSheetUtils.setFieldsAndContext(sheet, cache, SpreadSheetCategories.taskSpreadsheetCategory, "Spreadsheet.Task.entry", true);
			gantt = new Gantt(project, "Gantt");
			gantt.setCache(cache);
			gantt.setCoord(new CoordinatesConverter(project));
			gantt.setBarStyles((BarStyles) Dictionary.get(BarStyles.category, "standard"));
			gantt.updateSize();
			fixture[0] = new Fixture(sheet, gantt, tasks.size(), sequentialDependencyCount, independentTaskCount);
		});
		return fixture[0];
	}

	private record Fixture(SpreadSheet sheet, Gantt gantt, int taskCount, int sequentialDependencyCount,
		int independentTaskCount) { }
}
