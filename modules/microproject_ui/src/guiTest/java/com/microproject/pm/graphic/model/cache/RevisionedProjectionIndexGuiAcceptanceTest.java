/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.model.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.configuration.Dictionary;
import com.microproject.graphic.configuration.BarStyles;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.transform.ViewTransformer;
import com.microproject.grouping.core.transform.sorting.NodeSorter;
import com.microproject.pm.graphic.gantt.Gantt;
import com.microproject.pm.graphic.gantt.GanttUI;
import com.microproject.pm.graphic.graph.GraphZone;
import com.microproject.pm.graphic.timescale.CoordinatesConverter;
import com.microproject.options.CalendarOption;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.testsupport.GuiAcceptanceSupport;

/** Physical same-project Gantt verification for issue #405. */
class RevisionedProjectionIndexGuiAcceptanceTest {
	private JFrame frame;
	private Gantt firstGantt;
	private Gantt secondGantt;
	private NodeModelCache firstCache;
	private NodeModelCache secondCache;
	private ReferenceNodeModelCache reference;

	@AfterEach
	void closeWindows() throws Exception {
		if (frame != null) SwingUtilities.invokeAndWait(() -> {
			frame.dispose();
			frame = null;
		});
		if (firstGantt != null) firstGantt.cleanUp();
		if (secondGantt != null) secondGantt.cleanUp();
		if (firstCache != null) firstCache.close();
		if (secondCache != null) secondCache.close();
		if (reference != null) reference.close();
	}

	@Test
	void sameProjectViewsKeepOppositeRowsAndPhysicalGanttHitTargetsIndependent() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture();
		showFixture();
		Robot robot = new Robot();
		robot.setAutoDelay(40);
		SwingUtilities.invokeAndWait(() -> {
			frame.toFront();
			frame.requestFocus();
		});
		GuiAcceptanceSupport.await(() -> frame.isActive(), "same-project Gantt test frame did not become active");
		GuiAcceptanceSupport.await(() -> firstGantt.isShowing() && secondGantt.isShowing(),
			"both same-project Gantt views must be visible");
		robot.delay(350);
		captureLayout(robot);

