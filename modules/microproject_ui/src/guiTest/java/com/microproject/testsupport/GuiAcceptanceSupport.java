/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.testsupport;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;
import javax.swing.SwingUtilities;

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

	/**
	 * Runs Swing cleanup without allowing a native-window/EDT deadlock to hang a
	 * whole GUI test worker forever.  The action is still executed on the EDT;
	 * the timeout only bounds the caller's wait and leaves the failure observable.
	 */
	public static void runOnEdtWithTimeout(Runnable action, String description) throws Exception {
		if (SwingUtilities.isEventDispatchThread()) {
			action.run();
			return;
		}
		FutureTask<Void> task = new FutureTask<>(() -> {
			action.run();
			return null;
		});
		SwingUtilities.invokeLater(task);
		try {
			task.get(5, TimeUnit.SECONDS);
		} catch (TimeoutException exception) {
			task.cancel(true);
			throw new AssertionError(description + " timed out on the EDT", exception);
		} catch (ExecutionException exception) {
			Throwable cause = exception.getCause();
			if (cause instanceof Exception checked) throw checked;
			if (cause instanceof Error error) throw error;
			throw new AssertionError(description + " failed on the EDT", cause);
		}
	}
}
