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
package com.microproject.pm.scheduling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import com.microproject.field.FieldContext;
import com.microproject.util.DateTime;
import com.microproject.options.CalendarOption;
import com.microproject.pm.calendar.WorkDay;
import com.microproject.pm.calendar.WorkingCalendar;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

/** Named schedule transitions for task links, bar edits, and task-sheet dates. */
class TaskLinkAndBarScheduleTransitionsTest {
	private record LinkCase(int type, int lagDays) {}
	private record CalendarLagCase(int type, long predecessorStart, long predecessorDuration,
		long expectedPredecessorFinish, long successorDuration, long expectedInitialSuccessorFinish,
		long expectedSuccessorStart, long expectedSuccessorFinish) {}
	private record MoveCase(int offsetDays) {}
	private record ResizeCase(int durationDays) {}
	private record DurationCase(int durationDays) {}
	private record Fixture(Project project, DataFactoryUndoController undo) {}

	@TestFactory
	Stream<DynamicTest> taskLinkCases() {
		List<LinkCase> matrix = List.of(
			new LinkCase(DependencyType.FS, 0), new LinkCase(DependencyType.FS, 1),
			new LinkCase(DependencyType.FS, -1), new LinkCase(DependencyType.SS, 0),
			new LinkCase(DependencyType.SS, 2), new LinkCase(DependencyType.SS, -1),
			new LinkCase(DependencyType.FF, 0), new LinkCase(DependencyType.FF, 1),
			new LinkCase(DependencyType.FF, -2), new LinkCase(DependencyType.SF, 0),
			new LinkCase(DependencyType.SF, 1), new LinkCase(DependencyType.SF, -1));
		Stream<DynamicTest> typeAndLagCases = IntStream.range(0, matrix.size()).mapToObj(index ->
			DynamicTest.dynamicTest(id("LINK", index + 1), () -> verifyLinkAndRecalculation(matrix.get(index))));
		Stream<DynamicTest> structuralCases = Stream.of(
			DynamicTest.dynamicTest(id("LINK", 13), this::verifyThreeTaskChainPropagation),
			DynamicTest.dynamicTest(id("LINK", 15), this::verifyLinkRemovalDisconnectsBothTasks));
		return Stream.concat(typeAndLagCases, structuralCases);
	}

	@TestFactory
	Stream<DynamicTest> taskBarMovementAndDateCases() {
		List<MoveCase> moves = List.of(
			new MoveCase(-5), new MoveCase(-2), new MoveCase(-1), new MoveCase(1), new MoveCase(5));
		List<ResizeCase> resizes = List.of(
			new ResizeCase(1), new ResizeCase(2), new ResizeCase(5), new ResizeCase(10));
		List<DurationCase> durations = List.of(
			new DurationCase(1), new DurationCase(3), new DurationCase(7));

		Stream<DynamicTest> moveCases = IntStream.range(0, moves.size()).mapToObj(index ->
			DynamicTest.dynamicTest(id("BAR", index + 16), () -> verifyWholeBarMove(moves.get(index))));
		Stream<DynamicTest> resizeCases = IntStream.range(0, resizes.size()).mapToObj(index ->
			DynamicTest.dynamicTest(id("BAR", index + 21), () -> verifyFinishResize(resizes.get(index))));
		Stream<DynamicTest> durationCases = IntStream.range(0, durations.size()).mapToObj(index ->
			DynamicTest.dynamicTest(id("BAR", index + 25), () -> verifyDurationEdit(durations.get(index))));
		Stream<DynamicTest> dateCases = Stream.of(
			DynamicTest.dynamicTest(id("BAR", 28), this::verifyTaskSheetStartDateEdit),
			DynamicTest.dynamicTest(id("BAR", 29), this::verifyTaskSheetFinishDateEdit),
			DynamicTest.dynamicTest(id("BAR", 30), this::verifyMilestoneBarMove));
		return Stream.of(moveCases, resizeCases, durationCases, dateCases).flatMap(stream -> stream);
	}

