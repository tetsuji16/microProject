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
import com.microproject.pm.task.ScheduleDiagnosticsService;
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
	void finishToStartElapsedFourHourLagUsesFixedWallClockDatesAndUndoRedo() throws Exception {
		Fixture fixture = createFixture();
		Project project = fixture.project;
		long mondayEight = timestamp(2030, Calendar.JUNE, 3, 8);
		long mondayTwelve = timestamp(2030, Calendar.JUNE, 3, 12);
		long mondaySixteen = timestamp(2030, Calendar.JUNE, 3, 16);
		long mondaySeventeen = timestamp(2030, Calendar.JUNE, 3, 17);
		long tuesdaySixteen = timestamp(2030, Calendar.JUNE, 4, 16);
		long fourElapsedHours = com.microproject.datatype.Duration.setAsElapsed(4L * 60L * 60L * 1000L);
		// Document-derived contract: Microsoft describes positive lag as delaying an
		// FS successor and elapsed time as duration units; exact four-hour wall-clock
		// arithmetic is the narrow deterministic interpretation under this fixture.
		// https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task
		// https://support.microsoft.com/en-us/project/link-tasks-in-a-project
		project.setStart(mondayEight);
		NormalTask predecessor = createTask(project, "Elapsed FS predecessor", 1);
		NormalTask successor = createTask(project, "Elapsed FS successor", 1);
		NormalTask unrelated = createTask(project, "Elapsed FS unrelated", 1);
		predecessor.setManualDates(mondayEight, mondayTwelve);
		successor.setManualDates(mondayEight, mondaySeventeen);
		unrelated.setManualDates(mondayEight, mondaySeventeen);
		successor.setManuallyScheduled(false);
		project.recalculate();
		long unlinkedSuccessorStart = successor.getStart();
		long unlinkedSuccessorFinish = successor.getEnd();
		fixture.undo.clear();
		Dependency dependency = DependencyService.getInstance().newDependency(
			predecessor, successor, DependencyType.FS, fourElapsedHours, this);
		project.recalculate();

		assertEquals(DependencyType.FS, dependency.getDependencyType());
		assertEquals(fourElapsedHours, dependency.getLag());
		assertEquals(mondayEight, predecessor.getStart());
		assertEquals(mondayTwelve, predecessor.getEnd());
		assertEquals(mondaySixteen, successor.getStart());
		assertEquals(tuesdaySixteen, successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());

		fixture.undo.undo();
		project.recalculate();
		assertTrue(predecessor.getSuccessorList().isEmpty());
		assertEquals(unlinkedSuccessorStart, successor.getStart());
		assertEquals(unlinkedSuccessorFinish, successor.getEnd());
		fixture.undo.redo();
		project.recalculate();
		assertSame(dependency, predecessor.getSuccessorList().findRight(successor));
		assertEquals(mondaySixteen, successor.getStart());
		assertEquals(tuesdaySixteen, successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());
	}

	@Test
	void finishToStartNegativeElapsedFourHourLagUsesFixedWallClockDatesAndUndoRedo() throws Exception {
		Fixture fixture = createFixture();
		Project project = fixture.project;
		long mondayEight = timestamp(2030, Calendar.JUNE, 3, 8);
		long mondayFourteen = timestamp(2030, Calendar.JUNE, 3, 14);
		long mondayTen = timestamp(2030, Calendar.JUNE, 3, 10);
		long mondaySeventeen = timestamp(2030, Calendar.JUNE, 3, 17);
		long fourElapsedHours = com.microproject.datatype.Duration.setAsElapsed(4L * 60L * 60L * 1000L);
		// Document-derived: Microsoft defines FS endpoints and negative lag as lead/overlap.
		// The exact predecessor-finish minus four wall-clock hours boundary is inferred,
		// not an MSP measurement; standard-calendar normalization leaves it at Monday 10:00.
		// https://support.microsoft.com/en-us/project/link-tasks-in-a-project
		// https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task
		project.setStart(mondayEight);
		NormalTask predecessor = createTask(project, "Negative elapsed FS predecessor", 1);
		NormalTask successor = createTask(project, "Negative elapsed FS successor", 1);
		NormalTask unrelated = createTask(project, "Negative elapsed FS unrelated", 1);
		predecessor.setManualDates(mondayEight, mondayFourteen);
		predecessor.setDuration(5L * 60L * 60L * 1000L);
		successor.setManualDates(mondayEight, mondaySeventeen);
		successor.setDuration(3L * 60L * 60L * 1000L);
		unrelated.setManualDates(mondayEight, mondaySeventeen);
		successor.setManuallyScheduled(false);
		project.recalculate();
		long unlinkedStart = successor.getStart();
		long unlinkedFinish = successor.getEnd();
		fixture.undo.clear();
		Dependency dependency = DependencyService.getInstance().newDependency(
			predecessor, successor, DependencyType.FS, -fourElapsedHours, this);
		project.recalculate();

		assertEquals(DependencyType.FS, dependency.getDependencyType());
		assertEquals(-fourElapsedHours, dependency.getLag());
		assertTrue(com.microproject.datatype.Duration.isElapsed(dependency.getLag()));
		assertEquals(mondayTen, predecessor.getEnd() + com.microproject.datatype.Duration.millis(dependency.getLag()),
			"negative elapsed FS boundary is predecessor finish minus four wall-clock hours");
		assertEquals(mondayTen, successor.getStart());
		assertEquals(mondayFourteen, successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());

		fixture.undo.undo();
		project.recalculate();
		assertTrue(predecessor.getSuccessorList().isEmpty());
		assertEquals(unlinkedStart, successor.getStart());
		assertEquals(unlinkedFinish, successor.getEnd());
		fixture.undo.redo();
		project.recalculate();
		assertSame(dependency, predecessor.getSuccessorList().findRight(successor));
		assertEquals(mondayTen, successor.getStart());
		assertEquals(mondayFourteen, successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());
	}

	@Test
	void startToStartNegativeElapsedFourHourLagUsesFixedWallClockDatesAndUndoRedo() throws Exception {
		Fixture fixture = createFixture();
		Project project = fixture.project;
		long mondayEight = timestamp(2030, Calendar.JUNE, 3, 8);
		long mondayTwelve = timestamp(2030, Calendar.JUNE, 3, 12);
		long mondaySeventeen = timestamp(2030, Calendar.JUNE, 3, 17);
		long tuesdayFour = timestamp(2030, Calendar.JUNE, 4, 4);
		long tuesdayEight = timestamp(2030, Calendar.JUNE, 4, 8);
		long tuesdayEleven = timestamp(2030, Calendar.JUNE, 4, 11);
		long tuesdayFourteen = timestamp(2030, Calendar.JUNE, 4, 14);
		long fourElapsedHours = com.microproject.datatype.Duration.setAsElapsed(4L * 60L * 60L * 1000L);
		// Document-derived: Microsoft defines SS endpoints and negative lag as lead/overlap.
		// The exact start-minus-four-wall-clock-hours boundary and normalization from Tuesday
		// 04:00 to the standard-calendar Tuesday 08:00 boundary are inferred, not measured.
		// https://support.microsoft.com/en-us/project/link-tasks-in-a-project
		// https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task
		project.setStart(mondayEight);
		NormalTask predecessor = createTask(project, "Negative elapsed SS predecessor", 1);
		NormalTask successor = createTask(project, "Negative elapsed SS successor", 1);
		NormalTask unrelated = createTask(project, "Negative elapsed SS unrelated", 1);
		predecessor.setManualDates(tuesdayEight, tuesdayFourteen);
		predecessor.setDuration(5L * 60L * 60L * 1000L);
		successor.setManualDates(mondayEight, mondaySeventeen);
		successor.setDuration(3L * 60L * 60L * 1000L);
		unrelated.setManualDates(mondayEight, mondaySeventeen);
		successor.setManuallyScheduled(false);
		project.recalculate();
		long unlinkedStart = successor.getStart();
		long unlinkedFinish = successor.getEnd();
		fixture.undo.clear();
		Dependency dependency = DependencyService.getInstance().newDependency(
			predecessor, successor, DependencyType.SS, -fourElapsedHours, this);
		project.recalculate();

		assertEquals(DependencyType.SS, dependency.getDependencyType());
		assertEquals(-fourElapsedHours, dependency.getLag());
		assertTrue(com.microproject.datatype.Duration.isElapsed(dependency.getLag()));
		assertEquals(tuesdayFour, predecessor.getStart() + com.microproject.datatype.Duration.millis(dependency.getLag()),
			"negative elapsed SS boundary is predecessor start minus four wall-clock hours");
		assertEquals(tuesdayEight, successor.getStart(), "non-working boundary normalizes forward to Tuesday 08:00");
		assertTrue(successor.getStart() > unlinkedStart, "successor must move later than its initial Monday schedule");
		assertEquals(tuesdayEleven, successor.getEnd());
		assertEquals(tuesdayEight, predecessor.getStart());
		assertEquals(tuesdayFourteen, predecessor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());

		fixture.undo.undo();
		project.recalculate();
		assertTrue(predecessor.getSuccessorList().isEmpty());
		assertEquals(unlinkedStart, successor.getStart());
		assertEquals(unlinkedFinish, successor.getEnd());
		fixture.undo.redo();
		project.recalculate();
		assertSame(dependency, predecessor.getSuccessorList().findRight(successor));
		assertEquals(tuesdayEight, successor.getStart());
		assertEquals(tuesdayEleven, successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());
	}

	@Test
	void finishToFinishNegativeElapsedFourHourLagUsesFixedWallClockDatesAndUndoRedo() throws Exception {
		Fixture fixture = createFixture();
		Project project = fixture.project;
		long mondayEight = timestamp(2030, Calendar.JUNE, 3, 8);
		long mondaySeventeen = timestamp(2030, Calendar.JUNE, 3, 17);
		long tuesdayFour = timestamp(2030, Calendar.JUNE, 4, 4);
		long tuesdayEight = timestamp(2030, Calendar.JUNE, 4, 8);
		long tuesdayTen = timestamp(2030, Calendar.JUNE, 4, 10);
		long tuesdayFourteen = timestamp(2030, Calendar.JUNE, 4, 14);
		long fourElapsedHours = com.microproject.datatype.Duration.setAsElapsed(4L * 60L * 60L * 1000L);
		// Document-derived: Microsoft defines FF endpoints and negative lag as lead/overlap.
		// Exact finish-minus-four-wall-clock-hours and calendar normalization are inferred,
		// not an MSP measurement.
		// https://support.microsoft.com/en-us/project/link-tasks-in-a-project
		// https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task
		project.setStart(mondayEight);
		NormalTask predecessor = createTask(project, "Negative elapsed FF predecessor", 1);
		NormalTask successor = createTask(project, "Negative elapsed FF successor", 1);
		NormalTask unrelated = createTask(project, "Negative elapsed FF unrelated", 1);
		predecessor.setManualDates(tuesdayEight, tuesdayFourteen);
		predecessor.setDuration(5L * 60L * 60L * 1000L);
		successor.setManualDates(mondayEight, mondaySeventeen);
		successor.setDuration(2L * 60L * 60L * 1000L);
		unrelated.setManualDates(mondayEight, mondaySeventeen);
		successor.setManuallyScheduled(false);
		project.recalculate();
		long unlinkedStart = successor.getStart();
		long unlinkedFinish = successor.getEnd();
		fixture.undo.clear();
		Dependency dependency = DependencyService.getInstance().newDependency(
			predecessor, successor, DependencyType.FF, -fourElapsedHours, this);
		project.recalculate();

		assertEquals(DependencyType.FF, dependency.getDependencyType());
		assertEquals(-fourElapsedHours, dependency.getLag());
		assertTrue(com.microproject.datatype.Duration.isElapsed(dependency.getLag()));
		assertEquals(tuesdayTen, predecessor.getEnd() + com.microproject.datatype.Duration.millis(dependency.getLag()),
			"negative elapsed FF boundary is predecessor finish minus four wall-clock hours");
		assertEquals(tuesdayEight, successor.getStart());
		assertTrue(successor.getStart() > unlinkedStart, "successor must move from its initial Monday schedule");
		assertEquals(tuesdayTen, successor.getEnd());
		assertEquals(tuesdayEight, predecessor.getStart());
		assertEquals(tuesdayFourteen, predecessor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());

		fixture.undo.undo();
		project.recalculate();
		assertTrue(predecessor.getSuccessorList().isEmpty());
		assertEquals(unlinkedStart, successor.getStart());
		assertEquals(unlinkedFinish, successor.getEnd());
		fixture.undo.redo();
		project.recalculate();
		assertSame(dependency, predecessor.getSuccessorList().findRight(successor));
		assertEquals(tuesdayEight, successor.getStart());
		assertEquals(tuesdayTen, successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());
	}

	@Test
	void startToStartFiftyPercentLagUsesPredecessorDurationAndUndoRedo() throws Exception {
		Fixture fixture = createFixture();
		Project project = fixture.project;
		long mondayEight = timestamp(2030, Calendar.JUNE, 3, 8);
		long mondayThirteen = timestamp(2030, Calendar.JUNE, 3, 13);
		long mondaySeventeen = timestamp(2030, Calendar.JUNE, 3, 17);
		long tuesdayThirteen = timestamp(2030, Calendar.JUNE, 4, 13);
		long oneDay = CalendarOption.getInstance().getMillisPerDay();
		long halfDurationPercent = com.microproject.datatype.Duration.setAsPercent(
			com.microproject.datatype.Duration.setPercentAsDecimal(0L, 0.5F));
		// Microsoft defines SS as start-to-start and positive lag as delaying the
		// successor. The 50% duration arithmetic and exact boundary are document-derived.
		// https://support.microsoft.com/en-us/project/link-tasks-in-a-project
		// https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task
		project.setStart(mondayEight);
		NormalTask predecessor = createTask(project, "Percent SS predecessor", 1);
		NormalTask successor = createTask(project, "Percent SS successor", 1);
		NormalTask unrelated = createTask(project, "Percent SS unrelated", 1);
		predecessor.setDuration(oneDay);
		successor.setDuration(oneDay);
		unrelated.setDuration(oneDay);
		predecessor.setManualDates(mondayEight, mondaySeventeen);
		successor.setManualDates(mondayEight, mondaySeventeen);
		unrelated.setManualDates(mondayEight, mondaySeventeen);
		successor.setManuallyScheduled(false);
		project.recalculate();
		long unlinkedStart = successor.getStart();
		long unlinkedFinish = successor.getEnd();
		fixture.undo.clear();
		Dependency dependency = DependencyService.getInstance().newDependency(
			predecessor, successor, DependencyType.SS, halfDurationPercent, this);
		project.recalculate();
		assertEquals(oneDay / 2, com.microproject.datatype.Duration.millis(dependency.getLeadValue()));
		long requiredStart = dependency.calcForwardDependencyDate(
			predecessor.getStart(), predecessor.getEnd(), true);
		assertEquals(mondayThirteen, requiredStart, "SS percentage boundary is based on predecessor start");
		assertEquals(DependencyType.SS, dependency.getDependencyType());
		assertEquals(halfDurationPercent, dependency.getLag());
		assertTrue(com.microproject.datatype.Duration.isPercent(dependency.getLag()));
		assertEquals(0.5F, com.microproject.datatype.Duration.getPercentAsDecimal(dependency.getLag()));
		assertEquals(mondayEight, predecessor.getStart());
		assertEquals(mondaySeventeen, predecessor.getEnd());
		assertEquals(mondayThirteen, successor.getStart());
		assertTrue(successor.getStart() >= requiredStart);
		assertEquals(tuesdayThirteen, successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());
		fixture.undo.undo();
		project.recalculate();
		assertTrue(predecessor.getSuccessorList().isEmpty());
		assertEquals(unlinkedStart, successor.getStart());
		assertEquals(unlinkedFinish, successor.getEnd());
		fixture.undo.redo();
		project.recalculate();
		assertSame(dependency, predecessor.getSuccessorList().findRight(successor));
		assertEquals(mondayThirteen, successor.getStart());
		assertEquals(tuesdayThirteen, successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());
	}

	@Test
	void startToStartNegativeFiftyPercentLagUsesPredecessorDurationAndUndoRedo() throws Exception {
		Fixture fixture = createFixture();
		Project project = fixture.project;
		long mondayEight = timestamp(2030, Calendar.JUNE, 3, 8);
		long mondayTwelve = timestamp(2030, Calendar.JUNE, 3, 12);
		long mondayThirteen = timestamp(2030, Calendar.JUNE, 3, 13);
		long mondaySeventeen = timestamp(2030, Calendar.JUNE, 3, 17);
		long tuesdayEight = timestamp(2030, Calendar.JUNE, 4, 8);
		long tuesdaySeventeen = timestamp(2030, Calendar.JUNE, 4, 17);
		long tuesdayThirteen = timestamp(2030, Calendar.JUNE, 4, 13);
		long oneDay = CalendarOption.getInstance().getMillisPerDay();
		long negativeHalfDurationPercent = com.microproject.datatype.Duration.setAsPercent(
			com.microproject.datatype.Duration.setPercentAsDecimal(0L, -0.5F));
		// Microsoft documents SS endpoint ordering and negative lag as lead time.
		// The percentage arithmetic and exact boundary are document-derived, not measured MSP behavior.
		// https://support.microsoft.com/en-us/project/link-tasks-in-a-project
		// https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task
		project.setStart(mondayEight);
		NormalTask predecessor = createTask(project, "Negative percent SS predecessor", 1);
		NormalTask successor = createTask(project, "Negative percent SS successor", 1);
		NormalTask unrelated = createTask(project, "Negative percent SS unrelated", 1);
		predecessor.setDuration(oneDay);
		successor.setDuration(oneDay);
		unrelated.setDuration(oneDay);
		predecessor.setManualDates(tuesdayEight, tuesdaySeventeen);
		successor.setManualDates(mondayEight, mondaySeventeen);
		unrelated.setManualDates(mondayEight, mondaySeventeen);
		successor.setManuallyScheduled(false);
		project.recalculate();
		long unlinkedStart = successor.getStart();
		long unlinkedFinish = successor.getEnd();
		fixture.undo.clear();
		Dependency dependency = DependencyService.getInstance().newDependency(
			predecessor, successor, DependencyType.SS, negativeHalfDurationPercent, this);
		project.recalculate();

		assertEquals(-oneDay / 2, com.microproject.datatype.Duration.millis(dependency.getLeadValue()),
			"negative SS percentage lag must be half the predecessor's duration");
		long requiredStart = dependency.calcForwardDependencyDate(
			predecessor.getStart(), predecessor.getEnd(), true);
		assertEquals(mondayTwelve, requiredStart,
			"SS lead boundary is half a workday before predecessor start");
		assertEquals(DependencyType.SS, dependency.getDependencyType());
		assertEquals(negativeHalfDurationPercent, dependency.getLag());
		assertTrue(com.microproject.datatype.Duration.isPercent(dependency.getLag()));
		assertEquals(-0.5F, com.microproject.datatype.Duration.getPercentAsDecimal(dependency.getLag()));
		assertEquals(tuesdayEight, predecessor.getStart());
		assertEquals(tuesdaySeventeen, predecessor.getEnd());
		assertTrue(successor.getStart() > unlinkedStart, "successor must move to the normalized SS boundary");
		assertEquals(mondayThirteen, successor.getStart());
		assertEquals(tuesdayThirteen, successor.getEnd());
		assertTrue(successor.getStart() >= requiredStart);
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());

		fixture.undo.undo();
		project.recalculate();
		assertTrue(predecessor.getSuccessorList().isEmpty());
		assertEquals(unlinkedStart, successor.getStart());
		assertEquals(unlinkedFinish, successor.getEnd());
		fixture.undo.redo();
		project.recalculate();
		assertSame(dependency, predecessor.getSuccessorList().findRight(successor));
		assertEquals(mondayThirteen, successor.getStart());
		assertEquals(tuesdayThirteen, successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());
	}

	@Test
	void finishToFinishFiftyPercentLagUsesPredecessorDurationAndUndoRedo() throws Exception {
		Fixture fixture = createFixture();
		Project project = fixture.project;
		long mondayEight = timestamp(2030, Calendar.JUNE, 3, 8);
		long mondayThirteen = timestamp(2030, Calendar.JUNE, 3, 13);
		long mondaySeventeen = timestamp(2030, Calendar.JUNE, 3, 17);
		long tuesdayThirteen = timestamp(2030, Calendar.JUNE, 4, 13);
		long oneDay = CalendarOption.getInstance().getMillisPerDay();
		long halfDurationPercent = com.microproject.datatype.Duration.setAsPercent(
			com.microproject.datatype.Duration.setPercentAsDecimal(0L, 0.5F));
		// Microsoft defines FF as finish-to-finish and positive lag as delaying the
		// successor. The 50% duration arithmetic and exact boundary are document-derived.
		// https://support.microsoft.com/en-us/project/link-tasks-in-a-project
		// https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task
		project.setStart(mondayEight);
		NormalTask predecessor = createTask(project, "Percent FF predecessor", 1);
		NormalTask successor = createTask(project, "Percent FF successor", 1);
		NormalTask unrelated = createTask(project, "Percent FF unrelated", 1);
		predecessor.setDuration(oneDay);
		successor.setDuration(oneDay);
		unrelated.setDuration(oneDay);
		predecessor.setManualDates(mondayEight, mondaySeventeen);
		successor.setManualDates(mondayEight, mondaySeventeen);
		unrelated.setManualDates(mondayEight, mondaySeventeen);
		successor.setManuallyScheduled(false);
		project.recalculate();
		long unlinkedStart = successor.getStart();
		long unlinkedFinish = successor.getEnd();
		fixture.undo.clear();
		Dependency dependency = DependencyService.getInstance().newDependency(
			predecessor, successor, DependencyType.FF, halfDurationPercent, this);
		project.recalculate();

		assertEquals(oneDay / 2, com.microproject.datatype.Duration.millis(dependency.getLeadValue()));
		assertEquals(tuesdayThirteen, predecessor.getEffectiveWorkCalendar().add(
			predecessor.getEnd(), dependency.getLeadValue(), true), "FF percentage boundary from predecessor finish");
		assertEquals(DependencyType.FF, dependency.getDependencyType());
		assertEquals(halfDurationPercent, dependency.getLag());
		assertTrue(com.microproject.datatype.Duration.isPercent(dependency.getLag()));
		assertEquals(0.5F, com.microproject.datatype.Duration.getPercentAsDecimal(dependency.getLag()));
		assertEquals(mondayEight, predecessor.getStart());
		assertEquals(mondaySeventeen, predecessor.getEnd());
		assertEquals(tuesdayThirteen, successor.getEnd());
		assertTrue(successor.getEnd() > unlinkedFinish, "successor must move to the FF finish boundary");
		assertEquals(mondayThirteen, successor.getStart());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());

		fixture.undo.undo();
		project.recalculate();
		assertTrue(predecessor.getSuccessorList().isEmpty());
		assertEquals(unlinkedStart, successor.getStart());
		assertEquals(unlinkedFinish, successor.getEnd());
		fixture.undo.redo();
		project.recalculate();
		assertSame(dependency, predecessor.getSuccessorList().findRight(successor));
		assertEquals(mondayThirteen, successor.getStart());
		assertEquals(tuesdayThirteen, successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());
	}

	@Test
	void finishToFinishNegativeFiftyPercentLagUsesPredecessorDurationAndUndoRedo() throws Exception {
		Fixture fixture = createFixture();
		Project project = fixture.project;
		long mondayEight = timestamp(2030, Calendar.JUNE, 3, 8);
		long mondaySeventeen = timestamp(2030, Calendar.JUNE, 3, 17);
		long tuesdayThirteen = timestamp(2030, Calendar.JUNE, 4, 13);
		long wednesdayEight = timestamp(2030, Calendar.JUNE, 5, 8);
		long wednesdayTwelve = timestamp(2030, Calendar.JUNE, 5, 12);
		long wednesdayThirteen = timestamp(2030, Calendar.JUNE, 5, 13);
		long wednesdaySeventeen = timestamp(2030, Calendar.JUNE, 5, 17);
		long oneDay = CalendarOption.getInstance().getMillisPerDay();
		long negativeHalfDurationPercent = com.microproject.datatype.Duration.setAsPercent(
			com.microproject.datatype.Duration.setPercentAsDecimal(0L, -0.5F));
		// Microsoft documents FF endpoint ordering and negative lag as lead/overlap.
		// The exact half-duration arithmetic and calendar boundary are document-derived,
		// not measured MSP behavior.
		// https://support.microsoft.com/en-us/project/link-tasks-in-a-project
		// https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task
		project.setStart(mondayEight);
		NormalTask predecessor = createTask(project, "Negative percent FF predecessor", 1);
		NormalTask successor = createTask(project, "Negative percent FF successor", 1);
		NormalTask unrelated = createTask(project, "Negative percent FF unrelated", 1);
		predecessor.setDuration(oneDay);
		successor.setDuration(oneDay);
		unrelated.setDuration(oneDay);
		predecessor.setManualDates(wednesdayEight, wednesdaySeventeen);
		successor.setManualDates(mondayEight, mondaySeventeen);
		unrelated.setManualDates(mondayEight, mondaySeventeen);
		successor.setManuallyScheduled(false);
		project.recalculate();
		assertEquals(oneDay, predecessor.getDuration(), "predecessor duration before link");
		long unlinkedStart = successor.getStart();
		long unlinkedFinish = successor.getEnd();
		fixture.undo.clear();
		Dependency dependency = DependencyService.getInstance().newDependency(
			predecessor, successor, DependencyType.FF, negativeHalfDurationPercent, this);
		project.recalculate();

		assertEquals(-oneDay / 2, com.microproject.datatype.Duration.millis(dependency.getLeadValue()),
			"negative FF percentage lag must be half the predecessor's duration");
		long requiredFinish = predecessor.getEffectiveWorkCalendar().add(
			predecessor.getEnd(), dependency.getLeadValue(), true);
		assertEquals(wednesdayTwelve, requiredFinish,
			"FF lead boundary is half a workday before predecessor finish");
		assertEquals(DependencyType.FF, dependency.getDependencyType());
		assertEquals(negativeHalfDurationPercent, dependency.getLag());
		assertTrue(com.microproject.datatype.Duration.isPercent(dependency.getLag()));
		assertEquals(-0.5F, com.microproject.datatype.Duration.getPercentAsDecimal(dependency.getLag()));
		assertEquals(wednesdayEight, predecessor.getStart());
		assertEquals(wednesdaySeventeen, predecessor.getEnd());
		assertTrue(successor.getEnd() > unlinkedFinish, "successor must move to the FF lead boundary");
		assertEquals(tuesdayThirteen, successor.getStart());
		assertEquals(wednesdayThirteen, successor.getEnd());
		assertTrue(successor.getEnd() >= requiredFinish,
			"the automatic successor's canonical finish must meet the FF lead boundary");
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());

		fixture.undo.undo();
		project.recalculate();
		assertTrue(predecessor.getSuccessorList().isEmpty());
		assertEquals(unlinkedStart, successor.getStart());
		assertEquals(unlinkedFinish, successor.getEnd());
		fixture.undo.redo();
		project.recalculate();
		assertSame(dependency, predecessor.getSuccessorList().findRight(successor));
		assertEquals(tuesdayThirteen, successor.getStart());
		assertEquals(wednesdayThirteen, successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());
	}

	@Test
	void finishToStartFiftyPercentLagUsesPredecessorDurationAndUndoRedo() throws Exception {
		Fixture fixture = createFixture();
		Project project = fixture.project;
		long mondayEight = timestamp(2030, Calendar.JUNE, 3, 8);
		long mondaySeventeen = timestamp(2030, Calendar.JUNE, 3, 17);
		long tuesdayThirteen = timestamp(2030, Calendar.JUNE, 4, 13);
		long wednesdayThirteen = timestamp(2030, Calendar.JUNE, 5, 13);
		long oneDay = CalendarOption.getInstance().getMillisPerDay();
		long halfDurationPercent = com.microproject.datatype.Duration.setAsPercent(
			com.microproject.datatype.Duration.setPercentAsDecimal(0L, 0.5F));
		// Microsoft documents FS endpoint ordering and that positive lag delays the
		// successor. The 50% of predecessor-duration interpretation is the narrow
		// document-derived rule here; these exact clock dates are not MSP measurements.
		// https://support.microsoft.com/en-us/project/link-tasks-in-a-project
		// https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task
		project.setStart(mondayEight);
		NormalTask predecessor = createTask(project, "Percent FS predecessor", 1);
		NormalTask successor = createTask(project, "Percent FS successor", 1);
		NormalTask unrelated = createTask(project, "Percent FS unrelated", 1);
		predecessor.setDuration(oneDay);
		successor.setDuration(oneDay);
		unrelated.setDuration(oneDay);
		predecessor.setManualDates(mondayEight, mondaySeventeen);
		successor.setManualDates(mondayEight, mondaySeventeen);
		unrelated.setManualDates(mondayEight, mondaySeventeen);
		successor.setManuallyScheduled(false);
		project.recalculate();
		assertEquals(oneDay, predecessor.getDuration(), "predecessor duration before link");
		long unlinkedStart = successor.getStart();
		long unlinkedFinish = successor.getEnd();
		fixture.undo.clear();
		Dependency dependency = DependencyService.getInstance().newDependency(
			predecessor, successor, DependencyType.FS, halfDurationPercent, this);
		project.recalculate();
		assertEquals(oneDay / 2, com.microproject.datatype.Duration.millis(dependency.getLeadValue()),
			"percentage lag must be half the predecessor's duration; raw lag=" + dependency.getLag()
				+ ", lead=" + dependency.getLeadValue() + ", predecessor duration=" + predecessor.getDuration());
		assertEquals(tuesdayThirteen, predecessor.getEffectiveWorkCalendar().add(
			predecessor.getEnd(), dependency.getLeadValue(), true), "FS lag boundary from predecessor finish");

		assertEquals(DependencyType.FS, dependency.getDependencyType());
		assertEquals(halfDurationPercent, dependency.getLag());
		assertTrue(com.microproject.datatype.Duration.isPercent(dependency.getLag()));
		assertEquals(0.5F, com.microproject.datatype.Duration.getPercentAsDecimal(dependency.getLag()));
		assertEquals(mondayEight, predecessor.getStart());
		assertEquals(mondaySeventeen, predecessor.getEnd());
		assertEquals(tuesdayThirteen, successor.getStart());
		assertEquals(wednesdayThirteen, successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());

		fixture.undo.undo();
		project.recalculate();
		assertTrue(predecessor.getSuccessorList().isEmpty());
		assertEquals(unlinkedStart, successor.getStart());
		assertEquals(unlinkedFinish, successor.getEnd());
		fixture.undo.redo();
		project.recalculate();
		assertSame(dependency, predecessor.getSuccessorList().findRight(successor));
		assertEquals(tuesdayThirteen, successor.getStart());
		assertEquals(wednesdayThirteen, successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());
	}

	@Test
	void startToFinishFiftyPercentLagUsesPredecessorDurationAndUndoRedo() throws Exception {
		Fixture fixture = createFixture();
		Project project = fixture.project;
		long mondayEight = timestamp(2030, Calendar.JUNE, 3, 8);
		long mondayThirteen = timestamp(2030, Calendar.JUNE, 3, 13);
		long tuesdayEight = timestamp(2030, Calendar.JUNE, 4, 8);
		long tuesdayThirteen = timestamp(2030, Calendar.JUNE, 4, 13);
		long tuesdaySeventeen = timestamp(2030, Calendar.JUNE, 4, 17);
		long oneDay = CalendarOption.getInstance().getMillisPerDay();
		long halfDurationPercent = com.microproject.datatype.Duration.setAsPercent(
			com.microproject.datatype.Duration.setPercentAsDecimal(0L, 0.5F));
		// Microsoft documents SF endpoint ordering and positive lag delaying the
		// successor. The 50% duration arithmetic and exact boundary are document-derived,
		// not measured MSP behavior.
		// https://support.microsoft.com/en-us/project/link-tasks-in-a-project
		// https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task
		project.setStart(mondayEight);
		NormalTask predecessor = createTask(project, "Percent SF predecessor", 1);
		NormalTask successor = createTask(project, "Percent SF successor", 1);
		NormalTask unrelated = createTask(project, "Percent SF unrelated", 1);
		predecessor.setDuration(oneDay);
		successor.setDuration(oneDay);
		unrelated.setDuration(oneDay);
		predecessor.setManualDates(tuesdayEight, tuesdaySeventeen);
		successor.setManualDates(mondayEight, timestamp(2030, Calendar.JUNE, 3, 17));
		unrelated.setManualDates(mondayEight, timestamp(2030, Calendar.JUNE, 3, 17));
		successor.setManuallyScheduled(false);
		project.recalculate();
		long unlinkedStart = successor.getStart();
		long unlinkedFinish = successor.getEnd();
		fixture.undo.clear();
		Dependency dependency = DependencyService.getInstance().newDependency(
			predecessor, successor, DependencyType.SF, halfDurationPercent, this);
		project.recalculate();

		assertEquals(oneDay / 2, com.microproject.datatype.Duration.millis(dependency.getLeadValue()));
		long requiredFinish = predecessor.getEffectiveWorkCalendar().add(
			predecessor.getStart(), dependency.getLeadValue(), false);
		assertEquals(tuesdayThirteen, requiredFinish,
			"SF half-duration from predecessor start reaches lunch and normalizes to 13:00");
		assertEquals(DependencyType.SF, dependency.getDependencyType());
		assertEquals(halfDurationPercent, dependency.getLag());
		assertTrue(com.microproject.datatype.Duration.isPercent(dependency.getLag()));
		assertEquals(0.5F, com.microproject.datatype.Duration.getPercentAsDecimal(dependency.getLag()));
		assertEquals(tuesdayEight, predecessor.getStart());
		assertEquals(tuesdaySeventeen, predecessor.getEnd());
		assertTrue(successor.getStart() > unlinkedStart, "successor must move from its initial Monday schedule");
		assertEquals(mondayThirteen, successor.getStart());
		assertEquals(tuesdayThirteen, successor.getEnd());
		assertTrue(successor.getEnd() >= requiredFinish);
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(timestamp(2030, Calendar.JUNE, 3, 17), unrelated.getEnd());

		fixture.undo.undo();
		project.recalculate();
		assertTrue(predecessor.getSuccessorList().isEmpty());
		assertEquals(unlinkedStart, successor.getStart());
		assertEquals(unlinkedFinish, successor.getEnd());
		fixture.undo.redo();
		project.recalculate();
		assertSame(dependency, predecessor.getSuccessorList().findRight(successor));
		assertEquals(mondayThirteen, successor.getStart());
		assertEquals(tuesdayThirteen, successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(timestamp(2030, Calendar.JUNE, 3, 17), unrelated.getEnd());
	}

	@Test
	void startToFinishNegativeFiftyPercentLagUsesPredecessorDurationAndUndoRedo() throws Exception {
		Fixture fixture = createFixture();
		Project project = fixture.project;
		long mondayEight = timestamp(2030, Calendar.JUNE, 3, 8);
		long mondaySeventeen = timestamp(2030, Calendar.JUNE, 3, 17);
		long mondayThirteen = timestamp(2030, Calendar.JUNE, 3, 13);
		long tuesdayTwelve = timestamp(2030, Calendar.JUNE, 4, 12);
		long tuesdayThirteen = timestamp(2030, Calendar.JUNE, 4, 13);
		long wednesdayEight = timestamp(2030, Calendar.JUNE, 5, 8);
		long wednesdaySeventeen = timestamp(2030, Calendar.JUNE, 5, 17);
		long oneDay = CalendarOption.getInstance().getMillisPerDay();
		long negativeHalfDurationPercent = com.microproject.datatype.Duration.setAsPercent(
			com.microproject.datatype.Duration.setPercentAsDecimal(0L, -0.5F));
		// Microsoft documents SF endpoint ordering and negative lag as lead/overlap.
		// The percentage arithmetic and exact calendar boundary are document-derived,
		// not measured MSP behavior.
		// https://support.microsoft.com/en-us/project/link-tasks-in-a-project
		// https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task
		project.setStart(mondayEight);
		NormalTask predecessor = createTask(project, "Negative percent SF predecessor", 1);
		NormalTask successor = createTask(project, "Negative percent SF successor", 1);
		NormalTask unrelated = createTask(project, "Negative percent SF unrelated", 1);
		predecessor.setDuration(oneDay);
		successor.setDuration(oneDay);
		unrelated.setDuration(oneDay);
		predecessor.setManualDates(wednesdayEight, wednesdaySeventeen);
		successor.setManualDates(mondayEight, mondaySeventeen);
		unrelated.setManualDates(mondayEight, mondaySeventeen);
		successor.setManuallyScheduled(false);
		project.recalculate();
		long unlinkedStart = successor.getStart();
		long unlinkedFinish = successor.getEnd();
		fixture.undo.clear();
		Dependency dependency = DependencyService.getInstance().newDependency(
			predecessor, successor, DependencyType.SF, negativeHalfDurationPercent, this);
		project.recalculate();

		assertEquals(-oneDay / 2, com.microproject.datatype.Duration.millis(dependency.getLeadValue()),
			"negative SF percentage lag must be half the predecessor's duration");
		long requiredFinish = predecessor.getEffectiveWorkCalendar().add(
			predecessor.getStart(), dependency.getLeadValue(), false);
		assertEquals(tuesdayTwelve, requiredFinish,
			"SF lead boundary is half a workday before predecessor start");
		assertEquals(DependencyType.SF, dependency.getDependencyType());
		assertEquals(negativeHalfDurationPercent, dependency.getLag());
		assertTrue(com.microproject.datatype.Duration.isPercent(dependency.getLag()));
		assertEquals(-0.5F, com.microproject.datatype.Duration.getPercentAsDecimal(dependency.getLag()));
		assertEquals(wednesdayEight, predecessor.getStart());
		assertEquals(wednesdaySeventeen, predecessor.getEnd());
		assertTrue(successor.getStart() > unlinkedStart, "successor must move from its initial Monday schedule");
		assertEquals(mondayThirteen, successor.getStart());
		assertEquals(tuesdayThirteen, successor.getEnd());
		assertTrue(successor.getEnd() >= requiredFinish);
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());

		fixture.undo.undo();
		project.recalculate();
		assertTrue(predecessor.getSuccessorList().isEmpty());
		assertEquals(unlinkedStart, successor.getStart());
		assertEquals(unlinkedFinish, successor.getEnd());
		fixture.undo.redo();
		project.recalculate();
		assertSame(dependency, predecessor.getSuccessorList().findRight(successor));
		assertEquals(mondayThirteen, successor.getStart());
		assertEquals(tuesdayThirteen, successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());
	}

	@Test
	void finishToStartNegativeFiftyPercentLagUsesPredecessorDurationAndUndoRedo() throws Exception {
		Fixture fixture = createFixture();
		Project project = fixture.project;
		long mondayEight = timestamp(2030, Calendar.JUNE, 3, 8);
		long mondayTwelve = timestamp(2030, Calendar.JUNE, 3, 12);
		long mondayThirteen = timestamp(2030, Calendar.JUNE, 3, 13);
		long mondaySeventeen = timestamp(2030, Calendar.JUNE, 3, 17);
		long tuesdayThirteen = timestamp(2030, Calendar.JUNE, 4, 13);
		long oneDay = CalendarOption.getInstance().getMillisPerDay();
		long negativeHalfDurationPercent = com.microproject.datatype.Duration.setAsPercent(
			com.microproject.datatype.Duration.setPercentAsDecimal(0L, -0.5F));
		// Microsoft documents negative lag as lead/overlap. The 50% predecessor-
		// duration arithmetic and these exact dates are a document-derived decision,
		// not measured MSP behavior.
		// https://support.microsoft.com/en-us/project/link-tasks-in-a-project
		// https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task
		project.setStart(mondayEight);
		NormalTask predecessor = createTask(project, "Negative percent FS predecessor", 1);
		NormalTask successor = createTask(project, "Negative percent FS successor", 1);
		NormalTask unrelated = createTask(project, "Negative percent FS unrelated", 1);
		predecessor.setDuration(oneDay);
		successor.setDuration(oneDay);
		unrelated.setDuration(oneDay);
		predecessor.setManualDates(mondayEight, mondaySeventeen);
		successor.setManualDates(mondayEight, mondaySeventeen);
		unrelated.setManualDates(mondayEight, mondaySeventeen);
		successor.setManuallyScheduled(false);
		project.recalculate();
		long unlinkedStart = successor.getStart();
		long unlinkedFinish = successor.getEnd();
		fixture.undo.clear();
		Dependency dependency = DependencyService.getInstance().newDependency(
			predecessor, successor, DependencyType.FS, negativeHalfDurationPercent, this);
		project.recalculate();
		assertEquals(-oneDay / 2, com.microproject.datatype.Duration.millis(dependency.getLeadValue()),
			"negative percentage lag must be half the predecessor's duration");
		long requiredStart = dependency.calcForwardDependencyDate(
			predecessor.getStart(), predecessor.getEnd(), true);
		assertEquals(mondayTwelve, requiredStart,
			"negative FS boundary is the standard calendar's noon endpoint");
		assertEquals(DependencyType.FS, dependency.getDependencyType());
		assertEquals(negativeHalfDurationPercent, dependency.getLag());
		assertTrue(com.microproject.datatype.Duration.isPercent(dependency.getLag()));
		assertEquals(-0.5F, com.microproject.datatype.Duration.getPercentAsDecimal(dependency.getLag()));
		assertEquals(mondayEight, predecessor.getStart());
		assertEquals(mondaySeventeen, predecessor.getEnd());
		assertEquals(mondayThirteen, successor.getStart());
		assertTrue(successor.getStart() >= requiredStart,
			"the successor normalizes the noon boundary to the next working instant");
		assertEquals(tuesdayThirteen, successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());

		fixture.undo.undo();
		project.recalculate();
		assertTrue(predecessor.getSuccessorList().isEmpty());
		assertEquals(unlinkedStart, successor.getStart());
		assertEquals(unlinkedFinish, successor.getEnd());
		fixture.undo.redo();
		project.recalculate();
		assertSame(dependency, predecessor.getSuccessorList().findRight(successor));
		assertEquals(mondayThirteen, successor.getStart());
		assertEquals(tuesdayThirteen, successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());
	}

	@Test
	void startToStartElapsedFourHourLagUsesFixedWallClockDatesAndUndoRedo() throws Exception {
		Fixture fixture = createFixture();
		Project project = fixture.project;
		long mondayEight = timestamp(2030, Calendar.JUNE, 3, 8);
		long mondayTwelve = timestamp(2030, Calendar.JUNE, 3, 12);
		long mondayThirteen = timestamp(2030, Calendar.JUNE, 3, 13);
		long mondaySeventeen = timestamp(2030, Calendar.JUNE, 3, 17);
		long tuesdayThirteen = timestamp(2030, Calendar.JUNE, 4, 13);
		long fourElapsedHours = com.microproject.datatype.Duration.setAsElapsed(4L * 60L * 60L * 1000L);
		// Document-derived contract: Microsoft defines SS as a start-to-start
		// dependency and positive lag as delaying the successor. The exact four-hour
		// wall-clock arithmetic is this fixture's narrow inference, not measured MSP behavior.
		// https://support.microsoft.com/en-us/project/link-tasks-in-a-project
		// https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task
		project.setStart(mondayEight);
		NormalTask predecessor = createTask(project, "Elapsed SS predecessor", 1);
		NormalTask successor = createTask(project, "Elapsed SS successor", 1);
		NormalTask unrelated = createTask(project, "Elapsed SS unrelated", 1);
		predecessor.setManualDates(mondayEight, mondayTwelve);
		successor.setManualDates(mondayEight, mondaySeventeen);
		unrelated.setManualDates(mondayEight, mondaySeventeen);
		successor.setManuallyScheduled(false);
		project.recalculate();
		long unlinkedSuccessorStart = successor.getStart();
		long unlinkedSuccessorFinish = successor.getEnd();
		fixture.undo.clear();
		Dependency dependency = DependencyService.getInstance().newDependency(
			predecessor, successor, DependencyType.SS, fourElapsedHours, this);
		project.recalculate();

		assertEquals(DependencyType.SS, dependency.getDependencyType());
		assertEquals(fourElapsedHours, dependency.getLag());
		assertTrue(com.microproject.datatype.Duration.isElapsed(dependency.getLag()));
		assertEquals(mondayEight, predecessor.getStart());
		assertEquals(mondayTwelve, predecessor.getEnd());
		assertEquals(mondayThirteen, successor.getStart());
		assertEquals(tuesdayThirteen, successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());

		fixture.undo.undo();
		project.recalculate();
		assertTrue(predecessor.getSuccessorList().isEmpty());
		assertEquals(unlinkedSuccessorStart, successor.getStart());
		assertEquals(unlinkedSuccessorFinish, successor.getEnd());
		fixture.undo.redo();
		project.recalculate();
		assertSame(dependency, predecessor.getSuccessorList().findRight(successor));
		assertEquals(mondayThirteen, successor.getStart());
		assertEquals(tuesdayThirteen, successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());
	}

	@Test
	void finishToFinishElapsedFourHourLagUsesFixedWallClockDatesAndUndoRedo() throws Exception {
		Fixture fixture = createFixture();
		Project project = fixture.project;
		long mondayEight = timestamp(2030, Calendar.JUNE, 3, 8);
		long mondayTwelve = timestamp(2030, Calendar.JUNE, 3, 12);
		long mondayFourteen = timestamp(2030, Calendar.JUNE, 3, 14);
		long mondaySixteen = timestamp(2030, Calendar.JUNE, 3, 16);
		long mondaySeventeen = timestamp(2030, Calendar.JUNE, 3, 17);
		long fourElapsedHours = com.microproject.datatype.Duration.setAsElapsed(4L * 60L * 60L * 1000L);
		// Microsoft documents FF endpoint semantics and positive lag delay; the exact
		// elapsed-hour boundary arithmetic below is document-derived, not measured.
		// https://support.microsoft.com/en-us/project/link-tasks-in-a-project
		// https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task
		project.setStart(mondayEight);
		NormalTask predecessor = createTask(project, "Elapsed FF predecessor", 1);
		NormalTask successor = createTask(project, "Elapsed FF successor", 1);
		NormalTask unrelated = createTask(project, "Elapsed FF unrelated", 1);
		successor.setDuration(day() / 4);
		predecessor.setManualDates(mondayEight, mondayTwelve);
		successor.setManualDates(mondayEight, mondayTwelve);
		unrelated.setManualDates(mondayEight, mondaySeventeen);
		successor.setManuallyScheduled(false);
		project.recalculate();
		long unlinkedSuccessorStart = successor.getStart();
		long unlinkedSuccessorFinish = successor.getEnd();
		fixture.undo.clear();
		Dependency dependency = DependencyService.getInstance().newDependency(
			predecessor, successor, DependencyType.FF, fourElapsedHours, this);
		project.recalculate();

		assertEquals(DependencyType.FF, dependency.getDependencyType());
		assertEquals(fourElapsedHours, dependency.getLag());
		assertTrue(com.microproject.datatype.Duration.isElapsed(dependency.getLag()));
		assertEquals(mondayEight, predecessor.getStart());
		assertEquals(mondayTwelve, predecessor.getEnd());
		assertEquals(mondayFourteen, successor.getStart());
		assertEquals(mondaySixteen, successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());

		fixture.undo.undo();
		project.recalculate();
		assertTrue(predecessor.getSuccessorList().isEmpty());
		assertEquals(unlinkedSuccessorStart, successor.getStart());
		assertEquals(unlinkedSuccessorFinish, successor.getEnd());
		fixture.undo.redo();
		project.recalculate();
		assertSame(dependency, predecessor.getSuccessorList().findRight(successor));
		assertEquals(mondayFourteen, successor.getStart());
		assertEquals(mondaySixteen, successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());
	}

	@Test
	void startToFinishElapsedFourHourLagUsesFixedWallClockDatesAndUndoRedo() throws Exception {
		Fixture fixture = createFixture();
		Project project = fixture.project;
		long mondayEight = timestamp(2030, Calendar.JUNE, 3, 8);
		long mondayTen = timestamp(2030, Calendar.JUNE, 3, 10);
		long mondayTwelve = timestamp(2030, Calendar.JUNE, 3, 12);
		long mondayThirteen = timestamp(2030, Calendar.JUNE, 3, 13);
		long mondaySeventeen = timestamp(2030, Calendar.JUNE, 3, 17);
		long fourElapsedHours = com.microproject.datatype.Duration.setAsElapsed(4L * 60L * 60L * 1000L);
		// Microsoft documents SF endpoint semantics and positive lag delaying the
		// successor; the exact elapsed-hour boundary below is document-derived, not measured.
		// https://support.microsoft.com/en-us/project/link-tasks-in-a-project
		// https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task
		project.setStart(mondayEight);
		NormalTask predecessor = createTask(project, "Elapsed SF predecessor", 1);
		NormalTask successor = createTask(project, "Elapsed SF successor", 1);
		NormalTask unrelated = createTask(project, "Elapsed SF unrelated", 1);
		successor.setDuration(day() / 4);
		predecessor.setManualDates(mondayEight, mondayTwelve);
		successor.setManualDates(mondayEight, mondayTen);
		unrelated.setManualDates(mondayEight, mondaySeventeen);
		successor.setManuallyScheduled(false);
		project.recalculate();
		long unlinkedSuccessorStart = successor.getStart();
		long unlinkedSuccessorFinish = successor.getEnd();
		fixture.undo.clear();
		Dependency dependency = DependencyService.getInstance().newDependency(
			predecessor, successor, DependencyType.SF, fourElapsedHours, this);
		project.recalculate();

		assertEquals(DependencyType.SF, dependency.getDependencyType());
		assertEquals(fourElapsedHours, dependency.getLag());
		assertTrue(com.microproject.datatype.Duration.isElapsed(dependency.getLag()));
		assertEquals(mondayEight, predecessor.getStart());
		assertEquals(mondayTwelve, predecessor.getEnd());
		assertEquals(mondayTwelve, predecessor.getStart()
			+ com.microproject.datatype.Duration.millis(dependency.getLag()),
			"SF constrains the successor finish from predecessor start plus elapsed lag");
		assertEquals(mondayTen, successor.getStart());
		assertEquals(mondayThirteen, successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());

		fixture.undo.undo();
		project.recalculate();
		assertTrue(predecessor.getSuccessorList().isEmpty());
		assertEquals(unlinkedSuccessorStart, successor.getStart());
		assertEquals(unlinkedSuccessorFinish, successor.getEnd());
		fixture.undo.redo();
		project.recalculate();
		assertSame(dependency, predecessor.getSuccessorList().findRight(successor));
		assertEquals(mondayTen, successor.getStart());
		assertEquals(mondayThirteen, successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());
	}

	@Test
	void startToFinishNegativeElapsedFourHourLagUsesFixedWallClockDatesAndUndoRedo() throws Exception {
		Fixture fixture = createFixture();
		Project project = fixture.project;
		long mondayEight = timestamp(2030, Calendar.JUNE, 3, 8);
		long mondayTwelve = timestamp(2030, Calendar.JUNE, 3, 12);
		long mondaySeventeen = timestamp(2030, Calendar.JUNE, 3, 17);
		long tuesdayFour = timestamp(2030, Calendar.JUNE, 4, 4);
		long tuesdayEight = timestamp(2030, Calendar.JUNE, 4, 8);
		long tuesdayEleven = timestamp(2030, Calendar.JUNE, 4, 11);
		long tuesdayFourteen = timestamp(2030, Calendar.JUNE, 4, 14);
		long fourElapsedHours = com.microproject.datatype.Duration.setAsElapsed(4L * 60L * 60L * 1000L);
		// Document-derived: Microsoft defines SF endpoint ordering and negative lag as lead/overlap.
		// The exact predecessor-start-minus-four-wall-clock-hours boundary and calendar normalization
		// below are inferred, not an MSP measurement.
		// https://support.microsoft.com/en-us/project/link-tasks-in-a-project
		// https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task
		project.setStart(mondayEight);
		NormalTask predecessor = createTask(project, "Negative elapsed SF predecessor", 1);
		NormalTask successor = createTask(project, "Negative elapsed SF successor", 1);
		NormalTask unrelated = createTask(project, "Negative elapsed SF unrelated", 1);
		predecessor.setManualDates(tuesdayEight, tuesdayFourteen);
		predecessor.setDuration(5L * 60L * 60L * 1000L);
		successor.setManualDates(mondayEight, mondaySeventeen);
		successor.setDuration(3L * 60L * 60L * 1000L);
		unrelated.setManualDates(mondayEight, mondaySeventeen);
		successor.setManuallyScheduled(false);
		project.recalculate();
		long unlinkedStart = successor.getStart();
		long unlinkedFinish = successor.getEnd();
		fixture.undo.clear();
		Dependency dependency = DependencyService.getInstance().newDependency(
			predecessor, successor, DependencyType.SF, -fourElapsedHours, this);
		project.recalculate();

		assertEquals(DependencyType.SF, dependency.getDependencyType());
		assertEquals(-fourElapsedHours, dependency.getLag());
		assertTrue(com.microproject.datatype.Duration.isElapsed(dependency.getLag()));
		assertEquals(tuesdayFour, predecessor.getStart() + com.microproject.datatype.Duration.millis(dependency.getLag()),
			"negative elapsed SF boundary is predecessor start minus four wall-clock hours");
		assertEquals(tuesdayEight, successor.getStart(), "non-working finish boundary normalizes to Tuesday 08:00");
		assertEquals(tuesdayEleven, successor.getEnd());
		assertTrue(successor.getStart() > unlinkedStart, "successor must move later to satisfy its finish boundary");
		assertEquals(tuesdayEight, predecessor.getStart());
		assertEquals(tuesdayFourteen, predecessor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());

		fixture.undo.undo();
		project.recalculate();
		assertTrue(predecessor.getSuccessorList().isEmpty());
		assertEquals(unlinkedStart, successor.getStart());
		assertEquals(unlinkedFinish, successor.getEnd());
		fixture.undo.redo();
		project.recalculate();
		assertSame(dependency, predecessor.getSuccessorList().findRight(successor));
		assertEquals(tuesdayEight, successor.getStart());
		assertEquals(tuesdayEleven, successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());
	}

	@Test
	void finishToStartTwelveHourLeadRespectsSuccessorSnetAcrossDatedCalendarException() throws Exception {
		Fixture fixture = createFixture();
		Project project = fixture.project;
		long mondayEight = timestamp(2030, Calendar.JUNE, 3, 8);
		long mondaySeventeen = timestamp(2030, Calendar.JUNE, 3, 17);
		long tuesdayMidnight = timestamp(2030, Calendar.JUNE, 4, 0);
		long thursdayTwelve = timestamp(2030, Calendar.JUNE, 6, 12);
		long fridayEight = timestamp(2030, Calendar.JUNE, 7, 8);
		long fridaySeventeen = timestamp(2030, Calendar.JUNE, 7, 17);
		long thursdayThirteen = timestamp(2030, Calendar.JUNE, 6, 13);
		long fridayThirteen = timestamp(2030, Calendar.JUNE, 7, 13);
		long twelveHours = 12L * 60L * 60L * 1000L;
		project.setStart(mondayEight);
		((WorkingCalendar) project.getWorkCalendar()).addOrReplaceException(new WorkDay(tuesdayMidnight));

		NormalTask predecessor = createTask(project, "FS lead predecessor", 1);
		NormalTask successor = createTask(project, "FS lead successor", 1);
		NormalTask unrelated = createTask(project, "FS lead unrelated", 1);
		predecessor.setScheduleConstraint(ConstraintType.SNET, fridayEight);
		successor.setScheduleConstraint(ConstraintType.SNET, mondayEight);
		unrelated.setScheduleConstraint(ConstraintType.SNET, mondayEight);
		project.recalculate();
		assertEquals(fridayEight, predecessor.getStart());
		assertEquals(fridaySeventeen, predecessor.getEnd());
		assertEquals(mondayEight, successor.getStart());
		assertEquals(mondaySeventeen, successor.getEnd());

		// Document-derived inference, not measured MSP behavior: Microsoft documents FS
		// endpoints, working calendars, and negative lag as lead/overlap, but not lag-calendar
		// ownership or exception-boundary arithmetic. Under the established predecessor-effective
		// calendar policy, the existing calendar arithmetic places the lead boundary at
		// Thursday 12:00 across Tuesday's exception; successor normalization places it at 13:00.
		// https://support.microsoft.com/en-us/project/link-tasks-in-a-project
		// https://support.microsoft.com/en-us/project/how-project-schedules-tasks-behind-the-scenes
		// https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task
		Dependency dependency = DependencyService.getInstance().newDependency(
			predecessor, successor, DependencyType.FS, -twelveHours, this);
		assertEquals(thursdayTwelve,
			dependency.getEffectiveWorkCalendar().add(fridaySeventeen, dependency.getLeadValue(), false));
		project.recalculate();

		assertEquals(DependencyType.FS, dependency.getDependencyType());
		assertEquals(-twelveHours, dependency.getLag());
		assertEquals(fridayEight, predecessor.getStart());
		assertEquals(fridaySeventeen, predecessor.getEnd());
		assertEquals(thursdayThirteen, successor.getStart());
		assertEquals(fridayThirteen, successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());

		fixture.undo.undo();
		project.recalculate();
		assertTrue(predecessor.getSuccessorList().isEmpty());
		assertEquals(mondayEight, successor.getStart());
		assertEquals(mondaySeventeen, successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		fixture.undo.redo();
		project.recalculate();
		assertSame(dependency, predecessor.getSuccessorList().findRight(successor));
		assertEquals(thursdayThirteen, successor.getStart());
		assertEquals(fridayThirteen, successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());
	}

	@Test
	void finishToStartWorkingDayLagSkipsDatedNonworkingException() throws Exception {
		Fixture fixture = createFixture();
		Project project = fixture.project;
		long mondayEight = timestamp(2030, Calendar.JUNE, 3, 8);
		long mondayThirteen = timestamp(2030, Calendar.JUNE, 3, 13);
		long mondaySeventeen = timestamp(2030, Calendar.JUNE, 3, 17);
		long tuesdayMidnight = timestamp(2030, Calendar.JUNE, 4, 0);
		long tuesdayEight = timestamp(2030, Calendar.JUNE, 4, 8);
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

	@Test
	void startToFinishWorkingDayLagRespectsSuccessorSnetAcrossProjectException() throws Exception {
		Fixture fixture = createFixture();
		Project project = fixture.project;
		long mondayEight = timestamp(2030, Calendar.JUNE, 3, 8);
		long mondayThirteen = timestamp(2030, Calendar.JUNE, 3, 13);
		long mondaySeventeen = timestamp(2030, Calendar.JUNE, 3, 17);
		long tuesdayMidnight = timestamp(2030, Calendar.JUNE, 4, 0);
		long wednesdayEight = timestamp(2030, Calendar.JUNE, 5, 8);
		long wednesdayThirteen = timestamp(2030, Calendar.JUNE, 5, 13);
		long fourHours = day() / 2;
		project.setStart(mondayEight);
		((WorkingCalendar) project.getWorkCalendar()).addOrReplaceException(new WorkDay(tuesdayMidnight));

		NormalTask predecessor = createTask(project, "SF lag predecessor", 1);
		NormalTask successor = createTask(project, "SF lag successor", 1);
		NormalTask unrelated = createTask(project, "SF lag unrelated", 1);
		predecessor.setDuration(fourHours);
		successor.setDuration(fourHours);
		unrelated.setDuration(fourHours);
		predecessor.setScheduleConstraint(ConstraintType.SNET, mondayThirteen);
		successor.setScheduleConstraint(ConstraintType.SNET, mondayEight);
		unrelated.setScheduleConstraint(ConstraintType.SNET, mondayEight);
		project.recalculate();
		assertEquals(mondayThirteen, predecessor.getStart());
		assertEquals(mondaySeventeen, predecessor.getEnd());
		assertEquals(mondayEight, successor.getStart());
		assertEquals(mondayThirteen, successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondayThirteen, unrelated.getEnd());

		// Document-derived product rule, not measured MSP behavior: Microsoft documents
		// SF endpoint semantics, working calendars, and lead/lag, but not which calendar
		// owns lag. This fixture retains the established predecessor-effective-calendar
		// policy. One working day from Monday 13:00 crosses Tuesday's project exception
		// and reaches Wednesday 13:00; the successor's Monday SNET remains its lower bound.
		// Sources: https://support.microsoft.com/en-us/project/link-tasks-in-a-project
		// https://support.microsoft.com/en-us/project/how-project-schedules-tasks-behind-the-scenes
		// https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task
		Dependency dependency = DependencyService.getInstance().newDependency(
			predecessor, successor, DependencyType.SF, day(), this);
		project.recalculate();
		assertEquals(wednesdayThirteen,
			dependency.getEffectiveWorkCalendar().add(mondayThirteen, day(), false));
		assertEquals(DependencyType.SF, dependency.getDependencyType());
		assertEquals(day(), dependency.getLag());
		assertEquals(mondayThirteen, predecessor.getStart());
		assertEquals(mondaySeventeen, predecessor.getEnd());
		assertEquals(wednesdayEight, successor.getStart());
		assertEquals(timestamp(2030, Calendar.JUNE, 5, 13), successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondayThirteen, unrelated.getEnd());

		fixture.undo.undo();
		project.recalculate();
		assertTrue(predecessor.getSuccessorList().isEmpty());
		assertEquals(mondayEight, successor.getStart());
		assertEquals(mondayThirteen, successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		fixture.undo.redo();
		project.recalculate();
		assertSame(dependency, predecessor.getSuccessorList().findRight(successor));
		assertEquals(wednesdayEight, successor.getStart());
		assertEquals(timestamp(2030, Calendar.JUNE, 5, 13), successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondayThirteen, unrelated.getEnd());
	}

	@Test
	void finishToFinishTwelveHourLeadUsesBoundaryAcrossDatedCalendarException() throws Exception {
		Fixture fixture = createFixture();
		Project project = fixture.project;
		long mondayEight = timestamp(2030, Calendar.JUNE, 3, 8);
		long mondaySeventeen = timestamp(2030, Calendar.JUNE, 3, 17);
		long tuesdayMidnight = timestamp(2030, Calendar.JUNE, 4, 0);
		long wednesdayEight = timestamp(2030, Calendar.JUNE, 5, 8);
		long wednesdaySeventeen = timestamp(2030, Calendar.JUNE, 5, 17);
		project.setStart(mondayEight);
		((WorkingCalendar) project.getWorkCalendar()).addOrReplaceException(new WorkDay(tuesdayMidnight));

		NormalTask predecessor = createTask(project, "FF lead predecessor", 1);
		NormalTask successor = createTask(project, "FF lead successor", 1);
		NormalTask unrelated = createTask(project, "unrelated", 1);
		predecessor.setScheduleConstraint(ConstraintType.SNET, wednesdayEight);
		successor.setScheduleConstraint(ConstraintType.SNET, mondayEight);
		unrelated.setScheduleConstraint(ConstraintType.SNET, mondayEight);
		project.recalculate();
		assertEquals(wednesdayEight, predecessor.getStart());
		assertEquals(wednesdaySeventeen, predecessor.getEnd());

		// Document-derived compatibility inference, not an MSP experiment: Microsoft defines
		// negative lag as lead/overlap. The inferred 12h backward working-time calculation from
		// Wednesday 17:00 crosses the Tuesday nonworking project-calendar exception and lands
		// Monday 12:00, allowing the one-day successor to finish Monday 17:00. Microsoft does not
		// document this calendar-boundary detail.
		// https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task
		Dependency dependency = DependencyService.getInstance().newDependency(
			predecessor, successor, DependencyType.FF, -day() - day() / 2, this);
		assertEquals(timestamp(2030, Calendar.JUNE, 3, 12),
			dependency.getEffectiveWorkCalendar().add(wednesdaySeventeen, dependency.getLeadValue(), false),
			"the 12h lead crosses the Tuesday exception and reaches Monday 12:00");
		project.recalculate();

		assertEquals(DependencyType.FF, dependency.getDependencyType());
		assertEquals(-day() - day() / 2, dependency.getLag());
		assertEquals(wednesdayEight, predecessor.getStart());
		assertEquals(wednesdaySeventeen, predecessor.getEnd());
		assertEquals(mondayEight, successor.getStart());
		assertEquals(mondaySeventeen, successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());
	}

	@Test
	void finishToFinishTwelveHourLeadRespectsSuccessorSnetAcrossDatedCalendarException() throws Exception {
		Fixture fixture = createFixture();
		Project project = fixture.project;
		long mondayEight = timestamp(2030, Calendar.JUNE, 3, 8);
		long mondayTen = timestamp(2030, Calendar.JUNE, 3, 10);
		long mondaySeventeen = timestamp(2030, Calendar.JUNE, 3, 17);
		long tuesdayMidnight = timestamp(2030, Calendar.JUNE, 4, 0);
		long mondayTwelve = timestamp(2030, Calendar.JUNE, 3, 12);
		long mondayThirteen = timestamp(2030, Calendar.JUNE, 3, 13);
		long wednesdayEight = timestamp(2030, Calendar.JUNE, 5, 8);
		long wednesdaySeventeen = timestamp(2030, Calendar.JUNE, 5, 17);
		long twelveHours = 12L * 60L * 60L * 1000L;
		long twoHours = 2L * 60L * 60L * 1000L;
		project.setStart(mondayEight);
		((WorkingCalendar) project.getWorkCalendar()).addOrReplaceException(new WorkDay(tuesdayMidnight));

		NormalTask predecessor = createTask(project, "FF lead predecessor SNET", 1);
		NormalTask successor = createTask(project, "FF lead successor SNET", 1);
		NormalTask unrelated = createTask(project, "FF lead unrelated SNET", 1);
		successor.setDuration(twoHours);
		predecessor.setScheduleConstraint(ConstraintType.SNET, wednesdayEight);
		successor.setScheduleConstraint(ConstraintType.SNET, mondayEight);
		unrelated.setScheduleConstraint(ConstraintType.SNET, mondayEight);
		project.recalculate();
		assertEquals(wednesdayEight, predecessor.getStart());
		assertEquals(wednesdaySeventeen, predecessor.getEnd());
		assertEquals(mondayEight, successor.getStart());
		assertEquals(mondayTen, successor.getEnd());

		// Document-derived inference, not measured MSP behavior: Microsoft documents FF
		// endpoints, calendar scheduling, and negative lag as lead/overlap. The precise
		// exception-boundary calculation uses the existing predecessor-calendar policy.
		// Subtracting 12 work hours from Wednesday 17:00 crosses Tuesday's exception
		// and yields Monday 12:00. The two-hour successor's Monday SNET permits 08:00–10:00.
		// Its 10:00–12:00 working interval is represented with a 13:00 finish timestamp
		// because noon–13:00 is nonworking; this satisfies the required 12:00 finish boundary.
		// https://support.microsoft.com/en-us/project/link-tasks-in-a-project
		// https://support.microsoft.com/en-us/project/how-project-schedules-tasks-behind-the-scenes
		// https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task
		Dependency dependency = DependencyService.getInstance().newDependency(
			predecessor, successor, DependencyType.FF, -twelveHours, this);
		assertEquals(mondayTwelve,
			dependency.getEffectiveWorkCalendar().add(wednesdaySeventeen, dependency.getLeadValue(), false));
		project.recalculate();

		assertEquals(DependencyType.FF, dependency.getDependencyType());
		assertEquals(-twelveHours, dependency.getLag());
		assertEquals(ConstraintType.Kind.SNET, successor.getConstraintTypeKind());
		assertEquals(mondayEight, successor.getConstraintDate());
		assertEquals(wednesdayEight, predecessor.getStart());
		assertEquals(wednesdaySeventeen, predecessor.getEnd());
		assertEquals(mondayTen, successor.getStart());
		assertEquals(mondayThirteen, successor.getEnd(), "FF successor finish meets the required Monday 12:00 boundary");
		assertTrue(successor.getEnd() >= mondayTwelve);
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());

		fixture.undo.undo();
		project.recalculate();
		assertTrue(predecessor.getSuccessorList().isEmpty());
		assertEquals(mondayEight, successor.getStart());
		assertEquals(mondayTen, successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		fixture.undo.redo();
		project.recalculate();
		assertSame(dependency, predecessor.getSuccessorList().findRight(successor));
		assertEquals(mondayTen, successor.getStart());
		assertEquals(mondayThirteen, successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());
	}

	@Test
	void startToStartTwelveHourLeadRespectsSuccessorSnetAcrossDatedCalendarException() throws Exception {
		Fixture fixture = createFixture();
		Project project = fixture.project;
		long mondayEight = timestamp(2030, Calendar.JUNE, 3, 8);
		long mondaySeventeen = timestamp(2030, Calendar.JUNE, 3, 17);
		long tuesdayMidnight = timestamp(2030, Calendar.JUNE, 4, 0);
		long wednesdayEight = timestamp(2030, Calendar.JUNE, 5, 8);
		long wednesdayTwelve = timestamp(2030, Calendar.JUNE, 5, 12);
		long wednesdayThirteen = timestamp(2030, Calendar.JUNE, 5, 13);
		long thursdayThirteen = timestamp(2030, Calendar.JUNE, 6, 13);
		long wednesdaySeventeen = timestamp(2030, Calendar.JUNE, 5, 17);
		long fridayEight = timestamp(2030, Calendar.JUNE, 7, 8);
		long fridaySeventeen = timestamp(2030, Calendar.JUNE, 7, 17);
		long twelveHours = 12L * 60L * 60L * 1000L;
		project.setStart(mondayEight);
		((WorkingCalendar) project.getWorkCalendar()).addOrReplaceException(new WorkDay(tuesdayMidnight));

		NormalTask predecessor = createTask(project, "SS lead predecessor", 1);
		NormalTask successor = createTask(project, "SS lead successor", 1);
		NormalTask unrelated = createTask(project, "SS lead unrelated", 1);
		predecessor.setScheduleConstraint(ConstraintType.SNET, fridayEight);
		successor.setScheduleConstraint(ConstraintType.SNET, mondayEight);
		unrelated.setScheduleConstraint(ConstraintType.SNET, mondayEight);
		project.recalculate();
		assertEquals(fridayEight, predecessor.getStart());
		assertEquals(fridaySeventeen, predecessor.getEnd());
		assertEquals(mondayEight, successor.getStart());
		assertEquals(mondaySeventeen, successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());

		// Document-derived inference, not measured MSP behavior: Microsoft's published
		// guidance defines SS endpoints, working calendars, and negative lag as lead, but
		// does not specify lag-calendar ownership or this exception-boundary calculation.
		// Keep the established predecessor-effective-calendar policy. Subtracting 12
		// working hours backward from Friday 08:00 crosses Tuesday's exception to
		// Wednesday 12:00. Its earlier SNET remains a lower bound and does not override
		// the later link boundary; successor normalization places it at Wednesday 13:00.
		// https://support.microsoft.com/en-us/project/link-tasks-in-a-project
		// https://support.microsoft.com/en-us/project/how-project-schedules-tasks-behind-the-scenes
		// https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task
		Dependency dependency = DependencyService.getInstance().newDependency(
			predecessor, successor, DependencyType.SS, -twelveHours, this);
		assertEquals(wednesdayTwelve,
			dependency.getEffectiveWorkCalendar().add(fridayEight, dependency.getLeadValue(), false));
		project.recalculate();

		assertEquals(DependencyType.SS, dependency.getDependencyType());
		assertEquals(-twelveHours, dependency.getLag());
		assertEquals(fridayEight, predecessor.getStart());
		assertEquals(fridaySeventeen, predecessor.getEnd());
		assertEquals(wednesdayThirteen, successor.getStart());
		assertEquals(thursdayThirteen, successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());

		fixture.undo.undo();
		project.recalculate();
		assertTrue(predecessor.getSuccessorList().isEmpty());
		assertEquals(mondayEight, successor.getStart());
		assertEquals(mondaySeventeen, successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());
		fixture.undo.redo();
		project.recalculate();
		assertSame(dependency, predecessor.getSuccessorList().findRight(successor));
		assertEquals(wednesdayThirteen, successor.getStart());
		assertEquals(thursdayThirteen, successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());
	}

	@Test
	void startToFinishTwelveHourLeadRespectsSuccessorSnetAcrossDatedCalendarException() throws Exception {
		Fixture fixture = createFixture();
		Project project = fixture.project;
		long mondayEight = timestamp(2030, Calendar.JUNE, 3, 8);
		long mondayThirteen = timestamp(2030, Calendar.JUNE, 3, 13);
		long mondaySeventeen = timestamp(2030, Calendar.JUNE, 3, 17);
		long tuesdayMidnight = timestamp(2030, Calendar.JUNE, 4, 0);
		long wednesdayTwelve = timestamp(2030, Calendar.JUNE, 5, 12);
		long wednesdayThirteen = timestamp(2030, Calendar.JUNE, 5, 13);
		long thursdayThirteen = timestamp(2030, Calendar.JUNE, 6, 13);
		long fridayEight = timestamp(2030, Calendar.JUNE, 7, 8);
		long fridaySeventeen = timestamp(2030, Calendar.JUNE, 7, 17);
		long twelveHours = 12L * 60L * 60L * 1000L;
		project.setStart(mondayEight);
		((WorkingCalendar) project.getWorkCalendar()).addOrReplaceException(new WorkDay(tuesdayMidnight));

		NormalTask predecessor = createTask(project, "SF lead predecessor", 1);
		NormalTask successor = createTask(project, "SF lead successor", 1);
		NormalTask unrelated = createTask(project, "SF lead unrelated", 1);
		predecessor.setScheduleConstraint(ConstraintType.SNET, fridayEight);
		successor.setScheduleConstraint(ConstraintType.SNET, mondayEight);
		unrelated.setScheduleConstraint(ConstraintType.SNET, mondayEight);
		project.recalculate();
		assertEquals(fridayEight, predecessor.getStart());
		assertEquals(fridaySeventeen, predecessor.getEnd());
		assertEquals(mondayEight, successor.getStart());
		assertEquals(mondaySeventeen, successor.getEnd());

		// Document-derived inference, not measured MSP behavior: Microsoft documents SF
		// endpoints, project calendars, and negative lag as lead/overlap, but not lag-calendar
		// ownership or exception-boundary arithmetic. With the existing predecessor-effective
		// calendar policy, 12 working hours backward from Friday 08:00 crosses Tuesday's
		// exception and reaches Wednesday 12:00. SF constrains the successor finish, so its
		// one-day duration starts Monday 13:00 and ends Wednesday 13:00 across the exception.
		// https://support.microsoft.com/en-us/project/link-tasks-in-a-project
		// https://support.microsoft.com/en-us/project/how-project-schedules-tasks-behind-the-scenes
		// https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task
		Dependency dependency = DependencyService.getInstance().newDependency(
			predecessor, successor, DependencyType.SF, -twelveHours, this);
		assertEquals(wednesdayTwelve,
			dependency.getEffectiveWorkCalendar().add(fridayEight, dependency.getLeadValue(), false));
		project.recalculate();

		assertEquals(DependencyType.SF, dependency.getDependencyType());
		assertEquals(-twelveHours, dependency.getLag());
		assertEquals(fridayEight, predecessor.getStart());
		assertEquals(fridaySeventeen, predecessor.getEnd());
		assertEquals(mondayThirteen, successor.getStart());
		assertEquals(wednesdayThirteen, successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());

		fixture.undo.undo();
		project.recalculate();
		assertTrue(predecessor.getSuccessorList().isEmpty());
		assertEquals(mondayEight, successor.getStart());
		assertEquals(mondaySeventeen, successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());
		fixture.undo.redo();
		project.recalculate();
		assertSame(dependency, predecessor.getSuccessorList().findRight(successor));
		assertEquals(mondayThirteen, successor.getStart());
		assertEquals(wednesdayThirteen, successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());
	}

	@Test
	void zeroLagFinishToStartUsesSuccessorTaskCalendarException() throws Exception {
		Fixture fixture = createFixture();
		Project project = fixture.project;
		long tuesdayEight = timestamp(2026, Calendar.NOVEMBER, 3, 8);
		long tuesdaySeventeen = timestamp(2026, Calendar.NOVEMBER, 3, 17);
		long wednesdayMidnight = timestamp(2026, Calendar.NOVEMBER, 4, 0);
		long thursdayEight = timestamp(2026, Calendar.NOVEMBER, 5, 8);
		long thursdaySeventeen = timestamp(2026, Calendar.NOVEMBER, 5, 17);
		project.setStart(tuesdayEight);

		NormalTask predecessor = createTask(project, "successor-calendar predecessor", 1);
		NormalTask successor = createTask(project, "successor-calendar successor", 1);
		NormalTask unrelated = createTask(project, "unrelated task", 1);
		predecessor.setScheduleConstraint(ConstraintType.SNET, tuesdayEight);
		successor.setScheduleConstraint(ConstraintType.SNET, tuesdayEight);
		unrelated.setScheduleConstraint(ConstraintType.SNET, tuesdayEight);
		WorkingCalendar successorCalendar = WorkingCalendar.getInstanceBasedOn(project.getWorkCalendar());
		successorCalendar.setName("Successor Wednesday exception");
		successorCalendar.addOrReplaceException(new WorkDay(wednesdayMidnight));
		successor.setWorkCalendar(successorCalendar);
		project.recalculate();
		assertEquals(tuesdaySeventeen, predecessor.getEnd());
		assertEquals(tuesdaySeventeen, unrelated.getEnd());

		Dependency dependency = DependencyService.getInstance().newDependency(
			predecessor, successor, DependencyType.FS, 0L, this);
		project.recalculate();

		// Document-derived compatibility decision, not an empirical MSP observation:
		// FS starts the successor at the predecessor's finish boundary, then the
		// successor task calendar determines its next working time. Its Wednesday
		// exception therefore moves the successor to Thursday 08:00. Microsoft documents
		// FS boundary/task-calendar scheduling and task calendars superseding project
		// calendar working times; the standard day runs 08:00-12:00 and 13:00-17:00.
		// https://support.microsoft.com/en-us/project/how-project-schedules-tasks-behind-the-scenes
		// https://support.microsoft.com/en-us/project/task-calendar-task-field
		assertEquals(0L, dependency.getLag());
		assertEquals(tuesdaySeventeen, predecessor.getEnd());
		assertEquals(thursdayEight, successor.getStart());
		assertEquals(thursdaySeventeen, successor.getEnd());
		assertEquals(tuesdayEight, unrelated.getStart());
		assertEquals(tuesdaySeventeen, unrelated.getEnd());
	}

	@TestFactory
	Stream<DynamicTest> startNoEarlierThanConstraintAndFinishToStartLinkUseLaterBoundary() {
		return Stream.of(
			DynamicTest.dynamicTest("SNET-FS-predecessor-finishes-before-constraint", () ->
				verifySnetAndFinishBoundary(DependencyType.FS, 1, timestamp(2030, Calendar.JUNE, 4, 8),
					timestamp(2030, Calendar.JUNE, 4, 17), timestamp(2030, Calendar.JUNE, 3, 17),
					timestamp(2030, Calendar.JUNE, 4, 8), timestamp(2030, Calendar.JUNE, 4, 17))),
			DynamicTest.dynamicTest("SNET-FS-predecessor-finishes-after-constraint", () ->
				verifySnetAndFinishBoundary(DependencyType.FS, 2, timestamp(2030, Calendar.JUNE, 4, 8),
					timestamp(2030, Calendar.JUNE, 4, 17), timestamp(2030, Calendar.JUNE, 4, 17),
					timestamp(2030, Calendar.JUNE, 5, 8), timestamp(2030, Calendar.JUNE, 5, 17))));
	}

	@TestFactory
	Stream<DynamicTest> startNoEarlierThanConstraintAndFinishToFinishLinkUseLaterBoundary() {
		return Stream.of(
			DynamicTest.dynamicTest("SNET-FF-predecessor-finishes-before-constraint", () ->
				verifySnetAndFinishBoundary(DependencyType.FF, 1, timestamp(2030, Calendar.JUNE, 4, 8),
					timestamp(2030, Calendar.JUNE, 4, 17), timestamp(2030, Calendar.JUNE, 3, 17),
					timestamp(2030, Calendar.JUNE, 4, 8), timestamp(2030, Calendar.JUNE, 4, 17))),
			DynamicTest.dynamicTest("SNET-FF-predecessor-finishes-after-constraint", () ->
				verifySnetAndFinishBoundary(DependencyType.FF, 2, timestamp(2030, Calendar.JUNE, 3, 8),
					timestamp(2030, Calendar.JUNE, 3, 17), timestamp(2030, Calendar.JUNE, 4, 17),
					timestamp(2030, Calendar.JUNE, 4, 8), timestamp(2030, Calendar.JUNE, 4, 17))));
	}

	private void verifySnetAndFinishBoundary(int type, int predecessorDurationDays, long snet,
		long expectedInitialSuccessorFinish, long expectedPredecessorFinish, long expectedLinkedSuccessorStart,
		long expectedLinkedSuccessorFinish) throws Exception {
		Fixture fixture = createFixture();
		Project project = fixture.project;
		long mondayEight = timestamp(2030, Calendar.JUNE, 3, 8);
		long mondaySeventeen = timestamp(2030, Calendar.JUNE, 3, 17);
		long tuesdayEight = timestamp(2030, Calendar.JUNE, 4, 8);
		project.setStart(mondayEight);
		String typeName = type == DependencyType.FS ? "FS" : "FF";
		NormalTask predecessor = createTask(project, "SNET " + typeName + " predecessor", predecessorDurationDays);
		NormalTask successor = createTask(project, "SNET " + typeName + " successor", 1);
		NormalTask unrelated = createTask(project, "unrelated", 1);
		predecessor.setScheduleConstraint(ConstraintType.SNET, mondayEight);
		successor.setScheduleConstraint(ConstraintType.SNET, snet);
		unrelated.setScheduleConstraint(ConstraintType.SNET, mondayEight);
		project.recalculate();
		assertEquals(mondayEight, predecessor.getStart());
		assertEquals(expectedPredecessorFinish, predecessor.getEnd());
		assertEquals(snet, successor.getStart());
		assertEquals(expectedInitialSuccessorFinish, successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());

		// Document-derived compatibility decision, not a measured MSP run: Microsoft documents
		// SNET as a lower bound; FS cannot start before predecessor finish and FF cannot finish
		// before predecessor finish. The later applicable boundary governs.
		// https://support.microsoft.com/en-us/project/how-project-schedules-tasks-behind-the-scenes
		Dependency dependency = DependencyService.getInstance().newDependency(
			predecessor, successor, type, 0L, this);
		project.recalculate();
		ScheduleDiagnosticsService diagnostics = new ScheduleDiagnosticsService();
		List<ScheduleDiagnosticsService.Type> linkedDiagnostics = diagnostics.diagnose(successor).stream()
			.map(ScheduleDiagnosticsService.Issue::type).toList();
		assertEquals(type, dependency.getDependencyType());
		assertEquals(0L, dependency.getLag());
		if (type == DependencyType.FS) {
			assertEquals(expectedPredecessorFinish, dependency.getDate(true),
				"zero-lag FS boundary is predecessor finish");
		} else {
			assertTrue(successor.getEnd() >= expectedPredecessorFinish,
				"the successor finish must meet or follow the predecessor finish boundary");
		}
		assertEquals(expectedPredecessorFinish, predecessor.getEnd());
		assertEquals(expectedLinkedSuccessorStart, successor.getStart());
		assertEquals(expectedLinkedSuccessorFinish, successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());
		assertTrue(linkedDiagnostics.contains(ScheduleDiagnosticsService.Type.HARD_CONSTRAINT));
		assertFalse(linkedDiagnostics.contains(ScheduleDiagnosticsService.Type.DEPENDENCY_CONFLICT));

		fixture.undo.undo();
		project.recalculate();
		assertFalse(predecessor.getSuccessorList().iterator().hasNext());
		assertEquals(expectedPredecessorFinish, predecessor.getEnd());
		assertEquals(snet, successor.getStart());
		assertEquals(expectedInitialSuccessorFinish, successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());
		assertEquals(List.of(ScheduleDiagnosticsService.Type.HARD_CONSTRAINT), diagnostics.diagnose(successor).stream()
			.map(ScheduleDiagnosticsService.Issue::type).toList());

		fixture.undo.redo();
		project.recalculate();
		assertSame(dependency, predecessor.getSuccessorList().findRight(successor));
		assertEquals(expectedLinkedSuccessorStart, successor.getStart());
		assertEquals(expectedLinkedSuccessorFinish, successor.getEnd());
		assertEquals(expectedPredecessorFinish, predecessor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());
		assertEquals(linkedDiagnostics, diagnostics.diagnose(successor).stream()
			.map(ScheduleDiagnosticsService.Issue::type).toList());
	}

	@TestFactory
	Stream<DynamicTest> snetAndPositiveFsLagAcrossProjectCalendarExceptionUseLaterBoundary() {
		long tuesdayEight = timestamp(2030, Calendar.JUNE, 4, 8);
		long thursdayEight = timestamp(2030, Calendar.JUNE, 6, 8);
		long thursdaySeventeen = timestamp(2030, Calendar.JUNE, 6, 17);
		long fridayEight = timestamp(2030, Calendar.JUNE, 7, 8);
		long fridaySeventeen = timestamp(2030, Calendar.JUNE, 7, 17);
		return Stream.of(
			DynamicTest.dynamicTest("SNET-FS-lag-boundary-later", () ->
				verifySnetAndFsLagAcrossException(tuesdayEight, timestamp(2030, Calendar.JUNE, 5, 17),
					thursdayEight, thursdaySeventeen)),
			DynamicTest.dynamicTest("SNET-constraint-later", () ->
				verifySnetAndFsLagAcrossException(fridayEight, fridaySeventeen, fridayEight, fridaySeventeen)));
	}

	private void verifySnetAndFsLagAcrossException(long snet, long expectedInitialSuccessorFinish,
		long expectedSuccessorStart, long expectedSuccessorFinish) throws Exception {
		Fixture fixture = createFixture();
		Project project = fixture.project;
		long mondayEight = timestamp(2030, Calendar.JUNE, 3, 8);
		long mondaySeventeen = timestamp(2030, Calendar.JUNE, 3, 17);
		long tuesdayMidnight = timestamp(2030, Calendar.JUNE, 4, 0);
		project.setStart(mondayEight);
		((WorkingCalendar) project.getWorkCalendar()).addOrReplaceException(new WorkDay(tuesdayMidnight));
		NormalTask predecessor = createTask(project, "SNET lag predecessor", 1);
		NormalTask successor = createTask(project, "SNET lag successor", 1);
		NormalTask unrelated = createTask(project, "unrelated", 1);
		predecessor.setScheduleConstraint(ConstraintType.SNET, mondayEight);
		successor.setScheduleConstraint(ConstraintType.SNET, snet);
		unrelated.setScheduleConstraint(ConstraintType.SNET, mondayEight);
		project.recalculate();
		assertEquals(mondayEight, predecessor.getStart());
		assertEquals(mondaySeventeen, predecessor.getEnd());
		assertEquals(snet, successor.getStart());
		assertEquals(expectedInitialSuccessorFinish, successor.getEnd());
		assertEquals(mondaySeventeen, unrelated.getEnd());

		// Document-derived compatibility decision, not measured MSP behavior: Microsoft documents
		// SNET as a lower bound against an FS link, and positive lag as a delay to the successor;
		// the project calendar excludes the dated Tuesday exception. The later boundary wins.
		// https://support.microsoft.com/en-us/project/how-project-schedules-tasks-behind-the-scenes
		// https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task
		Dependency dependency = DependencyService.getInstance().newDependency(
			predecessor, successor, DependencyType.FS, day(), this);
		project.recalculate();
		ScheduleDiagnosticsService diagnostics = new ScheduleDiagnosticsService();
		List<ScheduleDiagnosticsService.Type> linkedDiagnostics = diagnostics.diagnose(successor).stream()
			.map(ScheduleDiagnosticsService.Issue::type).toList();
		assertEquals(DependencyType.FS, dependency.getDependencyType());
		assertEquals(day(), dependency.getLag());
		assertEquals(mondayEight, predecessor.getStart());
		assertEquals(timestamp(2030, Calendar.JUNE, 6, 8), dependency.getDate(true),
			"the positive one-day lag skips the dated Tuesday exception");
		assertEquals(mondaySeventeen, predecessor.getEnd());
		assertEquals(expectedSuccessorStart, successor.getStart());
		assertEquals(expectedSuccessorFinish, successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());
		assertTrue(linkedDiagnostics.contains(ScheduleDiagnosticsService.Type.HARD_CONSTRAINT));
		assertFalse(linkedDiagnostics.contains(ScheduleDiagnosticsService.Type.DEPENDENCY_CONFLICT));

		fixture.undo.undo();
		project.recalculate();
		assertFalse(predecessor.getSuccessorList().iterator().hasNext());
		assertEquals(mondayEight, predecessor.getStart());
		assertEquals(mondaySeventeen, predecessor.getEnd());
		assertEquals(snet, successor.getStart());
		assertEquals(expectedInitialSuccessorFinish, successor.getEnd());
		assertEquals(mondaySeventeen, unrelated.getEnd());

		fixture.undo.redo();
		project.recalculate();
		assertSame(dependency, predecessor.getSuccessorList().findRight(successor));
		assertEquals(expectedSuccessorStart, successor.getStart());
		assertEquals(expectedSuccessorFinish, successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());
		assertEquals(linkedDiagnostics, diagnostics.diagnose(successor).stream()
			.map(ScheduleDiagnosticsService.Issue::type).toList());
	}

	@TestFactory
	Stream<DynamicTest> startNoEarlierThanConstraintAndStartToStartLinkUseLaterBoundary() {
		return Stream.of(
			DynamicTest.dynamicTest("SNET-SS-predecessor-starts-before-constraint", () ->
				verifySnetAndSsBoundary(timestamp(2030, Calendar.JUNE, 3, 8),
					timestamp(2030, Calendar.JUNE, 4, 8), timestamp(2030, Calendar.JUNE, 3, 17),
					timestamp(2030, Calendar.JUNE, 4, 8), timestamp(2030, Calendar.JUNE, 4, 17))),
			DynamicTest.dynamicTest("SNET-SS-predecessor-starts-after-constraint", () ->
				verifySnetAndSsBoundary(timestamp(2030, Calendar.JUNE, 4, 8),
					timestamp(2030, Calendar.JUNE, 3, 8), timestamp(2030, Calendar.JUNE, 4, 17),
					timestamp(2030, Calendar.JUNE, 4, 8), timestamp(2030, Calendar.JUNE, 4, 17))));
	}

	@TestFactory
	Stream<DynamicTest> startNoEarlierThanConstraintAndStartToFinishLinkUseLaterBoundary() {
		return Stream.of(
			DynamicTest.dynamicTest("SNET-SF-predecessor-starts-before-constraint", () ->
				verifySnetAndSfBoundary(timestamp(2030, Calendar.JUNE, 3, 8),
					timestamp(2030, Calendar.JUNE, 4, 8), timestamp(2030, Calendar.JUNE, 3, 17),
					timestamp(2030, Calendar.JUNE, 4, 8), timestamp(2030, Calendar.JUNE, 4, 17))),
			DynamicTest.dynamicTest("SNET-SF-predecessor-starts-after-constraint", () ->
				verifySnetAndSfBoundary(timestamp(2030, Calendar.JUNE, 4, 8),
					timestamp(2030, Calendar.JUNE, 3, 8), timestamp(2030, Calendar.JUNE, 4, 17),
					timestamp(2030, Calendar.JUNE, 4, 8), timestamp(2030, Calendar.JUNE, 4, 17))));
	}

	private void verifySnetAndSfBoundary(long predecessorStart, long snet, long expectedPredecessorFinish,
		long expectedLinkedSuccessorStart, long expectedLinkedSuccessorFinish) throws Exception {
		Fixture fixture = createFixture();
		Project project = fixture.project;
		long mondayEight = timestamp(2030, Calendar.JUNE, 3, 8);
		long mondaySeventeen = timestamp(2030, Calendar.JUNE, 3, 17);
		long tuesdaySeventeen = timestamp(2030, Calendar.JUNE, 4, 17);
		project.setStart(mondayEight);
		NormalTask predecessor = createTask(project, "SNET SF predecessor", 1);
		NormalTask successor = createTask(project, "SNET SF successor", 1);
		NormalTask unrelated = createTask(project, "unrelated", 1);
		predecessor.setScheduleConstraint(ConstraintType.SNET, predecessorStart);
		successor.setScheduleConstraint(ConstraintType.SNET, snet);
		unrelated.setScheduleConstraint(ConstraintType.SNET, mondayEight);
		project.recalculate();
		assertEquals(predecessorStart, predecessor.getStart());
		assertEquals(expectedPredecessorFinish, predecessor.getEnd());
		assertEquals(snet, successor.getStart());
		assertEquals(snet == mondayEight ? mondaySeventeen : tuesdaySeventeen, successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());

		// Document-derived Project desktop rule, not a measured MSP run: SF constrains
		// the successor finish against the predecessor start, while SNET is a lower bound.
		// https://support.microsoft.com/en-us/project/how-project-schedules-tasks-behind-the-scenes
		Dependency dependency = DependencyService.getInstance().newDependency(
			predecessor, successor, DependencyType.SF, 0L, this);
		project.recalculate();
		ScheduleDiagnosticsService diagnostics = new ScheduleDiagnosticsService();
		List<ScheduleDiagnosticsService.Type> linkedDiagnostics = diagnostics.diagnose(successor).stream()
			.map(ScheduleDiagnosticsService.Issue::type).toList();
		assertEquals(DependencyType.SF, dependency.getDependencyType());
		assertEquals(0L, dependency.getLag());
		assertEquals(expectedPredecessorFinish, predecessor.getEnd(), "linking must not move the predecessor");
		assertEquals(expectedLinkedSuccessorStart, successor.getStart());
		assertEquals(expectedLinkedSuccessorFinish, successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());
		assertTrue(linkedDiagnostics.contains(ScheduleDiagnosticsService.Type.HARD_CONSTRAINT));
		assertFalse(linkedDiagnostics.contains(ScheduleDiagnosticsService.Type.DEPENDENCY_CONFLICT));

		fixture.undo.undo();
		project.recalculate();
		assertFalse(predecessor.getSuccessorList().iterator().hasNext());
		assertEquals(predecessorStart, predecessor.getStart());
		assertEquals(expectedPredecessorFinish, predecessor.getEnd());
		assertEquals(snet, successor.getStart());
		assertEquals(snet == mondayEight ? mondaySeventeen : tuesdaySeventeen, successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());
		assertEquals(List.of(ScheduleDiagnosticsService.Type.HARD_CONSTRAINT), diagnostics.diagnose(successor).stream()
			.map(ScheduleDiagnosticsService.Issue::type).toList());

		fixture.undo.redo();
		project.recalculate();
		assertSame(dependency, predecessor.getSuccessorList().findRight(successor));
		assertEquals(expectedLinkedSuccessorStart, successor.getStart());
		assertEquals(expectedLinkedSuccessorFinish, successor.getEnd());
		assertEquals(expectedPredecessorFinish, predecessor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());
		assertEquals(linkedDiagnostics, diagnostics.diagnose(successor).stream()
			.map(ScheduleDiagnosticsService.Issue::type).toList());
	}

	private void verifySnetAndSsBoundary(long predecessorStart, long snet, long expectedPredecessorFinish,
		long expectedLinkedSuccessorStart, long expectedLinkedSuccessorFinish) throws Exception {
		Fixture fixture = createFixture();
		Project project = fixture.project;
		long mondayEight = timestamp(2030, Calendar.JUNE, 3, 8);
		long mondaySeventeen = timestamp(2030, Calendar.JUNE, 3, 17);
		long initialSuccessorFinish = timestamp(2030, Calendar.JUNE, 4, 17);
		project.setStart(mondayEight);
		NormalTask predecessor = createTask(project, "SNET SS predecessor", 1);
		NormalTask successor = createTask(project, "SNET SS successor", 1);
		NormalTask unrelated = createTask(project, "unrelated", 1);
		predecessor.setScheduleConstraint(ConstraintType.SNET, predecessorStart);
		successor.setScheduleConstraint(ConstraintType.SNET, snet);
		unrelated.setScheduleConstraint(ConstraintType.SNET, mondayEight);
		project.recalculate();
		assertEquals(predecessorStart, predecessor.getStart());
		assertEquals(expectedPredecessorFinish, predecessor.getEnd());
		assertEquals(snet, successor.getStart());
		assertEquals(snet == mondayEight ? mondaySeventeen : initialSuccessorFinish, successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());

		// Document-derived Project desktop rule (not a measured MSP run): SS constrains
		// the successor start against the predecessor start, while SNET is a lower bound.
		// https://support.microsoft.com/en-us/project/how-project-schedules-tasks-behind-the-scenes
		Dependency dependency = DependencyService.getInstance().newDependency(
			predecessor, successor, DependencyType.SS, 0L, this);
		project.recalculate();
		assertEquals(expectedPredecessorFinish, predecessor.getEnd(), "linking must not move the predecessor");
		assertEquals(predecessorStart, dependency.getDate(true), "zero-lag SS boundary is predecessor start");
		ScheduleDiagnosticsService diagnostics = new ScheduleDiagnosticsService();
		List<ScheduleDiagnosticsService.Type> linkedDiagnostics = diagnostics.diagnose(successor).stream()
			.map(ScheduleDiagnosticsService.Issue::type).toList();
		assertEquals(expectedLinkedSuccessorStart, successor.getStart());
		assertEquals(expectedLinkedSuccessorFinish, successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());
		assertTrue(linkedDiagnostics.contains(ScheduleDiagnosticsService.Type.HARD_CONSTRAINT));
		assertFalse(linkedDiagnostics.contains(ScheduleDiagnosticsService.Type.DEPENDENCY_CONFLICT));

		fixture.undo.undo();
		project.recalculate();
		assertFalse(predecessor.getSuccessorList().iterator().hasNext());
		assertEquals(predecessorStart, predecessor.getStart());
		assertEquals(expectedPredecessorFinish, predecessor.getEnd());
		assertEquals(snet, successor.getStart());
		assertEquals(snet == mondayEight ? mondaySeventeen : initialSuccessorFinish, successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());
		assertEquals(List.of(ScheduleDiagnosticsService.Type.HARD_CONSTRAINT),
			diagnostics.diagnose(successor).stream().map(ScheduleDiagnosticsService.Issue::type).toList());

		fixture.undo.redo();
		project.recalculate();
		assertSame(dependency, predecessor.getSuccessorList().findRight(successor));
		assertEquals(expectedLinkedSuccessorStart, successor.getStart());
		assertEquals(expectedLinkedSuccessorFinish, successor.getEnd());
		assertEquals(expectedPredecessorFinish, predecessor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());
		assertEquals(linkedDiagnostics, diagnostics.diagnose(successor).stream()
			.map(ScheduleDiagnosticsService.Issue::type).toList());
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
		project.setWorkCalendar(((WorkingCalendar) project.getWorkCalendar()).makeScratchCopy());
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
