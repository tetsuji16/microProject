/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.configuration.Configuration;
import com.microproject.datatype.Duration;
import com.microproject.field.FieldContext;
import com.microproject.grouping.core.Node;
import com.microproject.options.CalendarOption;
import com.microproject.pm.assignment.AssignmentService;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.scheduling.ConstraintType;
import com.microproject.pm.scheduling.ScheduleService;
import com.microproject.undo.DataFactoryUndoController;

/**
 * Incremental scheduling (suspended-batch single-start recalculation, summary child bounds, assignment node
 * insertion) must yield exactly the early and late dates of a full recalculation, and undo must restore them.
 */
class IncrementalScheduleEquivalenceTest {
	private static long day() {
		return CalendarOption.getInstance().getMillisPerDay();
	}

	private final class Fixture {
		final Project project;
		final Node phase1Node;
		final Node phase2Node;
		final NormalTask phase1;
		final NormalTask phase2;
		final NormalTask[] a = new NormalTask[3];
		final NormalTask[] b = new NormalTask[3];

		Fixture(boolean forward) throws Exception {
			DataFactoryUndoController undoController = new DataFactoryUndoController();
			project = Project.createProject(ResourcePool.createRourcePool("equivalence", undoController), undoController);
			project.initialize(false, false);
			if (!forward)
				project.setForward(false);
			phase1Node = project.createLocalTaskNode(null);
			phase1 = (NormalTask) phase1Node.getImpl();
			phase2Node = project.createLocalTaskNode(null);
			phase2 = (NormalTask) phase2Node.getImpl();
			for (int i = 0; i < 3; i++) {
				a[i] = (NormalTask) project.createLocalTaskNode(phase1Node).getImpl();
				a[i].setDuration((i + 2) * day());
			}
			for (int i = 0; i < 3; i++) {
				b[i] = (NormalTask) project.createLocalTaskNode(phase2Node).getImpl();
				b[i].setDuration((i + 2) * day());
			}
			for (int i = 0; i < 2; i++) {
				DependencyService.getInstance().newDependency(a[i], a[i + 1], DependencyType.FS, 0L, IncrementalScheduleEquivalenceTest.this);
				DependencyService.getInstance().newDependency(b[i], b[i + 1], DependencyType.FS, 0L, IncrementalScheduleEquivalenceTest.this);
			}
			DependencyService.getInstance().newDependency(a[2], b[0], DependencyType.FS, 0L, IncrementalScheduleEquivalenceTest.this);
			if (!forward) {
				for (NormalTask t : all())
					t.setForward(false);
			}
			project.recalculate();
		}

		List<NormalTask> all() {
			List<NormalTask> list = new ArrayList<>();
			list.add(phase1);
			list.add(phase2);
			list.addAll(List.of(a));
			list.addAll(List.of(b));
			return list;
		}

		List<Long> snapshot() {
			List<Long> dates = new ArrayList<>();
			dates.add(project.getStart());
			dates.add(project.getEnd());
			for (NormalTask t : all()) {
				dates.add(t.getEarlyStart());
				dates.add(t.getEarlyFinish());
				dates.add(t.getLateStart());
				dates.add(t.getLateFinish());
			}
			return dates;
		}

		void assertEqualsFullRecalculation(String step) {
			List<Long> incremental = snapshot();
			project.recalculate();
			assertEquals(incremental, snapshot(), "incremental result differs from full recalculation: " + step);
		}
	}

	private void setDuration(NormalTask task, long duration) throws Exception {
		FieldContext context = new FieldContext();
		context.setTaskSheetUpdate(true);
		Configuration.getFieldFromId("Field.duration").setValue(task, this, new Duration(duration), context);
	}

	private void setConstraint(Fixture f, long date) {
		ScheduleService.getInstance().setConstraint(this, f.phase2, ConstraintType.Kind.SNET, date,
			f.project.getUndoController().getEditSupport());
	}

