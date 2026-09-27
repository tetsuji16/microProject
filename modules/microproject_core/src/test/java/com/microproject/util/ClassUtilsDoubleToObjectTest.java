/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ClassUtilsDoubleToObjectTest {
	@Test
	void convertsDoubleToByteAndShortValues() {
		assertEquals(Byte.valueOf((byte) 12), ClassUtils.doubleToObject(12.75, Byte.class));
		assertEquals(Short.valueOf((short) -34), ClassUtils.doubleToObject(-34.75, Short.class));
	}
}
