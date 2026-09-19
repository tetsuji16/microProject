/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.grouping.core.transform.filtering;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.microproject.grouping.core.NodeFactory;
import com.microproject.pm.task.NormalTask;

class BaseFilterTest {
	@Test
	void excludesHiddenTasksButKeepsVisibleTasks() {
		BaseFilter filter = new BaseFilter("false");
		NormalTask visible = new NormalTask();
		NormalTask hidden = new NormalTask();
		hidden.setHiddenTask(true);

		assertTrue(filter.evaluate(NodeFactory.getInstance().createNode(visible)));
		assertFalse(filter.evaluate(NodeFactory.getInstance().createNode(hidden)));
	}
}
