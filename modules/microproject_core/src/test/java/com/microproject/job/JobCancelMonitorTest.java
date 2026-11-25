/*******************************************************************************
 * MIT License
 *
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.microproject.util.UiDispatch;
import com.microproject.util.UiDispatcher;

/**
 * Issue #1379: the job cancel monitor is a named platform thread built from a
 * Runnable. Cancelling the progress monitor must run the cancel closure once,
 * and the monitor thread must stop afterwards.
 */
class JobCancelMonitorTest {

	@AfterEach
	void restoreDispatcher() {
		UiDispatch.setDispatcher(null);
	}

	@Test
	void cancelingTheProgressMonitorRunsTheClosureOnceAndStopsTheChecker() throws Exception {
		UiDispatch.setDispatcher(new UiDispatcher() {
			@Override public void invokeLater(Runnable task) { task.run(); }
			@Override public boolean isDispatchThread() { return true; }
		});
		// The monitor thread returns early when the job is already canceled, so the
		// fake monitor reports cancellation only after its first few checks: the
		// monitor's own start check plus the job's checks before the runnable runs.
		AtomicInteger cancelChecks = new AtomicInteger();
		AtomicBoolean closed = new AtomicBoolean();
		JobProgressMonitor monitor = new JobProgressMonitor() {
			@Override public void setProgress(int progress) { }
			@Override public void setNote(String note) { }
			@Override public void close() { closed.set(true); }
			@Override public boolean isCanceled() { return cancelChecks.incrementAndGet() > 4; }
			@Override public boolean isClosed() { return closed.get(); }
		};
		JobQueue queue = new JobQueue("cancel-monitor-test", false) {
			@Override public JobProgressMonitor getProgressMonitor(String name, Object component) {
				return monitor;
			}
		};
		Job job = new Job(queue, "cancelMonitorJob", "Cancel monitor test", true);
		AtomicInteger closureCalls = new AtomicInteger();
		job.setCancelMonitorClosure(ignored -> closureCalls.incrementAndGet());
		CountDownLatch workStarted = new CountDownLatch(1);
		CountDownLatch releaseWork = new CountDownLatch(1);
		job.addRunnable(new JobRunnable("blocking work") {
			@Override public Object run() throws Exception {
				workStarted.countDown();
				releaseWork.await();
				return null;
			}
		});
		Thread checker;
		try {
			assertTrue(queue.trySchedule(job));
			assertTrue(workStarted.await(5, TimeUnit.SECONDS), "the job must reach its first runnable");
			checker = job.monitorChecker;
			assertNotNull(checker, "a progress monitor must install the cancel monitor");
			assertEquals("cancelMonitorJob_cancelMonitor", checker.getName());
			assertEquals(job.isDaemon(), checker.isDaemon(), "the monitor inherits the job's daemon setting");

			long deadline = System.currentTimeMillis() + 5000L;
			while (closureCalls.get() == 0 && System.currentTimeMillis() < deadline)
				Thread.sleep(20);
			assertEquals(1, closureCalls.get(), "cancelling must run the closure exactly once");
			assertFalse(closed.get(), "cancelling is not closing the monitor");
		} finally {
			releaseWork.countDown();
			job.join(5000);
		}
		checker.join(5000);
		assertFalse(checker.isAlive(), "the cancel monitor must stop after running the closure");
		assertEquals(1, closureCalls.get(), "the cancel monitor must not run the closure again");
	}
}
