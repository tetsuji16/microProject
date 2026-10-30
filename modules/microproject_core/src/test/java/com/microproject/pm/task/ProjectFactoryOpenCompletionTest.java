/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.microproject.job.Job;
import com.microproject.job.JobQueue;
import com.microproject.job.JobRunnable;
import com.microproject.session.LoadOptions;
import com.microproject.util.UiDispatch;
import com.microproject.util.UiDispatcher;

class ProjectFactoryOpenCompletionTest {
	@AfterEach
	void restoreDispatcher() {
		UiDispatch.setDispatcher(null);
	}

	@Test
	void cancellationCompletesOpenOnceOnTheUiThreadAndClearsLoadingState() throws Exception {
		useSwingDispatcher();
		JobQueue queue = new JobQueue("open-cancel-completion-test", false);
		Job job = new Job(queue, "open-cancel", "Open", false);
		LoadOptions options = new LoadOptions();
		CountDownLatch started = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		CountDownLatch completed = new CountDownLatch(1);
		AtomicInteger callbacks = new AtomicInteger();
		AtomicInteger cleanups = new AtomicInteger();
		AtomicReference<Object> result = new AtomicReference<>(new Object());
		AtomicReference<Boolean> onUiThread = new AtomicReference<>(false);
		options.setEndSwingClosure(value -> {
			callbacks.incrementAndGet();
			result.set(value);
			onUiThread.set(SwingUtilities.isEventDispatchThread());
			completed.countDown();
		});
		ProjectFactory.registerOpenProjectCompletion(job, options, cleanups::incrementAndGet);
		job.addRunnable(new JobRunnable("wait for cancellation") {
			@Override public Object run() throws Exception {
				started.countDown();
				release.await();
				return null;
			}
		});

		queue.schedule(job);
		assertTrue(started.await(5, TimeUnit.SECONDS));
		job.cancel();
		release.countDown();

		assertTrue(completed.await(5, TimeUnit.SECONDS));
		job.join(5_000L);
		assertEquals(1, callbacks.get());
		assertEquals(1, cleanups.get());
		assertSame(null, result.get());
		assertEquals(Boolean.TRUE, onUiThread.get());
	}

	@Test
	void normalOpenResultAndTerminalFallbackShareTheSameOneShotCallback() throws Exception {
		useSwingDispatcher();
		JobQueue queue = new JobQueue("open-success-completion-test", false);
		Job job = new Job(queue, "open-success", "Open", false);
		LoadOptions options = new LoadOptions();
		CountDownLatch completed = new CountDownLatch(1);
		AtomicInteger callbacks = new AtomicInteger();
		AtomicReference<Object> result = new AtomicReference<>();
		Project project = Project.getDummy();
		options.setEndSwingClosure(value -> {
			callbacks.incrementAndGet();
			result.set(value);
			completed.countDown();
		});
		var completion = ProjectFactory.registerOpenProjectCompletion(job, options, null);
		job.addSwingRunnable(new JobRunnable("complete open") {
			@Override public Object run() throws Exception {
				return ProjectFactory.finishProjectOpen(project, completion, ignored -> { });
			}
		});
		assertTrue(ProjectFactory.scheduleOpenProjectJob(queue, job, completion));

		assertTrue(completed.await(5, TimeUnit.SECONDS));
		job.join(5_000L);
		assertEquals(1, callbacks.get());
		assertSame(project, result.get());
	}

	@Test
	void successfulQueuedSwingCompletionIsNotReplacedByPrematureTerminalNull() throws Exception {
		useSwingDispatcher();
		JobQueue queue = new JobQueue("open-blocked-edt-completion-test", false);
		Job job = new Job(queue, "open-blocked-edt", "Open", false);
		LoadOptions options = new LoadOptions();
		CountDownLatch edtBlocked = new CountDownLatch(1);
		CountDownLatch releaseEdt = new CountDownLatch(1);
		CountDownLatch terminal = new CountDownLatch(1);
		CountDownLatch completed = new CountDownLatch(1);
		AtomicInteger callbacks = new AtomicInteger();
		AtomicReference<Object> result = new AtomicReference<>();
		Project project = Project.getDummy();
		options.setEndSwingClosure(value -> {
			callbacks.incrementAndGet();
			result.set(value);
			completed.countDown();
		});
		var completion = ProjectFactory.registerOpenProjectCompletion(job, options, null);
		job.addSwingRunnable(new JobRunnable("finish open") {
			@Override public Object run() throws Exception {
				return ProjectFactory.finishProjectOpen(project, completion, ignored -> { });
			}
		});
		job.addCompletionRunnable(terminal::countDown);
		SwingUtilities.invokeLater(() -> {
			edtBlocked.countDown();
			try {
				releaseEdt.await();
			} catch (InterruptedException interrupted) {
				Thread.currentThread().interrupt();
			}
		});
		assertTrue(edtBlocked.await(5, TimeUnit.SECONDS));

		try {
			assertTrue(ProjectFactory.scheduleOpenProjectJob(queue, job, completion));
			assertTrue(terminal.await(5, TimeUnit.SECONDS), "job did not reach terminal while the EDT was blocked");
			assertEquals(0, callbacks.get(), "the queued Swing completion must remain pending until the EDT is released");
		} finally {
			releaseEdt.countDown();
		}

		assertTrue(completed.await(5, TimeUnit.SECONDS));
		assertTrue(terminal.await(5, TimeUnit.SECONDS));
		job.join(5_000L);
		assertEquals(1, callbacks.get());
		assertSame(project, result.get());
	}

