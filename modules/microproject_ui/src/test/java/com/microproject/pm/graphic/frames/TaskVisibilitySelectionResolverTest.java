/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.NodeFactory;
import com.microproject.grouping.core.model.WalkersNodeModel;
import com.microproject.grouping.core.transform.grouping.NodeGroup;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.undo.DataFactoryUndoController;

class TaskVisibilitySelectionResolverTest {
	@Test
	void resolvesVirtualGroupRowsToTheirStableTaskMembers() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("group-selection", undo), undo);
		project.initialize(false, false);
		Node first = project.createLocalTaskNode(null);
		Node second = project.createLocalTaskNode(null);
		Node group = NodeFactory.getInstance().createGroup(new NodeGroup(), "Grouped");
		Map<Node, List<?>> children = new HashMap<>();
		children.put(group, List.of(new GraphicNode(first, 1), new GraphicNode(second, 1)));

		WalkersNodeModel viewModel = new WalkersNodeModel() {
			@Override public List<?> getChildren(Node parent) { return children.get(parent); }
			@Override public Node getParent(Node child) { return null; }
			@Override public Node search(Object key) { return null; }
			@Override public boolean isSummary(Node node) { return node.getImpl() instanceof Task task && task.isSummary(); }
			@Override public com.microproject.document.Document getDocument() { return null; }
		};

		assertEquals(List.of(first, second), TaskVisibilitySelectionResolver.resolve(List.of(group), viewModel));
	}
}
