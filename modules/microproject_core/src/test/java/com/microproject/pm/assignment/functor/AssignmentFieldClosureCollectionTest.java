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
package com.microproject.pm.assignment.functor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

class AssignmentFieldClosureCollectionTest {
	@Test
	void valueSumsClosuresAndCachesTheLastNonZeroFunctor() {
		AssignmentFieldFunctor first = functorWithValue(2.0D);
		AssignmentFieldFunctor lastNonZero = functorWithValue(3.0D);
		AssignmentFieldFunctor zero = functorWithValue(0.0D);
		AssignmentFieldClosureCollection collection = AssignmentFieldClosureCollection
				.getInstance(List.of(first, lastNonZero, zero));

		assertEquals(5.0D, collection.getValue(), 0.0D);
		assertSame(lastNonZero, collection.getANonZeroFunctor());

		lastNonZero.value = 0.0D;
		assertEquals(2.0D, collection.getValue(), 0.0D);
		assertSame(first, collection.getANonZeroFunctor());
	}

	@Test
	void calculationsRetainTheSuppliedCollectionView() {
		AssignmentFieldFunctor first = functorWithValue(2.0D);
		AssignmentFieldFunctor addedLater = functorWithValue(3.0D);
		List<Object> suppliedClosures = new ArrayList<>(List.of(first));
		AssignmentFieldClosureCollection collection = AssignmentFieldClosureCollection.getInstance(suppliedClosures);

		suppliedClosures.add(addedLater);

		assertEquals(5.0D, collection.getValue(), 0.0D);
	}

	@Test
	void acceptRetainsSupportForConsumerOnlyEntries() {
		AtomicReference<Object> received = new AtomicReference<>();
		Consumer<Object> consumer = received::set;
		AssignmentFieldClosureCollection collection = AssignmentFieldClosureCollection
				.getInstance(List.of(consumer));

		collection.accept("interval");

		assertEquals("interval", received.get());
	}

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

	private static AssignmentFieldFunctor functorWithValue(double value) {
		AssignmentFieldFunctor functor = new AssignmentFieldFunctor() {
			@Override
			public void accept(Object object) {
			}
		};
		functor.value = value;
		return functor;
	}
}
