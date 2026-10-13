/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.scheduling.ScheduleEvent;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;
import org.junit.jupiter.api.Test;

class ProjectMutationStateTest {
	@Test
	void revisionsAdvanceOnlyForCommittedChangedOutermostMutation() {
		ProjectMutationState state = new ProjectMutationState();

		try (ProjectMutationState.Scope ignored = state.begin()) {
			// An uncommitted mutation is rolled back.
		}
		assertEquals(0, state.revision());

		try (ProjectMutationState.Scope outer = state.begin()) {
			try (ProjectMutationState.Scope inner = state.begin()) {
				inner.commit(true);
			}
			assertEquals(0, state.revision(), "nested commits remain hidden until the outer commit");
			outer.commit(true);
		}
		assertEquals(1, state.revision());

		try (ProjectMutationState.Scope noOp = state.begin()) {
			noOp.commit(false);
		}
		assertEquals(1, state.revision());
	}

	@Test
	void barrierPreventsConcurrentSnapshotCaptureFromInterleavingWithMutation() throws Exception {
		ProjectMutationState state = new ProjectMutationState();
		CountDownLatch mutationStarted = new CountDownLatch(1);
		CountDownLatch releaseMutation = new CountDownLatch(1);
		CountDownLatch captureStarted = new CountDownLatch(1);
		CountDownLatch captureEntered = new CountDownLatch(1);
		ExecutorService workers = Executors.newFixedThreadPool(2);
		try {
			workers.submit(() -> {
				try (ProjectMutationState.Scope mutation = state.begin()) {
					mutationStarted.countDown();
					releaseMutation.await();
					mutation.commit(true);
				} catch (InterruptedException failure) {
					Thread.currentThread().interrupt();
				}
			});
			assertTrue(mutationStarted.await(2, TimeUnit.SECONDS));
			workers.submit(() -> {
				captureStarted.countDown();
				try (ProjectMutationState.Scope ignored = state.begin()) {
					captureEntered.countDown();
				}
			});
			assertTrue(captureStarted.await(2, TimeUnit.SECONDS));
			assertFalse(captureEntered.await(100, TimeUnit.MILLISECONDS),
				"snapshot capture must wait until the in-flight mutation commits");
			releaseMutation.countDown();
			assertTrue(captureEntered.await(2, TimeUnit.SECONDS));
			assertEquals(1, state.revision());
		} finally {
			releaseMutation.countDown();
			workers.shutdownNow();
		}
	}

	@Test
	void detachedNotificationsStayWithTheirCommittedWriterRevision() throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("mutation-events", undo), undo);
		project.initialize(false, false);
		Object first = new Object();
		Object second = new Object();
		List<Object> delivered = Collections.synchronizedList(new ArrayList<>());
		project.addScheduleListener(event -> delivered.add(event.getObject()));
		CountDownLatch firstDetached = new CountDownLatch(1);
		CountDownLatch releaseFirstNotification = new CountDownLatch(1);
		CountDownLatch secondPublished = new CountDownLatch(1);
		AtomicLong firstRevision = new AtomicLong(-1L);
		AtomicLong secondRevision = new AtomicLong(-1L);
		AtomicReference<Throwable> failure = new AtomicReference<>();
		ExecutorService workers = Executors.newFixedThreadPool(2);
		try {
			workers.submit(() -> {
				try (ProjectMutationState.Scope mutation = project.getMutationState().begin()) {
					project.beginEventBatch();
					project.fireScheduleChanged("writer-a", ScheduleEvent.SCHEDULE, first);
					Project.PendingEventBatch pending = project.commitEventBatch(mutation, true);
					firstRevision.set(pending.revision());
					firstDetached.countDown();
					if (!releaseFirstNotification.await(2, TimeUnit.SECONDS))
						throw new AssertionError("writer A notification was not released");
					project.publishEventBatch(pending);
				} catch (Throwable error) {
					failure.compareAndSet(null, error);
					firstDetached.countDown();
				}
			});
			workers.submit(() -> {
				try {
					if (!firstDetached.await(2, TimeUnit.SECONDS))
						throw new AssertionError("writer A did not detach its notification");
					try (ProjectMutationState.Scope mutation = project.getMutationState().begin()) {
						project.beginEventBatch();
						project.fireScheduleChanged("writer-b", ScheduleEvent.SCHEDULE, second);
						Project.PendingEventBatch pending = project.commitEventBatch(mutation, true);
						secondRevision.set(pending.revision());
						project.publishEventBatch(pending);
					}
				} catch (Throwable error) {
					failure.compareAndSet(null, error);
				} finally {
					secondPublished.countDown();
				}
			});
			assertTrue(secondPublished.await(2, TimeUnit.SECONDS), "writer B must commit while A notification is delayed");
			assertEquals(List.of(second), delivered, "writer B cannot consume writer A's detached batch");
			releaseFirstNotification.countDown();
			workers.shutdown();
			assertTrue(workers.awaitTermination(2, TimeUnit.SECONDS));
			assertEquals(List.of(second, first), delivered);
			assertEquals(1L, firstRevision.get());
			assertEquals(2L, secondRevision.get());
			assertTrue(failure.get() == null, () -> "concurrent writer failed: " + failure.get());
		} finally {
			releaseFirstNotification.countDown();
			workers.shutdownNow();
		}
	}
}
