package com.projectlibre1.pm.assignment.functor;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.collections.Closure;
import org.junit.jupiter.api.Test;

class PrintValueFunctorTest {
	@Test
	void executeDelegatesToWrappedClosure() {
		AtomicBoolean invoked = new AtomicBoolean(false);
		Closure child = new Closure() {
			public void execute(Object object) {
				invoked.set(true);
			}
		};

		PrintValueFunctor functor = PrintValueFunctor.getInstance(child);
		functor.execute(new Object());

		assertTrue(invoked.get());
	}
}
