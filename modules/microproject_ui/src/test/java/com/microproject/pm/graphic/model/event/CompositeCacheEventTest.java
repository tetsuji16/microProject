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
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class CompositeCacheEventTest {
	@Test
	void diffGenerationDoesNotMutateSourceEventNodes() {
		List<Object> removed = new ArrayList<>(List.of("existing"));
		List<Object> inserted = new ArrayList<>(List.of("existing", "new"));
		CacheEvent removal = new CacheEvent(this, CacheEvent.NODES_REMOVED, removed, List.of());
		CacheEvent insertion = new CacheEvent(this, CacheEvent.NODES_INSERTED, inserted, List.of());
		CompositeCacheEvent composite = new CompositeCacheEvent(this, List.of(removal, insertion), List.of());

		assertEquals(List.of("existing"), composite.getUpdatedNodes());
		assertEquals(List.of("new"), composite.getInsertedNodes());
		assertEquals(List.of(), composite.getRemovedNodes());
		assertEquals(List.of("existing"), removed);
		assertEquals(List.of("existing", "new"), inserted);
	}

	@Test
	void nullNodePayloadAfterRemovalDoesNotAbortDiffGeneration() {
		CacheEvent removal = new CacheEvent(this, CacheEvent.NODES_REMOVED,
				new ArrayList<>(List.of("existing")), List.of());
		CacheEvent insertionWithoutNodes = new CacheEvent(this, CacheEvent.NODES_INSERTED, null, List.of());
		CompositeCacheEvent composite = new CompositeCacheEvent(this,
				List.of(removal, insertionWithoutNodes), List.of());

		assertEquals(List.of("existing"), composite.getRemovedNodes());
		assertNull(composite.getInsertedNodes());
		assertNull(composite.getUpdatedNodes());
	}
}
