package com.microproject.core.dictionary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class DictionaryCategoryTest {
	@Test
	void equalityIncludesCategoryAndMatchesHashCode() {
		DictionaryCategory first = new DictionaryCategory(String.class, "first");
		DictionaryCategory same = new DictionaryCategory(String.class, "first");
		DictionaryCategory otherCategory = new DictionaryCategory(String.class, "other");

		assertEquals(first, same);
		assertEquals(first.hashCode(), same.hashCode());
		assertNotEquals(first, otherCategory);
	}
}
