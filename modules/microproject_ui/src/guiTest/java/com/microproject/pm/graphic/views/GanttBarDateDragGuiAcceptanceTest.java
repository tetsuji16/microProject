/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Cursor;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JMenuItem;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.graphic.configuration.SpreadSheetCategories;
import com.microproject.configuration.Dictionary;
import com.microproject.graphic.configuration.BarStyles;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.AssignmentService;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.exchange.MpoFileImporter;
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
import com.microproject.strings.Messages;
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
		Fixture fixture = createFixture(DependencyType.FS, false, true);
		showFixture(fixture);
		Robot robot = new Robot();
		robot.setAutoDelay(45);
		activateGantt(robot);
		capture(robot, "gantt-schedule-gestures-initial.png");

		long originalStart = fixture.predecessor.getStart();
		int originalConstraintType = fixture.predecessor.getConstraintType();
		long originalConstraintDate = fixture.predecessor.getConstraintDate();
		Point startHandle = findResizeHandle(robot, fixture, true, Cursor.W_RESIZE_CURSOR, "start resize");
		assertTaskBarHit(fixture, startHandle);
		assertResizeCursor(robot, startHandle, Cursor.W_RESIZE_CURSOR, "start resize");
		java.util.Calendar earlierStartCalendar = java.util.Calendar.getInstance();
		earlierStartCalendar.setTimeInMillis(originalStart);
		earlierStartCalendar.add(java.util.Calendar.DAY_OF_MONTH, -3);
		long earlierRequestedStart = earlierStartCalendar.getTimeInMillis();
		Point earlierDate = screenPointForTaskDate(fixture, earlierRequestedStart);
		long normalizedEarlierStart = fixture.predecessor.getEffectiveWorkCalendar()
			.adjustInsideCalendar(earlierRequestedStart, false);
		assertTrue(normalizedEarlierStart < originalStart,
			"the resize destination must normalize to a working time before the original start");
		drag(robot, startHandle, earlierDate);
		try {
			GuiAcceptanceSupport.await(() -> fixture.predecessor.getStart() < originalStart,
				"dragging the leading bar handle did not resize the task start");
		} catch (AssertionError failure) {
			throw new AssertionError("Gantt start resize did not commit: expectedBefore=" + originalStart
				+ " actualAfter=" + fixture.predecessor.getStart() + " pressPoint=" + startHandle
				+ " dragPoint=" + earlierDate, failure);
		}
		assertTrue(barBounds(fixture).width > 0, "the resized bar must remain visible after redraw");
		long resizedStart = fixture.predecessor.getStart();
		SwingUtilities.invokeAndWait(() -> fixture.project.getUndoController().undo());
		assertEquals(originalStart, fixture.predecessor.getStart(), "one Undo must restore the original start");
		assertEquals(originalConstraintType, fixture.predecessor.getConstraintType(),
			"one Undo must restore the original start constraint");
		assertEquals(originalConstraintDate, fixture.predecessor.getConstraintDate(),
			"one Undo must restore the original constraint date");
		SwingUtilities.invokeAndWait(() -> fixture.project.getUndoController().redo());
		assertEquals(resizedStart, fixture.predecessor.getStart(), "one Redo must restore the resized start");
		assertEquals(com.microproject.pm.scheduling.ConstraintType.Kind.SNET, fixture.predecessor.getConstraintTypeKind(),
			"start resize must retain its start constraint after Redo");
		assertEquals(resizedStart, fixture.predecessor.getConstraintDate(),
			"start resize must retain its constraint date after Redo");

		long originalEnd = fixture.predecessor.getEnd();
		int startConstraintType = fixture.predecessor.getConstraintType();
		long startConstraintDate = fixture.predecessor.getConstraintDate();
		Point endHandle = findResizeHandle(robot, fixture, false, Cursor.E_RESIZE_CURSOR, "finish resize");
		assertTaskBarHit(fixture, endHandle);
		assertResizeCursor(robot, endHandle, Cursor.E_RESIZE_CURSOR, "finish resize");
		Point laterDate = screenPointForTaskDate(fixture,
			originalEnd + com.microproject.options.CalendarOption.getInstance().getMillisPerDay());
		drag(robot, endHandle, laterDate);
		GuiAcceptanceSupport.await(() -> fixture.predecessor.getEnd() > originalEnd,
			"dragging the trailing bar handle did not resize the task finish");
		long resizedEnd = fixture.predecessor.getEnd();
		assertEquals(resizedStart, fixture.predecessor.getStart(),
			"finish resize must retain the task start");
		assertEquals(startConstraintType, fixture.predecessor.getConstraintType(),
			"finish resize must preserve the existing start constraint");
		assertEquals(startConstraintDate, fixture.predecessor.getConstraintDate(),
			"finish resize must preserve the existing constraint date");
		SwingUtilities.invokeAndWait(() -> fixture.project.getUndoController().undo());
		assertEquals(originalEnd, fixture.predecessor.getEnd(), "one Undo must restore the original finish");
		assertEquals(startConstraintType, fixture.predecessor.getConstraintType(),
			"one Undo must preserve the start constraint after finish resize");
		SwingUtilities.invokeAndWait(() -> fixture.project.getUndoController().redo());
		assertEquals(resizedEnd, fixture.predecessor.getEnd(), "one Redo must restore the resized finish");
		assertEquals(startConstraintType, fixture.predecessor.getConstraintType(),
			"one Redo must preserve the start constraint after finish resize");

		SwingUtilities.invokeAndWait(() -> {
			fixture.predecessor.setPercentComplete(0.4d);
			fixture.project.recalculate();
		});
		long currentProgress = fixture.predecessor.getCompletedThrough();
		List<String> originalIntervals = assignmentIntervals(fixture.predecessor);
		List<String> originalContours = assignmentContours(fixture.predecessor);
		long splitAt = currentProgress + (fixture.predecessor.getEnd() - currentProgress) / 4L;
		assertTrue(splitAt > fixture.predecessor.getResume() && splitAt < fixture.predecessor.getEnd(),
			"split point must be inside remaining work: resume=" + fixture.predecessor.getResume()
				+ " completedThrough=" + currentProgress
				+ " splitAt=" + splitAt + " end=" + fixture.predecessor.getEnd());
		Point splitStart = screenPointForTaskDate(fixture, splitAt);
		assertTaskBarHit(fixture, splitStart);
		openPopupAndChoose(robot, splitStart, Messages.getString("Gantt.Popup.splitMode"));
		click(robot, splitStart);
		try {
			GuiAcceptanceSupport.await(() -> !assignmentIntervals(fixture.predecessor).equals(originalIntervals),
				"Gantt split gesture did not add a nonworking interval to the resource assignment");
		} catch (AssertionError failure) {
			throw new AssertionError(failure.getMessage() + ": intervals before=" + originalIntervals
				+ " after=" + assignmentIntervals(fixture.predecessor)
				+ " contours before=" + originalContours
				+ " after=" + assignmentContours(fixture.predecessor), failure);
		}
		List<String> splitIntervals = assignmentIntervals(fixture.predecessor);
		assertTrue(splitIntervals.size() > originalIntervals.size(),
			"a split must add a distinct scheduled interval: before=" + originalIntervals + " after=" + splitIntervals);
		assertTrue(barBounds(fixture).width > 0, "the split task must remain visible after redraw");
		SwingUtilities.invokeAndWait(() -> fixture.project.getUndoController().undo());
		assertEquals(originalIntervals, assignmentIntervals(fixture.predecessor),
			"one Undo must restore the original assignment work intervals");
		SwingUtilities.invokeAndWait(() -> fixture.project.getUndoController().redo());
		assertEquals(splitIntervals, assignmentIntervals(fixture.predecessor),
			"one Redo must restore the split assignment work intervals");

		long originalProgress = fixture.predecessor.getCompletedThrough();
		Point progressHandle = screenPointForTaskDate(fixture, originalProgress);
		assertProgressBarHit(fixture, progressHandle);
		long progressTarget = originalProgress + (fixture.predecessor.getEnd() - originalProgress) * 3L / 4L;
		assertTrue(progressTarget > originalProgress && progressTarget < fixture.predecessor.getEnd(),
			"progress target must remain inside the scheduled interval: progress=" + originalProgress
				+ " end=" + fixture.predecessor.getEnd() + " target=" + progressTarget);
		Point laterProgress = screenPointForTaskDate(fixture, progressTarget);
		drag(robot, progressHandle, laterProgress);
		GuiAcceptanceSupport.await(() -> fixture.predecessor.getCompletedThrough() > originalProgress,
			"dragging the progress handle did not update task progress: before=" + originalProgress
				+ " requested=" + progressTarget + " actual=" + fixture.predecessor.getCompletedThrough()
				+ " end=" + fixture.predecessor.getEnd() + " origin=" + progressHandle + " target=" + laterProgress);
		long updatedProgress = fixture.predecessor.getCompletedThrough();
		assertTrue(barBounds(fixture).width > 0, "the updated progress bar must be visible after redraw");
		SwingUtilities.invokeAndWait(() -> fixture.project.getUndoController().undo());
		assertEquals(originalProgress, fixture.predecessor.getCompletedThrough(), "one Undo must restore task progress");
		SwingUtilities.invokeAndWait(() -> fixture.project.getUndoController().redo());
		assertEquals(updatedProgress, fixture.predecessor.getCompletedThrough(), "one Redo must restore updated progress");
		List<String> persistedIntervals = assignmentIntervals(fixture.predecessor);

		ByteArrayOutputStream saved = new ByteArrayOutputStream();
		MpoFileImporter importer = new MpoFileImporter();
		assertTrue(importer.saveProject(fixture.project, saved), "edited schedule must be serializable");
		Project reopened = importer.loadProject(new ByteArrayInputStream(saved.toByteArray()));
		NormalTask reopenedTask = reopened.getTaskList().stream()
			.filter(task -> "Drag predecessor".equals(task.getName())).map(NormalTask.class::cast).findFirst().orElseThrow();
		assertEquals(resizedStart, reopenedTask.getStart(), "resized start must survive MPO reload");
		assertEquals(resizedEnd, reopenedTask.getEnd(), "resized finish must survive MPO reload");
		assertEquals(updatedProgress, reopenedTask.getCompletedThrough(), "progress must survive MPO reload");
		assertEquals(persistedIntervals, assignmentIntervals(reopenedTask), "split intervals must survive MPO reload");
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

	private List<String> assignmentIntervals(NormalTask task) {
		AtomicReference<List<String>> snapshot = new AtomicReference<>();
		try {
			SwingUtilities.invokeAndWait(() -> {
				List<String> intervals = new ArrayList<>();
				if (task.getAssignments().isEmpty()) {
					snapshot.set(List.of());
					return;
				}
				((Assignment) task.getAssignments().get(0)).consumeIntervals(
					interval -> intervals.add(interval.getStart() + ":" + interval.getEnd()));
				snapshot.set(List.copyOf(intervals));
			});
		} catch (Exception exception) {
			throw new AssertionError("Could not snapshot task intervals on the EDT", exception);
		}
		return snapshot.get();
	}

	private List<String> assignmentContours(NormalTask task) {
		AtomicReference<List<String>> snapshot = new AtomicReference<>();
		try {
			SwingUtilities.invokeAndWait(() -> {
				List<String> contours = new ArrayList<>();
				for (var association : task.getAssignments()) {
					Assignment assignment = (Assignment) association;
					contours.add(assignment.getWorkContour().toString(assignment.getDuration()));
				}
				snapshot.set(List.copyOf(contours));
			});
		} catch (Exception exception) {
			throw new AssertionError("Could not snapshot assignment work contours on the EDT", exception);
		}
		return snapshot.get();
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

	private void click(Robot robot, Point point) {
		robot.mouseMove(point.x, point.y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
	}

	private void openPopupAndChoose(Robot robot, Point point, String itemText) throws Exception {
		robot.mouseMove(point.x, point.y);
		robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
		robot.waitForIdle();
		JMenuItem[] target = new JMenuItem[1];
		GuiAcceptanceSupport.await(() -> findSelectedMenuItem(itemText, target),
			"Gantt popup item was not shown: " + itemText);
		Point[] itemCenter = new Point[1];
		SwingUtilities.invokeAndWait(() -> {
			Point itemLocation = target[0].getLocationOnScreen();
			itemCenter[0] = new Point(itemLocation.x + target[0].getWidth() / 2,
				itemLocation.y + target[0].getHeight() / 2);
		});
		robot.mouseMove(itemCenter[0].x, itemCenter[0].y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
	}

	private boolean findSelectedMenuItem(String text, JMenuItem[] target) {
		boolean[] found = new boolean[1];
		try {
			SwingUtilities.invokeAndWait(() -> {
				for (MenuElement element : MenuSelectionManager.defaultManager().getSelectedPath()) {
					for (MenuElement child : element.getSubElements()) {
						if (child.getComponent() instanceof JMenuItem item && text.equals(item.getText())) {
							target[0] = item;
							found[0] = true;
							break;
						}
					}
					if (found[0]) break;
				}
			});
		} catch (Exception exception) {
			throw new AssertionError("Could not inspect the Gantt popup on the EDT", exception);
		}
		return found[0];
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

	private Point findResizeHandle(Robot robot, Fixture fixture, boolean leading, int expectedCursor, String gesture)
			throws Exception {
		int[] barRowAndY = new int[2];
		Point[] location = new Point[1];
		int[] range = new int[] {Integer.MAX_VALUE, Integer.MIN_VALUE};
		SwingUtilities.invokeAndWait(() -> {
			var projection = fixture.cache.getVisibleNodes().getProjectionIndex();
			for (int row = 0; row < projection.size(); row++) {
				GraphicNode node = projection.nodeAt(row);
				if (node.getNode().getImpl() == fixture.predecessor) {
					barRowAndY[0] = row;
					barRowAndY[1] = (int)Math.round(((GanttUI)gantt.getUI()).getBarY(row)
						+ node.getGanttShapeOffset() + 1);
					location[0] = gantt.getLocationOnScreen();
					for (int x = 0; x < gantt.getWidth(); x++) {
						GraphZone zone = gantt.getUI().getNodeAt(x, barRowAndY[1]);
						if (zone != null && zone.getObject() instanceof GraphicNode hitNode
								&& hitNode.getNode().getImpl() == fixture.predecessor) {
							range[0] = Math.min(range[0], x);
							range[1] = Math.max(range[1], x);
						}
					}
					return;
				}
			}
			throw new AssertionError("Gantt task is absent from its projection");
		});
		if (range[0] > range[1])
			throw new AssertionError("No rendered Gantt bar hit region for " + gesture);

		int step = leading ? 1 : -1;
		for (int x = leading ? range[0] : range[1]; leading ? x <= range[1] : x >= range[0]; x += step) {
			robot.mouseMove(location[0].x + x, location[0].y + barRowAndY[1]);
			robot.waitForIdle();
			if (cursorTypeAt() == expectedCursor)
				return new Point(location[0].x + x, location[0].y + barRowAndY[1]);
		}
		throw new AssertionError(gesture + " did not expose cursor " + expectedCursor
			+ " in the rendered task hit range " + range[0] + ".." + range[1]);
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

	private void assertProgressBarHit(Fixture fixture, Point screenPoint) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			Point location = gantt.getLocationOnScreen();
			GraphZone hit = gantt.getUI().getNodeAt(screenPoint.x - location.x, screenPoint.y - location.y);
			assertTrue(hit != null && hit.getObject() instanceof GraphicNode hitNode
					&& hitNode.getNode().getImpl() == fixture.predecessor
					&& hit.getZoneId() == GanttUI.PROGRESS_BAR_ZONE_ID,
				"progress gesture origin must hit the task's rendered progress bar: " + screenPoint);
		});
	}

	private void assertResizeCursor(Robot robot, Point screenPoint, int cursorType, String gesture) throws Exception {
		robot.mouseMove(screenPoint.x, screenPoint.y);
		robot.waitForIdle();
		GuiAcceptanceSupport.await(() -> {
			int[] actualCursor = new int[1];
			try {
				SwingUtilities.invokeAndWait(() -> actualCursor[0] = gantt.getCursor().getType());
			} catch (Exception exception) {
				throw new AssertionError("Could not inspect Gantt resize cursor", exception);
			}
			return actualCursor[0] == cursorType;
		}, gesture + " handle did not expose the resize cursor at " + screenPoint
			+ " (expected=" + cursorType + ", actual=" + cursorTypeAt() + ")");
	}

	private int cursorTypeAt() {
		int[] actualCursor = new int[1];
		try {
			SwingUtilities.invokeAndWait(() -> actualCursor[0] = gantt.getCursor().getType());
		} catch (Exception exception) {
			throw new AssertionError("Could not inspect Gantt cursor after resize hit-test", exception);
		}
		return actualCursor[0];
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
		return createFixture(dependencyType, includeDependency, false);
	}

	private Fixture createFixture(int dependencyType, boolean includeDependency, boolean assignPredecessor)
			throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("gui-gantt-date-drag", undo);
		Project project = Project.createProject(pool, undo);
		project.initialize(false, false);
		NormalTask predecessor = task(project, "Drag predecessor", 3L);
		NormalTask successor = task(project, "Drag successor", 1L);
		long predecessorStart = DateTime.calendarInstance(2026, java.util.Calendar.JUNE, 8).getTimeInMillis();
		predecessor.getCurrentSchedule().setStart(predecessorStart);
		predecessor.setDuration(3L * com.microproject.options.CalendarOption.getInstance().getMillisPerDay());
		if (assignPredecessor) {
			var resource = project.getResourcePool().newResourceInstance();
			AssignmentService.getInstance().newAssignment(predecessor, resource, 1.0D, 0L, this);
		}
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
