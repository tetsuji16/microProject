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
package com.microproject.pm.assignment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.document.ObjectEvent;
import com.microproject.options.CalendarOption;
import com.microproject.transaction.MultipleTransaction;
import com.microproject.undo.DataFactoryUndoController;

class AssignmentServiceTest {
	@Test
	void assignmentRemovalSnapshotPreservesSourceOrderAndContents() {
		Project project = createProject();
		NormalTask task = createTask(project);
		ResourceImpl resource = project.getResourcePool().newResourceInstance();
		Assignment assigned = AssignmentService.getInstance().newAssignment(task, resource, 1.0D, 0L, this);
		List<?> source = new ArrayList<>(task.getAssignments());
		List<Object> snapshot = new ArrayList<>();

		AssignmentService.getInstance().remove(source, snapshot);

		assertEquals(source, snapshot);
		assertEquals(source.size(), task.getAssignments().size());
		assertSame(assigned, snapshot.get(0));
	}

	@Test
	void removesAssignmentsFromDetachedBatchCollection() {
		Project project = createProject();
		NormalTask task = createTask(project);
		ResourceImpl firstResource = project.getResourcePool().newResourceInstance();
		ResourceImpl secondResource = project.getResourcePool().newResourceInstance();
		Assignment first = AssignmentService.getInstance().newAssignment(task, firstResource, 1.0D, 0L, this);
		Assignment second = AssignmentService.getInstance().newAssignment(task, secondResource, 1.0D, 0L, this);

		AssignmentService.getInstance().remove(List.of(first, second), null, false);

		assertNull(task.findAssignment(firstResource));
		assertNull(task.findAssignment(secondResource));
		assertNull(firstResource.findAssignment(task));
		assertNull(secondResource.findAssignment(task));
	}

	@Test
	void singleEventSourceRemovalPreservesInputSnapshotAndRemovesAssignments() {
		Project project = createProject();
		NormalTask task = createTask(project);
		ResourceImpl firstResource = project.getResourcePool().newResourceInstance();
		ResourceImpl secondResource = project.getResourcePool().newResourceInstance();
		Assignment first = AssignmentService.getInstance().newAssignment(task, firstResource, 1.0D, 0L, this);
		Assignment second = AssignmentService.getInstance().newAssignment(task, secondResource, 1.0D, 0L, this);
		List<Assignment> source = new ArrayList<>(List.of(first, second));
		List<Assignment> expected = List.copyOf(source);

		AssignmentService.getInstance().remove(source, this);

		assertEquals(expected, source);
		assertNull(task.findAssignment(firstResource));
		assertNull(task.findAssignment(secondResource));
		assertNull(firstResource.findAssignment(task));
		assertNull(secondResource.findAssignment(task));
	}

	@Test
	void newAssignmentConnectsTaskAndResource() {
		Project project = createProject();
		NormalTask task = createTask(project);
		ResourceImpl resource = project.getResourcePool().newResourceInstance();

		Assignment assignment = AssignmentService.getInstance().newAssignment(task, resource, 1.0D, 0L, this);

		assertNotNull(assignment);
		assertSame(assignment, task.findAssignment(resource));
		assertSame(assignment, resource.findAssignment(task));
	}

	@Test
	void newAssignmentsSkipsDuplicateResourcesAndRestoresTaskState() {
		Project project = createProject();
		NormalTask task = createTask(project);
		ResourceImpl resource = project.getResourcePool().newResourceInstance();
		int initialSchedulingType = task.getSchedulingType();
		boolean initialEffortDriven = task.isEffortDriven();

		AssignmentService.getInstance().newAssignments(Arrays.asList(task), Arrays.asList(resource, resource), 1.0D, 0L, this, true);

		assertEquals(initialSchedulingType, task.getSchedulingType());
		assertEquals(initialEffortDriven, task.isEffortDriven());
		assertEquals(1, countAssignmentsForResource(task, resource));
		assertNotNull(task.findAssignment(resource));
		assertNotNull(resource.findAssignment(task));
	}