	@Test
	void edtRegistrationFailureCompletesNullAndCleansUpExactlyOnce() throws Exception {
		CountDownLatch registrationQueued = new CountDownLatch(1);
		useSwingDispatcher(registrationQueued);
		JobQueue queue = new JobQueue("open-edt-failure-completion-test", false);
		Job job = new Job(queue, "open-edt-failure", "Open", false);
		LoadOptions options = new LoadOptions();
		CountDownLatch edtBlocked = new CountDownLatch(1);
		CountDownLatch releaseEdt = new CountDownLatch(1);
		CountDownLatch completed = new CountDownLatch(1);
		CountDownLatch terminal = new CountDownLatch(1);
		AtomicInteger callbacks = new AtomicInteger();
		AtomicInteger cleanups = new AtomicInteger();
		AtomicReference<Object> result = new AtomicReference<>(new Object());
		AtomicReference<Boolean> onUiThread = new AtomicReference<>(false);
		options.setEndSwingClosure(value -> {
			callbacks.incrementAndGet();
			result.set(value);
			onUiThread.set(SwingUtilities.isEventDispatchThread());
			completed.countDown();
		});
		var completion = ProjectFactory.registerOpenProjectCompletion(job, options, cleanups::incrementAndGet);
		job.addSwingRunnable(new JobRunnable("initialize open") {
			@Override public Object run() throws Exception {
				return ProjectFactory.finishProjectOpen(null, completion, ignored -> {
					throw new IllegalStateException("injected EDT registration failure");
				});
			}
		});
		job.addExceptionRunnable(new JobRunnable("report registration failure") {
			@Override public Object run() {
				completion.accept(null);
				return null;
			}
		});
		job.addCompletionRunnable(terminal::countDown);
		blockEdt(edtBlocked, releaseEdt);
		assertTrue(edtBlocked.await(5, TimeUnit.SECONDS));
		try {
			queue.schedule(job);
			assertTrue(registrationQueued.await(5, TimeUnit.SECONDS));
			assertTrue(terminal.await(5, TimeUnit.SECONDS));
			assertEquals(0, callbacks.get());
		} finally {
			releaseEdt.countDown();
		}

		assertTrue(completed.await(5, TimeUnit.SECONDS));
		assertTrue(terminal.await(5, TimeUnit.SECONDS));
		job.join(5_000L);
		assertEquals(1, callbacks.get());
		assertEquals(1, cleanups.get());
		assertSame(null, result.get());
		assertEquals(Boolean.TRUE, onUiThread.get());
	}

