/* MIT License — Copyright (c) 2026 microProject */
package com.microproject.ui.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import com.microproject.datatype.Rate;
import com.microproject.datatype.TimeUnit;
import com.microproject.exchange.MpoFileImporter;
import com.microproject.options.CalendarOption;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.AssignmentService;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.snapshot.Snapshottable;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.undo.DataFactoryUndoController;

/**
 * Verifies the earned value indicators against a plan whose budget can be calculated by hand,
 * and fixes how they follow the status date and survive an MPO round trip (#943).
 *
 * <p>Costs are stored as {@code rate per hour × work in milliseconds}, so a 10-day task at the
 * default 8-hour day has a budget of {@code 60 × 80 × 3_600_000}. The project start is a fixed
 * Monday rather than the wall clock, so the elapsed part of the baseline is a constant of the
 * fixture and every value below is asserted against its hand calculation.
 */
class EarnedValueProjectValuesTest {
	private static final long HOUR_MILLIS = 3_600_000L;
	private static final long CALENDAR_DAY = 24L * HOUR_MILLIS;
	private static final double RATE_PER_HOUR = 60.0D;
	private static final long DAY = CalendarOption.getInstance().getMillisPerDay();
	/** 10 days of work at the default 8-hour day. */
	private static final long PLANNED_HOURS = 10L * DAY / HOUR_MILLIS;
	/** 40 hours reported as actual work: five of the ten planned days. */
	private static final long ACTUAL_HOURS = PLANNED_HOURS / 2;
	/** Three working days have elapsed at the status date: Monday, Tuesday and Wednesday. */
	private static final long ELAPSED_HOURS = 3L * DAY / HOUR_MILLIS;
	/**
	 * Monday 2026-11-16 08:00 JST. A fixed week keeps the fixture independent of the run date;
	 * {@link Project#initialize} would otherwise derive the schedule from the current clock.
	 */
	private static final long START = LocalDateTime.of(2026, 11, 16, 8, 0)
		.atZone(ZoneId.of("Asia/Tokyo")).toInstant().toEpochMilli();

	@Test
	void indicatorsFollowTheStatusDateAndSurviveAnMpoRoundTrip() throws Exception {
		Project project = newProject();
		NormalTask task = newTask(project, "EVM known values");
		task.setDuration(10L * DAY);
		Assignment assignment = assign(task, 10L * DAY);
		project.saveCurrentToSnapshot(Snapshottable.BASELINE, true, null);
		// The work doubles and five of the twenty days are done, so the task is 25% complete.
		assignment.setWork(20L * DAY, null);
		assignment.setActualWork(5L * DAY, null);

		assertEquals(START, task.getStart(), "the fixture starts on the pinned Monday");
		assertEquals(0.25D, task.getPercentComplete(), 1.0e-9D);
		assertCost(cost(PLANNED_HOURS), task.getBaselineCost(Snapshottable.BASELINE),
			"the baseline of 80 hours is the budget at completion");
		assertCost(cost(PLANNED_HOURS), task.getBac(null), "BAC is the baseline cost");
		assertCost(cost(ACTUAL_HOURS), task.getActualCost(null), "40 actual hours cost the full actual cost");

		// Three working days in, the plan has spent three days of the baseline, the resource has
		// reported three days of work, and the efficiency (80 h baseline ÷ 160 h work) earns half.
		project.setStatusDate(task.getStart() + 3L * CALENDAR_DAY);
		double bcwsMid = task.getBcws(null);
		double bcwpMid = task.getBcwp(null);
		double acwpMid = task.getAcwp(null);
		assertCost(cost(ELAPSED_HOURS), bcwsMid, "BCWS is the three elapsed days of the baseline");
		assertCost(cost(ELAPSED_HOURS / 2), bcwpMid, "BCWP is the earned half of those three days");
		assertCost(cost(ELAPSED_HOURS), acwpMid, "ACWP is the cost reported through the status date");
		assertCost(-cost(ELAPSED_HOURS / 2), task.getCv(null), "CV = BCWP − ACWP");
		assertCost(-cost(ELAPSED_HOURS / 2), task.getSv(null), "SV = BCWP − BCWS");
		assertEquals(0.5D, task.getCpi(null), 1.0e-9D, "CPI = BCWP ÷ ACWP");
		assertEquals(0.5D, task.getSpi(null), 1.0e-9D, "SPI = BCWP ÷ BCWS");

		// A status date at the finish schedules the whole baseline; the earned value is then the
		// completed share of it and the spent value is the full actual cost.
		project.setStatusDate(task.getEnd());
		assertCost(cost(PLANNED_HOURS), task.getBcws(null), "the finished baseline is fully scheduled");
		assertCost(cost(PLANNED_HOURS / 4), task.getBcwp(null), "BCWP is the completed quarter of BAC");
		assertCost(cost(ACTUAL_HOURS), task.getAcwp(null), "all actual cost is spent at the finish");
		assertCost(cost(PLANNED_HOURS / 4) - cost(ACTUAL_HOURS), task.getCv(null), "CV = BCWP − ACWP");
		assertCost(cost(PLANNED_HOURS / 4) - cost(PLANNED_HOURS), task.getSv(null), "SV = BCWP − BCWS");
		assertEquals(0.5D, task.getCpi(null), 1.0e-9D);
		assertEquals(0.25D, task.getSpi(null), 1.0e-9D, "the work doubled but the baseline did not");

		// The indicators are derived state, so an MPO round trip must reproduce them exactly.
		project.setStatusDate(task.getStart() + 3L * CALENDAR_DAY);
		Project reloaded = saveAndReload(project);
		NormalTask reloadedTask = taskNamed(reloaded, "EVM known values");
		assertEquals(project.getStatusDate(), reloaded.getStatusDate(), "the status date survives the round trip");
		assertCost(cost(PLANNED_HOURS), reloadedTask.getBac(null), "BAC");
		assertCost(cost(ACTUAL_HOURS), reloadedTask.getActualCost(null), "actual cost");
		assertCost(bcwsMid, reloadedTask.getBcws(null), "BCWS");
		assertCost(bcwpMid, reloadedTask.getBcwp(null), "BCWP");
		assertCost(acwpMid, reloadedTask.getAcwp(null), "ACWP");
		assertCost(task.getCv(null), reloadedTask.getCv(null), "CV");
		assertCost(task.getSv(null), reloadedTask.getSv(null), "SV");
		assertEquals(task.getCpi(null), reloadedTask.getCpi(null), 1.0e-9D, "CPI");
		assertEquals(task.getSpi(null), reloadedTask.getSpi(null), 1.0e-9D, "SPI");

		// Moving the status date after the reload proves the values come from the saved inputs
		// rather than from a copy persisted with the file.
		reloaded.setStatusDate(reloadedTask.getEnd());
		assertCost(cost(PLANNED_HOURS), reloadedTask.getBcws(null), "the reloaded baseline is fully scheduled");
		assertCost(cost(PLANNED_HOURS / 4), reloadedTask.getBcwp(null), "the reloaded task earned a quarter");
		assertCost(cost(ACTUAL_HOURS), reloadedTask.getAcwp(null), "the reloaded actual cost is unchanged");
		assertEquals(0.25D, reloadedTask.getSpi(null), 1.0e-9D);
	}

