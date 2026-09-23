/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.grouping.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.pm.task.NormalTask;

class NodeListTest {
	@Test
	void acceptsTypedNodeIteratorInIterationOrder() {
		Node first = NodeFactory.getInstance().createNode(new NormalTask());
		Node second = NodeFactory.getInstance().createNode(new NormalTask());
		List<Node> nodes = List.of(first, second);
		List<Object> visited = new ArrayList<>();
		NodeVisitor visitor = new NodeVisitor() {
			@Override
			public void accept(Object value) {
				visited.add(value);
			}

			@Override
			public void reset() {
				visited.clear();
			}
		};

		NodeList.accept(visitor, nodes.iterator());

		assertEquals(nodes, visited);
	}

	@Test
	void convertsTypedNodeCollectionAndPreservesNullFallback() throws NodeException {
		NormalTask task = new NormalTask();
		Node node = NodeFactory.getInstance().createNode(task);
		NodeList nodes = new NodeList();
		nodes.add(node);
		Class<? extends Node> nodeType = nodes.getType();

		List<Object> implementations = NodeList.nodeListToImplList(nodes);

		assertSame(node.getClass(), nodeType);
		assertEquals(List.of(task), implementations);
		assertEquals(List.of(), NodeList.nodeListToImplList(null));
	}
}