	@Test
	void newAssignmentsUseOneNestedProjectRevisionAndNoRevisionForDuplicateResources() {
		Project project = createProject();
		NormalTask task = createTask(project);
		ResourceImpl resource = project.getResourcePool().newResourceInstance();
		long initialRevision = project.getMutationState().revision();
		int[] poolEvents = { 0 };
		long[] observedRevision = { -1L };
		AtomicBoolean acquiredBarrierFromObserver = new AtomicBoolean();
		ObjectEvent.Listener listener = event -> {
			poolEvents[0]++;
			observedRevision[0] = event.getPublicationRevision();
			Thread barrierProbe = new Thread(() -> {
				var scope = project.getMutationState().begin();
				try {
					acquiredBarrierFromObserver.set(true);
				} finally {
					scope.close();
				}
			}, "assignment-event-barrier-probe");
			barrierProbe.setDaemon(true);
			barrierProbe.start();
			try {
				barrierProbe.join(1000L);
			} catch (InterruptedException interrupted) {
				Thread.currentThread().interrupt();
				throw new AssertionError(interrupted);
			}
		};
		project.getResourcePool().addObjectListener(listener);

		try {
			project.runModelMutation(markApplied -> {
				AssignmentService.getInstance().newAssignments(List.of(task), List.of(resource),
						1.0D, 0L, this, false);
				assertEquals(0, poolEvents[0], "nested pool events wait for the outer project commit");
				markApplied.run();
				return true;
			});
		} finally {
			project.getResourcePool().removeObjectListener(listener);
		}

		assertNotNull(task.findAssignment(resource));
		assertEquals(initialRevision + 1, project.getMutationState().revision(),
			"the nested assignment command shares its outer project revision");
		assertEquals(1, poolEvents[0], "pool facts publish once after the outer commit");
		assertEquals(initialRevision + 1, observedRevision[0], "pool facts carry the committed revision");
		assertTrue(acquiredBarrierFromObserver.get(), "observers run after the project barrier is released");

		AssignmentService.getInstance().newAssignments(List.of(task), List.of(resource),
				1.0D, 0L, this, false);

		assertEquals(initialRevision + 1, project.getMutationState().revision(),
			"an assignment request that only encounters existing resources is a no-op");
		assertFalse(project.getMutationState().isHeldByCurrentThread());
		project.getUndoController().clear();
		AssignmentService.getInstance().newAssignments(List.of(task), List.of(resource),
				1.0D, 0L, this, true);
		assertFalse(project.getUndoController().canUndo(),
			"a duplicate assignment request must not create an empty schedule-backup undo edit");
	}

	@Test
	void concurrentAssignmentCommandsKeepTheirUndoGroupsSeparateWhenPublicationOverlaps() throws Exception {
		Project project = createProject();
		NormalTask firstTask = (NormalTask) project.createLocalTaskNode(null).getImpl();
		NormalTask secondTask = (NormalTask) project.createLocalTaskNode(null).getImpl();
		ResourceImpl firstResource = project.getResourcePool().newResourceInstance();
		ResourceImpl secondResource = project.getResourcePool().newResourceInstance();
		project.getUndoController().clear();
		CountDownLatch firstPublicationEntered = new CountDownLatch(1);
		CountDownLatch releaseFirstPublication = new CountDownLatch(1);
		CountDownLatch firstCommandFinished = new CountDownLatch(1);
		AtomicReference<Throwable> firstFailure = new AtomicReference<>();
		ObjectEvent.Listener listener = event -> {
			if (event.isCreate() && event.getObject() instanceof Assignment assignment
					&& assignment.getResource() == firstResource) {
				firstPublicationEntered.countDown();
				try {
					if (!releaseFirstPublication.await(2, TimeUnit.SECONDS))
						throw new AssertionError("first assignment publication was not released");
				} catch (InterruptedException interrupted) {
					Thread.currentThread().interrupt();
					throw new AssertionError(interrupted);
				}
			}
		};
		project.getResourcePool().addObjectListener(listener);
		Thread firstCommand = new Thread(() -> {
			try {
				AssignmentService.getInstance().newAssignments(List.of(firstTask), List.of(firstResource),
					1.0D, 0L, this, true);
			} catch (Throwable failure) {
				firstFailure.set(failure);
			} finally {
				firstCommandFinished.countDown();
			}
		}, "assignment-command-first-publication");
		firstCommand.start();
		try {
			assertTrue(firstPublicationEntered.await(2, TimeUnit.SECONDS),
				"the first command must reach post-commit pool publication");
			AssignmentService.getInstance().newAssignments(List.of(secondTask), List.of(secondResource),
				1.0D, 0L, this, true);
			assertNotNull(secondTask.findAssignment(secondResource));
			assertFalse(project.getUndoController().canUndo(), "unfinished publication must block older Undo history");
			assertFalse(project.getUndoController().canRedo());
		} finally {
			releaseFirstPublication.countDown();
			project.getResourcePool().removeObjectListener(listener);
			assertTrue(firstCommandFinished.await(2, TimeUnit.SECONDS), "the first command must finish");
		}
		assertNull(firstFailure.get(), () -> "first assignment command failed: " + firstFailure.get());
		assertEquals(2, project.getUndoController().getEditNames().size(),
			"overlapping publications must leave one independent undo entry per command");
		project.getUndoController().undo();
		assertNotNull(firstTask.findAssignment(firstResource), "Undo must follow commit order, not publication order");
		assertNull(secondTask.findAssignment(secondResource));
		project.getUndoController().undo();
		assertNull(firstTask.findAssignment(firstResource));
		assertEquals(2, project.getUndoController().getEditNames().size(), "Undo must not record new delayed edits");
		assertTrue(project.getUndoController().canRedo(), "both committed command groups must remain redoable");
		project.getUndoController().redo();
		assertNotNull(firstTask.findAssignment(firstResource));
		assertNull(secondTask.findAssignment(secondResource));
		project.getUndoController().redo();
		assertNotNull(secondTask.findAssignment(secondResource));
	}

