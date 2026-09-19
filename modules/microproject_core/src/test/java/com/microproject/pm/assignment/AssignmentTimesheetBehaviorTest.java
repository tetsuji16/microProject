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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.microproject.pm.assignment.timesheet.TimesheetStatus;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

class AssignmentTimesheetBehaviorTest {
	@Test
	void applyTimesheetMarksTheAssignmentAndSourceIntegrated() {
		Assignment assignment = createAssignment();
		assignment.setTimesheetAssignment(true);
		assignment.setTimesheetStatus(TimesheetStatus.VALIDATED);

		long updateTime = 1_717_504_800_000L;
		boolean updated = assignment.applyTimesheet(Collections.emptyList(), updateTime);

		assertTrue(updated);
		assertEquals(TimesheetStatus.INTEGRATED, assignment.getTimesheetStatus());
		assertEquals(updateTime, assignment.getLastTimesheetUpdate());
	}

	@Test
	void timesheetStatusPredicatesPreserveTheExistingContract() {
		Assignment assignment = createAssignment();
		assignment.setTimesheetAssignment(true);
		assignment.setTimesheetStatus(TimesheetStatus.ENTERED);

		assertFalse(assignment.isTimesheetEntered());
		assertTrue(assignment.isTimesheetEditable());
		assertTrue(assignment.isTimesheetValidated());
		assertTrue(assignment.isTimesheetRejected());
	}

	private Assignment createAssignment() {
		Project project = createProject();
		NormalTask task = createTask(project);
		ResourceImpl resource = project.getResourcePool().newResourceInstance();
		return Assignment.getInstance(task, resource, 1.0D, 0);
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
}
