/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.AbstractButton;
import javax.swing.JScrollBar;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.pert.PertChart;
import com.microproject.pm.graphic.views.PertView;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.testsupport.GuiRobot;
import com.microproject.testsupport.RibbonGuiSupport;
import com.microproject.ui.ribbon.RibbonController;
import com.microproject.ui.ribbon.RibbonDisplayMode;
import com.microproject.ui.ribbon.RibbonDisplayPreferences;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.Environment;

import static com.microproject.testsupport.NetworkDiagramGuiSupport.capture;
import static com.microproject.testsupport.NetworkDiagramGuiSupport.click;
import static com.microproject.testsupport.NetworkDiagramGuiSupport.onEdt;
import static com.microproject.testsupport.NetworkDiagramGuiSupport.onScreen;
import static com.microproject.testsupport.NetworkDiagramGuiSupport.screenBounds;

/** Physical PERT selection, zoom, scrolling, and resize evidence for issue #748. */
class PertInteractionGuiAcceptanceTest {
	private MainRibbonFrame window;
	private GraphicManager manager;
	private boolean previousRibbonUi;
	private boolean previousNewLook;
	private RibbonDisplayMode previousRibbonDisplayMode;

	@AfterEach
	void closeWindow() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			if (manager != null) manager.cleanUp();
			if (window != null) window.dispose();
		});
		Environment.setRibbonUI(previousRibbonUi);
		Environment.setNewLook(previousNewLook);
		if (previousRibbonDisplayMode != null) RibbonDisplayPreferences.save(previousRibbonDisplayMode);
	}

	@Test
	void robotSelectsZoomsScrollsAndResizesPertView() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for PERT Robot coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		previousRibbonDisplayMode = RibbonDisplayPreferences.load();
		RibbonDisplayPreferences.save(RibbonDisplayMode.ALWAYS_SHOW);
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		Project project = dependencyProject();
		showProject(project);
		Robot robot = new GuiRobot();
		robot.setAutoDelay(45);
		GuiAcceptanceSupport.await(() -> onEdt(() -> window.isShowing() && manager.getCurrentFrame() != null),
			"project window did not become visible");

		AbstractButton viewTab = GuiAcceptanceSupport.findShowingButtonByText(window,
			ResourceBundle.getBundle("com.microproject.menu.menu").getString("ViewRibbonTask.title"));
		click(robot, screenBounds(viewTab));
		GuiAcceptanceSupport.await(() -> onEdt(viewTab::isSelected), "physical View tab click did not select the View ribbon");
		clickCommand(robot, "RibbonNetwork");
		GuiAcceptanceSupport.await(() -> onEdt(() -> manager.getCurrentFrame().getActiveTopView() instanceof PertView),
			"physical RibbonNetwork click did not activate PertView");
		PertView view = onEdt(() -> (PertView) manager.getCurrentFrame().getActiveTopView());
		PertChart graph = onEdt(() -> (PertChart) view.getViewport().getView());
		GuiAcceptanceSupport.await(() -> onEdt(() -> view.isShowing() && view.getCache().getSize() > 40),
			"PERT dependency DAG did not render");
		GraphicNode selectedNode = onEdt(() -> (GraphicNode) view.getCache().getElementAt(2));
		NormalTask selectedTask = onEdt(() -> (NormalTask) selectedNode.getNode().getImpl());
		clickNode(robot, graph, selectedNode);
		GuiAcceptanceSupport.await(() -> onEdt(() -> selectedImpl(graph) == selectedTask),
			"physical click on a PERT node did not select its task");
		capture(robot, window, "pert-selected.png");

		int oldZoom = onEdt(graph::getZoom);
		clickCommand(robot, "RibbonZoomIn");
		GuiAcceptanceSupport.await(() -> onEdt(() -> graph.getZoom() > oldZoom), "physical Zoom In did not change PERT zoom");
		clickNode(robot, graph, selectedNode);
		GuiAcceptanceSupport.await(() -> onEdt(() -> selectedImpl(graph) == selectedTask),
			"selected PERT node was not hit-testable after zoom");
		capture(robot, window, "pert-zoomed.png");

		JScrollBar vertical = onEdt(view::getVerticalScrollBar);
		Rectangle scrollBounds = screenBounds(vertical);
		int beforeScroll = onEdt(() -> view.getViewport().getViewPosition().y);
		assertTrue(onEdt(() -> vertical.getMaximum() > vertical.getVisibleAmount()),
			"PERT fixture must require vertical scrolling");
		robot.mouseMove(scrollBounds.x + scrollBounds.width / 2, scrollBounds.y + scrollBounds.height - 5);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
		GuiAcceptanceSupport.await(() -> onEdt(() -> view.getViewport().getViewPosition().y > beforeScroll),
			"physical vertical scrollbar click did not move the PERT viewport");
		capture(robot, window, "pert-scrolled.png");

		Rectangle beforeResize = screenBounds(window);
		DimensionState beforeExtent = extent(view);
		robot.mouseMove(beforeResize.x + beforeResize.width - 2, beforeResize.y + beforeResize.height - 2);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseMove(beforeResize.x + beforeResize.width - 100, beforeResize.y + beforeResize.height - 80);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
		GuiAcceptanceSupport.await(() -> onEdt(() -> window.getWidth() < beforeResize.width - 40
			&& window.getHeight() < beforeResize.height - 30), "physical window-edge drag did not resize PERT host");
		GuiAcceptanceSupport.await(() -> extent(view).width < beforeExtent.width
			&& extent(view).height < beforeExtent.height, "PERT viewport extent did not follow host resize");
		capture(robot, window, "pert-resized.png");
		assertEquals(selectedTask, onEdt(() -> selectedImpl(graph)), "resize must preserve selected task identity");
	}

	private void showProject(Project project) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			window = new MainRibbonFrame("microProject — PERT interaction acceptance", null);
			manager = new GraphicManager(window);
			window.setGraphicManager(manager);
			manager.initView();
			RibbonController ribbon = findRibbonController(window);
			if (ribbon == null) throw new AssertionError("application ribbon controller was not installed");
			ribbon.setRibbonDisplayMode(RibbonDisplayMode.ALWAYS_SHOW);
			manager.addProjectFrame(project);
			window.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			window.setSize(1600, 700);
			window.setLocationByPlatform(true);
			window.setAlwaysOnTop(false);
			window.setVisible(true);
			window.toFront();
			window.requestFocus();
		});
	}

	private static Project dependencyProject() throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("pert-interaction", undo), undo);
		project.initialize(false, false);
		List<NormalTask> tasks = new ArrayList<>();
		for (int index = 0; index < 42; index++) {
			NormalTask task = project.createScriptedTask();
			task.setName("PERT task " + index);
			project.connectTask(task);
			project.getTaskOutlines().addToAll(task, null);
			tasks.add(task);
		}
		DependencyService dependencyService = DependencyService.getInstance();
		NormalTask start = tasks.get(0);
		NormalTask finish = tasks.get(tasks.size() - 1);
		for (NormalTask branch : tasks.subList(1, tasks.size() - 1)) {
			dependencyService.newDependency(start, branch, DependencyType.Kind.FS, 0, PertInteractionGuiAcceptanceTest.class);
			dependencyService.newDependency(branch, finish, DependencyType.Kind.FS, 0, PertInteractionGuiAcceptanceTest.class);
		}
		return project;
	}

	private void clickCommand(Robot robot, String commandId) throws Exception {
		AbstractButton button = RibbonGuiSupport.findVisibleOrExpand(robot, window, commandId);
		click(robot, screenBounds(button));
	}

	private static Object selectedImpl(PertChart graph) {
		Object selected = graph.getUI().getInteractor().getSelectedObject();
		return selected instanceof GraphicNode node ? node.getNode().getImpl() : null;
	}

	private static void clickNode(Robot robot, PertChart graph, GraphicNode node) throws Exception {
		Point2D center = onEdt(() -> graph.scale(node.getPertCenter()));
		Point origin = onScreen(graph);
		robot.mouseMove(origin.x + (int) center.getX(), origin.y + (int) center.getY());
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
	}

	private static DimensionState extent(PertView view) {
		return onEdt(() -> new DimensionState(view.getViewport().getExtentSize().width,
			view.getViewport().getExtentSize().height));
	}

	private static RibbonController findRibbonController(java.awt.Container container) {
		for (java.awt.Component component : container.getComponents()) {
			if (component instanceof RibbonController ribbon) return ribbon;
			if (component instanceof java.awt.Container child) {
				RibbonController found = findRibbonController(child);
				if (found != null) return found;
			}
		}
		return null;
	}

	private record DimensionState(int width, int height) { }
}
