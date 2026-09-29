/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.job;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.microproject.util.UiDispatch;
import com.microproject.util.UiDispatcher;

class JobFailureExceptionTest {
	@AfterEach
	void restoreDispatcher() {
		UiDispatch.setDispatcher(null);
	}

	@Test
	void parentExceptionHandlerCanReadFailureFromComposedChildRunnable() throws InterruptedException {
		UiDispatch.setDispatcher(new UiDispatcher() {
			@Override public void invokeLater(Runnable task) { task.run(); }
			@Override public boolean isDispatchThread() { return true; }
		});
		JobQueue queue = new JobQueue("composed-job-test", false);
		Job parent = new Job(queue, "parent", "parent", false);
		Job child = new Job(queue, "child", "child", false);
		IOException expected = new IOException("save failed");
		CountDownLatch handled = new CountDownLatch(1);
		child.addRunnable(new JobRunnable("fail") {
			@Override public Object run() throws Exception { throw expected; }
		});
		parent.addJob(child);
		AtomicReference<Exception> observed = new AtomicReference<>();
		parent.addExceptionRunnable(new JobRunnable("observe failure") {
			@Override public Object run() {
				observed.set(parent.getFailureException());
				handled.countDown();
				return null;
			}
		});

		parent.execute();

		assertTrue(awaitHandler(handled), "the parent exception handler did not run");
		parent.join(5_000L);
		assertFalse(parent.isAlive(), "the composed job must finish before the dispatcher is restored");
		assertSame(expected, observed.get());
	}

	private static boolean awaitHandler(CountDownLatch handled) {
		try {
			return handled.await(5, TimeUnit.SECONDS);
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
			return false;
		}
	}
}
