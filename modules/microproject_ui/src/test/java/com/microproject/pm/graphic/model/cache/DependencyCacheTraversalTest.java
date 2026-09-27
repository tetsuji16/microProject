/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.graphic.model.cache;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DependencyCacheTraversalTest {
	@Test
	void nullChangeSetMeansNoIncrementalDependencyChanges() {
		DependencyCache cache = new DependencyCache();
		VisibleNodes nodes = new VisibleNodes("tasks", ignored -> { });
		VisibleDependencies dependencies = new VisibleDependencies("dependencies");
		dependencies.setVisibleNodes(nodes);
		GraphicDependency dependency = new GraphicDependency(null, null, null);
		Object key = new Object();
		cache.insertElement(dependency, key);
		GraphicDependency indexedDependency = cache.getElement(key);
		assertSame(dependency, indexedDependency);
		cache.addVisibleElements(dependencies);

		cache.updateVisibleElements(null);

		assertTrue(dependencies.getElements().isEmpty());
		assertTrue(dependencies.getEvents().isEmpty());
	}
}
