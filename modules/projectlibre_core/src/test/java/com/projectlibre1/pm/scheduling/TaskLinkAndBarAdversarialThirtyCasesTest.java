package com.projectlibre1.pm.scheduling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import com.projectlibre1.association.InvalidAssociationException;
import com.projectlibre1.options.CalendarOption;
import com.projectlibre1.pm.dependency.Dependency;
import com.projectlibre1.pm.dependency.DependencyService;
import com.projectlibre1.pm.dependency.DependencyType;
import com.projectlibre1.pm.resource.ResourcePool;
import com.projectlibre1.pm.task.NormalTask;
import com.projectlibre1.pm.task.Project;
import com.projectlibre1.undo.DataFactoryUndoController;

class TaskLinkAndBarAdversarialThirtyCasesTest {
	private record Fixture(Project project, DataFactoryUndoController undo) {}

	@TestFactory
	Stream<DynamicTest> adversarialLinkCases() {
		List<DynamicTest> tests = new ArrayList<>();
		int[] types = { DependencyType.FS, DependencyType.SS, DependencyType.FF, DependencyType.SF };
		for (int index = 0; index < types.length; index++) {
			int type = types[index];
			tests.add(DynamicTest.dynamicTest(id("LINK", index + 1), () -> rejectSelfLink(type)));
			tests.add(DynamicTest.dynamicTest(id("LINK", index + 5), () -> rejectDuplicateLink(type)));
		}
		tests.add(DynamicTest.dynamicTest(id("LINK", 9), () -> rejectCycle(3)));
		tests.add(DynamicTest.dynamicTest(id("LINK", 10), () -> rejectCycle(4)));
		tests.add(DynamicTest.dynamicTest(id("LINK", 11), () -> rejectExternal(true)));
		tests.add(DynamicTest.dynamicTest(id("LINK", 12), () -> rejectExternal(false)));
		tests.add(DynamicTest.dynamicTest(id("LINK", 13), this::rejectReadOnlyProjectLink));
		tests.add(DynamicTest.dynamicTest(id("LINK", 14), () -> verifyExtremeLag(260)));
		tests.add(DynamicTest.dynamicTest(id("LINK", 15), () -> verifyExtremeLag(-260)));
		return tests.stream();
	}

	@TestFactory
	Stream<DynamicTest> adversarialBarCases() {
		return Stream.of(
			DynamicTest.dynamicTest(id("BAR", 16), this::unchangedIntervalIsNoOp),
			DynamicTest.dynamicTest(id("BAR", 17), this::readOnlyMoveIsRejected),
			DynamicTest.dynamicTest(id("BAR", 18), () -> invertedIntervalBecomesMilestone(true)),
			DynamicTest.dynamicTest(id("BAR", 19), () -> invertedIntervalBecomesMilestone(false)),
			DynamicTest.dynamicTest(id("BAR", 20), this::resizeToZeroCreatesMilestone),
			DynamicTest.dynamicTest(id("BAR", 21), this::moveMilestoneBackward),
			DynamicTest.dynamicTest(id("BAR", 22), this::weekendMoveAdjustsIntoCalendar),
			DynamicTest.dynamicTest(id("BAR", 23), () -> moveByHours(1)),
			DynamicTest.dynamicTest(id("BAR", 24), this::midnightMoveAdjustsIntoCalendar),
			DynamicTest.dynamicTest(id("BAR", 25), this::resizeToOneHour),
			DynamicTest.dynamicTest(id("BAR", 26), this::resizeToHugeDuration),
			DynamicTest.dynamicTest(id("BAR", 27), this::repeatedMoveRoundTrips),
			DynamicTest.dynamicTest(id("BAR", 28), this::repeatedResizeUsesLastDuration),
			DynamicTest.dynamicTest(id("BAR", 29), this::resizeUndoRedoRoundTrips),
			DynamicTest.dynamicTest(id("BAR", 30), this::dragReplacesConflictingFinishConstraint));
	}

