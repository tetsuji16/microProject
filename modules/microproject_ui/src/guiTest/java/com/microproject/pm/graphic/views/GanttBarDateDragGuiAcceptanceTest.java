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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
import com.microproject.exchange.MpoFileImporter;
import com.microproject.pm.graphic.gantt.Gantt;
import com.microproject.pm.graphic.gantt.GanttInteractor;
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

	@Test
	void robotDragBetweenBarsCreatesDependencyAndUndoRestoresModel() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture(DependencyType.FS, false);
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
		Rectangle source = barBounds(fixture, fixture.predecessor);
		Rectangle target = barBounds(fixture, fixture.successor);
		int startX = source.x + source.width / 2;
		int startY = source.y + source.height / 2;
		int endX = target.x + target.width / 2;
		int endY = target.y + target.height / 2;
		assertTrue(fixture.successor.getPredecessorList().isEmpty(), "fixture must begin without a dependency");
		SwingUtilities.invokeAndWait(() -> {
			GraphZone sourceZone = gantt.getUI().getNodeAt(startX - gantt.getLocationOnScreen().x,
				startY - gantt.getLocationOnScreen().y);
			GraphZone targetZone = gantt.getUI().getNodeAt(endX - gantt.getLocationOnScreen().x,
				endY - gantt.getLocationOnScreen().y);
			assertTrue(sourceZone != null && sourceZone.getObject() instanceof GraphicNode sourceNode
				&& sourceNode.getNode().getImpl() == fixture.predecessor, "source point must hit predecessor bar");
			assertTrue(targetZone != null && targetZone.getObject() instanceof GraphicNode targetNode
				&& targetNode.getNode().getImpl() == fixture.successor, "target point must hit successor bar");
		});
		robot.mouseMove(startX, startY);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		for (int step = 1; step <= 10; step++)
			robot.mouseMove(startX + (endX - startX) * step / 10, startY + (endY - startY) * step / 10);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		GuiAcceptanceSupport.await(() -> !fixture.successor.getPredecessorList().isEmpty(),
			"Gantt bar-to-bar drag did not create a dependency");
		GuiAcceptanceSupport.await(() -> fixture.cache.getEdgesSize() == 1,
			"Gantt projection did not publish the newly created dependency edge");
		assertEquals(fixture.predecessor,
			((Dependency) fixture.successor.getPredecessorList().iterator().next()).getPredecessor());
		SwingUtilities.invokeAndWait(() -> fixture.project.getUndoController().undo());
		assertTrue(fixture.successor.getPredecessorList().isEmpty(), "Undo must restore the pre-drag model state");
		SwingUtilities.invokeAndWait(() -> fixture.project.getUndoController().redo());
		assertEquals(1, fixture.successor.getPredecessorList().size(), "Redo must restore the dependency");
		ByteArrayOutputStream saved = new ByteArrayOutputStream();
		MpoFileImporter importer = new MpoFileImporter();
		assertTrue(importer.saveProject(fixture.project, saved), "the Gantt-created dependency must be serializable");
		Project reopened = importer.loadProject(new ByteArrayInputStream(saved.toByteArray()));
		assertEquals(1, reopened.getTaskList().stream().filter(task -> "Drag successor".equals(task.getName()))
			.findFirst().orElseThrow().getPredecessorList().size(), "the Gantt-created dependency must survive MPO reload");
		capture(robot);
	}

	@Test
	void robotResizeProgressAndSplitUseTypedScheduleGatewayWithUndoAndPersistence() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture(DependencyType.FS, false);
		SwingUtilities.invokeAndWait(() -> {
			fixture.predecessor.setPercentComplete(0.4d);
			fixture.project.recalculate();
		});
		showFixture(fixture);
		Robot robot = new Robot();
		robot.setAutoDelay(45);
		activateGantt(robot);
		capture(robot, "gantt-schedule-gestures-initial.png");

		long originalStart = fixture.predecessor.getStart();
		Point startHandle = screenPointForTaskDate(fixture, originalStart);
		assertTaskBarHit(fixture, startHandle);
		Point earlierDate = screenPointForTaskDate(fixture,
			originalStart - com.microproject.options.CalendarOption.getInstance().getMillisPerDay());
		drag(robot, startHandle, earlierDate);
		GuiAcceptanceSupport.await(() -> fixture.predecessor.getStart() < originalStart,
			"dragging the leading bar handle did not resize the task start");
		assertTrue(barBounds(fixture).width > 0, "the resized bar must remain visible after redraw");
		long resizedStart = fixture.predecessor.getStart();
		SwingUtilities.invokeAndWait(() -> fixture.project.getUndoController().undo());
		assertEquals(originalStart, fixture.predecessor.getStart(), "one Undo must restore the original start");
		SwingUtilities.invokeAndWait(() -> fixture.project.getUndoController().redo());
		assertEquals(resizedStart, fixture.predecessor.getStart(), "one Redo must restore the resized start");

		long originalEnd = fixture.predecessor.getEnd();
		Point endHandle = screenPointForTaskDate(fixture, originalEnd);
		assertTaskBarHit(fixture, endHandle);
		Point laterDate = screenPointForTaskDate(fixture,
			originalEnd + com.microproject.options.CalendarOption.getInstance().getMillisPerDay());
		drag(robot, endHandle, laterDate);
		GuiAcceptanceSupport.await(() -> fixture.predecessor.getEnd() > originalEnd,
			"dragging the trailing bar handle did not resize the task finish");
		long resizedEnd = fixture.predecessor.getEnd();
		SwingUtilities.invokeAndWait(() -> fixture.project.getUndoController().undo());
		assertEquals(originalEnd, fixture.predecessor.getEnd(), "one Undo must restore the original finish");
		SwingUtilities.invokeAndWait(() -> fixture.project.getUndoController().redo());
		assertEquals(resizedEnd, fixture.predecessor.getEnd(), "one Redo must restore the resized finish");

		long originalProgress = fixture.predecessor.getCompletedThrough();
		Point progressHandle = screenPointForTaskDate(fixture, originalProgress);
		assertTaskBarHit(fixture, progressHandle);
		Point laterProgress = screenPointForTaskDate(fixture,
			originalProgress + com.microproject.options.CalendarOption.getInstance().getMillisPerDay());
		drag(robot, progressHandle, laterProgress);
		GuiAcceptanceSupport.await(() -> fixture.predecessor.getCompletedThrough() > originalProgress,
			"dragging the progress handle did not update task progress");
		long updatedProgress = fixture.predecessor.getCompletedThrough();
		assertTrue(barBounds(fixture).width > 0, "the updated progress bar must be visible after redraw");
		SwingUtilities.invokeAndWait(() -> fixture.project.getUndoController().undo());
		assertEquals(originalProgress, fixture.predecessor.getCompletedThrough(), "one Undo must restore task progress");
		SwingUtilities.invokeAndWait(() -> fixture.project.getUndoController().redo());
		assertEquals(updatedProgress, fixture.predecessor.getCompletedThrough(), "one Redo must restore updated progress");

		long originalResume = fixture.predecessor.getResume();
		long originalStop = fixture.predecessor.getStop();
		long splitAt = fixture.predecessor.getStart()
			+ com.microproject.options.CalendarOption.getInstance().getMillisPerDay();
		Point splitStart = screenPointForTaskDate(fixture, splitAt);
		assertTaskBarHit(fixture, splitStart);
		Point splitEnd = screenPointForTaskDate(fixture, splitAt + Math.max(1L,
			com.microproject.options.CalendarOption.getInstance().getMillisPerDay() / 8L));
		SwingUtilities.invokeAndWait(() -> ((GanttUI)gantt.getUI()).getInteractor().setSplitMode());
		drag(robot, splitStart, splitEnd);
		GuiAcceptanceSupport.await(() -> fixture.predecessor.getResume() != originalResume
				|| fixture.predecessor.getStop() != originalStop,
			"Gantt split gesture did not add a nonworking interval");
		long splitResume = fixture.predecessor.getResume();
		long splitStop = fixture.predecessor.getStop();
		assertTrue(barBounds(fixture).width > 0, "the split task must remain visible after redraw");
		SwingUtilities.invokeAndWait(() -> fixture.project.getUndoController().undo());
		assertEquals(originalResume, fixture.predecessor.getResume(), "one Undo must restore the original resume date");
		assertEquals(originalStop, fixture.predecessor.getStop(), "one Undo must restore the original stop date");
		SwingUtilities.invokeAndWait(() -> fixture.project.getUndoController().redo());
		assertEquals(splitResume, fixture.predecessor.getResume(), "one Redo must restore the split resume date");
		assertEquals(splitStop, fixture.predecessor.getStop(), "one Redo must restore the split stop date");

		ByteArrayOutputStream saved = new ByteArrayOutputStream();
		MpoFileImporter importer = new MpoFileImporter();
		assertTrue(importer.saveProject(fixture.project, saved), "edited schedule must be serializable");
		Project reopened = importer.loadProject(new ByteArrayInputStream(saved.toByteArray()));
		NormalTask reopenedTask = reopened.getTaskList().stream()
			.filter(task -> "Drag predecessor".equals(task.getName())).map(NormalTask.class::cast).findFirst().orElseThrow();
		assertEquals(resizedStart, reopenedTask.getStart(), "resized start must survive MPO reload");
		assertEquals(resizedEnd, reopenedTask.getEnd(), "resized finish must survive MPO reload");
		assertEquals(updatedProgress, reopenedTask.getCompletedThrough(), "progress must survive MPO reload");
		assertEquals(splitResume, reopenedTask.getResume(), "split resume must survive MPO reload");
		assertEquals(splitStop, reopenedTask.getStop(), "split stop must survive MPO reload");
		capture(robot, "gantt-schedule-gestures-final.png");
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
		return barBounds(fixture, fixture.predecessor);
	}

	private void activateGantt(Robot robot) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			frame.toFront();
			frame.requestFocus();
			gantt.requestFocusInWindow();
		});
		GuiAcceptanceSupport.await(gantt::isShowing, "Gantt was not visible");
		activateWindow(robot);
	}

	private void drag(Robot robot, Point start, Point end) {
		robot.mouseMove(start.x, start.y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		for (int step = 1; step <= 8; step++)
			robot.mouseMove(start.x + (end.x - start.x) * step / 8,
				start.y + (end.y - start.y) * step / 8);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
	}

	private Point screenPointForTaskDate(Fixture fixture, long date) throws Exception {
		Point[] result = new Point[1];
		SwingUtilities.invokeAndWait(() -> {
			var projection = fixture.cache.getVisibleNodes().getProjectionIndex();
			GraphicNode node = null;
			int row = -1;
			for (int candidate = 0; candidate < projection.size(); candidate++) {
				GraphicNode visible = projection.nodeAt(candidate);
				if (visible.getNode().getImpl() == fixture.predecessor) {
					node = visible;
					row = candidate;
					break;
				}
			}
			if (node == null)
				throw new AssertionError("Gantt task is absent from its projection");
			int x = (int)Math.round(gantt.getCoord().toX(date));
			int y = (int)Math.round(((GanttUI)gantt.getUI()).getBarY(row)
				+ node.getGanttShapeOffset() + node.getGanttShapeHeight() / 2.0d);
			Point location = gantt.getLocationOnScreen();
			result[0] = new Point(location.x + x, location.y + y);
		});
		return result[0];
	}

	private void assertTaskBarHit(Fixture fixture, Point screenPoint) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			Point location = gantt.getLocationOnScreen();
			GraphZone hit = gantt.getUI().getNodeAt(screenPoint.x - location.x, screenPoint.y - location.y);
			assertTrue(hit != null && hit.getObject() instanceof GraphicNode hitNode
				&& hitNode.getNode().getImpl() == fixture.predecessor,
				"gesture origin must hit the task's rendered Gantt bar: " + screenPoint);
		});
	}

	private Rectangle barBounds(Fixture fixture, NormalTask task) throws Exception {
		Rectangle[] result = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> {
			int rowHeight = gantt.getRowHeight();
			Point location = gantt.getLocationOnScreen();
			for (int y = 0; y < Math.min(gantt.getHeight(), rowHeight * 10) && result[0] == null; y += 2) {
				for (int x = 0; x < Math.min(gantt.getWidth(), 1200) && result[0] == null; x += 2) {
					GraphZone zone = gantt.getUI().getNodeAt(x, y);
					if (zone != null && zone.getObject() instanceof GraphicNode node
							&& node.getNode().getImpl() == task) {
						int row = gantt.getModel().getCache().getVisibleNodes().getProjectionIndex().rowForNode(node);
						int barY = (int) Math.round(((GanttUI) gantt.getUI()).getBarY(row)
							+ node.getGanttShapeOffset() + node.getGanttShapeHeight() / 2.0d);
						int barCenter = (int) Math.round((gantt.getCoord().toX(task.getStart())
							+ gantt.getCoord().toX(task.getEnd())) / 2.0d);
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
		capture(robot, "gantt-bar-date-drag.png");
	}

	private void capture(Robot robot, String fileName) throws Exception {
		Rectangle[] bounds = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> bounds[0] = new Rectangle(frame.getRootPane().getLocationOnScreen(), frame.getRootPane().getSize()));
		BufferedImage image = robot.createScreenCapture(bounds[0]);
		Path directory = Path.of(System.getProperty("microproject.gui.artifacts.dir", "build/guiTest-artifacts"));
		Files.createDirectories(directory);
		javax.imageio.ImageIO.write(image, "png", directory.resolve(fileName).toFile());
	}

	private Fixture createFixture(int dependencyType) throws Exception {
		return createFixture(dependencyType, true);
	}

	private Fixture createFixture(int dependencyType, boolean includeDependency) throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("gui-gantt-date-drag", undo);
		Project project = Project.createProject(pool, undo);
		project.initialize(false, false);
		NormalTask predecessor = task(project, "Drag predecessor", 3L);
		NormalTask successor = task(project, "Drag successor", 1L);
		long predecessorStart = DateTime.calendarInstance(2026, java.util.Calendar.JUNE, 8).getTimeInMillis();
		predecessor.getCurrentSchedule().setStart(predecessorStart);
		predecessor.setDuration(3L * com.microproject.options.CalendarOption.getInstance().getMillisPerDay());
		Dependency dependency = includeDependency
			? DependencyService.getInstance().newDependency(predecessor, successor, dependencyType, 0L, project) : null;
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
