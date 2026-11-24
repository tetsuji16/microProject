/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.node.transform.filtering;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.microproject.node.NodeFactory;
import com.microproject.model.task.NormalTask;

class NotVoidFilterTest {
	@Test
	void exposesOneStableFilterInstanceAndAcceptsRegularTaskNodes() {
		NotVoidFilter filter = NotVoidFilter.getInstance();

		assertSame(filter, NotVoidFilter.getInstance());
		assertTrue(filter.test(NodeFactory.getInstance().createNode(new NormalTask())));
	}
}
