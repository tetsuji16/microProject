/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.core.nodes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AbstractNodeTest {
	@Test
	void propertyAndFieldValuesShareTheTypedBackingMap() {
		AbstractNode node = new AbstractNode();

		node.setPropertyValue("name", "Task");
		node.setFieldValue("priority", 3);

		assertEquals("Task", node.getPropertyValue("name"));
		assertEquals("Task", node.getFieldValue("Field.name"));
		assertEquals(3, node.getFieldValue("priority"));
	}
}