	private ResourceImpl resource(Project project) {
		return project.getResourcePool().newResourceInstance();
	}

	/** Critical chain c0 -> c1 -> c2 (5 days each) and an independent noncritical task with slack. */
	private final class SlackFixture {
		final Project project;
		final NormalTask[] chain = new NormalTask[3];
		final NormalTask side;

		SlackFixture() throws Exception {
			DataFactoryUndoController undoController = new DataFactoryUndoController();
			project = Project.createProject(ResourcePool.createRourcePool("slack", undoController), undoController);
			project.initialize(false, false);
			for (int i = 0; i < 3; i++) {
				chain[i] = (NormalTask) project.createLocalTaskNode(null).getImpl();
				chain[i].setDuration(5L * day());
			}
			side = (NormalTask) project.createLocalTaskNode(null).getImpl();
			side.setDuration(2L * day());
			for (int i = 0; i < 2; i++)
				DependencyService.getInstance().newDependency(chain[i], chain[i + 1], DependencyType.FS, 0L, IncrementalScheduleEquivalenceTest.this);
			project.recalculate();
		}

		List<NormalTask> all() {
			List<NormalTask> list = new ArrayList<>(List.of(chain));
			list.add(side);
			return list;
		}

		List<Long> snapshot() {
			List<Long> dates = new ArrayList<>();
			dates.add(project.getStart());
			dates.add(project.getEnd());
			for (NormalTask t : all()) {
				dates.add(t.getEarlyStart());
				dates.add(t.getEarlyFinish());
				dates.add(t.getLateStart());
				dates.add(t.getLateFinish());
				dates.add(t.getTotalSlack());
				dates.add(t.isCritical() ? 1L : 0L);
			}
			return dates;
		}

		void assertEqualsFullRecalculation(String step) {
			List<Long> incremental = snapshot();
			project.recalculate();
			assertEquals(incremental, snapshot(), "incremental result differs from full recalculation: " + step);
		}
	}

	@Test
	void batchedNoncriticalDurationChangeKeepsLateDatesAndSlackOfFullRecalculation() throws Exception {
		SlackFixture f = new SlackFixture();
		assertEquals(false, f.side.isCritical());
		List<Long> before = f.snapshot();

		int transaction = f.project.fireMultipleTransaction(0, true);
		try {
			setDuration(f.side, 4L * day());
		} finally {
			f.project.fireMultipleTransaction(transaction, false);
		}
		assertNotEquals(before, f.snapshot(), "late start and slack must move");
		f.assertEqualsFullRecalculation("batched noncritical duration increase");
	}

	@Test
	void unbatchedNoncriticalDurationChangeKeepsLateDatesAndSlackOfFullRecalculation() throws Exception {
		SlackFixture f = new SlackFixture();
		List<Long> before = f.snapshot();

		setDuration(f.side, 4L * day());
		assertNotEquals(before, f.snapshot(), "late start and slack must move");
		f.assertEqualsFullRecalculation("unbatched noncritical duration increase");
		setDuration(f.side, day());
		f.assertEqualsFullRecalculation("unbatched noncritical duration decrease");
	}

	@Test
	void assignedNoncriticalDurationChangeWithoutBackwardPassMatchesFullRecalculation() throws Exception {
		SlackFixture f = new SlackFixture();
		AssignmentService.getInstance().newAssignment(f.side, resource(f.project), 1.0d, 0L, this);
		f.project.recalculate();
		assertEquals(true, f.side.hasRealAssignments());
		assertEquals(false, f.side.isCritical());
		List<Long> before = f.snapshot();

		setDuration(f.side, day());
		assertEquals(day(), f.side.getDuration());
		assertNotEquals(before, f.snapshot(), "early finish must move");
		assertEquals(false, f.side.isCritical());
		f.assertEqualsFullRecalculation("assigned noncritical duration decrease");

		setDuration(f.side, 4L * day());
		assertEquals(false, f.side.isCritical());
		f.assertEqualsFullRecalculation("assigned noncritical duration increase within slack");
	}

