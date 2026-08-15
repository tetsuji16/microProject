package com.microproject.pm.assignment.functor;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

class PrintValueFunctorTest {
	@Test
	void executeDelegatesToWrappedClosure() {
		AtomicBoolean invoked = new AtomicBoolean(false);
		Consumer<Object> child = new Consumer<Object>() { public void accept(Object object) {
				invoked.set(true);
			}
		};

		PrintValueFunctor functor = PrintValueFunctor.getInstance(child);
		functor.accept(new Object());

		assertTrue(invoked.get());
	}
}
