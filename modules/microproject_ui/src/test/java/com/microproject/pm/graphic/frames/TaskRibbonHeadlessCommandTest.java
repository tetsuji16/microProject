/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.frames;

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

import com.microproject.exchange.MpoFileImporter;
import com.microproject.options.CalendarOption;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheetModel;
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
			task.setDuration(CalendarOption.getInstance().getMillisPerDay());
			task.setPercentComplete(0.4D);
			fx.project.recalculate();
			int rowsBefore = fx.sheet().getRowCount();
			assertFalse(task.isInactiveTask(), "the fixture task must start active");

			fx.select(task);
			RibbonCommandResult result = fx.run(CommandId.INACTIVATE_TASK);
			assertEquals(RibbonCommandResult.Status.CHANGED, result.status(), "Inactivate result: " + result);
			assertEquals(List.of(task.getUniqueId()), result.affectedTaskIds(), "Inactivate affected task IDs");
			assertTrue(task.isInactiveTask(), "Inactivate must clear the selected task's Active field");
			assertTrue(fx.project.getTaskList().contains(task), "Inactivate must keep the task record");
			assertEquals(rowsBefore, fx.sheet().getRowCount(), "Inactivate must keep the task table row");
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
