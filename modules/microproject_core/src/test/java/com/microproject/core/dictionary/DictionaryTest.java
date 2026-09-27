/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.core.dictionary;

import static org.junit.jupiter.api.Assertions.assertSame;

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
