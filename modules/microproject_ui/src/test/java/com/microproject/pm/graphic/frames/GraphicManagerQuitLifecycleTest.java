/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CountDownLatch;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

class GraphicManagerQuitLifecycleTest {
	@Test
	void quitWaitReturnsWhenRemovalSignalsCompletion() throws Exception {
		Object monitor = new Object();
		AtomicBoolean completed = new AtomicBoolean();
		Thread signal = new Thread(() -> {
			synchronized (monitor) {
				completed.set(true);
				monitor.notifyAll();
			}
		}, "quit-test-signal");
		signal.start();

		assertTrue(GraphicManager.awaitQuitCompletion(monitor, completed::get, 1000L));
		signal.join(1000L);
	}

	@Test
	void quitWaitIsBoundedWhenRemovalNeverSignals() throws Exception {
		long start = System.nanoTime();
		assertFalse(GraphicManager.awaitQuitCompletion(new Object(), () -> false, 25L));
		long elapsedMillis = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
		assertTrue(elapsedMillis < 500L, "bounded quit wait exceeded test budget: " + elapsedMillis + "ms");
	}

	@Test
	void quitWaitPreservesInterruption() {
		Thread.currentThread().interrupt();
		try {
			org.junit.jupiter.api.Assertions.assertThrows(InterruptedException.class,
					() -> GraphicManager.awaitQuitCompletion(new Object(), () -> false, 10_000L));
		} finally {
			Thread.interrupted();
		}
	}

	@Test
	void asyncQuitWaitDoesNotBlockTheEdt() throws Exception {
		Object monitor = new Object();
		AtomicBoolean completed = new AtomicBoolean();
		AtomicBoolean edtReturned = new AtomicBoolean();
		CountDownLatch callback = new CountDownLatch(1);
		AtomicBoolean result = new AtomicBoolean();
		SwingUtilities.invokeAndWait(() -> {
			GraphicManager.awaitQuitCompletionAsync(monitor, completed::get, 1000L,
					value -> {
						result.set(value);
						callback.countDown();
					});
			edtReturned.set(true);
		});
		assertTrue(edtReturned.get(), "EDT did not return after starting quit wait");
		synchronized (monitor) {
			completed.set(true);
			monitor.notifyAll();
		}
		assertTrue(callback.await(1, java.util.concurrent.TimeUnit.SECONDS));
		assertTrue(result.get());
	}
}