	private void rejectSelfLink(int type) {
		Fixture f = fixture();
		NormalTask task = task(f, "self", 1);
		assertThrows(InvalidAssociationException.class,
			() -> DependencyService.getInstance().newDependency(task, task, type, 0L, this));
		assertFalse(task.getPredecessorList().iterator().hasNext());
		assertFalse(task.getSuccessorList().iterator().hasNext());
	}

	private void rejectDuplicateLink(int type) throws Exception {
		Fixture f = fixture();
		NormalTask first = task(f, "first", 1);
		NormalTask second = task(f, "second", 1);
		Dependency original = DependencyService.getInstance().newDependency(first, second, type, 0L, this);
		assertThrows(InvalidAssociationException.class,
			() -> DependencyService.getInstance().newDependency(first, second, type, day(), this));
		assertSame(original, first.getSuccessorList().findRight(second));
		assertEquals(1, count(first.getSuccessorList().iterator()));
	}

	private void rejectCycle(int taskCount) throws Exception {
		Fixture f = fixture();
		List<NormalTask> tasks = IntStream.range(0, taskCount)
			.mapToObj(i -> task(f, "cycle-" + i, 1)).toList();
		for (int i = 0; i < taskCount - 1; i++)
			DependencyService.getInstance().newDependency(tasks.get(i), tasks.get(i + 1), DependencyType.FS, 0L, this);
		assertThrows(InvalidAssociationException.class, () -> DependencyService.getInstance()
			.newDependency(tasks.get(taskCount - 1), tasks.get(0), DependencyType.FS, 0L, this));
		assertFalse(tasks.get(taskCount - 1).getSuccessorList().iterator().hasNext());
	}

	private void rejectExternal(boolean predecessorExternal) {
		Fixture f = fixture();
		NormalTask predecessor = task(f, "predecessor", 1);
		NormalTask successor = task(f, "successor", 1);
		(predecessorExternal ? predecessor : successor).setExternal(true);
		assertThrows(InvalidAssociationException.class, () -> DependencyService.getInstance()
			.newDependency(predecessor, successor, DependencyType.FS, 0L, this));
	}

	private void rejectReadOnlyProjectLink() {
		Fixture f = fixture();
		NormalTask predecessor = task(f, "predecessor", 1);
		NormalTask successor = task(f, "successor", 1);
		f.project.setReadOnly(true);
		assertThrows(InvalidAssociationException.class, () -> DependencyService.getInstance()
			.newDependency(predecessor, successor, DependencyType.FS, 0L, this));
		assertFalse(predecessor.getSuccessorList().iterator().hasNext());
	}

	private void verifyExtremeLag(int lagDays) throws Exception {
		Fixture f = fixture();
		NormalTask predecessor = task(f, "predecessor", 2);
		NormalTask successor = task(f, "successor", 1);
		Dependency dependency = DependencyService.getInstance().newDependency(
			predecessor, successor, DependencyType.FS, lagDays * day(), this);
		f.project.recalculate();
		assertEquals(lagDays * day(), dependency.getLag());
		assertEquals(dependency.calcForwardDependencyDate(predecessor.getStart(), predecessor.getEnd(), true),
			successor.getStart());
	}

	private void unchangedIntervalIsNoOp() {
		Fixture f = fixture();
		NormalTask task = task(f, "noop", 2);
		boolean changed = ScheduleService.getInstance().setInterval(this, task, task.getStart(), task.getEnd(),
			new ScheduleInterval(task.getStart(), task.getEnd()), f.undo.getEditSupport());
		assertFalse(changed);
		assertFalse(f.undo.canUndo());
	}

	private void readOnlyMoveIsRejected() {
		Fixture f = fixture();
		NormalTask task = task(f, "readonly", 2);
		long start = task.getStart();
		long end = task.getEnd();
		f.project.setReadOnly(true);
		boolean changed = ScheduleService.getInstance().setInterval(this, task, start + hour(), end + hour(),
			new ScheduleInterval(start, end), f.undo.getEditSupport());
		assertFalse(changed);
		assertEquals(start, task.getStart());
		assertEquals(end, task.getEnd());
	}

