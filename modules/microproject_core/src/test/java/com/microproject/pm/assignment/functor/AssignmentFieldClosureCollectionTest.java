package com.microproject.pm.assignment.functor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class AssignmentFieldClosureCollectionTest {
	@Test
	void fixedValueSumsOnlyCostFunctors() {
		CostFunctor costFunctor = CostFunctor.getInstance(null, null, null, 0.0D, null, 0L, false);
		costFunctor.fixedValue = 42.5D;
		AssignmentFieldFunctor otherFunctor = new AssignmentFieldFunctor() {
			@Override
			public void accept(Object object) {
			}
		};
		otherFunctor.value = 7.0D;

		AssignmentFieldClosureCollection collection = AssignmentFieldClosureCollection
				.getInstance(List.of(costFunctor, otherFunctor));

		assertEquals(42.5D, collection.getFixedValue(), 0.0D);
	}
}
