/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.exchange.MpoFileImporter;
import com.microproject.options.CalendarOption;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.ribbon.CommandId;
import com.microproject.ribbon.RibbonCommandResult;

/**
 * Command behavior for the Task ribbon commands whose only contract is the
 * model mutation, one Undo edit, Redo and MPO persistence (issue #1034). The
 * physical ribbon click, popup, menu and shortcut routes are covered once per
 * input mechanism by the {@code @GuiJourney} tests; these cases run headless on
 * a real {@link DocumentFrame} through the canonical command route.
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