	private void invertedIntervalBecomesMilestone(boolean startMoved) {
		Fixture f = fixture();
		NormalTask task = task(f, "inverted", 2);
		long oldStart = task.getStart();
		long oldEnd = task.getEnd();
		long crossing = startMoved ? oldEnd + day() : oldStart - day();
		long requestedStart = startMoved ? crossing : oldStart;
		long requestedEnd = startMoved ? oldEnd : crossing;
		ScheduleService.getInstance().setInterval(this, task, requestedStart, requestedEnd,
			new ScheduleInterval(oldStart, oldEnd), f.undo.getEditSupport());
		assertEquals(0L, task.getDuration());
		assertEquals(task.getStart(), task.getEnd());
	}

	private void resizeToZeroCreatesMilestone() {
		Fixture f = fixture();
		NormalTask task = task(f, "zero", 2);
		long start = task.getStart();
		ScheduleService.getInstance().setInterval(this, task, start, start,
			new ScheduleInterval(start, task.getEnd()), f.undo.getEditSupport());
		assertTrue(task.isMilestone());
		assertEquals(0L, task.getDuration());
	}

	private void moveMilestoneBackward() {
		Fixture f = fixture();
		NormalTask task = task(f, "milestone", 0);
		long initial = task.getStart();
		long old = task.getEffectiveWorkCalendar().add(initial, 10L * day(), false);
		task.setScheduleConstraint(ConstraintType.SNET, old);
		ScheduleService.getInstance().setInterval(this, task, old, old,
			new ScheduleInterval(initial, initial), f.undo.getEditSupport());
		long moved = task.getEffectiveWorkCalendar().add(old, -5L * day(), false);
		task.setScheduleConstraint(ConstraintType.SNET, moved);
		ScheduleService.getInstance().setInterval(this, task, moved, moved,
			new ScheduleInterval(old, old), f.undo.getEditSupport());
		assertEquals(moved, task.getStart());
		assertEquals(0L, task.getDuration());
	}

