package com.microproject.pm.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.microproject.field.FieldContext;
import com.microproject.options.CalendarOption;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.scheduling.ScheduleInterval;
import com.microproject.pm.scheduling.ScheduleEventListener;
import com.microproject.pm.snapshot.Snapshottable;
import com.microproject.undo.DataFactoryUndoController;

class ProjectScheduleBehaviorTest {
	@Test
	void moveIntervalUpdatesProjectSpan() {
		Project project = createProject();
		long start = project.getStart();
		long end = project.getEffectiveWorkCalendar().add(start, 2L * day(), false);
		project.setStart(start);
		project.setEnd(end);
		ScheduleInterval oldInterval = new ScheduleInterval(start, end);

		long newStart = project.getEffectiveWorkCalendar().add(start, day(), false);
		long newEnd = project.getEffectiveWorkCalendar().add(end, day(), false);

		project.moveInterval(this, newStart, newEnd, oldInterval, false);

		assertEquals(newStart, project.getStart(), "projectStart=" + project.getStart() + " projectEnd=" + project.getEnd());
		assertEquals(newEnd, project.getEnd(), "projectStart=" + project.getStart() + " projectEnd=" + project.getEnd());
	}

	@Test
	void backupDetailRoundTripsProjectSpan() {
		Project project = createProject();
		long originalStart = project.getStart();
		long originalEnd = project.getEffectiveWorkCalendar().add(originalStart, 3L * day(), false);
		project.setStart(originalStart);
		project.setEnd(originalEnd);
		Object backup = project.backupDetail();

		assertNotNull(backup);

		long movedStart = project.getEffectiveWorkCalendar().add(originalStart, day(), false);
		long movedEnd = project.getEffectiveWorkCalendar().add(originalEnd, day(), false);
		project.setStart(movedStart);
		project.setEnd(movedEnd);

		project.restoreDetail(this, backup, false);

		assertEquals(originalStart, project.getStart(), "projectStart=" + project.getStart() + " projectEnd=" + project.getEnd());
		assertEquals(originalEnd, project.getEnd(), "projectStart=" + project.getStart() + " projectEnd=" + project.getEnd());
	}

	@Test
	void setDirtyUpdatesProjectFlag() {
		Project project = createProject();

		project.setDirty(true);

		assertTrue(project.isDirty());
	}

	@Test
	void uniqueIdRoundTripsThroughIdentityFacade() {
		Project project = createProject();

		project.setUniqueId(42L);

		assertEquals(42L, project.getUniqueId());
	}

	@Test
	void taskSheetProjectSummaryEditsUseEnvelope() {
		Project project = createProject();
		NormalTask rootTask = project.createScriptedTask();
		rootTask.setName("Root");
		rootTask.setDuration(2L * day());
		project.connectTask(rootTask);
		project.getTaskOutlines().addToAll(rootTask, null);
		FieldContext context = new FieldContext();
		context.setTaskSheetUpdate(true);

		long manualStart = project.getEffectiveWorkCalendar().add(rootTask.getStart(), -day(), false);
		project.setStart(manualStart, context);
		project.setDuration(10L * day(), context);

		assertTrue(project.hasSummaryEnvelope());
		assertEquals(manualStart, project.getSummaryEnvelope().getManualStart().longValue());
		assertEquals(10L * day(), project.getSummaryEnvelope().getManualDuration().longValue());
		assertEquals(rootTask.getStart(), project.calculateRollupSpan().getStart());
		assertEquals(rootTask.getEnd(), project.calculateRollupSpan().getFinish());
	}

	@Test
	void taskLookupByIdAndUniqueIdFindTheInsertedTask() {
		Project project = createProject();
		NormalTask task = project.createScriptedTask();
		project.connectTask(task);
		project.getTaskOutlines().addToAll(task, null);
		task.setId(42);
		task.setUniqueId(84L);

		assertSame(task, Project.findTaskById(Integer.valueOf(42), project.getTasks()));
		assertSame(task, project.findByUniqueId(84L));
	}

	@Test
	void restoreSnapshotUsesTheSelectedTaskList() {
		Project project = createProject();
		NormalTask task = project.createScriptedTask();
		project.connectTask(task);

		long originalStart = project.getStart();
		long originalDuration = 2L * day();
		task.setStart(originalStart);
		task.setDuration(originalDuration);

		Integer snapshotId = Integer.valueOf(1);
		task.saveCurrentToSnapshot(snapshotId);
		Object backup = task.backupDetail(snapshotId);

		task.setStart(project.getEffectiveWorkCalendar().add(originalStart, day(), false));

		project.restoreSnapshot(snapshotId, false, Collections.singletonList(task), Collections.singletonList(backup));

		assertEquals(originalStart, task.getStart(), "taskStart=" + task.getStart() + " taskEnd=" + task.getEnd());
	}

	@Test
	void clearSnapshotRemovesTheStoredSnapshot() {
		Project project = createProject();
		NormalTask task = project.createScriptedTask();
		project.connectTask(task);

		Integer snapshotId = Integer.valueOf(2);
		task.saveCurrentToSnapshot(snapshotId);

		project.clearSnapshot(snapshotId, false, Collections.singletonList(task), true);

		Object backup = task.backupDetail(snapshotId);
		assertNotNull(backup);
		assertNull(((TaskBackup) backup).snapshot);
	}

	@Test
	void projectBaselinesRemainIndependentFromCurrentAndOtherBaselines() {
		Project project = createProject();
		NormalTask task = project.createScriptedTask();
		project.connectTask(task);
		long originalStart = project.getStart();
		task.setStart(originalStart);
		task.setDuration(day());

		project.saveCurrentToSnapshot(Snapshottable.BASELINE, true, null, false);
		long baselineStart = task.getBaselineStart(Snapshottable.BASELINE);

		task.setStart(project.getEffectiveWorkCalendar().add(originalStart, day(), false));
		project.saveCurrentToSnapshot(Snapshottable.BASELINE_1, true, null, false);

		assertEquals(originalStart, baselineStart);
		assertEquals(baselineStart, task.getBaselineStart(Snapshottable.BASELINE));
		assertEquals(task.getStart(), task.getBaselineStart(Snapshottable.BASELINE_1));
	}

	@Test
	void settingAnUnchangedActualStartDoesNotFireDuplicateScheduleEvents() {
		Project project = createProject();
		NormalTask task = project.createScriptedTask();
		project.connectTask(task);
		int[] events = { 0 };
		ScheduleEventListener listener = event -> events[0]++;
		project.addScheduleListener(listener);

		try {
			task.setActualStart(task.getStart());
			task.setActualStart(task.getStart());
		} finally {
			project.removeScheduleListener(listener);
		}

		assertEquals(1, events[0]);
	}

	private Project createProject() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		return project;
	}

	private long day() {
		return CalendarOption.getInstance().getMillisPerDay();
	}
}
