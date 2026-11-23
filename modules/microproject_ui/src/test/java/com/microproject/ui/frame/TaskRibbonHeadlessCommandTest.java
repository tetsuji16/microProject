/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.frame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.microproject.collaboration.CollaborationSession;
import com.microproject.exchange.MpoFileImporter;
import com.microproject.options.CalendarOption;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.AssignmentService;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.ui.view.model.cache.GraphicNode;
import com.microproject.ui.view.spreadsheet.SpreadSheet;
import com.microproject.ui.view.spreadsheet.common.CommonSpreadSheetModel;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.ribbon.CommandId;
import com.microproject.ribbon.RibbonCommandResult;

/**
 * Command behavior for the Task ribbon commands whose only contract is the
 * model mutation, the table/Gantt projection of that model, one Undo edit, Redo
 * and MPO persistence (issue #1034). The physical ribbon click, popup, menu and
 * shortcut routes are covered once per input mechanism by the {@code @GuiJourney}
 * tests; these cases run headless on a real {@link DocumentFrame} through the
 * canonical command route.
 */
class TaskRibbonHeadlessCommandTest {
	private static final String TASK_MODE_NAME = "Ribbon information acceptance";

	@Test
	void taskModeManualChangesModelAndRoundTripsUndoRedoAndMpo() throws Exception {
		try (HeadlessDocumentFixture fx = new HeadlessDocumentFixture(TASK_MODE_NAME)) {
			NormalTask task = fx.task(0);
			assertFalse(task.isManuallyScheduled(), "the fixture task must start automatically scheduled");

			fx.select(task);
			RibbonCommandResult result = fx.run(CommandId.TASK_MODE_MANUAL);
			assertEquals(RibbonCommandResult.Status.CHANGED, result.status(), "task mode result: " + result);
			assertEquals(List.of(task.getUniqueId()), result.affectedTaskIds(), "task mode affected task IDs");
			assertTrue(task.isManuallyScheduled(), "Manual Schedule must update the selected task");

			fx.undo();
			assertFalse(task.isManuallyScheduled(), "Undo must restore automatic scheduling");
			fx.redo();
			assertTrue(task.isManuallyScheduled(), "Redo must restore manual scheduling");

			Project reloaded = saveAndReload(fx.project);
			assertTrue(taskNamed(reloaded, TASK_MODE_NAME).isManuallyScheduled(),
				"MPO reload must preserve manual scheduling mode");
		}
	}

	@Test
	void markOnTrackSetsProgressThroughStatusDateAndRoundTripsUndoRedoAndMpo() throws Exception {
		try (HeadlessDocumentFixture fx = new HeadlessDocumentFixture("Mark on Track acceptance")) {
			NormalTask task = fx.task(0);
			Project project = fx.project;
			task.setDuration(3L * CalendarOption.getInstance().getMillisPerDay());
			project.recalculate();
			task.setImportedPercentComplete(0.25D);
			long statusDateInput = task.getStart() + (task.getEnd() - task.getStart()) / 2L;
			project.setStatusDate(statusDateInput);
			long explicitStatusDate = project.getStatusDate();
			long scheduledDuration = task.getEffectiveWorkCalendar().compare(task.getEnd(), task.getStart(), false);
			long scheduledThroughDate = task.getEffectiveWorkCalendar().compare(explicitStatusDate,
				task.getStart(), false);
			double expectedPercentComplete = Math.max(0D, Math.min(1D,
				(double) scheduledThroughDate / scheduledDuration));
			assertTrue(explicitStatusDate > task.getStart() && explicitStatusDate < task.getEnd(),
				"the explicit status date must fall inside the multi-day task");
			assertNotEquals(0.25D, expectedPercentComplete, 0.00001D,
				"the computed target must differ from the seeded progress");

			fx.select(task);
			RibbonCommandResult result = fx.run(CommandId.MARK_ON_TRACK);
			assertEquals(RibbonCommandResult.Status.CHANGED, result.status(), "Mark on Track result: " + result);
			assertEquals(List.of(task.getUniqueId()), result.affectedTaskIds(), "Mark on Track affected task IDs");
			assertEquals(expectedPercentComplete, task.getPercentComplete(), 0D,
				"Mark on Track must set progress through the status date");
			assertTrue(project.isStatusDateSet(), "Mark on Track must keep the explicit Status Date");
			assertEquals(explicitStatusDate, project.getStatusDate(), "Mark on Track must not move the Status Date");

			fx.undo();
			assertEquals(0.25D, task.getPercentComplete(), 0D, "Undo must restore the seeded progress");
			assertTrue(project.isStatusDateSet() && project.getStatusDate() == explicitStatusDate,
				"Undo must keep the explicit Status Date");
			fx.redo();
			assertEquals(expectedPercentComplete, task.getPercentComplete(), 0D,
				"Redo must restore the Mark on Track percentage");
			assertTrue(project.isStatusDateSet() && project.getStatusDate() == explicitStatusDate,
				"Redo must keep the explicit Status Date");

			Project reloaded = saveAndReload(project);
			assertTrue(reloaded.isStatusDateSet(), "MPO reopen lost the explicit Status Date");
			assertEquals(explicitStatusDate, reloaded.getStatusDate(),
				"MPO reopen must preserve the exact explicit Status Date");
			assertEquals(expectedPercentComplete,
				taskNamed(reloaded, "Mark on Track acceptance").getPercentComplete(), 0D,
				"MPO reopen must preserve the exact Mark on Track percentage");
		}
	}

