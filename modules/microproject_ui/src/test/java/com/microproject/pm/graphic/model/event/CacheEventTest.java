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
package com.microproject.pm.graphic.model.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class CacheEventTest {
	@Test
	void visitsRemovalIntervalsInReverseAndOtherIntervalsForward() {
		List<Object> intervals = new ArrayList<>(List.of("first", "second", "third"));
		List<Object> visited = new ArrayList<>();
		CacheEvent removal = new CacheEvent(this, CacheEvent.NODES_REMOVED, List.of(), intervals);
		CacheEvent insertion = new CacheEvent(this, CacheEvent.NODES_INSERTED, List.of(), intervals);

		removal.forIntervals(visited::add);
		assertEquals(List.of("third", "second", "first"), visited);

		visited.clear();
		insertion.forIntervals(visited::add);
		assertEquals(List.of("first", "second", "third"), visited);
	}

	@Test
	void keepsTheNodeListReferenceWhenReplaced() {
		List<Object> nodes = new ArrayList<>();
		CacheEvent event = new CacheEvent(this, CacheEvent.NODES_CHANGED, nodes, List.of());
		assertSame(nodes, event.getNodes());

		List<Object> replacement = new ArrayList<>();
		event.setNodes(replacement);
		assertSame(replacement, event.getNodes());
	}
}
