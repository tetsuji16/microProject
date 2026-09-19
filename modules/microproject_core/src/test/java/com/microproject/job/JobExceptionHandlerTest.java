/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
 * Copyright (c) 2026 microProject
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package com.microproject.job;

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
		java.util.concurrent.atomic.AtomicReference<Exception> observedFailure = new java.util.concurrent.atomic.AtomicReference<>();
		job.addRunnable(new JobRunnable("fail") {
			public Object run() throws Exception {
				throw new Exception("expected");
			}
		});
		job.addExceptionRunnable(new JobRunnable("handle") {
			public Object run() {
				observedFailure.set(job.getFailureException());
				handled.countDown();
				return null;
			}
		});

		queue.schedule(job);

		assertTrue(handled.await(5, TimeUnit.SECONDS));
		assertEquals("expected", observedFailure.get().getMessage());
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
