/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.testsupport;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/** Shared deterministic waits for non-headless Swing acceptance tests. */
public final class GuiAcceptanceSupport {
	private GuiAcceptanceSupport() {
	}

	public static void await(BooleanSupplier condition, String message) throws Exception {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		while (System.nanoTime() < deadline) {
			if (condition.getAsBoolean()) return;
			Thread.sleep(25);
		}
		assertTrue(false, message);
	}
}
