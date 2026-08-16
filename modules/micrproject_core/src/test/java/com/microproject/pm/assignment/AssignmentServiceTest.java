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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Iterator;

import org.junit.jupiter.api.Test;

import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.transaction.MultipleTransaction;
import com.microproject.undo.DataFactoryUndoController;

class AssignmentServiceTest {
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