	@Test
	void finishToStartWorkingDayLagSkipsDatedNonworkingException() throws Exception {
		Fixture fixture = createFixture();
		Project project = fixture.project;
		long mondayEight = timestamp(2030, Calendar.JUNE, 3, 8);
		long mondaySeventeen = timestamp(2030, Calendar.JUNE, 3, 17);
		long tuesdayMidnight = timestamp(2030, Calendar.JUNE, 4, 0);
		long thursdayEight = timestamp(2030, Calendar.JUNE, 6, 8);
		project.setStart(mondayEight);
		((WorkingCalendar) project.getWorkCalendar()).addOrReplaceException(new WorkDay(tuesdayMidnight));

		NormalTask predecessor = createTask(project, "holiday-lag predecessor", 1);
		NormalTask successor = createTask(project, "holiday-lag successor", 1);
		predecessor.setScheduleConstraint(ConstraintType.SNET, mondayEight);
		predecessor.getCurrentSchedule().setStart(mondayEight);
		successor.getCurrentSchedule().setStart(mondayEight);
		Dependency dependency = DependencyService.getInstance().newDependency(
			predecessor, successor, DependencyType.FS, day(), this);

		project.recalculate();

		assertEquals(mondayEight, predecessor.getStart());
		assertEquals(mondaySeventeen, predecessor.getEnd(),
			"the one-day predecessor uses Monday's 08:00-12:00 and 13:00-17:00 work intervals");
		// Document-derived: MSP uses the project calendar when no task/resource calendar overrides it;
		// an 8h lag consumes Wednesday's 08:00-12:00 and 13:00-17:00 intervals, so the task starts
		// Thursday 08:00. Sources: https://support.microsoft.com/en-us/project/how-project-schedules-tasks-behind-the-scenes
		// and https://support.microsoft.com/en-gb/project/add-lead-or-lag-time-to-a-task
		assertEquals(thursdayEight, successor.getStart(),
			"one working day of FS lag skips Tuesday; the next task starts at the next working interval");
		assertEquals(day(), dependency.getLag());
	}

	@TestFactory
	Stream<DynamicTest> workingDayLagUsesPredecessorTaskCalendarAcrossNonworkingDays() {
		long mondayEight = timestamp(2030, Calendar.JUNE, 3, 8);
		long mondayThirteen = timestamp(2030, Calendar.JUNE, 3, 13);
		long mondaySeventeen = timestamp(2030, Calendar.JUNE, 3, 17);
		long thursdayEight = timestamp(2030, Calendar.JUNE, 6, 8);
		long thursdayThirteen = timestamp(2030, Calendar.JUNE, 6, 13);
		long thursdaySeventeen = timestamp(2030, Calendar.JUNE, 6, 17);
		long fridayEight = timestamp(2030, Calendar.JUNE, 7, 8);
		long fridaySeventeen = timestamp(2030, Calendar.JUNE, 7, 17);
		long fourHours = day() / 2;
		List<CalendarLagCase> cases = List.of(
			// Retain the full-day end-boundary cases from the earlier oracle.
			new CalendarLagCase(DependencyType.FS, mondayEight, day(), mondaySeventeen,
				day(), mondaySeventeen, fridayEight, fridaySeventeen),
			new CalendarLagCase(DependencyType.SS, mondayEight, day(), mondaySeventeen,
				day(), mondaySeventeen, thursdayEight, thursdaySeventeen),
			// The half-day cases place lag endpoints inside working intervals or at
			// the explicit 12:00-13:00 break boundary.
			new CalendarLagCase(DependencyType.FS, mondayEight, fourHours, mondayThirteen,
				fourHours, mondayThirteen, thursdayThirteen, thursdaySeventeen),
			new CalendarLagCase(DependencyType.SS, mondayEight, fourHours, mondayThirteen,
				fourHours, mondayThirteen, thursdayEight, thursdayThirteen),
			new CalendarLagCase(DependencyType.FF, mondayEight, fourHours, mondayThirteen,
				fourHours, mondayThirteen, thursdayEight, thursdayThirteen),
			new CalendarLagCase(DependencyType.SF, mondayThirteen, fourHours, mondaySeventeen,
				fourHours, mondayThirteen, thursdayEight, thursdayThirteen));
		return IntStream.range(0, cases.size()).mapToObj(index ->
			DynamicTest.dynamicTest(id("LINK-CALENDAR", index + 1),
				() -> verifyWorkingDayLagUsesPredecessorTaskCalendar(cases.get(index))));
	}

