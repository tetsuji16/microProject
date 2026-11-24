/*******************************************************************************
 * MIT License
 *
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
package com.microproject.model.task;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import com.microproject.model.resource.ResourcePool;
import com.microproject.model.dependency.Dependency;
import com.microproject.model.dependency.DependencyService;
import com.microproject.model.dependency.DependencyType;
import com.microproject.schedule.ConstraintType;
import com.microproject.undo.DataFactoryUndoController;

class ScheduleDiagnosticsServiceTest {
	@Test
	void reportsManualInactiveAndConstraintCauses() throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("diagnostic-test", undo), undo);
		project.initialize(false, false);
		NormalTask task = new NormalTask(project);
		task.setName("Inspect me");
		project.connectTask(task);
		project.getSchedulingAlgorithm().addObject(task);
		task.setManualDates(project.getStart(), project.getStart() + 1L);
		task.setInactiveTask(true);
		task.setConstraintType(ConstraintType.MSO);

		var types = new ScheduleDiagnosticsService().diagnose(task).stream()
			.map(ScheduleDiagnosticsService.Issue::type).toList();

		assertTrue(types.contains(ScheduleDiagnosticsService.Type.MANUAL));
		assertTrue(types.contains(ScheduleDiagnosticsService.Type.INACTIVE));
	}

	@Test
	void reportsFinishToStartConflictAndClearsItInAutomaticMode() throws Exception {
		Project project = project();
		NormalTask predecessor = task(project, "Predecessor");
		NormalTask successor = task(project, "Successor");
		long day = com.microproject.options.CalendarOption.getInstance().getMillisPerDay();
		predecessor.setDuration(2L * day);
		Dependency dependency = DependencyService.getInstance().newDependency(
				predecessor, successor, DependencyType.FS, 0L, this);

		successor.setManualDates(predecessor.getStart(), predecessor.getEnd());
		assertTrue(ScheduleDiagnosticsService.hasDependencyConflict(successor));
		assertTrue(new ScheduleDiagnosticsService().diagnose(successor).stream()
				.anyMatch(issue -> issue.type() == ScheduleDiagnosticsService.Type.DEPENDENCY_CONFLICT));

		successor.setManuallyScheduled(false);
		project.recalculate();
		assertFalse(ScheduleDiagnosticsService.hasDependencyConflict(successor));
		assertTrue(successor.getStart() >= predecessor.getEnd());

		dependency.setDisabled(true);
		assertFalse(ScheduleDiagnosticsService.hasDependencyConflict(successor));
	}

	@Test
	void detectsStartAndFinishLinkConflictsIncludingLag() throws Exception {
		assertConflict(DependencyType.SS);
		assertConflict(DependencyType.FF);
		assertConflict(DependencyType.SF);
	}

	@Test
	void acceptsAZeroLagSuccessorAtTheExactPredecessorFinish() throws Exception {
		Project project = project();
		NormalTask predecessor = task(project, "Predecessor");
		NormalTask successor = task(project, "Successor");
		long day = com.microproject.options.CalendarOption.getInstance().getMillisPerDay();
		predecessor.setManualDates(project.getStart(), project.getWorkCalendar().add(project.getStart(), day, false));
		successor.setManualDates(predecessor.getEnd(), project.getWorkCalendar().add(predecessor.getEnd(), day, false));
		DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.FS, 0L, this);

		assertFalse(ScheduleDiagnosticsService.hasDependencyConflict(successor));
	}

	@Test
	void warningRequiresManualActiveNonSummaryTaskWithLinkOrConstraintConflict() throws Exception {
		Project project = project();
		NormalTask predecessor = task(project, "Predecessor");
		NormalTask successor = task(project, "Successor");
		long day = com.microproject.options.CalendarOption.getInstance().getMillisPerDay();
		predecessor.setDuration(2L * day);
		Dependency dependency = DependencyService.getInstance().newDependency(
				predecessor, successor, DependencyType.FS, 0L, this);
		successor.setManualDates(predecessor.getStart(), predecessor.getEnd());

		assertTrue(ScheduleDiagnosticsService.hasWarning(successor));
		assertTrue(ScheduleDiagnosticsService.showsWarning(successor));
		assertTrue(successor.isWarning());
		successor.setIgnoreWarnings(true);
		assertTrue(ScheduleDiagnosticsService.hasWarning(successor));
		assertFalse(ScheduleDiagnosticsService.showsWarning(successor));
		successor.setIgnoreWarnings(false);

		dependency.setDisabled(true);
		assertFalse(ScheduleDiagnosticsService.hasWarning(successor), "disabled link");
		dependency.setDisabled(false);
		successor.setInactiveTask(true);
		assertFalse(ScheduleDiagnosticsService.hasWarning(successor), "inactive task");
		successor.setInactiveTask(false);
		successor.setManuallyScheduled(false);
		assertFalse(ScheduleDiagnosticsService.hasWarning(successor), "auto scheduled");
		assertFalse(ScheduleDiagnosticsService.hasWarning(predecessor), "auto scheduled predecessor");
	}

	@Test
	void warningDetectsConstraintViolationsOnManualTasks() throws Exception {
		Project project = project();
		NormalTask task = task(project, "Constrained");
		long day = com.microproject.options.CalendarOption.getInstance().getMillisPerDay();
		long start = project.getWorkCalendar().add(project.getStart(), 2L * day, false);
		long finish = project.getWorkCalendar().add(start, day, false);
		task.setManualDates(start, finish);
		task.setConstraintType(ConstraintType.SNET);
		task.setConstraintDate(project.getWorkCalendar().add(start, day, false));
		assertTrue(ScheduleDiagnosticsService.hasWarning(task), "SNET after start");
		task.setConstraintDate(start);
		assertFalse(ScheduleDiagnosticsService.hasWarning(task), "SNET satisfied");
		task.setConstraintType(ConstraintType.MSO);
		task.setConstraintDate(project.getWorkCalendar().add(start, day, false));
		assertTrue(ScheduleDiagnosticsService.hasWarning(task), "MSO after start");
		task.setConstraintType(ConstraintType.FNLT);
		task.setConstraintDate(start);
		assertTrue(ScheduleDiagnosticsService.hasWarning(task), "FNLT before finish");
		task.setConstraintType(ConstraintType.MFO);
		assertTrue(ScheduleDiagnosticsService.hasWarning(task), "MFO before finish");
		task.setConstraintDate(finish);
		assertFalse(ScheduleDiagnosticsService.hasWarning(task), "MFO satisfied");
		task.setConstraintType(ConstraintType.ASAP);
		assertFalse(ScheduleDiagnosticsService.hasWarning(task), "no constraint");
	}

	@Test
	void suggestionFlagsManualTaskThatCouldStartEarlier() throws Exception {
		Project project = project();
		NormalTask predecessor = task(project, "Predecessor");
		NormalTask successor = task(project, "Successor");
		long day = com.microproject.options.CalendarOption.getInstance().getMillisPerDay();
		predecessor.setManualDates(project.getStart(), project.getWorkCalendar().add(project.getStart(), day, false));
		long late = project.getWorkCalendar().add(predecessor.getEnd(), 3L * day, false);
		successor.setManualDates(late, project.getWorkCalendar().add(late, day, false));
		Dependency dependency = DependencyService.getInstance().newDependency(
				predecessor, successor, DependencyType.FS, 0L, this);

		assertFalse(ScheduleDiagnosticsService.hasWarning(successor));
		assertTrue(ScheduleDiagnosticsService.hasSuggestion(successor));
		assertTrue(ScheduleDiagnosticsService.showsSuggestion(successor));
		assertFalse(ScheduleDiagnosticsService.hasSuggestion(predecessor), "no predecessor links");
		successor.setIgnoreWarnings(true);
		assertTrue(ScheduleDiagnosticsService.hasSuggestion(successor));
		assertFalse(ScheduleDiagnosticsService.showsSuggestion(successor));
		successor.setIgnoreWarnings(false);

		dependency.setDisabled(true);
		assertFalse(ScheduleDiagnosticsService.hasSuggestion(successor), "disabled link");
		dependency.setDisabled(false);
		successor.setManuallyScheduled(false);
		assertFalse(ScheduleDiagnosticsService.hasSuggestion(successor), "auto scheduled");
		successor.setManuallyScheduled(true);
		successor.setManualDates(predecessor.getEnd(), project.getWorkCalendar().add(predecessor.getEnd(), day, false));
		assertFalse(ScheduleDiagnosticsService.hasSuggestion(successor), "starts exactly at the boundary");
		successor.setManualDates(predecessor.getStart(), predecessor.getEnd());
		assertFalse(ScheduleDiagnosticsService.hasSuggestion(successor), "warning suppresses suggestion");
	}

	private void assertConflict(int type) throws Exception {
		Project project = project();
		NormalTask predecessor = task(project, "Predecessor");
		NormalTask successor = task(project, "Successor");
		long day = com.microproject.options.CalendarOption.getInstance().getMillisPerDay();
		long predecessorStart = project.getWorkCalendar().add(project.getStart(), 2L * day, false);
		long predecessorFinish = project.getWorkCalendar().add(predecessorStart, 2L * day, false);
		long successorStart = project.getStart();
		long successorFinish = project.getWorkCalendar().add(successorStart, day, false);
		predecessor.setManualDates(predecessorStart, predecessorFinish);
		successor.setManualDates(successorStart, successorFinish);
		DependencyService.getInstance().newDependency(predecessor, successor, type, day, this);

		assertTrue(ScheduleDiagnosticsService.hasDependencyConflict(successor), "type=" + type + " successor="
			+ successor.getStart() + "/" + successor.getEnd() + " required="
			+ ScheduleDiagnosticsService.requiredBoundary((Dependency) successor.getPredecessorList().get(0)));
	}

	private Project project() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("diagnostic-test", undo), undo);
		project.initialize(false, false);
		return project;
	}

	private NormalTask task(Project project, String name) {
		NormalTask task = new NormalTask(project);
		task.setName(name);
		project.connectTask(task);
		project.getSchedulingAlgorithm().addObject(task);
		task.getCurrentSchedule().setStart(project.getStart());
		task.setDuration(com.microproject.options.CalendarOption.getInstance().getMillisPerDay());
		return task;
	}
}
