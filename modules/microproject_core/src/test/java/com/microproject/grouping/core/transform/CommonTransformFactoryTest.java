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