	private void verifyWorkingDayLagUsesPredecessorTaskCalendar(CalendarLagCase c) throws Exception {
		Fixture fixture = createFixture();
		Project project = fixture.project;
		long mondayEight = timestamp(2030, Calendar.JUNE, 3, 8);
		long mondayThirteen = timestamp(2030, Calendar.JUNE, 3, 13);
		long mondaySeventeen = timestamp(2030, Calendar.JUNE, 3, 17);
		long tuesdayMidnight = timestamp(2030, Calendar.JUNE, 4, 0);
		long wednesdayMidnight = timestamp(2030, Calendar.JUNE, 5, 0);
		long wednesdayThirteen = timestamp(2030, Calendar.JUNE, 5, 13);
		long thursdayEight = timestamp(2030, Calendar.JUNE, 6, 8);
		long thursdayThirteen = timestamp(2030, Calendar.JUNE, 6, 13);
		long thursdaySeventeen = timestamp(2030, Calendar.JUNE, 6, 17);
		long fridayEight = timestamp(2030, Calendar.JUNE, 7, 8);
		long fridaySeventeen = timestamp(2030, Calendar.JUNE, 7, 17);
		project.setStart(mondayEight);
		((WorkingCalendar) project.getWorkCalendar()).addOrReplaceException(new WorkDay(tuesdayMidnight));

		NormalTask predecessor = createTask(project, "calendar-lag predecessor", 1);
		NormalTask successor = createTask(project, "calendar-lag successor", 1);
		NormalTask unrelated = createTask(project, "unrelated task", 1);
		WorkingCalendar predecessorCalendar = WorkingCalendar.getInstanceBasedOn(project.getWorkCalendar());
		predecessorCalendar.setName("Predecessor Wednesday exception");
		predecessorCalendar.addOrReplaceException(new WorkDay(wednesdayMidnight));
		predecessor.setWorkCalendar(predecessorCalendar);
		predecessor.setScheduleConstraint(ConstraintType.SNET, c.predecessorStart);
		successor.setScheduleConstraint(ConstraintType.SNET, mondayEight);
		unrelated.setScheduleConstraint(ConstraintType.SNET, mondayEight);
		predecessor.setDuration(c.predecessorDuration);
		successor.setDuration(c.successorDuration);
		predecessor.getCurrentSchedule().setStart(c.predecessorStart);
		successor.getCurrentSchedule().setStart(mondayEight);

		// The project calendar skips Tuesday; the predecessor calendar inherits that
		// exception and also skips Wednesday.
		assertEquals(thursdayEight, project.getWorkCalendar().add(mondaySeventeen, day(), false));
		assertEquals(thursdayEight, predecessorCalendar.add(mondayEight, day(), false));
		assertEquals(fridayEight, predecessor.getEffectiveWorkCalendar().add(mondaySeventeen, day(), false));
		assertEquals(wednesdayThirteen, project.getWorkCalendar().add(mondayEight + day() / 2, day(), false));
		assertEquals(thursdayThirteen, predecessorCalendar.add(mondayEight + day() / 2, day(), false));
		assertEquals(thursdayThirteen, predecessorCalendar.add(mondayThirteen, day(), false));
		assertSame(predecessorCalendar, predecessor.getEffectiveWorkCalendar());
		project.recalculate();
		assertEquals(c.predecessorStart, predecessor.getStart());
		assertEquals(c.expectedPredecessorFinish, predecessor.getEnd(), "predecessor finish before link creation");
		assertEquals(mondayEight, successor.getStart(), "successor start before link creation");
		assertEquals(c.expectedInitialSuccessorFinish, successor.getEnd(), "successor finish before link creation");
		assertEquals(mondayEight, unrelated.getStart(), "unrelated task start before link creation");
		assertEquals(mondaySeventeen, unrelated.getEnd(), "unrelated task finish before link creation");

		Dependency dependency = DependencyService.getInstance().newDependency(
			predecessor, successor, c.type, day(), this);
		project.recalculate();

		assertEquals(c.predecessorStart, predecessor.getStart());
		assertEquals(c.expectedPredecessorFinish, predecessor.getEnd());
		// Literal oracle uses the stated 08:00-12:00 and 13:00-17:00 intervals.
		// A 4h task starting Monday 08:00 finishes at Monday 13:00 (the next work
		// interval after the 12:00 break). FS/FF lag from that finish consumes Monday
		// 13:00-17:00 plus Thursday 08:00-12:00; the lag target normalizes to Thursday
		// 13:00. Thus FS starts Thursday 13:00, while a 4h FF successor starts Thursday
		// 08:00 and finishes Thursday 13:00. SS lag from Monday 08:00 consumes both
		// Monday intervals, then skips Tuesday/Wednesday to Thursday 08:00. SF lag from
		// Monday 13:00 consumes the afternoon plus Thursday morning, so a 4h successor
		// finishes Thursday 13:00. The retained full-day FS/SS cases assert their
		// Friday/Thursday 08:00 start boundaries.
		// Microsoft defines the four link endpoints and documents scheduling calendars
		// and lag, but not which calendar owns lag. Retain predecessor effective calendar
		// as a document-derived product decision, not measured MSP behavior or owner parity.
		// Sources: https://support.microsoft.com/en-us/project/link-tasks-in-a-project
		// https://support.microsoft.com/en-us/project/how-project-schedules-tasks-behind-the-scenes
		// https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task
		// https://learn.microsoft.com/en-us/office-project/xml-data-interchange/predecessorlink-element?view=project-client-2016
		assertEquals(c.type, dependency.getDependencyType());
		assertEquals(c.expectedSuccessorStart, successor.getStart(),
			"the predecessor task calendar skips Tuesday and Wednesday for type=" + c.type);
		assertEquals(c.expectedSuccessorFinish, successor.getEnd(),
			"the literal successor finish differs for type=" + c.type);
		assertEquals(mondayEight, unrelated.getStart(), "dependency recalculation leaves unrelated task unchanged");
		assertEquals(mondaySeventeen, unrelated.getEnd(), "dependency recalculation leaves unrelated finish unchanged");
		assertEquals(day(), dependency.getLag());

		assertTrue(fixture.undo.canUndo());
		fixture.undo.undo();
		project.recalculate();
		assertFalse(predecessor.getSuccessorList().iterator().hasNext());
		assertEquals(mondayEight, successor.getStart(), "Undo restores the independent schedule");
		assertEquals(c.expectedInitialSuccessorFinish, successor.getEnd(), "Undo restores the original finish");
		assertEquals(c.predecessorStart, predecessor.getStart(), "Undo leaves predecessor start unchanged");
		assertEquals(c.expectedPredecessorFinish, predecessor.getEnd(), "Undo leaves predecessor finish unchanged");
		assertEquals(mondaySeventeen, unrelated.getEnd(), "Undo leaves unrelated finish unchanged");
		fixture.undo.redo();
		project.recalculate();
		assertSame(dependency, predecessor.getSuccessorList().findRight(successor));
		assertEquals(c.expectedSuccessorStart, successor.getStart(), "Redo restores the calendar-derived date");
		assertEquals(c.expectedSuccessorFinish, successor.getEnd(), "Redo restores the calendar-derived finish");
		assertEquals(mondayEight, unrelated.getStart(), "Undo/Redo leaves unrelated task unchanged");
		assertEquals(mondaySeventeen, unrelated.getEnd(), "Undo/Redo leaves unrelated task finish unchanged");
	}

