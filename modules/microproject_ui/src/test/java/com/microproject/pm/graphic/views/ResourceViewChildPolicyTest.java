package com.microproject.pm.graphic.views;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.NodeFactory;

class ResourceViewChildPolicyTest {
	@Test
	void childPolicyDeterminesWhetherTheParentCanBeProcessed() {
		Node parent = NodeFactory.getInstance().createNode(new Object());
		Node allowedChild = NodeFactory.getInstance().createNode(new Object());
		Node protectedChild = NodeFactory.getInstance().createNode(new Object());
		parent.add(allowedChild);
		parent.add(protectedChild);

		assertTrue(ResourceView.areChildrenAllowed(parent, child -> true));
		assertFalse(ResourceView.areChildrenAllowed(parent, child -> child != protectedChild));
	}

	@Test
	void childPolicyAllowsNodesWithoutChildren() {
		Node leaf = NodeFactory.getInstance().createNode(new Object());

		assertTrue(ResourceView.areChildrenAllowed(leaf, child -> false));
	}
}
