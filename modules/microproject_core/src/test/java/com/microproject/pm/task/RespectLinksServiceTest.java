/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.task;

import static org.junit.jupiter.api.Assertions.*;

import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.assignment.AssignmentService;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.scheduling.ConstraintType;
import com.microproject.datatype.Duration;
import com.microproject.options.CalendarOption;
import com.microproject.util.DateTime;
import com.microproject.undo.DataFactoryUndoController;
import java.util.List;
import java.util.Calendar;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class RespectLinksServiceTest {
	@Test
	void afterSnapshotFailureRollsBackBeforeEventsOrUndoArePublished() throws Exception {
		Project project = project();
		NormalTask predecessor = task(project, "Predecessor", 2);
		NormalTask successor = task(project, "Successor", 1);
		long early = project.getWorkCalendar().adjustInsideCalendar(project.getStart(), false);
		successor.setManualDates(early, project.getWorkCalendar().add(early, day(), true));
		DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.FS, 0L, this);
		long originalStart = successor.getStart();
		long originalFinish = successor.getEnd();
		project.setDirty(false);
		successor.setDirty(false);
		project.getUndoController().clear();
		AtomicInteger schedules = new AtomicInteger();
		AtomicInteger objects = new AtomicInteger();
		project.addScheduleListener(event -> schedules.incrementAndGet());
		project.addObjectListener(event -> objects.incrementAndGet());
		AtomicInteger captures = new AtomicInteger();
		RespectLinksService service = new RespectLinksService(p -> {
			if (captures.incrementAndGet() == 2) throw new IllegalStateException("injected after-snapshot failure");
			return ProjectTaskSnapshot.capture(p);
		});

		assertThrows(IllegalStateException.class, () -> service.apply(project, List.of(successor)));

		assertEquals(originalStart, successor.getStart());
		assertEquals(originalFinish, successor.getEnd());
		assertTrue(successor.isManuallyScheduled());
		assertFalse(project.isDirty());
		assertFalse(successor.isDirty());
		assertFalse(project.getUndoController().canUndo());
		assertEquals(0, schedules.get());
		assertEquals(0, objects.get());
	}

	@Test
	void automaticConflictingTaskIsRecalculatedAndModePreserved() throws Exception {
		Project project = project();
		NormalTask predecessor = task(project, "Predecessor", 2);
		NormalTask successor = task(project, "Automatic successor", 1);
		long early = project.getWorkCalendar().adjustInsideCalendar(project.getStart(), false);
		long late = project.getWorkCalendar().add(early, 8L * day(), false);
		predecessor.setManualDates(late, project.getWorkCalendar().add(late, 2L * day(), true));
		DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.FS, 0L, this);
		successor.setManuallyScheduled(false);
		project.recalculate();
		assertFalse(ScheduleDiagnosticsService.hasCurrentDependencyConflict(successor));
		assertFalse(new RespectLinksService().apply(project, List.of(successor)).change().hasChanged(),
			"an already-consistent automatic task is a no-op");
		// Deliberately stale the current dates after recalculation; the command must
		// apply to both scheduling modes per TaskRespectLinks documentation.
		successor.getCurrentSchedule().setStart(early);
		successor.getCurrentSchedule().setEnd(project.getWorkCalendar().add(early, day(), true));
		assertTrue(ScheduleDiagnosticsService.hasCurrentDependencyConflict(successor));
		long staleStart = successor.getStart();

		RespectLinksService.Result result = new RespectLinksService().apply(project, List.of(successor));

		assertEquals(1, result.movedCount());
		assertFalse(successor.isManuallyScheduled());
		assertFalse(ScheduleDiagnosticsService.hasCurrentDependencyConflict(successor));
		long repairedStart = successor.getStart();
		assertTrue(repairedStart > staleStart);
		result.change().undo();
		assertFalse(successor.isManuallyScheduled());
		result.change().redo();
		assertEquals(repairedStart, successor.getStart());
		assertFalse(successor.isManuallyScheduled());
	}

	@Test
	void snapshotTaskMapIsImmutable() throws Exception {
		Project project = project();
		NormalTask task = task(project, "Task", 1);
		ProjectTaskSnapshot snapshot = ProjectTaskSnapshot.capture(project);
		assertThrows(UnsupportedOperationException.class,
			() -> snapshot.taskStates().put(task, snapshot.taskStates().get(task)));
	}
	@Test
	void automaticFinishLinkWithZeroLagIncludesExactFinishBoundary() throws Exception {
		Project project = project();
		NormalTask predecessor = task(project, "Predecessor", 3);
		NormalTask successor = task(project, "Successor", 1);
		long day = day();
		long base = project.getWorkCalendar().adjustInsideCalendar(project.getStart(), false);
		long predStart = project.getWorkCalendar().add(base, 4L * day, false);
		predecessor.setManualDates(predStart, project.getWorkCalendar().add(predStart, 3L * day, false));
		successor.setManualDates(base, project.getWorkCalendar().add(base, day, true));
		var dependency = DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.Kind.FF, 0L, this);
		successor.setManuallyScheduled(false);
		project.recalculate();
		assertTrue(successor.getEnd() >= predecessor.getEnd(), "finish=" + successor.getEnd() + " pred=" + predecessor.getEnd()
			+ " depDate=" + dependency.getDate(true) + " dur=" + successor.getDurationMillis());
	}

	@Test
	void automaticFinishLinkScheduleIncludesLag() throws Exception {
		Project project = project();
		NormalTask predecessor = task(project, "Predecessor", 3);
		NormalTask successor = task(project, "Successor", 1);
		long day = day();
		long base = project.getWorkCalendar().adjustInsideCalendar(project.getStart(), false);
		long start = project.getWorkCalendar().add(base, 4L * day, false);
		long finish = finish(project, start, 3L * day);
		predecessor.setManualDates(start, finish);
		successor.setManualDates(base, project.getWorkCalendar().add(base, day, false));
		DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.Kind.FF, day, this);
		long required = ((com.microproject.pm.dependency.Dependency) successor.getPredecessorList().iterator().next())
			.getEffectiveWorkCalendar().add(predecessor.getEnd(), day, true);
		successor.setManuallyScheduled(false);
		project.recalculate();
		assertTrue(successor.getEnd() >= required, "manual=" + successor.isManuallyScheduled() + " start=" + successor.getStart()
			+ " end=" + successor.getEnd() + " early=" + successor.getEarlyStart() + "/" + successor.getEarlyFinish()
			+ " dep=" + successor.getPredecessorList().iterator().next() + " depDate="
			+ ((com.microproject.pm.dependency.Dependency) successor.getPredecessorList().iterator().next()).getDate(true)
			+ " predecessor=" + predecessor.isManuallyScheduled() + "/" + predecessor.getStart() + "/" + predecessor.getEnd()
			+ " required=" + required);
	}

	@Test
	void elapsedLagFromMondayEveningResolvesToTuesdayMorningThroughSchedule() throws Exception {
		Project project = project();
		NormalTask predecessor = task(project, "Predecessor", 1);
		NormalTask successor = task(project, "Successor", 1);
		Calendar mondayAtFive = DateTime.calendarInstance();
		mondayAtFive.clear();
		mondayAtFive.set(2026, Calendar.NOVEMBER, 2, 17, 0, 0);
		long predecessorFinish = mondayAtFive.getTimeInMillis();
		Calendar tuesdayAtEight = DateTime.calendarInstance();
		tuesdayAtEight.clear();
		tuesdayAtEight.set(2026, Calendar.NOVEMBER, 3, 8, 0, 0);
		long expected = tuesdayAtEight.getTimeInMillis();
		predecessor.setManualDates(predecessorFinish - day(), predecessorFinish);
		successor.setManualDates(project.getStart(), project.getWorkCalendar().add(project.getStart(), day(), true));
		long elapsedFourHours = Duration.setAsElapsed(4L * 60L * 60L * 1000L);
		var dependency = DependencyService.getInstance().newDependency(predecessor, successor,
			DependencyType.Kind.FS, elapsedFourHours, this);

		long elapsedTarget = predecessorFinish + 4L * 60L * 60L * 1000L;
		assertEquals(elapsedTarget, dependency.getEffectiveWorkCalendar().add(predecessorFinish, elapsedFourHours, false),
			"elapsed addition preserves wall-clock Monday 21:00");
		assertEquals(expected, dependency.getEffectiveWorkCalendar().adjustInsideCalendar(elapsedTarget, false),
			"successor start normalization moves Monday 21:00 to Tuesday 08:00");
		assertEquals(elapsedTarget, dependency.calcForwardDependencyDate(predecessor.getStart(), predecessorFinish, false));
		successor.setManuallyScheduled(false);
		project.recalculate();
		assertTrue(successor.getStart() >= expected,
			"current schedule start=" + successor.getStart() + " required=" + expected);
	}

	@Test
	void elapsedTaskDurationKeepsElapsedDependencyStartAtMondayNinePm() throws Exception {
		Project project = project();
		NormalTask predecessor = task(project, "Predecessor", 1);
		NormalTask successor = task(project, "Elapsed successor", 1);
		Calendar mondayAtFive = DateTime.calendarInstance();
		mondayAtFive.clear();
		mondayAtFive.set(2026, Calendar.NOVEMBER, 2, 17, 0, 0);
		long predecessorFinish = mondayAtFive.getTimeInMillis();
		long elapsedTarget = predecessorFinish + 4L * 60L * 60L * 1000L;
		predecessor.setManualDates(predecessorFinish - day(), predecessorFinish);
		successor.setManualDates(project.getStart(), project.getWorkCalendar().add(project.getStart(), day(), true));
		successor.setRawDuration(Duration.setAsElapsed(8L * 60L * 60L * 1000L));
		DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.Kind.FS,
			Duration.setAsElapsed(4L * 60L * 60L * 1000L), this);
		successor.setManuallyScheduled(false);
		project.recalculate();
		assertEquals(elapsedTarget, successor.getStart(), "elapsed-duration task retains Monday 21:00 start");
	}
	@Test
	void respectsAllIncomingLinkKindsAndLagThenUndoRedoPreservesManualMode() throws Exception {
		// Document-derived rule: Microsoft defines FS/SS as a minimum successor-start
		// boundary and FF/SF as a minimum successor-finish boundary. In particular,
		// SF cannot finish until its predecessor starts and may finish later. Elapsed
		// lag is checked as wall-clock milliseconds; working lag uses the effective
		// work calendar. Source: https://support.microsoft.com/en-us/project/link-tasks-in-a-project
		// and https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task
		long day = day();
		long hour = 60L * 60L * 1000L;
		for (boolean assigned : List.of(false, true)) {
			for (DependencyType.Kind kind : DependencyType.Kind.values()) {
				for (long lag : List.of(0L, day, -day, Duration.setAsElapsed(4L * hour))) {
					Project project = project();
					NormalTask predecessor = task(project, "Predecessor", 3);
					NormalTask successor = task(project, "Successor", 1);
					if (assigned) {
						var resource = project.getResourcePool().newResourceInstance();
						AssignmentService.getInstance().newAssignment(successor, resource, 1.0d, 0L, this);
					}
					long base = project.getWorkCalendar().adjustInsideCalendar(project.getStart(), false);
					long start = project.getWorkCalendar().add(base, 4L * day, false);
					long finish = finish(project, start, 3L * day);
					predecessor.setManualDates(start, finish);
					long oldStart = base;
					long oldFinish = project.getWorkCalendar().add(oldStart, day, true);
					successor.setManualDates(oldStart, oldFinish);
					var dependency = DependencyService.getInstance().newDependency(predecessor, successor, kind, lag, this);

					long predecessorAnchor = switch (kind) {
						case FS, FF -> predecessor.getEnd();
						case SS, SF -> predecessor.getStart();
					};
					long required = Duration.millis(lag) == 0L ? predecessorAnchor
						: Duration.isElapsed(lag) ? predecessorAnchor + Duration.millis(lag)
						: dependency.getEffectiveWorkCalendar().add(predecessorAnchor, lag, false);
					if (Duration.isElapsed(lag)) {
						assertEquals(4L * hour, Duration.millis(lag));
						assertEquals(lag, dependency.getLeadValue());
					}
					assertFalse(dependency.isDisabled());
					long converted = dependency.calcForwardDependencyDate(predecessor.getStart(), predecessor.getEnd(), true);
					if (kind == DependencyType.Kind.FF || kind == DependencyType.Kind.SF) {
						long candidateFinish = successor.calcOffsetFrom(converted, converted, true, true, true);
						assertTrue(candidateFinish >= required, kind + " lag=" + lag + " converted=" + converted
							+ " candidateFinish=" + candidateFinish + " required=" + required);
					} else {
						assertEquals(required, converted, kind + " lag=" + lag + " Dependency.calcForwardDependencyDate");
					}
					RespectLinksService.Result result = new RespectLinksService().apply(project, List.of(successor));

					assertEquals(1, result.movedCount(), kind + " lag=" + lag + " assigned=" + assigned);
					assertTrue(successor.isManuallyScheduled(), kind.toString());
					assertFalse(ScheduleDiagnosticsService.hasDependencyConflict(successor), kind + " lag=" + lag);
					long related = switch (kind) {
						case FS, SS -> successor.getStart();
						case FF, SF -> successor.getEnd();
					};
					assertTrue(related >= required, kind + " lag=" + lag + " required=" + required + " actual=" + related
						+ " anchor=" + predecessorAnchor + " pred=" + predecessor.getStart() + "/" + predecessor.getEnd()
						+ " depDate=" + dependency.getDate(true) + " early=" + successor.getEarlyStart() + "/" + successor.getEarlyFinish());
					long movedStart = successor.getStart();
					result.change().undo();
					assertEquals(oldStart, successor.getStart(), kind + " lag=" + lag + " undo");
					assertTrue(successor.isManuallyScheduled());
					result.change().redo();
					assertEquals(movedStart, successor.getStart(), kind + " lag=" + lag + " redo");
					assertTrue(successor.isManuallyScheduled());
			}
		}
		}
	}

	@Test
	void actualProgressConflictRejectsAtomicallyAndPreservesActuals() throws Exception {
		Project project = project();
		NormalTask predecessor = task(project, "Predecessor", 2);
		NormalTask successor = task(project, "Successor", 2);
		long day = day();
		long base = project.getWorkCalendar().adjustInsideCalendar(project.getStart(), false);
		long predecessorStart = project.getWorkCalendar().add(base, 4L * day, false);
		predecessor.setManualDates(predecessorStart, finish(project, predecessorStart, 2L * day));
		successor.setManualDates(base, project.getWorkCalendar().add(base, 2L * day, true));
		successor.setPercentComplete(0.25);
		long actualStart = successor.getActualStart();
		double percentComplete = successor.getPercentComplete();
		DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.FS, 0L, this);

		long originalStart = successor.getStart();
		long originalFinish = successor.getEnd();
		assertThrows(IllegalStateException.class, () -> new RespectLinksService().apply(project, List.of(successor)));

		assertEquals(originalStart, successor.getStart());
		assertEquals(originalFinish, successor.getEnd());
		assertEquals(actualStart, successor.getActualStart());
		assertEquals(percentComplete, successor.getPercentComplete());
		assertTrue(successor.isManuallyScheduled());
	}

	@Test
	void unlinkedAndAutomaticSelectionsAreNoOpsAndUnselectedTasksDoNotMove() throws Exception {
		Project project = project();
		NormalTask predecessor = task(project, "Predecessor", 2);
		NormalTask selected = task(project, "Selected", 1);
		NormalTask outside = task(project, "Outside", 1);
		long early = project.getWorkCalendar().adjustInsideCalendar(project.getStart(), false);
		long late = project.getWorkCalendar().add(early, 9L * day(), false);
		selected.setManualDates(early, project.getWorkCalendar().add(early, day(), true));
		outside.setManualDates(late, project.getWorkCalendar().add(late, day(), true));
		DependencyService.getInstance().newDependency(predecessor, selected, DependencyType.FS, 0L, this);

		RespectLinksService service = new RespectLinksService();
		assertFalse(service.apply(project, List.of(outside)).change().hasChanged());
		assertFalse(service.apply(project, List.of(predecessor)).change().hasChanged());
		assertEquals(late, outside.getStart());
		assertEquals(1, service.apply(project, List.of(selected)).movedCount());
		assertTrue(selected.getStart() >= predecessor.getEnd());
		assertEquals(late, outside.getStart());
	}

	@Test
	void constraintConflictRollsBackDatesModeAndDirtyState() throws Exception {
		Project project = project();
		NormalTask predecessor = task(project, "Predecessor", 2);
		NormalTask successor = task(project, "Successor", 1);
		NormalTask unrelated = task(project, "Unrelated", 1);
		long day = day();
		long base = project.getWorkCalendar().adjustInsideCalendar(project.getStart(), false);
		long start = project.getWorkCalendar().add(base, 5L * day, false);
		predecessor.setManualDates(start, project.getWorkCalendar().add(start, 2L * day, true));
		long early = base;
		successor.setManualDates(early, project.getWorkCalendar().add(early, day, true));
		unrelated.setManualDates(start, project.getWorkCalendar().add(start, day, true));
		var resource = project.getResourcePool().newResourceInstance();
		var assignment = AssignmentService.getInstance().newAssignment(successor, resource, 1.0d, 0L, this);
		successor.setScheduleConstraint(ConstraintType.Kind.MSO, early);
		DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.FS, 0L, this);
		long oldStart = successor.getStart();
		long oldFinish = successor.getEnd();
		boolean oldDirty = successor.isDirty();
		long oldAssignmentStart = assignment.getStart();
		long oldAssignmentEnd = assignment.getEnd();
		long oldAssignmentWork = assignment.getWork(null);
		double oldAssignmentCost = assignment.getCost();
		long unrelatedStart = unrelated.getStart();
		long unrelatedFinish = unrelated.getEnd();
		boolean unrelatedDirty = unrelated.isDirty();
		long revision = project.getMutationState().revision();
		project.setDirty(false);
		project.getUndoController().clear();
		AtomicInteger schedules = new AtomicInteger();
		AtomicInteger objects = new AtomicInteger();
		project.addScheduleListener(event -> schedules.incrementAndGet());
		project.addObjectListener(event -> objects.incrementAndGet());

		IllegalStateException failure = assertThrows(IllegalStateException.class,
			() -> new RespectLinksService().apply(project, List.of(successor)));

		assertTrue(failure.getMessage().contains("Respect Links failed"));
		assertEquals(oldStart, successor.getStart());
		assertEquals(oldFinish, successor.getEnd());
		assertTrue(successor.isManuallyScheduled());
		assertEquals(ConstraintType.Kind.MSO, successor.getConstraintTypeKind());
		assertEquals(oldDirty, successor.isDirty());
		assertEquals(oldAssignmentStart, assignment.getStart());
		assertEquals(oldAssignmentEnd, assignment.getEnd());
		assertEquals(oldAssignmentWork, assignment.getWork(null));
		assertEquals(oldAssignmentCost, assignment.getCost(), 0.00001d);
		assertEquals(unrelatedStart, unrelated.getStart());
		assertEquals(unrelatedFinish, unrelated.getEnd());
		assertEquals(unrelatedDirty, unrelated.isDirty());
		assertFalse(project.isDirty());
		assertEquals(revision, project.getMutationState().revision());
		assertFalse(project.getUndoController().canUndo());
		assertFalse(project.getUndoController().canRedo());
		assertEquals(0, schedules.get());
		assertEquals(0, objects.get());
	}

	@Test
	void automaticConstraintConflictIsRejectedWithoutChangingModeOrDates() throws Exception {
		Project project = project();
		NormalTask predecessor = task(project, "Predecessor", 2);
		NormalTask successor = task(project, "Automatic successor", 1);
		long day = day();
		long base = project.getWorkCalendar().adjustInsideCalendar(project.getStart(), false);
		long late = project.getWorkCalendar().add(base, 5L * day, false);
		predecessor.setManualDates(late, project.getWorkCalendar().add(late, 2L * day, true));
		successor.setManualDates(base, project.getWorkCalendar().add(base, day, true));
		successor.setScheduleConstraint(ConstraintType.Kind.MSO, base);
		DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.FS, 0L, this);
		successor.setManuallyScheduled(false);
		project.recalculate();
		long oldStart = successor.getStart();
		long oldFinish = successor.getEnd();
		assertTrue(ScheduleDiagnosticsService.hasCurrentDependencyConflict(successor));

		assertThrows(IllegalStateException.class, () -> new RespectLinksService().apply(project, List.of(successor)));

		assertEquals(oldStart, successor.getStart());
		assertEquals(oldFinish, successor.getEnd());
		assertFalse(successor.isManuallyScheduled());
		assertEquals(ConstraintType.Kind.MSO, successor.getConstraintTypeKind());
	}

	private static Project project() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("respect-links", undo), undo);
		project.initialize(false, false);
		return project;
	}

	private static NormalTask task(Project project, String name, int days) {
		NormalTask task = new NormalTask(project);
		task.setName(name);
		project.connectTask(task);
		project.getSchedulingAlgorithm().addObject(task);
		task.setDuration(days * day());
		return task;
	}

	private static long day() {
		return com.microproject.options.CalendarOption.getInstance().getMillisPerDay();
	}

	private static long finish(Project project, long start, long duration) {
		return CalendarOption.getInstance().makeValidEnd(
			project.getWorkCalendar().add(start, duration, false), true);
	}
}
