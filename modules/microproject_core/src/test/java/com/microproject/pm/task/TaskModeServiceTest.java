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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.microproject.pm.criticalpath.SchedulingAlgorithm;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.undo.DataFactoryUndoController;

class TaskModeServiceTest {
	@Test
	void compensatesEarlierTasksWhenLaterModeApplicationFails() throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project firstProject = Project.createProject(ResourcePool.createRourcePool("first", undo), undo);
		firstProject.initialize(false, false);
		NormalTask first = firstProject.createScriptedTask();
		firstProject.recalculate();
		long firstStart = first.getStart();
		long firstFinish = first.getEnd();
		long firstManualStart = first.manualStart;
		long firstManualFinish = first.manualFinish;
		Project secondProject = Project.createProject(ResourcePool.createRourcePool("second", undo), undo);
		secondProject.initialize(false, false);
		NormalTask second = secondProject.createScriptedTask();
		secondProject.recalculate();
		first.setName("history checkpoint");
		undo.undo();
		List<String> undoNames = undo.getEditNames();
		String redoName = undo.getRedoName();
		boolean canUndo = undo.canUndo();
		boolean canRedo = undo.canRedo();
		long firstRevision = firstProject.getMutationState().revision();
		long secondRevision = secondProject.getMutationState().revision();
		long secondStart = second.getStart();
		long secondFinish = second.getEnd();
		long secondManualStart = second.manualStart;
		long secondManualFinish = second.manualFinish;
		boolean firstDirty = first.isDirty();
		boolean secondDirty = second.isDirty();
		boolean projectDirty = firstProject.isDirty();
		boolean secondProjectDirty = secondProject.isDirty();
		AtomicInteger objectEvents = new AtomicInteger();
		AtomicInteger scheduleEvents = new AtomicInteger();
		firstProject.addObjectListener(event -> objectEvents.incrementAndGet());
		firstProject.addScheduleListener(event -> scheduleEvents.incrementAndGet());
		secondProject.addObjectListener(event -> objectEvents.incrementAndGet());
		secondProject.addScheduleListener(event -> scheduleEvents.incrementAndGet());
		RuntimeException injectedFailure = new IllegalStateException("injected later-project scheduling failure");
		SchedulingAlgorithm delegate = secondProject.getSchedulingAlgorithm();
		AtomicBoolean failOnce = new AtomicBoolean(true);
		secondProject.setSchedulingAlgorithm((SchedulingAlgorithm) Proxy.newProxyInstance(
			SchedulingAlgorithm.class.getClassLoader(), new Class<?>[] { SchedulingAlgorithm.class }, (proxy, method, args) -> {
				if (method.getName().equals("calculate") && failOnce.compareAndSet(true, false)) throw injectedFailure;
				try { return method.invoke(delegate, args); }
				catch (InvocationTargetException failure) { throw failure.getCause(); }
			}));

		RuntimeException failure = assertThrows(RuntimeException.class,
			() -> new TaskModeService().apply(List.of(first, second), TaskModeService.Mode.MANUAL));
		assertSame(injectedFailure, failure);
		assertFalse(first.isManuallyScheduled());
		assertFalse(second.isManuallyScheduled());
		assertEquals(firstManualStart, first.manualStart);
		assertEquals(firstManualFinish, first.manualFinish);
		assertEquals(secondManualStart, second.manualStart);
		assertEquals(secondManualFinish, second.manualFinish);
		assertTrue(first.getStart() == firstStart && first.getEnd() == firstFinish);
		assertTrue(second.getStart() == secondStart && second.getEnd() == secondFinish);
		assertTrue(first.isDirty() == firstDirty && second.isDirty() == secondDirty);
		assertTrue(firstProject.isDirty() == projectDirty);
		assertTrue(secondProject.isDirty() == secondProjectDirty);
		assertEquals(firstRevision, firstProject.getMutationState().revision());
		assertEquals(secondRevision, secondProject.getMutationState().revision());
		assertEquals(canUndo, undo.canUndo());
		assertEquals(canRedo, undo.canRedo());
		assertEquals(undoNames, undo.getEditNames());
		assertEquals(redoName, undo.getRedoName());
		assertEquals(0, objectEvents.get());
		assertEquals(0, scheduleEvents.get());
	}

	@Test
	void appliesManualModeToSelectionAsOneReversibleChange() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("mode", undo), undo);
		project.initialize(false, false);
		NormalTask first = project.createScriptedTask();
		NormalTask second = project.createScriptedTask();
		TaskModeService service = new TaskModeService();

		assertFalse(first.isManuallyScheduled());
		assertFalse(second.isManuallyScheduled());
		TaskModeService.Result result = service.apply(List.of(first, second), TaskModeService.Mode.MANUAL);
		assertTrue(result.manual());
		assertTrue(first.isManuallyScheduled() && second.isManuallyScheduled());
		result.change().undo();
		assertFalse(first.isManuallyScheduled() || second.isManuallyScheduled());
		result.change().redo();
		assertTrue(first.isManuallyScheduled() && second.isManuallyScheduled());
	}

	/** Issue #946: a batch-setup failure on a later project must release every barrier already taken. */
	@Test
	void batchSetupFailureReleasesEveryProjectBarrier() throws Exception {
		DataFactoryUndoController firstUndo = new DataFactoryUndoController();
		Project firstProject = Project.createProject(ResourcePool.createRourcePool("first-barrier", firstUndo), firstUndo);
		firstProject.initialize(false, false);
		NormalTask first = firstProject.createScriptedTask();
		FailingUndoController secondUndo = new FailingUndoController();
		Project secondProject = Project.createProject(ResourcePool.createRourcePool("second-barrier", secondUndo), secondUndo);
		secondProject.initialize(false, false);
		NormalTask second = secondProject.createScriptedTask();
		long firstRevision = firstProject.getMutationState().revision();
		long secondRevision = secondProject.getMutationState().revision();

		secondUndo.fail = true;
		assertThrows(IllegalStateException.class,
			() -> new TaskModeService().apply(List.of(first, second), TaskModeService.Mode.MANUAL));
		secondUndo.fail = false;

		assertFalse(firstProject.getMutationState().isHeldByCurrentThread(), "the failing thread must release the barrier");
		assertFalse(first.isManuallyScheduled() || second.isManuallyScheduled());
		assertEquals(firstRevision, firstProject.getMutationState().revision());
		assertEquals(secondRevision, secondProject.getMutationState().revision());
		assertEquals(0, firstProject.pendingEventCount());
		assertFalse(firstUndo.getEditSupport().hasPendingHistory(), "the abandoned batch must not leave an open undo command");
		ExecutorService worker = Executors.newSingleThreadExecutor();
		try {
			TaskModeService.Result result = worker.submit(
				() -> new TaskModeService().apply(List.of(first, second), TaskModeService.Mode.MANUAL))
				.get(2, TimeUnit.SECONDS);
			assertTrue(result.manual());
		} finally {
			worker.shutdownNow();
		}
		assertTrue(first.isManuallyScheduled() && second.isManuallyScheduled());
	}

	/** Undo controller whose command support fails on demand, after project initialization. */
	private static final class FailingUndoController extends DataFactoryUndoController {
		volatile boolean fail;
		@Override public com.microproject.undo.EditSupport getEditSupport() {
			if (fail) throw new IllegalStateException("undo command support unavailable");
			return super.getEditSupport();
		}
	}
}
