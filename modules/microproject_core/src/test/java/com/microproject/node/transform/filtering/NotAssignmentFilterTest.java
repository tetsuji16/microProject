/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.node.transform.filtering;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.microproject.node.NodeFactory;
import com.microproject.model.task.NormalTask;

class NotAssignmentFilterTest {
	@Test
	void exposesStableDistinctSingletonsForEachFilteringMode() {
		NotAssignmentFilter standard = NotAssignmentFilter.getInstance();
		NotAssignmentFilter writable = NotAssignmentFilter.getWritableInstance();

		assertSame(standard, NotAssignmentFilter.getInstance());
		assertSame(writable, NotAssignmentFilter.getWritableInstance());
		assertNotSame(standard, writable);
		assertTrue(standard.test(NodeFactory.getInstance().createNode(new NormalTask())));
	}
}
