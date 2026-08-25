/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.grouping.core.transform.grouping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import org.junit.jupiter.api.Test;

class NodeGrouperIsolationTest {
	@Test
	void factoryCreatesIndependentRuntimeGroupers() throws Exception {
		NodeGrouper factory = new NodeGrouper();
		NodeGroup group = new NodeGroup();
		group.setSorterId("Sorter.Name");
		factory.addGroup(group);

		NodeGrouper first = (NodeGrouper)factory.getTransform();
		NodeGrouper second = (NodeGrouper)factory.getTransform();

		assertNotSame(factory, first);
		assertNotSame(first, second);
		assertNotSame(first.getGroups().get(0), second.getGroups().get(0));
		first.setShowSummary(false);
		assertEquals(true, second.isShowSummary());
	}
}
