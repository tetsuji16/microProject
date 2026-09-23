/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.grouping.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NodeFactoryTest {
	public static class TestNode extends NodeBridge {
		public TestNode() {
			super(new Object());
		}
	}

	@Test
	void reflectivelyCreatesVirtualNodesUsingPublicNoArgConstructor() {
		Node node = NodeFactory.getInstance().createVirtualNode(TestNode.class);

		assertNotNull(node);
		assertTrue(node.isVirtual());
		assertFalse(node.isVoid());
	}

	@Test
	void unsupportedImplementationClassRetainsNullFailureBehavior() {
		assertNull(NodeFactory.getInstance().createNode(String.class));
	}
}
