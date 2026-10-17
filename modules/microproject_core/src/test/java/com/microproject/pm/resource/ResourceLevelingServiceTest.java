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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.microproject.options.CalendarOption;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.AssignmentService;
import com.microproject.pm.assignment.contour.ContourTypes;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.grouping.core.NodeFactory;
import com.microproject.pm.scheduling.ScheduleService;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
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
			fixture.project, List.of(fixture.resource), wholeTaskOptions());

		assertEquals(1, plan.changes().size());
		assertEquals(second, plan.changes().getFirst().task());
		assertTrue(plan.changes().getFirst().addedDelayMillis() > 0L);
		assertEquals(0L, second.getLevelingDelay(), "preview must not mutate the task");
		assertTrue(plan.isComplete());
	}

	@Test
	void manuallyScheduledAssignmentsRemainCapacityAndMoveOnlyWhenOptedIn() {
		// Microsoft exposes a separate “Level manually scheduled tasks” option.
		// Task-mode guidance says manual dates otherwise remain fixed. This is a
		// document-derived compatibility rule, not a measurement against MSP.
		// https://support.microsoft.com/en-us/project/resource-leveling-dialog-box
		// https://support.microsoft.com/en-us/project/task-mode-task-field
		Fixture fixture = fixture();
		NormalTask manualFirst = task(fixture.project, "Manual first", 1L);
		NormalTask manualSecond = task(fixture.project, "Manual second", 1L);
		manualFirst.setManuallyScheduled(true);
		manualSecond.setManuallyScheduled(false);
		manualFirst.setPriority(100);
		manualSecond.setPriority(900);
		AssignmentService.getInstance().newAssignment(manualFirst, fixture.resource, 1D, 0L, this);
		AssignmentService.getInstance().newAssignment(manualSecond, fixture.resource, 1D, 0L, this);
		ResourceLevelingService service = new ResourceLevelingService();

		ResourceLevelingService.Plan excluded = service.preview(fixture.project, List.of(fixture.resource),
			wholeTaskOptions());
		assertEquals(List.of(manualSecond), excluded.changes().stream()
			.map(ResourceLevelingService.Change::task).toList(),
			"manual assignments count as fixed load and constrain otherwise movable work, regardless of priority");
		assertTrue(excluded.unresolved().isEmpty(), "fixed manual work is not a movable-task failure");
		assertEquals(0L, manualFirst.getLevelingDelay());
		assertEquals(0L, manualSecond.getLevelingDelay());

		manualSecond.setManuallyScheduled(true);
		var options = new ResourceLevelingService.Options(ResourceLevelingService.Order.STANDARD,
			false, true, Long.MIN_VALUE, Long.MAX_VALUE, false,
			ResourceLevelingService.Sensitivity.DAY, false, true);
		ResourceLevelingService.Plan included = service.previewSelectedTasks(fixture.project,
			List.of(fixture.resource), options, List.of(manualSecond));
		assertEquals(List.of(manualSecond), included.changes().stream()
			.map(ResourceLevelingService.Change::task).toList(),
			"selected scope may move its manual target while the unselected manual task stays fixed");
		long originalStart = manualSecond.getStart();
		long expectedStart = included.changes().getFirst().projectedStart();
		var undo = fixture.project.getUndoController();
		undo.getEditSupport().beginUpdate();
		try {
			included.apply(undo.getEditSupport());
		} finally {
			undo.getEditSupport().endUpdate();
		}
		assertTrue(manualSecond.getLevelingDelay() > 0L);
		assertEquals(expectedStart, manualSecond.getStart(), "opt-in must apply the projected start to the manual task");
		undo.undo();
		assertEquals(0L, manualSecond.getLevelingDelay());
		assertEquals(originalStart, manualSecond.getStart());
		undo.redo();
		assertTrue(manualSecond.getLevelingDelay() > 0L);
		assertEquals(expectedStart, manualSecond.getStart());
		new ResourceLevelingService().clear(fixture.project, List.of(manualSecond), undo.getEditSupport());
		assertEquals(originalStart, manualSecond.getStart(), "Clear restores dates generated by opt-in leveling");
		undo.undo();
		assertEquals(expectedStart, manualSecond.getStart(), "Undo Clear restores the leveled manual dates");
		undo.redo();
		assertEquals(originalStart, manualSecond.getStart());
	}

	@Test
	void clearPreservesManualDateEditsMadeAfterOptInLeveling() {
		Fixture fixture = fixture();
		NormalTask first = task(fixture.project, "First", 1L);
		NormalTask manual = task(fixture.project, "Manual", 1L);
		manual.setManuallyScheduled(true);
		AssignmentService.getInstance().newAssignment(first, fixture.resource, 1D, 0L, this);
		AssignmentService.getInstance().newAssignment(manual, fixture.resource, 1D, 0L, this);
		var options = new ResourceLevelingService.Options(ResourceLevelingService.Order.STANDARD,
			false, true, Long.MIN_VALUE, Long.MAX_VALUE, false,
			ResourceLevelingService.Sensitivity.DAY, false, true);
		ResourceLevelingService service = new ResourceLevelingService();
		service.preview(fixture.project, List.of(fixture.resource), options).apply();
		long editedStart = manual.getManualStart() + com.microproject.pm.calendar.WorkCalendar.MILLIS_IN_HOUR;
		manual.setManualDates(editedStart, manual.getManualFinish() + com.microproject.pm.calendar.WorkCalendar.MILLIS_IN_HOUR);

		service.clear(fixture.project, List.of(manual), null);

		assertEquals(editedStart, manual.getStart(), "Clear must not overwrite a later manual edit");
		assertEquals(0L, com.microproject.datatype.Duration.millis(manual.getLevelingDelay()));
	}

	@Test
	void clearAfterSwitchingLeveledTaskToAutomaticOnlyDropsProvenance() {
		Fixture fixture = fixture();
		NormalTask first = task(fixture.project, "First", 1L);
		NormalTask manual = task(fixture.project, "Manual", 1L);
		manual.setManuallyScheduled(true);
		AssignmentService.getInstance().newAssignment(first, fixture.resource, 1D, 0L, this);
		AssignmentService.getInstance().newAssignment(manual, fixture.resource, 1D, 0L, this);
		var options = new ResourceLevelingService.Options(ResourceLevelingService.Order.STANDARD,
			false, true, Long.MIN_VALUE, Long.MAX_VALUE, false,
			ResourceLevelingService.Sensitivity.DAY, false, true);
		ResourceLevelingService service = new ResourceLevelingService();
		service.preview(fixture.project, List.of(fixture.resource), options).apply();
		long leveledManualStart = manual.getManualStart();
		assertTrue(manual.getLevelingManualDateState() != null);
		manual.setManuallyScheduled(false);
		long automaticManualStart = manual.getManualStart();

		service.clear(fixture.project);

		assertEquals(automaticManualStart, manual.getManualStart(), "Clear must leave inactive manual date provenance alone after switching mode");
		assertEquals(leveledManualStart, automaticManualStart, "switching mode must keep captured manual dates without changing them");
		assertEquals(0L, com.microproject.datatype.Duration.millis(manual.getLevelingDelay()));
		assertEquals(null, manual.getLevelingManualDateState());
	}

	@Test
	void stateOnlyClearRestoresManualDatesAndUndoRestoresProvenance() {
		Fixture fixture = fixture();
		NormalTask manual = task(fixture.project, "Manual", 1L);
		manual.setManuallyScheduled(true);
		long originalStart = manual.getManualStart();
		long originalFinish = manual.getManualFinish();
		long shift = com.microproject.pm.calendar.WorkCalendar.MILLIS_IN_HOUR;
		manual.setManualDates(originalStart + shift, originalFinish + shift);
		manual.setLevelingManualDateState(new Task.LevelingManualDateState(originalStart, originalFinish,
			originalStart + shift, originalFinish + shift));
		var undo = fixture.project.getUndoController();
		undo.clear();

		assertEquals(List.of(manual), new ResourceLevelingService().clear(fixture.project, List.of(manual), undo.getEditSupport()));
		assertEquals(originalStart, manual.getManualStart());
		assertEquals(null, manual.getLevelingManualDateState());
		undo.undo();
		assertEquals(originalStart + shift, manual.getManualStart());
		assertTrue(manual.getLevelingManualDateState() != null);
		undo.redo();
		assertEquals(originalStart, manual.getManualStart());
		assertEquals(null, manual.getLevelingManualDateState());
	}

	@Test
	void manualTaskOptInDoesNotOverridePriorityOrActualProgressProtections() {
		Fixture fixture = fixture();
		NormalTask blocker = task(fixture.project, "Fixed blocker", 1L);
		NormalTask protectedTask = task(fixture.project, "Priority protected", 1L);
		NormalTask inProgress = task(fixture.project, "In progress", 1L);
		protectedTask.setManuallyScheduled(true);
		protectedTask.setPriority(1000);
		inProgress.setManuallyScheduled(true);
		inProgress.setPercentComplete(0.5D);
		AssignmentService.getInstance().newAssignment(blocker, fixture.resource, 1D, 0L, this);
		AssignmentService.getInstance().newAssignment(protectedTask, fixture.resource, 1D, 0L, this);
		AssignmentService.getInstance().newAssignment(inProgress, fixture.resource, 1D, 0L, this);
		var options = new ResourceLevelingService.Options(ResourceLevelingService.Order.ID_ONLY,
			false, true, Long.MIN_VALUE, Long.MAX_VALUE, false,
			ResourceLevelingService.Sensitivity.DAY, false, true);

		ResourceLevelingService.Plan plan = new ResourceLevelingService().preview(fixture.project,
			List.of(fixture.resource), options);
		assertTrue(plan.changes().isEmpty());
		assertTrue(plan.unresolved().stream().anyMatch(value -> value.reason().contains("priority 1000")));
		assertTrue(plan.unresolved().stream().anyMatch(value -> value.reason().contains("In-progress")));
	}

	@Test
	void applyRejectsStaleResourceAndAssignmentInputsBeforeMutation() {
		Fixture fixture = fixture();
		NormalTask first = task(fixture.project, "First", 1L);
		NormalTask second = task(fixture.project, "Second", 1L);
		AssignmentService.getInstance().newAssignment(first, fixture.resource, 1D, 0L, this);
		Assignment secondAssignment = AssignmentService.getInstance().newAssignment(second, fixture.resource, 1D, 0L, this);
		ResourceLevelingService.Plan staleAssignment = new ResourceLevelingService().preview(
			fixture.project, List.of(fixture.resource), wholeTaskOptions());
		secondAssignment.forceUnits(0.5D);
		assertThrows(IllegalStateException.class, staleAssignment::apply,
			"assignment edits after preview must invalidate its capacity calculation");
		assertEquals(0L, second.getLevelingDelay());

		ResourceLevelingService.Plan staleCapacity = new ResourceLevelingService().preview(
			fixture.project, List.of(fixture.resource), wholeTaskOptions());
		fixture.resource.setMaximumUnits(0.5D);
		assertThrows(IllegalStateException.class, staleCapacity::apply,
			"resource unit changes after preview must invalidate its capacity calculation");
		assertEquals(0L, second.getLevelingDelay());
	}

	@Test
	void applyRejectsStaleTaskScheduleRevisionBeforeMutation() {
		Fixture fixture = fixture();
		NormalTask first = task(fixture.project, "First", 1L);
		NormalTask second = task(fixture.project, "Second", 1L);
		AssignmentService.getInstance().newAssignment(first, fixture.resource, 1D, 0L, this);
		AssignmentService.getInstance().newAssignment(second, fixture.resource, 1D, 0L, this);
		ResourceLevelingService.Plan stale = new ResourceLevelingService().preview(
			fixture.project, List.of(fixture.resource), wholeTaskOptions());
		second.setPriority(900);

		assertThrows(IllegalStateException.class, stale::apply,
			"project scheduling edits after preview must invalidate its task ordering");
		assertEquals(0L, second.getLevelingDelay());
	}

	@Test
	void levelingSensitivityComparesWorkWithinTheSelectedCalendarPeriod() {
		assertEquals(ResourceLevelingService.Sensitivity.DAY, ResourceLevelingService.Options.defaults().sensitivity(),
			"day basis is the product default while Microsoft support pages conflict on the default");
		Fixture fixture = fixture();
		List<NormalTask> tasks = new java.util.ArrayList<>();
		for (int index = 0; index < 6; index++) {
			NormalTask task = task(fixture.project, "Task " + index, 1L);
			tasks.add(task);
			AssignmentService.getInstance().newAssignment(task, fixture.resource, 1D, 0L, this);
		}
		ResourceLevelingService service = new ResourceLevelingService();
		long workday = CalendarOption.getInstance().getMillisPerDay();
		// The current Resource Leveling dialog documentation lists Minute, Hour,
		// Day, Week, and Month. The support page and the separate distribution
		// guidance disagree on the default (Month vs Day), so Day is our explicit
		// product default, not an MSP-parity claim.
		// https://support.microsoft.com/en-us/project/resource-leveling-dialog-box
		// https://support.microsoft.com/en-us/project/distribute-project-work-evenly-level-resource-assignments
		ResourceLevelingService.Plan daily = previewAt(service, fixture, ResourceLevelingService.Sensitivity.DAY);
		ResourceLevelingService.Plan weekly = previewAt(service, fixture, ResourceLevelingService.Sensitivity.WEEK);
		ResourceLevelingService.Plan monthly = previewAt(service, fixture, ResourceLevelingService.Sensitivity.MONTH);
		ResourceLevelingService.Plan hourly = previewAt(service, fixture, ResourceLevelingService.Sensitivity.HOUR);
		ResourceLevelingService.Plan minutely = previewAt(service, fixture, ResourceLevelingService.Sensitivity.MINUTE);

		assertTrue(workday > 0L);
		Set<NormalTask> expectedDelayed = Set.copyOf(tasks.subList(1, tasks.size()));
		assertEquals(expectedDelayed, daily.changes().stream().map(ResourceLevelingService.Change::task).collect(Collectors.toSet()));
		assertTrue(daily.changes().stream().allMatch(change -> change.addedDelayMillis() >= workday));
		assertEquals(Set.of(tasks.getLast()), weekly.changes().stream().map(ResourceLevelingService.Change::task).collect(Collectors.toSet()),
			"48 hours exceed normal 40-hour weekly availability");
		assertTrue(monthly.changes().isEmpty(), "48 hours fit within monthly availability");
		assertEquals(expectedDelayed, hourly.changes().stream().map(ResourceLevelingService.Change::task).collect(Collectors.toSet()));
		assertEquals(expectedDelayed, minutely.changes().stream().map(ResourceLevelingService.Change::task).collect(Collectors.toSet()));
	}

	@Test
	void minuteAndHourUseTheirOwnWorkBucketsWhileDayAggregationFits() {
		Fixture fixture = fixture();
		long oneHour = com.microproject.pm.calendar.WorkCalendar.MILLIS_IN_HOUR;
		long overlapStart = fixture.project.getEffectiveWorkCalendar().add(fixture.project.getStart(), oneHour, false);
		NormalTask first = task(fixture.project, "Short first", 1L);
		NormalTask second = task(fixture.project, "Short second", 1L);
		AssignmentService.getInstance().newAssignment(first, fixture.resource, 1D, 0L, this);
		AssignmentService.getInstance().newAssignment(second, fixture.resource, 1D, 0L, this);
		first.setStart(overlapStart);
		second.setStart(overlapStart);
		first.setDuration(oneHour);
		second.setDuration(oneHour);
		ResourceLevelingService service = new ResourceLevelingService();

		assertEquals(oneHour, first.getEffectiveWorkCalendar().compare(first.getEnd(), first.getStart(), false),
			"the independent fixture must contain one work-hour per assignment");
		assertTrue(previewAt(service, fixture, ResourceLevelingService.Sensitivity.DAY).changes().isEmpty(),
			"two hours fit the resource's daily capacity");
		for (ResourceLevelingService.Sensitivity sensitivity : List.of(
			ResourceLevelingService.Sensitivity.MINUTE, ResourceLevelingService.Sensitivity.HOUR)) {
			ResourceLevelingService.Plan plan = previewAt(service, fixture, sensitivity);
			assertEquals(List.of(second), plan.changes().stream()
				.map(ResourceLevelingService.Change::task).toList(), sensitivity + " buckets expose the overlapping work");
		}
	}

	@Test
	void sensitivityBucketsUseUtcAndConfiguredWeekStartBoundaries() {
		java.util.GregorianCalendar timestamp = com.microproject.util.DateTime.calendarInstance();
		timestamp.clear();
		timestamp.set(2026, java.util.Calendar.APRIL, 8, 13, 47, 32);
		long value = timestamp.getTimeInMillis();
		assertEquals(0, timestamp.getTimeZone().getOffset(value), "project buckets use the shared UTC calendar basis");
		java.util.GregorianCalendar minute = com.microproject.util.DateTime.calendarInstance();
		minute.setTimeInMillis(ResourceLevelingService.periodStart(value, ResourceLevelingService.Sensitivity.MINUTE));
		assertEquals(13, minute.get(java.util.Calendar.HOUR_OF_DAY));
		assertEquals(47, minute.get(java.util.Calendar.MINUTE));
		assertEquals(0, minute.get(java.util.Calendar.SECOND));
		java.util.GregorianCalendar hour = com.microproject.util.DateTime.calendarInstance();
		hour.setTimeInMillis(ResourceLevelingService.periodStart(value, ResourceLevelingService.Sensitivity.HOUR));
		assertEquals(13, hour.get(java.util.Calendar.HOUR_OF_DAY));
		assertEquals(0, hour.get(java.util.Calendar.MINUTE));

		CalendarOption option = CalendarOption.getInstance();
		int previousWeekStart = option.getWeekStartsOn();
		try {
			option.setWeekStartsOn(java.util.Calendar.MONDAY);
			java.util.GregorianCalendar week = com.microproject.util.DateTime.calendarInstance();
			week.setTimeInMillis(ResourceLevelingService.periodStart(value, ResourceLevelingService.Sensitivity.WEEK));
			assertEquals(java.util.Calendar.MONDAY, week.get(java.util.Calendar.DAY_OF_WEEK));
			assertEquals(0, week.get(java.util.Calendar.HOUR_OF_DAY));
			option.setWeekStartsOn(java.util.Calendar.SUNDAY);
			week.setTimeInMillis(ResourceLevelingService.periodStart(value, ResourceLevelingService.Sensitivity.WEEK));
			assertEquals(java.util.Calendar.SUNDAY, week.get(java.util.Calendar.DAY_OF_WEEK));
			assertEquals(0, week.get(java.util.Calendar.HOUR_OF_DAY));
		} finally {
			option.setWeekStartsOn(previousWeekStart);
		}
	}

	@Test
	void hourlySensitivityUsesTimephasedContourWorkInsteadOfConstantAssignmentUnits() {
		Fixture fixture = fixture();
		long hour = com.microproject.pm.calendar.WorkCalendar.MILLIS_IN_HOUR;
		fixture.resource.setMaximumUnits(0.75D);
		NormalTask contoured = task(fixture.project, "Back-loaded", 1L);
		NormalTask candidate = task(fixture.project, "Early hour", 1L);
		Assignment firstAssignment = AssignmentService.getInstance().newAssignment(contoured, fixture.resource, 0.5D, 0L, this);
		AssignmentService.getInstance().newAssignment(candidate, fixture.resource, 0.5D, 0L, this);
		contoured.setDuration(10L * hour);
		candidate.setDuration(hour);
		firstAssignment.setWorkContourTypeKind(ContourTypes.Kind.BACK_LOADED);
		long start = firstAssignment.getStart();
		long firstHourWork = firstAssignment.work(start, start + hour);
		assertTrue(firstHourWork > 0L && firstHourWork < hour / 4L,
			"fixture must independently prove the back-loaded contour has low first-hour work");

		ResourceLevelingService.Plan plan = previewAt(new ResourceLevelingService(), fixture,
			ResourceLevelingService.Sensitivity.HOUR);
		assertTrue(plan.changes().isEmpty(), () -> plan.changes() + " / " + plan.unresolved() +
			"0.05 + 0.5 units of first-hour work fit 0.75 availability; constant units would falsely level it");
	}

	@Test
	void hourlySensitivityDoesNotCountAssignmentWorkInsideTaskOwnedSplitGaps() {
		Fixture fixture = fixture();
		long hour = com.microproject.pm.calendar.WorkCalendar.MILLIS_IN_HOUR;
		NormalTask first = task(fixture.project, "Task with manual gap", 1L);
		NormalTask second = task(fixture.project, "Gap occupant", 1L);
		Assignment firstAssignment = AssignmentService.getInstance().newAssignment(first, fixture.resource, 1D, 0L, this);
		first.setDuration(8L * hour);
		long gapStart = first.getEffectiveWorkCalendar().add(first.getStart(), hour, false);
		long gapEnd = first.getEffectiveWorkCalendar().add(gapStart, hour, false);
		assertTrue(ScheduleService.getInstance().split(this, first, gapStart, gapEnd, null));
		assertEquals(hour, firstAssignment.work(gapStart, gapEnd),
			"the assignment contour still stores work, while the task's manual split marks that time nonworking");
		second.setStart(gapStart);
		Assignment secondAssignment = AssignmentService.getInstance().newAssignment(second, fixture.resource, 1D, 0L, this);
		second.setDuration(hour);

		ResourceLevelingService.Plan plan = previewAt(new ResourceLevelingService(), fixture,
			ResourceLevelingService.Sensitivity.HOUR);
		assertTrue(plan.changes().isEmpty(), () -> plan.changes() + " / " + plan.unresolved() + " an empty task gap must not consume resource capacity");
	}

	@Test
	void shiftedCandidateRetainsItsContourAtTheNewScheduledStart() {
		Fixture fixture = fixture();
		long hour = com.microproject.pm.calendar.WorkCalendar.MILLIS_IN_HOUR;
		fixture.resource.setMaximumUnits(0.5D);
		NormalTask blocker = task(fixture.project, "Earlier assignment", 1L);
		NormalTask candidateTask = task(fixture.project, "Shifted back-loaded", 1L);
		AssignmentService.getInstance().newAssignment(blocker, fixture.resource, 0.5D, 0L, this);
		Assignment candidate = AssignmentService.getInstance().newAssignment(candidateTask, fixture.resource, 0.5D, 0L, this);
		blocker.setDuration(hour);
		candidateTask.setDuration(10L * hour);
		candidate.setWorkContourTypeKind(ContourTypes.Kind.BACK_LOADED);
		long originalStart = candidate.getStart();
		long firstHourWork = candidate.work(originalStart, originalStart + hour);
		assertTrue(firstHourWork > 0L && firstHourWork < hour / 4L);

		ResourceLevelingService.Plan plan = previewAt(new ResourceLevelingService(), fixture,
			ResourceLevelingService.Sensitivity.HOUR);
		assertEquals(1, plan.changes().size(), () -> plan.changes() + " / " + plan.unresolved());
		assertEquals(hour, plan.changes().getFirst().newDelay());
		assertTrue(plan.unresolved().isEmpty(), () -> plan.unresolved().toString());
	}

	@Test
	void singleAssignmentTaskDelayLivesOnItsAssignmentAndMovesTheAssignmentStart() {
		// Document-derived decision (Microsoft Project "Level Assignments"): with the option on, the delay is
		// stored per assignment even for a one-assignment task; the assignment's start follows it.
		Fixture fixture = fixture();
		NormalTask anchor = task(fixture.project, "Anchor", 1L);
		NormalTask target = task(fixture.project, "Target", 1L);
		anchor.setPriority(900);
		AssignmentService.getInstance().newAssignment(anchor, fixture.resource, 1D, 0L, this);
		Assignment assignment = AssignmentService.getInstance().newAssignment(target, fixture.resource, 1D, 0L, this);
		long assignmentStart = assignment.getStart();
		ResourceLevelingService.Plan plan = new ResourceLevelingService().preview(fixture.project,
			List.of(fixture.resource), ResourceLevelingService.Options.defaults());
		assertTrue(plan.changes().isEmpty(), "no task-level delay is planned when assignments are leveled");
		assertEquals(1, plan.assignmentChanges().size());
		assertTrue(plan.hasChanges(), "an assignment-only plan must report changes");
		plan.apply();
		assertEquals(0L, target.getLevelingDelay());
		assertTrue(com.microproject.datatype.Duration.millis(assignment.getLevelingDelay()) > 0L);
		assertTrue(assignment.getStart() > assignmentStart, "the leveled assignment must start later");
	}

	@Test
	void minuteSensitivityBoundsLongRangeBeforeBuildingPeriodBuckets() {
		long minute = com.microproject.pm.calendar.WorkCalendar.MILLIS_IN_MINUTE;
		assertTrue(ResourceLevelingService.exceedsBucketLimit(0L, 100_001L * minute,
			ResourceLevelingService.Sensitivity.MINUTE));
		assertFalse(ResourceLevelingService.exceedsBucketLimit(0L, 100_000L * minute,
			ResourceLevelingService.Sensitivity.MINUTE));
	}

	@Test
	void candidateWhoseOwnLoadExceedsCapacityDoesNotTreatLongMaxAsADate() {
		Fixture fixture = fixture();
		long hour = com.microproject.pm.calendar.WorkCalendar.MILLIS_IN_HOUR;
		fixture.resource.setMaximumUnits(0.25D);
		NormalTask task = task(fixture.project, "Cannot fit", 1L);
		AssignmentService.getInstance().newAssignment(task, fixture.resource, 0.5D, 0L, this);
		task.setDuration(hour);

		ResourceLevelingService.Plan plan = previewAt(new ResourceLevelingService(), fixture,
			ResourceLevelingService.Sensitivity.HOUR);
		assertTrue(plan.changes().isEmpty());
		assertTrue(plan.unresolved().stream().anyMatch(conflict ->
			conflict.reason().equals("No later capacity boundary was found")), () -> plan.unresolved().toString());
	}

	private ResourceLevelingService.Plan previewAt(ResourceLevelingService service, Fixture fixture,
		ResourceLevelingService.Sensitivity sensitivity) {
		return service.preview(fixture.project, List.of(fixture.resource), new ResourceLevelingService.Options(
			ResourceLevelingService.Order.ID_ONLY, false, true, Long.MIN_VALUE, Long.MAX_VALUE, false,
			sensitivity, false, false));
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
			wholeTaskOptions());
		assertEquals(List.of(committedSecond), defaultPlan.changes().stream()
			.map(ResourceLevelingService.Change::task).toList(),
			"default leveling keeps committed resources active and excludes proposed-resource conflicts");
		assertTrue(defaultPlan.unresolved().isEmpty(), "excluded proposed work is not a leveling conflict");
		assertEquals(0L, second.getLevelingDelay(), "excluded proposed work must remain unscheduled");

		ResourceLevelingService.Plan includedPlan = service.preview(fixture.project, null,
			new ResourceLevelingService.Options(ResourceLevelingService.Order.STANDARD, false, true,
				Long.MIN_VALUE, Long.MAX_VALUE, true, ResourceLevelingService.Sensitivity.DAY, false, false));
		assertEquals(2, includedPlan.changes().size(), "explicit inclusion adds proposed resources to committed resources");
		ResourceLevelingService.Plan selectedPlan = service.preview(fixture.project, List.of(fixture.resource),
			new ResourceLevelingService.Options(ResourceLevelingService.Order.STANDARD, false, true,
				Long.MIN_VALUE, Long.MAX_VALUE, true, ResourceLevelingService.Sensitivity.DAY, false, false));
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
				true, Long.MIN_VALUE, Long.MAX_VALUE, false, ResourceLevelingService.Sensitivity.DAY, false, false),
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
			fixture.project, List.of(fixture.resource), wholeTaskOptions());

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
			fixture.project, List.of(fixture.resource), wholeTaskOptions());
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
	void scopedClearRemovesOnlyGeneratedSplitsAndIsUndoable() {
		Fixture fixture = fixture();
		NormalTask selected = task(fixture.project, "Selected", 1L);
		NormalTask untouched = task(fixture.project, "Untouched", 1L);
		long from = selected.getStart() + com.microproject.pm.calendar.WorkCalendar.MILLIS_IN_HOUR;
		long to = from + com.microproject.pm.calendar.WorkCalendar.MILLIS_IN_HOUR;
		selected.splitForLeveling(this, from, to);
		selected.split(this, from, to);
		untouched.splitForLeveling(this, untouched.getStart() + 3_600_000L, untouched.getStart() + 7_200_000L);
		selected.setLevelingDelay(com.microproject.datatype.Duration.millis(86_400_000L));
		untouched.setLevelingDelay(com.microproject.datatype.Duration.millis(172_800_000L));
		var undo = fixture.project.getUndoController();
		undo.clear();

		List<Task> affected = new ResourceLevelingService().clear(fixture.project, List.of(selected),
			undo.getEditSupport());

		assertEquals(List.of(selected), affected);
		assertEquals(0L, com.microproject.datatype.Duration.millis(selected.getLevelingDelay()));
		assertTrue(selected.getLevelingTaskSplitIntervals().isEmpty());
		assertEquals(1, selected.getManualTaskSplitIntervals().size(), "manual overlap remains owned by the user");
		assertEquals(172_800_000L, com.microproject.datatype.Duration.millis(untouched.getLevelingDelay()));
		assertEquals(1, untouched.getLevelingTaskSplitIntervals().size(), "selection clear leaves other tasks unchanged");
		assertTrue(undo.canUndo());

		undo.undo();
		assertEquals(86_400_000L, com.microproject.datatype.Duration.millis(selected.getLevelingDelay()));
		assertEquals(1, selected.getLevelingTaskSplitIntervals().size());
		undo.redo();
		assertEquals(0L, com.microproject.datatype.Duration.millis(selected.getLevelingDelay()));
		assertTrue(selected.getLevelingTaskSplitIntervals().isEmpty());
		assertEquals(1, selected.getManualTaskSplitIntervals().size());
	}

	@Test
	void scopedClearRejectsTasksOwnedByAnotherProjectBeforeMutation() {
		Fixture fixture = fixture();
		Fixture other = fixture();
		NormalTask target = task(fixture.project, "Target", 1L);
		NormalTask foreign = task(other.project, "Foreign", 1L);
		target.setLevelingDelay(com.microproject.datatype.Duration.millis(86_400_000L));

		assertThrows(IllegalArgumentException.class, () -> new ResourceLevelingService().clear(
			fixture.project, List.of(foreign), null));
		assertEquals(86_400_000L, com.microproject.datatype.Duration.millis(target.getLevelingDelay()),
			"invalid scoped requests must not mutate project tasks");
	}

	@Test
	void clearRejectsReadOnlyTaskInProjectWideScopeBeforeMutation() {
		Fixture fixture = fixture();
		NormalTask editable = task(fixture.project, "Editable", 1L);
		NormalTask linked = task(fixture.project, "Linked", 1L);
		editable.setLevelingDelay(com.microproject.datatype.Duration.millis(86_400_000L));
		linked.setLevelingDelay(com.microproject.datatype.Duration.millis(172_800_000L));
		linked.setExternal(true);

		assertThrows(IllegalStateException.class,
			() -> new ResourceLevelingService().clear(fixture.project, null, null));
		assertEquals(86_400_000L, com.microproject.datatype.Duration.millis(editable.getLevelingDelay()));
		assertEquals(172_800_000L, com.microproject.datatype.Duration.millis(linked.getLevelingDelay()));
	}

	@Test
	void failedClearRestoresEveryTaskAndDoesNotPublishUndo() {
		Fixture fixture = fixture();
		NormalTask first = task(fixture.project, "First", 1L);
		ThrowingClearTask second = new ThrowingClearTask(fixture.project);
		second.setName("Second");
		fixture.project.connectTask(second);
		fixture.project.getSchedulingAlgorithm().addObject(second);
		second.getCurrentSchedule().setStart(fixture.project.getStart());
		second.setDuration(CalendarOption.getInstance().getMillisPerDay());
		for (NormalTask target : List.of(first, second)) {
			long from = target.getStart() + com.microproject.pm.calendar.WorkCalendar.MILLIS_IN_HOUR;
			target.splitForLeveling(this, from, from + com.microproject.pm.calendar.WorkCalendar.MILLIS_IN_HOUR);
			target.setLevelingDelay(com.microproject.datatype.Duration.millis(86_400_000L));
		}
		var undo = fixture.project.getUndoController();
		undo.clear();
		second.failAfterClear = true;

		assertThrows(IllegalStateException.class,
			() -> new ResourceLevelingService().clear(fixture.project, null, undo.getEditSupport()));
		for (NormalTask target : List.of(first, second)) {
			assertEquals(86_400_000L, com.microproject.datatype.Duration.millis(target.getLevelingDelay()));
			assertEquals(1, target.getLevelingTaskSplitIntervals().size());
		}
		assertFalse(undo.canUndo(), "a rolled-back clear must not leave an Undo edit");
	}

	@Test
	void failedApplyRestoresMutatedSplitAndDoesNotPublishUndo() {
		Fixture fixture = fixture();
		ThrowingApplySplitTask longTask = new ThrowingApplySplitTask(fixture.project);
		longTask.setName("Long task");
		fixture.project.connectTask(longTask);
		fixture.project.getSchedulingAlgorithm().addObject(longTask);
		longTask.getCurrentSchedule().setStart(fixture.project.getStart());
		longTask.setDuration(3L * CalendarOption.getInstance().getMillisPerDay());
		NormalTask blocker = task(fixture.project, "Priority work", 1L);
		blocker.setPriority(900);
		long oneDayLater = fixture.project.getEffectiveWorkCalendar().add(
			fixture.project.getStart(), CalendarOption.getInstance().getMillisPerDay(), false);
		blocker.setStart(oneDayLater);
		AssignmentService.getInstance().newAssignment(longTask, fixture.resource, 1D, 0L, this);
		Assignment blockerAssignment = AssignmentService.getInstance().newAssignment(blocker, fixture.resource, 1D, 0L, this);
		long start = longTask.getStart();
		long finish = longTask.getEnd();
		var undo = fixture.project.getUndoController();
		undo.clear();
		longTask.failAfterSplit = true;

		ResourceLevelingService.Plan plan = new ResourceLevelingService().preview(
			fixture.project, List.of(fixture.resource), new ResourceLevelingService.Options(
				ResourceLevelingService.Order.PRIORITY_STANDARD, false, true, Long.MIN_VALUE, Long.MAX_VALUE,
				false, ResourceLevelingService.Sensitivity.DAY, false, false));
		assertFalse(plan.splits().isEmpty());
		assertThrows(IllegalStateException.class, () -> plan.apply(undo.getEditSupport()));

		assertTrue(longTask.getLevelingTaskSplitIntervals().isEmpty(), "the partial split must be restored");
		assertEquals(start, longTask.getStart());
		assertEquals(finish, longTask.getEnd());
		assertFalse(undo.canUndo(), "a rolled-back apply must not leave an Undo edit");
	}

	@Test
	void failedApplyRestoresEveryAssignmentDelayAndDoesNotPublishUndo() {
		Fixture fixture = fixture();
		ResourceImpl secondResource = fixture.project.getResourcePool().newResourceInstance();
		secondResource.setName("Second resource");
		NormalTask first = task(fixture.project, "First assignment", 3L);
		ThrowingDirtyTask second = new ThrowingDirtyTask(fixture.project);
		second.setName("Second assignment");
		fixture.project.connectTask(second);
		fixture.project.getSchedulingAlgorithm().addObject(second);
		second.getCurrentSchedule().setStart(fixture.project.getStart());
		second.setDuration(3L * CalendarOption.getInstance().getMillisPerDay());
		NormalTask firstBlocker = task(fixture.project, "First blocker", 1L);
		NormalTask secondBlocker = task(fixture.project, "Second blocker", 1L);
		firstBlocker.setPriority(900);
		secondBlocker.setPriority(900);
		long oneDayLater = fixture.project.getEffectiveWorkCalendar().add(
			fixture.project.getStart(), CalendarOption.getInstance().getMillisPerDay(), false);
		firstBlocker.setStart(oneDayLater);
		secondBlocker.setStart(oneDayLater);
		Assignment firstAssignment = AssignmentService.getInstance().newAssignment(first, fixture.resource, 1D, 0L, this);
		Assignment secondAssignment = AssignmentService.getInstance().newAssignment(second, secondResource, 1D, 0L, this);
		AssignmentService.getInstance().newAssignment(firstBlocker, fixture.resource, 1D, 0L, this);
		AssignmentService.getInstance().newAssignment(secondBlocker, secondResource, 1D, 0L, this);
		ResourceLevelingService.Plan plan = new ResourceLevelingService().preview(
			fixture.project, List.of(fixture.resource, secondResource), new ResourceLevelingService.Options(
				ResourceLevelingService.Order.PRIORITY_STANDARD, false, false, Long.MIN_VALUE, Long.MAX_VALUE,
				false, ResourceLevelingService.Sensitivity.DAY, true, false));
		assertEquals(2, plan.assignmentChanges().size());
		var undo = fixture.project.getUndoController();
		undo.clear();
		long revision = fixture.project.getMutationState().revision();
		second.failNextDirty = true;

		assertThrows(IllegalStateException.class, () -> plan.apply(undo.getEditSupport()));

		assertEquals(0L, com.microproject.datatype.Duration.millis(firstAssignment.getLevelingDelay()));
		assertEquals(0L, com.microproject.datatype.Duration.millis(secondAssignment.getLevelingDelay()));
		assertEquals(revision, fixture.project.getMutationState().revision(), "a failed apply must not publish a revision");
		assertFalse(undo.canUndo(), "a rolled-back multi-assignment apply must not leave an Undo edit");
	}

	@Test
	void invariantFailureAfterClearRestoresTasksAndDoesNotPublishUndo() {
		Fixture fixture = fixture();
		NormalTask first = task(fixture.project, "First", 1L);
		InvariantFailClearTask second = new InvariantFailClearTask(fixture.project);
		second.setName("Second");
		fixture.project.connectTask(second);
		fixture.project.getSchedulingAlgorithm().addObject(second);
		second.getCurrentSchedule().setStart(fixture.project.getStart());
		second.setDuration(CalendarOption.getInstance().getMillisPerDay());
		for (NormalTask target : List.of(first, second)) {
			long from = target.getStart() + com.microproject.pm.calendar.WorkCalendar.MILLIS_IN_HOUR;
			target.splitForLeveling(this, from, from + com.microproject.pm.calendar.WorkCalendar.MILLIS_IN_HOUR);
			target.setLevelingDelay(com.microproject.datatype.Duration.millis(86_400_000L));
		}
		var undo = fixture.project.getUndoController();
		undo.clear();
		second.failInvariantAfterClear = true;

		IllegalStateException failure = assertThrows(IllegalStateException.class,
			() -> new ResourceLevelingService().clear(fixture.project, null, undo.getEditSupport()));
		assertTrue(failure.getMessage().contains("INVARIANT"));
		second.failInvariantAfterClear = false;
		for (NormalTask target : List.of(first, second)) {
			assertEquals(86_400_000L, com.microproject.datatype.Duration.millis(target.getLevelingDelay()));
			assertEquals(1, target.getLevelingTaskSplitIntervals().size());
		}
		assertFalse(undo.canUndo(), "an invariant rollback must not leave an Undo edit");
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
				ResourceLevelingService.Order.PRIORITY_STANDARD, false, true, Long.MIN_VALUE, Long.MAX_VALUE,
				false, ResourceLevelingService.Sensitivity.DAY, false, false));

		assertEquals(1, plan.splits().size());
		assertEquals(longTask, plan.splits().getFirst().task());
		assertTrue(plan.isComplete());
	}

	@Test
	void taskLevelingCanSplitFieldMustAlsoAllowSplitting() {
		// Microsoft documents the global split option and the per-task Leveling
		// Can Split Yes/No field as conjunctive gates. This is document-derived.
		// https://support.microsoft.com/en-us/project/resource-leveling-dialog-box
		Fixture fixture = fixture();
		NormalTask longTask = task(fixture.project, "Long task", 3L);
		NormalTask blocker = task(fixture.project, "Priority work", 1L);
		blocker.setPriority(900);
		long oneDayLater = fixture.project.getEffectiveWorkCalendar().add(
			fixture.project.getStart(), CalendarOption.getInstance().getMillisPerDay(), false);
		blocker.setStart(oneDayLater);
		AssignmentService.getInstance().newAssignment(longTask, fixture.resource, 1D, 0L, this);
		AssignmentService.getInstance().newAssignment(blocker, fixture.resource, 1D, 0L, this);
		longTask.setLevelingCanSplit(false);

		ResourceLevelingService.Plan plan = new ResourceLevelingService().preview(
			fixture.project, List.of(fixture.resource), new ResourceLevelingService.Options(
				ResourceLevelingService.Order.PRIORITY_STANDARD, false, true, Long.MIN_VALUE, Long.MAX_VALUE,
				false, ResourceLevelingService.Sensitivity.DAY, false, false));

		assertTrue(plan.splits().isEmpty(), "the per-task No value blocks the global split setting");
		assertTrue(plan.changes().stream().anyMatch(change -> change.task() == longTask && change.addedDelayMillis() > 0L),
			"the task remains eligible for ordinary whole-task leveling");
		longTask.setLevelingCanSplit(true);
		ResourceLevelingService.Plan globallyDisabled = new ResourceLevelingService().preview(
			fixture.project, List.of(fixture.resource), new ResourceLevelingService.Options(
				ResourceLevelingService.Order.PRIORITY_STANDARD, false, false, Long.MIN_VALUE, Long.MAX_VALUE,
				false, ResourceLevelingService.Sensitivity.DAY, false, false));
		assertTrue(globallyDisabled.splits().isEmpty(), "task Yes cannot bypass the global split No");
	}

	@Test
	void levelingCanTargetOneAssignmentOrMoveItsWholeTask() {
		// Microsoft documents the global Level Assignments option as selected by
		// default and allows the task-sheet field to override it per task. This is
		// document-derived compatibility, not an empirical MSP run.
		// https://support.microsoft.com/en-us/project/resource-leveling-dialog-box
		assertTrue(ResourceLevelingService.Options.defaults().levelAssignments(),
			"global assignment-level leveling defaults on per Microsoft's documented dialog behavior");
		Fixture fixture = fixture();
		ResourceImpl independent = fixture.project.getResourcePool().newResourceInstance();
		independent.setName("Independent");
		NormalTask longTask = task(fixture.project, "Long task", 3L);
		NormalTask blocker = task(fixture.project, "Priority work", 1L);
		blocker.setPriority(900);
		long oneDayLater = fixture.project.getEffectiveWorkCalendar().add(
			fixture.project.getStart(), CalendarOption.getInstance().getMillisPerDay(), false);
		blocker.setStart(oneDayLater);
		Assignment targetAssignment = AssignmentService.getInstance().newAssignment(longTask, fixture.resource, 1D, 0L, this);
		Assignment unaffectedAssignment = AssignmentService.getInstance().newAssignment(longTask, independent, 1D, 0L, this);
		AssignmentService.getInstance().newAssignment(blocker, fixture.resource, 1D, 0L, this);
		long unaffectedStart = unaffectedAssignment.getStart();

		ResourceLevelingService.Plan assignmentPlan = new ResourceLevelingService().preview(
			fixture.project, List.of(fixture.resource), new ResourceLevelingService.Options(
				ResourceLevelingService.Order.PRIORITY_STANDARD, false, false, Long.MIN_VALUE,
				Long.MAX_VALUE, false, ResourceLevelingService.Sensitivity.DAY, true, false));
		assertTrue(assignmentPlan.changes().isEmpty());
		assertEquals(1, assignmentPlan.assignmentChanges().size());
		assertSame(targetAssignment, assignmentPlan.assignmentChanges().getFirst().assignment());
		assignmentPlan.apply();
		assertTrue(targetAssignment.getStart() > unaffectedAssignment.getStart());
		assertEquals(unaffectedStart, unaffectedAssignment.getStart());
		assertTrue(new ResourceLevelingService().tasksWithLevelingState(fixture.project, null).contains(longTask));
		var undo = fixture.project.getUndoController();
		undo.clear();
		new ResourceLevelingService().clear(fixture.project, null, undo.getEditSupport());
		assertEquals(0L, com.microproject.datatype.Duration.millis(targetAssignment.getLevelingDelay()));
		undo.undo();
		assertTrue(com.microproject.datatype.Duration.millis(targetAssignment.getLevelingDelay()) > 0L);
		undo.redo();
		assertEquals(0L, com.microproject.datatype.Duration.millis(targetAssignment.getLevelingDelay()));
		longTask.setLevelAssignments(true);
		ResourceLevelingService.Plan globallyDisabled = new ResourceLevelingService().preview(
			fixture.project, List.of(fixture.resource), new ResourceLevelingService.Options(
				ResourceLevelingService.Order.PRIORITY_STANDARD, false, false, Long.MIN_VALUE,
				Long.MAX_VALUE, false, ResourceLevelingService.Sensitivity.DAY, false, false));
		assertTrue(globallyDisabled.assignmentChanges().isEmpty());
		assertTrue(globallyDisabled.changes().stream().anyMatch(change -> change.task() == longTask),
			"the global No value forces whole-task leveling even when the task field is Yes");
		longTask.setLevelAssignments(false);
		ResourceLevelingService.Plan taskPlan = new ResourceLevelingService().preview(
			fixture.project, List.of(fixture.resource), new ResourceLevelingService.Options(
				ResourceLevelingService.Order.PRIORITY_STANDARD, false, false, Long.MIN_VALUE,
				Long.MAX_VALUE, false, ResourceLevelingService.Sensitivity.DAY, true, false));
		assertTrue(taskPlan.assignmentChanges().isEmpty());
		assertTrue(taskPlan.changes().stream().anyMatch(change -> change.task() == longTask));
		taskPlan.apply();
		assertEquals(targetAssignment.getStart(), unaffectedAssignment.getStart(),
			"disabling assignment-level leveling applies one whole-task delay");
	}

	@Test
	void assignmentLevelingSplitCanBeClearedAndUndoneWithoutChangingTaskOrOtherAssignments() throws Exception {
		Fixture fixture = fixture();
		NormalTask longTask = task(fixture.project, "Long task", 5L);
		NormalTask summary = task(fixture.project, "Summary", 0L);
		longTask.setWbsParent(summary);
		summary.setWbsChildrenNodes(List.of(NodeFactory.getInstance().createNode(longTask)));
		NormalTask blocker = task(fixture.project, "Priority work", 1L);
		blocker.setPriority(900);
		long day = CalendarOption.getInstance().getMillisPerDay();
		long oneDayLater = fixture.project.getEffectiveWorkCalendar().add(
			fixture.project.getStart(), day, false);
		blocker.setStart(oneDayLater);
		Assignment target = AssignmentService.getInstance().newAssignment(longTask, fixture.resource, 1D, 0L, this);
		long manualGapStart = target.getStart() + com.microproject.pm.calendar.WorkCalendar.MILLIS_IN_HOUR;
		target.split(this, manualGapStart, manualGapStart + com.microproject.pm.calendar.WorkCalendar.MILLIS_IN_HOUR);
		ResourceImpl otherResource = fixture.project.getResourcePool().newResourceInstance();
		otherResource.setName("Other");
		Assignment unaffected = AssignmentService.getInstance().newAssignment(longTask, otherResource, 1D, 0L, this);
		Assignment blockerAssignment = AssignmentService.getInstance().newAssignment(blocker, fixture.resource, 1D, 0L, this);
		NormalTask successor = task(fixture.project, "Successor", 1L);
		DependencyService.getInstance().newDependency(longTask, successor, DependencyType.FS, 0L, this);
		long taskStart = longTask.getStart();
		long unaffectedStart = unaffected.getStart();
		long beforeDuration = target.getDurationMillis();
		String beforeContour = target.getWorkContour().toString(beforeDuration);
		ResourceLevelingService service = new ResourceLevelingService();
		ResourceLevelingService.Plan plan = service.preview(fixture.project, List.of(fixture.resource),
			new ResourceLevelingService.Options(ResourceLevelingService.Order.PRIORITY_STANDARD, false, true,
				Long.MIN_VALUE, Long.MAX_VALUE, false, ResourceLevelingService.Sensitivity.HOUR, true, false));

		assertTrue(plan.changes().isEmpty(), "assignment mode must not move the shared task");
		assertTrue(plan.splits().isEmpty(), "assignment mode must not add a whole-task split");
		assertEquals(1, plan.assignmentSplits().size(), "the target assignment can split around the blocker: "
			+ plan.unresolved() + " changes=" + plan.assignmentChanges() + " target=" + target.getStart() + ".."
			+ target.getEnd() + " blocker=" + blockerAssignment.getStart() + ".." + blockerAssignment.getEnd());
		plan.apply();
		assertNotNull(target.getLevelingSplitState());
		assertEquals(taskStart, longTask.getStart());
		assertEquals(unaffectedStart, unaffected.getStart());
		assertTrue(longTask.getEnd() >= target.getEnd(), "task finish must cover the extended assignment span");
		assertTrue(summary.getEnd() >= target.getEnd(), "automatic summary finish must roll up the extended assignment");
		assertTrue(successor.getStart() >= longTask.getEnd(), "FS successor must follow the updated task finish");
		assertNotEquals(beforeContour, target.getWorkContour().toString(target.getDurationMillis()));

		var undo = fixture.project.getUndoController();
		undo.clear();
		service.clear(fixture.project, null, undo.getEditSupport());
		assertNull(target.getLevelingSplitState());
		assertEquals(beforeDuration, target.getDurationMillis());
		assertEquals(beforeContour, target.getWorkContour().toString(beforeDuration));
		undo.undo();
		assertNotNull(target.getLevelingSplitState(), "Undo restores the generated assignment split and its ownership");
		undo.redo();
		assertNull(target.getLevelingSplitState());
	}

	@Test
	void dialogDefaultSettingsPlanAnAssignmentSplitAroundAnInteriorBlocker() {
		// Mirrors the Resource Leveling dialog fixture: product-default CCPM settings
		// (priority/standard order, day sensitivity, assignment leveling) after recalculation.
		Fixture fixture = fixture();
		NormalTask longTask = task(fixture.project, "Long task", 5L);
		NormalTask blocker = task(fixture.project, "Priority work", 1L);
		blocker.setPriority(900);
		blocker.setStart(fixture.project.getEffectiveWorkCalendar().add(
			fixture.project.getStart(), CalendarOption.getInstance().getMillisPerDay(), false));
		AssignmentService.getInstance().newAssignment(longTask, fixture.resource, 1D, 0L, this);
		AssignmentService.getInstance().newAssignment(blocker, fixture.resource, 1D, 0L, this);
		fixture.project.recalculate();
		com.microproject.pm.ccpm.CriticalChainService.Settings defaults =
			new com.microproject.pm.ccpm.CriticalChainService.Settings();
		ResourceLevelingService.Plan plan = new ResourceLevelingService().preview(fixture.project,
			List.of(fixture.resource), new ResourceLevelingService.Options(defaults.getLevelingOrder(),
				defaults.isOnlyWithinAvailableSlack(), defaults.isAllowTaskSplits(), Long.MIN_VALUE, Long.MAX_VALUE,
				defaults.isIncludeProposedResources(), defaults.getSensitivity(), defaults.isLevelAssignments(), defaults.isLevelManuallyScheduledTasks()));

		assertEquals(1, plan.assignmentSplits().size(), "dialog defaults must offer the assignment split: "
			+ plan.unresolved() + " changes=" + plan.assignmentChanges());
	}

	@Test
	void assignmentLevelingClearRejectsAContourEditedAfterApply() {
		Fixture fixture = fixture();
		NormalTask longTask = task(fixture.project, "Long task", 5L);
		NormalTask blocker = task(fixture.project, "Priority work", 1L);
		blocker.setPriority(900);
		blocker.setStart(fixture.project.getEffectiveWorkCalendar().add(
			fixture.project.getStart(), CalendarOption.getInstance().getMillisPerDay(), false));
		Assignment target = AssignmentService.getInstance().newAssignment(longTask, fixture.resource, 1D, 0L, this);
		AssignmentService.getInstance().newAssignment(blocker, fixture.resource, 1D, 0L, this);
		ResourceLevelingService service = new ResourceLevelingService();
		ResourceLevelingService.Plan plan = service.preview(fixture.project, List.of(fixture.resource),
			new ResourceLevelingService.Options(ResourceLevelingService.Order.PRIORITY_STANDARD, false, true,
				Long.MIN_VALUE, Long.MAX_VALUE, false, ResourceLevelingService.Sensitivity.HOUR, true, false));
		assertEquals(1, plan.assignmentSplits().size());
		plan.apply();
		String generatedContour = target.getWorkContour().toString(target.getDurationMillis());
		target.extend(target.getStart(), target.getEnd(), com.microproject.pm.calendar.WorkCalendar.MILLIS_IN_HOUR);
		String userContour = target.getWorkContour().toString(target.getDurationMillis());

		assertThrows(IllegalStateException.class, () -> service.clear(fixture.project));
		assertEquals(userContour, target.getWorkContour().toString(target.getDurationMillis()));
		assertNotEquals(generatedContour, userContour);
		assertNotNull(target.getLevelingSplitState());
	}

	@Test
	void assignmentLevelingSplitLeavesPartialAndCompleteActualWorkUntouched() {
		for (double percentComplete : new double[] {0.5D, 1.0D}) {
			Fixture fixture = fixture();
			NormalTask targetTask = task(fixture.project, "Actual work " + percentComplete, 5L);
			NormalTask blockerTask = task(fixture.project, "Blocker " + percentComplete, 1L);
			blockerTask.setPriority(900);
			blockerTask.setStart(fixture.project.getEffectiveWorkCalendar().add(
				fixture.project.getStart(), CalendarOption.getInstance().getMillisPerDay(), false));
			Assignment target = AssignmentService.getInstance().newAssignment(targetTask, fixture.resource, 1D, 0L, this);
			AssignmentService.getInstance().newAssignment(blockerTask, fixture.resource, 1D, 0L, this);
			target.setPercentComplete(percentComplete);
			long actualWorkBefore = target.getActualWork(null);

			ResourceLevelingService.Plan plan = new ResourceLevelingService().preview(fixture.project,
				List.of(fixture.resource), new ResourceLevelingService.Options(ResourceLevelingService.Order.PRIORITY_STANDARD,
					false, true, Long.MIN_VALUE, Long.MAX_VALUE, false, ResourceLevelingService.Sensitivity.HOUR, true, false));

			assertTrue(plan.assignmentSplits().isEmpty(), "generated assignment splits must not rewrite actual work at "
				+ percentComplete * 100 + "% complete");
			assertEquals(actualWorkBefore, target.getActualWork(null));
		}
	}

	@Test
	void clearingLevelingPreservesAssignmentStartEditedByTheUser() {
		Fixture fixture = fixture();
		NormalTask longTask = task(fixture.project, "Long task", 3L);
		NormalTask blocker = task(fixture.project, "Priority work", 1L);
		blocker.setPriority(900);
		long oneDayLater = fixture.project.getEffectiveWorkCalendar().add(
			fixture.project.getStart(), CalendarOption.getInstance().getMillisPerDay(), false);
		blocker.setStart(oneDayLater);
		Assignment target = AssignmentService.getInstance().newAssignment(longTask, fixture.resource, 1D, 0L, this);
		AssignmentService.getInstance().newAssignment(blocker, fixture.resource, 1D, 0L, this);
		ResourceLevelingService.Plan plan = new ResourceLevelingService().preview(fixture.project,
			List.of(fixture.resource), new ResourceLevelingService.Options(ResourceLevelingService.Order.PRIORITY_STANDARD,
				false, false, Long.MIN_VALUE, Long.MAX_VALUE, false, ResourceLevelingService.Sensitivity.DAY, true, false));
		plan.apply();
		long userStart = target.getEffectiveWorkCalendar().add(target.getStart(), CalendarOption.getInstance().getMillisPerDay(), false);
		target.setStart(userStart);
		assertEquals(0L, com.microproject.datatype.Duration.millis(target.getLevelingDelay()));
		assertEquals(userStart, target.getStart());

		new ResourceLevelingService().clear(fixture.project);
		assertEquals(userStart, target.getStart(), "Clear must retain the user's assignment start edit");
		assertTrue(com.microproject.datatype.Duration.millis(target.getDelay()) > 0L);
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

	private static ResourceLevelingService.Options wholeTaskOptions() {
		return new ResourceLevelingService.Options(ResourceLevelingService.Order.STANDARD, false, true,
			Long.MIN_VALUE, Long.MAX_VALUE, false, ResourceLevelingService.Sensitivity.DAY, false, false);
	}

	private NormalTask task(Project project, String name, long days) {
		NormalTask task = new NormalTask(project);
		task.setName(name);
		task.setManuallyScheduled(false);
		project.connectTask(task);
		project.getSchedulingAlgorithm().addObject(task);
		task.getCurrentSchedule().setStart(project.getStart());
		task.setDuration(days * CalendarOption.getInstance().getMillisPerDay());
		return task;
	}

	private record Fixture(Project project, ResourceImpl resource) {
	}

	private static final class ThrowingClearTask extends NormalTask {
		private boolean failAfterClear;
		private ThrowingClearTask(Project project) { super(project); }
		@Override public boolean clearLevelingGeneratedSplits(Object eventSource) {
			boolean changed = super.clearLevelingGeneratedSplits(eventSource);
			if (failAfterClear) throw new IllegalStateException("injected clear failure");
			return changed;
		}
	}

	private static final class ThrowingApplySplitTask extends NormalTask {
		private boolean failAfterSplit;
		private ThrowingApplySplitTask(Project project) { super(project); }
		@Override public void splitForLeveling(Object eventSource, long from, long to) {
			super.splitForLeveling(eventSource, from, to);
			if (failAfterSplit) throw new IllegalStateException("injected apply failure");
		}
	}

	private static final class ThrowingDirtyTask extends NormalTask {
		private boolean failNextDirty;
		private ThrowingDirtyTask(Project project) { super(project); }
		@Override public void setDirty(boolean dirty) {
			if (failNextDirty) {
				failNextDirty = false;
				throw new IllegalStateException("injected second assignment failure");
			}
			super.setDirty(dirty);
		}
	}

	private static final class InvariantFailClearTask extends NormalTask {
		private boolean failInvariantAfterClear;
		private InvariantFailClearTask(Project project) { super(project); }
		@Override public boolean clearLevelingGeneratedSplits(Object eventSource) {
			boolean changed = super.clearLevelingGeneratedSplits(eventSource);
			failInvariantAfterClear = true;
			return changed;
		}
		@Override public java.util.List<com.microproject.pm.task.TaskSplitInterval> getLevelingTaskSplitIntervals() {
			if (failInvariantAfterClear && super.getLevelingTaskSplitIntervals().isEmpty())
				return java.util.List.of(new com.microproject.pm.task.TaskSplitInterval(0L, 1L));
			return super.getLevelingTaskSplitIntervals();
		}
	}
}