	@Test
	void cancellationAfterSuccessfulRunnableQueuesButBeforeEdtRunsCompletesNull() throws Exception {
		CountDownLatch registrationQueued = new CountDownLatch(1);
		useSwingDispatcher(registrationQueued);
		JobQueue queue = new JobQueue("open-late-cancel-completion-test", false);
		Job job = new Job(queue, "open-late-cancel", "Open", false);
		LoadOptions options = new LoadOptions();
		CountDownLatch edtBlocked = new CountDownLatch(1);
		CountDownLatch releaseEdt = new CountDownLatch(1);
		CountDownLatch completed = new CountDownLatch(1);
		CountDownLatch terminal = new CountDownLatch(1);
		AtomicInteger callbacks = new AtomicInteger();
		AtomicInteger cleanups = new AtomicInteger();
		AtomicReference<Object> result = new AtomicReference<>(new Object());
		AtomicReference<Boolean> onUiThread = new AtomicReference<>(false);
		options.setEndSwingClosure(value -> {
			callbacks.incrementAndGet();
			result.set(value);
			onUiThread.set(SwingUtilities.isEventDispatchThread());
			completed.countDown();
		});
		var completion = ProjectFactory.registerOpenProjectCompletion(job, options, cleanups::incrementAndGet);
		job.addSwingRunnable(new JobRunnable("finish open") {
			@Override public Object run() throws Exception {
				return ProjectFactory.finishProjectOpen(Project.getDummy(), completion, ignored -> { });
			}
		});
		job.addCompletionRunnable(terminal::countDown);
		blockEdt(edtBlocked, releaseEdt);
		assertTrue(edtBlocked.await(5, TimeUnit.SECONDS));
		try {
			queue.schedule(job);
			assertTrue(registrationQueued.await(5, TimeUnit.SECONDS));
			assertTrue(terminal.await(5, TimeUnit.SECONDS));
			job.cancel();
		} finally {
			releaseEdt.countDown();
		}

		assertTrue(completed.await(5, TimeUnit.SECONDS));
		job.join(5_000L);
		assertEquals(1, callbacks.get());
		assertEquals(1, cleanups.get());
		assertSame(null, result.get());
		assertEquals(Boolean.TRUE, onUiThread.get());
	}

	@Test
	void loadFailureCallbackAndTerminalFallbackUseTheSameOneShotCallback() throws Exception {
		useSwingDispatcher();
		JobQueue queue = new JobQueue("open-failure-completion-test", false);
		Job job = new Job(queue, "open-failure", "Open", false);
		LoadOptions options = new LoadOptions();
		CountDownLatch completed = new CountDownLatch(1);
		AtomicInteger callbacks = new AtomicInteger();
		AtomicReference<Object> result = new AtomicReference<>(new Object());
		options.setEndSwingClosure(value -> {
			callbacks.incrementAndGet();
			result.set(value);
			completed.countDown();
		});
		var completion = ProjectFactory.registerOpenProjectCompletion(job, options, null);
		job.addRunnable(new JobRunnable("fail load") {
			@Override public Object run() throws Exception { throw new Exception("expected load failure"); }
		});
		job.addExceptionRunnable(new JobRunnable("report load failure") {
			@Override public Object run() {
				completion.accept(null);
				return null;
			}
		});

		queue.schedule(job);

		assertTrue(completed.await(5, TimeUnit.SECONDS));
		job.join(5_000L);
		assertEquals(1, callbacks.get());
		assertSame(null, result.get());
	}

	@Test
	void concurrentResultAndTerminalDeliveryRacesInvokeTheCallbackOnceOnTheUiThread() throws Exception {
		useSwingDispatcher();
		JobQueue queue = new JobQueue("open-callback-race-test", false);
		Job job = new Job(queue, "open-callback-race", "Open", false);
		LoadOptions options = new LoadOptions();
		CountDownLatch started = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		CountDownLatch completed = new CountDownLatch(1);
		CountDownLatch producersReady = new CountDownLatch(2);
		CountDownLatch race = new CountDownLatch(1);
		AtomicInteger callbacks = new AtomicInteger();
		AtomicReference<Object> result = new AtomicReference<>();
		AtomicReference<Boolean> onUiThread = new AtomicReference<>(false);
		options.setEndSwingClosure(value -> {
			callbacks.incrementAndGet();
			result.set(value);
			onUiThread.set(SwingUtilities.isEventDispatchThread());
			completed.countDown();
		});
		var completion = ProjectFactory.registerOpenProjectCompletion(job, options, null);
		Object firstProject = new Object();
		Object secondProject = new Object();
		job.addRunnable(new JobRunnable("wait for callback race") {
			@Override public Object run() throws Exception {
				started.countDown();
				release.await();
				return null;
			}
		});
		Thread firstProducer = new Thread(() -> raceToComplete(producersReady, race, completion, firstProject));
		Thread secondProducer = new Thread(() -> raceToComplete(producersReady, race, completion, secondProject));

		queue.schedule(job);
		assertTrue(started.await(5, TimeUnit.SECONDS));
		firstProducer.start();
		secondProducer.start();
		assertTrue(producersReady.await(5, TimeUnit.SECONDS));
		race.countDown();
		job.cancel();
		release.countDown();
		firstProducer.join(5_000L);
		secondProducer.join(5_000L);

		assertTrue(completed.await(5, TimeUnit.SECONDS));
		job.join(5_000L);
		assertEquals(1, callbacks.get());
		assertTrue(result.get() == null || result.get() == firstProject || result.get() == secondProject);
		assertEquals(Boolean.TRUE, onUiThread.get());
	}