	private void weekendMoveAdjustsIntoCalendar() {
		Fixture f = fixture();
		NormalTask task = task(f, "weekend", 2);
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(task.getStart());
		while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY)
			calendar.add(Calendar.DAY_OF_MONTH, 1);
		long requested = calendar.getTimeInMillis();
		long adjusted = task.getEffectiveWorkCalendar().adjustInsideCalendar(requested, false);
		moveWholeBar(f, task, adjusted);
		assertEquals(adjusted, task.getStart());
	}

	private void moveByHours(int hours) {
		Fixture f = fixture();
		NormalTask task = task(f, "hours", 2);
		long oldStart = task.getStart();
		moveWholeBar(f, task, oldStart + hours * hour());
		assertEquals(oldStart + hours * hour(), task.getStart());
		assertEquals(2L * day(), task.getDuration());
	}

	private void midnightMoveAdjustsIntoCalendar() {
		Fixture f = fixture();
		NormalTask task = task(f, "midnight", 2);
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(task.getStart());
		calendar.add(Calendar.DAY_OF_MONTH, 1);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		long adjusted = task.getEffectiveWorkCalendar().adjustInsideCalendar(calendar.getTimeInMillis(), false);
		moveWholeBar(f, task, adjusted);
		assertEquals(adjusted, task.getStart());
	}

	private void resizeToOneHour() {
		Fixture f = fixture();
		NormalTask task = task(f, "one-hour", 2);
		resize(f, task, hour());
		assertEquals(hour(), task.getDuration());
	}

	private void resizeToHugeDuration() {
		Fixture f = fixture();
		NormalTask task = task(f, "huge", 2);
		resize(f, task, 260L * day());
		assertEquals(260L * day(), task.getDuration());
	}

	private void repeatedMoveRoundTrips() {
		Fixture f = fixture();
		NormalTask task = task(f, "repeat-move", 2);
		long original = task.getStart();
		for (int i = 0; i < 10; i++) {
			moveWholeBar(f, task, task.getEffectiveWorkCalendar().add(task.getStart(), day(), false));
			moveWholeBar(f, task, task.getEffectiveWorkCalendar().add(task.getStart(), -day(), false));
		}
		assertEquals(original, task.getStart());
		assertEquals(2L * day(), task.getDuration());
	}

	private void repeatedResizeUsesLastDuration() {
		Fixture f = fixture();
		NormalTask task = task(f, "repeat-resize", 2);
		for (long duration : new long[] { day(), 10L * day(), 2L * day(), 7L * day() })
			resize(f, task, duration);
		assertEquals(7L * day(), task.getDuration());
	}

	private void resizeUndoRedoRoundTrips() {
		Fixture f = fixture();
		NormalTask task = task(f, "undo", 2);
		task.setScheduleConstraint(ConstraintType.SNET, task.getStart());
		f.project.recalculate();
		long oldStart = task.getStart();
		long oldEnd = task.getEnd();
		resize(f, task, 5L * day());
		long resizedEnd = task.getEnd();
		f.undo.undo();
		assertEquals(oldStart, task.getStart());
		assertEquals(oldEnd, task.getEnd());
		f.undo.redo();
		assertEquals(resizedEnd, task.getEnd());
		assertEquals(5L * day(), task.getDuration());
	}

	private void dragReplacesConflictingFinishConstraint() {
		Fixture f = fixture();
		NormalTask task = task(f, "constraint", 2);
		task.setScheduleConstraint(ConstraintType.FNLT, task.getEnd());
		long newStart = task.getEffectiveWorkCalendar().add(task.getStart(), 2L * day(), false);
		moveWholeBar(f, task, newStart);
		assertEquals(ConstraintType.SNET, task.getConstraintType());
		assertEquals(newStart, task.getConstraintDate());
	}

	private void moveWholeBar(Fixture f, NormalTask task, long newStart) {
		long oldStart = task.getStart();
		long oldEnd = task.getEnd();
		long targetEnd = canonicalFinish(task, newStart, task.getDuration());
		task.setScheduleConstraint(ConstraintType.SNET, newStart);
		ScheduleService.getInstance().setInterval(this, task, newStart, targetEnd,
			new ScheduleInterval(oldStart, oldEnd), f.undo.getEditSupport());
		task.setScheduleConstraint(ConstraintType.SNET, task.getStart());
	}

	private void resize(Fixture f, NormalTask task, long duration) {
		long start = task.getStart();
		long oldEnd = task.getEnd();
		long targetEnd = canonicalFinish(task, start, duration);
		task.setScheduleConstraint(ConstraintType.SNET, start);
		ScheduleService.getInstance().setInterval(this, task, start, targetEnd,
			new ScheduleInterval(start, oldEnd), f.undo.getEditSupport());
		task.setScheduleConstraint(ConstraintType.SNET, task.getStart());
	}

	private Fixture fixture() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("adversarial", undo);
		Project project = Project.createProject(pool, undo);
		project.initialize(false, false);
		return new Fixture(project, undo);
	}

	private NormalTask task(Fixture f, String name, int days) {
		NormalTask task = new NormalTask(f.project);
		task.setName(name);
		f.project.connectTask(task);
		f.project.getSchedulingAlgorithm().addObject(task);
		task.getCurrentSchedule().setStart(f.project.getStart());
		task.setDuration(days * day());
		return task;
	}

	private static long canonicalFinish(NormalTask task, long start, long duration) {
		if (duration == 0L) return start;
		long approximate = task.getEffectiveWorkCalendar().add(start, duration, false);
		if (task.getEffectiveWorkCalendar().compare(approximate, start, false) == duration)
			return approximate;
		for (int offset = -24 * 400; offset <= 24 * 400; offset++) {
			long candidate = approximate + offset * hour();
			if (task.getEffectiveWorkCalendar().compare(candidate, start, false) == duration)
				return candidate;
		}
		throw new AssertionError("No canonical finish for duration=" + duration);
	}

	private static int count(java.util.Iterator<?> iterator) {
		int count = 0;
		while (iterator.hasNext()) { iterator.next(); count++; }
		return count;
	}

	private static long day() { return CalendarOption.getInstance().getMillisPerDay(); }
	private static long hour() { return 60L * 60L * 1000L; }
	private static String id(String group, int number) {
		return "ADV-" + group + String.format("-%02d", number);
	}
}
