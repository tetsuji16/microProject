/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.offline_graphics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

import org.junit.jupiter.api.Test;

import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.pm.graphic.model.cache.GraphicDependency;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.graphic.model.cache.ReferenceNodeModelCache;
import com.microproject.pm.graphic.xbs.XbsLayout;
import com.microproject.pm.graphic.xbs.Xbs;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.undo.DataFactoryUndoController;

class XbsLayoutGeometryTest {
	@Test
	void wbsHitTestingAndInteractorSelectionFollowZoomedNodeGeometry() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("xbs-hit-test", undo), undo);
		project.initialize(false, false);
		NormalTask root = addTask(project, "Root");
		NormalTask parent = addTask(project, "Parent");
		NormalTask leaf = addTask(project, "Leaf");
		NodeModel model = project.getTaskModel();
		model.indent(List.of((Node) model.search(leaf)), 1, NodeModel.SILENT);
		model.indent(List.of((Node) model.search(parent)), 1, NodeModel.SILENT);

		Xbs tree = new Xbs(project, "WBS");
		ReferenceNodeModelCache reference = NodeModelCacheFactory.createTaskNodeModelCache(project, model);
		NodeModelCache cache = NodeModelCacheFactory.getInstance().createFilteredCache(reference, "WBS", null);
		try {
			tree.setCache(cache);
			tree.setBarStyles((com.microproject.graphic.configuration.BarStyles)
				com.microproject.configuration.Dictionary.get(
					com.microproject.graphic.configuration.BarStyles.category, "WBS"));
			cache.update();
			((com.microproject.pm.graphic.network.NetworkModel) tree.getModel()).updateCellBounds();
			GraphicNode leafNode = (GraphicNode) cache.getGraphicNode(model.search(leaf));
			assertNotNull(leafNode.getXbsCenter());
			assertNodeHitAndSelect(tree, leafNode, leaf);
			int initialZoom = tree.getZoom();
			assertEquals(true, tree.canZoomIn(), "fixture must allow at least one zoom step");
			tree.zoomIn();
			assertTrue(tree.getZoom() > initialZoom);
			assertNodeHitAndSelect(tree, leafNode, leaf);
		} finally {
			tree.cleanUp();
			cache.close();
			reference.close();
		}
	}

	@Test
	void multilevelHierarchyCentersParentsAndOrdersConnectorsByDepthThenOutline() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("xbs-layout-geometry", undo), undo);
		project.initialize(false, false);

		NormalTask root = addTask(project, "Root");
		NormalTask parent = addTask(project, "Parent");
		NormalTask firstLeaf = addTask(project, "First leaf");
		NormalTask secondLeaf = addTask(project, "Second leaf");
		NodeModel model = project.getTaskModel();
		model.indent(List.of((Node) model.search(firstLeaf), (Node) model.search(secondLeaf)), 1, NodeModel.SILENT);
		model.indent(List.of((Node) model.search(parent)), 1, NodeModel.SILENT);

		ReferenceNodeModelCache reference = NodeModelCacheFactory.createTaskNodeModelCache(project, model);
		NodeModelCache cache = NodeModelCacheFactory.getInstance().createFilteredCache(reference, "Network", null);
		NetworkSVGRenderer renderer = new NetworkSVGRenderer();
		try {
			renderer.init(project, cache, NetworkSVGRenderer.WBS, -1);
			XbsLayout layout = (XbsLayout) ((com.microproject.pm.graphic.network.NetworkParams) renderer.getParams()).getNetworkLayout();
			GraphicNode rootNode = (GraphicNode) cache.getGraphicNode(model.search(root));
			GraphicNode parentNode = (GraphicNode) cache.getGraphicNode(model.search(parent));
			GraphicNode firstNode = (GraphicNode) cache.getGraphicNode(model.search(firstLeaf));
			GraphicNode secondNode = (GraphicNode) cache.getGraphicNode(model.search(secondLeaf));

			assertNotNull(firstNode.getXbsCenter());
			assertNotNull(secondNode.getXbsCenter());
			assertNotNull(parentNode.getXbsCenter());
			assertEquals((firstNode.getXbsCenter().getX() + secondNode.getXbsCenter().getX()) / 2,
				parentNode.getXbsCenter().getX(), 0.01);
			assertEquals(parentNode.getXbsCenter().getX(), rootNode.getXbsCenter().getX(), 0.01);
			assertEquals(3, layout.getDependencies().size());
			assertConnector(layout.getDependencies().get(0), parentNode, firstNode);
			assertConnector(layout.getDependencies().get(1), parentNode, secondNode);
			assertConnector(layout.getDependencies().get(2), rootNode, parentNode);
		} finally {
			renderer.cleanUp();
			reference.close();
		}
	}

	private static NormalTask addTask(Project project, String name) {
		NormalTask task = project.createScriptedTask();
		task.setName(name);
		project.connectTask(task);
		project.getTaskOutlines().addToAll(task, null);
		return task;
	}

	private static void assertConnector(GraphicDependency dependency, GraphicNode parent, GraphicNode child) {
		assertSame(parent, dependency.getPredecessor());
		assertSame(child, dependency.getSuccessor());
	}

	private static void assertNodeHitAndSelect(Xbs tree, GraphicNode expected, NormalTask implementation) {
		Point2D point = tree.scale(expected.getXbsCenter());
		assertSame(expected, tree.getUI().getNodeAt(point.getX(), point.getY()).getObject());
		tree.getUI().getInteractor().mouseMoved(new MouseEvent(tree, MouseEvent.MOUSE_MOVED,
			System.currentTimeMillis(), 0, (int) point.getX(), (int) point.getY(), 0, false));
		assertSame(expected, tree.getUI().getInteractor().getSelectedObject());
		assertSame(implementation, expected.getNode().getImpl());
	}
}
