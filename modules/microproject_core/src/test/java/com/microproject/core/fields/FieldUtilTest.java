/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.core.fields;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FieldUtilTest {
	@Test
	void getCategoriesPreservesTypeHierarchyOrder() {
		assertArrayEquals(
				new String[] { Leaf.class.getName(), Parent.class.getName(), Object.class.getName() },
				FieldUtil.getCategories(Leaf.class));
	}

	@Test
	void convertFieldSkipsFalseBooleanButWritesTrue() {
		BooleanProperties properties = new BooleanProperties();
		BooleanTarget target = new BooleanTarget();

		properties.value = false;
		FieldUtil.convertField(properties, BooleanTarget.class, target, "enabled", -1, "enabled", -1, null, false);
		assertNull(target.enabled);

		properties.value = true;
		FieldUtil.convertField(properties, BooleanTarget.class, target, "enabled", -1, "enabled", -1, null, false);
		assertTrue(target.enabled);
	}

	private static final class BooleanProperties implements HasFields {
		private Boolean value;

		@Override
		public Object getPropertyValue(String property) {
			return value;
		}

		@Override
		public void setPropertyValue(String property, Object value) {
			this.value = (Boolean) value;
		}

		@Override
		public Object getFieldValue(String fieldId) {
			return value;
		}

		@Override
		public void setFieldValue(String fieldId, Object value) {
			this.value = (Boolean) value;
		}
	}

	private static final class BooleanTarget {
		private Boolean enabled;

		public void setEnabled(Boolean enabled) {
			this.enabled = enabled;
		}
	}

	private static class Parent {
	}

	private static final class Leaf extends Parent {
	}
}
