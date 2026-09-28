/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.graphic.frames;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Waits for asynchronous project removal without blocking an AWT callback. */
final class QuitCompletionAwaiter {
	private QuitCompletionAwaiter() { }

	static boolean await(Object monitor, BooleanSupplier completed, long timeoutMillis)
			throws InterruptedException {
		Objects.requireNonNull(monitor, "monitor");
		Objects.requireNonNull(completed, "completed");
		if (timeoutMillis < 0L)
			throw new IllegalArgumentException("timeoutMillis must not be negative");
		long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
		synchronized (monitor) {
			while (!completed.getAsBoolean()) {
				long remainingNanos = deadline - System.nanoTime();
				if (remainingNanos <= 0L)
					return false;
				long waitMillis = TimeUnit.NANOSECONDS.toMillis(remainingNanos);
				int waitNanos = (int) (remainingNanos - TimeUnit.MILLISECONDS.toNanos(waitMillis));
				monitor.wait(Math.max(1L, waitMillis), Math.max(0, waitNanos));
			}
			return true;
		}
	}

	/** Waits on a daemon worker so a desktop quit callback never blocks the EDT. */
	static Thread awaitAsync(Object monitor, BooleanSupplier completed, long timeoutMillis,
			Consumer<Boolean> result) {
		Objects.requireNonNull(result, "result");
		Thread waiter = new Thread(() -> {
			boolean finished;
			try {
				finished = await(monitor, completed, timeoutMillis);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				finished = false;
			}
			result.accept(finished);
		}, "microProject-quit-wait");
		waiter.setDaemon(true);
		waiter.start();
		return waiter;
	}
}
