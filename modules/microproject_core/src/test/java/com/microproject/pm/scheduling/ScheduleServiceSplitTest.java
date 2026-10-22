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
package com.microproject.pm.scheduling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.microproject.document.ObjectEvent;

import com.microproject.options.CalendarOption;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.AssignmentService;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.resource.ResourceLevelingService;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.scheduling.IntervalConsumer;
import com.microproject.undo.DataFactoryUndoController;

class ScheduleServiceSplitTest {
	@Test
	void failedOrdinaryTaskSplitRestoresModelAndDiscardsEventsAndUndo() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		NormalTask task = new NormalTask(project) {
			@Override
			public void split(Object source, long from, long to) {
				super.split(source, from, to);
				throw new IllegalStateException("split application failed");
			}
		};
		project.connectTask(task);
		task.setDuration(3L * CalendarOption.getInstance().getMillisPerDay());
		undoController.discardAllEdits();
		task.setDirty(false);
		project.setDirty(false);
		long originalStart = task.getStart();
		long originalEnd = task.getEnd();
		long originalDuration = task.getDurationMillis();
		List<com.microproject.pm.task.TaskSplitInterval> originalDetails = task.getTaskSplitIntervals();
		long splitFrom = task.getEffectiveWorkCalendar().add(originalStart,
			CalendarOption.getInstance().getMillisPerDay(), false);
		long splitTo = task.getEffectiveWorkCalendar().add(splitFrom,
			CalendarOption.getInstance().getMillisPerDay(), false);
		long revision = project.getMutationState().revision();
		AtomicInteger objectEvents = new AtomicInteger();
		AtomicInteger scheduleEvents = new AtomicInteger();
		ObjectEvent.Listener objectListener = event -> objectEvents.incrementAndGet();
		com.microproject.pm.scheduling.ScheduleEventListener scheduleListener = event -> scheduleEvents.incrementAndGet();
		project.addObjectListener(objectListener);
		project.addScheduleListener(scheduleListener);
		try {
			assertEquals("split application failed", assertThrows(IllegalStateException.class,
				() -> ScheduleService.getInstance().split(this, task, splitFrom, splitTo,
					undoController.getEditSupport())).getMessage());
		} finally {
			project.removeObjectListener(objectListener);
			project.removeScheduleListener(scheduleListener);
		}