	@Test
	void aPlanWithoutABaselineReportsNoEarnedValue() {
		Project project = newProject();
		NormalTask task = newTask(project, "EVM without baseline");
		task.setDuration(10L * DAY);
		Assignment assignment = assign(task, 10L * DAY);
		assignment.setActualWork(5L * DAY, null);
		// A status date is set so the zeros below cannot come from a missing status date.
		project.setStatusDate(task.getStart() + 3L * CALENDAR_DAY);

		assertCost(0.0D, task.getBac(null), "a task without a baseline has no budget at completion");
		assertCost(0.0D, task.getBcws(null), "nothing was scheduled without a baseline");
		assertCost(0.0D, task.getBcwp(null), "nothing can be earned without a baseline");
		// Actual cost does not need a baseline: the three days of reported work still cost money,
		// so CV is negative and SV is zero rather than both being zero.
		assertCost(cost(ELAPSED_HOURS), task.getAcwp(null), "ACWP reports the cost of the work performed");
		assertCost(-cost(ELAPSED_HOURS), task.getCv(null), "CV = BCWP − ACWP with no earned value");
		assertCost(0.0D, task.getSv(null), "SV = BCWP − BCWS with neither side earned");
		assertEquals(0.0D, task.getCpi(null), 1.0e-9D, "CPI is BCWP ÷ ACWP, so it is zero without earned value");
		assertEquals(0.0D, task.getSpi(null), 1.0e-9D, "SPI divides by a zero BCWS and reports the policy value");
	}

	/** Asserts a cost in internal units, where the stored value is a rate × work product. */
	private static void assertCost(double expected, double actual, String message) {
		assertEquals(expected, actual, Math.max(Math.abs(expected), 1.0D) * 1.0e-9D, message);
	}

	private static double cost(long hours) {
		return RATE_PER_HOUR * hours * HOUR_MILLIS;
	}

	private static Project newProject() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("evm-values", undo), undo);
		// Pin the start before initializing: with a start already set, initialize() does not
		// schedule a background recalculation from the current clock.
		project.setStart(START);
		project.initialize(false, false);
		return project;
	}

	private static NormalTask newTask(Project project, String name) {
		NormalTask task = project.createScriptedTask();
		task.setName(name);
		project.connectTask(task);
		return task;
	}

	private static Assignment assign(NormalTask task, long work) {
		ResourceImpl resource = task.getProject().getResourcePool().newResourceInstance();
		resource.setStandardRate(new Rate(RATE_PER_HOUR, TimeUnit.HOURS));
		Assignment assignment = AssignmentService.getInstance().newAssignment(task, resource, 1.0D, 0L, null);
		assignment.setWork(work, null);
		return assignment;
	}

	private static Project saveAndReload(Project project) throws Exception {
		ByteArrayOutputStream saved = new ByteArrayOutputStream();
		MpoFileImporter importer = new MpoFileImporter();
		assertTrue(importer.saveProject(project, saved), "MPO save rejected the earned value fixture");
		return importer.loadProject(new ByteArrayInputStream(saved.toByteArray()));
	}

	private static NormalTask taskNamed(Project project, String name) {
		for (Task task : project.getTaskList()) {
			if (name.equals(task.getName())) {
				return (NormalTask) task;
			}
		}
		throw new AssertionError("Reloaded task is absent: " + name);
	}
}
