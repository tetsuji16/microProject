/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.microproject.field.FieldContext;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.undo.DataFactoryUndoController;

class TaskSheetScheduleWorkflowTest {
	@Test
	void durationEditRecalculatesSuccessorAsOneUndoableTransaction() throws Exception {
		Project project = createProject();
		NormalTask predecessor = createTask(project);
		NormalTask successor = createTask(project);
		long day = day();
		predecessor.setDuration(day);
		successor.setDuration(day);
		DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.FS, 0L, this);
		project.recalculate();
		long start = predecessor.getStart();
		long oldFinish = predecessor.getEnd();
		long oldSuccessorStart = successor.getStart();
		long revision = project.getMutationState().revision();

		predecessor.setDuration(2L * day, taskSheetContext());

		assertEquals(start, predecessor.getStart());
		assertTrue(predecessor.getEnd() > oldFinish);
		assertTrue(successor.getStart() > oldSuccessorStart);
		assertEquals(revision + 1L, project.getMutationState().revision());
		var undo = project.getUndoController();
		undo.undo();
		assertEquals(start, predecessor.getStart());
		assertEquals(oldFinish, predecessor.getEnd());
		assertEquals(oldSuccessorStart, successor.getStart());
		undo.redo();
		assertEquals(start, predecessor.getStart());
		assertTrue(predecessor.getEnd() > oldFinish);
		assertTrue(successor.getStart() > oldSuccessorStart);
	}

	@Test
	void lateDurationFailureRestoresDatesMetadataCountersHistoryAndObservers() throws Exception {
		Project project = createProject();
		AtomicBoolean injectFailure = new AtomicBoolean();
		AtomicBoolean sawParentUpdate = new AtomicBoolean();
		RuntimeException injected = new IllegalStateException("injected after duration manual-date update");
		class FailingTask extends NormalTask {
			long originalFinish;
			NormalTask parent;
			FailingTask(Project owner) { super(owner); }
			@Override protected void assignParentActualDatesFromChildren() {
				super.assignParentActualDatesFromChildren();
				if (injectFailure.compareAndSet(true, false)) {
					sawParentUpdate.set(getEnd() != originalFinish && parent != null);
					throw injected;
				}
			}
		}
		FailingTask task = new FailingTask(project);
		project.connectTask(task);
		project.getSchedulingAlgorithm().addObject(task);
		NormalTask parent = createTask(project);
		task.parent = parent;
		task.setWbsParent(parent);
		parent.setWbsChildrenNodes(List.of(com.microproject.grouping.core.NodeFactory.getInstance().createNode(task)));
		NormalTask successor = createTask(project);
		long day = day();
		task.setDuration(day);
		successor.setDuration(day);
		DependencyService.getInstance().newDependency(task, successor, DependencyType.FS, 0L, this);
		project.recalculate();
		long start = task.getStart();
		long finish = task.getEnd();
		long duration = task.getDuration();
		long successorStart = successor.getStart();
		long parentStart = parent.getStart();
		long parentFinish = parent.getEnd();
		long parentActualStart = parent.getActualStart();
		long parentActualFinish = parent.getActualFinish();
		int taskState = task.getCalculationStateCount();
		int successorState = successor.getCalculationStateCount();
		int parentState = parent.getCalculationStateCount();
		boolean estimated = task.isEstimated();
		int displayUnit = task.getDurationDisplayUnit();
		boolean taskDirty = task.isDirty();
		boolean successorDirty = successor.isDirty();
		boolean parentDirty = parent.isDirty();
		boolean projectDirty = project.isDirty();
		long revision = project.getMutationState().revision();
		var undo = project.getUndoController();
		List<String> history = List.copyOf(undo.getEditNames());
		boolean canUndo = undo.canUndo();
		boolean canRedo = undo.canRedo();
		AtomicInteger objectEvents = new AtomicInteger();
		AtomicInteger scheduleEvents = new AtomicInteger();
		project.addObjectListener(event -> objectEvents.incrementAndGet());
		project.addScheduleListener(event -> scheduleEvents.incrementAndGet());
		task.originalFinish = finish;
		injectFailure.set(true);

		assertSame(injected, assertThrows(RuntimeException.class,
			() -> task.setDuration(2L * day, taskSheetContext())));

		assertTrue(sawParentUpdate.get());
		assertEquals(start, task.getStart());
		assertEquals(finish, task.getEnd());
		assertEquals(duration, task.getDuration());
		assertEquals(successorStart, successor.getStart());
		assertEquals(parentStart, parent.getStart());
		assertEquals(parentFinish, parent.getEnd());
		assertEquals(parentActualStart, parent.getActualStart());
		assertEquals(parentActualFinish, parent.getActualFinish());
		assertEquals(taskState, task.getCalculationStateCount());
		assertEquals(successorState, successor.getCalculationStateCount());
		assertEquals(parentState, parent.getCalculationStateCount());
		assertEquals(estimated, task.isEstimated());
		assertEquals(displayUnit, task.getDurationDisplayUnit());
		assertEquals(taskDirty, task.isDirty());
		assertEquals(successorDirty, successor.isDirty());
		assertEquals(parentDirty, parent.isDirty());
		assertEquals(projectDirty, project.isDirty());
		assertEquals(revision, project.getMutationState().revision());
		assertEquals(history, undo.getEditNames());
		assertEquals(canUndo, undo.canUndo());
		assertEquals(canRedo, undo.canRedo());
		assertEquals(0, objectEvents.get());
		assertEquals(0, scheduleEvents.get());
	}

	@Test
	void wbsParentDurationEditUpdatesOnlyManualEnvelope() {
		Project project = createProject();
		NormalTask parent = createTask(project);
		NormalTask child = createTask(project);
		child.setWbsParent(parent);
		parent.setWbsChildrenNodes(List.of(com.microproject.grouping.core.NodeFactory.getInstance().createNode(child)));
		child.setDuration(day());
		long childStart = child.getStart();
		long childFinish = child.getEnd();

		parent.setDuration(3L * day(), taskSheetContext());

		assertEquals(3L * day(), parent.getSummaryEnvelope().getManualDuration().longValue());
		assertEquals(childStart, child.getStart());
		assertEquals(childFinish, child.getEnd());
	}

	private Project createProject() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("task sheet test", undo), undo);
		project.initialize(false, false);
		return project;
	}

	private NormalTask createTask(Project project) {
		NormalTask task = new NormalTask(project);
		project.connectTask(task);
		project.getSchedulingAlgorithm().addObject(task);
		return task;
	}

	private FieldContext taskSheetContext() {
		FieldContext context = new FieldContext();
		context.setTaskSheetUpdate(true);
		return context;
	}

	private long day() {
		return com.microproject.options.CalendarOption.getInstance().getMillisPerDay();
	}
}
