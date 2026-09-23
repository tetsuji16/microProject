package com.microproject.server.data.linker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class LinkerTest {
	@Test
	void initClearsPriorTransformationsAndRebuildsTypedResults() throws Exception {
		TestLinker linker = new TestLinker(List.of("one", "two"));
		linker.seedStaleState();

		linker.init();
		assertTrue(linker.hasNext());
		linker.addTransformedObjects();

		assertEquals(Map.of("one", "ONE", "two", "TWO"), linker.getTransformationMap());
		assertEquals(List.of("ONE", "TWO"), linker.getTransformed());
		assertFalse(linker.hasNext());

		linker.init();
		assertTrue(linker.hasNext());
		assertEquals(Map.of(), linker.getTransformationMap());
		assertEquals(List.of(), linker.getTransformed());
	}

	private static final class TestLinker extends Linker {
		private final List<String> source;

		private TestLinker(List<String> source) {
			this.source = source;
		}

		private void seedStaleState() {
			transformationMap.put("stale", "old");
			transformed.add("old-result");
		}

		@Override
		protected void initIterator() {
			Iterator<String> sourceIterator = source.iterator();
			iterator = sourceIterator;
		}

		@Override
		public Object executeNext() {
			return iterator.next();
		}

		@Override
		public Object addTransformedObjects(Object child) {
			String transformedValue = ((String) child).toUpperCase();
			transformationMap.put(child, transformedValue);
			return transformedValue;
		}
	}
}
