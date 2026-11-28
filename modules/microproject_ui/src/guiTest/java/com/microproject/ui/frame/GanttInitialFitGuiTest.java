/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.frame;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Robot;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JSplitPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.exchange.MpoFileImporter;
import com.microproject.model.dependency.DependencyService;
import com.microproject.model.dependency.DependencyType;
import com.microproject.model.resource.ResourcePool;
import com.microproject.model.task.NormalTask;
import com.microproject.model.task.Project;
import com.microproject.ui.testsupport.GuiAcceptanceSupport;
import com.microproject.ui.testsupport.GuiRobot;
import com.microproject.ui.view.gantt.Gantt;
import com.microproject.ui.view.timescale.ScaledScrollPane;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.Environment;

/** Physical desktop regression coverage for the initial Gantt fit at a narrow window width. */
class GanttInitialFitGuiTest {
	private MainRibbonFrame window;
	private GraphicManager manager;
	private GraphicManager previousGraphicManager;
	private boolean previousRibbonUi;
	private boolean previousNewLook;

	@AfterEach
	void closeWindow() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			if (manager != null) manager.cleanUp();
			if (window != null) window.dispose();
		});
		assertSame(previousGraphicManager, GraphicManager.getInstance(), "cleanup restores the previous process-wide graphic manager");
		Environment.setRibbonUI(previousRibbonUi);
		Environment.setNewLook(previousNewLook);
	}

	@Test
	void projectStartIsVisibleAfterResponsiveLayoutAtAbout1010Pixels() throws Exception {
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for GUI acceptance coverage.");
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);

		Project project = project();

		DocumentFrame[] document = new DocumentFrame[1];
		SwingUtilities.invokeAndWait(() -> {
			previousGraphicManager = GraphicManager.getInstance();
			window = new MainRibbonFrame("Gantt initial fit GUI regression", null);
			manager = new GraphicManager(window);
			window.setGraphicManager(manager);
			manager.initView();
			document[0] = manager.addProjectFrame(project);
			window.setSize(new Dimension(1010, 761));
			window.setLocationByPlatform(true);
			window.setAlwaysOnTop(true);
			window.setVisible(true);
		});

		GuiAcceptanceSupport.await(() -> window.isShowing() && document[0].isShowing()
			&& document[0].getGanttView().getGantt().getWidth() > 0,
			"the 1010-pixel project window and Gantt chart did not become visible");
		Robot robot = new GuiRobot();
		robot.setAutoDelay(35);
		robot.waitForIdle();
		var ganttView = document[0].getGanttView();
		awaitStableLayout(window, ganttView);
		com.microproject.ui.view.views.SelectedTasksTimescaleZoom.DateRange[] initialRangeRef = new com.microproject.ui.view.views.SelectedTasksTimescaleZoom.DateRange[1];
		SwingUtilities.invokeAndWait(() -> initialRangeRef[0] = ganttView.getVisibleDateRange());
		var initialRange = initialRangeRef[0];
		assertTrue(initialRange.start() <= project.getStart(),
			"empty project's start must be visible after initial responsive layout; start=" + project.getStart()
				+ ", visible range=" + initialRange + ", window width=" + window.getWidth());

		// Model edits mirror adding the first rows immediately after creating a blank project.
		// The GUI regression is specifically the real startup layout; model entry remains
		// deterministic and is verified through the rendered bar geometry below.
		NormalTask[] tasks = new NormalTask[2];
		SwingUtilities.invokeAndWait(() -> {
			tasks[0] = project.createScriptedTask();
			tasks[0].setName("Task A");
			tasks[0].getCurrentSchedule().setStart(project.getStart());
			tasks[0].setDuration(2 * com.microproject.options.CalendarOption.getInstance().getMillisPerDay());
			tasks[1] = project.createScriptedTask();
			tasks[1].setName("Task B");
			tasks[1].getCurrentSchedule().setStart(tasks[0].getEnd());
			tasks[1].setDuration(2 * com.microproject.options.CalendarOption.getInstance().getMillisPerDay());
			try {
				DependencyService.getInstance().newDependency(tasks[0], tasks[1], DependencyType.FS, 0L, project);
			} catch (com.microproject.association.InvalidAssociationException failure) {
				throw new AssertionError("Could not create the task dependency", failure);
			}
			project.recalculate();
		});
		NormalTask first = tasks[0];
		NormalTask second = tasks[1];
		GuiAcceptanceSupport.await(() -> document[0].getTopSpreadSheet().getRowCount() >= 2,
			"the two task rows did not appear in the real project window");
		SwingUtilities.invokeAndWait(() -> ganttView.getCache().update());
		Gantt gantt = ganttView.getGantt();
		com.microproject.ui.view.views.SelectedTasksTimescaleZoom.DateRange[] taskRangeRef = new com.microproject.ui.view.views.SelectedTasksTimescaleZoom.DateRange[1];
		SwingUtilities.invokeAndWait(() -> {
			taskRangeRef[0] = ganttView.getVisibleDateRange();
			assertTrue(barIsVisible(gantt, first), "Task A bar must be visible in the chart");
			assertTrue(barIsVisible(gantt, second), "Task B bar must be visible in the chart");
		});
		var visibleRange = taskRangeRef[0];
		assertTrue(visibleRange.start() <= first.getStart() && visibleRange.end() >= second.getEnd(),
			"both newly created tasks must remain in the initial chart view; visible range=" + visibleRange);
		long initialCenter = midpoint(visibleRange.start(), visibleRange.end());
		assertEquals(1, second.getPredecessorList().size(), "Task B must have Task A as its predecessor");
		assertEquals(second.getEffectiveWorkCalendar().adjustInsideCalendar(first.getEnd(), false), second.getStart(),
			"the FS predecessor must place Task B at the next working time after Task A");

		ByteArrayOutputStream saved = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(project, saved), "MPO save must succeed");
		Project reopened = new MpoFileImporter().loadProject(new ByteArrayInputStream(saved.toByteArray()));
		assertEquals(2, reopened.getTasks().size(), "both tasks must survive MPO reload");
		assertEquals(first.getStart(), reopened.getTasks().get(0).getStart());
		assertEquals(second.getStart(), reopened.getTasks().get(1).getStart());
		assertEquals(1, reopened.getTasks().get(1).getPredecessorList().size());

		// The initial-fit assertion above distinguishes startup from later user resizing.
		// A subsequent width change should preserve the center date already in view.
		SwingUtilities.invokeAndWait(() -> window.setSize(new Dimension(1110, 761)));
		awaitStableLayout(window, ganttView);
		com.microproject.ui.view.views.SelectedTasksTimescaleZoom.DateRange[] resizedRangeRef = new com.microproject.ui.view.views.SelectedTasksTimescaleZoom.DateRange[1];
		SwingUtilities.invokeAndWait(() -> resizedRangeRef[0] = ganttView.getVisibleDateRange());
		var resizedRange = resizedRangeRef[0];
		long resizedCenter = midpoint(resizedRange.start(), resizedRange.end());
		long twoDays = 2 * com.microproject.options.CalendarOption.getInstance().getMillisPerDay();
		assertTrue(Math.abs(resizedCenter - initialCenter) <= twoDays,
			"a later explicit resize should preserve the visible center date; before=" + visibleRange
				+ ", after=" + resizedRange);
	}

	private static void awaitStableLayout(MainRibbonFrame window, com.microproject.ui.view.views.GanttView view) throws Exception {
		AtomicReference<String> previous = new AtomicReference<>();
		AtomicInteger stableSamples = new AtomicInteger();
		GuiAcceptanceSupport.await(() -> {
			String[] snapshot = new String[1];
			try {
				SwingUtilities.invokeAndWait(() -> {
					JSplitPane split = findSplit(window);
					ScaledScrollPane pane = findScrollPane(view);
					JViewport viewport = pane == null ? null : pane.getViewport();
					snapshot[0] = window.getWidth() + ":" + view.getGantt().getWidth() + ":"
						+ (split == null ? -1 : split.getDividerLocation()) + ":"
						+ (viewport == null ? "none" : viewport.getExtentSize().width);
				});
			} catch (Exception failure) {
				throw new AssertionError("Could not read responsive layout on EDT", failure);
			}
			if (snapshot[0].equals(previous.get())) stableSamples.incrementAndGet();
			else {
				previous.set(snapshot[0]);
				stableSamples.set(0);
			}
			return stableSamples.get() >= 4 && snapshot[0].startsWith(window.getWidth() + ":");
		}, "responsive split and Gantt dimensions did not stabilize");
		// Cross an EDT barrier after the stable measurements so already queued validation runs first.
		SwingUtilities.invokeAndWait(() -> { });
	}

	private static JSplitPane findSplit(Container container) {
		for (Component child : container.getComponents()) {
			if (child instanceof JSplitPane split) return split;
			if (child instanceof Container nested) {
				JSplitPane found = findSplit(nested);
				if (found != null) return found;
			}
		}
		return null;
	}

	private static ScaledScrollPane findScrollPane(Container container) {
		for (Component child : container.getComponents()) {
			if (child instanceof ScaledScrollPane pane) return pane;
			if (child instanceof Container nested) {
				ScaledScrollPane found = findScrollPane(nested);
				if (found != null) return found;
			}
		}
		return null;
	}

	private static boolean barIsVisible(Gantt gantt, NormalTask task) {
		Rectangle2D visible = gantt.getVisibleRect();
		double x1 = gantt.getCoord().toX(task.getStart());
		double x2 = gantt.getCoord().toX(task.getEnd());
		return x2 > visible.getMinX() && x1 < visible.getMaxX();
	}

	private static long midpoint(long start, long end) {
		return start + (end - start) / 2;
	}

	private static Project project() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("initial-gantt-fit", undo), undo);
		project.initialize(false, false);
		project.setName("Initial Gantt fit");
		return project;
	}
}