	@Test
	void batchedAssignedNoncriticalDurationChangeWithoutBackwardPassMatchesFullRecalculation() throws Exception {
		SlackFixture f = new SlackFixture();
		AssignmentService.getInstance().newAssignment(f.side, resource(f.project), 1.0d, 0L, this);
		f.project.recalculate();
		List<Long> before = f.snapshot();

		int transaction = f.project.fireMultipleTransaction(0, true);
		try {
			setDuration(f.side, 4L * day());
		} finally {
			f.project.fireMultipleTransaction(transaction, false);
		}
		assertNotEquals(before, f.snapshot(), "early finish must move");
		assertEquals(false, f.side.isCritical());
		f.assertEqualsFullRecalculation("batched assigned noncritical duration increase within slack");
	}

	@Test
	void noncriticalTaskBecomingCriticalMatchesFullRecalculation() throws Exception {
		SlackFixture f = new SlackFixture();
		assertEquals(false, f.side.isCritical());

		int transaction = f.project.fireMultipleTransaction(0, true);
		try {
			setDuration(f.side, 15L * day());
		} finally {
			f.project.fireMultipleTransaction(transaction, false);
		}
		f.assertEqualsFullRecalculation("batched noncritical task becoming critical");

		setDuration(f.side, 20L * day());
		f.assertEqualsFullRecalculation("unbatched, longer than the old chain");
		setDuration(f.side, 2L * day());
		f.assertEqualsFullRecalculation("unbatched, back to noncritical");
	}

	@Test
	void durationChangingAssignmentOnCriticalChildMatchesFullRecalculation()throws Exception {
		Fixture f = new Fixture(true);
		NormalTask target = f.b[1];
		AssignmentService.getInstance().newAssignment(target, resource(f.project), 1.0d, 0L, this);
		f.project.recalculate();
		List<Long> before = f.snapshot();
		long durationBefore = target.getDuration();

		AssignmentService.getInstance().newAssignment(target, resource(f.project), 1.0d, 0L, this);
		assertNotEquals(durationBefore, target.getDuration(), "second resource must shorten the effort-driven task");
		assertNotEquals(before, f.snapshot(), "dates must move");
		f.assertEqualsFullRecalculation("after duration-changing assignment");

		f.project.getUndoController().undo();
		assertEquals(before, f.snapshot(), "undo must restore all dates");
		f.assertEqualsFullRecalculation("after undo");
	}

	@Test
	void shorteningAssignmentOnLatestFinishingTaskRefreshesScheduleExtrema() throws Exception {
		Fixture f = new Fixture(true);
		NormalTask last = f.b[2];
		AssignmentService.getInstance().newAssignment(last, resource(f.project), 1.0d, 0L, this);
		f.project.recalculate();
		long latestBefore = f.project.getLatestFinishingTask();
		long durationBefore = last.getDuration();

		AssignmentService.getInstance().newAssignment(last, resource(f.project), 1.0d, 0L, this);
		assertNotEquals(durationBefore, last.getDuration(), "second resource must shorten the effort-driven task");
		long latestIncremental = f.project.getLatestFinishingTask();
		long earliestIncremental = f.project.getEarliestStartingTask();
		assertNotEquals(latestBefore, latestIncremental, "latest finish must move");

		f.project.recalculate();
		assertEquals(f.project.getLatestFinishingTask(), latestIncremental, "latest finish differs from full recalculation");
		assertEquals(f.project.getEarliestStartingTask(), earliestIncremental, "earliest start differs from full recalculation");
	}

