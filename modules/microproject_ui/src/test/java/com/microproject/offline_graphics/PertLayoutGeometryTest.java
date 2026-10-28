/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.offline_graphics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.microproject.grouping.core.Node;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.graphic.model.cache.GraphicDependency;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.graphic.model.cache.ReferenceNodeModelCache;
import com.microproject.pm.graphic.network.NetworkParams;
import com.microproject.pm.graphic.pert.PertLayout;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.undo.DataFactoryUndoController;

class PertLayoutGeometryTest {
	@Test
	void diamondDependenciesPlaceSuccessorsAfterTheirLongestPredecessorPath() throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("pert-layout-geometry", undo), undo);
		project.initialize(false, false);
		NormalTask start = addTask(project, "Start");
		NormalTask branchA = addTask(project, "Branch A");
		NormalTask branchB = addTask(project, "Branch B");
		NormalTask finish = addTask(project, "Finish");
		NormalTask independent = addTask(project, "Independent");
		DependencyService dependencies = DependencyService.getInstance();
		dependencies.newDependency(start, branchA, DependencyType.Kind.FS, 0, this);
		dependencies.newDependency(start, branchB, DependencyType.Kind.FS, 0, this);
		dependencies.newDependency(branchA, finish, DependencyType.Kind.FS, 0, this);
		dependencies.newDependency(branchB, finish, DependencyType.Kind.FS, 0, this);

		ReferenceNodeModelCache reference = NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel());
		NodeModelCache cache = NodeModelCacheFactory.getInstance().createFilteredCache(reference, "Network", null);
		NetworkSVGRenderer renderer = new NetworkSVGRenderer();
		try {
			renderer.init(project, cache, NetworkSVGRenderer.PERT, -1);
			PertLayout layout = (PertLayout) ((NetworkParams) renderer.getParams()).getNetworkLayout();
			GraphicNode startNode = node(cache, project, start);
			GraphicNode branchANode = node(cache, project, branchA);
			GraphicNode branchBNode = node(cache, project, branchB);
			GraphicNode finishNode = node(cache, project, finish);
			GraphicNode independentNode = node(cache, project, independent);

			assertEquals(1, cache.getPertLevel(startNode));
			assertEquals(2, cache.getPertLevel(branchANode));
			assertEquals(2, cache.getPertLevel(branchBNode));
			assertEquals(3, cache.getPertLevel(finishNode));
			assertEquals(1, cache.getPertLevel(independentNode));
			assertTrue(branchANode.getPertCenter().getX() > startNode.getPertCenter().getX());
			assertEquals(branchANode.getPertCenter().getX(), branchBNode.getPertCenter().getX(), 0.01);
			assertTrue(finishNode.getPertCenter().getX() > branchANode.getPertCenter().getX());
			assertEquals(startNode.getPertCenter().getX(), independentNode.getPertCenter().getX(), 0.01);
			assertTrue(branchANode.getPertCenter().getY() < branchBNode.getPertCenter().getY());
			assertEquals(Set.of("Start->Branch A", "Start->Branch B", "Branch A->Finish", "Branch B->Finish"),
				edgeDescriptions(cache));
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

	private static GraphicNode node(NodeModelCache cache, Project project, NormalTask task) {
		Node modelNode = (Node) project.getTaskModel().search(task);
		return (GraphicNode) cache.getGraphicNode(modelNode);
	}

	private static Set<String> edgeDescriptions(NodeModelCache cache) {
		Set<String> result = new java.util.HashSet<>();
		ListIterator<GraphicDependency> edges = cache.getEdgesIterator();
		while (edges.hasNext()) {
			GraphicDependency edge = edges.next();
			String predecessor = ((NormalTask) edge.getPredecessor().getNode().getImpl()).getName();
			String successor = ((NormalTask) edge.getSuccessor().getNode().getImpl()).getName();
			result.add(predecessor + "->" + successor);
		}
		return result;
	}
}
