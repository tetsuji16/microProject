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
package com.microproject.pm.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.options.CalendarOption;
import com.microproject.pm.assignment.AssignmentService;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

class ResourceLevelingServiceTest {
	@Test
	void previewDelaysSecondOverlappingTaskWithoutMutatingProject() {
		Fixture fixture = fixture();
		NormalTask first = task(fixture.project, "First", 1L);
		NormalTask second = task(fixture.project, "Second", 1L);
		AssignmentService.getInstance().newAssignment(first, fixture.resource, 1D, 0L, this);
		AssignmentService.getInstance().newAssignment(second, fixture.resource, 1D, 0L, this);

		ResourceLevelingService.Plan plan = new ResourceLevelingService().preview(
			fixture.project, List.of(fixture.resource), ResourceLevelingService.Options.defaults());

		assertEquals(1, plan.changes().size());
		assertEquals(second, plan.changes().getFirst().task());
		assertTrue(plan.changes().getFirst().addedDelayMillis() > 0L);
		assertEquals(0L, second.getLevelingDelay(), "preview must not mutate the task");
		assertTrue(plan.isComplete());
	}

	@Test
	void applyAndRevertRoundTripLevelingDelay() {
		Fixture fixture = fixture();
		NormalTask first = task(fixture.project, "First", 1L);
		NormalTask second = task(fixture.project, "Second", 1L);
		AssignmentService.getInstance().newAssignment(first, fixture.resource, 1D, 0L, this);
		AssignmentService.getInstance().newAssignment(second, fixture.resource, 1D, 0L, this);
		ResourceLevelingService.Plan plan = new ResourceLevelingService().preview(
			fixture.project, List.of(fixture.resource), ResourceLevelingService.Options.defaults());

		plan.apply();
		assertTrue(second.getLevelingDelay() > 0L);
		plan.revert();
		assertEquals(0L, second.getLevelingDelay());
	}

	@Test
	void applyPostsUndoableLevelingEdit() {
		Fixture fixture = fixture();
		NormalTask first = task(fixture.project, "First", 1L);
		NormalTask second = task(fixture.project, "Second", 1L);
		AssignmentService.getInstance().newAssignment(first, fixture.resource, 1D, 0L, this);
		AssignmentService.getInstance().newAssignment(second, fixture.resource, 1D, 0L, this);
		ResourceLevelingService.Plan plan = new ResourceLevelingService().preview(
			fixture.project, List.of(fixture.resource), ResourceLevelingService.Options.defaults());
		var undo = fixture.project.getUndoController();
		undo.clear();

		plan.apply(undo.getEditSupport());
		assertTrue(second.getLevelingDelay() > 0L);
		assertTrue(undo.canUndo());
		undo.undo();
		assertEquals(0L, second.getLevelingDelay());
		undo.redo();
		assertTrue(second.getLevelingDelay() > 0L);
	}

	@Test
	void priorityOneThousandReportsUnresolvedConflict() {
		Fixture fixture = fixture();
		NormalTask first = task(fixture.project, "First", 1L);
		NormalTask protectedTask = task(fixture.project, "Protected", 1L);
		protectedTask.setPriority(1000);
		AssignmentService.getInstance().newAssignment(first, fixture.resource, 1D, 0L, this);
		AssignmentService.getInstance().newAssignment(protectedTask, fixture.resource, 1D, 0L, this);

		ResourceLevelingService.Plan plan = new ResourceLevelingService().preview(
			fixture.project, List.of(fixture.resource), new ResourceLevelingService.Options(
				ResourceLevelingService.Order.ID_ONLY, false, Long.MIN_VALUE, Long.MAX_VALUE));

		assertFalse(plan.isComplete());
		assertTrue(plan.unresolved().stream().anyMatch(value -> value.reason().contains("priority 1000")));
	}

	@Test
	void previewCanSplitAnUnstartedTaskAroundAHigherPriorityAssignment() {
		Fixture fixture = fixture();
		NormalTask longTask = task(fixture.project, "Long task", 3L);
		NormalTask blocker = task(fixture.project, "Priority work", 1L);
		blocker.setPriority(900);
		long oneDayLater = fixture.project.getEffectiveWorkCalendar().add(
			fixture.project.getStart(), CalendarOption.getInstance().getMillisPerDay(), false);
		blocker.setStart(oneDayLater);
		AssignmentService.getInstance().newAssignment(longTask, fixture.resource, 1D, 0L, this);
		AssignmentService.getInstance().newAssignment(blocker, fixture.resource, 1D, 0L, this);

		ResourceLevelingService.Plan plan = new ResourceLevelingService().preview(
			fixture.project, List.of(fixture.resource), new ResourceLevelingService.Options(
				ResourceLevelingService.Order.PRIORITY_STANDARD, false, true, Long.MIN_VALUE, Long.MAX_VALUE));

		assertEquals(1, plan.splits().size());
		assertEquals(longTask, plan.splits().getFirst().task());
		assertTrue(plan.isComplete());
	}

	private Fixture fixture() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("leveling-test", undo);
		Project project = Project.createProject(pool, undo);
		project.initialize(false, false);
		ResourceImpl resource = pool.newResourceInstance();
		resource.setName("Engineer");
		return new Fixture(project, resource);
	}

	private NormalTask task(Project project, String name, long days) {
		NormalTask task = new NormalTask(project);
		task.setName(name);
		project.connectTask(task);
		project.getSchedulingAlgorithm().addObject(task);
		task.getCurrentSchedule().setStart(project.getStart());
		task.setDuration(days * CalendarOption.getInstance().getMillisPerDay());
		return task;
	}

	private record Fixture(Project project, ResourceImpl resource) {
	}
}
