/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.core.fields;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

class FieldUtilTest {
	@Test
	void getCategoriesPreservesTypeHierarchyOrder() {
		assertArrayEquals(
				new String[] { Leaf.class.getName(), Parent.class.getName(), Object.class.getName() },
				FieldUtil.getCategories(Leaf.class));
	}

	private static class Parent {
	}

	private static final class Leaf extends Parent {
	}
}
