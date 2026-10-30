/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.microproject.util.UiDispatch;
import com.microproject.util.UiDispatcher;

class JobQueueAdmissionTest {
	@AfterEach
	void restoreDispatcher() {
		UiDispatch.setDispatcher(null);
	}

	@Test
	void differentFileKeysSerializeWhileSameFileRequestsShareTheActiveResult() throws Exception {
		UiDispatch.setDispatcher(new UiDispatcher() {
			@Override public void invokeLater(Runnable task) { task.run(); }
			@Override public boolean isDispatchThread() { return true; }
		});
		JobQueue queue = new JobQueue("load-admission-serialization-test", false);
		Job parent = new Job(queue, "loadProject", "Parent load", false);
		Job child = new Job(queue, "loadProject", "Child load", false);
		parent.setQueueAdmissionKey("local-project-load:path:master.mpo");
		child.setQueueAdmissionKey("local-project-load:path:child.mpo");
		CountDownLatch parentStarted = new CountDownLatch(1);
		CountDownLatch releaseParent = new CountDownLatch(1);
		CountDownLatch parentDone = new CountDownLatch(1);
		CountDownLatch childStarted = new CountDownLatch(1);
		CountDownLatch releaseChild = new CountDownLatch(1);
		CountDownLatch childDone = new CountDownLatch(1);
		CountDownLatch coalesced = new CountDownLatch(1);
		AtomicInteger duplicateRuns = new AtomicInteger();
		AtomicReference<Object> joinedResult = new AtomicReference<>();
		Object loadedChild = new Object();
		parent.addRunnable(new JobRunnable("load parent") {
			@Override public Object run() throws Exception {
				parentStarted.countDown();
				releaseParent.await();
				return null;
			}
		});
		parent.addCompletionRunnable(parentDone::countDown);
		child.addRunnable(new JobRunnable("load child") {
			@Override public Object run() throws Exception {
				childStarted.countDown();
				releaseChild.await();
				return loadedChild;
			}
		});
		child.addCompletionRunnable(childDone::countDown);

		try {
			assertTrue(queue.trySchedule(parent));
			assertTrue(parentStarted.await(5, TimeUnit.SECONDS));
			assertTrue(queue.trySchedule(child), "different files with the same display name must be admitted");
			assertFalse(childStarted.await(150, TimeUnit.MILLISECONDS),
				"different file jobs still serialize through the shared critical section");

			Job duplicateChild = new Job(queue, "loadProject", "Duplicate child load", false);
			duplicateChild.setQueueAdmissionKey("local-project-load:path:child.mpo");
			duplicateChild.addRunnable(new JobRunnable("duplicate work") {
				@Override public Object run() {
					duplicateRuns.incrementAndGet();
					return null;
				}
			});
			assertFalse(queue.tryScheduleOrCoalesce(duplicateChild, active -> {
				joinedResult.set(active.getCompletionResult());
				coalesced.countDown();
			}), "same-file work must join its active load");

			releaseParent.countDown();
			assertTrue(childStarted.await(5, TimeUnit.SECONDS));
			releaseChild.countDown();
			assertTrue(parentDone.await(5, TimeUnit.SECONDS));
			assertTrue(childDone.await(5, TimeUnit.SECONDS));
			assertTrue(coalesced.await(5, TimeUnit.SECONDS));
			assertSame(loadedChild, joinedResult.get());
			assertEquals(0, duplicateRuns.get());
		} finally {
			releaseParent.countDown();
			releaseChild.countDown();
		}
	}