	@TestFactory
	Stream<DynamicTest> movedPredecessorPropagatesToSuccessorCases() {
		List<LinkCase> links = List.of(
			new LinkCase(DependencyType.FS, 0),
			new LinkCase(DependencyType.SS, 1),
			new LinkCase(DependencyType.FF, -1),
			new LinkCase(DependencyType.SF, 0));
		return IntStream.range(0, links.size()).mapToObj(index ->
			DynamicTest.dynamicTest("TLBM-MSP-" + String.format("%02d", index + 1),
				() -> verifyMovedPredecessorPropagatesToSuccessor(links.get(index))));
	}

	private void verifyLinkAndRecalculation(LinkCase c) throws Exception {
		Fixture fixture = createFixture();
		NormalTask predecessor = createTask(fixture.project, "predecessor", 2);
		NormalTask successor = createTask(fixture.project, "successor", 1);
		Dependency dependency = DependencyService.getInstance().newDependency(
			predecessor, successor, c.type, c.lagDays * day(), this);

		fixture.project.recalculate();

		assertSame(predecessor, dependency.getPredecessor());
		assertSame(successor, dependency.getSuccessor());
		assertEquals(c.type, dependency.getDependencyType());
		assertEquals(c.lagDays * day(), dependency.getLag());
		assertSame(dependency, predecessor.getSuccessorList().findRight(successor));
		assertSame(dependency, successor.getPredecessorList().findLeft(predecessor));
		long requiredStart = dependency.calcForwardDependencyDate(
			predecessor.getStart(), predecessor.getEnd(), successor.getDuration() != 0L);
		assertTrue(successor.getStart() >= requiredStart,
			"automatic task must not start before the link boundary for type=" + c.type + " lagDays=" + c.lagDays);
	}

