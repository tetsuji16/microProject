/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package com.microproject.pm.graphic.model.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.pm.graphic.model.event.CacheEvent;

class NodeCacheEventTest {
	@Test
	void updateDiffsAreCacheEventsInRemovalThenInsertionOrder() {
		Object source = new Object();
		List<CacheEvent> events = new ArrayList<>();

		NodeCache.applyUpdates(new ArrayList<>(List.of("removed")),
				new ArrayList<>(List.of("inserted")), null, events, source);

		assertEquals(2, events.size());
		assertSame(source, events.get(0).getSource());
		assertEquals(CacheEvent.NODES_REMOVED, events.get(0).getType());
		assertEquals(List.of("removed"), events.get(0).getNodes());
		assertEquals(CacheEvent.NODES_INSERTED, events.get(1).getType());
		assertEquals(List.of("inserted"), events.get(1).getNodes());
	}

	@Test
	void visibleElementsStoreAndClearTypedCacheEvents() {
		VisibleDependencies visibleDependencies = new VisibleDependencies("event-test");
		CacheEvent event = new CacheEvent(this, CacheEvent.NODES_CHANGED, List.of("node"), List.of());

		visibleDependencies.addEvent(event);

		assertSame(event, visibleDependencies.getEvents().get(0));
		visibleDependencies.clearEvents();
		assertEquals(List.of(), visibleDependencies.getEvents());
	}
}