	@Test
	void retainedAdmissionCoalescesUntilEdtCompletionAndReleasesAfterReentrantAndExceptionalListeners() throws Exception {
		LinkedBlockingQueue<Runnable> edtQueue = new LinkedBlockingQueue<>();
		UiDispatch.setDispatcher(new UiDispatcher() {
			@Override public void invokeLater(Runnable task) { edtQueue.add(task); }
			@Override public boolean isDispatchThread() { return true; }
		});
		JobQueue queue = new JobQueue("retained-admission-test", false);
		Job first = new Job(queue, "loadProject", "First load", false);
		first.setQueueAdmissionKey("same-file");
		AtomicInteger admittedCount = new AtomicInteger();
		AtomicInteger coalescedCount = new AtomicInteger();
		AtomicReference<JobQueue.AdmissionTicket> firstTicket = new AtomicReference<>();
		CountDownLatch firstTerminal = new CountDownLatch(1);
		CountDownLatch reentrantCoalesced = new CountDownLatch(1);

		queue.scheduleOrJoinRetained(first, ticket -> {
			admittedCount.incrementAndGet();
			firstTicket.set(ticket);
			first.addCompletionRunnable(() -> UiDispatch.invokeLater(() -> {
				Job reentrant = new Job(queue, "loadProject", "Reentrant load", false);
				reentrant.setQueueAdmissionKey("same-file");
				// Key resolution leaves the EDT, so this request may join or follow the owner.
				queue.scheduleOrJoinRetained(reentrant, reentrantTicket -> reentrant.addCompletionRunnable(() -> {
						reentrantTicket.close();
						reentrantCoalesced.countDown();
					}),
					(active, joined) -> {
						joined.close();
						reentrantCoalesced.countDown();
					});
				firstTicket.get().close();
				firstTerminal.countDown();
			}));
		}, (active, joined) -> {
			coalescedCount.incrementAndGet();
			joined.close();
		});
		Runnable ownerUiCompletion = edtQueue.poll(5, TimeUnit.SECONDS);
		assertTrue(ownerUiCompletion != null, "terminal UI completion must be queued");
		Job duplicate = new Job(queue, "loadProject", "Duplicate load", false);
		duplicate.setQueueAdmissionKey("same-file");
		queue.scheduleOrJoinRetained(duplicate, ignored -> admittedCount.incrementAndGet(),
			(active, joined) -> {
				coalescedCount.incrementAndGet();
				joined.close();
			});
		// The EDT route resolves its admission key on a background thread.
		for (int wait = 0; wait < 500 && coalescedCount.get() < 1; wait++) Thread.sleep(10);
		assertEquals(1, admittedCount.get(), "a terminal job remains reserved until its UI callback releases it");
		assertEquals(1, coalescedCount.get());

		// Run the owner's delayed EDT callback. It synchronously starts a same-file
		// request before releasing its ticket, exercising reentrant admission.
		ownerUiCompletion.run();
		assertTrue(firstTerminal.await(5, TimeUnit.SECONDS));
		assertTrue(reentrantCoalesced.await(5, TimeUnit.SECONDS));

		// The remaining cases exercise listener failures on the synchronous route.
		UiDispatch.setDispatcher(new UiDispatcher() {
			@Override public void invokeLater(Runnable task) { task.run(); }
			@Override public boolean isDispatchThread() { return false; }
		});
		Job next = new Job(queue, "loadProject", "Next load", false);
		next.setQueueAdmissionKey("same-file");
		AtomicReference<JobQueue.AdmissionTicket> nextTicket = new AtomicReference<>();
		CountDownLatch nextTerminal = new CountDownLatch(1);
		queue.scheduleOrJoinRetained(next, ticket -> {
			admittedCount.incrementAndGet();
			nextTicket.set(ticket);
			next.addCompletionRunnable(nextTerminal::countDown);
		}, (active, joined) -> { throw new IllegalStateException("completion listener failed"); });
		assertTrue(nextTerminal.await(5, TimeUnit.SECONDS));
		Job failingDuplicate = new Job(queue, "loadProject", "Failing duplicate", false);
		failingDuplicate.setQueueAdmissionKey("same-file");
		assertThrows(IllegalStateException.class, () -> queue.scheduleOrJoinRetained(failingDuplicate,
			ignored -> admittedCount.incrementAndGet(), (active, joined) -> {
				throw new IllegalStateException("completion listener failed");
			}));
		nextTicket.get().close();
		Job afterFailure = new Job(queue, "loadProject", "After failure", false);
		afterFailure.setQueueAdmissionKey("same-file");
		CountDownLatch afterFailureDone = new CountDownLatch(1);
		queue.scheduleOrJoinRetained(afterFailure, ticket -> {
			admittedCount.incrementAndGet();
			afterFailure.addCompletionRunnable(() -> {
				ticket.close();
				afterFailureDone.countDown();
			});
		}, (active, joined) -> { throw new AssertionError("key should have been released"); });
		assertTrue(afterFailureDone.await(5, TimeUnit.SECONDS));
		assertEquals(3, admittedCount.get(), "the owner, the next load, and the load after failure are admitted");
	}

	@Test
	void canonicalAdmissionKeyResolverRunsOffEdtBeforeJobAdmission() throws Exception {
		UiDispatch.setDispatcher(new UiDispatcher() {
			@Override public void invokeLater(Runnable task) { SwingUtilities.invokeLater(task); }
			@Override public boolean isDispatchThread() { return SwingUtilities.isEventDispatchThread(); }
		});
		JobQueue queue = new JobQueue("canonical-admission-worker-test", false);
		Job job = new Job(queue, "loadProject", "Canonical load", false);
		CountDownLatch terminalUiCompletion = new CountDownLatch(1);
		AtomicBoolean resolverRanOnEdt = new AtomicBoolean(true);
		AtomicBoolean completionRanOnEdt = new AtomicBoolean(false);
		SwingUtilities.invokeAndWait(() -> queue.scheduleOrJoinRetained(job, () -> {
			resolverRanOnEdt.set(SwingUtilities.isEventDispatchThread());
			return "canonical:file";
		}, ticket -> job.addCompletionRunnable(() -> UiDispatch.invokeLater(() -> {
			completionRanOnEdt.set(SwingUtilities.isEventDispatchThread());
			ticket.close();
			terminalUiCompletion.countDown();
		})), (active, joined) -> joined.close()));

		assertTrue(terminalUiCompletion.await(5, TimeUnit.SECONDS));
		assertFalse(resolverRanOnEdt.get(), "real-path resolution must not block the EDT");
		assertTrue(completionRanOnEdt.get());
		assertEquals("canonical:file", job.getQueueAdmissionKey());
	}
}
