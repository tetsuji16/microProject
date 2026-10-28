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

import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.views.TreeView;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.testsupport.GuiRobot;
import com.microproject.testsupport.RibbonGuiSupport;
import com.microproject.ui.ribbon.RibbonDisplayMode;
import com.microproject.ui.ribbon.RibbonDisplayPreferences;
import com.microproject.ui.ribbon.RibbonController;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.Environment;

import static com.microproject.testsupport.NetworkDiagramGuiSupport.capture;
import static com.microproject.testsupport.NetworkDiagramGuiSupport.click;
import static com.microproject.testsupport.NetworkDiagramGuiSupport.onEdt;
import static com.microproject.testsupport.NetworkDiagramGuiSupport.onScreen;
import static com.microproject.testsupport.NetworkDiagramGuiSupport.screenBounds;

/** Physical WBS selection, zoom, scrolling, and resize evidence for issue #748. */
class WbsInteractionGuiAcceptanceTest {
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
	void robotSelectsZoomsScrollsAndResizesWbsView() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for WBS Robot coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		previousRibbonDisplayMode = RibbonDisplayPreferences.load();
		RibbonDisplayPreferences.save(RibbonDisplayMode.ALWAYS_SHOW);
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		Project project = hierarchyProject();
		showProject(project);
		Robot robot = new GuiRobot();
		robot.setAutoDelay(45);
		GuiAcceptanceSupport.await(() -> onEdt(() -> window.isShowing() && manager.getCurrentFrame() != null),
			"project window did not become visible");

		AbstractButton viewTab = GuiAcceptanceSupport.findShowingButtonByText(window,
			ResourceBundle.getBundle("com.microproject.menu.menu").getString("ViewRibbonTask.title"));
		click(robot, screenBounds(viewTab));
		GuiAcceptanceSupport.await(() -> onEdt(viewTab::isSelected), "physical View tab click did not select the View ribbon");
		clickCommand(robot, "RibbonWBS");
		GuiAcceptanceSupport.await(() -> onEdt(() -> manager.getCurrentFrame().getActiveTopView() instanceof TreeView),
			"physical RibbonWBS click did not activate TreeView");
		TreeView treeView = onEdt(() -> (TreeView) manager.getCurrentFrame().getActiveTopView());
		com.microproject.pm.graphic.xbs.Xbs graph = onEdt(() -> (com.microproject.pm.graphic.xbs.Xbs) treeView.getViewport().getView());
		GuiAcceptanceSupport.await(() -> onEdt(() -> treeView.isShowing() && graph.getCache().getSize() > 40),
			"WBS multi-leaf hierarchy did not render");
		GraphicNode selectedNode = onEdt(() -> (GraphicNode) graph.getCache().getElementAt(2));
		NormalTask selectedTask = onEdt(() -> (NormalTask) selectedNode.getNode().getImpl());
		clickNode(robot, graph, selectedNode);
		GuiAcceptanceSupport.await(() -> onEdt(() -> treeView.getSelectedImpl() == selectedTask),
			"physical click on a WBS node did not select its task");
		capture(robot, window, "wbs-selected.png");

		int oldZoom = onEdt(treeView::getScale);
		clickCommand(robot, "RibbonZoomIn");
		GuiAcceptanceSupport.await(() -> onEdt(() -> treeView.getScale() > oldZoom), "physical Zoom In command did not change WBS zoom");
		clickNode(robot, graph, selectedNode);
		GuiAcceptanceSupport.await(() -> onEdt(() -> treeView.getSelectedImpl() == selectedTask),
			"selected WBS node was not hit-testable after zoom");
		capture(robot, window, "wbs-zoomed.png");

		JScrollBar horizontal = onEdt(treeView::getHorizontalScrollBar);
		Rectangle scrollBounds = screenBounds(horizontal);
		int beforeScroll = onEdt(() -> treeView.getViewport().getViewPosition().x);
		assertTrue(onEdt(() -> horizontal.getMaximum() > horizontal.getVisibleAmount()), "WBS fixture must require horizontal scrolling");
		robot.mouseMove(scrollBounds.x + scrollBounds.width - 5, scrollBounds.y + scrollBounds.height / 2);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
		GuiAcceptanceSupport.await(() -> onEdt(() -> treeView.getViewport().getViewPosition().x > beforeScroll),
			"physical horizontal scrollbar click did not move the WBS viewport");
		capture(robot, window, "wbs-scrolled.png");

		Rectangle beforeResize = screenBounds(window);
		DimensionState beforeExtent = extent(treeView);
		robot.mouseMove(beforeResize.x + beforeResize.width - 2, beforeResize.y + beforeResize.height - 2);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseMove(beforeResize.x + beforeResize.width - 100, beforeResize.y + beforeResize.height - 80);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
		GuiAcceptanceSupport.await(() -> onEdt(() -> window.getWidth() < beforeResize.width - 40
			&& window.getHeight() < beforeResize.height - 30), "physical window-edge drag did not resize WBS host");
		GuiAcceptanceSupport.await(() -> extent(treeView).width < beforeExtent.width
			&& extent(treeView).height < beforeExtent.height, "WBS viewport extent did not follow host resize");
		capture(robot, window, "wbs-resized.png");
		assertEquals(selectedTask, onEdt(treeView::getSelectedImpl), "resize must preserve selected task identity");
	}

	private void showProject(Project project) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			window = new MainRibbonFrame("microProject — WBS interaction acceptance", null);
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

	private static Project hierarchyProject() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("wbs-interaction", undo), undo);
		project.initialize(false, false);
		List<NormalTask> tasks = new ArrayList<>();
		for (int index = 0; index < 42; index++) {
			NormalTask task = project.createScriptedTask();
			task.setName("WBS task " + index);
			project.connectTask(task);
			project.getTaskOutlines().addToAll(task, null);
			tasks.add(task);
		}
		NodeModel model = project.getTaskModel();
		List<Node> leaves = tasks.subList(2, tasks.size()).stream()
			.map(task -> (Node) model.search(task)).toList();
		model.indent(leaves, 1, NodeModel.SILENT);
		model.indent(List.of((Node) model.search(tasks.get(1))), 1, NodeModel.SILENT);
		return project;
	}

	private void clickCommand(Robot robot, String commandId) throws Exception {
		AbstractButton button = RibbonGuiSupport.findVisibleOrExpand(robot, window, commandId);
		click(robot, screenBounds(button));
	}

	private static void clickNode(Robot robot, com.microproject.pm.graphic.xbs.Xbs graph, GraphicNode node) throws Exception {
		Point2D center = onEdt(() -> graph.scale(node.getXbsCenter()));
		Point origin = onScreen(graph);
		robot.mouseMove(origin.x + (int) center.getX(), origin.y + (int) center.getY());
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
	}

	private static DimensionState extent(TreeView view) {
		return onEdt(() -> new DimensionState(view.getViewport().getExtentSize().width,
			view.getViewport().getExtentSize().height));
	}

	private record DimensionState(int width, int height) { }
}
