/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.testsupport;

import java.time.Duration;
import java.util.function.BooleanSupplier;

/** Condition wait for tests: replaces fixed sleeps with a bounded poll (issue #1052). */
public final class TestAwait {
	private static final long POLL_MILLIS = 20L;

	private TestAwait() {
	}

	/**
	 * Polls {@code condition} every 20 ms until it holds and returns. Fails with an
	 * {@link AssertionError} naming {@code description} when {@code timeout} elapses first.
	 */
	public static void awaitTrue(BooleanSupplier condition, Duration timeout, String description)
			throws InterruptedException {
		long deadline = System.nanoTime() + timeout.toNanos();
		while (true) {
			if (condition.getAsBoolean()) return;
			long remaining = deadline - System.nanoTime();
			if (remaining <= 0L) break;
			java.util.concurrent.TimeUnit.MILLISECONDS.sleep(Math.min(POLL_MILLIS, Math.max(1L, remaining / 1_000_000L)));
		}
		if (condition.getAsBoolean()) return;
		throw new AssertionError("Timed out after " + timeout + " waiting for: " + description);
	}
}
