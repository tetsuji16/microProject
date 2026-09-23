/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.grouping.core.transform.filtering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.grouping.core.NodeFactory;
import com.microproject.pm.task.NormalTask;

class BaseFilterTest {
	@Test
	void filterListPreservesElementTypeIdentityOrderAndInPlaceMutation() {
		NodeFilter evenNumbers = new NodeFilter() {
			@Override
			public boolean evaluate(Object value) {
				return value instanceof Integer number && number % 2 == 0;
			}
		};
		List<Integer> values = new ArrayList<>(List.of(1, 2, 3, 4));

		List<Integer> filtered = evenNumbers.filterList(values);

		assertSame(values, filtered);
		assertEquals(List.of(2, 4), filtered);
	}

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
