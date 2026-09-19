/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.graphic.chart;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.microproject.pm.graphic.model.cache.NodeModelCache;

class ChartInfoCacheTest {
	@Test
	void firstCacheInstallationDoesNotRemoveFromANullPreviousCache() {
		AtomicInteger updates = new AtomicInteger();
		AtomicInteger additions = new AtomicInteger();
		AtomicInteger removals = new AtomicInteger();
		NodeModelCache cache = cache(updates, additions, removals);

		ChartInfo info = new ChartInfo();
		assertDoesNotThrow(() -> info.setCache(cache));
		assertEquals(1, updates.get());
		assertEquals(1, additions.get());
		assertEquals(0, removals.get());
	}

	@Test
	void replacingTheCacheDetachesOnlyThePreviousCache() {
		AtomicInteger oldRemovals = new AtomicInteger();
		ChartInfo info = new ChartInfo();
		info.setCache(cache(new AtomicInteger(), new AtomicInteger(), oldRemovals));
		AtomicInteger newRemovals = new AtomicInteger();
		info.setCache(cache(new AtomicInteger(), new AtomicInteger(), newRemovals));

		assertEquals(1, oldRemovals.get());
		assertEquals(0, newRemovals.get());
	}

	private static NodeModelCache cache(AtomicInteger updates, AtomicInteger additions, AtomicInteger removals) {
		return (NodeModelCache) Proxy.newProxyInstance(ChartInfoCacheTest.class.getClassLoader(),
			new Class<?>[] { NodeModelCache.class }, (proxy, method, args) -> {
				if ("update".equals(method.getName())) updates.incrementAndGet();
				if ("addNodeModelListener".equals(method.getName())) additions.incrementAndGet();
				if ("removeNodeModelListener".equals(method.getName())) removals.incrementAndGet();
				Class<?> result = method.getReturnType();
				if (!result.isPrimitive()) return null;
				if (result == boolean.class) return false;
				if (result == char.class) return '\0';
				return 0;
			});
	}
}
