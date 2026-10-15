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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
	void proposedResourcesAreExcludedByDefaultAndIncludedOnlyByExplicitOption() {
		// Document-derived MSP rule: proposed resources are excluded unless the
		// leveling dialog explicitly opts them in. See the current Desktop support
		// guidance and the leveling dialog option:
		// https://support.microsoft.com/en-us/project/resource-leveling-dialog-box
		// https://support.microsoft.com/en-us/project/distribute-project-work-evenly-level-resource-assignments
		Fixture fixture = fixture();
		fixture.resource.setBookingTypeKind(BookingType.Kind.PROPOSED);
		NormalTask first = task(fixture.project, "Proposed first", 1L);
		NormalTask second = task(fixture.project, "Proposed second", 1L);
		ResourceImpl committed = fixture.project.getResourcePool().newResourceInstance();
		committed.setName("Committed resource");
		NormalTask committedFirst = task(fixture.project, "Committed first", 1L);
		NormalTask committedSecond = task(fixture.project, "Committed second", 1L);
		AssignmentService.getInstance().newAssignment(first, fixture.resource, 1D, 0L, this);
		AssignmentService.getInstance().newAssignment(second, fixture.resource, 1D, 0L, this);
		AssignmentService.getInstance().newAssignment(committedFirst, committed, 1D, 0L, this);
		AssignmentService.getInstance().newAssignment(committedSecond, committed, 1D, 0L, this);
		ResourceLevelingService service = new ResourceLevelingService();

		ResourceLevelingService.Plan defaultPlan = service.preview(fixture.project, null,
			ResourceLevelingService.Options.defaults());
		assertEquals(List.of(committedSecond), defaultPlan.changes().stream()
			.map(ResourceLevelingService.Change::task).toList(),
			"default leveling keeps committed resources active and excludes proposed-resource conflicts");
		assertTrue(defaultPlan.unresolved().isEmpty(), "excluded proposed work is not a leveling conflict");
		assertEquals(0L, second.getLevelingDelay(), "excluded proposed work must remain unscheduled");

		ResourceLevelingService.Plan includedPlan = service.preview(fixture.project, null,
			new ResourceLevelingService.Options(ResourceLevelingService.Order.STANDARD, false, true,
				Long.MIN_VALUE, Long.MAX_VALUE, true));
		assertEquals(2, includedPlan.changes().size(), "explicit inclusion adds proposed resources to committed resources");
		ResourceLevelingService.Plan selectedPlan = service.preview(fixture.project, List.of(fixture.resource),
			new ResourceLevelingService.Options(ResourceLevelingService.Order.STANDARD, false, true,
				Long.MIN_VALUE, Long.MAX_VALUE, true));
		assertEquals(1, selectedPlan.changes().size(), "the selected-resource scope must remain authoritative");
		assertEquals(second, selectedPlan.changes().getFirst().task());
		var undo = fixture.project.getUndoController();
		selectedPlan.apply(undo.getEditSupport());
		assertTrue(second.getLevelingDelay() > 0L, "explicitly included proposed work must be leveled");
		assertEquals(0L, committedSecond.getLevelingDelay(), "unselected committed resources must remain untouched");
		undo.undo();
		assertEquals(0L, second.getLevelingDelay(), "Undo must restore the proposed-resource assignment schedule");
		undo.redo();
		assertTrue(second.getLevelingDelay() > 0L, "Redo must restore the leveled schedule");
		assertEquals(0L, committedSecond.getLevelingDelay(), "Redo must not affect the unselected resource scope");
	}

	@Test
	void selectedTaskLevelingMovesOnlySelectedTasksAgainstFixedUnselectedAssignments() {
		Fixture fixture = fixture();
		NormalTask fixed = task(fixture.project, "Unselected", 1L);
		NormalTask selected = task(fixture.project, "Selected", 1L);
		NormalTask selectedSecond = task(fixture.project, "Selected second", 1L);
		AssignmentService.getInstance().newAssignment(selected, fixture.resource, 1D, 0L, this);
		AssignmentService.getInstance().newAssignment(fixed, fixture.resource, 1D, 0L, this);
		AssignmentService.getInstance().newAssignment(selectedSecond, fixture.resource, 1D, 0L, this);

		ResourceLevelingService.Plan plan = new ResourceLevelingService().previewSelectedTasks(
			fixture.project, null,
			new ResourceLevelingService.Options(ResourceLevelingService.Order.ID_ONLY, false,
				Long.MIN_VALUE, Long.MAX_VALUE),
			List.of(selected, selectedSecond));

		assertEquals(Set.of(selected, selectedSecond), plan.changes().stream()
			.map(ResourceLevelingService.Change::task).collect(Collectors.toSet()));
		assertTrue(selected.getLevelingDelay() == 0L, "preview must not mutate the selected task");
		assertEquals(0L, selectedSecond.getLevelingDelay(), "preview must not mutate the second selected task");
		assertTrue(plan.changes().getFirst().addedDelayMillis() > 0L,
			"selected tasks must move around fixed unselected assignments");
		assertTrue(plan.changes().stream().anyMatch(change -> change.task() == selectedSecond
			&& change.addedDelayMillis() > 0L), "every selected task that conflicts must be leveled");
		assertEquals(0L, fixed.getLevelingDelay(), "unselected tasks must remain unchanged");
		assertTrue(plan.isComplete());

		plan.apply(fixture.project.getUndoController().getEditSupport());
		assertTrue(selected.getLevelingDelay() > 0L);
		assertTrue(selectedSecond.getLevelingDelay() > 0L);
		assertEquals(0L, fixed.getLevelingDelay());
		fixture.project.getUndoController().undo();
		assertEquals(0L, selected.getLevelingDelay());
		assertEquals(0L, selectedSecond.getLevelingDelay());
		assertEquals(0L, fixed.getLevelingDelay());
		fixture.project.getUndoController().redo();
		assertTrue(selected.getLevelingDelay() > 0L);
		assertTrue(selectedSecond.getLevelingDelay() > 0L);
		assertEquals(0L, fixed.getLevelingDelay());
	}

	@Test
	void selectedTaskLevelingWithNoSelectionProducesNoChanges() {
		Fixture fixture = fixture();
		NormalTask task = task(fixture.project, "Task", 1L);
		AssignmentService.getInstance().newAssignment(task, fixture.resource, 1D, 0L, this);

		ResourceLevelingService.Plan plan = new ResourceLevelingService().previewSelectedTasks(
			fixture.project, null, ResourceLevelingService.Options.defaults(), List.of());

		assertTrue(plan.changes().isEmpty());
		assertTrue(plan.splits().isEmpty());
		assertTrue(plan.unresolved().isEmpty());
		assertEquals(0L, task.getLevelingDelay());
	}

	@Test
	void selectedTaskLevelingRejectsTasksFromAnotherProject() {
		Fixture fixture = fixture();
		Fixture other = fixture();
		NormalTask foreignTask = task(other.project, "Foreign", 1L);

		assertThrows(IllegalArgumentException.class, () -> new ResourceLevelingService().previewSelectedTasks(
			fixture.project, null, ResourceLevelingService.Options.defaults(), List.of(foreignTask)));
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
