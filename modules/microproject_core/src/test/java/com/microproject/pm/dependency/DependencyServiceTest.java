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
package com.microproject.pm.dependency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.microproject.association.InvalidAssociationException;
import com.microproject.document.ObjectEvent;
import com.microproject.field.FieldParseException;
import com.microproject.options.CalendarOption;
import com.microproject.grouping.core.NodeFactory;
import com.microproject.grouping.core.Node;
import com.microproject.pm.scheduling.ScheduleInterval;
import com.microproject.pm.scheduling.ScheduleService;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.SubProj;
import com.microproject.undo.DataFactoryUndoController;

class DependencyServiceTest {
	@AfterEach
	void clearWarningHandler() {
		DependencyWarningNotifications.setCircularLinkWarningHandler(null);
	}

	@Test
	void circularCrossProjectWarningIsDeliveredThroughNotificationPort() {
		AtomicReference<String> received = new AtomicReference<>();
		DependencyWarningNotifications.setCircularLinkWarningHandler(received::set);

		DependencyService.warnCircularCrossProjectLinkMessage("Predecessor task", "Successor task");

		assertEquals(DependencyService.getCircularCrossProjectLinkMessage("Predecessor task", "Successor task"),
				received.get());
	}

	@Test
	void unknownTaskReferenceDoesNotCreateTaskOrReplaceExistingLink() throws Exception {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		NormalTask predecessor = new NormalTask(project);
		NormalTask successor = new NormalTask(project);
		project.connectTask(predecessor);
		project.connectTask(successor);
		DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.FS, 0L, this);
		int taskCount = project.getTaskList().size();

