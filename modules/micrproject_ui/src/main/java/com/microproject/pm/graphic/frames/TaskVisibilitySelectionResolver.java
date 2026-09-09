/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.graphic.frames;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;

import com.microproject.grouping.core.GroupNodeImpl;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.model.WalkersNodeModel;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.task.Task;

/** Resolves visible task rows to stable domain tasks for visibility commands. */
final class TaskVisibilitySelectionResolver {
	private TaskVisibilitySelectionResolver() {
	}

	static List<Node> resolve(Collection<Node> selectedNodes, WalkersNodeModel viewModel) {
		List<Node> result = new ArrayList<>();
		if (selectedNodes == null) return result;
		IdentityHashMap<Node, Boolean> visited = new IdentityHashMap<>();
		for (Node node : selectedNodes)
			collect(node, viewModel, result, visited);
		return result;
	}

	private static void collect(Node node, WalkersNodeModel viewModel, List<Node> result,
			IdentityHashMap<Node, Boolean> visited) {
		if (node == null || visited.put(node, Boolean.TRUE) != null) return;
		if (!(node.getImpl() instanceof GroupNodeImpl)) {
			if (node.getImpl() instanceof Task) result.add(node);
			return;
		}
		if (viewModel == null) return;
		List<?> children = viewModel.getChildren(node);
		if (children == null) return;
		for (Object child : children) {
			Node childNode = child instanceof GraphicNode graphicNode ? graphicNode.getNode()
					: child instanceof Node candidate ? candidate : null;
			collect(childNode, viewModel, result, visited);
		}
	}
}
