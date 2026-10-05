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
import java.util.concurrent.locks.LockSupport;

import org.junit.jupiter.api.Test;

class JobQueueCriticalSectionTest {
	@Test
	void reportsScheduledExecutionsThatRunOutsideItsThreadGroup() {
		JobQueue queue = new JobQueue("executing-job-report-test", false);
		Job job = new Job(queue, "scheduled", "Scheduled", false);

		assertFalse(queue.hasExecutingJobs());
		queue.addExecutingJob(job);
		assertTrue(queue.hasExecutingJobs());
		queue.removeExecutingJob(job);
		assertFalse(queue.hasExecutingJobs());
	}

	@Test
	void cancelCancelsRunningJobsInItsThreadGroup() throws Exception {
		JobQueue queue = new JobQueue("cancel-running-job-test", false);
		CountDownLatch started = new CountDownLatch(1);
		CountDownLatch finish = new CountDownLatch(1);
		Job job = new Job(queue, "running", "Running", false) {
			@Override
			public void run() {
				started.countDown();
				try {
					finish.await(5, TimeUnit.SECONDS);
				} catch (InterruptedException interrupted) {
					Thread.currentThread().interrupt();
				}
			}
		};
		job.start();
		try {
			assertTrue(started.await(5, TimeUnit.SECONDS));
			queue.cancel();
			assertTrue(job.isCanceled());
		} finally {
			finish.countDown();
			job.join(5_000L);
		}
		assertFalse(job.isAlive());
	}

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
		awaitCriticalSectionWait(thread);
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
		awaitCriticalSectionWait(thread);
		cancelled.cancel();
		thread.join(5_000L);

		assertFalse(thread.isAlive());
		assertFalse(acquired.get());
		queue.endCriticalSection(owner);
		assertTrue(queue.tryBeginCriticalSection(next));
		queue.endCriticalSection(next);
	}

	private static void awaitCriticalSectionWait(Thread thread) throws InterruptedException {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		while (thread.isAlive() && thread.getState() != Thread.State.TIMED_WAITING
				&& System.nanoTime() < deadline) {
			LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
		}
		assertTrue(thread.getState() == Thread.State.TIMED_WAITING,
			"waiter must reach the queue's timed critical-section wait");
	}
}
