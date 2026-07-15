package com.projectlibre1.job;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class JobExceptionHandlerTest {
	@Test
	void exceptionHandlerRunsAfterARunnableFailureCancelsTheJob() throws Exception {
		JobQueue queue = new JobQueue("exception-handler-test", false);
		Job job = new Job(queue, "failing-job", "Failing job", false);
		CountDownLatch handled = new CountDownLatch(1);
		job.addRunnable(new JobRunnable("fail") {
			public Object run() throws Exception {
				throw new Exception("expected");
			}
		});
		job.addExceptionRunnable(new JobRunnable("handle") {
			public Object run() {
				handled.countDown();
				return null;
			}
		});

		queue.schedule(job);

		assertTrue(handled.await(5, TimeUnit.SECONDS));
	}

	@Test
	void completionRunsExactlyOnceAfterExplicitCancellation() throws Exception {
		JobQueue queue = new JobQueue("completion-cancel-test", false);
		Job job = new Job(queue, "cancelled-job", "Cancelled job", false);
		CountDownLatch started = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		CountDownLatch completed = new CountDownLatch(1);
		AtomicInteger completionCount = new AtomicInteger();
		job.addRunnable(new JobRunnable("wait") {
			public Object run() throws Exception {
				started.countDown();
				release.await(5, TimeUnit.SECONDS);
				return null;
			}
		});
		job.addCompletionRunnable(() -> {
			completionCount.incrementAndGet();
			completed.countDown();
		});

		queue.schedule(job);
		assertTrue(started.await(5, TimeUnit.SECONDS));
		job.cancel();
		release.countDown();

		assertTrue(completed.await(5, TimeUnit.SECONDS));
		assertEquals(1, completionCount.get());
	}

	@Test
	void completionRunsAfterFailureEvenWhenWorkRemainsAfterTheExceptionHandler() throws Exception {
		JobQueue queue = new JobQueue("completion-failure-test", false);
		Job job = new Job(queue, "failing-job-with-tail", "Failing job", false);
		CountDownLatch handled = new CountDownLatch(1);
		CountDownLatch completed = new CountDownLatch(1);
		AtomicInteger trailingRunCount = new AtomicInteger();
		AtomicInteger completionCount = new AtomicInteger();
		job.addRunnable(new JobRunnable("fail") {
			public Object run() throws Exception {
				throw new Exception("expected");
			}
		});
		job.addExceptionRunnable(new JobRunnable("handle") {
			public Object run() {
				handled.countDown();
				return null;
			}
		});
		job.addRunnable(new JobRunnable("must-not-run") {
			public Object run() {
				trailingRunCount.incrementAndGet();
				return null;
			}
		});
		job.addCompletionRunnable(() -> {
			completionCount.incrementAndGet();
			completed.countDown();
		});

		queue.schedule(job);

		assertTrue(handled.await(5, TimeUnit.SECONDS));
		assertTrue(completed.await(5, TimeUnit.SECONDS));
		assertEquals(0, trailingRunCount.get());
		assertEquals(1, completionCount.get());
	}
}