	@Test
	void rejectedDuplicateLoadStillCleansUpAndCompletesOnTheUiThread() throws Exception {
		useSwingDispatcher();
		JobQueue queue = new JobQueue("open-rejected-completion-test", false);
		ProjectFactory factory = ProjectFactory.getInstance();
		long projectId = Long.MAX_VALUE - System.nanoTime();
		assertFalse(factory.getOpenOrLoadingProjects().contains(projectId));
		factory.addLoadingProject(projectId); // the first same-file load owns one reference
		Job active = new Job(queue, "loadProject", "Active open", false);
		CountDownLatch activeStarted = new CountDownLatch(1);
		CountDownLatch releaseActive = new CountDownLatch(1);
		active.addRunnable(new JobRunnable("hold active open") {
			@Override public Object run() throws Exception {
				activeStarted.countDown();
				releaseActive.await();
				return null;
			}
		});
		queue.schedule(active);
		assertTrue(activeStarted.await(5, TimeUnit.SECONDS));

		Job rejected = new Job(queue, "loadProject", "Duplicate open", false);
		LoadOptions options = new LoadOptions();
		LoadOptions firstOptions = new LoadOptions();
		CountDownLatch completed = new CountDownLatch(1);
		AtomicInteger callbacks = new AtomicInteger();
		AtomicInteger cleanups = new AtomicInteger();
		AtomicReference<Boolean> onUiThread = new AtomicReference<>(false);
		AtomicReference<Object> result = new AtomicReference<>(new Object());
		options.setEndSwingClosure(value -> {
			callbacks.incrementAndGet();
			result.set(value);
			onUiThread.set(SwingUtilities.isEventDispatchThread());
			completed.countDown();
		});
		var firstCompletion = ProjectFactory.registerOpenProjectCompletion(
			new Job(queue, "first-open", "First open", false), firstOptions,
			() -> factory.removeLoadingProject(projectId));
		factory.addLoadingProject(projectId); // the rejected request owns its own reference
		var completion = ProjectFactory.registerOpenProjectCompletion(rejected, options, () -> {
			cleanups.incrementAndGet();
			factory.removeLoadingProject(projectId);
		});
		try {
			assertFalse(ProjectFactory.scheduleOpenProjectJob(queue, rejected, completion));
			assertTrue(completed.await(5, TimeUnit.SECONDS));
			assertEquals(1, callbacks.get());
			assertEquals(1, cleanups.get());
			assertSame(null, result.get());
			assertEquals(Boolean.TRUE, onUiThread.get());
			assertTrue(factory.getOpenOrLoadingProjects().contains(projectId),
				"rejecting the second same-file open must preserve the first load's identity");
			firstCompletion.accept(null);
			assertFalse(factory.getOpenOrLoadingProjects().contains(projectId));
		} finally {
			factory.removeLoadingProject(projectId);
			factory.removeLoadingProject(projectId);
			releaseActive.countDown();
		}
		active.join(5_000L);
	}

	private static void raceToComplete(CountDownLatch ready, CountDownLatch race,
			java.util.function.Consumer<Object> completion, Object result) {
		ready.countDown();
		try {
			race.await();
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
			return;
		}
		completion.accept(result);
	}

	private static void useSwingDispatcher() {
		useSwingDispatcher(null);
	}

	private static void useSwingDispatcher(CountDownLatch dispatched) {
		UiDispatch.setDispatcher(new UiDispatcher() {
			@Override public void invokeLater(Runnable task) {
				if (dispatched != null) dispatched.countDown();
				SwingUtilities.invokeLater(task);
			}
			@Override public boolean isDispatchThread() { return SwingUtilities.isEventDispatchThread(); }
		});
	}

	private static void blockEdt(CountDownLatch blocked, CountDownLatch release) {
		SwingUtilities.invokeLater(() -> {
			blocked.countDown();
			try {
				release.await();
			} catch (InterruptedException interrupted) {
				Thread.currentThread().interrupt();
			}
		});
	}
	@Test
	void streamBackedXmlRecoveryDoesNotShareTheFileLoadAdmissionKey() {
		LoadOptions file = new LoadOptions();
		file.setFileName("recovery-key.pod");
		LoadOptions recovery = new LoadOptions();
		recovery.setFileName("recovery-key.pod");
		recovery.setFileInputStream(new java.io.ByteArrayInputStream(new byte[0]));
		assertFalse(ProjectFactory.openAdmissionKey(file).equals(ProjectFactory.openAdmissionKey(recovery)),
			"the XML recovery started from inside the file load must be admitted, not joined to it");
	}
}