		assertEquals(originalDetails, task.getTaskSplitIntervals());
		assertTrue(task.getManualTaskSplitIntervals().isEmpty());
		assertTrue(task.getLevelingTaskSplitIntervals().isEmpty());
		assertEquals(originalStart, task.getStart());
		assertEquals(originalEnd, task.getEnd());
		assertEquals(originalDuration, task.getDurationMillis());
		assertFalse(task.isDirty());
		assertFalse(project.isDirty());
		assertEquals(revision, project.getMutationState().revision());
		assertFalse(undoController.canUndo());
		assertFalse(undoController.canRedo());
		assertEquals(0, objectEvents.get());
		assertEquals(0, scheduleEvents.get());
	}

	@Test
	void failedOrdinaryAssignmentSplitRestoresAssignmentAndTaskAndDiscardsEventsAndUndo() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		NormalTask task = new NormalTask(project);
		project.connectTask(task);
		long day = CalendarOption.getInstance().getMillisPerDay();
		task.setDuration(3L * day);
		Assignment sourceAssignment = AssignmentService.getInstance().newAssignment(task,
			resourcePool.newResourceInstance(), 1.0D, 0L, this);
		Assignment assignment = sourceAssignment;
		ScheduleService failingService = new ScheduleService() {
			@Override
			void applyOrdinarySplit(com.microproject.pm.scheduling.Schedule schedule, Object source, long from, long to) {
				super.applyOrdinarySplit(schedule, source, from, to);
				throw new IllegalStateException("assignment split application failed");
			}
		};
		undoController.discardAllEdits();
		assignment.setDirty(false);
		task.setDirty(false);
		project.setDirty(false);
		long originalStart = assignment.getStart();
		long originalEnd = assignment.getEnd();
		long originalTaskStart = task.getStart();
		long originalTaskEnd = task.getEnd();
		long originalDuration = task.getDurationMillis();
		List<String> originalAssignmentIntervals = assignmentIntervals(assignment);
		List<String> originalTaskIntervals = taskIntervals(task);
		long splitFrom = assignment.getEffectiveWorkCalendar().add(originalStart, day, false);
		long splitTo = assignment.getEffectiveWorkCalendar().add(splitFrom, day, false);
		long revision = project.getMutationState().revision();
		AtomicInteger objectEvents = new AtomicInteger();
		AtomicInteger scheduleEvents = new AtomicInteger();
		ObjectEvent.Listener objectListener = event -> objectEvents.incrementAndGet();
		com.microproject.pm.scheduling.ScheduleEventListener scheduleListener = event -> scheduleEvents.incrementAndGet();
		project.addObjectListener(objectListener);
		project.addScheduleListener(scheduleListener);
		try {
			assertEquals("assignment split application failed", assertThrows(IllegalStateException.class,
				() -> failingService.split(this, assignment, splitFrom, splitTo,
					undoController.getEditSupport())).getMessage());
		} finally {
			project.removeObjectListener(objectListener);
			project.removeScheduleListener(scheduleListener);
		}

		assertEquals(originalAssignmentIntervals, assignmentIntervals(assignment));
		assertEquals(originalTaskIntervals, taskIntervals(task));
		assertEquals(originalStart, assignment.getStart());
		assertEquals(originalEnd, assignment.getEnd());
		assertEquals(originalTaskStart, task.getStart());
		assertEquals(originalTaskEnd, task.getEnd());
		assertEquals(originalDuration, task.getDurationMillis());
		assertFalse(assignment.isDirty());
		assertFalse(task.isDirty());
		assertFalse(project.isDirty());
		assertEquals(revision, project.getMutationState().revision());
		assertFalse(undoController.canUndo());
		assertFalse(undoController.canRedo());
		assertEquals(0, objectEvents.get());
		assertEquals(0, scheduleEvents.get());
	}

	@Test
	void failedIntervalMoveRestoresTaskAndDiscardsEventsAndUndo() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		NormalTask task = new NormalTask(project) {
			@Override
			public void moveInterval(Object source, long start, long end, ScheduleInterval oldInterval, boolean isChild) {
				super.moveInterval(source, start, end, oldInterval, isChild);
				throw new IllegalStateException("interval application failed");
			}
		};
		project.connectTask(task);
		task.setDuration(2L * CalendarOption.getInstance().getMillisPerDay());
		undoController.discardAllEdits();
		task.setDirty(false);
		project.setDirty(false);
		long originalStart = task.getStart();
		long originalEnd = task.getEnd();
		long revision = project.getMutationState().revision();
		AtomicInteger objectEvents = new AtomicInteger();
		AtomicInteger scheduleEvents = new AtomicInteger();
		ObjectEvent.Listener objectListener = event -> objectEvents.incrementAndGet();
		com.microproject.pm.scheduling.ScheduleEventListener scheduleListener = event -> scheduleEvents.incrementAndGet();
		project.addObjectListener(objectListener);
		project.addScheduleListener(scheduleListener);
		try {
			assertEquals("interval application failed", assertThrows(IllegalStateException.class,
				() -> ScheduleService.getInstance().setInterval(this, task, originalStart + CalendarOption.getInstance().getMillisPerDay(),
					originalEnd + CalendarOption.getInstance().getMillisPerDay(), new ScheduleInterval(originalStart, originalEnd),
					undoController.getEditSupport())).getMessage());
		} finally {
			project.removeObjectListener(objectListener);
			project.removeScheduleListener(scheduleListener);
		}

		assertEquals(originalStart, task.getStart());
		assertEquals(originalEnd, task.getEnd());
		assertFalse(task.isDirty());
		assertFalse(project.isDirty());
		assertEquals(revision, project.getMutationState().revision());
		assertFalse(undoController.canUndo());
		assertFalse(undoController.canRedo());
		assertEquals(0, objectEvents.get());
		assertEquals(0, scheduleEvents.get());
	}

	@Test
	void failedAssignmentIntervalMoveRestoresAssignmentTaskAndDiscardsEventsAndUndo() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		NormalTask task = new NormalTask(project);
		project.connectTask(task);
		task.setDuration(2L * CalendarOption.getInstance().getMillisPerDay());
		Assignment original = AssignmentService.getInstance().newAssignment(task, resourcePool.newResourceInstance(), 1.0D, 0L, this);
		Assignment assignment = original;
		ScheduleService failingService = new ScheduleService() {
			@Override
			protected void applyInterval(com.microproject.pm.scheduling.Schedule schedule, Object source,
					long start, long end, ScheduleInterval interval) {
				super.applyInterval(schedule, source, start, end, interval);
				if (schedule == assignment) throw new IllegalStateException("assignment interval application failed");
			}
		};
		undoController.discardAllEdits();
		assignment.setDirty(false);
		task.setDirty(false);
		project.setDirty(false);
		long originalStart = original.getStart();
		long originalEnd = original.getEnd();
		long taskStart = task.getStart();
		long taskEnd = task.getEnd();
		long revision = project.getMutationState().revision();
		AtomicInteger objectEvents = new AtomicInteger();
		AtomicInteger scheduleEvents = new AtomicInteger();
		ObjectEvent.Listener objectListener = event -> objectEvents.incrementAndGet();
		com.microproject.pm.scheduling.ScheduleEventListener scheduleListener = event -> scheduleEvents.incrementAndGet();
		project.addObjectListener(objectListener);
		project.addScheduleListener(scheduleListener);
		try {
			assertEquals("assignment interval application failed", assertThrows(IllegalStateException.class,
				() -> failingService.setInterval(this, assignment,
					originalStart + CalendarOption.getInstance().getMillisPerDay(),
					originalEnd + CalendarOption.getInstance().getMillisPerDay(), new ScheduleInterval(originalStart, originalEnd),
					undoController.getEditSupport())).getMessage());
		} finally {
			project.removeObjectListener(objectListener);
			project.removeScheduleListener(scheduleListener);
		}

		assertEquals(originalStart, original.getStart());
		assertEquals(originalEnd, original.getEnd());
		assertEquals(taskStart, task.getStart());
		assertEquals(taskEnd, task.getEnd());
		assertFalse(assignment.isDirty());
		assertFalse(original.isDirty());
		assertFalse(task.isDirty());
		assertFalse(project.isDirty());
		assertEquals(revision, project.getMutationState().revision());
		assertFalse(undoController.canUndo());
		assertFalse(undoController.canRedo());
		assertEquals(0, objectEvents.get());
		assertEquals(0, scheduleEvents.get());
	}

	@Test
	void splittingProjectDoesNotCreateUndoEditWhenNothingChanges() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);

		long originalStart = project.getStart();
		long originalEnd = project.getEnd();

		ScheduleService.getInstance().split(this, project, originalStart, originalStart, undoController.getEditSupport());

		assertEquals(originalStart, project.getStart());
		assertEquals(originalEnd, project.getEnd());
		assertFalse(undoController.canUndo());
	}

	@Test
	void splitReturnsFalseForReadOnlySchedules() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		NormalTask task = new NormalTask(project);
		project.connectTask(task);
		task.setExternal(true);

		long originalStart = task.getStart();
		long originalEnd = task.getEnd();

		boolean changed = ScheduleService.getInstance().split(this, task, originalStart, originalStart, undoController.getEditSupport());

		assertFalse(changed);
		assertEquals(originalStart, task.getStart());
		assertEquals(originalEnd, task.getEnd());
		assertFalse(undoController.canUndo());
	}

	@Test
	void setCompletedReturnsFalseForReadOnlySchedules() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		NormalTask task = new NormalTask(project);
		project.connectTask(task);
		task.setExternal(true);

		long originalCompleted = task.getCompletedThrough();
		boolean changed = ScheduleService.getInstance().setCompleted(this, task, originalCompleted + 24L * 60L * 60L * 1000L,
				undoController.getEditSupport());

		assertFalse(changed);
		assertEquals(originalCompleted, task.getCompletedThrough());
		assertFalse(undoController.canUndo());
	}

	@Test
	void splittingUnassignedTaskAddsTaskOwnedGapAndUndoRedoRestoreIt() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		NormalTask task = new NormalTask(project);
		project.connectTask(task);
		task.setDuration(3L * CalendarOption.getInstance().getMillisPerDay());
		undoController.discardAllEdits();
		project.setDirty(false);
		task.setDirty(false);
		long originalStart = task.getStart();
		long originalEnd = task.getEnd();
		long splitFrom = task.getEffectiveWorkCalendar().add(originalStart,
			CalendarOption.getInstance().getMillisPerDay(), false);
		long splitEnd = task.getEffectiveWorkCalendar().add(splitFrom,
			CalendarOption.getInstance().getMillisPerDay(), false);

		boolean changed = ScheduleService.getInstance().split(this, task, splitFrom, splitEnd,
			undoController.getEditSupport());

		assertTrue(changed);
		assertEquals(originalStart, task.getStart());
		assertTrue(task.getEnd() > originalEnd, "task finish must move to retain work after the nonworking gap");
		assertEquals(List.of(originalStart + ":" + splitFrom, splitEnd + ":" + task.getEnd()), taskIntervals(task));
		assertTrue(undoController.canUndo());
		undoController.undo();
		assertTrue(task.getTaskSplitIntervals().isEmpty());
		assertEquals(List.of(originalStart + ":" + originalEnd), taskIntervals(task));
		undoController.redo();
		assertEquals(List.of(originalStart + ":" + splitFrom, splitEnd + ":" + task.getEnd()), taskIntervals(task));
		long splitFinish = task.getEnd();
		project.recalculate();
		assertEquals(splitFinish, task.getEnd(), "ordinary schedule recalculation must not erase a task-owned gap");
		assertEquals(List.of(originalStart + ":" + splitFrom, splitEnd + ":" + task.getEnd()), taskIntervals(task));
	}

	@Test
	void splittingAssignedTaskAddsOneGapAndUndoRedoRestoreIt() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		NormalTask task = new NormalTask(project);
		project.connectTask(task);
		long day = CalendarOption.getInstance().getMillisPerDay();
		task.setDuration(3L * day);
		Assignment assignment = AssignmentService.getInstance().newAssignment(task,
			resourcePool.newResourceInstance(), 1.0D, 0L, this);
		Assignment secondAssignment = AssignmentService.getInstance().newAssignment(task,
			resourcePool.newResourceInstance(), 1.0D, 0L, this);
		long splitFrom = assignment.getEffectiveWorkCalendar().add(task.getStart(), day, false);
		long splitTo = assignment.getEffectiveWorkCalendar().add(splitFrom, day, false);
		List<String> originalIntervals = assignmentIntervals(assignment);
		List<String> originalSecondIntervals = assignmentIntervals(secondAssignment);
		List<String> originalTaskIntervals = taskIntervals(task);
		undoController.discardAllEdits();

		boolean changed = ScheduleService.getInstance().split(this, task, splitFrom, splitTo,
			undoController.getEditSupport());

		assertTrue(changed);
		List<String> splitIntervals = assignmentIntervals(assignment);
		List<String> splitTaskIntervals = taskIntervals(task);
		assertEquals(originalIntervals, splitIntervals, "task-owned split must preserve assignment contours");
		assertEquals(originalSecondIntervals, assignmentIntervals(secondAssignment),
			"task-owned split must preserve every assigned resource contour");
		assertTrue(splitTaskIntervals.size() > originalTaskIntervals.size());
		assertTrue(undoController.canUndo());
		undoController.undo();
		assertEquals(originalIntervals, assignmentIntervals(assignment));
		assertEquals(originalSecondIntervals, assignmentIntervals(secondAssignment));
		assertEquals(originalTaskIntervals, taskIntervals(task));
		undoController.redo();
		assertEquals(splitIntervals, assignmentIntervals(assignment));
		assertEquals(originalSecondIntervals, assignmentIntervals(secondAssignment));
		assertEquals(splitTaskIntervals, taskIntervals(task));
		long splitFinish = task.getEnd();
		project.recalculate();
		assertEquals(splitFinish, task.getEnd(), "ordinary schedule recalculation must preserve task-owned gaps with assignments");
		assertEquals(splitTaskIntervals, taskIntervals(task));
	}

	@Test
	void overlappingSplitExtendsTheGapOnlyByNewlyAddedTime() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		NormalTask task = new NormalTask(project);
		project.connectTask(task);
		long day = CalendarOption.getInstance().getMillisPerDay();
		long halfDay = day / 2L;
		task.setDuration(4L * day);
		long originalStart = task.getStart();
		long originalEnd = task.getEnd();
		long splitFrom = task.getEffectiveWorkCalendar().add(originalStart, day, false);
		long splitTo = task.getEffectiveWorkCalendar().add(splitFrom, day, false);

		assertTrue(ScheduleService.getInstance().split(this, task, splitFrom, splitTo, undoController.getEditSupport()));
		long finishAfterFirstSplit = task.getEnd();
		long overlapFrom = task.getEffectiveWorkCalendar().add(splitFrom, halfDay, false);
		long extendedTo = task.getEffectiveWorkCalendar().add(splitTo, halfDay, false);

		assertTrue(ScheduleService.getInstance().split(this, task, overlapFrom, extendedTo, undoController.getEditSupport()));
		assertEquals(finishAfterFirstSplit + halfDay, task.getEnd(),
			"overlapping a prior gap must extend the schedule only by its newly added portion");
		assertEquals(1, task.getTaskSplitIntervals().size(), "overlapping split intervals must have one canonical union");
		assertTrue(task.getEnd() > originalEnd);
	}

	@Test
	void repeatingAnExistingSplitIsANoOpWithoutUndoOrDirtyState() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		NormalTask task = new NormalTask(project);
		project.connectTask(task);
		task.setDuration(3L * CalendarOption.getInstance().getMillisPerDay());
		long splitFrom = task.getEffectiveWorkCalendar().add(task.getStart(),
			CalendarOption.getInstance().getMillisPerDay(), false);
		long splitTo = task.getEffectiveWorkCalendar().add(splitFrom,
			CalendarOption.getInstance().getMillisPerDay(), false);
		assertTrue(ScheduleService.getInstance().split(this, task, splitFrom, splitTo, undoController.getEditSupport()));
		undoController.discardAllEdits();
		project.setDirty(false);
		task.setDirty(false);

		assertFalse(ScheduleService.getInstance().split(this, task, splitFrom, splitTo, undoController.getEditSupport()));
		assertFalse(undoController.canUndo(), "a repeated split must not create phantom Undo history");
		assertFalse(project.isDirty(), "a repeated split must not dirty the project");
		assertFalse(task.isDirty(), "a repeated split must not dirty the task");
	}

	@Test
	void clearLevelingRemovesOnlyGeneratedSplitCoverageAndUndoRestoresIt() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		NormalTask task = new NormalTask(project);
		project.connectTask(task);
		long day = CalendarOption.getInstance().getMillisPerDay();
		task.setDuration(5L * day);
		long start = task.getStart();
		long originalEnd = task.getEnd();
		long manualFrom = task.getEffectiveWorkCalendar().add(start, day, false);
		long manualTo = task.getEffectiveWorkCalendar().add(manualFrom, day, false);
		assertTrue(ScheduleService.getInstance().split(this, task, manualFrom, manualTo, null));
		long generatedFrom = task.getEffectiveWorkCalendar().add(start, day + day / 2L, false);
		long generatedTo = task.getEffectiveWorkCalendar().add(start, 2L * day + day / 2L, false);
		assertTrue(ScheduleService.getInstance().splitForLeveling(this, task, generatedFrom, generatedTo));
		assertEquals(1, task.getTaskSplitIntervals().size(), "manual and generated overlap should remain one visible gap");
		assertEquals(1, task.getManualTaskSplitIntervals().size());
		assertEquals(1, task.getLevelingTaskSplitIntervals().size());
		long endWithBoth = task.getEnd();

		new ResourceLevelingService().clear(project, undoController.getEditSupport());
		assertEquals(List.of(new com.microproject.pm.task.TaskSplitInterval(manualFrom - start, manualTo - start)),
			task.getTaskSplitIntervals(), "clear must preserve the manual interval even where it overlapped a generated split");
		assertTrue(task.getEnd() < endWithBoth);
		assertTrue(undoController.canUndo(), "clear must be undoable when it removes generated split state");
		undoController.undo();
		assertEquals(endWithBoth, task.getEnd());
		assertEquals(1, task.getLevelingTaskSplitIntervals().size());
		undoController.redo();
		assertEquals(originalEnd + (manualTo - manualFrom), task.getEnd(),
			"after clearing, only the manual gap's elapsed duration remains");
		assertTrue(task.getLevelingTaskSplitIntervals().isEmpty());
	}

	@Test
	void manualOwnershipAddedOverGeneratedSplitIsUndoableAndSurvivesClear() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		NormalTask task = new NormalTask(project);
		project.connectTask(task);
		long day = CalendarOption.getInstance().getMillisPerDay();
		task.setDuration(5L * day);
		long start = task.getStart();
		long generatedFrom = task.getEffectiveWorkCalendar().add(start, day, false);
		long generatedTo = task.getEffectiveWorkCalendar().add(generatedFrom, day, false);
		assertTrue(ScheduleService.getInstance().splitForLeveling(this, task, generatedFrom, generatedTo));
		List<com.microproject.pm.task.TaskSplitInterval> visibleBeforeManual = task.getTaskSplitIntervals();
		long finishBeforeManual = task.getEnd();
		undoController.discardAllEdits();

		assertTrue(ScheduleService.getInstance().split(this, task, generatedFrom, generatedTo,
			undoController.getEditSupport()),
			"adding manual ownership is a real edit even when it does not change visible split coverage");
		assertEquals(visibleBeforeManual, task.getTaskSplitIntervals());
		assertEquals(finishBeforeManual, task.getEnd());
		assertEquals(1, task.getManualTaskSplitIntervals().size());
		assertEquals(1, task.getLevelingTaskSplitIntervals().size());
		assertTrue(undoController.canUndo());

		undoController.undo();
		assertTrue(task.getManualTaskSplitIntervals().isEmpty());		assertEquals(1, task.getLevelingTaskSplitIntervals().size());
		undoController.redo();
		assertEquals(1, task.getManualTaskSplitIntervals().size());
		assertEquals(1, task.getLevelingTaskSplitIntervals().size());

		new ResourceLevelingService().clear(project, undoController.getEditSupport());
		assertEquals(visibleBeforeManual, task.getTaskSplitIntervals(),
			"clearing leveling must retain the gap that now has manual ownership");
		assertEquals(1, task.getManualTaskSplitIntervals().size());
		assertTrue(task.getLevelingTaskSplitIntervals().isEmpty());
	}

	private List<String> taskIntervals(NormalTask task) {
		List<String> intervals = new ArrayList<>();
		task.consumeIntervals(interval -> intervals.add(interval.getStart() + ":" + interval.getEnd()));
		return List.copyOf(intervals);
	}

	private List<String> assignmentIntervals(Assignment assignment) {
		List<String> intervals = new ArrayList<>();
		assignment.consumeIntervals(interval -> intervals.add(interval.getStart() + ":" + interval.getEnd()));
		return List.copyOf(intervals);
	}

	@Test
	void setCompletedClampsProgressDragToTheTaskInterval() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		NormalTask task = new NormalTask(project);
		project.connectTask(task);
		task.setDuration(2L * 8L * 60L * 60L * 1000L);

		assertTrue(ScheduleService.getInstance().setCompleted(this, task, task.getEnd() + 60L * 60L * 1000L,
			undoController.getEditSupport()));
		assertTrue(task.getCompletedThrough() >= task.getStart());
		assertTrue(task.getCompletedThrough() <= task.getEnd());

		assertTrue(ScheduleService.getInstance().setCompleted(this, task, task.getStart() - 60L * 60L * 1000L,
			undoController.getEditSupport()));
		assertTrue(task.getCompletedThrough() >= task.getStart());
		assertTrue(task.getCompletedThrough() <= task.getEnd());
	}

	@Test
	void setCompletedUndoRestoresPercentDerivedProgressExactly() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		NormalTask task = project.createScriptedTask();
		task.setDuration(3L * 8L * 60L * 60L * 1000L);
		project.recalculate();
		task.setPercentComplete(0.4d);
		long originalCompleted = task.getCompletedThrough();
		long laterCompleted = task.getEffectiveWorkCalendar().add(originalCompleted, 8L * 60L * 60L * 1000L, false);

		assertTrue(ScheduleService.getInstance().setCompleted(this, task, laterCompleted, undoController.getEditSupport()));
		long updatedCompleted = task.getCompletedThrough();
		assertTrue(updatedCompleted > originalCompleted);
		undoController.undo();
		assertEquals(originalCompleted, task.getCompletedThrough(), "Undo must restore the exact prior progress instant"
			+ "; percent=" + task.getPercentComplete() + ", actualDuration=" + task.getActualDuration()
			+ ", actualStart=" + task.getActualStart() + ", assignments=" + task.getAssignments().size());
		undoController.redo();
		assertEquals(updatedCompleted, task.getCompletedThrough());
	}

	@Test
	void consumeIntervalsRecoversAfterConsumerThrows() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		NormalTask task = new NormalTask(project);
		project.connectTask(task);

		assertThrows(RuntimeException.class, () ->
				ScheduleService.getInstance().consumeIntervals(task, new IntervalConsumer() {
					public void consumeInterval(ScheduleInterval interval) {
						throw new RuntimeException("boom");
					}
				}));

		AtomicInteger count = new AtomicInteger();
		ScheduleService.getInstance().consumeIntervals(task, new IntervalConsumer() {
			public void consumeInterval(ScheduleInterval interval) {
				count.incrementAndGet();
			}
		});

		assertEquals(1, count.get());
	}
}
