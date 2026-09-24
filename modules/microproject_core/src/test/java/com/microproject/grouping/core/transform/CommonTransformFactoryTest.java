/*
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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 */
package com.microproject.grouping.core.transform;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.microproject.grouping.core.transform.filtering.BaseFilter;

class CommonTransformFactoryTest {
	@Test
	void instantiatesConfiguredTransformAndPassesArguments() throws Exception {
		var factory = new TestFactory();
		factory.setDefinition(BaseFilter.class.getName());
		factory.setArguments("true");

		var transform = (BaseFilter) factory.getTransformFromDefinition();

		assertTrue(transform.isShowAssignments());
	}

	@Test
	void passesNullArgumentsThroughToStringConstructor() throws Exception {
		var factory = new TestFactory();
		factory.setDefinition(BaseFilter.class.getName());

		var transform = (BaseFilter) factory.getTransformFromDefinition();

		assertFalse(transform.isShowAssignments());
	}

	private static final class TestFactory extends CommonTransformFactory {
		@Override
		public CommonTransform getTransform() throws com.microproject.field.InvalidFormulaException {
			return getTransformFromDefinition();
		}
	}
}
