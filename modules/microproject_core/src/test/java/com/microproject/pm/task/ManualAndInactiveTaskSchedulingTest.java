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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.options.CalendarOption;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.AssignmentService;
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
		assertEquals(project.getStart(), successor.getStart(), "inactive predecessor must stop constraining the active plan");
		assertFalse(inactive.getSuccessors().isBlank(), "inactivation must retain the dependency for reactivation");
		inactive.setInactiveTask(false);
		project.recalculate();
		assertFalse(inactive.isInactiveTask());
		assertEquals(linkedStart, successor.getStart(), "reactivation must restore the retained dependency effect");
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

	@Test
	void inactivationServiceChangesOnlySelectedTasksAndProvidesUndoRedo() {
		Project project = project();
		NormalTask selected = task(project, "Selected alternative");
		NormalTask unrelated = task(project, "Committed work");
		selected.setPercentComplete(0.4d);
		selected.setDuration(day() * 3L);
		unrelated.setManualDates(project.getWorkCalendar().add(project.getStart(), day() * 4L, false),
			project.getWorkCalendar().add(project.getStart(), day() * 5L, false));
		unrelated.setPercentComplete(0.2d);
		long unrelatedStart = unrelated.getStart();
		long unrelatedEnd = unrelated.getEnd();

		var result = new TaskInactivationService().inactivate(List.of(selected, selected));

		assertEquals(1, result.changedCount());
		assertTrue(selected.isInactiveTask());
		assertEquals(0.4d, selected.getPercentComplete(), 0.00001d);
		assertEquals(unrelatedStart, unrelated.getStart());
		assertEquals(unrelatedEnd, unrelated.getEnd());
		result.change().undo();
		assertFalse(selected.isInactiveTask());
		assertEquals(0.4d, selected.getPercentComplete(), 0.00001d);
		result.change().redo();
		assertTrue(selected.isInactiveTask());
		assertFalse(new TaskInactivationService().inactivate(List.of(selected)).change().hasChanged());
	}

	@Test
	void inactivationRollsBackEntireSelectionWithoutPublishingOrDirtyingWhenMidwayMutationFails() {
		Project project = project();
		NormalTask first = task(project, "First selected task");
		NormalTask second = task(project, "Second selected task");
		project.recalculate();
		long firstStart = first.getStart();
		long firstEnd = first.getEnd();
		long secondStart = second.getStart();
		long secondEnd = second.getEnd();
		project.setDirty(false);
		first.setDirty(false);
		second.setDirty(false);
		project.getUndoController().clear();
		java.util.concurrent.atomic.AtomicInteger objectEvents = new java.util.concurrent.atomic.AtomicInteger();
		java.util.concurrent.atomic.AtomicInteger scheduleEvents = new java.util.concurrent.atomic.AtomicInteger();
		project.addObjectListener(event -> objectEvents.incrementAndGet());
		project.addScheduleListener(event -> scheduleEvents.incrementAndGet());
		TaskInactivationService service = new TaskInactivationService(task -> {
			if (task == first) throw new IllegalStateException("injected after first selected task");
		});

		assertThrows(IllegalStateException.class, () -> service.inactivate(List.of(first, second)));

		assertFalse(first.isInactiveTask());
		assertFalse(second.isInactiveTask());
		assertEquals(firstStart, first.getStart(), "first task start must roll back");
		assertEquals(firstEnd, first.getEnd(), "first task finish must roll back");
		assertEquals(secondStart, second.getStart(), "second task start must remain unchanged");
		assertEquals(secondEnd, second.getEnd(), "second task finish must remain unchanged");
		assertFalse(first.isDirty());
		assertFalse(second.isDirty());
		assertFalse(project.isDirty());
		assertEquals(0, objectEvents.get(), "failed transaction must not publish object events");
		assertEquals(0, scheduleEvents.get(), "failed transaction must not publish schedule events");
		assertFalse(project.getUndoController().canUndo(), "failed transaction must not create an Undo edit");
	}

	@Test
	void inactivationRetainsAssignmentsAndResourceWorkForReactivation() {
		Project project = project();
		NormalTask task = task(project, "Retained assignment");
		var resource = project.getResourcePool().createScriptedResource();
		Assignment assignment = AssignmentService.getInstance().newAssignment(task, resource, 1D, 0L, this);
		long work = assignment.getWork(null);
		double cost = assignment.getCost();
		assertTrue(work > 0L);
		assertTrue(resource.hasActiveAssignment(task.getStart(), task.getEnd()));

		var change = new TaskInactivationService().inactivate(List.of(task)).change();

		assertTrue(task.getAssignments().contains(assignment));
		assertEquals(work, assignment.getWork(null));
		assertEquals(cost, assignment.getCost(), 0.00001d);
		assertFalse(resource.hasActiveAssignment(task.getStart(), task.getEnd()));
		change.undo();
		assertTrue(task.getAssignments().contains(assignment));
		assertEquals(work, assignment.getWork(null));
		assertTrue(resource.hasActiveAssignment(task.getStart(), task.getEnd()));
		change.redo();
		assertTrue(task.getAssignments().contains(assignment));
		assertEquals(work, assignment.getWork(null));
		assertFalse(resource.hasActiveAssignment(task.getStart(), task.getEnd()));
	}

	@Test
	void inactiveTaskCostAndWorkRemainReportableButLeaveProjectTotals() {
		Project project = project();
		NormalTask historical = task(project, "Historical option");
		NormalTask committed = task(project, "Committed option");
		var resource = project.getResourcePool().createScriptedResource();
		Assignment historicalAssignment = AssignmentService.getInstance().newAssignment(historical, resource, 1D, 0L, this);
		Assignment committedAssignment = AssignmentService.getInstance().newAssignment(committed, resource, 1D, 0L, this);
		historical.setFixedCost(100d);
		committed.setFixedCost(25d);
		project.recalculate();
		long totalWork = project.getWork(null);
		double totalCost = project.getCost(null);
		long resourceWork = resource.getWork(null);
		double resourceCost = resource.getCost(null);
		assertTrue(totalWork > 0L);
		assertTrue(totalCost >= 125d);

		var change = new TaskInactivationService().inactivate(List.of(historical)).change();

		assertTrue(historical.getCost(null) >= 100d, "inactive row remains reportable");
		assertTrue(historicalAssignment.getWork(null) > 0L, "assignment data is retained");
		assertTrue(committedAssignment.getWork(null) > 0L);
		assertEquals(committed.getWork(null), project.getWork(null));
		assertEquals(committed.getCost(null), project.getCost(null), 0.00001d);
		assertEquals(resourceWork - historicalAssignment.getWork(null), resource.getWork(null));
		assertEquals(committedAssignment.getCost(), resource.getCost(null), 0.00001d);
		change.undo();
		assertEquals(totalWork, project.getWork(null));
		assertEquals(totalCost, project.getCost(null), 0.00001d);
		assertEquals(resourceWork, resource.getWork(null));
		assertEquals(resourceCost, resource.getCost(null), 0.00001d);
	}

	@Test
	void activeSummaryCostAndWorkExcludeInactiveChildrenButInactiveSummaryRemainsReportable() {
		Project project = project();
		var summaryNode = project.createLocalTaskNode(null);
		NormalTask summary = (NormalTask) summaryNode.getImpl();
		NormalTask child = (NormalTask) project.createLocalTaskNode(summaryNode).getImpl();
		child.setName("Optional work");
		child.setDuration(day() * 2L);
		var resource = project.getResourcePool().createScriptedResource();
		resource.setStandardRate(new com.microproject.datatype.Rate(60d, com.microproject.datatype.TimeUnit.HOURS));
		AssignmentService.getInstance().newAssignment(child, resource, 1d, 0L, this);
		project.recalculate();
		long plannedWork = summary.getWork(null);
		double plannedCost = summary.getCost(null);
		assertTrue(plannedWork > 0L);
		assertTrue(plannedCost > 0d);

		var change = new TaskInactivationService().inactivate(List.of(child)).change();
		assertEquals(0L, summary.getWork(null));
		assertEquals(0d, summary.getCost(null), 0.00001d);
		assertEquals(plannedWork, child.getWork(null), "inactive task row keeps its historical assignment data");
		assertEquals(plannedCost, child.getCost(null), 0.00001d);
		change.undo();
		assertEquals(plannedWork, summary.getWork(null));
		assertEquals(plannedCost, summary.getCost(null), 0.00001d);
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
