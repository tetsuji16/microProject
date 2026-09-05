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
package com.microproject.pm.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.microproject.options.CalendarOption;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.undo.DataFactoryUndoController;

class ManualAndInactiveTaskSchedulingTest {
	@Test
	void newTasksDefaultToAutomaticSchedulingLikeMicrosoftProject() {
		Project project = project();

		assertFalse(project.newNormalTaskInstance().isManuallyScheduled());
		assertFalse(project.newNormalTaskInstance(false).isManuallyScheduled());
		assertFalse(((Task) project.createLocalTaskNode(null).getImpl()).isManuallyScheduled());
		assertFalse(((Task) project.createLocalTaskNode(null, false).getImpl()).isManuallyScheduled());
	}

	@Test
	void manuallyScheduledDatesSurviveFullRecalculation() {
		Project project = project();
		NormalTask task = task(project, "Draft");
		long start = project.getWorkCalendar().add(project.getStart(), day() * 5L, false);
		long finish = project.getWorkCalendar().add(start, day() * 2L, false);

		task.setManualDates(start, finish);
		project.recalculate();

		assertTrue(task.isManuallyScheduled());
		assertEquals(start, task.getStart());
		assertEquals(finish, task.getEnd());
	}

	@Test
	void inactivePredecessorDoesNotPushSuccessor() throws Exception {
		Project project = project();
		NormalTask inactive = task(project, "Alternative");
		NormalTask successor = task(project, "Committed");
		inactive.setDuration(day() * 10L);
		successor.setDuration(day());
		DependencyService.getInstance().newDependency(inactive, successor, DependencyType.FS, 0L, this);
		project.recalculate();
		long linkedStart = successor.getStart();

		inactive.setInactiveTask(true);
		project.recalculate();

		assertTrue(inactive.isInactiveTask());
		assertTrue(successor.getStart() <= linkedStart);
		inactive.setInactiveTask(false);
		assertFalse(inactive.isInactiveTask());
	}

	@Test
	void inactiveTaskRetainsProgressAndRestoresItWhenReactivated() {
		Project project = project();
		NormalTask task = task(project, "Historical alternative");
		task.setPercentComplete(0.40d);

		task.setInactiveTask(true);
		project.recalculate();
		assertEquals(0.40d, task.getPercentComplete(), 0.00001d);

		task.setPercentComplete(0.60d);
		assertEquals(0.60d, task.getPercentComplete(), 0.00001d);

		task.setInactiveTask(false);
		assertEquals(0.60d, task.getPercentComplete(), 0.00001d);
	}

	private Project project() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("manual-test", undo), undo);
		project.initialize(false, false);
		return project;
	}

	private NormalTask task(Project project, String name) {
		NormalTask task = new NormalTask(project);
		task.setName(name);
		project.connectTask(task);
		project.getSchedulingAlgorithm().addObject(task);
		task.getCurrentSchedule().setStart(project.getStart());
		task.setDuration(day());
		return task;
	}

	private long day() {
		return CalendarOption.getInstance().getMillisPerDay();
	}
}