	@Test
	void inactivateTaskKeepsTaskVisibleAndRoundTripsUndoRedoAndMpo() throws Exception {
		String name = "Inactivate acceptance";
		try (HeadlessDocumentFixture fx = new HeadlessDocumentFixture(name)) {
			NormalTask task = fx.task(0);
			HeadlessDocumentFixture.onEdt(() -> {
				task.setDuration(CalendarOption.getInstance().getMillisPerDay());
				task.setPercentComplete(0.4D);
				fx.project.recalculate();
			});
			int rowsBefore = fx.rowCount();
			assertFalse(task.isInactiveTask(), "the fixture task must start active");

			fx.select(task);
			RibbonCommandResult result = fx.run(CommandId.INACTIVATE_TASK);
			assertEquals(RibbonCommandResult.Status.CHANGED, result.status(), "Inactivate result: " + result);
			assertEquals(List.of(task.getUniqueId()), result.affectedTaskIds(), "Inactivate affected task IDs");
			assertTrue(task.isInactiveTask(), "Inactivate must clear the selected task's Active field");
			assertTrue(fx.project.getTaskList().contains(task), "Inactivate must keep the task record");
			assertEquals(rowsBefore, fx.rowCount(), "Inactivate must keep the task table row");
			assertEquals(0.4D, task.getPercentComplete(), 0.00001D, "Inactivate must preserve progress");

			fx.undo();
			assertFalse(task.isInactiveTask(), "Undo must reactivate the task");
			fx.redo();
			assertTrue(task.isInactiveTask(), "Redo must inactivate the task");

			Project reloaded = saveAndReload(fx.project);
			NormalTask reloadedTask = taskNamed(reloaded, name);
			assertTrue(reloadedTask.isInactiveTask(), "MPO reload lost the inactive state");
			assertEquals(0.4D, reloadedTask.getPercentComplete(), 0.00001D,
				"MPO reload lost inactive task progress");
		}
	}

