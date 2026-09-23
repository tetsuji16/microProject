/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.grouping.core.transform.filtering;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.microproject.grouping.core.NodeFactory;
import com.microproject.pm.task.NormalTask;

class NotVoidFilterTest {
	@Test
	void exposesOneStableFilterInstanceAndAcceptsRegularTaskNodes() {
		NotVoidFilter filter = NotVoidFilter.getInstance();

		assertSame(filter, NotVoidFilter.getInstance());
		assertTrue(filter.evaluate(NodeFactory.getInstance().createNode(new NormalTask())));
	}
}
