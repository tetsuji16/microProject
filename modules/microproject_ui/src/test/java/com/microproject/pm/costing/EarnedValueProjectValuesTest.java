package com.microproject.pm.costing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

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
 * default 8-hour day has a budget of {@code 60 × 80 × 3_600_000} and its 40 actual hours cost
 * {@code 60 × 40 × 3_600_000}. The status date is the only time input: BCWS covers the part of
 * the baseline that has elapsed, BCWP the part of the work earned by then, ACWP the cost
 * incurred by then.
 */
class EarnedValueProjectValuesTest {
	private static final double RATE_PER_HOUR = 60.0D;
	private static final long HOUR_MILLIS = 3_600_000L;
	private static final double BUDGET = RATE_PER_HOUR * 80L * HOUR_MILLIS;
	private static final double ACTUAL_COST = RATE_PER_HOUR * 40L * HOUR_MILLIS;

	@Test
	void indicatorsFollowTheStatusDateAndSurviveAnMpoRoundTrip() throws Exception {
		Project project = newProject();
		NormalTask task = newTask(project, "EVM known values");
		long day = CalendarOption.getInstance().getMillisPerDay();
		task.setDuration(10L * day);
		Assignment assignment = assign(task, 10L * day);
		project.saveCurrentToSnapshot(Snapshottable.BASELINE, true, null);
		// The work doubles and five of the twenty days are done, so the task is 25% complete.
		assignment.setWork(20L * day, null);
		assignment.setActualWork(5L * day, null);
		project.setStatusDate(task.getStart() + 3L * day);

		assertEquals(0.25D, task.getPercentComplete(), 0.0001D);
		assertEquals(BUDGET, task.getBaselineCost(Snapshottable.BASELINE), 1.0D,
			"the baseline of 80 hours is the budget at completion");
		assertEquals(BUDGET, task.getBac(null), 1.0D);
		assertEquals(ACTUAL_COST, task.getActualCost(null), 1.0D, "40 actual hours cost the full actual cost");

		// Part way through the plan the three values are the elapsed, earned and spent parts of it.
		double bcwsMid = task.getBcws(null);
		double bcwpMid = task.getBcwp(null);
		double acwpMid = task.getAcwp(null);
		assertTrue(bcwsMid > 0.0D && bcwsMid < BUDGET, "BCWS covers only the elapsed baseline: " + bcwsMid);
		assertTrue(bcwpMid > 0.0D && bcwpMid < bcwsMid,
			"BCWP earns half of the scheduled value in this fixture: " + bcwpMid + " against " + bcwsMid);
		assertTrue(acwpMid > 0.0D && acwpMid < ACTUAL_COST,
			"ACWP covers only the cost incurred by the status date: " + acwpMid);
		assertEquals(bcwpMid - acwpMid, task.getCv(null), 1.0D, "CV = BCWP - ACWP");
		assertEquals(bcwpMid - bcwsMid, task.getSv(null), 1.0D, "SV = BCWP - BCWS");
		assertEquals(bcwpMid / acwpMid, task.getCpi(null), 0.0001D, "CPI = BCWP / ACWP");
		assertEquals(bcwpMid / bcwsMid, task.getSpi(null), 0.0001D, "SPI = BCWP / BCWS");

		// A status date at the finish schedules the whole baseline; the earned value is then the
		// completed share of it and the spent value is the full actual cost.
		project.setStatusDate(task.getEnd());
		assertEquals(BUDGET, task.getBcws(null), 1.0D, "the finished baseline is fully scheduled");
		assertEquals(0.25D * BUDGET, task.getBcwp(null), 1.0D, "BCWP is the completed share at the finish");
		assertEquals(ACTUAL_COST, task.getAcwp(null), 1.0D, "all actual cost is spent at the finish");
		assertEquals(task.getBcwp(null) - task.getAcwp(null), task.getCv(null), 1.0D);
		assertEquals(task.getBcwp(null) - task.getBcws(null), task.getSv(null), 1.0D);
		assertEquals(task.getBcwp(null) / task.getAcwp(null), task.getCpi(null), 0.0001D);
		assertEquals(task.getBcwp(null) / task.getBcws(null), task.getSpi(null), 0.0001D);

		// The indicators are derived state, so an MPO round trip must reproduce them exactly.
		project.setStatusDate(task.getStart() + 3L * day);
		Project reloaded = saveAndReload(project);
		NormalTask reloadedTask = taskNamed(reloaded, "EVM known values");
		assertEquals(project.getStatusDate(), reloaded.getStatusDate(), "the status date survives the round trip");
		assertEquals(BUDGET, reloadedTask.getBac(null), 1.0D);
		assertEquals(ACTUAL_COST, reloadedTask.getActualCost(null), 1.0D);
		assertEquals(bcwsMid, reloadedTask.getBcws(null), 1.0D);
		assertEquals(bcwpMid, reloadedTask.getBcwp(null), 1.0D);
		assertEquals(acwpMid, reloadedTask.getAcwp(null), 1.0D);
		assertEquals(task.getCv(null), reloadedTask.getCv(null), 1.0D);
		assertEquals(task.getSv(null), reloadedTask.getSv(null), 1.0D);
		assertEquals(task.getCpi(null), reloadedTask.getCpi(null), 0.0001D);
		assertEquals(task.getSpi(null), reloadedTask.getSpi(null), 0.0001D);
	}

	@Test
	void aPlanWithoutABaselineReportsNoEarnedValue() throws Exception {
		Project project = newProject();
		NormalTask task = newTask(project, "EVM without baseline");
		assign(task, 5L * CalendarOption.getInstance().getMillisPerDay());

		assertEquals(0.0D, task.getBac(null), 0.0001D, "a task without a baseline has no budget at completion");
		assertEquals(0.0D, task.getBcws(null), 0.0001D);
		assertEquals(0.0D, task.getBcwp(null), 0.0001D);
		assertEquals(0.0D, task.getSpi(null), 0.0001D, "a plan without a baseline has no schedule index");
	}

	private static Project newProject() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("evm-values", undo), undo);
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
