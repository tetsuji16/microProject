/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.scheduling.ScheduleEvent;
import com.microproject.transaction.ProjectMutationState;
import com.microproject.undo.DataFactoryUndoController;

/** Issue #946: a failed command must not leave the process-wide mutation coordinator held. */
class ProjectModelCommandReleaseTest {
	@Test
	void releasesCoordinatorWhenChangedResultCallbackFails() throws Exception {
		Project project = newProject();
		IllegalStateException failure = new IllegalStateException("changedResult");
		IllegalStateException thrown = assertThrows(IllegalStateException.class,
			() -> project.runModelCommand(markApplied -> Boolean.TRUE, result -> { throw failure; }, result -> result));
		assertSame(failure, thrown);
		assertCoordinatorReleased(project);
		assertEventsStillPublish(project);
	}

	@Test
	void releasesCoordinatorAndPublishesEmittedFactsWhenResultCallbackFails() throws Exception {
		Project project = newProject();
		AtomicInteger scheduleEvents = new AtomicInteger();
		project.addScheduleListener(event -> scheduleEvents.incrementAndGet());
		IllegalStateException failure = new IllegalStateException("resultWhenEventsWereEmitted");
		assertThrows(IllegalStateException.class, () -> project.runModelCommand(markApplied -> {
			markApplied.run();
			project.fireScheduleChanged(project, ScheduleEvent.SCHEDULE);
			return Boolean.TRUE;
		}, Boolean.TRUE::equals, result -> { throw failure; }));
		assertTrue(scheduleEvents.get() > 0, "facts emitted by an applied operation must still reach views");
		assertCoordinatorReleased(project);
		assertEventsStillPublish(project);
	}

	private static Project newProject() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("release", undo), undo);
		project.initialize(false, false);
		return project;
	}

	private static void assertCoordinatorReleased(Project project) throws Exception {
		assertFalse(project.getMutationState().isHeldByCurrentThread());
		ExecutorService other = Executors.newSingleThreadExecutor();
		try {
			assertEquals(Boolean.TRUE, other.submit(() -> ProjectMutationState.coordinated(() -> Boolean.TRUE))
				.get(5, TimeUnit.SECONDS), "another thread must acquire the coordinator");
		} finally {
			other.shutdownNow();
		}
	}

	private static void assertEventsStillPublish(Project project) {
		AtomicInteger scheduleEvents = new AtomicInteger();
		project.addScheduleListener(event -> scheduleEvents.incrementAndGet());
		assertTrue(project.runModelMutation(markApplied -> {
			markApplied.run();
			project.fireScheduleChanged(project, ScheduleEvent.SCHEDULE);
			return true;
		}));
		assertTrue(scheduleEvents.get() > 0, "the event batch must be closed so later commands publish");
	}
}