		assertThrows(FieldParseException.class, () -> successor.setPredecessors("999999"));
		assertEquals(taskCount, project.getTaskList().size());
		assertEquals(1, successor.getPredecessorList().size());
	}

	@Test
	void unknownDependencyTypeFailsExplicitlyWhenFormatted() {
		assertThrows(IllegalArgumentException.class, () -> DependencyType.toLongString(99));
	}

	@Test
	void unknownIntegerDependencyTypeIsRejectedBeforeCreatingALink() {
		Project project = createProject("unknown-dependency-kind");
		NormalTask predecessor = new NormalTask(project);
		NormalTask successor = new NormalTask(project);
		project.connectTask(predecessor);
		project.connectTask(successor);

		assertThrows(IllegalArgumentException.class,
				() -> DependencyService.getInstance().newDependency(predecessor, successor, 99, 0L, this));
		assertEquals(0, predecessor.getSuccessorList().size());
		assertEquals(0, successor.getPredecessorList().size());
	}

	@Test
	void newDependencyCreatesFsZeroLagLink() throws InvalidAssociationException {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		NormalTask predecessor = new NormalTask(project);
		NormalTask successor = new NormalTask(project);
		project.connectTask(predecessor);
		project.connectTask(successor);

		Dependency dependency = DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.FS, 0L, this);

		assertEquals(DependencyType.FS, dependency.getDependencyType());
		assertEquals(0L, dependency.getLag());
	}

	@Test
	void dependencyWritesPublishOneRevisionAndNoOpsDoNotAdvanceIt() throws InvalidAssociationException {
		Project project = createProject("dependency-revision");
		NormalTask predecessor = new NormalTask(project);
		NormalTask successor = new NormalTask(project);
		project.connectTask(predecessor);
		project.connectTask(successor);
		long revision = project.getMutationState().revision();

		Dependency dependency = DependencyService.getInstance()
			.newDependency(predecessor, successor, DependencyType.FS, 0L, this);

		assertEquals(revision + 1, project.getMutationState().revision());
		revision = project.getMutationState().revision();
		DependencyService.getInstance().setFields(dependency, 1L, DependencyType.Kind.FS, this);
		assertEquals(revision + 1, project.getMutationState().revision());
		revision = project.getMutationState().revision();
		DependencyService.getInstance().setFields(dependency, 1L, DependencyType.Kind.FS, this);
		assertEquals(revision, project.getMutationState().revision(), "setting identical dependency fields is a no-op");
		DependencyService.getInstance().remove(dependency, this, true);
		assertEquals(revision + 1, project.getMutationState().revision());
		revision = project.getMutationState().revision();
		DependencyService.getInstance().remove(dependency, this, true);
		assertEquals(revision, project.getMutationState().revision(), "removing a detached dependency is a no-op");
	}

	@Test
	void initializedNullSourceDependencyCreationPublishesLiveRefreshWithoutUndo() throws InvalidAssociationException {
		Project project = createProject("dependency-live-refresh");
		NormalTask predecessor = new NormalTask(project);
		NormalTask successor = new NormalTask(project);
		project.connectTask(predecessor);
		project.connectTask(successor);
		project.initialize(false, false);
		List<ObjectEvent> events = new ArrayList<>();
		ObjectEvent.Listener listener = events::add;
		project.getObjectEventManager().addListener(listener);
		long revision = project.getMutationState().revision();
		try {
			DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.FS, 0L, null);
		} finally {
			project.getObjectEventManager().removeListener(listener);
		}

		assertEquals(1, events.size());
		assertTrue(events.getFirst().isCreate());
		assertEquals(revision + 1, events.getFirst().getPublicationRevision());
		assertEquals(revision + 1, project.getMutationState().revision());
		assertFalse(project.getUndoController().canUndo(), "a source-less live refresh must not create Undo history");
	}

	@Test
	void kindApiRoundTripsThroughThePersistedIntegerBoundary() throws InvalidAssociationException {
		Project project = createProject("kind-api-project");
		NormalTask predecessor = new NormalTask(project);
		NormalTask successor = new NormalTask(project);
		project.connectTask(predecessor);
		project.connectTask(successor);

		Dependency dependency = DependencyService.getInstance().newDependency(
				predecessor, successor, DependencyType.Kind.SS, 0L, this);
		assertEquals(DependencyType.Kind.SS, dependency.getDependencyKind());
		assertEquals(DependencyType.Kind.SS.code(), dependency.getDependencyType());

		dependency.setDependencyKind(DependencyType.Kind.FF);
		assertEquals(DependencyType.Kind.FF, dependency.getDependencyKind());
		assertEquals(DependencyType.Kind.FF.code(), dependency.getDependencyType());
	}

	@Test
	void crossProjectLinksSupportEveryMicrosoftProjectDependencyType() throws InvalidAssociationException {
		Project predecessorProject = createProject("predecessor-project");
		Project successorProject = createProject("successor-project");
		NormalTask predecessor = new NormalTask(predecessorProject);
		NormalTask successor = new NormalTask(successorProject);
		predecessorProject.connectTask(predecessor);
		successorProject.connectTask(successor);

		for (int type : new int[] { DependencyType.FS, DependencyType.SS, DependencyType.FF, DependencyType.SF }) {
			long predecessorRevision = predecessorProject.getMutationState().revision();
			long successorRevision = successorProject.getMutationState().revision();
			Dependency dependency = DependencyService.getInstance().newDependency(predecessor, successor, type, 0L, this);
			assertEquals(type, dependency.getDependencyType());
			assertSame(predecessor, dependency.getPredecessor());
			assertSame(successor, dependency.getSuccessor());
			assertEquals(predecessorRevision + 1, predecessorProject.getMutationState().revision());
			assertEquals(successorRevision + 1, successorProject.getMutationState().revision());
			DependencyService.getInstance().remove(dependency, this, false);
			assertEquals(predecessorRevision + 2, predecessorProject.getMutationState().revision());
			assertEquals(successorRevision + 2, successorProject.getMutationState().revision());
		}
	}

	@Test
	void threeProjectBulkLinkPublishesAfterAllRevisionsAndKeepsUndoWithEachPredecessor() throws Exception {
		Project firstProject = createProject("bulk-first");
		Project middleProject = createProject("bulk-middle");
		Project lastProject = createProject("bulk-last");
		NormalTask first = new NormalTask(firstProject);
		NormalTask middle = new NormalTask(middleProject);
		NormalTask last = new NormalTask(lastProject);
		firstProject.connectTask(first);
		middleProject.connectTask(middle);
		lastProject.connectTask(last);
		long firstRevision = firstProject.getMutationState().revision();
		long middleRevision = middleProject.getMutationState().revision();
		long lastRevision = lastProject.getMutationState().revision();
		List<Boolean> callbacksAfterUnlock = new ArrayList<>();
		List<Long> publishedRevisions = new ArrayList<>();
		ObjectEvent.Listener callbackProbe = event -> {
			CountDownLatch checked = new CountDownLatch(1);
			AtomicBoolean acquired = new AtomicBoolean();
			Thread reader = new Thread(() -> {
				try (var ignored = middleProject.getMutationState().begin()) {
					acquired.set(true);
				} finally {
					checked.countDown();
				}
			}, "dependency-publication-probe");
			reader.start();
			try {
				callbacksAfterUnlock.add(checked.await(2, TimeUnit.SECONDS) && acquired.get());
				reader.join(2000);
			} catch (InterruptedException interrupted) {
				Thread.currentThread().interrupt();
				throw new AssertionError(interrupted);
			}
			publishedRevisions.add(event.getPublicationRevision());
		};
		middleProject.getObjectEventManager().addListener(callbackProbe);
		lastProject.getObjectEventManager().addListener(callbackProbe);
		try {
			DependencyService.getInstance().connect(Arrays.asList(first, middle, last), this, null);
		} finally {
			middleProject.getObjectEventManager().removeListener(callbackProbe);
			lastProject.getObjectEventManager().removeListener(callbackProbe);
		}

		assertEquals(firstRevision + 1, firstProject.getMutationState().revision());
		assertEquals(middleRevision + 1, middleProject.getMutationState().revision());
		assertEquals(lastRevision + 1, lastProject.getMutationState().revision());
		assertEquals(List.of(true, true), callbacksAfterUnlock);
		assertEquals(List.of(middleRevision + 1, lastRevision + 1), publishedRevisions);
		assertTrue(firstProject.getUndoController().canUndo());
		assertTrue(middleProject.getUndoController().canUndo());
		firstProject.getUndoController().undo();
		assertEquals(0, first.getSuccessorList().size());
		assertEquals(1, middle.getSuccessorList().size());
		middleProject.getUndoController().undo();
		assertEquals(0, middle.getSuccessorList().size());
		assertEquals(0, last.getPredecessorList().size());
	}

	@Test
	void bulkRemovalKeepsSameIdTasksFromDifferentProjectsInParticipantSet() throws Exception {
		Project firstProject = createProject("same-id-first-project");
		Project secondProject = createProject("same-id-second-project");
		Project thirdProject = createProject("same-id-third-project");
		Project fourthProject = createProject("same-id-fourth-project");
		NormalTask first = new NormalTask(firstProject);
		NormalTask second = new NormalTask(secondProject);
		NormalTask third = new NormalTask(thirdProject);
		NormalTask fourth = new NormalTask(fourthProject);
		for (NormalTask task : List.of(first, second, third, fourth)) task.setUniqueId(77L);
		assertEquals(first.getUniqueId(), third.getUniqueId(), "tasks in separate projects may share a unique ID");
		firstProject.connectTask(first);
		secondProject.connectTask(second);
		thirdProject.connectTask(third);
		fourthProject.connectTask(fourth);
		Dependency firstLink = DependencyService.getInstance().newDependency(first, second, DependencyType.FS, 0L, this);
		Dependency secondLink = DependencyService.getInstance().newDependency(third, fourth, DependencyType.FS, 0L, this);
		long firstRevision = firstProject.getMutationState().revision();
		long secondRevision = secondProject.getMutationState().revision();
		long thirdRevision = thirdProject.getMutationState().revision();
		long fourthRevision = fourthProject.getMutationState().revision();

		DependencyService.getInstance().remove(List.of(firstLink, secondLink), this, true);

		assertEquals(firstRevision + 1, firstProject.getMutationState().revision());
		assertEquals(secondRevision + 1, secondProject.getMutationState().revision());
		assertEquals(thirdRevision + 1, thirdProject.getMutationState().revision());
		assertEquals(fourthRevision + 1, fourthProject.getMutationState().revision());
		assertTrue(first.getSuccessorList().isEmpty());
		assertTrue(second.getPredecessorList().isEmpty());
		assertTrue(third.getSuccessorList().isEmpty());
		assertTrue(fourth.getPredecessorList().isEmpty());
	}

	@Test
	void collectionRemovalUnlinksEveryDependencyInSnapshot() throws InvalidAssociationException {
		Project project = createProject("remove-dependency-collection");
		NormalTask first = new NormalTask(project);
		NormalTask second = new NormalTask(project);
		NormalTask third = new NormalTask(project);
		project.connectTask(first);
		project.connectTask(second);
		project.connectTask(third);
		Dependency firstLink = DependencyService.getInstance().newDependency(first, second, DependencyType.FS, 0L, this);
		Dependency secondLink = DependencyService.getInstance().newDependency(second, third, DependencyType.SS, 0L, this);

		DependencyService.getInstance().remove(Arrays.asList(firstLink, secondLink), this);

		assertTrue(first.getSuccessorList().isEmpty());
		assertTrue(second.getPredecessorList().isEmpty());
		assertTrue(second.getSuccessorList().isEmpty());
		assertTrue(third.getPredecessorList().isEmpty());
	}

	@Test
	void fireTaskPredecessorsPreservesTaskAndLinkEncounterOrder() throws InvalidAssociationException {
		Project project = createProject("fire-task-predecessors-order");
		NormalTask firstPredecessor = new NormalTask(project);
		NormalTask secondPredecessor = new NormalTask(project);
		NormalTask firstSuccessor = new NormalTask(project);
		NormalTask secondSuccessor = new NormalTask(project);
		project.connectTask(firstPredecessor);
		project.connectTask(secondPredecessor);
		project.connectTask(firstSuccessor);
		project.connectTask(secondSuccessor);
		Dependency firstLink = DependencyService.getInstance().newDependency(
			firstPredecessor, firstSuccessor, DependencyType.FS, 0L, this);
		Dependency secondLink = DependencyService.getInstance().newDependency(
			secondPredecessor, firstSuccessor, DependencyType.SS, 0L, this);
		Dependency thirdLink = DependencyService.getInstance().newDependency(
			firstSuccessor, secondSuccessor, DependencyType.FF, 0L, this);
		List<Object> created = new ArrayList<>();
		project.addObjectListener(event -> created.add(event.getObject()));

		DependencyService.getInstance().fireTaskPredecessors(Arrays.asList(firstSuccessor, secondSuccessor));

		assertEquals(Arrays.asList(firstLink, secondLink, thirdLink), created);
	}

	@Test
	void rejectsACycleThatCrossesProjectBoundariesWithoutAddingAPartialLink() throws InvalidAssociationException {
		Project firstProject = createProject("first-project");
		Project secondProject = createProject("second-project");
		NormalTask first = new NormalTask(firstProject);
		NormalTask second = new NormalTask(secondProject);
		firstProject.connectTask(first);
		secondProject.connectTask(second);
		DependencyService.getInstance().newDependency(first, second, DependencyType.FS, 0L, this);

		assertThrows(InvalidAssociationException.class,
				() -> DependencyService.getInstance().newDependency(second, first, DependencyType.FS, 0L, this));
		assertEquals(1, first.getSuccessorList().size());
		assertEquals(1, second.getPredecessorList().size());
		assertEquals(0, first.getPredecessorList().size());
		assertEquals(0, second.getSuccessorList().size());
	}

	@Test
	void dependencyDataObjectHasStableIdentityAndName() throws InvalidAssociationException {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		NormalTask predecessor = new NormalTask(project);
		NormalTask successor = new NormalTask(project);
		project.connectTask(predecessor);
		project.connectTask(successor);

		Dependency dependency = DependencyService.getInstance().newDependency(predecessor, successor,
				DependencyType.FS, 0L, this);

		assertEquals(predecessor.getUniqueId() + "." + successor.getUniqueId(), dependency.getName());
		assertNotEquals(0L, dependency.getUniqueId());
		dependency.setName("link");
		assertEquals("link", dependency.getName());
		dependency.setUniqueId(42L);
		assertEquals(42L, dependency.getUniqueId());
	}

	@Test
	void newDependencyRejectsSelfLink() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		NormalTask task = new NormalTask(project);
		project.connectTask(task);

		assertThrows(InvalidAssociationException.class, () -> DependencyService.getInstance().newDependency(task, task, DependencyType.FS, 0L, this));
	}

	@Test
	void newDependencyRejectsExternalSourceTask() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		NormalTask predecessor = new NormalTask(project);
		NormalTask successor = new NormalTask(project);
		project.connectTask(predecessor);
		project.connectTask(successor);
		predecessor.setExternal(true);

		assertThrows(InvalidAssociationException.class,
				() -> DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.FS, 0L, this));
	}

	@Test
	void newDependencyAllowsWritableSubprojectTask() throws InvalidAssociationException {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		SubprojectTask predecessor = new SubprojectTask(project, true);
		NormalTask successor = new NormalTask(project);
		project.connectTask(predecessor);
		project.connectTask(successor);

		Dependency dependency = DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.FS, 0L, this);

		assertEquals(predecessor, dependency.getPredecessor());
		assertEquals(successor, dependency.getSuccessor());
	}

	@Test
	void newDependencyRejectsClosedSubprojectTask() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		SubprojectTask predecessor = new SubprojectTask(project, false);
		NormalTask successor = new NormalTask(project);
		project.connectTask(predecessor);
		project.connectTask(successor);

		assertThrows(InvalidAssociationException.class,
				() -> DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.FS, 0L, this));
	}

	@Test
	void resizingSuccessorFromEndKeepsDependencyAlignedStart() throws InvalidAssociationException {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		NormalTask predecessor = new NormalTask(project);
		NormalTask successor = new NormalTask(project);
		project.connectTask(predecessor);
		project.connectTask(successor);

		long day = CalendarOption.getInstance().getMillisPerDay();
		predecessor.setEnd(predecessor.getEffectiveWorkCalendar().add(predecessor.getStart(), day, false));
		successor.setEnd(successor.getEffectiveWorkCalendar().add(successor.getStart(), day, false));
		DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.FS, 0L, this);

		long successorStartBeforeResize = successor.getStart();
		long successorEndBeforeResize = successor.getEnd();
		long successorEndAfterResize = CalendarOption.getInstance().makeValidEnd(
				successor.getEffectiveWorkCalendar().add(successorEndBeforeResize, day, false), true);

		ScheduleService.getInstance().setInterval(this, successor, successorStartBeforeResize, successorEndAfterResize,
				new ScheduleInterval(successorStartBeforeResize, successorEndBeforeResize), undoController.getEditSupport());

		assertEquals(successorStartBeforeResize, successor.getStart());
		assertEquals(successorEndAfterResize, successor.getEnd());
	}

	@Test
	void setFieldsRollsBackLagWhenDependencyTypeChangeIsRejected() throws InvalidAssociationException {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		NormalTask predecessor = new NormalTask(project);
		NormalTask successor = new NormalTask(project);
		NormalTask child = new NormalTask(project);
		project.connectTask(predecessor);
		project.connectTask(successor);
		project.connectTask(child);
		child.setWbsParent(successor);
		successor.setWbsChildrenNodes(Collections.singletonList((Node) NodeFactory.getInstance().createNode(child)));

		Dependency dependency = DependencyService.getInstance().newDependency(predecessor, successor,
				DependencyType.FS, 0L, this);

		assertThrows(InvalidAssociationException.class,
				() -> DependencyService.getInstance().setFields(dependency, 2L * CalendarOption.getInstance().getMillisPerDay(),
						DependencyType.FF, this));

		assertEquals(0L, dependency.getLag());
		assertEquals(DependencyType.FS, dependency.getDependencyType());
		assertSame(predecessor, dependency.getPredecessor());
		assertSame(successor, dependency.getSuccessor());
	}

	@Test
	void summaryTaskPredecessorSearchRejectsChildDependencyCycles() throws InvalidAssociationException {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		NormalTask outside = new NormalTask(project);
		NormalTask parent = new NormalTask(project);
		NormalTask child = new NormalTask(project);
		project.connectTask(outside);
		project.connectTask(parent);
		project.connectTask(child);
		child.setWbsParent(parent);
		parent.setWbsChildrenNodes(Collections.singletonList(NodeFactory.getInstance().createNode(child)));
		DependencyService.getInstance().newDependency(outside, child, DependencyType.FS, 0L, this);

		assertThrows(InvalidAssociationException.class,
				() -> DependencyService.getInstance().newDependency(parent, outside, DependencyType.FS, 0L, this));
		assertEquals(1, child.getPredecessorList().size());
		assertEquals(0, outside.getPredecessorList().size());
	}

	@Test
	void connectListSkipsReadOnlyPredecessorTasks() throws InvalidAssociationException {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		NormalTask readOnly = new NormalTask(project);
		NormalTask successor = new NormalTask(project);
		project.connectTask(readOnly);
		project.connectTask(successor);
		readOnly.setExternal(true);

		DependencyService.getInstance().connect(Arrays.asList(readOnly, successor), this, null);

		assertFalse(readOnly.getSuccessorList().iterator().hasNext());
		assertFalse(successor.getPredecessorList().iterator().hasNext());
	}

	@Test
	void connectListSkipsReadonlyItemsButStillLinksEditablePairs() throws InvalidAssociationException {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		NormalTask predecessor = new NormalTask(project);
		NormalTask readOnly = new NormalTask(project);
		NormalTask successor = new NormalTask(project);
		project.connectTask(predecessor);
		project.connectTask(readOnly);
		project.connectTask(successor);
		readOnly.setExternal(true);

		DependencyService.getInstance().connect(Arrays.asList(predecessor, readOnly, successor), this, null);

		assertTrue(predecessor.getSuccessorList().iterator().hasNext());
		assertTrue(successor.getPredecessorList().iterator().hasNext());
	}

	@Test
	void connectListPredicateFiltersOnlySuccessorCandidates() throws InvalidAssociationException {
		Project predecessorProject = createProject("connect-successor-filter-predecessor");
		Project excludedProject = createProject("connect-successor-filter-excluded");
		Project successorProject = createProject("connect-successor-filter-successor");
		NormalTask predecessor = new NormalTask(predecessorProject);
		NormalTask excludedSuccessor = new NormalTask(excludedProject);
		NormalTask successor = new NormalTask(successorProject);
		predecessorProject.connectTask(predecessor);
		excludedProject.connectTask(excludedSuccessor);
		successorProject.connectTask(successor);
		long predecessorRevision = predecessorProject.getMutationState().revision();
		long excludedRevision = excludedProject.getMutationState().revision();
		long successorRevision = successorProject.getMutationState().revision();

		DependencyService.getInstance().connect(List.of(predecessor, successor, excludedSuccessor), this,
				task -> task != excludedSuccessor);

		assertTrue(excludedSuccessor.getPredecessorList().isEmpty());
		assertEquals(1, successor.getPredecessorList().size());
		assertSame(predecessor,
				((Dependency) successor.getPredecessorList().iterator().next()).getPredecessor());
		assertEquals(predecessorRevision + 1, predecessorProject.getMutationState().revision());
		assertEquals(excludedRevision, excludedProject.getMutationState().revision(),
				"projects represented only by filtered candidates must not advance");
		assertEquals(successorRevision + 1, successorProject.getMutationState().revision());
	}

	@Test
	void removeAnyDependenciesSkipsReadOnlyTasks() throws InvalidAssociationException {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		NormalTask predecessor = new NormalTask(project);
		NormalTask successor = new NormalTask(project);
		project.connectTask(predecessor);
		project.connectTask(successor);
		DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.FS, 0L, this);
		project.setReadOnly(true);

		DependencyService.getInstance().removeAnyDependencies(Arrays.asList(predecessor, successor), this);

		assertTrue(predecessor.getSuccessorList().iterator().hasNext());
		assertTrue(successor.getPredecessorList().iterator().hasNext());
	}

	@Test
	void removeAnyDependenciesUnlinksIncidentLinkWhenOnlyOneEndpointIsSelected() throws InvalidAssociationException {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		NormalTask predecessor = new NormalTask(project);
		NormalTask successor = new NormalTask(project);
		project.connectTask(predecessor);
		project.connectTask(successor);
		DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.FS, 0L, this);

		DependencyService.getInstance().removeAnyDependencies(Arrays.asList(successor), this);

		assertFalse(predecessor.getSuccessorList().iterator().hasNext());
		assertFalse(successor.getPredecessorList().iterator().hasNext());
	}

	@Test
	void removeAnyDependenciesSkipsReadonlyItemsButStillUnlinksEditablePairs() throws InvalidAssociationException {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		NormalTask predecessor = new NormalTask(project);
		NormalTask readOnly = new NormalTask(project);
		NormalTask successor = new NormalTask(project);
		project.connectTask(predecessor);
		project.connectTask(readOnly);
		project.connectTask(successor);
		DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.FS, 0L, this);
		readOnly.setExternal(true);
		Object unsupportedSelection = new Object();

		DependencyService.getInstance().removeAnyDependencies(
			Arrays.asList(unsupportedSelection, predecessor, readOnly, successor), this);

		assertFalse(predecessor.getSuccessorList().iterator().hasNext());
		assertFalse(successor.getPredecessorList().iterator().hasNext());
	}

	@Test
	void incidentDependencySnapshotSupportsRemovingOnlyTheChosenLink() throws InvalidAssociationException {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("unlink-choice-test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		NormalTask predecessor = new NormalTask(project);
		NormalTask selected = new NormalTask(project);
		NormalTask successor = new NormalTask(project);
		project.connectTask(predecessor);
		project.connectTask(selected);
		project.connectTask(successor);
		Dependency incoming = DependencyService.getInstance().newDependency(predecessor, selected, DependencyType.FS, 0L, this);
		Dependency outgoing = DependencyService.getInstance().newDependency(selected, successor, DependencyType.SS, 0L, this);

		assertEquals(Arrays.asList(incoming, outgoing), DependencyService.getInstance().getIncidentDependencies(selected));
		DependencyService.getInstance().remove(incoming, this, true);
		assertEquals(0, selected.getPredecessorList().size());
		assertEquals(1, selected.getSuccessorList().size());
		assertSame(outgoing, selected.getSuccessorList().iterator().next());
	}

	private static Project createProject(String name) {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool(name, undoController);
		return Project.createProject(resourcePool, undoController);
	}

	private static final class SubprojectTask extends NormalTask implements SubProj {
		private final boolean writable;

		private SubprojectTask(Project project, boolean writable) {
			super(project);
			this.writable = writable;
		}

		public Project getSubproject() {
			return null;
		}

		public boolean isSubprojectOpen() {
			return writable;
		}

		public boolean isValidAndOpen() {
			return writable;
		}

		public boolean isWritable() {
			return writable;
		}

		public long getSubprojectUniqueId() {
			return 0L;
		}

		public void setFetching(boolean b) {
		}

		public boolean isValid() {
			return writable;
		}

		public void setSubprojectFieldValues(java.util.Map<String, Object> subprojectFieldValues) {
		}

		public void setSubprojectUniqueId(long subprojectId) {
		}

		public void setSchedulesFromSubprojectFieldValues() {
		}
	}
}