	@Test
	void batchedShorteningAssignmentOnLatestFinishingTaskRefreshesScheduleExtrema() throws Exception {
		Fixture f = new Fixture(true);
		NormalTask last = f.b[2];
		AssignmentService.getInstance().newAssignment(last, resource(f.project), 1.0d, 0L, this);
		f.project.recalculate();
		long durationBefore = last.getDuration();

		int transaction = f.project.fireMultipleTransaction(0, true);
		try {
			AssignmentService.getInstance().newAssignment(last, resource(f.project), 1.0d, 0L, this);
		} finally {
			f.project.fireMultipleTransaction(transaction, false);
		}
		assertNotEquals(durationBefore, last.getDuration(), "second resource must shorten the effort-driven task");
		long latestIncremental = f.project.getLatestFinishingTask();
		long earliestIncremental = f.project.getEarliestStartingTask();

		f.project.recalculate();
		assertEquals(f.project.getLatestFinishingTask(), latestIncremental, "latest finish differs from full recalculation");
		assertEquals(f.project.getEarliestStartingTask(), earliestIncremental, "earliest start differs from full recalculation");
	}

	@Test
	void durationPreservingAssignmentKeepsAllDatesAndMatchesFullRecalculation() throws Exception {
		Fixture f = new Fixture(true);
		List<Long> before = f.snapshot();
		long durationBefore = f.b[1].getDuration();

		AssignmentService.getInstance().newAssignment(f.b[1], resource(f.project), 1.0d, 0L, this);
		assertEquals(durationBefore, f.b[1].getDuration());
		assertEquals(before, f.snapshot(), "first assignment must not move any date");
		f.assertEqualsFullRecalculation("after duration-preserving assignment");

		f.project.getUndoController().undo();
		assertEquals(before, f.snapshot(), "undo must restore all dates");
	}

	@Test
	void summaryConstraintBoundChangesMatchFullRecalculation() throws Exception {
		Fixture f = new Fixture(true);
		long start = f.phase2.getStart();
		var cal = f.project.getEffectiveWorkCalendar();
		List<Long> before = f.snapshot();

		setConstraint(f, cal.add(start, 10L * day(), false));
		f.assertEqualsFullRecalculation("SNET later than natural start");
		setConstraint(f, cal.add(start, 20L * day(), false));
		f.assertEqualsFullRecalculation("SNET moved later");
		setConstraint(f, cal.add(start, 5L * day(), false));
		f.assertEqualsFullRecalculation("SNET moved earlier");
		setConstraint(f, start);
		f.assertEqualsFullRecalculation("SNET back at natural start");
		assertEquals(before, f.snapshot());
	}

	@Test
	void childDurationChangeAndUndoMatchFullRecalculation() throws Exception {
		Fixture f = new Fixture(true);
		List<Long> before = f.snapshot();

		setDuration(f.a[1], 6L * day());
		assertNotEquals(before, f.snapshot(), "dates must move");
		f.assertEqualsFullRecalculation("after child duration increase");
		setDuration(f.b[2], day());
		f.assertEqualsFullRecalculation("after later child duration decrease");

		f.project.getUndoController().undo();
		f.assertEqualsFullRecalculation("after first undo");
		f.project.getUndoController().undo();
		assertEquals(before, f.snapshot(), "undo must restore all dates");
		f.assertEqualsFullRecalculation("after second undo");
	}

	@Test
	void backwardScheduledTasksWithAssignmentMatchFullRecalculation() throws Exception {
		Fixture f = new Fixture(false);
		AssignmentService.getInstance().newAssignment(f.b[1], resource(f.project), 1.0d, 0L, this);
		f.project.recalculate();
		List<Long> before = f.snapshot();

		AssignmentService.getInstance().newAssignment(f.b[1], resource(f.project), 1.0d, 0L, this);
		f.assertEqualsFullRecalculation("backward, after second assignment");
		AssignmentService.getInstance().newAssignment(f.a[0], resource(f.project), 1.0d, 0L, this);
		f.assertEqualsFullRecalculation("backward, after assignment in phase 1");

		f.project.getUndoController().undo();
		f.project.getUndoController().undo();
		assertEquals(before, f.snapshot(), "undo must restore all dates");
	}
}
