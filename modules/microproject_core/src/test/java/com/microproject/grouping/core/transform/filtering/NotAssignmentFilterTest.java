/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.grouping.core.transform.filtering;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.microproject.grouping.core.NodeFactory;
import com.microproject.pm.task.NormalTask;

class NotAssignmentFilterTest {
	@Test
	void exposesStableDistinctSingletonsForEachFilteringMode() {
		NotAssignmentFilter standard = NotAssignmentFilter.getInstance();
		NotAssignmentFilter writable = NotAssignmentFilter.getWritableInstance();

		assertSame(standard, NotAssignmentFilter.getInstance());
		assertSame(writable, NotAssignmentFilter.getWritableInstance());
		assertNotSame(standard, writable);
		assertTrue(standard.evaluate(NodeFactory.getInstance().createNode(new NormalTask())));
	}
}