	@Test
	void rolledBackNestedPoolFactsRefreshCommittedAssignmentAfterOuterUnlock() {
		Project project = createProject();
		NormalTask task = createTask(project);
		ResourcePool pool = project.getResourcePool();
		ResourceImpl resource = pool.newResourceInstance();
		ResourceImpl secondResource = pool.newResourceInstance();
		long initialRevision = project.getMutationState().revision();
		List<Boolean> observedUpdates = new ArrayList<>();
		List<Object> observedObjects = new ArrayList<>();
		List<Long> observedRevisions = new ArrayList<>();
		ObjectEvent.Listener listener = event -> {
			observedUpdates.add(event.isUpdate());
			observedObjects.add(event.getObject());
			observedRevisions.add(event.getPublicationRevision());
		};
		pool.addObjectListener(listener);

		try {
			project.runModelMutation(markApplied -> {
				AssignmentService.getInstance().newAssignments(List.of(task), List.of(resource),
						1.0D, 0L, this, false);
				AssignmentService.getInstance().newAssignments(List.of(task), List.of(secondResource),
						1.0D, 0L, this, false);
				assertEquals(0, observedUpdates.size(), "duplicate participant enlistment stays deferred");
				pool.getObjectEventManager().beginBatch();
				pool.fireUpdateEvent(this, secondResource);
				pool.getObjectEventManager().endBatch(false);
				markApplied.run();
				return true;
			});
		} finally {
			pool.removeObjectListener(listener);
		}

		assertNotNull(task.findAssignment(resource));
		assertNotNull(task.findAssignment(secondResource));
		assertEquals(initialRevision + 1, project.getMutationState().revision());
		assertEquals(2, observedUpdates.size(), "rolled-back facts are replaced by committed-state refreshes");
		assertTrue(observedUpdates.get(0));
		assertTrue(observedUpdates.get(1));
		assertTrue(observedObjects.contains(resource));
		assertTrue(observedObjects.contains(secondResource));
		assertEquals(initialRevision + 1, observedRevisions.get(0));
		assertEquals(initialRevision + 1, observedRevisions.get(1));
	}

	@Test
	void newAssignmentsPreservesRequestedDelayAndDoesNotCreateUndoWhenDisabled() {
		Project project = createProject();
		NormalTask task = createTask(project);
		ResourceImpl resource = project.getResourcePool().newResourceInstance();
		long delay = 2L * 60L * 60L * 1000L;

		AssignmentService.getInstance().newAssignments(Arrays.asList(task), Arrays.asList(resource),
				1.0D, delay, this, false);

		assertEquals(delay, task.findAssignment(resource).getDelay());
		org.junit.jupiter.api.Assertions.assertFalse(project.getUndoController().canUndo());
	}

	@Test
	void newAssignmentsRejectsTasksFromDifferentProjectsBeforeMutatingEitherProject() {
		Project firstProject = createProject();
		Project secondProject = createProject();
		NormalTask firstTask = createTask(firstProject);
		NormalTask secondTask = createTask(secondProject);
		ResourceImpl resource = firstProject.getResourcePool().newResourceInstance();

		assertThrows(IllegalArgumentException.class, () -> AssignmentService.getInstance().newAssignments(
				Arrays.asList(firstTask, secondTask), Arrays.asList(resource), 1.0D, 0L, this, true));

		assertEquals(0, countAssignmentsForResource(firstTask, resource));
		assertEquals(1, secondTask.getAssignments().size(), "the pre-existing default assignment must remain");
	}