	private void verifyThreeTaskChainPropagation() throws Exception {
		Fixture fixture = createFixture();
		NormalTask first = createTask(fixture.project, "first", 1);
		NormalTask second = createTask(fixture.project, "second", 1);
		NormalTask third = createTask(fixture.project, "third", 1);
		DependencyService.getInstance().newDependency(first, second, DependencyType.FS, 0L, this);
		DependencyService.getInstance().newDependency(second, third, DependencyType.FS, 0L, this);
		fixture.project.recalculate();
		long oldSecondStart = second.getStart();
		long oldThirdStart = third.getStart();

		first.setDuration(4L * day());
		fixture.project.recalculate();

		assertTrue(second.getStart() > oldSecondStart);
		assertTrue(third.getStart() > oldThirdStart);
		assertTrue(second.getStart() >= first.getEnd());
		assertTrue(third.getStart() >= second.getEnd());
	}

	private void verifyMovedPredecessorPropagatesToSuccessor(LinkCase c) throws Exception {
		Fixture fixture = createFixture();
		NormalTask predecessor = createTask(fixture.project, "moved-predecessor", 2);
		NormalTask successor = createTask(fixture.project, "linked-successor", 2);
		Dependency dependency = DependencyService.getInstance().newDependency(
			predecessor, successor, c.type, c.lagDays * day(), this);
		fixture.project.recalculate();

		long movedStart = predecessor.getEffectiveWorkCalendar().add(predecessor.getStart(), 3L * day(), false);
		predecessor.setStart(movedStart, taskSheetContext());
		fixture.project.recalculate();

		long requiredSuccessorStart = dependency.calcForwardDependencyDate(
			predecessor.getStart(), predecessor.getEnd(), successor.getDuration() != 0L);
		assertTrue(successor.getStart() >= requiredSuccessorStart,
			"link type=" + c.type + " lagDays=" + c.lagDays
				+ " successor must not precede its converted dependency date after the predecessor moves");
	}

