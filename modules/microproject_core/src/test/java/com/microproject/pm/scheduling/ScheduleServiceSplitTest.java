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

import com.microproject.options.CalendarOption;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.AssignmentService;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.scheduling.IntervalConsumer;
import com.microproject.undo.DataFactoryUndoController;

class ScheduleServiceSplitTest {
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
