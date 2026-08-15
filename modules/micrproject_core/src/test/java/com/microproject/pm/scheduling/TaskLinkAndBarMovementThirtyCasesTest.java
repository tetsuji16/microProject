package com.microproject.pm.scheduling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import com.microproject.association.InvalidAssociationException;
import com.microproject.field.FieldContext;
import com.microproject.options.CalendarOption;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

class TaskLinkAndBarMovementThirtyCasesTest {
	private record LinkCase(int type, int lagDays) {}
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
			DynamicTest.dynamicTest(id("LINK", 14), this::verifyCircularLinkIsRejected),
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
		long expectedStart = dependency.calcForwardDependencyDate(
			predecessor.getStart(), predecessor.getEnd(), successor.getDuration() != 0L);
		assertEquals(expectedStart, successor.getStart(),
			"type=" + c.type + " lagDays=" + c.lagDays);
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

	private void verifyCircularLinkIsRejected() throws Exception {
		Fixture fixture = createFixture();
		NormalTask first = createTask(fixture.project, "first", 1);
		NormalTask second = createTask(fixture.project, "second", 1);
		DependencyService.getInstance().newDependency(first, second, DependencyType.FS, 0L, this);

		assertThrows(InvalidAssociationException.class, () -> DependencyService.getInstance()
			.newDependency(second, first, DependencyType.FS, 0L, this));
		assertFalse(second.getSuccessorList().iterator().hasNext());
		assertEquals(1, count(first.getSuccessorList().iterator()));
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

	private static String id(String group, int number) {
		return "TLBM-" + group + String.format("-%02d", number);
	}
}