	@Test
	void respectLinksMovesManualSuccessorAndRoundTripsUndoRedoAndMpo() throws Exception {
		try (HeadlessDocumentFixture fx = new HeadlessDocumentFixture("Respect Links predecessor",
				"Respect Links successor")) {
			Project project = fx.project;
			NormalTask predecessor = fx.task(0);
			NormalTask successor = fx.task(1);
			long day = CalendarOption.getInstance().getMillisPerDay();
			predecessor.setDuration(day);
			successor.setDuration(day);
			long base = project.getWorkCalendar().adjustInsideCalendar(project.getStart(), false);
			long predecessorStart = project.getWorkCalendar().add(base, 2L * day, false);
			long predecessorFinish = project.getWorkCalendar().add(predecessorStart, day, false);
			long oldSuccessorStart = base;
			long oldSuccessorFinish = project.getWorkCalendar().add(oldSuccessorStart, day, false);
			predecessor.setManualDates(predecessorStart, predecessorFinish);
			successor.setManualDates(oldSuccessorStart, oldSuccessorFinish);
			DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.FS, 0L, null);
			project.recalculate();
			assertTrue(successor.isManuallyScheduled(), "fixture successor must be manually scheduled");
			assertTrue(successor.getStart() < predecessor.getEnd(), "fixture must begin with an FS conflict");
			fx.undo.clear();

			fx.select(successor);
			RibbonCommandResult result = fx.run(CommandId.RESPECT_LINKS);
			assertEquals(RibbonCommandResult.Status.CHANGED, result.status(), "Respect Links result: " + result);
			assertEquals(List.of(successor.getUniqueId()), result.affectedTaskIds(),
				"Respect Links affected task IDs");
			assertTrue(successor.isManuallyScheduled(),
				"Respect Links must preserve manual mode while moving the successor");
			assertTrue(successor.getStart() >= predecessor.getEnd(),
				"Respect Links must move the successor after its predecessor");
			long respectedStart = successor.getStart();

			fx.undo();
			assertTrue(successor.isManuallyScheduled() && successor.getStart() == oldSuccessorStart
					&& successor.getEnd() == oldSuccessorFinish,
				"Undo must restore the original manually scheduled dates");
			fx.redo();
			assertTrue(successor.isManuallyScheduled() && successor.getStart() == respectedStart
					&& successor.getStart() >= predecessor.getEnd(),
				"Redo must restore the link-respecting dates and manual mode");

			Project reloaded = saveAndReload(project);
			NormalTask reloadedSuccessor = taskNamed(reloaded, "Respect Links successor");
			NormalTask reloadedPredecessor = taskNamed(reloaded, "Respect Links predecessor");
			assertTrue(reloadedSuccessor.isManuallyScheduled(), "MPO reload lost manual scheduling mode");
			assertTrue(reloadedSuccessor.getStart() >= reloadedPredecessor.getEnd(),
				"MPO reload lost the respected FS boundary");
		}
	}

	@Test
	void hideAndShowSelectedTaskRoundTripsWithUndoRedoAndMpo() throws Exception {
		try (HeadlessDocumentFixture fx = new HeadlessDocumentFixture("Hide target", "Other task")) {
			NormalTask target = fx.task(0);
			NormalTask other = fx.task(1);
			Project project = fx.project;
			assertTrue(rowOf(fx, target) >= 0, "the fixture task must start visible in the task table");
			assertTrue(ganttRowOf(fx, target) >= 0, "the fixture task must start visible in the Gantt");

			fx.select(target);
			RibbonCommandResult hideResult = fx.run(CommandId.HIDE_SELECTED);
			assertEquals(RibbonCommandResult.Status.CHANGED, hideResult.status(), "Hide result: " + hideResult);
			assertEquals(List.of(target.getUniqueId()), hideResult.selectedTaskIds(), "Hide selected task IDs");
			assertEquals(List.of(target.getUniqueId()), hideResult.affectedTaskIds(), "Hide affected task IDs");
			assertTrue(target.isHiddenTask(), "Hide Selected Tasks must update the task model");
			assertFalse(other.isHiddenTask(), "Hide must leave an unselected task visible");
			assertEquals(-1, rowOf(fx, target), "a hidden task must leave the task table projection");
			assertEquals(-1, ganttRowOf(fx, target), "a hidden task must leave the Gantt projection");
			assertTrue(rowOf(fx, other) >= 0, "an unselected task must stay in the task table");

			Project hiddenReload = saveAndReload(project);
			assertTrue(taskNamed(hiddenReload, "Hide target").isHiddenTask(),
				"MPO reload lost the hidden task state");

			fx.undo();
			assertFalse(target.isHiddenTask(), "Undo must restore task visibility");
			assertTrue(rowOf(fx, target) >= 0, "Undo must restore the visible task row");
			assertTrue(ganttRowOf(fx, target) >= 0, "Undo must restore the visible Gantt row");

			// The table rebuild after Undo clears the row selection, so reselect before the second Hide.
			fx.select(target);
			fx.run(CommandId.HIDE_SELECTED);
			assertTrue(target.isHiddenTask(), "the second Hide must hide the task again");
			fx.redo();
			assertTrue(target.isHiddenTask(), "Redo after a new edit must keep the hidden state");

			RibbonCommandResult showResult = fx.run(CommandId.SHOW_ALL);
			assertEquals(RibbonCommandResult.Status.CHANGED, showResult.status(), "Show All result: " + showResult);
			assertTrue(showResult.affectedTaskIds().contains(target.getUniqueId()),
				"Show All must report the restored task: " + showResult);
			assertFalse(target.isHiddenTask(), "Show All Tasks must restore the task model");
			assertTrue(rowOf(fx, target) >= 0, "Show All Tasks must restore the visible task row");
			assertTrue(ganttRowOf(fx, target) >= 0, "Show All Tasks must restore the visible Gantt row");

			Project shownReload = saveAndReload(project);
			assertFalse(taskNamed(shownReload, "Hide target").isHiddenTask(),
				"MPO reload lost the task restored through Show All Tasks");

			fx.undo();
			assertTrue(target.isHiddenTask(), "Undo must undo Show All Tasks");
			assertEquals(-1, rowOf(fx, target), "Undo Show All Tasks must hide the task row");
			fx.redo();
			assertFalse(target.isHiddenTask(), "Redo must redo Show All Tasks");
			assertTrue(rowOf(fx, target) >= 0, "Redo Show All Tasks must restore the task row");
		}
	}

	@Test
	void insertSummaryAndMilestoneRoutesInsertRowsWithUndoRedoAndMpo() throws Exception {
		try (HeadlessDocumentFixture fx = new HeadlessDocumentFixture("Insert task 1", "Insert task 2",
				"Insert task 3")) {
			NormalTask first = fx.task(0);
			NormalTask second = fx.task(1);
			NormalTask third = fx.task(2);
			Project project = fx.project;
			Set<Task> existing = Collections.newSetFromMap(new IdentityHashMap<>());
			existing.addAll(project.getTaskList());

			fx.select(first, second);
			fx.undo.clear();
			RibbonCommandResult summaryResult = fx.run(CommandId.INSERT_SUMMARY);
			assertEquals(RibbonCommandResult.Status.CHANGED, summaryResult.status(),
				"Insert Summary result: " + summaryResult);
			assertNotNull(first.getWbsParentTask(), "Insert Summary must indent the selected tasks");
			NormalTask summary = (NormalTask) first.getWbsParentTask();
			assertFalse(existing.contains(summary), "the summary parent must be a newly inserted task");
			assertSame(summary, second.getWbsParentTask(), "both selected tasks must share the new summary");
			assertNull(third.getWbsParentTask(), "the unselected task must stay top level");
			int summaryRow = rowOf(fx, summary);
			assertTrue(summaryRow >= 0, "the inserted summary must be visible in the task table");
			assertEquals(summaryRow + 1, rowOf(fx, first), "the summary row must precede its first child");
			assertEquals(summaryRow + 2, rowOf(fx, second), "the summary row must precede its second child");
			assertEquals(summaryRow + 3, rowOf(fx, third), "the unselected task must follow the children");
			assertEquals(summaryRow, ganttRowOf(fx, summary), "the Gantt row must align with the summary row");
			assertEquals(summaryRow + 1, ganttRowOf(fx, first), "the Gantt row must align with the first child");

			fx.undo();
			assertNull(first.getWbsParentTask(), "one Undo must restore the original hierarchy");
			assertNull(second.getWbsParentTask(), "one Undo must restore the second task");
			assertEquals(-1, rowOf(fx, summary), "Undo must remove the inserted summary row");
			assertEquals(0, first.getOutlineLevel(), "Undo must restore the first task outline level");
			assertEquals(0, second.getOutlineLevel(), "Undo must restore the second task outline level");
			fx.redo();
			assertSame(summary, first.getWbsParentTask(), "Redo must re-apply the summary");
			assertSame(summary, second.getWbsParentTask(), "Redo must re-apply the summary to both tasks");

			Project summaryReload = saveAndReload(project);
			Task reloadedParent = taskNamed(summaryReload, "Insert task 1").getWbsParentTask();
			assertNotNull(reloadedParent, "MPO reload lost the inserted summary parent");
			assertSame(reloadedParent, taskNamed(summaryReload, "Insert task 2").getWbsParentTask(),
				"MPO reload split the children");
			assertTrue(reloadedParent.isWbsParent(), "MPO reload parent is not a summary");
			assertEquals(1, taskNamed(summaryReload, "Insert task 1").getOutlineLevel(),
				"MPO reload lost the child outline level");
			assertNull(taskNamed(summaryReload, "Insert task 3").getWbsParentTask(),
				"MPO reload indented the unselected task");

			Set<Task> beforeMilestone = Collections.newSetFromMap(new IdentityHashMap<>());
			beforeMilestone.addAll(project.getTaskList());
			fx.select(third);
			fx.undo.clear();
			RibbonCommandResult milestoneResult = fx.run(CommandId.INSERT_MILESTONE);
			assertEquals(RibbonCommandResult.Status.CHANGED, milestoneResult.status(),
				"Insert Milestone result: " + milestoneResult);
			NormalTask milestone = project.getTaskList().stream()
				.filter(candidate -> !beforeMilestone.contains(candidate))
				.map(NormalTask.class::cast).findFirst().orElse(null);
			assertNotNull(milestone, "Insert Milestone must add a task");
			assertTrue(milestone.isMilestone(), "the inserted task must be a milestone");
			assertEquals(0L, milestone.getDuration(), "the inserted milestone must have zero duration");
			assertEquals(rowOf(fx, third) - 1, rowOf(fx, milestone),
				"the milestone row must sit directly above the selected task");
			assertEquals(rowOf(fx, milestone), ganttRowOf(fx, milestone),
				"the milestone must have an aligned Gantt row");

			fx.undo();
			assertEquals(-1, rowOf(fx, milestone), "one Undo must remove the milestone row");
			assertSame(summary, first.getWbsParentTask(), "Undoing the milestone must keep the earlier summary");

			Project milestoneReload = saveAndReload(project);
			assertTrue(taskNamed(milestoneReload, "Insert task 3").getWbsParentTask() == null,
				"MPO reload must keep the unselected task at top level after undoing the milestone");
		}
	}

	@Test
	void percentCompleteSetsSelectedTasksAndRoundTripsUndoRedoLockAndMpo() throws Exception {
		try (HeadlessDocumentFixture fx = new HeadlessDocumentFixture("Percent task 1", "Percent task 2",
				"Percent task 3")) {
			NormalTask first = fx.task(0);
			NormalTask second = fx.task(1);
			NormalTask third = fx.task(2);
			Project project = fx.project;
			HeadlessDocumentFixture.onEdt(() -> {
				for (NormalTask task : fx.tasks) {
					task.setDuration(4L * CalendarOption.getInstance().getMillisPerDay());
					task.setPercentComplete(0.1D);
				}
				project.recalculate();
				fx.sheet().clearSelection();
				fx.undo.clear();
			});

			// No selection: the command refuses instead of changing every task.
			RibbonCommandResult noSelection = fx.run(CommandId.PERCENT_COMPLETE_50);
			assertEquals(RibbonCommandResult.Status.REJECTED, noSelection.status(),
				"no-selection result: " + noSelection);
			assertEquals("no-selection", noSelection.reason(), "no-selection reason");
			assertEquals(0.1D, first.getPercentComplete(), 0.00001D, "a rejected % Complete changed a task");
			assertEquals(0.1D, second.getPercentComplete(), 0.00001D, "a rejected % Complete changed a task");
			assertFalse(fx.undo.canUndo(), "a rejected % Complete must not post an Undo edit");

			fx.select(first, second);
			RibbonCommandResult result = fx.run(CommandId.PERCENT_COMPLETE_50);
			assertEquals(RibbonCommandResult.Status.CHANGED, result.status(), "% Complete result: " + result);
			assertEquals(List.of(first.getUniqueId(), second.getUniqueId()), result.affectedTaskIds(),
				"% Complete affected task IDs");
			assertEquals(0.5D, first.getPercentComplete(), 0D, "50% Complete must set the first selected task");
			assertEquals(0.5D, second.getPercentComplete(), 0D, "50% Complete must set the second selected task");
			assertEquals(0.1D, third.getPercentComplete(), 0.00001D, "50% Complete changed an unselected task");
			assertEquals(rowOf(fx, first), ganttRowOf(fx, first),
				"the Gantt progress row must stay aligned with the first task row");
			assertEquals(rowOf(fx, second), ganttRowOf(fx, second),
				"the Gantt progress row must stay aligned with the second task row");

			fx.undo();
			assertEquals(0.1D, first.getPercentComplete(), 0.00001D, "one Undo must restore the first task");
			assertEquals(0.1D, second.getPercentComplete(), 0.00001D, "one Undo must restore the second task");
			fx.redo();
			assertEquals(0.5D, first.getPercentComplete(), 0D, "Redo must re-apply 50% Complete");
			assertEquals(0.5D, second.getPercentComplete(), 0D, "Redo must re-apply 50% Complete to both tasks");
			assertEquals(0.1D, third.getPercentComplete(), 0.00001D, "Redo changed an unselected task");

			Project reloaded = saveAndReload(project);
			assertEquals(0.5D, taskNamed(reloaded, "Percent task 1").getPercentComplete(), 0D,
				"MPO reload lost 50% (first)");
			assertEquals(0.5D, taskNamed(reloaded, "Percent task 2").getPercentComplete(), 0D,
				"MPO reload lost 50% (second)");
			assertEquals(0.1D, taskNamed(reloaded, "Percent task 3").getPercentComplete(), 0.00001D,
				"MPO reload changed the unselected task");

			// A collaborator lock on the target rejects the command without mutation or Undo.
			fx.select(third);
			fx.undo.clear();
			HeadlessDocumentFixture.onEdt(() -> project.setCollaborationSession(new CollaborationSession(project,
					System.getProperty("java.io.tmpdir") + "/microproject-headless-percent-lock.mpo", "percent-lock-test") {
				@Override
				public Task tryAcquireTasks(Iterable<Task> tasks) {
					if (tasks == null)
						return null;
					java.util.Iterator<Task> iterator = tasks.iterator();
					return iterator.hasNext() ? iterator.next() : null;
				}

				@Override
				public String describeLockOwner(Task task) {
					return "percent lock test";
				}
			}));
			try {
				RibbonCommandResult locked = fx.run(CommandId.PERCENT_COMPLETE_100);
				assertEquals(RibbonCommandResult.Status.REJECTED, locked.status(), "locked % Complete result: " + locked);
				assertEquals("collaboration-lock-denied", locked.reason(), "locked % Complete reason");
				assertEquals(0.1D, third.getPercentComplete(), 0.00001D, "a lock-rejected % Complete changed the task");
				assertFalse(fx.undo.canUndo(), "a lock-rejected % Complete must not post an Undo edit");
			} finally {
				HeadlessDocumentFixture.onEdt(() -> project.setCollaborationSession(null));
			}

			RibbonCommandResult completed = fx.run(CommandId.PERCENT_COMPLETE_100);
			assertEquals(RibbonCommandResult.Status.CHANGED, completed.status(), "100% result: " + completed);
			assertEquals(1D, third.getPercentComplete(), 0D, "100% Complete must complete the selected task");
			assertEquals(0.5D, first.getPercentComplete(), 0D, "100% Complete changed an unselected task");
		}
	}

	@Test
	void deleteRemovesTheSelectedRowAndRoundTripsUndoRedoAndMpo() throws Exception {
		try (HeadlessDocumentFixture fx = new HeadlessDocumentFixture("Delete target", "Delete survivor")) {
			NormalTask target = fx.task(0);
			NormalTask survivor = fx.task(1);
			Project project = fx.project;
			HeadlessDocumentFixture.onEdt(fx.undo::clear);
			assertTrue(rowOf(fx, target) >= 0, "the deleted task must start visible in the task table");

			fx.select(target);
			RibbonCommandResult result = fx.run(CommandId.DELETE);
			assertEquals(RibbonCommandResult.Status.CHANGED, result.status(), "Delete result: " + result);
			assertEquals(List.of(target.getUniqueId()), result.affectedTaskIds(), "Delete affected task IDs");
			assertFalse(project.getTaskList().contains(target), "Delete must remove the task record");
			assertEquals(-1, rowOf(fx, target), "Delete must remove the task table row");
			assertEquals(-1, ganttRowOf(fx, target), "Delete must remove the Gantt row");
			assertTrue(rowOf(fx, survivor) >= 0, "Delete must keep an unselected task");

			Project deletedReload = saveAndReload(project);
			assertTrue(deletedReload.getTaskList().stream().noneMatch(task -> "Delete target".equals(task.getName())),
				"MPO reload retained a task deleted through the command route");

			fx.undo();
			assertTrue(project.getTaskList().contains(target), "Undo must restore the task record");
			assertTrue(rowOf(fx, target) >= 0, "Undo must restore the deleted row");
			assertTrue(ganttRowOf(fx, target) >= 0, "Undo must restore the deleted Gantt row");
			fx.redo();
			assertFalse(project.getTaskList().contains(target), "Redo must delete the task again");
			assertEquals(-1, rowOf(fx, target), "Redo must remove the restored row again");
		}
	}

	@Test
	void indentAndOutdentRoundTripHierarchyProjectionAndMpo() throws Exception {
		try (HeadlessDocumentFixture fx = new HeadlessDocumentFixture("Outline first", "Outline second",
				"Outline external")) {
			NormalTask first = fx.task(0);
			NormalTask second = fx.task(1);
			NormalTask external = fx.task(2);
			Project project = fx.project;
			assertNull(second.getWbsParentTask(), "the fixture target must start at the top level");
			assertEquals(0, second.getOutlineLevel(), "the fixture target must start at outline level 0");
			assertEquals(rowOf(fx, first) + 1, rowOf(fx, second), "the fixture must keep the sibling order");
			HeadlessDocumentFixture.onEdt(fx.undo::clear);

			fx.select(second);
			RibbonCommandResult indent = fx.run(CommandId.INDENT);
			assertEquals(RibbonCommandResult.Status.CHANGED, indent.status(), "Indent result: " + indent);
			assertEquals(List.of(second.getUniqueId()), indent.affectedTaskIds(), "Indent affected task IDs");
			assertSame(first, second.getWbsParentTask(), "Indent must make the target a child of its predecessor");
			assertEquals(0, first.getOutlineLevel(), "Indent must keep the parent at outline level 0");
			assertEquals(1, second.getOutlineLevel(), "Indent must deepen the target outline level");
			assertEquals(rowOf(fx, first) + 1, rowOf(fx, second),
				"Indent must render the child directly below its parent");
			assertEquals(rowOf(fx, second), ganttRowOf(fx, second),
				"Indent must keep the Gantt row aligned with the task row");

			fx.undo();
			assertNull(second.getWbsParentTask(), "Undo must restore the top level");
			assertEquals(0, second.getOutlineLevel(), "Undo must restore the outline level");
			fx.redo();
			assertSame(first, second.getWbsParentTask(), "Redo must re-apply the indent");
			assertEquals(1, second.getOutlineLevel(), "Redo must re-apply the outline level");

			// The hierarchy refresh after Undo/Redo rebuilds the rows, so the table no
			// longer holds the row the user still sees selected; reselect that row.
			fx.select(second);
			RibbonCommandResult outdent = fx.run(CommandId.OUTDENT);
			assertEquals(RibbonCommandResult.Status.CHANGED, outdent.status(), "Outdent result: " + outdent);
			assertEquals(List.of(second.getUniqueId()), outdent.affectedTaskIds(), "Outdent affected task IDs");
			assertNull(second.getWbsParentTask(), "Outdent must restore the target to the top level");
			assertEquals(0, second.getOutlineLevel(), "Outdent must restore outline level 0");
			assertEquals(rowOf(fx, first) + 1, rowOf(fx, second),
				"Outdent must render the siblings in project order");
			assertEquals(rowOf(fx, second), ganttRowOf(fx, second),
				"Outdent must keep the Gantt row aligned with the task row");

			fx.undo();
			assertSame(first, second.getWbsParentTask(), "Undo must restore the outdented child");
			fx.redo();
			assertNull(second.getWbsParentTask(), "Redo must re-apply the outdent");

			Project reloaded = saveAndReload(project);
			NormalTask reloadedSecond = taskNamed(reloaded, "Outline second");
			assertNull(reloadedSecond.getWbsParentTask(), "MPO reload retained a parent after Outdent");
			assertEquals(0, reloadedSecond.getOutlineLevel(), "MPO reload changed the Outdent target outline level");
			assertEquals(0, taskNamed(reloaded, "Outline first").getOutlineLevel(),
				"MPO reload changed the predecessor outline level");
			assertTrue(reloaded.getTaskList().indexOf(taskNamed(reloaded, "Outline first")) < reloaded.getTaskList()
					.indexOf(reloadedSecond), "MPO reload changed the visible task ordering after Outdent");

			// An external task is individually read-only inside an editable project.
			HeadlessDocumentFixture.onEdt(() -> external.setExternal(true));
			assertTrue(external.isExternal(), "the designated task must model an external task");
			assertTrue(external.isReadOnly(), "an external task must be individually read-only");
			assertFalse(project.isReadOnly(), "the containing project must remain editable");
			fx.undo.clear();
			int externalRow = rowOf(fx, external);
			assertTrue(externalRow >= 0, "the external task must stay in the task table");
			fx.select(external);
			RibbonCommandResult readOnly = fx.run(CommandId.INDENT);
			assertEquals(RibbonCommandResult.Status.REJECTED, readOnly.status(), "read-only Indent result: " + readOnly);
			assertEquals("task-read-only", readOnly.reason(), "read-only Indent reason");
			assertEquals(List.of(external.getUniqueId()), readOnly.selectedTaskIds(),
				"the rejection must retain the selected task identity");
			assertEquals(List.of(), readOnly.affectedTaskIds(), "the rejection must report no affected tasks");
			assertNull(external.getWbsParentTask(), "read-only Indent changed the task hierarchy");
			assertEquals(0, external.getOutlineLevel(), "read-only Indent changed the outline level");
			assertEquals(externalRow, rowOf(fx, external), "read-only Indent changed the visible row");
			assertFalse(fx.undo.canUndo(), "read-only Indent created an Undo edit");
		}
	}

	@Test
	void levelSelectionDelaysTheSelectedAssignmentAndRoundTripsUndoRedoAndMpo() throws Exception {
		try (HeadlessDocumentFixture fx = new HeadlessDocumentFixture("Unselected capacity anchor",
				"Selected leveling target")) {
			Project project = fx.project;
			NormalTask fixed = fx.task(0);
			NormalTask selected = fx.task(1);
			ResourcePool pool = project.getResourcePool();
			Assignment[] assignments = new Assignment[2];
			HeadlessDocumentFixture.onEdt(() -> {
				pool.setLocal(true);
				Resource shared = pool.newResourceInstance();
				shared.setName("Leveling resource");
				assignments[0] = AssignmentService.getInstance().newAssignment(fixed, shared, 1.0D, 0L, getClass());
				assignments[1] = AssignmentService.getInstance().newAssignment(selected, shared, 1.0D, 0L, getClass());
				project.recalculate();
				fx.undo.clear();
			});
			Assignment selectedAssignment = assignments[1];
			long originalStart = selected.getStart();
			long originalAssignmentStart = selectedAssignment.getStart();
			assertEquals(0L, selectedAssignment.getLevelingDelay(), "the fixture must start unleveled");

			fx.select(selected);
			RibbonCommandResult result = fx.run(CommandId.RESOURCE_LEVEL_SELECTION);
			assertEquals(RibbonCommandResult.Status.CHANGED, result.status(), "Level Selection result: " + result);
			assertEquals(List.of(selected.getUniqueId()), result.affectedTaskIds(),
				"Level Selection affected task IDs");
			assertTrue(selectedAssignment.getLevelingDelay() > 0L,
				"Level Selection must delay the selected task's assignment");
			assertTrue(selectedAssignment.getStart() > originalAssignmentStart,
				"the selected assignment's scheduled start must reflect the leveling change");
			assertEquals(originalStart, selected.getStart(),
				"assignment-level leveling must preserve the selected task start");
			assertEquals(0L, selected.getLevelingDelay(),
				"assignment-level leveling must preserve the task-wide leveling delay");
			assertEquals(0L, fixed.getLevelingDelay(), "the unselected task must remain a fixed leveling anchor");
			assertEquals(List.of(selected.getUniqueId()), result.selectedTaskIds(),
					"Level Selection must report the preserved task selection");
			assertTrue(rowOf(fx, selected) >= 0, "Level Selection must keep the leveled task in the task table");

			Project reloaded = saveAndReload(project);
			NormalTask reloadedSelected = taskNamed(reloaded, "Selected leveling target");
			Assignment reloadedAssignment = (Assignment) reloadedSelected.getAssignments().iterator().next();
			assertTrue(reloadedAssignment.getLevelingDelay() > 0L, "MPO reload lost the assignment leveling delay");
			assertEquals(0L, reloadedSelected.getLevelingDelay(),
				"MPO reload must preserve the task-wide leveling delay");

			fx.undo();
			assertEquals(0L, selectedAssignment.getLevelingDelay(), "one Undo must clear the assignment delay");
			assertEquals(originalAssignmentStart, selectedAssignment.getStart(),
				"one Undo must restore the assignment start");
			assertEquals(originalStart, selected.getStart(), "Undo must preserve the selected task start");
			assertEquals(0L, selected.getLevelingDelay(), "Undo must preserve the task-wide leveling delay");
			assertEquals(0L, fixed.getLevelingDelay(), "Undo must not mutate the unselected anchor");
			fx.redo();
			assertTrue(selectedAssignment.getLevelingDelay() > 0L, "one Redo must restore the assignment delay");
			assertTrue(selectedAssignment.getStart() > originalAssignmentStart, "one Redo must restore the assignment start");
			assertEquals(originalStart, selected.getStart(), "Redo must preserve the selected task start");
			assertEquals(0L, selected.getLevelingDelay(), "Redo must preserve the task-wide leveling delay");
		}
	}

	@Test
	void levelAllDelaysEveryResourceAssignmentAndRoundTripsUndoRedoAndMpo() throws Exception {
		try (HeadlessDocumentFixture fx = new HeadlessDocumentFixture("Level All resource one anchor",
				"Level All resource one target", "Level All resource two anchor", "Level All resource two target")) {
			Project project = fx.project;
			NormalTask anchorOne = fx.task(0);
			NormalTask targetOne = fx.task(1);
			NormalTask anchorTwo = fx.task(2);
			NormalTask targetTwo = fx.task(3);
			ResourcePool pool = project.getResourcePool();
			Assignment[] assignments = new Assignment[2];
			HeadlessDocumentFixture.onEdt(() -> {
				pool.setLocal(true);
				Resource resourceOne = pool.newResourceInstance();
				resourceOne.setName("Level All resource one");
				Resource resourceTwo = pool.newResourceInstance();
				resourceTwo.setName("Level All resource two");
				AssignmentService.getInstance().newAssignment(anchorOne, resourceOne, 1.0D, 0L, getClass());
				assignments[0] = AssignmentService.getInstance().newAssignment(targetOne, resourceOne, 1.0D, 0L, getClass());
				AssignmentService.getInstance().newAssignment(anchorTwo, resourceTwo, 1.0D, 0L, getClass());
				assignments[1] = AssignmentService.getInstance().newAssignment(targetTwo, resourceTwo, 1.0D, 0L, getClass());
				project.recalculate();
				fx.undo.clear();
			});
			Assignment targetOneAssignment = assignments[0];
			Assignment targetTwoAssignment = assignments[1];
			assertEquals(0L, targetOne.getLevelingDelay(), "the first target must start unleveled");
			assertEquals(0L, targetTwo.getLevelingDelay(), "the second target must start unleveled");
			long targetOneStart = targetOne.getStart();
			long targetTwoStart = targetTwo.getStart();
			long targetOneAssignmentStart = targetOneAssignment.getStart();
			long targetTwoAssignmentStart = targetTwoAssignment.getStart();

			fx.select(anchorOne);
			RibbonCommandResult result = fx.run(CommandId.RESOURCE_LEVEL_ALL);
			assertEquals(RibbonCommandResult.Status.CHANGED, result.status(), "Level All result: " + result);
			assertTrue(targetOneAssignment.getLevelingDelay() > 0L,
				"Level All must level an eligible assignment on the first project resource");
			assertTrue(targetTwoAssignment.getLevelingDelay() > 0L,
				"Level All must level an eligible assignment on the second project resource");
			assertTrue(targetOneAssignment.getStart() > targetOneAssignmentStart,
				"Level All must move the first eligible assignment start");
			assertTrue(targetTwoAssignment.getStart() > targetTwoAssignmentStart,
				"Level All must move the second eligible assignment start");
			assertEquals(targetOneStart, targetOne.getStart(), "Level All must preserve the first target task start");
			assertEquals(targetTwoStart, targetTwo.getStart(), "Level All must preserve the second target task start");
			assertEquals(0L, targetOne.getLevelingDelay(), "Level All must preserve the first task-wide delay");
			assertEquals(0L, targetTwo.getLevelingDelay(), "Level All must preserve the second task-wide delay");
			assertEquals(0L, anchorOne.getLevelingDelay(), "Level All must retain the first resource's anchor");
			assertEquals(0L, anchorTwo.getLevelingDelay(), "Level All must retain the second resource's anchor");
			assertEquals(List.of(anchorOne.getUniqueId()), result.selectedTaskIds(),
					"Level All must report the preserved task selection");

			Project reloaded = saveAndReload(project);
			NormalTask reloadedTargetOne = taskNamed(reloaded, "Level All resource one target");
			NormalTask reloadedTargetTwo = taskNamed(reloaded, "Level All resource two target");
			assertTrue(((Assignment) reloadedTargetOne.getAssignments().iterator().next()).getLevelingDelay() > 0L,
				"MPO reload lost Level All's first assignment delay");
			assertTrue(((Assignment) reloadedTargetTwo.getAssignments().iterator().next()).getLevelingDelay() > 0L,
				"MPO reload lost Level All's second assignment delay");
			assertEquals(0L, reloadedTargetOne.getLevelingDelay(), "MPO reload must preserve the first task-wide delay");
			assertEquals(0L, reloadedTargetTwo.getLevelingDelay(), "MPO reload must preserve the second task-wide delay");

			fx.undo();
			assertEquals(0L, targetOneAssignment.getLevelingDelay(), "one Undo must clear the first assignment delay");
			assertEquals(0L, targetTwoAssignment.getLevelingDelay(), "one Undo must clear the second assignment delay");
			assertEquals(targetOneAssignmentStart, targetOneAssignment.getStart(),
				"one Undo must restore the first assignment start");
			assertEquals(targetTwoAssignmentStart, targetTwoAssignment.getStart(),
				"one Undo must restore the second assignment start");
			assertEquals(targetOneStart, targetOne.getStart(), "Undo must preserve the first target task start");
			assertEquals(targetTwoStart, targetTwo.getStart(), "Undo must preserve the second target task start");
			fx.redo();
			assertTrue(targetOneAssignment.getLevelingDelay() > 0L, "one Redo must restore the first assignment delay");
			assertTrue(targetTwoAssignment.getLevelingDelay() > 0L, "one Redo must restore the second assignment delay");
			assertTrue(targetOneAssignment.getStart() > targetOneAssignmentStart, "one Redo must restore the first start");
			assertTrue(targetTwoAssignment.getStart() > targetTwoAssignmentStart, "one Redo must restore the second start");
		}
	}

	/** Row of the task in the active spreadsheet projection, or -1 when the table does not project it. */
	private static int rowOf(HeadlessDocumentFixture fx, NormalTask task) throws Exception {
		AtomicReference<Integer> row = new AtomicReference<>(-1);
		HeadlessDocumentFixture.onEdt(() -> {
			SpreadSheet sheet = fx.sheet();
			CommonSpreadSheetModel model = (CommonSpreadSheetModel) sheet.getModel();
			for (int index = 0; index < sheet.getRowCount(); index++) {
				GraphicNode node = model.getNode(index);
				if (node != null && node.getNode() != null && node.getNode().getImpl() == task) {
					row.set(index);
					return;
				}
			}
		});
		return row.get();
	}

	/** Row of the task in the Gantt projection, or -1 when the Gantt does not project it. */
	private static int ganttRowOf(HeadlessDocumentFixture fx, NormalTask task) throws Exception {
		AtomicReference<Integer> row = new AtomicReference<>(-1);
		HeadlessDocumentFixture.onEdt(() -> {
			var cache = fx.frame.getGanttView().getGantt().getModel().getCache();
			for (int index = 0; index < cache.getSize(); index++) {
				Object element = cache.getElementAt(index);
				if (element instanceof GraphicNode graphic && graphic.getNode() != null
						&& graphic.getNode().getImpl() == task) {
					row.set(index);
					return;
				}
			}
		});
		return row.get();
	}

	private static Project saveAndReload(Project project) throws Exception {
		ByteArrayOutputStream saved = new ByteArrayOutputStream();
		MpoFileImporter importer = new MpoFileImporter();
		assertTrue(importer.saveProject(project, saved), "MPO save rejected the command result");
		return importer.loadProject(new ByteArrayInputStream(saved.toByteArray()));
	}

	private static NormalTask taskNamed(Project project, String name) {
		for (Task task : project.getTaskList()) {
			if (name.equals(task.getName()))
				return (NormalTask) task;
		}
		throw new AssertionError("Reloaded task is absent: " + name);
	}
}