	private void verifyLinkRemovalDisconnectsBothTasks() throws Exception {
		Fixture fixture = createFixture();
		NormalTask predecessor = createTask(fixture.project, "predecessor", 1);
		NormalTask successor = createTask(fixture.project, "successor", 1);
		Dependency dependency = DependencyService.getInstance().newDependency(
			predecessor, successor, DependencyType.FS, day(), this);

		DependencyService.getInstance().remove(dependency, this, true);

		assertFalse(predecessor.getSuccessorList().iterator().hasNext());
		assertFalse(successor.getPredecessorList().iterator().hasNext());
	}

	private void verifyWholeBarMove(MoveCase c) {
		Fixture fixture = createFixture();
		NormalTask task = createTask(fixture.project, "move", 3);
		long initialStart = task.getEffectiveWorkCalendar().add(task.getStart(), 10L * day(), false);
		task.setStart(initialStart, taskSheetContext());
		long originalStart = task.getStart();
		long originalEnd = task.getEnd();
		long originalDuration = task.getDuration();
		long movedStart = task.getEffectiveWorkCalendar().add(originalStart, c.offsetDays * day(), false);
		long requestedMovedEnd = canonicalFinish(task, movedStart, originalDuration);
		task.setScheduleConstraint(ConstraintType.SNET, movedStart);

		boolean changed = ScheduleService.getInstance().setInterval(this, task, movedStart, requestedMovedEnd,
			new ScheduleInterval(originalStart, originalEnd), fixture.undo.getEditSupport());
		task.setScheduleConstraint(ConstraintType.SNET, task.getStart());

		assertTrue(changed);
		assertEquals(movedStart, task.getStart());
		assertEquals(originalDuration, task.getDuration());
		assertEquals(originalDuration,
			task.getEffectiveWorkCalendar().compare(task.getEnd(), task.getStart(), false));
		assertTrue(c.offsetDays < 0 ? task.getEnd() < originalEnd : task.getEnd() > originalEnd);
		assertEquals(ConstraintType.SNET, task.getConstraintType());
		assertEquals(movedStart, task.getConstraintDate());
	}

	private void verifyFinishResize(ResizeCase c) {
		Fixture fixture = createFixture();
		NormalTask task = createTask(fixture.project, "resize", 3);
		long start = task.getStart();
		long originalEnd = task.getEnd();
		long resizedEnd = canonicalFinish(task, start, c.durationDays * day());
		task.setScheduleConstraint(ConstraintType.SNET, start);

		boolean changed = ScheduleService.getInstance().setInterval(this, task, start, resizedEnd,
			new ScheduleInterval(start, originalEnd), fixture.undo.getEditSupport());
		task.setScheduleConstraint(ConstraintType.SNET, task.getStart());

		assertTrue(changed);
		assertEquals(start, task.getStart());
		assertEquals(c.durationDays * day(), task.getDuration());
		assertEquals(c.durationDays * day(),
			task.getEffectiveWorkCalendar().compare(task.getEnd(), task.getStart(), false));
		assertEquals(ConstraintType.SNET, task.getConstraintType());
		assertEquals(start, task.getConstraintDate());
	}

	private void verifyDurationEdit(DurationCase c) {
		Fixture fixture = createFixture();
		NormalTask task = createTask(fixture.project, "duration", 2);
		long start = task.getStart();
		long expectedEnd = task.getEffectiveWorkCalendar().add(start, c.durationDays * day(), false);

		task.setDuration(c.durationDays * day());

		assertEquals(start, task.getStart());
		assertEquals(expectedEnd, task.getEnd());
		assertEquals(c.durationDays * day(), task.getDuration());
	}

