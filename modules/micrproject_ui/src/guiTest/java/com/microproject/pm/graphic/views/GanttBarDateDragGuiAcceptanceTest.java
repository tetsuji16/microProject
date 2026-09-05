/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.views;

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

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.graphic.configuration.SpreadSheetCategories;
import com.microproject.configuration.Dictionary;
import com.microproject.graphic.configuration.BarStyles;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.graphic.gantt.Gantt;
import com.microproject.pm.graphic.gantt.GanttUI;
import com.microproject.pm.graphic.graph.GraphZone;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetUtils;
import com.microproject.pm.graphic.timescale.CoordinatesConverter;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.DateTime;

/** Robot coverage for moving a task bar and propagating an FS successor date. */
class GanttBarDateDragGuiAcceptanceTest {
	private JFrame frame;
	private Gantt gantt;

	@AfterEach
	void closeWindow() throws Exception {
		if (frame != null) SwingUtilities.invokeAndWait(() -> {
			frame.dispose();
			frame = null;
		});
		if (gantt != null) {
			gantt.cleanUp();
			gantt = null;
		}
	}

	@Test
	void robotDragMovesBarAndRecalculatesFsSuccessor() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture(DependencyType.FS);
		dragBarAndAssertSuccessor(fixture);
	}

	@Test
	void robotDragMovesBarAndRecalculatesFfSuccessor() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture(DependencyType.FF);
		dragBarAndAssertSuccessor(fixture);
	}

	@Test
	void robotDragMovesBarAndRecalculatesSsSuccessor() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		dragBarAndAssertSuccessor(createFixture(DependencyType.SS));
	}

	@Test
	void robotDragMovesBarAndRecalculatesSfSuccessor() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		dragBarAndAssertSuccessor(createFixture(DependencyType.SF));
	}

	private void dragBarAndAssertSuccessor(Fixture fixture) throws Exception {
		long oldStart = fixture.predecessor.getStart();
		showFixture(fixture);
		Robot robot = new Robot();
		robot.setAutoDelay(45);
		SwingUtilities.invokeAndWait(() -> {
			frame.toFront();
			frame.requestFocus();
			gantt.requestFocusInWindow();
		});
		GuiAcceptanceSupport.await(gantt::isShowing, "Gantt was not visible");
		activateWindow(robot);
		Rectangle bar = barBounds(fixture);
		capture(robot);
		SwingUtilities.invokeAndWait(() -> assertTrue(gantt.getUI().getNodeAt(
			bar.x - gantt.getLocationOnScreen().x + bar.width / 2,
			bar.y - gantt.getLocationOnScreen().y + bar.height / 2) != null,
			"computed drag point must hit the visible Gantt bar: " + bar + " gantt=" + gantt.getBounds()));
		long twoDays = 2L * com.microproject.options.CalendarOption.getInstance().getMillisPerDay();
		int delta = (int) Math.round(gantt.getCoord().toW(twoDays));
		assertTrue(delta > 0, "Gantt scale must produce a positive drag distance");
		robot.mouseMove(bar.x + Math.max(2, bar.width / 2), bar.y + bar.height / 2);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		int targetX = bar.x + Math.max(2, bar.width / 2) + delta;
		int targetY = bar.y + bar.height / 2;
		int startX = bar.x + Math.max(2, bar.width / 2);
		for (int step = 1; step <= 8; step++)
			robot.mouseMove(startX + (targetX - startX) * step / 8, targetY);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		GuiAcceptanceSupport.await(() -> fixture.predecessor.getStart() > oldStart,
			"Gantt bar drag did not move the predecessor: oldStart=" + oldStart
				+ " actual=" + fixture.predecessor.getStart() + " delta=" + delta
				+ " bar=" + bar + " gantt=" + gantt.getBounds());
		SwingUtilities.invokeAndWait(fixture.project::recalculate);
		if (fixture.dependency.getDependencyType() == DependencyType.FF) {
			assertEquals(fixture.predecessor.getEnd(), fixture.successor.getEnd(),
				"FF successor finish must match the dragged predecessor finish: pred="
					+ fixture.predecessor.getStart() + ".." + fixture.predecessor.getEnd()
					+ " successor=" + fixture.successor.getStart() + ".." + fixture.successor.getEnd()
					+ " early=" + fixture.successor.getEarlyStart() + ".." + fixture.successor.getEarlyFinish());
		} else if (fixture.dependency.getDependencyType() == DependencyType.SF) {
			assertTrue(fixture.successor.getEnd() <= fixture.predecessor.getStart(),
				"SF successor finish must not be later than the dragged predecessor start");
		} else {
			long expected = fixture.dependency.calcForwardDependencyDate(fixture.predecessor.getStart(), fixture.predecessor.getEnd(), true);
			assertEquals(expected, fixture.successor.getStart(), "Successor must match " + DependencyType.toLongString(fixture.dependency.getDependencyType())
				+ " date implied by the dragged bar (expected=" + expected + ", actual=" + fixture.successor.getStart() + ")");
		}
		capture(robot);
	}

	private void activateWindow(Robot robot) throws Exception {
		Rectangle bounds = new Rectangle(frame.getLocationOnScreen(), frame.getSize());
		robot.mouseMove(bounds.x + Math.min(40, bounds.width / 2), bounds.y + 12);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
	}

	private void showFixture(Fixture fixture) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			frame = new JFrame("Gantt bar date drag GUI acceptance");
			frame.add(new JScrollPane(gantt));
			frame.setPreferredSize(new Dimension(1100, 520));
			frame.pack();
			frame.setLocationByPlatform(true);
			frame.setAlwaysOnTop(true);
			frame.setVisible(true);
		});
	}

	private Rectangle barBounds(Fixture fixture) throws Exception {
		Rectangle[] result = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> {
			int rowHeight = gantt.getRowHeight();
			Point location = gantt.getLocationOnScreen();
			for (int y = 0; y < Math.min(gantt.getHeight(), rowHeight * 10) && result[0] == null; y += 2) {
				for (int x = 0; x < Math.min(gantt.getWidth(), 1200) && result[0] == null; x += 2) {
					GraphZone zone = gantt.getUI().getNodeAt(x, y);
					if (zone != null && zone.getObject() instanceof GraphicNode node
							&& node.getNode().getImpl() == fixture.predecessor) {
						int barY = (int) Math.round(((GanttUI) gantt.getUI()).getBarY(node.getRow())
							+ node.getGanttShapeOffset() + node.getGanttShapeHeight() / 2.0d);
						int barCenter = (int) Math.round((gantt.getCoord().toX(fixture.predecessor.getStart())
							+ gantt.getCoord().toX(fixture.predecessor.getEnd())) / 2.0d);
						result[0] = new Rectangle(location.x + barCenter - 4,
							location.y + barY - 4, 8, 8);
					}
				}
			}
			if (result[0] == null)
				throw new AssertionError("no interactive Gantt bar was rendered; cacheSize=" + fixture.cache.getSize()
					+ " gantt=" + gantt.getSize());
		});
		return result[0];
	}

	private void capture(Robot robot) throws Exception {
		Rectangle[] bounds = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> bounds[0] = new Rectangle(frame.getRootPane().getLocationOnScreen(), frame.getRootPane().getSize()));
		BufferedImage image = robot.createScreenCapture(bounds[0]);
		Path directory = Path.of(System.getProperty("micrproject.gui.artifacts.dir", "build/guiTest-artifacts"));
		Files.createDirectories(directory);
		javax.imageio.ImageIO.write(image, "png", directory.resolve("gantt-bar-date-drag.png").toFile());
	}

	private Fixture createFixture(int dependencyType) throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("gui-gantt-date-drag", undo);
		Project project = Project.createProject(pool, undo);
		project.initialize(false, false);
		NormalTask predecessor = task(project, "Drag predecessor", 3L);
		NormalTask successor = task(project, "Drag successor", 1L);
		long predecessorStart = DateTime.calendarInstance(2026, java.util.Calendar.JUNE, 8).getTimeInMillis();
		predecessor.getCurrentSchedule().setStart(predecessorStart);
		predecessor.setDuration(3L * com.microproject.options.CalendarOption.getInstance().getMillisPerDay());
		Dependency dependency = DependencyService.getInstance().newDependency(predecessor, successor, dependencyType, 0L, project);
		project.recalculate();
		final Fixture[] result = new Fixture[1];
		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet sheet = new SpreadSheet();
			sheet.setSpreadSheetCategory(SpreadSheetCategories.taskSpreadsheetCategory);
			NodeModelCache cache = NodeModelCacheFactory.getInstance().createFilteredCache(
				NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel()), "gui-gantt-date-drag", null);
			SpreadSheetUtils.setFieldsAndContext(sheet, cache, SpreadSheetCategories.taskSpreadsheetCategory, "Spreadsheet.Task.entry", true);
			gantt = new Gantt(project, "Gantt");
			gantt.setCache(cache);
			gantt.setCoord(new CoordinatesConverter(project));
			gantt.setBarStyles((BarStyles) Dictionary.get(BarStyles.category, "standard"));
			gantt.updateSize();
			result[0] = new Fixture(project, predecessor, successor, dependency, cache);
		});
		return result[0];
	}

	private static NormalTask task(Project project, String name, long days) {
		NormalTask task = project.createScriptedTask();
		task.setName(name);
		task.getCurrentSchedule().setStart(project.getStart());
		task.setDuration(days * com.microproject.options.CalendarOption.getInstance().getMillisPerDay());
		return task;
	}

	private record Fixture(Project project, NormalTask predecessor, NormalTask successor, Dependency dependency, NodeModelCache cache) { }
}