	@Test
	void newAssignmentsBatchesResourcePoolTransaction() {
		Project project = createProject();
		NormalTask task = createTask(project);
		ResourceImpl first = project.getResourcePool().newResourceInstance();
		ResourceImpl second = project.getResourcePool().newResourceInstance();
		int[] transactionEvents = { 0 };
		MultipleTransaction.Listener listener = event -> transactionEvents[0]++;
		project.getResourcePool().addMultipleTransactionListener(listener);

		try {
			AssignmentService.getInstance().newAssignments(Arrays.asList(task), Arrays.asList(first, second),
					1.0D, 0L, this, true);
		} finally {
			project.getResourcePool().removeMultipleTransactionListener(listener);
		}

		assertEquals(2, transactionEvents[0]);
	}

	@Test
	void replaceAssignmentWithActualWorkRetainsSourceAndTransfersRemainingWork() {
		Project project = createProject();
		NormalTask task = createTask(project);
		long day = CalendarOption.getInstance().getMillisPerDay();
		task.setDuration(2L * day);
		ResourceImpl original = project.getResourcePool().newResourceInstance();
		ResourceImpl replacement = project.getResourcePool().newResourceInstance();
		Assignment source = AssignmentService.getInstance().newAssignment(task, original, 1.0D, 0L, this);
		long originalWork = source.getWork(null);
		source.setActualWork(originalWork / 2L, null);
		long actualWork = source.getActualWork(null);
		long remainingWork = source.getRemainingWork();

		List<Assignment> replacements = AssignmentService.getInstance().replaceAssignment(source,
				List.of(replacement), this, true);
		// The desktop route schedules a recalculation after the assignment
		// mutation; verify the actual/remaining split survives that same pass.
		project.recalculate();

		assertEquals(1, replacements.size());
		Assignment replacementAssignment = replacements.get(0);
		assertSame(source, task.findAssignment(original), "actuals must remain with the original resource");
		assertEquals(actualWork, source.getActualWork(null));
		assertEquals(0L, source.getRemainingWork(), "the original resource must no longer own remaining work");
		assertSame(replacementAssignment, task.findAssignment(replacement));
		assertEquals(remainingWork, replacementAssignment.getRemainingWork(),
				"the replacement must receive the original remaining work");

		project.getUndoController().undo();
		assertSame(source, task.findAssignment(original));
		assertEquals(actualWork, source.getActualWork(null));
		assertEquals(remainingWork, source.getRemainingWork());
		project.getUndoController().redo();
		Assignment redoneReplacement = task.findAssignment(replacement);
		assertSame(replacementAssignment, redoneReplacement);
		assertEquals(actualWork, source.getActualWork(null));
		assertEquals(0L, source.getRemainingWork());
		assertEquals(remainingWork, redoneReplacement.getRemainingWork());

	}

	@Test
	void replaceAssignmentWithoutActualWorkRemovesSource() {
		Project project = createProject();
		NormalTask task = createTask(project);
		ResourceImpl original = project.getResourcePool().newResourceInstance();
		ResourceImpl replacement = project.getResourcePool().newResourceInstance();
		Assignment source = AssignmentService.getInstance().newAssignment(task, original, 1.0D, 0L, this);

		List<Assignment> replacements = AssignmentService.getInstance().replaceAssignment(source,
				List.of(replacement), this, true);

		assertEquals(1, replacements.size());
		assertNull(task.findAssignment(original));
		assertSame(replacements.get(0), task.findAssignment(replacement));
	}

	@Test
	void replaceAssignmentLeavesSourceUntouchedWhenTaskBecomesReadOnly() {
		Project project = createProject();
		NormalTask task = createTask(project);
		ResourceImpl original = project.getResourcePool().newResourceInstance();
		ResourceImpl replacement = project.getResourcePool().newResourceInstance();
		Assignment source = AssignmentService.getInstance().newAssignment(task, original, 1.0D, 0L, this);
		project.setReadOnly(true);

		List<Assignment> replacements = AssignmentService.getInstance().replaceAssignment(source,
				List.of(replacement), this, true);

		assertEquals(0, replacements.size());
		assertSame(source, task.findAssignment(original));
		assertNull(task.findAssignment(replacement));
	}

	private Project createProject() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		return project;
	}

	private NormalTask createTask(Project project) {
		NormalTask task = new NormalTask(project);
		project.connectTask(task);
		return task;
	}

	private int countAssignmentsForResource(NormalTask task, ResourceImpl resource) {
		int count = 0;
		for (Iterator<?> iterator = task.getAssignments().iterator(); iterator.hasNext();) {
			Assignment assignment = (Assignment) iterator.next();
			if (resource.equals(assignment.getResource())) {
				count++;
			}
		}
		return count;
	}
}
