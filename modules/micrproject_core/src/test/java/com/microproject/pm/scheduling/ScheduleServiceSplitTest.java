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

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.NormalTask;
import com.microproject.functor.IntervalConsumer;
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
