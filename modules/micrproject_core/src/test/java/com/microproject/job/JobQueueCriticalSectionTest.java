/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.job;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

class JobQueueCriticalSectionTest {
	@Test
	void interruptedWaiterDoesNotBecomeCriticalSectionOwner() throws Exception {
		JobQueue queue = new JobQueue("critical-section-interruption-test", false);
		Job owner = new Job(queue, "owner", "Owner", false);
		Job waiter = new Job(queue, "waiter", "Waiter", false);
		assertTrue(queue.tryBeginCriticalSection(owner));

		CountDownLatch waiting = new CountDownLatch(1);
		AtomicBoolean acquired = new AtomicBoolean(true);
		AtomicBoolean interruptPreserved = new AtomicBoolean(false);
		Thread thread = new Thread(() -> {
			waiting.countDown();
			acquired.set(queue.tryBeginCriticalSection(waiter));
			interruptPreserved.set(Thread.currentThread().isInterrupted());
		}, "critical-section-waiter");
		thread.start();
		assertTrue(waiting.await(5, TimeUnit.SECONDS));
		// Ensure the waiter has entered the wait before interrupting it.
		Thread.sleep(50L);
		thread.interrupt();
		thread.join(5_000L);

		assertFalse(thread.isAlive());
		assertFalse(acquired.get());
		assertTrue(interruptPreserved.get());
		queue.endCriticalSection(owner);
	}

	@Test
	void cancelledWaiterLeavesSectionAvailableToTheNextJob() throws Exception {
		JobQueue queue = new JobQueue("critical-section-cancel-test", false);
		Job owner = new Job(queue, "owner", "Owner", false);
		Job cancelled = new Job(queue, "cancelled", "Cancelled", false);
		Job next = new Job(queue, "next", "Next", false);
		assertTrue(queue.tryBeginCriticalSection(owner));

		CountDownLatch waiting = new CountDownLatch(1);
		AtomicBoolean acquired = new AtomicBoolean(true);
		Thread thread = new Thread(() -> {
			waiting.countDown();
			acquired.set(queue.tryBeginCriticalSection(cancelled));
		}, "critical-section-cancelled-waiter");
		thread.start();
		assertTrue(waiting.await(5, TimeUnit.SECONDS));
		Thread.sleep(50L);
		cancelled.cancel();
		thread.join(5_000L);

		assertFalse(thread.isAlive());
		assertFalse(acquired.get());
		queue.endCriticalSection(owner);
		assertTrue(queue.tryBeginCriticalSection(next));
		queue.endCriticalSection(next);
	}
}
