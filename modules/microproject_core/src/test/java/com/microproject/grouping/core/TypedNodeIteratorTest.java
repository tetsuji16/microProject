/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.grouping.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

import com.microproject.pm.task.NormalTask;

class TypedNodeIteratorTest {
	@Test
	void removesTheLastReturnedNodeEvenAfterLookahead() {
		Node firstTask = NodeFactory.getInstance().createNode(new NormalTask());
		Node unrelated = NodeFactory.getInstance().createNode(new Object());
		Node nullImpl = NodeFactory.getInstance().createNode(new Object());
		nullImpl.setImpl(null);
		NormalTask secondTask = new NormalTask();
		Node secondTaskNode = NodeFactory.getInstance().createNode(secondTask);
		List<Node> nodes = new ArrayList<>(List.of(firstTask, unrelated, nullImpl, secondTaskNode));
		Iterator<Object> tasks = TypedNodeIterator.getTaskInstance(nodes);

		assertSame(firstTask.getImpl(), tasks.next());
		assertTrue(tasks.hasNext());
		tasks.remove();

		assertEquals(List.of(unrelated, nullImpl, secondTaskNode), nodes);
		assertSame(secondTask, tasks.next());
		assertFalse(tasks.hasNext());
		assertThrows(NoSuchElementException.class, tasks::next);
	}

	@Test
	void removeRequiresAnUnremovedListElementToHaveBeenReturned() {
		List<Node> nodes = new ArrayList<>(List.of(NodeFactory.getInstance().createNode(new NormalTask())));
		Iterator<Object> tasks = TypedNodeIterator.getTaskInstance(nodes);

		assertThrows(IllegalStateException.class, tasks::remove);
		tasks.next();
		tasks.remove();
		assertThrows(IllegalStateException.class, tasks::remove);
		assertTrue(nodes.isEmpty());
	}

	@Test
	void removalIsExplicitlyUnsupportedForNonListCollections() {
		Node task = NodeFactory.getInstance().createNode(new NormalTask());
		LinkedHashSet<Node> nodes = new LinkedHashSet<>(List.of(task));
		Iterator<Object> tasks = TypedNodeIterator.getTaskInstance(nodes);

		tasks.next();

		assertThrows(UnsupportedOperationException.class, tasks::remove);
		assertEquals(1, nodes.size());
	}
}