		int firstTaskRow = rowOf(firstCache, fixture.tasks().getFirst());
		int secondTaskRow = rowOf(firstCache, fixture.tasks().getLast());
		assertTrue(firstTaskRow < secondTaskRow, "first view must retain project order");
		assertTrue(rowOf(secondCache, fixture.tasks().getFirst()) > rowOf(secondCache, fixture.tasks().getLast()),
			"second view must have the opposite order");
		for (Task task : fixture.tasks()) {
			int firstRow = rowOf(firstCache, task);
			int secondRow = rowOf(secondCache, task);
			assertNotEquals(firstRow, secondRow, "the views must present the same task in different rows");
			Point firstPoint = hitPoint(firstGantt, firstRow, task);
			Point secondPoint = hitPoint(secondGantt, secondRow, task);
			assertHit(firstGantt, firstPoint, task, "first view");
			assertHit(secondGantt, secondPoint, task, "reverse view");
			click(robot, firstGantt, firstPoint);
			assertSelected(firstGantt, task, "first view after its physical click");
			click(robot, secondGantt, secondPoint);
			assertSelected(secondGantt, task, "reverse view after its physical click");
			assertSelected(firstGantt, task, "first view after the reverse view was clicked");
			assertHit(firstGantt, firstPoint, task, "first view after the reverse view was clicked");
			assertHit(secondGantt, secondPoint, task, "reverse view after physical interaction");
		}
	}

	private Fixture createFixture() throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("issue-405-two-view", undo), undo);
		project.initialize(false, false);
		List<NormalTask> tasks = new ArrayList<>();
		for (int index = 0; index < 2; index++) {
			NormalTask task = project.createScriptedTask();
			task.setName("Projection task " + (char) ('A' + index));
			task.getCurrentSchedule().setStart(project.getStart());
			task.setDuration(CalendarOption.getInstance().getMillisPerDay() * 3);
			tasks.add(task);
		}
		project.recalculate();
		String runId = UUID.randomUUID().toString();
		String firstViewName = "issue-405-first-" + runId;
		String reverseViewName = "issue-405-reverse-" + runId;
		SwingUtilities.invokeAndWait(() -> {
			reference = NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel());
			firstCache = NodeModelCacheFactory.getInstance().createFilteredCache(reference, firstViewName, null);
			secondCache = NodeModelCacheFactory.getInstance().createFilteredCache(reference, reverseViewName,
				transformer -> ((ViewTransformer) transformer).setUserSorter(reverseTaskNameSorter()));
			firstCache.update();
			secondCache.update();
			firstGantt = createGantt(project, firstCache, firstViewName);
			secondGantt = createGantt(project, secondCache, reverseViewName);
		});
		return new Fixture(List.copyOf(tasks));
	}

	private static NodeSorter reverseTaskNameSorter() {
		return new NodeSorter() {
			@Override
			public int compare(Object left, Object right) {
				String leftName = taskName(left);
				String rightName = taskName(right);
				return rightName.compareTo(leftName);
			}
		};
	}

	private static String taskName(Object value) {
		Object implementation = ((Node) value).getImpl();
		return implementation instanceof Task task ? task.getName() : "";
	}

	private static Gantt createGantt(Project project, NodeModelCache cache, String viewName) {
		Gantt gantt = new Gantt(project, viewName);
		gantt.setCache(cache);
		gantt.setCoord(new CoordinatesConverter(project));
		gantt.setBarStyles((BarStyles) Dictionary.get(BarStyles.category, "standard"));
		gantt.setRowHeight(28);
		gantt.updateSize();
		return gantt;
	}

	private void showFixture() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			frame = new JFrame("Issue #405 same-project Gantt projection");
			JPanel views = new JPanel(new java.awt.GridLayout(1, 2, 8, 0));
			views.add(new JScrollPane(firstGantt));
			views.add(new JScrollPane(secondGantt));
			frame.setContentPane(views);
			frame.setPreferredSize(new Dimension(1400, 600));
			frame.pack();
			frame.setLocationByPlatform(true);
			frame.setAlwaysOnTop(true);
			frame.setVisible(true);
			firstGantt.updateSize();
			secondGantt.updateSize();
		});
	}

	private static int rowOf(NodeModelCache cache, Task task) throws Exception {
		int[] result = { -1 };
		SwingUtilities.invokeAndWait(() -> {
			for (int row = 0; row < cache.getSize(); row++) {
				GraphicNode node = (GraphicNode) cache.getElementAt(row);
				if (node.getNode().getImpl() == task) {
					result[0] = row;
					return;
				}
			}
		});
		if (result[0] < 0) throw new AssertionError("task is missing from view: " + task.getName());
		return result[0];
	}

	private static Point hitPoint(Gantt gantt, int row, Task task) throws Exception {
		Point[] point = new Point[1];
		SwingUtilities.invokeAndWait(() -> {
			int y = row * gantt.getRowHeight() + gantt.getRowHeight() / 2;
			double middle = task.getCurrentSchedule().getStart()
				+ (task.getCurrentSchedule().getEnd() - task.getCurrentSchedule().getStart()) / 2.0d;
			int x = (int) Math.round(gantt.getCoord().toX((long) middle));
			point[0] = new Point(x, y);
			assertTrue(gantt.getVisibleRect().contains(point[0]),
				"expected task bar point must be in the visible Gantt: " + point[0]);
		});
		return point[0];
	}

	private void captureLayout(Robot robot) throws Exception {
		Point location = new Point();
		Dimension size = new Dimension();
		SwingUtilities.invokeAndWait(() -> {
			location.setLocation(frame.getLocationOnScreen());
			size.setSize(frame.getSize());
		});
		BufferedImage image = robot.createScreenCapture(new java.awt.Rectangle(location, size));
		Path artifacts = Path.of(System.getProperty("microproject.gui.artifacts.dir", "build/guiTest-artifacts"));
		Files.createDirectories(artifacts);
		ImageIO.write(image, "png", artifacts.resolve("issue-405-two-view-projection.png").toFile());
	}

	private static void assertHit(Gantt gantt, Point point, Task task, String view) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			GraphZone zone = ((GanttUI) gantt.getUI()).getNodeAt(point.x, point.y);
			assertNotNull(zone, view + " must resolve a painted task-bar hit target at " + point);
			assertEquals(task, ((GraphicNode) zone.getObject()).getNode().getImpl(),
				view + " must resolve the task represented by its own projected row");
		});
	}

	private static void assertSelected(Gantt gantt, Task task, String view) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			Object selected = ((GanttUI) gantt.getUI()).getInteractor().getSelectedObject();
			assertSame(task, selected instanceof GraphicNode node ? node.getNode().getImpl() : null,
				view + " must retain its own selected task identity");
		});
	}

	private static void click(Robot robot, Gantt gantt, Point point) throws Exception {
		Point[] screen = new Point[1];
		SwingUtilities.invokeAndWait(() -> {
			screen[0] = new Point(point);
			SwingUtilities.convertPointToScreen(screen[0], gantt);
		});
		robot.mouseMove(screen[0].x, screen[0].y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.delay(100);
	}

	private record Fixture(List<NormalTask> tasks) { }
}
