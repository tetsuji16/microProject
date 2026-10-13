/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
	void modelMutationBoundaryPreservesNoOpAndPublishesAppliedFailureAfterUnlock() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("model-mutation-boundary", undo), undo);
		project.initialize(false, false);
		List<ScheduleEvent> delivered = new ArrayList<>();
		project.addScheduleListener(event -> {
			assertFalse(project.getMutationState().isHeldByCurrentThread(),
				"observers are called only after the project barrier is released");
			delivered.add(event);
		});

		long initialRevision = project.getMutationState().revision();
		assertFalse(project.runModelMutation(markApplied -> false));
		assertEquals(initialRevision, project.getMutationState().revision());
		assertFalse(project.runModelMutation(markApplied -> {
			markApplied.run();
			return false;
		}), "the apply marker is conservative for exceptions, not a successful no-op result");
		assertEquals(initialRevision, project.getMutationState().revision());
		assertTrue(delivered.isEmpty(), "a preflight no-op has no publication");

		assertThrows(IllegalStateException.class, () -> project.runModelMutation(markApplied -> {
			markApplied.run();
			throw new IllegalStateException("failure after mutation began");
		}));
		assertEquals(initialRevision + 1, project.getMutationState().revision());
		assertEquals(1, delivered.size());
		assertEquals(ScheduleEvent.SCHEDULE, delivered.getFirst().getType());
		assertEquals(initialRevision + 1, delivered.getFirst().getPublicationRevision());
	}

	@Test
	void nestedModelMutationPublishesOnlyWithItsOutermostRevision() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("nested-model-mutation", undo), undo);
		project.initialize(false, false);
		List<ScheduleEvent> delivered = new ArrayList<>();
		project.addScheduleListener(delivered::add);
		long initialRevision = project.getMutationState().revision();

		Project.PendingEventBatch pending;
		try (ProjectMutationState.Scope outer = project.getMutationState().begin()) {
			project.beginEventBatch();
			assertTrue(project.runModelMutation(markApplied -> {
				markApplied.run();
				project.fireScheduleChanged("nested-command", ScheduleEvent.SCHEDULE);
				return true;
			}));
			assertEquals(initialRevision, project.getMutationState().revision());
			assertTrue(delivered.isEmpty(), "nested commands cannot publish while the outer mutation is active");
			pending = project.commitEventBatch(outer, true);
		}
		project.publishEventBatch(pending);

		assertEquals(initialRevision + 1, project.getMutationState().revision());
		assertEquals(1, delivered.size());
		assertEquals(initialRevision + 1, delivered.getFirst().getPublicationRevision());
	}

	@Test
	void unavailableProjectUndoAndRedoDoNotAdvanceRevision() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("empty-project-history", undo), undo);
		project.initialize(false, false);
		long revision = project.getMutationState().revision();

		undo.undo();
		undo.redo();

		assertEquals(revision, project.getMutationState().revision());
	}

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
		List<Long> deliveredRevisions = Collections.synchronizedList(new ArrayList<>());
		project.addScheduleListener(event -> {
			delivered.add(event.getObject());
			deliveredRevisions.add(event.getPublicationRevision());
		});
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
			assertEquals(List.of(2L, 1L), deliveredRevisions,
				"each observer event must retain its detached batch revision even when delivery is reordered");
			assertEquals(1L, firstRevision.get());
			assertEquals(2L, secondRevision.get());
			assertTrue(failure.get() == null, () -> "concurrent writer failed: " + failure.get());
		} finally {
			releaseFirstNotification.countDown();
			workers.shutdownNow();
		}
	}

	@Test
	void committedOuterMutationReplacesNestedRolledBackFactsWithFreshInvalidation() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("nested-event-rollback", undo), undo);
		project.initialize(false, false);
		List<ScheduleEvent> delivered = new ArrayList<>();
		project.addScheduleListener(delivered::add);

		Project.PendingEventBatch pending;
		try (ProjectMutationState.Scope outer = project.getMutationState().begin()) {
			project.beginEventBatch();
			project.beginEventBatch();
			project.fireScheduleChanged("rolled-back-inner-operation", ScheduleEvent.SCHEDULE, "stale-fact");
			project.endEventBatch(false);
			pending = project.commitEventBatch(outer, true);
		}
		project.publishEventBatch(pending);

		assertEquals(1, delivered.size());
		assertEquals(ScheduleEvent.SCHEDULE, delivered.getFirst().getType());
		assertNull(delivered.getFirst().getObject(), "rolled-back facts must be replaced, not replayed");
		assertEquals(1L, delivered.getFirst().getPublicationRevision());
	}
}
