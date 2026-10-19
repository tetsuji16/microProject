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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.microproject.association.InvalidAssociationException;
import com.microproject.association.Association;
import com.microproject.association.AssociationList;
import com.microproject.document.ObjectEvent;
import com.microproject.field.FieldParseException;
import com.microproject.options.CalendarOption;
import com.microproject.grouping.core.NodeFactory;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.pm.scheduling.ScheduleInterval;
import com.microproject.pm.scheduling.ScheduleService;
import com.microproject.pm.criticalpath.CriticalPath;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.SubProj;
import com.microproject.pm.task.Task;
import com.microproject.pm.task.ExternalTaskManager;
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
	void setFieldsPublicationFailureKeepsCommittedFieldsAndUndoRedoHistory() throws InvalidAssociationException {
		Project project = createProject("dependency-set-fields-publication-failure");
		NormalTask predecessor = new NormalTask(project);
		NormalTask successor = new NormalTask(project);
		project.connectTask(predecessor);
		project.connectTask(successor);
		Dependency dependency = DependencyService.getInstance()
			.newDependency(predecessor, successor, DependencyType.FS, 0L, this);
		long revision = project.getMutationState().revision();
		RuntimeException publicationFailure = new IllegalStateException("listener failure");
		AtomicReference<Object> publishedObject = new AtomicReference<>();
		AtomicReference<Long> publicationRevision = new AtomicReference<>();
		AtomicInteger listenerCalls = new AtomicInteger();
		ObjectEvent.Listener listener = throwingPublicationObserver(publicationFailure, publishedObject,
			publicationRevision, listenerCalls);
		project.getObjectEventManager().addListener(listener);
		try {
			assertSame(publicationFailure, assertThrows(RuntimeException.class,
				() -> DependencyService.getInstance().setFields(dependency,
					2L * CalendarOption.getInstance().getMillisPerDay(), DependencyType.Kind.SS, this)));
		} finally {
			project.getObjectEventManager().removeListener(listener);
		}

		long committedLag = 2L * CalendarOption.getInstance().getMillisPerDay();
		assertEquals(committedLag, dependency.getLag());
		assertEquals(DependencyType.SS, dependency.getDependencyType());
		assertEquals(revision + 1, project.getMutationState().revision());
		assertSame(dependency, publishedObject.get());
		assertEquals(revision + 1, publicationRevision.get());
		assertTrue(project.getUndoController().canUndo());

		project.getObjectEventManager().fireUpdateEvent(this, dependency);
		assertEquals(1, listenerCalls.get(), "listener was removed in finally");
		project.getUndoController().undo();
		assertEquals(0L, dependency.getLag());
		assertEquals(DependencyType.FS, dependency.getDependencyType());
		assertTrue(project.getUndoController().canRedo());
		project.getUndoController().redo();
		assertEquals(committedLag, dependency.getLag());
		assertEquals(DependencyType.SS, dependency.getDependencyType());
	}

	@Test
	void legacyUpdateServicePublicationFailureKeepsCommittedDirtyStateAndRevision() throws InvalidAssociationException {
		Project project = createProject("dependency-legacy-update-publication-failure");
		NormalTask predecessor = new NormalTask(project);
		NormalTask successor = new NormalTask(project);
		project.connectTask(predecessor);
		project.connectTask(successor);
		Dependency dependency = DependencyService.getInstance()
			.newDependency(predecessor, successor, DependencyType.FS, 0L, this);
		dependency.setDirty(false);
		long revision = project.getMutationState().revision();
		RuntimeException publicationFailure = new IllegalStateException("listener failure");
		AtomicReference<Object> publishedObject = new AtomicReference<>();
		AtomicReference<Long> publicationRevision = new AtomicReference<>();
		AtomicInteger listenerCalls = new AtomicInteger();
		ObjectEvent.Listener listener = throwingPublicationObserver(publicationFailure, publishedObject,
			publicationRevision, listenerCalls);
		project.getObjectEventManager().addListener(listener);
		try {
			assertSame(publicationFailure, assertThrows(RuntimeException.class,
				() -> dependency.doUpdateService(this)));
		} finally {
			project.getObjectEventManager().removeListener(listener);
		}

		assertTrue(dependency.isDirty());
		assertEquals(revision + 1, project.getMutationState().revision());
		assertEquals(1, listenerCalls.get(), "one update event was published");
		assertSame(dependency, publishedObject.get());
		assertEquals(revision + 1, publicationRevision.get());

		project.getObjectEventManager().fireUpdateEvent(this, dependency);
		assertEquals(1, listenerCalls.get(), "listener was removed in finally");
	}

	private static ObjectEvent.Listener throwingPublicationObserver(RuntimeException failure,
		AtomicReference<Object> publishedObject, AtomicReference<Long> publicationRevision,
		AtomicInteger listenerCalls) {
		return event -> {
			listenerCalls.incrementAndGet();
			publishedObject.set(event.getObject());
			publicationRevision.set(event.getPublicationRevision());
			throw failure;
		};
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
	void externalTaskResolutionRebindsBothDirectionsWithoutUndo() {
		Project placeholderProject = createProject("external-placeholder");
		Project resolvedProject = createProject("external-resolved");
		NormalTask placeholder = new NormalTask(placeholderProject);
		NormalTask other = new NormalTask(placeholderProject);
		NormalTask resolved = new NormalTask(resolvedProject);
		placeholder.setExternal(true);
		placeholderProject.connectTask(placeholder);
		placeholderProject.connectTask(other);
		resolvedProject.connectTask(resolved);
		Dependency outgoing = Dependency.getInstance(placeholder, other);
		placeholder.getSuccessorList().add(outgoing);
		other.getPredecessorList().add(outgoing);
		Dependency incoming = Dependency.getInstance(other, placeholder);
		other.getSuccessorList().add(incoming);
		placeholder.getPredecessorList().add(incoming);
		placeholderProject.initializeProject();
		resolvedProject.initializeProject();
		addTaskToOutline(placeholderProject, placeholder);
		addTaskToOutline(placeholderProject, other);
		addTaskToOutline(resolvedProject, resolved);
		placeholderProject.initialize(false, false);
		resolvedProject.initialize(false, false);
		DependencyService.getInstance().updateSentinels(outgoing);
		DependencyService.getInstance().updateSentinels(incoming);
		assertFalse(hasStartSentinel(placeholderProject, placeholder));
		assertFalse(hasEndSentinel(placeholderProject, placeholder));
		assertFalse(hasStartSentinel(placeholderProject, other));
		assertFalse(hasEndSentinel(placeholderProject, other));
		assertTrue(hasStartSentinel(resolvedProject, resolved));
		assertTrue(hasEndSentinel(resolvedProject, resolved));
		List<ObjectEvent> events = new ArrayList<>();
		ObjectEvent.Listener listener = events::add;
		placeholderProject.getObjectEventManager().addListener(listener);
		resolvedProject.getObjectEventManager().addListener(listener);
		long placeholderRevision = placeholderProject.getMutationState().revision();
		long resolvedRevision = resolvedProject.getMutationState().revision();
		placeholderProject.setDirty(false);
		placeholderProject.setGroupDirty(false);
		resolvedProject.setDirty(false);
		resolvedProject.setGroupDirty(false);
		try {
			DependencyService.getInstance().resolveExternalTask(placeholder, resolved, this);
		} finally {
			placeholderProject.getObjectEventManager().removeListener(listener);
			resolvedProject.getObjectEventManager().removeListener(listener);
		}

		assertFalse(placeholder.getSuccessorList().contains(outgoing));
		assertFalse(placeholder.getPredecessorList().contains(incoming));
		assertEquals(1, resolved.getSuccessorList().stream().filter(value -> value == outgoing).count());
		assertEquals(1, resolved.getPredecessorList().stream().filter(value -> value == incoming).count());
		assertSame(resolved, outgoing.getPredecessor());
		assertSame(other, outgoing.getSuccessor());
		assertSame(other, incoming.getPredecessor());
		assertSame(resolved, incoming.getSuccessor());
		assertEquals(4, events.size(), "each rebind publishes delete then create");
		assertEquals(placeholderRevision + 1, placeholderProject.getMutationState().revision());
		assertEquals(resolvedRevision + 1, resolvedProject.getMutationState().revision());
		assertFalse(placeholderProject.getUndoController().canUndo());
		assertFalse(resolvedProject.getUndoController().canUndo());
		assertFalse(outgoing.isDirty());
		assertFalse(incoming.isDirty());
		assertFalse(placeholderProject.isDirty());
		assertFalse(placeholderProject.isGroupDirty());
		assertFalse(resolvedProject.isDirty());
		assertFalse(resolvedProject.isGroupDirty());
		assertFalse(hasStartSentinel(resolvedProject, resolved));
		assertFalse(hasEndSentinel(resolvedProject, resolved));
	}

	@Test
	void externalTaskResolutionRollsBackTheWholeBatchOnMidRebindFailure() {
		Project placeholderProject = createProject("external-rollback-placeholder");
		Project resolvedProject = createProject("external-rollback-resolved");
		NormalTask placeholder = new NormalTask(placeholderProject);
		NormalTask firstSuccessor = new NormalTask(placeholderProject);
		NormalTask secondSuccessor = new NormalTask(placeholderProject);
		FailingSuccessorListTask resolved = new FailingSuccessorListTask(resolvedProject);
		placeholderProject.connectTask(placeholder);
		placeholderProject.connectTask(firstSuccessor);
		placeholderProject.connectTask(secondSuccessor);
		resolvedProject.connectTask(resolved);
		placeholder.setExternal(true);
		Dependency first = Dependency.getInstance(placeholder, firstSuccessor);
		Dependency second = Dependency.getInstance(placeholder, secondSuccessor);
		for (Dependency dependency : List.of(first, second)) {
			placeholder.getSuccessorList().add(dependency);
			((Task) dependency.getSuccessor()).getPredecessorList().add(dependency);
		}
		placeholderProject.initializeProject();
		resolvedProject.initializeProject();
		for (Task task : List.of(placeholder, firstSuccessor, secondSuccessor)) addTaskToOutline(placeholderProject, task);
		addTaskToOutline(resolvedProject, resolved);
		placeholderProject.initialize(false, false);
		resolvedProject.initialize(false, false);
		List<ObjectEvent> events = new ArrayList<>();
		ObjectEvent.Listener listener = events::add;
		placeholderProject.getObjectEventManager().addListener(listener);
		resolved.failOnSecondAdd();
		long placeholderRevision = placeholderProject.getMutationState().revision();
		long resolvedRevision = resolvedProject.getMutationState().revision();
		try {
			assertThrows(IllegalStateException.class,
					() -> DependencyService.getInstance().resolveExternalTask(placeholder, resolved, this));
		} finally {
			placeholderProject.getObjectEventManager().removeListener(listener);
		}

		assertSame(placeholder, first.getPredecessor());
		assertSame(placeholder, second.getPredecessor());
		assertTrue(placeholder.getSuccessorList().contains(first));
		assertTrue(placeholder.getSuccessorList().contains(second));
		assertTrue(resolved.getSuccessorList().isEmpty());
		assertTrue(hasStartSentinel(resolvedProject, resolved));
		assertTrue(hasEndSentinel(resolvedProject, resolved));
		assertTrue(events.isEmpty(), "failed rebind events must not be published");
		assertEquals(placeholderRevision, placeholderProject.getMutationState().revision());
		assertEquals(resolvedRevision, resolvedProject.getMutationState().revision());
		assertFalse(placeholderProject.getUndoController().canUndo());
		assertFalse(resolvedProject.getUndoController().canUndo());
	}

	@Test
	void externalTaskManagerKeepsPlaceholderAndCachesOnRebindFailure() {
		Project placeholderProject = createProject("external-manager-placeholder");
		Project resolvedProject = createProject("external-manager-resolved");
		NormalTask placeholder = new NormalTask(placeholderProject);
		NormalTask firstSuccessor = new NormalTask(placeholderProject);
		NormalTask secondSuccessor = new NormalTask(placeholderProject);
		FailingSuccessorListTask resolved = new FailingSuccessorListTask(resolvedProject);
		placeholder.setExternal(true);
		placeholderProject.connectTask(placeholder);
		placeholderProject.connectTask(firstSuccessor);
		placeholderProject.connectTask(secondSuccessor);
		resolvedProject.connectTask(resolved);
		placeholder.setProjectId(resolvedProject.getUniqueId());
		placeholder.setUniqueId(resolved.getUniqueId());
		for (Task successor : List.of(firstSuccessor, secondSuccessor)) {
			Dependency dependency = Dependency.getInstance(placeholder, successor);
			placeholder.getSuccessorList().add(dependency);
			successor.getPredecessorList().add(dependency);
			DependencyService.getInstance().updateSentinels(dependency);
		}
		placeholderProject.initializeProject();
		resolvedProject.initializeProject();
		for (Task task : List.of(placeholder, firstSuccessor, secondSuccessor)) addTaskToOutline(placeholderProject, task);
		addTaskToOutline(resolvedProject, resolved);
		placeholderProject.initialize(false, false);
		resolvedProject.initialize(false, false);
		ExternalTaskManager manager = new ExternalTaskManager();
		manager.add(placeholder);
		assertEquals(resolvedProject.getUniqueId(), placeholder.getProjectId());
		assertSame(resolved, resolvedProject.findByUniqueId(placeholder.getUniqueId()));
		resolved.resetInvalidateCount();
		resolved.failOnSecondAdd();
		placeholderProject.setDirty(false);
		placeholderProject.setGroupDirty(false);
		resolvedProject.setDirty(false);
		resolvedProject.setGroupDirty(false);

		assertThrows(IllegalStateException.class, () -> manager.handleExternalTasks(resolvedProject, true, false));
		assertTrue(placeholder.isExternal());
		assertTrue(placeholderProject.getTaskList().contains(placeholder));
		assertEquals(0, resolved.invalidateCount());
		assertTrue(hasStartSentinel(resolvedProject, resolved));
		assertTrue(hasEndSentinel(resolvedProject, resolved));
		assertFalse(placeholderProject.isDirty());
		assertFalse(placeholderProject.isGroupDirty());
		assertFalse(resolvedProject.isDirty());
		assertFalse(resolvedProject.isGroupDirty());

		// A second open broadcast proves the failed attempt stayed registered and can retry.
		manager.handleExternalTasks(resolvedProject, true, false);
		assertFalse(placeholder.isExternal());
		assertFalse(placeholderProject.getTaskList().contains(placeholder));
		assertTrue(resolved.invalidateCount() > 0);
		assertTrue(hasStartSentinel(resolvedProject, resolved), "the resolved task has no predecessors after outgoing-only rebind");
		assertFalse(hasEndSentinel(resolvedProject, resolved));
		assertNull(placeholderProject.getTaskOutline().search(placeholder), "successful retry removes the owner projection");
	}

	@Test
	void externalTaskResolutionRemovesPlaceholderFromOwnerAndRootProjections() {
		Project rootProject = createProject("external-root-removal");
		Project ownerProject = createProject("external-owner-removal");
		Project resolvedProject = createProject("external-resolved-removal");
		SubprojectTask subproject = new SubprojectTask(rootProject, true);
		rootProject.connectTask(subproject);
		NormalTask placeholder = new NormalTask(ownerProject);
		NormalTask resolved = new NormalTask(resolvedProject);
		ownerProject.connectTask(placeholder);
		resolvedProject.connectTask(resolved);
		placeholder.setExternal(true);
		placeholder.setWbsParent(subproject);
		placeholder.setProjectId(resolvedProject.getUniqueId());
		placeholder.setUniqueId(resolved.getUniqueId());
		addTaskToOutline(ownerProject, placeholder);
		addTaskToOutline(resolvedProject, resolved);
		addTaskToOutline(rootProject, placeholder);
		resolvedProject.initialize(false, false);
		ownerProject.initialize(false, false);
		rootProject.initialize(false, false);
		ExternalTaskManager manager = new ExternalTaskManager();
		manager.add(placeholder);

		manager.handleExternalTasks(resolvedProject, true, false);

		assertFalse(ownerProject.getTaskList().contains(placeholder));
		assertNull(ownerProject.getTaskOutline().search(placeholder));
		assertNull(rootProject.getTaskOutline().search(placeholder));
		assertFalse(placeholder.isExternal());
	}

	@Test
	void externalTaskResolutionRestoresOwnerWhenRootProjectionRemovalFails() {
		Project rootProject = createProject("external-root-removal-failure");
		Project ownerProject = createProject("external-owner-removal-failure");
		Project resolvedProject = createProject("external-resolved-removal-failure");
		SubprojectTask subproject = new SubprojectTask(rootProject, true);
		rootProject.connectTask(subproject);
		NormalTask placeholder = new NormalTask(ownerProject);
		NormalTask resolved = new NormalTask(resolvedProject);
		ownerProject.connectTask(placeholder);
		resolvedProject.connectTask(resolved);
		placeholder.setExternal(true);
		placeholder.setWbsParent(subproject);
		placeholder.setProjectId(resolvedProject.getUniqueId());
		placeholder.setUniqueId(resolved.getUniqueId());
		addTaskToOutline(ownerProject, placeholder);
		addTaskToOutline(resolvedProject, resolved);
		addTaskToOutline(rootProject, placeholder);
		resolvedProject.initialize(false, false);
		ownerProject.initialize(false, false);
		rootProject.initialize(false, false);
		NodeModel rootSecondaryOutline = rootProject.getTaskOutline(1);
		rootSecondaryOutline.add(null, NodeFactory.getInstance().createNode(placeholder), NodeModel.SILENT);
		NodeModel[] rootOutlines = ((com.microproject.grouping.core.OutlineCollectionImpl) rootProject.getTaskOutlines())
				.getOutlines();
		rootOutlines[1] = (NodeModel) Proxy.newProxyInstance(NodeModel.class.getClassLoader(),
				new Class<?>[] { NodeModel.class }, (proxy, method, args) -> {
					if (method.getName().equals("remove")) throw new IllegalStateException("injected root projection removal failure");
					try { return method.invoke(rootSecondaryOutline, args); }
					catch (InvocationTargetException failure) { throw failure.getCause(); }
				});
		ExternalTaskManager manager = new ExternalTaskManager();
		manager.add(placeholder);
		List<ObjectEvent> events = new ArrayList<>();
		ObjectEvent.Listener listener = events::add;
		ownerProject.getObjectEventManager().addListener(listener);
		rootProject.getObjectEventManager().addListener(listener);
		long ownerRevision = ownerProject.getMutationState().revision();
		long rootRevision = rootProject.getMutationState().revision();
		ownerProject.setDirty(false);
		ownerProject.setGroupDirty(false);
		rootProject.setDirty(false);
		rootProject.setGroupDirty(false);

		try {
			assertThrows(IllegalStateException.class, () -> manager.handleExternalTasks(resolvedProject, true, false));
		} finally {
			ownerProject.getObjectEventManager().removeListener(listener);
			rootProject.getObjectEventManager().removeListener(listener);
		}

		assertTrue(ownerProject.getTaskList().contains(placeholder));
		assertSame(placeholder, ownerProject.getTaskOutline().search(placeholder).getImpl());
		assertSame(placeholder, rootProject.getTaskOutline().search(placeholder).getImpl());
		assertSame(placeholder, rootSecondaryOutline.search(placeholder).getImpl());
		assertTrue(placeholder.isExternal());
		assertTrue(events.isEmpty());
		assertEquals(ownerRevision, ownerProject.getMutationState().revision());
		assertEquals(rootRevision, rootProject.getMutationState().revision());
		assertFalse(ownerProject.isDirty());
		assertFalse(ownerProject.isGroupDirty());
		assertFalse(rootProject.isDirty());
		assertFalse(rootProject.isGroupDirty());

		rootOutlines[1] = rootSecondaryOutline;
		manager.handleExternalTasks(resolvedProject, true, false);
		assertFalse(ownerProject.getTaskList().contains(placeholder));
		assertNull(ownerProject.getTaskOutline().search(placeholder));
		assertNull(rootProject.getTaskOutline().search(placeholder));
		assertNull(rootSecondaryOutline.search(placeholder));
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
	void crossProjectLinkAndUnlinkCanRecordUndoInCommandProject() throws Exception {
		Project predecessorProject = createProject("command-owner-predecessor");
		Project commandProject = createProject("command-owner-successor");
		NormalTask predecessor = new NormalTask(predecessorProject);
		NormalTask successor = new NormalTask(commandProject);
		predecessorProject.connectTask(predecessor);
		commandProject.connectTask(successor);
		predecessorProject.getUndoController().clear();
		commandProject.getUndoController().clear();

		DependencyService.getInstance().connect(List.of(predecessor, successor), this, null, commandProject);
		assertEquals(1, predecessor.getSuccessorList().size());
		assertEquals(1, successor.getPredecessorList().size());
		assertFalse(predecessorProject.getUndoController().canUndo());
		assertTrue(commandProject.getUndoController().canUndo());

		commandProject.getUndoController().undo();
		assertTrue(predecessor.getSuccessorList().isEmpty());
		assertTrue(successor.getPredecessorList().isEmpty());
		assertTrue(commandProject.getUndoController().canRedo());
		commandProject.getUndoController().redo();
		assertEquals(1, predecessor.getSuccessorList().size());
		assertEquals(1, successor.getPredecessorList().size());
		assertFalse(predecessorProject.getUndoController().canUndo());

		predecessorProject.getUndoController().clear();
		commandProject.getUndoController().clear();
		DependencyService.getInstance().removeAnyDependencies(List.of(successor), this, commandProject);
		assertTrue(predecessor.getSuccessorList().isEmpty());
		assertTrue(successor.getPredecessorList().isEmpty());
		assertFalse(predecessorProject.getUndoController().canUndo());
		assertTrue(commandProject.getUndoController().canUndo());

		commandProject.getUndoController().undo();
		assertEquals(1, predecessor.getSuccessorList().size());
		assertEquals(1, successor.getPredecessorList().size());
		assertTrue(commandProject.getUndoController().canRedo());
		commandProject.getUndoController().redo();
		assertTrue(predecessor.getSuccessorList().isEmpty());
		assertTrue(successor.getPredecessorList().isEmpty());
		assertFalse(predecessorProject.getUndoController().canUndo());
	}

	@Test
	void crossProjectNewDependencyCanRecordUndoInCommandProject() throws Exception {
		Project predecessorProject = createProject("new-dependency-owner-predecessor");
		Project commandProject = createProject("new-dependency-owner-command");
		NormalTask predecessor = new NormalTask(predecessorProject);
		NormalTask successor = new NormalTask(commandProject);
		predecessorProject.connectTask(predecessor);
		commandProject.connectTask(successor);
		predecessorProject.getUndoController().clear();
		commandProject.getUndoController().clear();

		DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.Kind.FS,
				0L, this, commandProject);

		assertEquals(1, predecessor.getSuccessorList().size());
		assertEquals(1, successor.getPredecessorList().size());
		assertFalse(predecessorProject.getUndoController().canUndo());
		assertTrue(commandProject.getUndoController().canUndo());

		commandProject.getUndoController().undo();
		assertTrue(predecessor.getSuccessorList().isEmpty());
		assertTrue(successor.getPredecessorList().isEmpty());
		assertTrue(commandProject.getUndoController().canRedo());
		commandProject.getUndoController().redo();
		assertEquals(1, predecessor.getSuccessorList().size());
		assertEquals(1, successor.getPredecessorList().size());
		assertFalse(predecessorProject.getUndoController().canUndo());
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
	void failedBulkRemovalRestoresGraphEventsRevisionAndUndoState() throws InvalidAssociationException {
		Project project = createProject("atomic-dependency-removal");
		NormalTask first = new NormalTask(project);
		FailingSentinelTask middle = new FailingSentinelTask(project);
		NormalTask last = new NormalTask(project);
		project.connectTask(first);
		project.connectTask(middle);
		project.connectTask(last);
		Dependency firstLink = DependencyService.getInstance().newDependency(first, middle, DependencyType.FS, 0L, this);
		Dependency secondLink = DependencyService.getInstance().newDependency(middle, last, DependencyType.SS, 0L, this);
		List<Object> notifications = new ArrayList<>();
		project.addObjectListener(event -> notifications.add(event.getObject()));
		project.beginEventBatch();
		project.getObjectEventManager().fireUpdateEvent(this, first);
		int pendingBefore = project.getObjectEventManager().pendingEventCount();
		project.fireScheduleChanged(this, com.microproject.pm.scheduling.ScheduleEvent.SCHEDULE);
		int pendingProjectEventsBefore = project.pendingEventCount();
		List<Association> firstPredecessors = new ArrayList<>(first.getPredecessorList());
		List<Association> firstSuccessors = new ArrayList<>(first.getSuccessorList());
		List<Association> middlePredecessors = new ArrayList<>(middle.getPredecessorList());
		List<Association> middleSuccessors = new ArrayList<>(middle.getSuccessorList());
		List<Association> lastPredecessors = new ArrayList<>(last.getPredecessorList());
		List<Association> lastSuccessors = new ArrayList<>(last.getSuccessorList());
		long revision = project.getMutationState().revision();
		String undoName = project.getUndoController().getUndoPresentationName();

		middle.failNextEndSentinelUpdate();
		assertThrows(IllegalStateException.class,
			() -> DependencyService.getInstance().remove(List.of(firstLink, secondLink), this, true));

		assertEquals(firstPredecessors, new ArrayList<>(first.getPredecessorList()));
		assertEquals(firstSuccessors, new ArrayList<>(first.getSuccessorList()));
		assertEquals(middlePredecessors, new ArrayList<>(middle.getPredecessorList()));
		assertEquals(middleSuccessors, new ArrayList<>(middle.getSuccessorList()));
		assertEquals(lastPredecessors, new ArrayList<>(last.getPredecessorList()));
		assertEquals(lastSuccessors, new ArrayList<>(last.getSuccessorList()));
		assertTrue(first.getSuccessorList().contains(firstLink));
		assertTrue(middle.getSuccessorList().contains(secondLink));
		assertEquals(revision, project.getMutationState().revision());
		assertEquals(pendingBefore, project.getObjectEventManager().pendingEventCount());
		assertEquals(pendingProjectEventsBefore, project.pendingEventCount());
		assertEquals(0, notifications.size());
		assertEquals(undoName, project.getUndoController().getUndoPresentationName());
		assertTrue(project.getUndoController().canUndo());
		project.endEventBatch(false);
		assertEquals(0, project.getObjectEventManager().pendingEventCount());
		assertEquals(0, project.pendingEventCount());

		project.getUndoController().undo();
		assertTrue(first.getSuccessorList().contains(firstLink));
		assertFalse(middle.getSuccessorList().contains(secondLink),
			"the last history entry must remain the second link's creation, not a failed removal");
	}

	@Test
	void failedBulkConnectRestoresGraphEventsRevisionAndUndoState() {
		Project project = createProject("atomic-dependency-connect");
		NormalTask first = new NormalTask(project);
		FailingSentinelTask middle = new FailingSentinelTask(project);
		NormalTask last = new NormalTask(project);
		project.connectTask(first);
		project.connectTask(middle);
		project.connectTask(last);
		List<Object> notifications = new ArrayList<>();
		project.addObjectListener(event -> notifications.add(event.getObject()));
		project.beginEventBatch();
		project.getObjectEventManager().fireUpdateEvent(this, first);
		int pendingBefore = project.getObjectEventManager().pendingEventCount();
		project.fireScheduleChanged(this, com.microproject.pm.scheduling.ScheduleEvent.SCHEDULE);
		int pendingProjectEventsBefore = project.pendingEventCount();
		List<Association> firstPredecessors = new ArrayList<>(first.getPredecessorList());
		List<Association> firstSuccessors = new ArrayList<>(first.getSuccessorList());
		List<Association> middlePredecessors = new ArrayList<>(middle.getPredecessorList());
		List<Association> middleSuccessors = new ArrayList<>(middle.getSuccessorList());
		List<Association> lastPredecessors = new ArrayList<>(last.getPredecessorList());
		List<Association> lastSuccessors = new ArrayList<>(last.getSuccessorList());
		long revision = project.getMutationState().revision();
		String undoName = project.getUndoController().getUndoPresentationName();
		String redoName = project.getUndoController().getRedoPresentationName();
		boolean canUndo = project.getUndoController().canUndo();
		boolean canRedo = project.getUndoController().canRedo();

		middle.failNextEndSentinelUpdate();
		assertThrows(IllegalStateException.class,
			() -> DependencyService.getInstance().connect(List.of(first, middle, last), this, null));

		assertEquals(firstPredecessors, new ArrayList<>(first.getPredecessorList()));
		assertEquals(firstSuccessors, new ArrayList<>(first.getSuccessorList()));
		assertEquals(middlePredecessors, new ArrayList<>(middle.getPredecessorList()));
		assertEquals(middleSuccessors, new ArrayList<>(middle.getSuccessorList()));
		assertEquals(lastPredecessors, new ArrayList<>(last.getPredecessorList()));
		assertEquals(lastSuccessors, new ArrayList<>(last.getSuccessorList()));
		assertEquals(revision, project.getMutationState().revision());
		assertEquals(pendingBefore, project.getObjectEventManager().pendingEventCount());
		assertEquals(pendingProjectEventsBefore, project.pendingEventCount());
		assertEquals(0, notifications.size());
		assertEquals(undoName, project.getUndoController().getUndoPresentationName());
		assertEquals(redoName, project.getUndoController().getRedoPresentationName());
		assertEquals(canUndo, project.getUndoController().canUndo());
		assertEquals(canRedo, project.getUndoController().canRedo());
		project.endEventBatch(false);
		assertEquals(0, project.getObjectEventManager().pendingEventCount());
		assertEquals(0, project.pendingEventCount());
	}

	@Test
	void failedSingleDependencyCreationRestoresGraphAndUndoState() {
		Project project = createProject("atomic-single-dependency-creation");
		FailingSentinelTask predecessor = new FailingSentinelTask(project);
		NormalTask successor = new NormalTask(project);
		project.connectTask(predecessor);
		project.connectTask(successor);
		long revision = project.getMutationState().revision();
		String undoName = project.getUndoController().getUndoPresentationName();
		predecessor.failNextEndSentinelUpdate();

		assertThrows(IllegalStateException.class, () -> DependencyService.getInstance()
			.newDependency(predecessor, successor, DependencyType.FS, 0L, this));

		assertTrue(predecessor.getSuccessorList().isEmpty());
		assertTrue(successor.getPredecessorList().isEmpty());
		assertEquals(revision, project.getMutationState().revision());
		assertEquals(undoName, project.getUndoController().getUndoPresentationName());
	}

	@Test
	void failedSingleDependencyConnectRestoresGraphAndUndoState() {
		Project project = createProject("atomic-single-dependency-connect");
		FailingSentinelTask predecessor = new FailingSentinelTask(project);
		NormalTask successor = new NormalTask(project);
		project.connectTask(predecessor);
		project.connectTask(successor);
		Dependency dependency = Dependency.getInstance(predecessor, successor, DependencyType.FS, 0L);
		long revision = project.getMutationState().revision();
		String undoName = project.getUndoController().getUndoPresentationName();
		predecessor.failNextEndSentinelUpdate();

		assertThrows(IllegalStateException.class, () -> DependencyService.getInstance().connect(dependency, this));

		assertTrue(predecessor.getSuccessorList().isEmpty());
		assertTrue(successor.getPredecessorList().isEmpty());
		assertEquals(revision, project.getMutationState().revision());
		assertEquals(undoName, project.getUndoController().getUndoPresentationName());
	}

	@Test
	void failedDependencyInitializationRestoresEndpointsGraphEventsAndUndoState() {
		Project project = createProject("atomic-dependency-initialization");
		NormalTask oldPredecessor = new NormalTask(project);
		NormalTask oldSuccessor = new NormalTask(project);
		FailingSentinelTask predecessor = new FailingSentinelTask(project);
		NormalTask successor = new NormalTask(project);
		project.connectTask(oldPredecessor);
		project.connectTask(oldSuccessor);
		project.connectTask(predecessor);
		project.connectTask(successor);
		Dependency dependency = Dependency.getInstance(oldPredecessor, oldSuccessor, DependencyType.FS, 0L);
		List<Association> oldPredecessorSuccessors = new ArrayList<>(oldPredecessor.getSuccessorList());
		List<Association> oldSuccessorPredecessors = new ArrayList<>(oldSuccessor.getPredecessorList());
		List<Association> predecessorSuccessors = new ArrayList<>(predecessor.getSuccessorList());
		List<Association> successorPredecessors = new ArrayList<>(successor.getPredecessorList());
		long revision = project.getMutationState().revision();
		String undoName = project.getUndoController().getUndoPresentationName();
		List<Object> notifications = new ArrayList<>();
		project.addObjectListener(event -> notifications.add(event.getObject()));
		project.beginEventBatch();
		project.getObjectEventManager().fireUpdateEvent(this, oldPredecessor);
		int pendingObjectEvents = project.getObjectEventManager().pendingEventCount();
		project.fireScheduleChanged(this, com.microproject.pm.scheduling.ScheduleEvent.SCHEDULE);
		int pendingProjectEvents = project.pendingEventCount();
		predecessor.failNextEndSentinelUpdate();

		assertThrows(IllegalStateException.class, () -> DependencyService.getInstance()
			.initDependency(dependency, predecessor, successor, this));

		assertSame(oldPredecessor, dependency.getPredecessor());
		assertSame(oldSuccessor, dependency.getSuccessor());
		assertEquals(oldPredecessorSuccessors, new ArrayList<>(oldPredecessor.getSuccessorList()));
		assertEquals(oldSuccessorPredecessors, new ArrayList<>(oldSuccessor.getPredecessorList()));
		assertEquals(predecessorSuccessors, new ArrayList<>(predecessor.getSuccessorList()));
		assertEquals(successorPredecessors, new ArrayList<>(successor.getPredecessorList()));
		assertFalse(dependency.isDirty());
		assertEquals(revision, project.getMutationState().revision());
		assertEquals(pendingObjectEvents, project.getObjectEventManager().pendingEventCount());
		assertEquals(pendingProjectEvents, project.pendingEventCount());
		assertTrue(notifications.isEmpty());
		assertEquals(undoName, project.getUndoController().getUndoPresentationName());
		project.endEventBatch(false);
	}

	@Test
	void failedSingleDependencyRemovalRestoresGraphAndUndoState() throws InvalidAssociationException {
		Project project = createProject("atomic-single-dependency-removal");
		FailingSentinelTask predecessor = new FailingSentinelTask(project);
		NormalTask successor = new NormalTask(project);
		project.connectTask(predecessor);
		project.connectTask(successor);
		Dependency dependency = DependencyService.getInstance()
			.newDependency(predecessor, successor, DependencyType.FS, 0L, this);
		long revision = project.getMutationState().revision();
		String undoName = project.getUndoController().getUndoPresentationName();
		predecessor.failNextEndSentinelUpdate();

		assertThrows(IllegalStateException.class,
			() -> DependencyService.getInstance().remove(dependency, this, true));

		assertTrue(predecessor.getSuccessorList().contains(dependency));
		assertTrue(successor.getPredecessorList().contains(dependency));
		assertEquals(revision, project.getMutationState().revision());
		assertEquals(undoName, project.getUndoController().getUndoPresentationName());
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
	void setFieldsRollsBackPartialMutationWhenTypeApplicationFailsAtRuntime() throws InvalidAssociationException {
		Project project = createProject("dependency-set-fields-apply-failure");
		NormalTask predecessor = new NormalTask(project);
		NormalTask successor = new NormalTask(project);
		project.connectTask(predecessor);
		project.connectTask(successor);
		Dependency dependency = DependencyService.getInstance()
			.newDependency(predecessor, successor, DependencyType.FS, 0L, this);
		long revision = project.getMutationState().revision();
		String undoName = project.getUndoController().getUndoPresentationName();
		String redoName = project.getUndoController().getRedoPresentationName();
		boolean canUndo = project.getUndoController().canUndo();
		boolean canRedo = project.getUndoController().canRedo();
		List<ObjectEvent> events = new ArrayList<>();
		ObjectEvent.Listener listener = events::add;
		project.getObjectEventManager().addListener(listener);
		try {
			assertThrows(NullPointerException.class,
				() -> DependencyService.getInstance().setFields(dependency, 123L, null, this));
		} finally {
			project.getObjectEventManager().removeListener(listener);
		}

		assertEquals(0L, dependency.getLag());
		assertEquals(DependencyType.FS, dependency.getDependencyType());
		assertSame(predecessor, dependency.getPredecessor());
		assertSame(successor, dependency.getSuccessor());
		assertEquals(revision, project.getMutationState().revision());
		assertTrue(events.isEmpty());
		assertEquals(undoName, project.getUndoController().getUndoPresentationName());
		assertEquals(redoName, project.getUndoController().getRedoPresentationName());
		assertEquals(canUndo, project.getUndoController().canUndo());
		assertEquals(canRedo, project.getUndoController().canRedo());
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
		project.beginEventBatch();
		project.getObjectEventManager().fireUpdateEvent(this, predecessor);
		project.fireScheduleChanged(this, com.microproject.pm.scheduling.ScheduleEvent.SCHEDULE);
		List<Association> predecessorPredecessors = new ArrayList<>(predecessor.getPredecessorList());
		List<Association> predecessorSuccessors = new ArrayList<>(predecessor.getSuccessorList());
		List<Association> successorPredecessors = new ArrayList<>(successor.getPredecessorList());
		List<Association> successorSuccessors = new ArrayList<>(successor.getSuccessorList());
		List<Association> childPredecessors = new ArrayList<>(child.getPredecessorList());
		List<Association> childSuccessors = new ArrayList<>(child.getSuccessorList());
		long dependencyLag = dependency.getLag();
		int dependencyType = dependency.getDependencyType();
		NormalTask dependencyPredecessor = (NormalTask) dependency.getPredecessor();
		NormalTask dependencySuccessor = (NormalTask) dependency.getSuccessor();
		boolean dependencyDirty = dependency.isDirty();
		boolean predecessorDirty = predecessor.isDirty();
		boolean successorDirty = successor.isDirty();
		boolean childDirty = child.isDirty();
		boolean projectDirty = project.isDirty();
		boolean projectGroupDirty = project.isGroupDirty();
		long revision = project.getMutationState().revision();
		int pendingObjectEvents = project.getObjectEventManager().pendingEventCount();
		int pendingProjectEvents = project.pendingEventCount();
		String undoName = project.getUndoController().getUndoPresentationName();
		String redoName = project.getUndoController().getRedoPresentationName();
		boolean canUndo = project.getUndoController().canUndo();
		boolean canRedo = project.getUndoController().canRedo();

		assertThrows(InvalidAssociationException.class,
				() -> DependencyService.getInstance().setFields(dependency, 2L * CalendarOption.getInstance().getMillisPerDay(),
						DependencyType.FF, this));

		assertEquals(predecessorPredecessors, new ArrayList<>(predecessor.getPredecessorList()));
		assertEquals(predecessorSuccessors, new ArrayList<>(predecessor.getSuccessorList()));
		assertEquals(successorPredecessors, new ArrayList<>(successor.getPredecessorList()));
		assertEquals(successorSuccessors, new ArrayList<>(successor.getSuccessorList()));
		assertEquals(childPredecessors, new ArrayList<>(child.getPredecessorList()));
		assertEquals(childSuccessors, new ArrayList<>(child.getSuccessorList()));
		assertEquals(dependencyLag, dependency.getLag());
		assertEquals(dependencyType, dependency.getDependencyType());
		assertSame(dependencyPredecessor, dependency.getPredecessor());
		assertSame(dependencySuccessor, dependency.getSuccessor());
		assertEquals(dependencyDirty, dependency.isDirty());
		assertEquals(predecessorDirty, predecessor.isDirty());
		assertEquals(successorDirty, successor.isDirty());
		assertEquals(childDirty, child.isDirty());
		assertEquals(projectDirty, project.isDirty());
		assertEquals(projectGroupDirty, project.isGroupDirty());
		assertEquals(revision, project.getMutationState().revision());
		assertEquals(pendingObjectEvents, project.getObjectEventManager().pendingEventCount());
		assertEquals(pendingProjectEvents, project.pendingEventCount());
		assertEquals(undoName, project.getUndoController().getUndoPresentationName());
		assertEquals(redoName, project.getUndoController().getRedoPresentationName());
		assertEquals(canUndo, project.getUndoController().canUndo());
		assertEquals(canRedo, project.getUndoController().canRedo());
		project.endEventBatch(false);
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

	private static void addTaskToOutline(Project project, Task task) {
		Node node = NodeFactory.getInstance().createNode(task);
		project.addToDefaultOutline(null, node);
	}

	private static boolean hasStartSentinel(Project project, Task task) {
		try {
			var field = CriticalPath.class.getDeclaredField("startSentinel");
			field.setAccessible(true);
			Task sentinel = (Task) field.get(project.getSchedulingAlgorithm());
			for (Association association : sentinel.getSuccessorList())
				if (((Dependency) association).getSuccessor() == task) return true;
			return false;
		} catch (ReflectiveOperationException failure) {
			throw new AssertionError(failure);
		}
	}

	private static boolean hasEndSentinel(Project project, Task task) {
		try {
			var field = CriticalPath.class.getDeclaredField("finishSentinel");
			field.setAccessible(true);
			Task sentinel = (Task) field.get(project.getSchedulingAlgorithm());
			for (Association association : sentinel.getPredecessorList())
				if (((Dependency) association).getPredecessor() == task) return true;
			return false;
		} catch (ReflectiveOperationException failure) {
			throw new AssertionError(failure);
		}
	}

	private static final class FailingSuccessorListTask extends NormalTask {
		private final AssociationList successorList;
		private boolean failOnSecondAdd;
		private int invalidateCount;
		private FailingSuccessorListTask(Project project) {
			super(project);
			this.successorList = new AssociationList() {
				private int additions;
				@Override public boolean add(Association association) {
					if (failOnSecondAdd && ++additions == 2)
						throw new IllegalStateException("injected external dependency rebind failure");
					return super.add(association);
				}
			};
		}
		private void failOnSecondAdd() { failOnSecondAdd = true; }
		private int invalidateCount() { return invalidateCount; }
		private void resetInvalidateCount() { invalidateCount = 0; }
		@Override public void invalidateSchedules() { invalidateCount++; super.invalidateSchedules(); }
		@Override public AssociationList getSuccessorList() { return successorList; }
	}

	private static final class FailingSentinelTask extends NormalTask {
		private boolean failNextEndSentinelUpdate;

		private FailingSentinelTask(Project project) { super(project); }

		private void failNextEndSentinelUpdate() { failNextEndSentinelUpdate = true; }

		@Override public void updateEndSentinel() {
			if (failNextEndSentinelUpdate) {
				failNextEndSentinelUpdate = false;
				throw new IllegalStateException("injected sentinel update failure");
			}
			super.updateEndSentinel();
		}
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
