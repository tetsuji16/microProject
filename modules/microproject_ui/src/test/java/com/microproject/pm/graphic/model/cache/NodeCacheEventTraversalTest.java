/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.graphic.model.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.pm.graphic.model.event.CacheEvent;

class NodeCacheEventTraversalTest {
	@Test
	void firesVisibleNodeEventsInRegistrationOrderAndClearsThem() {
		NodeCache cache = new NodeCache();
		List<String> delivered = new ArrayList<>();
		VisibleNodes first = visibleNodes("first");
		VisibleNodes second = visibleNodes("second");
		first.addNodeModelListener(event -> delivered.add("first"));
		second.addNodeModelListener(event -> delivered.add("second"));
		cache.addVisibleElements(first);
		cache.addVisibleElements(second);

		cache.fireEvents(this);

		assertEquals(List.of("first", "second"), delivered);
		assertTrue(first.getEvents().isEmpty());
		assertTrue(second.getEvents().isEmpty());
	}

	private static VisibleNodes visibleNodes(String name) {
		VisibleNodes nodes = new VisibleNodes(name, ignored -> { });
		nodes.setVisibleDependencies(new VisibleDependencies(name));
		nodes.addEvent(new CacheEvent(nodes, CacheEvent.NODES_CHANGED, List.of(name), List.of()));
		return nodes;
	}
}