	private void verifyTaskSheetStartDateEdit() {
		Fixture fixture = createFixture();
		NormalTask task = createTask(fixture.project, "start-date", 2);
		long newStart = task.getEffectiveWorkCalendar().add(task.getStart(), 3L * day(), false);
		long expectedEnd = task.getEffectiveWorkCalendar().add(newStart, 2L * day(), false);

		task.setStart(newStart, taskSheetContext());

		assertEquals(newStart, task.getStart());
		assertEquals(expectedEnd, task.getEnd());
		assertEquals(2L * day(), task.getDuration());
	}

	private void verifyTaskSheetFinishDateEdit() {
		Fixture fixture = createFixture();
		NormalTask task = createTask(fixture.project, "finish-date", 2);
		long newEnd = task.getEffectiveWorkCalendar().add(task.getEnd(), 4L * day(), false);
		long expectedStart = task.getEffectiveWorkCalendar().add(newEnd, -2L * day(), true);

		task.setEnd(newEnd, taskSheetContext());

		assertEquals(expectedStart, task.getStart());
		assertEquals(newEnd, task.getEnd());
		assertEquals(2L * day(), task.getDuration());
	}

	private void verifyMilestoneBarMove() {
		Fixture fixture = createFixture();
		NormalTask task = createTask(fixture.project, "milestone", 0);
		long original = task.getStart();
		long moved = task.getEffectiveWorkCalendar().add(original, 5L * day(), false);
		task.setScheduleConstraint(ConstraintType.SNET, moved);

		boolean changed = ScheduleService.getInstance().setInterval(this, task, moved, moved,
			new ScheduleInterval(original, original), fixture.undo.getEditSupport());
		task.setScheduleConstraint(ConstraintType.SNET, task.getStart());

		assertTrue(changed);
		assertTrue(task.isMilestone());
		assertEquals(0L, task.getDuration());
		assertEquals(moved, task.getStart());
		assertEquals(moved, task.getEnd());
	}

	private Fixture createFixture() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("link-bar-test", undo);
		Project project = Project.createProject(pool, undo);
		project.initialize(false, false);
		return new Fixture(project, undo);
	}

	private NormalTask createTask(Project project, String name, int durationDays) {
		NormalTask task = new NormalTask(project);
		task.setName(name);
		project.connectTask(task);
		project.getSchedulingAlgorithm().addObject(task);
		task.getCurrentSchedule().setStart(project.getStart());
		task.setDuration(durationDays * day());
		return task;
	}

	private FieldContext taskSheetContext() {
		FieldContext context = new FieldContext();
		context.setTaskSheetUpdate(true);
		return context;
	}

	private static int count(java.util.Iterator<?> iterator) {
		int count = 0;
		while (iterator.hasNext()) {
			iterator.next();
			count++;
		}
		return count;
	}

	private static long canonicalFinish(NormalTask task, long start, long duration) {
		long approximate = task.getEffectiveWorkCalendar().add(start, duration, false);
		long calendarDay = 24L * 60L * 60L * 1000L;
		for (int offset = -10; offset <= 10; offset++) {
			long candidate = CalendarOption.getInstance().makeValidEnd(approximate + offset * calendarDay, true);
			if (task.getEffectiveWorkCalendar().compare(candidate, start, false) == duration)
				return candidate;
		}
		throw new AssertionError("No canonical finish for start=" + start + " duration=" + duration);
	}

	private static long day() {
		return CalendarOption.getInstance().getMillisPerDay();
	}

	private static long timestamp(int year, int month, int dayOfMonth, int hourOfDay) {
		Calendar calendar = DateTime.calendarInstance(year, month, dayOfMonth);
		calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
		return calendar.getTimeInMillis();
	}

	private static String id(String group, int number) {
		return "TLBM-" + group + String.format("-%02d", number);
	}
}
