/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ClassUtilsSetSimplePropertyTest {
	@Test
	void setsWritableJavaBeanProperty() {
		var bean = new TestBean();

		assertTrue(ClassUtils.setSimpleProperty(bean, "name", "Renamed"));

		assertEquals("Renamed", bean.getName());
	}

	@Test
	void returnsFalseForMissingOrInvalidSimpleProperty() {
		var bean = new TestBean();

		assertFalse(ClassUtils.setSimpleProperty(bean, "missing", "value"));
		assertFalse(ClassUtils.setSimpleProperty(bean, "name", 42));
		assertFalse(ClassUtils.setSimpleProperty(null, "name", "value"));
		assertEquals("original", bean.getName());
	}

	public static final class TestBean {
		private String name = "original";

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
