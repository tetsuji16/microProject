/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.core.dictionary;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Iterator;
import java.util.Set;

import org.junit.jupiter.api.Test;

class DictionaryTest {
	@Test
	void addIndexesCategorizedValuesInTheirCategoryAndAllCategory() {
		Dictionary dictionary = new Dictionary();
		TestValue value = new TestValue("planned", Set.of("task"));

		dictionary.add(value);

		assertSame(value, dictionary.get(new DictionaryCategory(TestValue.class, "task"), "planned"));
		assertSame(value, dictionary.get(TestValue.class, "planned"));
		assertArrayEquals(new Class<?>[] { TestValue.class }, dictionary.getClassesAsArray());
	}

	@Test
	void missingCategoryReturnsStandardEmptyIterator() {
		Iterator<HasStringId> iterator = new Dictionary().iterator(new DictionaryCategory(TestValue.class, "missing"));

		assertFalse(iterator.hasNext());
		assertThrows(java.util.NoSuchElementException.class, iterator::next);
		assertThrows(IllegalStateException.class, iterator::remove);
	}

	private static final class TestValue implements HasStringId, HasCategories {
		private String id;
		private final Set<String> categories;

		private TestValue(String id, Set<String> categories) {
			this.id = id;
			this.categories = categories;
		}

		@Override
		public String getId() {
			return id;
		}

		@Override
		public void setId(String id) {
			this.id = id;
		}

		@Override
		public Set<String> getCategories() {
			return categories;
		}

		@Override
		public void setCategories(Set<String> categories) {
			throw new UnsupportedOperationException();
		}
	}
}
