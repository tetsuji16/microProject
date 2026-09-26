/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.graphic.model.cache;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DependencyCacheTraversalTest {
	@Test
	void nullChangeSetMeansNoIncrementalDependencyChanges() {
		DependencyCache cache = new DependencyCache();
		VisibleNodes nodes = new VisibleNodes("tasks", ignored -> { });
		VisibleDependencies dependencies = new VisibleDependencies("dependencies");
		dependencies.setVisibleNodes(nodes);
		cache.addVisibleElements(dependencies);

		cache.updateVisibleElements(null);

		assertTrue(dependencies.getElements().isEmpty());
		assertTrue(dependencies.getEvents().isEmpty());
	}
}
