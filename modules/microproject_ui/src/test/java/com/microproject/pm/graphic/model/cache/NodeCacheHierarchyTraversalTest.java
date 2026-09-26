/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.graphic.model.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.NodeFactory;

class NodeCacheHierarchyTraversalTest {
	@Test
	void collapsedParentSkipsDescendantsButKeepsFollowingSiblingRows() {
		Node rootNode = node("root");
		Node parentNode = node("parent");
		Node childNode = node("child");
		Node siblingNode = node("sibling");
		GraphicNode root = new GraphicNode(rootNode, 0);
		GraphicNode parent = new GraphicNode(parentNode, 1);
		parent.setComposite(true);
		parent.setCollapsed(true);
		GraphicNode child = new GraphicNode(childNode, 2);
		GraphicNode sibling = new GraphicNode(siblingNode, 1);

		NodeCache cache = new NodeCache();
		cache.insertElement(root, rootNode);
		cache.insertElement(parent, parentNode);
		cache.insertElement(child, childNode);
		cache.insertElement(sibling, siblingNode);
		VisibleNodes view = new VisibleNodes("hierarchy", ignored -> { });
		view.setVisibleDependencies(new VisibleDependencies("hierarchy-dependencies"));

		cache.updateVisibleElements(view, new HashSet<>());

		assertEquals(List.of(root, parent, sibling), view.getElements());
	}

	private static Node node(String name) {
		return NodeFactory.getInstance().createNode(name);
	}
}
